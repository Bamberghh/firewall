package me.bamberghh.firewall.mixin;

import io.netty.channel.ChannelHandlerContext;
import me.bamberghh.firewall.Firewall;
import me.bamberghh.firewall.util.StringFilter;
import net.fabricmc.fabric.impl.networking.RegistrationPayload;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.PacketCallbacks;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.common.CustomPayloadC2SPacket;
import net.minecraft.network.packet.s2c.common.CustomPayloadS2CPacket;
import net.minecraft.network.packet.s2c.login.LoginQueryRequestPayload;
import net.minecraft.network.packet.s2c.login.LoginQueryRequestS2CPacket;
import net.minecraft.util.Identifier;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@SuppressWarnings("UnstableApiUsage") // Fabric's RegistrationPayload's package is unstable
@Mixin(ClientConnection.class)
public abstract class ClientConnectionMixin {
	@Shadow public abstract void flush();

	@Inject(method = "send(Lnet/minecraft/network/packet/Packet;Lnet/minecraft/network/PacketCallbacks;Z)V", at = @At("HEAD"), cancellable = true)
	private void send(Packet<?> packet, @Nullable PacketCallbacks callbacks, boolean flush, CallbackInfo ci) {
		String packetId = packet.getPacketType().id().toString();
		if (Firewall.CONFIG.loggedPacketIdentifiers.sendMerged().accepts(packetId)) {
			Firewall.LOGGER.info("{}: send packet {}: {}", Firewall.MOD_ID, packetId, packet);
		}
		CustomPayload payload = null;
		String customPayloadId = null;
		if (packet instanceof CustomPayloadC2SPacket(CustomPayload customPayload)) {
			payload = customPayload;
			customPayloadId = payload.getId().id().toString();
			if (Firewall.CONFIG.loggedCustomPayloadIdentifiers.sendMerged().accepts(customPayloadId)) {
				Firewall.LOGGER.info("{}: send custom payload {}: {}", Firewall.MOD_ID, customPayloadId, payload);
			}
		}
		if (!Firewall.CONFIG.packetIdentifiers.sendMerged().accepts(packetId)) {
			Firewall.LOGGER.info("{}: rejected sent packet {}", Firewall.MOD_ID, packetId);
			onSendCancel(flush, callbacks);
			ci.cancel();
			return;
		}
		if (payload == null) {
			return;
		}
		if (!Firewall.CONFIG.customPayloadIdentifiers.sendMerged().accepts(customPayloadId)) {
			Firewall.LOGGER.info("{}: rejected sent custom payload packet {}", Firewall.MOD_ID, customPayloadId);
			onSendCancel(flush, callbacks);
			ci.cancel();
			return;
		}
        if (!(payload instanceof RegistrationPayload registration)) {
			return;
		}
		var sendMerged = Firewall.CONFIG.registerIdentifiers.sendMerged();
		boolean cancel = modifyRegistrationPayload(false, sendMerged, registration);
		if (cancel) {
			onSendCancel(flush, callbacks);
			ci.cancel();
		}
	}

	@Inject(method = "channelRead0(Lio/netty/channel/ChannelHandlerContext;Lnet/minecraft/network/packet/Packet;)V", at = @At("HEAD"), cancellable = true)
	protected void channelRead0(ChannelHandlerContext channelHandlerContext, Packet<?> packet, CallbackInfo ci) {
		String packetId = packet.getPacketType().id().toString();
		if (Firewall.CONFIG.loggedPacketIdentifiers.recvMerged().accepts(packetId)) {
			Firewall.LOGGER.info("{}: receive packet {}: {}", Firewall.MOD_ID, packetId, packet);
		}
		CustomPayload payload = null;
		String customPayloadId = null;
		if (packet instanceof CustomPayloadS2CPacket(CustomPayload customPayload)) {
			payload = customPayload;
			customPayloadId = payload.getId().id().toString();
			if (Firewall.CONFIG.loggedCustomPayloadIdentifiers.recvMerged().accepts(customPayloadId)) {
				Firewall.LOGGER.info("{}: receive custom payload {}: {}", Firewall.MOD_ID, customPayloadId, payload);
			}
		}
		else if (packet instanceof LoginQueryRequestS2CPacket(int queryId, LoginQueryRequestPayload queryPayload)) {
			customPayloadId = queryPayload.id().toString();
			if (Firewall.CONFIG.loggedCustomPayloadIdentifiers.recvMerged().accepts(customPayloadId)) {
				Firewall.LOGGER.info("{}: receive custom query request {}", Firewall.MOD_ID, customPayloadId);
			}
		}
		if (!Firewall.CONFIG.packetIdentifiers.recvMerged().accepts(packetId)) {
			Firewall.LOGGER.info("{}: rejected received packet {}", Firewall.MOD_ID, packetId);
			ci.cancel();
			return;
		}
		if (customPayloadId == null) {
			return;
		}
		if (!Firewall.CONFIG.customPayloadIdentifiers.recvMerged().accepts(customPayloadId)) {
			Firewall.LOGGER.info("{}: rejected received custom {} packet {}",
					Firewall.MOD_ID,
					payload != null ? "payload" : "query request",
					customPayloadId);
			ci.cancel();
			return;
		}
		if (!(payload instanceof RegistrationPayload registration)) {
			return;
		}
		var recvMerged = Firewall.CONFIG.registerIdentifiers.recvMerged();
		boolean cancel = modifyRegistrationPayload(true, recvMerged, registration);
		if (cancel) {
			ci.cancel();
		}
	}

	@Unique
	private void onSendCancel(boolean flush, @Nullable PacketCallbacks callbacks) {
		// Mimic the normal sending behavior when rejecting a packet (cancelling CallbackInfo)
		if (flush) {
			flush();
		}
		if (callbacks != null) {
			callbacks.onSuccess();
		}
	}

	@Unique
	private static Pair<List<Identifier>, List<Identifier>> partitionChannels(List<Identifier> channels, StringFilter filter) {
		if (filter.acceptsNothing()) {
			return Pair.of(channels, Collections.emptyList());
		}
		if (filter.acceptsEverything()) {
			return Pair.of(Collections.emptyList(), channels);
		}
		var partitionedChannels = channels
				.stream()
				.collect(Collectors.partitioningBy(filter::accepts));
		return Pair.of(partitionedChannels.get(false), partitionedChannels.get(true));
	}

	@Unique
	private static boolean modifyRegistrationPayload(boolean recv, StringFilter filter, RegistrationPayload registration) {
		var partitionedChannels = partitionChannels(registration.channels(), filter);
		var rejectedChannels = partitionedChannels.getLeft();
		var acceptedChannels = partitionedChannels.getRight();
		boolean cancel =
				acceptedChannels.isEmpty()
						&& (recv
						? !Firewall.CONFIG.registerIdentifiers.recvEmptyChannelLists()
						: !Firewall.CONFIG.registerIdentifiers.sendEmptyChannelLists());
		if (!rejectedChannels.isEmpty()) {
			((RegistrationPayloadMixin) (Object) registration).setChannels(acceptedChannels);
			Firewall.LOGGER.info("{}: filtered{} {} {} packet channels: rejected: {} ({}); accepted: {} ({})",
					Firewall.MOD_ID,
					cancel ? " & rejected" : "",
					recv ? "received" : "sent",
					registration.id().id(),
					rejectedChannels, rejectedChannels.size(),
					acceptedChannels, acceptedChannels.size());
		}
		return cancel;
	}
}
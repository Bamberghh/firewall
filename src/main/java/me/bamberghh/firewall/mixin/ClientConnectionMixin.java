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
		String packetId = packet.getPacketId().id().toString();
		if (!Firewall.CONFIG.packetIdentifiers.sendMerged().accepts(packetId)) {
			Firewall.LOGGER.info("{}: suppressed sent packet {}", Firewall.MOD_ID, packetId);
			// Mimic the normal sending behavior
			if (flush) {
				flush();
			}
			if (callbacks != null) {
				callbacks.onSuccess();
			}
			ci.cancel();
			return;
		}
		if (!(packet instanceof CustomPayloadC2SPacket(CustomPayload payload))) {
			return;
		}
		String customPayloadId = payload.getId().id().toString();
		if (!Firewall.CONFIG.customPayloadIdentifiers.sendMerged().accepts(customPayloadId)) {
			Firewall.LOGGER.info("{}: suppressed sent custom payload packet {}", Firewall.MOD_ID, customPayloadId);
			// Mimic the normal sending behavior
			if (flush) {
				flush();
			}
			if (callbacks != null) {
				callbacks.onSuccess();
			}
			ci.cancel();
			return;
		}
        if (!(payload instanceof RegistrationPayload registration)) {
			return;
		}
		var sendMerged = Firewall.CONFIG.registerIdentifiers.sendMerged();
		var partitionedChannels = partitionChannels(registration.channels(), sendMerged);
		var rejectedChannels = partitionedChannels.getLeft();
		var acceptedChannels = partitionedChannels.getRight();
		if (!rejectedChannels.isEmpty()) {
			((RegistrationPayloadMixin) (Object) registration).setChannels(acceptedChannels);
			Firewall.LOGGER.info("{}: filtered sent {} packet: rejected: {} ({}); accepted: {} ({})",
					Firewall.MOD_ID,
					registration.id().id(),
					rejectedChannels, rejectedChannels.size(),
					acceptedChannels, acceptedChannels.size());
		}
	}

	@Inject(method = "channelRead0(Lio/netty/channel/ChannelHandlerContext;Lnet/minecraft/network/packet/Packet;)V", at = @At("HEAD"), cancellable = true)
	protected void channelRead0(ChannelHandlerContext channelHandlerContext, Packet<?> packet, CallbackInfo ci) {
		String packetId = packet.getPacketId().id().toString();
		if (!Firewall.CONFIG.packetIdentifiers.recvMerged().accepts(packetId)) {
			Firewall.LOGGER.info("{}: suppressed received packet {}", Firewall.MOD_ID, packetId);
			ci.cancel();
			return;
		}
		if (!(packet instanceof CustomPayloadC2SPacket(CustomPayload payload))) {
			return;
		}
		String customPayloadId = payload.getId().id().toString();
		if (!Firewall.CONFIG.customPayloadIdentifiers.recvMerged().accepts(customPayloadId)) {
			Firewall.LOGGER.info("{}: suppressed received custom payload packet {}", Firewall.MOD_ID, customPayloadId);
			ci.cancel();
			return;
		}
		if (!(payload instanceof RegistrationPayload registration)) {
			return;
		}
		var recvMerged = Firewall.CONFIG.registerIdentifiers.recvMerged();
		var partitionedChannels = partitionChannels(registration.channels(), recvMerged);
		var rejectedChannels = partitionedChannels.getLeft();
		var acceptedChannels = partitionedChannels.getRight();
		if (!rejectedChannels.isEmpty()) {
			((RegistrationPayloadMixin) (Object) registration).setChannels(acceptedChannels);
			Firewall.LOGGER.info("{}: filtered received {} packet: rejected: {} ({}); accepted: {} ({})",
					Firewall.MOD_ID,
					registration.id().id(),
					rejectedChannels, rejectedChannels.size(),
					acceptedChannels, acceptedChannels.size());
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
}
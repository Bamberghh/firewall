package me.bamberghh.firewall.mixin;

import io.netty.channel.ChannelHandlerContext;
import me.bamberghh.firewall.Firewall;
import me.bamberghh.firewall.config.FirewallConfigModel;
import me.bamberghh.firewall.util.RegisterPayloadCommonInterface;
import me.bamberghh.firewall.util.StringFilter;
import net.fabricmc.fabric.impl.networking.CommonRegisterPayload;
import net.fabricmc.fabric.impl.networking.RegistrationPayload;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.NetworkSide;
import net.minecraft.network.PacketCallbacks;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.common.CustomPayloadC2SPacket;
import net.minecraft.network.packet.c2s.login.LoginQueryResponseC2SPacket;
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

import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;

@SuppressWarnings("UnstableApiUsage") // Fabric's RegistrationPayload's package is unstable
@Mixin(ClientConnection.class)
public abstract class ClientConnectionMixin {
	@Unique private static final String LOG_PREFIX_MOD = Firewall.MOD_ID + ": ";
	@Unique private static final String LOG_PREFIX_SERVER_SEND = LOG_PREFIX_MOD + "server-> (send): ";
	@Unique private static final String LOG_PREFIX_SERVER_RECV = LOG_PREFIX_MOD + "->server (recv): ";
	@Unique private static final String LOG_PREFIX_CLIENT_SEND = LOG_PREFIX_MOD + "client-> (send): ";
	@Unique private static final String LOG_PREFIX_CLIENT_RECV = LOG_PREFIX_MOD + "->client (recv): ";

	@Shadow public abstract void flush();

	@Shadow public abstract NetworkSide getSide();

	@Shadow protected abstract void channelRead0(ChannelHandlerContext channelHandlerContext, Packet<?> packet);

	@Shadow public abstract void send(Packet<?> packet);

	@Unique
	private void handlePacket(
			boolean send,
			Packet<?> packet,
			@Nullable PacketCallbacks callbacks,
			boolean flush,
			CallbackInfo ci)
	{
		String logPrefix =
				send
				? getSide() == NetworkSide.SERVERBOUND
						? LOG_PREFIX_SERVER_SEND
						: LOG_PREFIX_CLIENT_SEND
				: getSide() == NetworkSide.SERVERBOUND
						? LOG_PREFIX_SERVER_RECV
						: LOG_PREFIX_CLIENT_RECV;

		FirewallConfigModel.SidedConfig config =
				send
				? Firewall.CONFIG.sendMerged()
				: Firewall.CONFIG.recvMerged();

		String packetId = packet.getPacketType().id().toString();

		if (config.loggedPacketIdentifiers().accepts(packetId)) {
			Firewall.LOGGER.info("{}packet {}: {}", logPrefix, packetId, packet);
		}

		Integer queryRequestId = null;
		String customPayloadId = null;
		CustomPayload payload = null;
		switch (packet) {
            case CustomPayloadC2SPacket(CustomPayload customPayload) -> payload = customPayload;
            case CustomPayloadS2CPacket(CustomPayload customPayload) -> payload = customPayload;
            case LoginQueryRequestS2CPacket(int queryId, LoginQueryRequestPayload queryRequestPayload) -> {
                customPayloadId = queryRequestPayload.id().toString();
                if (config.loggedCustomPayloadIdentifiers().accepts(customPayloadId)) {
                    Firewall.LOGGER.info("{}custom query request {}", logPrefix, customPayloadId);
                }
				queryRequestId = queryId;
            }
			// LoginQueryResponseC2SPacket shouldn't be filtered since the response is needed during login.
            default -> {}
        };
		if (payload != null) {
			customPayloadId = payload.getId().id().toString();
			if (config.loggedCustomPayloadIdentifiers().accepts(customPayloadId)) {
				Firewall.LOGGER.info("{}custom payload {}: {}", logPrefix, customPayloadId, payload);
			}
		}

		if (!config.packetIdentifiers().accepts(packetId)) {
			Firewall.LOGGER.info("{}rejected packet {}", logPrefix, packetId);
			if (send) onSendCancel(flush, callbacks);
			else onRecvCancel(queryRequestId);
			ci.cancel();
			return;
		}
		if (customPayloadId == null) {
			return;
		}
		if (!config.customPayloadIdentifiers().accepts(customPayloadId)) {
			Firewall.LOGGER.info("{}rejected custom payload packet {}", logPrefix, customPayloadId);
			if (send) onSendCancel(flush, callbacks);
			else onRecvCancel(queryRequestId);
			ci.cancel();
			return;
		}
		if (payload == null) {
			return;
		}
		RegisterPayloadCommonInterface registerCommon = null;
		if (payload instanceof RegistrationPayload registration) {
			registerCommon = (RegisterPayloadCommonInterface) (Object) registration;
		}
		else if (payload instanceof CommonRegisterPayload register) {
			registerCommon = (RegisterPayloadCommonInterface) (Object) register;
		}
		// For some reason IntelliJ says that registerCommon is always null, but that's a false positive.
		//noinspection ConstantValue
		if (registerCommon != null) {
			var partitionedChannels = partitionChannels(registerCommon.firewall$channelsCollection(), config.registerIdentifiers());
			var rejectedChannels = partitionedChannels.getLeft();
			var acceptedChannels = partitionedChannels.getRight();
			boolean cancel =
					acceptedChannels.isEmpty()
							&& (!send
							? !Firewall.CONFIG.registerIdentifiers.recvEmptyChannelLists()
							: !Firewall.CONFIG.registerIdentifiers.sendEmptyChannelLists());
			if (!rejectedChannels.isEmpty()) {
				registerCommon.firewall$setChannelsCollection(acceptedChannels);
				Firewall.LOGGER.info("{}filtered{} {} packet channels: rejected: {} ({}); accepted: {} ({})", logPrefix,
						cancel ? " & rejected" : "",
						payload.getId().id(),
						rejectedChannels, rejectedChannels.size(),
						acceptedChannels, acceptedChannels.size());
			}
			if (cancel) {
				if (send) onSendCancel(flush, callbacks);
				else onRecvCancel(queryRequestId);
				ci.cancel();
			}
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
	private void onRecvCancel(@Nullable Integer queryRequestId) {
		if (queryRequestId == null) {
			return;
		}
		if (!Firewall.CONFIG.customPayloadIdentifiers.respondToRejectedQueryRequests()) {
			return;
		}
		// Send the vanilla response to the request because as I understand it,
		// the query request of the configuration phase requires a response otherwise it breaks.
		send(new LoginQueryResponseC2SPacket(queryRequestId, null));
	}

	@Unique
	private static Pair<Collection<Identifier>, Collection<Identifier>> partitionChannels(Collection<Identifier> channels, StringFilter filter) {
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
	private static boolean modifyRegistrationPayload(String logPrefix, boolean send, StringFilter filter, RegisterPayloadCommonInterface payload) {
		var partitionedChannels = partitionChannels(payload.firewall$channelsCollection(), filter);
		var rejectedChannels = partitionedChannels.getLeft();
		var acceptedChannels = partitionedChannels.getRight();
		boolean cancel =
				acceptedChannels.isEmpty()
						&& (!send
						? !Firewall.CONFIG.registerIdentifiers.recvEmptyChannelLists()
						: !Firewall.CONFIG.registerIdentifiers.sendEmptyChannelLists());
		if (!rejectedChannels.isEmpty()) {
			payload.firewall$setChannelsCollection(acceptedChannels);
			Firewall.LOGGER.info("{}filtered{} {} packet channels: rejected: {} ({}); accepted: {} ({})", logPrefix,
					cancel ? " & rejected" : "",
					payload.getId().id(),
					rejectedChannels, rejectedChannels.size(),
					acceptedChannels, acceptedChannels.size());
		}
		return cancel;
	}

	@Inject(method = "send(Lnet/minecraft/network/packet/Packet;Lnet/minecraft/network/PacketCallbacks;Z)V", at = @At("HEAD"), cancellable = true)
	private void send(Packet<?> packet, @Nullable PacketCallbacks callbacks, boolean flush, CallbackInfo ci) {
		handlePacket(true, packet, callbacks, flush, ci);
	}

	@Inject(method = "channelRead0(Lio/netty/channel/ChannelHandlerContext;Lnet/minecraft/network/packet/Packet;)V", at = @At("HEAD"), cancellable = true)
	protected void channelRead0(ChannelHandlerContext channelHandlerContext, Packet<?> packet, CallbackInfo ci) {
		handlePacket(false, packet, null, false, ci);
	}
}
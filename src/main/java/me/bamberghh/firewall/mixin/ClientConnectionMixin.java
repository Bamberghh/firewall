package me.bamberghh.firewall.mixin;

import io.netty.channel.ChannelHandlerContext;
import me.bamberghh.firewall.Firewall;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.PacketCallbacks;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.common.CustomPayloadC2SPacket;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientConnection.class)
public class ClientConnectionMixin {
	@Inject(method = "send(Lnet/minecraft/network/packet/Packet;Lnet/minecraft/network/PacketCallbacks;Z)V", at = @At("HEAD"), cancellable = true)
	private void send(Packet<?> packet, @Nullable PacketCallbacks callbacks, boolean flush, CallbackInfo ci) {
		String packetId = packet.getPacketId().id().toString();
		if ( !(Firewall.CONFIG.packetIdentifiers.comm().accepts(packetId)
			&& Firewall.CONFIG.packetIdentifiers.send().accepts(packetId))) {
			Firewall.LOGGER.info("{}: suppressed sent packet {}", Firewall.MOD_ID, packetId);
			ci.cancel();
			return;
		}
		if (!(packet instanceof CustomPayloadC2SPacket(CustomPayload payload))) {
			return;
		}
		String customPayloadId = payload.getId().id().toString();
		if ( !(Firewall.CONFIG.customPayloadIdentifiers.comm().accepts(customPayloadId)
			&& Firewall.CONFIG.customPayloadIdentifiers.send().accepts(customPayloadId))) {
			Firewall.LOGGER.info("{}: suppressed sent custom payload packet {}", Firewall.MOD_ID, customPayloadId);
			ci.cancel();
		}
	}

	@Inject(method = "channelRead0(Lio/netty/channel/ChannelHandlerContext;Lnet/minecraft/network/packet/Packet;)V", at = @At("HEAD"), cancellable = true)
	protected void channelRead0(ChannelHandlerContext channelHandlerContext, Packet<?> packet, CallbackInfo ci) {
		String packetId = packet.getPacketId().id().toString();
		if ( !(Firewall.CONFIG.packetIdentifiers.comm().accepts(packetId)
			&& Firewall.CONFIG.packetIdentifiers.recv().accepts(packetId))) {
			Firewall.LOGGER.info("{}: suppressed received packet {}", Firewall.MOD_ID, packetId);
			ci.cancel();
			return;
		}
		if (!(packet instanceof CustomPayloadC2SPacket(CustomPayload payload))) {
			return;
		}
		String customPayloadId = payload.getId().id().toString();
		if ( !(Firewall.CONFIG.customPayloadIdentifiers.comm().accepts(customPayloadId)
			&& Firewall.CONFIG.customPayloadIdentifiers.recv().accepts(customPayloadId))) {
			Firewall.LOGGER.info("{}: suppressed received custom payload packet {}", Firewall.MOD_ID, customPayloadId);
			ci.cancel();
		}
	}
}
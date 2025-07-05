package me.bamberghh.firewall.mixin;

import io.netty.channel.ChannelHandlerContext;
import me.bamberghh.firewall.Firewall;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.PacketCallbacks;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.common.CustomPayloadC2SPacket;
import net.minecraft.network.packet.s2c.common.CustomPayloadS2CPacket;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientConnection.class)
public class ClientConnectionMixin {
	@Inject(method = "send(Lnet/minecraft/network/packet/Packet;Lnet/minecraft/network/PacketCallbacks;Z)V", at = @At("HEAD"), cancellable = true)
	private void send(Packet<?> packet, @Nullable PacketCallbacks callbacks, boolean flush, CallbackInfo ci) {
		if (!Firewall.CONFIG.packetIdentifiers.send().accepts(packet.getPacketId().id())) {
			Firewall.LOGGER.info("{}: suppressed sent packet {}", Firewall.MOD_ID, packet.getPacketId().id());
			ci.cancel();
			return;
		}
		if (packet instanceof CustomPayloadC2SPacket(CustomPayload payload)
				&& !Firewall.CONFIG.customPayloadIdentifiers.send().accepts(payload.getId().id())) {
			Firewall.LOGGER.info("{}: suppressed sent custom payload packet {}", Firewall.MOD_ID, payload.getId().id());
			ci.cancel();
		}
	}

	@Inject(method = "channelRead0(Lio/netty/channel/ChannelHandlerContext;Lnet/minecraft/network/packet/Packet;)V", at = @At("HEAD"), cancellable = true)
	protected void channelRead0(ChannelHandlerContext channelHandlerContext, Packet<?> packet, CallbackInfo ci) {
		if (!Firewall.CONFIG.packetIdentifiers.recv().accepts(packet.getPacketId().id())) {
			Firewall.LOGGER.info("{}: suppressed received packet {}", Firewall.MOD_ID, packet.getPacketId().id());
			ci.cancel();
			return;
		}
		if (packet instanceof CustomPayloadS2CPacket(CustomPayload payload)
				&& !Firewall.CONFIG.customPayloadIdentifiers.recv().accepts(payload.getId().id())) {
			Firewall.LOGGER.info("{}: suppressed received custom payload packet {}", Firewall.MOD_ID, payload.getId().id());
			ci.cancel();
		}
	}
}
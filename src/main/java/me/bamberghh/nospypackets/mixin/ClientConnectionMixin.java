package me.bamberghh.nospypackets.mixin;

import me.bamberghh.nospypackets.NoSpyPackets;
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
	@Inject(at = @At("HEAD"), method = "send(Lnet/minecraft/network/packet/Packet;Lnet/minecraft/network/PacketCallbacks;Z)V", cancellable = true)
	private void send(Packet<?> packet, @Nullable PacketCallbacks callbacks, boolean flush, CallbackInfo info) {
		if (!(packet instanceof CustomPayloadC2SPacket(CustomPayload payload))) {
			return;
		}
		if (NoSpyPackets.CONFIG.suppressedSentCustomPayloadIdentifiers().isEmpty()) {
			return;
		}
		String payloadIdString = payload.getId().id().toString();
		if (NoSpyPackets.CONFIG.suppressedSentCustomPayloadIdentifiers().contains(payloadIdString)) {
			NoSpyPackets.LOGGER.info("no-spy-packets: suppressed sent packet {}", payloadIdString);
			info.cancel();
		}
	}
}
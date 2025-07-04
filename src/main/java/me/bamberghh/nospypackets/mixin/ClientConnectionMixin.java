package me.bamberghh.nospypackets.mixin;

import me.bamberghh.nospypackets.NoSpyPackets;
import me.bamberghh.nospypackets.util.StringMask;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.PacketCallbacks;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.common.CustomPayloadC2SPacket;
import net.minecraft.util.Identifier;
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
		StringMask idMask = NoSpyPackets.CONFIG.sentCustomPayloadIdentifiers();
		if (idMask.acceptsEverything()) {
			return;
		}
		Identifier payloadId = payload.getId().id();
		if (idMask.acceptsNothing() || idMask.accepts(payloadId.toString())) {
			NoSpyPackets.LOGGER.info("no-spy-packets: suppressed sent packet {}", payloadId);
			info.cancel();
		}
	}
}
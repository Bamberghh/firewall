package me.bamberghh.nospypackets.mixin;

import me.bamberghh.nospypackets.NoSpyPackets;
import net.minecraft.client.network.ClientCommonNetworkHandler;
import net.minecraft.network.listener.ClientCommonPacketListener;
import net.minecraft.network.listener.ClientCookieRequestPacketListener;
import net.minecraft.network.packet.s2c.common.CustomPayloadS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientCommonNetworkHandler.class)
public abstract class ClientCommonNetworkHandlerMixin {
    @Inject(method = "onCustomPayload(Lnet/minecraft/network/packet/s2c/common/CustomPayloadS2CPacket;)V", at = @At("HEAD"), cancellable = true)
    void onCustomPayload(CustomPayloadS2CPacket packet, CallbackInfo ci) {
        if (NoSpyPackets.CONFIG_suppressedReceivedCustomPayloadIdentifiersSet.isEmpty()) {
            return;
        }
        String payloadIdString = packet.payload().getId().id().toString();
        if (NoSpyPackets.CONFIG_suppressedReceivedCustomPayloadIdentifiersSet.contains(payloadIdString)) {
            NoSpyPackets.LOGGER.info("no-spy-packets: suppressed received packet {}", payloadIdString);
            ci.cancel();
        }
    }
}

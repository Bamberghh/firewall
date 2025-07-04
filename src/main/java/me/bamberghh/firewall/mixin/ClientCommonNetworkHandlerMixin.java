package me.bamberghh.firewall.mixin;

import me.bamberghh.firewall.Firewall;
import me.bamberghh.firewall.util.StringMask;
import net.minecraft.client.network.ClientCommonNetworkHandler;
import net.minecraft.network.packet.s2c.common.CustomPayloadS2CPacket;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientCommonNetworkHandler.class)
public abstract class ClientCommonNetworkHandlerMixin {
    @Inject(method = "onCustomPayload(Lnet/minecraft/network/packet/s2c/common/CustomPayloadS2CPacket;)V", at = @At("HEAD"), cancellable = true)
    void onCustomPayload(CustomPayloadS2CPacket packet, CallbackInfo ci) {
        StringMask idMask = Firewall.CONFIG.receivedCustomPayloadIdentifiers();
        if (idMask.acceptsEverything()) {
            return;
        }
        Identifier payloadId = packet.payload().getId().id();
        if (idMask.acceptsNothing() || idMask.accepts(payloadId.toString())) {
            Firewall.LOGGER.info("firewall: suppressed received packet {}", payloadId);
            ci.cancel();
        }
    }
}

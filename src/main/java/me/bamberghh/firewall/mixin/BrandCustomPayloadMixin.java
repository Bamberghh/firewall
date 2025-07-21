package me.bamberghh.firewall.mixin;

import me.bamberghh.firewall.Firewall;
import net.minecraft.network.packet.BrandCustomPayload;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BrandCustomPayload.class)
public class BrandCustomPayloadMixin {
    @Mutable
    @Shadow @Final private String brand;

    @Inject(at = @At("RETURN"), method = "<init>(Ljava/lang/String;)V")
    private void init(String string, CallbackInfo ci) {
        if (Firewall.CONFIG.shouldOverwriteBrand()) {
            this.brand = Firewall.CONFIG.brandOverwriteValue();
        }
    }
}
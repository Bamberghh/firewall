package me.bamberghh.nospypackets.mixin;

import me.bamberghh.nospypackets.NoSpyPackets;
import me.bamberghh.nospypackets.NoSpyPacketsConfigModel;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.PacketCallbacks;
import net.minecraft.network.packet.BrandCustomPayload;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.common.CustomPayloadC2SPacket;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.amymialee.visiblebarriers.common.VisibleBarriersCommon;

@Mixin(BrandCustomPayload.class)
public class BrandCustomPayloadMixin {
	@Mutable
	@Shadow @Final private String brand;

	@Inject(at = @At("RETURN"), method = "<init>(Ljava/lang/String;)V")
	private void init(String string, CallbackInfo ci) {
		if (NoSpyPackets.CONFIG.shouldOverwriteBrand()) {
			this.brand = NoSpyPackets.CONFIG.brandOverwriteValue();
		}
	}
}
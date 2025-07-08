package me.bamberghh.firewall.mixin;

import net.fabricmc.fabric.impl.networking.RegistrationPayload;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@SuppressWarnings("UnstableApiUsage")
@Mixin(RegistrationPayload.class)
public interface RegistrationPayloadMixin {
    @Mutable
    @Accessor("channels")
    void setChannels(List<Identifier> channels);
}

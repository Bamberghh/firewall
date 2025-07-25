package me.bamberghh.firewall.mixin;

import me.bamberghh.firewall.util.RegisterPayloadCommonInterface;
import net.fabricmc.fabric.impl.networking.RegistrationPayload;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.*;

@SuppressWarnings("UnstableApiUsage")
@Mixin(value = RegistrationPayload.class, remap = false)
public abstract class RegistrationPayloadMixin implements RegisterPayloadCommonInterface {
    @Shadow public abstract List<Identifier> channels();

    @Mutable
    @Accessor("channels")
    abstract void setChannels(List<Identifier> channels);

    @Override
    public Collection<Identifier> firewall$channelsCollection() {
        return channels();
    }

    @Override
    public void firewall$setChannelsCollection(Collection<Identifier> channels) {
        setChannels(channels instanceof List<Identifier> channelsList ? channelsList : new ArrayList<>(channels));
    }
}

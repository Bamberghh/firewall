package me.bamberghh.firewall.mixin;

import me.bamberghh.firewall.util.RegisterPayloadCommonInterface;
import net.fabricmc.fabric.impl.networking.CommonRegisterPayload;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.*;

@SuppressWarnings("UnstableApiUsage")
@Mixin(value = CommonRegisterPayload.class, remap = false)
public abstract class CommonRegisterPayloadMixin implements RegisterPayloadCommonInterface {
    @Shadow public abstract Set<Identifier> channels();

    @Mutable
    @Accessor("channels")
    abstract void setChannels(Set<Identifier> channels);

    @Override
    public Collection<Identifier> firewall$channelsCollection() {
        return channels();
    }

    @Override
    public void firewall$setChannelsCollection(Collection<Identifier> channels) {
        setChannels(channels instanceof Set<Identifier> channelsList ? channelsList : new HashSet<>(channels));
    }
}

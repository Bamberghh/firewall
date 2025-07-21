package me.bamberghh.firewall.util;

import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.Collection;

public interface RegisterPayloadCommonInterface extends CustomPayload {
    Collection<Identifier> firewall$channelsCollection();
    void firewall$setChannelsCollection(Collection<Identifier> channels);
}

package me.bamberghh.firewall.util;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.Collection;

public interface RegisterPayloadCommonInterface extends CustomPacketPayload {
    Collection<Identifier> firewall$channelsCollection();
    void firewall$setChannelsCollection(Collection<Identifier> channels);
}

package me.bamberghh.firewall.mixin;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import me.bamberghh.firewall.Firewall;
import me.bamberghh.firewall.config.FirewallConfigModel;
import me.bamberghh.firewall.util.RegisterPayloadCommonInterface;
import me.bamberghh.firewall.util.StringFilter;
import net.fabricmc.fabric.impl.networking.CommonRegisterPayload;
import net.fabricmc.fabric.impl.networking.RegistrationPayload;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.login.ClientboundCustomQueryPacket;
import net.minecraft.network.protocol.login.ServerboundCustomQueryAnswerPacket;
import net.minecraft.network.protocol.login.custom.CustomQueryPayload;
import net.minecraft.resources.Identifier;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;

@SuppressWarnings("UnstableApiUsage") // Fabric's RegistrationPayload's package is unstable
@Mixin(Connection.class)
public abstract class ConnectionMixin {
    @Unique private static final String LOG_PREFIX_MOD = Firewall.MOD_ID + ": ";
    @Unique private static final String LOG_PREFIX_SERVER_SEND = LOG_PREFIX_MOD + "server-> (send): ";
    @Unique private static final String LOG_PREFIX_SERVER_RECV = LOG_PREFIX_MOD + "->server (recv): ";
    @Unique private static final String LOG_PREFIX_CLIENT_SEND = LOG_PREFIX_MOD + "client-> (send): ";
    @Unique private static final String LOG_PREFIX_CLIENT_RECV = LOG_PREFIX_MOD + "->client (recv): ";

    @Shadow public abstract PacketFlow getReceiving();

    @Shadow public abstract void send(Packet<?> packet);

    @Shadow private Channel channel;

    @Unique
    private void firewall$handlePacket(
            boolean send,
            Packet<?> packet,
            @Nullable ChannelFutureListener channelFutureListener,
            boolean flush,
            CallbackInfo ci)
    {
        if (!Firewall.CONFIG.isEnabled()) return;

        String logPrefix =
                send
                ? getReceiving() == PacketFlow.SERVERBOUND
                        ? LOG_PREFIX_SERVER_SEND
                        : LOG_PREFIX_CLIENT_SEND
                : getReceiving() == PacketFlow.SERVERBOUND
                        ? LOG_PREFIX_SERVER_RECV
                        : LOG_PREFIX_CLIENT_RECV;

        FirewallConfigModel.SidedConfig config =
                send
                ? Firewall.CONFIG.sendMerged()
                : Firewall.CONFIG.recvMerged();

        String packetId = packet.type().id().toString();

        if (Firewall.CONFIG.logging.isEnabled() && config.loggedPacketIdentifiers().accepts(packetId)) {
            Firewall.LOGGER.info("{}packet {}: {}", logPrefix, packetId, packet);
        }

        Integer queryRequestId = null;
        String customPayloadId = null;
        CustomPacketPayload payload = null;
        switch (packet) {
            case ServerboundCustomPayloadPacket(CustomPacketPayload customPayload) -> payload = customPayload;
            case ClientboundCustomPayloadPacket(CustomPacketPayload customPayload) -> payload = customPayload;
            case ClientboundCustomQueryPacket(int queryId, CustomQueryPayload queryRequestPayload) -> {
                customPayloadId = queryRequestPayload.id().toString();
                if (Firewall.CONFIG.logging.isEnabled() && config.loggedCustomPayloadIdentifiers().accepts(customPayloadId)) {
                    Firewall.LOGGER.info("{}custom query request {}", logPrefix, customPayloadId);
                }
                queryRequestId = queryId;
            }
            // ServerboundCustomQueryAnswerPacket shouldn't be filtered since the response is needed during login.
            default -> {}
        };
        if (payload != null) {
            customPayloadId = payload.type().id().toString();
            if (Firewall.CONFIG.logging.isEnabled() && config.loggedCustomPayloadIdentifiers().accepts(customPayloadId)) {
                Firewall.LOGGER.info("{}custom payload {}: {}", logPrefix, customPayloadId, payload);
            }
        }

        if (!config.packetIdentifiers().accepts(packetId)) {
            if (Firewall.CONFIG.logging.isEnabled()) {
                Firewall.LOGGER.info("{}rejected packet {}", logPrefix, packetId);
            }
            if (send) firewall$onSendCancel(channelFutureListener, flush);
            else firewall$onRecvCancel(queryRequestId);
            ci.cancel();
            return;
        }
        if (customPayloadId == null) {
            return;
        }
        if (!config.customPayloadIdentifiers().accepts(customPayloadId)) {
            if (Firewall.CONFIG.logging.isEnabled()) {
                Firewall.LOGGER.info("{}rejected custom payload packet {}", logPrefix, customPayloadId);
            }
            if (send) firewall$onSendCancel(channelFutureListener, flush);
            else firewall$onRecvCancel(queryRequestId);
            ci.cancel();
            return;
        }
        if (payload == null) {
            return;
        }
        RegisterPayloadCommonInterface registerCommon = null;
        if (payload instanceof RegistrationPayload registration) {
            registerCommon = (RegisterPayloadCommonInterface) (Object) registration;
        }
        else if (payload instanceof CommonRegisterPayload register) {
            registerCommon = (RegisterPayloadCommonInterface) (Object) register;
        }
        // For some reason IntelliJ says that registerCommon is always null, but that's a false positive.
        //noinspection ConstantValue
        if (registerCommon != null) {
            var partitionedChannels = firewall$partitionChannels(registerCommon.firewall$channelsCollection(), config.registerIdentifiers());
            var rejectedChannels = partitionedChannels.getLeft();
            var acceptedChannels = partitionedChannels.getRight();
            boolean cancel =
                    acceptedChannels.isEmpty()
                            && (!send
                            ? !Firewall.CONFIG.registerIdentifiers.recvEmptyChannelLists()
                            : !Firewall.CONFIG.registerIdentifiers.sendEmptyChannelLists());
            if (!rejectedChannels.isEmpty()) {
                registerCommon.firewall$setChannelsCollection(acceptedChannels);
                if (Firewall.CONFIG.logging.isEnabled()) {
                    Firewall.LOGGER.info("{}filtered{} {} packet channels: rejected: {} ({}); accepted: {} ({})", logPrefix,
                            cancel ? " & rejected" : "",
                            payload.type().id(),
                            rejectedChannels, rejectedChannels.size(),
                            acceptedChannels, acceptedChannels.size());
                }
            }
            if (cancel) {
                if (send) firewall$onSendCancel(channelFutureListener, flush);
                else firewall$onRecvCancel(queryRequestId);
                ci.cancel();
            }
        }
    }

    @Unique
    private void firewall$onSendCancel(@Nullable ChannelFutureListener channelFutureListener, boolean flush) {
        // Mimic the normal sending behavior when rejecting a packet (cancelling CallbackInfo)
        if (channelFutureListener != null) {
            channel.newSucceededFuture().addListener(channelFutureListener);
        }
        if (flush) {
            channel.flush();
        }
    }

    @Unique
    private void firewall$onRecvCancel(@Nullable Integer queryRequestId) {
        if (queryRequestId == null) {
            return;
        }
        if (!Firewall.CONFIG.customPayloadIdentifiers.respondToRejectedQueryRequests()) {
            return;
        }
        // Send the vanilla response to the request because as I understand it,
        // the query request of the configuration phase requires a response otherwise it breaks.
        send(new ServerboundCustomQueryAnswerPacket(queryRequestId, null));
    }

    @Unique
    private static Pair<Collection<Identifier>, Collection<Identifier>> firewall$partitionChannels(Collection<Identifier> channels, StringFilter filter) {
        if (filter.acceptsNothing()) {
            return Pair.of(channels, Collections.emptyList());
        }
        if (filter.acceptsEverything()) {
            return Pair.of(Collections.emptyList(), channels);
        }
        var partitionedChannels = channels
                .stream()
                .collect(Collectors.partitioningBy(filter::accepts));
        return Pair.of(partitionedChannels.get(false), partitionedChannels.get(true));
    }

    @Inject(method = "doSendPacket", at = @At("HEAD"), cancellable = true)
    private void sendInternal(Packet<?> packet, @Nullable ChannelFutureListener channelFutureListener, boolean flush, CallbackInfo ci) {
        firewall$handlePacket(true, packet, channelFutureListener, flush, ci);
    }

    @Inject(method = "channelRead0(Lio/netty/channel/ChannelHandlerContext;Lnet/minecraft/network/protocol/Packet;)V", at = @At("HEAD"), cancellable = true)
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, Packet<?> packet, CallbackInfo ci) {
        firewall$handlePacket(false, packet, null, false, ci);
    }
}
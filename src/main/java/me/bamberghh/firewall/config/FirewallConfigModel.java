package me.bamberghh.firewall.config;

import io.wispforest.endec.Endec;
import io.wispforest.owo.config.ConfigWrapper;
import io.wispforest.owo.config.annotation.*;
import me.bamberghh.firewall.util.StringMask;

import java.util.regex.Pattern;

@SuppressWarnings("unused")
@Config(name = "firewall-config", wrapperName = "FirewallConfig", defaultHook = true)
public class FirewallConfigModel {
    public static class SendRecvStringMask {
        public StringMask send = new StringMask();
        public StringMask recv = new StringMask();
    }
    @Nest
    public SendRecvStringMask packetIdentifiers = new SendRecvStringMask();
    @Nest
    public SendRecvStringMask customPayloadIdentifiers = new SendRecvStringMask();

    public boolean shouldOverwriteBrand = false;
    public String brandOverwriteValue = "vanilla";

    public static void builderConsumer(ConfigWrapper.SerializationBuilder builder) {
        builder.addEndec(Pattern.class, Endec.STRING.xmap(
                Pattern::compile,
                Pattern::pattern));
    }
}

package me.bamberghh.nospypackets.config;

import blue.endless.jankson.JsonPrimitive;
import io.wispforest.endec.Endec;
import io.wispforest.owo.config.ConfigWrapper;
import io.wispforest.owo.config.annotation.*;
import io.wispforest.owo.ui.core.Color;
import me.bamberghh.nospypackets.util.IndexHashSet;
import me.bamberghh.nospypackets.util.StringMask;
import net.minecraft.util.Identifier;

import java.util.*;
import java.util.regex.Pattern;

@SuppressWarnings("unused")
@Config(name = "no-spy-packets-config", wrapperName = "NoSpyPacketsConfig", defaultHook = true)
public class NoSpyPacketsConfigModel {
    public StringMask sentCustomPayloadIdentifiers = new StringMask();
    public StringMask receivedCustomPayloadIdentifiers = new StringMask();

    public boolean shouldOverwriteBrand = false;
    public String brandOverwriteValue = "vanilla";

    public static void builderConsumer(ConfigWrapper.SerializationBuilder builder) {
        builder.addEndec(Pattern.class, Endec.STRING.xmap(
                Pattern::compile,
                Pattern::pattern));
    }
}

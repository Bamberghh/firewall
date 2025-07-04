package me.bamberghh.nospypackets.config;

import io.wispforest.owo.config.annotation.*;
import me.bamberghh.nospypackets.util.IndexHashSet;

import java.util.*;

@SuppressWarnings("unused")
@Config(name = "no-spy-packets-config", wrapperName = "NoSpyPacketsConfig", defaultHook = true)
public class NoSpyPacketsConfigModel {
    public Set<String> suppressedSentCustomPayloadIdentifiers = new IndexHashSet<>();
    public Set<String> suppressedReceivedCustomPayloadIdentifiers = new IndexHashSet<>();

    public boolean shouldOverwriteBrand = false;
    public String brandOverwriteValue = "vanilla";
}

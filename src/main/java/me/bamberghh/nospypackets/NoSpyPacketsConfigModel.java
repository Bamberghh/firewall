package me.bamberghh.nospypackets;

import io.wispforest.owo.config.annotation.*;

import java.util.*;

@SuppressWarnings("unused")
@Modmenu(modId = "no-spy-packets")
@Config(name = "no-spy-packets-config", wrapperName = "NoSpyPacketsConfig", defaultHook = true)
public class NoSpyPacketsConfigModel {
    public List<String> suppressedSentCustomPayloadIdentifiers = new ArrayList<>();
    public List<String> suppressedReceivedCustomPayloadIdentifiers = new ArrayList<>();

    public boolean shouldOverwriteBrand = false;
    public String brandOverwriteValue = "vanilla";
}

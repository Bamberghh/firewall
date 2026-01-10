package me.bamberghh.firewall;

import me.bamberghh.firewall.config.ConfigInit;
import me.bamberghh.firewall.config.FirewallConfig;
import me.bamberghh.firewall.config.FirewallConfigModel;
import net.fabricmc.api.ModInitializer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;

public class Firewall implements ModInitializer {
    public static final String MOD_ID = "firewall";

    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static final FirewallConfig CONFIG = FirewallConfig.createAndLoad(FirewallConfigModel::builderConsumer);

    public static void sendStatusMessage(@Nullable MinecraftClient client, Text message) {
        if (client == null) return;
        var player = client.player;
        if (player == null) return;
        player.sendMessage(message, true);
    }

    public static void setEnabled(@Nullable MinecraftClient client, boolean isEnabled) {
        CONFIG.isEnabled(isEnabled);
        sendStatusMessage(client, Text.translatable(isEnabled ? "firewall.status.enabled" : "firewall.status.disabled"));
    }

    public static void setLoggingEnabled(@Nullable MinecraftClient client, boolean isEnabled) {
        CONFIG.logging.isEnabled(isEnabled);
        sendStatusMessage(client, Text.translatable(isEnabled ? "firewall.status.logging.enabled" : "firewall.status.logging.disabled"));
    }

    @Override
    public void onInitialize() {
        try {
            ConfigInit.init(CONFIG);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException | InstantiationException e) {
            throw new RuntimeException(e);
        }
    }
}
package me.bamberghh.firewall;

import io.wispforest.owo.config.ui.ConfigScreenProviders;
import me.bamberghh.firewall.config.FirewallConfigScreen;
import net.fabricmc.api.ClientModInitializer;

public class FirewallClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ConfigScreenProviders.register(Firewall.MOD_ID, screen -> FirewallConfigScreen.create(Firewall.CONFIG, screen));
    }
}

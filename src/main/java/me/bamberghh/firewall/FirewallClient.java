package me.bamberghh.firewall;

import io.wispforest.owo.config.ui.ConfigScreenProviders;
import me.bamberghh.firewall.config.FirewallConfigScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

public class FirewallClient implements ClientModInitializer {
    public static KeyBindings keyBindings;
    @Override
    public void onInitializeClient() {
        keyBindings = new KeyBindings();
        ConfigScreenProviders.register(Firewall.MOD_ID, screen -> FirewallConfigScreen.create(Firewall.CONFIG, screen));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            keyBindings.handle(client);
        });
    }
}

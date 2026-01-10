package me.bamberghh.firewall;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import org.lwjgl.glfw.GLFW;

public class KeyBindings {
    public String category;
    public KeyBinding toggleEnabled;
    public KeyBinding toggleLoggingEnabled;

    public KeyBindings() {
        this.category = "%s.main".formatted(Firewall.MOD_ID);
        this.toggleEnabled = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.firewall.toggleEnabled",
                GLFW.GLFW_KEY_UNKNOWN,
                this.category));
        this.toggleLoggingEnabled = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.firewall.toggleLoggingEnabled",
                GLFW.GLFW_KEY_UNKNOWN,
                this.category));
    }

    public void handle(MinecraftClient client) {
        while (this.toggleEnabled.wasPressed()) {
            Firewall.setEnabled(client, !Firewall.CONFIG.isEnabled());
        }
        while (this.toggleLoggingEnabled.wasPressed()) {
            Firewall.setLoggingEnabled(client, !Firewall.CONFIG.logging.isEnabled());
        }
    }
}

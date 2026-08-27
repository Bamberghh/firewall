package me.bamberghh.firewall;

import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

public class KeyBindings {
    public KeyMapping.Category category;
    public KeyMapping toggleEnabled;
    public KeyMapping toggleLoggingEnabled;

    public KeyBindings() {
        this.category = KeyMapping.Category.register(Identifier.fromNamespaceAndPath(Firewall.MOD_ID, "main"));
        this.toggleEnabled = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.firewall.toggleEnabled",
                GLFW.GLFW_KEY_UNKNOWN,
                this.category));
        this.toggleLoggingEnabled = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.firewall.toggleLoggingEnabled",
                GLFW.GLFW_KEY_UNKNOWN,
                this.category));
    }

    public void handle(Minecraft client) {
        while (this.toggleEnabled.consumeClick()) {
            Firewall.setEnabled(client, !Firewall.CONFIG.isEnabled());
        }
        while (this.toggleLoggingEnabled.consumeClick()) {
            Firewall.setLoggingEnabled(client, !Firewall.CONFIG.logging.isEnabled());
        }
    }
}

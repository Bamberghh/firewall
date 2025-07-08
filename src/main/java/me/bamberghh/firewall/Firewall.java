package me.bamberghh.firewall;

import io.wispforest.owo.config.ui.ConfigScreenProviders;
import me.bamberghh.firewall.config.ConfigInit;
import me.bamberghh.firewall.config.FirewallConfig;
import me.bamberghh.firewall.config.FirewallConfigModel;
import me.bamberghh.firewall.config.FirewallConfigScreen;
import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;

public class Firewall implements ClientModInitializer {
	public static final String MOD_ID = "firewall";

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static final FirewallConfig CONFIG = FirewallConfig.createAndLoad(FirewallConfigModel::builderConsumer);

	@Override
	public void onInitializeClient() {
        try {
            ConfigInit.init(CONFIG);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException | InstantiationException e) {
            throw new RuntimeException(e);
        }
        ConfigScreenProviders.register(MOD_ID, screen -> FirewallConfigScreen.create(CONFIG, screen));
	}
}
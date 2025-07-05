package me.bamberghh.firewall;

import io.wispforest.owo.config.ui.ConfigScreenProviders;
import me.bamberghh.firewall.config.FirewallConfigModel;
import me.bamberghh.firewall.config.FirewallConfigScreen;
import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Firewall implements ClientModInitializer {
	public static final String MOD_ID = "firewall";

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static final me.bamberghh.firewall.config.FirewallConfig CONFIG
			= me.bamberghh.firewall.config.FirewallConfig.createAndLoad(FirewallConfigModel::builderConsumer);

	@Override
	public void onInitializeClient() {
		ConfigScreenProviders.register(MOD_ID, screen -> FirewallConfigScreen.create(CONFIG, screen));
	}
}
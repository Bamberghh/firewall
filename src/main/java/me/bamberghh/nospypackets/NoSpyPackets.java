package me.bamberghh.nospypackets;

import io.wispforest.owo.config.ui.ConfigScreenProviders;
import me.bamberghh.nospypackets.config.NoSpyPacketsConfigModel;
import me.bamberghh.nospypackets.config.NoSpyPacketsConfigScreen;
import net.fabricmc.api.ClientModInitializer;

import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class NoSpyPackets implements ClientModInitializer {
	public static final String MOD_ID = "no-spy-packets";

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static final me.bamberghh.nospypackets.config.NoSpyPacketsConfig CONFIG
			= me.bamberghh.nospypackets.config.NoSpyPacketsConfig.createAndLoad(NoSpyPacketsConfigModel::builderConsumer);

	private static Set<String> suppressedListToSet(List<String> list) {
		return list
				.stream()
				.filter(str -> !str.isEmpty())
				.collect(Collectors.toSet());
	}

	@Override
	public void onInitializeClient() {
		ConfigScreenProviders.register("no-spy-packets", screen -> NoSpyPacketsConfigScreen.create(CONFIG, screen));
	}
}
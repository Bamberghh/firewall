package me.bamberghh.nospypackets;

import net.fabricmc.api.ClientModInitializer;

import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

public class NoSpyPackets implements ClientModInitializer {
	public static final String MOD_ID = "no-spy-packets";

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static final me.bamberghh.nospypackets.NoSpyPacketsConfig CONFIG;
	public static Set<String> CONFIG_suppressedSentCustomPayloadIdentifiersSet;
	public static Set<String> CONFIG_suppressedReceivedCustomPayloadIdentifiersSet;

	private static Set<String> suppressedListToSet(List<String> list) {
		return list
				.stream()
				.filter(str -> !str.isEmpty())
				.collect(Collectors.toSet());
	}

	static {
		CONFIG = me.bamberghh.nospypackets.NoSpyPacketsConfig.createAndLoad();
		CONFIG_suppressedSentCustomPayloadIdentifiersSet
				= suppressedListToSet(CONFIG.suppressedSentCustomPayloadIdentifiers());
		CONFIG.subscribeToSuppressedSentCustomPayloadIdentifiers(list ->
				CONFIG_suppressedSentCustomPayloadIdentifiersSet = suppressedListToSet(list));
		CONFIG_suppressedReceivedCustomPayloadIdentifiersSet
				= suppressedListToSet(CONFIG.suppressedReceivedCustomPayloadIdentifiers());
		CONFIG.subscribeToSuppressedReceivedCustomPayloadIdentifiers(list ->
				CONFIG_suppressedReceivedCustomPayloadIdentifiersSet = suppressedListToSet(list));
	}

	@Override
	public void onInitializeClient() {
	}
}
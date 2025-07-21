This is a Minecraft Fabric mod that allows you to configure what packets are sent/received.

### Installation

- [Fabric API](https://modrinth.com/mod/fabric-api) (required);
- [owo-lib](https://modrinth.com/mod/owo-lib) (required);
- [Mod Menu](https://modrinth.com/mod/modmenu) (or some other mod to change the mod's config) (very much recommended).

### Usage

The mod filters the sent/received packets, by default (default config) it does nothing.

⚠️ Note that since it directly rejects/filters the sent/received packets (as configured), it may break some mods or vanilla systems, especially if you filter the packet identifiers (the first option), changing the other options probably won't break the vanilla game. Configure carefully!

The entire mod is configured through the Mod Menu UI with helpful tooltips; note that each change to the packets (rejecting or filtering them) is logged to the console. Below is a short description of every option currently there:

#### Packet identifiers

Control which packet identifiers are allowed.

For a list of all of them see [the Minecraft Wiki article on Java Edition packets](https://minecraft.wiki/w/Java_Edition_protocol/Packets), specifically `resouce` in the `Packet ID` field of any packet. Note that you need to prefix the id with `minecraft:` namespace, for example `minecraft:custom_payload`.

#### Custom payload identifiers

Control which custom payload packet (aka plugin message or channel) identifiers are allowed, sent by most of the mod/plugin loaders and some of mods/plugins.

You can select the setting `Use from "Registered channel identifiers" option below` to also apply the filters from the respective option.

For a list of *some* of the identifiers see [the Minecraft Wiki article on plugin channels](https://minecraft.wiki/w/Java_Edition_protocol/Plugin_channels), or the respective mod/plugin documentation for the sent packets. Again, like the packet identifiers, don't forget the `minecraft:` or some other namespace.

The `Respond to rejected received query requests` should probably be enabled, as to not break the handshaking flow.

#### Registered channel identifiers

Control which channel identifiers in minecraft:register, minecraft:unregister (used by Bukkit, BungeeCord, Velocity and Fabric API) and c:register packets (used by FabricMC, NeoForged, PaperMC and SpongePowered team) are allowed.

You can select the setting `Use from "Custom payload identifiers" option above` to also apply the filters from the respective option.

You can deselect the `Send/Receive empty channel lists` options to not send/receive empty channel lists.

For a list of *some* of the identifiers, again, see [the Minecraft Wiki article on plugin channels](https://minecraft.wiki/w/Java_Edition_protocol/Plugin_channels).

#### Identifiers of logged packets

What packets should be logged - regardless of if they're rejected or not. By default none are logged.

#### Identifiers of logged custom payloads

What custom payloads should be logged - regardless of if they're rejected or not. By default none are logged.


### I have a problem/suggestion/translation

[Please open an issue on GitHub](https://github.com/Bamberghh/firewall/issues/new).

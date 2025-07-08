package me.bamberghh.firewall.util;

import net.minecraft.util.Identifier;

public interface StringFilter {
    boolean acceptsNothing();

    boolean acceptsEverything();

    boolean accepts(String str);

    default boolean accepts(Identifier id) {
        return acceptsEverything() || !acceptsNothing() && accepts(id.toString());
    }
}

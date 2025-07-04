package me.bamberghh.firewall.util;

import java.util.regex.Pattern;

public class StringMask {
    public enum Kind {
        Blacklist,
        Whitelist,
        Regex,
    }
    public Kind kind = Kind.Blacklist;
    public IndexHashSet<String> list = new IndexHashSet<>();
    public Pattern regex = Pattern.compile("");

    public boolean acceptsNothing() {
        return kind == Kind.Whitelist && list.isEmpty();
    }

    public boolean acceptsEverything() {
        return kind == Kind.Blacklist && list.isEmpty();
    }

    public boolean accepts(String str) {
        switch (kind) {
            case Blacklist -> {
                return !list.contains(str);
            }
            case Whitelist -> {
                return list.contains(str);
            }
            case Regex -> {
                return regex.matcher(str).matches();
            }
        }
        return false;
    }
}

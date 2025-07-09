package me.bamberghh.firewall.util;

import java.util.regex.Pattern;

public class SimpleStringFilter implements StringFilter {
    public enum Kind {
        Blacklist,
        Whitelist,
        Regex,
    }
    public Kind kind = Kind.Blacklist;
    public IndexHashSet<String> list = new IndexHashSet<>();
    public Pattern regex = Pattern.compile("");

    private SimpleStringFilter() {}

    public SimpleStringFilter(Kind kind) {
        this.kind = kind;
    }

    public SimpleStringFilter(Kind kind, IndexHashSet<String> list, Pattern regex) {
        this.kind = kind;
        this.list = list;
        this.regex = regex;
    }

    public static SimpleStringFilter blacklist() {
        return new SimpleStringFilter(Kind.Blacklist);
    }

    public static SimpleStringFilter blacklist(IndexHashSet<String> list) {
        return new SimpleStringFilter(Kind.Blacklist, list, Pattern.compile(""));
    }

    public static SimpleStringFilter whitelist() {
        return new SimpleStringFilter(Kind.Whitelist);
    }

    public static SimpleStringFilter whitelist(IndexHashSet<String> list) {
        return new SimpleStringFilter(Kind.Whitelist, list, Pattern.compile(""));
    }

    public static SimpleStringFilter regex() {
        return new SimpleStringFilter(Kind.Regex);
    }

    public static SimpleStringFilter regex(Pattern pattern) {
        return new SimpleStringFilter(Kind.Regex, new IndexHashSet<>(), pattern);
    }

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

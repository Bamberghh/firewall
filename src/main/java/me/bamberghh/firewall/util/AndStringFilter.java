package me.bamberghh.firewall.util;

import java.util.ArrayList;
import java.util.List;

public class AndStringFilter implements StringFilter {
    boolean acceptsNothing = false;
    boolean acceptsEverything = true;
    private final List<StringFilter> filters = new ArrayList<>();

    private AndStringFilter() {}

    public static StringFilter of(StringFilter... filters) {
        AndStringFilter self = new AndStringFilter();
        for (var filter : filters) {
            self.acceptsNothing = self.acceptsNothing || filter.acceptsNothing();
            self.acceptsEverything = self.acceptsEverything && filter.acceptsEverything();
            if (self.acceptsNothing) {
                break;
            }
            if (filter instanceof AndStringFilter orFilter) {
                self.filters.addAll(orFilter.filters);
            } else {
                self.filters.add(filter);
            }
        }
        if (self.acceptsNothing || self.acceptsEverything) {
            self.filters.clear();
        }
        else if (self.filters.size() == 1) {
            return self.filters.getFirst();
        }
        return self;
    }

    @Override
    public boolean acceptsNothing() {
        return acceptsNothing;
    }

    @Override
    public boolean acceptsEverything() {
        return acceptsEverything;
    }

    @Override
    public boolean accepts(String str) {
        if (acceptsNothing) {
            return false;
        }
        if (acceptsEverything) {
            return true;
        }
        for (var filter : filters) {
            if (!filter.accepts(str)) {
                return false;
            }
        }
        return true;
    }
}

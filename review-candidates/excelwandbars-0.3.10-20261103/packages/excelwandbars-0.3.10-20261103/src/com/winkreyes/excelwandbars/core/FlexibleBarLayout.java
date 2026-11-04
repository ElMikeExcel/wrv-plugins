package com.winkreyes.excelwandbars.core;

import java.util.*;

/** Keeps valid visible icon columns while enforcing seven icons per eight-column page. */
public final class FlexibleBarLayout {
    private FlexibleBarLayout() {}

    /**
     * Desired page assignments come from the logical loadout. Current positions only
     * preserve columns within that same page. Pass null current for an explicit
     * loadout reorder so its new column choices take precedence. Book positions are
     * copied unchanged. Neither input map is mutated.
     */
    public static Map<String,Integer> preserve(Map<String,Integer> desired,
                                                Map<String,Integer> current, int pages) {
        if (pages < 1 || pages > Integer.MAX_VALUE / 8)
            throw new IllegalArgumentException("Invalid hotbar page count");
        Objects.requireNonNull(desired, "desired");
        int boundary = pages * 8;
        Map<Integer,List<String>> onPages = new LinkedHashMap<>();
        Set<Integer> desiredPositions = new HashSet<>();
        for (var e : desired.entrySet()) {
            Integer pos = e.getValue();
            if (pos == null || pos < 0 || !desiredPositions.add(pos))
                throw new IllegalArgumentException("Invalid or duplicate desired position");
            if (pos < boundary) onPages.computeIfAbsent(pos / 8, p -> new ArrayList<>()).add(e.getKey());
        }
        Map<String,Integer> placed = new HashMap<>();
        for (var page : onPages.entrySet()) {
            if (page.getValue().size() > 7)
                throw new IllegalArgumentException("More than seven icons on a page");
            boolean[] used = new boolean[8];
            // Preserve only unambiguous, valid positions on the intended page.
            for (String spell : page.getValue()) {
                Integer old = current == null ? null : current.get(spell);
                if (old != null && old >= 0 && old < boundary && old / 8 == page.getKey() && !used[old % 8]) {
                    placed.put(spell, old);
                    used[old % 8] = true;
                }
            }
            for (String spell : page.getValue()) {
                if (placed.containsKey(spell)) continue;
                int column = desired.get(spell) % 8;
                if (used[column]) {
                    column = 0;
                    while (used[column]) column++;
                }
                used[column] = true;
                placed.put(spell, page.getKey() * 8 + column);
            }
        }
        Map<String,Integer> result = new LinkedHashMap<>();
        for (var e : desired.entrySet())
            result.put(e.getKey(), e.getValue() >= boundary ? e.getValue() : placed.get(e.getKey()));
        return result;
    }
}

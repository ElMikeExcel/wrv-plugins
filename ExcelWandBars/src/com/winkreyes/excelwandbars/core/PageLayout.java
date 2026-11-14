package com.winkreyes.excelwandbars.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 0.2.5: every unlocked spell on the hotbar, in pages. Pure (no Bukkit/Magic types),
 * unit-tested on the plain JVM.
 *
 * Magic (elmakers) keeps a wand's spell slots in "spell_inventory" (spell key to a
 * global slot; hotbar page p, slot i = p * 8 + i) and shows "hotbar_count" pages of
 * 8 slots in inventory mode. A wand that was upgraded over time can have gaps and
 * spells parked past the last page, which then never show on the hotbar. This packs
 * the spells in their current order (no gaps) and says how many pages are needed.
 *
 * perPage 7 (Tap bar, Tap to pick): the 8th slot of every page stays empty, so a
 * phone player can always tap it to get their items back. perPage 8 (Sneak bars).
 */
public final class PageLayout {
    private PageLayout() {}

    public static final int SLOTS_PER_BAR = 8;

    public static int clampPerPage(int perPage) { return Math.max(1, Math.min(SLOTS_PER_BAR, perPage)); }

    /** Pages needed for n spells (at least 1). */
    public static int pages(int n, int perPage) {
        int pp = clampPerPage(perPage);
        return Math.max(1, (Math.max(0, n) + pp - 1) / pp);
    }

    /** The spells in their current order: by current slot, then spells with no slot by name. */
    public static List<String> order(Map<String, Integer> current, Collection<String> spells) {
        List<String> slotted = new ArrayList<>(), loose = new ArrayList<>();
        java.util.Set<String> seen = new java.util.HashSet<>();
        for (String s : spells) {
            if (s == null || s.isBlank()) continue;
            String k = s.trim().toLowerCase(Locale.ROOT);
            if (!seen.add(k)) continue;
            Integer slot = current == null ? null : current.get(k);
            if (slot != null && slot >= 0) slotted.add(k); else loose.add(k);
        }
        slotted.sort((a, b) -> {
            int c = Integer.compare(current.get(a), current.get(b));
            return c != 0 ? c : a.compareTo(b);
        });
        loose.sort(String::compareTo);
        slotted.addAll(loose);
        return slotted;
    }

    /** Spell key to global slot, packed: page = i / perPage, slot = page * 8 + i % perPage. */
    public static Map<String, Integer> pack(Map<String, Integer> current, Collection<String> spells, int perPage) {
        int pp = clampPerPage(perPage);
        Map<String, Integer> out = new LinkedHashMap<>();
        int i = 0;
        for (String k : order(current, spells)) {
            out.put(k, (i / pp) * SLOTS_PER_BAR + (i % pp));
            i++;
        }
        return out;
    }

    /**
     * 0.3.17 (Shape A): pack against the wand's EFFECTIVE page budget.
     * The first maxPages pages (in current order) go on the bar exactly as
     * pack() places them; every spell past the budget parks in the book
     * region (dense slots from maxPages * 8, in the same order), where
     * Magic's build for that page count puts them in the Spellbook instead
     * of spilling them through a too-small hotbar. With a budget at or
     * past the pages the spells need, this is exactly pack().
     */
    public static Map<String, Integer> packCapped(Map<String, Integer> current, Collection<String> spells, int perPage, int maxPages) {
        int pp = clampPerPage(perPage);
        int cap = Math.max(1, maxPages);
        List<String> ordered = order(current, spells);
        Map<String, Integer> out = new LinkedHashMap<>();
        int onBar = Math.min(ordered.size(), cap * pp);
        for (int i = 0; i < onBar; i++) {
            out.put(ordered.get(i), (i / pp) * SLOTS_PER_BAR + (i % pp));
        }
        int next = cap * SLOTS_PER_BAR;
        for (int i = onBar; i < ordered.size(); i++) {
            out.put(ordered.get(i), next++);
        }
        return out;
    }

    /** True when the wand already has exactly this layout (only the listed spells are compared). */
    public static boolean same(Map<String, Integer> current, Map<String, Integer> packed) {
        if (current == null) return packed.isEmpty();
        for (Map.Entry<String, Integer> e : packed.entrySet()) {
            Integer c = current.get(e.getKey());
            if (c == null || !c.equals(e.getValue())) return false;
        }
        return true;
    }

    /** Action-bar line: "Page 2/3: Boon, Feed" (Tap bar and Tap to pick) or "Bar 2/3: ..." (Sneak bars). */
    public static String pageLine(int current, int count, List<String> names) {
        return "Page" + BarText.line(current, count, names).substring(3);
    }
}


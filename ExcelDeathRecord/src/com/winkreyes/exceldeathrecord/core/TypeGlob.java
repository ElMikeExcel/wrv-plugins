package com.winkreyes.exceldeathrecord.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 0.3.0: the "valuable item types" list (config despawn-log-types).
 * A despawning item whose type matches any pattern reaches the Burn
 * Ledger even when it is not enchanted or named. Patterns are item
 * ids with or without "minecraft:", case-insensitive, and * matches
 * any run of characters: diamond_*, *shulker_box, elytra.
 */
public final class TypeGlob {

    private final List<String> patterns = new ArrayList<>();

    public TypeGlob(List<String> raw) {
        if (raw == null) return;
        for (String p : raw) {
            if (p == null) continue;
            String t = strip(p.trim().toLowerCase(Locale.ROOT));
            if (!t.isEmpty()) patterns.add(t);
        }
    }

    public int size() { return patterns.size(); }

    /** True when the item id (e.g. minecraft:diamond_sword) matches a pattern. */
    public boolean matches(String itemId) {
        if (itemId == null || itemId.isEmpty()) return false;
        String id = strip(itemId.toLowerCase(Locale.ROOT));
        for (String p : patterns) if (glob(p, id)) return true;
        return false;
    }

    private static String strip(String s) {
        return s.startsWith("minecraft:") ? s.substring("minecraft:".length()) : s;
    }

    /** Iterative * matcher (no regex, so a pattern can't misbehave). */
    static boolean glob(String p, String s) {
        int pi = 0, si = 0, star = -1, mark = 0;
        while (si < s.length()) {
            if (pi < p.length() && p.charAt(pi) != '*' && p.charAt(pi) == s.charAt(si)) { pi++; si++; }
            else if (pi < p.length() && p.charAt(pi) == '*') { star = pi++; mark = si; }
            else if (star >= 0) { pi = star + 1; si = ++mark; }
            else return false;
        }
        while (pi < p.length() && p.charAt(pi) == '*') pi++;
        return pi == p.length();
    }
}

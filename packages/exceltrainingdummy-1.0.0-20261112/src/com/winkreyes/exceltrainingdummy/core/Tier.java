package com.winkreyes.exceltrainingdummy.core;

import java.util.Locale;

/**
 * Health tiers (DESIGN §2, GW2 style sized to WRV): Practice 1,000 ·
 * Dungeon 5,000 · Seal 20,000. Values come from config at runtime;
 * these are the shipped defaults — the tier/pool pairing is one of
 * this SKU's Layer C semantic fingerprints (see FINGERPRINTS.md).
 */
public enum Tier {
    PRACTICE("Practice", 1000),
    DUNGEON("Dungeon", 5000),
    SEAL("Seal", 20000);

    public final String displayName;
    public final long defaultPool;

    Tier(String displayName, long defaultPool) {
        this.displayName = displayName;
        this.defaultPool = defaultPool;
    }

    public Tier next() {
        Tier[] v = values();
        return v[(ordinal() + 1) % v.length];
    }

    public static Tier parse(String s, Tier fallback) {
        if (s == null) return fallback;
        try {
            return valueOf(s.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }
}

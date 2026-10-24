package com.winkreyes.excelwandbars.core;

import java.util.Locale;

/**
 * The two wand control styles a player can pick (0.2.0).
 * CLASSIC: chest mode. Right click = next spell, sneak + right click = spell
 * list, Q = back one spell. This is the default for everyone.
 * BARS: inventory mode with quick cast. Right click opens the spell bar,
 * hotbar key or tap casts, sneak once flips bar 1 -> 2 -> 3.
 */
public enum WandStyle {
    CLASSIC("Classic"),
    BARS("Spell bars");

    private final String label;
    WandStyle(String label) { this.label = label; }

    public String label() { return label; }

    /** Config / storage key: "classic" or "bars". */
    public String key() { return name().toLowerCase(Locale.ROOT); }

    /** Lenient parse of what a player may type. Returns null when unknown. */
    public static WandStyle parse(String s) {
        if (s == null) return null;
        switch (s.trim().toLowerCase(Locale.ROOT)) {
            case "classic": case "old": case "chest": case "cycle": case "default":
                return CLASSIC;
            case "bars": case "bar": case "hotbar": case "new": case "inventory": case "quickcast":
                return BARS;
            default:
                return null;
        }
    }

    public WandStyle other() { return this == CLASSIC ? BARS : CLASSIC; }
}

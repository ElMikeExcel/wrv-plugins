package com.winkreyes.excelwandbars.core;

import java.util.Locale;

/**
 * The two wand control styles a player can pick (0.2.0).
 * CLASSIC: chest mode. Right click = next spell, sneak + right click = spell
 * list, Q = back one spell. This is the default for everyone.
 * BARS: inventory mode with quick cast. Right click opens the spell bar,
 * hotbar key or tap casts, sneak once flips bar 1 -> 2 -> 3.
 * 0.2.1: BARS is shown to players as "Sneak bars" and is for Bedrock
 * players only (wand-style.bars-bedrock-only). The storage key stays "bars"
 * so 0.2.0 styles.yml files load unchanged.
 */
public enum WandStyle {
    CLASSIC("Classic"),
    BARS("Sneak bars"),
    /**
     * 0.2.3: just cycling. Chest mode like Classic, but no spell list ever opens from
     * the wand: right click (Bedrock: press and hold, controller LT) = next spell,
     * sneak + right click, Q or sneak + Q = back one spell, left click (RT) casts.
     * The spell name shows on screen through the wand's own action_bar ($spell) and
     * item name. Allowed on Java and Bedrock. Storage key "cycle"; 0.2.2 reads that
     * key as Classic, so a rollback is safe.
     */
    CYCLE("Cycle"),
    /**
     * 0.2.4: the first Magic wand setup (WRV-070 Phase 1 wands.yml, Oct 17 2026):
     * inventory mode, no quick cast. Holding the wand fills the hotbar with spell
     * icons; picking a hotbar slot makes that spell active (Magic activateIcon),
     * left click (Bedrock touch: press and hold) casts it. Storage key "original";
     * 0.2.6: quick_cast "manual" so a tap really only picks (quick_cast "false" let
     * spells marked quick_cast in spells.yml cast on the tap), right_click handled by
     * ExcelWandBars so a phone's press and hold casts instead of closing the bar.
     * 0.2.3 and older do not know it and use Classic, so a rollback is safe.
     */
    ORIGINAL("Tap to pick (original)"),
    /**
     * 0.2.4: Administrator's phone action bar. Inventory mode + quick cast: holding the wand
     * fills the hotbar with spell icons, picking a slot casts that spell at once
     * (Magic cancels the slot change, so the wand stays selected and the next tap
     * always registers). Same icon in the same slot every time. Storage key "tapbar".
     */
    /*
     * 0.2.5: Tap bar shows EVERY unlocked spell: pages of 7 (the 8th slot stays empty
     * for "items back"), crouch flips the page ("Page 1/2" on the action bar). Left click
     * and press and hold never cast (left_click none); Q / press and hold on the wand
     * slot is read by the plugin (drop none). ORIGINAL's label is now "Tap to pick
     * (original)" (storage key "original" unchanged, the old name still parses).
     */
    TAPBAR("Tap bar");

    private final String label;
    WandStyle(String label) { this.label = label; }

    public String label() { return label; }

    /** Config / storage key: "classic" or "bars". */
    public String key() { return name().toLowerCase(Locale.ROOT); }

    /** Lenient parse of what a player may type. Returns null when unknown. */
    public static WandStyle parse(String s) {
        if (s == null) return null;
        switch (s.trim().toLowerCase(Locale.ROOT)) {
            case "classic": case "old": case "chest": case "default": case "list":
                return CLASSIC;
            case "cycle": case "cycler": case "cycling": case "next": case "quickcycle": case "quick-cycle":
                return CYCLE;
            case "original": case "first": case "tapcycle": case "tap-cycle": case "tap_cycle": case "tap-to-cycle":
            case "taptocycle": case "v1": case "pick": case "taptopick": case "tap-to-pick": case "tap_to_pick":
                return ORIGINAL;
            case "tapbar": case "tap-bar": case "tap_bar": case "tap": case "phone": case "mobile": case "actionbar":
            case "action-bar": case "wow":
                return TAPBAR;
            case "bars": case "bar": case "hotbar": case "new": case "inventory": case "quickcast":
            case "sneakbars": case "sneak-bars": case "sneak_bars": case "sneak": case "sneakbar":
                return BARS;
            default:
                return null;
        }
    }

    /** "/wandstyle switch": Classic <-> Sneak bars as before; Cycle goes to Classic. */
    public WandStyle other() { return this == CLASSIC ? BARS : CLASSIC; }

    /** Chest-mode styles (Classic and Cycle): no bar, no sneak flip. */
    public boolean chest() { return this == CLASSIC || this == CYCLE; }

    /** 0.2.4: inventory-mode styles whose spell bar can be flipped with sneak (Sneak bars, original, Tap bar). */
    public boolean inventory() { return !chest(); }

    /** 0.2.4: styles whose spell bar opens by itself when the wand is picked in the hotbar. */
    public boolean opensOnHold() { return this == ORIGINAL || this == TAPBAR; }

    /** Button / command key players type (Sneak bars is typed "sneakbars"). */
    public String typed() { return this == BARS ? "sneakbars" : key(); }
}


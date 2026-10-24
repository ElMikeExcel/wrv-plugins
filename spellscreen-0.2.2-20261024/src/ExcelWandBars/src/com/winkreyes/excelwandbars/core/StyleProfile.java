package com.winkreyes.excelwandbars.core;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * The Magic (elmakers) wand control properties each style needs, and the pure
 * "what has to change on this wand" decision. No Bukkit or Magic types.
 *
 * The values are the live WRV-150 chest set (noab install, Oct 24 2026 2:05 AM
 * CDT) and the action-bar overlay set (BEDROCK-CASTING.md option 1). Because
 * the plugin compares against the wand's EFFECTIVE values (its own overrides
 * plus its template), it does the right thing whichever base_wand is live.
 */
public final class StyleProfile {
    /** Every key the plugin may write, in write order (mode first). */
    public static final String[] KEYS = {
        "mode", "left_click", "right_click", "right_click_sneak",
        "drop", "drop_sneak", "swap", "swap_sneak", "quick_cast",
        "live_hotbar_cooldown", "live_hotbar_mana"
    };

    private final Map<WandStyle, Map<String, String>> wanted = new LinkedHashMap<>();

    public StyleProfile() {
        Map<String, String> c = new LinkedHashMap<>();
        c.put("mode", "chest");
        c.put("left_click", "cast");
        c.put("right_click", "cycle");
        c.put("right_click_sneak", "toggle");
        c.put("drop", "cycle_reverse");
        c.put("drop_sneak", "toggle");
        c.put("swap", "cycle");
        c.put("swap_sneak", "cycle_reverse");
        c.put("quick_cast", "false");
        wanted.put(WandStyle.CLASSIC, c);
        Map<String, String> b = new LinkedHashMap<>();
        b.put("mode", "inventory");
        b.put("left_click", "cast");
        b.put("right_click", "toggle");
        b.put("right_click_sneak", "cycle");
        b.put("drop", "cycle_hotbar");
        b.put("drop_sneak", "cycle_hotbar_reverse");
        b.put("swap", "cycle_hotbar");
        b.put("swap_sneak", "cycle_reverse");
        b.put("quick_cast", "true");
        // Bars only (as in the action-bar overlay): cooldown and mana shown on the bar icons.
        // Classic does not list them, so they are never touched for classic players.
        b.put("live_hotbar_cooldown", "true");
        b.put("live_hotbar_mana", "true");
        wanted.put(WandStyle.BARS, b);
    }

    /** Override one style's value (from config.yml). Unknown keys are ignored. Blank = leave that key alone. */
    public boolean set(WandStyle style, String key, String value) {
        String k = key == null ? "" : key.trim().toLowerCase(Locale.ROOT);
        boolean known = false;
        for (String x : KEYS) if (x.equals(k)) known = true;
        if (!known) return false;
        String v = norm(k, value);
        if (v == null || v.isEmpty()) wanted.get(style).remove(k);
        else wanted.get(style).put(k, v);
        return true;
    }

    public Map<String, String> wanted(WandStyle style) {
        return Collections.unmodifiableMap(wanted.get(style));
    }

    /**
     * Magic treats a missing quick_cast as "off" (chest mode resets it) and
     * reads booleans loosely, so compare normalised lower-case strings.
     */
    static String norm(String key, String value) {
        if (value == null) return "quick_cast".equals(key) ? "false" : null;
        String v = value.trim().toLowerCase(Locale.ROOT);
        if (v.isEmpty()) return "quick_cast".equals(key) ? "false" : "";
        return v;
    }

    /**
     * Keys (and values) that must be written so the wand's effective controls
     * equal the style. Empty map = the wand already matches, do nothing.
     * effective = what Magic reports for each key (null = unset everywhere).
     */
    public Map<String, String> changes(Map<String, String> effective, WandStyle style) {
        Map<String, String> out = new LinkedHashMap<>();
        for (Map.Entry<String, String> e : wanted.get(style).entrySet()) {
            String have = norm(e.getKey(), effective == null ? null : effective.get(e.getKey()));
            if (!e.getValue().equals(have)) out.put(e.getKey(), e.getValue());
        }
        return out;
    }

    /** Which style the effective controls look like (by mode only), or null when neither. */
    public static WandStyle classify(Map<String, String> effective) {
        String m = effective == null ? null : effective.get("mode");
        if (m == null) return null;
        m = m.trim().toLowerCase(Locale.ROOT);
        if (m.equals("chest")) return WandStyle.CLASSIC;
        if (m.equals("inventory")) return WandStyle.BARS;
        return null;
    }
}

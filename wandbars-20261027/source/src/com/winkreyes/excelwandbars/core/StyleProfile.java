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
        "live_hotbar_cooldown", "live_hotbar_mana", "xp_display", "level_display"
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
        // 0.2.3 Cycle: Classic with every "open the list" key turned into "back one spell".
        Map<String, String> y = new LinkedHashMap<>();
        y.put("mode", "chest");
        y.put("left_click", "cast");
        y.put("right_click", "cycle");
        y.put("right_click_sneak", "cycle_reverse");
        y.put("drop", "cycle_reverse");
        y.put("drop_sneak", "cycle_reverse");
        y.put("swap", "cycle");
        y.put("swap_sneak", "cycle_reverse");
        y.put("quick_cast", "false");
        wanted.put(WandStyle.CYCLE, y);
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
        // 0.2.4 original: the WRV-070 Phase 1 base_wand (Oct 17 2026) as it was:
        // inventory, left_click cast, right_click toggle, drop toggle, swap
        // cycle_hotbar, swap_sneak cycle_hotbar_reverse, no quick cast. The two
        // sneak keys it never set are written as bar flips so nothing is left
        // over from another style. Cooldowns show on the icons.
        Map<String, String> o = new LinkedHashMap<>();
        o.put("mode", "inventory");
        o.put("left_click", "cast");
        o.put("right_click", "toggle");
        o.put("right_click_sneak", "cycle_hotbar");
        o.put("drop", "toggle");
        o.put("drop_sneak", "cycle_hotbar_reverse");
        o.put("swap", "cycle_hotbar");
        o.put("swap_sneak", "cycle_hotbar_reverse");
        // 0.2.6 fix: "manual", not "false". With quick_cast false, Magic (Mage.activateIcon)
        // still casts any spell whose own config says quick_cast: true (cure, boon,
        // superheal, feed, recall in the live spells.yml) the moment its icon is picked,
        // because only quick_cast "manual" (or "true") sets the wand's quickCastDisabled.
        // "manual" = picking an icon only selects the spell, for every spell.
        o.put("quick_cast", "manual");
        o.put("live_hotbar_cooldown", "true");
        o.put("live_hotbar_mana", "true");
        // 0.2.6: right click is handled by ExcelWandBars (as in Tap bar). A phone's press and
        // hold arrives as a right click too, and Magic's toggle closed the bar under the finger
        // instead of casting. Java: right click still opens the bar / puts items back.
        o.put("right_click", "none");
        wanted.put(WandStyle.ORIGINAL, o);
        // 0.2.4 Tap bar: the original's layout with quick cast on (picking a slot casts).
        Map<String, String> t = new LinkedHashMap<>(o);
        t.put("quick_cast", "true"); // 0.2.6: unchanged, Tap bar still casts on tap
        // 0.2.5: left click / press and hold never casts in Tap bar (Magic's survival
        // left click casts from PlayerInteractEvent even when another plugin cancels it,
        // so the key itself is "none"). Drop (Q, press and hold the wand slot) is read by
        // ExcelWandBars: close the bar, or read the spell icon just tapped.
        t.put("left_click", "none");
        t.put("drop", "none");
        // 0.2.5: right click / a phone's press and hold is handled by ExcelWandBars (read on Bedrock,
        // open or items back on Java). Magic's toggle here closed the bar under a held finger.
        t.put("right_click", "none");
        // Crouch-start already flips one page. A repeated held-use must not cycle
        // pages again while the protocol-qualified crouch hold requests Book.
        t.put("right_click_sneak", "none");
        wanted.put(WandStyle.TAPBAR, t);
        // Normal vanilla XP below; mana remains in the separate existing boss bar.
        // StyleHook compares effective values and updates existing rank wands in place.
        // No XP/mana/SP amounts or boss_bar properties are written here.
        for (Map<String, String> controls : wanted.values()) {
            controls.put("xp_display", "none");
            controls.put("level_display", "none");
        }
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

    /** Which style the effective controls look like (mode, then quick_cast and drop), or null when none. */
    public static WandStyle classify(Map<String, String> effective) {
        String m = effective == null ? null : effective.get("mode");
        if (m == null) return null;
        m = m.trim().toLowerCase(Locale.ROOT);
        if (m.equals("chest")) {
            String rs = effective.get("right_click_sneak");
            return rs != null && rs.trim().equalsIgnoreCase("cycle_reverse") ? WandStyle.CYCLE : WandStyle.CLASSIC;
        }
        if (m.equals("inventory")) {
            String qc = norm("quick_cast", effective.get("quick_cast"));
            String d = effective.get("drop");
            // 0.2.5 Tap bar sets drop "none" (0.2.4 used "toggle"); both mean Tap bar.
            boolean dropToggle = d != null && (d.trim().equalsIgnoreCase("toggle") || d.trim().equalsIgnoreCase("none"));
            if (!"true".equals(qc)) return WandStyle.ORIGINAL;
            return dropToggle ? WandStyle.TAPBAR : WandStyle.BARS;
        }
        return null;
    }
}


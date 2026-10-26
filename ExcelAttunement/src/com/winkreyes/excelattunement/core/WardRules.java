package com.winkreyes.excelattunement.core;

import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * 0.1.5 guild ward decisions, pure logic.
 *
 * Carried-in buffs: Magic's buffs are plain vanilla potion effects with
 * no source tag (Magic's PotionEffectAction even marks them ambient by
 * default), so they cannot be told apart from brewed ones. The rule is
 * by type and time left: a configured type (default resistance,
 * absorption, regeneration) with {@code maxSeconds} or less remaining
 * is stripped on entry. Magic's are all 15 s or less (Blessing 10 s,
 * Ward 15 s, Superheal regen 5 s); golden apples and brewed potions
 * run longer and are kept. Infinite effects are always kept.
 */
public final class WardRules {
    private WardRules() {}

    /** Effect keys normalized: "RESISTANCE", "minecraft:resistance", " Resistance " -> "resistance". */
    public static String normalizeEffect(String key) {
        if (key == null) return "";
        String k = key.trim().toLowerCase(Locale.ROOT);
        int colon = k.indexOf(':');
        if (colon >= 0) k = k.substring(colon + 1);
        // Legacy Bukkit / Magic names for the same effects.
        return switch (k) {
            case "damage_resistance" -> "resistance";
            default -> k;
        };
    }

    public static Set<String> normalizeAll(Collection<String> keys) {
        Set<String> out = new HashSet<>();
        if (keys != null) for (String k : keys) {
            String n = normalizeEffect(k);
            if (!n.isEmpty()) out.add(n);
        }
        return out;
    }

    /**
     * @param effectKey     the effect's key (any of the accepted spellings)
     * @param durationTicks ticks remaining; negative = infinite
     * @param stripTypes    normalized keys (see {@link #normalizeAll})
     * @param maxSeconds    strip only at or below this many seconds left
     */
    public static boolean shouldStrip(String effectKey, int durationTicks, Set<String> stripTypes, long maxSeconds) {
        if (durationTicks < 0) return false; // infinite (-1): never a Magic buff
        if (!stripTypes.contains(normalizeEffect(effectKey))) return false;
        return durationTicks <= maxSeconds * 20L;
    }

    /**
     * A teleport INTO a warded world is blocked when it is driven by
     * Magic (a Magic class on the call stack: Recall, Warp, any
     * teleport action) and the player has no bypass.
     */
    public static boolean blockTeleport(boolean wardOn, boolean blockTeleports, boolean destinationWarded,
                                        boolean magicDriven, boolean bypass) {
        return wardOn && blockTeleports && destinationWarded && magicDriven && !bypass;
    }

    /** A Magic cast is blocked in a warded world, heals included, unless bypassed. */
    public static boolean blockCast(boolean wardOn, boolean worldWarded, boolean bypass) {
        return wardOn && worldWarded && !bypass;
    }

    /** Stack-frame test for "driven by Magic". */
    public static boolean anyFrameMatches(Iterable<String> classNames, Collection<String> prefixes) {
        for (String c : classNames) {
            if (c == null) continue;
            for (String p : prefixes) if (!p.isEmpty() && c.startsWith(p)) return true;
        }
        return false;
    }
}

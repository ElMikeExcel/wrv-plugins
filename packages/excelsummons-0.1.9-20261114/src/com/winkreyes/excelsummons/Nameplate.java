package com.winkreyes.excelsummons;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * The summon nameplate (0.1.1 scope addition, Mike: "make sure that
 * we can see how much health your mob has"). Format, chosen once and
 * used everywhere (nameplate + /summons list):
 *
 *     Archer — 18/20
 *
 * Base name, an em dash, current/max health in health POINTS (not
 * hearts — the numbers the spells and the bridge reason in).
 * Whole numbers print whole; a fractional value keeps one decimal.
 *
 * Composition rule (never clobber): the base is whatever name the
 * entity already carries — Magic's configured name if a spell ever
 * sets one, a player's name tag on a pet — with any previous health
 * suffix of ours stripped; only an unnamed summon falls back to its
 * spell's display name. The bridge re-renders base + suffix on its
 * ticks, so a rename by anyone is adopted as the new base on the
 * next refresh instead of being fought.
 *
 * NOT a boss bar. Mike's law: the bar is the boss. Summons get a
 * nameplate (Java and Bedrock both render custom names) and exact
 * numbers in /summons list — never a BossBar.
 *
 * Pure and unit-tested; no Bukkit types.
 *
 * Excel — Wink Reyes Valley (Iris / Mike).
 */
public final class Nameplate {

    private static final Pattern HEALTH_SUFFIX =
            Pattern.compile(" — [0-9]+(\\.[0-9])?/[0-9]+(\\.[0-9])?$");

    private Nameplate() {}

    /** 18.0 -> "18"; 17.5 -> "17.5". */
    public static String formatHp(double v) {
        long whole = Math.round(v);
        if (Math.abs(v - whole) < 0.001) return Long.toString(whole);
        return String.format(Locale.ROOT, "%.1f", v);
    }

    /** The full plate: base + " — cur/max". */
    public static String display(String base, double health, double maxHealth) {
        return base + " — " + formatHp(health) + "/" + formatHp(maxHealth);
    }

    /** Remove OUR health suffix from a rendered name, if present.
     *  Anything else is returned untouched (null stays null). */
    public static String stripHealthSuffix(String name) {
        if (name == null) return null;
        return HEALTH_SUFFIX.matcher(name).replaceAll("");
    }

    /** The base a render should use: the entity's existing name
     *  (suffix-stripped) when it has one, else the spell's display
     *  name. */
    public static String baseName(String existingCustomName, String spellBaseKey) {
        if (existingCustomName != null) {
            String stripped = stripHealthSuffix(existingCustomName).trim();
            if (!stripped.isEmpty()) return stripped;
        }
        return spellDisplayName(spellBaseKey);
    }

    /**
     * One /summons list line (0.1.3: extracted from the self list so
     * the console admin list renders a player's summons in EXACTLY
     * the same format — spell, branch, time left, HP, mana hold).
     * The rendered string is byte-for-byte the 0.1.0–0.1.2 self-list
     * line; {@code hp} is the pre-rendered "HP a/b" or "not loaded",
     * {@code timeLeft} is "persistent" or "Ns left", and a hold of
     * 0 (non-combat branch, or a family with no reserve) adds no
     * mana clause.
     */
    public static String listLine(String spellKey, String branchLower, String timeLeft,
                                  String hp, int holdPercent) {
        return "- " + spellKey + " (" + branchLower + ", " + timeLeft + ", " + hp
                + (holdPercent > 0 ? ", holds " + holdPercent + "% mana" : "") + ")";
    }

    /** Display names for the tree's spell families. */
    public static String spellDisplayName(String spellBaseKey) {
        if (spellBaseKey == null || spellBaseKey.isEmpty()) return "Summon";
        switch (spellBaseKey) {
            case "archer":   return "Archer";
            case "bruiser":  return "Bruiser";
            case "guardian": return "Guardian";
            case "revenant": return "Revenant";
            case "ghast":    return "Ghast";
            case "mount":    return "Happy Ghast";
            case "wolf":     return "Wolf";
            case "cat":      return "Cat";
            case "familiar": return "Familiar";
            default:
                return Character.toUpperCase(spellBaseKey.charAt(0))
                        + spellBaseKey.substring(1);
        }
    }
}

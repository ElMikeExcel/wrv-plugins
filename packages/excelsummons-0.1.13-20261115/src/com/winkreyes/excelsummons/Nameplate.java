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
 * 0.1.11 (P-SUMMONS-TUNING-1, Mike's live-play report, Nov 14,
 * 2026): two players' Archers both read "Archer — 20/20" and could
 * not be told apart, so the plate now carries the OWNER's name:
 *
 *     Archer (MikeExcel) — 20/20
 *
 * and, in a timed summon's final 15 seconds, a countdown segment:
 *
 *     Archer (MikeExcel) — 20/20 · 12s
 *
 * Composition rule (never clobber): the base is whatever name the
 * entity already carries — Magic's configured name if a spell ever
 * sets one, a player's name tag on a pet — with OUR OWN rendered
 * segments stripped (health suffix, countdown segment, and the
 * exact trailing " (<ownerName>)" segment for the record's current
 * owner); only an unnamed summon falls back to its spell's display
 * name. The strip of the owner segment is an EXACT match on the
 * current owner's name, never a generic parenthesis pattern — a pet
 * legitimately named "Sir Barksalot (the Great)" keeps its name.
 * The bridge re-renders base + segments on its ticks, so a rename
 * by anyone is adopted as the new base on the next refresh instead
 * of being fought, and re-rendering our own plate is idempotent.
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
    private static final Pattern COUNTDOWN_SUFFIX =
            Pattern.compile(" · [0-9]+s$");

    private Nameplate() {}

    /** 18.0 -> "18"; 17.5 -> "17.5". */
    public static String formatHp(double v) {
        long whole = Math.round(v);
        if (Math.abs(v - whole) < 0.001) return Long.toString(whole);
        return String.format(Locale.ROOT, "%.1f", v);
    }

    /** The full plate: base + " — cur/max". */
    public static String display(String base, double health, double maxHealth) {
        return display(base, null, health, maxHealth, null);
    }

    /** The owner-aware plate (0.1.11): base + " (owner)" when an
     *  owner name is known, + " — cur/max", + " · Ns" while a
     *  countdown is supplied. A null/blank owner renders exactly
     *  the pre-0.1.11 plate — never a UUID, never "null". */
    public static String display(String base, String ownerName, double health,
                                 double maxHealth, Long countdownSeconds) {
        StringBuilder sb = new StringBuilder(base);
        if (ownerName != null && !ownerName.isBlank()) {
            sb.append(" (").append(ownerName).append(")");
        }
        sb.append(" — ").append(formatHp(health)).append("/").append(formatHp(maxHealth));
        if (countdownSeconds != null && countdownSeconds > 0) {
            sb.append(" · ").append(countdownSeconds).append("s");
        }
        return sb.toString();
    }

    /** Remove OUR health suffix from a rendered name, if present.
     *  Anything else is returned untouched (null stays null). A
     *  trailing countdown segment is removed first — it renders
     *  AFTER the health suffix, so it must come off before the
     *  health pattern can match the end of the string. */
    public static String stripHealthSuffix(String name) {
        if (name == null) return null;
        String s = COUNTDOWN_SUFFIX.matcher(name).replaceAll("");
        return HEALTH_SUFFIX.matcher(s).replaceAll("");
    }

    /** Remove OUR owner segment: a trailing " (<ownerName>)" that
     *  matches the record's CURRENT owner name exactly. Any other
     *  parenthesised ending is the pet's own name and survives. */
    public static String stripOwnerSegment(String name, String ownerName) {
        if (name == null) return null;
        if (ownerName == null || ownerName.isBlank()) return name;
        String seg = " (" + ownerName + ")";
        return name.endsWith(seg) ? name.substring(0, name.length() - seg.length()) : name;
    }

    /** Strip every segment a render of ours may have added, in
     *  render order from the end: countdown, health, owner. */
    public static String stripPlate(String name, String ownerName) {
        if (name == null) return null;
        return stripOwnerSegment(stripHealthSuffix(name), ownerName);
    }

    /** The base a render should use: the entity's existing name
     *  (our segments stripped) when it has one, else the spell's
     *  display name. */
    public static String baseName(String existingCustomName, String spellBaseKey) {
        return baseName(existingCustomName, spellBaseKey, null);
    }

    /** Owner-aware base (0.1.11): the owner segment for the
     *  record's current owner is stripped before the base is read,
     *  so a re-render never stacks "(Owner) (Owner)". */
    public static String baseName(String existingCustomName, String spellBaseKey,
                                  String ownerName) {
        if (existingCustomName != null) {
            String stripped = stripPlate(existingCustomName, ownerName).trim();
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

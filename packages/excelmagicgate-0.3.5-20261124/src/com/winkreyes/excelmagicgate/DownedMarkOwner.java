package com.winkreyes.excelmagicgate;

/**
 * Pure matcher for the wand-owner contamination repaired in 0.3.4
 * (the 0.2.1 fix, re-based onto the 0.3.x rank system).
 *
 * While a player is downed, ExcelDowned shows their name as
 * "✝ DOWNED <name>". A Magic wand bound to its owner's display name
 * derives its owner string from that display name, so a wand saved
 * while its owner was downed came back with the owner
 * "✝ DOWNED <name>" — and at the next login it failed Magic's owner
 * validation and was silently deleted (proven live Nov 23, 2026;
 * the wand is the one item class whose validity depends on an owner
 * string, and it carries keep + undroppable marks precisely so it
 * survives death).
 *
 * A wand is contaminated — and only then — when its owner string,
 * color codes stripped, is exactly the downed-marked form of the
 * CARRYING player's own name. Anything else (a clean owner, another
 * player's name marked or not, an unowned wand) is not ours to
 * touch: a genuinely foreign wand must never be re-stamped.
 *
 * The prefix literal is ExcelDowned's DownedMark.STRIPPED_PREFIX;
 * the two are pinned to the incident string by unit tests on both
 * sides so they can never drift apart.
 *
 * Excel — Wink Reyes Valley (Iris / Mike).
 */
public final class DownedMarkOwner {

    /** ExcelDowned's downed mark with no color codes: "✝ DOWNED ". */
    public static final String DOWNED_PREFIX = "✝ DOWNED ";

    private DownedMarkOwner() {}

    /**
     * True when {@code owner} is the downed-marked form of
     * {@code playerName}: the mark followed by exactly that name,
     * with any legacy color codes in the stored owner ignored
     * (Magic stores owners color-stripped; the raw marked name
     * carries codes — both forms match).
     */
    public static boolean isContaminated(String owner, String playerName) {
        if (owner == null || playerName == null || playerName.isEmpty()) {
            return false;
        }
        return stripLegacyCodes(owner).equals(DOWNED_PREFIX + playerName);
    }

    /**
     * ChatColor.stripColor, hand-rolled: this matcher stays free of
     * Bukkit types so the plugin's plain-JVM test harness can run it
     * (the same reason RankFloor and FloorPlan are pure). Semantics
     * match stripColor exactly — a section sign followed by one code
     * character (0-9, a-f, k-o, r, x; either case) is removed.
     */
    static String stripLegacyCodes(String s) {
        StringBuilder out = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '§' && i + 1 < s.length() && isCodeChar(s.charAt(i + 1))) {
                i++;
                continue;
            }
            out.append(c);
        }
        return out.toString();
    }

    private static boolean isCodeChar(char c) {
        return (c >= '0' && c <= '9')
                || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F')
                || (c >= 'k' && c <= 'o') || (c >= 'K' && c <= 'O')
                || c == 'r' || c == 'R' || c == 'x' || c == 'X';
    }
}

package excelemfixes.meleemitigation;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The on-item indicator for the off-hand bonus — pure text
 * and lore-list logic.
 *
 * The ruling: while the off-hand bonus is ACTIVE (off hand
 * holds a totem/shield-like item AND the main hand holds a
 * melee weapon), the off-hand item itself shows a line with
 * ITS OWN percentage — "7% mitigation activated" for a
 * shield, "2% mitigation activated" for a totem. While the
 * bonus is not active the item shows nothing: no line at
 * all, not a greyed-out one.
 *
 * The line is identified by its exact plain text (the
 * module writes it in green, {@link #LINE_COLOR}), so the
 * reconcile step can add it when it becomes active, remove
 * exactly it when it stops being active, refresh it when
 * the percentage changes, and never touch any other lore
 * line the item carries (EliteMobs items carry long lore —
 * preserving it byte-for-byte is the contract).
 */
public final class IndicatorText {

    private IndicatorText() {}

    /** ChatColor green, as a raw section-sign prefix. */
    public static final String LINE_COLOR = "§a";

    /** Plain (colour-stripped) suffix every indicator line ends with. */
    public static final String SUFFIX = "% mitigation activated";

    /** Formats a fraction as a player-facing percent: 0.07 -> "7", 0.025 -> "2.5". */
    public static String formatPercent(double percent) {
        double pct = percent * 100.0;
        if (pct == Math.floor(pct)) {
            return String.format(Locale.ROOT, "%d", (long) pct);
        }
        String s = String.format(Locale.ROOT, "%.2f", pct);
        while (s.contains(".") && s.endsWith("0")) s = s.substring(0, s.length() - 1);
        if (s.endsWith(".")) s = s.substring(0, s.length() - 1);
        return s;
    }

    /** The full lore line (with colour) for a bonus percentage. */
    public static String lineFor(double bonusPercent) {
        return LINE_COLOR + formatPercent(bonusPercent) + SUFFIX;
    }

    /** True when a lore line is one of this module's indicator lines. */
    public static boolean isIndicatorLine(String line) {
        if (line == null) return false;
        String plain = line.replaceAll("§[0-9a-fk-orx]", "");
        return plain.endsWith(SUFFIX) && plain.length() > SUFFIX.length();
    }

    /**
     * Reconciles a lore list with the desired state.
     *
     * @param lore         current lore (may be null)
     * @param desiredLine  the indicator line to show, or null to show nothing
     * @return the new lore list, or null when no change is needed
     *         (so the caller can skip rewriting the item entirely)
     */
    public static List<String> reconcile(List<String> lore, String desiredLine) {
        List<String> current = lore == null ? List.of() : lore;
        List<String> kept = new ArrayList<>();
        boolean alreadyPresent = false;
        for (String line : current) {
            if (isIndicatorLine(line)) {
                if (desiredLine != null && line.equals(desiredLine)) {
                    alreadyPresent = true;
                    kept.add(line);
                }
                // Any other indicator line (stale percent or
                // no longer active) is dropped.
            } else {
                kept.add(line);
            }
        }
        if (desiredLine != null && !alreadyPresent) kept.add(desiredLine);
        if (kept.equals(current)) return null;
        return kept;
    }
}

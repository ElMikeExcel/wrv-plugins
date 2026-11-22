package com.winkreyes.excelemcompanion.tracker;

import com.winkreyes.excelemcompanion.shared.Views;

import java.util.ArrayList;
import java.util.List;

/**
 * The expertise scale (D187): a kill/clear count's color IS the
 * mastery signal. The tiers reuse EliteMobs' own quality colors in
 * the same order as the drop guide's rarity ladder — gray, white,
 * green, blue, purple, gold, mythic light-blue — with clean count
 * thresholds, identical for per-boss kills and per-dungeon clears.
 *
 * The legend (Mike, Nov 21, 2026: the tracker must explain the
 * colors, with the count range for each tier, in small text that
 * looks good) is an info item in the bosses and dungeons menus:
 * its lore lists every tier, and its detail screen carries the
 * same lines to Bedrock forms.
 */
public final class Expertise {

    public record Tier(int min, int max, String color, String label) {
        String range() {
            if (min == max) return "×" + min;
            if (max == Integer.MAX_VALUE) return "×" + min + "+";
            return "×" + min + "–" + max;
        }
    }

    public static final List<Tier> TIERS = List.of(
            new Tier(1, 1, "&7", "Novice"),
            new Tier(2, 4, "&f", "Familiar"),
            new Tier(5, 9, "&a", "Experienced"),
            new Tier(10, 19, "&9", "Veteran"),
            new Tier(20, 39, "&5", "Expert"),
            new Tier(40, 74, "&6", "Master"),
            new Tier(75, Integer.MAX_VALUE, "&b", "Mastered"));

    private Expertise() {}

    /** The tier for a count, or null when there is nothing to show. */
    public static Tier tierFor(int count) {
        if (count <= 0) return null;
        for (Tier t : TIERS) {
            if (count >= t.min() && count <= t.max()) return t;
        }
        return TIERS.get(TIERS.size() - 1);
    }

    /** "&7Kills: &a×21" — the label gray, the count in tier color. */
    public static String countLine(String label, int count) {
        Tier t = tierFor(count);
        if (t == null) return "&7" + label + ": &8—";
        return "&7" + label + ": " + t.color() + "×" + count;
    }

    /** The tier's label in its own color, e.g. "&aExperienced". */
    public static String tierLine(int count) {
        Tier t = tierFor(count);
        return t == null ? "" : t.color() + t.label();
    }

    /** Compact legend lines: gray range, tier name in tier color. */
    public static List<String> legendLines() {
        List<String> lines = new ArrayList<>();
        lines.add("&fExpertise — the color is the mastery:");
        for (Tier t : TIERS) {
            lines.add("&8" + t.range() + "  " + t.color() + t.label());
        }
        lines.add("&8Counts are your killing blows and clears");
        lines.add("&8in the server's records (ExcelProtect).");
        return lines;
    }

    /** The legend as a menu info item (same content both platforms). */
    public static Views.Button legendButton() {
        List<String> lines = legendLines();
        return new Views.Button("core:noop", "&fExpertise Legend", "BOOK",
                lines, new Views.Detail("Expertise Legend", "BOOK", lines),
                false);
    }
}

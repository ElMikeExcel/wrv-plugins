package com.winkreyes.makersmark.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Pattern;

/** The plain text of the lines this plugin owns on an item. Colors are added by the caller (gray). */
public final class LoreModel {
    private LoreModel() {}

    private static final Pattern MARK = Pattern.compile("^Crafted by .{1,40}$");
    private static final Pattern KILLS = Pattern.compile("^Kills: \\d+( \\(Players: \\d+\\))?$");
    private static final Pattern HITS = Pattern.compile("^Hits blocked: \\d+$");
    private static final Pattern SLAIN = Pattern.compile("^Slain: .+$");

    /** A line that looks like ours: these are always removed and rebuilt from the tag, so they cannot be faked. */
    public static boolean looksOurs(String plain) {
        if (plain == null) return false;
        return MARK.matcher(plain).matches() || KILLS.matcher(plain).matches() || HITS.matcher(plain).matches() || SLAIN.matcher(plain).matches();
    }

    public static String killsLine(int mobs, int players) {
        int total = Math.max(0, mobs) + Math.max(0, players);
        return "Kills: " + total + (players > 0 ? " (Players: " + players + ")" : "");
    }

    /**
     * Lines in order: maker's mark, kills (weapons), hits blocked (armor), slain (weapons).
     * Counter lines only when showStats and the count is above zero.
     */
    public static List<String> lines(String crafter, boolean weapon, boolean armor, int mobKills, int playerKills, int hits,
                                     Map<String, Integer> trophies, boolean showStats, Function<String, String> label, int wrapAt) {
        List<String> out = new ArrayList<>();
        if (crafter != null && !crafter.isBlank()) out.add("Crafted by " + crafter);
        if (showStats) {
            if (weapon && mobKills + playerKills > 0) out.add(killsLine(mobKills, playerKills));
            if (armor && hits > 0) out.add("Hits blocked: " + hits);
            if (trophies != null && !trophies.isEmpty()) out.addAll(Trophies.lines(trophies, label, wrapAt));
        }
        return out;
    }
}

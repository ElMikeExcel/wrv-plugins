package com.winkreyes.exceleliteladder.core;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Key-level reader and patcher for EliteMobs' MobCombatSettings.yml. Only the six ladder
 * lines are ever rewritten; every other byte (comments, order, line endings) is kept.
 * Pure Java, no Bukkit, unit tested on the plain JVM.
 */
public final class MobCombatFile {
    public static final String SPAWN = "eliteMobsSpawnPercentage";
    public static final String CAP = "naturalEliteMobsLevelCap";
    public static final String NORM_ELITE = "normalizedDamageToEliteMultiplier";
    public static final String NORM_PLAYER = "normalizedDamageToPlayerMultiplier";
    public static final String SCALED_ELITE = "scaledDamageToEliteMultiplier";
    public static final String SCALED_PLAYER = "scaledDamageToPlayerMultiplier";
    public static final List<String> KEYS = List.of(SPAWN, CAP, NORM_ELITE, NORM_PLAYER, SCALED_ELITE, SCALED_PLAYER);

    private static final Pattern NUMBER = Pattern.compile("-?[0-9]+(\\.[0-9]+)?");

    private MobCombatFile() {
    }

    /**
     * Number text exactly as EliteMobs 10.9.10 writes it back when it loads the file
     * (proven on the rig: 0.10 -> 0.1, 2.20 -> 2.2, 0.50 -> 0.5). Writing this form means
     * EliteMobs' own rewrite on load leaves our bytes unchanged.
     */
    public static String format(String key, double v) {
        if (CAP.equals(key)) {
            return Integer.toString((int) Math.round(v));
        }
        return Double.toString(v);
    }

    /** Line index of a top-level key, or -1. Throws when the key appears more than once. */
    static int lineOf(List<String> lines, String key) {
        int found = -1;
        String prefix = key + ":";
        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).startsWith(prefix)) {
                if (found >= 0) {
                    throw new LadderException(key + " appears more than once (lines " + (found + 1) + " and " + (i + 1) + ")");
                }
                found = i;
            }
        }
        return found;
    }

    /** Split keeping each line's own terminator out of the content (\r stays with content). */
    static List<String> lines(String text) {
        List<String> out = new ArrayList<>();
        int start = 0;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '\n') {
                out.add(text.substring(start, i));
                start = i + 1;
            }
        }
        out.add(text.substring(start));
        return out;
    }

    private static String valuePart(String line, String key) {
        String rest = line.substring(key.length() + 1);
        if (rest.endsWith("\r")) {
            rest = rest.substring(0, rest.length() - 1);
        }
        int hash = rest.indexOf(" #");
        if (hash >= 0) {
            rest = rest.substring(0, hash);
        }
        return rest.trim();
    }

    /** The six ladder values from the file text. Throws LadderException with a clear reason. */
    public static Map<String, Double> read(String text) {
        List<String> lines = lines(text);
        Map<String, Double> out = new LinkedHashMap<>();
        for (String key : KEYS) {
            int i = lineOf(lines, key);
            if (i < 0) {
                throw new LadderException(key + " is missing");
            }
            String v = valuePart(lines.get(i), key);
            if (!NUMBER.matcher(v).matches()) {
                throw new LadderException(key + " is not a plain number (line " + (i + 1) + ": '" + v + "')");
            }
            out.put(key, Double.parseDouble(v));
        }
        return out;
    }

    /** New file text with the six lines set to the step's values; everything else identical. */
    public static String patch(String text, LadderStep step) {
        read(text); // validates presence, uniqueness and number form first
        List<String> lines = lines(text);
        for (Map.Entry<String, Double> e : step.values().entrySet()) {
            int i = lineOf(lines, e.getKey());
            String cr = lines.get(i).endsWith("\r") ? "\r" : "";
            lines.set(i, e.getKey() + ": " + format(e.getKey(), e.getValue()) + cr);
        }
        return String.join("\n", lines);
    }

    /**
     * Checks a written file against the text it was made from: same line count, only the six
     * ladder lines changed, and the six values equal the step. Returns null when good.
     */
    public static String verify(String before, String after, LadderStep step) {
        List<String> a = lines(before);
        List<String> b = lines(after);
        if (a.size() != b.size()) {
            return "line count changed (" + a.size() + " -> " + b.size() + ")";
        }
        Map<String, Double> got;
        try {
            got = read(after);
        } catch (LadderException ex) {
            return "written file unreadable: " + ex.getMessage();
        }
        if (!LadderTable.matches(got, step)) {
            return "written values differ from " + step.label() + ": " + LadderTable.differences(got, step);
        }
        for (int i = 0; i < a.size(); i++) {
            if (a.get(i).equals(b.get(i))) {
                continue;
            }
            boolean ladderLine = false;
            for (String key : KEYS) {
                if (b.get(i).startsWith(key + ":") && a.get(i).startsWith(key + ":")) {
                    ladderLine = true;
                    break;
                }
            }
            if (!ladderLine) {
                return "unexpected change on line " + (i + 1);
            }
        }
        return null;
    }

    static boolean isNumber(String s) {
        Matcher m = NUMBER.matcher(s);
        return m.matches();
    }
}

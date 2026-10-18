package com.winkreyes.excelquartermaster;

import java.util.List;

/**
 * Chat shaping for one tale: a tale is sent as ONE chat message when it is
 * short, or split into AT MOST TWO when it is long, so a single click never
 * floods the chat.
 *
 * No Bukkit dependencies (unit-testable). Lengths are measured in VISIBLE
 * characters: legacy color/format codes (&amp;x or §x, two characters) do
 * not count. When a split happens, it lands on the space nearest the middle
 * of the visible text, and the second line re-applies the first line's
 * leading color/format code run, so both halves read in the same color.
 * A line with no usable space (one giant word) is returned unsplit rather
 * than cut mid-word.
 */
public final class TaleText {

    /** Visible characters above which a tale is split in two. */
    public static final int MAX_SINGLE_LINE_VISIBLE = 110;

    private TaleText() {}

    /** Visible length of a chat string, ignoring &x / §x code pairs. */
    public static int visibleLength(String text) {
        if (text == null) {
            return 0;
        }
        int visible = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if ((c == '&' || c == '§') && i + 1 < text.length() && isCodeChar(text.charAt(i + 1))) {
                i++;
                continue;
            }
            visible++;
        }
        return visible;
    }

    /**
     * Split one tale into 1 or 2 chat lines (see class docs). Null or blank
     * input yields an empty list.
     */
    public static List<String> splitForChat(String line) {
        if (line == null || line.isBlank()) {
            return List.of();
        }
        int visible = visibleLength(line);
        if (visible <= MAX_SINGLE_LINE_VISIBLE) {
            return List.of(line);
        }
        int target = visible / 2;
        int bestSpace = -1;
        int bestDistance = Integer.MAX_VALUE;
        int seen = 0;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if ((c == '&' || c == '§') && i + 1 < line.length() && isCodeChar(line.charAt(i + 1))) {
                i++;
                continue;
            }
            if (c == ' ' && seen > 0) {
                int distance = Math.abs(seen - target);
                if (distance < bestDistance) {
                    bestDistance = distance;
                    bestSpace = i;
                }
            }
            seen++;
        }
        if (bestSpace < 0) {
            return List.of(line);
        }
        String first = line.substring(0, bestSpace);
        String second = line.substring(bestSpace + 1);
        String prefix = leadingCodeRun(line);
        if (!prefix.isEmpty() && !second.startsWith("&") && !second.startsWith("§")) {
            second = prefix + second;
        }
        return List.of(first, second);
    }

    /** The run of color/format codes at the very start of the string. */
    private static String leadingCodeRun(String text) {
        StringBuilder out = new StringBuilder();
        int i = 0;
        while (i + 1 < text.length()
                && (text.charAt(i) == '&' || text.charAt(i) == '§')
                && isCodeChar(text.charAt(i + 1))) {
            out.append(text.charAt(i)).append(text.charAt(i + 1));
            i += 2;
        }
        return out.toString();
    }

    /** Legacy code characters: 0-9, a-f, k-o, r (either case), and x for hex. */
    private static boolean isCodeChar(char c) {
        char lower = Character.toLowerCase(c);
        return (lower >= '0' && lower <= '9')
                || (lower >= 'a' && lower <= 'f')
                || (lower >= 'k' && lower <= 'o')
                || lower == 'r'
                || lower == 'x';
    }
}

package com.winkreyes.excelwandbars.core;

import java.util.List;

/** Short action-bar line after a flip: "Bar 2/3: Boon, Feed, Extinguish". */
public final class BarText {
    private BarText() {}

    public static final int MAX_LEN = 60;

    /** @param current 0-based bar index; names = spell names on that bar, in slot order. */
    public static String line(int current, int count, List<String> names) {
        StringBuilder sb = new StringBuilder("Bar ").append(current + 1).append('/').append(count);
        if (names == null || names.isEmpty()) return sb.append(": (empty)").toString();
        sb.append(": ");
        int shown = 0;
        for (String n : names) {
            if (n == null || n.isBlank()) continue;
            String add = (shown == 0 ? "" : ", ") + n.trim();
            int remaining = names.size() - shown;
            if (sb.length() + add.length() > MAX_LEN && shown > 0) {
                sb.append(" +").append(remaining);
                return sb.toString();
            }
            sb.append(add);
            shown++;
        }
        if (shown == 0) sb.append("(empty)");
        return sb.toString();
    }
}

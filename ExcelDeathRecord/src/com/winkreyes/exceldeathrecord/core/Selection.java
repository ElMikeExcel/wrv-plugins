package com.winkreyes.exceldeathrecord.core;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

/**
 * Item selection for /deathrecord restore: "all", one number
 * ("3"), a list ("2,3,7"), a range ("1-5"), or any mix
 * ("1-3,7"). Numbers are the 1-based item numbers the record
 * view prints. Pure, unit-tested.
 */
public final class Selection {

    /** Parse result: sorted 1-based numbers, or an error line. */
    public static final class Result {
        public final List<Integer> numbers;
        public final String error;   // null on success
        public final boolean all;
        Result(List<Integer> numbers, String error, boolean all) {
            this.numbers = numbers; this.error = error; this.all = all;
        }
        public boolean ok() { return error == null; }
    }

    private Selection() {}

    public static Result parse(String text, int itemCount) {
        if (text == null || text.isBlank()) return new Result(List.of(), "No items picked. Use all, 3, 2,3,7 or 1-5.", false);
        String t = text.trim().toLowerCase(java.util.Locale.ROOT);
        if (itemCount <= 0) return new Result(List.of(), "That record has no items to restore.", false);
        if (t.equals("all")) {
            List<Integer> all = new ArrayList<>();
            for (int i = 1; i <= itemCount; i++) all.add(i);
            return new Result(all, null, true);
        }
        TreeSet<Integer> picked = new TreeSet<>();
        for (String part : t.split(",")) {
            String p = part.trim();
            if (p.isEmpty()) continue;
            int dash = p.indexOf('-');
            try {
                if (dash > 0) {
                    int a = Integer.parseInt(p.substring(0, dash).trim());
                    int b = Integer.parseInt(p.substring(dash + 1).trim());
                    if (a > b) { int s = a; a = b; b = s; }
                    if (a < 1 || b > itemCount) return outOfRange(p, itemCount);
                    for (int i = a; i <= b; i++) picked.add(i);
                } else {
                    int n = Integer.parseInt(p);
                    if (n < 1 || n > itemCount) return outOfRange(p, itemCount);
                    picked.add(n);
                }
            } catch (NumberFormatException e) {
                return new Result(List.of(), "'" + part.trim() + "' is not an item number. Use all, 3, 2,3,7 or 1-5.", false);
            }
        }
        if (picked.isEmpty()) return new Result(List.of(), "No items picked. Use all, 3, 2,3,7 or 1-5.", false);
        return new Result(new ArrayList<>(picked), null, picked.size() == itemCount);
    }

    private static Result outOfRange(String p, int itemCount) {
        return new Result(List.of(), "Item " + p + " is out of range: this record has items 1-" + itemCount + ".", false);
    }

    /** Compact text form of a selection for hints and logs: "2,3,7", "1-5", "all". */
    public static String format(List<Integer> numbers, int itemCount) {
        if (numbers.isEmpty()) return "";
        if (numbers.size() == itemCount && itemCount > 1) return "all";
        StringBuilder sb = new StringBuilder();
        int i = 0;
        while (i < numbers.size()) {
            int start = numbers.get(i), end = start;
            while (i + 1 < numbers.size() && numbers.get(i + 1) == end + 1) { end++; i++; }
            if (sb.length() > 0) sb.append(',');
            if (end - start >= 2) sb.append(start).append('-').append(end);
            else if (end == start + 1) sb.append(start).append(',').append(end);
            else sb.append(start);
            i++;
        }
        return sb.toString();
    }
}

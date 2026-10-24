package com.winkreyes.makersmark.core;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/** Boss trophies: "ENDER_DRAGON=1;WITHER=2" in the item tag, "Slain: Ender Dragon, Wither x2" on the item. */
public final class Trophies {
    private Trophies() {}

    public static Map<String, Integer> parse(String s) {
        Map<String, Integer> m = new LinkedHashMap<>();
        if (s == null || s.isBlank()) return m;
        for (String part : s.split(";")) {
            int eq = part.lastIndexOf('=');
            if (eq <= 0) continue;
            try {
                int n = Integer.parseInt(part.substring(eq + 1).trim());
                if (n > 0) m.merge(part.substring(0, eq).trim(), n, Integer::sum);
            } catch (NumberFormatException ignored) { }
        }
        return m;
    }

    public static String serialize(Map<String, Integer> m) {
        StringBuilder b = new StringBuilder();
        for (Map.Entry<String, Integer> e : m.entrySet()) {
            if (e.getValue() == null || e.getValue() <= 0) continue;
            if (b.length() > 0) b.append(';');
            b.append(e.getKey().replace(";", "").replace("=", "")).append('=').append(e.getValue());
        }
        return b.toString();
    }

    /** Higher count wins per boss (grid repair, anvil combine). */
    public static Map<String, Integer> max(Map<String, Integer> a, Map<String, Integer> b) {
        Map<String, Integer> m = new LinkedHashMap<>(a);
        for (Map.Entry<String, Integer> e : b.entrySet()) m.merge(e.getKey(), e.getValue(), Math::max);
        return m;
    }

    /** One line, or two when it is longer than wrapAt characters. The second line is indented. */
    public static List<String> lines(Map<String, Integer> m, Function<String, String> label, int wrapAt) {
        List<String> parts = new ArrayList<>();
        for (Map.Entry<String, Integer> e : m.entrySet()) {
            if (e.getValue() == null || e.getValue() <= 0) continue;
            parts.add(label.apply(e.getKey()) + (e.getValue() > 1 ? " x" + e.getValue() : ""));
        }
        List<String> out = new ArrayList<>();
        if (parts.isEmpty()) return out;
        String first = "Slain: " + String.join(", ", parts);
        if (wrapAt <= 0 || first.length() <= wrapAt || parts.size() == 1) { out.add(first); return out; }
        StringBuilder l1 = new StringBuilder("Slain: ");
        int i = 0;
        for (; i < parts.size(); i++) {
            String add = (i == 0 ? "" : ", ") + parts.get(i);
            if (i > 0 && l1.length() + add.length() + 1 > wrapAt) break;
            l1.append(add);
        }
        if (i < parts.size()) l1.append(',');
        out.add(l1.toString());
        if (i < parts.size()) out.add("  " + String.join(", ", parts.subList(i, parts.size())));
        return out;
    }

    /** "ENDER_DRAGON" becomes "Ender Dragon"; "name:zombie_king" becomes "Zombie King". */
    public static String pretty(String id) {
        String s = id.contains(":") ? id.substring(id.indexOf(':') + 1) : id;
        StringBuilder b = new StringBuilder();
        for (String w : s.toLowerCase().split("[_\\s]+")) {
            if (w.isEmpty()) continue;
            if (b.length() > 0) b.append(' ');
            b.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1));
        }
        return b.toString();
    }
}

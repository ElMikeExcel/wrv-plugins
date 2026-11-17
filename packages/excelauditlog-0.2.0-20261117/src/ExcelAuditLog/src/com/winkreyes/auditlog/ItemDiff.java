package com.winkreyes.auditlog;

import java.util.Map;
import java.util.TreeMap;

/** Diff of item key -> count maps. Keys are item descriptions (material + name + enchants). */
public final class ItemDiff {
    private ItemDiff() {}

    /** Returns "+3 DIAMOND; -1 NETHERITE_SHOVEL{...}" style text, or "" when nothing changed. */
    public static String diff(Map<String, Integer> before, Map<String, Integer> after) {
        TreeMap<String, Integer> d = new TreeMap<>();
        for (Map.Entry<String, Integer> e : after.entrySet()) d.merge(e.getKey(), e.getValue(), Integer::sum);
        for (Map.Entry<String, Integer> e : before.entrySet()) d.merge(e.getKey(), -e.getValue(), Integer::sum);
        StringBuilder b = new StringBuilder();
        for (Map.Entry<String, Integer> e : d.entrySet()) {
            int n = e.getValue();
            if (n == 0) continue;
            if (b.length() > 0) b.append("; ");
            b.append(n > 0 ? "+" : "").append(n).append(' ').append(e.getKey());
        }
        return b.toString();
    }
}

package com.winkreyes.excelautosort.core;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Filter matching and reject computation (SPEC §5.2, §5.8). Matching is
 * by material name only in 0.1.0; a stack passes or goes, entire.
 */
public final class FilterCore {

    private FilterCore() {}

    /** One physical stack in a chest: material name + size. */
    public record ContentStack(String material, int count) {}

    /** The itemized reject line the arm/confirm screen shows (§5.8). */
    public record RejectLine(String material, int totalCount, int stacks) {}

    /** True when a stack of this material would be removed under the filter. */
    public static boolean isReject(Mode mode, Set<String> list, String material) {
        if (mode == null || mode == Mode.OFF || material == null) return false;
        boolean listed = list != null && list.contains(material);
        return switch (mode) {
            case WHITELIST -> !listed;
            case BLACKLIST -> listed;
            case OFF -> false;
        };
    }

    /**
     * The exact reject set of a chest's current contents under a mode +
     * list, aggregated per material in first-seen order — the confirm
     * screen's itemization (§5.8, §7.4).
     */
    public static List<RejectLine> rejects(Mode mode, Set<String> list, List<ContentStack> contents) {
        Map<String, int[]> byMaterial = new LinkedHashMap<>();
        if (contents != null) {
            for (ContentStack s : contents) {
                if (s == null || s.count() <= 0) continue;
                if (!isReject(mode, list, s.material())) continue;
                int[] agg = byMaterial.computeIfAbsent(s.material(), k -> new int[2]);
                agg[0] += s.count();
                agg[1] += 1;
            }
        }
        List<RejectLine> out = new ArrayList<>();
        byMaterial.forEach((mat, agg) -> out.add(new RejectLine(mat, agg[0], agg[1])));
        return out;
    }

    public static int totalStacks(List<RejectLine> lines) {
        int n = 0;
        for (RejectLine l : lines) n += l.stacks();
        return n;
    }

    public static int totalItems(List<RejectLine> lines) {
        int n = 0;
        for (RejectLine l : lines) n += l.totalCount();
        return n;
    }
}

// ExcelAutoSort — Iris / Mike, for Wink Reyes Valley

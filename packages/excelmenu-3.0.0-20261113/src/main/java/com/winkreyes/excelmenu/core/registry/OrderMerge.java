// Excel signature - WRV maintained source; existing authorship notices preserved.
package com.winkreyes.excelmenu.core.registry;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The pure merge step of the 3.0 rearrange feature (DESIGN §3.3):
 * a player's stored id lists are merged over the resolver's default
 * order at resolve time. Properties pinned by OrderMergeTest:
 * untouched groups keep default order; stored order is honoured;
 * entries new since the save append at their group's end (in
 * default order); vanished ids drop silently; unknown groups are
 * ignored; the merge is idempotent and never reorders a group the
 * player did not touch.
 */
public final class OrderMerge {

    private OrderMerge() {}

    /** Merge two id lists: stored ids that still exist, in stored
     *  order, then default ids not in stored, in default order. */
    public static List<String> mergeIds(List<String> defaultIds, List<String> stored) {
        if (stored == null || stored.isEmpty()) return List.copyOf(defaultIds);
        List<String> out = new ArrayList<>();
        for (String id : stored) {
            if (defaultIds.contains(id) && !out.contains(id)) out.add(id);
        }
        for (String id : defaultIds) {
            if (!out.contains(id)) out.add(id);
        }
        return List.copyOf(out);
    }

    /** Effective picker group order: stored groups first (those
     *  that still exist), then unlisted groups in default order. */
    public static List<String> effectiveGroupOrder(List<String> defaultOrder,
                                                   PlayerMenuPrefs prefs) {
        if (prefs == null || prefs.groupOrder().isEmpty()) return List.copyOf(defaultOrder);
        return mergeIds(defaultOrder, prefs.groupOrder());
    }

    /**
     * Re-sort a resolved list by a player's preferences.
     *
     * @param defaultGroupOrder the menu's default group sequence
     *        (config group order; may be empty, in which case the
     *        resolved list's first-appearance order is the default)
     * @param applyGroupOrder true for the Bedrock picker path (the
     *        player's group-order preference applies); false for
     *        Java, where groups own fixed pages/slots and the
     *        preference has no visual effect (DESIGN §3.3) — only
     *        within-group order applies there.
     */
    public static List<ResolvedEntry> merge(List<ResolvedEntry> resolved,
                                            PlayerMenuPrefs prefs,
                                            List<String> defaultGroupOrder,
                                            boolean applyGroupOrder) {
        if (prefs == null || prefs.isEmpty()) return resolved;
        // Default group sequence.
        List<String> baseSeq = new ArrayList<>();
        if (defaultGroupOrder != null) baseSeq.addAll(defaultGroupOrder);
        for (ResolvedEntry r : resolved) {
            if (!baseSeq.contains(r.entry().group())) baseSeq.add(r.entry().group());
        }
        List<String> groupSeq = applyGroupOrder
                ? effectiveGroupOrder(baseSeq, prefs) : baseSeq;
        Map<String, Integer> groupIdx = new HashMap<>();
        for (int i = 0; i < groupSeq.size(); i++) groupIdx.put(groupSeq.get(i), i);

        // Within-group position: stored index when the group was
        // touched; otherwise "untouched" and resolver order stands.
        Map<String, Map<String, Integer>> storedPos = new HashMap<>();
        for (Map.Entry<String, List<String>> e : prefs.groups().entrySet()) {
            Map<String, Integer> pos = new HashMap<>();
            for (int i = 0; i < e.getValue().size(); i++) pos.put(e.getValue().get(i), i);
            storedPos.put(e.getKey(), pos);
        }
        Map<ResolvedEntry, Integer> original = new LinkedHashMap<>();
        for (int i = 0; i < resolved.size(); i++) original.put(resolved.get(i), i);

        List<ResolvedEntry> out = new ArrayList<>(resolved);
        out.sort(Comparator
                .comparingInt((ResolvedEntry r) -> groupIdx.getOrDefault(r.entry().group(), Integer.MAX_VALUE))
                .thenComparingInt(r -> {
                    Map<String, Integer> pos = storedPos.get(r.entry().group());
                    if (pos == null) return Integer.MAX_VALUE;
                    return pos.getOrDefault(r.id(), Integer.MAX_VALUE);
                })
                .thenComparingInt(original::get));
        return List.copyOf(out);
    }
}

// ExcelMenu — an Excel plugin by WinkReyesValley. (Excel signature — MikeExcel)

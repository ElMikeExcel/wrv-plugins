package com.winkreyes.excelautosort.core;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * The selector's shared model (SPEC §7): ONE ordered entry list drives
 * both the Java grid and the Bedrock toggle form, so the two platforms
 * can never drift apart in order, state or wording inputs. Pure JDK.
 *
 * Order: distinct materials currently in the chest (first-seen order),
 * then list entries not present in the chest (list order) — prunable
 * without fetching the item (§7.2).
 */
public final class SelectorModel {

    private SelectorModel() {}

    /** One selector row: a material, whether it is on the list, how many are in the chest. */
    public record Entry(String material, boolean onList, int inChest) {}

    /**
     * Build the full entry list.
     *
     * @param chestContents  the chest's stacks, in slot order (duplicates merge)
     * @param list           the filter list (material names)
     */
    public static List<Entry> entries(List<FilterCore.ContentStack> chestContents, Set<String> list) {
        List<Entry> out = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        if (chestContents != null) {
            // Aggregate counts in first-seen order.
            java.util.Map<String, Integer> counts = new java.util.LinkedHashMap<>();
            for (FilterCore.ContentStack s : chestContents) {
                if (s == null || s.count() <= 0) continue;
                counts.merge(s.material(), s.count(), Integer::sum);
            }
            counts.forEach((mat, n) -> {
                out.add(new Entry(mat, list != null && list.contains(mat), n));
                seen.add(mat);
            });
        }
        if (list != null) {
            for (String mat : list) {
                if (!seen.contains(mat)) out.add(new Entry(mat, true, 0));
            }
        }
        return out;
    }

    /** One page of the entry list (page-size from config, default 28). */
    public static List<Entry> page(List<Entry> all, int page, int pageSize) {
        if (all == null || all.isEmpty() || pageSize <= 0) return List.of();
        int from = Math.max(0, page) * pageSize;
        if (from >= all.size()) return List.of();
        return new ArrayList<>(all.subList(from, Math.min(all.size(), from + pageSize)));
    }

    public static int pageCount(int entryCount, int pageSize) {
        if (entryCount <= 0 || pageSize <= 0) return 1;
        return (entryCount + pageSize - 1) / pageSize;
    }

    /**
     * The Bedrock mode dropdown's pre-selected index (§7.5: the current
     * mode is pre-selected and named). Order matches the Java row:
     * 0 = off, 1 = whitelist, 2 = blacklist.
     */
    public static int modeIndex(Mode mode) {
        return switch (mode == null ? Mode.OFF : mode) {
            case OFF -> 0;
            case WHITELIST -> 1;
            case BLACKLIST -> 2;
        };
    }

    public static Mode modeAt(int index) {
        return switch (index) {
            case 1 -> Mode.WHITELIST;
            case 2 -> Mode.BLACKLIST;
            default -> Mode.OFF;
        };
    }
}

// ExcelAutoSort — Iris / Mike, for Wink Reyes Valley

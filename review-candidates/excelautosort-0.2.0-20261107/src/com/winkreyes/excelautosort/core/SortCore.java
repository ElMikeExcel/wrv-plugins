package com.winkreyes.excelautosort.core;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * The sort engine's pure core (SPEC §4): the ordering contract the
 * incumbent AutoSort 0.1.17 shipped, reimplemented from SPEC §1/§4 —
 * registry order, then material key, then item-meta string — plus the
 * stack merging Bukkit's {@code addItem} performed on re-add.
 *
 * The Bukkit layer extracts one {@link StackView} per non-empty stack;
 * everything from there is plain JDK and unit-tested here, including
 * the upstream {@code SortTest} ordering cases ported as data.
 */
public final class SortCore {

    private SortCore() {}

    /**
     * One stack, reduced to what ordering and merging need.
     *
     * @param orderIndex   the material's index in the material registry
     * @param materialKey  the material's resource key (e.g. "dirt")
     * @param metaString   the item meta rendered as a string ("{}" when plain)
     * @param amount       stack size
     * @param maxStack     the material's max stack size
     */
    public record StackView(int orderIndex, String materialKey, String metaString,
                            int amount, int maxStack) {
        /** Two stacks merge iff type and meta agree — Bukkit's similarity rule. */
        public String similarityKey() {
            return materialKey + "\0" + metaString;
        }
    }

    /** The parity comparator (SPEC §4): registry order → key → meta string. */
    public static final Comparator<StackView> COMPARATOR = Comparator
            .comparingInt(StackView::orderIndex)
            .thenComparing(StackView::materialKey)
            .thenComparing(StackView::metaString);

    /**
     * Sort (stable) and merge: consecutive similar stacks are combined
     * into max-stack chunks, exactly as re-adding the sorted stacks to
     * an empty inventory does. The input list is not mutated.
     */
    public static List<StackView> sortAndMerge(List<StackView> input) {
        List<StackView> sorted = new ArrayList<>(input);
        sorted.sort(COMPARATOR);
        List<StackView> out = new ArrayList<>();
        for (StackView s : sorted) {
            if (s.amount() <= 0) continue;
            if (!out.isEmpty()) {
                StackView last = out.get(out.size() - 1);
                if (last.similarityKey().equals(s.similarityKey()) && last.maxStack() > 1) {
                    int remaining = s.amount();
                    // Top up the previous chunk first, then spill into fresh chunks.
                    if (last.amount() < last.maxStack()) {
                        int move = Math.min(last.maxStack() - last.amount(), remaining);
                        out.set(out.size() - 1, new StackView(last.orderIndex(), last.materialKey(),
                                last.metaString(), last.amount() + move, last.maxStack()));
                        remaining -= move;
                    }
                    while (remaining > 0) {
                        int chunk = Math.min(last.maxStack(), remaining);
                        out.add(new StackView(s.orderIndex(), s.materialKey(), s.metaString(),
                                chunk, s.maxStack()));
                        remaining -= chunk;
                    }
                    continue;
                }
            }
            out.add(s);
        }
        return out;
    }
}

// ExcelAutoSort — Iris / Mike, for Wink Reyes Valley

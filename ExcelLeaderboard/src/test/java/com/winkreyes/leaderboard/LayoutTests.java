package com.winkreyes.leaderboard;

import com.winkreyes.leaderboard.paper.BoardPresentation;

import java.util.Arrays;

/** Chest-layout suite for 1.2's fallback menu (Mike's spacing rule,
 * Oct 15, 2026): the main-menu slot constant is exactly the nine spaced
 * slots, no two occupied slots sit side by side horizontally, and the
 * slot <-> category-index mapping the click handler uses round-trips. */
public final class LayoutTests {

    private static int checks = 0;

    private static void eq(Object actual, Object expected, String label) {
        checks++;
        if (!expected.equals(actual)) {
            throw new AssertionError(label + ": expected <" + expected + "> but was <" + actual + ">");
        }
    }

    private static void check(boolean condition, String label) {
        checks++;
        if (!condition) {
            throw new AssertionError(label);
        }
    }

    public static void main(String[] args) {
        int[] slots = BoardPresentation.MAIN_SLOTS;

        eq(slots.length, 9, "nine spaced slots");
        check(Arrays.equals(slots, new int[]{10, 12, 14, 16, 19, 21, 23, 25, 31}),
                "exact slot constant");

        // No two occupied slots adjacent horizontally (same row, |diff| 1).
        for (int i = 0; i < slots.length; i++) {
            for (int j = i + 1; j < slots.length; j++) {
                if (slots[i] / 9 == slots[j] / 9) {
                    check(Math.abs(slots[i] - slots[j]) >= 2,
                            "slots " + slots[i] + " and " + slots[j] + " not adjacent");
                }
            }
        }

        // Round trip: index -> slot -> index.
        for (int i = 0; i < slots.length; i++) {
            eq(BoardPresentation.categoryIndexForSlot(slots[i]), i, "slot " + slots[i] + " maps to index " + i);
            eq(BoardPresentation.slotForCategoryIndex(i), slots[i], "index " + i + " maps to slot " + slots[i]);
        }

        // Slots outside the layout mean no category.
        for (int free : new int[]{0, 11, 13, 17, 26, 30, 53}) {
            eq(BoardPresentation.categoryIndexForSlot(free), -1, "free slot " + free + " unmapped");
        }
        eq(BoardPresentation.slotForCategoryIndex(9), -1, "tenth category has no spaced slot");
        eq(BoardPresentation.slotForCategoryIndex(-1), -1, "negative index has no slot");

        // Everything fits the 54-slot chest, in ascending layout order.
        boolean inRangeAndAscending = true;
        for (int i = 0; i < slots.length; i++) {
            if (slots[i] < 0 || slots[i] >= 54 || (i > 0 && slots[i] <= slots[i - 1])) {
                inRangeAndAscending = false;
            }
        }
        check(inRangeAndAscending, "slots ascending inside the 54-slot chest");

        System.out.println("LayoutTests: " + checks + " assertions passed");
    }
}

package com.winkreyes.excelwandbars.core;

/**
 * 0.3.6: the exit-slot relocation sweep's decisions (pure; the hook-side
 * ExitSweep does the Bukkit work). While the tap bar is up, its exit column
 * is a genuinely empty storage slot, and Bukkit fills hotbar-first — so any
 * programmatic give (another plugin's addItem, a ground pickup, a merchant
 * result, the Halloween helmet swap) can land a foreign item in the bar,
 * where the click/drag exit guards never see it. The item then kills the
 * empty-tap exit on a full page and can make the next open refuse outright.
 * The sweep runs in TapBarHook.tryOpen's tap-bar branch, before the wand
 * opens: foreign items in the rendered pages are moved into the player's
 * stored inventory (Wand.addToStoredInventory; the live inventory when no
 * stored inventory exists yet, which Magic then stores as-is). Item
 * preservation is absolute: whatever cannot be moved stays exactly where
 * it is.
 */
public final class ExitSweepPlan {
    private ExitSweepPlan() {}

    /** A rendered bar slot needs the sweep when it holds an item that is not a spell icon. */
    public static boolean foreign(boolean empty, boolean spellKeyed) {
        return !empty && !spellKeyed;
    }

    /**
     * How much of a stack stays in the bar after one relocation attempt.
     * accepted = the sink reports the whole stack taken (leftover 0).
     * Otherwise the sink may still have taken part of the clone it was
     * handed: cloneAfter is that clone's amount after the call, so the
     * moved part is amount - cloneAfter and never negative, never more
     * than the stack held.
     */
    public static int leftover(int amount, boolean accepted, int cloneAfter) {
        if (accepted) return 0;
        if (cloneAfter < 0) return amount;
        return Math.max(0, Math.min(amount, cloneAfter));
    }
}

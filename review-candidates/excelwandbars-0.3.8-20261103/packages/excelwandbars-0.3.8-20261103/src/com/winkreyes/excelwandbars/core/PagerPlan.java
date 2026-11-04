package com.winkreyes.excelwandbars.core;

/**
 * 0.3.8: the spell inventory's pager buttons (Mike's settled design, Nov 2 2026:
 * "We can keep both" — the buttons AND the wand-click page cycle). Pure (no
 * Bukkit/Magic types), unit-tested on the plain JVM.
 *
 * While the Tap bar spell inventory is open in the player's own inventory
 * screen, the LAST TWO slots of the bottom row of the main grid — Bukkit
 * player-inventory slots {@link #BACK_SLOT} (34) and {@link #FORWARD_SLOT}
 * (35), the bottom-right pair — are pager buttons: back a page, forward a
 * page, with the same wraparound as the wand-click / sneak cycle. The forward
 * button carries the "Page X of Y" state in its lore. The buttons are
 * presentation only: they are hard-protected from the drag-write and Arrange
 * interpreters (the pager hook cancels every click or drag that touches
 * them before those interpreters run, so a button can never be lifted,
 * carried, written or counted as a spell), and they are removed again when
 * the bar closes. The cost is the two grid slots while the bar is up.
 *
 * Buttons show only when the wand actually has more than one page: with one
 * page there is nowhere to flip to.
 */
public final class PagerPlan {
    private PagerPlan() {}

    /** Bottom-right pair of the inventory grid: back is the left of the pair. */
    public static final int BACK_SLOT = 34;
    public static final int FORWARD_SLOT = 35;

    public enum Button { NONE, BACK, FORWARD }

    public static Button buttonAt(int playerInventorySlot) {
        if (playerInventorySlot == BACK_SLOT) return Button.BACK;
        if (playerInventorySlot == FORWARD_SLOT) return Button.FORWARD;
        return Button.NONE;
    }

    public static boolean isButtonSlot(int playerInventorySlot) {
        return buttonAt(playerInventorySlot) != Button.NONE;
    }

    /** Buttons only earn their two slots when there is somewhere to flip to. */
    public static boolean shows(int pages) { return pages >= 2; }

    /** Current page clamped into range (a wand can shrink under the open bar). */
    public static int clamp(int page, int pages) {
        if (pages < 1) return 0;
        if (page < 0) return 0;
        return Math.min(page, pages - 1);
    }

    /** The page a flip lands on, wrapping at both ends exactly like cycleHotbar. */
    public static int target(int page, int pages, int dir) {
        if (pages < 1) return 0;
        int p = clamp(page, pages);
        int step = dir < 0 ? -1 : 1;
        return ((p + step) % pages + pages) % pages;
    }

    /** "Page 2 of 3" — the state line both buttons carry; the forward one leads with it. */
    public static String stateLine(int page, int pages) {
        int p = clamp(page, pages);
        return "Page " + (p + 1) + " of " + Math.max(1, pages);
    }

    public static String backName() { return "◀ Back page"; }
    public static String forwardName() { return "Next page ▶"; }
}

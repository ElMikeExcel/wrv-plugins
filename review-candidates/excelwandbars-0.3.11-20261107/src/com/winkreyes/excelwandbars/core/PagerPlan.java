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

    // ------------------------------------------------------------ 0.3.9: the spell-list axis

    /**
     * 0.3.9: the buttons page TWO things in this screen — the wand's hotbar
     * (Bar) pages (0.3.8) and the spell list's own pages (Mike, Nov 3 2026:
     * the buttons belong on every part of the spellbook, the spell list
     * included — which was the Nov 2 design's original target: Jester's wolf
     * sat on a later LIST page). In "auto" the axis follows the paging the
     * player last used; the config can pin it to one axis.
     */
    public enum Axis { BAR, LIST }

    /** Slots on one spell-list page (the grid above the bar row). */
    public static final int LIST_PAGE_SIZE = 27;

    /** Pages in the spell list for this many parked (not-on-a-bar) spells; always at least one. */
    public static int listPages(int parkedSpells) {
        if (parkedSpells <= 0) return 1;
        return (parkedSpells + LIST_PAGE_SIZE - 1) / LIST_PAGE_SIZE;
    }

    /** "List page 2 of 3" — the state line while the buttons page the spell list. */
    public static String listStateLine(int page, int pages) {
        int p = clamp(page, pages);
        return "List page " + (p + 1) + " of " + Math.max(1, pages);
    }

    /** Buttons render when EITHER axis has somewhere to flip to. */
    public static boolean shows(int barPages, int listPages) {
        return shows(barPages) || shows(listPages);
    }

    /** The axis a flip goes to under "auto": the pinned one wins; a single-page axis never takes it. */
    public static Axis autoAxis(Axis lastUsed, int barPages, int listPages) {
        boolean bar = shows(barPages), list = shows(listPages);
        if (bar && !list) return Axis.BAR;
        if (list && !bar) return Axis.LIST;
        return lastUsed == null ? Axis.BAR : lastUsed;
    }

    // ------------------------------------------------------------ 0.3.9: displacing a list icon

    /**
     * What may happen when a pager button wants a grid slot. FREE: the slot
     * is empty or already ours — write the button. DISPLACE: the slot holds
     * a spell icon of the open list page; the icon is a presentation of a
     * spell Magic keeps in its spell map (a save never deletes a map entry
     * it did not read), so the button may cover it for as long as this page
     * is on screen, provided the icon is restored when the button leaves
     * and the wand's paint is rebuilt from the map afterward. BLOCKED: the
     * slot holds anything else (a brush icon, a foreign item) — never
     * overwrite it; the button simply does not render (0.3.8 behavior).
     */
    public enum SlotFate { FREE, DISPLACE, BLOCKED }

    public static SlotFate classify(boolean empty, boolean ours, boolean spellIcon) {
        if (empty || ours) return SlotFate.FREE;
        return spellIcon ? SlotFate.DISPLACE : SlotFate.BLOCKED;
    }
}

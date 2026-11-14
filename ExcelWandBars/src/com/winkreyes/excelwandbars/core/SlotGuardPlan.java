package com.winkreyes.excelwandbars.core;

/**
 * 0.3.12: the wand-bar slot guard's decisions (pure; the hook-side work
 * lives in TapBarHook.protectExitClick / protectExitDrag / onPickup and
 * in PresentedSweep). Commissioned after Mike's Nov 7, 2026 fight intake:
 * arrows occupied a protected wand slot mid-combat, while the bar was
 * open. The 0.3.6 exit sweep (ExitSweepPlan / ExitSweep) already proved
 * the entry mechanism — Bukkit fills hotbar-first, so any programmatic
 * give, ground pickup or merchant result can park a foreign item in the
 * bar — but it only ran when the bar OPENED (tryOpen) or at the join
 * guard's final check. A bar that stays open through a fight had no
 * guard at all. This plan owns the per-path verdicts for a bar that is
 * open RIGHT NOW; the audit table behind them is in BUILD-RECORD.md.
 *
 * "Protected" = the presented inventory (player-inventory slots 0–35)
 * while a Tap bar is open on a rank wand (TapBarHook.reservesExit): the
 * hotbar row is the current wand page, the grid is the spell list, and
 * every item legitimately there is a spell icon, a pager button, or the
 * empty exit slot. A FOREIGN item — a real item that is none of those —
 * has no business entering a presented slot: placed there it displaces
 * an icon, kills the empty-tap exit, and is baked into the saved page
 * by the next Magic save (a hotbar flip, a close), exactly how the menu
 * compass got baked in before 0.3.11.
 *
 * What this plan does NOT cover, by design: the Geyser drag-and-drop
 * occupied-slot swap (investigation H1) is a protocol-layer defect with
 * its own gated debug item; nothing here changes drag-write, Arrange,
 * or any spell-icon gesture — icon movements are the interpreters'
 * business and every verdict below is computed only for FOREIGN items.
 */
public final class SlotGuardPlan {
    private SlotGuardPlan() {}

    /**
     * A presented item is foreign when it is a real item that is not a
     * spell icon, not one of the pager's own buttons, and not a wand.
     * (Same predicate family as ExitSweepPlan.foreign, extended with the
     * two presentation-owned exclusions a live screen adds.)
     */
    public static boolean foreign(boolean empty, boolean spellKeyed, boolean pagerButton, boolean wandItem) {
        return !empty && !spellKeyed && !pagerButton && !wandItem;
    }

    /** The click families the hook distinguishes (mapped from Bukkit's InventoryAction). */
    public enum ClickKind {
        /** PLACE_ALL / PLACE_ONE / PLACE_SOME: the cursor's stack lands in the clicked slot. */
        PLACE,
        /** SWAP_WITH_CURSOR: cursor and clicked slot exchange contents. */
        SWAP_CURSOR,
        /** HOTBAR_SWAP / HOTBAR_MOVE_AND_READD: clicked slot and a hotbar slot exchange contents. */
        HOTBAR_SWAP,
        /** MOVE_TO_OTHER_INVENTORY (shift-click): the clicked stack moves to the other part of the view. */
        SHIFT_MOVE,
        /** Everything else (pickups, drops, collects, clones, no-ops): never places a foreign item into a slot by itself. */
        OTHER
    }

    /**
     * Does this click place a FOREIGN item into a presented slot?
     *
     * @param guardActive      the slot guard's context holds (Tap bar open on a rank wand)
     * @param kind             the click family
     * @param clickedPresented the clicked slot is in the presented inventory
     * @param clickedForeign   the clicked slot currently holds a foreign item
     * @param otherForeign     the incoming side is foreign — the cursor stack for
     *                         PLACE / SWAP_CURSOR, the hotbar-button slot's item for HOTBAR_SWAP
     * @param otherPresented   the incoming side is itself a presented slot (HOTBAR_SWAP
     *                         whose clicked side sits outside the presentation: the
     *                         hotbar slot it would receive the clicked item into is presented)
     *
     * PLACE / SWAP_CURSOR: blocked when the incoming foreign stack would
     * land in a presented slot. HOTBAR_SWAP: blocked in either direction
     * — a foreign hotbar item entering a presented clicked slot, or a
     * foreign clicked item entering the presented hotbar slot. (A swap
     * whose hotbar side holds a spell ICON is the interpreters' case —
     * they already swallow it; otherForeign is false for an icon, so this
     * plan never double-decides it.) SHIFT_MOVE: in the bar's own screen
     * the "other inventory" is the other half of the same presentation,
     * so a foreign stack shift-clicked from a presented slot lands in a
     * presented slot — blocked; the item itself is relocated by the
     * sweep, never by vanilla. OTHER never places, so never blocks here
     * (pickups and collects only REMOVE from slots; the exit invariant
     * and the icon interpreters keep their own rules).
     */
    public static boolean blocksClick(boolean guardActive, ClickKind kind,
                                      boolean clickedPresented, boolean clickedForeign,
                                      boolean otherForeign, boolean otherPresented) {
        if (!guardActive || kind == null) return false;
        switch (kind) {
            case PLACE, SWAP_CURSOR:
                return clickedPresented && otherForeign;
            case HOTBAR_SWAP:
                return (clickedPresented && otherForeign) || (otherPresented && clickedForeign);
            case SHIFT_MOVE:
                return clickedPresented && clickedForeign;
            default:
                return false;
        }
    }

    /**
     * Does this drag place a FOREIGN item into a presented slot? A drag
     * whose cursor stack is foreign and which touches any presented slot
     * would paint that stack across the touched slots — blocked whole
     * (a drag is one gesture; partial application is not a verdict).
     * Drags carrying a spell icon are the drag-write / Arrange
     * interpreters' case and are never blocked here (cursorForeign is
     * false for an icon). The Geyser occupied-slot swap (H1) does not
     * travel as a Bukkit drag of a foreign cursor and is untouched.
     */
    public static boolean blocksDrag(boolean guardActive, boolean cursorForeign, boolean touchesPresented) {
        return guardActive && cursorForeign && touchesPresented;
    }

    /**
     * A ground pickup while the guard is active: the pickup itself is
     * never cancelled (the player keeps their loot), but the stack lands
     * in the presented inventory hotbar-first — the exit slot is the
     * first empty slot by design — so a presented sweep is scheduled to
     * relocate it into the wand's stored inventory, where the player's
     * real items wait. This is the Nov 7 fight-intake path: arrows
     * picked up mid-combat with the bar open.
     */
    public static boolean pickupSweeps(boolean guardActive) {
        return guardActive;
    }

    /**
     * Before any WandBars-driven Magic save of the open inventory (a
     * hotbar page flip, a spell-list page flip, the sneak flip), the
     * presented foreign items are swept out first — the same rule the
     * pager buttons (0.3.9) and the cooldown name tags (0.3.10) already
     * follow: a save must only ever see the presentation's own items,
     * never a foreign stack it would bake into the saved page.
     */
    public static boolean flipSweeps(boolean guardActive) {
        return guardActive;
    }
}

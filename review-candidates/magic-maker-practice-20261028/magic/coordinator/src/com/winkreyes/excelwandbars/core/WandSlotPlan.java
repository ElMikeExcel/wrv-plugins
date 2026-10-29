package com.winkreyes.excelwandbars.core;

/**
 * 0.2.7: where the rank wand lives (the last hotbar slot by default) and when it may be
 * moved there. Pure logic, no Bukkit. A move is always ONE swap of two hotbar slots in
 * the same tick: the wand goes to the target slot and whatever was in the target slot
 * (an item, a stack, or nothing) goes to the slot the wand came from. Nothing is created,
 * dropped or deleted, so a move can never dupe or lose an item.
 */
public final class WandSlotPlan {
    private WandSlotPlan() {}

    /** Config "slot" is 1-9 like the hotbar keys; anything else falls back to 9. Returns the 0-8 index. */
    public static int targetIndex(int configuredSlot) {
        if (configuredSlot < 1 || configuredSlot > 9) return 8;
        return configuredSlot - 1;
    }

    /** What the player is doing right now, as seen by the server. */
    public static final class State {
        public boolean barOpen;          // Magic's spell bar is up (real hotbar stored by Magic)
        public boolean otherScreenOpen;  // a chest, furnace, villager... (top inventory not the player's own)
        public boolean cursorHasItem;    // an item is on the mouse cursor (inventory screen)
        public boolean deadOrSpectator;  // dead, spectator or creative
        public boolean skipWorld;        // Magic is off here
        public boolean recentInventoryClick; // clicked an inventory slot in the last few seconds
        public int wandSlot = -1;        // hotbar index of the rank wand, -1 = none in the hotbar
        public int heldSlot;             // selected hotbar index
        public boolean targetIsRankWand; // the target slot already holds a rank wand
    }

    public enum Action { NONE, SWAP }

    /** Why a sweep did or did not move the wand (for /wandbars wandslot and the counters). */
    public static String why(State s, int target) {
        if (s.wandSlot < 0) return "no wand in hotbar";
        if (s.wandSlot == target) return "already there";
        if (s.targetIsRankWand) return "target holds a wand";
        if (s.barOpen) return "bar open";
        if (s.otherScreenOpen) return "screen open";
        if (s.cursorHasItem) return "cursor item";
        if (s.deadOrSpectator) return "dead/spectator/creative";
        if (s.skipWorld) return "skip world";
        if (s.recentInventoryClick) return "inventory click";
        if (s.heldSlot == target && s.heldSlot != s.wandSlot) return "holding the target slot";
        return "move";
    }

    /**
     * The timed sweep (and join / respawn / after the bar closes). Moves only when it cannot
     * surprise the player: never while the bar is up (Magic holds the real hotbar then), never
     * with another screen open or an item on the cursor, never right after an inventory click,
     * and never while the player holds the item in the target slot (their hand would change).
     */
    public static Action sweep(State s, int target) {
        return "move".equals(why(s, target)) ? Action.SWAP : Action.NONE;
    }

    /**
     * The player picks the wand in another hotbar slot (bar down). Same checks as the sweep,
     * except the selection itself is the trigger: the item in the target slot goes to the slot
     * they picked and the selection moves to the target, so they hold the wand either way.
     */
    public static Action onPick(State s, int pickedSlot, int target) {
        if (pickedSlot != s.wandSlot || pickedSlot == target) return Action.NONE;
        if (s.targetIsRankWand || s.barOpen || s.otherScreenOpen || s.cursorHasItem || s.deadOrSpectator || s.skipWorld) return Action.NONE;
        return Action.SWAP;
    }

    /** Swap two slots of a 9-slot hotbar model (used by the tests and to describe a move). */
    public static <T> void swap(T[] hotbar, int a, int b) {
        T t = hotbar[a]; hotbar[a] = hotbar[b]; hotbar[b] = t;
    }

    /**
     * 0.2.7 Tap bar: a Bedrock tap on the hotbar slot that is ALREADY selected (the wand).
     * Geyser does not pass that on to the server, so ExcelWandBars reads it from Geyser.
     * A phone may also re-send the selected slot right after the server changes it (bar opened,
     * wand moved, spell cast), so anything within guardMs of such a change is not a tap.
     */
    public static boolean isWandTap(boolean tapBarStyle, boolean wandHeld, int tappedSlot, int heldSlot,
                                    long now, long lastServerChange, long guardMs) {
        if (!tapBarStyle || !wandHeld) return false;
        if (tappedSlot != heldSlot) return false;
        return now - lastServerChange >= guardMs;
    }
}


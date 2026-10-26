package com.winkreyes.excelattunement.core;

import java.util.function.IntPredicate;

/**
 * 0.1.7 (Mike, Oct 26, 2026, after a player died unable to scroll past his wand): the stance
 * lock may NEVER stop a player from reaching a hotbar slot.
 *
 * Scrolling (Java mouse wheel, Bedrock controller bumpers) moves ONE slot at a
 * time. In 0.1.4 to 0.1.6 a scroll onto a locked item (a wand in Fighter
 * stance, a weapon in Caster stance) was cancelled, so the player bounced off
 * that slot and could never scroll to the slots behind it.
 *
 * Now a one-slot move onto a locked item hops OVER it, in the same direction,
 * to the next slot that is not locked (wrapping 8 -> 0 like the vanilla
 * hotbar). The locked item itself still never lands in the main hand.
 * A direct jump (number key, Bedrock touch tap) onto a locked slot is still
 * refused: the player stays where they were and can pick any other slot.
 *
 * Pure decision, unit-tested; no Bukkit types.
 */
public final class HotbarSkip {
    private HotbarSkip() {}

    /** No hop: refuse the move (old behavior). */
    public static final int REFUSE = -1;

    /** +1 for a one-slot move right, -1 for a one-slot move left (with wrap), 0 for a jump. */
    public static int direction(int oldSlot, int newSlot) {
        if (!valid(oldSlot) || !valid(newSlot) || oldSlot == newSlot) return 0;
        int diff = Math.floorMod(newSlot - oldSlot, 9);
        if (diff == 1) return 1;
        if (diff == 8) return -1;
        return 0;
    }

    /**
     * Where the player should end up when the move oldSlot -> newSlot lands on
     * a locked item. Returns the first unlocked slot past newSlot in the scroll
     * direction, or REFUSE (a jump, or every other slot locked).
     * Never returns oldSlot or a locked slot.
     */
    public static int target(int oldSlot, int newSlot, IntPredicate locked) {
        int d = direction(oldSlot, newSlot);
        if (d == 0) return REFUSE;
        int s = newSlot;
        for (int i = 0; i < 8; i++) {
            s = Math.floorMod(s + d, 9);
            if (s == oldSlot) return REFUSE;
            if (!locked.test(s)) return s;
        }
        return REFUSE;
    }

    private static boolean valid(int s) { return s >= 0 && s <= 8; }
}

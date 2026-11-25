package com.winkreyes.excelmagicgate;

/**
 * Per-mage floor-reconciliation decision (pure logic, no Bukkit deps).
 *
 * The Bukkit hook (MagicFloorHook) discovers the facts — where the
 * mage's bound wand physically is, and what rank it currently reads —
 * and this class decides the outcome, so every branch is unit-tested
 * on a plain JVM:
 *
 * - ACTIVE_OR_INVENTORY: a live wand item is in hand / inventory.
 *   It can be stepped in place via Magic's upgrade(). (A wand in the
 *   ender chest is deliberately NOT "carried" for stepping — Magic's
 *   upgrade() lands on the mage's active properties and an
 *   ender-chest wand cannot be activated where it sits; it defers to
 *   activation like the pouch case. See MagicFloorHook.itemIsCarried.)
 * - PARKED_IN_POUCH: PouchQuery (ExcelPouch, Oct 18 spec) reports the
 *   player's pouch holds a magic_wand, and no live wand item is
 *   accessible. PouchQuery is query-only by contract — it cannot hand
 *   back or mutate the serialized wand — so the outcome is an
 *   EXPLICIT deferral to wand activation (the activation + sweep legs
 *   complete the step when the wand is a live item again).
 * - NO_LIVE_WAND: the mage has no accessible wand and the pouch does
 *   not report one (wand in a chest somewhere, or never issued).
 *   Also a deferral to activation — there is nothing safe to mutate.
 * - UNKNOWN_RANK: the wand's path key is not one of the four WRV
 *   ranks. Never guessed at, never mutated.
 */
public final class FloorPlan {

    public enum WandAccess {ACTIVE_OR_INVENTORY, PARKED_IN_POUCH, NO_LIVE_WAND}

    public enum Outcome {
        STEP,               // step up to the floor now
        ALREADY_AT_FLOOR,   // at or above the floor: no-op
        DEFERRED_PARKED_POUCH,
        DEFERRED_NO_LIVE_WAND,
        SKIP_UNKNOWN_RANK
    }

    public record Decision(Outcome outcome, int steps) {}

    private FloorPlan() {}

    public static Decision decide(WandAccess access, String currentRankKey,
                                  RankFloor.Rank floor) {
        if (floor == null) {
            return new Decision(Outcome.SKIP_UNKNOWN_RANK, 0);
        }
        RankFloor.Rank current = RankFloor.fromKey(currentRankKey);
        if (access == WandAccess.PARKED_IN_POUCH) {
            // Even a pouch report cannot be rank-read (the wand is
            // serialized bytes); defer regardless of rank knowledge.
            return new Decision(Outcome.DEFERRED_PARKED_POUCH, 0);
        }
        if (access == WandAccess.NO_LIVE_WAND) {
            return new Decision(Outcome.DEFERRED_NO_LIVE_WAND, 0);
        }
        if (current == null) {
            return new Decision(Outcome.SKIP_UNKNOWN_RANK, 0);
        }
        int steps = RankFloor.stepsToFloor(current, floor).size();
        if (steps == 0) {
            return new Decision(Outcome.ALREADY_AT_FLOOR, 0);
        }
        return new Decision(Outcome.STEP, steps);
    }
}

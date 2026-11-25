package com.winkreyes.excelmagicgate;

/**
 * Per-mage ceiling/earned reconciliation decision (pure logic, no
 * Bukkit deps) — the 0.3.0 generalization of FloorPlan.
 *
 * The reconciler's target is no longer the stage floor; it is
 *   target = min(earnedRank(creditedTotal), ceilingRank(appliedStage))
 * (DESIGN.md §1). The Bukkit hook (MagicFloorHook) discovers the
 * facts — where the bound wand physically is, what rank it currently
 * reads, and the player's credited total — and this class decides
 * the outcome, so the whole (current × earned × ceiling × access)
 * matrix is unit-tested on a plain JVM, exactly as FloorPlan's was.
 *
 * Outcomes:
 * - STEP_UP(n): current &lt; target; step up n ranks via Magic's own
 *   ProgressionPath.upgrade() (the 0.2.0 machinery, retargeted).
 * - DEMOTE(n): current &gt; target and demotion is enabled; bring the
 *   wand DOWN n ranks via the reflective property seam (DESIGN.md §1
 *   Option A — rig-gated; see MagicFloorHook.demote).
 * - DEMOTE_HELD: current &gt; target but demotion is disabled in
 *   config. The rank is left untouched and the state is NAMED, never
 *   silently treated as at-target (DESIGN.md §11 Q2: shipping with
 *   demotion held is Mike's explicit call, never a silent fallback).
 * - AT_TARGET: current == target; no-op.
 * - DEFERRED_PARKED_POUCH / DEFERRED_NO_LIVE_WAND: the wand is not a
 *   live item; demotion and step-up alike defer to wand activation —
 *   a serialized wand is never edited (same rule as 0.2.0).
 * - SKIP_UNKNOWN_RANK: the wand's path key is not one of the four
 *   WRV ranks. Never guessed at, never mutated.
 */
public final class CeilingPlan {

    public enum Outcome {
        STEP_UP,
        DEMOTE,
        DEMOTE_HELD,
        AT_TARGET,
        DEFERRED_PARKED_POUCH,
        DEFERRED_NO_LIVE_WAND,
        SKIP_UNKNOWN_RANK
    }

    /** steps = number of rank steps to move (up or down); 0 otherwise. */
    public record Decision(Outcome outcome, int steps, RankFloor.Rank target) {}

    private CeilingPlan() {}

    public static Decision decide(FloorPlan.WandAccess access, String currentRankKey,
                                  RankFloor.Rank earned, RankFloor.Rank ceiling,
                                  boolean demotionEnabled) {
        if (earned == null || ceiling == null) {
            return new Decision(Outcome.SKIP_UNKNOWN_RANK, 0, null);
        }
        RankFloor.Rank target = earned.ordinal() <= ceiling.ordinal() ? earned : ceiling;
        if (access == FloorPlan.WandAccess.PARKED_IN_POUCH) {
            // A pouch report cannot be rank-read or rank-written (the
            // wand is serialized bytes); defer regardless of direction.
            return new Decision(Outcome.DEFERRED_PARKED_POUCH, 0, target);
        }
        if (access == FloorPlan.WandAccess.NO_LIVE_WAND) {
            return new Decision(Outcome.DEFERRED_NO_LIVE_WAND, 0, target);
        }
        RankFloor.Rank current = RankFloor.fromKey(currentRankKey);
        if (current == null) {
            return new Decision(Outcome.SKIP_UNKNOWN_RANK, 0, target);
        }
        int delta = target.ordinal() - current.ordinal();
        if (delta > 0) {
            return new Decision(Outcome.STEP_UP, delta, target);
        }
        if (delta < 0) {
            return demotionEnabled
                    ? new Decision(Outcome.DEMOTE, -delta, target)
                    : new Decision(Outcome.DEMOTE_HELD, 0, target);
        }
        return new Decision(Outcome.AT_TARGET, 0, target);
    }

    /** The rank key one step DOWN from the given rank, or null at beginner. */
    public static RankFloor.Rank oneDown(RankFloor.Rank rank) {
        if (rank == null || rank.ordinal() == 0) {
            return null;
        }
        return RankFloor.Rank.values()[rank.ordinal() - 1];
    }
}

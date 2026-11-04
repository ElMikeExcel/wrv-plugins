package com.winkreyes.exceleliteladder.core;

/** What the watch tick should do. Pure decision, unit tested. Up only, never down. */
public final class StepPlan {
    private StepPlan() {
    }

    public enum Action {
        DISABLED,
        NO_SEAL_SOURCE,
        NO_BASELINE,
        UP_TO_DATE,
        WARN_DOWNGRADE,
        WAIT_FOR_EMPTY,
        APPLY
    }

    public record Decision(Action action, int target) {
    }

    /**
     * @param applied applied step, 0 = no verified baseline yet (live is not on the ladder)
     */
    public static Decision decide(boolean enabled, Integer seals, int applied, int playersOnline,
                                  boolean allowWithPlayers) {
        if (!enabled) {
            return new Decision(Action.DISABLED, applied);
        }
        if (seals == null) {
            return new Decision(Action.NO_SEAL_SOURCE, applied);
        }
        if (applied < LadderTable.MIN_STEP) {
            return new Decision(Action.NO_BASELINE, applied);
        }
        int target = LadderTable.clampSeals(seals);
        if (target < applied) {
            return new Decision(Action.WARN_DOWNGRADE, applied);
        }
        if (target == applied) {
            return new Decision(Action.UP_TO_DATE, applied);
        }
        if (playersOnline > 0 && !allowWithPlayers) {
            return new Decision(Action.WAIT_FOR_EMPTY, target);
        }
        return new Decision(Action.APPLY, target);
    }

    /**
     * Drift guard: the live file may only be moved to {@code target} when it is exactly the
     * applied step (normal case) or already the target (re-run after a crash).
     * Returns null when allowed, else the refusal reason.
     */
    public static String driftCheck(java.util.Map<String, Double> live, int applied, int target) {
        LadderStep from = LadderTable.step(applied);
        LadderStep to = LadderTable.step(target);
        if (LadderTable.matches(live, from) || LadderTable.matches(live, to)) {
            return null;
        }
        return LadderTable.describeOffLadder(live, from);
    }
}

package com.winkreyes.excelmagicgate;

/**
 * Pure stage-eligibility logic for ExcelMagicGate (WRV-070).
 *
 * No Bukkit dependencies, so the whole decision table is unit-testable on
 * a plain JVM (see tests/com/winkreyes/excelmagicgate/ExcelMagicGateTest.java).
 *
 * Rules:
 * - Stages are 0..5. Stage N is eligible when N guild seals have fallen
 *   (ExcelGuildGate's broken-seal count). Packets are cumulative, so the
 *   bridge always applies straight to the highest eligible stage.
 * - The bridge only ever moves UP. If the eligible count ever drops below
 *   the applied stage (a /guildgate revoke), the decision is
 *   WARN_DOWNGRADE: log it, tell the operators, change nothing.
 * - Master switch off (config enabled=false) or an unreadable seal source
 *   both mean "do nothing" - the two cases stay distinguishable for
 *   /magicgate status.
 */
public final class StagePlan {

    public static final int MAX_STAGE = 5;

    public enum Action {NONE, APPLY, WARN_DOWNGRADE, UNAVAILABLE}

    /** targetStage is only meaningful for APPLY (the stage to apply). */
    public record Decision(Action action, int targetStage) {}

    private StagePlan() {}

    public static int clampStage(int stage) {
        return Math.max(0, Math.min(MAX_STAGE, stage));
    }

    public static Decision decide(boolean enabled, boolean sealSourceAvailable,
                                  int eligibleStage, int appliedStage) {
        if (!enabled) {
            return new Decision(Action.NONE, clampStage(appliedStage));
        }
        if (!sealSourceAvailable) {
            return new Decision(Action.UNAVAILABLE, clampStage(appliedStage));
        }
        int eligible = clampStage(eligibleStage);
        int applied = clampStage(appliedStage);
        if (eligible > applied) {
            return new Decision(Action.APPLY, eligible);
        }
        if (eligible < applied) {
            return new Decision(Action.WARN_DOWNGRADE, applied);
        }
        return new Decision(Action.NONE, applied);
    }
}

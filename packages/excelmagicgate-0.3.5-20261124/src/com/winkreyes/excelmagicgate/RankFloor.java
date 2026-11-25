package com.winkreyes.excelmagicgate;

import java.util.List;

/**
 * The stage-floor rank table for ExcelMagicGate 0.2.0 (parent-backlog
 * Item ㉝-b; brief: ~/workspace/magicgate-floor-build/BUILD-BRIEF.md).
 *
 * Pure logic, no Bukkit deps. The mapping was FIXED IN THE BRIEF
 * BEFORE CODING:
 *
 *   floorRankIndex(stage) = clamp(stage - 1, 0, 3)
 *
 *   stage 0 -> beginner (50/5,  hotbar 1)
 *   stage 1 -> beginner (50/5,  hotbar 1)   [stage-1 earn stays earned]
 *   stage 2 -> student  (100/15, hotbar 2)
 *   stage 3 -> apprentice (150/25, hotbar 2)
 *   stage 4 -> master   (200/35, hotbar 3)
 *   stage 5 -> master   (200/35, hotbar 3)  [SATURATES: no rank above
 *       master exists, so the stage-5 band growth (student 23 -> 30)
 *       can never re-arm the trap this class exists to disarm]
 *
 * Mana pool / regen ride the mage's path RANK in Magic (investigation
 * of record: spell-audit/MANA-POOL-STAGE-INVESTIGATION.md). The floor
 * only ever steps a mage UP to the stage's frame; learning remains
 * the content ladder above the floor.
 */
public final class RankFloor {

    /** The four WRV path ranks, in progression order. */
    public enum Rank {
        BEGINNER("beginner", 50, 5, 1),
        STUDENT("student", 100, 15, 2),
        APPRENTICE("apprentice", 150, 25, 2),
        MASTER("master", 200, 35, 3);

        private final String key;
        private final int manaMax;
        private final int manaRegeneration;
        private final int hotbarCount;

        Rank(String key, int manaMax, int manaRegeneration, int hotbarCount) {
            this.key = key;
            this.manaMax = manaMax;
            this.manaRegeneration = manaRegeneration;
            this.hotbarCount = hotbarCount;
        }

        public String key() { return key; }
        public int manaMax() { return manaMax; }
        public int manaRegeneration() { return manaRegeneration; }
        public int hotbarCount() { return hotbarCount; }

        /** The next rank up, or null at master. */
        public Rank next() {
            int i = ordinal() + 1;
            return i < values().length ? values()[i] : null;
        }
    }

    private RankFloor() {}

    /** Rank for a paths.yml / wand path key, or null when unknown
     *  (unknown keys are never guessed at — callers skip them). */
    public static Rank fromKey(String key) {
        if (key == null) {
            return null;
        }
        for (Rank r : Rank.values()) {
            if (r.key.equals(key.trim().toLowerCase(java.util.Locale.ROOT))) {
                return r;
            }
        }
        return null;
    }

    /** The floor rank for an applied Magic stage (0..5, clamped). */
    public static Rank floorForStage(int stage) {
        int s = StagePlan.clampStage(stage);
        int idx = Math.max(0, Math.min(Rank.values().length - 1, s - 1));
        return Rank.values()[idx];
    }

    /**
     * The ordered rank keys to step THROUGH to reach the floor from
     * the current rank (exclusive of current, inclusive of floor).
     * Empty when already at or above the floor — the floor never
     * steps anyone down.
     */
    public static List<String> stepsToFloor(Rank current, Rank floor) {
        return stepsTo(current, floor);
    }

    /**
     * The ordered rank keys to step UP through to reach an arbitrary
     * target rank (exclusive of current, inclusive of target). Empty
     * when already at or above the target. Added in 0.3.0: the
     * reconciler's step-up destination is now min(earned, ceiling),
     * which is the stage floor only by coincidence.
     */
    public static List<String> stepsTo(Rank current, Rank target) {
        if (current == null || target == null || current.ordinal() >= target.ordinal()) {
            return List.of();
        }
        java.util.ArrayList<String> out = new java.util.ArrayList<>();
        Rank r = current;
        while (r.ordinal() < target.ordinal()) {
            r = r.next();
            out.add(r.key());
        }
        return List.copyOf(out);
    }

    /** True when the observed frame matches the rank's frame exactly
     *  (the post-step verification the reconciler performs against
     *  the live wand's own numbers). */
    public static boolean frameMatches(Rank rank, int manaMax, int manaRegeneration) {
        return rank != null && manaMax == rank.manaMax() && manaRegeneration == rank.manaRegeneration();
    }
}

package com.winkreyes.excelmagicgate;

/**
 * The earned-rank table for ExcelMagicGate 0.3.0 (Magic Rank earning
 * system; spec of record: parent-backlog "MAGIC RANK-EARNING SYSTEM —
 * SPEC APPROVED", Mike, Nov 9, 2026; design of record:
 * ~/workspace/magic-rank-packet-20261110/DESIGN.md §1/§7).
 *
 * Pure logic, no Bukkit deps. Ranks are earned by TOTAL CREDITED CAST
 * COUNT ONLY — the thresholds are LOCKED and are not re-litigated:
 *
 *   Student    100 credited casts
 *   Apprentice 400 credited casts
 *   Master   2,000 credited casts
 *
 * Seals set the CEILING a player may hold (the RankFloor stage table,
 * reused verbatim as the ceiling table); the rank a player actually
 * holds is min(earned, ceiling). Thresholds arrive as parameters so
 * the rig can shorten them through config without touching the locked
 * defaults, and so every boundary is unit-testable on a plain JVM.
 */
public final class EarnedRank {

    /** Locked default thresholds (credited casts). */
    public static final long STUDENT_AT = 100;
    public static final long APPRENTICE_AT = 400;
    public static final long MASTER_AT = 2000;

    /** The three thresholds, in rank order. */
    public record Thresholds(long studentAt, long apprenticeAt, long masterAt) {
        public Thresholds {
            if (studentAt < 0 || apprenticeAt < studentAt || masterAt < apprenticeAt) {
                throw new IllegalArgumentException(
                        "thresholds must be non-negative and non-decreasing");
            }
        }

        public static Thresholds locked() {
            return new Thresholds(STUDENT_AT, APPRENTICE_AT, MASTER_AT);
        }

        /** The credited total at which a rank is earned (beginner: 0). */
        public long at(RankFloor.Rank rank) {
            return switch (rank) {
                case BEGINNER -> 0;
                case STUDENT -> studentAt;
                case APPRENTICE -> apprenticeAt;
                case MASTER -> masterAt;
            };
        }
    }

    private EarnedRank() {}

    /** The rank a credited total has earned, ignoring any ceiling. */
    public static RankFloor.Rank rankFor(long credited, Thresholds t) {
        if (credited >= t.masterAt()) {
            return RankFloor.Rank.MASTER;
        }
        if (credited >= t.apprenticeAt()) {
            return RankFloor.Rank.APPRENTICE;
        }
        if (credited >= t.studentAt()) {
            return RankFloor.Rank.STUDENT;
        }
        return RankFloor.Rank.BEGINNER;
    }

    /** The rank a player may HOLD: earned, capped by the seal ceiling. */
    public static RankFloor.Rank targetFor(long credited, RankFloor.Rank ceiling, Thresholds t) {
        RankFloor.Rank earned = rankFor(credited, t);
        if (ceiling == null || earned.ordinal() <= ceiling.ordinal()) {
            return earned;
        }
        return ceiling;
    }

    /** The next rank above the earned rank, or null at master. */
    public static RankFloor.Rank nextRank(long credited, Thresholds t) {
        return rankFor(credited, t).next();
    }

    /** Credited casts still needed to reach the next rank (0 at master). */
    public static long remainingToNext(long credited, Thresholds t) {
        RankFloor.Rank next = nextRank(credited, t);
        if (next == null) {
            return 0;
        }
        return Math.max(0, t.at(next) - credited);
    }
}

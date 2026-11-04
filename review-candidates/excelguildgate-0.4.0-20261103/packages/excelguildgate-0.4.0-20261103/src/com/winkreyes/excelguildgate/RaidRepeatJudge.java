package com.winkreyes.excelguildgate;

/**
 * ExcelGuildGate 0.4.0 (RAID-COMPANION-SPEC section 1.1): the raid
 * repeat judge. Pure and Bukkit-free, in the house pattern of
 * RaidWinnerCheck; the Bukkit adapter reads the seal state, the
 * winner/omen floors and the repeat window, and this class alone
 * decides what a finished raid MEANS.
 *
 * The judge is the whole change to the 0.3.4 shape: 0.3.4 returns
 * early when the raid seal already stands, and everything below is
 * what happens instead of that return. The pre-break path is
 * untouched - an unbroken seal always takes the FIRST_BREAK_PATH
 * and the 0.3.4 floors decide it exactly as before.
 *
 * Floor discipline on the repeat path (Mike's words: repeats are
 * "redoing the more difficult seals"): the winner floor and the
 * omen floor apply to repeats unchanged, and the omen floor is
 * never weaker than the vanilla cap the first break demanded -
 * the adapter decides the floors with {@link #requiredOmen(int)}
 * so a config that lowered the first-break floor cannot lower
 * the repeat floor with it.
 *
 * Statelessness: REPEAT_NO_WINNER and REPEAT_OMEN_LOW start no
 * window, consume nothing, and record nothing - the exact
 * 0.3.4 denial discipline carried onto the repeat layer.
 */
public final class RaidRepeatJudge {

    /** What one finished raid means, on both the old and new paths. */
    public enum Outcome {
        /** The seal is not broken yet: the 0.3.4 path decides (it may still deny on the floors). */
        FIRST_BREAK_PATH,
        /** A repeat clear inside every rule: the repeat layer pays. */
        REPEAT_CREDIT,
        /** Floors passed, but the 24h repeat window from the last credit is still running. */
        DENY_REPEAT_WINDOW,
        /** A finished raid over a broken seal, but nobody stood among its winners. */
        REPEAT_NO_WINNER,
        /** A real winner, but the omen behind the repeat was below the repeat floor. */
        REPEAT_OMEN_LOW
    }

    private RaidRepeatJudge() {
    }

    /**
     * The omen floor a REPEAT is judged against: never weaker than
     * {@link RaidWinnerCheck#REQUIRED_OMEN_LEVEL} (the vanilla cap,
     * what the first break demanded on the shipped defaults),
     * whatever the configured first-break floor says.
     */
    public static int requiredOmen(int configuredFloor) {
        return Math.max(RaidWinnerCheck.REQUIRED_OMEN_LEVEL, configuredFloor);
    }

    /**
     * Judge one finished raid.
     *
     * @param sealBroken    GuildGate's live word: the raid seal already stands
     * @param floors        RaidWinnerCheck's decision for THIS raid, judged by
     *                      the adapter against the floor that applies to the
     *                      path (the configured floor pre-break, the
     *                      {@link #requiredOmen(int)} floor on a repeat)
     * @param windowActive  the repeat window from the last credit (break or
     *                      repeat) is still running
     * @param repeatEnabled raid.repeat.enabled; false restores the 0.3.4
     *                      early return exactly (broken seal = nothing to decide)
     */
    public static Outcome judge(boolean sealBroken, RaidWinnerCheck.Decision floors,
                                boolean windowActive, boolean repeatEnabled) {
        if (!sealBroken) {
            return Outcome.FIRST_BREAK_PATH;
        }
        if (!repeatEnabled) {
            return Outcome.FIRST_BREAK_PATH;
        }
        if (floors == null) {
            // An undecided raid can never pay: read it as no winner,
            // the fail-closed denial, never as a credit.
            return Outcome.REPEAT_NO_WINNER;
        }
        return switch (floors) {
            case NO_PLAYER_WINNER -> Outcome.REPEAT_NO_WINNER;
            case OMEN_TOO_LOW -> Outcome.REPEAT_OMEN_LOW;
            case CREDIT -> windowActive
                    ? Outcome.DENY_REPEAT_WINDOW
                    : Outcome.REPEAT_CREDIT;
        };
    }
}

// ExcelGuildGate - a Wink Reyes Valley Excel plugin.

package com.winkreyes.excelweekendxp;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.ZoneId;
import java.util.function.DoublePredicate;
import java.util.function.Predicate;

/**
 * Tracks only complete applications of all XP legs (mcMMO command, vanilla
 * multiplier, EliteMobs skill multiplier), including the initial weekday
 * reset. A transition counts only when every leg applied; any failed leg
 * invalidates the cached state so the whole transition is retried at the
 * next check.
 */
final class WeekendSchedule {
    static final ZoneId ZONE = ZoneId.of("America/Chicago");
    private Boolean lastAppliedWeekend;

    enum Result { UNCHANGED, APPLIED, FAILED }

    static boolean isWeekend(Instant instant) {
        DayOfWeek day = instant.atZone(ZONE).getDayOfWeek();
        return day == DayOfWeek.FRIDAY || day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY;
    }

    /**
     * Two-leg form, kept for the pre-1.3.0 call shape: the EliteMobs leg is
     * treated as not present, so it can never fail the transition.
     */
    Result apply(Instant instant, Predicate<String> dispatch, DoublePredicate applyVanilla) {
        return apply(instant, dispatch, applyVanilla, multiplier -> true);
    }

    Result apply(Instant instant, Predicate<String> dispatch, DoublePredicate applyVanilla,
            DoublePredicate applyEmSkill) {
        boolean weekend = isWeekend(instant);
        if (lastAppliedWeekend != null && lastAppliedWeekend == weekend) {
            return Result.UNCHANGED;
        }
        // A partial transition must invalidate the old state too: a later transition
        // back cannot assume all rates still match that formerly successful state.
        lastAppliedWeekend = null;
        String command = weekend ? "mcmmo:xprate 1.2 true" : "mcmmo:xprate reset";
        if (!dispatch.test(command)) {
            return Result.FAILED;
        }
        if (!applyVanilla.test(weekend ? 1.2 : 1.0)) {
            return Result.FAILED;
        }
        if (!applyEmSkill.test(weekend ? 1.2 : 1.0)) {
            return Result.FAILED;
        }
        lastAppliedWeekend = weekend;
        return Result.APPLIED;
    }
}

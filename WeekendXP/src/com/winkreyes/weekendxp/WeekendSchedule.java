package com.winkreyes.weekendxp;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.ZoneId;
import java.util.function.Predicate;

/** Tracks only successfully dispatched rates, including the initial weekday reset. */
final class WeekendSchedule {
    static final ZoneId ZONE = ZoneId.of("America/Chicago");
    private Boolean lastAppliedWeekend;

    enum Result { UNCHANGED, APPLIED, FAILED }

    static boolean isWeekend(Instant instant) {
        DayOfWeek day = instant.atZone(ZONE).getDayOfWeek();
        return day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY;
    }

    Result apply(Instant instant, Predicate<String> dispatch) {
        boolean weekend = isWeekend(instant);
        if (lastAppliedWeekend != null && lastAppliedWeekend == weekend) {
            return Result.UNCHANGED;
        }
        String command = weekend ? "mcmmo:xprate 1.2 true" : "mcmmo:xprate reset";
        if (!dispatch.test(command)) {
            return Result.FAILED;
        }
        lastAppliedWeekend = weekend;
        return Result.APPLIED;
    }
}

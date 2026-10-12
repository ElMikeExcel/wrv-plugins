package com.winkreyes.weekendxp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class WeekendScheduleTest {
    @Test
    void initialWeekdayResetIsDispatchedExactlyOnce() {
        WeekendSchedule schedule = new WeekendSchedule();
        List<String> commands = new ArrayList<>();
        Instant monday = Instant.parse("2026-10-12T12:00:00Z");
        assertEquals(WeekendSchedule.Result.APPLIED, schedule.apply(monday, commands::add, multiplier -> true));
        assertEquals(WeekendSchedule.Result.UNCHANGED, schedule.apply(monday.plusSeconds(1800), commands::add, multiplier -> true));
        assertEquals(List.of("mcmmo:xprate reset"), commands);
    }

    @Test
    void transitionsFollowChicagoMidnightNotUtc() {
        WeekendSchedule schedule = new WeekendSchedule();
        List<String> commands = new ArrayList<>();
        schedule.apply(Instant.parse("2026-10-10T04:59:59Z"), commands::add, multiplier -> true); // Friday Chicago
        schedule.apply(Instant.parse("2026-10-10T05:00:00Z"), commands::add, multiplier -> true); // Saturday Chicago
        schedule.apply(Instant.parse("2026-10-12T04:59:59Z"), commands::add, multiplier -> true); // Sunday Chicago
        schedule.apply(Instant.parse("2026-10-12T05:00:00Z"), commands::add, multiplier -> true); // Monday Chicago
        assertEquals(List.of("mcmmo:xprate reset", "mcmmo:xprate 1.2 true", "mcmmo:xprate reset"), commands);
    }

    @Test
    void winterBoundaryAndDstFallbackRemainInChicagoTime() {
        assertFalse(WeekendSchedule.isWeekend(Instant.parse("2026-12-05T05:59:59Z")));
        assertTrue(WeekendSchedule.isWeekend(Instant.parse("2026-12-05T06:00:00Z")));
        assertTrue(WeekendSchedule.isWeekend(Instant.parse("2026-11-01T06:30:00Z")));
        assertTrue(WeekendSchedule.isWeekend(Instant.parse("2026-11-01T07:30:00Z")));
        assertTrue(WeekendSchedule.isWeekend(Instant.parse("2026-11-02T05:59:59Z")));
        assertFalse(WeekendSchedule.isWeekend(Instant.parse("2026-11-02T06:00:00Z")));
    }

    @Test
    void failedDispatchRetriesInsteadOfCachingSuccess() {
        WeekendSchedule schedule = new WeekendSchedule();
        Instant saturday = Instant.parse("2026-10-10T12:00:00Z");
        assertEquals(WeekendSchedule.Result.FAILED, schedule.apply(saturday, command -> false, multiplier -> true));
        List<String> commands = new ArrayList<>();
        assertEquals(WeekendSchedule.Result.APPLIED, schedule.apply(saturday, commands::add, multiplier -> true));
        assertEquals(List.of("mcmmo:xprate 1.2 true"), commands);
    }

    @Test
    void throwingDispatchAlsoLeavesTheRateRetryable() {
        WeekendSchedule schedule = new WeekendSchedule();
        Instant monday = Instant.parse("2026-10-12T12:00:00Z");
        assertThrows(IllegalStateException.class,
                () -> schedule.apply(monday, command -> { throw new IllegalStateException("command failed"); }, multiplier -> true));
        assertEquals(WeekendSchedule.Result.APPLIED, schedule.apply(monday, command -> true, multiplier -> true));
    }

    @Test
    void commandPrecedesVanillaAndBothRatesFollowTheSameTransitions() {
        WeekendSchedule schedule = new WeekendSchedule();
        List<String> effects = new ArrayList<>();
        Instant friday = Instant.parse("2026-10-10T04:59:59Z");
        Instant saturday = friday.plusSeconds(1);
        Instant monday = Instant.parse("2026-10-12T05:00:00Z");
        for (Instant instant : List.of(friday, saturday, saturday.plusSeconds(1800), monday)) {
            schedule.apply(instant, command -> effects.add(command),
                    multiplier -> effects.add("vanilla " + multiplier));
        }
        assertEquals(List.of("mcmmo:xprate reset", "vanilla 1.0", "mcmmo:xprate 1.2 true",
                "vanilla 1.2", "mcmmo:xprate reset", "vanilla 1.0"), effects);
    }

    @Test
    void failedCommandDoesNotApplyVanilla() {
        WeekendSchedule schedule = new WeekendSchedule();
        assertEquals(WeekendSchedule.Result.FAILED,
                schedule.apply(Instant.parse("2026-10-10T12:00:00Z"), command -> false,
                        multiplier -> fail("Vanilla must wait for successful command dispatch")));
    }

    @Test
    void failedVanillaRetriesBothApplicationsAndOnlyThenCachesSuccess() {
        WeekendSchedule schedule = new WeekendSchedule();
        Instant saturday = Instant.parse("2026-10-10T12:00:00Z");
        List<String> commands = new ArrayList<>();
        List<Double> multipliers = new ArrayList<>();
        assertEquals(WeekendSchedule.Result.FAILED,
                schedule.apply(saturday, commands::add, multiplier -> false));
        assertEquals(WeekendSchedule.Result.APPLIED,
                schedule.apply(saturday.plusSeconds(1800), commands::add, multipliers::add));
        assertEquals(WeekendSchedule.Result.UNCHANGED,
                schedule.apply(saturday.plusSeconds(3600), commands::add, multipliers::add));
        assertEquals(List.of("mcmmo:xprate 1.2 true", "mcmmo:xprate 1.2 true"), commands);
        assertEquals(List.of(1.2), multipliers);
    }

    @Test
    void throwingVanillaAlsoLeavesBothSettingsRetryable() {
        WeekendSchedule schedule = new WeekendSchedule();
        Instant monday = Instant.parse("2026-10-12T12:00:00Z");
        List<String> commands = new ArrayList<>();
        assertThrows(IllegalStateException.class, () -> schedule.apply(monday, commands::add,
                multiplier -> { throw new IllegalStateException("bridge failed"); }));
        assertEquals(WeekendSchedule.Result.APPLIED,
                schedule.apply(monday, commands::add, multiplier -> true));
        assertEquals(List.of("mcmmo:xprate reset", "mcmmo:xprate reset"), commands);
    }

    @Test
    void partialTransitionCannotReuseAnOldSuccessfulState() {
        WeekendSchedule schedule = new WeekendSchedule();
        Instant saturday = Instant.parse("2026-10-10T12:00:00Z");
        Instant monday = Instant.parse("2026-10-12T12:00:00Z");
        assertEquals(WeekendSchedule.Result.APPLIED,
                schedule.apply(saturday, command -> true, multiplier -> true));
        assertEquals(WeekendSchedule.Result.FAILED,
                schedule.apply(monday, command -> true, multiplier -> false));
        List<String> commands = new ArrayList<>();
        assertEquals(WeekendSchedule.Result.APPLIED,
                schedule.apply(saturday.plusSeconds(7 * 24 * 3600), commands::add, multiplier -> true));
        assertEquals(List.of("mcmmo:xprate 1.2 true"), commands);
    }
}

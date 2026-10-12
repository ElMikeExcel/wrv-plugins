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
        assertEquals(WeekendSchedule.Result.APPLIED, schedule.apply(monday, commands::add));
        assertEquals(WeekendSchedule.Result.UNCHANGED, schedule.apply(monday.plusSeconds(1800), commands::add));
        assertEquals(List.of("mcmmo:xprate reset"), commands);
    }

    @Test
    void transitionsFollowChicagoMidnightNotUtc() {
        WeekendSchedule schedule = new WeekendSchedule();
        List<String> commands = new ArrayList<>();
        schedule.apply(Instant.parse("2026-10-10T04:59:59Z"), commands::add); // Friday Chicago
        schedule.apply(Instant.parse("2026-10-10T05:00:00Z"), commands::add); // Saturday Chicago
        schedule.apply(Instant.parse("2026-10-12T04:59:59Z"), commands::add); // Sunday Chicago
        schedule.apply(Instant.parse("2026-10-12T05:00:00Z"), commands::add); // Monday Chicago
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
        assertEquals(WeekendSchedule.Result.FAILED, schedule.apply(saturday, command -> false));
        List<String> commands = new ArrayList<>();
        assertEquals(WeekendSchedule.Result.APPLIED, schedule.apply(saturday, commands::add));
        assertEquals(List.of("mcmmo:xprate 1.2 true"), commands);
    }

    @Test
    void throwingDispatchAlsoLeavesTheRateRetryable() {
        WeekendSchedule schedule = new WeekendSchedule();
        Instant monday = Instant.parse("2026-10-12T12:00:00Z");
        assertThrows(IllegalStateException.class,
                () -> schedule.apply(monday, command -> { throw new IllegalStateException("command failed"); }));
        assertEquals(WeekendSchedule.Result.APPLIED, schedule.apply(monday, command -> true));
    }
}

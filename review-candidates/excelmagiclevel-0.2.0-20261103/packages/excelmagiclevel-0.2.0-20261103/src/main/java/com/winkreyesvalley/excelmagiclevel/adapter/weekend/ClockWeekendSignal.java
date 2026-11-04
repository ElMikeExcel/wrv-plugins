package com.winkreyesvalley.excelmagiclevel.adapter.weekend;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Set;

/**
 * ExcelMagicLevel — weekend window from the clock. The defaults mirror
 * ExcelWeekendXP's ACTUAL shipped behaviour, verified from its source
 * (WeekendSchedule.java): Saturday and Sunday in America/Chicago. (The
 * SPEC §2 text says "Fri-Sun"; the plugin that owns the rhythm runs
 * Sat/Sun, and matching the live rhythm is the requirement — the days and
 * zone are config keys either way.)
 */
public final class ClockWeekendSignal implements WeekendSignal {
    private final ZoneId zone;
    private final Set<DayOfWeek> days;

    public ClockWeekendSignal(ZoneId zone, Set<DayOfWeek> days) {
        this.zone = zone;
        this.days = Set.copyOf(days);
    }

    @Override
    public boolean isActive(Instant now) {
        return days.contains(now.atZone(zone).getDayOfWeek());
    }
}

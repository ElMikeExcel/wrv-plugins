package com.winkreyesvalley.excelhalloweentoggles;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

/**
 * ExcelHalloweenToggles — the event window.
 *
 * The companion is active THROUGH the configured end date (inclusive)
 * in the configured timezone, and retired from the next local midnight
 * on. Settled values: end date 2026-11-05, America/Chicago — the same
 * day the Halloween plugin's own season retires. Pure date math, no
 * Bukkit state, so the boundary is unit-tested directly.
 */
public final class EventWindow {

    private EventWindow() { }

    /** True while {@code date} is on or before the inclusive end date. */
    public static boolean isActiveOn(LocalDate date, LocalDate endDateInclusive) {
        return date != null && endDateInclusive != null && !date.isAfter(endDateInclusive);
    }

    /** True while the local date of {@code now} in {@code zone} is in the window. */
    public static boolean isActiveAt(Instant now, ZoneId zone, LocalDate endDateInclusive) {
        if (now == null || zone == null) {
            return false;
        }
        return isActiveOn(now.atZone(zone).toLocalDate(), endDateInclusive);
    }
}

package com.winkreyesvalley.excelhalloweentoggles;

import org.junit.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * ExcelHalloweenToggles — auto-retirement boundary.
 *
 * Settled: active THROUGH Nov 5, 2026 in America/Chicago; retired from
 * Nov 6 local midnight. Nov 5 2026 is after the Nov 1 DST fall-back, so
 * Chicago is CST (UTC-6) on the boundary — the instant-level tests pin
 * exactly that.
 */
public class EventWindowTest {

    private static final LocalDate END = LocalDate.of(2026, 11, 5);
    private static final ZoneId CHICAGO = ZoneId.of("America/Chicago");

    @Test
    public void dateLevelBoundary() {
        assertTrue(EventWindow.isActiveOn(LocalDate.of(2026, 10, 31), END));
        assertTrue(EventWindow.isActiveOn(LocalDate.of(2026, 11, 5), END));
        assertFalse(EventWindow.isActiveOn(LocalDate.of(2026, 11, 6), END));
        assertFalse(EventWindow.isActiveOn(LocalDate.of(2026, 12, 25), END));
    }

    @Test
    public void instantLevelBoundaryUsesLocalDate() {
        // Nov 5, 23:30 CST = Nov 6, 05:30 UTC — still the 5th in Chicago.
        assertTrue(EventWindow.isActiveAt(Instant.parse("2026-11-06T05:30:00Z"), CHICAGO, END));
        // Nov 6, 00:30 CST = Nov 6, 06:30 UTC — retired.
        assertFalse(EventWindow.isActiveAt(Instant.parse("2026-11-06T06:30:00Z"), CHICAGO, END));
        // Nov 5, 00:30 CST = Nov 5, 06:30 UTC — active from the day's start.
        assertTrue(EventWindow.isActiveAt(Instant.parse("2026-11-05T06:30:00Z"), CHICAGO, END));
    }

    @Test
    public void nullInputsAreInactive() {
        assertFalse(EventWindow.isActiveOn(null, END));
        assertFalse(EventWindow.isActiveAt(null, CHICAGO, END));
        assertFalse(EventWindow.isActiveAt(Instant.now(), null, END));
    }
}

package com.winkreyes.exceldetails;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pure-logic proofs: segment lifecycle, budget self-cap, line
 * shedding, formatting. Excel — Wink Reyes Valley (Iris / Mike).
 */
public class DetailsTest {

    private static final UUID A = UUID.randomUUID();
    private static final UUID B = UUID.randomUUID();
    private static final long T0 = 1_000_000L;

    // ---------- segment lifecycle ----------

    @Test
    void segmentOpensOnFirstHitAndAccumulates() {
        SegmentTracker t = new SegmentTracker();
        assertFalse(t.isActive());
        t.recordDamageDone(A, "Mike", 12.5, "Warden", T0);
        assertTrue(t.isActive());
        t.recordDamageDone(A, "Mike", 7.5, "Warden", T0 + 1000);
        t.recordDamageTaken(A, "Mike", 18.75, "Warden", T0 + 1000);
        assertEquals(20.0, t.currentStats(A).damageDone(), 1e-9);
        assertEquals(18.75, t.currentStats(A).damageTaken(), 1e-9);
        assertEquals(20.0, t.currentStats(A).doneByTarget().get("Warden"), 1e-9);
        assertEquals(1000, t.elapsedMs(T0 + 1000));
    }

    @Test
    void segmentClosesAfterQuietTimeoutAndMergesOverall() {
        SegmentTracker t = new SegmentTracker();
        t.recordDamageDone(A, "Mike", 100, "Zombie", T0);
        assertFalse(t.tick(T0 + 7_999, 8_000));
        assertTrue(t.isActive());
        assertTrue(t.tick(T0 + 8_000, 8_000));
        assertFalse(t.isActive());
        assertEquals(100.0, t.overallStats(A).damageDone(), 1e-9);
        // last segment stays readable while idle
        assertEquals(1, t.ranked(CombatStats.Window.DAMAGE_DONE).size());
        assertEquals(100.0, t.ranked(CombatStats.Window.DAMAGE_DONE).get(0).amount(), 1e-9);
        // a fresh hit opens a NEW segment with clean numbers
        t.recordDamageDone(A, "Mike", 5, "Zombie", T0 + 20_000);
        assertEquals(5.0, t.currentStats(A).damageDone(), 1e-9);
        assertEquals(100.0, t.overallStats(A).damageDone(), 1e-9);
    }

    @Test
    void healingIsTrackedSeparately() {
        SegmentTracker t = new SegmentTracker();
        t.recordHealing(B, "Kay", 6.0, T0);
        t.recordDamageDone(B, "Kay", 3.0, "Skeleton", T0);
        assertEquals(6.0, t.currentStats(B).healingDone(), 1e-9);
        assertEquals(3.0, t.currentStats(B).damageDone(), 1e-9);
        assertEquals(6.0, t.windowTotal(CombatStats.Window.HEALING_DONE), 1e-9);
    }

    @Test
    void manualClearDiscardsAndRestartsClock() {
        SegmentTracker t = new SegmentTracker();
        t.recordDamageDone(A, "Mike", 50, "Zombie", T0);
        t.clearCurrent(T0 + 5_000);
        assertTrue(t.isActive());
        assertEquals(0.0, t.currentStats(A).damageDone(), 1e-9);
        assertEquals(0, t.elapsedMs(T0 + 5_000));
        // cleared numbers never reach the overall totals
        assertTrue(t.tick(T0 + 13_000, 8_000));
        assertEquals(0.0, t.overallStats(A).damageDone(), 1e-9);
    }

    @Test
    void rankingIsDescendingAndZeroesExcluded() {
        SegmentTracker t = new SegmentTracker();
        t.recordDamageDone(A, "Mike", 30, "Zombie", T0);
        t.recordDamageDone(B, "Kay", 90, "Zombie", T0);
        List<SegmentTracker.Row> rows = t.ranked(CombatStats.Window.DAMAGE_DONE);
        assertEquals(2, rows.size());
        assertEquals("Kay", rows.get(0).name());
        assertEquals(120.0, t.windowTotal(CombatStats.Window.DAMAGE_DONE), 1e-9);
        assertTrue(t.ranked(CombatStats.Window.DAMAGE_TAKEN).isEmpty());
    }

    @Test
    void nonPositiveAmountsNeverOpenASegment() {
        SegmentTracker t = new SegmentTracker();
        t.recordDamageDone(A, "Mike", 0, "Zombie", T0);
        t.recordDamageTaken(A, "Mike", -3, "Fall", T0);
        assertFalse(t.isActive());
    }

    // ---------- budget / placement math ----------

    @Test
    void piggybackBudgetIsFifteenMinusForeignLines() {
        assertEquals(15, MeterMath.meterBudget(0));
        assertEquals(9, MeterMath.meterBudget(6));   // typical EM party box
        assertEquals(0, MeterMath.meterBudget(15));  // quest view owns the box
        assertEquals(0, MeterMath.meterBudget(20));
    }

    @Test
    void linesShedFooterThenLowestRowsHeaderLast() {
        SegmentTracker t = new SegmentTracker();
        t.recordDamageDone(A, "Mike", 100, "Warden", T0);
        t.recordDamageDone(B, "Kay", 50, "Warden", T0);
        List<SegmentTracker.Row> rows = t.ranked(CombatStats.Window.DAMAGE_DONE);
        // budget 2: header + top row only, footer shed
        List<String> two = LineBuilder.build(CombatStats.Window.DAMAGE_DONE, rows, A, 150, 30_000, 6, 2);
        assertEquals(2, two.size());
        assertTrue(two.get(0).contains("Details!"));
        assertTrue(two.get(1).contains("Mike"));
        // budget 1: header alone survives
        List<String> one = LineBuilder.build(CombatStats.Window.DAMAGE_DONE, rows, A, 150, 30_000, 6, 1);
        assertEquals(1, one.size());
        // budget 0: nothing written at all
        assertTrue(LineBuilder.build(CombatStats.Window.DAMAGE_DONE, rows, A, 150, 30_000, 6, 0).isEmpty());
    }

    @Test
    void ownRowSurvivesTheCut() {
        SegmentTracker t = new SegmentTracker();
        UUID c = UUID.randomUUID();
        t.recordDamageDone(A, "Mike", 300, "Warden", T0);
        t.recordDamageDone(B, "Kay", 200, "Warden", T0);
        t.recordDamageDone(c, "Rylie", 10, "Warden", T0);
        List<SegmentTracker.Row> rows = t.ranked(CombatStats.Window.DAMAGE_DONE);
        // budget 4 = header + 1 row + footer; viewer is rank 3 but keeps a row
        List<String> lines = LineBuilder.build(CombatStats.Window.DAMAGE_DONE, rows, c, 510, 60_000, 6, 4);
        assertEquals(4, lines.size());
        assertTrue(lines.stream().anyMatch(l -> l.contains("Rylie")), String.join(" | ", lines));
    }

    // ---------- formatting ----------

    @Test
    void compactAmountsReadLikeDetails() {
        assertEquals("950", MeterMath.compact(950));
        assertEquals("12.5", MeterMath.compact(12.5));
        assertEquals("12.5k", MeterMath.compact(12_500));
        assertEquals("3.4M", MeterMath.compact(3_400_000));
        assertEquals("0", MeterMath.compact(0));
    }

    @Test
    void percentAndPerSecondAndClock() {
        assertEquals(45, MeterMath.percent(45, 100));
        assertEquals(0, MeterMath.percent(5, 0));
        assertEquals(10.0, MeterMath.perSecond(20, 2_000), 1e-9);
        assertEquals(20.0, MeterMath.perSecond(20, 500), 1e-9); // 1 s floor
        assertEquals("0:42", MeterMath.clock(42_000));
        assertEquals("12:05", MeterMath.clock(725_000));
    }

    @Test
    void windowParsingAndCycling() {
        assertEquals(CombatStats.Window.DAMAGE_TAKEN, CombatStats.Window.parse("taken"));
        assertEquals(CombatStats.Window.HEALING_DONE, CombatStats.Window.parse("healing"));
        assertEquals(CombatStats.Window.DAMAGE_DONE, CombatStats.Window.parse("damage"));
        assertNull(CombatStats.Window.parse("bogus"));
        assertEquals(CombatStats.Window.DAMAGE_TAKEN, CombatStats.Window.DAMAGE_DONE.next());
        assertEquals(CombatStats.Window.DAMAGE_DONE, CombatStats.Window.HEALING_DONE.next());
    }

    @Test
    void prefsDefaultOnAndToggleFlips() {
        PlayerPrefs p = new PlayerPrefs();
        assertTrue(p.isEnabled(A));
        assertFalse(p.toggle(A));
        assertTrue(p.toggle(A));
        assertEquals(CombatStats.Window.DAMAGE_DONE, p.window(A));
    }
}

package com.winkreyesvalley.excelmagiclevel.core;

import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

/**
 * ExcelMagicLevel — curve derivation. The shipping pin is the 1000-level
 * table (SPEC §10 as amended Oct 31, 2026: scale 1000, even spread, same
 * grind re-sliced). The Phase 0–1 §5.1 20-level table survives as an
 * engine regression: its TOTAL is the invariant the rescale had to keep.
 */
public class CurveTest {
    /** The shipping curve: base 11, exponent 0.30, round-to 1, 1000 levels. */
    private final Curve standard = new Curve(1000, 11.0, 0.30, 1, Map.of());

    @Test
    public void thousandLevelTablePinned() {
        // Derived with the production Curve class and pinned here: first
        // step in the low double digits, last step 87, monotonic, total
        // 67,168 — the specced Standard total (67,140) + 28 (+0.04%).
        assertEquals(11, standard.xpToNext(1));
        assertEquals(14, standard.xpToNext(2));
        assertEquals(44, standard.xpToNext(99));
        assertEquals(71, standard.xpToNext(500));
        assertEquals(87, standard.xpToNext(999));
        assertEquals(3346, standard.cumulativeFor(100));
        assertEquals(27260, standard.cumulativeFor(500));
        assertEquals(67081, standard.cumulativeFor(999));
        assertEquals(67168, standard.cumulativeFor(1000));
        assertEquals(67168, standard.totalToCap());
    }

    @Test
    public void thousandLevelCurveIsMonotoneAndPositive() {
        long prevStep = -1;
        long prevCum = -1;
        for (int level = 1; level <= 999; level++) {
            long step = standard.xpToNext(level);
            assertTrue("xp-to-next must be positive at " + level, step > 0);
            assertTrue("xp-to-next must be non-decreasing at " + level, step >= prevStep);
            prevStep = step;
            long cum = standard.cumulativeFor(level + 1);
            assertTrue("cumulative must strictly increase at " + level, cum > prevCum);
            prevCum = cum;
            assertEquals(level + 1, standard.levelFor(cum));
            assertEquals(level, standard.levelFor(cum - 1));
        }
    }

    @Test
    public void levelBoundariesAreExact() {
        assertEquals(1, standard.levelFor(0));
        assertEquals(1, standard.levelFor(10));
        assertEquals(2, standard.levelFor(11));     // exact threshold levels up
        assertEquals(500, standard.levelFor(27260));
        assertEquals(499, standard.levelFor(27259));
        assertEquals(999, standard.levelFor(67167));
        assertEquals(1000, standard.levelFor(67168)); // cap threshold
        assertEquals(1000, standard.levelFor(Long.MAX_VALUE / 4)); // far past cap stays capped
    }

    @Test
    public void intoAndRemaining() {
        assertEquals(0, standard.xpIntoLevel(11));
        assertEquals(0, standard.xpIntoLevel(27260));
        assertEquals(71, standard.xpToNextLevel(27260));
        assertEquals(1, standard.xpToNextLevel(67167));
        assertEquals(0, standard.xpToNextLevel(67168)); // cap: nothing remaining
        assertEquals(0, standard.xpToNextLevel(999_999_999L));
    }

    @Test
    public void legacyTwentyLevelTableUnchanged() {
        // SPEC §5.1's printed table, kept as the rounding-semantics
        // regression — and its 67,140 total is the invariant the
        // 1000-level rescale was derived against (67,168, +0.04%).
        Curve legacy = new Curve(20, 100.0, 1.5, 10, Map.of());
        long[][] table = {
                {1, 100, 100}, {2, 280, 380}, {3, 520, 900}, {4, 800, 1700},
                {5, 1120, 2820}, {6, 1470, 4290}, {7, 1850, 6140}, {8, 2260, 8400},
                {9, 2700, 11100}, {10, 3160, 14260}, {11, 3650, 17910}, {12, 4160, 22070},
                {13, 4690, 26760}, {14, 5240, 32000}, {15, 5810, 37810}, {16, 6400, 44210},
                {17, 7010, 51220}, {18, 7640, 58860}, {19, 8280, 67140},
        };
        for (long[] row : table) {
            int from = (int) row[0];
            assertEquals("xpToNext(" + from + ")", row[1], legacy.xpToNext(from));
            assertEquals("cumulativeFor(" + (from + 1) + ")", row[2], legacy.cumulativeFor(from + 1));
        }
        assertEquals(67140, legacy.totalToCap());
    }

    @Test
    public void overridesWinOverFormula() {
        Curve custom = new Curve(5, 100.0, 1.5, 10, Map.of(2, 999L));
        assertEquals(999, custom.xpToNext(2));
        assertEquals(100 + 999, custom.cumulativeFor(3));
        assertEquals(100, custom.xpToNext(1));
    }

    @Test
    public void rescaleIsConfigOnly() {
        // The level count is data: a 100-level curve is the same code with
        // different numbers — derivation stays consistent and monotone.
        // (Pace equivalence under a rescale is a config choice; what must
        // never happen is the ENGINE changing.)
        Curve hundred = new Curve(100, 100.0, 1.5, 10, Map.of());
        long prev = -1;
        for (int level = 1; level <= 100; level++) {
            long cum = hundred.cumulativeFor(level);
            assertTrue("cumulative must strictly increase", cum > prev);
            prev = cum;
            assertEquals(level, hundred.levelFor(cum));
            if (level > 1) assertEquals(level - 1, hundred.levelFor(cum - 1));
        }
    }

    @Test
    public void invalidCurvesRejected() {
        assertThrows(IllegalArgumentException.class, () -> new Curve(1, 100, 1.5, 10, Map.of()));
        assertThrows(IllegalArgumentException.class, () -> new Curve(20, 0, 1.5, 10, Map.of()));
        assertThrows(IllegalArgumentException.class, () -> new Curve(20, 100, 0, 10, Map.of()));
        assertThrows(IllegalArgumentException.class, () -> standard.xpToNext(1000));
        assertThrows(IllegalArgumentException.class, () -> standard.xpToNext(0));
    }
}

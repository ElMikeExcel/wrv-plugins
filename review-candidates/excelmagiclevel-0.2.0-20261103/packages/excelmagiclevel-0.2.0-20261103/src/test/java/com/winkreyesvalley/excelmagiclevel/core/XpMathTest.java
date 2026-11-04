package com.winkreyesvalley.excelmagiclevel.core;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/** ExcelMagicLevel — the §4.6 payment order: window → target → multiplier → floor. */
public class XpMathTest {
    private static final long WINDOW = 300_000L; // 300 s

    @Test
    public void noHistoryPaysFullWeight() {
        assertEquals(6, XpMath.payment(6, null, WINDOW, false, 0.5, 1.0));
    }

    @Test
    public void fullWindowElapsedPaysFullWeight() {
        assertEquals(6, XpMath.payment(6, WINDOW, WINDOW, false, 0.5, 1.0));
        assertEquals(6, XpMath.payment(6, WINDOW * 3, WINDOW, false, 0.5, 1.0));
    }

    @Test
    public void windowRampIsFloorOfLinear() {
        // SPEC §4.2: floor(weight × min(1, elapsed / window))
        assertEquals(3, XpMath.payment(6, WINDOW / 2, WINDOW, false, 0.5, 1.0)); // floor(6 × 0.5)
        assertEquals(5, XpMath.payment(20, WINDOW / 4, WINDOW, false, 0.5, 1.0)); // floor(20 × 0.25)
        assertEquals(0, XpMath.payment(2, 1_000L, WINDOW, false, 0.5, 1.0));      // spam asymptotes to 0
        assertEquals(0, XpMath.payment(6, 0L, WINDOW, false, 0.5, 1.0));          // same-tick repeat pays 0
    }

    @Test
    public void targetRepeatHalvesAfterWindow() {
        assertEquals(10, XpMath.payment(20, null, WINDOW, true, 0.5, 1.0));       // full window, halved
        assertEquals(2, XpMath.payment(20, WINDOW / 4, WINDOW, true, 0.5, 1.0));  // floor(20 × 0.25 × 0.5) = floor(2.5)
    }

    @Test
    public void weekendMultiplierAppliesBeforeSingleFloor() {
        assertEquals(24, XpMath.payment(20, null, WINDOW, false, 0.5, 1.2));      // floor(20 × 1.2)
        assertEquals(1, XpMath.payment(6, WINDOW / 2, WINDOW, true, 0.5, 1.2));   // floor(6 × .5 × .5 × 1.2) = floor(1.8)
        assertEquals(2, XpMath.payment(2, null, WINDOW, false, 0.5, 1.2));        // floor(2.4) — multiplier can't create XP from a 0-weight
    }

    @Test
    public void zeroOrNegativeWeightPaysNothing() {
        assertEquals(0, XpMath.payment(0, null, WINDOW, false, 0.5, 1.2));
        assertEquals(0, XpMath.payment(-3, null, WINDOW, false, 0.5, 1.2));
    }
}

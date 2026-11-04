package com.winkreyesvalley.excelhalloweentoggles;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * ExcelHalloweenToggles — the full fire-gate matrix.
 *
 * The guarantee the whole plugin exists for lives here: a toggled-off
 * scare NEVER fires (STOP, chain ends) — including while the Witching
 * Hour would halve intervals, because the delay math is irrelevant once
 * the gate says STOP. Global-off and momentary ineligibility are RETRY
 * (the event isn't over, the player hasn't opted out).
 */
public class ScareGateTest {

    @Test
    public void firesOnlyWhenEverythingPasses() {
        assertEquals(ScareGate.Decision.FIRE,
                ScareGate.decide(true, true, true, true, true));
    }

    @Test
    public void toggleOffAlwaysStops_evenInWitchingHourConditions() {
        // Witching Hour changes only the delay, never the gate inputs —
        // every combination with toggleOn=false must be STOP.
        for (boolean global : new boolean[]{true, false}) {
            for (boolean eligible : new boolean[]{true, false}) {
                assertEquals("toggle off must stop (global=" + global + ", eligible=" + eligible + ")",
                        ScareGate.Decision.STOP,
                        ScareGate.decide(true, true, global, false, eligible));
            }
        }
    }

    @Test
    public void allTogglesOffMeansNoFireForAnyInput() {
        // The "player with every toggle off gets zero scheduled scares"
        // edge, at the decision level for every feature shape.
        for (boolean global : new boolean[]{true, false}) {
            for (boolean eligible : new boolean[]{true, false}) {
                assertEquals(ScareGate.Decision.STOP, ScareGate.decide(true, true, global, false, eligible));
            }
        }
    }

    @Test
    public void retiredEventStopsEverything() {
        assertEquals(ScareGate.Decision.STOP, ScareGate.decide(false, true, true, true, true));
        assertEquals(ScareGate.Decision.STOP, ScareGate.decide(false, true, false, false, false));
    }

    @Test
    public void schedulerDisabledStopsEverything() {
        assertEquals(ScareGate.Decision.STOP, ScareGate.decide(true, false, true, true, true));
    }

    @Test
    public void globalOffRetries() {
        assertEquals(ScareGate.Decision.RETRY, ScareGate.decide(true, true, false, true, true));
    }

    @Test
    public void ineligibleRetries() {
        // Covers /halloween disable, blacklisted world, vanish, spectator:
        // all fold into isEligible() and all are temporary states.
        assertEquals(ScareGate.Decision.RETRY, ScareGate.decide(true, true, true, true, false));
    }

    @Test
    public void loginGateIsAPlainAnd() {
        assertEquals(true, ScareGate.shouldApplyLogin(true, true, true, true, true));
        assertEquals(false, ScareGate.shouldApplyLogin(false, true, true, true, true));
        assertEquals(false, ScareGate.shouldApplyLogin(true, false, true, true, true));
        assertEquals(false, ScareGate.shouldApplyLogin(true, true, false, true, true));
        assertEquals(false, ScareGate.shouldApplyLogin(true, true, true, false, true));
        assertEquals(false, ScareGate.shouldApplyLogin(true, true, true, true, false));
    }
}

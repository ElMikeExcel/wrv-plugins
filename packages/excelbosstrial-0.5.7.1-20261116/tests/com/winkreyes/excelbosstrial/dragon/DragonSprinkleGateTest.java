/* Excel WRV maintenance; existing authorship retained. */ package com.winkreyes.excelbosstrial.dragon;

/**
 * The dragon sprinkle's phase gate (Packet B, D-4): the
 * cadence runs in PHASE 2 only - the design's F-D6 failure
 * (phase-1 rain, a leash anchor chasing the flying dragon)
 * is impossible through this gate - and the fountain anchor
 * registers at battle registration, in either fighting phase.
 */
public final class DragonSprinkleGateTest {

    private static int passed;
    private static int failed;

    private static void check(boolean cond, String label) {
        if (cond) {
            passed++;
        } else {
            failed++;
            System.out.println("FAIL: " + label);
        }
    }

    public static void main(String[] args) {
        check(!DragonSprinkleGate.cadenceRuns(BattlePhase.NONE),
                "gate: no battle, no cadence");
        check(!DragonSprinkleGate.cadenceRuns(BattlePhase.PHASE1),
                "gate: phase 1 stays vanilla - no cadence (F-D6)");
        check(!DragonSprinkleGate.cadenceRuns(BattlePhase.TRANSITION),
                "gate: the transition's invulnerable beat runs no cadence");
        check(DragonSprinkleGate.cadenceRuns(BattlePhase.PHASE2),
                "gate: phase 2 is the one phase the cadence runs in");
        check(!DragonSprinkleGate.cadenceRuns(BattlePhase.DONE),
                "gate: a finished battle runs no cadence");
        check(!DragonSprinkleGate.cadenceRuns(null),
                "gate: a null phase runs no cadence (fail-dark)");

        check(DragonSprinkleGate.anchorRegisters(BattlePhase.PHASE1),
                "anchor: registration at battle start (phase 1) registers the fountain");
        check(DragonSprinkleGate.anchorRegisters(BattlePhase.PHASE2),
                "anchor: a battle first seen in phase 2 still registers");
        check(!DragonSprinkleGate.anchorRegisters(BattlePhase.NONE),
                "anchor: no battle, no registration");
        check(!DragonSprinkleGate.anchorRegisters(BattlePhase.DONE),
                "anchor: a finished battle does not re-register");

        System.out.println("DragonSprinkleGateTest: " + passed + " passed, " + failed + " failed");
        if (failed > 0) {
            System.exit(1);
        }
    }
}

// ExcelBossTrial - a Wink Reyes Valley Excel plugin.

/* Excel WRV maintenance; existing authorship retained. */ package com.winkreyes.excelbosstrial.dragon;

/**
 * The dragon sprinkle's phase gate (Item 35 Packet B, D-4):
 * the dragon's cadence sprinkle runs in PHASE 2 ONLY, anchored
 * on the fountain - never in phase 1, never in the transition,
 * never after the battle. This is the seam design finding F-D6
 * requires: arming dragon.sprinkle by config alone would rain
 * waves during phase 1 (vanilla by design) and, until the first
 * stamp registered the fountain as a fixed anchor, leash adds
 * after a dragon that flies 50+ blocks from the island.
 *
 * Two halves, both pinned here so the adapter wiring can be
 * unit-checked without a server:
 * - {@link #cadenceRuns} - the host's 1-second walk asks this
 *   before it may call the sprinkle's cadence tick for a
 *   dragon encounter (the chapter answers from its own state
 *   machine - the phase seam);
 * - the fountain anchor is registered with the sprinkle at
 *   BATTLE REGISTRATION (DragonChapter's registration sink),
 *   not at the first milestone stamp - so the very first
 *   phase-2 cadence wave already leashes to the island, not
 *   to the flying dragon.
 */
public final class DragonSprinkleGate {

    private DragonSprinkleGate() {
    }

    /** True only in PHASE2: the one phase the cadence may run. */
    public static boolean cadenceRuns(BattlePhase phase) {
        return phase == BattlePhase.PHASE2;
    }

    /** True when a battle registration should (re)register the
     *  fountain anchor: a battle is live in either fighting
     *  phase. Registration happens at battle start (phase 1) -
     *  long before the cadence can fire - by design. */
    public static boolean anchorRegisters(BattlePhase phase) {
        return phase == BattlePhase.PHASE1 || phase == BattlePhase.PHASE2;
    }
}

// ExcelBossTrial - a Wink Reyes Valley Excel plugin.

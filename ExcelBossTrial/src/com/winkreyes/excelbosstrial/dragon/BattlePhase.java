package com.winkreyes.excelbosstrial.dragon;

/**
 * The trial's view of one End battle.
 *
 * NONE      - no dragon battle is being tracked for the world.
 * PHASE1    - the near-vanilla first bar. A death is NEVER permitted
 *             in this phase (see TrialStateMachine.onDamage).
 * TRANSITION- the flip is running: the killing blow was intercepted,
 *             the dragon is invulnerable and already healed onto its
 *             phase-2 bar. Restored from disk as PHASE2.
 * PHASE2    - the real test. Exactly one death is permitted here.
 * DONE      - the battle is over (dragon died for real, or vanished);
 *             bookkeeping only.
 */
public enum BattlePhase {
    NONE,
    PHASE1,
    TRANSITION,
    PHASE2,
    DONE
}


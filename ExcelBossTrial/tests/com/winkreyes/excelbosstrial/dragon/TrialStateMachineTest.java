package com.winkreyes.excelbosstrial.dragon;

import com.winkreyes.excelbosstrial.dragon.BattlePhase;
import com.winkreyes.excelbosstrial.dragon.DamagerKind;
import com.winkreyes.excelbosstrial.dragon.TrialConfig;
import com.winkreyes.excelbosstrial.dragon.TrialStateMachine;

/** Waves, buff gating, and lifecycle behaviour of the state machine. */
public final class TrialStateMachineTest {

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

    private static TrialStateMachine phase2() {
        TrialStateMachine m = new TrialStateMachine();
        m.startBattle("d");
        m.onDamage(250, 250); // flip
        m.completeTransition();
        return m;
    }

    public static void main(String[] args) {
        // --- crossedMask thresholds --------------------------------
        check(TrialStateMachine.crossedMask(0.90) == 0, "90% crosses nothing");
        check(TrialStateMachine.crossedMask(0.75) == 0b001, "75% crosses wave 1");
        check(TrialStateMachine.crossedMask(0.60) == 0b001, "60% still only wave 1");
        check(TrialStateMachine.crossedMask(0.50) == 0b011, "50% crosses waves 1+2");
        check(TrialStateMachine.crossedMask(0.25) == 0b111, "25% crosses all waves");
        check(TrialStateMachine.crossedMask(0.0) == 0b111, "0% crosses all waves");

        // --- pending waves fire once each --------------------------
        TrialStateMachine m = phase2();
        check(m.pendingWaveMask(0.80) == 0, "phase2: nothing pending at 80%");
        check(m.pendingWaveMask(0.70) == 0b001, "phase2: wave 1 pending at 70%");
        m.commitWaves(0b001);
        check(m.pendingWaveMask(0.70) == 0, "wave 1 does not refire");
        check(m.pendingWaveMask(0.40) == 0b010, "wave 2 pending at 40%");
        check(m.pendingWaveMask(0.10) == 0b110, "one big blow can owe waves 2+3 together");
        m.commitWaves(0b110);
        check(m.pendingWaveMask(0.05) == 0, "all waves spent");
        check(m.wavesFiredMask() == 0b111, "mask remembers all three waves");

        // --- waves never pend outside phase 2 -----------------------
        TrialStateMachine p1 = new TrialStateMachine();
        p1.startBattle("d");
        check(p1.pendingWaveMask(0.10) == 0, "phase 1 never pends waves");

        // --- waves survive a restart (bitmask persisted) ------------
        TrialStateMachine rs = new TrialStateMachine();
        rs.restore(BattlePhase.PHASE2, "d", 0b101, false, false);
        check(rs.pendingWaveMask(0.10) == 0b010, "restored mask: only wave 2 still owed");

        // --- buff gating ---------------------------------------------
        TrialConfig cfg = TrialConfig.defaults();
        check(m.buffApplies(DamagerKind.CONTACT), "phase2 contact buffed");
        check(m.buffApplies(DamagerKind.FIREBALL), "phase2 fireball buffed");
        check(m.buffApplies(DamagerKind.BREATH), "phase2 breath buffed");
        check(!m.buffApplies(DamagerKind.OTHER), "phase2 other never buffed");
        check(!p1.buffApplies(DamagerKind.CONTACT), "phase1 contact not buffed");
        check(TrialStateMachine.scaledDamage(10.0, cfg) == 15.0, "10 x 1.5 = 15");
        check(TrialStateMachine.scaledDamage(7.0, cfg) == 10.5, "7 x 1.5 = 10.5");

        // --- lifecycle ------------------------------------------------
        TrialStateMachine life = new TrialStateMachine();
        check(!life.hasBattle() && life.phase() == BattlePhase.NONE, "new machine idle");
        life.startBattle("aaa");
        check(life.hasBattle() && life.tracks("aaa") && !life.tracks("bbb"), "tracks its dragon");
        life.endBattle();
        check(!life.hasBattle() && life.phase() == BattlePhase.DONE, "ended battle is DONE");
        check(life.onDamage(10, 999) == TrialStateMachine.Verdict.PASS,
                "ended machine passes everything");

        System.out.println("TrialStateMachineTest: " + passed + " passed, " + failed + " failed");
        if (failed > 0) {
            System.exit(1);
        }
    }
}

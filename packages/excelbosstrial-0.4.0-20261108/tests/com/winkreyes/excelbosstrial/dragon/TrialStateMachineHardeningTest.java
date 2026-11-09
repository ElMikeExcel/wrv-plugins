/* Excel WRV maintenance; existing authorship retained. */ package com.winkreyes.excelbosstrial.dragon;

/**
 * 0.2.11 (B-1): the double-judgment contract the HIGHEST re-judgment
 * pass relies on, plus the danger-zone predicate. The adapter now
 * judges a PHASE1 blow twice (HIGH with governed numbers, HIGHEST
 * with the event's final numbers); these checks pin that the second
 * judgment can only ever escalate a PASS into the FLIP the first
 * pass missed — never double-flip, never double-consume a death,
 * and never disturb a PHASE2 death. The amplifier scenario is the
 * Oct 30, 2026 kill: judged 40 at HIGH against 50 HP (PASS), grown
 * to 100 by a HIGHEST modifier — the second judgment must FLIP.
 */
public final class TrialStateMachineHardeningTest {

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
        TrialConfig cfg = TrialConfig.defaults(); // per-event cap 60

        // --- Double PASS: two judgments of a non-lethal blow mutate nothing.
        TrialStateMachine m = new TrialStateMachine();
        m.startBattle("d1");
        check(m.onDamage(400, 60) == TrialStateMachine.Verdict.PASS, "first judgment PASS");
        check(m.onDamage(400, 60) == TrialStateMachine.Verdict.PASS, "second judgment PASS");
        check(m.phase() == BattlePhase.PHASE1 && !m.deathConsumed(),
                "double PASS leaves the battle untouched");

        // --- The amplifier reproduction: PASS at HIGH, FLIP at HIGHEST.
        TrialStateMachine amp = new TrialStateMachine();
        amp.startBattle("d2");
        check(amp.onDamage(50, 40) == TrialStateMachine.Verdict.PASS,
                "amplifier: governed 40 vs 50 HP passes the HIGH judgment");
        check(amp.onDamage(50, 100) == TrialStateMachine.Verdict.FLIP,
                "amplifier: grown to 100, the HIGHEST re-judgment FLIPs");
        check(amp.phase() == BattlePhase.TRANSITION && !amp.deathConsumed(),
                "amplifier: the flip consumed no death");

        // --- Post-FLIP judgments are inert: TRANSITION never re-judges.
        check(amp.onDamage(50, 10_000) == TrialStateMachine.Verdict.PASS,
                "post-FLIP judgment in TRANSITION passes");
        check(amp.phase() == BattlePhase.TRANSITION && !amp.deathConsumed(),
                "post-FLIP: still TRANSITION, death still unconsumed");

        // --- A HIGH FLIP is not re-flippable: same lethal numbers twice.
        TrialStateMachine flip = new TrialStateMachine();
        flip.startBattle("d3");
        check(flip.onDamage(30, 45) == TrialStateMachine.Verdict.FLIP, "lethal in PHASE1 flips");
        check(flip.onDamage(30, 45) == TrialStateMachine.Verdict.PASS,
                "the same blow judged again does not flip twice");

        // --- PHASE2: the one real death is consumed exactly once,
        // however many passes judge the blow.
        TrialStateMachine p2 = new TrialStateMachine();
        p2.startBattle("d4");
        p2.onDamage(30, 45); // FLIP -> TRANSITION
        p2.completeTransition();
        check(p2.phase() == BattlePhase.PHASE2, "transition completes into PHASE2");
        check(p2.onDamage(900, 950) == TrialStateMachine.Verdict.ALLOW_DEATH,
                "PHASE2 lethal allows the one death");
        check(p2.onDamage(900, 950) == TrialStateMachine.Verdict.PASS,
                "PHASE2 lethal judged again passes (death already consumed)");
        check(p2.deathConsumed(), "PHASE2 death consumed exactly once");

        // --- The Oct 30 shape: a restored mid-trial state. A battle
        // saved in TRANSITION restores as PHASE2 and can die properly;
        // a battle saved in PHASE1 restores in PHASE1 and the flip
        // still guards it.
        TrialStateMachine restored = new TrialStateMachine();
        restored.restore(BattlePhase.TRANSITION, "d5", 0, false, true);
        check(restored.phase() == BattlePhase.PHASE2, "saved TRANSITION restores as PHASE2");
        check(restored.onDamage(900, 950) == TrialStateMachine.Verdict.ALLOW_DEATH,
                "restored PHASE2 trial can reach its real death");
        TrialStateMachine stale = new TrialStateMachine();
        stale.restore(BattlePhase.PHASE1, "d6", 0, false, false);
        check(stale.onDamage(50, 40) == TrialStateMachine.Verdict.PASS,
                "restored PHASE1: governed blow passes");
        check(stale.onDamage(50, 100) == TrialStateMachine.Verdict.FLIP,
                "restored PHASE1: amplified blow flips on re-judgment");

        // --- Danger zone: health at or below the per-event cap.
        check(TrialStateMachine.inDangerZone(60, cfg), "health == cap is in the zone");
        check(TrialStateMachine.inDangerZone(59.5, cfg), "health just below cap is in the zone");
        check(TrialStateMachine.inDangerZone(1, cfg), "health 1 is in the zone");
        check(!TrialStateMachine.inDangerZone(60.5, cfg), "health just above cap is outside");
        check(!TrialStateMachine.inDangerZone(400, cfg), "full phase-1 bar is outside");
        check(!TrialStateMachine.inDangerZone(0, cfg), "health 0 (already dead) is outside");
        check(!TrialStateMachine.inDangerZone(-10, cfg), "negative health is outside");
        check(!TrialStateMachine.inDangerZone(30, null), "no config, no zone");
        TrialConfig noCap = new TrialConfig(true, true, 250.0, 500.0, 1.5, 3, 7, 0.6,
                true, 3, 9, 0.0, 75.0, true);
        check(!TrialStateMachine.inDangerZone(30, noCap),
                "a disabled cap (0) empties the zone, matching the governor");

        System.out.println("TrialStateMachineHardeningTest: " + passed + " passed, " + failed + " failed");
        if (failed > 0) {
            System.exit(1);
        }
    }
}

package com.winkreyes.excelbosstrial.dragon;

import com.winkreyes.excelbosstrial.dragon.BattlePhase;
import com.winkreyes.excelbosstrial.dragon.DamageGovernor;
import com.winkreyes.excelbosstrial.dragon.TrialConfig;
import com.winkreyes.excelbosstrial.dragon.TrialStateMachine;
import com.winkreyes.excelbosstrial.dragon.TrialStateMachine.Verdict;
import java.util.HashMap;
import java.util.Map;

/**
 * THE seal-integrity proof (SPEC Step 0).
 *
 * GuildGate 0.3.0 credits the ender_dragon seal from EntityDeathEvent
 * alone (ExcelGuildGate.java onEntityDeath, MONITOR). A death event
 * can only exist if the dragon's health reaches 0 through an
 * uncancelled damage path. This harness plays the adapter's role
 * exactly: PASS = damage applies, FLIP = event cancelled + dragon
 * healed to the phase-2 bar, ALLOW_DEATH = damage applies and the
 * dragon dies. It then asserts the only thing that matters:
 * a death is permitted ONLY in phase 2, and ONLY once.
 */
public final class NoFalseCreditTest {

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

    /** A tiny adapter simulation: hit the machine, apply its verdict. */
    private static final class SimFight {
        final TrialStateMachine m = new TrialStateMachine();
        double hp;
        int deaths;

        SimFight(double phase1Hp) {
            m.startBattle("dragon-1");
            hp = phase1Hp;
        }

        Verdict blow(double damage) {
            if (m.phase() == BattlePhase.TRANSITION) {
                // Adapter cancels ALL damage during the transition.
                return Verdict.PASS;
            }
            Verdict v = m.onDamage(hp, damage);
            switch (v) {
                case FLIP -> hp = 300; // healed onto the phase-2 bar, synchronously
                case ALLOW_DEATH -> {
                    hp = 0;
                    deaths++;
                }
                case PASS -> hp = Math.max(0, hp - damage);
            }
            return v;
        }
    }

    /**
     * The 0.1.1 adapter order, simulated exactly: TRANSITION cancels
     * before anything else; otherwise the governor judges the raw hit
     * first and the machine only ever sees the governed value.
     */
    private static final class GovernedFight {
        final TrialStateMachine m = new TrialStateMachine();
        final DamageGovernor gov = new DamageGovernor();
        final TrialConfig cfg;
        double hp;
        int deaths;
        long tick = 1;

        GovernedFight(TrialConfig cfg, double phase1Hp) {
            this.cfg = cfg;
            m.startBattle("dragon-cap");
            hp = phase1Hp;
        }

        Verdict blow(double damage, boolean explosion) {
            if (m.phase() == BattlePhase.TRANSITION) {
                // Adapter cancels ALL damage during the transition;
                // the governor never runs and the pool is untouched.
                return Verdict.PASS;
            }
            double judged = gov.govern(damage, explosion, tick, cfg).damage;
            Verdict v = m.onDamage(hp, judged);
            switch (v) {
                case FLIP -> hp = 300;
                case ALLOW_DEATH -> {
                    hp = 0;
                    deaths++;
                }
                case PASS -> hp = Math.max(0, hp - judged);
            }
            return v;
        }

        void nextTick() {
            tick++;
        }
    }

    public static void main(String[] args) {
        // --- A normal winning fight, start to finish ----------------
        SimFight f = new SimFight(250);
        check(f.blow(60) == Verdict.PASS && f.hp == 190, "phase1 chip damage passes");
        check(f.blow(500) == Verdict.FLIP, "phase1 overkill blow flips, never dies");
        check(f.deaths == 0, "no death at the flip");
        check(f.m.phase() == BattlePhase.TRANSITION, "machine in TRANSITION after flip");
        check(f.blow(9999) == Verdict.PASS && f.hp == 300 && f.deaths == 0,
                "transition blow is cancelled, dragon untouched");
        f.m.completeTransition();
        check(f.m.phase() == BattlePhase.PHASE2, "phase 2 after transition completes");
        check(f.blow(100) == Verdict.PASS && f.hp == 200, "phase2 chip damage passes");
        check(f.blow(250) == Verdict.ALLOW_DEATH, "phase2 lethal blow is the one real death");
        check(f.deaths == 1, "exactly one death in a full fight");
        check(f.blow(250) == Verdict.PASS && f.deaths == 1, "no second death can be permitted");

        // --- Two lethal blows in the same tick, phase 1 ------------
        SimFight g = new SimFight(250);
        check(g.blow(250) == Verdict.FLIP, "exact-lethal (damage == health) flips");
        check(g.blow(250) == Verdict.PASS && g.deaths == 0,
                "second same-tick lethal lands in TRANSITION and is cancelled");

        // --- Boundary: exact lethal in phase 2 ----------------------
        SimFight h = new SimFight(250);
        h.blow(300); // flip
        h.m.completeTransition();
        check(h.blow(300) == Verdict.ALLOW_DEATH && h.deaths == 1,
                "exact-lethal in phase 2 dies, once");

        // --- Untracked dragon: machine never invents verdicts ------
        TrialStateMachine idle = new TrialStateMachine();
        check(idle.onDamage(200, 9999) == Verdict.PASS, "no battle tracked -> PASS");
        check(idle.phase() == BattlePhase.NONE, "idle machine stays NONE");

        // --- Restart mid-phase-2 (state restored from disk) ---------
        TrialStateMachine r2 = new TrialStateMachine();
        r2.restore(BattlePhase.PHASE2, "dragon-1", 0, false, false);
        check(r2.onDamage(120, 999) == Verdict.ALLOW_DEATH,
                "restored phase 2 still permits its one death");
        check(r2.onDamage(120, 999) == Verdict.PASS,
                "restored phase 2 permits it only once");

        // --- Restart mid-transition (saved during the flip) --------
        TrialStateMachine rt = new TrialStateMachine();
        rt.restore(BattlePhase.TRANSITION, "dragon-1", 0, false, true);
        check(rt.phase() == BattlePhase.PHASE2, "restored TRANSITION resolves to PHASE2");
        check(rt.onDamage(300, 300) == Verdict.ALLOW_DEATH,
                "post-restart phase 2 death works (no soft-lock)");

        // --- Restart mid-phase-1 ------------------------------------
        TrialStateMachine r1 = new TrialStateMachine();
        r1.restore(BattlePhase.PHASE1, "dragon-1", 0, false, false);
        check(r1.onDamage(250, 250) == Verdict.FLIP, "restored phase 1 still flips, not dies");
        check(r1.onDamage(250, 250) == Verdict.PASS, "restored flip enters TRANSITION safely");

        // --- A second battle (crystal re-summon) starts clean -------
        SimFight s = new SimFight(250);
        s.blow(250);
        s.m.completeTransition();
        s.blow(300);
        check(s.deaths == 1, "first battle: one death");
        s.m.endBattle();
        s.m.startBattle("dragon-2");
        check(s.m.phase() == BattlePhase.PHASE1 && !s.m.deathConsumed(),
                "re-summoned dragon starts a fresh phase 1");
        check(s.m.onDamage(250, 250) == Verdict.FLIP,
                "re-summoned dragon flips instead of dying");

        // =============================================================
        // 0.1.1 — the seal proof UNDER the anti-nuke caps. The adapter
        // governs first and the machine judges governed damage only;
        // this sim plays that exact order.
        // =============================================================

        // --- Mace one-shots cannot flip, skip or kill phase 1 -------
        GovernedFight cap1 = new GovernedFight(TrialConfig.defaults(), 250);
        check(cap1.blow(10000, false) == Verdict.PASS && cap1.hp == 190,
                "capped phase 1: a 10000 smash is judged as 60, dragon at 190");
        check(cap1.m.phase() == BattlePhase.PHASE1 && cap1.deaths == 0,
                "capped phase 1: no flip, no death from one mega-hit");
        cap1.blow(10000, false);
        cap1.blow(10000, false);
        cap1.blow(10000, false);
        check(cap1.hp == 10 && cap1.deaths == 0, "four capped smashes leave 10 HP, still no death");
        check(cap1.blow(10000, false) == Verdict.FLIP && cap1.deaths == 0,
                "the capped blow that finally reaches lethal flips, exactly once");
        check(cap1.blow(10000, true) == Verdict.PASS && cap1.hp == 300 && cap1.deaths == 0,
                "transition still eats everything, caps or not");

        // --- Capped phase 2 still yields the ONE genuine death ------
        cap1.m.completeTransition();
        check(cap1.blow(70, true) == Verdict.PASS && cap1.hp == 240,
                "phase 2: first bed judged 60 (pool 15 left)");
        check(cap1.blow(70, true) == Verdict.PASS && cap1.hp == 225,
                "phase 2: second same-tick bed judged at the pooled 15");
        check(cap1.blow(70, true) == Verdict.PASS && cap1.hp == 225,
                "phase 2: third same-tick bed judged 0 - the tick is spent");
        cap1.nextTick();
        cap1.blow(60, false);
        cap1.blow(60, false);
        cap1.blow(60, false);
        check(cap1.hp == 45 && cap1.deaths == 0, "honest capped DPS grinds phase 2 to 45");
        check(cap1.blow(60, false) == Verdict.ALLOW_DEATH && cap1.deaths == 1,
                "a capped lethal blow in phase 2 is the one real death");
        check(cap1.blow(60, false) == Verdict.PASS && cap1.deaths == 1,
                "and it can still happen only once");

        // --- Budget alone cannot stall a kill forever -----------------
        GovernedFight grind = new GovernedFight(TrialConfig.defaults(), 250);
        for (int i = 0; i < 5; i++) {
            grind.blow(10000, false); // capped to 60 each: 250 -> 190 -> 130 -> 70 -> 10 -> FLIP
        }
        check(grind.m.phase() == BattlePhase.TRANSITION && grind.deaths == 0,
                "grind: phase 1 flipped by honest capped pressure, no death");
        grind.m.completeTransition();
        int guard = 0;
        while (grind.deaths == 0 && guard++ < 20) {
            grind.blow(70, true);
            grind.blow(70, true);
            grind.blow(70, true);
            grind.nextTick();
        }
        check(grind.deaths == 1, "bed-only pressure across ticks still earns the one death");

        // --- Exact-lethal at the cap value still counts ---------------
        GovernedFight exact = new GovernedFight(TrialConfig.defaults(), 250);
        for (int i = 0; i < 5; i++) {
            exact.blow(10000, false); // capped grind to the flip
        }
        exact.m.completeTransition();
        exact.blow(60, false);
        exact.blow(60, false);
        exact.blow(60, false);
        exact.blow(60, false);
        check(exact.hp == 60, "phase 2 ground to exactly the cap value");
        check(exact.blow(60, false) == Verdict.ALLOW_DEATH && exact.deaths == 1,
                "a hit of exactly the cap (60) is lethal and untrimmed");

        // --- Caps OFF = 0.1.0 behaviour, bit for bit -------------------
        Map<String, Object> offMap = new HashMap<>();
        offMap.put("caps.per-event-damage", 0);
        offMap.put("caps.explosion-per-tick", 0);
        GovernedFight off = new GovernedFight(TrialConfig.fromMap(offMap), 250);
        check(off.blow(10000, false) == Verdict.FLIP && off.deaths == 0,
                "caps off: a phase-1 overkill flips (0.1.0 behaviour)");
        off.m.completeTransition();
        check(off.blow(10000, false) == Verdict.ALLOW_DEATH && off.deaths == 1,
                "caps off: a phase-2 overkill is the one death (0.1.0 behaviour)");

        System.out.println("NoFalseCreditTest: " + passed + " passed, " + failed + " failed");
        if (failed > 0) {
            System.exit(1);
        }
    }
}


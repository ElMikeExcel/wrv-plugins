package com.winkreyes.excelbosstrial.dragon;

import com.winkreyes.excelbosstrial.dragon.TrialConfig;
import java.util.HashMap;
import java.util.Map;

/** Shipped defaults match SPEC.md; bad config clamps toward vanilla. */
public final class TrialConfigTest {

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
        TrialConfig d = TrialConfig.defaults();
        check(d.enabled && d.applyToResummons, "defaults: enabled + resummons on");
        check(d.phase1MaxHealth == 250.0, "default phase1 HP 250 (vanilla 200 + 25%)");
        check(d.phase2MaxHealth == 500.0, "default phase2 HP 500 (Mike, Oct 19, 2026: 250 + 500 = 750 total)");
        check(d.phase2DamageMultiplier == 1.5, "default damage multiplier 1.5");
        check(d.invulnerableSeconds == 3, "default invulnerable window 3s");
        check(d.aggressionIntervalSeconds == 7, "default aggression interval 7s");
        check(d.aggressionChance == 0.6, "default aggression chance 0.6");
        check(d.addsEnabled && d.addsPerWave == 3 && d.addsMaxAlive == 9, "default adds 3/wave, cap 9");

        Map<String, Object> over = new HashMap<>();
        over.put("phase1.max-health", 400);
        over.put("phase2.damage-multiplier", 2.0);
        over.put("enabled", false);
        TrialConfig c = TrialConfig.fromMap(over);
        check(c.phase1MaxHealth == 400.0 && c.phase2DamageMultiplier == 2.0 && !c.enabled,
                "fromMap applies overrides");
        check(c.phase2MaxHealth == 500.0, "fromMap keeps defaults for absent keys");

        Map<String, Object> junk = new HashMap<>();
        junk.put("phase2.damage-multiplier", 99.0);
        junk.put("transition.invulnerable-seconds", 99);
        junk.put("phase1.max-health", Double.NaN);
        junk.put("phase2.aggression-chance", 7.0);
        TrialConfig j = TrialConfig.fromMap(junk);
        check(j.phase2DamageMultiplier == 10.0, "multiplier clamps to 10");
        check(j.invulnerableSeconds == 30, "invulnerable window clamps to 30s");
        check(j.phase1MaxHealth == 1.0, "NaN health clamps to floor, never crashes");
        check(j.aggressionChance == 1.0, "chance clamps to 1");

        check(TrialConfig.fromMap(null).phase1MaxHealth == 250.0, "null map -> defaults");

        // --- 0.1.1 caps ------------------------------------------------
        check(d.perEventDamageCap == 60.0 && d.perEventCapEnabled(), "default P1 cap 60, armed");
        check(d.explosionTickBudget == 75.0 && d.explosionBudgetEnabled(),
                "default P2 budget 75, armed");
        check(d.blockCrystalPlacement, "default P3 crystal lock on");

        Map<String, Object> caps = new HashMap<>();
        caps.put("caps.per-event-damage", 45);
        caps.put("caps.explosion-per-tick", 90);
        caps.put("caps.block-crystal-placement", false);
        TrialConfig cc = TrialConfig.fromMap(caps);
        check(cc.perEventDamageCap == 45.0 && cc.explosionTickBudget == 90.0
                && !cc.blockCrystalPlacement, "fromMap applies cap overrides");

        Map<String, Object> off = new HashMap<>();
        off.put("caps.per-event-damage", 0);
        off.put("caps.explosion-per-tick", -5);
        TrialConfig oc = TrialConfig.fromMap(off);
        check(oc.perEventDamageCap == 0.0 && !oc.perEventCapEnabled(),
                "cap of 0 disables P1 (fail-soft off)");
        check(oc.explosionTickBudget == 0.0 && !oc.explosionBudgetEnabled(),
                "negative budget disables P2 (fail-soft off)");

        Map<String, Object> nan = new HashMap<>();
        nan.put("caps.per-event-damage", Double.NaN);
        check(TrialConfig.fromMap(nan).perEventDamageCap == 0.0,
                "NaN cap disables the rule, never crashes");

        System.out.println("TrialConfigTest: " + passed + " passed, " + failed + " failed");
        if (failed > 0) {
            System.exit(1);
        }
    }
}

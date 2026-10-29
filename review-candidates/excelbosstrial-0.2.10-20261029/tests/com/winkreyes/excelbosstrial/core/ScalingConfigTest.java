/* Excel WRV maintenance; existing authorship retained. */ package com.winkreyes.excelbosstrial.core;

import java.util.HashMap;
import java.util.Map;

/**
 * The 0.2 scaling config surface: fromMap carries the settled
 * defaults, per-chapter overrides land, the off switches work,
 * and junk degrades toward the defaults instead of crashing.
 */
public final class ScalingConfigTest {

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

    private static void eq(double actual, double expected, String label) {
        check(Math.abs(actual - expected) < 1e-9,
                label + " (expected " + expected + ", got " + actual + ")");
    }

    public static void main(String[] args) {
        // Defaults (empty map): the settled spec numbers.
        BossTrialConfig d = BossTrialConfig.fromMap(Map.of());
        eq(d.scaling(BossKind.WITHER).baseHealth(), 750, "default wither base 750");
        eq(d.scaling(BossKind.WITHER).perExtraDamager(), 200, "default wither step 200");
        eq(d.scaling(BossKind.ELDER_GUARDIAN).baseHealth(), 400, "default elder base 400");
        eq(d.scaling(BossKind.ELDER_GUARDIAN).perExtraDamager(), 50, "default elder step 50");
        eq(d.scaling(BossKind.WARDEN).baseHealth(), 800, "default warden base 800");
        eq(d.scaling(BossKind.WARDEN).perExtraDamager(), 150, "default warden step 150");
        check(d.scaling(BossKind.WARDEN).maxDamagersCounted() == 5, "default damager cap 5");
        check(d.scaling(BossKind.ENDER_DRAGON) == ScalingRule.OFF, "dragon scaling always OFF");
        check(d.scaling(BossKind.WITHER).active(), "default wither scaling active");

        // The pre-0.2 constructor shapes still produce the settled
        // scaling defaults (an old config without scaling keys is
        // the 0.2 fight - the off switch is scaling.enabled=false).
        BossTrialConfig legacy = BossTrialConfig.defaults();
        eq(legacy.scaling(BossKind.WITHER).baseHealth(), 750, "defaults() carries scaling");

        // Overrides land per chapter and touch nothing else.
        Map<String, Object> m = new HashMap<>();
        m.put("wither.scaling.base-health", 900);
        m.put("wither.scaling.per-extra-damager", 250);
        m.put("warden.scaling.enabled", false);
        m.put("elder-guardian.scaling.max-damagers-counted", 3);
        BossTrialConfig c = BossTrialConfig.fromMap(m);
        eq(c.scaling(BossKind.WITHER).baseHealth(), 900, "wither base override lands");
        eq(c.scaling(BossKind.WITHER).perExtraDamager(), 250, "wither step override lands");
        eq(c.scaling(BossKind.WITHER).poolFor(2), 1150, "override pool math follows");
        check(!c.scaling(BossKind.WARDEN).active(), "warden scaling off switch works");
        eq(c.scaling(BossKind.WARDEN).baseHealth(), 800, "warden numbers stay while off");
        check(c.scaling(BossKind.ELDER_GUARDIAN).maxDamagersCounted() == 3,
                "elder damager cap override lands");
        eq(c.scaling(BossKind.ELDER_GUARDIAN).poolFor(5), 500,
                "elder pool respects the lower damager cap (400 + 2 steps)");

        // Junk values degrade: wrong types fall back to defaults,
        // out-of-range numbers clamp inside ScalingRule.
        Map<String, Object> junk = new HashMap<>();
        junk.put("wither.scaling.base-health", "lots");
        junk.put("warden.scaling.base-health", -5);
        BossTrialConfig j = BossTrialConfig.fromMap(junk);
        eq(j.scaling(BossKind.WITHER).baseHealth(), 750, "string base falls back to default");
        eq(j.scaling(BossKind.WARDEN).baseHealth(), 0, "negative base clamps to 0 (inactive)");

        System.out.println("ScalingConfigTest: " + passed + " passed, " + failed + " failed");
        if (failed > 0) {
            System.exit(1);
        }
    }
}

// ExcelBossTrial - a Wink Reyes Valley Excel plugin.


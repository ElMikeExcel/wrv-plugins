package com.winkreyes.excelraidbuff.core;

import java.util.HashMap;
import java.util.Map;

/**
 * The scaling table (Mike, Oct 20, 2026 late): Omen I is vanilla
 * end to end, the multipliers climb with the omen, the late-wave
 * HP ramp leans only on the final waves of a buffed raid, and
 * every degenerate input degrades to identity — never a crash,
 * never a weakened raider.
 */
public final class ScalingTableTest {

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
        RaidBuffConfig cfg = RaidBuffConfig.defaults();

        // Omen I: identity at every wave, final wave included.
        for (int wave = 1; wave <= 8; wave++) {
            ScalingTable.Factors f = ScalingTable.factors(cfg, 1, wave, 8);
            eq(f.health(), 1.0, "omen I wave " + wave + " hp");
            eq(f.damage(), 1.0, "omen I wave " + wave + " dmg");
            check(!f.active(), "omen I wave " + wave + " inactive");
            check(!f.lateWave(), "omen I wave " + wave + " not late");
        }

        // The settled columns, mid-raid waves (no late ramp).
        eq(ScalingTable.factors(cfg, 2, 1, 5).health(), 1.25, "omen II hp");
        eq(ScalingTable.factors(cfg, 2, 1, 5).damage(), 1.08, "omen II dmg");
        eq(ScalingTable.factors(cfg, 3, 2, 6).health(), 1.50, "omen III hp");
        eq(ScalingTable.factors(cfg, 3, 2, 6).damage(), 1.15, "omen III dmg");
        eq(ScalingTable.factors(cfg, 4, 3, 7).health(), 1.80, "omen IV hp");
        eq(ScalingTable.factors(cfg, 4, 3, 7).damage(), 1.22, "omen IV dmg");
        eq(ScalingTable.factors(cfg, 5, 1, 8).health(), 2.20, "omen V hp");
        eq(ScalingTable.factors(cfg, 5, 1, 8).damage(), 1.30, "omen V dmg");
        check(ScalingTable.factors(cfg, 5, 1, 8).active(), "omen V active");

        // Late-wave ramp: final 2 waves only, HP only, at Omen V of 8.
        ScalingTable.Factors w6 = ScalingTable.factors(cfg, 5, 6, 8);
        check(!w6.lateWave(), "omen V wave 6 of 8 not late");
        eq(w6.health(), 2.20, "omen V wave 6 hp unramped");
        for (int wave = 7; wave <= 8; wave++) {
            ScalingTable.Factors f = ScalingTable.factors(cfg, 5, wave, 8);
            check(f.lateWave(), "omen V wave " + wave + " of 8 late");
            eq(f.health(), 2.20 * 1.15, "omen V wave " + wave + " hp ramped");
            eq(f.damage(), 1.30, "omen V wave " + wave + " dmg never ramps");
        }
        // A shorter raid (Omen II, 5 waves): waves 4-5 are late.
        check(ScalingTable.factors(cfg, 2, 4, 5).lateWave(), "omen II wave 4 of 5 late");
        eq(ScalingTable.factors(cfg, 2, 5, 5).health(), 1.25 * 1.15, "omen II final wave hp ramped");
        check(!ScalingTable.factors(cfg, 2, 3, 5).lateWave(), "omen II wave 3 of 5 not late");

        // Degenerate inputs degrade to identity.
        check(ScalingTable.factors(cfg, 0, 1, 8) == ScalingTable.Factors.IDENTITY, "omen 0 identity");
        check(ScalingTable.factors(cfg, 6, 1, 8) == ScalingTable.Factors.IDENTITY, "omen 6 identity");
        check(ScalingTable.factors(null, 5, 1, 8) == ScalingTable.Factors.IDENTITY, "null config identity");
        eq(ScalingTable.factors(cfg, 5, 0, 8).health(), 2.20, "wave 0 treated as wave 1");
        check(!ScalingTable.factors(cfg, 5, 8, 0).lateWave(), "totalWaves 0 disables the ramp");

        // Disabled plugin: identity even at Omen V.
        Map<String, Object> off = new HashMap<>();
        off.put("enabled", false);
        check(ScalingTable.factors(RaidBuffConfig.fromMap(off), 5, 8, 8)
                == ScalingTable.Factors.IDENTITY, "disabled config identity");

        // The ramp is gated on the raid being HP-buffed at all: a
        // damage-only retune (HP column flattened to 1.0) must not
        // resurrect HP on the final waves.
        Map<String, Object> dmgOnly = new HashMap<>();
        Map<String, Object> scaling = new HashMap<>();
        Map<String, Object> hp = new HashMap<>();
        hp.put("5", 1.0);
        Map<String, Object> dmg = new HashMap<>();
        dmg.put("5", 1.30);
        scaling.put("health-multiplier", hp);
        scaling.put("damage-multiplier", dmg);
        dmgOnly.put("scaling", scaling);
        ScalingTable.Factors f = ScalingTable.factors(RaidBuffConfig.fromMap(dmgOnly), 5, 8, 8);
        eq(f.health(), 1.0, "damage-only retune: final-wave hp stays vanilla");
        check(!f.lateWave(), "damage-only retune: no late flag");
        check(f.active(), "damage-only retune: still active via damage");

        System.out.println("ScalingTableTest: " + passed + " passed, " + failed + " failed");
        if (failed > 0) System.exit(1);
    }
}

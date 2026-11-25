/* Excel WRV maintenance; existing authorship retained. */ package com.winkreyes.excelbosstrial.core;

import java.util.Map;

/**
 * Item 39 R1 as shipped in 0.5.3 (Nov 9, 2026): the sonic-boom
 * pressure exemption. Pins (a) the SonicBoomRule truth table -
 * only (SONIC_BOOM cause, WARDEN kind, key off) is exempt;
 * (b) the config key {@code <chapter>.pressure.apply-to-sonic-boom}
 * through the same fromMap path as every other pressure key:
 * shipped default FALSE everywhere (0.5.3 files the retune as
 * the default behavior - the R1 test build's conservative TRUE
 * default is deliberately reversed for the shipped filing), an
 * absent key parses false, an explicit true lands on its own
 * chapter only, and the R2 neighbor values are undisturbed.
 */
public final class SonicBoomRuleTest {

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
        // (a) Truth table.
        check(SonicBoomRule.isExempt(true, BossKind.WARDEN, false),
                "boom + warden + key off: exempt");
        check(!SonicBoomRule.isExempt(true, BossKind.WARDEN, true),
                "boom + warden + key on: scales (pre-0.5.3 behavior)");
        check(!SonicBoomRule.isExempt(false, BossKind.WARDEN, false),
                "melee cause + warden + key off: still scales");
        check(!SonicBoomRule.isExempt(true, BossKind.WITHER, false),
                "boom + wither + key off: not exempt (chapter-local)");
        check(!SonicBoomRule.isExempt(true, BossKind.ELDER_GUARDIAN, false),
                "boom + elder + key off: not exempt");
        check(!SonicBoomRule.isExempt(true, BossKind.ENDER_DRAGON, false),
                "boom + dragon + key off: not exempt");
        check(!SonicBoomRule.isExempt(true, null, false),
                "boom + no kind + key off: not exempt");
        check(!SonicBoomRule.isExempt(false, BossKind.WARDEN, true),
                "melee + warden + key on: scales");

        // (b) Settled defaults: off for every chapter (the
        // retune ships as the default behavior).
        for (BossKind kind : BossKind.values()) {
            check(!BossTrialConfig.Pressure.settled(kind).applyToSonicBoom(),
                    "settled default off (retune shipped): " + kind);
        }
        check(!BossTrialConfig.Pressure.OFF.applyToSonicBoom(),
                "Pressure.OFF carries the shipped default (false)");

        // Absent key (every pre-0.5.3 live config): off everywhere.
        BossTrialConfig absent = BossTrialConfig.fromMap(Map.of());
        for (BossKind kind : BossKind.values()) {
            check(!absent.pressure(kind).applyToSonicBoom(),
                    "key absent parses off: " + kind);
        }

        // Explicit on lands on its own chapter only (the opt-back-in).
        BossTrialConfig wardenOn = BossTrialConfig.fromMap(Map.of(
                "warden.pressure.apply-to-sonic-boom", true));
        check(wardenOn.pressure(BossKind.WARDEN).applyToSonicBoom(),
                "warden key true: warden back on the scale");
        check(!wardenOn.pressure(BossKind.WITHER).applyToSonicBoom(),
                "warden key true: wither still off");
        check(!wardenOn.pressure(BossKind.ELDER_GUARDIAN).applyToSonicBoom(),
                "warden key true: elder still off");
        check(!wardenOn.pressure(BossKind.ENDER_DRAGON).applyToSonicBoom(),
                "warden key true: dragon still off");

        // The R2 candidate values ride undisturbed beside the new key.
        BossTrialConfig candidate = BossTrialConfig.fromMap(Map.of(
                "warden.pressure.apply-to-sonic-boom", false,
                "warden.pressure.outgoing-multiplier", 1.0,
                "warden.pressure.ramp-max", 1.5));
        check(Math.abs(candidate.pressure(BossKind.WARDEN).outgoingMultiplier() - 1.0) < 1e-9,
                "candidate: warden outgoing-multiplier 1.0 undisturbed");
        check(Math.abs(candidate.pressure(BossKind.WARDEN).rampMax() - 1.5) < 1e-9,
                "candidate: warden ramp-max 1.5 undisturbed");
        check(Math.abs(candidate.pressure(BossKind.WARDEN).rampPerMinute() - 0.06) < 1e-9,
                "candidate: warden ramp-per-minute 0.06 default undisturbed");

        System.out.println("SonicBoomRuleTest: " + passed + " passed, " + failed + " failed");
        if (failed > 0) {
            System.exit(1);
        }
    }
}

// ExcelBossTrial - a Wink Reyes Valley Excel plugin.

/* Excel WRV maintenance; existing authorship retained. */ package com.winkreyes.excelbosstrial.core;

import java.util.Map;

/**
 * The pulse-respects-TrainingWheels key (0.3.7, TW-PRESSURE-CHECK
 * fix shape 1): per chapter at
 * {@code <chapter>.pressure.pulse-respect-training-wheels},
 * settled default TRUE - the fix is on unless an owner turns it
 * off - parsed through the same fromMap path as every other
 * pressure key, so a live config that predates the key gets the
 * fixed behavior with no edit.
 */
public final class PulseRespectConfigTest {

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
        // Settled defaults: on for every chapter.
        for (BossKind kind : BossKind.values()) {
            check(BossTrialConfig.Pressure.settled(kind).pulseRespectTrainingWheels(),
                    "settled default on: " + kind);
        }
        check(BossTrialConfig.Pressure.OFF.pulseRespectTrainingWheels(),
                "Pressure.OFF carries the inert default (true)");

        // Absent key (every pre-0.3.7 live config): on everywhere.
        BossTrialConfig absent = BossTrialConfig.fromMap(Map.of());
        for (BossKind kind : BossKind.values()) {
            check(absent.pressure(kind).pulseRespectTrainingWheels(),
                    "key absent parses on: " + kind);
        }

        // Explicit off lands on its own chapter only.
        BossTrialConfig witherOff = BossTrialConfig.fromMap(Map.of(
                "wither.pressure.pulse-respect-training-wheels", false));
        check(!witherOff.pressure(BossKind.WITHER).pulseRespectTrainingWheels(),
                "wither key false: wither off");
        check(witherOff.pressure(BossKind.WARDEN).pulseRespectTrainingWheels(),
                "wither key false: warden still on");
        check(witherOff.pressure(BossKind.ELDER_GUARDIAN).pulseRespectTrainingWheels(),
                "wither key false: elder still on");
        check(witherOff.pressure(BossKind.ENDER_DRAGON).pulseRespectTrainingWheels(),
                "wither key false: dragon still on");

        // Explicit on parses on; the other pressure values in the
        // same block are undisturbed by the new key's presence.
        BossTrialConfig explicit = BossTrialConfig.fromMap(Map.of(
                "warden.pressure.pulse-respect-training-wheels", true,
                "warden.pressure.pulse-damage", 3.5));
        check(explicit.pressure(BossKind.WARDEN).pulseRespectTrainingWheels(),
                "warden key true: on");
        check(Math.abs(explicit.pressure(BossKind.WARDEN).pulseDamage() - 3.5) < 1e-9,
                "neighboring pulse-damage undisturbed");

        System.out.println("PulseRespectConfigTest: " + passed + " passed, " + failed + " failed");
        if (failed > 0) {
            System.exit(1);
        }
    }
}

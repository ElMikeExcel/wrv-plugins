/* Excel WRV maintenance; existing authorship retained. */ package com.winkreyes.excelbosstrial.core;

import java.util.Map;

/**
 * The 0.3.5 config surface: the elder reflect key parses from
 * the flat map BossTrialConfig consumes, the settled default is
 * Mike's Nov 2, 2026 ruling (0.0 - reflect retired for seal
 * elders), a config missing the block runs the settled default,
 * and no other chapter grows a reflect block.
 */
public final class ReflectConfigTest {

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
        // Settled default, no config at all.
        BossTrialConfig d = BossTrialConfig.defaults();
        check(d.elderReflect.multiplier() == 0.0,
                "settled default: elder reflect multiplier is 0.0 (Mike, Nov 2, 2026)");
        check(d.reflect(BossKind.ELDER_GUARDIAN).multiplier() == 0.0,
                "reflect(elder) reads the elder block");
        check(d.reflect(BossKind.WITHER).multiplier() == 1.0,
                "reflect(wither) is the neutral 1.0 - no reflect block on that chapter");
        check(d.reflect(BossKind.WARDEN).multiplier() == 1.0,
                "reflect(warden) is the neutral 1.0");
        check(d.reflect(BossKind.ENDER_DRAGON).multiplier() == 1.0,
                "reflect(dragon) is the neutral 1.0");
        check(BossTrialConfig.Reflect.settled().multiplier() == 0.0,
                "Reflect.settled() is 0.0");

        // The flat key parses.
        BossTrialConfig tuned = BossTrialConfig.fromMap(
                Map.of("elder-guardian.reflect.multiplier", 0.25));
        check(tuned.elderReflect.multiplier() == 0.25,
                "elder-guardian.reflect.multiplier parses from the flat map");

        // A config missing the block runs the settled default,
        // exactly as pressure does.
        BossTrialConfig absent = BossTrialConfig.fromMap(Map.of("enabled", true));
        check(absent.elderReflect.multiplier() == 0.0,
                "missing reflect block: settled default 0.0 applies");

        // Vanilla remains one config edit away.
        BossTrialConfig vanilla = BossTrialConfig.fromMap(
                Map.of("elder-guardian.reflect.multiplier", 1.0));
        check(vanilla.elderReflect.multiplier() == 1.0,
                "multiplier 1.0 (exact vanilla) parses and is kept");

        System.out.println("ReflectConfigTest: " + passed + " passed, " + failed + " failed");
        if (failed > 0) {
            System.exit(1);
        }
    }
}

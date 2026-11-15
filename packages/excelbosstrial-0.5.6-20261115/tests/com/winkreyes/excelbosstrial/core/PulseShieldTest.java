/* Excel WRV maintenance; existing authorship retained. */ package com.winkreyes.excelbosstrial.core;

/**
 * Pulse shield arithmetic (0.3.7, TW-PRESSURE-CHECK fix shape 1):
 * the protected player's quarter holds against the pulse, an
 * unprotected player takes the full amount, and a garbage factor
 * read can neither amplify the pulse nor zero it by accident.
 */
public final class PulseShieldTest {

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

    private static void close(double actual, double expected, String label) {
        check(Math.abs(actual - expected) < 1e-9,
                label + " (expected " + expected + ", got " + actual + ")");
    }

    public static void main(String[] args) {
        // The family-Wither case: pulse 8.0 on the rig, Rylie's 0.25.
        close(PulseShield.shieldedAmount(8.0, 0.25), 2.0, "protected: 8.0 x 0.25 = 2.0");
        close(PulseShield.shieldedAmount(2.0, 0.25), 0.5, "live pulse at minute 0: 2.0 x 0.25 = 0.5");
        close(PulseShield.shieldedAmount(4.0, 0.25), 1.0, "live pulse at ramp cap: 4.0 x 0.25 = 1.0");

        // Unprotected / no shield: the full amount, untouched.
        close(PulseShield.shieldedAmount(8.0, 1.0), 8.0, "factor 1.0: full pulse");

        // A genuine factor of 0 is registry-valid and stays 0.
        close(PulseShield.shieldedAmount(8.0, 0.0), 0.0, "factor 0.0: no pulse");

        // Garbage factors fail soft to NO shield - never amplification,
        // never an accidental zero.
        close(PulseShield.shieldedAmount(8.0, Double.NaN), 8.0, "NaN factor: full pulse");
        close(PulseShield.shieldedAmount(8.0, -0.5), 8.0, "negative factor: full pulse");
        close(PulseShield.shieldedAmount(8.0, 1.7), 8.0, "factor > 1: full pulse, never amplified");
        close(PulseShield.shieldedAmount(8.0, Double.POSITIVE_INFINITY), 8.0,
                "infinite factor: full pulse");

        // Amount hygiene (PressureMath's rules).
        close(PulseShield.shieldedAmount(Double.NaN, 0.25), 0.0, "NaN amount: no pulse");
        close(PulseShield.shieldedAmount(-3.0, 0.25), 0.0, "negative amount: no pulse");
        close(PulseShield.shieldedAmount(0.0, 0.25), 0.0, "zero amount: zero");

        // sanitizeFactor itself.
        close(PulseShield.sanitizeFactor(0.25), 0.25, "sanitize keeps 0.25");
        close(PulseShield.sanitizeFactor(0.0), 0.0, "sanitize keeps 0.0");
        close(PulseShield.sanitizeFactor(1.0), 1.0, "sanitize keeps 1.0");
        close(PulseShield.sanitizeFactor(1.0000001), 1.0, "sanitize clamps >1 to 1.0");
        close(PulseShield.sanitizeFactor(Double.NaN), 1.0, "sanitize NaN to 1.0");

        System.out.println("PulseShieldTest: " + passed + " passed, " + failed + " failed");
        if (failed > 0) {
            System.exit(1);
        }
    }
}

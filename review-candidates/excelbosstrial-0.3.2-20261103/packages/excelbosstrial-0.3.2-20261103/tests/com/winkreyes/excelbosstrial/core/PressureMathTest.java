/* Excel WRV maintenance; existing authorship retained. */ package com.winkreyes.excelbosstrial.core;

/**
 * Attrition pressure (0.3.0, SPEC section 3, epic retarget):
 * the ramp 0.06/min to a x2.0 cap reaches peak at about minute
 * 17 and HOLDS - a 25-minute fight must not go soft after
 * minute 10, and the pulse rides the same ramp.
 */
public final class PressureMathTest {

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
        // Ramp shape.
        close(PressureMath.rampFactor(0, 0.06, 2.0), 1.0, "minute 0: no ramp");
        close(PressureMath.rampFactor(10, 0.06, 2.0), 1.6, "minute 10: x1.6");
        close(PressureMath.rampFactor(50.0 / 3.0, 0.06, 2.0), 2.0, "minute ~16.7: peak reached");
        close(PressureMath.rampFactor(25, 0.06, 2.0), 2.0, "minute 25: peak HOLDS (epic duration)");
        close(PressureMath.rampFactor(60, 0.06, 2.0), 2.0, "minute 60: still capped, never beyond");
        close(PressureMath.rampFactor(10, 0.0, 2.0), 1.0, "ramp 0 disables the ramp");
        close(PressureMath.rampFactor(-5, 0.06, 2.0), 1.0, "negative elapsed reads as minute 0");
        close(PressureMath.rampFactor(10, 0.06, 0.5), 1.0,
                "a sub-1 ramp cap clamps the cap to 1: the ramp is fully suppressed");

        // Outgoing composition: chapter multiplier under the ramp.
        close(PressureMath.outgoingScale(1.25, 0, 0.06, 2.0), 1.25, "wither at minute 0: x1.25");
        close(PressureMath.outgoingScale(1.25, 25, 0.06, 2.0), 2.5, "wither at peak: x2.5");
        close(PressureMath.outgoingScale(1.0, 25, 0.06, 2.0), 2.0, "elder at peak: x2.0");
        close(PressureMath.outgoingScale(0, 25, 0.06, 2.0), 0.0, "multiplier 0 mutes outgoing (config kill-switch)");

        // Pulse rides the ramp too.
        close(PressureMath.pulseAmount(2.0, 0, 0.06, 2.0), 2.0, "pulse at minute 0: 2.0");
        close(PressureMath.pulseAmount(2.0, 25, 0.06, 2.0), 4.0, "pulse at peak: 4.0");

        // Hot + due.
        check(!PressureMath.hot(0, 0, 100_000, 30_000), "no evidence ever: not hot");
        check(PressureMath.hot(90_000, 0, 100_000, 30_000), "counted blow 10 s ago: hot");
        check(!PressureMath.hot(10_000, 0, 100_000, 30_000), "evidence 90 s old: cold");
        check(PressureMath.hot(0, 95_000, 100_000, 30_000), "taken blow 5 s ago: hot");
        check(!PressureMath.pulseDue(10_000, 0, 0, 100_000, 20_000, 30_000),
                "cold fight: no pulse even if never pulsed");
        check(PressureMath.pulseDue(90_000, 0, 0, 100_000, 20_000, 30_000),
                "hot + never pulsed: due");
        check(!PressureMath.pulseDue(90_000, 0, 95_000, 100_000, 20_000, 30_000),
                "hot + pulsed 5 s ago on a 20 s interval: not due");
        check(PressureMath.pulseDue(90_000, 0, 75_000, 100_000, 20_000, 30_000),
                "hot + pulsed 25 s ago: due");

        System.out.println("PressureMathTest: " + passed + " passed, " + failed + " failed");
        if (failed > 0) {
            System.exit(1);
        }
    }
}

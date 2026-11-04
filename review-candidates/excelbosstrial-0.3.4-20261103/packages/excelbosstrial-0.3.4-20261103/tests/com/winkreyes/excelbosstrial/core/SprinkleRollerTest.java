/* Excel WRV maintenance; existing authorship retained. */ package com.winkreyes.excelbosstrial.core;

import java.util.List;
import java.util.Random;

/**
 * Elite sprinkle roll logic (0.3.3, study section 3): stamp
 * multipliers, the 0.90 ceiling, caps beating chance, partial
 * waves, passed rolls spent (never banked), and the per-fight
 * cap ending the rolls outright. A rigged Random makes every
 * roll deterministic.
 */
public final class SprinkleRollerTest {

    private static int checks;
    private static int failures;

    private static void check(boolean ok, String label) {
        checks++;
        if (!ok) {
            failures++;
            System.out.println("FAIL: " + label);
        }
    }

    private static void eqD(double actual, double expected, String label) {
        check(Math.abs(actual - expected) < 1e-9,
                label + " (expected " + expected + ", got " + actual + ")");
    }

    /** A Random whose nextDouble is a fixed script value. */
    private static Random rigged(double v) {
        return new Random() {
            @Override
            public double nextDouble() {
                return v;
            }
        };
    }

    public static void main(String[] args) {
        // --- effective chance: base x stamp multiplier, ceiling-clamped
        eqD(SprinkleRoller.effectiveChance(0.35, 0.75, 0.90), 0.2625,
                "wither 75% stamp: 0.35 x 0.75");
        eqD(SprinkleRoller.effectiveChance(0.35, 1.0, 0.90), 0.35,
                "wither 50% stamp: base chance");
        eqD(SprinkleRoller.effectiveChance(0.35, 1.25, 0.90), 0.4375,
                "wither 25% stamp: 0.35 x 1.25");
        eqD(SprinkleRoller.effectiveChance(0.40, 1.25, 0.90), 0.50,
                "warden 25% stamp: 0.40 x 1.25");
        eqD(SprinkleRoller.effectiveChance(0.85, 1.25, 0.90), 0.90,
                "ceiling clamps 0.85 x 1.25 to 0.90");
        eqD(SprinkleRoller.effectiveChance(1.0, 1.25, 0.90), 0.90,
                "ceiling clamps chance 1.0 x 1.25 to 0.90");
        eqD(SprinkleRoller.effectiveChance(Double.NaN, 1.0, 0.90), 0.0,
                "NaN base reads as 0 (fail-dark)");

        // --- stamp multiplier lookup and its fallbacks
        List<Double> mults = SprinkleSettings.DEFAULT_STAMP_MULTIPLIERS;
        eqD(SprinkleRoller.stampMultiplier(mults, 0), 0.75, "stamp 0 multiplier 0.75");
        eqD(SprinkleRoller.stampMultiplier(mults, 1), 1.0, "stamp 1 multiplier 1.0");
        eqD(SprinkleRoller.stampMultiplier(mults, 2), 1.25, "stamp 2 multiplier 1.25");
        eqD(SprinkleRoller.stampMultiplier(List.of(), 2), 1.25,
                "empty list falls back at the index");
        eqD(SprinkleRoller.stampMultiplier(null, 0), 0.75, "null list falls back");
        eqD(SprinkleRoller.stampMultiplier(List.of(2.0), 1), 1.0,
                "short list falls back past its end");
        eqD(SprinkleRoller.stampMultiplier(List.of(-1.0, 1.0, 1.0), 0), 0.75,
                "negative entry falls back");

        // --- a pass spawns the wave; a fail spawns nothing and stays silent
        SprinkleRoller.Decision pass = SprinkleRoller.decide(
                rigged(0.10), 0.35, 1.0, 0.90, 2, 0, 3, 0, 6);
        check(pass.rolled() && pass.passed() && pass.spawnCount() == 2,
                "roll 0.10 under 0.35 passes and spawns the full wave of 2");
        SprinkleRoller.Decision fail = SprinkleRoller.decide(
                rigged(0.50), 0.35, 1.0, 0.90, 2, 0, 3, 0, 6);
        check(fail.rolled() && !fail.passed() && fail.spawnCount() == 0,
                "roll 0.50 over 0.35 fails, spawns nothing");
        SprinkleRoller.Decision edge = SprinkleRoller.decide(
                rigged(0.35), 0.35, 1.0, 0.90, 2, 0, 3, 0, 6);
        check(edge.rolled() && !edge.passed(),
                "a roll exactly AT the chance fails (pass is strictly under)");

        // --- caps beat chance
        SprinkleRoller.Decision capped = SprinkleRoller.decide(
                rigged(0.0), 1.0, 1.25, 0.90, 2, 0, 3, 6, 6);
        check(!capped.rolled() && !capped.passed() && capped.spawnCount() == 0,
                "per-fight cap reached: the stamp does not roll at all");
        SprinkleRoller.Decision zeroWave = SprinkleRoller.decide(
                rigged(0.0), 1.0, 1.0, 0.90, 0, 0, 3, 0, 6);
        check(!zeroWave.rolled(), "count 0 never rolls");

        // --- a passed roll with no room is spent, not banked
        SprinkleRoller.Decision noRoom = SprinkleRoller.decide(
                rigged(0.0), 1.0, 1.0, 0.90, 2, 3, 3, 0, 6);
        check(noRoom.rolled() && noRoom.passed() && noRoom.spawnCount() == 0,
                "alive at max-alive: pass is spent with spawnCount 0 (no banking)");

        // --- partial waves are normal
        SprinkleRoller.Decision partialAlive = SprinkleRoller.decide(
                rigged(0.0), 1.0, 1.0, 0.90, 2, 2, 3, 0, 6);
        check(partialAlive.spawnCount() == 1,
                "alive room of 1 trims a wave of 2 to a partial 1");
        SprinkleRoller.Decision partialFight = SprinkleRoller.decide(
                rigged(0.0), 1.0, 1.0, 0.90, 2, 0, 3, 5, 6);
        check(partialFight.spawnCount() == 1,
                "per-fight room of 1 trims a wave of 2 to a partial 1");

        // --- the three stamps of one fight, end to end (wither defaults)
        SprinkleSettings w = SprinkleSettings.defaults(BossKind.WITHER);
        int spawned = 0;
        int alive = 0;
        double[] rolls = {0.20, 0.60, 0.10};
        boolean[] expectPass = {true, false, true};
        for (int stamp = 0; stamp < 3; stamp++) {
            SprinkleRoller.Decision d = SprinkleRoller.decide(rigged(rolls[stamp]),
                    w.chance(), SprinkleRoller.stampMultiplier(mults, stamp), 0.90,
                    w.count(), alive, w.maxAlive(), spawned, w.maxPerFight());
            check(d.passed() == expectPass[stamp],
                    "wither stamp " + stamp + " pass expectation");
            spawned += d.spawnCount();
            alive += d.spawnCount();
        }
        check(spawned == 3, "wither three stamps at scripted rolls spawn 3 total "
                + "(the third wave is trimmed to 1 by max-alive: 2 alive + room 1)");

        System.out.println("SprinkleRollerTest: " + checks + " checks, " + failures + " failures");
        if (failures > 0) {
            System.exit(1);
        }
    }
}

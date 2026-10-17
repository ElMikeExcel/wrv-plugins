package com.winkreyes.acrobatics;

import com.winkreyes.acrobatics.core.PayoutMath;
import com.winkreyes.acrobatics.core.SpotMemory;

import java.util.List;
import java.util.UUID;

public final class PayoutTests {
    private static int checks = 0;

    private static void eq(Object actual, Object expected, String label) {
        checks++;
        if (!actual.equals(expected)) {
            throw new AssertionError(label + ": expected <" + expected + "> but was <" + actual + ">");
        }
    }

    private static void close(double actual, double expected, String label) {
        checks++;
        if (Math.abs(actual - expected) > 0.0001) {
            throw new AssertionError(label + ": expected <" + expected + "> but was <" + actual + ">");
        }
    }

    public static void main(String[] args) {
        // Would-be fall damage: nothing at 3 blocks or under, 1 point per block past 3.
        eq(PayoutMath.fallDamagePoints(3.0), 0.0, "3 blocks no damage");
        eq(PayoutMath.fallDamagePoints(3.9), 0.0, "3.9 blocks no damage");
        eq(PayoutMath.fallDamagePoints(4.0), 1.0, "4 blocks = 1 point");
        eq(PayoutMath.fallDamagePoints(13.7), 10.0, "13.7 blocks = 10 points (floored)");
        eq(PayoutMath.fallDamagePoints(-5), 0.0, "negative safe");

        // Feather Falling: IV cuts damage 48%.
        close(PayoutMath.featherFallAdjustedDamage(10.0, 4), 5.2, "FF IV reduction");
        close(PayoutMath.featherFallAdjustedDamage(10.0, 0), 10.0, "no FF no change");
        close(PayoutMath.featherFallAdjustedDamage(10.0, 9), 2.0, "FF reduction caps at 80%");

        // Style: none at 10 blocks, +5% per block after, capped at +100%.
        close(PayoutMath.styleMultiplier(9.0, 10, 0.05, 1.0), 1.0, "no style under start");
        close(PayoutMath.styleMultiplier(20.0, 10, 0.05, 1.0), 1.5, "20 blocks = +50%");
        close(PayoutMath.styleMultiplier(30.0, 10, 0.05, 1.0), 2.0, "30 blocks = cap");
        close(PayoutMath.styleMultiplier(90.0, 10, 0.05, 1.0), 2.0, "huge fall still capped");

        // Repeat table.
        List<Double> table = List.of(1.0, 0.75, 0.5, 0.25, 0.1);
        close(PayoutMath.repeatMultiplier(0, table), 1.0, "first landing full");
        close(PayoutMath.repeatMultiplier(1, table), 0.75, "first repeat");
        close(PayoutMath.repeatMultiplier(4, table), 0.1, "fourth repeat floor");
        close(PayoutMath.repeatMultiplier(40, table), 0.1, "deep repeat stays at floor");

        // Full award: 14-block fall, no FF, fresh spot, default rate.
        // damage 11 * 600 = 6600; style at 14 = 1.2 -> 7920.
        eq(PayoutMath.awardXp(14.0, 0, 0, new PayoutMath.Settings(600, 10, 0.05, 1.0, table, 25_000, 2.0)),
                7920, "full award math");
        // Same fall with FF IV: damage 11*0.52=5.72 *600=3432 *2.0 compensation=6864 *1.2 style=8236.8 -> 8237.
        eq(PayoutMath.awardXp(14.0, 4, 0, new PayoutMath.Settings(600, 10, 0.05, 1.0, table, 25_000, 2.0)),
                8237, "FF compensated award");
        // Cap applies.
        eq(PayoutMath.awardXp(200.0, 0, 0, new PayoutMath.Settings(600, 10, 0.05, 1.0, table, 25_000, 2.0)),
                25_000, "award capped");

        // Spot memory: radius + window behavior.
        SpotMemory memory = new SpotMemory();
        UUID a = UUID.fromString("00000000-0000-0000-0000-000000000001");
        long now = 1_000_000L;
        eq(memory.countRecent(a, "world", 100, 64, 5, 600_000, now), 0, "empty memory");
        memory.record(a, "world", 100, 64, now);
        eq(memory.countRecent(a, "world", 102, 64, 5, 600_000, now), 1, "same spot counts");
        eq(memory.countRecent(a, "world", 120, 64, 5, 600_000, now), 0, "far spot does not count");
        eq(memory.countRecent(a, "world_nether", 100, 64, 5, 600_000, now), 0, "other world does not count");
        eq(memory.countRecent(a, "world", 100, 64, 5, 600_000, now + 700_000), 0, "expired spot forgotten");
        memory.record(a, "world", 101, 64, now);
        eq(memory.countRecent(a, "world", 100, 64, 5, 600_000, now), 2, "repeats accumulate");

        System.out.println("PayoutTests: " + checks + " assertions passed");
    }
}

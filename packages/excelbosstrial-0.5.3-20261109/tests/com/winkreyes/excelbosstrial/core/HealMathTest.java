/* Excel WRV maintenance; existing authorship retained. */ package com.winkreyes.excelbosstrial.core;

/**
 * Boss-heal math (0.4.0, PM-1): effective healing is capped at
 * what the bar was actually missing; the entity-to-pool
 * conversion is the exact inverse of IntakeFactor.toEntity;
 * net damage never drops below zero however much healing a
 * fight records.
 */
public final class HealMathTest {

    private static int passed;
    private static int failed;

    private static void check(boolean ok, String label) {
        if (ok) {
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
        // --- effective heal: capped at the missing health
        close(HealMath.effectiveHeal(50, 300, 1000), 50, "a heal under the gap lands whole");
        close(HealMath.effectiveHeal(900, 300, 1000), 700, "a heal over the gap caps at the gap");
        close(HealMath.effectiveHeal(10, 1000, 1000), 0, "a heal at full health is nothing");
        close(HealMath.effectiveHeal(0, 300, 1000), 0, "a zero heal is nothing");
        close(HealMath.effectiveHeal(-5, 300, 1000), 0, "a negative heal is nothing");
        close(HealMath.effectiveHeal(Double.NaN, 300, 1000), 0, "a NaN heal is nothing");
        close(HealMath.effectiveHeal(10, 1200, 1000), 0, "health above max (mid-scale) heals nothing");

        // --- pool space: the exact inverse of the intake conversion
        close(HealMath.toPoolSpace(100, 1.0), 100, "factor 1 is the identity");
        close(HealMath.toPoolSpace(100, 2.0), 100, "a factor above 1 reads as identity, never shrinks");
        close(HealMath.toPoolSpace(100, 0.0), 100, "a zero factor reads as identity, never divides");
        // factor 0.0457 (a 21,900 pool over a 1002 cap): entity heal 10 -> pool ~218.8
        double f = IntakeFactor.factor(1002.0, 21900.0);
        double poolHeal = HealMath.toPoolSpace(10, f);
        close(poolHeal * f, 10, "toPoolSpace inverts IntakeFactor exactly (round-trip)");
        check(poolHeal > 200 && poolHeal < 240, "a 10 HP entity heal is ~219 pool HP at the wither factor");
        close(HealMath.toPoolSpace(0, f), 0, "a zero heal converts to zero");
        close(HealMath.toPoolSpace(Double.NaN, f), 0, "a NaN heal converts to zero");

        // --- net damage: gross minus healed, floored at zero
        close(HealMath.netAfterHeal(1000, 0, 0), 1000, "no healing: net is gross");
        close(HealMath.netAfterHeal(1000, 250, 0), 750, "recorded healing subtracts");
        close(HealMath.netAfterHeal(1000, 0, 250), 750, "the incoming heal subtracts the same");
        close(HealMath.netAfterHeal(1000, 600, 600), 0, "healing past gross floors at zero, never negative");
        close(HealMath.netAfterHeal(0, 500, 0), 0, "healing with no damage is still zero net");

        // --- the Nov 7 wither arithmetic, end to end: the party
        // dealt 16,430 gross of a 21,900 pool; vanilla wither regen
        // (1 HP / 10 s entity space at the live factor) over the
        // 54-minute fight is the heal that silently ate the kill.
        double gross = 16430.0;
        double entityRegen = 324.0; // 54 min x 6 heals/min x 1 HP (illustrative)
        double healed = HealMath.toPoolSpace(entityRegen, f);
        double net = HealMath.netAfterHeal(gross, healed, 0);
        check(healed > 7000, "the regen stream is thousands of pool HP (healed=" + healed + ")");
        check(net < gross && net > 0, "net progress survives below gross (" + net + ")");

        System.out.println("HealMathTest: " + passed + " passed, " + failed + " failed");
        if (failed > 0) {
            System.exit(1);
        }
    }
}

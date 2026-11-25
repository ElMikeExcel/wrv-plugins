import excelemfixes.arcane.CompensationMath;

import java.util.List;

/**
 * Plain-JVM suite for the arcane-bolt module's CompensationMath.
 * Add to ExcelEMFixes' TestMain with one line: ArcaneMathTest.run();
 * (and add arcane/CompensationMath.java to run-tests.sh's compile
 * list). Uses the shared Check harness.
 */
public final class ArcaneMathTest {

    public static void run() {
        System.out.println("ArcaneMathTest:");
        // extraDamage: scale only (10.0 at scale 1.5 -> +5.0)
        eq("scale only", 5.0, CompensationMath.extraDamage(10.0, 1.5, 1.0));
        // extraDamage: scale x buff (Spell Echo 1.14 -> 10 * (1.71 - 1) = 7.1)
        eq("scale x echo", 7.1, CompensationMath.extraDamage(10.0, 1.5, 1.14));
        // extraDamage: buff only at scale 1.0 still applies the buff fix
        eq("buff only", 1.4, CompensationMath.extraDamage(10.0, 1.0, 1.14));
        // extraDamage: no scale, no buff -> nothing
        eq("neutral", 0.0, CompensationMath.extraDamage(10.0, 1.0, 1.0));
        // extraDamage: zero / negative / NaN dealt -> nothing
        eq("zero dealt", 0.0, CompensationMath.extraDamage(0.0, 1.5, 1.14));
        eq("negative dealt", 0.0, CompensationMath.extraDamage(-3.0, 1.5, 1.14));
        eq("nan dealt", 0.0, CompensationMath.extraDamage(Double.NaN, 1.5, 1.14));
        // extraDamage: sub-1 combined multiplier never subtracts damage
        eq("weak buff no negative", 0.0, CompensationMath.extraDamage(10.0, 0.9, 1.0));

        long now = 1_000_000L;
        // strongestBuff: empty / null -> 1.0
        eq("buff empty", 1.0, CompensationMath.strongestBuff(List.of(), now));
        eq("buff null", 1.0, CompensationMath.strongestBuff(null, now));
        // strongestBuff: live ANY modifier wins
        eq("buff any", 1.14, CompensationMath.strongestBuff(List.of(
                new CompensationMath.BuffEntry(1.14, now + 1000, "ANY")), now));
        // strongestBuff: live SPELL modifier applies to spell damage
        eq("buff spell", 1.20, CompensationMath.strongestBuff(List.of(
                new CompensationMath.BuffEntry(1.20, now + 1000, "SPELL")), now));
        // strongestBuff: expired entries are ignored
        eq("buff expired", 1.0, CompensationMath.strongestBuff(List.of(
                new CompensationMath.BuffEntry(1.50, now - 1, "ANY")), now));
        // strongestBuff: strongest wins, not product (EM semantics)
        eq("buff strongest not product", 1.20, CompensationMath.strongestBuff(List.of(
                new CompensationMath.BuffEntry(1.14, now + 1000, "ANY"),
                new CompensationMath.BuffEntry(1.20, now + 1000, "SPELL")), now));
        // strongestBuff: unrelated domains ignored
        eq("buff other domain", 1.0, CompensationMath.strongestBuff(List.of(
                new CompensationMath.BuffEntry(1.50, now + 1000, "INCOMING")), now));
        // strongestBuff: a weaker live buff beats an expired stronger one
        eq("buff live weaker wins", 1.10, CompensationMath.strongestBuff(List.of(
                new CompensationMath.BuffEntry(1.50, now - 5, "ANY"),
                new CompensationMath.BuffEntry(1.10, now + 500, "ANY")), now));

        // applySplit: no absorption -> all from health
        split("split plain", 0.0, 20.0, 5.0, 0.0, 15.0);
        // applySplit: absorption soaks first
        split("split absorption first", 4.0, 20.0, 5.0, 0.0, 19.0);
        // applySplit: extra larger than absorption + health clamps at 0
        split("split lethal clamps", 4.0, 20.0, 99.0, 0.0, 0.0);
        // applySplit: zero extra changes nothing
        split("split zero", 4.0, 20.0, 0.0, 4.0, 20.0);
        // applySplit: partial absorption soak
        split("split partial soak", 4.0, 20.0, 2.5, 1.5, 20.0);
    }

    private static void eq(String name, double expected, double actual) {
        boolean same = Math.abs(expected - actual) < 1e-9;
        if (!same) System.out.println("    expected=" + expected + " actual=" + actual);
        Check.ok(same, "arcane " + name);
    }

    private static void split(String name, double absorption, double health, double extra,
                              double expectedAbsorption, double expectedHealth) {
        double[] result = CompensationMath.applySplit(absorption, health, extra);
        boolean same = Math.abs(expectedAbsorption - result[0]) < 1e-9
                && Math.abs(expectedHealth - result[1]) < 1e-9;
        if (!same) {
            System.out.println("    expected=[" + expectedAbsorption + ", " + expectedHealth
                    + "] actual=[" + result[0] + ", " + result[1] + "]");
        }
        Check.ok(same, "arcane " + name);
    }
}

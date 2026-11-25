import excelemfixes.arcane.AbilityHitPairing;
import excelemfixes.arcane.CompensationMath;

import java.util.UUID;

/**
 * Plain-JVM suite for the arcane-bolt module's CompensationMath and
 * AbilityHitPairing. Add to ExcelEMFixes' TestMain with one line:
 * ArcaneMathTest.run(); (and add arcane/CompensationMath.java +
 * arcane/AbilityHitPairing.java to run-tests.sh's compile list).
 * Uses the shared Check harness.
 */
public final class ArcaneMathTest {

    public static void run() {
        System.out.println("ArcaneMathTest:");
        // extraDamage: scale only (10.0 at scale 1.5 -> +5.0)
        eq("scale only", 5.0, CompensationMath.extraDamage(10.0, 1.5));
        // extraDamage: the measured Spell Echo case - the dealt amount
        // ALREADY includes EliteMobs' own 1.154x buff application, and
        // the module must add the scale's share only (38.51 x 0.5).
        // The pre-0.11.0 formula would have added 38.51 x 0.731 here,
        // counting the buff a second time.
        eq("echo-inclusive dealt gets scale only", 19.255,
                CompensationMath.extraDamage(38.51, 1.5));
        // extraDamage: scale 1.0 -> nothing (shipped default is inert)
        eq("neutral", 0.0, CompensationMath.extraDamage(10.0, 1.0));
        // extraDamage: sub-1 scale never subtracts damage
        eq("weak scale no negative", 0.0, CompensationMath.extraDamage(10.0, 0.9));
        // extraDamage: zero / negative / NaN dealt -> nothing
        eq("zero dealt", 0.0, CompensationMath.extraDamage(0.0, 1.5));
        eq("negative dealt", 0.0, CompensationMath.extraDamage(-3.0, 1.5));
        eq("nan dealt", 0.0, CompensationMath.extraDamage(Double.NaN, 1.5));
        // extraDamage: NaN scale -> nothing
        eq("nan scale", 0.0, CompensationMath.extraDamage(10.0, Double.NaN));

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

        pairing();
    }

    private static void pairing() {
        UUID caster = UUID.randomUUID();
        UUID other = UUID.randomUUID();
        UUID elite = UUID.randomUUID();
        AbilityHitPairing pairing = new AbilityHitPairing(10);
        // Nothing marked -> not authorized
        Check.ok(!pairing.isAuthorized(caster, elite, 100L), "arcane pairing unmarked");
        // Marked -> authorized at the same tick and inside the window
        pairing.mark(caster, elite, 100L);
        Check.ok(pairing.isAuthorized(caster, elite, 100L), "arcane pairing same tick");
        Check.ok(pairing.isAuthorized(caster, elite, 110L), "arcane pairing window edge");
        // Past the window -> not authorized
        Check.ok(!pairing.isAuthorized(caster, elite, 111L), "arcane pairing expired");
        // A different caster or a different elite is not authorized
        pairing.mark(caster, elite, 200L);
        Check.ok(!pairing.isAuthorized(other, elite, 200L), "arcane pairing wrong caster");
        Check.ok(!pairing.isAuthorized(caster, other, 200L), "arcane pairing wrong elite");
        // The mark is not consumed: several damage steps of one cast
        // (a volley) all qualify inside the window
        Check.ok(pairing.isAuthorized(caster, elite, 201L), "arcane pairing multi-step 1");
        Check.ok(pairing.isAuthorized(caster, elite, 205L), "arcane pairing multi-step 2");
        // A fresh mark refreshes the window
        pairing.mark(caster, elite, 209L);
        Check.ok(pairing.isAuthorized(caster, elite, 219L), "arcane pairing refreshed");
        Check.ok(!pairing.isAuthorized(caster, elite, 220L), "arcane pairing refresh expiry");
        // Null ids never authorize and never throw
        AbilityHitPairing nullPairing = new AbilityHitPairing(10);
        Check.ok(!nullPairing.isAuthorized(null, elite, 200L), "arcane pairing null caster");
        nullPairing.mark(null, elite, 200L);
        nullPairing.mark(caster, null, 200L);
        Check.ok(!nullPairing.isAuthorized(caster, elite, 200L), "arcane pairing null mark safe");
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

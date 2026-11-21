import com.winkreyes.exceltrainingwheels.core.BossDivision;
import com.winkreyes.exceltrainingwheels.core.ShieldRule;
import static com.winkreyes.exceltrainingwheels.core.BossDivision.Shape.*;

/**
 * The 0.1.3 BossTrial-compensation pins. The contract under test
 * (TW-PRESSURE-CHECK, Nov 2, 2026): against a seal boss in an
 * open encounter, BossTrial scales the event by chapter x ramp;
 * the shield divides its factor by that same scale, read through
 * ExcelBossTrialAPI.outgoingScaleFor — so the composed take is
 * baseFactor x VANILLA at every ramp point. Settled live scales:
 * Wither/Warden chapter x1.25 (fight start 1.25, ramp cap 2.5),
 * Elder/Dragon chapter x1.0 (fight start 1.0, ramp cap 2.0).
 * Rylie's live base factor is 0.25 (Mike's absolute line).
 */
public class BossDivisionTest {

    private static final double BASE = 0.25;

    /**
     * The production composition for a DIRECT boss hit: the event
     * arrives BossTrial-scaled (vanilla x scale); the shield's
     * effective factor is base / scale; the take must be the
     * quarter of the UNSCALED hit.
     */
    private static double composedTake(double vanilla, double scale, double baseFactor) {
        double eventDamage = vanilla * scale;
        double effective = BossDivision.dividedFactor(baseFactor, scale);
        return ShieldRule.scaled(eventDamage, effective);
    }

    private static boolean bits(double a, double b) {
        return Double.doubleToLongBits(a) == Double.doubleToLongBits(b);
    }

    public static void run() {
        System.out.println("BossDivisionTest");

        // ---- Shape gate: only the pressure-scaled shape divides. ----
        // The 0.3.7 contract reports the PRESSURE scale only; the
        // dragon's phase-2 factor is a separate axis it does not
        // report. Dragon part/fireball/breath events are scaled
        // ONLY on that phase-2 axis, so dividing them by the
        // pressure number would break the line downward in
        // phase 1 — they are admitted at the flat factor instead.
        Check.ok(BossDivision.divides(DIRECT_BOSS), "direct boss damager divides (pressure handler's domain)");
        Check.ok(!BossDivision.divides(DRAGON_PROJECTILE), "dragon fireball does NOT divide (phase-2 axis only; API reports pressure)");
        Check.ok(!BossDivision.divides(DRAGON_BREATH), "dragon breath does NOT divide (phase-2 axis only)");
        Check.ok(!BossDivision.divides(DRAGON_PART), "dragon body part does NOT divide (phase-2 axis only)");
        Check.ok(!BossDivision.divides(WITHER_PROJECTILE), "wither skull shapes NEVER divide (no BossTrial scaler exists)");
        Check.ok(!BossDivision.divides(NON_BOSS), "non-boss sources never divide");

        // ---- Scale sanitation: trust finite >= 1.0, collapse the rest. ----
        Check.close(1.0, BossDivision.sanitize(1.0), "sanitize keeps 1.0 (no encounter)");
        Check.close(1.25, BossDivision.sanitize(1.25), "sanitize keeps the fight-start scale 1.25");
        Check.close(2.5, BossDivision.sanitize(2.5), "sanitize keeps the ramp-cap scale 2.5");
        Check.close(1.0, BossDivision.sanitize(0.5), "sanitize collapses a sub-1.0 read to 1.0");
        Check.close(1.0, BossDivision.sanitize(0.0), "sanitize collapses 0.0 to 1.0");
        Check.close(1.0, BossDivision.sanitize(-2.5), "sanitize collapses a negative read to 1.0");
        Check.close(1.0, BossDivision.sanitize(Double.NaN), "sanitize collapses NaN to 1.0");
        Check.close(1.0, BossDivision.sanitize(Double.POSITIVE_INFINITY), "sanitize collapses infinity to 1.0");

        // ---- THE PINS: composed take == 0.25 x vanilla, every scale. ----
        double[] vanillas = {3.0, 8.0, 13.0};
        double[] scales = {1.0, 1.25, 1.5, 2.0, 2.5};
        for (double scale : scales) {
            for (double vanilla : vanillas) {
                Check.close(0.25 * vanilla, composedTake(vanilla, scale, BASE),
                        "scale x" + scale + ": vanilla " + vanilla
                                + " lands at exactly a quarter (" + (0.25 * vanilla) + ")");
            }
        }
        // Headline pins, named: the task's three scale points.
        Check.close(0.1, BossDivision.dividedFactor(0.25, 2.5),
                "ramp cap: factor 0.25 / scale 2.5 = 0.1");
        Check.close(2.0, composedTake(8.0, 2.5, BASE),
                "ramp cap: a ramped wither melee 8 lands at 2.0 (0.25 x unscaled)");
        Check.close(0.2, BossDivision.dividedFactor(0.25, 1.25),
                "fight start: factor 0.25 / scale 1.25 = 0.2");
        Check.close(2.0, composedTake(8.0, 1.25, BASE),
                "fight start: a wither melee 8 lands at 2.0 (0.25 x unscaled)");
        Check.close(2.0, composedTake(8.0, 1.0, BASE),
                "no encounter: a boss-type melee 8 lands at 2.0 (flat quarter)");

        // ---- Fail-soft: an absent/failed seam is a bit-exact no-op. ----
        Check.ok(bits(0.25, BossDivision.dividedFactor(0.25, 1.0)),
                "scale 1.0 (BossTrial absent / no encounter) leaves the factor bit-exact");
        Check.ok(bits(0.25, BossDivision.dividedFactor(0.25, Double.NaN)),
                "a NaN read leaves the factor bit-exact (sanitized to 1.0)");
        Check.ok(bits(0.25, BossDivision.dividedFactor(0.25, 0.4)),
                "a sub-1.0 read leaves the factor bit-exact (sanitized to 1.0)");

        // ---- The skull guard: skull events compose WITHOUT division. ----
        // A skull direct hit / blast arrives UNSCALED (BossTrial has
        // no scaler for it), so the shield applies the flat factor:
        // the take is the quarter — never the quarter divided again.
        Check.close(2.25, ShieldRule.scaled(9.0, BASE),
                "skull hit 9 lands at exactly 2.25 (flat quarter, undivided)");
        Check.close(3.25, ShieldRule.scaled(13.0, BASE),
                "skull blast 13 lands at exactly 3.25 (flat quarter, undivided)");

        // ---- Dragon phase-2 shapes: admitted at the FLAT factor. ----
        // Part contact / breath / fireball events carry no pressure
        // scale; outside phase 2 they are unscaled entirely, so the
        // flat factor is the exact quarter. During phase 2 the
        // DragonChapter buff (x1.5, an axis the API does not
        // report) leaves a bounded residual of 0.375 x vanilla —
        // named here so the number is on the record, not hidden.
        Check.close(2.0, ShieldRule.scaled(8.0, BASE),
                "dragon part contact 8 outside phase 2 lands at exactly 2.0 (flat quarter)");
        Check.close(3.0, ShieldRule.scaled(8.0 * 1.5, BASE),
                "dragon part contact 8 in phase 2 lands at 3.0 (0.375 x vanilla — the named phase-2 residual)");

        // ---- Graduated player: base 1.0 still gets the compensation. ----
        Check.close(8.0, composedTake(8.0, 2.5, 1.0),
                "base 1.0 at ramp cap: take is the unscaled 8, never the ramped 20");

        // ---- Factor validation mirrors the rest of the shield math. ----
        boolean threw = false;
        try { BossDivision.dividedFactor(1.5, 2.5); } catch (IllegalArgumentException e) { threw = true; }
        Check.ok(threw, "factor > 1 rejected");
        threw = false;
        try { BossDivision.dividedFactor(Double.NaN, 2.5); } catch (IllegalArgumentException e) { threw = true; }
        Check.ok(threw, "NaN factor rejected");
    }
}

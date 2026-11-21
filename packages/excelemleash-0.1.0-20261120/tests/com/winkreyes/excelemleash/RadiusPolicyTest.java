import com.winkreyes.excelemleash.RadiusPolicy;

/** Leg B resolution: floor, overrides, and the no-invented-leashes rule. */
public final class RadiusPolicyTest {

    private static final double FLOOR = 35.0;

    public static void run() {
        System.out.println("RadiusPolicyTest");

        // Below the floor is raised to the floor (the Primis wolf case: 20 -> 35).
        Check.close(35.0, RadiusPolicy.effectiveTarget(null, 20.0, FLOOR), "below floor raised to floor");
        Check.eq(RadiusPolicy.ChangeKind.FLOOR_RAISE, RadiusPolicy.classify(null, 20.0, FLOOR), "below floor classifies FLOOR_RAISE");
        Check.close(35.0, RadiusPolicy.effectiveTarget(null, 2.0, FLOOR), "radius 2 raised to floor");
        Check.close(35.0, RadiusPolicy.effectiveTarget(null, 34.5, FLOOR), "just below floor raised");

        // At or above the floor is untouched.
        Check.close(35.0, RadiusPolicy.effectiveTarget(null, 35.0, FLOOR), "at floor untouched");
        Check.eq(RadiusPolicy.ChangeKind.NONE, RadiusPolicy.classify(null, 35.0, FLOOR), "at floor classifies NONE");
        Check.close(69.0, RadiusPolicy.effectiveTarget(null, 69.0, FLOOR), "above floor untouched (69)");
        Check.eq(RadiusPolicy.ChangeKind.NONE, RadiusPolicy.classify(null, 69.0, FLOOR), "above floor classifies NONE");
        Check.close(46.0, RadiusPolicy.effectiveTarget(null, 46.0, FLOOR), "above floor untouched (46)");

        // No leash (0) stays unleashed — the floor never invents leashes.
        Check.close(0.0, RadiusPolicy.effectiveTarget(null, 0.0, FLOOR), "no-radius boss untouched by floor");
        Check.eq(RadiusPolicy.ChangeKind.NONE, RadiusPolicy.classify(null, 0.0, FLOOR), "no-radius classifies NONE");

        // Overrides win in both directions.
        Check.close(50.0, RadiusPolicy.effectiveTarget(50.0, 20.0, FLOOR), "override raises above floor");
        Check.eq(RadiusPolicy.ChangeKind.OVERRIDE, RadiusPolicy.classify(50.0, 20.0, FLOOR), "override-up classifies OVERRIDE");
        Check.close(25.0, RadiusPolicy.effectiveTarget(25.0, 69.0, FLOOR), "override lowers below floor and file value");
        Check.eq(RadiusPolicy.ChangeKind.OVERRIDE, RadiusPolicy.classify(25.0, 69.0, FLOOR), "override-down classifies OVERRIDE");
        Check.close(25.0, RadiusPolicy.effectiveTarget(25.0, 20.0, FLOOR), "override between file value and floor wins");

        // An override is a deliberate act: it can leash an unleashed boss...
        Check.close(40.0, RadiusPolicy.effectiveTarget(40.0, 0.0, FLOOR), "override leashes an unleashed boss");
        Check.eq(RadiusPolicy.ChangeKind.OVERRIDE, RadiusPolicy.classify(40.0, 0.0, FLOOR), "override-on-unleashed classifies OVERRIDE");
        // ...and unleash a leashed one.
        Check.close(0.0, RadiusPolicy.effectiveTarget(0.0, 20.0, FLOOR), "override 0 unleashes");
        Check.eq(RadiusPolicy.ChangeKind.OVERRIDE, RadiusPolicy.classify(0.0, 20.0, FLOOR), "override-0 classifies OVERRIDE");

        // An override equal to the current value is in force but needs no write.
        Check.close(35.0, RadiusPolicy.effectiveTarget(35.0, 35.0, FLOOR), "override equal to current stays");
        Check.eq(RadiusPolicy.ChangeKind.NONE, RadiusPolicy.classify(35.0, 35.0, FLOOR), "override equal to current classifies NONE");

        // A different floor is honored (the number is config, not law).
        Check.close(40.0, RadiusPolicy.effectiveTarget(null, 20.0, 40.0), "floor 40 raises 20 to 40");
        Check.close(20.0, RadiusPolicy.effectiveTarget(null, 20.0, 10.0), "floor 10 leaves 20 alone");
    }
}

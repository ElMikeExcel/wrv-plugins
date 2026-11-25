import excelemfixes.leash.LeashRemovalPolicy;

/** D182 unleash-all resolution: every leash goes to 0, no exceptions. */
public final class LeashRemovalPolicyTest {

    public static void run() {
        System.out.println("LeashRemovalPolicyTest");

        // Every positive radius is removed — the tight Primis cases,
        // the old floor value itself, and the largest stock radii.
        Check.close(0.0, LeashRemovalPolicy.effectiveTarget(2.0), "radius 2 removed");
        Check.eq(LeashRemovalPolicy.ChangeKind.UNLEASH, LeashRemovalPolicy.classify(2.0), "radius 2 classifies UNLEASH");
        Check.close(0.0, LeashRemovalPolicy.effectiveTarget(20.0), "radius 20 removed (feral wolf case)");
        Check.eq(LeashRemovalPolicy.ChangeKind.UNLEASH, LeashRemovalPolicy.classify(20.0), "radius 20 classifies UNLEASH");
        Check.close(0.0, LeashRemovalPolicy.effectiveTarget(35.0), "radius 35 removed (the old floor is not a refuge)");
        Check.close(0.0, LeashRemovalPolicy.effectiveTarget(46.0), "radius 46 removed");
        Check.close(0.0, LeashRemovalPolicy.effectiveTarget(80.0), "radius 80 removed (largest stock)");
        Check.eq(LeashRemovalPolicy.ChangeKind.UNLEASH, LeashRemovalPolicy.classify(80.0), "radius 80 classifies UNLEASH");

        // Fractional radii are leashes too (EM's fallback engages at >= 1,
        // and the native guard is > 0 — anything positive goes).
        Check.close(0.0, LeashRemovalPolicy.effectiveTarget(0.5), "radius 0.5 removed");
        Check.eq(LeashRemovalPolicy.ChangeKind.UNLEASH, LeashRemovalPolicy.classify(0.5), "radius 0.5 classifies UNLEASH");

        // Already unleashed stays untouched — no write, no change kind.
        Check.close(0.0, LeashRemovalPolicy.effectiveTarget(0.0), "no-radius boss stays 0");
        Check.eq(LeashRemovalPolicy.ChangeKind.NONE, LeashRemovalPolicy.classify(0.0), "no-radius classifies NONE");

        // Defensive: a non-positive sentinel is never "raised" to a leash.
        Check.close(-1.0, LeashRemovalPolicy.effectiveTarget(-1.0), "negative sentinel untouched");
        Check.eq(LeashRemovalPolicy.ChangeKind.NONE, LeashRemovalPolicy.classify(-1.0), "negative classifies NONE");
    }
}

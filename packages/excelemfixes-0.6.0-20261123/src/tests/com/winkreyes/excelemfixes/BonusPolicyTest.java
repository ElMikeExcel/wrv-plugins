import excelemfixes.foodheal.BonusPolicy;

/**
 * D184 bonus decision truth table: the bonus delivers its
 * configured rate only when it is enabled, the rate is positive,
 * the player is in an EliteMobs world, and the player is out of
 * combat. Every other combination delivers 0.
 */
public final class BonusPolicyTest {

    public static void run() {
        System.out.println("BonusPolicyTest");
        Check.close(1.0, BonusPolicy.tickPercent(true, 1.0, true, false),
                "enabled + EM world + out of combat delivers the 1.0 default");
        Check.close(2.5, BonusPolicy.tickPercent(true, 2.5, true, false),
                "configured rate passes through (2.5)");
        Check.close(0.0, BonusPolicy.tickPercent(false, 1.0, true, false),
                "disabled by config delivers 0");
        Check.close(0.0, BonusPolicy.tickPercent(true, 1.0, false, false),
                "overworld (not an EM world) delivers 0");
        Check.close(0.0, BonusPolicy.tickPercent(true, 1.0, true, true),
                "in combat delivers 0");
        Check.close(0.0, BonusPolicy.tickPercent(true, 0.0, true, false),
                "zero rate delivers 0");
        Check.close(0.0, BonusPolicy.tickPercent(true, -1.0, true, false),
                "negative rate delivers 0");
        Check.close(0.0, BonusPolicy.tickPercent(false, 0.0, false, true),
                "all conditions failing delivers 0");
    }
}

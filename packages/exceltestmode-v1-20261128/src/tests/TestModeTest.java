import exceltestmode.TestModeMath;

/**
 * Plain-JVM suite for the standalone plugin's pure class:
 * TestModeMath (floor sanitize, the break test, the
 * allowance, and the base-damage rescale that expresses
 * the clamp through the event's own modifier ratio).
 *
 * The contract under test is the module's one promise:
 * damage that fits above the floor passes untouched;
 * only the lethal excess is trimmed; a victim already
 * at the floor may take 0 (hits land as 0-damage
 * events, never cancelled, never negative).
 */
public final class TestModeTest {

    public static void run() {
        System.out.println("TestModeTest:");

        // ---- floor sanitize: shipped default + rejection shape
        Check.close(2.0, TestModeMath.sanitizeFloor(2.0), "floor default kept");
        Check.close(5.5, TestModeMath.sanitizeFloor(5.5), "floor 5.5 kept");
        Check.close(100.0, TestModeMath.sanitizeFloor(100.0), "floor cap kept");
        Check.close(2.0, TestModeMath.sanitizeFloor(0.0), "floor 0 -> default (0 is death)");
        Check.close(2.0, TestModeMath.sanitizeFloor(-3.0), "floor negative -> default");
        Check.close(2.0, TestModeMath.sanitizeFloor(100.1), "floor >cap -> default");
        Check.close(2.0, TestModeMath.sanitizeFloor(Double.NaN), "floor NaN -> default");
        Check.close(2.0, TestModeMath.sanitizeFloor(Double.POSITIVE_INFINITY), "floor +inf -> default");

        // ---- break test: pool (health + absorption) vs the floor
        Check.ok(!TestModeMath.wouldBreakFloor(20.0, 0.0, 5.0, 2.0), "break: 20hp hit 5 stays above floor");
        Check.ok(!TestModeMath.wouldBreakFloor(5.0, 0.0, 3.0, 2.0), "break: landing exactly ON the floor is not breaking it");
        Check.ok(TestModeMath.wouldBreakFloor(5.0, 0.0, 3.5, 2.0), "break: 5hp hit 3.5 dips below floor");
        Check.ok(TestModeMath.wouldBreakFloor(5.0, 0.0, 5.0, 2.0), "break: a lethal hit breaks the floor");
        Check.ok(TestModeMath.wouldBreakFloor(5.0, 0.0, 50.0, 2.0), "break: a huge hit breaks the floor");
        Check.ok(!TestModeMath.wouldBreakFloor(20.0, 0.0, 0.0, 2.0), "break: zero damage never breaks");
        Check.ok(!TestModeMath.wouldBreakFloor(20.0, 0.0, -1.0, 2.0), "break: negative damage never breaks");
        Check.ok(!TestModeMath.wouldBreakFloor(20.0, 0.0, Double.NaN, 2.0), "break: NaN damage never breaks");
        Check.ok(!TestModeMath.wouldBreakFloor(2.0, 10.0, 10.0, 2.0), "break: absorption pool absorbs the hit exactly");
        Check.ok(TestModeMath.wouldBreakFloor(2.0, 10.0, 11.0, 2.0), "break: hit past the absorption pool breaks");
        Check.ok(TestModeMath.wouldBreakFloor(2.0, 0.0, 0.5, 2.0), "break: at the floor, any real hit breaks");

        // ---- allowance: pool - floor, floored at 0
        Check.close(18.0, TestModeMath.allowedDamage(20.0, 0.0, 2.0), "allowed: 20hp floor 2 -> 18");
        Check.close(3.0, TestModeMath.allowedDamage(5.0, 0.0, 2.0), "allowed: 5hp floor 2 -> 3");
        Check.close(0.0, TestModeMath.allowedDamage(2.0, 0.0, 2.0), "allowed: at the floor -> 0");
        Check.close(0.0, TestModeMath.allowedDamage(1.5, 0.0, 2.0), "allowed: below the floor -> 0");
        Check.close(10.0, TestModeMath.allowedDamage(2.0, 10.0, 2.0), "allowed: absorption counts into the pool");
        Check.close(18.0, TestModeMath.allowedDamage(20.0, -4.0, 2.0), "allowed: negative absorption ignored");

        // ---- base rescale: the clamp expressed through the event ratio
        Check.close(10.0, TestModeMath.scaledBaseDamage(10.0, 8.0, 8.0), "scale: allowance covers the hit -> base untouched");
        Check.close(10.0, TestModeMath.scaledBaseDamage(10.0, 8.0, 20.0), "scale: allowance above the hit -> base untouched");
        Check.close(5.0, TestModeMath.scaledBaseDamage(10.0, 8.0, 4.0), "scale: half the final -> half the base");
        Check.close(0.0, TestModeMath.scaledBaseDamage(10.0, 8.0, 0.0), "scale: zero allowance -> zero base (event still fires)");
        Check.close(10.0, TestModeMath.scaledBaseDamage(10.0, 0.0, 0.0), "scale: zero final -> base untouched");
        Check.close(10.0, TestModeMath.scaledBaseDamage(10.0, Double.NaN, 4.0), "scale: NaN final -> base untouched");

        // ---- the end-to-end promise, in numbers
        // 20 HP, floor 2, a 25-final hit on base 30: allowed 18,
        // base scales to 30 * 18/25 = 21.6, final settles at 18,
        // health lands exactly on 2.0.
        double allowed = TestModeMath.allowedDamage(20.0, 0.0, 2.0);
        double newBase = TestModeMath.scaledBaseDamage(30.0, 25.0, allowed);
        Check.close(21.6, newBase, "promise: lethal hit base 30 -> 21.6");
        Check.close(2.0, 20.0 - (newBase * (25.0 / 30.0)), "promise: health lands exactly on the floor");
    }
}

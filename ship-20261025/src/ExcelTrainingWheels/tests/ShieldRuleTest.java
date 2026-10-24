import com.winkreyes.exceltrainingwheels.core.ShieldRule;
import static com.winkreyes.exceltrainingwheels.core.ShieldRule.Cause.*;

public class ShieldRuleTest {
    public static void run() {
        System.out.println("ShieldRuleTest");

        // Melee: hostile mob only.
        Check.ok(ShieldRule.shouldScale(ENTITY_ATTACK, true, false, false), "melee from hostile mob scales");
        Check.ok(!ShieldRule.shouldScale(ENTITY_ATTACK, false, false, false), "melee from non-hostile (animal) does not scale");
        Check.ok(!ShieldRule.shouldScale(ENTITY_ATTACK, false, true, false), "ENTITY_ATTACK ignores shooter flags (player hit does not scale)");

        // Sweep: the melee family (0.1.2) — same hostile gate as ENTITY_ATTACK.
        Check.ok(ShieldRule.shouldScale(ENTITY_SWEEP_ATTACK, true, false, false), "sweep from hostile mob scales (melee family)");
        Check.ok(!ShieldRule.shouldScale(ENTITY_SWEEP_ATTACK, false, false, false), "sweep from non-hostile does not scale (players sweep; players are not hostile)");

        // Projectile: the SHOOTER decides.
        Check.ok(ShieldRule.shouldScale(PROJECTILE, false, true, false), "skeleton arrow scales (hostile shooter)");
        Check.ok(!ShieldRule.shouldScale(PROJECTILE, false, false, false), "player-shot arrow does not scale");
        Check.ok(!ShieldRule.shouldScale(PROJECTILE, false, false, false), "dispenser arrow does not scale (no entity shooter)");
        Check.ok(ShieldRule.shouldScale(PROJECTILE, false, true, false), "drowned trident scales (hostile shooter)");

        // Explosion: creeper only.
        Check.ok(ShieldRule.shouldScale(ENTITY_EXPLOSION, true, false, true), "creeper blast scales");
        Check.ok(!ShieldRule.shouldScale(ENTITY_EXPLOSION, false, false, false), "primed TNT blast does not scale (not a creeper)");
        Check.ok(!ShieldRule.shouldScale(ENTITY_EXPLOSION, true, false, false), "non-creeper entity explosion does not scale");

        // Everything else: never.
        Check.ok(!ShieldRule.shouldScale(OTHER, true, true, true), "OTHER cause never scales (falls/lava/fire/starvation...)");
        Check.ok(!ShieldRule.shouldScale(null, true, true, true), "null cause never scales");

        // Scaling math.
        Check.close(5.0, ShieldRule.scaled(10.0, 0.5), "10 x 0.5 = 5");
        Check.close(2.5, ShieldRule.scaled(10.0, 0.25), "10 x 0.25 = 2.5");
        Check.close(10.0, ShieldRule.scaled(10.0, 1.0), "factor 1.0 is a no-op");
        Check.close(0.0, ShieldRule.scaled(10.0, 0.0), "factor 0.0 zeroes mob damage");
        Check.close(0.0, ShieldRule.scaled(0.0, 0.5), "zero damage stays zero");
        Check.close(-3.0, ShieldRule.scaled(-3.0, 0.5), "negative damage passes through untouched");

        boolean threw = false;
        try { ShieldRule.scaled(10.0, 1.5); } catch (IllegalArgumentException e) { threw = true; }
        Check.ok(threw, "factor > 1 rejected");
        threw = false;
        try { ShieldRule.scaled(10.0, -0.1); } catch (IllegalArgumentException e) { threw = true; }
        Check.ok(threw, "factor < 0 rejected");
        threw = false;
        try { ShieldRule.scaled(10.0, Double.NaN); } catch (IllegalArgumentException e) { threw = true; }
        Check.ok(threw, "NaN factor rejected");
    }
}

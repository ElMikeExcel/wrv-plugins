import com.winkreyes.exceltrainingwheels.core.CompensationTable;
import com.winkreyes.exceltrainingwheels.core.ShieldRule;
import static com.winkreyes.exceltrainingwheels.core.ShieldRule.Cause.*;

/**
 * ShieldRule under the D158 rule (0.1.4): admission is the cause
 * alone; only the environmental family is excluded.
 *
 * SUPERSEDED by D158 (Mike's direct, Nov 20, 2026) — the 0.1.0–0.1.3
 * checks below were REMOVED because the rule they pinned is gone:
 * "melee from non-hostile (animal) does not scale", "player-shot /
 * dispenser arrow does not scale", "primed TNT blast does not
 * scale", "sonic boom / dragon breath from a non-boss source does
 * not scale", and every other damager-kind admission check. Under
 * D158 those hits ALL scale. The scaling-math checks are carried
 * over unchanged.
 */
public class ShieldRuleTest {
    public static void run() {
        System.out.println("ShieldRuleTest");

        // ---- Covered: melee from ANY damager kind (D158) ----
        Check.ok(ShieldRule.shouldScale(ENTITY_ATTACK), "zombie melee scales");
        Check.ok(ShieldRule.shouldScale(ENTITY_ATTACK), "elite rabbit melee admitted (animal base is no longer a gate)");
        Check.ok(ShieldRule.shouldScale(ENTITY_ATTACK), "tamed wolf melee scales (damager kind is not an admission input)");
        Check.ok(ShieldRule.shouldScale(ENTITY_ATTACK), "player melee (PvP) scales");
        Check.ok(ShieldRule.shouldScale(ENTITY_SWEEP_ATTACK), "sweep attack scales");

        // ---- Covered: projectiles and explosions, any source ----
        Check.ok(ShieldRule.shouldScale(PROJECTILE), "skeleton arrow scales");
        Check.ok(ShieldRule.shouldScale(PROJECTILE), "player-shot arrow scales (D158: any damager)");
        Check.ok(ShieldRule.shouldScale(ENTITY_EXPLOSION), "creeper explosion scales");
        Check.ok(ShieldRule.shouldScale(ENTITY_EXPLOSION), "TNT / skull explosion scales (creeper-only arm removed)");
        Check.ok(ShieldRule.shouldScale(BLOCK_EXPLOSION), "block explosion scales");

        // ---- Covered: the causes excluded by design before 0.1.4 ----
        Check.ok(ShieldRule.shouldScale(MAGIC), "witch MAGIC splash scales (new in 0.1.4)");
        Check.ok(ShieldRule.shouldScale(POISON), "poison tick scales (new in 0.1.4)");
        Check.ok(ShieldRule.shouldScale(WITHER), "wither tick scales (new in 0.1.4)");
        Check.ok(ShieldRule.shouldScale(THORNS), "thorns scales (new in 0.1.4)");
        Check.ok(ShieldRule.shouldScale(LIGHTNING), "lightning scales (new in 0.1.4)");
        Check.ok(ShieldRule.shouldScale(FALLING_BLOCK), "falling block scales (new in 0.1.4)");
        Check.ok(ShieldRule.shouldScale(SONIC_BOOM), "sonic boom scales from ANY source (0.1.3: boss-only)");
        Check.ok(ShieldRule.shouldScale(DRAGON_BREATH), "dragon breath scales from ANY source (0.1.3: boss-only)");
        Check.ok(ShieldRule.shouldScale(CUSTOM), "custom (plugin-inflicted) scales");
        Check.ok(ShieldRule.shouldScale(OTHER), "unmapped future cause scales (Mike's bias: anything that hits her)");

        // ---- Excluded: Mike's named environmental family — UNSCALED ----
        Check.ok(!ShieldRule.shouldScale(FALL), "fall does NOT scale");
        Check.ok(!ShieldRule.shouldScale(SUFFOCATION), "suffocation does NOT scale");
        Check.ok(!ShieldRule.shouldScale(LAVA), "lava does NOT scale");
        Check.ok(!ShieldRule.shouldScale(DROWNING), "drowning does NOT scale");
        Check.ok(!ShieldRule.shouldScale(STARVATION), "starvation does NOT scale");
        Check.ok(!ShieldRule.shouldScale(FIRE), "fire does NOT scale");
        Check.ok(!ShieldRule.shouldScale(FIRE_TICK), "fire tick does NOT scale");
        Check.ok(!ShieldRule.shouldScale(CONTACT), "contact (cactus/berry) does NOT scale");
        Check.ok(!ShieldRule.shouldScale(HOT_FLOOR), "hot floor (magma) does NOT scale");
        Check.ok(!ShieldRule.shouldScale(FREEZE), "freeze does NOT scale");
        Check.ok(!ShieldRule.shouldScale(CRAMMING), "cramming does NOT scale");
        Check.ok(!ShieldRule.shouldScale(VOID), "void does NOT scale");
        Check.ok(!ShieldRule.shouldScale(FLY_INTO_WALL), "fly into wall does NOT scale");

        // ---- Excluded: the world/block tail, on Mike's principle ----
        Check.ok(!ShieldRule.shouldScale(WORLD_BORDER), "world border does NOT scale (world-inflicted)");
        Check.ok(!ShieldRule.shouldScale(CAMPFIRE), "campfire does NOT scale (contact family)");
        Check.ok(!ShieldRule.shouldScale(DRYOUT), "dryout does NOT scale (environmental)");
        Check.ok(!ShieldRule.shouldScale(MELTING), "melting does NOT scale (environmental)");

        // ---- The set is consistent across the whole enum ----
        int envCount = 0;
        boolean consistent = true;
        for (ShieldRule.Cause c : ShieldRule.Cause.values()) {
            if (ShieldRule.environmental(c)) envCount++;
            if (ShieldRule.shouldScale(c) == ShieldRule.environmental(c)) consistent = false;
        }
        Check.ok(consistent, "every cause: shouldScale == !environmental");
        Check.eq(17, envCount, "environmental exclusion set has exactly 17 causes");
        Check.ok(!ShieldRule.shouldScale(null), "null cause never scales");
        Check.ok(!ShieldRule.environmental(null), "null cause is not environmental (it is simply nothing)");

        // ---- Elite rabbit at the COMPENSATED factor (D158 keeps compensation) ----
        // Live-shaped ladder (the Nov 20, 2026 live pull): seal 5 dealt x2.35.
        CompensationTable live = CompensationTable.of(1.60, 1.75, 1.90, 2.05, 2.35, 2.35);
        double eliteEff = live.effectiveFactor(0.25, true, 5);
        Check.close(0.25 / 2.35, eliteEff, "elite factor at seal 5 = 0.25 / 2.35 (the live 0.1064)");
        // An elite rabbit hit arrives pre-scaled: unscaled 2.0 x dealt 2.35 = 4.7 on the event.
        // Composed take must be exactly a quarter of the UNSCALED hit: 0.5.
        Check.close(0.5, ShieldRule.scaled(4.7, eliteEff), "elite rabbit: 4.7 event -> 0.5 (quarter of unscaled 2.0)");
        // Non-elite rabbit-shaped hit (a plain animal that somehow deals damage): flat factor.
        Check.close(0.25, live.effectiveFactor(0.25, false, 5), "non-elite uses the flat factor at any seal");

        // ---- Flat-factor composed takes for the newly covered causes ----
        Check.close(1.5, ShieldRule.scaled(6.0, 0.25), "witch splash 6.0 -> 1.5");
        Check.close(0.25, ShieldRule.scaled(1.0, 0.25), "poison tick 1.0 -> 0.25");
        Check.close(2.0, ShieldRule.scaled(8.0, 0.25), "tamed wolf melee 8.0 -> 2.0");
        Check.close(5.625, ShieldRule.scaled(22.5, 0.25), "creeper-shaped blast 22.5 -> 5.625");

        // ---- Scaling math (carried from 0.1.3 unchanged) ----
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

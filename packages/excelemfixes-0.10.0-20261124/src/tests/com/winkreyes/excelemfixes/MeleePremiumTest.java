import excelemfixes.meleepremium.MeleeClassifier;
import excelemfixes.meleepremium.MeleePremiumMath;
import excelemfixes.meleepremium.MeleeScope;

/**
 * Plain-JVM suite for the melee-premium module's pure classes:
 * MeleePremiumMath (multiplier sanitize + premium sizing),
 * MeleeClassifier (which hits earn the premium) and MeleeScope
 * (the world-scope truth table). Uses the shared Check harness.
 */
public final class MeleePremiumTest {

    public static void run() {
        System.out.println("MeleePremiumTest:");

        // ---- multiplier sanitize: in-range passes through,
        //      penalty / absurd / junk -> shipped default 1.15
        Check.close(1.15, MeleePremiumMath.sanitizeMultiplier(1.15),
                "melee multiplier 1.15 kept");
        Check.close(1.0, MeleePremiumMath.sanitizeMultiplier(1.0),
                "melee multiplier 1.0 kept (deliberate no-premium)");
        Check.close(2.5, MeleePremiumMath.sanitizeMultiplier(2.5),
                "melee multiplier 2.5 kept");
        Check.close(1.15, MeleePremiumMath.sanitizeMultiplier(0.85),
                "melee multiplier <1 (penalty) -> default");
        Check.close(1.15, MeleePremiumMath.sanitizeMultiplier(3.5),
                "melee multiplier >3 -> default");
        Check.close(1.15, MeleePremiumMath.sanitizeMultiplier(Double.NaN),
                "melee multiplier NaN -> default");
        Check.close(1.15, MeleePremiumMath.sanitizeMultiplier(Double.POSITIVE_INFINITY),
                "melee multiplier +Inf -> default");

        // ---- premium sizing at the shipped 1.15
        Check.close(1.4835, MeleePremiumMath.premiumDamage(1.15, 1.29),
                "melee premium: Marvin-baseline 1.29 hit -> 1.4835");
        Check.close(0.1935, MeleePremiumMath.extraDamage(1.15, 1.29),
                "melee premium: extra on 1.29 is 0.1935");
        Check.close(2.0, MeleePremiumMath.premiumDamage(1.0, 2.0),
                "melee premium: multiplier 1.0 changes nothing");
        Check.close(0.0, MeleePremiumMath.premiumDamage(1.15, 0.0),
                "melee premium: zero damage stays zero");
        Check.close(0.0, MeleePremiumMath.premiumDamage(1.15, -4.0),
                "melee premium: negative damage stays zero");
        Check.close(0.0, MeleePremiumMath.premiumDamage(1.15, Double.NaN),
                "melee premium: NaN damage stays zero");
        Check.close(0.0, MeleePremiumMath.extraDamage(1.15, 0.0),
                "melee premium: extra on zero is zero");

        // ---- classifier: the one delivery that earns it
        Check.ok(hit("ENTITY_ATTACK", MeleeClassifier.DamagerKind.DIRECT_PLAYER,
                        false, false, false, false),
                "melee classifier: direct sword hit earns the premium");
        Check.ok(!hit("ENTITY_SWEEP_ATTACK", MeleeClassifier.DamagerKind.DIRECT_PLAYER,
                        false, false, false, false),
                "melee classifier: sweep echo excluded");
        Check.ok(!hit("PROJECTILE", MeleeClassifier.DamagerKind.PROJECTILE,
                        true, false, false, false),
                "melee classifier: arrow excluded");
        Check.ok(!hit("ENTITY_ATTACK", MeleeClassifier.DamagerKind.PROJECTILE,
                        false, false, false, false),
                "melee classifier: projectile damager excluded even on attack cause");
        Check.ok(!hit("ENTITY_ATTACK", MeleeClassifier.DamagerKind.OTHER,
                        false, false, false, false),
                "melee classifier: non-player damager excluded");
        Check.ok(!hit("ENTITY_ATTACK", MeleeClassifier.DamagerKind.DIRECT_PLAYER,
                        true, false, false, false),
                "melee classifier: EliteMobs ranged flag excluded");
        Check.ok(!hit("ENTITY_ATTACK", MeleeClassifier.DamagerKind.DIRECT_PLAYER,
                        false, true, false, false),
                "melee classifier: wand/staff in hand is magic delivery, excluded");
        Check.ok(!hit("ENTITY_ATTACK", MeleeClassifier.DamagerKind.DIRECT_PLAYER,
                        false, false, true, false),
                "melee classifier: EliteMobs magic hit excluded");
        Check.ok(!hit("ENTITY_ATTACK", MeleeClassifier.DamagerKind.DIRECT_PLAYER,
                        false, false, false, true),
                "melee classifier: custom/synthetic damage excluded");
        Check.ok(!hit("MAGIC", MeleeClassifier.DamagerKind.DIRECT_PLAYER,
                        false, false, false, false),
                "melee classifier: MAGIC cause excluded");
        Check.ok(!hit(null, MeleeClassifier.DamagerKind.DIRECT_PLAYER,
                        false, false, false, false),
                "melee classifier: null cause excluded");

        // ---- scope truth table (no elite-anywhere exception:
        //      the overworld stays untouched even for elites)
        Check.ok(MeleeScope.isWorldInScope(true, false, "world", "em_"),
                "melee scope: registry world in");
        Check.ok(MeleeScope.isWorldInScope(false, true, "world", "em_"),
                "melee scope: listed world in");
        Check.ok(MeleeScope.isWorldInScope(false, false, "em_primis", "em_"),
                "melee scope: prefix world in");
        Check.ok(MeleeScope.isWorldInScope(false, false, "EM_ARENA", "em_"),
                "melee scope: prefix is case-insensitive");
        Check.ok(!MeleeScope.isWorldInScope(false, false, "world", "em_"),
                "melee scope: overworld out");
        Check.ok(!MeleeScope.isWorldInScope(false, false, "world_the_end", "em_"),
                "melee scope: the End out");
        Check.ok(!MeleeScope.isWorldInScope(false, false, "em_primis", ""),
                "melee scope: empty prefix never matches");
        Check.ok(!MeleeScope.isWorldInScope(false, false, null, "em_"),
                "melee scope: null world out");
    }

    private static boolean hit(String cause, MeleeClassifier.DamagerKind kind,
                               boolean ranged, boolean magicWeapon,
                               boolean magicHit, boolean custom) {
        return MeleeClassifier.isPremiumHit(cause, kind, ranged,
                magicWeapon, magicHit, custom);
    }
}

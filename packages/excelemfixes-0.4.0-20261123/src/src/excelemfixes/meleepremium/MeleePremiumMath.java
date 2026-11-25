package excelemfixes.meleepremium;

/**
 * Pure math for the melee-premium module. No Bukkit or EliteMobs
 * types, so the whole class is unit-testable on a plain JVM.
 *
 * The module's one tuning idea: a direct melee hit by a player in
 * an EliteMobs-scope world deals {@code multiplier} times the
 * damage it would otherwise deal. The shipped default is 1.15 —
 * the melee-range risk premium from the reference server's
 * design ruling (a melee fighter eats ladder-scaled contact
 * damage at point-blank range while a ranged fighter kites for
 * free, so melee damage sits slightly ABOVE magic single-target).
 */
public final class MeleePremiumMath {

    private MeleePremiumMath() {}

    /** Shipped default multiplier. */
    public static final double DEFAULT_MULTIPLIER = 1.15;

    /**
     * Sanitizes the configured multiplier. A deliberate 1.0 (no
     * premium) is accepted; anything below 1.0 would be a melee
     * PENALTY — the opposite of the module's purpose — and
     * anything above 3.0 or non-finite is treated as a typo. Bad
     * values fall back to the shipped default rather than
     * silently becoming a penalty or a one-shot cannon.
     */
    public static double sanitizeMultiplier(double multiplier) {
        if (!Double.isFinite(multiplier)
                || multiplier < 1.0 || multiplier > 3.0) {
            return DEFAULT_MULTIPLIER;
        }
        return multiplier;
    }

    /** The damage a hit deals after the premium. */
    public static double premiumDamage(double multiplier, double damage) {
        if (!Double.isFinite(damage) || damage <= 0.0) return 0.0;
        return damage * sanitizeMultiplier(multiplier);
    }

    /** The extra damage the premium adds to a hit. */
    public static double extraDamage(double multiplier, double damage) {
        if (!Double.isFinite(damage) || damage <= 0.0) return 0.0;
        return premiumDamage(multiplier, damage) - damage;
    }
}

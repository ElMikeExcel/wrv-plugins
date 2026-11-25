package excelemfixes.meleemitigation;

/**
 * Pure math for the melee-mitigation module. No Bukkit or
 * EliteMobs types, so the whole class is unit-testable on a
 * plain JVM.
 *
 * The shape is borrowed from ExcelTrainingWheels' ShieldRule
 * (the scaling bones this module was directed to reuse): a
 * victim-axis damage scaling, damage x factor, where the
 * factor is 1 - mitigation and a factor of 1.0 is a no-op.
 * TrainingWheels scales damage for LISTED protected players;
 * this module scales it for ANY player whose main hand holds
 * a melee weapon — the protection follows the weapon in hand,
 * never a class, a skill pick, or a player list.
 *
 * All percentages are fractions (0.15 = 15%). Every number is
 * a config knob; the sanitizers here only reject values that
 * cannot be a tuning choice (negative, non-finite, or so
 * large the player would be effectively invulnerable).
 */
public final class MitigationMath {

    private MitigationMath() {}

    /** Shipped defaults (D206 ruling, Nov 23, 2026). */
    public static final double DEFAULT_BASE_PERCENT = 0.15;
    public static final double DEFAULT_TOTEM_PERCENT = 0.02;
    public static final double DEFAULT_SHIELD_PERCENT = 0.07;
    public static final double DEFAULT_MAX_TOTAL_PERCENT = 0.90;

    /**
     * Sanitizes one configured percentage. Valid range is
     * [0.0, 0.90]: 0.0 is a deliberate "this piece pays
     * nothing" tuning, anything above 0.90 for a single piece
     * or non-finite is treated as a typo and falls back to
     * that piece's shipped default rather than silently
     * making a player near-invulnerable.
     */
    public static double sanitizePercent(double percent, double fallback) {
        if (!Double.isFinite(percent) || percent < 0.0 || percent > 0.90) {
            return fallback;
        }
        return percent;
    }

    /** Sanitizes the total cap: valid range (0.0, 0.95]. */
    public static double sanitizeCap(double cap) {
        if (!Double.isFinite(cap) || cap <= 0.0 || cap > 0.95) {
            return DEFAULT_MAX_TOTAL_PERCENT;
        }
        return cap;
    }

    /**
     * Total mitigation for a holder: the base percentage,
     * plus the off-hand bonus when one applies, capped at
     * the configured maximum total. The off-hand bonus is
     * 0 when the off hand holds nothing qualifying — and the
     * caller only asks for a bonus at all when the main hand
     * holds a melee weapon, so a bonus can never pay alone.
     */
    public static double totalPercent(double basePercent,
                                      double offHandBonusPercent,
                                      double maxTotalPercent) {
        double base = sanitizePercent(basePercent, DEFAULT_BASE_PERCENT);
        double bonus = sanitizePercent(offHandBonusPercent, 0.0);
        double cap = sanitizeCap(maxTotalPercent);
        return Math.min(base + bonus, cap);
    }

    /** The TrainingWheels-shaped scaling factor: 1 - mitigation. */
    public static double factor(double totalPercent) {
        double total = sanitizePercent(totalPercent, 0.0);
        return 1.0 - total;
    }

    /**
     * Damage after mitigation (ShieldRule.scaled's shape):
     * non-positive or non-finite damage passes through
     * untouched; everything else is multiplied by the factor.
     */
    public static double mitigatedDamage(double damage, double totalPercent) {
        if (!Double.isFinite(damage) || damage <= 0.0) return damage;
        return damage * factor(totalPercent);
    }
}

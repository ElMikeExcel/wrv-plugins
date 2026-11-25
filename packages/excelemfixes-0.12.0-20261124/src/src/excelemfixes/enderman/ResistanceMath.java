package excelemfixes.enderman;

/**
 * Pure math for the enderman-resistance module. No Bukkit or
 * EliteMobs types, so the whole class is unit-testable on a plain
 * JVM.
 *
 * The module's one tuning idea: of the damage a player projectile
 * or magic hit WOULD deal to an enderman, only {@code fraction}
 * lands; the rest is resisted. Damage that EliteMobs applied
 * itself is reconciled by restoring the resisted share of the
 * measured effective-HP drop; damage vanilla refused outright is
 * re-applied through a player-sourced hit and then reconciled the
 * same way.
 */
public final class ResistanceMath {

    private ResistanceMath() {}

    public enum Decision { LANDED, REROUTE, NONE }

    /** Smallest HP movement treated as a real hit. */
    public static final double EPSILON = 1.0e-4;

    /**
     * Sanitizes the configured landed fraction. Anything outside
     * [0, 1] (or non-finite) falls back to the shipped default
     * 0.5 — a bad config value must never silently become total
     * immunity (0) or no resistance at all (1) by accident... 0 and
     * 1 ARE accepted when written deliberately; only out-of-range
     * and non-finite values fall back.
     */
    public static double sanitizeFraction(double fraction) {
        if (!Double.isFinite(fraction) || fraction < 0.0 || fraction > 1.0) return 0.5;
        return fraction;
    }

    /** The share of a measured hit that stays dealt. */
    public static double landedAmount(double fraction, double dealt) {
        if (!Double.isFinite(dealt) || dealt <= 0.0) return 0.0;
        return dealt * sanitizeFraction(fraction);
    }

    /** The share of a measured hit the module restores. */
    public static double restoreAmount(double fraction, double dealt) {
        if (!Double.isFinite(dealt) || dealt <= 0.0) return 0.0;
        return dealt * (1.0 - sanitizeFraction(fraction));
    }

    /**
     * What to do with one observed projectile/magic record once
     * its settle window closes: measured damage means the hit
     * landed (reconcile by restoring the resisted share); no
     * measured damage on a record kind that may be re-applied
     * means vanilla refused it (re-route); anything else is
     * dropped.
     */
    public static Decision decision(double measuredDelta, boolean canReroute) {
        if (Double.isFinite(measuredDelta) && measuredDelta > EPSILON) return Decision.LANDED;
        return canReroute ? Decision.REROUTE : Decision.NONE;
    }

    /**
     * Restores {@code amount} of effective HP across absorption
     * and health, undoing a damage split in reverse: health lost
     * is restored first (damage burns absorption before health,
     * so the last damage a hit dealt was to health), then any
     * remaining amount refills absorption. Restoration never
     * exceeds what was actually lost between the two snapshots.
     *
     * @return {newAbsorption, newHealth}
     */
    public static double[] restoreSplit(double absorptionBefore, double healthBefore,
                                        double absorptionNow, double healthNow,
                                        double amount) {
        double safeAmount = Double.isFinite(amount) ? Math.max(0.0, amount) : 0.0;
        double healthMissing = Math.max(0.0, healthBefore - healthNow);
        double absorptionMissing = Math.max(0.0, absorptionBefore - absorptionNow);
        double healthAdd = Math.min(safeAmount, healthMissing);
        double absorptionAdd = Math.min(safeAmount - healthAdd, absorptionMissing);
        return new double[]{absorptionNow + absorptionAdd, healthNow + healthAdd};
    }

    /**
     * Estimates the vanilla damage a connecting arrow/trident
     * would have rolled, mirroring vanilla's projectile formula
     * (ceil of base damage x speed). Used as the seed for a
     * re-applied hit when the real projectile was refused, so the
     * re-application is sized by the shot itself rather than by a
     * flat constant. Clamped to a sane band.
     */
    public static double arrowSeed(double baseDamage, double speed) {
        if (!Double.isFinite(baseDamage) || !Double.isFinite(speed)) return 0.0;
        if (baseDamage <= 0.0 || speed <= 0.0) return 0.0;
        double seed = Math.ceil(baseDamage * speed);
        return Math.min(30.0, seed);
    }

    /** Sanitizes a configured re-route seed: finite, in (0, 100]. */
    public static double sanitizeSeed(double seed, double fallback) {
        if (Double.isFinite(seed) && seed > 0.0 && seed <= 100.0) return seed;
        return fallback;
    }
}

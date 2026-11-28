package exceltestmode;

/**
 * Pure math for the test-mode module. No Bukkit or EliteMobs
 * types, so the whole class is unit-testable on a plain JVM.
 *
 * The module's one idea: a listed test player takes FULL
 * damage — the hit is never scaled, never cancelled — except
 * that the portion of a hit which would carry them below the
 * configured health floor is trimmed, so the hit lands them
 * exactly ON the floor. The numbers here are that trim:
 *
 *  - the victim's damage pool is health + absorption
 *    (absorption hearts are spent before health, so a hit
 *    eats the pool in that order and health after the hit
 *    is pool - damage whenever the hit outlasts absorption);
 *  - the damage the victim may still take is pool - floor;
 *  - a hit whose final damage fits inside that allowance
 *    passes completely untouched;
 *  - a hit that would break the floor is cut back to the
 *    allowance, expressed on the event's BASE damage through
 *    the event's own final/base ratio, so the event's
 *    modifier chain (armour, resistance, blocking) settles
 *    at the allowance rather than at a hand-computed number.
 *
 * The floor is in health points (2.0 = one heart). The
 * sanitizer only rejects values that cannot be a tuning
 * choice: non-positive (a floor of 0 is death itself — the
 * module would do nothing), non-finite, or above the cap
 * (a floor above any sane health pool would silently zero
 * every hit, which is god mode, not a test floor).
 */
public final class TestModeMath {

    private TestModeMath() {}

    /** Shipped default floor: 2.0 HP = one heart. */
    public static final double DEFAULT_FLOOR_HP = 2.0;

    /** Highest floor the sanitizer accepts (see class doc). */
    public static final double MAX_FLOOR_HP = 100.0;

    /** Sanitizes the configured floor (see class doc). */
    public static double sanitizeFloor(double floorHp) {
        if (!Double.isFinite(floorHp) || floorHp <= 0.0
                || floorHp > MAX_FLOOR_HP) {
            return DEFAULT_FLOOR_HP;
        }
        return floorHp;
    }

    /**
     * The damage the victim may still take before the hit
     * would break the floor: (health + absorption) - floor,
     * never negative. A victim already at or below the floor
     * may take 0 — further hits land as 0-damage events.
     */
    public static double allowedDamage(double health, double absorption,
                                       double floorHp) {
        double pool = health + Math.max(0.0, absorption);
        return Math.max(0.0, pool - sanitizeFloor(floorHp));
    }

    /**
     * True when a hit of the given FINAL damage would take
     * the victim below the floor. Non-positive or non-finite
     * damage never breaks the floor (nothing to trim).
     * Landing exactly ON the floor is not breaking it.
     */
    public static boolean wouldBreakFloor(double health, double absorption,
                                          double finalDamage, double floorHp) {
        if (!Double.isFinite(finalDamage) || finalDamage <= 0.0) {
            return false;
        }
        double pool = health + Math.max(0.0, absorption);
        return pool - finalDamage < sanitizeFloor(floorHp);
    }

    /**
     * The new BASE damage that makes the event's final damage
     * settle at allowedFinal, preserving the event's own
     * final/base ratio (the modifier chain's pricing). When
     * the allowance covers the whole hit, the base passes
     * untouched; when the allowance is 0, the base becomes 0
     * (the event still fires — nothing is ever cancelled).
     */
    public static double scaledBaseDamage(double baseDamage, double finalDamage,
                                          double allowedFinal) {
        if (!Double.isFinite(baseDamage) || !Double.isFinite(finalDamage)
                || finalDamage <= 0.0) {
            return baseDamage;
        }
        if (allowedFinal <= 0.0) return 0.0;
        if (allowedFinal >= finalDamage) return baseDamage;
        return baseDamage * (allowedFinal / finalDamage);
    }
}

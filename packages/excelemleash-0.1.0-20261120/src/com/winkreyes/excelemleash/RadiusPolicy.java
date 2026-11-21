package com.winkreyes.excelemleash;

/**
 * Pure radius-resolution policy for Leg B (radii management).
 *
 * Rules (D167):
 *  - An explicit per-boss override always wins, in both directions
 *    (raise, lower, set on an unleashed boss, or 0 to unleash one).
 *    An override is a deliberate admin act, distinct from the floor.
 *  - Without an override, a boss that HAS a leash radius gets
 *    max(current, floor). Values above the floor are never lowered.
 *  - Without an override, a boss with NO leash radius (current == 0,
 *    EM's disabled sentinel) stays unleashed: the floor never
 *    invents leashes.
 */
public final class RadiusPolicy {

    public enum ChangeKind { NONE, FLOOR_RAISE, OVERRIDE }

    private RadiusPolicy() {
    }

    /**
     * The radius that should be in force for a boss.
     *
     * @param override the per-boss override from config, or null
     * @param current  the boss's current (file/loaded) leash radius; 0 = none
     * @param floor    the configured minimum radius
     * @return the effective target radius
     */
    public static double effectiveTarget(Double override, double current, double floor) {
        if (override != null) return override.doubleValue();
        if (current > 0) return Math.max(current, floor);
        return current;
    }

    /** How applying the policy to this boss would change it. */
    public static ChangeKind classify(Double override, double current, double floor) {
        double target = effectiveTarget(override, current, floor);
        if (target == current) return ChangeKind.NONE;
        return override != null ? ChangeKind.OVERRIDE : ChangeKind.FLOOR_RAISE;
    }
}

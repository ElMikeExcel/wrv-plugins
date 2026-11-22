package com.winkreyes.excelemleash;

/**
 * Pure decisions for the D183 return engine (ExcelEMLeash 0.3.0).
 *
 * Mike's MMO leash (Nov 21, 2026, range picked at 96): an elite
 * fights normally anywhere within the range of its spawn point;
 * pulled beyond it, it deaggros, walks home, and heals to full on
 * arrival — but player damage mid-return re-aggroes it instantly
 * and the heal never happens. The Bukkit/EM glue in EmLeashBridge
 * resolves the facts (distances, combat state, timestamps); this
 * policy decides. No Bukkit or EliteMobs types appear here, so the
 * whole state machine is unit-testable on the bare JVM.
 */
public final class ReturnPolicy {

    /** Close enough to the spawn point to count as home. */
    public static final double ARRIVE_RADIUS = 2.0;

    /** No distance progress for this long = the walk is stuck. */
    public static final long STUCK_PROGRESS_MILLIS = 5_000L;

    /** A return may never run longer than this, progress or not. */
    public static final long MAX_RETURN_MILLIS = 60_000L;

    /** What player damage does to an elite. */
    public enum DamageOutcome { NONE, REAGGRO }

    private ReturnPolicy() {
    }

    /**
     * A return starts only for an elite that is in combat and has
     * been pulled strictly beyond its leash range from home. At or
     * inside the range, aggro behaves normally — no deaggro ever
     * (a camp fight can never trip the leash).
     */
    public static boolean shouldTriggerReturn(boolean alreadyReturning, boolean inCombat,
                                              double distanceFromHome, double range) {
        return !alreadyReturning && inCombat && distanceFromHome > range;
    }

    /** Arrival is measured against the spawn point, not the path. */
    public static boolean hasArrived(double distanceFromHome) {
        return distanceFromHome <= ARRIVE_RADIUS;
    }

    /**
     * Teleport fallback (EM's own teleport-on-fail precedent): the
     * walk is abandoned for a teleport home when it has made no
     * progress for STUCK_PROGRESS_MILLIS, or has run past
     * MAX_RETURN_MILLIS in total.
     */
    public static boolean shouldTeleportFallback(long returningMillis, long sinceProgressMillis) {
        return returningMillis > MAX_RETURN_MILLIS || sinceProgressMillis > STUCK_PROGRESS_MILLIS;
    }

    /**
     * Player damage while RETURNING breaks the return: instant
     * re-aggro on the damager, and the arrival heal never runs.
     * Damage to an elite that is not returning is ordinary combat.
     */
    public static DamageOutcome onPlayerDamage(boolean returning) {
        return returning ? DamageOutcome.REAGGRO : DamageOutcome.NONE;
    }
}

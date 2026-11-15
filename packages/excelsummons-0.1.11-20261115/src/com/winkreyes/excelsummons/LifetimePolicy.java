package com.winkreyes.excelsummons;

/**
 * Lifetime decisions, pure and unit-tested. The two incidents that own
 * this policy (DESIGN §6.2):
 *  - Wolf: a relog orphaned every prior wolf and reset the budget —
 *    lifetime cannot live in a casting session's memory.
 *  - Familiar: `undo: 20000` was configured and animals still persisted
 *    for hours — a configured lifetime nobody enforces is a rumor.
 * So expiry is decided here, enforced by the bridge sweep, and every
 * rule carries a live proof in the ship gates before "done" is said.
 *
 * The one hard rule: a summon is NEVER despawned while it is ridden.
 * Dropping the family out of the sky on a timer is the one failure
 * this tree must never produce — removal defers until riders are off.
 *
 * Excel — Wink Reyes Valley (Iris / Mike).
 */
public final class LifetimePolicy {

    public enum Decision { KEEP, REMOVE, DEFER_RIDDEN }

    /**
     * @param rec        the summon's registry record
     * @param nowMillis  current time
     * @param ridden     the entity currently carries at least one passenger
     * @param shuttingDown the plugin/server is stopping (combat summons are
     *                   removed on disable; pets and mounts are exempt)
     */
    public static Decision decide(SummonRecord rec, long nowMillis, boolean ridden,
                                  boolean shuttingDown) {
        if (rec == null) return Decision.KEEP;
        boolean due = rec.expiryDue(nowMillis)
                || (shuttingDown && rec.branch() == SummonRecord.Branch.COMBAT);
        if (!due) return Decision.KEEP;
        if (ridden) return Decision.DEFER_RIDDEN;
        return Decision.REMOVE;
    }

    /** Combat summons leave with their caster (logout, death). */
    public static boolean leavesWithCaster(SummonRecord rec) {
        return rec != null && rec.branch() == SummonRecord.Branch.COMBAT;
    }

    /** Expiry timestamp for a fresh registration; null = no bridge expiry. */
    public static Long expiresAt(long nowMillis, long durationMillis) {
        return durationMillis > 0 ? nowMillis + durationMillis : null;
    }
}

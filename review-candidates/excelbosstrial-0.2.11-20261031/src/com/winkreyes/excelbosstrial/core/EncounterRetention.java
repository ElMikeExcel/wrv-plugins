/* Excel WRV maintenance; existing authorship retained. */ package com.winkreyes.excelbosstrial.core;

/**
 * 0.2.7 (Iris audit, Oct 24, 2026): when an encounter ledger may be
 * forgotten, and what a re-opened ledger must set aside.
 *
 * <p>Up to 0.2.6 the maintenance pass dropped a ledger the moment
 * {@code Bukkit.getEntity(uuid)} returned null - but that call is
 * null for an UNLOADED boss too, not only a removed one. A family
 * that fought an elder, swam home (or logged off) and came back
 * found the elder still wounded under a brand-new ledger at the full
 * pool: the 70% share became impossible and the honest kill was
 * DENIED with a 6 h cooldown (rig-proven on 0.2.6).
 *
 * <p>The rule now:
 * <ul>
 *   <li>boss loaded and alive - keep;</li>
 *   <li>boss loaded and dead - prune (the death handler normally got
 *       there first);</li>
 *   <li>boss missing while the chunk it was last seen in is loaded
 *       with its entities - it really is gone (despawn, burrow,
 *       remove): prune after {@link #GONE_STREAK} consecutive passes
 *       (entity loading can trail chunk loading by a moment);</li>
 *   <li>boss missing because its area is unloaded - keep, until
 *       nobody has touched the fight for {@link #DEFAULT_TTL_MS}.</li>
 * </ul>
 *
 * <p>A ledger can still be lost (a server restart, or the TTL). The
 * boss then carries damage no ledger saw, so a re-opened ledger sets
 * that damage aside: the share is measured on the fight that is
 * actually left. Only a boss whose bar this plugin already raised
 * (pool at or above the trial base) counts as a re-open - a fresh
 * vanilla boss never does - so nothing about a normal fight changes.
 */
public final class EncounterRetention {

    /** Untouched-and-unloaded fights are forgotten after 24 hours. Long
     *  on purpose: a forgotten ledger re-opens with its unseen damage
     *  set aside, so a short TTL would let "let something else wound
     *  it, wait, then finish it" through. A restart clears ledgers
     *  anyway. */
    public static final long DEFAULT_TTL_MS = 24L * 60L * 60L * 1000L;
    /** Consecutive "area loaded, boss missing" passes before pruning. */
    public static final int GONE_STREAK = 2;

    public enum Decision { KEEP, PRUNE }

    private EncounterRetention() {
    }

    /**
     * @param entityFound  the boss entity is loaded (getEntity non-null)
     * @param entityDead   the loaded boss is dead
     * @param areaLoaded   the chunk the boss was last seen in is loaded
     *                     with its entities (false when unknown)
     * @param goneStreak   consecutive passes (including this one) with
     *                     the area loaded and the boss missing
     */
    public static Decision decide(boolean entityFound, boolean entityDead,
                                  boolean areaLoaded, int goneStreak,
                                  long lastTouchMs, long nowMs, long ttlMs) {
        if (entityFound) {
            return entityDead ? Decision.PRUNE : Decision.KEEP;
        }
        if (areaLoaded && goneStreak >= GONE_STREAK) {
            return Decision.PRUNE;
        }
        if (ttlMs > 0 && nowMs - lastTouchMs > ttlMs) {
            return Decision.PRUNE;
        }
        return Decision.KEEP;
    }

    /**
     * Damage a re-opened ledger must set aside, for a boss whose pool
     * this plugin scales. 0 for a fresh boss (its vanilla bar sits
     * below the trial base) or an unwounded one.
     *
     * @param openingMax       the pool the new ledger opens at
     * @param vanillaMaxBefore the boss's max health before the open
     * @param baseHealth       the trial base pool for the boss kind
     * @param healthNow        the boss's health after the open
     */
    public static double unseenOnReopen(double openingMax, double vanillaMaxBefore,
                                        double baseHealth, double healthNow) {
        if (!(baseHealth > 0) || !(openingMax > 0) || Double.isNaN(healthNow)) {
            return 0;
        }
        if (vanillaMaxBefore < baseHealth - 0.01) {
            return 0; // a fresh boss: this plugin never raised its bar
        }
        double unseen = openingMax - Math.max(0, healthNow);
        return unseen > 0.01 ? unseen : 0;
    }

    /**
     * Damage a re-opened DRAGON ledger must set aside: everything the
     * tracked dragon already lost across the phases the trial spans.
     */
    public static double dragonUnseen(boolean phase2, double phase1Max, double phase2Max,
                                      double currentMax, double healthNow) {
        if (Double.isNaN(healthNow)) {
            return 0;
        }
        double h = Math.max(0, healthNow);
        double unseen;
        if (phase2) {
            if (Math.abs(currentMax - phase2Max) > 0.01) {
                return 0;
            }
            unseen = phase1Max + Math.max(0, phase2Max - h);
        } else {
            if (Math.abs(currentMax - phase1Max) > 0.01) {
                return 0; // phase setup not applied yet: a fresh dragon
            }
            unseen = Math.max(0, phase1Max - h);
        }
        return unseen > 0.01 ? unseen : 0;
    }
}


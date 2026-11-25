/* Excel WRV maintenance; existing authorship retained. */ package com.winkreyes.excelbosstrial.core;

/**
 * The damage-intake factor (ExcelBossTrial 0.3.0, SPEC section
 * 0.3): the platform clamps MAX_HEALTH at 1,024 (Paper 26.3-235,
 * rig-proven Nov 1, 2026 - an attribute set to a 2,400 pool reads
 * back an effective value of exactly 1024.0, and setHealth above
 * the clamp throws), so a seal pool above the cap cannot live on
 * the entity's health bar. The bar therefore carries an ENTITY
 * pool E = min(configured pool P, the cap), and every governed hit
 * is scaled by f = E / P before it reaches the entity: the fight
 * lasts exactly as long as the configured pool says, the ledger
 * judges in configured space throughout, and verdicts, shares and
 * growth behave exactly as they did when pools fit under the cap.
 *
 * The factor is PER ENCOUNTER and CURRENT: participant growth
 * raises P mid-fight, so f shrinks at every rally step and the
 * adapter recomputes it from the ledger's live maximum at every
 * touch. When P <= cap, f = 1 and every function here is the
 * identity - sub-cap fights (today's elder guardians and dragon)
 * are byte-for-byte the 0.2.11 fight.
 *
 * Pure arithmetic, unit-tested directly; the Bukkit adapter owns
 * the attribute writes and the event scaling that use it.
 */
public final class IntakeFactor {

    private IntakeFactor() {
    }

    /** True when the configured pool overflows the entity cap. */
    public static boolean active(double entityCap, double pool) {
        return pool > entityCap && entityCap > 0;
    }

    /** f = E / P, or exactly 1 when the pool fits under the cap. */
    public static double factor(double entityCap, double pool) {
        if (!active(entityCap, pool)) {
            return 1.0;
        }
        return entityCap / pool;
    }

    /** E: the health-bar maximum the entity actually carries. */
    public static double entityMax(double entityCap, double pool) {
        if (pool <= 0 || Double.isNaN(pool)) {
            return pool;
        }
        return active(entityCap, pool) ? entityCap : pool;
    }

    /** One governed hit, configured (ledger) space -> entity space. */
    public static double toEntity(double ledgerDamage, double factor) {
        return ledgerDamage * factor;
    }

    /** One landed hit, entity space -> configured (ledger) space.
     *  A non-positive factor cannot arise from {@link #factor}
     *  (it is E/P with E > 0, or 1); if one ever arrives anyway it
     *  reads as identity - Infinity must never enter a ledger. */
    public static double toLedger(double entityDamage, double factor) {
        return factor <= 0.0 || factor >= 1.0 ? entityDamage : entityDamage / factor;
    }

    /**
     * The entity health that matches a ledger state: the bar
     * mirrors the ledger's remaining fraction of the pool, so the
     * entity dies in the same blow the ledger's damage reaches
     * the pool - death timing is exact, never approximate. The
     * triggering hit's own entity damage is added by the caller
     * when it sets health mid-event (the event subtracts it right
     * after the handler returns).
     */
    public static double entityHealthFor(double entityCap, double pool,
                                         double totalLedgerDamage, double unseenDamage) {
        double e = entityMax(entityCap, pool);
        if (pool <= 0 || Double.isNaN(pool)) {
            return e;
        }
        double remaining = pool - Math.max(0.0, totalLedgerDamage)
                - Math.max(0.0, unseenDamage);
        double fraction = remaining / pool;
        if (fraction < 0.0) {
            fraction = 0.0;
        }
        if (fraction > 1.0) {
            fraction = 1.0;
        }
        return e * fraction;
    }
}

// ExcelBossTrial - a Wink Reyes Valley Excel plugin.

/* Excel WRV maintenance; existing authorship retained. */ package com.winkreyes.excelbosstrial.core;

/**
 * The pool-exhaustion verdict (ExcelBossTrial 0.4.0, the
 * Wither-night fix train, PM-2). Before 0.4.0 an encounter could
 * only close on the entity's death event: a pool fully dealt
 * against a boss whose vanilla health kept refilling (the Wither
 * heals on every kill) was a permanent zombie encounter - the
 * bar read 0 on a living boss and nothing ever resolved it.
 *
 * The judge is the ledger half of the fix: NET damage (gross
 * dealt minus pool-space healing, EncounterLedger.netDamage)
 * reaching the pool IS the kill, judged in configured space
 * exactly like the share. The adapter half executes the verdict
 * through the real death path (a governed lethal blow), so
 * EntityDeathEvent, drops, seal credit and claims all run as
 * for a natural kill - this class decides; it never executes.
 *
 * Pure arithmetic, unit-tested directly.
 */
public final class VerdictJudge {

    private VerdictJudge() {
    }

    /**
     * True when net damage has spent the pool. A non-positive or
     * NaN pool is never exhausted (no pool, no verdict - the
     * pre-0.4.0 behavior for untracked fights), and NaN net
     * reads as unspent.
     */
    public static boolean exhausted(double pool, double netDamage) {
        if (Double.isNaN(pool) || pool <= 0) {
            return false;
        }
        if (Double.isNaN(netDamage)) {
            return false;
        }
        return netDamage >= pool - 1e-9;
    }
}

// ExcelBossTrial - a Wink Reyes Valley Excel plugin.

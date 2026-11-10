/* Excel WRV maintenance; existing authorship retained. */ package com.winkreyes.excelbosstrial.core;

/**
 * Heal-space conversion (ExcelBossTrial 0.4.0, the Wither-night
 * fix train, PM-1): the ledger judges in configured (pool)
 * space, the entity heals in entity space, and {@link
 * IntakeFactor} converts damage entity-&gt;pool by dividing by
 * the intake factor f. Healing is the same conversion run the
 * other way through damage's lens: a heal of H entity HP undoes
 * a hit whose LEDGER size was H / f, because a governed hit of
 * ledger size L lands as L * f entity damage (IntakeFactor.
 * toEntity). At f = 1 (every sub-cap fight) this is the identity,
 * exactly like the damage side.
 *
 * The clamp law lives here too, so the adapter never re-derives
 * it: a heal can only un-deal damage that was actually dealt -
 * net damage never goes below 0 - and the effective part of a
 * heal is the part that lands on missing health (overheal is
 * not healing, the FightTracker's rule since 0.3.1).
 *
 * Pure arithmetic, unit-tested directly.
 */
public final class HealMath {

    private HealMath() {
    }

    /**
     * The effective part of one regain event: the raw amount
     * capped at the health actually missing. Non-positive or
     * NaN inputs read as 0.
     */
    public static double effectiveHeal(double rawAmount, double healthBefore,
                                       double maxHealth) {
        if (Double.isNaN(rawAmount) || rawAmount <= 0) {
            return 0;
        }
        double missing = maxHealth - healthBefore;
        if (Double.isNaN(missing) || missing <= 0) {
            return 0;
        }
        return Math.min(rawAmount, missing);
    }

    /**
     * One heal, entity space -> configured (pool) space: the
     * inverse of IntakeFactor.toEntity for the same factor.
     * Mirrors IntakeFactor.toLedger's fail-safe: a degenerate
     * factor reads as identity rather than admitting Infinity
     * into a ledger.
     */
    public static double toPoolSpace(double entityHeal, double factor) {
        if (Double.isNaN(entityHeal) || entityHeal <= 0) {
            return 0;
        }
        return factor <= 0.0 || factor >= 1.0 ? entityHeal : entityHeal / factor;
    }

    /**
     * Net damage after one heal lands: gross damage minus
     * healing, clamped at 0 - the boss can never be healed past
     * the damage the fight has actually dealt it (its opening
     * state), however large the heal.
     */
    public static double netAfterHeal(double grossDamage, double healedPool,
                                      double healPoolSpace) {
        double healed = Math.max(0.0, healedPool) + Math.max(0.0, healPoolSpace);
        double net = Math.max(0.0, grossDamage) - healed;
        return Math.max(0.0, net);
    }
}

// ExcelBossTrial - a Wink Reyes Valley Excel plugin.

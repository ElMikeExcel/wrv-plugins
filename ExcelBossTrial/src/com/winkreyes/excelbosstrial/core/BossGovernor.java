package com.winkreyes.excelbosstrial.core;

/**
 * Per-encounter damage governor for the wither / elder guardian /
 * warden chapters: the DragonTrial 0.1.1 DamageGovernor semantics
 * (P1 per-event cap first, then the P2 per-tick explosion pool),
 * parameterized by BossCaps instead of the dragon's TrialConfig.
 * One instance per tracked boss encounter; all calls happen on the
 * main thread. The dragon chapter keeps its own ported governor.
 */
public final class BossGovernor {

    /** The governed outcome of one damage event. */
    public static final class Result {
        /** The damage the encounter will actually take / record. */
        public final double damage;
        /** True if the P1 per-event cap trimmed this event. */
        public final boolean perEventTrimmed;
        /** True if the P2 tick budget trimmed this event. */
        public final boolean budgetTrimmed;

        Result(double damage, boolean perEventTrimmed, boolean budgetTrimmed) {
            this.damage = damage;
            this.perEventTrimmed = perEventTrimmed;
            this.budgetTrimmed = budgetTrimmed;
        }

        public boolean trimmed() {
            return perEventTrimmed || budgetTrimmed;
        }

        /** Short rule tag for the console trim line: "P1", "P2" or "P1+P2". */
        public String ruleLabel() {
            if (perEventTrimmed && budgetTrimmed) {
                return "P1+P2";
            }
            if (perEventTrimmed) {
                return "P1";
            }
            if (budgetTrimmed) {
                return "P2";
            }
            return "";
        }
    }

    private long poolTick = Long.MIN_VALUE;
    private double poolRemaining;

    /**
     * Govern one damage event against one boss.
     *
     * @param rawDamage the event's final damage, before governing
     * @param explosion true when the damage cause is an explosion
     *                  (BLOCK_EXPLOSION or ENTITY_EXPLOSION)
     * @param tick      the current server tick; the pool resets
     *                  whenever it changes
     * @param caps      the chapter's live caps (passed per call so a
     *                  config reload takes effect immediately)
     */
    public Result govern(double rawDamage, boolean explosion, long tick, BossCaps caps) {
        // Math.max propagates NaN; a junk damage value must degrade
        // to "no damage", never to a NaN the adapter would scale by.
        double damage = Double.isNaN(rawDamage) ? 0.0 : Math.max(0.0, rawDamage);
        boolean p1 = false;
        boolean p2 = false;
        if (caps != null && caps.perEventEnabled() && damage > caps.perEventDamage()) {
            damage = caps.perEventDamage();
            p1 = true;
        }
        if (explosion && caps != null && caps.explosionEnabled()) {
            if (tick != poolTick) {
                poolTick = tick;
                poolRemaining = caps.explosionPerTick();
            }
            if (damage > poolRemaining) {
                damage = poolRemaining;
                p2 = true;
            }
            poolRemaining -= damage;
        }
        return new Result(damage, p1, p2);
    }

    /** Budget left in the current tick's pool (test/diagnostic surface). */
    public double poolRemaining() {
        return poolRemaining;
    }

    /** The tick the current pool belongs to (test/diagnostic surface). */
    public long poolTick() {
        return poolTick;
    }
}

package com.winkreyes.excelbosstrial.dragon;

/**
 * The 0.1.1 anti-nuke governor (DRAGON-CHEESE-RESEARCH.md section 2).
 * Pure logic, no Bukkit: the adapter feeds it each damage event's
 * final damage, whether the event's cause is an explosion, and the
 * current server tick; it returns the damage the trial will judge.
 *
 * P1 — per-event cap: no single damage event against the dragon may
 * exceed caps.per-event-damage (default 60). Applied to EVERY event,
 * explosion or not, BEFORE the state machine's verdict, so the flip
 * logic only ever sees governed values.
 *
 * P2 — per-tick explosion budget: explosion-cause events (the adapter
 * classifies BLOCK_EXPLOSION + ENTITY_EXPLOSION) pool against
 * caps.explosion-per-tick (default 75) within one server tick. The
 * pool refills when the tick changes. Non-explosion events never
 * touch the pool. One full bed blast lands; a same-tick chain of
 * beds / crystals / TNT minecarts cannot delete a phase.
 *
 * Order matters: P1 first (a single blast is judged as one event),
 * then P2 pools what P1 let through.
 *
 * Fail-soft: a cap configured at 0 or negative is DISABLED (see
 * TrialConfig.capValue) and its rule is an exact pass-through, so
 * all-caps-off is byte-for-byte 0.1.0 behaviour.
 *
 * One instance per End world (held by the adapter's per-world
 * record); all calls happen on the main thread.
 */
public final class DamageGovernor {

    /** The governed outcome of one damage event. */
    public static final class Result {
        /** The damage the trial judges (and lets through). */
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

        /** True if either rule trimmed this event. */
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
     * Govern one damage event against the dragon.
     *
     * @param rawDamage the event's final damage, before governing
     * @param explosion true when the damage cause is an explosion
     *                  (BLOCK_EXPLOSION or ENTITY_EXPLOSION)
     * @param tick      the current server tick (any monotonic tick id;
     *                  the pool resets whenever it changes)
     * @param cfg       the live trial config (passed per call so a
     *                  config reload takes effect immediately)
     */
    public Result govern(double rawDamage, boolean explosion, long tick, TrialConfig cfg) {
        double damage = Math.max(0.0, rawDamage);
        boolean p1 = false;
        boolean p2 = false;
        if (cfg.perEventCapEnabled() && damage > cfg.perEventDamageCap) {
            damage = cfg.perEventDamageCap;
            p1 = true;
        }
        if (explosion && cfg.explosionBudgetEnabled()) {
            if (tick != poolTick) {
                poolTick = tick;
                poolRemaining = cfg.explosionTickBudget;
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

package com.winkreyes.excelbosstrial.core;

/**
 * One boss chapter's anti-nuke caps (the DragonTrial 0.1.1 pattern,
 * generalized in SEAL-INTEGRITY-RESEARCH.md section 5.1):
 *
 * P1 - per-event damage cap: no single damage event against the boss
 *      counts for more than this many HP.
 * P2 - per-tick explosion budget: explosion-cause damage pools
 *      against this budget per server tick.
 *
 * A value of 0 or negative DISABLES that rule (fail-soft off, the
 * DragonTrial convention): with both off the chapter is a pure
 * observer of damage, exactly like the boss had no governor.
 */
public record BossCaps(double perEventDamage, double explosionPerTick) {

    public static final BossCaps OFF = new BossCaps(0, 0);

    public BossCaps {
        perEventDamage = sanitize(perEventDamage);
        explosionPerTick = sanitize(explosionPerTick);
    }

    private static double sanitize(double v) {
        if (Double.isNaN(v) || v <= 0) {
            return 0.0;
        }
        return Math.min(v, 100000.0);
    }

    public boolean perEventEnabled() {
        return perEventDamage > 0;
    }

    public boolean explosionEnabled() {
        return explosionPerTick > 0;
    }

    public boolean anyEnabled() {
        return perEventEnabled() || explosionEnabled();
    }
}

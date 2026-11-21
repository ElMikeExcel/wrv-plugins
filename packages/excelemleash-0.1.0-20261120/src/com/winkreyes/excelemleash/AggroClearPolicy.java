package com.winkreyes.excelemleash;

/**
 * Pure targeting decision for Leg A (aggro-clear). The Bukkit/EM glue
 * resolves the three facts from the live event; this policy decides.
 *
 * Scope guard (D167): clear only when the event's entity resolves to
 * an EliteMobs RegionalBossEntity. Dungeon bosses and natural elites
 * are never touched, even if an event somehow reaches the handler.
 */
public final class AggroClearPolicy {

    public enum Decision {
        CLEAR,
        SKIP_LEG_DISABLED,
        SKIP_NO_ELITE,
        SKIP_NOT_REGIONAL
    }

    private AggroClearPolicy() {
    }

    public static Decision decide(boolean legEnabled, boolean eliteResolved, boolean regionalBoss) {
        if (!legEnabled) return Decision.SKIP_LEG_DISABLED;
        if (!eliteResolved) return Decision.SKIP_NO_ELITE;
        if (!regionalBoss) return Decision.SKIP_NOT_REGIONAL;
        return Decision.CLEAR;
    }
}

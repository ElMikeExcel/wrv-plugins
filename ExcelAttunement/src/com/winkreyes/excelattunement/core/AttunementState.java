package com.winkreyes.excelattunement.core;

/**
 * One player's attunement state machine — pure logic, clock injected
 * by the caller so every rule is unit-testable. In-memory only by
 * design: a server restart clears all stances, which is correct (a
 * stance is a mid-fight commitment, not progression).
 *
 * Rules (Combat Attunement design, Oct 19, 2026):
 *  - A landed martial hit SETS/REFRESHES martial stance.
 *  - A COMBAT cast SETS/REFRESHES caster stance. A SUPPORT cast does
 *    the same ONLY while in combat (dealt or took damage within the
 *    combat-context window). A UTILITY cast never does.
 *  - While martial is active the Bukkit layer blocks casts outright
 *    (PreCastEvent), and while caster is active it zeroes weapon
 *    damage, so a stance can never flip directly. The guards here are
 *    defense in depth AND the no-flip rule for zeroed hits: a hit that
 *    was zeroed never reaches onMartialHit (the glue only calls it for
 *    final damage > 0), and even if called, onMartialHit refuses
 *    while caster is active.
 *  - Dealing damage refreshes only the combat context. TAKING hostile
 *    damage refreshes the combat context AND the active stance (a
 *    fight you are being hit in keeps your commitment current).
 *  - Expiry is lazy: activeStance(now) reports NONE once the window
 *    passes; the glue polls pollExpired(now) to send the one expire
 *    line exactly once.
 */
public final class AttunementState {
    public enum HitResult { SET, REFRESHED, IGNORED_CASTER_ACTIVE }
    public enum CastResult { SET, REFRESHED, NOT_QUALIFYING, IGNORED_MARTIAL_ACTIVE }

    private final long windowMs;
    private final long combatContextMs;

    private Stance stance = Stance.NONE;
    private long stanceExpiresAt = Long.MIN_VALUE;
    private long lastCombatAt = Long.MIN_VALUE;
    private boolean expiryPending = false;

    public AttunementState(long windowSeconds, long combatContextSeconds) {
        this.windowMs = windowSeconds * 1000L;
        this.combatContextMs = combatContextSeconds * 1000L;
    }

    /** The stance in force at {@code now}; NONE once the window has passed. */
    public Stance activeStance(long now) {
        if (stance != Stance.NONE && now >= stanceExpiresAt) return Stance.NONE;
        return stance;
    }

    /** True if the player is inside the combat-context window at {@code now}. */
    public boolean inCombat(long now) {
        return lastCombatAt != Long.MIN_VALUE && now - lastCombatAt <= combatContextMs;
    }

    /** Milliseconds of stance left at {@code now}; 0 when not stanced. */
    public long remainingMs(long now) {
        if (activeStance(now) == Stance.NONE) return 0L;
        return Math.max(0L, stanceExpiresAt - now);
    }

    /** Whole seconds of stance left, rounded up (for the action bar). */
    public long remainingSeconds(long now) {
        long ms = remainingMs(now);
        return ms == 0L ? 0L : (ms + 999L) / 1000L;
    }

    /**
     * A martial-weapon hit LANDED (final damage > 0 — the glue enforces
     * that; zeroed hits never call this). Sets or refreshes martial.
     */
    public HitResult onMartialHit(long now) {
        Stance active = activeStance(now);
        if (active == Stance.CASTER) return HitResult.IGNORED_CASTER_ACTIVE;
        lastCombatAt = now;
        stanceExpiresAt = now + windowMs;
        if (active == Stance.MARTIAL) return HitResult.REFRESHED;
        stance = Stance.MARTIAL;
        expiryPending = false;
        return HitResult.SET;
    }

    /**
     * A spell actually cast (successful CastEvent — fizzles and failed
     * attempts never call this). Whether it attunes depends on class
     * and, for SUPPORT, on the combat context.
     */
    public CastResult onCast(SpellClass spellClass, long now) {
        Stance active = activeStance(now);
        if (active == Stance.MARTIAL) return CastResult.IGNORED_MARTIAL_ACTIVE;
        boolean qualifies = switch (spellClass) {
            case COMBAT -> true;
            case SUPPORT -> inCombat(now);
            case UTILITY -> false;
        };
        if (!qualifies) return CastResult.NOT_QUALIFYING;
        stanceExpiresAt = now + windowMs;
        if (active == Stance.CASTER) return CastResult.REFRESHED;
        stance = Stance.CASTER;
        expiryPending = false;
        return CastResult.SET;
    }

    /** The player dealt damage (any source: weapon, spell, other). Context only. */
    public void onDamageDealt(long now) {
        lastCombatAt = now;
    }

    /**
     * The player TOOK hostile damage (an entity source, not themselves,
     * not the environment). Refreshes the combat context and, while a
     * stance is active, the stance itself.
     */
    public void onHostileDamageTaken(long now) {
        lastCombatAt = now;
        if (activeStance(now) != Stance.NONE) {
            stanceExpiresAt = now + windowMs;
        }
    }

    /**
     * Glue polls this (ticker): returns true exactly once after a
     * stance lapses, so the expire line is sent a single time.
     */
    public boolean pollExpired(long now) {
        if (stance != Stance.NONE && now >= stanceExpiresAt) {
            stance = Stance.NONE;
            stanceExpiresAt = Long.MIN_VALUE;
            if (!expiryPending) {
                expiryPending = true;
                return true;
            }
        }
        return false;
    }

    /** Admin clear / death / quit: stance and combat context both reset. */
    public void clear() {
        stance = Stance.NONE;
        stanceExpiresAt = Long.MIN_VALUE;
        lastCombatAt = Long.MIN_VALUE;
        expiryPending = false;
    }
}

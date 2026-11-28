package excelemfixes.leash;

/**
 * Pure decisions for the D183 return engine (LeashModule 0.3.0).
 *
 * Mike's MMO leash (Nov 21, 2026, range picked at 96): an elite
 * fights normally anywhere within the range of its spawn point;
 * pulled beyond it, it deaggros, walks home, and heals to full on
 * arrival — but player damage mid-return re-aggroes it instantly
 * and the heal never happens. The Bukkit/EM glue in EmLeashBridge
 * resolves the facts (distances, combat state, timestamps); this
 * policy decides. No Bukkit or EliteMobs types appear here, so the
 * whole state machine is unit-testable on the bare JVM.
 */
public final class ReturnPolicy {

    /** Close enough to the spawn point to count as home. */
    public static final double ARRIVE_RADIUS = 2.0;

    /** No distance progress for this long = the walk is stuck. */
    public static final long STUCK_PROGRESS_MILLIS = 5_000L;

    /** A return may never run longer than this, progress or not. */
    public static final long MAX_RETURN_MILLIS = 60_000L;

    /** What player damage does to an elite. */
    public enum DamageOutcome { NONE, REAGGRO }

    /** What happens to an elite's health when a return completes. */
    public enum ArrivalHeal { FULL, NONE }

    private ReturnPolicy() {
    }

    /**
     * A return starts only for an elite that is in combat and has
     * been pulled strictly beyond its leash range from home. At or
     * inside the range, aggro behaves normally — no deaggro ever
     * (a camp fight can never trip the leash).
     */
    public static boolean shouldTriggerReturn(boolean alreadyReturning, boolean inCombat,
                                              double distanceFromHome, double range) {
        return !alreadyReturning && inCombat && distanceFromHome > range;
    }

    /** Arrival is measured against the spawn point, not the path. */
    public static boolean hasArrived(double distanceFromHome) {
        return distanceFromHome <= ARRIVE_RADIUS;
    }

    /**
     * Teleport fallback (EM's own teleport-on-fail precedent): the
     * walk is abandoned for a teleport home when it has made no
     * progress for STUCK_PROGRESS_MILLIS, or has run past
     * MAX_RETURN_MILLIS in total.
     */
    public static boolean shouldTeleportFallback(long returningMillis, long sinceProgressMillis) {
        return returningMillis > MAX_RETURN_MILLIS || sinceProgressMillis > STUCK_PROGRESS_MILLIS;
    }

    /**
     * Player damage while RETURNING breaks the return: instant
     * re-aggro on the damager, and the arrival heal never runs.
     * Damage to an elite that is not returning is ordinary combat.
     */
    public static DamageOutcome onPlayerDamage(boolean returning) {
        return returning ? DamageOutcome.REAGGRO : DamageOutcome.NONE;
    }

    /**
     * The D251 arrival-heal decision (Nov 26, 2026 — the
     * Manifested Nightmare teleport-heal cycle Jester reported
     * Nov 22 + Nov 26): an elite that took player damage during
     * this episode does NOT heal when its leash return completes
     * — it comes home at its current HP, and the only recovery
     * afterward is the standing out-of-combat regen (D184).
     * D183's heal-to-full survives only for genuinely untouched
     * returns (pulled by aggro alone, never damaged). The tuning
     * is a config knob: with suppression off, or for a boss
     * outside the configured scope, the classic D183 full heal
     * applies unchanged.
     */
    public static ArrivalHeal arrivalHeal(boolean suppressionEnabled, boolean combatTouched,
                                          boolean inScope) {
        return suppressionEnabled && combatTouched && inScope ? ArrivalHeal.NONE : ArrivalHeal.FULL;
    }

    /**
     * Health after an arrival: FULL restores max health; NONE
     * preserves the current health EXACTLY — no instant heal of
     * any size lands on a combat-touched return, so post-return
     * recovery is the standing out-of-combat regen and nothing
     * else (D251).
     */
    public static double healthAfterArrival(ArrivalHeal heal, double currentHealth, double maxHealth) {
        return heal == ArrivalHeal.FULL ? maxHealth : currentHealth;
    }

    /**
     * The combat-end reset-heal veto (v30 — the second heal path
     * in Jester's report, found by the v29 live proof on
     * Nov 27, 2026): EliteMobs 10.9.16 heals a custom boss to
     * FULL the moment combat ends, wherever the boss stands —
     * its combat watchdog (a 20-tick timer per elite in combat)
     * fires EliteMobExitCombatEvent when no player is within
     * the boss's follow range, and that event's own constructor
     * calls fullHeal() when MobCombatSettings'
     * regenerateCustomBossHealthOnCombatEnd is on (the shipped
     * default, and live on WRV). No leash return is involved:
     * the boss can be well inside its leash range, mid-chase,
     * and a kiting or teleport-hopping player trips it
     * constantly.
     *
     * The veto fires only when EVERY fact matches the reset
     * heal and nothing else:
     *  - the tuning is enabled;
     *  - the heal is a FULL heal (amount-based heals from boss
     *    powers are a different event shape and never vetoed);
     *  - the elite is combat-touched (landed player damage this
     *    episode — the same D251 mark; untouched elites keep
     *    the stock combat-end heal exactly);
     *  - EliteMobs itself reports the elite as OUT of combat —
     *    the exit-combat constructor clears the combat flag
     *    BEFORE it heals, so the reset heal always presents
     *    this way, while in-combat full heals (phase resets,
     *    in-fight powers) present in combat and are never
     *    vetoed;
     *  - the boss is in the tuning's scope.
     * The module's own arrival heal is never at risk: under
     * D251 it only ever runs for untouched elites, and an
     * untouched elite fails the combat-touched test.
     */
    public static boolean shouldVetoCombatEndHeal(boolean suppressionEnabled, boolean fullHeal,
                                                  boolean combatTouched, boolean emInCombat,
                                                  boolean inScope) {
        return suppressionEnabled && fullHeal && combatTouched && !emInCombat && inScope;
    }

    /**
     * Scope for the D251 tuning: an empty scope list means every
     * boss; a non-empty list means only the named EliteMobs
     * custombosses filenames (exact match). A boss whose
     * filename cannot be determined is out of scope whenever a
     * list is configured — the classic heal fails open rather
     * than the tuning applying to an unidentified boss.
     */
    public static boolean scopeApplies(java.util.List<String> bossFileScope, String bossFilename) {
        if (bossFileScope == null || bossFileScope.isEmpty()) return true;
        if (bossFilename == null) return false;
        return bossFileScope.contains(bossFilename);
    }
}

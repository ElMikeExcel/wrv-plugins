package excelemfixes.respawngrace;

import java.util.UUID;

/**
 * Pure decisions and config clamps for the respawn-grace module.
 * Scope is the food-heal sibling rule (EliteMobs registry OR the
 * module's config world list); the listener computes the two
 * booleans and everything after that is decided here, so the
 * whole trigger table is unit-testable without Bukkit.
 */
public final class RespawnGracePolicy {

    public static final int DEFAULT_MAX_SECONDS = 60;
    public static final int MIN_MAX_SECONDS = 1;
    public static final int MAX_MAX_SECONDS = 600;

    public static final int DEFAULT_RETURN_WINDOW_SECONDS = 600;
    public static final int MIN_RETURN_WINDOW_SECONDS = 30;
    public static final int MAX_RETURN_WINDOW_SECONDS = 3600;

    public static final int DEFAULT_COUNTDOWN_SECONDS = 10;
    public static final int MIN_COUNTDOWN_SECONDS = 1;
    public static final int MAX_COUNTDOWN_SECONDS = 60;

    private RespawnGracePolicy() {}

    // Clamp convention for all three knobs: 0 means "unset" and
    // falls back to the shipped default; any other value clamps
    // into the supported band.

    public static int clampMaxSeconds(int configured) {
        if (configured == 0) {
            return DEFAULT_MAX_SECONDS;
        }
        return Math.max(MIN_MAX_SECONDS, Math.min(MAX_MAX_SECONDS, configured));
    }

    public static int clampReturnWindowSeconds(int configured) {
        if (configured == 0) {
            return DEFAULT_RETURN_WINDOW_SECONDS;
        }
        return Math.max(MIN_RETURN_WINDOW_SECONDS,
                Math.min(MAX_RETURN_WINDOW_SECONDS, configured));
    }

    public static int clampCountdownSeconds(int configured) {
        if (configured == 0) {
            return DEFAULT_COUNTDOWN_SECONDS;
        }
        return Math.max(MIN_COUNTDOWN_SECONDS,
                Math.min(MAX_COUNTDOWN_SECONDS, configured));
    }

    /** EliteMobs-scope test result, supplied by the listener. */
    public static boolean inScope(boolean registryWorld, boolean listedWorld) {
        return registryWorld || listedWorld;
    }

    /**
     * A death arms the return ticket only when it happens inside
     * EM scope. A death anywhere else arms nothing (and the
     * listener clears any stale ticket instead).
     */
    public static boolean deathArmsTicket(boolean deathWorldInScope) {
        return deathWorldInScope;
    }

    /**
     * A respawn grants protection when the player lands in EM
     * scope holding a live ticket. The ledger enforces the
     * ticket; this is the scope half of the rule.
     */
    public static boolean grantOnRespawn(boolean respawnWorldInScope) {
        return respawnWorldInScope;
    }

    /**
     * A world change grants protection when the player crosses
     * INTO EM scope from outside it holding a live ticket — the
     * normal corpse run (respawn at a bed in the overworld, then
     * re-enter the EM world). Moving between two EM worlds does
     * not re-grant (the ticket is already consumed by then, but
     * the rule stands on its own too).
     */
    public static boolean grantOnWorldChange(boolean fromInScope, boolean toInScope) {
        return toInScope && !fromInScope;
    }

    /** Leaving EM scope while protected ends the protection. */
    public static boolean endOnWorldChange(boolean fromInScope, boolean toInScope) {
        return fromInScope && !toInScope;
    }

    /**
     * Incoming damage is blocked only while the victim is
     * protected AND standing in EM scope (belt-and-braces: the
     * world-change handler already ends protection on exit).
     */
    public static boolean damageBlocked(boolean ledgerProtected, boolean victimWorldInScope) {
        return ledgerProtected && victimWorldInScope;
    }

    /**
     * A pickup is the player's OWN death-drop loot when the
     * stack's tags carry the player's UUID and the death instance
     * id of the grace currently running. Anything else — untagged
     * ground loot, another player's drops, loot from an earlier
     * death — never starts the countdown.
     */
    public static boolean isOwnDeathLoot(UUID tagOwner, UUID tagDeath,
                                           UUID playerId, UUID currentDeathId) {
        return tagOwner != null && tagDeath != null
                && currentDeathId != null
                && tagOwner.equals(playerId)
                && tagDeath.equals(currentDeathId);
    }
}

package excelemfixes.bedrockabilities;

/**
 * The bridge's own failure vocabulary for a refused ability fire.
 *
 * Mirrors EliteMobs' AbilityFailureReason one-for-one (the gateway maps
 * by constant; a test running against the real EliteMobs jar pins the
 * correspondence so a future EliteMobs reason fails the suite loudly
 * instead of degrading silently). UNKNOWN is the forward-compatible
 * fallback for anything the bridge does not recognise, and for a fire
 * that blew up before EliteMobs could answer at all.
 *
 * Note on INVALID_PLAYER: EliteMobs' module returns it for every
 * module-level gate (in transit, class controls not active here, no
 * class selected, cannot afford) and EliteMobs itself sends the player
 * the specific message in each case. The bridge's line for it is
 * therefore a deliberately general plain-words floor, never a guess at
 * which gate tripped.
 */
public enum BridgeFailure {
    NONE,
    WRONG_THREAD,
    INVALID_PLAYER,
    INVALID_LEVEL,
    ABILITY_NOT_REGISTERED,
    NO_VALID_TARGET,
    NO_CORPSE,
    PATH_BLOCKED,
    UNSAFE_DESTINATION,
    ENGINE_CLOSED,
    NOT_MOVING,
    NO_LOW_HEALTH_ALLY,
    NO_WOUNDED_TARGET,
    NO_ELITE_TARGET,
    NO_LARGE_TARGET,
    NO_BOSS_TARGET,
    UNKNOWN
}

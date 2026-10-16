package com.winkreyes.rewardguard;

import java.util.Set;

/**
 * Conservative source policy, deliberately not a natural-grinder classifier.
 *
 * Denied origins are the farm signals the family agreed on (Oct 13 design):
 * mobs born from a spawner block, from a spawn egg, or through a Nether portal
 * (the zombified-piglin gold-farm route). These mirror the provenance flags the
 * WRV mcMMO build itself tracks (MobMetaFlagType MOB_SPAWNER_MOB covers the
 * SPAWNER and SPAWNER_EGG reasons; NETHER_PORTAL_MOB covers NETHER_PORTAL).
 * Everything else stays UNKNOWN: this policy never guesses that a natural mob
 * is farmed, and it never produces an ALLOW verdict.
 */
public record FarmVerdict(String decision, String reason) {
    private static final Set<String> KNOWN_EXCLUDED_ORIGINS = Set.of("SPAWNER", "SPAWNER_EGG", "NETHER_PORTAL");
    public static FarmVerdict forOrigin(String reason) {
        return KNOWN_EXCLUDED_ORIGINS.contains(reason)
            ? new FarmVerdict("DENY", "spawn_origin_" + reason.toLowerCase(java.util.Locale.ROOT))
            : unknown();
    }
    public static FarmVerdict unknown() { return new FarmVerdict("UNKNOWN", "unclassified_origin"); }
    public static FarmVerdict unavailable() { return new FarmVerdict("DENY", "classification_unavailable"); }
    public String[] apiValue() { return new String[] {decision, reason, "1"}; }
}

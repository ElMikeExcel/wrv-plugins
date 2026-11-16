package com.winkreyes.excelsummons;

import java.util.UUID;

/**
 * The allegiance rulebook, pure and unit-tested. The bridge target
 * engine feeds it facts; it answers with a decision.
 *
 * REVISED in ExcelSummons 0.1.1 (Mike's ruling, Nov 8, 2026): the
 * 0.1.0 invariant — a summon can never be pointed at a player, not
 * even in retaliation — is superseded by Mike's party rule, in his
 * words: summons must not target people in the caster's party; if
 * somebody NOT in the party hits the summoned mob, the summon SHOULD
 * target them; if a party member accidentally hits the summon, the
 * summon takes NO damage from that hit and must NOT retarget onto
 * that person. The rulebook now reads:
 *
 *   - A summon engages hostile mobs by priority: (a) a hostile mob
 *     attacking the caster, (b) the hostile mob the caster is
 *     fighting, (c) a hostile mob a party member is fighting
 *     (target-sharing, config-gated) — as in 0.1.0.
 *   - The ONE player a summon may ever engage is its recorded
 *     aggressor (AggressorMemory): a known non-party player whose
 *     hit on the summon landed. Retaliation outranks the mob
 *     priorities (the summon defends itself first, like a vanilla
 *     mob switching to its attacker).
 *   - The owner, party members, and any player whose party status
 *     is UNKNOWN are PROTECTED: never targeted, never damaged by a
 *     summon, and their hits on a summon are cancelled outright and
 *     never recorded. Fail-soft leans toward protection — the hard
 *     invariant is that a summon must never attack someone who
 *     might be in the party.
 *
 * With no valid target the summon leashes home instead of wandering
 * aggro.
 *
 * Excel — Wink Reyes Valley (Iris / Mike).
 */
public final class TargetRules {

    /** A candidate target, described in plain facts (no Bukkit types). */
    public static final class Candidate {
        public final UUID entityId;
        public final boolean hostile;      // Bukkit Enemy (monster-class)
        public final boolean player;
        public final boolean alive;
        public final boolean sameWorld;
        public final double distanceToSummon;

        public Candidate(UUID entityId, boolean hostile, boolean player, boolean alive,
                         boolean sameWorld, double distanceToSummon) {
            this.entityId = entityId;
            this.hostile = hostile;
            this.player = player;
            this.alive = alive;
            this.sameWorld = sameWorld;
            this.distanceToSummon = distanceToSummon;
        }
    }

    // ------------------------------------------------------------ party relation

    /** How a player stands to a summon's owner for combat purposes.
     *  PROTECTED covers the owner, party members, and — deliberately —
     *  anyone whose party status could not be determined. STRANGER is
     *  reserved for a player KNOWN to be outside the owner's party. */
    public enum Relation { PROTECTED, STRANGER }

    /** Map a party lookup onto a Relation. Anything short of a known
     *  NOT_PARTY answer protects the player (the fail-soft rule). */
    public static Relation relationFor(boolean isOwner, PartyProvider.Membership membership) {
        if (isOwner) return Relation.PROTECTED;
        return membership == PartyProvider.Membership.NOT_PARTY
                ? Relation.STRANGER : Relation.PROTECTED;
    }

    /** What happens when a player hits a registered summon. */
    public enum IncomingHit {
        /** Owner / party / unknown: the hit is cancelled and nothing
         *  is recorded — as if it never happened (Mike's rule). */
        CANCEL_PROTECTED,
        /** Known stranger: the hit lands and the hitter becomes the
         *  summon's recorded aggressor. */
        LAND_AND_RECORD
    }

    public static IncomingHit incomingPlayerHit(Relation relation) {
        return relation == Relation.STRANGER
                ? IncomingHit.LAND_AND_RECORD : IncomingHit.CANCEL_PROTECTED;
    }

    /** May a registered summon's damage land on this player victim?
     *  Only on its own recorded aggressor, and only while that player
     *  is still a known stranger (a player who has since joined the
     *  party is protected again — membership is read live). */
    public static boolean playerDamageAllowed(Relation relation,
                                              boolean victimIsRecordedAggressor) {
        return relation == Relation.STRANGER && victimIsRecordedAggressor;
    }

    // ------------------------------------------------------------ targeting

    /** A candidate is engageable only if it is a living hostile mob in
     *  the summon's world and inside the engagement radius. Players and
     *  passive animals fail this by construction. */
    public static boolean engageable(Candidate c, double engageRadius) {
        return c != null
                && c.alive
                && c.hostile
                && !c.player
                && c.sameWorld
                && c.distanceToSummon <= engageRadius;
    }

    /** The retaliation exception: a PLAYER candidate is engageable
     *  only through this door, and the caller passes a candidate here
     *  only when it is the summon's recorded aggressor. Liveness,
     *  world, and radius still apply. */
    public static boolean retaliationEngageable(Candidate c, double engageRadius) {
        return c != null
                && c.player
                && c.alive
                && c.sameWorld
                && c.distanceToSummon <= engageRadius;
    }

    /**
     * Pick the summon's target from the mob priorities: the first
     * engageable candidate in priority order (caster's attacker,
     * caster's target, party target). Returns null when nothing is
     * engageable — the engine then clears the summon's target and
     * leashes it home. Player candidates are never picked here.
     */
    public static UUID choose(Candidate casterAttacker, Candidate casterTarget,
                              Candidate partyTarget, double engageRadius) {
        if (engageable(casterAttacker, engageRadius)) return casterAttacker.entityId;
        if (engageable(casterTarget, engageRadius)) return casterTarget.entityId;
        if (engageable(partyTarget, engageRadius)) return partyTarget.entityId;
        return null;
    }

    /**
     * The engine's full choice: the recorded aggressor, when passed
     * in the aggressor slot and still engageable, outranks the mob
     * priorities; otherwise the mob priorities decide as in choose().
     */
    public static UUID chooseWithRetaliation(Candidate aggressor, Candidate casterAttacker,
                                             Candidate casterTarget, Candidate partyTarget,
                                             double engageRadius) {
        if (retaliationEngageable(aggressor, engageRadius)) return aggressor.entityId;
        return choose(casterAttacker, casterTarget, partyTarget, engageRadius);
    }

    /** Leash: with no target, a summon farther than this from its
     *  caster is brought home rather than left standing in the wild. */
    public static boolean shouldLeashHome(double distanceToCaster, double leashDistance,
                                          boolean hasTarget) {
        return !hasTarget && distanceToCaster > leashDistance;
    }

    /** The damage scrub's question for NON-player victims: may a
     *  registered summon's damage land on this victim? Never on a
     *  passive or owned animal. Hostile mobs and other combat
     *  entities: yes. (Player victims are playerDamageAllowed's
     *  question, not this one's.) */
    public static boolean damageAllowed(boolean victimIsPlayer, boolean victimIsPassiveAnimal,
                                        boolean victimIsOwnedTameable) {
        if (victimIsPlayer) return false;
        if (victimIsOwnedTameable) return false;
        if (victimIsPassiveAnimal) return false;
        return true;
    }
}

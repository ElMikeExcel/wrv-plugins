package com.winkreyes.excelraidbuff.core;

/**
 * Who gets buffed and who gets stripped — the raid-membership
 * decisions, kept Bukkit-free so the matrix is unit-testable.
 *
 * Membership is the ONLY criterion (Mike's scope rule): a raider
 * belongs to a raid exactly while the game's own raid object
 * claims it. Patrols, outpost and mansion illagers, and evoker
 * vexes are never raid members and never enter either decision.
 *
 * Elites are decided before this gate is consulted: a raider
 * EliteMobs has converted is EM's to scale, and this plugin never
 * puts attribute modifiers on it (no double-scaling) nor strips
 * anything from it when the raid ends.
 */
public final class MembershipGate {

    public enum BuffDecision { BUFF, SKIP }
    public enum StripDecision { STRIP, KEEP }

    private MembershipGate() {}

    /**
     * @param raidMember   the game's raid object currently claims this raider
     * @param raidOngoing  that raid is still running
     * @param alreadyBuffed this plugin's modifiers are already on it
     */
    public static BuffDecision decideBuff(boolean raidMember, boolean raidOngoing, boolean alreadyBuffed) {
        if (!raidMember || !raidOngoing) return BuffDecision.SKIP;
        if (alreadyBuffed) return BuffDecision.SKIP; // repair is the sweep's separate path
        return BuffDecision.BUFF;
    }

    /**
     * @param marked          this plugin's PDC marker is on the entity
     * @param entityRaidId    the raid the game currently assigns the
     *                        entity to (-1 when it assigns none)
     * @param markedRaidId    the raid id recorded when the buff was applied
     * @param raidStillActive the marked raid is still running
     */
    public static StripDecision decideStrip(boolean marked, int entityRaidId, int markedRaidId,
                                            boolean raidStillActive) {
        if (!marked) return StripDecision.KEEP;
        if (!raidStillActive) return StripDecision.STRIP;
        if (entityRaidId != markedRaidId) return StripDecision.STRIP;
        return StripDecision.KEEP;
    }
}

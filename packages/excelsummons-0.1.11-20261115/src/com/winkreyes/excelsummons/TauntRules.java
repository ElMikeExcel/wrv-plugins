package com.winkreyes.excelsummons;

/**
 * The Guardian taunt rulebook, pure and unit-tested (ExcelSummons
 * 0.1.2, Mike's ruling, Nov 8, 2026: "a summon that actually does
 * his job like he's a tanky summon. And it's able to take aggro and
 * tank some hits.").
 *
 * The Guardian (iron golem — the roster's tank slot, DESIGN §2.2)
 * is the ONLY summon that taunts. On the bridge's target tick, a
 * hostile mob near the Guardian whose current target is the
 * Guardian's owner or a member of the owner's party is re-pointed
 * at the Guardian. The conditions are deliberately narrow:
 *
 *   - The hostile must ALREADY be engaged with the party — its
 *     current target being the owner or a party member is the
 *     engagement evidence. A hostile with no target, a hostile
 *     chasing a non-party player, and passive/neutral-unprovoked
 *     mobs are never touched: taunt steals fights, it never
 *     starts them and never trains a pack onto the group.
 *   - The taunt's target is always the Guardian itself. Taunt can
 *     never make anything target a player — the 0.1.1 protection
 *     invariants are untouched (this rulebook only ever moves a
 *     hostile OFF a player).
 *   - Governed bosses are NOT special-cased here (see the build
 *     record for the per-boss effectiveness analysis): BossTrial
 *     manages damage intake, never boss targets, so boss targeting
 *     is vanilla AI and the same re-point applies, competing per
 *     tick with whatever re-pick the vanilla goal makes next.
 *
 * The listener gathers the facts; this class answers.
 *
 * Excel — Wink Reyes Valley (Iris / Mike).
 */
public final class TauntRules {

    private TauntRules() {}

    /**
     * Should the bridge re-point this hostile at the Guardian?
     *
     * @param hostileIsMob              the hostile is a Bukkit Mob (has a target to move)
     * @param hostileAlive              the hostile is alive
     * @param hostileSameWorld          the hostile is in the Guardian's world
     * @param distanceToGuardian        blocks between hostile and Guardian
     * @param tauntRadius               the configured taunt radius
     * @param currentTargetIsPlayer     the hostile's current target is a player
     * @param targetIsOwnerOrParty      that player is the Guardian's owner or in the owner's party
     * @param alreadyTargetingGuardian  the hostile is already on the Guardian (nothing to steal)
     */
    public static boolean shouldTaunt(boolean hostileIsMob, boolean hostileAlive,
                                      boolean hostileSameWorld, double distanceToGuardian,
                                      double tauntRadius, boolean currentTargetIsPlayer,
                                      boolean targetIsOwnerOrParty,
                                      boolean alreadyTargetingGuardian) {
        if (!hostileIsMob || !hostileAlive || !hostileSameWorld) return false;
        if (distanceToGuardian > tauntRadius) return false;
        if (alreadyTargetingGuardian) return false;
        // The steal condition IS the engagement condition: only a
        // hostile already fighting the owner's party can be pulled,
        // and only off the owner or a party member.
        return currentTargetIsPlayer && targetIsOwnerOrParty;
    }

    /**
     * The Guardian's tank skin: the damage a guardian record actually
     * takes from a hit after the bridge-side reduction. The reduction
     * is the bridge's half of the tank build (the spell-side half is
     * the max_health attribute in the packet's guardian delta); it
     * applies to every damage source alike, so no single mechanic —
     * elite melee, boss contact, arrows — bypasses the tanking.
     */
    public static double guardianDamageTaken(double damage, double reductionFraction) {
        if (damage <= 0) return damage;
        double r = Math.max(0.0, Math.min(0.95, reductionFraction));
        return damage * (1.0 - r);
    }
}

package com.winkreyes.exceltrainingwheels.core;

/**
 * Pure decision logic for the damage shield (Training Wheels Protocol):
 * WHICH damage events are shieldable, and the scaled value.
 *
 * Shieldable sources — hostile mobs only (Mike's rule, Oct 19, 2026):
 *  - ENTITY_ATTACK (a hostile mob's melee),
 *  - ENTITY_SWEEP_ATTACK (the melee family — in practice only
 *    players sweep, and players are never hostile, so this arm
 *    changes nothing for real events; it exists so the shield
 *    and the RAID COMPENSATION cause test — see RaidDivision —
 *    agree exactly on what "melee" means),
 *  - PROJECTILE whose shooter is a hostile mob (skeleton arrows,
 *    drowned tridents, ghast fireballs...),
 *  - ENTITY_EXPLOSION whose source entity is a creeper.
 * Everything else is never touched: falls, lava, drowning, fire,
 * starvation, suffocation, void, block/TNT explosions, magic, and
 * anything a player causes. The Bukkit glue resolves the three
 * booleans; this class owns the decision so it is unit-testable.
 */
public final class ShieldRule {

    /** Damage-cause groups the glue maps Bukkit causes onto. */
    public enum Cause { ENTITY_ATTACK, ENTITY_SWEEP_ATTACK, PROJECTILE, ENTITY_EXPLOSION, OTHER }

    private ShieldRule() {}

    /**
     * @param cause          mapped damage cause
     * @param damagerHostile the direct damager entity is a hostile mob
     *                       (melee attacker; false for projectiles)
     * @param shooterHostile the projectile's shooter is a hostile mob
     *                       (false for player-shot and for dispenser /
     *                       block sources, whose shooter is not an entity)
     * @param damagerCreeper the direct damager entity is a creeper
     */
    public static boolean shouldScale(Cause cause, boolean damagerHostile,
                                      boolean shooterHostile, boolean damagerCreeper) {
        if (cause == null) return false;
        return switch (cause) {
            case ENTITY_ATTACK, ENTITY_SWEEP_ATTACK -> damagerHostile;
            case PROJECTILE -> shooterHostile;
            case ENTITY_EXPLOSION -> damagerCreeper;
            case OTHER -> false;
        };
    }

    /** damage x factor. Factor must be in [0, 1]; 1.0 is a no-op, 0.0 is full stop. */
    public static double scaled(double damage, double factor) {
        if (!ProtectedRegistry.validFactor(factor)) {
            throw new IllegalArgumentException("factor out of [0,1]: " + factor);
        }
        if (damage <= 0.0) return damage;
        return damage * factor;
    }
}

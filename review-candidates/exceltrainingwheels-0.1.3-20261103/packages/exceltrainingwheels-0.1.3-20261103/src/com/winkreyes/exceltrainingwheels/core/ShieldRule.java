package com.winkreyes.exceltrainingwheels.core;

/**
 * Pure decision logic for the kid shield (Training Wheels Protocol):
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
 *  - ENTITY_EXPLOSION whose source entity is a creeper, OR whose
 *    damager is a projectile shot by a hostile mob (0.1.3 — the
 *    Wither skull blast: the skull itself is the explosion's
 *    damager, and its shooter is the Wither; this arm mirrors
 *    the PROJECTILE arm's shooter test exactly).
 * 0.1.3 adds two boss-only causes (Mike's absolute line in seal
 * fights; the TW-PRESSURE-CHECK of Nov 2, 2026 found both
 * uncovered):
 *  - SONIC_BOOM attributed to a seal boss (only the Warden
 *    produces this cause at all, and only a warden damager is
 *    ever boss-attributed by the glue),
 *  - DRAGON_BREATH attributed to a seal boss (the damager is the
 *    dragon's breath cloud; the glue attributes it through the
 *    cloud's source).
 * Both new arms key on boss attribution ALONE: they admit the
 * cause for seal-boss damagers only and do NOT widen the shield's
 * general cause scope — a sonic boom or breath event from any
 * non-boss source (none exists in vanilla, but the rule does not
 * rely on that) stays unscaled. The Wither effect ticks remain
 * uncovered by construction: they arrive as a plain
 * EntityDamageEvent with NO damager entity, so they never reach
 * this rule at all.
 * Everything else is never touched: falls, lava, drowning, fire,
 * starvation, suffocation, void, block/TNT explosions, magic, and
 * anything a player causes. The Bukkit glue resolves the
 * booleans; this class owns the decision so it is unit-testable.
 */
public final class ShieldRule {

    /** Damage-cause groups the glue maps Bukkit causes onto. */
    public enum Cause {
        ENTITY_ATTACK, ENTITY_SWEEP_ATTACK, PROJECTILE, ENTITY_EXPLOSION,
        SONIC_BOOM, DRAGON_BREATH, OTHER
    }

    private ShieldRule() {}

    /**
     * @param cause          mapped damage cause
     * @param damagerHostile the direct damager entity is a hostile mob
     *                       (melee attacker; false for projectiles)
     * @param shooterHostile the projectile's shooter is a hostile mob
     *                       (false for player-shot and for dispenser /
     *                       block sources, whose shooter is not an entity)
     * @param damagerCreeper the direct damager entity is a creeper
     * @param bossAttributed the responsible source (the damager itself,
     *                       a projectile's shooter, a breath cloud's
     *                       source, or a dragon part's parent, as the
     *                       glue resolves it) is a seal boss — a Wither,
     *                       ElderGuardian, Warden or EnderDragon, per
     *                       ExcelBossTrial's own attribution
     */
    public static boolean shouldScale(Cause cause, boolean damagerHostile,
                                      boolean shooterHostile, boolean damagerCreeper,
                                      boolean bossAttributed) {
        if (cause == null) return false;
        return switch (cause) {
            case ENTITY_ATTACK, ENTITY_SWEEP_ATTACK -> damagerHostile;
            case PROJECTILE -> shooterHostile;
            case ENTITY_EXPLOSION -> damagerCreeper || shooterHostile;
            case SONIC_BOOM, DRAGON_BREATH -> bossAttributed;
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

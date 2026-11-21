package com.winkreyes.exceltrainingwheels.core;

import java.util.EnumSet;
import java.util.Set;

/**
 * Pure decision logic for the kid shield (Training Wheels Protocol):
 * WHICH damage events are shieldable, and the scaled value.
 *
 * 0.1.4 — THE D158 RULE (Mike's direct, Nov 20, 2026): "Just cover
 * everything. All her incoming damage is reduced by .25 except for
 * fall damage and dying from suffocation and stuff. Basically
 * anything that hits her should be reduced." Admission is now the
 * CAUSE ALONE. There is no hostile test, no damager test, no elite
 * test on the admission path: every cause scales except the
 * environmental family below, and damage from ANY damager kind —
 * Enemy, animal, EliteMobs elite, tamed pet, another player —
 * scales the same. (The 0.1.0–0.1.3 hostile-mob admission this
 * replaces left a verified hole: EliteMobs' Primis rabbit bosses
 * are Animals, never Enemies, and hit a protected player
 * completely unshielded — the live check of Nov 20, 2026.)
 *
 * THE ENVIRONMENTAL EXCLUSION SET (never scaled) — Mike's named
 * family: FALL, SUFFOCATION, DROWNING, STARVATION, FIRE, FIRE_TICK,
 * LAVA, CONTACT, HOT_FLOOR, FREEZE, CRAMMING, VOID, FLY_INTO_WALL.
 * The governing principle for any cause outside that named set: if
 * an entity inflicted it, scale it; if the world inflicted it,
 * don't. Applied to the Bukkit tail Mike did not name:
 *  - WORLD_BORDER, CAMPFIRE, DRYOUT, MELTING are world/block
 *    inflicted (campfire is the CONTACT / HOT_FLOOR family) —
 *    EXCLUDED on the principle.
 *  - CUSTOM (plugin-inflicted by definition) and KILL / SUICIDE
 *    (command-inflicted, and scaling them changes nothing
 *    observable — the damage is lethal at any factor) — COVERED,
 *    i.e. simply not in the exclusion set.
 *  - OTHER (a cause a future API adds and the glue has not mapped
 *    yet) — COVERED, on Mike's stated bias: anything that hits her
 *    is reduced until a human rules the new cause environmental.
 *
 * Newly covered by this rule (excluded by design before 0.1.4):
 * MAGIC (witch splash), POISON and WITHER ticks, THORNS,
 * BLOCK_EXPLOSION, LIGHTNING, FALLING_BLOCK, and SONIC_BOOM /
 * DRAGON_BREATH from ANY source (0.1.3 admitted those two for
 * seal-boss damagers only). ENTITY_EXPLOSION is covered whatever
 * its source (creeper, skull, TNT — the creeper-only arm is gone).
 *
 * Compensation is NOT admission and is unchanged: once a cause is
 * admitted, the glue still divides the factor for the damager
 * shapes that arrive pre-scaled — EliteMobs elites (ladder
 * division, core/CompensationTable), raid-buffed melee
 * (core/RaidDivision), seal-boss direct hits (core/BossDivision) —
 * so the take stays factor x the mob's UNSCALED damage. Non-elite
 * and unattributed damage (no damager entity: poison ticks, wither
 * ticks) takes the flat factor. Armor reduces further afterwards,
 * as vanilla does.
 *
 * The set lives here, in code, not in config: it is Mike's design
 * law, not a per-server tuning knob, and the config shape is
 * unchanged by 0.1.4. The Bukkit glue maps Bukkit causes onto this
 * enum exhaustively; this class owns the decision so it is
 * unit-testable.
 */
public final class ShieldRule {

    /**
     * Damage-cause groups the glue maps Bukkit causes onto —
     * one value per Bukkit DamageCause, plus OTHER for unmapped
     * future causes.
     */
    public enum Cause {
        // Entity-inflicted (scaled): anything that hits her.
        ENTITY_ATTACK, ENTITY_SWEEP_ATTACK, PROJECTILE, ENTITY_EXPLOSION,
        BLOCK_EXPLOSION, MAGIC, POISON, WITHER, THORNS, LIGHTNING,
        DRAGON_BREATH, SONIC_BOOM, FALLING_BLOCK, CUSTOM, KILL, SUICIDE,
        // Environmental (never scaled): the world, not an attacker.
        FALL, SUFFOCATION, DROWNING, STARVATION, FIRE, FIRE_TICK, LAVA,
        CONTACT, HOT_FLOOR, FREEZE, CRAMMING, VOID, FLY_INTO_WALL,
        WORLD_BORDER, CAMPFIRE, DRYOUT, MELTING,
        // Unmapped future cause: scaled (Mike's bias, see class doc).
        OTHER
    }

    /** The environmental exclusion set — the ONLY causes never scaled. */
    private static final Set<Cause> ENVIRONMENTAL = EnumSet.of(
            Cause.FALL, Cause.SUFFOCATION, Cause.DROWNING, Cause.STARVATION,
            Cause.FIRE, Cause.FIRE_TICK, Cause.LAVA, Cause.CONTACT,
            Cause.HOT_FLOOR, Cause.FREEZE, Cause.CRAMMING, Cause.VOID,
            Cause.FLY_INTO_WALL, Cause.WORLD_BORDER, Cause.CAMPFIRE,
            Cause.DRYOUT, Cause.MELTING);

    private ShieldRule() {}

    /** True when this cause is in the environmental exclusion set. */
    public static boolean environmental(Cause cause) {
        return cause != null && ENVIRONMENTAL.contains(cause);
    }

    /**
     * The whole admission decision (0.1.4): a cause scales unless
     * it is environmental. Null never scales (a causeless event is
     * not a hit). Damager kind, elite status and attribution play
     * NO part here — they only steer compensation, in the glue.
     */
    public static boolean shouldScale(Cause cause) {
        return cause != null && !ENVIRONMENTAL.contains(cause);
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

// ExcelTrainingWheels — an Excel plugin by WinkReyesValley. (Excel signature — MikeExcel)

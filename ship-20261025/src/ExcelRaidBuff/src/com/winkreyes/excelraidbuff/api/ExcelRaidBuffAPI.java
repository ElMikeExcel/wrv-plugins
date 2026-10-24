package com.winkreyes.excelraidbuff.api;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.persistence.PersistentDataType;

/**
 * Public, read-only API for ExcelRaidBuff — the compensation seam.
 *
 * The reader this exists for is ExcelTrainingWheels: its guarantee
 * is that Rowan takes 0.25 x a mob's NORMAL unscaled damage, and a
 * raid-buffed raider's attribute-level damage multiplier would
 * silently raise her effective take to 0.25 x the multiplier
 * unless TrainingWheels divides by it — exactly the division it
 * already performs for EliteMobs elites. {@link #isBuffed} and
 * {@link #damageMultiplierOf} give it (and any future consumer)
 * the numbers to do that division, read off the same PDC markers
 * the buff itself writes, so the API can never disagree with the
 * fight. Consumers should read it reflectively (the house pattern:
 * no hard dependency) and treat an unbound API as "no buff".
 *
 * THE COMPENSATION CONTRACT (Mike's decision, Oct 21, 2026:
 * "Keep her at exactly a quarter of vanilla — compensate for the
 * buff"). Rowan's effective take from any buffed raider must be
 * EXACTLY 0.25 x the raider's vanilla damage, at every omen level.
 * The consumer divides its shield factor by
 * {@link #damageMultiplierOf} for the damager, under these rules:
 *  - Apply the division ONLY where the attack attribute actually
 *    scaled the damage — melee causes (ENTITY_ATTACK and
 *    ENTITY_SWEEP_ATTACK). Projectile damage (a pillager's
 *    crossbow bolt) and potion damage are NOT attribute-scaled,
 *    were never raised, and must NOT be divided, or she would
 *    take LESS than the guaranteed quarter.
 *  - Elites are never marked (the buff loop skips every
 *    EliteMobs-tagged raider), so for an elite damager this method
 *    returns exactly 1.0 and the division is a no-op: the existing
 *    elite-factor compensation is numerically unchanged.
 *  - An unbuffed damager, and an unbound API (plugin absent),
 *    likewise read 1.0 — the compensation fails safe to today's
 *    uncompensated behavior only in a world where nothing is
 *    buffed, which is the only world where 1.0 is correct.
 * The composed arithmetic is pinned per omen level by
 * RowanCompensationTest in this project's test tree.
 *
 * The bind/unbind methods are internal wiring used by the plugin
 * itself; consumers should only call the getters.
 */
public final class ExcelRaidBuffAPI {

    /** The marker keys the API reads; bound by the plugin on enable. */
    public record Markers(NamespacedKey buffed, NamespacedKey healthFactor, NamespacedKey damageFactor) {}

    private static volatile Markers markers;

    private ExcelRaidBuffAPI() {}

    /** Internal: called by ExcelRaidBuff once its keys exist. */
    public static void bind(Markers bound) {
        markers = bound;
    }

    /** Internal: called by ExcelRaidBuff on disable. */
    public static void unbind() {
        markers = null;
    }

    /** True when this entity currently carries a raid buff. */
    public static boolean isBuffed(Entity entity) {
        Markers m = markers;
        if (m == null || entity == null) return false;
        return entity.getPersistentDataContainer().has(m.buffed(), PersistentDataType.BYTE);
    }

    /**
     * The damage multiplier this entity's raid buff applies
     * (1.0 when unbuffed, elite, or the API is unbound) — the
     * compensation divisor of the contract above. The value is
     * the factor stored on the entity when the buff was applied,
     * which is also the attribute's effective multiplier
     * (1 + the modifier amount), so the division cancels the buff
     * exactly, never approximately.
     */
    public static double damageMultiplierOf(Entity entity) {
        Markers m = markers;
        if (m == null || entity == null) return 1.0;
        Double v = entity.getPersistentDataContainer().get(m.damageFactor(), PersistentDataType.DOUBLE);
        return v == null ? 1.0 : v;
    }

    /** The health multiplier this entity's raid buff applies (1.0 when unbuffed). */
    public static double healthMultiplierOf(Entity entity) {
        Markers m = markers;
        if (m == null || entity == null) return 1.0;
        Double v = entity.getPersistentDataContainer().get(m.healthFactor(), PersistentDataType.DOUBLE);
        return v == null ? 1.0 : v;
    }
}

package com.winkreyes.excelsummons;

import org.bukkit.entity.AreaEffectCloud;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.projectiles.ProjectileSource;

import java.util.UUID;

/**
 * ExcelSummonsAPI — the one static attribution facade every consumer
 * reads (DESIGN §4.3). ExcelBossTrial's damage intake, ExcelProtect's
 * recorder, the post-fight stats, and any future consumer all ask the
 * same question the same way: "whose damage is this?"
 *
 * Resolution order (exactly what the phase-2 consumer patches call):
 *   1. If the damager is a registered summon, its owner is the answer.
 *   2. If the damager is a projectile (arrow, fireball, trident), the
 *      shooter is resolved and step 1 applies to the shooter — a
 *      skeleton summon's arrow and a ghast summon's fireball both land
 *      here. A player shooter answers as themselves (callers usually
 *      only ask when the direct damager was not a player, but the
 *      answer is correct either way).
 *   3. Thrown potions and area-effect clouds resolve through their
 *      source the same way.
 *   4. Anything unregistered answers null — the caller keeps its own
 *      unattributed bucket for genuinely wild damage, and a registered
 *      summon can never fall into it.
 *
 * The plugin installs the live registry on enable and clears it on
 * disable; with the plugin absent or disabled every call answers null
 * and consumers degrade to their pre-bridge behavior (house adapter
 * style: fail-soft, quiet).
 *
 * Excel — Wink Reyes Valley (Iris / Mike).
 */
public final class ExcelSummonsAPI {

    private static volatile SummonRegistry registry;

    private ExcelSummonsAPI() {}

    static void install(SummonRegistry reg) { registry = reg; }
    static void uninstall() { registry = null; }

    /** True when the bridge is live and answering. */
    public static boolean isAvailable() {
        return registry != null;
    }

    /** Owner of the registered summon with this entity id, or null. */
    public static UUID getSummoner(UUID entityId) {
        SummonRegistry reg = registry;
        return reg == null ? null : reg.summonerOf(entityId);
    }

    /** Owner of this registered summon entity, or null. */
    public static UUID getSummoner(Entity entity) {
        return entity == null ? null : getSummoner(entity.getUniqueId());
    }

    /** Is this entity a registered summon (any branch)? */
    public static boolean isSummon(Entity entity) {
        SummonRegistry reg = registry;
        return reg != null && entity != null && reg.isRegistered(entity.getUniqueId());
    }

    /** The full registry record for a summon entity, or null. */
    public static SummonRecord getRecord(Entity entity) {
        SummonRegistry reg = registry;
        return (reg == null || entity == null) ? null : reg.get(entity.getUniqueId());
    }

    /**
     * THE attribution call. Given the damager entity from a damage
     * event, answer the UUID the damage should be credited to: the
     * summon's owner when the damager (or its projectile's shooter, or
     * its potion/cloud source) is a registered summon; the shooter
     * themselves when the shooter is a player; null when nothing about
     * the damager is registered with the bridge.
     */
    public static UUID resolveDamager(Entity damager) {
        if (damager == null || registry == null) return null;
        Entity source = unwrapSource(damager);
        if (source == null) return null;
        UUID owner = getSummoner(source.getUniqueId());
        if (owner != null) return owner;
        if (source instanceof org.bukkit.entity.Player) return source.getUniqueId();
        return null;
    }

    /**
     * Reduce a damager to the entity that owns the act: projectiles to
     * their shooter, thrown potions and lingering clouds to their
     * source. Direct entities answer as themselves.
     */
    public static Entity unwrapSource(Entity damager) {
        if (damager instanceof Projectile) {
            ProjectileSource shooter = ((Projectile) damager).getShooter();
            return (shooter instanceof Entity) ? (Entity) shooter : null;
        }
        if (damager instanceof ThrownPotion) {
            ProjectileSource shooter = ((ThrownPotion) damager).getShooter();
            return (shooter instanceof Entity) ? (Entity) shooter : null;
        }
        if (damager instanceof AreaEffectCloud) {
            ProjectileSource source = ((AreaEffectCloud) damager).getSource();
            return (source instanceof Entity) ? (Entity) source : null;
        }
        return damager;
    }

    /** Convenience for consumers that only hold a LivingEntity view. */
    public static UUID resolveDamager(LivingEntity damager) {
        return resolveDamager((Entity) damager);
    }
}

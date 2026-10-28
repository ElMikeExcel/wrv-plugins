package com.winkreyes.excelbosstrial;

import com.winkreyes.excelbosstrial.core.BossKind;
import com.winkreyes.excelbosstrial.core.PlacementRegistry;
import java.util.UUID;
import org.bukkit.entity.AreaEffectCloud;
import org.bukkit.entity.ComplexEntityPart;
import org.bukkit.entity.ElderGuardian;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.entity.Tameable;
import org.bukkit.entity.Warden;
import org.bukkit.entity.Wither;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

/**
 * Damage attribution, following ExcelTelemetry's attacker-key model:
 * damage is player-attributed only when Bukkit ties it to a player -
 * a direct hit, a projectile the player fired, a TNT the player lit,
 * a tamed pet the player owns, or a lingering cloud the player
 * created. 0.2.2 adds the two sources Bukkit cannot tie on its
 * own, via the placement registry (PlacementRegistry): an end
 * crystal answers to the player who placed it, and a respawn
 * anchor's blast resolves to its placer through the registry's
 * detonation log (the adapter's half of that path). Everything
 * else (suffocation, lava, falls, conduit magic, other mobs,
 * redstone-lit or otherwise unattributable explosions)
 * is non-player damage: it counts AGAINST the seal share, never
 * toward it (SEAL-INTEGRITY-RESEARCH.md W1, section 5.1).
 */
final class Attribution {

    private Attribution() {
    }

    /** The seal-boss kind of an entity, or null when it is not one. */
    static BossKind kindOf(Entity entity) {
        if (entity instanceof Wither) {
            return BossKind.WITHER;
        }
        if (entity instanceof ElderGuardian) {
            return BossKind.ELDER_GUARDIAN;
        }
        if (entity instanceof Warden) {
            return BossKind.WARDEN;
        }
        if (entity instanceof EnderDragon) {
            return BossKind.ENDER_DRAGON;
        }
        return null;
    }

    static boolean isBoss(Entity entity) {
        return kindOf(entity) != null;
    }

    /**
     * The player responsible for a damage event's damage, or null
     * when the damage is not player-attributed.
     */
    static UUID dealtPlayer(EntityDamageEvent event) {
        return dealtPlayer(event, null);
    }

    /**
     * As {@link #dealtPlayer(EntityDamageEvent)}, with the
     * placement registry (0.2.2): a registered end crystal's
     * blast answers to its placer. Anchor blasts carry no
     * damager entity at all and resolve in the adapter through
     * the registry's detonation log instead.
     */
    static UUID dealtPlayer(EntityDamageEvent event, PlacementRegistry registry) {
        if (event instanceof EntityDamageByEntityEvent by) {
            return playerOf(by.getDamager(), registry);
        }
        return null;
    }

    /** Resolve a damager entity to its responsible player, if any. */
    static UUID playerOf(Entity damager) {
        return playerOf(damager, null);
    }

    /**
     * As {@link #playerOf(Entity)}, with the placement registry:
     * an end crystal Bukkit cannot tie to anyone answers to the
     * player who PLACED it (0.2.2) - whoever detonated it is
     * irrelevant, exactly as a lit TNT answers to its lighter.
     * Unregistered crystals (the End's natural pillars, anything
     * placed before a restart) stay unattributed.
     */
    static UUID playerOf(Entity damager, PlacementRegistry registry) {
        UUID direct = directPlayerOf(damager);
        if (direct != null) {
            return direct;
        }
        if (registry != null && damager instanceof EnderCrystal crystal) {
            return registry.crystalPlacer(crystal.getUniqueId());
        }
        return null;
    }

    private static UUID directPlayerOf(Entity damager) {
        if (damager instanceof Player p) {
            return p.getUniqueId();
        }
        if (damager instanceof Projectile proj && proj.getShooter() instanceof Player p) {
            return p.getUniqueId();
        }
        if (damager instanceof TNTPrimed tnt && tnt.getSource() instanceof Player p) {
            return p.getUniqueId();
        }
        if (damager instanceof Tameable tame && tame.getOwner() instanceof Player p) {
            return p.getUniqueId();
        }
        if (damager instanceof AreaEffectCloud cloud && cloud.getSource() instanceof Player p) {
            return p.getUniqueId();
        }
        return null;
    }

    /**
     * The seal boss a damager belongs to (for "took damage from the
     * boss" participant evidence): the boss itself, its projectiles
     * (wither skulls, dragon fireballs), its breath clouds, or the
     * dragon's body parts. Null for anything else.
     */
    static LivingEntity bossOf(Entity damager) {
        if (damager instanceof LivingEntity le && isBoss(le)) {
            return le;
        }
        if (damager instanceof Projectile proj && proj.getShooter() instanceof LivingEntity le
                && isBoss(le)) {
            return le;
        }
        if (damager instanceof AreaEffectCloud cloud && cloud.getSource() instanceof LivingEntity le
                && isBoss(le)) {
            return le;
        }
        if (damager instanceof ComplexEntityPart part && part.getParent() instanceof EnderDragon d) {
            return d;
        }
        return null;
    }

    /** Explosion classification for the P2 budget: block + entity explosions pool. */
    static boolean isExplosion(EntityDamageEvent.DamageCause cause) {
        return cause == EntityDamageEvent.DamageCause.BLOCK_EXPLOSION
                || cause == EntityDamageEvent.DamageCause.ENTITY_EXPLOSION;
    }
}


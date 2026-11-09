package com.winkreyes.exceldetails;

import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.projectiles.ProjectileSource;

/**
 * Combat intake. One MONITOR handler on EntityDamageEvent covers both
 * plain and by-entity damage (the by-entity event is a subclass, so a
 * second handler would double-count). Final damage is what Details!
 * counts, and final damage is what lands on health bars here.
 *
 * Excel — Wink Reyes Valley (Iris / Mike).
 */
public final class DetailsListener implements Listener {

    private final ExcelDetails plugin;

    public DetailsListener(ExcelDetails plugin) { this.plugin = plugin; }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        long now = System.currentTimeMillis();
        double amount = event.getFinalDamage();
        if (amount <= 0) return;
        Entity victim = event.getEntity();

        Player damager = null;
        String sourceName;
        if (event instanceof EntityDamageByEntityEvent byEntity) {
            Entity d = byEntity.getDamager();
            damager = resolvePlayer(d);
            sourceName = displayName(d);
        } else {
            sourceName = causeName(event.getCause());
        }

        if (damager != null && victim instanceof LivingEntity) {
            plugin.tracker().recordDamageDone(damager.getUniqueId(), damager.getName(),
                    amount, displayName(victim), now);
        }
        if (victim instanceof Player p) {
            plugin.tracker().recordDamageTaken(p.getUniqueId(), p.getName(),
                    amount, sourceName, now);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onRegain(EntityRegainHealthEvent event) {
        if (!(event.getEntity() instanceof Player p)) return;
        // DESIGN §4: v1 credits healing that lands on a player (magic,
        // potions, regen) to that player — the event names no healer.
        switch (event.getRegainReason()) {
            case MAGIC, MAGIC_REGEN, CUSTOM, EATING, SATIATED -> {
                if (event.getAmount() > 0) {
                    plugin.tracker().recordHealing(p.getUniqueId(), p.getName(),
                            event.getAmount(), System.currentTimeMillis());
                }
            }
            default -> { /* natural regen at full saturation etc. still counts as SATIATED above */ }
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        plugin.renderer().forget(event.getPlayer());
    }

    /** The player behind an entity: direct, projectile shooter, or TNT source. */
    static Player resolvePlayer(Entity e) {
        if (e instanceof Player p) return p;
        if (e instanceof Projectile proj) {
            ProjectileSource src = proj.getShooter();
            if (src instanceof Player p) return p;
        }
        if (e instanceof TNTPrimed tnt && tnt.getSource() instanceof Player p) return p;
        return null;
    }

    /** Readable entity name: custom name first, then a prettified type (WARDEN -> Warden). */
    static String displayName(Entity e) {
        if (e == null) return "Unknown";
        if (e instanceof Player p) return p.getName();
        String custom = e.getCustomName();
        if (custom != null && !custom.isBlank()) return custom;
        String raw = e.getType().name().toLowerCase().replace('_', ' ');
        return Character.toUpperCase(raw.charAt(0)) + raw.substring(1);
    }

    static String causeName(EntityDamageEvent.DamageCause cause) {
        return switch (cause) {
            case FALL -> "Fall";
            case FIRE, FIRE_TICK, LAVA -> "Fire";
            case DROWNING -> "Drowning";
            case SUFFOCATION -> "Suffocation";
            case STARVATION -> "Starvation";
            case POISON -> "Poison";
            case WITHER -> "Wither";
            case MAGIC -> "Magic";
            case FALLING_BLOCK -> "Falling block";
            case CONTACT -> "Contact";
            case FREEZE -> "Freezing";
            case HOT_FLOOR -> "Hot floor";
            case FLY_INTO_WALL -> "Kinetic";
            case VOID -> "Void";
            case LIGHTNING -> "Lightning";
            case SUICIDE, KILL -> "Self";
            default -> "Environment";
        };
    }
}

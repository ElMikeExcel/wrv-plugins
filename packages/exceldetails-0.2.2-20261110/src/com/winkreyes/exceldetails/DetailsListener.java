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
 * 0.2.2 routing (DESIGN.md): damage DONE is classified before it is
 * recorded — EM-space damage (an EliteMobs-tracked victim, or an
 * amount beyond every real HP pool) goes to the segment's EM bucket
 * and never renders; damage done to a seal boss is recovered into
 * BossTrial's pop-up space (finalDamage / intake x display-scale);
 * everything else records as-is. Damage TAKEN records as-is except
 * for the simulated-amount floor — elite-to-player damage is real
 * HP and stays a live metric.
 *
 * Excel — Wink Reyes Valley (Iris / Mike).
 */
public final class DetailsListener implements Listener {

    private final ExcelDetails plugin;
    private final BossScaleLookup bossScale;
    private final EliteLookup elites;
    private final boolean bossDisplayScale;
    private final boolean eliteBucket;

    public DetailsListener(ExcelDetails plugin, BossScaleLookup bossScale, EliteLookup elites,
                           boolean bossDisplayScale, boolean eliteBucket) {
        this.plugin = plugin;
        this.bossScale = bossScale;
        this.elites = elites;
        this.bossDisplayScale = bossDisplayScale;
        this.eliteBucket = eliteBucket;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        long now = System.currentTimeMillis();
        double amount = event.getFinalDamage();
        if (amount <= 0) return;
        Entity victim = event.getEntity();

        Player damager = null;
        Entity damagerEntity = null;
        String sourceName;
        if (event instanceof EntityDamageByEntityEvent byEntity) {
            Entity d = byEntity.getDamager();
            damagerEntity = d;
            damager = resolvePlayer(d);
            sourceName = displayName(d);
        } else {
            sourceName = causeName(event.getCause());
        }

        if (damager != null && victim instanceof LivingEntity) {
            // EM first (DESIGN §1c): EM content belongs to EM's
            // number space even when its type is a boss type.
            if (eliteBucket
                    && EmCombat.routeDamageDone(amount, elites.isElite(victim)) == EmCombat.Route.EM_BUCKET) {
                plugin.tracker().recordEmDamageDone(damager.getUniqueId(), damager.getName(),
                        amount, displayName(victim), now);
            } else if (bossDisplayScale
                    && BossScale.isSealBossType(victim.getType().name())
                    && bossScale.bossTrialPresent()) {
                double shown = BossScale.forDisplay(amount,
                        bossScale.displayScale(), bossScale.intakeFactorFor(victim));
                if (shown > 0) {
                    plugin.tracker().recordDamageDone(damager.getUniqueId(), damager.getName(),
                            shown, displayName(victim), now);
                }
            } else {
                plugin.tracker().recordDamageDone(damager.getUniqueId(), damager.getName(),
                        amount, displayName(victim), now);
            }
        }
        if (victim instanceof Player p) {
            if (eliteBucket
                    && EmCombat.routeDamageTaken(amount,
                            damagerEntity != null && elites.isElite(damagerEntity)) == EmCombat.Route.EM_BUCKET) {
                plugin.tracker().recordEmDamageTaken(p.getUniqueId(), p.getName(),
                        amount, sourceName, now);
            } else {
                plugin.tracker().recordDamageTaken(p.getUniqueId(), p.getName(),
                        amount, sourceName, now);
            }
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

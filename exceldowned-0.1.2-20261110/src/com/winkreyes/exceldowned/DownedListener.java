package com.winkreyes.exceldowned;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;

/**
 * All Bukkit-side enforcement of the downed state (DESIGN §1–§5).
 *
 * Excel — Wink Reyes Valley (Iris / Mike).
 */
public final class DownedListener implements Listener {

    private final ExcelDowned plugin;

    public DownedListener(ExcelDowned plugin) { this.plugin = plugin; }

    // ----- the lethal intercept + downed damage absorption -----

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onDamage(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player)) return;
        Player p = (Player) e.getEntity();
        if (plugin.bypassDeath().contains(p.getUniqueId())) return;

        if (plugin.isDowned(p.getUniqueId())) {
            if (e.getCause() == EntityDamageEvent.DamageCause.VOID
                    || p.getLocation().getY() < p.getWorld().getMinHeight() - 16) {
                e.setCancelled(true);
                plugin.finalizeDeath(p, DownedSession.Exit.BLEED_OUT);
                return;
            }
            if (e.getCause() == EntityDamageEvent.DamageCause.FALL) {
                e.setDamage(0); // prone body: no new injury, no window cost
                return;
            }
            DownedSession s = plugin.sessionOf(p.getUniqueId());
            double scaled = DownedMath.scaleDamage(e.getFinalDamage(), plugin.downedDamageFactor);
            long eaten = s.eatDamage(scaled, plugin.damageSecondsPerPoint);
            String source = "environment";
            if (e instanceof EntityDamageByEntityEvent) {
                Entity d = ((EntityDamageByEntityEvent) e).getDamager();
                source = d instanceof Player ? "player:" + ((Player) d).getName()
                        : d.getType().name().toLowerCase();
            }
            plugin.record("damage_while_downed", "\"player\":\"" + ExcelDowned.esc(p.getName())
                    + "\",\"uuid\":\"" + p.getUniqueId() + "\""
                    + ",\"source\":\"" + ExcelDowned.esc(source) + "\""
                    + ",\"scaled_amount\":" + ExcelDowned.round2(scaled)
                    + ",\"window_seconds_eaten\":" + ExcelDowned.round2(eaten / 1000.0));
            e.setDamage(0); // absorbed: health stays frozen at the downed value
            return;
        }

        // standing player: a lethal non-void hit becomes a down instead
        if (e.getCause() == EntityDamageEvent.DamageCause.VOID) return;
        if (e.isCancelled()) return;
        if (e.getFinalDamage() >= p.getHealth()) {
            e.setCancelled(true);
            plugin.enterDowned(p, e.getCause().name().toLowerCase());
        }
    }

    // ----- downed players cannot deal damage -----

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = false)
    public void onDamageByDowned(EntityDamageByEntityEvent e) {
        Player damager = null;
        if (e.getDamager() instanceof Player) damager = (Player) e.getDamager();
        else if (e.getDamager() instanceof Projectile) {
            Object shooter = ((Projectile) e.getDamager()).getShooter();
            if (shooter instanceof Player) damager = (Player) shooter;
        }
        if (damager != null && plugin.isDowned(damager.getUniqueId())) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onProjectileLaunch(ProjectileLaunchEvent e) {
        if (e.getEntity().getShooter() instanceof Player
                && plugin.isDowned(((Player) e.getEntity().getShooter()).getUniqueId())) {
            e.setCancelled(true);
        }
    }

    // ----- deaggro: mobs may not target a downed player -----

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = false)
    public void onTarget(EntityTargetEvent e) {
        if (e.getTarget() instanceof Player
                && plugin.isDowned(e.getTarget().getUniqueId())) {
            e.setCancelled(true);
            if (e.getEntity() instanceof Mob) ((Mob) e.getEntity()).setTarget(null);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = false)
    public void onTargetLiving(EntityTargetLivingEntityEvent e) {
        if (e.getTarget() instanceof Player
                && plugin.isDowned(e.getTarget().getUniqueId())) {
            e.setCancelled(true);
            if (e.getEntity() instanceof Mob) ((Mob) e.getEntity()).setTarget(null);
        }
    }

    // ----- health is frozen while downed: heals land, state does not lift -----

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onRegain(EntityRegainHealthEvent e) {
        if (e.getEntity() instanceof Player
                && plugin.isDowned(e.getEntity().getUniqueId())) {
            e.setCancelled(true);
        }
    }

    // ----- no function: world interaction is closed -----

    @EventHandler(priority = EventPriority.HIGH)
    public void onBreak(BlockBreakEvent e) {
        if (plugin.isDowned(e.getPlayer().getUniqueId())) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlace(BlockPlaceEvent e) {
        if (plugin.isDowned(e.getPlayer().getUniqueId())) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEvent e) {
        if (plugin.isDowned(e.getPlayer().getUniqueId())) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDrop(PlayerDropItemEvent e) {
        if (plugin.isDowned(e.getPlayer().getUniqueId())) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onSwap(PlayerSwapHandItemsEvent e) {
        if (plugin.isDowned(e.getPlayer().getUniqueId())) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onConsume(PlayerItemConsumeEvent e) {
        if (plugin.isDowned(e.getPlayer().getUniqueId())) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPickup(EntityPickupItemEvent e) {
        if (e.getEntity() instanceof Player
                && plugin.isDowned(e.getEntity().getUniqueId())) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onOpen(InventoryOpenEvent e) {
        // The give-up confirm menu is the one sanctioned exception to
        // the no-inventory rule (0.1.1) — like chat and commands, it is
        // how a downed player acts at all.
        if (e.getInventory().getHolder() instanceof GiveUpHolder) return;
        if (e.getPlayer() instanceof Player
                && plugin.isDowned(e.getPlayer().getUniqueId())) e.setCancelled(true);
    }

    // ----- death bookkeeping: a downed session must never double-count -----

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(PlayerDeathEvent e) {
        Player p = e.getEntity();
        if (plugin.bypassDeath().remove(p.getUniqueId())) return; // our own finalize path
        // any other real death while a session existed (e.g. /kill, void
        // from standing is session-less): close the bookkeeping exactly once
        plugin.noteExternalDeath(p);
    }

    // ----- logout / relog: wall clock rules -----

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        DownedSession s = plugin.sessionOf(p.getUniqueId());
        if (s == null) return;
        if (s.isExpired(System.currentTimeMillis())) {
            p.sendMessage("§c✝ You bled out while you were gone.");
            plugin.finalizeDeath(p, DownedSession.Exit.BLEED_OUT);
        } else {
            plugin.applyDownedState(p);
            p.sendMessage("§c✝ You are still DOWN — bleed out in "
                    + ExcelDowned.formatClock(s.remainingMs(System.currentTimeMillis()))
                    + ". /giveup to let go.");
        }
    }
}

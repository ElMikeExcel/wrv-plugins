package com.winkreyes.excelprotect;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

import java.util.List;
import java.util.UUID;

/**
 * ExcelProtect 0.1.3 — the Bukkit glue for damage DEALT incidents (SPEC §2):
 * a MONITOR listener over damage to NON-PLAYER living victims where the
 * resolved damager (projectile shooters included) is a player. Feeds the
 * pure DealtIncidents coalescer; closed incidents go to the Ledger's async
 * writer on the `dealt` stream — the event path does no IO.
 *
 * Registration order matters for spell attribution: this listener must be
 * registered BEFORE CombatCore so its claim PEEK runs before CombatCore's
 * per-event listener consumes the same claim for the combat stream (peek
 * never consumes; both layers attribute the same hit, neither starves).
 * ExcelProtect.onEnable wires it that way.
 * Excel — Wink Reyes Valley
 */
public final class DealtWatch implements Listener {
    private final Ledger ledger;
    private final DealtIncidents incidents;
    private final CombatCore combat;

    public DealtWatch(Plugin plugin, Ledger ledger, Settings cfg, CombatCore combat) {
        this.ledger = ledger;
        this.combat = combat;
        this.incidents = new DealtIncidents(cfg.damageIncidentGapMs());
        EliteProbe.init(plugin);
    }

    public String eliteState() { return EliteProbe.stateName(); }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof LivingEntity victim)) return;
        if (victim instanceof Player) return; // victim-side stream owns player victims
        double dmg = e.getFinalDamage();
        if (dmg <= 0) return;

        // Resolve the damager: a projectile hit belongs to its shooter.
        Entity resolved = null;
        String projectile = null;
        if (e instanceof EntityDamageByEntityEvent by) {
            Entity d = by.getDamager();
            if (d instanceof Projectile proj) {
                projectile = proj.getType().getKey().toString();
                d = proj.getShooter() instanceof Entity shooter ? shooter : null;
            }
            resolved = d;
        }
        if (!(resolved instanceof Player attacker)) return;

        String spell = combat == null ? null : combat.peekClaim(attacker.getUniqueId());
        String victimEntity = victim.getType().getKey().toString();
        String victimKey = victimEntity + ":" + victim.getUniqueId();
        String delivery = projectile != null ? "proj:" + projectile
                : spell != null ? "spell:" + spell : "hand";
        String sourceKey = "victim:" + victimKey + "|" + e.getCause().name() + "|" + delivery;
        Location loc = attacker.getLocation();
        AttributeInstance maxHp = victim.getAttribute(Attribute.MAX_HEALTH);
        DealtIncidents.Tick tick = new DealtIncidents.Tick(
                attacker.getUniqueId(), attacker.getName(),
                loc.getWorld() == null ? "" : loc.getWorld().getName(),
                loc.getX(), loc.getY(), loc.getZ(),
                e.getCause().name(), sourceKey, victimKey, victimEntity,
                victim.getUniqueId().toString(), victim.getCustomName(),
                projectile, spell, EliteProbe.levelOf(victim),
                dmg, victim.getHealth(), maxHp == null ? 20.0 : maxHp.getValue(),
                System.currentTimeMillis());
        emit(incidents.record(tick));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onVictimDeath(EntityDeathEvent e) {
        LivingEntity victim = e.getEntity();
        if (victim instanceof Player) return;
        String victimKey = victim.getType().getKey() + ":" + victim.getUniqueId();
        Player killer = victim.getKiller();
        emit(incidents.closeVictim(victimKey, killer == null ? null : killer.getUniqueId()));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent e) {
        emit(incidents.closePlayer(e.getPlayer().getUniqueId(), "quit"));
    }

    /** Sweep tick (1 s, main thread, map work only): close gap-expired incidents. */
    public void tick() {
        emit(incidents.sweep(System.currentTimeMillis()));
    }

    /** Plugin shutdown: close everything so no engagement tail is lost. */
    public void closeAll(String reason) {
        emit(incidents.closeAll(reason));
    }

    public int openCount() { return incidents.openCount(); }

    private void emit(List<DealtIncidents.Incident> closed) {
        for (DealtIncidents.Incident inc : closed) {
            World w = inc.world == null || inc.world.isEmpty() ? null : Bukkit.getWorld(inc.world);
            Location loc = w == null ? null : new Location(w, inc.x, inc.y, inc.z);
            ledger.emit("dealt", loc, inc.render());
        }
    }
}

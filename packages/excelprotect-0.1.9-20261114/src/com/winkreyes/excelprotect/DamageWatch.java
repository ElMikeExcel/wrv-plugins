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
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

import java.util.List;

/**
 * ExcelProtect 0.1.2 — the Bukkit glue for damage incidents (SPEC §3): a
 * MONITOR listener over player damage that resolves the attacker (projectile
 * owners included), captures FINAL damage and health, probes EliteMobs for
 * elite attribution, and feeds the pure DamageIncidents coalescer. Closed
 * incidents go to the Ledger's async writer — the event path does no IO.
 * Excel — Wink Reyes Valley
 */
public final class DamageWatch implements Listener {
    private final Ledger ledger;
    private final DamageIncidents incidents;

    public DamageWatch(Plugin plugin, Ledger ledger, Settings cfg) {
        this.ledger = ledger;
        this.incidents = new DamageIncidents(cfg.damageIncidentGapMs(), cfg.nearDeathFraction());
        EliteProbe.init(plugin);
    }

    public String eliteState() { return EliteProbe.stateName(); }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player victim)) return;
        double dmg = e.getFinalDamage();
        if (dmg <= 0) return;

        // Resolve the attacker: a projectile hit belongs to its shooter.
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

        String kind, attackerName = null, attackerEntity = null, attackerUuid = null;
        int eliteLevel = DamageIncidents.NOT_ELITE;
        if (resolved instanceof Player ap) {
            kind = "player";
            attackerName = ap.getName();
            attackerUuid = ap.getUniqueId().toString();
        } else if (resolved instanceof LivingEntity mob) {
            kind = "mob";
            attackerEntity = mob.getType().getKey().toString();
            attackerUuid = mob.getUniqueId().toString();
            eliteLevel = EliteProbe.levelOf(mob);
        } else if (resolved != null) {
            kind = "entity"; // non-living damager: primed TNT, falling block, …
            attackerEntity = resolved.getType().getKey().toString();
            attackerUuid = resolved.getUniqueId().toString();
        } else if (projectile != null) {
            kind = "projectile"; // owner not resolvable (dispenser, removed shooter)
        } else {
            kind = "environment";
        }

        String sourceKey = sourceKey(resolved, projectile, kind) + "|" + e.getCause().name();
        Location loc = victim.getLocation();
        AttributeInstance maxHp = victim.getAttribute(Attribute.MAX_HEALTH);
        DamageIncidents.Tick tick = new DamageIncidents.Tick(
                victim.getUniqueId(), victim.getName(),
                loc.getWorld() == null ? "" : loc.getWorld().getName(),
                loc.getX(), loc.getY(), loc.getZ(),
                e.getCause().name(), sourceKey, kind, attackerName, attackerEntity,
                attackerUuid, projectile, eliteLevel,
                dmg, victim.getHealth(), maxHp == null ? 20.0 : maxHp.getValue(),
                System.currentTimeMillis());
        emit(incidents.record(tick));
    }

    /** Coalescing identity: who (or what) is hurting the player, before the cause suffix. */
    static String sourceKey(Entity resolved, String projectile, String kind) {
        if (resolved instanceof Player ap) return "player:" + ap.getUniqueId();
        if (resolved != null) return "entity:" + resolved.getType().getKey() + ":" + resolved.getUniqueId();
        if (projectile != null) return "projectile:" + projectile;
        return "env";
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(PlayerDeathEvent e) {
        emit(incidents.closePlayer(e.getEntity().getUniqueId(), "death", true));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent e) {
        emit(incidents.closePlayer(e.getPlayer().getUniqueId(), "quit", false));
    }

    /** Sweep tick (1 s, main thread, map work only): close gap-expired incidents. */
    public void tick() {
        emit(incidents.sweep(System.currentTimeMillis()));
    }

    /** Plugin shutdown: close everything so no exposure tail is lost. */
    public void closeAll(String reason) {
        emit(incidents.closeAll(reason));
    }

    private void emit(List<DamageIncidents.Incident> closed) {
        for (DamageIncidents.Incident inc : closed) {
            World w = inc.world == null || inc.world.isEmpty() ? null : Bukkit.getWorld(inc.world);
            Location loc = w == null ? null : new Location(w, inc.x, inc.y, inc.z);
            ledger.emit("damage", loc, inc.render(incidents.nearDeathFraction()));
        }
    }
}

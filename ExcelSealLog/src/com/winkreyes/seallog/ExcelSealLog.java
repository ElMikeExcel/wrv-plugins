package com.winkreyes.seallog;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.boss.DragonBattle;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.raid.*;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.Raid;

import java.util.*;

/**
 * ExcelSealLog: one-line console logs for guild seal fights, prefixed [SealLog].
 * Format: "EVENT seal=<id> key=value ..." (values have no spaces; players comma-separated).
 * Read-only: MONITOR priority listeners, never cancels or modifies anything. No commands.
 */
public final class ExcelSealLog extends JavaPlugin implements Listener {

    static final int[] MILESTONES = {75, 50, 25};
    /** Per-boss tracking, keyed by entity UUID. */
    static final class Fight {
        final String seal; final Set<String> players = new TreeSet<>(); int nextMilestone = 0; boolean started;
        Fight(String seal) { this.seal = seal; }
    }
    private final Map<UUID, Fight> fights = new HashMap<>();
    private final Map<String, Set<String>> raidPlayers = new HashMap<>();

    @Override public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("READY version=" + getPluginMeta().getVersion());
    }

    static String sealOf(EntityType t) {
        switch (t) {
            case ENDER_DRAGON: return "dragon";
            case WITHER: return "wither";
            case ELDER_GUARDIAN: return "elder_guardian";
            case WARDEN: return "warden";
            default: return null;
        }
    }
    static String loc(Location l) {
        return l.getWorld().getName() + "@" + l.getBlockX() + "," + l.getBlockY() + "," + l.getBlockZ();
    }
    static String join(Collection<String> c) { return c.isEmpty() ? "-" : String.join(",", c); }
    /** Health milestone index crossed: returns new nextMilestone given percent remaining. */
    static int milestonesCrossed(int next, double pct, List<Integer> out) {
        while (next < MILESTONES.length && pct <= MILESTONES[next]) { out.add(MILESTONES[next]); next++; }
        return next;
    }
    /** Stable raid key: its center block (Raid#getId is deprecated for removal). */
    static String rid(Raid r) { Location l = r.getLocation(); return l.getBlockX() + "_" + l.getBlockY() + "_" + l.getBlockZ(); }
    private void log(String s) { getLogger().info(s); }
    private Fight fight(LivingEntity e, String seal) {
        if (fights.size() > 200) fights.clear(); // safety cap; boss fights are rare
        return fights.computeIfAbsent(e.getUniqueId(), k -> new Fight(seal));
    }
    private static List<String> nearbyPlayers(Location l, double r) {
        List<String> n = new ArrayList<>();
        for (Player p : l.getWorld().getPlayers()) if (p.getLocation().distanceSquared(l) <= r * r) n.add(p.getName());
        Collections.sort(n); return n;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onWorld(PlayerChangedWorldEvent e) {
        World w = e.getPlayer().getWorld();
        if (w.getEnvironment() != World.Environment.THE_END) return;
        DragonBattle b = w.getEnderDragonBattle();
        if (b == null) return;
        EnderDragon d = b.getEnderDragon();
        if (d == null || d.isDead()) return;
        Fight f = fight(d, "dragon");
        boolean first = !f.started; f.started = true; f.players.add(e.getPlayer().getName());
        double pct = 100.0 * d.getHealth() / d.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue();
        log((first ? "FIGHT_START" : "PLAYER_JOINED") + " seal=dragon player=" + e.getPlayer().getName()
                + " health=" + Math.round(pct) + "% players=" + join(f.players) + " at=" + loc(d.getLocation()));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSpawn(CreatureSpawnEvent e) {
        String seal = sealOf(e.getEntityType());
        if (!"wither".equals(seal) && !"warden".equals(seal)) return;
        Fight f = fight(e.getEntity(), seal); f.started = true;
        List<String> near = nearbyPlayers(e.getLocation(), 48); f.players.addAll(near);
        log((seal.equals("wither") ? "BOSS_SPAWN" : "BOSS_EMERGE") + " seal=" + seal + " reason=" + e.getSpawnReason()
                + " nearby=" + join(near) + " at=" + loc(e.getLocation()));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof LivingEntity le)) return;
        String seal = sealOf(le.getType());
        if (seal == null) return;
        String player = null;
        if (e instanceof EntityDamageByEntityEvent ed) {
            Entity src = ed.getDamager();
            if (src instanceof Projectile pr) { ProjectileSource s = pr.getShooter(); if (s instanceof Entity se) src = se; }
            if (src instanceof Player p) player = p.getName();
        }
        Fight f = fights.get(le.getUniqueId());
        if (f == null) { if (player == null) return; f = fight(le, seal); }
        if (player != null) {
            boolean added = f.players.add(player);
            if (!f.started) { f.started = true;
                log("FIGHT_START seal=" + seal + " trigger=first_player_damage player=" + player + " at=" + loc(le.getLocation())); }
            else if (added) log("PLAYER_JOINED seal=" + seal + " player=" + player + " players=" + join(f.players));
        }
        double max = le.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue();
        double after = Math.max(0, le.getHealth() - e.getFinalDamage());
        double pct = 100.0 * after / max;
        List<Integer> hit = new ArrayList<>();
        f.nextMilestone = milestonesCrossed(f.nextMilestone, pct, hit);
        for (int m : hit) if (after > 0)
            log("BOSS_HEALTH seal=" + seal + " milestone=" + m + "% health=" + Math.round(pct) + "% players=" + join(f.players));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(EntityDeathEvent e) {
        String seal = sealOf(e.getEntityType());
        if (seal == null) return;
        Fight f = fights.remove(e.getEntity().getUniqueId());
        Player k = e.getEntity().getKiller();
        Set<String> ps = f == null ? new TreeSet<>() : f.players;
        if (k != null) ps.add(k.getName());
        log("BOSS_DEATH seal=" + seal + " killer=" + (k == null ? "-" : k.getName()) + " players=" + join(ps)
                + " at=" + loc(e.getEntity().getLocation()));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onRaidTrigger(RaidTriggerEvent e) {
        Raid r = e.getRaid();
        Set<String> s = raidPlayers.computeIfAbsent(rid(r), k -> new TreeSet<>()); s.add(e.getPlayer().getName());
        log("RAID_START seal=raid id=" + rid(r) + " player=" + e.getPlayer().getName() + " omen=" + r.getBadOmenLevel()
                + " waves=" + r.getTotalWaves() + " village=" + loc(r.getLocation()));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRaidWave(RaidSpawnWaveEvent e) {
        Raid r = e.getRaid();
        Set<String> s = raidPlayers.computeIfAbsent(rid(r), k -> new TreeSet<>());
        s.addAll(nearbyPlayers(r.getLocation(), 96));
        log("RAID_WAVE seal=raid id=" + rid(r) + " wave=" + r.getSpawnedGroups() + "/" + r.getTotalWaves()
                + " raiders=" + e.getRaiders().size() + " players=" + join(s) + " village=" + loc(r.getLocation()));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRaidFinish(RaidFinishEvent e) {
        Raid r = e.getRaid();
        Set<String> s = raidPlayers.computeIfAbsent(rid(r), k -> new TreeSet<>());
        for (Player p : e.getWinners()) s.add(p.getName());
        log("RAID_WON seal=raid id=" + rid(r) + " winners=" + join(new TreeSet<>(e.getWinners().stream().map(Player::getName).toList()))
                + " players=" + join(s) + " village=" + loc(r.getLocation()));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRaidStop(RaidStopEvent e) {
        Raid r = e.getRaid();
        Set<String> s = raidPlayers.remove(rid(r));
        boolean lost = r.getStatus() == Raid.RaidStatus.LOSS;
        if (e.getReason() == RaidStopEvent.Reason.FINISHED && !lost) return; // RAID_WON already logged
        String kind = lost ? "RAID_LOST" : "RAID_ENDED";
        log(kind + " seal=raid id=" + rid(r) + " reason=" + e.getReason() + " players=" + join(s == null ? List.of() : s)
                + " village=" + loc(r.getLocation()));
    }
}

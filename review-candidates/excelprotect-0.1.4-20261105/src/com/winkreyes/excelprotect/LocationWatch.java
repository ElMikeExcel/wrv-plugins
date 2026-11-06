package com.winkreyes.excelprotect;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

/**
 * ExcelProtect 0.1.4 — the LOCATION TIMELINE (locations stream): where every
 * online player was, in order, so any stretch of a play session can be
 * rebuilt ("where was X between 6 and 7?" — the spine of every
 * Kailey-shaped reconstruction). Two record shapes on one stream:
 *
 *   location_sample — one position dot per online player per sample
 *     interval (Settings.locationsSampleSeconds, default 60 s): world +
 *     coords ride the ledger envelope; gamemode + health/food snapshot
 *     in the body. Samples are single records, never merged — at 60 s
 *     and ≤10 players the ceiling is ~14,400 lines/day and real play
 *     gives 2–5k (coverage map's volume math), noise next to blocks.
 *
 *   location_anchor — the events that EXPLAIN movement, each typed so a
 *     timeline rebuilds in order without touching another stream:
 *     join / quit / teleport (any cause, from in the envelope, to in
 *     the body — the players stream's convention) / world_change /
 *     respawn (bed/anchor flags) / gamemode (from → to).
 *
 * This is SAMPLING, not movement tracking: no PlayerMoveEvent hook
 * exists in this class or anywhere in 0.1.4. Mike's Nov 1 decision
 * excluded per-step movement records; a dot a minute plus anchors that
 * name every jump is the settled reconciliation (coverage map 0.1.4).
 *
 * The sampler is stateless: sample() reads the live player list, so a
 * quit needs no cleanup — an offline player simply stops appearing.
 * Map work: none. IO: ledger.emit only (writer thread owns the disk).
 * Excel — Wink Reyes Valley
 */
public final class LocationWatch implements Listener {
    private final Ledger ledger;
    private final Settings cfg;

    public LocationWatch(Ledger ledger, Settings cfg) {
        this.ledger = ledger; this.cfg = cfg;
    }

    // ------------------------------------------------------------ pure math
    /** Scheduler period for a sample interval in seconds (Bukkit ticks). */
    public static long periodTicks(int sampleSeconds) { return sampleSeconds * 20L; }

    /** Worst-case sample lines per day at a player count + interval. */
    public static long samplesPerDay(int players, int sampleSeconds) {
        return players * (86_400L / Math.max(1, sampleSeconds));
    }

    // ------------------------------------------------------------ sampling
    /** One sample per online player. Called by the sync timer in ExcelProtect. */
    public void sample() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            Json.Obj o = who(p).put("type", "location_sample")
                    .put("gamemode", p.getGameMode().name().toLowerCase())
                    .put("health", round1(p.getHealth()))
                    .put("food", p.getFoodLevel());
            ledger.emit("locations", p.getLocation(), o);
        }
    }

    private Json.Obj who(Player p) {
        return new Json.Obj().put("player", p.getName()).put("player_uuid", p.getUniqueId().toString())
                .put("platform", p.getClientBrandName() != null && p.getName().startsWith(".") ? "bedrock" : "java");
    }

    private Json.Obj anchor(Player p, String kind) {
        return who(p).put("type", "location_anchor").put("anchor", kind)
                .put("gamemode", p.getGameMode().name().toLowerCase());
    }

    static double round1(double v) { return Math.round(v * 10.0) / 10.0; }

    // ------------------------------------------------------------ anchors
    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent e) {
        ledger.emit("locations", e.getPlayer().getLocation(), anchor(e.getPlayer(), "join"));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent e) {
        ledger.emit("locations", e.getPlayer().getLocation(), anchor(e.getPlayer(), "quit"));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent e) {
        Location to = e.getTo();
        Json.Obj o = anchor(e.getPlayer(), "teleport")
                .put("cause", e.getCause().name().toLowerCase())
                .put("to_world", to.getWorld().getName())
                .put("to_x", Ledger.round(to.getX()))
                .put("to_y", Ledger.round(to.getY()))
                .put("to_z", Ledger.round(to.getZ()));
        ledger.emit("locations", e.getFrom(), o);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldChange(PlayerChangedWorldEvent e) {
        Json.Obj o = anchor(e.getPlayer(), "world_change")
                .put("from_world", e.getFrom().getName());
        ledger.emit("locations", e.getPlayer().getLocation(), o);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRespawn(PlayerRespawnEvent e) {
        Json.Obj o = anchor(e.getPlayer(), "respawn")
                .put("is_bed_spawn", e.isBedSpawn())
                .put("is_anchor_spawn", e.isAnchorSpawn());
        ledger.emit("locations", e.getRespawnLocation(), o);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onGameMode(PlayerGameModeChangeEvent e) {
        Json.Obj o = anchor(e.getPlayer(), "gamemode")
                .put("gamemode_from", e.getPlayer().getGameMode().name().toLowerCase())
                .put("gamemode_to", e.getNewGameMode().name().toLowerCase());
        ledger.emit("locations", e.getPlayer().getLocation(), o);
    }
}

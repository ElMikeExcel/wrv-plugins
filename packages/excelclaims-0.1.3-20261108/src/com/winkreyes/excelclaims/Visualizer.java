package com.winkreyes.excelclaims;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * ExcelClaims — claim-boundary visualization: corner markers
 * sent only to the viewing player (block-change packets, the
 * world itself is never touched), restored automatically. Shows
 * the four corners of a claim at the surface so a player can see
 * exactly what they own.
 *
 * 0.1.3: each corner is a short PILLAR (config
 * visualization.corner-pillar-height blocks in total, default
 * 4: glowstone body with a sea-lantern cap) instead of 0.1.2's
 * single glowstone block — a lone ground-level block disappears
 * behind terrain from many angles (Marv's player-proof report,
 * Nov 8, 2026; Mike's ruling the same evening). The pillar shape
 * itself is planned by CornerPillars (pure, unit-tested); this
 * class only resolves each corner's surface base Y and sends
 * the packets. Still packet-only per viewer; still 45 seconds.
 * Excel — Iris / Mike.
 */
public final class Visualizer {

    private final Plugin plugin;
    private final ClaimsConfig config;
    private final Map<UUID, List<Location>> shown = new HashMap<>();
    private final Map<UUID, BukkitTask> timers = new HashMap<>();

    public Visualizer(Plugin plugin, ClaimsConfig config) {
        this.plugin = plugin;
        this.config = config;
    }

    public void showCorners(Player player, ClaimRecord claim) {
        clear(player);
        World world = Bukkit.getWorld(claim.world);
        if (world == null) return;
        int[][] corners = {{claim.lx, claim.lz}, {claim.lx, claim.gz}, {claim.gx, claim.lz}, {claim.gx, claim.gz}};
        int[] baseYs = new int[corners.length];
        for (int i = 0; i < corners.length; i++) {
            baseYs[i] = CornerPillars.baseY(world.getHighestBlockYAt(corners[i][0], corners[i][1]), claim.ly);
        }
        List<CornerPillars.Block> plan = CornerPillars.plan(
                corners, baseYs, config.cornerPillarHeight, world.getMaxHeight() - 1);
        BlockData body = blockDataOr(config.cornerBlockMaterial, Material.GLOWSTONE);
        BlockData cap = blockDataOr(config.cornerCapMaterial, Material.SEA_LANTERN);
        List<Location> sent = new ArrayList<>();
        for (CornerPillars.Block b : plan) {
            Location loc = new Location(world, b.x(), b.y(), b.z());
            player.sendBlockChange(loc, b.cap() ? cap : body);
            sent.add(loc);
        }
        shown.put(player.getUniqueId(), sent);
        BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> clear(player), 20L * 45);
        timers.put(player.getUniqueId(), task);
    }

    /**
     * Block data for a configured marker material, falling back
     * when the config named a non-block material (whose
     * createBlockData throws): a typo'd config degrades to the
     * shipped marker, never to a broken visualization.
     */
    private static BlockData blockDataOr(Material configured, Material fallback) {
        try {
            return configured.createBlockData();
        } catch (RuntimeException e) {
            return fallback.createBlockData();
        }
    }

    public void clear(Player player) {
        BukkitTask t = timers.remove(player.getUniqueId());
        if (t != null) t.cancel();
        List<Location> sent = shown.remove(player.getUniqueId());
        if (sent == null || !player.isOnline()) return;
        for (Location loc : sent) {
            Block b = loc.getBlock();
            player.sendBlockChange(loc, b.getBlockData());
        }
    }

    public void clearAll() {
        for (UUID id : new ArrayList<>(shown.keySet())) {
            Player p = Bukkit.getPlayer(id);
            if (p != null) clear(p);
        }
        shown.clear();
    }
}

// ExcelClaims — Iris / Mike, for Wink Reyes Valley

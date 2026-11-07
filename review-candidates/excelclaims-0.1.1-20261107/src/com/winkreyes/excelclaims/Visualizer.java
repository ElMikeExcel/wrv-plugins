package com.winkreyes.excelclaims;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * ExcelClaims — claim-boundary visualization: glowstone corner
 * markers sent only to the viewing player (block-change packets, the
 * world itself is never touched), restored automatically. Shows the
 * four corners of a claim at the surface so a player can see exactly
 * what they own. Excel — Iris / Mike.
 */
public final class Visualizer {

    private final Plugin plugin;
    private final Map<UUID, List<Location>> shown = new HashMap<>();
    private final Map<UUID, BukkitTask> timers = new HashMap<>();

    public Visualizer(Plugin plugin) {
        this.plugin = plugin;
    }

    public void showCorners(Player player, ClaimRecord claim) {
        clear(player);
        World world = Bukkit.getWorld(claim.world);
        if (world == null) return;
        List<Location> sent = new ArrayList<>();
        int[][] corners = {{claim.lx, claim.lz}, {claim.lx, claim.gz}, {claim.gx, claim.lz}, {claim.gx, claim.gz}};
        for (int[] c : corners) {
            int y = world.getHighestBlockYAt(c[0], c[1]) + 1;
            if (y <= claim.ly) y = claim.ly + 1;
            Location loc = new Location(world, c[0], y, c[1]);
            player.sendBlockChange(loc, Material.GLOWSTONE.createBlockData());
            sent.add(loc);
        }
        shown.put(player.getUniqueId(), sent);
        BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> clear(player), 20L * 45);
        timers.put(player.getUniqueId(), task);
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

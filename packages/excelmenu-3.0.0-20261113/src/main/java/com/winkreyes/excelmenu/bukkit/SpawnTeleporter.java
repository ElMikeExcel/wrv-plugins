// ExcelMenu — an Excel plugin by WinkReyesValley. (Excel signature — MikeExcel)
// Excel signature - WRV maintained source; existing authorship notices preserved.
package com.winkreyes.excelmenu.bukkit;

import com.winkreyes.excelmenu.core.screens.SpawnFallback;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * The confirmed Spawn action, ported from ExcelBedrockMenu 1.5.28
 * (teleportToSpawn / villageFallback): run the real EssentialsX
 * spawn for the player from the console; if the command is
 * unavailable, or one tick later the player is still in the guild
 * world, fall back to the village coordinates (config spawn.x /
 * spawn.z — the same keys BedrockMenu used, now in ExcelMenu's
 * config.yml) in the overworld, landing on the highest block.
 */
final class SpawnTeleporter {

    private final JavaPlugin plugin;

    SpawnTeleporter(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    void teleportToSpawn(Player player) {
        boolean ok = false;
        try {
            ok = Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                    "spawn " + player.getName());
        } catch (Exception ignored) {
            ok = false;
        }
        final boolean dispatched = ok;
        if (!dispatched) {
            villageFallback(player);
            return;
        }
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (player.isOnline() && SpawnFallback.needsSpawnFallback(
                    true, player.getWorld().getName())) {
                villageFallback(player);
            }
        });
    }

    private void villageFallback(Player player) {
        World overworld = null;
        for (World w : Bukkit.getWorlds()) {
            if (SpawnFallback.isSpawnWorld(
                    w.getEnvironment() == World.Environment.NORMAL, w.getName())) {
                overworld = w;
                break;
            }
        }
        if (overworld == null) {
            player.sendMessage(SpawnFallback.UNAVAILABLE_MESSAGE);
            return;
        }
        double vx = plugin.getConfig().getDouble("spawn.x", SpawnFallback.DEFAULT_X);
        double vz = plugin.getConfig().getDouble("spawn.z", SpawnFallback.DEFAULT_Z);
        Location village = new Location(overworld, vx, SpawnFallback.PLACEHOLDER_Y, vz);
        village.setY(overworld.getHighestBlockYAt((int) vx, (int) vz) + 1);
        player.teleport(village);
    }
}

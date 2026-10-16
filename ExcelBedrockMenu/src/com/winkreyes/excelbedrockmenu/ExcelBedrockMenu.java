package com.winkreyes.excelbedrockmenu;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * /menu that actually works on Bedrock.
 * Geyser forms go through the reflection-based GeyserForms helper (no
 * compile-time Geyser dependency); falls back to the DeluxeMenus chest GUI
 * for Java players.
 *
 * v1.5.0: full rename of BedrockMenu 1.4.2-menu.1 to ExcelBedrockMenu
 * (package + main class + plugin name). No behavior changes. The button set
 * is the 1.4.2 one (includes the "Spawn Shop" button added by the 1.4.2 ASM
 * patch). On first enable, any data folder left by the old BedrockMenu
 * plugin is copied across so its config is preserved.
 *
 * v1.5.1: added the "Adventurers Guild" button, right after the shop
 * button (mirroring the Java menu). It runs /ag as the player via the same
 * performCommand path as the other command buttons. Nothing else changed.
 */
public class ExcelBedrockMenu extends JavaPlugin implements CommandExecutor {

    private static final String[] BUTTONS = {
        "Spawn", "Teleport to a player", "Spawn Shop", "Adventurers Guild",
        "Jobs & Money", "mcMMO Stats", "Leaderboard", "Server Info", "Auto-open on join"
    };
    private static final String[] COMMANDS = {
        "spawn", "tpmenu", "shop", "ag", "jobs browse",
        "mcstats", "leaderboard", "motd", "menujoin"
    };
    // Village spawn coordinates (fallback if Essentials spawn lookup fails)
    private static final double SPAWN_X = -784;
    private static final double SPAWN_Y = 70;
    private static final double SPAWN_Z = -272;

    @Override
    public void onEnable() {
        migrateLegacyDataFolder("BedrockMenu");
        getCommand("menu").setExecutor(this);
        TeleportMenu tp = new TeleportMenu(this);
        getCommand("tpmenu").setExecutor(tp);
        getServer().getPluginManager().registerEvents(tp, this);
        getLogger().info("ExcelBedrockMenu enabled: /menu uses native Bedrock dialogs.");
    }

    /**
     * One-time rename migration: if our data folder has no files yet and the
     * legacy plugin's data folder (plugins/<legacyName>) exists, copy its
     * contents across so no config or data is lost in the rename.
     */
    private void migrateLegacyDataFolder(String legacyName) {
        try {
            File newDir = getDataFolder();
            File legacyDir = new File(newDir.getParentFile(), legacyName);
            if (!legacyDir.isDirectory() || legacyDir.equals(newDir)) {
                return;
            }
            if (newDir.isDirectory()) {
                try (var stream = Files.walk(newDir.toPath())) {
                    if (stream.anyMatch(p -> !p.equals(newDir.toPath()))) {
                        return; // new folder already has content; never overwrite
                    }
                }
            }
            final Path from = legacyDir.toPath();
            final Path to = newDir.toPath();
            try (var stream = Files.walk(from)) {
                stream.forEach(src -> {
                    try {
                        Path dst = to.resolve(from.relativize(src));
                        if (Files.isDirectory(src)) {
                            Files.createDirectories(dst);
                        } else {
                            Files.createDirectories(dst.getParent());
                            Files.copy(src, dst);
                        }
                    } catch (IOException e) {
                        getLogger().warning("Data migration copy failed for " + src + ": " + e.getMessage());
                    }
                });
            }
            getLogger().info("Migrated legacy data folder plugins/" + legacyName + " -> plugins/" + newDir.getName());
        } catch (Exception e) {
            getLogger().warning("Legacy data migration (" + legacyName + ") failed: " + e.getMessage());
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use /menu.");
            return true;
        }
        Player player = (Player) sender;
        openMenu(player);
        return true;
    }

    private void openMenu(Player player) {
        boolean sent = GeyserForms.sendSimpleForm(player, "Server Menu",
                "What would you like to do?", BUTTONS,
                id -> {
                    if (id < 0 || id >= COMMANDS.length || !player.isOnline()) {
                        return;
                    }
                    String command = COMMANDS[id];
                    Bukkit.getScheduler().runTask(this, () -> runAction(player, command));
                },
                getLogger());
        if (sent) {
            return;
        }
        // Fallback for Java players (or if the form failed)
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                "dm open server_menu " + player.getName());
    }

    private void runAction(Player player, String command) {
        switch (command) {
            case "spawn":
                teleportToSpawn(player);
                break;
            case "tpmenu":
                TeleportMenu.open(this, player);
                break;
            default:
                player.performCommand(command);
                break;
        }
    }

    /**
     * Teleports the player to spawn without relying on the /spawn command.
     * Tries EssentialsXSpawn's spawn location first, falls back to the
     * hardcoded village coordinates.
     */
    private void teleportToSpawn(Player player) {
        try {
            // Try EssentialsXSpawn via reflection
            Plugin essentialsSpawn = Bukkit.getPluginManager().getPlugin("EssentialsXSpawn");
            if (essentialsSpawn == null) {
                essentialsSpawn = Bukkit.getPluginManager().getPlugin("EssentialsSpawn");
            }
            if (essentialsSpawn != null) {
                // EssentialsXSpawn stores spawn; get it via the Essentials API
                Plugin essentials = Bukkit.getPluginManager().getPlugin("Essentials");
                if (essentials != null) {
                    ClassLoader essLoader = essentials.getClass().getClassLoader();
                    Class<?> essClass = Class.forName("com.earth2me.essentials.Essentials",
                            true, essLoader);
                    Object essApi = essClass.getMethod("getInstance").invoke(null);
                    Object spawn = essClass.getMethod("getSpawn").invoke(essApi);
                    if (spawn != null) {
                        // Location is from Bukkit API, use it directly
                        org.bukkit.Location loc = (org.bukkit.Location) spawn;
                        player.teleport(loc);
                        getLogger().info("Teleported " + player.getName() + " to Essentials spawn.");
                        return;
                    }
                }
            }
        } catch (Exception e) {
            getLogger().warning("Essentials spawn lookup failed: " + e.getMessage()
                    + ", using village coordinates.");
        }
        // Fallback: village coordinates
        org.bukkit.World world = player.getWorld();
        org.bukkit.Location village = new org.bukkit.Location(world, SPAWN_X, SPAWN_Y, SPAWN_Z);
        // Find safe Y (highest block at x,z)
        int safeY = world.getHighestBlockYAt((int) SPAWN_X, (int) SPAWN_Z) + 1;
        village.setY(safeY);
        player.teleport(village);
        getLogger().info("Teleported " + player.getName() + " to village fallback.");
    }
}

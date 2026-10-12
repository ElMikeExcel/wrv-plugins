package com.winkreyes.bedrockmenu;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * /menu that actually works on Bedrock.
 * Geyser forms go through the reflection-based GeyserForms helper (no
 * compile-time Geyser dependency); falls back to the DeluxeMenus chest GUI
 * for Java players.
 *
 * v1.4.1: merged Steve's 1.3.0 teleport-to-player flow (TeleportMenu with
 * Bedrock Accept/Decline popup + Java clickable chat + player-head picker),
 * kept 1.2.3's Leaderboard button, Home/Set Home removed (LuckPerms revoked).
 */
public class BedrockMenu extends JavaPlugin implements CommandExecutor {

    private static final String[] BUTTONS = {
        "Spawn", "Teleport to a player", "Jobs & Money",
        "mcMMO Stats", "Leaderboard", "Server Info", "Auto-open on join"
    };
    private static final String[] COMMANDS = {
        "spawn", "tpmenu", "jobs browse",
        "mcstats", "leaderboard", "motd", "menujoin"
    };
    // Village spawn coordinates (fallback if Essentials spawn lookup fails)
    private static final double SPAWN_X = -784;
    private static final double SPAWN_Y = 70;
    private static final double SPAWN_Z = -272;

    @Override
    public void onEnable() {
        getCommand("menu").setExecutor(this);
        TeleportMenu tp = new TeleportMenu(this);
        getCommand("tpmenu").setExecutor(tp);
        getServer().getPluginManager().registerEvents(tp, this);
        getLogger().info("BedrockMenu enabled: /menu uses native Bedrock dialogs.");
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

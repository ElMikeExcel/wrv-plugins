package com.winkreyes.bedrockmenu;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * /menu that actually works on Bedrock.
 * Geyser forms go through the reflection-based GeyserForms helper (no
 * compile-time Geyser dependency); falls back to the DeluxeMenus chest GUI
 * for Java players.
 *
 * v1.4.1 merged Steve's 1.3.0 teleport-to-player flow (TeleportMenu with
 * Bedrock Accept/Decline popup + Java clickable chat + player-head picker),
 * kept 1.2.3's Leaderboard button, Home/Set Home removed (LuckPerms revoked).
 * v1.4.3 restores maintainable source, preserves the 1.4.2 Spawn Shop button,
 * and fixes spawn lookup and fallback-world selection.
 */
public class BedrockMenu extends JavaPlugin implements CommandExecutor {

    private static final String[] BUTTONS = {
        "Spawn", "Teleport to a player", "Spawn Shop",
        "mcMMO Stats", "Leaderboard", "Server Info", "Auto-open on join"
    };
    private static final String[] COMMANDS = {
        "spawn", "tpmenu", "shop",
        "mcstats", "leaderboard", "motd", "menujoin"
    };

    @Override
    public void onEnable() {
        saveDefaultConfig();
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
        if (!player.isOnline()) {
            return;
        }
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
     * Keeps the established direct-teleport behavior: Essentials' default spawn
     * first, then the village coordinates in the configured overworld.
     */
    private void teleportToSpawn(Player player) {
        Location destination = null;
        try {
            destination = SpawnLocations.essentialsSpawn(Bukkit.getPluginManager());
        } catch (ReflectiveOperationException | RuntimeException e) {
            getLogger().warning("Essentials spawn lookup failed: " + e.getMessage()
                    + ", using village coordinates.");
        }
        String source = "Essentials spawn";
        if (destination == null) {
            source = "village fallback";
            try {
                destination = SpawnLocations.villageFallback(Bukkit.getWorlds(),
                        getConfig().getString("fallback-world", ""));
            } catch (IllegalStateException e) {
                getLogger().warning("Cannot find a village spawn: " + e.getMessage());
                player.sendMessage(ChatColor.RED + "Spawn isn't available right now. Ask an adult to check the server.");
                return;
            }
        }
        // A cancelled teleport must not silently retry another destination.
        if (player.teleport(destination)) {
            getLogger().info("Teleported " + player.getName() + " to " + source + ".");
        } else {
            player.sendMessage(ChatColor.YELLOW + "Couldn't teleport to spawn right now. Please try again later.");
        }
    }
}

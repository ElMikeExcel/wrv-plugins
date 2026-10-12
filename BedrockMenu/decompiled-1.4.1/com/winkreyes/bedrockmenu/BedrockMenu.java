/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.Location
 *  org.bukkit.World
 *  org.bukkit.command.Command
 *  org.bukkit.command.CommandExecutor
 *  org.bukkit.command.CommandSender
 *  org.bukkit.entity.Player
 *  org.bukkit.event.Listener
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.plugin.java.JavaPlugin
 */
package com.winkreyes.bedrockmenu;

import com.winkreyes.bedrockmenu.GeyserForms;
import com.winkreyes.bedrockmenu.TeleportMenu;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public class BedrockMenu
extends JavaPlugin
implements CommandExecutor {
    private static final String[] BUTTONS = new String[]{"Spawn", "Teleport to a player", "Jobs & Money", "mcMMO Stats", "Leaderboard", "Server Info", "Auto-open on join"};
    private static final String[] COMMANDS = new String[]{"spawn", "tpmenu", "jobs browse", "mcstats", "leaderboard", "motd", "menujoin"};
    private static final double SPAWN_X = -784.0;
    private static final double SPAWN_Y = 70.0;
    private static final double SPAWN_Z = -272.0;

    public void onEnable() {
        this.getCommand("menu").setExecutor((CommandExecutor)this);
        TeleportMenu teleportMenu = new TeleportMenu(this);
        this.getCommand("tpmenu").setExecutor((CommandExecutor)teleportMenu);
        this.getServer().getPluginManager().registerEvents((Listener)teleportMenu, (Plugin)this);
        this.getLogger().info("BedrockMenu enabled: /menu uses native Bedrock dialogs.");
    }

    public boolean onCommand(CommandSender commandSender, Command command, String string, String[] stringArray) {
        if (!(commandSender instanceof Player)) {
            commandSender.sendMessage("Only players can use /menu.");
            return true;
        }
        Player player = (Player)commandSender;
        this.openMenu(player);
        return true;
    }

    private void openMenu(Player player) {
        boolean bl = GeyserForms.sendSimpleForm(player, "Server Menu", "What would you like to do?", BUTTONS, n -> {
            if (n < 0 || n >= COMMANDS.length || !player.isOnline()) {
                return;
            }
            String string = COMMANDS[n];
            Bukkit.getScheduler().runTask((Plugin)this, () -> this.runAction(player, string));
        }, this.getLogger());
        if (bl) {
            return;
        }
        Bukkit.dispatchCommand((CommandSender)Bukkit.getConsoleSender(), (String)("dm open server_menu " + player.getName()));
    }

    private void runAction(Player player, String string) {
        switch (string) {
            case "spawn": {
                this.teleportToSpawn(player);
                break;
            }
            case "tpmenu": {
                TeleportMenu.open(this, player);
                break;
            }
            default: {
                player.performCommand(string);
            }
        }
    }

    private void teleportToSpawn(Player player) {
        Plugin plugin;
        Plugin plugin2;
        try {
            plugin2 = Bukkit.getPluginManager().getPlugin("EssentialsXSpawn");
            if (plugin2 == null) {
                plugin2 = Bukkit.getPluginManager().getPlugin("EssentialsSpawn");
            }
            if (plugin2 != null && (plugin = Bukkit.getPluginManager().getPlugin("Essentials")) != null) {
                ClassLoader classLoader = plugin.getClass().getClassLoader();
                Class<?> clazz = Class.forName("com.earth2me.essentials.Essentials", true, classLoader);
                Object object = clazz.getMethod("getInstance", new Class[0]).invoke(null, new Object[0]);
                Object object2 = clazz.getMethod("getSpawn", new Class[0]).invoke(object, new Object[0]);
                if (object2 != null) {
                    Location location = (Location)object2;
                    player.teleport(location);
                    this.getLogger().info("Teleported " + player.getName() + " to Essentials spawn.");
                    return;
                }
            }
        }
        catch (Exception exception) {
            this.getLogger().warning("Essentials spawn lookup failed: " + exception.getMessage() + ", using village coordinates.");
        }
        plugin2 = player.getWorld();
        plugin = new Location((World)plugin2, -784.0, 70.0, -272.0);
        int n = plugin2.getHighestBlockYAt(-784, -272) + 1;
        plugin.setY((double)n);
        player.teleport((Location)plugin);
        this.getLogger().info("Teleported " + player.getName() + " to village fallback.");
    }
}


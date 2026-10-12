/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.command.Command
 *  org.bukkit.command.CommandExecutor
 *  org.bukkit.command.CommandSender
 *  org.bukkit.entity.Player
 *  org.bukkit.plugin.java.JavaPlugin
 */
package com.winkreyes.leaderboard;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class Leaderboard
extends JavaPlugin
implements CommandExecutor {
    public void onEnable() {
        this.getCommand("leaderboard").setExecutor((CommandExecutor)this);
        this.getLogger().info("Leaderboard enabled: /leaderboard shows all boards.");
    }

    public boolean onCommand(CommandSender commandSender, Command command, String string, String[] stringArray) {
        if (!(commandSender instanceof Player)) {
            commandSender.sendMessage("Only players can use /leaderboard.");
            return true;
        }
        Player player = (Player)commandSender;
        player.sendMessage("");
        player.sendMessage("\u00a76\u00a7l===== \u00a7e\u00a7lServer Leaderboards \u00a76\u00a7l=====");
        player.sendMessage("");
        player.sendMessage("\u00a7b\u00a7lWealth:");
        if (!player.performCommand("baltop")) {
            player.sendMessage("\u00a77Wealth board unavailable.");
        }
        player.sendMessage("");
        player.sendMessage("\u00a7b\u00a7lmcMMO Power:");
        if (!player.performCommand("mctop")) {
            player.sendMessage("\u00a77mcMMO board unavailable.");
        }
        player.sendMessage("");
        player.sendMessage("\u00a77Tip: \u00a7e/mctop <skill> \u00a77for a single skill, \u00a7e/baltop \u00a77for wealth.");
        return true;
    }
}


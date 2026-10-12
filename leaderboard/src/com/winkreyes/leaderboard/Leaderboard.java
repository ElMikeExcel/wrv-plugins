package com.winkreyes.leaderboard;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * /leaderboard - shows every server leaderboard in one place.
 * Runs each plugin's native leaderboard command as the player
 * so the output is always the real, formatted board.
 */
public class Leaderboard extends JavaPlugin implements CommandExecutor {

    @Override
    public void onEnable() {
        getCommand("leaderboard").setExecutor(this);
        getLogger().info("Leaderboard enabled: /leaderboard shows all boards.");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use /leaderboard.");
            return true;
        }

        player.sendMessage("");
        player.sendMessage("§6§l===== §e§lServer Leaderboards §6§l=====");

        // Wealth board (Essentials /baltop)
        player.sendMessage("");
        player.sendMessage("§b§lWealth:");
        if (!player.performCommand("baltop")) {
            player.sendMessage("§7Wealth board unavailable.");
        }

        // mcMMO power board (/mctop)
        player.sendMessage("");
        player.sendMessage("§b§lmcMMO Power:");
        if (!player.performCommand("mctop")) {
            player.sendMessage("§7mcMMO board unavailable.");
        }

        player.sendMessage("");
        player.sendMessage("§7Tip: §e/mctop <skill> §7for a single skill, §e/baltop §7for wealth.");
        return true;
    }
}

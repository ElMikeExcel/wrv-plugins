package com.winkreyes.restartcountdown;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

/**
 * True 2-minute restart countdown. Run /restartwarn from console or as an op:
 * broadcasts at 2:00, 1:00, 0:30, then counts 10..1, then shuts the server down.
 * /restartwarn cancel aborts a running countdown.
 */
public class RestartCountdown extends JavaPlugin {

    private BukkitTask countdownTask;

    @Override
    public void onEnable() {
        getLogger().info("RestartCountdown enabled. Use /restartwarn [reason] to start a 2-minute countdown.");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("restartwarn")) {
            return false;
        }
        if (!sender.hasPermission("restartcountdown.warn")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to do that.");
            return true;
        }
        if (args.length > 0 && args[0].equalsIgnoreCase("cancel")) {
            if (countdownTask != null) {
                countdownTask.cancel();
                countdownTask = null;
                Bukkit.broadcastMessage(ChatColor.YELLOW + "Restart countdown cancelled.");
                sender.sendMessage(ChatColor.YELLOW + "Countdown cancelled.");
            } else {
                sender.sendMessage(ChatColor.YELLOW + "No countdown is running.");
            }
            return true;
        }
        if (countdownTask != null) {
            sender.sendMessage(ChatColor.YELLOW + "A countdown is already running. Use /restartwarn cancel to stop it.");
            return true;
        }
        String reason = args.length > 0 ? String.join(" ", args).trim() : "";
        startCountdown(reason);
        sender.sendMessage(ChatColor.GREEN + "Restart countdown started."
                + (reason.isEmpty() ? "" : " Reason: " + reason));
        return true;
    }

    private void startCountdown(String reason) {
        String suffix = reason.isEmpty() ? "" : ChatColor.GRAY + " (" + reason + ")";
        final int[] remaining = {120};
        countdownTask = Bukkit.getScheduler().runTaskTimer(this, () -> {
            int s = remaining[0];
            if (s == 120) {
                Bukkit.broadcastMessage(ChatColor.RED + "Server restarting in 2 minutes - get somewhere safe!" + suffix);
            } else if (s == 60) {
                Bukkit.broadcastMessage(ChatColor.RED + "Server restarting in 1 minute!" + suffix);
            } else if (s == 30) {
                Bukkit.broadcastMessage(ChatColor.RED + "Server restarting in 30 seconds!" + suffix);
            } else if (s <= 10 && s > 0) {
                Bukkit.broadcastMessage(ChatColor.RED + "Restarting in " + s + "..." + suffix);
            } else if (s <= 0) {
                Bukkit.broadcastMessage(ChatColor.RED + "Restarting now!" + suffix);
                countdownTask.cancel();
                countdownTask = null;
                // Small delay so the final message flushes before shutdown
                Bukkit.getScheduler().runTaskLater(this, () -> Bukkit.getServer().shutdown(), 40L);
                return;
            }
            remaining[0]--;
        }, 0L, 20L);
    }
}

package com.winkreyes.restartcountdown;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

/** A two-minute, chat-and-title restart warning, followed by server shutdown. */
public class RestartCountdown extends JavaPlugin {
    private BukkitTask countdownTask;
    private BukkitTask shutdownTask;

    @Override
    public void onEnable() {
        getLogger().info("RestartCountdown enabled. Use /restartwarn [reason] to start a 2-minute countdown.");
    }

    @Override
    public void onDisable() {
        cancelCountdown();
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
            if (isCountdownRunning()) {
                cancelCountdown();
                announce(ChatColor.YELLOW + "Restart countdown cancelled.",
                        ChatColor.YELLOW + "Restart cancelled", "", 60);
                sender.sendMessage(ChatColor.YELLOW + "Countdown cancelled.");
            } else {
                sender.sendMessage(ChatColor.YELLOW + "No countdown is running.");
            }
            return true;
        }
        if (isCountdownRunning()) {
            sender.sendMessage(ChatColor.YELLOW + "A countdown is already running. Use /restartwarn cancel to stop it.");
            return true;
        }
        String reason = args.length > 0 ? String.join(" ", args).trim() : "";
        startCountdown(reason);
        sender.sendMessage(ChatColor.GREEN + "Restart countdown started."
                + (reason.isEmpty() ? "" : " Reason: " + reason));
        return true;
    }

    private boolean isCountdownRunning() {
        // Keep the final message-flush grace period cancellable, too.
        return countdownTask != null || shutdownTask != null;
    }

    private void cancelCountdown() {
        if (countdownTask != null) {
            countdownTask.cancel();
            countdownTask = null;
        }
        if (shutdownTask != null) {
            shutdownTask.cancel();
            shutdownTask = null;
        }
    }

    private void startCountdown(String reason) {
        String suffix = reason.isEmpty() ? "" : ChatColor.GRAY + " (" + reason + ")";
        final int[] remaining = {120};
        countdownTask = getServer().getScheduler().runTaskTimer(this, () -> {
            int seconds = remaining[0]--;
            if (seconds == 120) {
                announce(ChatColor.RED + "Server restarting in 2 minutes - get somewhere safe!" + suffix,
                        ChatColor.RED + "Restart in 2 minutes", reason, 80);
            } else if (seconds == 60) {
                announce(ChatColor.RED + "Server restarting in 1 minute!" + suffix,
                        ChatColor.RED + "Restart in 1 minute", reason, 80);
            } else if (seconds == 30) {
                announce(ChatColor.RED + "Server restarting in 30 seconds!" + suffix,
                        ChatColor.RED + "Restart in 30 seconds", reason, 60);
            } else if (seconds <= 10 && seconds > 0) {
                announce(ChatColor.RED + "Restarting in " + seconds + "..." + suffix,
                        ChatColor.RED + "Restart in " + seconds, reason, 20);
            } else if (seconds <= 0) {
                announce(ChatColor.RED + "Restarting now!" + suffix,
                        ChatColor.RED + "Restarting now!", reason, 40);
                countdownTask.cancel();
                countdownTask = null;
                // Preserve the recovered 40-tick delay for the final messages to flush.
                shutdownTask = getServer().getScheduler().runTaskLater(this, () -> {
                    shutdownTask = null;
                    getServer().shutdown();
                }, 40L);
            }
        }, 0L, 20L);
    }

    private void announce(String chatMessage, String title, String reason, int stayTicks) {
        getServer().broadcastMessage(chatMessage);
        for (Player player : getServer().getOnlinePlayers()) {
            player.sendTitle(title, ChatColor.GRAY + reason, 0, stayTicks, 0);
        }
    }
}

package com.winkreyes.wealthboard;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

import com.gamingmesh.jobs.Jobs;
import com.gamingmesh.jobs.container.Job;
import com.gamingmesh.jobs.container.JobsPlayer;

/**
 * Wealth info without the permanent sidebar.
 * Shows a brief title popup with the top-3 richest on every join,
 * and auto-enrolls players in all Jobs so they earn money with zero friction.
 * Full boards are in /leaderboard.
 */
public class WealthBoard extends JavaPlugin implements Listener {

    private Economy econ;
    private static final int TOP_N = 3;

    @Override
    public void onEnable() {
        if (!setupEconomy()) {
            getLogger().severe("Vault economy not found - disabling WealthBoard.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("WealthBoard enabled (join popup mode, no sidebar).");
    }

    private boolean setupEconomy() {
        RegisteredServiceProvider<Economy> rsp =
                getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        econ = rsp.getProvider();
        return econ != null;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        // Brief wealth popup shortly after join
        Bukkit.getScheduler().runTaskLater(this, () -> {
            if (player.isOnline()) {
                showWealthPopup(player);
            }
        }, 40L);
        // Auto-enroll in every Jobs job so players earn money for normal play
        // with zero sign-up friction.
        Bukkit.getScheduler().runTaskLater(this, () -> autoJoinJobs(player), 100L);
    }

    private void showWealthPopup(Player player) {
        List<Entry> top = topBalances();
        String title = WealthPopupText.TITLE;
        String subtitle = top.isEmpty() ? WealthPopupText.EMPTY_SUBTITLE : WealthPopupText.SUBTITLE;
        // Titles do not wrap on mobile. Keep them short; put full names and
        // balances in separate chat messages, which the client can wrap.
        player.sendTitle(title, subtitle, 10, 60, 20);
        if (top.isEmpty()) {
            player.sendMessage(ChatColor.GRAY + "No balances yet - go earn!");
        } else {
            player.sendMessage(ChatColor.GOLD + "Richest players");
            for (int i = 0; i < top.size(); i++) {
                Entry e = top.get(i);
                player.sendMessage(WealthPopupText.nameLine(i + 1, e.name));
                player.sendMessage(WealthPopupText.balanceLine(formatMoney(e.balance)));
            }
            player.sendMessage(ChatColor.GRAY + "Full boards: /leaderboard");
        }
    }

    private String formatMoney(double balance) {
        try {
            String formatted = econ.format(balance);
            // Strip currency symbols for the compact popup, keep the number
            return formatted.replaceAll("[^0-9.,]", "");
        } catch (Exception e) {
            return String.valueOf((int) balance);
        }
    }

    private void autoJoinJobs(Player player) {
        if (getServer().getPluginManager().getPlugin("Jobs") == null) {
            return;
        }
        try {
            JobsPlayer jp = Jobs.getPlayerManager().getJobsPlayer(player);
            if (jp == null) {
                return;
            }
            for (Job job : Jobs.getJobs()) {
                if (!jp.isInJob(job)) {
                    Jobs.getPlayerManager().joinJob(jp, job);
                }
            }
        } catch (Exception e) {
            getLogger().warning("Auto-join Jobs failed for " + player.getName() + ": " + e.getMessage());
        }
    }

    private List<Entry> topBalances() {
        List<Entry> all = new ArrayList<>();
        for (OfflinePlayer op : Bukkit.getOfflinePlayers()) {
            if (op.getName() == null) {
                continue;
            }
            try {
                double bal = econ.getBalance(op);
                all.add(new Entry(op.getName(), bal));
            } catch (Exception ignored) {
                // Skip players whose balance can't be read
            }
        }
        all.sort((a, b) -> Double.compare(b.balance, a.balance));
        return all.subList(0, Math.min(TOP_N, all.size()));
    }

    private static class Entry {
        final String name;
        final double balance;

        Entry(String name, double balance) {
            this.name = name;
            this.balance = balance;
        }
    }
}

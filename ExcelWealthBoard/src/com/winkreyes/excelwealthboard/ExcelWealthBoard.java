package com.winkreyes.excelwealthboard;

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
public class ExcelWealthBoard extends JavaPlugin implements Listener {

    private Economy econ;
    private static final int TOP_N = 3;

    @Override
    public void onEnable() {
        migrateLegacyDataFolder();
        if (!setupEconomy()) {
            getLogger().severe("Vault economy not found - disabling ExcelWealthBoard.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("ExcelWealthBoard enabled (join popup mode, no sidebar).");
    }

    private void migrateLegacyDataFolder() {
        java.io.File dataFolder = getDataFolder();
        if (dataFolder == null || dataFolder.getParentFile() == null) {
            return;
        }
        java.io.File legacy = new java.io.File(dataFolder.getParentFile(), "WealthBoard");
        LegacyDataMigration.migrate(legacy, dataFolder, message -> getLogger().info(message));
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
        // Wealth popup after join. Delay is 200 ticks (10s), not 40: on
        // Bedrock the client is still on the loading screen for the first
        // few seconds, and titles sent then never get seen (mobile.4 test:
        // early entries were missed behind the loading screen).
        Bukkit.getScheduler().runTaskLater(this, () -> {
            if (player.isOnline()) {
                showWealthPopup(player);
            }
        }, 200L);
        // Auto-enroll in every Jobs job so players earn money for normal play
        // with zero sign-up friction.
        Bukkit.getScheduler().runTaskLater(this, () -> autoJoinJobs(player), 100L);
    }

    private void showWealthPopup(Player player) {
        List<Entry> top = topBalances();
        String title = ChatColor.GOLD + "" + ChatColor.BOLD + "Wink Reyes Valley";
        if (top.isEmpty()) {
            player.sendTitle(title, ChatColor.GRAY + "No balances yet - go earn!", 10, 60, 20);
            return;
        }
        // Bedrock (via Geyser) does not render extra lines in a title subtitle,
        // and a single line with all entries clips on phone screens. So the
        // popup cycles one entry at a time under the same title: every line
        // is short enough to fit, and repeated title sends are the same
        // pattern the restart countdown already uses successfully.
        for (int i = 0; i < top.size(); i++) {
            Entry e = top.get(i);
            String subtitle = (i == 0 ? ChatColor.YELLOW + "" + ChatColor.BOLD + "Richest: " : "")
                    + ChatColor.GOLD + (i + 1) + ". "
                    + ChatColor.WHITE + e.name
                    + ChatColor.GREEN + " $" + formatMoney(e.balance);
            int fadeIn = (i == 0) ? 10 : 0;
            boolean last = i == top.size() - 1;
            int fadeOut = last ? 15 : 0;
            // Keep intermediate frames opaque past the next 60-tick update.
            // A 55-tick stay plus 5-tick fade allows a fade before rank 3.
            int stay = last ? 55 : 70;
            // 60 ticks (3s) per entry: mobile.3's ~1.75s was too fast to read.
            long delay = (long) i * 60L;
            Bukkit.getScheduler().runTaskLater(this, () -> {
                if (player.isOnline()) {
                    player.sendTitle(title, subtitle, fadeIn, stay, fadeOut);
                }
            }, delay);
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

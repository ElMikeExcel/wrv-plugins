package com.winkreyes.leaderboard.paper;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.lang.reflect.Method;
import java.util.UUID;
import java.util.logging.Logger;

/** Soft-dependency adapters. Vault goes through the standard services API.
 * mcMMO, Jobs, and EliteMobs are reached reflectively: each probe resolves
 * the documented API entry points on the EXACT jar that is installed, and a
 * source whose probe fails simply hides its category (per the spec) instead
 * of breaking the board. Probe handles are cached after first resolution. */
public final class SoftAdapters {

    private final Logger log;
    private Object economy; // net.milkbowl.vault.economy.Economy, kept as Object to avoid a hard link.
    private Method econGetBalance;

    private Method mcMMOPowerByName;
    private Object mcMMOTarget;
    private boolean mcMMOProbed;

    private Method jobsGetPlayer;
    private Method jobsProgressionLevels;
    private Method jobsProgressionLevel;
    private Method jobsTotalLevel;
    private boolean jobsProbed;

    private Method eliteGetBalance;
    private Object eliteTarget;
    private boolean eliteProbed;

    public SoftAdapters(Logger log) {
        this.log = log;
        probeVault();
    }

    // ---- Vault ----------------------------------------------------------

    private void probeVault() {
        try {
            if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
                return;
            }
            Class<?> economyClass = Class.forName("net.milkbowl.vault.economy.Economy");
            RegisteredServiceProvider<?> rsp = Bukkit.getServicesManager().getRegistration(economyClass);
            if (rsp == null || rsp.getProvider() == null) {
                return;
            }
            economy = rsp.getProvider();
            econGetBalance = economyClass.getMethod("getBalance", OfflinePlayer.class);
            log.info("ExcelLeaderboard: Richest category available (Vault).");
        } catch (ReflectiveOperationException | RuntimeException e) {
            economy = null;
            econGetBalance = null;
        }
    }

    public boolean vaultAvailable() {
        return economy != null;
    }

    public Double balance(OfflinePlayer player) {
        if (economy == null) {
            return null;
        }
        try {
            Object result = econGetBalance.invoke(economy, player);
            return result instanceof Number n ? n.doubleValue() : null;
        } catch (ReflectiveOperationException | RuntimeException e) {
            return null;
        }
    }

    // ---- mcMMO ----------------------------------------------------------

    private void probeMcMMO(String playerName) {
        if (mcMMOProbed) {
            return;
        }
        mcMMOProbed = true;
        if (Bukkit.getPluginManager().getPlugin("mcMMO") == null) {
            return;
        }
        // Preferred: DatabaseAPI.getPowerLevel(String) reads stored data for
        // offline players. Fallback: ExperienceAPI.getPowerLevel(String).
        for (String className : new String[]{
                "com.gmail.nossr50.api.DatabaseAPI",
                "com.gmail.nossr50.api.ExperienceAPI"}) {
            try {
                Class<?> api = Class.forName(className);
                Method m = api.getMethod("getPowerLevel", String.class);
                Object probe = m.invoke(null, playerName);
                if (probe instanceof Number) {
                    mcMMOPowerByName = m;
                    mcMMOTarget = null;
                    log.info("ExcelLeaderboard: mcMMO Power category available (" + className + ").");
                    return;
                }
            } catch (ReflectiveOperationException | RuntimeException ignored) {
                // Try the next candidate.
            }
        }
        log.info("ExcelLeaderboard: mcMMO present but its power-level API did not verify; category hidden.");
    }

    public Long mcMMOPower(OfflinePlayer player) {
        if (player.getName() == null) {
            return null;
        }
        probeMcMMO(player.getName());
        if (mcMMOPowerByName == null) {
            return null;
        }
        try {
            Object result = mcMMOPowerByName.invoke(mcMMOTarget, player.getName());
            return result instanceof Number n ? n.longValue() : null;
        } catch (ReflectiveOperationException | RuntimeException e) {
            return null;
        }
    }

    // ---- Jobs -----------------------------------------------------------

    private void probeJobs() {
        if (jobsProbed) {
            return;
        }
        jobsProbed = true;
        if (Bukkit.getPluginManager().getPlugin("Jobs") == null) {
            return;
        }
        try {
            Class<?> api = Class.forName("com.gamingmesh.jobs.api.JobsAPI");
            jobsGetPlayer = api.getMethod("getJobsPlayer", UUID.class);
            // Probe the result type with a throwaway lookup is not possible
            // without a player; resolve the member methods lazily per player
            // in jobsLevel() instead. JobsAPI itself verifying is the gate.
            log.info("ExcelLeaderboard: Jobs API located; category resolves per player.");
        } catch (ReflectiveOperationException | RuntimeException e) {
            jobsGetPlayer = null;
            log.info("ExcelLeaderboard: Jobs present but its API did not verify; category hidden.");
        }
    }

    public boolean jobsAvailable() {
        probeJobs();
        return jobsGetPlayer != null;
    }

    /** Decision (per spec, made at build): the Jobs board shows TOTAL job
     * level, the sum of the player's levels across all jobs. Jobs exposes no
     * clean lifetime-earnings read for offline players. */
    public Long jobsTotalLevel(OfflinePlayer player) {
        probeJobs();
        if (jobsGetPlayer == null) {
            return null;
        }
        try {
            Object jobsPlayer = jobsGetPlayer.invoke(null, player.getUniqueId());
            if (jobsPlayer == null) {
                return 0L;
            }
            if (jobsTotalLevel == null) {
                try {
                    jobsTotalLevel = jobsPlayer.getClass().getMethod("getTotalLevel");
                } catch (NoSuchMethodException ignored) {
                    jobsTotalLevel = null;
                }
            }
            if (jobsTotalLevel != null) {
                Object total = jobsTotalLevel.invoke(jobsPlayer);
                if (total instanceof Number n) {
                    return n.longValue();
                }
            }
            if (jobsProgressionLevels == null) {
                jobsProgressionLevels = jobsPlayer.getClass().getMethod("getJobProgression");
            }
            Object progression = jobsProgressionLevels.invoke(jobsPlayer);
            if (!(progression instanceof Iterable<?> iterable)) {
                return null;
            }
            long sum = 0;
            for (Object job : iterable) {
                if (job == null) {
                    continue;
                }
                if (jobsProgressionLevel == null
                        || !jobsProgressionLevel.getDeclaringClass().isInstance(job)) {
                    jobsProgressionLevel = job.getClass().getMethod("getLevel");
                }
                Object level = jobsProgressionLevel.invoke(job);
                if (level instanceof Number n) {
                    sum += n.longValue();
                }
            }
            return sum;
        } catch (ReflectiveOperationException | RuntimeException e) {
            return null;
        }
    }

    // ---- EliteMobs ------------------------------------------------------

    private void probeElite() {
        if (eliteProbed) {
            return;
        }
        eliteProbed = true;
        if (Bukkit.getPluginManager().getPlugin("EliteMobs") == null) {
            return;
        }
        String[][] candidates = {
                {"com.magmaguy.elitemobs.economy.EconomyAPI", "getBalance"},
                {"com.magmaguy.elitemobs.economy.EconomyAPI", "getCurrency"},
                {"com.magmaguy.elitemobs.api.EliteMobsAPI", "getBalance"},
        };
        for (String[] candidate : candidates) {
            try {
                Class<?> api = Class.forName(candidate[0]);
                Method m = api.getMethod(candidate[1], UUID.class);
                eliteGetBalance = m;
                eliteTarget = null;
                log.info("ExcelLeaderboard: Elite Coins category available (" + candidate[0] + "." + candidate[1] + ").");
                return;
            } catch (ReflectiveOperationException | RuntimeException ignored) {
                // Try the next candidate.
            }
        }
        log.info("ExcelLeaderboard: EliteMobs present but no currency API verified; Elite Coins category hidden.");
    }

    public boolean eliteAvailable() {
        probeElite();
        return eliteGetBalance != null;
    }

    public Long eliteCoins(OfflinePlayer player) {
        probeElite();
        if (eliteGetBalance == null) {
            return null;
        }
        try {
            Object result = eliteGetBalance.invoke(eliteTarget, player.getUniqueId());
            return result instanceof Number n ? Math.round(n.doubleValue()) : null;
        } catch (ReflectiveOperationException | RuntimeException e) {
            return null;
        }
    }
}

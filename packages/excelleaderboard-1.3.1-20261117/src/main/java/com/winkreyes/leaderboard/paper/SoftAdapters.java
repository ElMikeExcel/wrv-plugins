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

    private Method mcMMOPowerOffline;
    private boolean mcMMOProbed;

    private Object jobsPlayerManager;
    private Method jobsGetPlayer;
    private Method jobsProgressionLevels;
    private Method jobsProgressionLevel;
    private Method jobsTotalLevels;
    private boolean jobsProbed;

    private Method eliteGetBalance;
    private Object eliteTarget;
    private boolean eliteProbed;

    private Object gpDataStore;
    private Method gpGetPlayerData;
    private Method gpGetAccrued;
    private Method gpGetBonus;
    private Method gpGetClaims;
    private Method gpClaimArea;
    private Method gpClaimLesserCorner;
    private java.lang.reflect.Field gpClaimParent;
    private Object gpInstance;
    private Method gpClaimsEnabledForWorld;
    private boolean gpProbed;

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
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
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
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            return null;
        }
    }

    // ---- mcMMO ----------------------------------------------------------

    private void probeMcMMO() {
        if (mcMMOProbed) {
            return;
        }
        mcMMOProbed = true;
        if (Bukkit.getPluginManager().getPlugin("mcMMO") == null) {
            return;
        }
        // Verified against the installed mcMMO 2.3.003 jar (javap, Oct 14):
        // ExperienceAPI.getPowerLevelOffline(UUID) is the offline power
        // read. (1.0 probed a getPowerLevel(String) method that does not
        // exist in this build, so the category hid at the first live boot.)
        // Existence of the exact method IS the verification; no probe
        // call is made. LiveApiSignatureTests guards this signature.
        try {
            Class<?> api = Class.forName("com.gmail.nossr50.api.ExperienceAPI");
            mcMMOPowerOffline = api.getMethod("getPowerLevelOffline", UUID.class);
            log.info("ExcelLeaderboard: mcMMO Power category available (ExperienceAPI.getPowerLevelOffline).");
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            mcMMOPowerOffline = null;
            log.info("ExcelLeaderboard: mcMMO present but its power-level API did not verify; category hidden.");
        }
    }

    public Long mcMMOPower(OfflinePlayer player) {
        probeMcMMO();
        if (mcMMOPowerOffline == null) {
            return null;
        }
        try {
            Object result = mcMMOPowerOffline.invoke(null, player.getUniqueId());
            return result instanceof Number n ? n.longValue() : null;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
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
        // Verified against the installed Jobs 5.2.6.3 jar (javap, Oct 14):
        // there is NO com.gamingmesh.jobs.api.JobsAPI class in this build
        // (the api package holds only events), which is why 1.0's probe
        // failed at the first live boot. The real entry point is the main
        // Jobs class: Jobs.getPlayerManager() -> PlayerManager, and
        // PlayerManager.getJobsPlayer(UUID) -> JobsPlayer.
        try {
            Class<?> jobsMain = Class.forName("com.gamingmesh.jobs.Jobs");
            Method getPlayerManager = jobsMain.getMethod("getPlayerManager");
            jobsPlayerManager = getPlayerManager.invoke(null);
            if (jobsPlayerManager == null) {
                throw new IllegalStateException("Jobs PlayerManager not ready");
            }
            jobsGetPlayer = jobsPlayerManager.getClass().getMethod("getJobsPlayer", UUID.class);
            log.info("ExcelLeaderboard: Jobs category available (Jobs.getPlayerManager).");
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            jobsPlayerManager = null;
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
            Object jobsPlayer = jobsGetPlayer.invoke(jobsPlayerManager, player.getUniqueId());
            if (jobsPlayer == null) {
                return 0L;
            }
            // JobsPlayer.getTotalLevels() (plural) is the real method in
            // Jobs 5.2.6.3, verified by javap; 1.0 looked up the singular.
            if (jobsTotalLevels == null) {
                try {
                    jobsTotalLevels = jobsPlayer.getClass().getMethod("getTotalLevels");
                } catch (NoSuchMethodException ignored) {
                    jobsTotalLevels = null;
                }
            }
            if (jobsTotalLevels != null) {
                Object total = jobsTotalLevels.invoke(jobsPlayer);
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
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
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
            } catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) {
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
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            return null;
        }
    }

    // ---- GriefPrevention --------------------------------------------------

    /** Raw GriefPrevention numbers for one player, exactly as GP stores
     * them; ClaimBlocksMath turns this into the board's totals. */
    public record RawClaims(long accrued, long bonus,
                            java.util.List<com.winkreyes.leaderboard.core.ClaimBlocksMath.ClaimFigure> claims) {}

    private void probeGriefPrevention() {
        if (gpProbed) {
            return;
        }
        gpProbed = true;
        if (Bukkit.getPluginManager().getPlugin("GriefPrevention") == null) {
            return;
        }
        // Verified against the installed GriefPrevention 16.18.7 jar
        // (javap, Oct 18, 2026): GriefPrevention.instance is a public
        // static field, dataStore a public field on it; DataStore
        // .getPlayerData(UUID) loads offline players from storage;
        // PlayerData exposes getAccruedClaimBlocks / getBonusClaimBlocks
        // / getClaims; Claim exposes getArea and getLesserBoundaryCorner
        // plus a public parent field (non-null = subdivision); and
        // GriefPrevention.claimsEnabledForWorld(World) is GP's own
        // per-world gate. Existence of the exact methods IS the
        // verification; LiveApiSignatureTests guards these signatures.
        try {
            Class<?> gpMain = Class.forName("me.ryanhamshire.GriefPrevention.GriefPrevention");
            gpInstance = gpMain.getField("instance").get(null);
            if (gpInstance == null) {
                throw new IllegalStateException("GriefPrevention instance not ready");
            }
            gpDataStore = gpMain.getField("dataStore").get(gpInstance);
            if (gpDataStore == null) {
                throw new IllegalStateException("GriefPrevention data store not ready");
            }
            gpClaimsEnabledForWorld = gpMain.getMethod("claimsEnabledForWorld", org.bukkit.World.class);
            gpGetPlayerData = gpDataStore.getClass().getMethod("getPlayerData", UUID.class);
            Class<?> playerData = gpGetPlayerData.getReturnType();
            gpGetAccrued = playerData.getMethod("getAccruedClaimBlocks");
            gpGetBonus = playerData.getMethod("getBonusClaimBlocks");
            gpGetClaims = playerData.getMethod("getClaims");
            Class<?> claim = Class.forName("me.ryanhamshire.GriefPrevention.Claim");
            gpClaimArea = claim.getMethod("getArea");
            gpClaimLesserCorner = claim.getMethod("getLesserBoundaryCorner");
            gpClaimParent = claim.getField("parent");
            log.info("ExcelLeaderboard: Land Claimed category available (GriefPrevention).");
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            gpDataStore = null;
            gpGetPlayerData = null;
            log.info("ExcelLeaderboard: GriefPrevention present but its API did not verify; Land Claimed category hidden.");
        }
    }

    public boolean claimsAvailable() {
        probeGriefPrevention();
        return gpGetPlayerData != null;
    }

    /** The player's GP numbers, or null when GP has no readable data.
     * getPlayerData loads from storage for offline players and creates
     * a fresh record (accrued = the configured initial blocks, no
     * claims) for a player GP has never seen, which is exactly the
     * brand-new-player row the board should show. */
    public RawClaims claimData(OfflinePlayer player) {
        probeGriefPrevention();
        if (gpGetPlayerData == null) {
            return null;
        }
        try {
            Object playerData = gpGetPlayerData.invoke(gpDataStore, player.getUniqueId());
            if (playerData == null) {
                return null;
            }
            long accrued = ((Number) gpGetAccrued.invoke(playerData)).longValue();
            long bonus = ((Number) gpGetBonus.invoke(playerData)).longValue();
            Object claims = gpGetClaims.invoke(playerData);
            java.util.List<com.winkreyes.leaderboard.core.ClaimBlocksMath.ClaimFigure> figures =
                    new java.util.ArrayList<>();
            if (claims instanceof Iterable<?> iterable) {
                for (Object claim : iterable) {
                    if (claim == null) {
                        continue;
                    }
                    int area = ((Number) gpClaimArea.invoke(claim)).intValue();
                    boolean subdivision = gpClaimParent.get(claim) != null;
                    figures.add(new com.winkreyes.leaderboard.core.ClaimBlocksMath.ClaimFigure(
                            area, claimWorldEnabled(claim), subdivision));
                }
            }
            return new RawClaims(accrued, bonus, figures);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            return null;
        }
    }

    /** Asks GP itself whether claims count in the claim's world. Any
     * failure answers "enabled": GP's own remaining-blocks count does
     * not world-filter either, so failing open matches GP. */
    private boolean claimWorldEnabled(Object claim) {
        try {
            Object corner = gpClaimLesserCorner.invoke(claim);
            if (corner instanceof org.bukkit.Location location && location.getWorld() != null) {
                Object enabled = gpClaimsEnabledForWorld.invoke(gpInstance, location.getWorld());
                return !(enabled instanceof Boolean b) || b;
            }
        } catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) {
            // Fall through to enabled.
        }
        return true;
    }
}

package com.winkreyes.excelguide;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

/**
 * The Guide's one GriefPrevention read (0.2.2): {accrued, bonus,
 * used, remaining} claim blocks for ONE player, or null when GP has
 * no readable data. Extracted from BukkitFacts, where this exact
 * reflection has run live since 0.2.0 (the AI fact sheet) - the
 * signatures are the ones ExcelLeaderboard 1.3 verified against the
 * installed GriefPrevention 16.18.7 jar (GriefPrevention.instance
 * .dataStore.getPlayerData(UUID); PlayerData getAccruedClaimBlocks /
 * getBonusClaimBlocks / getClaims; Claim getArea + parent field;
 * GriefPrevention.claimsEnabledForWorld). Used = summed area of the
 * player's own top-level claims in claims-enabled worlds (how GP
 * itself counts - subdivisions carve up a parent and consume
 * nothing). Any failure returns null instead of guessing; both
 * consumers (the deterministic stat answer and the AI fact sheet)
 * treat null as "omit the numbers".
 */
public final class GpClaims {

    // GriefPrevention reflection handles (probed once, lazily).
    private boolean gpProbed;
    private Object gpInstance;
    private Object gpDataStore;
    private Method gpGetPlayerData;
    private Method gpGetAccrued;
    private Method gpGetBonus;
    private Method gpGetClaims;
    private Method gpClaimArea;
    private Field gpClaimParent;
    private Method gpClaimLesserCorner;
    private Method gpClaimsEnabledForWorld;

    /** True when the GP probe succeeded and reads are worth attempting. */
    public synchronized boolean available() {
        return probeGriefPrevention();
    }

    /** {accrued, bonus, used, remaining} for ONE player, or null. */
    public long[] claimBlocks(UUID playerId) {
        if (!probeGriefPrevention()) {
            return null;
        }
        try {
            Object playerData = gpGetPlayerData.invoke(gpDataStore, playerId);
            if (playerData == null) {
                return null;
            }
            long accrued = ((Number) gpGetAccrued.invoke(playerData)).longValue();
            long bonus = ((Number) gpGetBonus.invoke(playerData)).longValue();
            long used = 0;
            Object claims = gpGetClaims.invoke(playerData);
            if (claims instanceof Iterable<?> iterable) {
                for (Object claim : iterable) {
                    if (claim == null || gpClaimParent.get(claim) != null) {
                        continue;
                    }
                    if (!claimWorldEnabled(claim)) {
                        continue;
                    }
                    used += Math.max(0, ((Number) gpClaimArea.invoke(claim)).intValue());
                }
            }
            long total = Math.max(0, accrued) + Math.max(0, bonus);
            return new long[]{accrued, bonus, used, total - used};
        } catch (Throwable t) {
            return null;
        }
    }

    private synchronized boolean probeGriefPrevention() {
        if (gpProbed) {
            return gpGetPlayerData != null;
        }
        gpProbed = true;
        if (Bukkit.getPluginManager().getPlugin("GriefPrevention") == null) {
            return false;
        }
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
            gpClaimsEnabledForWorld = gpMain.getMethod("claimsEnabledForWorld", World.class);
            gpGetPlayerData = gpDataStore.getClass().getMethod("getPlayerData", UUID.class);
            Class<?> playerData = gpGetPlayerData.getReturnType();
            gpGetAccrued = playerData.getMethod("getAccruedClaimBlocks");
            gpGetBonus = playerData.getMethod("getBonusClaimBlocks");
            gpGetClaims = playerData.getMethod("getClaims");
            Class<?> claim = Class.forName("me.ryanhamshire.GriefPrevention.Claim");
            gpClaimArea = claim.getMethod("getArea");
            gpClaimLesserCorner = claim.getMethod("getLesserBoundaryCorner");
            gpClaimParent = claim.getField("parent");
            return true;
        } catch (Throwable t) {
            gpDataStore = null;
            gpGetPlayerData = null;
            return false;
        }
    }

    /** GP's own per-world gate; any failure answers "enabled", matching GP's remaining count. */
    private boolean claimWorldEnabled(Object claim) {
        try {
            Object corner = gpClaimLesserCorner.invoke(claim);
            if (corner instanceof Location location && location.getWorld() != null) {
                Object enabled = gpClaimsEnabledForWorld.invoke(gpInstance, location.getWorld());
                return !(enabled instanceof Boolean b) || b;
            }
        } catch (Throwable ignored) {
            // fall through to enabled
        }
        return true;
    }
}

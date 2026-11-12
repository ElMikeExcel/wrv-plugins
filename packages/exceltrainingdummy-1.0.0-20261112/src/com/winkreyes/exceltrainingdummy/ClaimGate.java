package com.winkreyes.exceltrainingdummy;

import com.winkreyes.exceltrainingdummy.core.ZoneCheck;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Claim facts for the spawn-zone check (DESIGN §1), resolved by
 * reflection against the claims plugin — WRV runs ExcelClaims, the
 * house GriefPrevention fork, whose plugin name stays
 * "GriefPrevention" and whose API surface (DataStore.getClaimAt,
 * Claim.getOwnerID / isAdminClaim / parent) is the GP one. No
 * compile-time dependency: the plugin enables anywhere.
 *
 * Failure posture is conservative (fail closed): if a claims plugin
 * IS present but its API cannot be read, spawns are denied and the
 * reason is logged — a training dummy never lands somewhere the
 * check could not vouch for. If no claims plugin is present at all,
 * every spot is wilderness.
 */
public final class ClaimGate {
    private final Logger log;
    private boolean warnedBroken;

    public ClaimGate(Logger log) { this.log = log; }

    public ZoneCheck.Verdict verdictAt(Location loc, Player spawner) {
        Plugin gp = Bukkit.getPluginManager().getPlugin("GriefPrevention");
        if (gp == null || !gp.isEnabled()) return ZoneCheck.Verdict.WILDERNESS;
        try {
            ClassLoader cl = gp.getClass().getClassLoader();
            Class<?> gpClass = Class.forName("me.ryanhamshire.GriefPrevention.GriefPrevention", true, cl);
            Object instance = gpClass.getField("instance").get(null);
            Object dataStore = gpClass.getField("dataStore").get(instance);
            Class<?> claimClass = Class.forName("me.ryanhamshire.GriefPrevention.Claim", true, cl);
            Method getClaimAt = dataStore.getClass().getMethod(
                    "getClaimAt", Location.class, boolean.class, claimClass);
            Object claim = getClaimAt.invoke(dataStore, loc, true, null);
            if (claim == null) return ZoneCheck.Verdict.WILDERNESS;
            boolean admin = Boolean.TRUE.equals(claimClass.getMethod("isAdminClaim").invoke(claim));
            // Subclaims resolve to the parent's owner before judging.
            UUID owner = ownerId(claimClass, claim);
            Field parentField = claimClass.getField("parent");
            Object cursor = claim;
            while (owner == null && cursor != null) {
                cursor = parentField.get(cursor);
                if (cursor != null) owner = ownerId(claimClass, cursor);
            }
            boolean owns = owner != null && owner.equals(spawner.getUniqueId());
            return ZoneCheck.evaluate(true, admin, owns,
                    spawner.hasPermission("exceltrainingdummy.admin"));
        } catch (Exception e) {
            if (!warnedBroken) {
                warnedBroken = true;
                log.warning("ExcelTrainingDummy: claims API unreadable (" + e
                        + ") — spawns fail closed until this is resolved.");
            }
            // Present but unreadable: deny (evaluate as a foreign claim).
            return ZoneCheck.evaluate(true, false, false, false);
        }
    }

    private static UUID ownerId(Class<?> claimClass, Object claim) throws Exception {
        Object id = claimClass.getMethod("getOwnerID").invoke(claim);
        return id instanceof UUID ? (UUID) id : null;
    }
}

// Excel signature - WRV maintained source; original authorship and license notices preserved.
/* SPDX-License-Identifier: MIT */
package com.winkreyes.shops.store;

import java.lang.reflect.Method;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.plugin.Plugin;

/** Uses the public GP 16.18.7 API reflectively, with no copied or bundled GP code. */
public final class GriefPreventionOwnership implements ClaimOwnership {
    private final Server server;
    public GriefPreventionOwnership(Server server) { this.server = server; }
    @Override public ClaimStatus at(Location location) throws RegistryException {
        Plugin gp = server.getPluginManager().getPlugin("GriefPrevention");
        if (gp == null || !gp.isEnabled()) return ClaimStatus.unavailable();
        try {
            ClassLoader loader = gp.getClass().getClassLoader();
            Class<?> claimClass = Class.forName("me.ryanhamshire.GriefPrevention.Claim", false, loader);
            Object store = gp.getClass().getField("dataStore").get(gp);
            Method lookup = store.getClass().getMethod("getClaimAt", Location.class, boolean.class, claimClass);
            // Include vertically underlying claims so conversion cannot bypass a claim boundary.
            Object claim = lookup.invoke(store, location, true, null);
            if (claim == null) return ClaimStatus.wilderness();
            UUID owner = (UUID) claimClass.getMethod("getOwnerID").invoke(claim);
            Object id = claimClass.getMethod("getID").invoke(claim);
            Object parent = claimClass.getField("parent").get(claim);
            // Subdivisions may not have their own stable ID. Pin parent ID and exact boundaries.
            Object stable = parent == null ? id : claimClass.getMethod("getID").invoke(parent);
            if (stable == null) throw new ReflectiveOperationException("Claim has no stable ID");
            String identity = stable.toString();
            if (parent != null) {
                Location low = (Location) claimClass.getMethod("getLesserBoundaryCorner").invoke(claim);
                Location high = (Location) claimClass.getMethod("getGreaterBoundaryCorner").invoke(claim);
                identity += ":sub:" + low.getBlockX() + "," + low.getBlockY() + "," + low.getBlockZ()
                        + ":" + high.getBlockX() + "," + high.getBlockY() + "," + high.getBlockZ();
            }
            return new ClaimStatus(true, identity, owner);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError failure) {
            throw new RegistryException("Claim ownership could not be verified. Ask an administrator to check GriefPrevention.", failure);
        }
    }
}

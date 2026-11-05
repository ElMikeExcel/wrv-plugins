package com.winkreyes.excellandmarks.hook;

import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.CreateClaimResult;
import me.ryanhamshire.GriefPrevention.DataStore;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.UUID;
import java.util.logging.Logger;

/**
 * GriefPrevention 16.18.7 hook (SPEC §5). ONLY loaded when the GP plugin
 * is present (guarded Class.forName + plugin check in the main class).
 * No manual claim setup anywhere: the subdivision is created inside
 * whatever claim already covers the sign, at purchase, by API call.
 */
public final class GpHook implements GpAdapter {

    private final Logger log;

    public GpHook(Logger log) {
        this.log = log;
    }

    private DataStore store() {
        GriefPrevention gp = GriefPrevention.instance;
        return gp == null ? null : gp.dataStore;
    }

    @Override
    public boolean available() {
        return store() != null;
    }

    @Override
    public Long createSubdivision(String worldName, int x1, int y1, int z1,
                                  int x2, int y2, int z2, UUID owner) {
        try {
            DataStore ds = store();
            World world = Bukkit.getWorld(worldName);
            if (ds == null || world == null || owner == null) return null;

            Claim parent = ds.getClaimAt(new Location(world, x1, y1, z1), false, null);
            if (parent == null) {
                log.info("ExcelLandmarks: no GriefPrevention claim covers " + worldName
                        + " " + x1 + "," + y1 + "," + z1 + " — plot will be naming-only");
                return null;
            }
            while (parent.parent != null) parent = parent.parent; // top-level claim

            // Clamp the requested region inside the parent claim.
            Location lo = parent.getLesserBoundaryCorner();
            Location hi = parent.getGreaterBoundaryCorner();
            int ax = Math.max(Math.min(x1, x2), lo.getBlockX());
            int bx = Math.min(Math.max(x1, x2), hi.getBlockX());
            int az = Math.max(Math.min(z1, z2), lo.getBlockZ());
            int bz = Math.min(Math.max(z1, z2), hi.getBlockZ());
            if (bx < ax || bz < az) {
                log.info("ExcelLandmarks: plot region falls outside the parent claim — naming-only");
                return null;
            }
            CreateClaimResult res = ds.createClaim(world, ax, bx, Math.min(y1, y2), Math.max(y1, y2),
                    az, bz, owner, parent, null, null);
            if (res == null || !res.succeeded || res.claim == null) {
                log.info("ExcelLandmarks: GP subdivision creation refused — plot will be naming-only");
                return null;
            }
            // GP's model (rig-proven Nov 5, 2026): a subdivision CANNOT
            // change owners — DataStore.changeClaimOwner throws
            // NoTransferException ("Subdivisions can't be transferred").
            // Build rights in GP are trust, not title: the claim stays
            // under the admin claim and the BUYER receives Build
            // permission + manager status on the subdivision, which is
            // exactly what building (and trusting friends) takes. The
            // subdivision is deleted wholesale at finalize.
            String buyer = owner.toString();
            res.claim.setPermission(buyer, me.ryanhamshire.GriefPrevention.ClaimPermission.Build);
            if (!res.claim.managers.contains(buyer)) res.claim.managers.add(buyer);
            ds.saveClaim(res.claim);
            Claim check = ds.getClaim(res.claim.getID());
            if (check == null
                    || check.getPermission(buyer) != me.ryanhamshire.GriefPrevention.ClaimPermission.Build) {
                log.warning("ExcelLandmarks: GP build-rights grant did not take — deleting the subdivision, plot will be naming-only");
                if (check != null) ds.deleteClaim(check);
                return null;
            }
            return res.claim.getID();
        } catch (Throwable t) {
            log.warning("ExcelLandmarks: GP subdivision creation failed (" + t + ") — naming-only");
            return null;
        }
    }

    @Override
    public boolean deleteSubdivision(long claimId) {
        try {
            DataStore ds = store();
            if (ds == null) return false;
            Claim c = ds.getClaim(claimId);
            if (c == null) return true; // already gone — the desired end state
            ds.deleteClaim(c);
            return true;
        } catch (Throwable t) {
            log.warning("ExcelLandmarks: GP subdivision delete failed (" + t + ")");
            return false;
        }
    }
}

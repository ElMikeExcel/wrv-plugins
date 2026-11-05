package rig;

import com.winkreyes.excellandmarks.ExcelLandmarks;
import com.winkreyes.excellandmarks.core.Spot;
import com.winkreyes.excellandmarks.menu.Menus;
import com.winkreyes.excellandmarks.store.Store;

/**
 * GP-touching rig helpers, isolated in their own class so Boot B (no
 * GriefPrevention on the server) never loads them. Called reflectively.
 */
public final class GpRig {

    public static boolean createParentClaim(org.bukkit.World world, java.util.UUID owner) {
        try {
            me.ryanhamshire.GriefPrevention.DataStore ds =
                    me.ryanhamshire.GriefPrevention.GriefPrevention.instance.dataStore;
            me.ryanhamshire.GriefPrevention.CreateClaimResult r = ds.createClaim(world,
                    -60, 200, 0, 200, -60, 200, owner, null, null, null);
            return r.succeeded;
        } catch (Throwable t) {
            System.out.println("GpRig createParentClaim threw: " + t);
            return false;
        }
    }

    /** The buyer's effective permission in the claim ("Build" expected), or null. */
    public static String claimRights(long claimId, java.util.UUID who) {
        try {
            me.ryanhamshire.GriefPrevention.Claim c =
                    me.ryanhamshire.GriefPrevention.GriefPrevention.instance.dataStore.getClaim(claimId);
            if (c == null) return null;
            me.ryanhamshire.GriefPrevention.ClaimPermission p = c.getPermission(who.toString());
            return p == null ? null : p.name();
        } catch (Throwable t) {
            return "THREW:" + t;
        }
    }

    public static boolean claimExists(long claimId) {
        try {
            return me.ryanhamshire.GriefPrevention.GriefPrevention.instance.dataStore.getClaim(claimId) != null;
        } catch (Throwable t) {
            return false;
        }
    }

    // referenced to keep imports honest in the javadoc sense
    @SuppressWarnings("unused")
    private static void touch(ExcelLandmarks lm, Spot s, Store st, Menus m) { }
}

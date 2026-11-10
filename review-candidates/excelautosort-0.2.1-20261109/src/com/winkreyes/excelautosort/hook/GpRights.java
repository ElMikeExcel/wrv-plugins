package com.winkreyes.excelautosort.hook;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * The GriefPrevention port (SPEC §6.4). The implementation that touches
 * GP classes lives in {@link GpRightsHook} and is only instantiated
 * when the GP plugin is present; everything else talks to this
 * interface and can never NoClassDefFound. When GP is absent the
 * standing instance is {@link #UNAVAILABLE} and configure is DENIED —
 * fail closed (§3).
 */
public interface GpRights {

    GpRights UNAVAILABLE = new GpRights() {
        @Override public boolean available() { return false; }
        @Override public boolean canConfigure(Player player, Location loc, UUID recordOwner) { return false; }
        @Override public void setClaimChangeHandler(ClaimChangeHandler handler) {}
    };

    boolean available();

    /**
     * May this player mark/configure a filter chest at this location?
     * Rules (SPEC §6.4): claim owner; PermissionTrust (claim manager);
     * a Landmarks plot renter (manager of the subdivision); the record
     * owner in unclaimed land (anyone may newly mark there);
     * {@code excelautosort.admin} inside admin claims.
     * ContainerTrust / AccessTrust alone NEVER suffice — being allowed
     * to open a chest is not the right to change its settings (Mike's
     * explicit rule, Nov 6, 2026, 11:52 PM CST).
     *
     * @param recordOwner the existing record's owner, or null for a new mark
     */
    boolean canConfigure(Player player, Location loc, UUID recordOwner);

    /** GP pushes claim deletions/modifications here so filters can suspend (§6.4). */
    void setClaimChangeHandler(ClaimChangeHandler handler);

    interface ClaimChangeHandler {
        /** A claim covering this footprint was deleted or modified. */
        void onClaimChanged(String world, int minX, int maxX, int minZ, int maxZ, String reason);
    }
}

// ExcelAutoSort — Iris / Mike, for Wink Reyes Valley

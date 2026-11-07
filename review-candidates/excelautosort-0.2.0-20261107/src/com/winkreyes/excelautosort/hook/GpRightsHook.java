package com.winkreyes.excelautosort.hook;

import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import me.ryanhamshire.GriefPrevention.events.ClaimDeletedEvent;
import me.ryanhamshire.GriefPrevention.events.ClaimModifiedEvent;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

import java.util.UUID;
import java.util.logging.Logger;

/**
 * The GriefPrevention 16.18.7 adapter — the ONLY class in ExcelAutoSort
 * that touches GP classes. Loaded only when the GP plugin is present
 * (guarded in the main class), following the house isolation pattern.
 * All calls are fail-soft: any GP surprise reads as "no rights", which
 * is the closed side (§3 fail-safe law).
 */
public final class GpRightsHook implements GpRights, Listener {

    private final Logger log;
    private volatile ClaimChangeHandler handler;

    public GpRightsHook(Plugin host, Logger log) {
        this.log = log;
        host.getServer().getPluginManager().registerEvents(this, host);
    }

    @Override
    public boolean available() {
        try {
            return GriefPrevention.instance != null && GriefPrevention.instance.dataStore != null;
        } catch (Throwable t) {
            return false;
        }
    }

    @Override
    public boolean canConfigure(Player player, Location loc, UUID recordOwner) {
        try {
            if (!available() || player == null || loc == null) return false;
            Claim claim = GriefPrevention.instance.dataStore.getClaimAt(loc, true, null);
            if (claim == null) {
                // Unclaimed land: a new mark is open to anyone (they become
                // the owner); an existing record belongs to its owner.
                return recordOwner == null || recordOwner.equals(player.getUniqueId());
            }
            if (claim.isAdminClaim()) {
                return player.hasPermission("excelautosort.admin");
            }
            UUID me = player.getUniqueId();
            if (me.equals(claim.getOwnerID())) return true;
            // PermissionTrust == claim manager. A Landmarks plot renter is
            // the manager of the plot subdivision, so this one check also
            // carries the renter rule (the admin-claim owner keeps the
            // admin override above).
            return claim.managers != null && claim.managers.contains(me.toString());
        } catch (Throwable t) {
            log.warning("ExcelAutoSort: GP rights check failed (" + t + ") — configure denied");
            return false;
        }
    }

    @Override
    public void setClaimChangeHandler(ClaimChangeHandler handler) {
        this.handler = handler;
    }

    // ------------------------------------------------------------ GP events

    @EventHandler
    public void onClaimDeleted(ClaimDeletedEvent e) {
        push(e.getClaim(), "claim-deleted");
    }

    @EventHandler
    public void onClaimModified(ClaimModifiedEvent e) {
        push(e.getClaim(), "claim-changed");
    }

    private void push(Claim claim, String reason) {
        ClaimChangeHandler h = handler;
        if (h == null || claim == null) return;
        try {
            Location lo = claim.getLesserBoundaryCorner();
            Location hi = claim.getGreaterBoundaryCorner();
            if (lo == null || hi == null || lo.getWorld() == null) return;
            h.onClaimChanged(lo.getWorld().getName(),
                    Math.min(lo.getBlockX(), hi.getBlockX()), Math.max(lo.getBlockX(), hi.getBlockX()),
                    Math.min(lo.getBlockZ(), hi.getBlockZ()), Math.max(lo.getBlockZ(), hi.getBlockZ()),
                    reason);
        } catch (Throwable t) {
            log.warning("ExcelAutoSort: claim-change push failed (" + t + ")");
        }
    }
}

// ExcelAutoSort — Iris / Mike, for Wink Reyes Valley

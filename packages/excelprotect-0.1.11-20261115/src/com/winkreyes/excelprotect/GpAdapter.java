package com.winkreyes.excelprotect;

import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import me.ryanhamshire.GriefPrevention.events.ClaimChangeEvent;
import me.ryanhamshire.GriefPrevention.events.ClaimCreatedEvent;
import me.ryanhamshire.GriefPrevention.events.ClaimDeletedEvent;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

/**
 * ExcelProtect — GriefPrevention adapter (fail-soft, SPEC §1/§3.11): claim-owner
 * stamping for located events + claim create/delete/change events. This class is
 * only loaded when GriefPrevention is enabled; without GP the plugin runs fully
 * standalone. Excel — Wink Reyes Valley
 */
public final class GpAdapter implements Listener {
    private final Ledger ledger;
    private final Settings cfg;

    public GpAdapter(Ledger ledger, Settings cfg) { this.ledger = ledger; this.cfg = cfg; }

    public static String ownerAt(Location loc) {
        try {
            Claim c = GriefPrevention.instance.dataStore.getClaimAt(loc, true, null);
            return c == null ? null : c.getOwnerName();
        } catch (Throwable t) { return null; }
    }

    private Json.Obj claimObj(String type, Claim c) {
        Json.Obj o = new Json.Obj().put("type", type)
                .put("owner", c.getOwnerName())
                .put("owner_uuid", c.getOwnerID() == null ? null : c.getOwnerID().toString())
                .put("area", c.getArea());
        try {
            Location lo = c.getLesserBoundaryCorner(), hi = c.getGreaterBoundaryCorner();
            o.put("lesser", lo.getBlockX() + "," + lo.getBlockY() + "," + lo.getBlockZ())
             .put("greater", hi.getBlockX() + "," + hi.getBlockY() + "," + hi.getBlockZ());
        } catch (Throwable ignored) { }
        return o;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCreate(ClaimCreatedEvent e) {
        if (cfg.claimEvents()) ledger.emit("system", e.getClaim().getLesserBoundaryCorner(), claimObj("claim_created", e.getClaim()));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDelete(ClaimDeletedEvent e) {
        if (cfg.claimEvents()) ledger.emit("system", e.getClaim().getLesserBoundaryCorner(), claimObj("claim_deleted", e.getClaim()));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChange(ClaimChangeEvent e) {
        if (cfg.claimEvents()) ledger.emit("system", e.getTo().getLesserBoundaryCorner(), claimObj("claim_changed", e.getTo()));
    }
}

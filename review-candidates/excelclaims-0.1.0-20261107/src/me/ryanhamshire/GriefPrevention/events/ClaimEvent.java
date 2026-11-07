package me.ryanhamshire.GriefPrevention.events;

import me.ryanhamshire.GriefPrevention.Claim;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * ExcelClaims facade — base claim event, API-shaped like the public
 * GriefPrevention event type it stands in for. ExcelProtect and
 * ExcelAuditLog listen to the concrete subclasses.
 * Part of ExcelClaims (Iris / Mike).
 */
public class ClaimEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Claim claim;

    public ClaimEvent(Claim claim) {
        this.claim = claim;
    }

    public Claim getClaim() { return claim; }

    @Override public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}

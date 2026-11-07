package me.ryanhamshire.GriefPrevention.events;

import me.ryanhamshire.GriefPrevention.Claim;
import org.bukkit.event.HandlerList;

/**
 * ExcelClaims facade — fired when a claim is deleted. API-shaped
 * after the public GriefPrevention event. ExcelAutoSort listens to
 * this to suspend chest filters under deleted claims.
 * Part of ExcelClaims (Iris / Mike).
 */
public class ClaimDeletedEvent extends ClaimEvent {
    private static final HandlerList HANDLERS = new HandlerList();

    public ClaimDeletedEvent(Claim claim) {
        super(claim);
    }

    @Override public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}

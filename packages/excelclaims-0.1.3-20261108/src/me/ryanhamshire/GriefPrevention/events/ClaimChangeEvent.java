package me.ryanhamshire.GriefPrevention.events;

import me.ryanhamshire.GriefPrevention.Claim;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * ExcelClaims facade — fired when a claim's shape changes, carrying
 * the before/after claims; cancellable. API-shaped after the public
 * GriefPrevention event (ExcelProtect and ExcelAuditLog listen).
 * Part of ExcelClaims (Iris / Mike).
 */
public class ClaimChangeEvent extends Event implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Claim from;
    private final Claim to;
    private boolean cancelled;

    public ClaimChangeEvent(Claim from, Claim to) {
        this.from = from;
        this.to = to;
    }

    public Claim getFrom() { return from; }
    public Claim getTo() { return to; }

    @Override public boolean isCancelled() { return cancelled; }
    @Override public void setCancelled(boolean cancel) { this.cancelled = cancel; }
    @Override public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}

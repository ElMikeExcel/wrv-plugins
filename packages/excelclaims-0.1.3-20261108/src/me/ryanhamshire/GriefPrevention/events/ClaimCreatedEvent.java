package me.ryanhamshire.GriefPrevention.events;

import me.ryanhamshire.GriefPrevention.Claim;
import org.bukkit.command.CommandSender;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;

/**
 * ExcelClaims facade — fired before a claim is committed; cancelling
 * it refuses the creation. API-shaped after the public GriefPrevention
 * event. Part of ExcelClaims (Iris / Mike).
 */
public class ClaimCreatedEvent extends ClaimEvent implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();
    private final CommandSender creator;
    private boolean cancelled;

    public ClaimCreatedEvent(Claim claim, CommandSender creator) {
        super(claim);
        this.creator = creator;
    }

    public CommandSender getCreator() { return creator; }

    @Override public boolean isCancelled() { return cancelled; }
    @Override public void setCancelled(boolean cancel) { this.cancelled = cancel; }
    @Override public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}

package me.ryanhamshire.GriefPrevention.events;

import me.ryanhamshire.GriefPrevention.Claim;
import org.bukkit.command.CommandSender;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;

/**
 * ExcelClaims facade — base event for claim modifications (resizes
 * and trust changes), carrying the before/after claims and the actor.
 * API-shaped after the public GriefPrevention event family.
 * Part of ExcelClaims (Iris / Mike).
 */
public class ClaimResizeEvent extends ClaimEvent implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Claim from;
    private final CommandSender creator;
    private boolean cancelled;

    public ClaimResizeEvent(Claim from, Claim to, CommandSender creator) {
        super(to);
        this.from = from;
        this.creator = creator;
    }

    public Claim getFrom() { return from; }
    public CommandSender getCreator() { return creator; }

    @Override public boolean isCancelled() { return cancelled; }
    @Override public void setCancelled(boolean cancel) { this.cancelled = cancel; }
    @Override public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}

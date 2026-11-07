package me.ryanhamshire.GriefPrevention.events;

import me.ryanhamshire.GriefPrevention.Claim;
import org.bukkit.command.CommandSender;
import org.bukkit.event.HandlerList;

/**
 * ExcelClaims facade — fired after a claim is modified (resize or
 * trust change). API-shaped after the public GriefPrevention event;
 * ExcelAutoSort listens to suspend chest filters under changed claims.
 * Part of ExcelClaims (Iris / Mike).
 */
public class ClaimModifiedEvent extends ClaimResizeEvent {
    private static final HandlerList HANDLERS = new HandlerList();

    public ClaimModifiedEvent(Claim from, Claim to, CommandSender creator) {
        super(from, to, creator);
    }

    @Override public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}

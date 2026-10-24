package com.winkreyes.trash;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/** Fired after a Trash screen opens. ExcelTips listens for this (trigger type "event") for the first-use tip. */
public final class TrashOpenEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Player player;
    private final String mode;
    public TrashOpenEvent(Player player, String mode) { this.player = player; this.mode = mode; }
    public Player getPlayer() { return player; }
    /** "inventory" or "chest" */
    public String getMode() { return mode; }
    @Override public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}

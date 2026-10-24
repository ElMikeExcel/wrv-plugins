package com.winkreyes.exceltips;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.UUID;

/**
 * Fired (main thread) when a player says OK to a tip for the first time:
 * the [OK] click, the Bedrock pop-up's OK button, /tips ok, an "ok" chat reply,
 * or ExcelTipsAPI.acknowledge. Plugins with their own fallback tip can listen
 * and mark their copy done too. Not fired for admin resets.
 */
public final class ExcelTipsAckEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final UUID player;
    private final String feature;
    private final String source;

    public ExcelTipsAckEvent(UUID player, String feature, String source) { this.player = player; this.feature = feature; this.source = source; }

    public UUID getPlayerId() { return player; }
    /** Tip name, lower case, as in tips.yml. */
    public String getFeature() { return feature; }
    /** "chat" ([OK] click, /tips ok, "ok" reply), "popup" (Bedrock OK button) or "api". */
    public String getSource() { return source; }

    @Override public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}

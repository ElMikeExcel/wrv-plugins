package com.winkreyes.excelprotect.api;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.Map;

/**
 * ExcelProtect PUBLISH API (SPEC Amendment 3.7): any plugin fires this event to
 * write its own record into ExcelProtect's streams. stream: one of blocks,
 * containers, entities, items, players, economy, audit, combat, deaths, system,
 * snapshots, admin. fields become the event body (a "type" field is required).
 * If ExcelProtect is absent the event simply has no listeners.
 * Excel — Wink Reyes Valley
 */
public class RecordEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final String stream;
    private final Map<String, Object> fields;

    public RecordEvent(String stream, Map<String, Object> fields) {
        super(false); // sync event: emitters fire on the main thread (verdicts, casts, trades)
        this.stream = stream; this.fields = fields;
    }

    public String stream() { return stream; }
    public Map<String, Object> fields() { return fields; }

    @Override public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}

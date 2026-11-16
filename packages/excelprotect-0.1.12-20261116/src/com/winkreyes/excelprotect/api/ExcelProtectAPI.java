package com.winkreyes.excelprotect.api;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.util.Map;

/**
 * ExcelProtect PUBLISH API — the static emit door (SPEC Amendment 3.7). Suite
 * plugins call publish() to record their own facts in the unified streams
 * (BossTrial's combat publish and Together's party-repair casts ride this door
 * via their dedicated events; this is the general one). Fire-and-forget: if
 * ExcelProtect is not installed, the call is a no-op. Thread-safe: the record
 * events are SYNC Bukkit events (emitters fire on the main thread); calls made
 * off-thread are routed through the scheduler by the plugin reference set in
 * init(). Excel — Wink Reyes Valley
 */
public final class ExcelProtectAPI {
    private static volatile Plugin host;

    private ExcelProtectAPI() { }

    /** Called by ExcelProtect at enable. Other plugins never call this. */
    public static void init(Plugin plugin) { host = plugin; }

    public static void publish(String stream, String type, Map<String, Object> fields) {
        java.util.Map<String, Object> body = new java.util.LinkedHashMap<>(fields);
        body.put("type", type);
        fire(new RecordEvent(stream, body));
    }

    public static void publish(String stream, Map<String, Object> fieldsWithType) {
        fire(new RecordEvent(stream, fieldsWithType));
    }

    /**
     * 0.1.10 (P-PROTECT-WATCH-1): fire a gameplay fact into the audit watch —
     * the per-player audit log under plugins/ExcelProtect/audit/. kind +
     * expected fields are the GameplayEvent contract; fields MUST carry
     * player + player_uuid. Fire-and-forget exactly like publish().
     */
    public static void gameplay(String kind, Map<String, Object> fields) {
        fire(new GameplayEvent(kind, fields));
    }

    private static void fire(org.bukkit.event.Event ev) {
        if (Bukkit.isPrimaryThread()) {
            Bukkit.getPluginManager().callEvent(ev);
            return;
        }
        Plugin h = host;
        if (h != null && h.isEnabled()) {
            Bukkit.getScheduler().runTask(h, () -> Bukkit.getPluginManager().callEvent(ev));
        }
        // no host (Protect absent): the record is dropped by design — fire-and-forget
    }
}

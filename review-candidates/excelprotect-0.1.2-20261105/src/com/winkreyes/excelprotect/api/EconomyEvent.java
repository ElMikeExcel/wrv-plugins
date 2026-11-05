package com.winkreyes.excelprotect.api;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.Map;

/**
 * ExcelProtect integration contract (SPEC §4/§3.8): emitter plugins (ExcelShops,
 * ExcelRewardGuard, services) fire this event to record an economy fact in the
 * unified economy stream. kind: shop_purchase | reward_adjustment | service_charge |
 * service_cast. fields carry the emitter's native record (for shop_purchase: the
 * full 12-field Shops shape incl. balance_after; for reward_adjustment: origin,
 * channel, base, rate, paid — including blocked adjustments at paid=0).
 * Excel — Wink Reyes Valley
 */
public class EconomyEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final String kind;
    private final Map<String, Object> fields;

    public EconomyEvent(String kind, Map<String, Object> fields) {
        super(false); // sync event: emitters fire on the main thread (verdicts, casts, trades)
        this.kind = kind; this.fields = fields;
    }

    public String kind() { return kind; }
    public Map<String, Object> fields() { return fields; }

    @Override public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}

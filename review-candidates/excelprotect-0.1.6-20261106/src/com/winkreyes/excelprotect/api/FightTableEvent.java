package com.winkreyes.excelprotect.api;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.List;
import java.util.Map;

/**
 * ExcelProtect integration contract (SPEC §4): a boss/seal plugin (ExcelBossTrial)
 * fires this event at verdict with its FULL per-player fight table; ExcelProtect
 * records it as the encounter's authoritative fight_summary + seal_verdict. Firing
 * it requires nothing of the emitter beyond this class on the classpath, and if
 * ExcelProtect is absent the event simply has no listeners. Excel — Wink Reyes Valley
 *
 * rows: one map per player with keys name, uuid, damage_dealt, damage_taken,
 * healing_done, healing_received (numbers).
 */
public class FightTableEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final String encounter;
    private final String verdict;
    private final String credit;
    private final double pool;
    private final double durationSeconds;
    private final List<Map<String, Object>> rows;

    public FightTableEvent(String encounter, String verdict, String credit, double pool,
                           double durationSeconds, List<Map<String, Object>> rows) {
        super(false); // sync event: emitters fire on the main thread (verdicts, casts, trades)
        this.encounter = encounter; this.verdict = verdict; this.credit = credit;
        this.pool = pool; this.durationSeconds = durationSeconds; this.rows = rows;
    }

    public String encounter() { return encounter; }
    public String verdict() { return verdict; }
    public String credit() { return credit; }
    public double pool() { return pool; }
    public double durationSeconds() { return durationSeconds; }
    public List<Map<String, Object>> rows() { return rows; }

    @Override public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}

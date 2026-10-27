package com.winkreyes.exceltelemetry.core;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Per-victim damage ledger: who hurt this mob, with what kinds of
 * damage, since the first recorded hit. Accumulated from damage
 * events on the main thread; read out when the victim dies (kill
 * line) or pruned when it goes stale.
 *
 * Invariant the tests pin: total == sum(byAttacker) == sum(byCause).
 */
public final class DamageLedger {
    private final long firstSeenMs;
    private long lastTouchMs;
    private double total;
    private final Map<String, Double> byAttacker = new LinkedHashMap<>();
    private final Map<String, Double> byCause = new LinkedHashMap<>();

    public DamageLedger(long nowMs) {
        this.firstSeenMs = nowMs;
        this.lastTouchMs = nowMs;
    }

    public synchronized void record(String attackerKey, String cause, double amount, long nowMs) {
        if (amount <= 0 || Double.isNaN(amount)) return;
        total += amount;
        byAttacker.merge(attackerKey, amount, Double::sum);
        byCause.merge(cause, amount, Double::sum);
        lastTouchMs = nowMs;
    }

    public synchronized double total() { return total; }
    public long firstSeenMs() { return firstSeenMs; }
    public synchronized long lastTouchMs() { return lastTouchMs; }

    public synchronized double shareOf(String attackerKey) {
        if (total <= 0) return 0;
        return byAttacker.getOrDefault(attackerKey, 0.0) / total;
    }

    public synchronized double causeTotal(String cause) {
        return byCause.getOrDefault(cause, 0.0);
    }

    /** Share of total damage dealt by explosions (block + entity). */
    public synchronized double explosionShare() {
        if (total <= 0) return 0;
        double ex = byCause.getOrDefault("BLOCK_EXPLOSION", 0.0)
                + byCause.getOrDefault("ENTITY_EXPLOSION", 0.0);
        return ex / total;
    }

    /** Share of total damage from falling (RewardGuard's sky-farm signal). */
    public synchronized double fallShare() {
        if (total <= 0) return 0;
        return byCause.getOrDefault("FALL", 0.0) / total;
    }

    /** Attacker key with the highest damage share, or null when empty. */
    public synchronized String topAttacker() {
        String best = null;
        double bestVal = -1;
        for (Map.Entry<String, Double> e : byAttacker.entrySet()) {
            if (e.getValue() > bestVal) { bestVal = e.getValue(); best = e.getKey(); }
        }
        return best;
    }

    public synchronized boolean hasPlayerAttacker() {
        for (String k : byAttacker.keySet()) {
            if (k.startsWith("player:")) return true;
        }
        return false;
    }

    public synchronized Map<String, Object> snapshot() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("total", Json.round1(total));
        Map<String, Object> ba = new LinkedHashMap<>();
        byAttacker.forEach((k, v) -> ba.put(k, Json.round1(v)));
        m.put("by_attacker", ba);
        Map<String, Object> bc = new LinkedHashMap<>();
        byCause.forEach((k, v) -> bc.put(k, Json.round1(v)));
        m.put("by_cause", bc);
        m.put("fall_share", Json.round1(fallShare() * 1000) / 1000.0);
        m.put("explosion_share", Json.round1(explosionShare() * 1000) / 1000.0);
        return m;
    }
}

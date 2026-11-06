package com.winkreyes.excelprotect;

import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Excel — per-victim damage accounting between first hit and death (0.3.0), mirroring the
 * ExcelTelemetry kill schema that solved the Oct 31 armor-stand case: by_attacker keys are
 * "player:&lt;uuid&gt;", "entity:&lt;type&gt;:&lt;uuid&gt;" or "env:&lt;cause&gt;"; by_cause keys are
 * Bukkit DamageCause names. Pure logic on plain values; unit-tested.
 */
public final class DamageLedger {
    public static final class Book {
        public final Map<String, Double> byAttacker = new TreeMap<>();
        public final Map<String, Double> byCause = new TreeMap<>();
        public long firstMs, lastMs;
        public double total;
        public String lastAttackerKey, lastKind, lastUuid, lastLabel, lastCause;

        void record(long ms, String attackerKey, String kind, String uuid, String label, String cause, double dmg) {
            if (firstMs == 0) firstMs = ms;
            lastMs = ms;
            total += dmg;
            byAttacker.merge(attackerKey, dmg, Double::sum);
            byCause.merge(cause, dmg, Double::sum);
            lastAttackerKey = attackerKey; lastKind = kind; lastUuid = uuid; lastLabel = label; lastCause = cause;
        }

        public double fightSeconds() {
            return firstMs == 0 ? 0 : (lastMs - firstMs) / 1000.0;
        }

        public Json.Obj damageJson() {
            Json.Obj byA = new Json.Obj(); byAttacker.forEach((k, v) -> byA.put(k, round(v)));
            Json.Obj byC = new Json.Obj(); byCause.forEach((k, v) -> byC.put(k, round(v)));
            return new Json.Obj().put("total", round(total)).put("by_attacker", byA).put("by_cause", byC);
        }

        static double round(double d) { return Math.round(d * 10.0) / 10.0; }
    }

    private final ConcurrentHashMap<UUID, Book> books = new ConcurrentHashMap<>();
    private final int cap;

    public DamageLedger(int cap) { this.cap = Math.max(100, cap); }

    public void record(UUID victim, long ms, String attackerKey, String kind, String uuid, String label, String cause, double dmg) {
        if (books.size() > cap) prune(ms - 60_000L);
        books.computeIfAbsent(victim, k -> new Book()).record(ms, attackerKey, kind, uuid, label, cause, dmg);
    }

    /** Remove and return the victim's book at death (null when the death had no recorded damage). */
    public Book take(UUID victim) { return books.remove(victim); }

    public void forget(UUID victim) { books.remove(victim); }

    /** Drop books whose last damage is older than cutoffMs (despawned entities never die on record). */
    public void prune(long cutoffMs) {
        books.entrySet().removeIf(e -> e.getValue().lastMs < cutoffMs);
    }

    public int size() { return books.size(); }
}

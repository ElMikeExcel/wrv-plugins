package com.winkreyes.exceltelemetry;

import com.winkreyes.exceltelemetry.core.DamageLedger;
import com.winkreyes.exceltelemetry.core.LedgerStore;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Ledger accounting: total must equal the sum of both breakdowns. */
public class DamageLedgerTest {
    static int fails = 0;
    static void check(boolean c, String m) {
        if (!c) { fails++; System.out.println("FAIL " + m); } else System.out.println("ok   " + m);
    }
    static void eq(double a, double b, String m) { check(Math.abs(a - b) < 0.001, m + " (" + a + " vs " + b + ")"); }

    public static void main(String[] args) {
        DamageLedger led = new DamageLedger(1_000);
        led.record("player:aaa", "ENTITY_ATTACK", 10, 1_100);
        led.record("player:aaa", "ENTITY_ATTACK", 5, 1_200);
        led.record("player:bbb", "PROJECTILE", 7.5, 1_300);
        led.record("env:fall", "FALL", 20, 1_400);
        led.record("env:explosion", "ENTITY_EXPLOSION", 30, 1_500);
        led.record("env:explosion", "BLOCK_EXPLOSION", 10, 1_600);

        eq(led.total(), 82.5, "total sums every hit");
        Map<String, Object> snap = led.snapshot();
        @SuppressWarnings("unchecked") Map<String, Object> ba = (Map<String, Object>) snap.get("by_attacker");
        @SuppressWarnings("unchecked") Map<String, Object> bc = (Map<String, Object>) snap.get("by_cause");
        double sumA = ba.values().stream().mapToDouble(v -> ((Number) v).doubleValue()).sum();
        double sumC = bc.values().stream().mapToDouble(v -> ((Number) v).doubleValue()).sum();
        eq(sumA, 82.5, "by_attacker sums to the kill total");
        eq(sumC, 82.5, "by_cause sums to the kill total");
        eq(led.shareOf("player:aaa"), 15 / 82.5, "attacker share");
        check("env:explosion".equals(led.topAttacker()), "top attacker is the explosion source");
        check(led.hasPlayerAttacker(), "player involvement detected");
        eq(led.explosionShare(), 40 / 82.5, "explosion share pools block + entity explosions");
        eq(led.fallShare(), 20 / 82.5, "fall share");
        check(led.firstSeenMs() == 1_000 && led.lastTouchMs() == 1_600, "timestamps tracked");

        DamageLedger empty = new DamageLedger(0);
        check(empty.topAttacker() == null && !empty.hasPlayerAttacker(), "empty ledger is inert");
        eq(empty.explosionShare(), 0, "empty explosion share is 0, not NaN");
        empty.record("player:x", "ENTITY_ATTACK", -5, 10);
        empty.record("player:x", "ENTITY_ATTACK", Double.NaN, 10);
        eq(empty.total(), 0, "non-positive and NaN damage are refused");

        // LedgerStore pruning: dead victims and TTL both bound growth.
        LedgerStore store = new LedgerStore(10_000);
        UUID a = UUID.randomUUID(), b = UUID.randomUUID(), c = UUID.randomUUID();
        store.getOrCreate(a, 0).record("player:p", "ENTITY_ATTACK", 1, 0);
        store.getOrCreate(b, 0).record("player:p", "ENTITY_ATTACK", 1, 0);
        store.getOrCreate(c, 0).record("player:p", "ENTITY_ATTACK", 1, 9_500);
        int dropped = store.prune(Set.of(a, c), 10_001); // b is dead; a is stale (age > ttl); c alive+fresh
        check(dropped == 2 && store.size() == 1 && store.get(c) != null,
                "prune drops dead victims and stale entries, keeps live fights");
        check(store.remove(c) != null && store.size() == 0, "remove on death");

        System.out.println(fails == 0 ? "ALL PASS" : "FAILED " + fails);
        if (fails > 0) System.exit(1);
    }
}

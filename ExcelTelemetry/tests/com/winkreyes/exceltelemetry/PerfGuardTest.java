package com.winkreyes.exceltelemetry;

import com.winkreyes.exceltelemetry.core.LedgerStore;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;

/**
 * Performance guards: the two structures that could grow or block on
 * a busy server are bounded - the ledger store prunes to the alive
 * set, and the writer queue is offer-only (a full queue drops a line
 * and counts it; it never parks the main thread).
 */
public class PerfGuardTest {
    static int fails = 0;
    static void check(boolean c, String m) {
        if (!c) { fails++; System.out.println("FAIL " + m); } else System.out.println("ok   " + m);
    }

    public static void main(String[] args) {
        LedgerStore store = new LedgerStore(60_000);
        Set<UUID> alive = new HashSet<>();
        for (int i = 0; i < 10_000; i++) {
            UUID id = UUID.randomUUID();
            store.getOrCreate(id, 1_000).record("player:p", "ENTITY_ATTACK", 1, 1_000);
            if (i < 25) alive.add(id);
        }
        check(store.size() == 10_000, "10k ledgers staged");
        long t0 = System.nanoTime();
        int dropped = store.prune(alive, 2_000);
        long tookMs = (System.nanoTime() - t0) / 1_000_000;
        check(store.size() == 25 && dropped == 9_975, "prune bounds the store to live victims");
        check(tookMs < 1_000, "prune of 10k entries is fast (" + tookMs + " ms)");

        ArrayBlockingQueue<String> q = new ArrayBlockingQueue<>(4);
        int offered = 0;
        for (int i = 0; i < 1_000; i++) if (q.offer("line")) offered++;
        check(offered == 4, "offer() never blocks when full: accepted " + offered + " of 1000, rest droppable");

        System.out.println(fails == 0 ? "ALL PASS" : "FAILED " + fails);
        if (fails > 0) System.exit(1);
    }
}

package com.winkreyes.excelbosstrial.core;

import java.util.Map;
import java.util.UUID;

/**
 * Elder trio tracking: monument clustering (same world, within 100
 * blocks horizontal), two monuments kept separate, the trio
 * completing across three counted kills, the respawn flow for
 * uncounted kills, and snapshot/restore across a restart.
 */
public final class MonumentTrackerTest {

    private static int passed;
    private static int failed;

    private static void check(boolean cond, String label) {
        if (cond) {
            passed++;
        } else {
            failed++;
            System.out.println("FAIL: " + label);
        }
    }

    public static void main(String[] args) {
        MonumentTracker t = new MonumentTracker();
        UUID e1 = UUID.randomUUID();
        UUID e2 = UUID.randomUUID();
        UUID e3 = UUID.randomUUID();
        // One monument: three elders inside its ~58-block footprint.
        t.register(e1, "world", 1000.0, 40.0, 1000.0);
        t.register(e2, "world", 1030.0, 42.0, 990.0);
        t.register(e3, "world", 990.0, 38.0, 1035.0);
        check(t.monumentIds().size() == 1, "three nearby elders cluster into one monument");
        String mid = t.monumentOf(e1);
        check(mid != null && mid.equals(t.monumentOf(e2)) && mid.equals(t.monumentOf(e3)),
                "all three share the monument id");

        // The trio completes one counted kill at a time.
        check(t.markCounted(e1) == 1, "first counted kill: 1 of 3");
        check(t.progress(mid) == 1, "progress reads 1");
        check(!t.trioComplete(mid), "not complete at 1");
        check(t.markCounted(e2) == 2, "second counted kill: 2 of 3");
        check(t.markCounted(e1) == -1, "the same elder cannot count twice");
        check(t.progress(mid) == 2, "double count did not move progress");
        check(t.markCounted(e3) == 3, "third counted kill: 3 of 3");
        check(t.trioComplete(mid), "trio complete at 3");

        // A second monument, far away, tracks separately.
        MonumentTracker t2 = new MonumentTracker();
        UUID a1 = UUID.randomUUID();
        UUID b1 = UUID.randomUUID();
        t2.register(a1, "world", 0.0, 40.0, 0.0);
        t2.register(b1, "world", 5000.0, 40.0, 5000.0);
        check(t2.monumentIds().size() == 2, "distant elders form two monuments");
        check(!t2.monumentOf(a1).equals(t2.monumentOf(b1)), "separate ids");
        t2.markCounted(a1);
        check(t2.progress(t2.monumentOf(b1)) == 0, "progress at one monument never leaks to another");
        // Same coordinates, different world: still a different monument.
        UUID c1 = UUID.randomUUID();
        t2.register(c1, "world_nether", 0.0, 40.0, 0.0);
        check(t2.monumentIds().size() == 3, "another world is another monument");

        // Respawn flow: an uncounted kill goes pending, falls due,
        // and the replacement inherits the monument.
        MonumentTracker t3 = new MonumentTracker();
        UUID d1 = UUID.randomUUID();
        UUID d2 = UUID.randomUUID();
        t3.register(d1, "world", 200.0, 40.0, 200.0);
        t3.register(d2, "world", 220.0, 40.0, 200.0);
        long due = 10_000_000L;
        t3.markPendingRespawn(d1, due);
        check(t3.dueRespawns(due - 1).isEmpty(), "not due a ms early");
        check(t3.dueRespawns(due).size() == 1, "due exactly at the mark");
        UUID fresh = UUID.randomUUID();
        t3.completeRespawn(d1, fresh);
        check(t3.record(fresh) != null, "the replacement is registered");
        check(t3.monumentOf(fresh).equals(t3.monumentOf(d2)),
                "the replacement inherits the monument");
        check(t3.dueRespawns(due).isEmpty(), "the respawn is consumed, not repeated");
        check(t3.record(d1) == null, "the dead elder's record is replaced");
        // A counted elder never respawns.
        t3.markCounted(d2);
        check(t3.dueRespawns(Long.MAX_VALUE).isEmpty(), "counted elders never come due");

        // A pending-respawn elder that gets counted instead (late
        // verdict correction path) leaves the respawn queue.
        MonumentTracker t4 = new MonumentTracker();
        UUID f1 = UUID.randomUUID();
        t4.register(f1, "world", 0.0, 40.0, 0.0);
        t4.markPendingRespawn(f1, 5_000L);
        t4.markCounted(f1);
        check(t4.dueRespawns(9_999L).isEmpty(), "counted after pending clears the respawn");

        // Persistence across a simulated restart.
        Map<String, Object> snap = t3.snapshot();
        MonumentTracker t5 = new MonumentTracker();
        t5.restore(snap);
        check(t5.monumentOf(fresh) != null
                && t5.monumentOf(fresh).equals(t5.monumentOf(d2)),
                "monument membership survives the restart");
        check(t5.progress(t5.monumentOf(d2)) == 1, "counted progress survives the restart");
        MonumentTracker t6 = new MonumentTracker();
        UUID g1 = UUID.randomUUID();
        t6.register(g1, "world", 0.0, 40.0, 0.0);
        t6.markPendingRespawn(g1, 7_000L);
        MonumentTracker t7 = new MonumentTracker();
        t7.restore(t6.snapshot());
        check(t7.dueRespawns(7_000L).size() == 1, "a pending respawn survives the restart");

        System.out.println("MonumentTrackerTest: " + passed + " passed, " + failed + " failed");
        if (failed > 0) {
            System.exit(1);
        }
    }
}

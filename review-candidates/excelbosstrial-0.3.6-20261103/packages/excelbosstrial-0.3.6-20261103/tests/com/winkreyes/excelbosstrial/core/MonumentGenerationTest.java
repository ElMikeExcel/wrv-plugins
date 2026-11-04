/* Excel WRV maintenance; existing authorship retained. */ package com.winkreyes.excelbosstrial.core;

import java.util.UUID;

/**
 * Elder generations (0.3.0, SPEC 2.4/8): in 0.2.11 a COUNTED
 * elder NEVER respawned, which would have starved every repeat
 * elder fight. The generation flip moves a completed trio from
 * COUNTED to PENDING_RESPAWN so fresh elders surface, and the
 * next trio counts from zero.
 */
public final class MonumentGenerationTest {

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
        MonumentTracker tracker = new MonumentTracker();
        UUID e1 = UUID.randomUUID();
        UUID e2 = UUID.randomUUID();
        UUID e3 = UUID.randomUUID();
        // Three elders around one spot form one monument trio.
        tracker.register(e1, "world", 100, 40, 100);
        tracker.register(e2, "world", 104, 40, 100);
        tracker.register(e3, "world", 100, 40, 104);
        String monument = tracker.monumentOf(e1);
        check(monument != null && monument.equals(tracker.monumentOf(e3)),
                "co-located elders share one monument");

        check(tracker.markCounted(e1) == 1, "first elder counts: 1 of 3");
        check(tracker.markCounted(e2) == 2, "second elder counts: 2 of 3");
        check(tracker.monumentsWithCountedTrio().isEmpty(),
                "an incomplete trio is not a counted trio");
        check(tracker.completeTrioRespawn(monument, 999_999L) == 0,
                "flipping an incomplete trio flips nothing");
        check(tracker.markCounted(e3) == 3, "third elder completes the trio");
        check(tracker.monumentsWithCountedTrio().contains(monument),
                "the completed trio reads as counted");

        long due = System.currentTimeMillis() + 6L * 3600 * 1000;
        check(tracker.completeTrioRespawn(monument, due) == 3,
                "the generation flip turns over all three elders");
        check(tracker.monumentsWithCountedTrio().isEmpty(),
                "after the flip the trio no longer reads counted");
        check(tracker.progress(monument) == 0,
                "the next generation starts from zero counted");

        // A fresh elder at the same monument counts toward the
        // NEW generation (the repeat fight can assemble).
        UUID e4 = UUID.randomUUID();
        tracker.register(e4, "world", 102, 40, 102);
        check(tracker.markCounted(e4) == 1,
                "a post-flip elder counts toward the next generation");

        // Snapshot/restore keeps the flipped state (restart-safe).
        MonumentTracker restored = new MonumentTracker();
        restored.restore(tracker.snapshot());
        check(restored.progress(monument) == 1,
                "generation state survives snapshot/restore");

        System.out.println("MonumentGenerationTest: " + passed + " passed, " + failed + " failed");
        if (failed > 0) {
            System.exit(1);
        }
    }
}

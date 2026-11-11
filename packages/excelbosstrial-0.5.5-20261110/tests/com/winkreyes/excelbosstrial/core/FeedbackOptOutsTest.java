/* Excel WRV maintenance; existing authorship retained. */ package com.winkreyes.excelbosstrial.core;

import java.util.List;
import java.util.UUID;

/**
 * The per-player feedback opt-outs (0.3.6): default on for
 * everyone, toggle semantics the command relies on (save only
 * on a change), and the state.yml persistence shape - junk
 * rows degrade to skipped, never a failed load.
 */
public final class FeedbackOptOutsTest {

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
        UUID a = UUID.fromString("00000000-0000-0000-0000-0000000000a1");
        UUID b = UUID.fromString("00000000-0000-0000-0000-0000000000b2");

        FeedbackOptOuts set = new FeedbackOptOuts();
        check(!set.isOptedOut(a), "default: nobody is opted out");
        check(!set.isOptedOut(null), "a null player reads as not opted out");
        check(set.setOptedOut(a, true), "opting out reports a change");
        check(set.isOptedOut(a), "opted out after set");
        check(!set.setOptedOut(a, true), "re-setting the same state reports no change");
        check(!set.isOptedOut(b), "another player is unaffected");
        check(set.setOptedOut(a, false), "opting back in reports a change");
        check(!set.isOptedOut(a), "opted back in");
        check(!set.setOptedOut(null, true), "a null toggle changes nothing");

        // Persistence round-trip through the string-list shape.
        set.setOptedOut(a, true);
        set.setOptedOut(b, true);
        List<String> snap = set.snapshot();
        check(snap.size() == 2 && snap.contains(a.toString()) && snap.contains(b.toString()),
                "snapshot carries both uuids as strings");
        FeedbackOptOuts restored = new FeedbackOptOuts();
        restored.restore(snap);
        check(restored.isOptedOut(a) && restored.isOptedOut(b) && restored.size() == 2,
                "restore round-trips the set");

        // Junk degrades to skipped; restore replaces wholesale.
        restored.restore(List.of("not-a-uuid", "", a.toString()));
        check(restored.size() == 1 && restored.isOptedOut(a) && !restored.isOptedOut(b),
                "junk rows are skipped and restore replaces the old set");
        restored.restore(null);
        check(restored.size() == 0, "a null restore clears to the default (nobody out)");
        restored.restore(List.of());
        check(restored.size() == 0, "an empty restore stays empty");

        System.out.println("FeedbackOptOutsTest: " + passed + " passed, " + failed + " failed");
        if (failed > 0) {
            System.exit(1);
        }
    }
}

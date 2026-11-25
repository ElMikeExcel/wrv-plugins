/* Excel WRV maintenance; existing authorship retained. */ package com.winkreyes.excelbosstrial.core;

import java.util.UUID;

/**
 * Per-player first-break extras (0.5.6; Mike, Nov 11, 2026):
 * once ever per player per seal, in any cycle; veterans never
 * re-earn; a cycle boundary never clears the book.
 */
public final class FirstBreakBookTest {

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
        FirstBreakBook book = new FirstBreakBook();
        UUID alice = UUID.randomUUID();
        UUID bob = UUID.randomUUID();
        String warden = BossKind.WARDEN.sealId();
        String elder = BossKind.ELDER_GUARDIAN.sealId();

        check(!book.isPaid(warden, alice), "fresh player unpaid");
        check(book.markPaid(warden, alice), "first mark is the personal first break");
        check(book.isPaid(warden, alice), "marked player reads paid");
        check(!book.markPaid(warden, alice), "second mark is a veteran re-break: no extras");
        check(!book.markPaid(warden, alice), "third mark still no extras");
        // Per-seal: another seal is a separate first.
        check(book.markPaid(elder, alice), "same player's elder first break is fresh");
        // Per-player: another player on the same seal is fresh.
        check(book.markPaid(warden, bob), "teammate's warden first break is fresh");
        check(book.size() == 3, "book holds 3 player-seal pairs");
        check(book.playersFor(warden).size() == 2, "two players hold warden extras");
        check(book.playersFor(elder).size() == 1, "one player holds elder extras");
        check(book.playersFor(BossKind.WITHER.sealId()).isEmpty(), "nobody holds wither extras");

        // A cycle boundary does not touch the book: simulate
        // the full cycle rolling and re-marking.
        SealCycle cycle = new SealCycle(5);
        for (String seal : new String[] {"ender_dragon", "wither", "elder_guardian", "warden", "raid"}) {
            cycle.credit(seal, 1000L);
        }
        check(cycle.cycleNumber() == 2, "cycle rolled to 2");
        check(!book.markPaid(warden, alice), "cycle 2: veteran never re-earns warden extras");
        UUID carol = UUID.randomUUID();
        check(book.markPaid(warden, carol), "cycle 2: a NEW player earns warden extras");

        // Snapshot round-trip + malformed rows degrade.
        FirstBreakBook restored = new FirstBreakBook();
        java.util.List<String> snap = new java.util.ArrayList<>(book.snapshot());
        snap.add("garbage-row");
        snap.add("warden|not-a-uuid");
        restored.restore(snap);
        check(restored.size() == book.size(), "round-trip keeps every valid pair");
        check(restored.isPaid(warden, alice), "round-trip veteran still paid");
        check(restored.isPaid(warden, carol), "round-trip newcomer still paid");
        restored.restore(null);
        check(restored.size() == 0, "null snapshot restores empty");

        System.out.println("FirstBreakBookTest: " + passed + " passed, " + failed + " failed");
        if (failed > 0) {
            System.exit(1);
        }
    }
}

package com.winkreyes.leaderboard;

import com.winkreyes.leaderboard.core.AdjustmentLedger;

import java.util.Map;
import java.util.UUID;

public final class LedgerTests {
    private static int checks = 0;

    private static void eq(Object actual, Object expected, String label) {
        checks++;
        if (!actual.equals(expected)) {
            throw new AssertionError(label + ": expected <" + expected + "> but was <" + actual + ">");
        }
    }

    private static UUID id(int n) {
        return UUID.fromString(String.format("00000000-0000-0000-0000-%012d", n));
    }

    public static void main(String[] args) {
        AdjustmentLedger ledger = new AdjustmentLedger();
        UUID a = id(1);

        ledger.noteSessionStart(a, 1_000);
        eq(ledger.inSession(a), true, "in session after start");
        ledger.noteSessionEnd(a, 1_350);
        eq(ledger.adjustmentFor(a), 350L, "delta folded into adjustment");
        eq(ledger.inSession(a), false, "session closed");

        // Second session accumulates.
        ledger.noteSessionStart(a, 2_000);
        ledger.noteSessionEnd(a, 2_100);
        eq(ledger.adjustmentFor(a), 450L, "adjustments accumulate");

        // Duplicate start keeps the FIRST baseline (no re-baselining mid-session).
        ledger.noteSessionStart(a, 3_000);
        ledger.noteSessionStart(a, 3_500);
        ledger.noteSessionEnd(a, 3_600);
        eq(ledger.adjustmentFor(a), 450L + 600L, "first baseline wins");

        // End without start is ignored.
        ledger.noteSessionEnd(id(2), 999);
        eq(ledger.adjustmentFor(id(2)), 0L, "orphan end ignored");

        // Stat reset mid-session yields zero delta.
        ledger.noteSessionStart(id(3), 500);
        ledger.noteSessionEnd(id(3), 100);
        eq(ledger.adjustmentFor(id(3)), 0L, "reset yields zero");

        // Round trip through the persistence format.
        String text = ledger.serialize();
        AdjustmentLedger restored = AdjustmentLedger.deserialize(text);
        eq(restored.adjustmentsSnapshot(), ledger.adjustmentsSnapshot(), "round trip");

        // Corrupt lines are skipped, good lines survive.
        AdjustmentLedger messy = AdjustmentLedger.deserialize(
                "not-a-uuid=5\n" + id(7) + "=42\n\n# comment\n" + id(8) + "=-3\n");
        eq(messy.adjustmentFor(id(7)), 42L, "good line survives");
        eq(messy.adjustmentsSnapshot().size(), 1, "only valid positive lines kept");

        System.out.println("LedgerTests: " + checks + " assertions passed");
    }
}

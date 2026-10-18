package com.winkreyes.excelquartermaster;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Plain main-method test harness for TaleRotation (same style as the
 * ExcelGuildGate harnesses: no test framework, every check counted, any
 * failure prints and exits the JVM with status 1).
 *
 * Run via run-tests.sh, which compiles src + tests and runs this class.
 */
public final class TaleRotationTest {

    private static final UUID A = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID B = UUID.fromString("22222222-2222-2222-2222-222222222222");

    private static final int TALES = 3;
    private static final long COOLDOWN = 8000L;

    private static int checks = 0;
    private static int failures = 0;

    private static void check(String name, boolean condition) {
        checks++;
        if (!condition) {
            failures++;
            System.out.println("FAIL: " + name);
        }
    }

    private static void checkEq(String name, Object expected, Object actual) {
        checks++;
        if (!java.util.Objects.equals(expected, actual)) {
            failures++;
            System.out.println("FAIL: " + name + " expected=" + expected + " actual=" + actual);
        }
    }

    public static void main(String[] args) {
        firstClickTellsFirstTale();
        rotationAdvancesInOrder();
        rotationWrapsToTop();
        playersAreIndependent();
        cooldownBlocksAndDoesNotAdvance();
        cooldownExpiryTellsNextTale();
        cooldownIsNotExtendedByClicking();
        sneakPassesThroughAndChangesNothing();
        sneakDuringCooldownStillPassesThrough();
        zeroCooldownTellsEveryTime();
        emptyTaleListCancelsSilently();
        nullPlayerCancelsSilently();
        restoredIndexBeyondListWrapsByModulo();
        restoreSkipsNullsAndClampsNegatives();
        snapshotRestoreRoundTrip();
        System.out.println("TaleRotationTest: " + (checks - failures) + "/" + checks + " checks passed"
                + (failures == 0 ? " - ALL PASS" : " - " + failures + " FAILED"));
        if (failures > 0) {
            System.exit(1);
        }
    }

    private static void firstClickTellsFirstTale() {
        TaleRotation r = new TaleRotation();
        TaleRotation.Outcome o = r.decide(A, false, 1000L, TALES, COOLDOWN);
        checkEq("first click: kind", TaleRotation.Kind.TELL, o.kind());
        checkEq("first click: tale 0", 0, o.taleIndex());
        check("first click: changed", o.changed());
        checkEq("first click: stored next", 1, r.record(A).nextIndex());
        checkEq("first click: stored time", 1000L, r.record(A).lastTaleMillis());
    }

    private static void rotationAdvancesInOrder() {
        TaleRotation r = new TaleRotation();
        checkEq("order: 0", 0, r.decide(A, false, 0L, TALES, 0L).taleIndex());
        checkEq("order: 1", 1, r.decide(A, false, 1L, TALES, 0L).taleIndex());
        checkEq("order: 2", 2, r.decide(A, false, 2L, TALES, 0L).taleIndex());
    }

    private static void rotationWrapsToTop() {
        TaleRotation r = new TaleRotation();
        r.decide(A, false, 0L, TALES, 0L);
        r.decide(A, false, 1L, TALES, 0L);
        r.decide(A, false, 2L, TALES, 0L);
        TaleRotation.Outcome o = r.decide(A, false, 3L, TALES, 0L);
        checkEq("wrap: back to tale 0", 0, o.taleIndex());
        checkEq("wrap: stored next is 1", 1, r.record(A).nextIndex());
    }

    private static void playersAreIndependent() {
        TaleRotation r = new TaleRotation();
        r.decide(A, false, 0L, TALES, 0L);
        r.decide(A, false, 1L, TALES, 0L);
        TaleRotation.Outcome bFirst = r.decide(B, false, 2L, TALES, 0L);
        checkEq("independence: B starts at 0", 0, bFirst.taleIndex());
        checkEq("independence: A still at 2", 2, r.record(A).nextIndex());
        checkEq("independence: tracked", 2, r.trackedCount());
    }

    private static void cooldownBlocksAndDoesNotAdvance() {
        TaleRotation r = new TaleRotation();
        r.decide(A, false, 10_000L, TALES, COOLDOWN);
        TaleRotation.Outcome o = r.decide(A, false, 15_000L, TALES, COOLDOWN);
        checkEq("cooldown: kind", TaleRotation.Kind.CANCEL_SILENT, o.kind());
        checkEq("cooldown: no tale", -1, o.taleIndex());
        check("cooldown: unchanged", !o.changed());
        checkEq("cooldown: next still 1", 1, r.record(A).nextIndex());
        checkEq("cooldown: stamp still first tell", 10_000L, r.record(A).lastTaleMillis());
    }

    private static void cooldownExpiryTellsNextTale() {
        TaleRotation r = new TaleRotation();
        r.decide(A, false, 10_000L, TALES, COOLDOWN);
        // Exactly at the boundary (now - last == cooldown) the wait is over.
        TaleRotation.Outcome o = r.decide(A, false, 18_000L, TALES, COOLDOWN);
        checkEq("expiry: kind", TaleRotation.Kind.TELL, o.kind());
        checkEq("expiry: next tale told", 1, o.taleIndex());
    }

    private static void cooldownIsNotExtendedByClicking() {
        TaleRotation r = new TaleRotation();
        r.decide(A, false, 0L, TALES, COOLDOWN);
        r.decide(A, false, 7_000L, TALES, COOLDOWN); // silent, must not refresh
        TaleRotation.Outcome o = r.decide(A, false, 8_500L, TALES, COOLDOWN);
        checkEq("no-extend: tells after original window", TaleRotation.Kind.TELL, o.kind());
        checkEq("no-extend: tale 1", 1, o.taleIndex());
    }

    private static void sneakPassesThroughAndChangesNothing() {
        TaleRotation r = new TaleRotation();
        TaleRotation.Outcome o = r.decide(A, true, 0L, TALES, COOLDOWN);
        checkEq("sneak: kind", TaleRotation.Kind.PASSTHROUGH, o.kind());
        check("sneak: unchanged", !o.changed());
        checkEq("sneak: no record created", null, r.record(A));
        // And a plain click right after still gets tale 0 (sneak told nothing,
        // started no cooldown).
        checkEq("sneak: next plain click is tale 0", 0,
                r.decide(A, false, 1L, TALES, COOLDOWN).taleIndex());
    }

    private static void sneakDuringCooldownStillPassesThrough() {
        TaleRotation r = new TaleRotation();
        r.decide(A, false, 0L, TALES, COOLDOWN);
        TaleRotation.Outcome o = r.decide(A, true, 1_000L, TALES, COOLDOWN);
        checkEq("sneak in cooldown: passthrough, not silent-cancel",
                TaleRotation.Kind.PASSTHROUGH, o.kind());
        checkEq("sneak in cooldown: next still 1", 1, r.record(A).nextIndex());
    }

    private static void zeroCooldownTellsEveryTime() {
        TaleRotation r = new TaleRotation();
        checkEq("no cooldown: first", 0, r.decide(A, false, 500L, TALES, 0L).taleIndex());
        checkEq("no cooldown: same instant again", 1, r.decide(A, false, 500L, TALES, 0L).taleIndex());
    }

    private static void emptyTaleListCancelsSilently() {
        TaleRotation r = new TaleRotation();
        TaleRotation.Outcome o = r.decide(A, false, 0L, 0, COOLDOWN);
        checkEq("no tales: silent cancel", TaleRotation.Kind.CANCEL_SILENT, o.kind());
        checkEq("no tales: no record", null, r.record(A));
    }

    private static void nullPlayerCancelsSilently() {
        TaleRotation r = new TaleRotation();
        TaleRotation.Outcome o = r.decide(null, false, 0L, TALES, COOLDOWN);
        checkEq("null player: silent cancel", TaleRotation.Kind.CANCEL_SILENT, o.kind());
        checkEq("null player: nothing tracked", 0, r.trackedCount());
        checkEq("record(null) is null", null, r.record(null));
    }

    private static void restoredIndexBeyondListWrapsByModulo() {
        TaleRotation r = new TaleRotation();
        // Saved under an older, longer tales list: next index 7 of now 3 tales.
        r.restore(Map.of(A, new TaleRotation.PlayerTale(7, 0L)));
        TaleRotation.Outcome o = r.decide(A, false, 100_000L, TALES, COOLDOWN);
        checkEq("modulo: 7 mod 3 = tale 1", 1, o.taleIndex());
        checkEq("modulo: stored next 2", 2, r.record(A).nextIndex());
    }

    private static void restoreSkipsNullsAndClampsNegatives() {
        TaleRotation r = new TaleRotation();
        Map<UUID, TaleRotation.PlayerTale> in = new LinkedHashMap<>();
        in.put(A, new TaleRotation.PlayerTale(-5, -99L));
        in.put(null, new TaleRotation.PlayerTale(1, 1L));
        in.put(B, null);
        r.restore(in);
        checkEq("restore: only A kept", 1, r.trackedCount());
        checkEq("restore: index clamped", 0, r.record(A).nextIndex());
        checkEq("restore: time clamped", 0L, r.record(A).lastTaleMillis());
        r.restore(null);
        checkEq("restore(null): empties", 0, r.trackedCount());
    }

    private static void snapshotRestoreRoundTrip() {
        TaleRotation r = new TaleRotation();
        r.decide(A, false, 111L, TALES, 0L);
        r.decide(B, false, 222L, TALES, 0L);
        r.decide(B, false, 333L, TALES, 0L);
        TaleRotation copy = new TaleRotation();
        copy.restore(r.snapshot());
        checkEq("roundtrip: A", r.record(A), copy.record(A));
        checkEq("roundtrip: B", r.record(B), copy.record(B));
        checkEq("roundtrip: count", 2, copy.trackedCount());
    }
}

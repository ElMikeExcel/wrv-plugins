package com.winkreyes.excelguildgate;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Plain main-method test harness for CatchUpTracker (same style as
 * SealStateTest: no test framework, every check counted, any failure
 * prints and exits the JVM with status 1).
 *
 * Run via run-tests.sh, which compiles src + tests and runs this class.
 */
public final class CatchUpTrackerTest {

    private static final UUID A = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID B = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID C = UUID.fromString("33333333-3333-3333-3333-333333333333");

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
        if (!expected.equals(actual)) {
            failures++;
            System.out.println("FAIL: " + name + " expected=" + expected + " actual=" + actual);
        }
    }

    public static void main(String[] args) {
        firstJoinAtStageZeroIsSilent();
        firstJoinMidCampaignIsSilentStart();
        catchUpFiresOnceWithAllMissedBeats();
        secondJoinAtSameStageIsSilent();
        onlineAtFallAdvancesAndSkipsCatchUp();
        onlineWithoutRecordCountsAsTold();
        revokeClampsSilentlyThenNewFallRecatches();
        resetReplaysAllBeatsOnNextJoin();
        resetAtStageZeroPlaysNothing();
        setSeenNoOpsAndClamps();
        snapshotRestoreRoundTrip();
        restoreSkipsNullsAndClampsNegatives();
        nullSafety();
        System.out.println("CatchUpTrackerTest: " + (checks - failures) + "/" + checks + " checks passed"
                + (failures == 0 ? " - ALL PASS" : " - " + failures + " FAILED"));
        if (failures > 0) {
            System.exit(1);
        }
    }

    private static void firstJoinAtStageZeroIsSilent() {
        CatchUpTracker t = new CatchUpTracker();
        CatchUpTracker.JoinResult r = t.evaluateJoin(A, 0);
        check("first join @0: nothing missed", r.missedBeats().isEmpty());
        check("first join @0: record created (changed)", r.changed());
        checkEq("first join @0: seen", 0, t.seenStage(A));
    }

    private static void firstJoinMidCampaignIsSilentStart() {
        CatchUpTracker t = new CatchUpTracker();
        // Brand-new (or pre-0.2.0) player first seen with 3 seals down:
        // no fake "while you were away"; they start at the current stage.
        CatchUpTracker.JoinResult r = t.evaluateJoin(A, 3);
        check("first join @3: nothing missed", r.missedBeats().isEmpty());
        check("first join @3: record created (changed)", r.changed());
        checkEq("first join @3: seen starts at current", 3, t.seenStage(A));
        checkEq("first join @3: tracked count", 1, t.trackedCount());
    }

    private static void catchUpFiresOnceWithAllMissedBeats() {
        CatchUpTracker t = new CatchUpTracker();
        t.evaluateJoin(A, 0); // seen at stage 0
        CatchUpTracker.JoinResult r = t.evaluateJoin(A, 3); // three seals fell while away
        checkEq("catch-up: beats 1..3 in order", List.of(1, 2, 3), r.missedBeats());
        check("catch-up: changed", r.changed());
        checkEq("catch-up: seen advanced", 3, t.seenStage(A));
    }

    private static void secondJoinAtSameStageIsSilent() {
        CatchUpTracker t = new CatchUpTracker();
        t.evaluateJoin(A, 1);
        t.evaluateJoin(A, 4); // catches up beats 2..4
        CatchUpTracker.JoinResult r = t.evaluateJoin(A, 4);
        check("second join: nothing missed", r.missedBeats().isEmpty());
        check("second join: no change", !r.changed());
        checkEq("second join: seen", 4, t.seenStage(A));
    }

    private static void onlineAtFallAdvancesAndSkipsCatchUp() {
        CatchUpTracker t = new CatchUpTracker();
        t.evaluateJoin(A, 1);
        t.evaluateJoin(B, 1);
        // A seal falls (stage 1 -> 2) with A and B online.
        check("fall: advance reports change", t.advanceOnline(List.of(A, B), 2));
        checkEq("fall: A seen", 2, t.seenStage(A));
        checkEq("fall: B seen", 2, t.seenStage(B));
        // Re-running the same fall is a no-op.
        check("fall: re-advance no change", !t.advanceOnline(List.of(A, B), 2));
        // Their next joins are silent: they heard it live.
        check("fall: A next join silent", t.evaluateJoin(A, 2).missedBeats().isEmpty());
        check("fall: B next join silent", t.evaluateJoin(B, 2).missedBeats().isEmpty());
    }

    private static void onlineWithoutRecordCountsAsTold() {
        CatchUpTracker t = new CatchUpTracker();
        // C never joined under 0.2.0 but is online when a seal falls:
        // they are standing there for the broadcast, so they are told.
        check("online no-record: change", t.advanceOnline(List.of(C), 1));
        checkEq("online no-record: seen created at stage", 1, t.seenStage(C));
        check("online no-record: later join silent",
                t.evaluateJoin(C, 1).missedBeats().isEmpty());
    }

    private static void revokeClampsSilentlyThenNewFallRecatches() {
        CatchUpTracker t = new CatchUpTracker();
        t.evaluateJoin(A, 0);
        t.evaluateJoin(A, 5); // told through beat 5
        checkEq("revoke setup: seen", 5, t.seenStage(A));
        // A seal is revoked: stage 5 -> 4. The drop itself says nothing.
        CatchUpTracker.JoinResult r = t.evaluateJoin(A, 4);
        check("revoke: nothing missed", r.missedBeats().isEmpty());
        check("revoke: clamped (changed)", r.changed());
        checkEq("revoke: seen clamped to current", 4, t.seenStage(A));
        // No repeat nagging at the clamped stage.
        check("revoke: next join silent", t.evaluateJoin(A, 4).missedBeats().isEmpty());
        check("revoke: next join no change", !t.evaluateJoin(A, 4).changed());
        // The revoked seal genuinely falls again (stage 4 -> 5) while A is
        // away: that is a new fall past the clamped mark, reported once.
        CatchUpTracker.JoinResult r2 = t.evaluateJoin(A, 5);
        checkEq("revoke: re-fall reported once", List.of(5), r2.missedBeats());
        check("revoke: and then silent", t.evaluateJoin(A, 5).missedBeats().isEmpty());
    }

    private static void resetReplaysAllBeatsOnNextJoin() {
        CatchUpTracker t = new CatchUpTracker();
        t.evaluateJoin(A, 3); // starts at 3
        check("reset: changed", t.setSeen(A, 0));
        checkEq("reset: seen", 0, t.seenStage(A));
        CatchUpTracker.JoinResult r = t.evaluateJoin(A, 3);
        checkEq("reset: replays beats 1..3", List.of(1, 2, 3), r.missedBeats());
        checkEq("reset: seen back to current", 3, t.seenStage(A));
    }

    private static void resetAtStageZeroPlaysNothing() {
        CatchUpTracker t = new CatchUpTracker();
        t.evaluateJoin(A, 0);
        t.setSeen(A, 0);
        CatchUpTracker.JoinResult r = t.evaluateJoin(A, 0);
        check("reset @0: nothing to replay", r.missedBeats().isEmpty());
    }

    private static void setSeenNoOpsAndClamps() {
        CatchUpTracker t = new CatchUpTracker();
        check("setSeen: creates record", t.setSeen(A, 2));
        check("setSeen: same value no change", !t.setSeen(A, 2));
        check("setSeen: negative clamps", t.setSeen(A, -4));
        checkEq("setSeen: clamped to 0", 0, t.seenStage(A));
        check("setSeen: null id no change", !t.setSeen(null, 3));
    }

    private static void snapshotRestoreRoundTrip() {
        CatchUpTracker t = new CatchUpTracker();
        t.evaluateJoin(A, 2);
        t.setSeen(B, 5);
        Map<UUID, Integer> snap = t.snapshot();
        CatchUpTracker t2 = new CatchUpTracker();
        t2.restore(snap);
        checkEq("roundtrip: A", 2, t2.seenStage(A));
        checkEq("roundtrip: B", 5, t2.seenStage(B));
        checkEq("roundtrip: count", 2, t2.trackedCount());
        // Mutating the snapshot must not leak into the tracker.
        snap.put(A, 99);
        checkEq("roundtrip: snapshot is a copy", 2, t.seenStage(A));
    }

    private static void restoreSkipsNullsAndClampsNegatives() {
        CatchUpTracker t = new CatchUpTracker();
        Map<UUID, Integer> data = new LinkedHashMap<>();
        data.put(A, -7);
        data.put(B, null);
        data.put(null, 3);
        t.restore(data);
        checkEq("restore: negative clamped", 0, t.seenStage(A));
        check("restore: null value skipped", t.seenStage(B) == null);
        checkEq("restore: only valid entries kept", 1, t.trackedCount());
        t.restore(null);
        checkEq("restore null: cleared", 0, t.trackedCount());
    }

    private static void nullSafety() {
        CatchUpTracker t = new CatchUpTracker();
        check("null join: empty", t.evaluateJoin(null, 3).missedBeats().isEmpty());
        check("null join: no change", !t.evaluateJoin(null, 3).changed());
        check("null seenStage", t.seenStage(null) == null);
        check("null advance: no change", !t.advanceOnline(null, 2));
        check("advance with null member: ok",
                t.advanceOnline(java.util.Arrays.asList(A, null), 2));
        checkEq("advance with null member: A set", 2, t.seenStage(A));
        check("negative stage join: treated as 0",
                t.evaluateJoin(B, -1).missedBeats().isEmpty());
        checkEq("negative stage join: seen 0", 0, t.seenStage(B));
    }
}

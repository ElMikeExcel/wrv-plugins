package com.winkreyes.excelguildgate;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Plain main-method test harness for SealState (no test framework on the
 * classpath of the manual javac harness; same spirit as the other Excel
 * plugins' lightweight checks). Every check is counted; any failure prints
 * and exits the JVM with status 1.
 *
 * Run via build.sh (it compiles src + tests and runs this class), or:
 *   javac -d out $(find src tests -name '*.java')
 *   java -cp out com.winkreyes.excelguildgate.SealStateTest
 */
public final class SealStateTest {

    private static final List<String> FIVE = List.of(
            "ender_dragon", "wither", "elder_guardian", "warden", "raid");

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

    private static SealState full() {
        return new SealState(new LinkedHashSet<>(FIVE), new LinkedHashSet<>(FIVE));
    }

    public static void main(String[] args) {
        freshStateIsLockedAndEmpty();
        creditHappyPath();
        creditUnknownSealIsNoOp();
        creditNullAndEmptyIdsAreNoOps();
        duplicateCreditKeepsOriginalRecord();
        unlockTriggersExactlyAtCompletion();
        disabledSealsAreNotRequired();
        creditDisabledSealIsNoOp();
        grantDisabledSealRecordsButDoesNotCount();
        revokeAfterUnlockRelocks();
        revokeUnbrokenOrUnknownIsNoOp();
        resetClearsEverything();
        openAllBreaksEverything();
        allSealsDisabledIsVacuouslyUnlocked();
        progressCountsOnlyEnabledSeals();
        snapshotRestoreRoundTrip();
        restoreIgnoresUnknownSealsAndBogusUnlock();
        System.out.println("SealStateTest: " + (checks - failures) + "/" + checks + " checks passed"
                + (failures == 0 ? " - ALL PASS" : " - " + failures + " FAILED"));
        if (failures > 0) {
            System.exit(1);
        }
    }

    private static void freshStateIsLockedAndEmpty() {
        SealState s = full();
        check("fresh: locked", !s.isUnlocked());
        checkEq("fresh: brokenCount", 0, s.brokenCount());
        checkEq("fresh: requiredCount", 5, s.requiredCount());
        checkEq("fresh: unlockTimestamp", 0L, s.unlockTimestampMillis());
        for (String id : FIVE) {
            check("fresh: not broken " + id, !s.isBroken(id));
            check("fresh: no record " + id, s.record(id) == null);
        }
    }

    private static void creditHappyPath() {
        SealState s = full();
        SealState.CreditResult r = s.credit("wither", "Mike", 1000L);
        checkEq("credit: result", SealState.CreditResult.CREDITED, r);
        check("credit: broken", s.isBroken("wither"));
        checkEq("credit: count", 1, s.brokenCount());
        checkEq("credit: killer kept", "Mike", s.record("wither").killer());
        checkEq("credit: timestamp kept", 1000L, s.record("wither").timestampMillis());
        check("credit: still locked", !s.isUnlocked());
    }

    private static void creditUnknownSealIsNoOp() {
        SealState s = full();
        checkEq("unknown: result", SealState.CreditResult.NOT_A_SEAL,
                s.credit("herobrine", "Mike", 5L));
        checkEq("unknown: count", 0, s.brokenCount());
        check("unknown: nothing recorded", s.snapshot().isEmpty());
    }

    private static void creditNullAndEmptyIdsAreNoOps() {
        SealState s = full();
        checkEq("null id: result", SealState.CreditResult.NOT_A_SEAL, s.credit(null, "Mike", 5L));
        checkEq("empty id: result", SealState.CreditResult.NOT_A_SEAL, s.credit("", "Mike", 5L));
        check("null id: not broken", !s.isBroken(null));
        check("null id: no record", s.record(null) == null);
        check("null grant: no change", !s.grant(null, 5L));
        check("null revoke: no change", !s.revoke(null));
    }

    private static void duplicateCreditKeepsOriginalRecord() {
        SealState s = full();
        s.credit("wither", "First", 111L);
        SealState.CreditResult r = s.credit("wither", "Second", 222L);
        checkEq("dup: result", SealState.CreditResult.ALREADY_BROKEN, r);
        checkEq("dup: killer unchanged", "First", s.record("wither").killer());
        checkEq("dup: timestamp unchanged", 111L, s.record("wither").timestampMillis());
        checkEq("dup: count unchanged", 1, s.brokenCount());
    }

    private static void unlockTriggersExactlyAtCompletion() {
        SealState s = full();
        long t = 100L;
        for (int i = 0; i < 4; i++) {
            s.credit(FIVE.get(i), "Mike", t += 10);
            check("unlock: still locked at " + (i + 1) + " of 5", !s.isUnlocked());
            checkEq("unlock: no timestamp at " + (i + 1), 0L, s.unlockTimestampMillis());
        }
        s.credit(FIVE.get(4), "Kailey", t += 10);
        check("unlock: unlocked at 5 of 5", s.isUnlocked());
        checkEq("unlock: timestamp is the completing credit", t, s.unlockTimestampMillis());
        // Later credits are no-ops and never move the timestamp.
        s.credit("wither", "Mike", t + 9999);
        checkEq("unlock: timestamp stable after dup", t, s.unlockTimestampMillis());
    }

    private static void disabledSealsAreNotRequired() {
        Set<String> enabled = new LinkedHashSet<>(List.of("ender_dragon", "wither"));
        SealState s = new SealState(new LinkedHashSet<>(FIVE), enabled);
        checkEq("disabled-set: required", 2, s.requiredCount());
        s.credit("ender_dragon", "Mike", 1L);
        check("disabled-set: locked at 1 of 2", !s.isUnlocked());
        s.credit("wither", "Mike", 2L);
        check("disabled-set: unlocked at 2 of 2", s.isUnlocked());
    }

    private static void creditDisabledSealIsNoOp() {
        Set<String> enabled = new LinkedHashSet<>(List.of("ender_dragon"));
        SealState s = new SealState(new LinkedHashSet<>(FIVE), enabled);
        checkEq("credit disabled: result", SealState.CreditResult.DISABLED,
                s.credit("warden", "Mike", 7L));
        check("credit disabled: not recorded", !s.isBroken("warden"));
        checkEq("credit disabled: count", 0, s.brokenCount());
    }

    private static void grantDisabledSealRecordsButDoesNotCount() {
        Set<String> enabled = new LinkedHashSet<>(List.of("ender_dragon", "wither"));
        SealState s = new SealState(new LinkedHashSet<>(FIVE), enabled);
        check("grant disabled: changed", s.grant("warden", 50L));
        check("grant disabled: recorded broken", s.isBroken("warden"));
        checkEq("grant disabled: progress numerator excludes it", 0, s.brokenCount());
        check("grant disabled: still locked", !s.isUnlocked());
        check("grant twice: no change", !s.grant("warden", 60L));
        checkEq("grant twice: timestamp original", 50L, s.record("warden").timestampMillis());
        // If the owner later enables that seal, the record counts (restore path).
        SealState s2 = full();
        s2.restore(s.snapshot(), 0L);
        checkEq("grant disabled, enabled later: counts", 1, s2.brokenCount());
    }

    private static void revokeAfterUnlockRelocks() {
        SealState s = full();
        for (String id : FIVE) {
            s.credit(id, "Mike", 10L);
        }
        check("revoke: unlocked first", s.isUnlocked());
        check("revoke: changed", s.revoke("raid"));
        check("revoke: relocked", !s.isUnlocked());
        checkEq("revoke: unlock timestamp cleared", 0L, s.unlockTimestampMillis());
        checkEq("revoke: count", 4, s.brokenCount());
        check("revoke: record gone", s.record("raid") == null);
        // Re-crediting relocks the timestamp to the new completion time.
        s.credit("raid", "Mike", 42L);
        check("revoke: unlocked again", s.isUnlocked());
        checkEq("revoke: new unlock timestamp", 42L, s.unlockTimestampMillis());
    }

    private static void revokeUnbrokenOrUnknownIsNoOp() {
        SealState s = full();
        check("revoke unbroken: no change", !s.revoke("wither"));
        check("revoke unknown: no change", !s.revoke("herobrine"));
        checkEq("revoke no-op: count", 0, s.brokenCount());
    }

    private static void resetClearsEverything() {
        SealState s = full();
        for (String id : FIVE) {
            s.credit(id, "Mike", 10L);
        }
        s.reset();
        check("reset: locked", !s.isUnlocked());
        checkEq("reset: count", 0, s.brokenCount());
        checkEq("reset: timestamp", 0L, s.unlockTimestampMillis());
        check("reset: snapshot empty", s.snapshot().isEmpty());
    }

    private static void openAllBreaksEverything() {
        SealState s = full();
        s.credit("wither", "Mike", 3L);
        check("openAll: changed", s.openAll(99L));
        check("openAll: unlocked", s.isUnlocked());
        checkEq("openAll: count", 5, s.brokenCount());
        checkEq("openAll: kept existing record", 3L, s.record("wither").timestampMillis());
        checkEq("openAll: filled record timestamp", 99L, s.record("raid").timestampMillis());
        check("openAll: grant killer null", s.record("raid").killer() == null);
        check("openAll again: no change", !s.openAll(100L));
    }

    private static void allSealsDisabledIsVacuouslyUnlocked() {
        SealState s = new SealState(new LinkedHashSet<>(FIVE), Set.of());
        checkEq("none enabled: required", 0, s.requiredCount());
        check("none enabled: unlocked", s.isUnlocked());
        checkEq("none enabled: count", 0, s.brokenCount());
    }

    private static void progressCountsOnlyEnabledSeals() {
        Set<String> enabled = new LinkedHashSet<>(List.of("ender_dragon", "wither", "raid"));
        SealState s = new SealState(new LinkedHashSet<>(FIVE), enabled);
        s.credit("ender_dragon", "Mike", 1L);
        s.grant("warden", 2L); // disabled: recorded but excluded
        checkEq("progress: numerator", 1, s.brokenCount());
        checkEq("progress: denominator", 3, s.requiredCount());
    }

    private static void snapshotRestoreRoundTrip() {
        SealState s = full();
        s.credit("ender_dragon", "Mike", 111L);
        s.credit("wither", null, 222L);
        s.grant("raid", 333L);
        Map<String, SealState.SealRecord> snap = s.snapshot();
        SealState s2 = full();
        s2.restore(snap, s.unlockTimestampMillis());
        checkEq("roundtrip: snapshot equal", snap, s2.snapshot());
        checkEq("roundtrip: count equal", s.brokenCount(), s2.brokenCount());
        check("roundtrip: locked equal", s.isUnlocked() == s2.isUnlocked());
        checkEq("roundtrip: killer kept", "Mike", s2.record("ender_dragon").killer());
        check("roundtrip: null killer kept", s2.record("wither").killer() == null);
        // Full-set round trip keeps unlock + timestamp.
        SealState s3 = full();
        for (String id : FIVE) {
            s3.credit(id, "Mike", 500L);
        }
        SealState s4 = full();
        s4.restore(s3.snapshot(), s3.unlockTimestampMillis());
        check("roundtrip full: unlocked", s4.isUnlocked());
        checkEq("roundtrip full: timestamp", 500L, s4.unlockTimestampMillis());
    }

    private static void restoreIgnoresUnknownSealsAndBogusUnlock() {
        SealState s = full();
        Map<String, SealState.SealRecord> bogus = new java.util.LinkedHashMap<>();
        bogus.put("herobrine", new SealState.SealRecord(1L, "X"));
        bogus.put("wither", new SealState.SealRecord(2L, "Mike"));
        s.restore(bogus, 777L);
        check("restore bogus: unknown ignored", !s.isBroken("herobrine"));
        check("restore bogus: known kept", s.isBroken("wither"));
        check("restore bogus: still locked", !s.isUnlocked());
        checkEq("restore bogus: fake unlock timestamp dropped", 0L, s.unlockTimestampMillis());
    }
}

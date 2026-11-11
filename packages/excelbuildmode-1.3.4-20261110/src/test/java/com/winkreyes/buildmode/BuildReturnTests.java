package com.winkreyes.buildmode;

import com.winkreyes.buildmode.core.*;
import java.nio.file.*;
import java.util.*;

/**
 * ExcelBuildMode 1.3: standalone assertions for the build-return store
 * and the relog decision matrix. Run via build.sh; all data stays
 * under target/test-work. The Bukkit-side restore/landing behavior is
 * covered by the runtime probe, not here.
 */
public final class BuildReturnTests {
    private static int assertions;
    private static final UUID PLAYER = UUID.fromString("00000000-0000-0000-0000-000000000777");
    private static final UUID OTHER = UUID.fromString("00000000-0000-0000-0000-000000000778");
    private static final UUID SESSION = UUID.fromString("00000000-0000-0000-0000-000000000901");
    private static final UUID SESSION2 = UUID.fromString("00000000-0000-0000-0000-000000000902");
    private static Path root;

    public static void main(String[] args) throws Exception {
        root = Path.of("target/test-work"); Files.createDirectories(root);
        storeRoundTrip(); storeOverwriteAndClear(); storeCorruption();
        decisionMatrix();
        System.out.println("PASS " + assertions + " assertions (build-return store + relog policy; no Bukkit client simulation)");
        Files.writeString(Path.of("target/build-return-test-result.txt"), "PASS " + assertions + " assertions\n");
    }

    private static Path fixture(String label) throws Exception { return Files.createTempDirectory(root, label + "-"); }

    private static void storeRoundTrip() throws Exception {
        BuildReturnStore store = new BuildReturnStore(fixture("return-roundtrip"));
        check(store.read(PLAYER) instanceof BuildReturnStore.Record.Absent, "no record before entry");
        store.record(PLAYER, SESSION);
        var read = store.read(PLAYER);
        check(read instanceof BuildReturnStore.Record.Present present && present.session().equals(SESSION),
            "record round-trips the journal session id");
        check(store.read(OTHER) instanceof BuildReturnStore.Record.Absent, "records are per-player");
    }

    private static void storeOverwriteAndClear() throws Exception {
        BuildReturnStore store = new BuildReturnStore(fixture("return-overwrite"));
        store.record(PLAYER, SESSION);
        store.record(PLAYER, SESSION2);
        var read = store.read(PLAYER);
        check(read instanceof BuildReturnStore.Record.Present present && present.session().equals(SESSION2),
            "a fresh entry overwrites the prior record");
        store.clear(PLAYER);
        check(store.read(PLAYER) instanceof BuildReturnStore.Record.Absent, "clear removes the record");
        store.clear(PLAYER);
        check(store.read(PLAYER) instanceof BuildReturnStore.Record.Absent, "clearing twice is not an error");
    }

    private static void storeCorruption() throws Exception {
        Path dir = fixture("return-corrupt");
        BuildReturnStore store = new BuildReturnStore(dir);
        Path file = dir.resolve(PLAYER + ".return");
        Files.writeString(file, "garbage\n");
        check(store.read(PLAYER) instanceof BuildReturnStore.Record.Corrupt, "garbage file is Corrupt, never guessed");
        Files.writeString(file, "ExcelBuildMode build-return v1\n" + OTHER + "\n" + SESSION + "\n");
        check(store.read(PLAYER) instanceof BuildReturnStore.Record.Corrupt, "a record naming another player is Corrupt");
        Files.writeString(file, "ExcelBuildMode build-return v1\n" + PLAYER + "\nnot-a-uuid\n");
        check(store.read(PLAYER) instanceof BuildReturnStore.Record.Corrupt, "a non-UUID session is Corrupt");
        boolean refused = false;
        try { new BuildReturnStore(dir.resolve("missing")); } catch (Exception e) { refused = true; }
        check(refused, "an unprovisioned build-return directory is refused, not created silently");
    }

    private static void decisionMatrix() {
        var present = new BuildReturnStore.Record.Present(SESSION);
        var absent = new BuildReturnStore.Record.Absent();
        var corrupt = new BuildReturnStore.Record.Corrupt("synthetic");
        // The Option-1 happy paths: record bound to the live journal session.
        check(BuildReturnPolicy.decide(present, Optional.of(SESSION)) == BuildReturnPolicy.Decision.RESTORE_BUILD,
            "grounded logout in Build Mode: relog restores Build Mode");
        check(BuildReturnPolicy.decide(present, Optional.of(SESSION)) == BuildReturnPolicy.Decision.RESTORE_BUILD,
            "airborne logout in Build Mode: relog restores Build Mode (no grounding variant)");
        // Fallback: record stale/corrupt/missing but a session exists -> full survival recovery.
        check(BuildReturnPolicy.decide(present, Optional.of(SESSION2)) == BuildReturnPolicy.Decision.RECOVER_SURVIVAL,
            "record naming a different session falls back to survival recovery");
        check(BuildReturnPolicy.decide(corrupt, Optional.of(SESSION)) == BuildReturnPolicy.Decision.RECOVER_SURVIVAL,
            "corrupt record falls back to survival recovery");
        check(BuildReturnPolicy.decide(absent, Optional.of(SESSION)) == BuildReturnPolicy.Decision.RECOVER_SURVIVAL,
            "missing record falls back to survival recovery");
        // Fail closed: guarded state with no journal session to stand on.
        check(BuildReturnPolicy.decide(present, Optional.empty()) == BuildReturnPolicy.Decision.FAIL_CLOSED,
            "record without a journal session fails closed");
        check(BuildReturnPolicy.decide(corrupt, Optional.empty()) == BuildReturnPolicy.Decision.FAIL_CLOSED,
            "corrupt record and no session fails closed");
        check(BuildReturnPolicy.decide(absent, Optional.empty()) == BuildReturnPolicy.Decision.FAIL_CLOSED,
            "no record and no session fails closed");
    }

    private static void check(boolean condition, String label) {
        assertions++;
        if (!condition) throw new AssertionError("FAILED: " + label);
    }
}

package com.winkreyes.buildmode.core;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.util.*;
import static java.nio.file.StandardOpenOption.READ;

/**
 * ExcelBuildMode 1.3.5: the durable stores' directory sync is best-effort.
 * The raw idiom (open a directory channel and force it) is the operation
 * Windows NIO refuses outright (AccessDeniedException), and it fails the
 * same way anywhere the open cannot succeed. These tests pin both halves
 * of the fix: the unguarded idiom genuinely throws where a directory
 * cannot be opened, and DirectorySync.bestEffort tolerates exactly that
 * failure, while every store save still lands and reads back. Run via
 * build.sh; all data stays under target/test-work.
 */
public final class DirectorySyncTests {
    private static int assertions;
    private static final UUID PLAYER = UUID.fromString("00000000-0000-0000-0000-000000000801");
    private static final UUID SESSION = UUID.fromString("00000000-0000-0000-0000-000000000901");
    private static final byte[] SNAPSHOT = {9, 8, 7, 6};
    private static Path root;

    public static void main(String[] args) throws Exception {
        root = Path.of("target/test-work"); Files.createDirectories(root);
        rawIdiomFailsWhereDirectoryCannotOpen();
        bestEffortToleratesTheFailure();
        storeSavesLandThroughTheTolerantForce();
        System.out.println("PASS " + assertions + " assertions (directory sync tolerance + durable-store saves; no Bukkit client simulation)");
        Files.writeString(Path.of("target/directory-sync-test-result.txt"), "PASS " + assertions + " assertions\n");
    }

    private static Path fixture(String label) throws Exception { return Files.createTempDirectory(root, label + "-"); }

    private static void rawIdiomFailsWhereDirectoryCannotOpen() throws Exception {
        Path missing = fixture("sync-raw").resolve("not-provisioned");
        boolean openThrew = false;
        try (FileChannel channel = FileChannel.open(missing, READ)) { channel.force(true); }
        catch (IOException expected) { openThrew = true; }
        check(openThrew, "the unguarded directory open+force throws where the directory cannot be opened (the Windows failure class)");
        check(!Files.exists(missing), "the failed raw probe creates nothing");
    }

    private static void bestEffortToleratesTheFailure() throws Exception {
        Path base = fixture("sync-tolerant");
        Path missing = base.resolve("not-provisioned");
        DirectorySync.bestEffort(missing);
        check(true, "best-effort directory sync returns normally where the open fails");
        DirectorySync.bestEffort(missing);
        check(true, "best-effort directory sync tolerates the failure repeatedly");
        DirectorySync.bestEffort(base);
        check(true, "best-effort directory sync returns normally on a real directory");
        check(!Files.exists(missing), "best-effort sync never creates the directory as a side effect");
    }

    private static void storeSavesLandThroughTheTolerantForce() throws Exception {
        Path holdDir = fixture("sync-holds");
        ReviewHoldStore holds = new ReviewHoldStore(holdDir);
        holds.hold(PLAYER);
        check(holds.players().equals(Set.of(PLAYER)), "review hold saves with the tolerant directory sync");
        check(new ReviewHoldStore(holdDir).players().contains(PLAYER), "review hold reads back from a new store instance");

        Path returnDir = fixture("sync-returns");
        BuildReturnStore returns = new BuildReturnStore(returnDir);
        returns.record(PLAYER, SESSION);
        var record = returns.read(PLAYER);
        check(record instanceof BuildReturnStore.Record.Present present && present.session().equals(SESSION),
            "build-return record saves and round-trips with the tolerant directory sync");
        returns.clear(PLAYER);
        check(returns.read(PLAYER) instanceof BuildReturnStore.Record.Absent, "build-return clear still works");

        RecoveryJournal journal = new RecoveryJournal(fixture("sync-journal").resolve("recovery"));
        var session = journal.begin(PLAYER, SNAPSHOT);
        check(journal.active(PLAYER).isPresent() && Arrays.equals(journal.active(PLAYER).orElseThrow().snapshot(), SNAPSHOT),
            "journal session saves and reads back with the tolerant directory sync");
        journal.complete(session);
        check(journal.active(PLAYER).isEmpty(), "journal completion saves with the tolerant directory sync");

        ActiveSessionIndex index = new ActiveSessionIndex(fixture("sync-index").resolve("active-index"));
        var indexed = new RecoveryJournal.Session(PLAYER, SESSION, SNAPSHOT);
        index.begin(indexed);
        check(index.read(PLAYER).orElseThrow().active(), "active index saves with the tolerant directory sync");
        index.complete(indexed);
        check(!index.read(PLAYER).orElseThrow().active(), "active index tombstone saves with the tolerant directory sync");
    }

    private static void check(boolean condition, String label) {
        assertions++;
        if (!condition) throw new AssertionError("FAILED: " + label);
    }
}

// SPDX-License-Identifier: MIT
package com.winkreyes.shops;

import static org.junit.jupiter.api.Assertions.*;

import com.winkreyes.shops.checkout.CheckoutService;
import com.winkreyes.shops.checkout.FileJournal;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class LegacyDataMigrationTest {
    @TempDir Path parent;
    private Path legacy() { return parent.resolve("WRVShops"); }
    private Path excel() { return parent.resolve("ExcelShops"); }
    private Path stage() { return parent.resolve(".ExcelShops.migration-staging"); }
    private Path intent() { return parent.resolve(".ExcelShops.migration-in-progress"); }
    private LegacyDataMigration.Session open() throws IOException {
        return LegacyDataMigration.open(legacy(), excel(), false);
    }
    private Path put(Path root, String name, String value) throws IOException {
        Path path = root.resolve(name);
        Files.createDirectories(path.getParent());
        return Files.writeString(path, value);
    }
    private void populate() throws IOException {
        put(legacy(), "config.yml", "shops-enabled: true\ncustom-setting: preserved\n");
        put(legacy(), "spawn-catalog.yml", "catalog: complete\n");
        put(legacy(), "shops.dat", "registry bytes\n");
        try (FileJournal ignored = new FileJournal(legacy().resolve("checkout"))) { }
        put(legacy(), "recovery/admin-note", "Do not refund without reconciliation.\n");
        put(legacy(), "nested/名字.txt", "Unicode names and bytes: 名字\n");
        Files.createDirectories(legacy().resolve("empty"));
    }
    private Map<String, String> contents(Path root) throws IOException {
        Map<String, String> result = new TreeMap<>();
        try (var paths = Files.walk(root)) {
            for (Path path : paths.toList()) {
                String name = root.relativize(path).toString();
                if (name.equals(LegacyDataMigration.MANIFEST) || name.equals(LegacyDataMigration.COMPLETE)) continue;
                result.put(name, Files.isDirectory(path) ? "directory" : "file:" + Files.readString(path));
            }
        }
        return result;
    }
    private void assertBlocked() {
        assertThrows(IOException.class, this::open);
    }

    @Test void absentLegacyDoesNotCreateDefaultsOrCheckoutFiles() throws Exception {
        try (var session = open()) { assertEquals(LegacyDataMigration.Outcome.FRESH, session.outcome()); }
        assertFalse(Files.exists(legacy()));
        assertFalse(Files.exists(excel()));
        assertFalse(Files.exists(intent()));
    }

    @Test void emptyLegacyDoesNotInventDataOrReceipts() throws Exception {
        Files.createDirectory(legacy());
        try (var session = open()) { assertEquals(LegacyDataMigration.Outcome.FRESH, session.outcome()); }
        assertEquals(Map.of("", "directory"), contents(legacy()));
        assertFalse(Files.exists(excel()));
    }

    @Test void nativeExcelDataWithoutLegacyCanRestart() throws Exception {
        put(excel(), "shops.dat", "native shop data");
        try (var session = open()) { assertEquals(LegacyDataMigration.Outcome.EXISTING_TARGET, session.outcome()); }
        assertEquals("native shop data", Files.readString(excel().resolve("shops.dat")));
    }

    @ParameterizedTest @ValueSource(strings = {"shops.dat", "checkout/active-session", "legacy-checkout/active-session"})
    void legacyRegistryOrSessionWithoutJournalBlocksBeforeStagingAndPreservesSource(String evidence) throws Exception {
        put(legacy(), evidence, "existing evidence must not become a clean fresh journal");
        Map<String, String> before = contents(legacy());
        IOException failure = assertThrows(IOException.class, this::open);
        assertTrue(failure.getMessage().contains("transactions.log"));
        assertEquals(before, contents(legacy()));
        assertFalse(Files.exists(excel()));
        assertFalse(Files.exists(stage()));
        assertFalse(Files.exists(intent()));
    }

    @ParameterizedTest @ValueSource(booleans = {false, true})
    void missingOrNonRegularLegacyTradeLogInExistingTargetBlocksBeforeAnyStartupWrite(boolean directoryLog) throws Exception {
        put(excel(), "legacy-checkout/active-session", "unresolved legacy trade");
        if (directoryLog) Files.createDirectory(excel().resolve("legacy-checkout/transactions.log"));
        Map<String, String> before = contents(excel());
        IOException failure = assertThrows(IOException.class, this::open);
        assertTrue(failure.getMessage().contains("legacy-checkout/transactions.log"));
        assertEquals(before, contents(excel()));
        assertFalse(Files.exists(legacy()));
        assertFalse(Files.exists(stage()));
        assertFalse(Files.exists(intent()));
    }

    @Test void nonRegularLegacyTradeLogInSourceIsNotReplacedByFreshEvidence() throws Exception {
        put(legacy(), "legacy-checkout/active-session", "unresolved legacy trade");
        Files.createDirectory(legacy().resolve("legacy-checkout/transactions.log"));
        Map<String, String> before = contents(legacy());
        IOException failure = assertThrows(IOException.class, this::open);
        assertTrue(failure.getMessage().contains("legacy-checkout/transactions.log"));
        assertEquals(before, contents(legacy()));
        assertFalse(Files.exists(excel()));
        assertFalse(Files.exists(stage()));
        assertFalse(Files.exists(intent()));
    }

    @ParameterizedTest @ValueSource(booleans = {false, true})
    void legacyFreeTradeIntentAndDirtyEvidenceSurviveMigrationUnchanged(boolean keepMarker) throws Exception {
        populate();
        try (FileJournal journal = new FileJournal(legacy().resolve("legacy-checkout"))) {
            var quote = new CheckoutService.Quote(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                    "legacy:sign", 1, 4, 0, "both-inventories-before", Instant.parse("2026-10-12T12:00:00Z"));
            journal.record(quote, CheckoutService.Stage.LEGACY_INTENT, "free trade started, result unknown");
        }
        if (!keepMarker) Files.delete(legacy().resolve("legacy-checkout/active-session"));
        Map<String, String> before = contents(legacy());
        try (var session = open()) {
            assertEquals(LegacyDataMigration.Outcome.MIGRATED, session.outcome());
            assertEquals(before, contents(excel()));
            try (FileJournal journal = new FileJournal(excel().resolve("legacy-checkout"))) {
                assertTrue(journal.requiresRecovery());
            }
        }
        assertEquals(before, contents(legacy()));
        assertTrue(Files.exists(excel().resolve("legacy-checkout/active-session")));
    }

    @ParameterizedTest @ValueSource(strings = {"config.yml", "spawn-catalog.yml", "empty-checkout"})
    void earlierProtectionOnlySourceWithoutRegistryOrSessionCanMigrate(String sourceState) throws Exception {
        if (sourceState.equals("empty-checkout")) Files.createDirectories(legacy().resolve("checkout"));
        else put(legacy(), sourceState, "earlier protection-only configuration");
        Map<String, String> before = contents(legacy());
        try (var session = open()) {
            assertEquals(LegacyDataMigration.Outcome.MIGRATED, session.outcome());
            assertEquals(before, contents(excel()));
        }
        assertEquals(before, contents(legacy()));
        assertFalse(Files.exists(stage()));
        assertFalse(Files.exists(intent()));
    }

    @Test void entireLegacyTreeCopiedTogetherAndSourceUnchanged() throws Exception {
        populate();
        Map<String, String> before = contents(legacy());
        try (var session = open()) {
            assertEquals(LegacyDataMigration.Outcome.MIGRATED, session.outcome());
            assertEquals(before, contents(excel()));
            assertTrue(Files.isRegularFile(excel().resolve(LegacyDataMigration.MANIFEST)));
            assertTrue(Files.isRegularFile(excel().resolve(LegacyDataMigration.COMPLETE)));
            assertFalse(Files.exists(stage()));
            assertFalse(Files.exists(intent()));
        }
        assertEquals(before, contents(legacy()));
    }

    @Test void privateLegacyFileAndDirectoryPermissionsArePreserved() throws Exception {
        populate();
        var privateFile = java.nio.file.attribute.PosixFilePermissions.fromString("rw-------");
        var privateDirectory = java.nio.file.attribute.PosixFilePermissions.fromString("rwx------");
        Files.setPosixFilePermissions(legacy(), privateDirectory);
        Files.setPosixFilePermissions(legacy().resolve("config.yml"), privateFile);
        try (var ignored = open()) {
            assertEquals(privateDirectory, Files.getPosixFilePermissions(excel()));
            assertEquals(privateFile, Files.getPosixFilePermissions(excel().resolve("config.yml")));
        }
    }

    @Test void arbitraryBinaryRegistryAndUnknownFilesAreCopiedByteForByte() throws Exception {
        populate();
        byte[] data = {0, -1, 4, 127, -128, 0, 9};
        Files.write(legacy().resolve("shops.dat"), data);
        Files.write(legacy().resolve("unknown-recovery.bin"), data);
        try (var ignored = open()) {
            assertArrayEquals(data, Files.readAllBytes(excel().resolve("shops.dat")));
            assertArrayEquals(data, Files.readAllBytes(excel().resolve("unknown-recovery.bin")));
        }
        assertArrayEquals(data, Files.readAllBytes(legacy().resolve("shops.dat")));
    }

    @Test void anEmptyPrecreatedTargetIsSafelyReplacedAtomically() throws Exception {
        populate();
        Files.createDirectory(excel());
        try (var session = open()) { assertEquals(LegacyDataMigration.Outcome.MIGRATED, session.outcome()); }
        assertEquals(contents(legacy()), contents(excel()));
    }

    @Test void dirtySessionAndAllRecoveryMarkersSurviveAndCheckoutRemainsBlocked() throws Exception {
        populate();
        try (FileJournal journal = new FileJournal(legacy().resolve("checkout"))) { journal.closeUnclean(); }
        put(legacy(), "checkout/recovery-required", "offline reconciliation required");
        try (var session = open()) {
            assertTrue(Files.exists(excel().resolve("checkout/active-session")));
            assertEquals("offline reconciliation required", Files.readString(excel().resolve("checkout/recovery-required")));
            try (FileJournal journal = new FileJournal(excel().resolve("checkout"))) {
                assertTrue(journal.requiresRecovery());
            }
            assertTrue(Files.exists(excel().resolve("checkout/active-session")));
        }
        assertTrue(Files.exists(legacy().resolve("checkout/active-session")));
    }

    @Test void pendingWithdrawalEvidenceSurvivesEvenIfLegacySessionMarkerWasRemoved() throws Exception {
        populate();
        try (FileJournal journal = new FileJournal(legacy().resolve("checkout"))) {
            var quote = new CheckoutService.Quote(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                    "shop:one", 1, 1, 100, "stock-fingerprint", Instant.parse("2026-10-12T12:00:00Z"));
            journal.record(quote, CheckoutService.Stage.WITHDRAW_INTENT, "durable debit intent");
        }
        Files.delete(legacy().resolve("checkout/active-session"));
        String evidence = Files.readString(legacy().resolve("checkout/transactions.log"));
        try (var session = open(); FileJournal journal = new FileJournal(excel().resolve("checkout"))) {
            assertTrue(journal.requiresRecovery());
            assertTrue(journal.recoveryReason().contains("incomplete"));
            assertEquals(evidence, Files.readString(excel().resolve("checkout/transactions.log")));
        }
    }

    @Test void restartUsesReceiptAndNeverCopiesStaleLegacyOverNewData() throws Exception {
        populate();
        Map<String, String> legacyBefore = contents(legacy());
        try (var ignored = open()) { }
        put(excel(), "shops.dat", "new shops registered after migration");
        put(excel(), "config.yml", "new runtime configuration");
        put(excel(), "checkout/new-recovery-marker", "new uncertain transaction");
        try (FileJournal journal = new FileJournal(excel().resolve("checkout"))) { journal.closeUnclean(); }
        Map<String, String> newer = contents(excel());
        try (var session = open()) {
            assertEquals(LegacyDataMigration.Outcome.ALREADY_MIGRATED, session.outcome());
            assertEquals(newer, contents(excel()));
        }
        assertEquals(legacyBefore, contents(legacy()));
    }

    @Test void cleanRuntimeCanRemoveMigratedDirtyMarkerAfterManualReconciliationWithoutBreakingProvenance() throws Exception {
        populate();
        put(legacy(), "checkout/active-session", "dirty session");
        try (var ignored = open()) { }
        Files.delete(excel().resolve("checkout/active-session"));
        try (var session = open()) { assertEquals(LegacyDataMigration.Outcome.ALREADY_MIGRATED, session.outcome()); }
        assertFalse(Files.exists(excel().resolve("checkout/active-session")));
        assertTrue(Files.exists(legacy().resolve("checkout/active-session")));
    }

    @Test void archivedLegacyAbsenceDoesNotDiscardValidReceiptOrNewData() throws Exception {
        populate();
        try (var ignored = open()) { }
        Files.move(legacy(), parent.resolve("archived-legacy"));
        try (var session = open()) { assertEquals(LegacyDataMigration.Outcome.ALREADY_MIGRATED, session.outcome()); }
    }

    @Test void changedLegacyAfterSuccessfulMigrationFailsClosedWithoutTouchingNewData() throws Exception {
        populate();
        try (var ignored = open()) { }
        put(excel(), "shops.dat", "new data");
        put(legacy(), "shops.dat", "another old server wrote data");
        Map<String, String> before = contents(excel());
        assertBlocked();
        assertEquals(before, contents(excel()));
    }

    @ParameterizedTest @ValueSource(strings = {"config.yml", "shops.dat", "checkout/transactions.log"})
    void unprovenExistingTargetEvenDefaultsOnlyFailsClosed(String file) throws Exception {
        populate();
        put(excel(), file, "conflicting or default data");
        Map<String, String> before = contents(legacy());
        assertBlocked();
        assertEquals(before, contents(legacy()));
        assertEquals("conflicting or default data", Files.readString(excel().resolve(file)));
        assertFalse(Files.exists(stage()));
    }

    @Test void identicalUnprovenBothDirectoriesAlsoFailClosed() throws Exception {
        put(legacy(), "config.yml", "identical bytes");
        put(excel(), "config.yml", "identical bytes");
        assertBlocked();
    }

    @Test void aLoadedEvenDisabledLegacyPluginBlocksBeforeAnyFilesAreCreated() throws Exception {
        assertThrows(IOException.class, () -> LegacyDataMigration.open(legacy(), excel(), true));
        try (var children = Files.list(parent)) { assertEquals(0, children.count()); }
    }

    @Test void alreadyOpenLegacyJournalBlocksMigration() throws Exception {
        populate();
        try (FileJournal ignored = new FileJournal(legacy().resolve("checkout"))) { assertBlocked(); }
        assertFalse(Files.exists(excel()));
        assertFalse(Files.exists(stage()));
        try (var session = open()) { assertEquals(LegacyDataMigration.Outcome.MIGRATED, session.outcome()); }
    }

    @Test void actualSeparateLegacyProcessPreventsMigrationAndFailureReleasesExcelLock() throws Exception {
        populate();
        Process holder = probeProcess(legacy().resolve("checkout/transactions.log"), "hold");
        try {
            var reader = holder.inputReader();
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(15);
            String ready = null;
            while (holder.isAlive() && System.nanoTime() < deadline) {
                if (!reader.ready()) { Thread.sleep(10); continue; }
                String line = reader.readLine();
                // The controlled test runner sets safe JVM limits; the launcher prints this before main().
                if (line != null && line.startsWith("Picked up JAVA_TOOL_OPTIONS:")) continue;
                ready = line; break;
            }
            assertEquals("ready", ready, "Lock-holder process did not become ready");
            assertBlocked();
            assertFalse(Files.exists(stage()));
            assertFalse(Files.exists(excel()));
            assertEquals(0, probe(parent.resolve(".ExcelShops.migration.lock")));
        } finally {
            holder.getOutputStream().close();
            if (!holder.waitFor(15, TimeUnit.SECONDS)) holder.destroyForcibly();
        }
        try (var session = open()) { assertEquals(LegacyDataMigration.Outcome.MIGRATED, session.outcome()); }
    }

    @Test void duplicateExcelSessionCannotStartAndReleaseAllowsRestart() throws Exception {
        populate();
        try (var ignored = open()) {
            assertBlocked();
            assertEquals(13, probe(parent.resolve(".ExcelShops.migration.lock")));
            assertEquals(13, probe(legacy().resolve("checkout/transactions.log")));
        }
        try (var session = open()) { assertEquals(LegacyDataMigration.Outcome.ALREADY_MIGRATED, session.outcome()); }
    }

    @Test void actualSeparateProcessCannotLockLegacyLogDuringMigratedOrRestartedSession() throws Exception {
        populate();
        for (int i = 0; i < 2; i++) {
            try (var ignored = open()) { assertEquals(13, probe(legacy().resolve("checkout/transactions.log"))); }
            assertEquals(0, probe(legacy().resolve("checkout/transactions.log")));
        }
    }

    @ParameterizedTest @ValueSource(strings = {"stage", "intent", "both"})
    void interruptedMigrationArtifactsAlwaysBlockAndArePreserved(String leftover) throws Exception {
        populate();
        if (!leftover.equals("intent")) put(stage(), "shops.dat", "partial evidence");
        if (!leftover.equals("stage")) Files.writeString(intent(), "interrupted");
        assertBlocked();
        assertFalse(Files.exists(excel()));
        if (!leftover.equals("intent")) assertEquals("partial evidence", Files.readString(stage().resolve("shops.dat")));
        if (!leftover.equals("stage")) assertEquals("interrupted", Files.readString(intent()));
    }

    @Test void interruptedFinalCommitMarkerBlocksEvenWithValidReceipt() throws Exception {
        populate();
        try (var ignored = open()) { }
        Files.writeString(intent(), "final durability barrier not completed");
        assertBlocked();
    }

    @ParameterizedTest @ValueSource(strings = {"manifest-missing", "complete-missing", "manifest-corrupt", "complete-corrupt"})
    void damagedProvenanceBlocksRestart(String damage) throws Exception {
        populate();
        try (var ignored = open()) { }
        Path artifact = excel().resolve(damage.startsWith("manifest") ? LegacyDataMigration.MANIFEST : LegacyDataMigration.COMPLETE);
        if (damage.endsWith("missing")) Files.delete(artifact); else Files.writeString(artifact, "tampered");
        assertBlocked();
    }

    @ParameterizedTest @ValueSource(strings = {"config.yml", "spawn-catalog.yml", "shops.dat", "checkout/transactions.log", "legacy-checkout/transactions.log"})
    void missingMigratedCoreFileCannotBeRecreatedAsDefaults(String file) throws Exception {
        populate();
        if (file.startsWith("legacy-checkout/")) {
            try (FileJournal ignored = new FileJournal(legacy().resolve("legacy-checkout"))) { }
        }
        Map<String, String> sourceBefore = contents(legacy());
        try (var ignored = open()) { }
        Files.delete(excel().resolve(file));
        assertBlocked();
        assertFalse(Files.exists(excel().resolve(file)));
        assertEquals(sourceBefore, contents(legacy()));
    }

    @Test void movingTheEntireServerRequiresOfflineProvenanceReview() throws Exception {
        Path originalServer = parent.resolve("original-server");
        Path oldSource = originalServer.resolve("plugins/WRVShops");
        Path oldTarget = originalServer.resolve("plugins/ExcelShops");
        put(oldSource, "config.yml", "source data");
        try (var ignored = LegacyDataMigration.open(oldSource, oldTarget, false)) { }
        Path movedServer = parent.resolve("moved-server");
        Files.move(originalServer, movedServer);
        assertThrows(IOException.class, () -> LegacyDataMigration.open(
                movedServer.resolve("plugins/WRVShops"), movedServer.resolve("plugins/ExcelShops"), false));
        assertEquals("source data", Files.readString(movedServer.resolve("plugins/ExcelShops/config.yml")));
    }

    @Test void receiptCannotBeReusedForAnotherTargetFolder() throws Exception {
        populate();
        try (var ignored = open()) { }
        Path different = parent.resolve("OtherShops");
        Files.move(excel(), different);
        assertThrows(IOException.class, () -> LegacyDataMigration.open(legacy(), different, false));
    }

    @ParameterizedTest @ValueSource(strings = {"legacy-root", "target-root", "legacy-child", "target-child", "lock", "ancestor"})
    void symlinksAlwaysFailClosed(String link) throws Exception {
        Path outside = Files.createDirectory(parent.resolve("outside"));
        put(outside, "evidence", "outside must stay unchanged");
        switch (link) {
            case "legacy-root" -> Files.createSymbolicLink(legacy(), outside);
            case "target-root" -> Files.createSymbolicLink(excel(), outside);
            case "legacy-child" -> { populate(); Files.createSymbolicLink(legacy().resolve("linked"), outside); }
            case "target-child" -> { Files.createDirectory(excel()); Files.createSymbolicLink(excel().resolve("linked"), outside); }
            case "lock" -> Files.createSymbolicLink(parent.resolve(".ExcelShops.migration.lock"), outside.resolve("evidence"));
            case "ancestor" -> {
                Path alias = parent.resolve("alias");
                Files.createSymbolicLink(alias, outside);
                assertThrows(IOException.class, () -> LegacyDataMigration.open(alias.resolve("WRVShops"), alias.resolve("ExcelShops"), false));
                return;
            }
            default -> throw new AssertionError(link);
        }
        assertBlocked();
        assertEquals("outside must stay unchanged", Files.readString(outside.resolve("evidence")));
    }

    @ParameterizedTest @ValueSource(strings = {"checkout/transactions.log", "config.yml"})
    void hardlinkedSourceCannotDropLegacyLockDuringHashOrCopy(String file) throws Exception {
        populate();
        Files.createLink(legacy().resolve("hardlink-alias"), legacy().resolve(file));
        assertBlocked();
        assertFalse(Files.exists(excel()));
        assertFalse(Files.exists(stage()));
        assertFalse(Files.exists(intent()));
    }

    @Test void danglingSymlinkIsNotMistakenForAbsentLegacyData() throws Exception {
        Files.createSymbolicLink(legacy(), parent.resolve("missing"));
        assertBlocked();
    }

    @Test void fileCopyFailureLeavesOnlyBlockedStagingAndUntouchedSource() throws Exception {
        populate();
        Map<String, String> before = contents(legacy());
        assertThrows(IOException.class, () -> LegacyDataMigration.open(legacy(), excel(), false,
                (source, staged) -> { throw new IOException("simulated disk failure"); }));
        assertTrue(Files.exists(intent()));
        assertTrue(Files.exists(stage()));
        assertFalse(Files.exists(excel()));
        assertEquals(before, contents(legacy()));
        assertBlocked();
    }

    @Test void copiedBytesTamperedDuringCopyFailChecksumAndNeverPublish() throws Exception {
        populate();
        assertThrows(IOException.class, () -> LegacyDataMigration.open(legacy(), excel(), false,
                (source, staged) -> Files.writeString(staged, "corrupted copy")));
        assertTrue(Files.exists(stage()));
        assertTrue(Files.exists(intent()));
        assertFalse(Files.exists(excel()));
    }

    @ParameterizedTest @ValueSource(strings = {"modify", "add", "delete", "replace-with-symlink", "replace-identical-inode"})
    void concurrentSourceChangesAreDetectedBeforePublication(String change) throws Exception {
        populate();
        AtomicBoolean once = new AtomicBoolean();
        assertThrows(IOException.class, () -> LegacyDataMigration.open(legacy(), excel(), false, (source, staged) -> {
            if (!once.compareAndSet(false, true)) return;
            Path file = legacy().resolve("config.yml");
            switch (change) {
                case "modify" -> Files.writeString(file, "unexpected source edit");
                case "add" -> put(legacy(), "new-recovery-marker", "stop");
                case "delete" -> Files.delete(file);
                case "replace-with-symlink" -> { Files.delete(file); Files.createSymbolicLink(file, staged); }
                case "replace-identical-inode" -> {
                    String value = Files.readString(file);
                    var modified = Files.getLastModifiedTime(file);
                    Files.move(file, parent.resolve("old-config"));
                    Files.writeString(file, value);
                    Files.setLastModifiedTime(file, modified);
                }
                default -> throw new AssertionError(change);
            }
        }));
        assertTrue(Files.exists(intent()));
        assertFalse(Files.exists(excel()));
        assertBlocked();
    }

    @Test void targetCreatedByConcurrentWriterIsNeverReplaced() throws Exception {
        populate();
        AtomicBoolean once = new AtomicBoolean();
        assertThrows(IOException.class, () -> LegacyDataMigration.open(legacy(), excel(), false, (source, staged) -> {
            if (once.compareAndSet(false, true)) put(excel(), "shops.dat", "concurrent target data");
        }));
        assertEquals("concurrent target data", Files.readString(excel().resolve("shops.dat")));
        assertTrue(Files.exists(stage()));
        assertTrue(Files.exists(intent()));
    }

    @Test void precreatedEmptyTargetChangedDuringCopyIsNeverReplaced() throws Exception {
        populate();
        Files.createDirectory(excel());
        AtomicBoolean once = new AtomicBoolean();
        assertThrows(IOException.class, () -> LegacyDataMigration.open(legacy(), excel(), false, (source, staged) -> {
            if (once.compareAndSet(false, true)) put(excel(), "config.yml", "concurrent target data");
        }));
        assertEquals("concurrent target data", Files.readString(excel().resolve("config.yml")));
    }

    @Test void reservedReceiptNamesInLegacyAreNotSilentlyOverwritten() throws Exception {
        populate();
        put(legacy(), LegacyDataMigration.MANIFEST, "unknown origin");
        assertBlocked();
        assertFalse(Files.exists(stage()));
    }

    @ParameterizedTest @ValueSource(strings = {"source", "target"})
    void aFileInPlaceOfDataDirectoryBlocks(String path) throws Exception {
        Files.writeString(path.equals("source") ? legacy() : excel(), "not a directory");
        assertBlocked();
    }

    @Test void nonSiblingAndIdenticalPathsAreRejected() {
        assertThrows(IOException.class, () -> LegacyDataMigration.open(legacy(), legacy(), false));
        assertThrows(IOException.class, () -> LegacyDataMigration.open(legacy(), excel().resolve("nested"), false));
    }

    @Test void sessionCloseIsIdempotent() throws Exception {
        var session = open();
        session.close();
        session.close();
        try (var reopened = open()) { assertEquals(LegacyDataMigration.Outcome.FRESH, reopened.outcome()); }
    }

    private int probe(Path log) throws Exception {
        Process process = probeProcess(log, "probe");
        try {
            assertTrue(process.waitFor(15, TimeUnit.SECONDS), "Lock probe timed out");
            int exit = process.exitValue();
            assertTrue(exit == 0 || exit == 13, () -> {
                try { return new String(process.getInputStream().readAllBytes()); }
                catch (IOException failure) { return failure.toString(); }
            });
            return exit;
        } finally { process.destroyForcibly(); }
    }

    private Process probeProcess(Path log, String mode) throws IOException {
        String classPath = System.getProperty("surefire.test.class.path", System.getProperty("java.class.path"));
        return new ProcessBuilder(Path.of(System.getProperty("java.home"), "bin", "java").toString(),
                "-cp", classPath, LockProbe.class.getName(), log.toString(), mode).redirectErrorStream(true).start();
    }

    /** Separate JVM proves that hashing/copying did not inadvertently release a POSIX lock. */
    public static final class LockProbe {
        public static void main(String[] args) throws Exception {
            try (FileChannel channel = FileChannel.open(Path.of(args[0]), StandardOpenOption.READ, StandardOpenOption.WRITE)) {
                try (FileLock lock = channel.tryLock()) {
                    if (lock == null) System.exit(13);
                    if (args.length > 1 && args[1].equals("hold")) {
                        System.out.println("ready");
                        System.out.flush();
                        System.in.read();
                    }
                }
            }
        }
    }
}

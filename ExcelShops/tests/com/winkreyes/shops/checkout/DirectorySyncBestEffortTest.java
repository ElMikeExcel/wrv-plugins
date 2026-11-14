// SPDX-License-Identifier: MIT
package com.winkreyes.shops.checkout;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.CALLS_REAL_METHODS;

import com.winkreyes.shops.LegacyDataMigration;
import java.nio.channels.FileChannel;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.util.Map;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

/**
 * ExcelShops 1.3.9: directory fsync is best-effort. Windows refuses directory
 * channels outright (AccessDeniedException on FileChannel.open, proven by the
 * Nov 14, 2026 mirror diagnosis probe), so a store save must still succeed on
 * the file-level fsync alone. These tests simulate exactly that refusal:
 * every FileChannel.open on a directory throws AccessDeniedException, while
 * file opens run for real. On the 1.3.8 code each scenario fails at the first
 * directory sync; on 1.3.9 the stores open, write, and close cleanly.
 */
class DirectorySyncBestEffortTest {
    @TempDir Path parent;

    private MockedStatic<FileChannel> refuseDirectoryChannels() {
        MockedStatic<FileChannel> channels = Mockito.mockStatic(FileChannel.class, CALLS_REAL_METHODS);
        channels.when(() -> FileChannel.open(any(Path.class), any(OpenOption[].class)))
                .thenAnswer(invocation -> {
                    Path path = invocation.getArgument(0);
                    if (path != null && Files.isDirectory(path)) {
                        throw new AccessDeniedException(path.toString());
                    }
                    return invocation.callRealMethod();
                });
        return channels;
    }

    @Test void fileJournalOpensAndCleanClosesWhenDirectorySyncRefused() throws Exception {
        Path checkout = parent.resolve("ExcelShops").resolve("checkout");
        try (MockedStatic<FileChannel> ignored = refuseDirectoryChannels()) {
            FileJournal journal = new FileJournal(checkout);
            assertFalse(journal.requiresRecovery());
            journal.close();
            assertFalse(Files.exists(checkout.resolve(FileJournal.SESSION_FILE)),
                    "clean close still removes the session marker");
        }
        try (FileJournal reopened = new FileJournal(checkout)) {
            assertFalse(reopened.requiresRecovery(), "no false recovery after a refused directory sync");
        }
    }

    @Test void globalSessionGuardRecordsReadyAndClosesCleanWhenDirectorySyncRefused() throws Exception {
        Path root = parent.resolve("ExcelShops");
        try (MockedStatic<FileChannel> ignored = refuseDirectoryChannels();
             FileJournal checkout = new FileJournal(root.resolve("checkout"));
             FileJournal legacy = new FileJournal(root.resolve("legacy-checkout"))) {
            Map<String, GlobalSessionGuard.Fingerprint> proofs = Map.of(
                    "checkout", checkout.proof(), "legacy-checkout", legacy.proof());
            GlobalSessionGuard guard = GlobalSessionGuard.open(root, true);
            assertEquals(GlobalSessionGuard.Decision.CLEAN_START, guard.assess(proofs, false));
            guard.recordReady(proofs);
            assertTrue(Files.isRegularFile(root.resolve("global-session-v2")),
                    "READY envelope was written despite the refused directory sync");
            guard.closeClean(true);
            assertFalse(Files.exists(root.resolve("global-session-v2")),
                    "clean close still removes the envelope");
        }
    }

    @Test void legacyMigrationCompletesWhenDirectorySyncRefused() throws Exception {
        Path legacy = parent.resolve("WRVShops");
        Path excel = parent.resolve("ExcelShops");
        put(legacy, "config.yml", "shops-enabled: true\ncustom-setting: preserved\n");
        put(legacy, "spawn-catalog.yml", "catalog: complete\n");
        put(legacy, "shops.dat", "registry bytes\n");
        try (FileJournal ignored = new FileJournal(legacy.resolve("checkout"))) { }
        put(legacy, "recovery/admin-note", "Do not refund without reconciliation.\n");
        Files.createDirectories(legacy.resolve("empty"));
        Map<String, String> before = contents(legacy);
        try (MockedStatic<FileChannel> ignored = refuseDirectoryChannels();
             var session = LegacyDataMigration.open(legacy, excel, false)) {
            assertEquals(LegacyDataMigration.Outcome.MIGRATED, session.outcome());
            assertEquals(before, contents(excel));
        }
        assertEquals(before, contents(legacy), "source tree untouched by the migration");
    }

    private Path put(Path root, String name, String value) throws Exception {
        Path path = root.resolve(name);
        Files.createDirectories(path.getParent());
        return Files.writeString(path, value);
    }

    private Map<String, String> contents(Path root) throws Exception {
        Map<String, String> result = new TreeMap<>();
        try (var paths = Files.walk(root)) {
            for (Path path : paths.toList()) {
                String name = root.relativize(path).toString();
                if (name.equals(".excelshops-legacy-manifest-v1") || name.equals(".excelshops-legacy-complete-v1")) continue;
                result.put(name, Files.isDirectory(path) ? "directory" : "file:" + Files.readString(path));
            }
        }
        return result;
    }
}

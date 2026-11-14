package com.winkreyes.makersmark.history;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;

/** Coverage for the Windows-safe directory sync (0.1.6).
 *  The production failure is Windows Java NIO throwing
 *  AccessDeniedException on FileChannel.open of a directory. That exact
 *  exception cannot occur on the Linux build host, where directory
 *  channels open fine; the tests therefore pin the contract with the
 *  sibling FileSystemException failure modes that DO occur here
 *  (missing directory -> NoSuchFileException, path through a regular
 *  file -> NotDirectoryException): before the fix, forceDirectory
 *  propagated them and the save failed; after the fix it must return
 *  normally. The exact AccessDeniedException path is verified on the
 *  Windows test-server mirror at rig time. */
class HistoryLedgerFsyncTest {

    @Test void directoryForceOnExistingDirectoryReturnsNormally(@TempDir Path dir) {
        assertDoesNotThrow(() -> HistoryLedger.forceDirectory(dir));
    }

    @Test void directoryForceFailureIsSwallowedMissingDirectory(@TempDir Path dir) {
        Path missing = dir.resolve("no-such-directory");
        assertDoesNotThrow(() -> HistoryLedger.forceDirectory(missing));
    }

    @Test void directoryForceFailureIsSwallowedThroughRegularFile(@TempDir Path dir) throws IOException {
        Path file = Files.createFile(dir.resolve("plain-file"));
        Path throughFile = file.resolve("child");
        assertDoesNotThrow(() -> HistoryLedger.forceDirectory(throughFile));
    }

    /** End-to-end: open (constructor directory sync), award, prepare,
     *  commit, checkpoint (both publication directory syncs), close,
     *  reopen and replay — the full save surface must succeed with the
     *  best-effort directory sync in place, and state must survive. */
    @Test void ledgerSaveReloadAndCheckpointSucceed(@TempDir Path dir) throws Exception {
        Path archive = dir.resolve("history.mmh2");
        UUID receipt = UUID.randomUUID(), item = UUID.randomUUID();
        UUID victim = UUID.randomUUID(), actor = UUID.randomUUID();
        HistoryLedger.Key key = new HistoryLedger.Key("boss", "Ender Dragon");
        try (HistoryLedger ledger = new HistoryLedger(archive)) {
            ledger.acceptAward(new HistoryLedger.Award(receipt, item, victim, actor, key));
            HistoryLedger.Plan plan = ledger.preview(receipt, List.of(), item, null);
            ledger.prepare(plan);
            ledger.commit(receipt, item, plan.output().revision(), true, true);
            assertEquals(HistoryLedger.Status.COMMITTED, ledger.status(receipt));
            Path target = dir.resolve("checkpoint.mmh2");
            ledger.checkpoint(target);
            assertTrue(Files.exists(target));
        }
        try (HistoryLedger reopened = new HistoryLedger(archive)) {
            assertEquals(HistoryLedger.Status.COMMITTED, reopened.status(receipt));
            assertNotNull(reopened.state(item));
            assertEquals(1L, reopened.state(item).revision());
        }
    }
}

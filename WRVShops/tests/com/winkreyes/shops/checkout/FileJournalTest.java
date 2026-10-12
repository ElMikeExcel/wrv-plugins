// SPDX-License-Identifier: MIT
package com.winkreyes.shops.checkout;

import static org.junit.jupiter.api.Assertions.*;
import static com.winkreyes.shops.checkout.CheckoutService.*;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

class FileJournalTest {
    @TempDir Path directory;

    private Quote quote() {
        return new Quote(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "shop:metadata", 2,
                4, 500, "sha256-and-exact-stock", Instant.parse("2026-10-12T12:00:00Z"));
    }

    @Test void cleanEmptySessionCanBeReopenedWithoutRecovery() throws Exception {
        try (FileJournal journal = new FileJournal(directory)) {
            assertFalse(journal.requiresRecovery());
            assertTrue(Files.exists(directory.resolve(FileJournal.SESSION_FILE)));
            assertTrue(Files.exists(directory.resolve(FileJournal.LOG_FILE)));
        }
        assertFalse(Files.exists(directory.resolve(FileJournal.SESSION_FILE)));
        try (FileJournal reopened = new FileJournal(directory)) { assertFalse(reopened.requiresRecovery()); }
    }

    @Test void previousUncleanEmptySessionLocksBeforeAnyTransaction() throws Exception {
        FileJournal journal = new FileJournal(directory);
        journal.closeUnclean();
        try (FileJournal reopened = new FileJournal(directory)) {
            assertTrue(reopened.requiresRecovery());
            assertTrue(reopened.recoveryReason().contains("cleanly"));
            assertThrows(IOException.class, () -> reopened.record(quote(), Stage.WITHDRAW_INTENT, "do not charge"));
        }
        assertTrue(Files.exists(directory.resolve(FileJournal.SESSION_FILE)));
    }

    @Test void cleanCommittedTransactionPersistsChecksummedAuditEvidence() throws Exception {
        Quote quote = quote();
        String evidence = "inventories-before=[enchanted sword,名字];after=[]\nsource=world\t10,64,20";
        try (FileJournal journal = new FileJournal(directory)) { commit(journal, quote, evidence); }
        List<String> lines = Files.readAllLines(directory.resolve(FileJournal.LOG_FILE));
        assertEquals(7, lines.size());
        String[] first = lines.getFirst().split("\t", -1);
        assertEquals(14, first.length);
        assertEquals(quote.id().toString(), first[1]);
        assertEquals(quote.buyerId().toString(), first[2]);
        assertEquals(quote.sellerId().toString(), first[3]);
        assertEquals("500", first[7]);
        assertEquals("WITHDRAW_INTENT", first[10]);
        assertEquals(evidence, new String(Base64.getUrlDecoder().decode(first[12]), StandardCharsets.UTF_8));
        assertEquals(64, first[13].length());
        try (FileJournal reopened = new FileJournal(directory)) { assertFalse(reopened.requiresRecovery()); }
    }

    @Test void committedTransactionStillLocksAfterUncleanShutdownBecauseVaultAndWorldAreNotAtomic() throws Exception {
        FileJournal journal = new FileJournal(directory);
        commit(journal, quote(), "inventory evidence");
        journal.closeUnclean();
        try (FileJournal reopened = new FileJournal(directory)) { assertTrue(reopened.requiresRecovery()); }
    }

    @ParameterizedTest @EnumSource(value = Stage.class, names = {"WITHDRAW_INTENT", "WITHDRAWN", "TRANSFER_INTENT", "TRANSFER_APPLIED", "PAYOUT_INTENT", "PAID"})
    void everyIncompletePhaseRemainsLockedEvenWhenCleanCloseIsRequested(Stage stop) throws Exception {
        try (FileJournal journal = new FileJournal(directory)) {
            Quote quote = quote();
            for (Stage stage : List.of(Stage.WITHDRAW_INTENT, Stage.WITHDRAWN, Stage.TRANSFER_INTENT,
                    Stage.TRANSFER_APPLIED, Stage.PAYOUT_INTENT, Stage.PAID)) {
                journal.record(quote, stage, "test");
                if (stage == stop) break;
            }
        }
        assertTrue(Files.exists(directory.resolve(FileJournal.SESSION_FILE)));
        // Missing marker alone cannot bypass incomplete transaction evidence.
        Files.delete(directory.resolve(FileJournal.SESSION_FILE));
        try (FileJournal reopened = new FileJournal(directory)) {
            assertTrue(reopened.requiresRecovery());
            assertTrue(reopened.recoveryReason().contains("incomplete"));
        }
    }

    @Test void definitiveDeclineClosesCleanly() throws Exception {
        try (FileJournal journal = new FileJournal(directory)) {
            Quote quote = quote();
            journal.record(quote, Stage.WITHDRAW_INTENT, "before");
            journal.record(quote, Stage.NO_CHARGE, "definitive decline");
        }
        try (FileJournal reopened = new FileJournal(directory)) { assertFalse(reopened.requiresRecovery()); }
    }

    @Test void unchangedDeliveryThenRefundClosesCleanly() throws Exception {
        try (FileJournal journal = new FileJournal(directory)) {
            Quote quote = quote();
            for (Stage stage : List.of(Stage.WITHDRAW_INTENT, Stage.WITHDRAWN, Stage.TRANSFER_INTENT,
                    Stage.REFUND_INTENT, Stage.REFUNDED)) journal.record(quote, stage, "test");
        }
        try (FileJournal reopened = new FileJournal(directory)) { assertFalse(reopened.requiresRecovery()); }
    }

    @Test void sellerDeclineRollbackRefundClosesCleanly() throws Exception {
        try (FileJournal journal = new FileJournal(directory)) {
            Quote quote = quote();
            for (Stage stage : List.of(Stage.WITHDRAW_INTENT, Stage.WITHDRAWN, Stage.TRANSFER_INTENT,
                    Stage.TRANSFER_APPLIED, Stage.PAYOUT_INTENT, Stage.PAYOUT_DECLINED,
                    Stage.ROLLBACK_INTENT, Stage.ROLLED_BACK, Stage.REFUND_INTENT, Stage.REFUNDED)) {
                journal.record(quote, stage, "test");
            }
        }
        try (FileJournal reopened = new FileJournal(directory)) { assertFalse(reopened.requiresRecovery()); }
    }

    @Test void serverShopWithoutSellerSupportsCommit() throws Exception {
        Quote original = quote();
        Quote quote = new Quote(original.id(), original.buyerId(), null, original.offerId(), original.revision(),
                original.quantity(), original.amountMinorUnits(), original.fingerprint(), original.expiresAt());
        try (FileJournal journal = new FileJournal(directory)) {
            for (Stage stage : List.of(Stage.WITHDRAW_INTENT, Stage.WITHDRAWN, Stage.TRANSFER_INTENT,
                    Stage.TRANSFER_APPLIED, Stage.COMMITTED)) journal.record(quote, stage, "test");
        }
        assertEquals("-", Files.readAllLines(directory.resolve(FileJournal.LOG_FILE)).getFirst().split("\t")[3]);
        try (FileJournal reopened = new FileJournal(directory)) { assertFalse(reopened.requiresRecovery()); }
    }

    @Test void explicitRecoveryPersistsEvenWithoutSessionMarker() throws Exception {
        try (FileJournal journal = new FileJournal(directory)) {
            journal.record(quote(), Stage.RECOVERY_REQUIRED, "unknown withdrawal");
            assertTrue(journal.requiresRecovery());
        }
        Files.delete(directory.resolve(FileJournal.SESSION_FILE));
        try (FileJournal reopened = new FileJournal(directory)) {
            assertTrue(reopened.requiresRecovery());
            assertEquals("unknown withdrawal", reopened.recoveryReason());
        }
    }

    @Test void anotherProcessOrPluginInstanceCannotOpenSameJournal() throws Exception {
        try (FileJournal journal = new FileJournal(directory)) {
            assertThrows(IOException.class, () -> new FileJournal(directory));
            assertFalse(journal.requiresRecovery());
        }
        try (FileJournal reopened = new FileJournal(directory)) { assertFalse(reopened.requiresRecovery()); }
    }

    @Test void invalidTransitionFailsClosedInsteadOfLoggingImpossibleSuccess() throws Exception {
        try (FileJournal journal = new FileJournal(directory)) {
            assertThrows(IOException.class, () -> journal.record(quote(), Stage.COMMITTED, "no debit"));
            assertTrue(journal.requiresRecovery());
        }
        assertTrue(Files.exists(directory.resolve(FileJournal.SESSION_FILE)));
    }

    @Test void terminalTransactionCannotBeChargedAgain() throws Exception {
        try (FileJournal journal = new FileJournal(directory)) {
            Quote quote = quote();
            commit(journal, quote, "evidence");
            assertThrows(IOException.class, () -> journal.record(quote, Stage.WITHDRAW_INTENT, "replay"));
            assertTrue(journal.requiresRecovery());
        }
    }

    @ParameterizedTest @ValueSource(strings = {"truncated", "checksum", "garbage", "newline"})
    void corruptedOrTruncatedJournalFailsClosed(String corruption) throws Exception {
        try (FileJournal journal = new FileJournal(directory)) { commit(journal, quote(), "evidence"); }
        Path file = directory.resolve(FileJournal.LOG_FILE);
        String original = Files.readString(file);
        String broken = switch (corruption) {
            case "truncated" -> original + "1\tpartial";
            case "checksum" -> original.replaceFirst("WITHDRAW_INTENT", "WITHDRAW_1NTENT");
            case "garbage" -> "garbage\n";
            case "newline" -> original.substring(0, original.length() - 1);
            default -> throw new AssertionError();
        };
        Files.writeString(file, broken);
        try (FileJournal reopened = new FileJournal(directory)) {
            assertTrue(reopened.requiresRecovery());
            assertTrue(reopened.recoveryReason().contains("corrupt"));
        }
    }

    @Test void evidenceNearOneMiBLimitCanBeStoredAndReadBack() throws Exception {
        String evidence = "x".repeat(MAX_EVIDENCE_BYTES);
        try (FileJournal journal = new FileJournal(directory)) { commit(journal, quote(), evidence); }
        try (FileJournal reopened = new FileJournal(directory)) { assertFalse(reopened.requiresRecovery()); }
    }

    @Test void evidenceBeyondRecordLimitFailsBeforeAppending() throws Exception {
        try (FileJournal journal = new FileJournal(directory)) {
            assertThrows(IOException.class, () -> journal.record(quote(), Stage.WITHDRAW_INTENT, "x".repeat(MAX_EVIDENCE_BYTES + 1025)));
            assertEquals(0, Files.size(directory.resolve(FileJournal.LOG_FILE)));
            assertTrue(journal.requiresRecovery());
        }
    }

    @Test void oversizedJournalLocksWithoutReadingUnboundedHistory() throws Exception {
        Path file = directory.resolve(FileJournal.LOG_FILE);
        try (RandomAccessFile sparse = new RandomAccessFile(file.toFile(), "rw")) {
            sparse.setLength(FileJournal.MAX_JOURNAL_BYTES + 1);
        }
        try (FileJournal journal = new FileJournal(directory)) {
            assertTrue(journal.requiresRecovery());
            assertTrue(journal.recoveryReason().contains("size limit"));
        }
    }

    @Test void closedJournalCannotAppendAndCloseIsIdempotent() throws Exception {
        FileJournal journal = new FileJournal(directory);
        journal.close();
        journal.close();
        journal.closeUnclean();
        assertThrows(IOException.class, () -> journal.record(quote(), Stage.WITHDRAW_INTENT, "closed"));
        assertFalse(Files.exists(directory.resolve(FileJournal.SESSION_FILE)));
    }

    private static void commit(FileJournal journal, Quote quote, String evidence) throws Exception {
        for (Stage stage : List.of(Stage.WITHDRAW_INTENT, Stage.WITHDRAWN, Stage.TRANSFER_INTENT,
                Stage.TRANSFER_APPLIED, Stage.PAYOUT_INTENT, Stage.PAID, Stage.COMMITTED)) {
            journal.record(quote, stage, stage == Stage.WITHDRAW_INTENT ? evidence : "test");
        }
    }
}

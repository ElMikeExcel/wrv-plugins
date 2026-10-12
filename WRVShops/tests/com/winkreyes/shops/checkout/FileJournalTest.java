// SPDX-License-Identifier: MIT
package com.winkreyes.shops.checkout;

import static org.junit.jupiter.api.Assertions.*;
import static com.winkreyes.shops.checkout.CheckoutService.*;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.concurrent.TimeUnit;
import com.winkreyes.shops.LegacyDataMigration;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.CsvSource;
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

    @ParameterizedTest @EnumSource(value = Stage.class, names = {"LEGACY_COMMITTED", "LEGACY_NO_CHANGE"})
    void legacyPaidAndFreeTerminalTransactionsCloseAndReopenCleanly(Stage terminal) throws Exception {
        try (FileJournal journal = new FileJournal(directory)) {
            for (long amount : List.of(0L, 1L, 500L, Money.MAX_MINOR_UNITS)) {
                Quote quote = quote(amount);
                journal.record(quote, Stage.LEGACY_INTENT, "external inventories and accounts before commit");
                journal.record(quote, terminal, "verified external outcome");
            }
        }
        assertFalse(Files.exists(directory.resolve(FileJournal.SESSION_FILE)));
        List<String> lines = Files.readAllLines(directory.resolve(FileJournal.LOG_FILE));
        assertEquals(8, lines.size());
        assertEquals("0", lines.getFirst().split("\t")[7]);
        assertEquals("LEGACY_INTENT", lines.getFirst().split("\t")[10]);
        try (FileJournal reopened = new FileJournal(directory)) { assertFalse(reopened.requiresRecovery()); }
    }

    @ParameterizedTest @ValueSource(longs = {0, 500})
    void unfinishedLegacyIntentLocksRestartEvenWithoutSessionMarker(long amount) throws Exception {
        try (FileJournal journal = new FileJournal(directory)) {
            journal.record(quote(amount), Stage.LEGACY_INTENT, "before any external side effect");
        }
        assertTrue(Files.exists(directory.resolve(FileJournal.SESSION_FILE)));
        Files.delete(directory.resolve(FileJournal.SESSION_FILE));
        try (FileJournal reopened = new FileJournal(directory)) {
            assertTrue(reopened.requiresRecovery());
            assertTrue(reopened.recoveryReason().contains("incomplete"));
            assertThrows(IOException.class, () -> reopened.record(quote(), Stage.LEGACY_INTENT, "blocked"));
            assertThrows(IOException.class, () -> reopened.record(quote(), Stage.WITHDRAW_INTENT, "blocked"));
        }
    }

    @ParameterizedTest @ValueSource(longs = {0, 500})
    void legacyRecoveryPreservesFlowAndReasonAfterRestart(long amount) throws Exception {
        try (FileJournal journal = new FileJournal(directory)) {
            Quote quote = quote(amount);
            journal.record(quote, Stage.LEGACY_INTENT, "before external effects");
            journal.record(quote, Stage.RECOVERY_REQUIRED, "external result could not be established");
        }
        Files.delete(directory.resolve(FileJournal.SESSION_FILE));
        try (FileJournal reopened = new FileJournal(directory)) {
            assertTrue(reopened.requiresRecovery());
            assertEquals("external result could not be established", reopened.recoveryReason());
            assertThrows(IOException.class, () -> reopened.record(quote(), Stage.LEGACY_INTENT, "blocked"));
        }
    }

    @ParameterizedTest @EnumSource(value = Stage.class, names = {"LEGACY_COMMITTED", "LEGACY_NO_CHANGE"})
    void completedLegacyTransactionStillRequiresDurableShutdown(Stage terminal) throws Exception {
        FileJournal journal = new FileJournal(directory);
        Quote quote = quote(0);
        journal.record(quote, Stage.LEGACY_INTENT, "before");
        journal.record(quote, terminal, "after");
        journal.closeUnclean();
        try (FileJournal reopened = new FileJournal(directory)) { assertTrue(reopened.requiresRecovery()); }
    }

    @ParameterizedTest @EnumSource(value = Stage.class, names = {"LEGACY_COMMITTED", "LEGACY_NO_CHANGE"})
    void legacyTerminalWithoutIntentIsRejectedBeforeAppending(Stage terminal) throws Exception {
        try (FileJournal journal = new FileJournal(directory)) {
            assertThrows(IOException.class, () -> journal.record(quote(), terminal, "no intent"));
            assertEquals(0, Files.size(directory.resolve(FileJournal.LOG_FILE)));
            assertTrue(journal.requiresRecovery());
        }
    }

    @ParameterizedTest @CsvSource({
            "LEGACY_INTENT, WITHDRAW_INTENT", "LEGACY_INTENT, WITHDRAWN", "LEGACY_INTENT, NO_CHARGE",
            "LEGACY_INTENT, TRANSFER_INTENT", "LEGACY_INTENT, REFUNDED", "LEGACY_INTENT, COMMITTED",
            "LEGACY_INTENT, LEGACY_INTENT", "WITHDRAW_INTENT, LEGACY_INTENT",
            "WITHDRAW_INTENT, LEGACY_COMMITTED", "WITHDRAW_INTENT, LEGACY_NO_CHANGE",
            "RECOVERY_REQUIRED, LEGACY_INTENT", "RECOVERY_REQUIRED, LEGACY_COMMITTED"})
    void nativeAndLegacyStagesCannotCrossFlows(Stage first, Stage next) throws Exception {
        try (FileJournal journal = new FileJournal(directory)) {
            Quote quote = quote();
            journal.record(quote, first, "valid first stage");
            long priorSize = Files.size(directory.resolve(FileJournal.LOG_FILE));
            assertThrows(IOException.class, () -> journal.record(quote, next, "cross-flow attempt"));
            assertEquals(priorSize, Files.size(directory.resolve(FileJournal.LOG_FILE)));
            assertTrue(journal.requiresRecovery());
        }
    }

    @ParameterizedTest @EnumSource(value = Stage.class, names = {"LEGACY_COMMITTED", "LEGACY_NO_CHANGE", "RECOVERY_REQUIRED"})
    void legacyOutcomeIsSingleTransitionAndCannotBeRewritten(Stage terminal) throws Exception {
        try (FileJournal journal = new FileJournal(directory)) {
            Quote quote = quote(0);
            journal.record(quote, Stage.LEGACY_INTENT, "before");
            journal.record(quote, terminal, "verified or ambiguous outcome");
            long priorSize = Files.size(directory.resolve(FileJournal.LOG_FILE));
            assertThrows(IOException.class, () -> journal.record(quote, Stage.RECOVERY_REQUIRED, "rewrite outcome"));
            assertEquals(priorSize, Files.size(directory.resolve(FileJournal.LOG_FILE)));
            assertTrue(journal.requiresRecovery());
        }
    }

    @ParameterizedTest @CsvSource({
            "WITHDRAW_INTENT, 0", "RECOVERY_REQUIRED, 0", "WITHDRAW_INTENT, -1", "LEGACY_INTENT, -1",
            "WITHDRAW_INTENT, 10000000001", "LEGACY_INTENT, 10000000001",
            "WITHDRAW_INTENT, 9223372036854775807", "LEGACY_INTENT, 9223372036854775807",
            "WITHDRAW_INTENT, -9223372036854775808", "LEGACY_INTENT, -9223372036854775808"})
    void negativeOverflowOrNonLegacyZeroAmountsCannotBeAppended(Stage initial, long amount) throws Exception {
        try (FileJournal journal = new FileJournal(directory)) {
            assertThrows(IOException.class, () -> journal.record(quote(amount), initial, "invalid amount"));
            assertEquals(0, Files.size(directory.resolve(FileJournal.LOG_FILE)));
            assertTrue(journal.requiresRecovery());
        }
    }

    @ParameterizedTest @ValueSource(strings = {"buyer", "offer", "revision", "quantity-zero", "quantity-overflow", "fingerprint", "expiry"})
    void invalidLegacyQuoteMetadataFailsBeforeAppending(String invalid) throws Exception {
        Quote valid = quote(0);
        Quote invalidQuote = new Quote(valid.id(), invalid.equals("buyer") ? null : valid.buyerId(), valid.sellerId(),
                invalid.equals("offer") ? " " : valid.offerId(), invalid.equals("revision") ? -1 : valid.revision(),
                invalid.equals("quantity-zero") ? 0 : invalid.equals("quantity-overflow") ? Integer.MAX_VALUE : valid.quantity(),
                valid.amountMinorUnits(), invalid.equals("fingerprint") ? "" : valid.fingerprint(),
                invalid.equals("expiry") ? null : valid.expiresAt());
        try (FileJournal journal = new FileJournal(directory)) {
            assertThrows(IOException.class, () -> journal.record(invalidQuote, Stage.LEGACY_INTENT, "invalid metadata"));
            assertEquals(0, Files.size(directory.resolve(FileJournal.LOG_FILE)));
        }
    }

    @ParameterizedTest @CsvSource({
            "WITHDRAW_INTENT, 0", "RECOVERY_REQUIRED, 0", "LEGACY_COMMITTED, 0", "LEGACY_NO_CHANGE, 0",
            "LEGACY_INTENT, -1", "LEGACY_INTENT, 10000000001", "LEGACY_INTENT, 9223372036854775808",
            "WITHDRAW_INTENT, -1", "WITHDRAW_INTENT, 10000000001"})
    void checksummedHistoryStillRejectsInvalidAmountsAndMissingLegacyIntent(Stage stage, String amount) throws Exception {
        writeHistory(amount, stage);
        try (FileJournal journal = new FileJournal(directory)) {
            assertTrue(journal.requiresRecovery());
            assertTrue(journal.recoveryReason().contains("corrupt"));
        }
    }

    @ParameterizedTest @CsvSource({
            "WITHDRAW_INTENT, LEGACY_COMMITTED", "WITHDRAW_INTENT, LEGACY_INTENT",
            "LEGACY_INTENT, COMMITTED", "LEGACY_INTENT, NO_CHARGE", "LEGACY_INTENT, WITHDRAW_INTENT"})
    void checksummedCrossFlowHistoryCannotFakeCompletion(Stage first, Stage second) throws Exception {
        writeHistory("500", first, second);
        try (FileJournal journal = new FileJournal(directory)) {
            assertTrue(journal.requiresRecovery());
            assertTrue(journal.recoveryReason().contains("corrupt"));
        }
    }

    @Test void existingVersionOneNativeRecoveryHistoryRemainsReadable() throws Exception {
        writeHistory("500", Stage.RECOVERY_REQUIRED, Stage.RECOVERY_REQUIRED);
        try (FileJournal journal = new FileJournal(directory)) {
            assertTrue(journal.requiresRecovery());
            assertEquals("historical evidence", journal.recoveryReason());
        }
    }

    private Quote quote(long amount) {
        Quote quote = quote();
        return new Quote(quote.id(), quote.buyerId(), quote.sellerId(), quote.offerId(), quote.revision(),
                quote.quantity(), amount, quote.fingerprint(), quote.expiresAt());
    }

    private void writeHistory(String amount, Stage... stages) throws Exception {
        Quote quote = quote();
        StringBuilder history = new StringBuilder();
        for (Stage stage : stages) {
            String body = String.join("\t", "1", quote.id().toString(), quote.buyerId().toString(),
                    quote.sellerId().toString(), encoded(quote.offerId()), Long.toString(quote.revision()),
                    Integer.toString(quote.quantity()), amount, encoded(quote.fingerprint()),
                    quote.expiresAt().toString(), stage.name(), Instant.now().toString(), encoded("historical evidence"));
            String checksum = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(body.getBytes(StandardCharsets.UTF_8)));
            history.append(body).append('\t').append(checksum).append('\n');
        }
        Files.writeString(directory.resolve(FileJournal.LOG_FILE), history);
    }

    private static String encoded(String text) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(text.getBytes(StandardCharsets.UTF_8));
    }

    @Test void anotherProcessOrPluginInstanceCannotOpenSameJournal() throws Exception {
        try (FileJournal journal = new FileJournal(directory)) {
            assertThrows(IOException.class, () -> new FileJournal(directory));
            assertFalse(journal.requiresRecovery());
        }
        try (FileJournal reopened = new FileJournal(directory)) { assertFalse(reopened.requiresRecovery()); }
    }

    @Test void realOtherProcessRemainsExcludedAfterEmptyAndPopulatedJournalParsing() throws Exception {
        try (FileJournal journal = new FileJournal(directory)) {
            assertEquals(13, probe(directory.resolve(FileJournal.LOG_FILE)));
            commit(journal, quote(), "durable evidence");
            assertEquals(13, probe(directory.resolve(FileJournal.LOG_FILE)));
        }
        assertEquals(0, probe(directory.resolve(FileJournal.LOG_FILE)));
        try (FileJournal journal = new FileJournal(directory)) {
            assertFalse(journal.requiresRecovery());
            assertEquals(13, probe(directory.resolve(FileJournal.LOG_FILE)));
            commit(journal, quote(), "more durable evidence after parsing");
            assertEquals(13, probe(directory.resolve(FileJournal.LOG_FILE)));
        }
    }

    @Test void failedSameJvmDuplicateDoesNotReleaseOriginalPosixLock() throws Exception {
        try (FileJournal journal = new FileJournal(directory)) {
            assertThrows(IOException.class, () -> new FileJournal(directory));
            assertEquals(13, probe(directory.resolve(FileJournal.LOG_FILE)));
            commit(journal, quote(), "owner remains usable");
        }
        assertEquals(0, probe(directory.resolve(FileJournal.LOG_FILE)));
    }

    @Test void migrationCannotDropSameJvmLegacyJournalLock() throws Exception {
        Path source = directory.resolve("WRVShops");
        Path target = directory.resolve("ExcelShops");
        try (FileJournal journal = new FileJournal(source.resolve("checkout"))) {
            assertThrows(IOException.class, () -> LegacyDataMigration.open(source, target, false));
            assertEquals(13, probe(source.resolve("checkout/transactions.log")));
            assertFalse(Files.exists(target));
        }
    }

    @Test void journalCannotDropMigrationOwnedLegacyLockOrCreateSessionMarker() throws Exception {
        Path source = directory.resolve("WRVShops");
        Path target = directory.resolve("ExcelShops");
        try (FileJournal ignored = new FileJournal(source.resolve("checkout"))) { }
        try (var migration = LegacyDataMigration.open(source, target, false)) {
            assertThrows(IOException.class, () -> new FileJournal(source.resolve("checkout")));
            assertEquals(13, probe(source.resolve("checkout/transactions.log")));
            assertFalse(Files.exists(source.resolve("checkout/active-session")));
        }
        assertEquals(0, probe(source.resolve("checkout/transactions.log")));
    }

    @Test void targetSnapshotCannotDropAlreadyOpenSameJvmExcelJournalLock() throws Exception {
        Path source = directory.resolve("WRVShops");
        Path target = directory.resolve("ExcelShops");
        try (FileJournal journal = new FileJournal(target.resolve("checkout"))) {
            assertThrows(IOException.class, () -> LegacyDataMigration.open(source, target, false));
            assertEquals(13, probe(target.resolve("checkout/transactions.log")));
            commit(journal, quote(), "target still owns lock");
        }
    }

    @Test void malformedUtf8StillRejectsStartupAndReleasesReservation() throws Exception {
        Path log = directory.resolve(FileJournal.LOG_FILE);
        Files.write(log, new byte[]{(byte) 0xc3, 0x28, 0x0a});
        assertThrows(IOException.class, () -> new FileJournal(directory));
        assertEquals(0, probe(log));
        Files.writeString(log, "");
        try (FileJournal journal = new FileJournal(directory)) { assertFalse(journal.requiresRecovery()); }
    }

    @Test void hardlinkedJournalIsRejectedBeforeAnotherDescriptorCanReleaseAnyLock() throws Exception {
        Path log = Files.writeString(directory.resolve(FileJournal.LOG_FILE), "");
        Files.createLink(directory.resolve("log-alias"), log);
        assertThrows(IOException.class, () -> new FileJournal(directory));
        assertFalse(Files.exists(directory.resolve(FileJournal.SESSION_FILE)));
    }

    @Test void failedOrdinaryReaderCannotDropOwnedJournalLock() throws Exception {
        try (FileJournal ignored = new FileJournal(directory)) {
            assertThrows(IOException.class, () -> DataFileLock.openReadOnly(directory.resolve(FileJournal.LOG_FILE)));
            assertEquals(13, probe(directory.resolve(FileJournal.LOG_FILE)));
        }
    }

    @Test void ordinaryReaderReservationPreventsNewJournalOwnerUntilClosed() throws Exception {
        Path log = Files.writeString(directory.resolve(FileJournal.LOG_FILE), "");
        try (var reader = DataFileLock.openReadOnly(log)) {
            assertThrows(IOException.class, () -> new FileJournal(directory));
        }
        try (FileJournal journal = new FileJournal(directory)) {
            assertFalse(journal.requiresRecovery());
            assertEquals(13, probe(log));
        }
    }

    private int probe(Path log) throws Exception {
        String classPath = System.getProperty("surefire.test.class.path", System.getProperty("java.class.path"));
        Process process = new ProcessBuilder(Path.of(System.getProperty("java.home"), "bin", "java").toString(),
                "-cp", classPath, LockProbe.class.getName(), log.toString()).redirectErrorStream(true).start();
        try {
            assertTrue(process.waitFor(15, TimeUnit.SECONDS), "Lock probe timed out");
            int exit = process.exitValue();
            assertTrue(exit == 0 || exit == 13, () -> {
                try { return new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8); }
                catch (IOException failure) { return failure.toString(); }
            });
            return exit;
        } finally { process.destroyForcibly(); }
    }

    public static final class LockProbe {
        public static void main(String[] args) throws Exception {
            try (FileChannel channel = FileChannel.open(Path.of(args[0]), StandardOpenOption.READ, StandardOpenOption.WRITE)) {
                try (FileLock lock = channel.tryLock()) { if (lock == null) System.exit(13); }
            }
        }
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

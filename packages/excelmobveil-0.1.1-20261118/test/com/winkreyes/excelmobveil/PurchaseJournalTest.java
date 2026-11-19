package com.winkreyes.excelmobveil;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static com.winkreyes.excelmobveil.PurchaseJournal.Phase.*;

class PurchaseJournalTest {
    @TempDir Path directory;
    final UUID transaction = UUID.randomUUID();
    final UUID player = UUID.randomUUID();

    PurchaseJournal.Entry entry(PurchaseJournal.Phase phase) {
        return new PurchaseJournal.Entry(transaction, player, PurchaseCoordinator.Product.VEIL,
                2_000, phase, 123);
    }

    @Test void forcedAppendRoundTripsAndPreservesUnfinishedIntent() throws Exception {
        Path path = directory.resolve("journal");
        PurchaseJournal journal = new PurchaseJournal(path);
        journal.append(entry(WITHDRAW_INTENT));
        PurchaseJournal reopened = new PurchaseJournal(path);
        assertEquals(entry(WITHDRAW_INTENT), reopened.unresolved().get(transaction));
        journal.append(entry(WITHDRAWN));
        assertEquals(entry(WITHDRAWN), new PurchaseJournal(path).unresolved().get(transaction));
    }

    @Test void everyNonterminalPhaseRemainsUnresolvedAfterRestart() throws Exception {
        Path path = directory.resolve("journal");
        PurchaseJournal journal = new PurchaseJournal(path);
        for (PurchaseJournal.Phase phase : new PurchaseJournal.Phase[]{WITHDRAW_INTENT, WITHDRAWN, REFUND_INTENT, RECONCILE}) {
            journal.append(entry(phase));
            assertEquals(phase, new PurchaseJournal(path).unresolved().get(transaction).phase());
        }
    }

    @Test void completionRemovesPendingButPreservesAudit() throws Exception {
        Path path = directory.resolve("journal");
        PurchaseJournal journal = new PurchaseJournal(path);
        journal.append(entry(WITHDRAW_INTENT));
        journal.append(entry(WITHDRAWN));
        journal.append(entry(COMPLETED));
        PurchaseJournal reopened = new PurchaseJournal(path);
        assertTrue(reopened.unresolved().isEmpty());
        assertEquals(entry(COMPLETED), reopened.entries().get(transaction));
        assertEquals(3, Files.readAllLines(path).size());
    }

    @Test void refundCompletionAndDeclineAreTerminal() throws Exception {
        PurchaseJournal refund = new PurchaseJournal(directory.resolve("refund"));
        refund.append(entry(WITHDRAW_INTENT));
        refund.append(entry(WITHDRAWN));
        refund.append(entry(REFUND_INTENT));
        refund.append(entry(REFUNDED));
        assertTrue(new PurchaseJournal(directory.resolve("refund")).unresolved().isEmpty());
        PurchaseJournal decline = new PurchaseJournal(directory.resolve("decline"));
        decline.append(entry(WITHDRAW_INTENT));
        decline.append(entry(DECLINED));
        assertTrue(new PurchaseJournal(directory.resolve("decline")).unresolved().isEmpty());
    }

    @Test void truncatedTailFailsClosed() throws Exception {
        Path path = directory.resolve("journal");
        PurchaseJournal journal = new PurchaseJournal(path);
        journal.append(entry(WITHDRAW_INTENT));
        String contents = Files.readString(path);
        Files.writeString(path, contents.substring(0, contents.length() - 1));
        assertThrows(IOException.class, () -> new PurchaseJournal(path));
    }

    @Test void checksumCorruptionFailsClosed() throws Exception {
        Path path = directory.resolve("journal");
        PurchaseJournal journal = new PurchaseJournal(path);
        journal.append(entry(WITHDRAW_INTENT));
        Files.writeString(path, Files.readString(path).replace("VEIL", "VEIX"));
        assertThrows(IOException.class, () -> new PurchaseJournal(path));
    }

    @Test void illegalTransitionPoisonsJournalAndBlocksFurtherWrites() throws Exception {
        PurchaseJournal journal = new PurchaseJournal(directory.resolve("journal"));
        assertThrows(IOException.class, () -> journal.append(entry(COMPLETED)));
        assertFalse(journal.healthy());
        assertThrows(IOException.class, () -> journal.append(entry(WITHDRAW_INTENT)));
    }

    @Test void playerOrPriceCannotChangeWithinTransaction() throws Exception {
        PurchaseJournal journal = new PurchaseJournal(directory.resolve("journal"));
        journal.append(entry(WITHDRAW_INTENT));
        assertThrows(IOException.class, () -> journal.append(new PurchaseJournal.Entry(transaction,
                UUID.randomUUID(), PurchaseCoordinator.Product.VEIL, 2_000, WITHDRAWN, 123)));
        PurchaseJournal other = new PurchaseJournal(directory.resolve("other"));
        other.append(entry(WITHDRAW_INTENT));
        assertThrows(IOException.class, () -> other.append(new PurchaseJournal.Entry(transaction,
                player, PurchaseCoordinator.Product.VEIL, 2_500, WITHDRAWN, 123)));
    }

    @Test void terminalTransactionsCannotBeReplayed() throws Exception {
        PurchaseJournal journal = new PurchaseJournal(directory.resolve("journal"));
        journal.append(entry(WITHDRAW_INTENT));
        journal.append(entry(DECLINED));
        assertThrows(IOException.class, () -> journal.append(entry(WITHDRAW_INTENT)));
    }

    @Test void immutableSnapshotsDoNotChangeAfterAppend() throws Exception {
        PurchaseJournal journal = new PurchaseJournal(directory.resolve("journal"));
        var snapshot = journal.entries();
        journal.append(entry(WITHDRAW_INTENT));
        assertTrue(snapshot.isEmpty());
        assertThrows(UnsupportedOperationException.class, () -> snapshot.put(transaction, entry(COMPLETED)));
    }

    @Test void diskWriteFailurePoisonsJournal() throws Exception {
        Path path = directory.resolve("journal");
        PurchaseJournal journal = new PurchaseJournal(path);
        Files.delete(path);
        Files.createDirectory(path);
        assertThrows(IOException.class, () -> journal.append(entry(WITHDRAW_INTENT)));
        assertFalse(journal.healthy());
    }
}

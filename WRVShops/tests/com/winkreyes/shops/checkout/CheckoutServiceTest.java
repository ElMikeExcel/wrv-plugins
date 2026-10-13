// SPDX-License-Identifier: MIT
package com.winkreyes.shops.checkout;

import static org.junit.jupiter.api.Assertions.*;
import static com.winkreyes.shops.checkout.CheckoutService.*;
import com.winkreyes.shops.checkout.CheckoutService.ExternalLease;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

class CheckoutServiceTest {
    private final UUID buyer = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private final UUID seller = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private MutableClock clock;
    private TestJournal journal;
    private TestAdapter adapter;
    private CheckoutService service;

    @BeforeEach void setup() {
        clock = new MutableClock();
        journal = new TestJournal();
        adapter = new TestAdapter();
        service = new CheckoutService(adapter, journal, clock, Duration.ofSeconds(30));
    }

    private Quote quote() { return service.quote(buyer, seller, "chest:abc:item", 7, 4, 500, "metadata-and-stock-v1"); }
    private Result buy() { return service.confirm(quote().id(), buyer); }

    @Test void quoteCancelAndNavigationNeverMutateMoneyStockOrJournal() {
        Quote first = quote();
        assertTrue(service.cancel(first.id(), buyer));
        assertEquals(Status.NO_CHARGE, service.confirm(first.id(), buyer).status());
        Quote second = quote();
        service.invalidateBuyer(buyer);
        assertEquals(Status.NO_CHARGE, service.confirm(second.id(), buyer).status());
        Quote third = quote();
        service.invalidateAll();
        assertEquals(Status.NO_CHARGE, service.confirm(third.id(), buyer).status());
        assertEquals(5000, adapter.buyerFunds);
        assertEquals(0, adapter.withdrawals);
        assertEquals(32, adapter.stock);
        assertEquals(0, adapter.prepares);
        assertTrue(journal.stages.isEmpty());
    }

    @Test void successIsSingleUseAndDurableIntentsPrecedeEveryMutation() {
        Quote quote = quote();
        assertEquals(Status.SUCCESS, service.confirm(quote.id(), buyer).status());
        assertEquals(Status.NO_CHARGE, service.confirm(quote.id(), buyer).status());
        assertEquals(4500, adapter.buyerFunds);
        assertEquals(500, adapter.sellerFunds);
        assertEquals(28, adapter.stock);
        assertEquals(4, adapter.delivered);
        assertEquals(1, adapter.withdrawals);
        assertEquals(1, adapter.payouts);
        assertEquals(List.of(Stage.WITHDRAW_INTENT, Stage.WITHDRAWN, Stage.TRANSFER_INTENT,
                Stage.TRANSFER_APPLIED, Stage.PAYOUT_INTENT, Stage.PAID, Stage.COMMITTED), journal.stages);
        assertFalse(service.recoveryRequired());
    }

    @Test void twoBuyersCompetingForLastStockCannotBothPurchase() {
        adapter.stock = 4;
        Quote first = quote();
        UUID otherBuyer = UUID.randomUUID();
        Quote second = service.quote(otherBuyer, seller, "chest:abc:item", 7, 4, 500, "metadata-and-stock-v1");
        assertEquals(Status.SUCCESS, service.confirm(first.id(), buyer).status());
        assertEquals(Status.NO_CHARGE, service.confirm(second.id(), otherBuyer).status());
        assertEquals(0, adapter.stock);
        assertEquals(4, adapter.delivered);
        assertEquals(1, adapter.withdrawals);
    }

    @Test void concurrentConfirmationsAreSerializedEvenBeforePrimaryThreadAdapterGuard() throws Exception {
        Quote quote = quote();
        try (java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newFixedThreadPool(2)) {
            java.util.concurrent.CountDownLatch start = new java.util.concurrent.CountDownLatch(1);
            java.util.concurrent.Callable<Status> attempt = () -> {
                start.await();
                return service.confirm(quote.id(), buyer).status();
            };
            var first = executor.submit(attempt);
            var second = executor.submit(attempt);
            start.countDown();
            List<Status> results = List.of(first.get(), second.get());
            assertEquals(1, Collections.frequency(results, Status.SUCCESS));
            assertEquals(1, Collections.frequency(results, Status.NO_CHARGE));
            assertEquals(1, adapter.withdrawals);
        }
    }

    @Test void serverShopDoesNotCreditAnySeller() {
        adapter.expectedSeller = null;
        Quote quote = service.quote(buyer, null, "chest:abc:item", 7, 4, 500, "metadata-and-stock-v1");
        assertEquals(Status.SUCCESS, service.confirm(quote.id(), buyer).status());
        assertEquals(0, adapter.payouts);
        assertFalse(journal.stages.contains(Stage.PAYOUT_INTENT));
        assertEquals(Stage.COMMITTED, journal.last());
    }

    @Test void wrongBuyerCannotConsumeOrCancelARealBuyersQuote() {
        Quote quote = quote();
        assertEquals(Status.NO_CHARGE, service.confirm(quote.id(), seller).status());
        assertFalse(service.cancel(quote.id(), seller));
        assertEquals(0, adapter.withdrawals);
        assertEquals(Status.SUCCESS, service.confirm(quote.id(), buyer).status());
    }

    @Test void newerQuoteInvalidatesOlderConfirmation() {
        Quote old = quote();
        Quote current = quote();
        assertNotEquals(old.id(), current.id());
        assertEquals(Status.NO_CHARGE, service.confirm(old.id(), buyer).status());
        assertEquals(Status.SUCCESS, service.confirm(current.id(), buyer).status());
        assertEquals(1, adapter.withdrawals);
    }

    @Test void expiryIsExclusiveAtExactDeadlineAndNewQuoteStillWorks() {
        Quote quote = quote();
        clock.now = quote.expiresAt();
        assertEquals(Status.NO_CHARGE, service.confirm(quote.id(), buyer).status());
        assertEquals(0, adapter.withdrawals);
        assertEquals(Status.SUCCESS, buy().status());
    }

    @Test void disconnectInvalidatesOnlyThatBuyersQuote() {
        Quote mine = quote();
        UUID other = UUID.randomUUID();
        Quote otherQuote = service.quote(other, seller, "another-offer", 1, 1, 100, "other");
        service.invalidateBuyer(other);
        assertEquals(Status.NO_CHARGE, service.confirm(otherQuote.id(), other).status());
        assertEquals(Status.SUCCESS, service.confirm(mine.id(), buyer).status());
    }

    @ParameterizedTest @ValueSource(strings = {"owner", "revision", "metadata", "stock", "price", "funds", "capacity", "protection", "offline"})
    void revalidatesEveryBoundaryImmediatelyBeforePayment(String changed) {
        Quote quote = quote();
        switch (changed) {
            case "owner" -> adapter.expectedSeller = UUID.randomUUID();
            case "revision" -> adapter.revision++;
            case "metadata" -> adapter.fingerprint = "different-enchantment-or-stack";
            case "stock" -> adapter.stock = 3;
            case "price" -> adapter.unitPrice++;
            case "funds" -> adapter.buyerFunds = 499;
            case "capacity" -> adapter.capacity = 3;
            case "protection" -> adapter.allowed = false;
            case "offline" -> adapter.online = false;
        }
        assertEquals(Status.NO_CHARGE, service.confirm(quote.id(), buyer).status());
        assertEquals(0, adapter.withdrawals);
        assertTrue(journal.stages.isEmpty());
        assertEquals(0, adapter.delivered);
    }

    @Test void exceptionDuringReadOnlyPreparationDoesNotChargeOrLock() {
        adapter.prepareThrows = true;
        assertEquals(Status.NO_CHARGE, buy().status());
        assertFalse(service.recoveryRequired());
        assertEquals(0, adapter.withdrawals);
        assertTrue(journal.stages.isEmpty());
    }

    @Test void definiteWithdrawalDeclineNeverDeliversOrRefunds() {
        adapter.withdrawResult = MoneyResult.DECLINED;
        Quote quote = quote();
        assertEquals(Status.NO_CHARGE, service.confirm(quote.id(), buyer).status());
        assertEquals(Status.NO_CHARGE, service.confirm(quote.id(), buyer).status());
        assertEquals(1, adapter.withdrawals);
        assertEquals(0, adapter.refunds);
        assertEquals(0, adapter.applies);
        assertEquals(Stage.NO_CHARGE, journal.last());
    }

    @Test void unchangedDeliveryRefundsExactlyOnce() {
        adapter.applyResult = MutationResult.UNCHANGED;
        assertEquals(Status.REFUNDED, buy().status());
        assertEquals(5000, adapter.buyerFunds);
        assertEquals(0, adapter.delivered);
        assertEquals(32, adapter.stock);
        assertEquals(1, adapter.refunds);
        assertEquals(0, adapter.rollbacks);
        assertEquals(Stage.REFUNDED, journal.last());
    }

    @Test void stockChangeReentrantlyDuringWithdrawalDoesNotOverwriteOrDeliver() {
        adapter.afterWithdraw = () -> adapter.stock--;
        assertEquals(Status.REFUNDED, buy().status());
        assertEquals(31, adapter.stock);
        assertEquals(0, adapter.delivered);
        assertEquals(5000, adapter.buyerFunds);
    }

    @Test void sellerDeclineRestoresBothSnapshotsThenRefunds() {
        adapter.payoutResult = MoneyResult.DECLINED;
        assertEquals(Status.REFUNDED, buy().status());
        assertEquals(5000, adapter.buyerFunds);
        assertEquals(0, adapter.sellerFunds);
        assertEquals(32, adapter.stock);
        assertEquals(0, adapter.delivered);
        assertEquals(1, adapter.rollbacks);
        assertEquals(1, adapter.refunds);
        assertEquals(List.of(Stage.WITHDRAW_INTENT, Stage.WITHDRAWN, Stage.TRANSFER_INTENT,
                Stage.TRANSFER_APPLIED, Stage.PAYOUT_INTENT, Stage.PAYOUT_DECLINED,
                Stage.ROLLBACK_INTENT, Stage.ROLLED_BACK, Stage.REFUND_INTENT, Stage.REFUNDED), journal.stages);
    }

    @ParameterizedTest @ValueSource(strings = {"withdraw-exception", "withdraw-uncertain", "withdraw-null", "transfer-exception", "transfer-uncertain", "transfer-null", "payout-exception", "payout-uncertain", "payout-null"})
    void ambiguousOutcomesHaltGloballyAndNeverRefundOrRetry(String failure) {
        switch (failure) {
            case "withdraw-exception" -> adapter.withdrawThrows = true;
            case "withdraw-uncertain" -> adapter.withdrawResult = MoneyResult.UNCERTAIN;
            case "withdraw-null" -> adapter.withdrawResult = null;
            case "transfer-exception" -> adapter.applyThrows = true;
            case "transfer-uncertain" -> adapter.applyResult = MutationResult.UNCERTAIN;
            case "transfer-null" -> adapter.applyResult = null;
            case "payout-exception" -> adapter.payoutThrows = true;
            case "payout-uncertain" -> adapter.payoutResult = MoneyResult.UNCERTAIN;
            case "payout-null" -> adapter.payoutResult = null;
        }
        Quote quote = quote();
        assertEquals(Status.RECOVERY_REQUIRED, service.confirm(quote.id(), buyer).status());
        assertTrue(service.recoveryRequired());
        assertEquals(Status.RECOVERY_REQUIRED, service.confirm(quote.id(), buyer).status());
        assertEquals(Status.RECOVERY_REQUIRED, service.confirm(UUID.randomUUID(), UUID.randomUUID()).status());
        assertThrows(IllegalStateException.class, this::quote);
        assertEquals(1, adapter.withdrawals);
        assertEquals(0, adapter.refunds);
        assertEquals(0, adapter.rollbacks);
        assertEquals(Stage.RECOVERY_REQUIRED, journal.last());
    }

    @ParameterizedTest @ValueSource(strings = {"rollback-false", "rollback-exception", "refund-decline", "refund-exception", "refund-uncertain", "refund-null"})
    void compensationFailureIsNeverBlindlyRetried(String failure) {
        adapter.payoutResult = MoneyResult.DECLINED;
        switch (failure) {
            case "rollback-false" -> adapter.rollbackWorks = false;
            case "rollback-exception" -> adapter.rollbackThrows = true;
            case "refund-decline" -> adapter.refundResult = MoneyResult.DECLINED;
            case "refund-exception" -> adapter.refundThrows = true;
            case "refund-uncertain" -> adapter.refundResult = MoneyResult.UNCERTAIN;
            case "refund-null" -> adapter.refundResult = null;
        }
        Quote quote = quote();
        assertEquals(Status.RECOVERY_REQUIRED, service.confirm(quote.id(), buyer).status());
        assertEquals(Status.RECOVERY_REQUIRED, service.confirm(quote.id(), buyer).status());
        assertEquals(1, adapter.rollbacks);
        assertEquals(failure.startsWith("rollback") ? 0 : 1, adapter.refunds);
        assertTrue(service.recoveryRequired());
    }

    @ParameterizedTest @EnumSource(value = Stage.class, names = {"WITHDRAW_INTENT", "WITHDRAWN", "TRANSFER_INTENT", "TRANSFER_APPLIED", "PAYOUT_INTENT", "PAID", "COMMITTED"})
    void journalFailureAtEveryHappyPathBoundaryStopsFurtherEffects(Stage stage) {
        journal.failAt = stage;
        Result result = buy();
        assertEquals(stage == Stage.WITHDRAW_INTENT ? Status.NO_CHARGE : Status.RECOVERY_REQUIRED, result.status());
        assertTrue(service.recoveryRequired());
        assertEquals(0, adapter.refunds);
        assertEquals(stage.ordinal() < Stage.TRANSFER_APPLIED.ordinal() ? 0 : 1, adapter.applies);
        assertEquals(stage == Stage.PAID || stage == Stage.COMMITTED ? 1 : 0, adapter.payouts);
        assertEquals(stage == Stage.WITHDRAW_INTENT ? 0 : 1, adapter.withdrawals);
    }

    @ParameterizedTest @EnumSource(value = Stage.class, names = {"PAYOUT_DECLINED", "ROLLBACK_INTENT", "ROLLED_BACK", "REFUND_INTENT", "REFUNDED"})
    void journalFailureAtEveryCompensationBoundaryDoesNotContinue(Stage stage) {
        adapter.payoutResult = MoneyResult.DECLINED;
        journal.failAt = stage;
        assertEquals(Status.RECOVERY_REQUIRED, buy().status());
        assertEquals(stage == Stage.ROLLED_BACK || stage == Stage.REFUND_INTENT || stage == Stage.REFUNDED ? 1 : 0, adapter.rollbacks);
        assertEquals(stage == Stage.REFUNDED ? 1 : 0, adapter.refunds);
        assertTrue(service.recoveryRequired());
    }

    @Test void failedNoChargeRecordStillTruthfullyReportsNoChargeAndLocks() {
        adapter.withdrawResult = MoneyResult.DECLINED;
        journal.failAt = Stage.NO_CHARGE;
        assertEquals(Status.NO_CHARGE, buy().status());
        assertEquals(5000, adapter.buyerFunds);
        assertTrue(service.recoveryRequired());
    }

    @Test void failedRecoveryRecordRetainsInMemoryFailClosedState() {
        adapter.withdrawThrows = true;
        journal.failAt = Stage.RECOVERY_REQUIRED;
        assertEquals(Status.RECOVERY_REQUIRED, buy().status());
        assertTrue(service.recoveryRequired());
        assertEquals(0, adapter.refunds);
    }

    @Test void missingOrOversizedOrThrowingEvidenceCannotCharge() {
        for (String evidence : Arrays.asList(null, "", "   ", "a".repeat(MAX_EVIDENCE_BYTES + 1), "🎉".repeat(MAX_EVIDENCE_BYTES / 4 + 1))) {
            adapter.evidence = evidence;
            assertEquals(Status.NO_CHARGE, buy().status());
        }
        adapter.evidenceThrows = true;
        assertEquals(Status.NO_CHARGE, buy().status());
        assertEquals(0, adapter.withdrawals);
        assertTrue(journal.stages.isEmpty());
    }

    @Test void evidenceIsJournaledBeforeWithdrawal() {
        adapter.evidence = "before=32;after=28;balance=5000";
        assertEquals(Status.SUCCESS, buy().status());
        assertEquals("Before buyer withdrawal; evidence: " + adapter.evidence, journal.details.getFirst());
    }

    @Test void reentrantConfirmCannotBypassSerialization() {
        Quote first = quote();
        UUID other = UUID.randomUUID();
        Quote second = service.quote(other, seller, "second", 1, 1, 100, "second");
        adapter.afterWithdraw = () -> {
            assertEquals(Status.NO_CHARGE, service.confirm(first.id(), buyer).status());
            assertEquals(Status.NO_CHARGE, service.confirm(second.id(), other).status());
            assertThrows(IllegalStateException.class, () -> service.quote(other, seller, "third", 1, 1, 100, "third"));
            assertNull(service.tryExternalLease());
        };
        assertEquals(Status.SUCCESS, service.confirm(first.id(), buyer).status());
        assertEquals(1, adapter.withdrawals);
    }

    @Test void externalLeaseBlocksNativeAndLegacyAdmissionUntilItsOwnerReleasesIt() {
        Quote nativeQuote = quote();
        ExternalLease lease = service.tryExternalLease();
        assertNotNull(lease);
        assertNull(service.tryExternalLease());
        assertThrows(IllegalStateException.class, this::quote);
        assertEquals(Status.NO_CHARGE, service.confirm(nativeQuote.id(), buyer).status());
        assertEquals(0, adapter.prepares);
        assertEquals(0, adapter.withdrawals);
        assertTrue(journal.stages.isEmpty());
        lease.close();
        assertEquals(Status.SUCCESS, service.confirm(nativeQuote.id(), buyer).status());
    }

    @Test void staleOrRepeatedLeaseCloseCannotReleaseNewOwnerOrNativeExecution() {
        ExternalLease previous = service.tryExternalLease();
        assertNotNull(previous);
        previous.close();
        previous.close();
        ExternalLease current = service.tryExternalLease();
        assertNotNull(current);
        previous.close();
        assertNull(service.tryExternalLease());
        assertThrows(IllegalStateException.class, this::quote);
        current.close();
        adapter.afterWithdraw = () -> {
            previous.close();
            current.close();
            assertNull(service.tryExternalLease());
            assertThrows(IllegalStateException.class, this::quote);
        };
        assertEquals(Status.SUCCESS, buy().status());
    }

    @Test void exceptionDuringExternalValidationReleasesLeaseWithoutAnyCharge() {
        assertThrows(IllegalStateException.class, () -> {
            try (ExternalLease lease = service.tryExternalLease()) {
                assertNotNull(lease);
                throw new IllegalStateException("read-only external validator failed");
            }
        });
        assertTrue(journal.stages.isEmpty());
        assertEquals(0, adapter.withdrawals);
        assertEquals(Status.SUCCESS, buy().status());
    }

    @Test void disconnectAndCancelInvalidateQuotesWithoutReleasingExternalLease() {
        Quote original = quote();
        UUID otherBuyer = UUID.randomUUID();
        Quote other = service.quote(otherBuyer, seller, "second", 1, 1, 100, "second");
        ExternalLease lease = service.tryExternalLease();
        service.invalidateBuyer(buyer);
        assertTrue(service.cancel(other.id(), otherBuyer));
        service.invalidateAll();
        assertNull(service.tryExternalLease());
        assertThrows(IllegalStateException.class, this::quote);
        lease.close();
        assertEquals(Status.NO_CHARGE, service.confirm(original.id(), buyer).status());
        assertEquals(Status.NO_CHARGE, service.confirm(other.id(), otherBuyer).status());
        assertEquals(0, adapter.withdrawals);
    }

    @Test void externalLeaseAndPauseEnforcePrimaryThreadWithoutChangingOwnership() {
        Quote original = quote();
        adapter.mainThread = false;
        assertThrows(IllegalStateException.class, service::tryExternalLease);
        assertThrows(IllegalStateException.class, () -> service.pauseExternal("ambiguous external payment"));
        adapter.mainThread = true;
        ExternalLease lease = service.tryExternalLease();
        adapter.mainThread = false;
        assertThrows(IllegalStateException.class, lease::close);
        assertThrows(IllegalStateException.class, service::close);
        assertThrows(IllegalStateException.class, service::closeUnclean);
        adapter.mainThread = true;
        assertNull(service.tryExternalLease());
        lease.close();
        assertEquals(Status.SUCCESS, service.confirm(original.id(), buyer).status());
    }

    @Test void externalPauseClearsQuotesAndCannotBeUndoneByLeaseOrCleanClose() throws Exception {
        Quote original = quote();
        ExternalLease lease = service.tryExternalLease();
        service.pauseExternal("external transaction requires reconciliation");
        service.pauseExternal("a later symptom");
        lease.close();
        assertTrue(service.recoveryRequired());
        assertEquals("external transaction requires reconciliation", service.recoveryReason());
        assertNull(service.tryExternalLease());
        assertFalse(service.cancel(original.id(), buyer));
        assertThrows(IllegalStateException.class, this::quote);
        assertEquals(Status.RECOVERY_REQUIRED, service.confirm(original.id(), buyer).status());
        service.close();
        service.close();
        assertTrue(journal.uncleanClosed);
        assertFalse(journal.cleanClosed);
        assertTrue(journal.stages.isEmpty());
    }

    @Test void externalPauseRetainsDurableNativeMarkerAcrossRestart(@TempDir Path directory) throws Exception {
        FileJournal durable = new FileJournal(directory);
        service = new CheckoutService(adapter, durable, clock, Duration.ofSeconds(30));
        try (ExternalLease lease = service.tryExternalLease()) {
            assertNotNull(lease);
            service.pauseExternal("see separate legacy journal");
        }
        service.close();
        assertTrue(Files.exists(directory.resolve(FileJournal.SESSION_FILE)));
        assertEquals(0, Files.size(directory.resolve(FileJournal.LOG_FILE)));
        try (FileJournal reopened = new FileJournal(directory)) {
            assertTrue(reopened.requiresRecovery());
            CheckoutService restarted = new CheckoutService(adapter, reopened, clock, Duration.ofSeconds(30));
            assertNull(restarted.tryExternalLease());
            assertThrows(IllegalStateException.class,
                    () -> restarted.quote(buyer, seller, "shop", 1, 1, 100, "fp"));
        }
    }

    @ParameterizedTest @ValueSource(booleans = {false, true})
    void shutdownDuringExternalLeaseAlwaysRetainsDurableMarker(boolean explicitlyUnclean, @TempDir Path directory) throws Exception {
        FileJournal durable = new FileJournal(directory);
        service = new CheckoutService(adapter, durable, clock, Duration.ofSeconds(30));
        ExternalLease lease = service.tryExternalLease();
        if (explicitlyUnclean) service.closeUnclean();
        else service.close();
        assertTrue(service.recoveryRequired());
        lease.close();
        lease.close();
        assertNull(service.tryExternalLease());
        service.close();
        assertTrue(Files.exists(directory.resolve(FileJournal.SESSION_FILE)));
        try (FileJournal reopened = new FileJournal(directory)) { assertTrue(reopened.requiresRecovery()); }
    }

    @ParameterizedTest @ValueSource(strings = {"prepare", "evidence", "withdraw", "apply", "payout", "rollback", "refund"})
    void nativeCallbacksCannotAdmitExternalTransaction(String phase) {
        int[] callbacks = {0};
        adapter.onCallback = current -> {
            if (current.equals(phase)) {
                callbacks[0]++;
                assertNull(service.tryExternalLease());
                assertThrows(IllegalStateException.class, this::quote);
            }
        };
        if (phase.equals("rollback") || phase.equals("refund")) adapter.payoutResult = MoneyResult.DECLINED;
        assertEquals(adapter.payoutResult == MoneyResult.DECLINED ? Status.REFUNDED : Status.SUCCESS, buy().status());
        assertEquals(1, callbacks[0]);
    }

    @ParameterizedTest @ValueSource(strings = {"prepare", "evidence", "withdraw", "apply", "payout", "rollback", "refund"})
    void shutdownInNativeCallbackStopsLaterEffectsAndCannotMarkClean(String phase) throws Exception {
        adapter.onCallback = current -> {
            if (current.equals(phase)) assertDoesNotThrow(service::close);
        };
        if (phase.equals("rollback") || phase.equals("refund")) adapter.payoutResult = MoneyResult.DECLINED;
        Status expected = phase.equals("prepare") || phase.equals("evidence") ? Status.NO_CHARGE : Status.RECOVERY_REQUIRED;
        assertEquals(expected, buy().status());
        assertTrue(service.recoveryRequired());
        assertTrue(journal.uncleanClosed);
        assertFalse(journal.cleanClosed);
        assertEquals(phase.equals("prepare") || phase.equals("evidence") ? 0 : 1, adapter.withdrawals);
        assertEquals(phase.equals("prepare") || phase.equals("evidence") || phase.equals("withdraw") ? 0 : 1, adapter.applies);
        assertEquals(phase.equals("payout") || phase.equals("rollback") || phase.equals("refund") ? 1 : 0, adapter.payouts);
        assertEquals(phase.equals("refund") ? 1 : 0, adapter.refunds);
        assertNull(service.tryExternalLease());
        service.close();
        assertFalse(journal.cleanClosed);
    }

    @Test void pauseDuringNativeValidationStopsBeforeFirstEconomicEffect() throws Exception {
        adapter.onCallback = current -> {
            if (current.equals("prepare")) service.pauseExternal("external safety failure");
        };
        assertEquals(Status.NO_CHARGE, buy().status());
        assertEquals(0, adapter.withdrawals);
        assertTrue(journal.stages.isEmpty());
        service.close();
        assertTrue(journal.uncleanClosed);
    }

    @Test void primaryThreadGuardRunsBeforeQuoteConsumptionOrAnyMutation() {
        Quote quote = quote();
        adapter.mainThread = false;
        assertThrows(IllegalStateException.class, () -> service.confirm(quote.id(), buyer));
        assertThrows(IllegalStateException.class, this::quote);
        assertThrows(IllegalStateException.class, () -> service.cancel(quote.id(), buyer));
        assertThrows(IllegalStateException.class, () -> service.invalidateAll());
        assertThrows(IllegalStateException.class, () -> service.invalidateBuyer(buyer));
        assertEquals(0, adapter.withdrawals);
        adapter.mainThread = true;
        assertEquals(Status.SUCCESS, service.confirm(quote.id(), buyer).status());
    }

    @Test void fatalMutationErrorHaltsAndPreservesDirtyJournalBeforeRethrowing() {
        adapter.withdrawError = true;
        assertThrows(AssertionError.class, this::buy);
        assertTrue(service.recoveryRequired());
        assertEquals(Stage.RECOVERY_REQUIRED, journal.last());
    }

    @Test void startupRecoveryStatePreventsAnyNewCharge() {
        journal.recovery = true;
        service = new CheckoutService(adapter, journal, clock, Duration.ofSeconds(30));
        assertThrows(IllegalStateException.class, this::quote);
        assertEquals(Status.RECOVERY_REQUIRED, service.confirm(UUID.randomUUID(), buyer).status());
        assertTrue(service.recoveryRequired());
        assertNull(service.tryExternalLease());
        assertEquals(0, adapter.withdrawals);
    }

    @Test void cleanAndUncleanShutdownInvalidateAllQuotes() throws Exception {
        Quote quote = quote();
        service.close();
        assertTrue(journal.cleanClosed);
        assertNull(service.tryExternalLease());
        assertThrows(IllegalStateException.class, () -> service.pauseExternal("already closed"));
        assertEquals(Status.NO_CHARGE, service.confirm(quote.id(), buyer).status());
        assertThrows(IllegalStateException.class, this::quote);
        setup();
        service.closeUnclean();
        assertTrue(journal.uncleanClosed);
        assertTrue(service.recoveryRequired());
    }

    @Test void recoveryCanNeverBeClearedByCleanClose() throws Exception {
        adapter.withdrawThrows = true;
        buy();
        service.close();
        assertFalse(journal.cleanClosed);
        assertTrue(journal.uncleanClosed);
    }

    @Test void invalidInputsAndBoundedQuoteCacheAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> service.quote(buyer, buyer, "self", 1, 1, 100, "fp"));
        assertThrows(IllegalArgumentException.class, () -> service.quote(buyer, seller, "", 1, 1, 100, "fp"));
        assertThrows(IllegalArgumentException.class, () -> service.quote(buyer, seller, "id", -1, 1, 100, "fp"));
        assertThrows(IllegalArgumentException.class, () -> service.quote(buyer, seller, "id", 1, 0, 100, "fp"));
        assertThrows(IllegalArgumentException.class, () -> service.quote(buyer, seller, "id", 1, 1, 0, "fp"));
        assertThrows(IllegalArgumentException.class, () -> service.quote(buyer, seller, "id", 1, 1, 100, ""));
        for (int i = 0; i < MAX_ACTIVE_QUOTES; i++) service.quote(UUID.randomUUID(), seller, "id", 1, 1, 100, "fp");
        assertThrows(IllegalStateException.class, this::quote);
        clock.now = clock.now.plusSeconds(31);
        assertNotNull(quote());
    }

    @Test void invalidTtlAndPreparationAreRejected() {
        for (Duration duration : List.of(Duration.ZERO, Duration.ofSeconds(-1), Duration.ofMinutes(6))) {
            assertThrows(IllegalArgumentException.class, () -> new CheckoutService(adapter, journal, clock, duration));
        }
        assertThrows(IllegalArgumentException.class, () -> new Preparation(null, null));
        assertThrows(IllegalArgumentException.class, () -> new Preparation(new Transfer() {
            public MutationResult apply() { return MutationResult.UNCHANGED; }
            public boolean rollback() { return false; }
        }, "denied"));
    }

    private final class TestAdapter implements Adapter {
        long buyerFunds = 5000, sellerFunds, unitPrice = 125, revision = 7;
        int stock = 32, delivered, capacity = 36, withdrawals, refunds, payouts, applies, rollbacks, prepares;
        String fingerprint = "metadata-and-stock-v1", evidence = "buyer-before=[];stock-before=32;balance-before=5000";
        UUID expectedSeller = seller;
        boolean allowed = true, online = true, mainThread = true, prepareThrows, withdrawThrows, withdrawError,
                applyThrows, payoutThrows, rollbackThrows, refundThrows, evidenceThrows, rollbackWorks = true;
        MoneyResult withdrawResult = MoneyResult.SUCCESS, payoutResult = MoneyResult.SUCCESS, refundResult = MoneyResult.SUCCESS;
        MutationResult applyResult = MutationResult.APPLIED;
        Runnable afterWithdraw = () -> { };
        Consumer<String> onCallback = phase -> { };
        public void assertMainThread() { if (!mainThread) throw new IllegalStateException("Not primary thread"); }
        public Preparation prepare(Quote quote) {
            prepares++;
            onCallback.accept("prepare");
            if (prepareThrows) throw new IllegalStateException("prepare");
            if (!Objects.equals(expectedSeller, quote.sellerId()) || quote.revision() != revision
                    || !quote.fingerprint().equals(fingerprint) || quote.amountMinorUnits() != Money.multiply(unitPrice, quote.quantity())
                    || stock < quote.quantity() || buyerFunds < quote.amountMinorUnits() || capacity < quote.quantity() || !allowed || !online) {
                return Preparation.denied("Changed conditions. No money was taken.");
            }
            final int stockBefore = stock, deliveryBefore = delivered;
            return Preparation.ready(new Transfer() {
                public String auditEvidence() { onCallback.accept("evidence"); if (evidenceThrows) throw new IllegalStateException("evidence"); return evidence; }
                public MutationResult apply() {
                    assertEquals(Stage.TRANSFER_INTENT, journal.last());
                    applies++;
                    onCallback.accept("apply");
                    if (stock != stockBefore || delivered != deliveryBefore) return MutationResult.UNCHANGED;
                    if (applyThrows) { stock--; throw new IllegalStateException("partial apply"); }
                    if (applyResult == MutationResult.APPLIED) { stock -= quote.quantity(); delivered += quote.quantity(); }
                    return applyResult;
                }
                public boolean rollback() {
                    assertEquals(Stage.ROLLBACK_INTENT, journal.last());
                    rollbacks++;
                    onCallback.accept("rollback");
                    if (rollbackThrows) throw new IllegalStateException("rollback");
                    if (rollbackWorks) { stock = stockBefore; delivered = deliveryBefore; }
                    return rollbackWorks;
                }
            });
        }
        public MoneyResult withdraw(UUID id, long amount) {
            assertEquals(buyer, id);
            assertEquals(Stage.WITHDRAW_INTENT, journal.last());
            withdrawals++;
            onCallback.accept("withdraw");
            if (withdrawError) throw new AssertionError("provider fatal error");
            if (withdrawThrows) { buyerFunds -= amount; throw new IllegalStateException("deducted then threw"); }
            if (withdrawResult == MoneyResult.SUCCESS) buyerFunds -= amount;
            afterWithdraw.run();
            return withdrawResult;
        }
        public MoneyResult deposit(UUID id, long amount) {
            if (buyer.equals(id)) {
                assertEquals(Stage.REFUND_INTENT, journal.last());
                refunds++;
                onCallback.accept("refund");
                if (refundThrows) { buyerFunds += amount; throw new IllegalStateException("refunded then threw"); }
                if (refundResult == MoneyResult.SUCCESS) buyerFunds += amount;
                return refundResult;
            }
            assertEquals(seller, id);
            assertEquals(Stage.PAYOUT_INTENT, journal.last());
            payouts++;
            onCallback.accept("payout");
            if (payoutThrows) { sellerFunds += amount; throw new IllegalStateException("paid then threw"); }
            if (payoutResult == MoneyResult.SUCCESS) sellerFunds += amount;
            return payoutResult;
        }
    }

    private static final class TestJournal implements Journal {
        final List<Stage> stages = new ArrayList<>();
        final List<String> details = new ArrayList<>();
        boolean recovery, cleanClosed, uncleanClosed;
        Stage failAt;
        public boolean requiresRecovery() { return recovery; }
        public String recoveryReason() { return "previous unclean session"; }
        public void record(Quote quote, Stage stage, String detail) throws IOException {
            if (cleanClosed || uncleanClosed) throw new IOException("journal closed");
            if (stage == failAt) { recovery = true; throw new IOException("forced failure at " + stage); }
            stages.add(stage); details.add(detail);
            if (stage == Stage.RECOVERY_REQUIRED) recovery = true;
        }
        Stage last() { return stages.isEmpty() ? null : stages.getLast(); }
        public void close() { cleanClosed = true; }
        public void closeUnclean() { uncleanClosed = true; }
    }

    private static final class MutableClock extends Clock {
        Instant now = Instant.parse("2026-10-12T00:00:00Z");
        public ZoneId getZone() { return ZoneOffset.UTC; }
        public Clock withZone(ZoneId zone) { return this; }
        public Instant instant() { return now; }
    }
}

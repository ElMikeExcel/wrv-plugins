// SPDX-License-Identifier: MIT
package com.winkreyes.shops.checkout;

import static org.junit.jupiter.api.Assertions.*;
import static com.winkreyes.shops.checkout.CheckoutService.*;
import java.io.IOException;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
            assertThrows(IllegalStateException.class, () -> service.close());
        };
        assertEquals(Status.SUCCESS, service.confirm(first.id(), buyer).status());
        assertEquals(1, adapter.withdrawals);
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
        assertEquals(0, adapter.withdrawals);
    }

    @Test void cleanAndUncleanShutdownInvalidateAllQuotes() throws Exception {
        Quote quote = quote();
        service.close();
        assertTrue(journal.cleanClosed);
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
        public void assertMainThread() { if (!mainThread) throw new IllegalStateException("Not primary thread"); }
        public Preparation prepare(Quote quote) {
            prepares++;
            if (prepareThrows) throw new IllegalStateException("prepare");
            if (!Objects.equals(expectedSeller, quote.sellerId()) || quote.revision() != revision
                    || !quote.fingerprint().equals(fingerprint) || quote.amountMinorUnits() != Money.multiply(unitPrice, quote.quantity())
                    || stock < quote.quantity() || buyerFunds < quote.amountMinorUnits() || capacity < quote.quantity() || !allowed || !online) {
                return Preparation.denied("Changed conditions. No money was taken.");
            }
            final int stockBefore = stock, deliveryBefore = delivered;
            return Preparation.ready(new Transfer() {
                public String auditEvidence() { if (evidenceThrows) throw new IllegalStateException("evidence"); return evidence; }
                public MutationResult apply() {
                    assertEquals(Stage.TRANSFER_INTENT, journal.last());
                    applies++;
                    if (stock != stockBefore || delivered != deliveryBefore) return MutationResult.UNCHANGED;
                    if (applyThrows) { stock--; throw new IllegalStateException("partial apply"); }
                    if (applyResult == MutationResult.APPLIED) { stock -= quote.quantity(); delivered += quote.quantity(); }
                    return applyResult;
                }
                public boolean rollback() {
                    assertEquals(Stage.ROLLBACK_INTENT, journal.last());
                    rollbacks++;
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
                if (refundThrows) { buyerFunds += amount; throw new IllegalStateException("refunded then threw"); }
                if (refundResult == MoneyResult.SUCCESS) buyerFunds += amount;
                return refundResult;
            }
            assertEquals(seller, id);
            assertEquals(Stage.PAYOUT_INTENT, journal.last());
            payouts++;
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

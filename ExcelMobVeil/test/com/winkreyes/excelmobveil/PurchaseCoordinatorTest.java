package com.winkreyes.excelmobveil;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static com.winkreyes.excelmobveil.PurchaseCoordinator.*;
import static org.junit.jupiter.api.Assertions.*;

class PurchaseCoordinatorTest {
    @TempDir Path directory;
    final UUID player = UUID.randomUUID();
    final AtomicLong now = new AtomicLong(10_000);
    FakePorts ports;
    PurchaseJournal journal;
    PurchaseCoordinator coordinator;

    @BeforeEach void setup() throws Exception {
        ports = new FakePorts();
        journal = new PurchaseJournal(directory.resolve("purchases.journal"));
        coordinator = create(journal);
    }

    PurchaseCoordinator create(PurchaseJournal log) {
        return new PurchaseCoordinator(ports, log, now::get, 20_000, 1_000);
    }
    Result click(Product product) { return coordinator.click(player, product, price(product)); }
    Result quote(Product product) { return coordinator.quote(player, product, price(product)); }
    Result preview(Product product) { return coordinator.preview(player, product, price(product)); }
    Result confirm(Product product) { return coordinator.confirm(player, product, price(product)); }
    static double price(Product product) { return 2_000; }
    Result buy(Product product) {
        assertEquals(Status.ARMED, click(product).status());
        return click(product);
    }

    @Test void repeatedQuotesNeverChargeAndReplacePreviousExpiry() {
        assertEquals(Status.ARMED, quote(Product.VEIL).status());
        now.addAndGet(19_999);
        for (int i = 0; i < 5; i++) {
            Result quoted = quote(Product.VEIL);
            assertEquals(Status.ARMED, quoted.status());
            assertEquals(Product.VEIL, quoted.product());
            assertEquals(2_000, quoted.price());
            assertEquals(100_000, quoted.balance());
            assertNull(quoted.transactionId());
        }
        assertNoTransactions();
        now.incrementAndGet();
        assertEquals(Status.PURCHASED, confirm(Product.VEIL).status());
        assertEquals(1, ports.withdrawals);
    }

    @Test void previewNeverPrimesAPurchase() {
        for (Product product : Product.values()) {
            for (int i = 0; i < 3; i++) assertEquals(Status.ARMED, preview(product).status());
            assertEquals(Status.ARMED, confirm(product).status());
            coordinator.clearConfirmation(player);
        }
        assertNoTransactions();
    }

    @Test void quotedProductCanBeConfirmedAfterAnyReadOnlyPreview() {
        assertEquals(Status.ARMED, quote(Product.VEIL).status());
        assertEquals(Status.ARMED, preview(Product.VEIL).status());
        assertEquals(Status.ARMED, coordinator.preview(player, Product.VEIL, 2_500).status());
        assertNoTransactions();
        assertEquals(Status.PURCHASED, confirm(Product.VEIL).status());
        assertEquals(98_000, ports.money);
    }

    @Test void previewFailuresDoNotConsumeAnExistingQuote() {
        quote(Product.VEIL);
        ports.money = 1;
        assertEquals(Status.INSUFFICIENT_FUNDS, preview(Product.VEIL).status());
        ports.money = 100_000;
        ports.eligibility = Eligibility.NO_SPACE;
        assertEquals(Status.INELIGIBLE, preview(Product.VEIL).status());
        ports.eligibility = Eligibility.ELIGIBLE;
        ports.failBalance = true;
        assertEquals(Status.UNAVAILABLE, preview(Product.VEIL).status());
        ports.failBalance = false;
        assertEquals(Status.INVALID_PRICE, coordinator.preview(player, Product.VEIL, Double.NaN).status());
        assertNoTransactions();
        assertEquals(Status.PURCHASED, confirm(Product.VEIL).status());
        assertEquals(98_000, ports.money);
    }

    @Test void previewsNeverRefreshAQuoteExpiry() {
        quote(Product.VEIL);
        now.addAndGet(19_999);
        assertEquals(Status.ARMED, preview(Product.VEIL).status());
        now.incrementAndGet();
        assertEquals(Status.ARMED, confirm(Product.VEIL).status());
        assertNoTransactions();
        assertEquals(Status.PURCHASED, confirm(Product.VEIL).status());
    }

    @Test void insufficientResultsExposeVeilPriceAndBalanceWithoutArming() {
        for (Product product : Product.values()) {
            double expectedPrice = price(product);
            ports.money = expectedPrice - 123.45;
            for (Result result : new Result[]{quote(product), preview(product), confirm(product)}) {
                assertEquals(Status.INSUFFICIENT_FUNDS, result.status());
                assertEquals(product, result.product());
                assertEquals(expectedPrice, result.price());
                assertEquals(ports.money, result.balance());
                assertEquals(123.45, result.price() - result.balance(), 0.000001);
                assertNull(result.transactionId());
            }
            ports.money = 100_000;
            assertEquals(Status.ARMED, confirm(product).status());
            coordinator.clearConfirmation(player);
        }
        assertNoTransactions();
    }

    @Test void confirmWithoutAQuoteRequiresAnotherDistinctConfirmation() {
        assertEquals(Status.ARMED, confirm(Product.VEIL).status());
        assertNoTransactions();
        assertEquals(Status.PURCHASED, confirm(Product.VEIL).status());
    }

    @Test void confirmPriceChangeReturnsFreshReviewWithoutDebit() {
        quote(Product.VEIL);
        Result changed = coordinator.confirm(player, Product.VEIL, 2_500);
        assertEquals(Status.ARMED, changed.status());
        assertEquals(2_500, changed.price());
        assertNoTransactions();
        assertEquals(Status.PURCHASED, coordinator.confirm(player, Product.VEIL, 2_500).status());
        assertEquals(97_500, ports.money);
    }

    @Test void differentPlayerCannotUseOrConsumeAQuote() {
        quote(Product.VEIL);
        assertEquals(Status.ARMED, coordinator.confirm(UUID.randomUUID(), Product.VEIL, 2_000).status());
        assertNoTransactions();
        assertEquals(Status.PURCHASED, confirm(Product.VEIL).status());
    }

    @Test void quoteFailureInvalidatesEarlierConfirmation() {
        quote(Product.VEIL);
        ports.money = 1;
        assertEquals(Status.INSUFFICIENT_FUNDS, quote(Product.VEIL).status());
        ports.money = 100_000;
        assertEquals(Status.ARMED, confirm(Product.VEIL).status());
        assertNoTransactions();
    }

    @Test void allConfirmationPrecheckFailuresConsumeThePreviousToken() throws Exception {
        SwitchableJournal log = new SwitchableJournal(directory.resolve("switchable"));
        journal = log;
        coordinator = create(log);
        quote(Product.VEIL);
        log.available = false;
        assertEquals(Status.UNAVAILABLE, confirm(Product.VEIL).status());
        log.available = true;
        assertEquals(Status.ARMED, confirm(Product.VEIL).status());
        assertEquals(Status.INVALID_PRICE, coordinator.confirm(player, Product.VEIL, 0).status());
        assertEquals(Status.ARMED, confirm(Product.VEIL).status());
        ports.eligibility = Eligibility.NO_SPACE;
        assertEquals(Status.INELIGIBLE, confirm(Product.VEIL).status());
        ports.eligibility = Eligibility.ELIGIBLE;
        assertEquals(Status.ARMED, confirm(Product.VEIL).status());
        ports.failBalance = true;
        assertEquals(Status.UNAVAILABLE, confirm(Product.VEIL).status());
        ports.failBalance = false;
        assertEquals(Status.ARMED, confirm(Product.VEIL).status());
        ports.money = 1;
        assertEquals(Status.INSUFFICIENT_FUNDS, confirm(Product.VEIL).status());
        ports.money = 100_000;
        assertEquals(Status.ARMED, confirm(Product.VEIL).status());
        assertNoTransactions();
    }

    @Test void previewChecksJournalHealthWithoutConsumingTheQuote() throws Exception {
        SwitchableJournal log = new SwitchableJournal(directory.resolve("switchable"));
        journal = log;
        coordinator = create(log);
        quote(Product.VEIL);
        int balanceReads = ports.balanceReads;
        log.available = false;
        assertEquals(Status.UNAVAILABLE, preview(Product.VEIL).status());
        assertEquals(balanceReads, ports.balanceReads);
        log.available = true;
        assertEquals(Status.PURCHASED, confirm(Product.VEIL).status());
    }

    @Test void quoteAndPreviewRespectCooldownAndLockedState() {
        assertEquals(Status.PURCHASED, buy(Product.VEIL).status());
        int entries = journal.entries().size();
        int balanceReads = ports.balanceReads;
        assertEquals(Status.COOLDOWN, quote(Product.VEIL).status());
        assertEquals(Status.COOLDOWN, preview(Product.VEIL).status());
        assertEquals(entries, journal.entries().size());
        assertEquals(balanceReads, ports.balanceReads);
        now.addAndGet(1_000);
        ports.delivery = DeliveryOutcome.UNKNOWN;
        quote(Product.VEIL);
        assertEquals(Status.LOCKED, confirm(Product.VEIL).status());
        entries = journal.entries().size();
        balanceReads = ports.balanceReads;
        assertEquals(Status.LOCKED, quote(Product.VEIL).status());
        assertEquals(Status.LOCKED, preview(Product.VEIL).status());
        assertEquals(entries, journal.entries().size());
        assertEquals(balanceReads, ports.balanceReads);
        assertEquals(2, ports.withdrawals);
    }

    @Test void expiryIsRecheckedAfterSlowProviderPrechecks() {
        quote(Product.VEIL);
        ports.onCheck = () -> now.addAndGet(20_000);
        assertEquals(Status.ARMED, confirm(Product.VEIL).status());
        assertNoTransactions();
        ports.onCheck = () -> {};
        assertEquals(Status.PURCHASED, confirm(Product.VEIL).status());
    }

    @Test void backwardClockRequiresFreshConfirmationForExplicitApi() {
        quote(Product.VEIL);
        now.decrementAndGet();
        assertEquals(Status.ARMED, confirm(Product.VEIL).status());
        assertNoTransactions();
    }

    @Test void allEntryPointsShareTheReentrantGuardDuringPreviewAndPurchase() {
        ports.onCheck = () -> {
            assertEquals(Status.BUSY, preview(Product.VEIL).status());
            assertEquals(Status.BUSY, quote(Product.VEIL).status());
            assertEquals(Status.BUSY, confirm(Product.VEIL).status());
            assertEquals(Status.BUSY, click(Product.VEIL).status());
        };
        assertEquals(Status.ARMED, preview(Product.VEIL).status());
        assertEquals(Status.ARMED, confirm(Product.VEIL).status());
        assertNoTransactions();
        ports.onWithdraw = ports.onCheck;
        assertEquals(Status.PURCHASED, confirm(Product.VEIL).status());
        assertEquals(1, ports.withdrawals);
    }

    @Test void concurrentReviewCallsCannotInterfereWithAnInProgressConfirmation() throws Exception {
        CountDownLatch inside = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        AtomicReference<Result> result = new AtomicReference<>();
        ports.onWithdraw = () -> {
            inside.countDown();
            try { assertTrue(release.await(5, TimeUnit.SECONDS)); }
            catch (InterruptedException failure) { throw new RuntimeException(failure); }
        };
        quote(Product.VEIL);
        Thread thread = new Thread(() -> result.set(confirm(Product.VEIL)));
        thread.start();
        try {
            assertTrue(inside.await(5, TimeUnit.SECONDS));
            assertEquals(Status.BUSY, quote(Product.VEIL).status());
            assertEquals(Status.BUSY, preview(Product.VEIL).status());
            assertEquals(Status.BUSY, confirm(Product.VEIL).status());
        } finally { release.countDown(); }
        thread.join(5_000);
        assertFalse(thread.isAlive());
        assertEquals(Status.PURCHASED, result.get().status());
        assertEquals(1, ports.withdrawals);
        assertEquals(1, ports.deliveries);
    }

    void assertNoTransactions() {
        assertEquals(0, ports.withdrawals);
        assertEquals(0, ports.refunds);
        assertEquals(0, ports.deliveries);
        assertTrue(journal.entries().isEmpty());
    }

    @Test void veilChargesTwoThousandAndDeliversOnce() {
        Result result = buy(Product.VEIL);
        assertEquals(Status.PURCHASED, result.status());
        assertEquals(98_000, result.balance());
        assertEquals(Product.VEIL, ports.deliveredProduct);
        assertEquals(1, ports.withdrawals);
        assertEquals(1, ports.deliveries);
        assertEquals(0, ports.refunds);
        assertTrue(journal.unresolved().isEmpty());
        assertEquals(PurchaseJournal.Phase.COMPLETED,
                journal.entries().get(result.transactionId()).phase());
    }

    @Test void priceChangeRequiresFreshConfirmation() {
        assertEquals(Status.ARMED, click(Product.VEIL).status());
        assertEquals(Status.ARMED, coordinator.click(player, Product.VEIL, 2_500).status());
        assertEquals(0, ports.withdrawals);
        assertEquals(Status.PURCHASED, coordinator.click(player, Product.VEIL, 2_500).status());
        assertEquals(97_500, ports.money);
    }

    @Test void otherPlayersCannotConsumeConfirmation() {
        assertEquals(Status.ARMED, click(Product.VEIL).status());
        assertEquals(Status.ARMED, coordinator.click(UUID.randomUUID(), Product.VEIL, 2_000).status());
        assertEquals(0, ports.withdrawals);
    }

    @Test void exactExpiryRearmsWithoutCharge() {
        click(Product.VEIL);
        now.addAndGet(20_000);
        assertEquals(Status.ARMED, click(Product.VEIL).status());
        assertEquals(0, ports.withdrawals);
    }

    @Test void backwardClockRearmsWithoutCharge() {
        click(Product.VEIL);
        now.decrementAndGet();
        assertEquals(Status.ARMED, click(Product.VEIL).status());
        assertEquals(0, ports.withdrawals);
    }

    @Test void cancelConsumesConfirmation() {
        click(Product.VEIL);
        coordinator.clearConfirmation(player);
        assertEquals(Status.ARMED, click(Product.VEIL).status());
        assertEquals(0, ports.withdrawals);
    }

    @Test void insufficientFundsNeverArmsOrCharges() {
        ports.money = 1_999;
        assertEquals(Status.INSUFFICIENT_FUNDS, click(Product.VEIL).status());
        ports.money = 100_000;
        assertEquals(Status.ARMED, click(Product.VEIL).status());
        assertEquals(0, ports.withdrawals);
    }

    @Test void balanceRecheckedOnConfirmationAndFailedTokenConsumed() {
        click(Product.VEIL);
        ports.money = 1;
        assertEquals(Status.INSUFFICIENT_FUNDS, click(Product.VEIL).status());
        ports.money = 100_000;
        assertEquals(Status.ARMED, click(Product.VEIL).status());
        assertEquals(0, ports.withdrawals);
    }

    @Test void inventoryAndAvailabilityCheckedBeforeArmingAndConfirming() {
        ports.eligibility = Eligibility.NO_SPACE;
        assertEquals(Status.INELIGIBLE, click(Product.VEIL).status());
        ports.eligibility = Eligibility.ELIGIBLE;
        assertEquals(Status.ARMED, click(Product.VEIL).status());
        ports.eligibility = Eligibility.OFFLINE;
        assertEquals(Status.INELIGIBLE, click(Product.VEIL).status());
        ports.eligibility = Eligibility.ELIGIBLE;
        assertEquals(Status.ARMED, click(Product.VEIL).status());
        assertEquals(0, ports.withdrawals);
    }

    @Test void invalidPricesNeverArmOrReachProvider() {
        for (double invalid : new double[]{0, -1, Double.NaN, Double.POSITIVE_INFINITY}) {
            assertEquals(Status.INVALID_PRICE, coordinator.click(player, Product.VEIL, invalid).status());
        }
        assertEquals(0, ports.balanceReads);
        assertEquals(0, ports.withdrawals);
    }

    @Test void invalidBalanceAndReadExceptionFailWithoutCharge() {
        ports.money = Double.NaN;
        assertEquals(Status.UNAVAILABLE, click(Product.VEIL).status());
        ports.money = Double.POSITIVE_INFINITY;
        assertEquals(Status.UNAVAILABLE, click(Product.VEIL).status());
        ports.money = 100_000;
        click(Product.VEIL);
        ports.failBalance = true;
        assertEquals(Status.UNAVAILABLE, click(Product.VEIL).status());
        ports.failBalance = false;
        assertEquals(Status.ARMED, click(Product.VEIL).status());
        assertEquals(0, ports.withdrawals);
    }

    @Test void repeatedClicksHitOneSecondCooldown() {
        assertEquals(Status.PURCHASED, buy(Product.VEIL).status());
        for (int i = 0; i < 10; i++) assertEquals(Status.COOLDOWN, click(Product.VEIL).status());
        assertEquals(Status.COOLDOWN, click(Product.VEIL).status());
        now.addAndGet(999);
        assertEquals(Status.COOLDOWN, click(Product.VEIL).status());
        now.incrementAndGet();
        assertEquals(Status.ARMED, click(Product.VEIL).status());
        assertEquals(1, ports.withdrawals);
    }

    @Test void knownProviderDeclineHasNoDeliveryOrRefund() {
        ports.decline = true;
        assertEquals(Status.DECLINED, buy(Product.VEIL).status());
        assertEquals(100_000, ports.money);
        assertEquals(0, ports.deliveries);
        assertEquals(0, ports.refunds);
        assertFalse(coordinator.isLocked(player));
        assertTrue(journal.unresolved().isEmpty());
        assertEquals(Status.ARMED, click(Product.VEIL).status());
    }

    @Test void declineAfterDebitLocksWithoutRefund() {
        ports.returnFalseAfterDebit = true;
        assertLocked(buy(Product.VEIL));
        assertEquals(98_000, ports.money);
        assertEquals(0, ports.deliveries);
        assertEquals(0, ports.refunds);
    }

    @Test void throwingProviderAfterDebitLocksAndNeverRetries() {
        ports.throwAfterDebit = true;
        assertLocked(buy(Product.VEIL));
        assertEquals(98_000, ports.money);
        assertEquals(0, ports.deliveries);
        assertEquals(0, ports.refunds);
    }

    @Test void nullWithdrawalResponseLocks() {
        ports.nullWithdrawal = true;
        assertLocked(buy(Product.VEIL));
        assertEquals(0, ports.deliveries);
    }

    @Test void malformedWithdrawalAmountLocks() {
        ports.wrongAmount = true;
        assertLocked(buy(Product.VEIL));
        assertEquals(0, ports.deliveries);
    }

    @Test void responseBalanceMustMatchFreshObservedBalance() {
        ports.wrongReportedBalance = true;
        assertLocked(buy(Product.VEIL));
        assertEquals(0, ports.deliveries);
    }

    @Test void verifiedRollbackPrecedesRefundAndIntentExistsAtBothMoneyCalls() {
        ports.delivery = DeliveryOutcome.FAILED_ROLLED_BACK;
        ports.onWithdraw = () -> assertEquals(PurchaseJournal.Phase.WITHDRAW_INTENT,
                journal.unresolved().values().iterator().next().phase());
        ports.onRefund = () -> {
            assertEquals(1, ports.deliveries);
            assertEquals(PurchaseJournal.Phase.REFUND_INTENT,
                    journal.unresolved().values().iterator().next().phase());
        };
        assertEquals(Status.REFUNDED, buy(Product.VEIL).status());
        assertEquals(100_000, ports.money);
        assertEquals(1, ports.refunds);
        assertTrue(journal.unresolved().isEmpty());
    }

    @Test void unknownDeliveryNeverRefunds() {
        ports.delivery = DeliveryOutcome.UNKNOWN;
        assertLocked(buy(Product.VEIL));
        assertEquals(0, ports.refunds);
    }

    @Test void deliveryExceptionNeverAssumesRollbackOrRefunds() {
        ports.throwDelivery = true;
        assertLocked(buy(Product.VEIL));
        assertEquals(0, ports.refunds);
    }

    @Test void nullDeliveryNeverRefunds() {
        ports.delivery = null;
        assertLocked(buy(Product.VEIL));
        assertEquals(0, ports.refunds);
    }

    @Test void refundFailureLocksAndNeverRetries() {
        ports.delivery = DeliveryOutcome.FAILED_ROLLED_BACK;
        ports.rejectRefund = true;
        assertLocked(buy(Product.VEIL));
        assertEquals(98_000, ports.money);
        assertEquals(1, ports.refunds);
    }

    @Test void refundExceptionAfterCreditLocksAndNeverDoubleRefunds() {
        ports.delivery = DeliveryOutcome.FAILED_ROLLED_BACK;
        ports.throwAfterRefund = true;
        assertLocked(buy(Product.VEIL));
        assertEquals(100_000, ports.money);
        assertEquals(1, ports.refunds);
    }

    @Test void reentrantProviderCallbackCannotUseConsumedToken() {
        ports.onWithdraw = () -> assertEquals(Status.BUSY, click(Product.VEIL).status());
        assertEquals(Status.PURCHASED, buy(Product.VEIL).status());
        assertEquals(1, ports.withdrawals);
    }

    @Test void concurrentRequestsCannotRepeatWithdrawal() throws Exception {
        CountDownLatch inside = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        AtomicReference<Result> firstResult = new AtomicReference<>();
        ports.onWithdraw = () -> {
            inside.countDown();
            try { assertTrue(release.await(5, TimeUnit.SECONDS)); }
            catch (InterruptedException failure) { throw new RuntimeException(failure); }
        };
        click(Product.VEIL);
        Thread thread = new Thread(() -> firstResult.set(click(Product.VEIL)));
        thread.start();
        try {
            assertTrue(inside.await(5, TimeUnit.SECONDS));
            assertEquals(Status.BUSY, click(Product.VEIL).status());
        } finally { release.countDown(); }
        thread.join(5_000);
        assertFalse(thread.isAlive());
        assertEquals(Status.PURCHASED, firstResult.get().status());
        assertEquals(1, ports.withdrawals);
    }

    @Test void writeFailureBeforeWithdrawalPreventsAllMoneyCalls() throws Exception {
        coordinator = create(new FaultyJournal(directory.resolve("broken"), PurchaseJournal.Phase.WITHDRAW_INTENT));
        assertEquals(Status.UNAVAILABLE, buy(Product.VEIL).status());
        assertEquals(0, ports.withdrawals);
        assertEquals(0, ports.refunds);
        assertEquals(Status.LOCKED, click(Product.VEIL).status());
    }

    @Test void journalFailureAfterDebitStopsDeliveryAndRefund() throws Exception {
        coordinator = create(new FaultyJournal(directory.resolve("broken"), PurchaseJournal.Phase.WITHDRAWN));
        assertLocked(buy(Product.VEIL));
        assertEquals(0, ports.deliveries);
        assertEquals(0, ports.refunds);
    }

    @Test void journalFailureAfterDeliveryDoesNotRefundDeliveredItem() throws Exception {
        coordinator = create(new FaultyJournal(directory.resolve("broken"), PurchaseJournal.Phase.COMPLETED));
        assertLocked(buy(Product.VEIL));
        assertEquals(1, ports.deliveries);
        assertEquals(0, ports.refunds);
    }

    @Test void failedRefundIntentWritePreventsDeposit() throws Exception {
        coordinator = create(new FaultyJournal(directory.resolve("broken"), PurchaseJournal.Phase.REFUND_INTENT));
        ports.delivery = DeliveryOutcome.FAILED_ROLLED_BACK;
        assertLocked(buy(Product.VEIL));
        assertEquals(0, ports.refunds);
    }

    @Test void failedRefundCompletionWriteNeverRetriesDeposit() throws Exception {
        coordinator = create(new FaultyJournal(directory.resolve("broken"), PurchaseJournal.Phase.REFUNDED));
        ports.delivery = DeliveryOutcome.FAILED_ROLLED_BACK;
        assertLocked(buy(Product.VEIL));
        assertEquals(1, ports.refunds);
        assertEquals(100_000, ports.money);
    }

    @Test void interruptedPurchaseLocksSamePlayerAfterRestartWithoutReplaying() throws Exception {
        ports.delivery = DeliveryOutcome.UNKNOWN;
        Result result = buy(Product.VEIL);
        assertEquals(Status.LOCKED, result.status());
        coordinator = create(new PurchaseJournal(directory.resolve("purchases.journal")));
        assertTrue(coordinator.lockedPlayers().contains(player));
        assertEquals(Status.LOCKED, click(Product.VEIL).status());
        assertEquals(Status.ARMED, coordinator.click(UUID.randomUUID(), Product.VEIL, 2_000).status());
        assertEquals(1, ports.withdrawals);
        assertEquals(0, ports.refunds);
    }

    void assertLocked(Result result) {
        assertEquals(Status.LOCKED, result.status());
        assertTrue(coordinator.isLocked(player));
        assertEquals(Status.LOCKED, click(Product.VEIL).status());
        assertEquals(1, ports.withdrawals);
    }

    static class SwitchableJournal extends PurchaseJournal {
        boolean available = true;
        SwitchableJournal(Path path) throws IOException { super(path); }
        @Override public synchronized boolean healthy() { return available && super.healthy(); }
    }

    static class FaultyJournal extends PurchaseJournal {
        final Phase failurePhase;
        FaultyJournal(Path path, Phase failurePhase) throws IOException { super(path); this.failurePhase = failurePhase; }
        @Override public synchronized void append(Entry entry) throws IOException {
            if (entry.phase() == failurePhase) throw new IOException("injected disk failure");
            super.append(entry);
        }
    }

    static class FakePorts implements Ports {
        double money = 100_000;
        int withdrawals, deliveries, refunds, balanceReads;
        Eligibility eligibility = Eligibility.ELIGIBLE;
        DeliveryOutcome delivery = DeliveryOutcome.SUCCESS;
        Product deliveredProduct;
        boolean failBalance, decline, returnFalseAfterDebit, throwAfterDebit, nullWithdrawal,
                wrongAmount, wrongReportedBalance, throwDelivery, rejectRefund, throwAfterRefund;
        Runnable onWithdraw = () -> {};
        Runnable onRefund = () -> {};
        Runnable onCheck = () -> {};
        public Eligibility check(UUID id, Product product) { onCheck.run(); return eligibility; }
        public double balance(UUID id) {
            balanceReads++;
            if (failBalance) throw new IllegalStateException("balance unavailable");
            return money;
        }
        public MoneyResult withdraw(UUID id, double price) {
            withdrawals++;
            onWithdraw.run();
            if (decline) return new MoneyResult(false, 0, money);
            money -= price;
            if (throwAfterDebit) throw new IllegalStateException("provider threw after mutation");
            if (nullWithdrawal) return null;
            return new MoneyResult(!returnFalseAfterDebit, wrongAmount ? 1 : price,
                    wrongReportedBalance ? money + 10 : money);
        }
        public MoneyResult deposit(UUID id, double price) {
            refunds++;
            onRefund.run();
            if (rejectRefund) return new MoneyResult(false, 0, money);
            money += price;
            if (throwAfterRefund) throw new IllegalStateException("provider threw after credit");
            return new MoneyResult(true, price, money);
        }
        public DeliveryOutcome deliver(UUID id, Product product) {
            deliveries++;
            deliveredProduct = product;
            if (throwDelivery) throw new IllegalStateException("delivery uncertain");
            return delivery;
        }
    }
}

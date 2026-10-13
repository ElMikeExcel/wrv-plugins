package com.winkreyes.exceldeathprotection;

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

import static com.winkreyes.exceldeathprotection.PurchaseCoordinator.*;
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
    static double price(Product product) { return product == Product.BASIC ? 15_000 : 30_000; }
    Result buy(Product product) {
        assertEquals(Status.ARMED, click(product).status());
        return click(product);
    }

    @Test void basicChargesFifteenThousandAndActivatesOnce() {
        Result result = buy(Product.BASIC);
        assertEquals(Status.PURCHASED, result.status());
        assertEquals(85_000, result.balance());
        assertEquals(Product.BASIC, ports.deliveredProduct);
        assertEquals(1, ports.withdrawals);
        assertEquals(1, ports.deliveries);
        assertEquals(0, ports.refunds);
        assertTrue(journal.unresolved().isEmpty());
        assertEquals(PurchaseJournal.Phase.COMPLETED,
                journal.entries().get(result.transactionId()).phase());
    }

    @Test void premiumChargesThirtyThousandAndDeliversOnce() {
        assertEquals(Status.PURCHASED, buy(Product.PREMIUM).status());
        assertEquals(70_000, ports.money);
        assertEquals(Product.PREMIUM, ports.deliveredProduct);
    }

    @Test void productChangeRequiresFreshConfirmation() {
        assertEquals(Status.ARMED, click(Product.BASIC).status());
        assertEquals(Status.ARMED, click(Product.PREMIUM).status());
        assertEquals(0, ports.withdrawals);
        assertEquals(Status.PURCHASED, click(Product.PREMIUM).status());
        assertEquals(70_000, ports.money);
    }

    @Test void priceChangeRequiresFreshConfirmation() {
        assertEquals(Status.ARMED, click(Product.BASIC).status());
        assertEquals(Status.ARMED, coordinator.click(player, Product.BASIC, 16_000).status());
        assertEquals(0, ports.withdrawals);
        assertEquals(Status.PURCHASED, coordinator.click(player, Product.BASIC, 16_000).status());
        assertEquals(84_000, ports.money);
    }

    @Test void otherPlayersCannotConsumeConfirmation() {
        assertEquals(Status.ARMED, click(Product.BASIC).status());
        assertEquals(Status.ARMED, coordinator.click(UUID.randomUUID(), Product.BASIC, 15_000).status());
        assertEquals(0, ports.withdrawals);
    }

    @Test void exactExpiryRearmsWithoutCharge() {
        click(Product.PREMIUM);
        now.addAndGet(20_000);
        assertEquals(Status.ARMED, click(Product.PREMIUM).status());
        assertEquals(0, ports.withdrawals);
    }

    @Test void backwardClockRearmsWithoutCharge() {
        click(Product.BASIC);
        now.decrementAndGet();
        assertEquals(Status.ARMED, click(Product.BASIC).status());
        assertEquals(0, ports.withdrawals);
    }

    @Test void cancelConsumesConfirmation() {
        click(Product.BASIC);
        coordinator.clearConfirmation(player);
        assertEquals(Status.ARMED, click(Product.BASIC).status());
        assertEquals(0, ports.withdrawals);
    }

    @Test void insufficientFundsNeverArmsOrCharges() {
        ports.money = 14_999;
        assertEquals(Status.INSUFFICIENT_FUNDS, click(Product.BASIC).status());
        ports.money = 100_000;
        assertEquals(Status.ARMED, click(Product.BASIC).status());
        assertEquals(0, ports.withdrawals);
    }

    @Test void balanceRecheckedOnConfirmationAndFailedTokenConsumed() {
        click(Product.PREMIUM);
        ports.money = 1;
        assertEquals(Status.INSUFFICIENT_FUNDS, click(Product.PREMIUM).status());
        ports.money = 100_000;
        assertEquals(Status.ARMED, click(Product.PREMIUM).status());
        assertEquals(0, ports.withdrawals);
    }

    @Test void inventoryAndActiveEligibilityCheckedBeforeArmingAndConfirming() {
        ports.eligibility = Eligibility.NO_SPACE;
        assertEquals(Status.INELIGIBLE, click(Product.PREMIUM).status());
        ports.eligibility = Eligibility.ELIGIBLE;
        assertEquals(Status.ARMED, click(Product.BASIC).status());
        ports.eligibility = Eligibility.ALREADY_ACTIVE;
        assertEquals(Status.INELIGIBLE, click(Product.BASIC).status());
        ports.eligibility = Eligibility.ELIGIBLE;
        assertEquals(Status.ARMED, click(Product.BASIC).status());
        assertEquals(0, ports.withdrawals);
    }

    @Test void invalidPricesNeverArmOrReachProvider() {
        for (double invalid : new double[]{0, -1, Double.NaN, Double.POSITIVE_INFINITY}) {
            assertEquals(Status.INVALID_PRICE, coordinator.click(player, Product.BASIC, invalid).status());
        }
        assertEquals(0, ports.balanceReads);
        assertEquals(0, ports.withdrawals);
    }

    @Test void invalidBalanceAndReadExceptionFailWithoutCharge() {
        ports.money = Double.NaN;
        assertEquals(Status.UNAVAILABLE, click(Product.BASIC).status());
        ports.money = Double.POSITIVE_INFINITY;
        assertEquals(Status.UNAVAILABLE, click(Product.BASIC).status());
        ports.money = 100_000;
        click(Product.BASIC);
        ports.failBalance = true;
        assertEquals(Status.UNAVAILABLE, click(Product.BASIC).status());
        ports.failBalance = false;
        assertEquals(Status.ARMED, click(Product.BASIC).status());
        assertEquals(0, ports.withdrawals);
    }

    @Test void repeatedClicksAndCrossProductClickHitOneSecondCooldown() {
        assertEquals(Status.PURCHASED, buy(Product.PREMIUM).status());
        for (int i = 0; i < 10; i++) assertEquals(Status.COOLDOWN, click(Product.PREMIUM).status());
        assertEquals(Status.COOLDOWN, click(Product.BASIC).status());
        now.addAndGet(999);
        assertEquals(Status.COOLDOWN, click(Product.PREMIUM).status());
        now.incrementAndGet();
        assertEquals(Status.ARMED, click(Product.PREMIUM).status());
        assertEquals(1, ports.withdrawals);
    }

    @Test void knownProviderDeclineHasNoDeliveryOrRefund() {
        ports.decline = true;
        assertEquals(Status.DECLINED, buy(Product.PREMIUM).status());
        assertEquals(100_000, ports.money);
        assertEquals(0, ports.deliveries);
        assertEquals(0, ports.refunds);
        assertFalse(coordinator.isLocked(player));
        assertTrue(journal.unresolved().isEmpty());
        assertEquals(Status.ARMED, click(Product.PREMIUM).status());
    }

    @Test void declineAfterDebitLocksWithoutRefund() {
        ports.returnFalseAfterDebit = true;
        assertLocked(buy(Product.BASIC));
        assertEquals(85_000, ports.money);
        assertEquals(0, ports.deliveries);
        assertEquals(0, ports.refunds);
    }

    @Test void throwingProviderAfterDebitLocksAndNeverRetries() {
        ports.throwAfterDebit = true;
        assertLocked(buy(Product.BASIC));
        assertEquals(85_000, ports.money);
        assertEquals(0, ports.deliveries);
        assertEquals(0, ports.refunds);
    }

    @Test void nullWithdrawalResponseLocks() {
        ports.nullWithdrawal = true;
        assertLocked(buy(Product.BASIC));
        assertEquals(0, ports.deliveries);
    }

    @Test void malformedWithdrawalAmountLocks() {
        ports.wrongAmount = true;
        assertLocked(buy(Product.BASIC));
        assertEquals(0, ports.deliveries);
    }

    @Test void responseBalanceMustMatchFreshObservedBalance() {
        ports.wrongReportedBalance = true;
        assertLocked(buy(Product.BASIC));
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
        assertEquals(Status.REFUNDED, buy(Product.PREMIUM).status());
        assertEquals(100_000, ports.money);
        assertEquals(1, ports.refunds);
        assertTrue(journal.unresolved().isEmpty());
    }

    @Test void unknownDeliveryNeverRefunds() {
        ports.delivery = DeliveryOutcome.UNKNOWN;
        assertLocked(buy(Product.PREMIUM));
        assertEquals(0, ports.refunds);
    }

    @Test void deliveryExceptionNeverAssumesRollbackOrRefunds() {
        ports.throwDelivery = true;
        assertLocked(buy(Product.PREMIUM));
        assertEquals(0, ports.refunds);
    }

    @Test void nullDeliveryNeverRefunds() {
        ports.delivery = null;
        assertLocked(buy(Product.BASIC));
        assertEquals(0, ports.refunds);
    }

    @Test void refundFailureLocksAndNeverRetries() {
        ports.delivery = DeliveryOutcome.FAILED_ROLLED_BACK;
        ports.rejectRefund = true;
        assertLocked(buy(Product.BASIC));
        assertEquals(85_000, ports.money);
        assertEquals(1, ports.refunds);
    }

    @Test void refundExceptionAfterCreditLocksAndNeverDoubleRefunds() {
        ports.delivery = DeliveryOutcome.FAILED_ROLLED_BACK;
        ports.throwAfterRefund = true;
        assertLocked(buy(Product.BASIC));
        assertEquals(100_000, ports.money);
        assertEquals(1, ports.refunds);
    }

    @Test void reentrantProviderCallbackCannotUseConsumedToken() {
        ports.onWithdraw = () -> assertEquals(Status.BUSY, click(Product.PREMIUM).status());
        assertEquals(Status.PURCHASED, buy(Product.PREMIUM).status());
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
        click(Product.PREMIUM);
        Thread thread = new Thread(() -> firstResult.set(click(Product.PREMIUM)));
        thread.start();
        try {
            assertTrue(inside.await(5, TimeUnit.SECONDS));
            assertEquals(Status.BUSY, click(Product.PREMIUM).status());
        } finally { release.countDown(); }
        thread.join(5_000);
        assertFalse(thread.isAlive());
        assertEquals(Status.PURCHASED, firstResult.get().status());
        assertEquals(1, ports.withdrawals);
    }

    @Test void writeFailureBeforeWithdrawalPreventsAllMoneyCalls() throws Exception {
        coordinator = create(new FaultyJournal(directory.resolve("broken"), PurchaseJournal.Phase.WITHDRAW_INTENT));
        assertEquals(Status.UNAVAILABLE, buy(Product.BASIC).status());
        assertEquals(0, ports.withdrawals);
        assertEquals(0, ports.refunds);
        assertEquals(Status.LOCKED, click(Product.BASIC).status());
    }

    @Test void journalFailureAfterDebitStopsDeliveryAndRefund() throws Exception {
        coordinator = create(new FaultyJournal(directory.resolve("broken"), PurchaseJournal.Phase.WITHDRAWN));
        assertLocked(buy(Product.BASIC));
        assertEquals(0, ports.deliveries);
        assertEquals(0, ports.refunds);
    }

    @Test void journalFailureAfterDeliveryDoesNotRefundDeliveredItem() throws Exception {
        coordinator = create(new FaultyJournal(directory.resolve("broken"), PurchaseJournal.Phase.COMPLETED));
        assertLocked(buy(Product.BASIC));
        assertEquals(1, ports.deliveries);
        assertEquals(0, ports.refunds);
    }

    @Test void failedRefundIntentWritePreventsDeposit() throws Exception {
        coordinator = create(new FaultyJournal(directory.resolve("broken"), PurchaseJournal.Phase.REFUND_INTENT));
        ports.delivery = DeliveryOutcome.FAILED_ROLLED_BACK;
        assertLocked(buy(Product.BASIC));
        assertEquals(0, ports.refunds);
    }

    @Test void failedRefundCompletionWriteNeverRetriesDeposit() throws Exception {
        coordinator = create(new FaultyJournal(directory.resolve("broken"), PurchaseJournal.Phase.REFUNDED));
        ports.delivery = DeliveryOutcome.FAILED_ROLLED_BACK;
        assertLocked(buy(Product.BASIC));
        assertEquals(1, ports.refunds);
        assertEquals(100_000, ports.money);
    }

    @Test void interruptedPurchaseLocksSamePlayerAfterRestartWithoutReplaying() throws Exception {
        ports.delivery = DeliveryOutcome.UNKNOWN;
        Result result = buy(Product.PREMIUM);
        assertEquals(Status.LOCKED, result.status());
        coordinator = create(new PurchaseJournal(directory.resolve("purchases.journal")));
        assertTrue(coordinator.lockedPlayers().contains(player));
        assertEquals(Status.LOCKED, click(Product.BASIC).status());
        assertEquals(Status.ARMED, coordinator.click(UUID.randomUUID(), Product.BASIC, 15_000).status());
        assertEquals(1, ports.withdrawals);
        assertEquals(0, ports.refunds);
    }

    void assertLocked(Result result) {
        assertEquals(Status.LOCKED, result.status());
        assertTrue(coordinator.isLocked(player));
        assertEquals(Status.LOCKED, click(Product.BASIC).status());
        assertEquals(Status.LOCKED, click(Product.PREMIUM).status());
        assertEquals(1, ports.withdrawals);
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
        public Eligibility check(UUID id, Product product) { return eligibility; }
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

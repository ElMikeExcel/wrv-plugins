package com.winkreyes.exceldeathprotection;

import java.io.IOException;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.LongSupplier;

/** Pure Java purchase policy. All Bukkit/Vault work is delegated to the ports. */
public final class PurchaseCoordinator {
    public enum Product { BASIC, PREMIUM }
    public enum Eligibility { ELIGIBLE, OFFLINE, ALREADY_ACTIVE, NO_SPACE, UNAVAILABLE }
    public enum DeliveryOutcome { SUCCESS, FAILED_ROLLED_BACK, UNKNOWN }
    public enum Status {
        ARMED, PURCHASED, INELIGIBLE, INSUFFICIENT_FUNDS, DECLINED, REFUNDED,
        COOLDOWN, BUSY, LOCKED, UNAVAILABLE, INVALID_PRICE
    }

    /** Exact provider response; null, malformed or inconsistent values are ambiguous. */
    public record MoneyResult(boolean success, double amount, double balance) {}
    public record Result(Status status, Product product, double price, double balance,
                         UUID transactionId, String detail) {}

    public interface Ports {
        Eligibility check(UUID playerId, Product product);
        double balance(UUID playerId);
        MoneyResult withdraw(UUID playerId, double price);
        MoneyResult deposit(UUID playerId, double price);
        /**
         * SUCCESS requires observed delivery and a completed player-data save call.
         * FAILED_ROLLED_BACK requires observed rollback AND a completed save call.
         * Any uncertain inventory, protection state or persistence is UNKNOWN.
         * A void Bukkit save call cannot prove disk durability: this interface
         * does not promise an atomic Vault/playerdata transaction or crash safety.
         */
        DeliveryOutcome deliver(UUID playerId, Product product);
    }

    private record Confirmation(UUID playerId, Product product, double price, long armedAt) {}
    private static final class State {
        final AtomicBoolean busy = new AtomicBoolean();
        volatile boolean locked;
        volatile Confirmation confirmation;
        boolean completedOnce;
        long lastSuccess;
    }

    private final Ports ports;
    private final PurchaseJournal journal;
    private final LongSupplier clock;
    private final long confirmWindowMs;
    private final long successCooldownMs;
    private final ConcurrentHashMap<UUID, State> states = new ConcurrentHashMap<>();

    public PurchaseCoordinator(Ports ports, PurchaseJournal journal, LongSupplier clock,
                               long confirmWindowMs, long successCooldownMs) {
        this.ports = Objects.requireNonNull(ports);
        this.journal = Objects.requireNonNull(journal);
        this.clock = Objects.requireNonNull(clock);
        if (confirmWindowMs <= 0 || successCooldownMs < 1_000L) {
            throw new IllegalArgumentException("Positive confirm window and >= 1s cooldown required");
        }
        this.confirmWindowMs = confirmWindowMs;
        this.successCooldownMs = successCooldownMs;
        journal.unresolved().values().forEach(entry -> state(entry.playerId()).locked = true);
    }

    public void cancel(UUID playerId) { state(playerId).confirmation = null; }
    public void clearConfirmation(UUID playerId) { cancel(playerId); }
    public boolean isLocked(UUID playerId) { return state(playerId).locked; }
    public Set<UUID> lockedPlayers() {
        Set<UUID> result = new HashSet<>();
        states.forEach((id, state) -> { if (state.locked) result.add(id); });
        return Set.copyOf(result);
    }

    public Result click(UUID playerId, Product product, double price) {
        Objects.requireNonNull(playerId);
        Objects.requireNonNull(product);
        State state = state(playerId);
        if (!state.busy.compareAndSet(false, true)) {
            return result(Status.BUSY, product, price, Double.NaN, null, "Purchase already in progress");
        }
        try {
            return guardedClick(playerId, state, product, price);
        } finally {
            state.busy.set(false);
        }
    }

    private Result guardedClick(UUID id, State state, Product product, double price) {
        if (state.locked) return result(Status.LOCKED, product, price, Double.NaN, null,
                "An administrator must reconcile an earlier purchase");
        if (!journal.healthy()) return result(Status.UNAVAILABLE, product, price, Double.NaN, null,
                "Purchase journal is unavailable");
        if (!Double.isFinite(price) || price <= 0) {
            state.confirmation = null;
            return result(Status.INVALID_PRICE, product, price, Double.NaN, null, "Invalid configured price");
        }
        long now = clock.getAsLong();
        if (state.completedOnce && (now < state.lastSuccess
                || now - state.lastSuccess < successCooldownMs)) {
            state.confirmation = null;
            return result(Status.COOLDOWN, product, price, Double.NaN, null, "Please wait before buying again");
        }
        Confirmation token = state.confirmation;
        // Every attempted confirmation consumes its token, including precheck failures.
        state.confirmation = null;
        Eligibility eligibility;
        double balance;
        try {
            eligibility = ports.check(id, product);
            if (eligibility != Eligibility.ELIGIBLE) {
                return result(Status.INELIGIBLE, product, price, Double.NaN, null,
                        eligibility == null ? "UNAVAILABLE" : eligibility.name());
            }
            balance = ports.balance(id);
            if (!validBalance(balance)) throw new IllegalStateException("Invalid provider balance");
        } catch (RuntimeException unavailable) {
            return result(Status.UNAVAILABLE, product, price, Double.NaN, null, "Eligibility or balance unavailable");
        }
        if (balance < price) return result(Status.INSUFFICIENT_FUNDS, product, price, balance, null,
                "Balance is below the price");
        if (token == null || !token.playerId().equals(id) || token.product() != product
                || Double.compare(token.price(), price) != 0 || now < token.armedAt()
                || now - token.armedAt() >= confirmWindowMs) {
            state.confirmation = new Confirmation(id, product, price, now);
            return result(Status.ARMED, product, price, balance, null, "Tap again to confirm this product and price");
        }

        UUID transactionId = UUID.randomUUID();
        try {
            append(transactionId, id, product, price, PurchaseJournal.Phase.WITHDRAW_INTENT);
        } catch (IOException unavailable) {
            state.locked = true;
            return result(Status.UNAVAILABLE, product, price, balance, transactionId,
                    "Journal write failed before withdrawal; no withdrawal was attempted");
        }
        MoneyResult debit;
        double afterDebit;
        try {
            debit = ports.withdraw(id, price);
            afterDebit = ports.balance(id);
        } catch (RuntimeException ambiguous) {
            return lock(id, state, product, price, transactionId, "Withdrawal outcome is unknown");
        }
        if (knownDecline(debit, balance, afterDebit)) {
            try { append(transactionId, id, product, price, PurchaseJournal.Phase.DECLINED); }
            catch (IOException failure) {
                return lock(id, state, product, price, transactionId, "Declined withdrawal could not be journaled");
            }
            return result(Status.DECLINED, product, price, afterDebit, transactionId, "Provider declined; balance unchanged");
        }
        if (!knownSuccess(debit, price, balance - price, afterDebit)) {
            return lock(id, state, product, price, transactionId, "Withdrawal response or balance is inconsistent");
        }
        try { append(transactionId, id, product, price, PurchaseJournal.Phase.WITHDRAWN); }
        catch (IOException failure) {
            return lock(id, state, product, price, transactionId, "Debit succeeded but could not be journaled");
        }

        DeliveryOutcome delivery;
        try { delivery = ports.deliver(id, product); }
        catch (RuntimeException ambiguous) { delivery = DeliveryOutcome.UNKNOWN; }
        if (delivery == DeliveryOutcome.SUCCESS) {
            try { append(transactionId, id, product, price, PurchaseJournal.Phase.COMPLETED); }
            catch (IOException failure) {
                return lock(id, state, product, price, transactionId, "Delivery completed but could not be journaled");
            }
            state.completedOnce = true;
            state.lastSuccess = clock.getAsLong();
            return result(Status.PURCHASED, product, price, afterDebit, transactionId, "Purchase completed");
        }
        if (delivery != DeliveryOutcome.FAILED_ROLLED_BACK) {
            return lock(id, state, product, price, transactionId, "Delivery or rollback is unknown; refund withheld");
        }
        double beforeRefund;
        try {
            beforeRefund = ports.balance(id);
            if (!validBalance(beforeRefund) || !Double.isFinite(beforeRefund + price)) {
                throw new IllegalStateException("Invalid refund balance");
            }
            append(transactionId, id, product, price, PurchaseJournal.Phase.REFUND_INTENT);
        } catch (IOException | RuntimeException failure) {
            return lock(id, state, product, price, transactionId, "Rollback verified; refund could not be safely started");
        }
        MoneyResult refund;
        double afterRefund;
        try {
            refund = ports.deposit(id, price);
            afterRefund = ports.balance(id);
        } catch (RuntimeException ambiguous) {
            return lock(id, state, product, price, transactionId, "Refund outcome is unknown; no retry attempted");
        }
        if (!knownSuccess(refund, price, beforeRefund + price, afterRefund)) {
            return lock(id, state, product, price, transactionId, "Refund was rejected or inconsistent; no retry attempted");
        }
        try { append(transactionId, id, product, price, PurchaseJournal.Phase.REFUNDED); }
        catch (IOException failure) {
            return lock(id, state, product, price, transactionId, "Refund succeeded but could not be journaled");
        }
        return result(Status.REFUNDED, product, price, afterRefund, transactionId,
                "Delivery failed; rollback and refund verified");
    }

    private Result lock(UUID id, State state, Product product, double price, UUID transactionId, String detail) {
        state.locked = true;
        state.confirmation = null;
        try { append(transactionId, id, product, price, PurchaseJournal.Phase.RECONCILE); }
        catch (IOException ignored) { /* Prior write-ahead phase remains nonterminal on disk. */ }
        return result(Status.LOCKED, product, price, Double.NaN, transactionId, detail);
    }

    private void append(UUID transactionId, UUID id, Product product, double price, PurchaseJournal.Phase phase)
            throws IOException {
        journal.append(new PurchaseJournal.Entry(transactionId, id, product, price, phase, clock.getAsLong()));
    }

    private static boolean validBalance(double balance) { return Double.isFinite(balance) && balance >= 0; }
    private static boolean same(double a, double b) {
        return Double.isFinite(a) && Double.isFinite(b) && Math.abs(a - b) < 0.000001d;
    }
    private static boolean knownDecline(MoneyResult response, double before, double after) {
        return response != null && !response.success() && same(response.amount(), 0)
                && validBalance(response.balance()) && same(response.balance(), before) && same(after, before);
    }
    private static boolean knownSuccess(MoneyResult response, double price, double expected, double observed) {
        return response != null && response.success() && same(response.amount(), price)
                && validBalance(response.balance()) && validBalance(observed)
                && same(response.balance(), expected) && same(observed, expected);
    }
    private State state(UUID id) { return states.computeIfAbsent(id, ignored -> new State()); }
    private static Result result(Status status, Product product, double price, double balance,
                                 UUID transactionId, String detail) {
        return new Result(status, product, price, balance, transactionId, detail);
    }
}

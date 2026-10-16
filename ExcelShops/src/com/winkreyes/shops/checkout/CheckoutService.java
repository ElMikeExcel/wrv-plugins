// SPDX-License-Identifier: MIT
package com.winkreyes.shops.checkout;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Serialized, final-confirm-only checkout coordinator. Vault and Minecraft storage are not
 * one atomic database: ambiguous calls or durability failures halt every checkout for manual
 * reconciliation. There is deliberately no automatic replay or recovery-payment API.
 */
public final class CheckoutService implements AutoCloseable {
    public static final int MAX_ACTIVE_QUOTES = 4096;
    public static final int MAX_EVIDENCE_BYTES = 1024 * 1024;
    public static final Duration MAX_QUOTE_TTL = Duration.ofMinutes(5);

    public enum Status { SUCCESS, NO_CHARGE, REFUNDED, RECOVERY_REQUIRED }
    public enum MoneyResult { SUCCESS, DECLINED, UNCERTAIN }
    public enum MutationResult { APPLIED, UNCHANGED, UNCERTAIN }
    public enum Stage {
        WITHDRAW_INTENT, WITHDRAWN, TRANSFER_INTENT, TRANSFER_APPLIED,
        PAYOUT_INTENT, PAYOUT_DECLINED, PAID, ROLLBACK_INTENT, ROLLED_BACK,
        REFUND_INTENT, REFUNDED, NO_CHARGE, COMMITTED, RECOVERY_REQUIRED,
        LEGACY_INTENT, LEGACY_COMMITTED, LEGACY_NO_CHANGE;

        public boolean terminal() {
            return this == REFUNDED || this == NO_CHARGE || this == COMMITTED
                    || this == LEGACY_COMMITTED || this == LEGACY_NO_CHANGE;
        }
    }

    public record Quote(UUID id, UUID buyerId, UUID sellerId, String offerId, long revision,
                        int quantity, long amountMinorUnits, String fingerprint, Instant expiresAt) { }

    public record Result(Status status, String message, UUID transactionId) { }

    public record Preparation(Transfer transfer, String rejection) {
        public Preparation {
            if ((transfer == null) == (rejection == null)) {
                throw new IllegalArgumentException("Preparation must have a transfer or a rejection.");
            }
        }
        public static Preparation ready(Transfer transfer) {
            return new Preparation(Objects.requireNonNull(transfer), null);
        }
        public static Preparation denied(String reason) {
            return new Preparation(null, Objects.requireNonNull(reason));
        }
    }

    public interface Transfer {
        /** Read-only, nonempty pre/post inventory snapshots and account evidence for reconciliation. */
        default String auditEvidence() { return ""; }
        /**
         * Recheck captured snapshots immediately before applying: an economy callback can be
         * reentrant. Return UNCHANGED only when neither stock nor delivery was altered.
         */
        MutationResult apply() throws Exception;
        /** Restore captured snapshots. True means both stock and delivery are fully restored. */
        boolean rollback() throws Exception;
    }

    public interface Adapter {
        /** Throw before any work if this is not the server's primary thread. */
        void assertMainThread();
        /**
         * Read-only final validation: current owner, price/revision, exact metadata and stock
         * fingerprint, protection, access, buyer funds and capacity. Capture both inventory
         * snapshots here. No money/inventory mutation or external event dispatch is allowed.
         */
        Preparation prepare(Quote quote) throws Exception;
        /** DECLINED guarantees no funds moved; uncertain responses must be UNCERTAIN. */
        MoneyResult withdraw(UUID accountId, long amountMinorUnits) throws Exception;
        /** DECLINED guarantees no funds moved; uncertain responses must be UNCERTAIN. */
        MoneyResult deposit(UUID accountId, long amountMinorUnits) throws Exception;
    }

    public interface Journal extends AutoCloseable {
        boolean requiresRecovery();
        String recoveryReason();
        /** Must force durable storage before returning, not merely flush a writer. */
        void record(Quote quote, Stage stage, String detail) throws IOException;
        /** Close must retain a dirty session marker if recovery or a pending transaction exists. */
        @Override void close() throws IOException;
        /** Release resources while retaining the durable dirty-session marker. */
        void closeUnclean() throws IOException;
    }

    private final Adapter adapter;
    private final Journal journal;
    private final Clock clock;
    private final Duration quoteTtl;
    private final Map<UUID, Quote> quotes = new HashMap<>();
    private boolean executing;
    private ExternalLease externalLease;
    private boolean recovery;
    private String recoveryReason;
    private boolean closed;

    public CheckoutService(Adapter adapter, Journal journal, Clock clock, Duration quoteTtl) {
        this.adapter = Objects.requireNonNull(adapter);
        this.journal = Objects.requireNonNull(journal);
        this.clock = Objects.requireNonNull(clock);
        this.quoteTtl = Objects.requireNonNull(quoteTtl);
        if (quoteTtl.isZero() || quoteTtl.isNegative() || quoteTtl.compareTo(MAX_QUOTE_TTL) > 0) {
            throw new IllegalArgumentException("Quote lifetime must be positive and at most five minutes.");
        }
        recovery = journal.requiresRecovery();
        recoveryReason = recovery ? journal.recoveryReason() : "";
    }

    /**
     * Serializes a guarded external checkout with native checkouts, including read-only external
     * validation that may call reentrant plugins. The caller must close the lease in a finally
     * block and journal every external side effect in its own durable transaction journal.
     */
    public synchronized ExternalLease tryExternalLease() {
        adapter.assertMainThread();
        if (closed || recovery || executing) return null;
        executing = true;
        externalLease = new ExternalLease();
        return externalLease;
    }

    /** An identity-bound, primary-thread lease; stale or repeated closes cannot release a newer lease. */
    public final class ExternalLease implements AutoCloseable {
        private ExternalLease() { }

        @Override public void close() {
            synchronized (CheckoutService.this) {
                adapter.assertMainThread();
                if (externalLease != this) return;
                externalLease = null;
                executing = false;
            }
        }
    }

    /**
     * Latches an external ambiguity globally. The already-forced active-session marker must be
     * retained at shutdown; transaction-specific evidence belongs in the external journal.
     */
    public synchronized void pauseExternal(String reason) {
        adapter.assertMainThread();
        requireText(reason, MAX_EVIDENCE_BYTES, "recovery reason");
        if (closed && !recovery) throw new IllegalStateException("Checkout is closed.");
        if (!recovery) recoveryReason = reason;
        recovery = true;
        quotes.clear();
    }

    /** Creating, replacing, navigating away from, or cancelling a quote never calls the economy. */
    public synchronized Quote quote(UUID buyerId, UUID sellerId, String offerId, long revision,
                                    int quantity, long totalMinorUnits, String fingerprint) {
        adapter.assertMainThread();
        if (closed || recovery || executing) throw new IllegalStateException("Checkout is unavailable.");
        Objects.requireNonNull(buyerId, "buyerId");
        if (buyerId.equals(sellerId)) throw new IllegalArgumentException("You cannot buy your own listing.");
        requireText(offerId, 128, "offerId");
        requireText(fingerprint, 1024, "fingerprint");
        if (revision < 0) throw new IllegalArgumentException("Revision cannot be negative.");
        Money.requireQuantity(quantity);
        Money.requirePositive(totalMinorUnits);
        Instant now = clock.instant();
        quotes.values().removeIf(q -> q.buyerId().equals(buyerId) || !now.isBefore(q.expiresAt()));
        if (quotes.size() >= MAX_ACTIVE_QUOTES) throw new IllegalStateException("Too many pending quotes.");
        Quote quote = new Quote(UUID.randomUUID(), buyerId, sellerId, offerId, revision, quantity,
                totalMinorUnits, fingerprint, now.plus(quoteTtl));
        quotes.put(quote.id(), quote);
        return quote;
    }

    public synchronized boolean cancel(UUID quoteId, UUID buyerId) {
        adapter.assertMainThread();
        Quote quote = quotes.get(quoteId);
        return quote != null && quote.buyerId().equals(buyerId) && quotes.remove(quoteId, quote);
    }

    public synchronized void invalidateBuyer(UUID buyerId) {
        adapter.assertMainThread();
        quotes.values().removeIf(quote -> quote.buyerId().equals(buyerId));
    }

    public synchronized void invalidateAll() {
        adapter.assertMainThread();
        quotes.clear();
    }

    public synchronized boolean recoveryRequired() { return recovery; }
    public synchronized String recoveryReason() { return recoveryReason; }

    public synchronized Result confirm(UUID quoteId, UUID buyerId) {
        adapter.assertMainThread();
        if (recovery) return result(Status.RECOVERY_REQUIRED, "Checkout is paused for administrator reconciliation.", quoteId);
        if (closed || executing) return result(Status.NO_CHARGE, "Checkout is unavailable. No money was taken.", quoteId);
        Quote quote = quotes.get(quoteId);
        if (quote == null || !quote.buyerId().equals(buyerId)) {
            return result(Status.NO_CHARGE, "This confirmation is no longer valid. No money was taken.", quoteId);
        }
        quotes.remove(quoteId); // Consume before callbacks, even when validation or a payment fails.
        if (!clock.instant().isBefore(quote.expiresAt())) {
            return result(Status.NO_CHARGE, "This price quote expired. Open a new confirmation.", quoteId);
        }
        executing = true;
        try {
            return execute(quote);
        } catch (Error fatal) {
            halt(quote, "Fatal interruption; transaction state requires reconciliation.");
            throw fatal;
        } finally {
            executing = false;
        }
    }

    private Result execute(Quote quote) {
        final Preparation preparation;
        try {
            preparation = Objects.requireNonNull(adapter.prepare(quote), "prepare returned null");
        } catch (Exception ex) {
            return result(Status.NO_CHARGE, "Could not recheck this listing. No money was taken.", quote.id());
        }
        if (preparation.rejection() != null) {
            return result(Status.NO_CHARGE, preparation.rejection(), quote.id());
        }
        final String evidence;
        try {
            evidence = preparation.transfer().auditEvidence();
            if (evidence == null || evidence.isBlank()
                    || evidence.getBytes(StandardCharsets.UTF_8).length > MAX_EVIDENCE_BYTES) {
                return result(Status.NO_CHARGE, "Reconciliation evidence is missing or too large. No money was taken.", quote.id());
            }
        } catch (Exception ex) {
            return result(Status.NO_CHARGE, "Could not capture reconciliation evidence. No money was taken.", quote.id());
        }
        if (!record(quote, Stage.WITHDRAW_INTENT, "Before buyer withdrawal; evidence: " + evidence)) {
            return result(Status.NO_CHARGE, "Journal unavailable; no money was taken. Checkout has been paused.", quote.id());
        }
        MoneyResult withdrawn;
        try {
            withdrawn = adapter.withdraw(quote.buyerId(), quote.amountMinorUnits());
        } catch (Exception ex) {
            return halt(quote, "Buyer withdrawal threw; do not retry withdrawal or refund automatically.");
        }
        if (withdrawn == MoneyResult.DECLINED) {
            if (!record(quote, Stage.NO_CHARGE, "Buyer withdrawal definitively declined")) {
                return result(Status.NO_CHARGE, "Payment was declined; no money was taken. Checkout has been paused.", quote.id());
            }
            return result(Status.NO_CHARGE, "Payment was declined. No money was taken.", quote.id());
        }
        if (withdrawn != MoneyResult.SUCCESS) return halt(quote, "Buyer withdrawal outcome is uncertain.");
        if (!record(quote, Stage.WITHDRAWN, "Buyer withdrawal succeeded")
                || !record(quote, Stage.TRANSFER_INTENT, "Before inventory transfer")) {
            return haltedResult(quote);
        }
        MutationResult applied;
        try {
            applied = preparation.transfer().apply();
        } catch (Exception ex) {
            return halt(quote, "Inventory transfer threw; delivery may be partial. No automatic retry.");
        }
        if (applied == MutationResult.UNCHANGED) return refund(quote);
        if (applied != MutationResult.APPLIED) return halt(quote, "Inventory transfer outcome is uncertain.");
        if (!record(quote, Stage.TRANSFER_APPLIED, "Inventory transfer succeeded")) return haltedResult(quote);
        if (quote.sellerId() != null) {
            if (!record(quote, Stage.PAYOUT_INTENT, "Before seller payment")) return haltedResult(quote);
            final MoneyResult payout;
            try {
                payout = adapter.deposit(quote.sellerId(), quote.amountMinorUnits());
            } catch (Exception ex) {
                return halt(quote, "Seller payment threw; do not refund or repeat payment automatically.");
            }
            if (payout == MoneyResult.DECLINED) return rollbackAndRefund(quote, preparation.transfer());
            if (payout != MoneyResult.SUCCESS) return halt(quote, "Seller payment outcome is uncertain.");
            if (!record(quote, Stage.PAID, "Seller payment succeeded")) return haltedResult(quote);
        }
        if (!record(quote, Stage.COMMITTED, "Purchase finished")) return haltedResult(quote);
        return result(Status.SUCCESS, "Purchase complete.", quote.id());
    }

    private Result rollbackAndRefund(Quote quote, Transfer transfer) {
        if (!record(quote, Stage.PAYOUT_DECLINED, "Seller payment definitively declined")
                || !record(quote, Stage.ROLLBACK_INTENT, "Before restoring inventories")) return haltedResult(quote);
        try {
            if (!transfer.rollback()) return halt(quote, "Could not prove complete inventory restoration.");
        } catch (Exception ex) {
            return halt(quote, "Inventory restoration threw; refund withheld for reconciliation.");
        }
        if (!record(quote, Stage.ROLLED_BACK, "Both inventories restored")) return haltedResult(quote);
        return refund(quote);
    }

    private Result refund(Quote quote) {
        if (!record(quote, Stage.REFUND_INTENT, "Before returning buyer payment")) return haltedResult(quote);
        final MoneyResult refunded;
        try {
            refunded = adapter.deposit(quote.buyerId(), quote.amountMinorUnits());
        } catch (Exception ex) {
            return halt(quote, "Buyer refund threw; do not repeat it automatically.");
        }
        if (refunded != MoneyResult.SUCCESS) return halt(quote, "Buyer refund failed or is uncertain; manual reconciliation required.");
        if (!record(quote, Stage.REFUNDED, "Buyer payment returned; no delivered items retained")) return haltedResult(quote);
        return result(Status.REFUNDED, "Purchase could not finish. Your payment was returned.", quote.id());
    }

    private boolean record(Quote quote, Stage stage, String detail) {
        // A callback can disable or pause checkout before control returns to this transaction.
        if (closed || recovery) return false;
        try {
            journal.record(quote, stage, detail);
            return true;
        } catch (Exception ex) {
            recovery = true;
            recoveryReason = "Journal failure at " + stage + "; inspect transaction " + quote.id() + ".";
            quotes.clear();
            return false;
        }
    }

    private Result halt(Quote quote, String reason) {
        recovery = true;
        recoveryReason = reason + " Transaction " + quote.id() + ".";
        quotes.clear();
        try { journal.record(quote, Stage.RECOVERY_REQUIRED, reason); }
        catch (Exception ignored) { /* The existing durable intent / dirty session remains. */ }
        return haltedResult(quote);
    }

    private Result haltedResult(Quote quote) {
        return result(Status.RECOVERY_REQUIRED,
                "Purchase needs administrator reconciliation. Do not repeat it; reference " + quote.id() + ".", quote.id());
    }

    private static Result result(Status status, String message, UUID id) {
        return new Result(status, message, id);
    }

    private static void requireText(String text, int maximum, String name) {
        if (text == null || text.isBlank() || text.length() > maximum || text.indexOf('\0') >= 0) {
            throw new IllegalArgumentException("Invalid " + name + ".");
        }
    }

    /**
     * Caller MUST successfully save affected player data and worlds first. A clean marker is
     * meaningful only after that save. If saving fails, do not call close; keep the dirty marker.
     */
    @Override public synchronized void close() throws IOException {
        adapter.assertMainThread();
        if (closed) return;
        quotes.clear();
        closed = true;
        if (executing && !recovery) {
            recovery = true;
            recoveryReason = "Shutdown interrupted an active checkout; durability is unconfirmed.";
        }
        if (recovery) journal.closeUnclean();
        else journal.close();
    }

    /** Use when player/world saving failed or orderly shutdown could not be established. */
    public synchronized void closeUnclean() throws IOException {
        adapter.assertMainThread();
        if (closed) return;
        quotes.clear();
        closed = true;
        if (!recovery) recoveryReason = "Shutdown was not confirmed durable.";
        recovery = true;
        journal.closeUnclean();
    }
}

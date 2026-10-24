package com.winkreyes.excelcosmetics.core;

import java.util.UUID;

/**
 * The buy path — the atomicity story is the product. ExcelShops'
 * special-product shelf dispatches a console command; this service
 * charges Vault and records the grant as one guarded sequence:
 *
 *   validate SKU -> check economy -> check balance -> withdraw
 *   -> apply grant -> (on grant failure) refund the withdrawal.
 *
 * A failed charge NEVER grants; a failed grant NEVER keeps the
 * money. The two seams (EconomyGateway, GrantApplier) are
 * interfaces so the whole matrix is unit-testable with fakes.
 */
public final class PurchaseService {

    public interface EconomyGateway {
        boolean available();
        double balance(UUID player);
        /** Returns true only when the money actually moved. */
        boolean withdraw(UUID player, double amount);
        boolean deposit(UUID player, double amount);
    }

    public interface GrantApplier {
        /** Apply + persist the grant. False = it did not stick. */
        boolean apply(UUID player, Kind kind, String id, int weeks, boolean own);
    }

    /** Read-only ownership probe (0.1.2): true when the player holds
     *  this SKU FOREVER (the own-it tier). Checked before any money moves. */
    public interface OwnershipCheck {
        boolean ownsForever(UUID player, Kind kind, String id);
    }

    public enum Code {
        SUCCESS, UNKNOWN_SKU, NOT_FOR_SALE, ALREADY_OWNED, NO_ECONOMY, INSUFFICIENT_FUNDS,
        CHARGE_FAILED, GRANT_FAILED_REFUNDED, GRANT_FAILED_REFUND_LOST
    }

    public record Result(Code code, double price, String detail) {
        public boolean success() { return code == Code.SUCCESS; }
    }

    private final Catalog catalog;
    private final EconomyGateway economy;
    private final GrantApplier applier;
    private final OwnershipCheck ownership;

    public PurchaseService(Catalog catalog, EconomyGateway economy, GrantApplier applier) {
        this(catalog, economy, applier, null);
    }

    public PurchaseService(Catalog catalog, EconomyGateway economy, GrantApplier applier,
                           OwnershipCheck ownership) {
        this.catalog = catalog;
        this.economy = economy;
        this.applier = applier;
        this.ownership = ownership;
    }

    public Result buy(UUID player, Kind kind, String id, int weeks, boolean own) {
        if (kind == null || !catalog.exists(kind, id)) {
            return new Result(Code.UNKNOWN_SKU, 0, "unknown sku " + kind + ":" + id);
        }
        double price = catalog.prices().price(kind, weeks, own);
        if (!catalog.forSale(kind, id)) {
            return new Result(Code.NOT_FOR_SALE, price, "retired sku " + kind + ":" + id);
        }
        // 0.1.2: owned forever already -> refuse BOTH rent and own
        // before any money moves (0.1.1 charged again for nothing).
        if (ownership != null && ownership.ownsForever(player, kind, id)) {
            return new Result(Code.ALREADY_OWNED, price, "already owned forever");
        }
        if (!economy.available()) {
            return new Result(Code.NO_ECONOMY, price, "no economy available");
        }
        if (economy.balance(player) < price) {
            return new Result(Code.INSUFFICIENT_FUNDS, price, "balance below price");
        }
        if (!economy.withdraw(player, price)) {
            return new Result(Code.CHARGE_FAILED, price, "withdraw declined");
        }
        boolean granted;
        try {
            granted = applier.apply(player, kind, id, weeks, own);
        } catch (RuntimeException e) {
            granted = false;
        }
        if (!granted) {
            boolean refunded = false;
            try {
                refunded = economy.deposit(player, price);
            } catch (RuntimeException ignored) {
                refunded = false;
            }
            return refunded
                    ? new Result(Code.GRANT_FAILED_REFUNDED, price, "grant failed; charge refunded")
                    : new Result(Code.GRANT_FAILED_REFUND_LOST, price, "grant failed AND refund failed — admin attention required");
        }
        return new Result(Code.SUCCESS, price, "granted " + kind + ":" + id);
    }
}

// ExcelCosmetics — an Excel plugin by WinkReyesValley. (Excel signature — MikeExcel)

// SPDX-License-Identifier: MIT
package com.winkreyes.shops.checkout;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Exact, bounded two-decimal currency. Never calculate a price with a double. */
public final class Money {
    public static final long MAX_MINOR_UNITS = 10_000_000_000L; // 100,000,000.00
    public static final int MAX_QUANTITY = 2304; // One 36-slot inventory of 64-stacks.

    private Money() { }

    public static long parse(String decimal) {
        if (decimal == null || !decimal.matches("(?:0|[1-9][0-9]{0,8})(?:\\.[0-9]{1,2})?")) {
            throw new IllegalArgumentException("Use a positive price with at most two decimal places.");
        }
        try {
            return requirePositive(new BigDecimal(decimal).movePointRight(2).longValueExact());
        } catch (ArithmeticException ex) {
            throw new IllegalArgumentException("Price is out of range.", ex);
        }
    }

    public static long requirePositive(long minorUnits) {
        if (minorUnits <= 0 || minorUnits > MAX_MINOR_UNITS) {
            throw new IllegalArgumentException("Price must be between 0.01 and 100000000.00.");
        }
        return minorUnits;
    }

    public static int requireQuantity(int quantity) {
        if (quantity < 1 || quantity > MAX_QUANTITY) {
            throw new IllegalArgumentException("Quantity must be between 1 and " + MAX_QUANTITY + ".");
        }
        return quantity;
    }

    public static long multiply(long unitMinor, int quantity) {
        requirePositive(unitMinor);
        requireQuantity(quantity);
        try {
            return requirePositive(Math.multiplyExact(unitMinor, quantity));
        } catch (ArithmeticException ex) {
            throw new IllegalArgumentException("Total exceeds the transaction limit.", ex);
        }
    }

    public static long add(long left, long right) {
        requirePositive(left);
        requirePositive(right);
        try {
            return requirePositive(Math.addExact(left, right));
        } catch (ArithmeticException ex) {
            throw new IllegalArgumentException("Total exceeds the transaction limit.", ex);
        }
    }

    public static String format(long minorUnits) {
        requirePositive(minorUnits);
        return BigDecimal.valueOf(minorUnits, 2).setScale(2, RoundingMode.UNNECESSARY).toPlainString();
    }

    /** Convert only at the Vault boundary; quoted and journaled amounts remain integers. */
    public static double toVault(long minorUnits) {
        requirePositive(minorUnits);
        return BigDecimal.valueOf(minorUnits, 2).doubleValue();
    }
}

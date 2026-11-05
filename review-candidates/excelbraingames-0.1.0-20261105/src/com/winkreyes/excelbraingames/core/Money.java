package com.winkreyes.excelbraingames.core;

import java.util.Locale;

/**
 * Money is carried as integer cents everywhere inside the plugin so payout
 * math is exact. Vault deposits convert to dollars at the boundary.
 */
public final class Money {
    private Money() {}

    public static long dollarsToCents(double dollars) {
        return Math.round(dollars * 100.0);
    }

    public static double centsToDollars(long cents) {
        return cents / 100.0;
    }

    /** Half of a payout, rounded DOWN to the cent (Mike's Q6 amendment, Nov 4, 2026). */
    public static long halfRoundedDown(long cents) {
        return cents / 2; // cents are never negative here; integer division floors for positives
    }

    public static String fmt(long cents) {
        long abs = Math.abs(cents);
        String body = String.format(Locale.US, "$%,d.%02d", abs / 100, abs % 100);
        return cents < 0 ? "-" + body : body;
    }
}

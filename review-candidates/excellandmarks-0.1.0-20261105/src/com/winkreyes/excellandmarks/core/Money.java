package com.winkreyes.excellandmarks.core;

import java.text.NumberFormat;
import java.util.Locale;

/** Money in whole cents, the fleet convention (BrainGames core/Money shape). */
public final class Money {
    private Money() { }

    public static long dollars(double d) {
        return Math.round(d * 100.0);
    }

    public static String fmt(long cents) {
        NumberFormat nf = NumberFormat.getCurrencyInstance(Locale.US);
        return nf.format(cents / 100.0);
    }

    /** "$500" style for sign faces: no cents when whole. */
    public static String fmtShort(long cents) {
        if (cents % 100 == 0) return "$" + String.format(Locale.US, "%,d", cents / 100);
        return fmt(cents);
    }
}

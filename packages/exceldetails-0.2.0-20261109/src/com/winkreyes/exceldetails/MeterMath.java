package com.winkreyes.exceldetails;

import java.util.Locale;

/**
 * Pure formatting + budget math for the meter (unit-tested, no Bukkit).
 *
 * Excel — Wink Reyes Valley (Iris / Mike).
 */
public final class MeterMath {

    /** The sidebar's hard line ceiling (client + EM's own cap agree). */
    public static final int SIDEBAR_CEILING = 15;

    private MeterMath() {}

    /**
     * Piggyback budget (placement constraint 3): the combined box
     * self-caps, so meter lines never exceed what EM's lines leave.
     */
    public static int meterBudget(int foreignLines) {
        if (foreignLines < 0) foreignLines = 0;
        return Math.max(0, SIDEBAR_CEILING - foreignLines);
    }

    /** Details-style compact amount: 950, 12.5k, 3.4M. */
    public static String compact(double v) {
        if (v < 0) v = 0;
        if (v < 1000.0) {
            if (v == Math.floor(v)) return String.format(Locale.US, "%d", (long) v);
            return trim(String.format(Locale.US, "%.1f", v));
        }
        if (v < 1_000_000.0) return trim(String.format(Locale.US, "%.2f", v / 1000.0)) + "k";
        return trim(String.format(Locale.US, "%.2f", v / 1_000_000.0)) + "M";
    }

    private static String trim(String s) {
        if (s.contains(".")) {
            s = s.replaceAll("0+$", "").replaceAll("\\.$", "");
        }
        return s;
    }

    /** Whole-percent share, 0 when the total is 0. */
    public static int percent(double part, double total) {
        if (total <= 0) return 0;
        return (int) Math.round(part * 100.0 / total);
    }

    /** Per-second rate over a segment; 0 before a full second elapses is fine — rate uses elapsed with a 1 s floor. */
    public static double perSecond(double amount, long elapsedMs) {
        if (amount <= 0) return 0;
        long eff = Math.max(1000L, elapsedMs);
        return amount * 1000.0 / eff;
    }

    /** m:ss segment clock. */
    public static String clock(long elapsedMs) {
        long s = Math.max(0, elapsedMs / 1000);
        return String.format(Locale.US, "%d:%02d", s / 60, s % 60);
    }
}

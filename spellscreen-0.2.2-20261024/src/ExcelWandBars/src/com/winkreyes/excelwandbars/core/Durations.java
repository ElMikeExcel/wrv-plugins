package com.winkreyes.excelwandbars.core;

/** Milliseconds to short, phone-friendly text ("1 sec", "1.5 sec", "2 min"). */
public final class Durations {
    private Durations() {}

    public static String ms(long ms) {
        if (ms <= 0) return "none";
        if (ms < 60_000) {
            double s = ms / 1000.0;
            if (Math.abs(s - Math.rint(s)) < 1e-9) return ((long) Math.rint(s)) + " sec";
            return trim(String.format(java.util.Locale.ROOT, "%.1f", s)) + " sec";
        }
        double m = ms / 60_000.0;
        if (Math.abs(m - Math.rint(m)) < 1e-9) return ((long) Math.rint(m)) + " min";
        return trim(String.format(java.util.Locale.ROOT, "%.1f", m)) + " min";
    }

    public static String number(double d) {
        if (Math.abs(d - Math.rint(d)) < 1e-9) return Long.toString((long) Math.rint(d));
        return trim(String.format(java.util.Locale.ROOT, "%.1f", d));
    }

    private static String trim(String s) {
        return s.endsWith(".0") ? s.substring(0, s.length() - 2) : s;
    }
}

package com.winkreyes.excelwandbars.core;

/**
 * 0.3.10: the name-tag cooldown countdown (Mike's pick, Nov 3 2026 — "Build
 * the name-tag countdown into the next WandBars packet"). Pure (no
 * Bukkit/Magic types), unit-tested on the plain JVM.
 *
 * Why this exists (COOLDOWN-64-INVESTIGATION.md, bytecode-verified): Magic
 * writes each hotbar spell icon's stack amount as the seconds until the
 * spell can be cast again — the max of remaining cooldown, charge recharge
 * and mana wait — clamped to [1, 99]; Geyser then flattens any amount above
 * 64 to exactly 64 for Bedrock. So on Bedrock a 4-minute Familiar cooldown
 * reads "64" for most of its life, and even on Java everything past 99 s
 * reads "99". The number cannot carry the true time; the icon's NAME can.
 *
 * The rule: while a spell's true time-to-cast EXCEEDS the configured
 * threshold (config cooldown-name-tag.threshold-seconds, default 60), the
 * icon's display name is annotated "Familiar · 3:24" — the spell's own
 * name, a middle dot, and the true remaining time as m:ss. At or below the
 * threshold the name stays exactly as Magic (or the tap-bar icon lore)
 * painted it and Magic's number does the talking; the moment the spell is
 * castable the annotation is gone. The annotation is a display-copy edit
 * only — see NameTagHook for the safety contract (PDC-marked copies,
 * pre-save sweeps, standing restore).
 */
public final class CooldownTagPlan {
    private CooldownTagPlan() {}

    /** Between the spell's own name and the countdown: "Familiar · 3:24". */
    public static final String SEPARATOR = " \u00B7 ";

    /**
     * Annotate only while the true time-to-cast EXCEEDS the threshold.
     * Exactly at the threshold the name stays clean (Magic's number still
     * shows the seconds there). A null time-to-cast means castable now (or
     * unreadable): never annotate.
     */
    public static boolean shouldTag(Long timeToCastMs, int thresholdSeconds) {
        if (timeToCastMs == null) return false;
        long thresholdMs = Math.max(0, thresholdSeconds) * 1000L;
        return timeToCastMs > thresholdMs;
    }

    /** Whole seconds shown, rounded UP — the same ceil Magic applies to its hotbar number. */
    public static long secondsShown(long timeToCastMs) {
        if (timeToCastMs <= 0) return 0;
        return (timeToCastMs + 999L) / 1000L;
    }

    /**
     * m:ss — minutes unpadded (they can pass 59 on the longest cooldowns),
     * seconds zero-padded to two. 240 -&gt; "4:00", 204 -&gt; "3:24",
     * 61 -&gt; "1:01", 7 -&gt; "0:07", 1800 -&gt; "30:00".
     */
    public static String format(long totalSeconds) {
        if (totalSeconds < 0) totalSeconds = 0;
        long m = totalSeconds / 60;
        long s = totalSeconds % 60;
        return m + ":" + (s < 10 ? "0" : "") + s;
    }

    /** The exact text appended to the spell's own name: " · 3:24". */
    public static String suffix(long totalSeconds) {
        return SEPARATOR + format(totalSeconds);
    }

    /** The full annotated plain name: base name + suffix. */
    public static String annotated(String basePlainName, long totalSeconds) {
        return (basePlainName == null ? "" : basePlainName) + suffix(totalSeconds);
    }

    /**
     * True when a plain name ends with our suffix shape: " · " + digits +
     * ":" + exactly two second digits under 60. This is the pattern
     * fallback only — the runtime identifies its copies by a PDC marker
     * and restores the recorded original name exactly; the pattern exists
     * so a copy whose marker payload is unreadable can still be cleaned.
     */
    public static boolean hasOurSuffix(String plainName) {
        return suffixStart(plainName) >= 0;
    }

    /**
     * Remove exactly one trailing suffix of our shape; the input is
     * returned unchanged when there is none. "Familiar · 3:24 · 1:01"
     * loses only the last one.
     */
    public static String stripSuffix(String plainName) {
        int i = suffixStart(plainName);
        return i < 0 ? plainName : plainName.substring(0, i);
    }

    /** Index where a trailing " · m:ss" of our shape starts, or -1. */
    private static int suffixStart(String name) {
        if (name == null) return -1;
        int i = name.lastIndexOf(SEPARATOR);
        if (i < 0) return -1;
        String tail = name.substring(i + SEPARATOR.length());
        int colon = tail.indexOf(':');
        if (colon <= 0 || colon != tail.lastIndexOf(':')) return -1;
        String min = tail.substring(0, colon);
        String sec = tail.substring(colon + 1);
        if (sec.length() != 2 || !allDigits(min) || !allDigits(sec)) return -1;
        int s;
        try { s = Integer.parseInt(sec); } catch (NumberFormatException e) { return -1; }
        return s < 60 ? i : -1;
    }

    private static boolean allDigits(String s) {
        if (s.isEmpty()) return false;
        for (int i = 0; i < s.length(); i++) if (!Character.isDigit(s.charAt(i))) return false;
        return true;
    }
}

package com.winkreyes.exceltrainingwheels.core;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Console log throttle (0.1.5). One line per key per window; the
 * repeats inside the window are counted, and the next line that is
 * let through carries that count ("+N similar"). Pure logic, no
 * Bukkit: the glue passes a monotonic millisecond clock.
 *
 * Window 0 = off (every line passes, the 0.1.4 behavior).
 * Memory is bounded: at most MAX_KEYS keys; expired keys are
 * dropped first, then the least recently used.
 */
public final class LogThrottle {

    public static final long DEFAULT_SECONDS = 300L;
    public static final long MAX_SECONDS = 86_400L;
    public static final int MAX_KEYS = 1024;

    private final long windowMillis;
    private final int maxKeys;
    /** key -> {lastEmittedMillis, suppressedSinceThen}; access order = LRU. */
    private final LinkedHashMap<String, long[]> state = new LinkedHashMap<>(64, 0.75f, true);

    public LogThrottle(long windowMillis) {
        this(windowMillis, MAX_KEYS);
    }

    public LogThrottle(long windowMillis, int maxKeys) {
        if (maxKeys < 1) throw new IllegalArgumentException("maxKeys < 1");
        this.windowMillis = Math.max(0L, windowMillis);
        this.maxKeys = maxKeys;
    }

    /** Config value in seconds -> clamped 0..MAX_SECONDS (negative = 0 = off). */
    public static long sanitizeSeconds(long raw) {
        if (raw < 0) return 0L;
        return Math.min(raw, MAX_SECONDS);
    }

    /**
     * May a line for this key be logged now?
     * Returns -1 when it is suppressed (counted), otherwise the number
     * of lines suppressed for this key since the last one logged (0 if none).
     */
    public long admit(String key, long nowMillis) {
        if (windowMillis == 0L) return 0L;
        String k = key == null ? "" : key;
        long[] s = state.get(k);
        if (s != null) {
            long age = nowMillis - s[0];
            if (age >= 0 && age < windowMillis) {
                s[1]++;
                return -1L;
            }
            long suppressed = s[1];
            s[0] = nowMillis;
            s[1] = 0L;
            return suppressed;
        }
        if (state.size() >= maxKeys) evict(nowMillis);
        state.put(k, new long[]{nowMillis, 0L});
        return 0L;
    }

    private void evict(long nowMillis) {
        Iterator<Map.Entry<String, long[]>> it = state.entrySet().iterator();
        while (it.hasNext()) {
            long age = nowMillis - it.next().getValue()[0];
            if (age < 0 || age >= windowMillis) it.remove();
        }
        it = state.entrySet().iterator();
        while (state.size() >= maxKeys && it.hasNext()) {
            it.next();
            it.remove();
        }
    }

    /** Suffix for a logged line: "" or " (+N similar in the last Ss)". */
    public static String suffix(long suppressed, long windowSeconds) {
        if (suppressed <= 0) return "";
        return " (+" + suppressed + " similar in the last " + windowSeconds + "s)";
    }

    public int size() {
        return state.size();
    }
}

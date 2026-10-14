package com.winkreyes.excelmobveil;

/** Absolute wall-clock deadline. Only successful drinks extend it. */
final class VeilTiming {
    static final long DURATION_MILLIS = 480_000L;
    static long remaining(Long started, Long expires, long now) {
        if (started == null || expires == null || started < 0 || now < started
                || expires <= started || now >= expires) return 0;
        return expires - now;
    }
    static long extension(Long expires, long now) {
        if (now < 0) throw new IllegalArgumentException("Invalid clock");
        return Math.addExact(expires == null ? now : Math.max(expires, now), DURATION_MILLIS);
    }
    static int ticks(long millis) {
        if (millis <= 0) return 1;
        long rounded = millis / 50 + (millis % 50 == 0 ? 0 : 1);
        return (int) Math.min(Integer.MAX_VALUE, rounded);
    }
}

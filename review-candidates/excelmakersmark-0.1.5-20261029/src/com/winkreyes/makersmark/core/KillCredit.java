/* Excel WRV maintained source; original attribution and license preserved. */ package com.winkreyes.makersmark.core;
/** Integer fifths for cosmetics only; actual kills remain independent. */
public final class KillCredit {
    private KillCredit() {}
    public static long baseline(int actual) { return Math.max(0L, (long) actual) * 5L; }
    public static long add(long units, boolean reduced) {
        long current = Math.max(0L, units), gain = reduced ? 1L : 5L;
        return current > Long.MAX_VALUE - gain ? Long.MAX_VALUE : current + gain;
    }
    public static boolean reduced(String paperOrigin, String recordedOrigin, String recordedReason, boolean softened) {
        return softened || "SPAWNER".equals(paperOrigin) || "SPAWNER".equals(recordedOrigin)
            || "spawn_origin_spawner".equals(recordedReason);
    }
}

package com.winkreyes.leaderboard.core;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;

/** Pure board math: ranking with shared tie ranks, value formatting, and the
 * statistic summations the spec defines. No Bukkit types here so every rule
 * is unit-testable off-server. */
public final class BoardMath {

    public record Entry(UUID player, String name, long value) {}

    private BoardMath() {}

    /** Sort value-descending; ties listed by name (case-insensitive). Rank =
     * 1 + number of entries with a strictly greater value, so ties share a
     * rank and the next rank skips accordingly (1, 2, 2, 4). */
    public static List<Entry> rank(List<Entry> entries) {
        List<Entry> sorted = new ArrayList<>(entries);
        sorted.sort(Comparator.comparingLong(Entry::value).reversed()
                .thenComparing(e -> e.name().toLowerCase(Locale.ROOT)));
        return sorted;
    }

    public static int rankOf(List<Entry> ranked, UUID player) {
        long previousValue = Long.MIN_VALUE;
        int currentRank = 0;
        for (int i = 0; i < ranked.size(); i++) {
            Entry e = ranked.get(i);
            if (e.value() != previousValue) {
                currentRank = i + 1;
                previousValue = e.value();
            }
            if (e.player().equals(player)) {
                return currentRank;
            }
        }
        return -1;
    }

    public static List<Entry> top(List<Entry> ranked, int n) {
        return ranked.subList(0, Math.min(n, ranked.size()));
    }

    /** PLAY_ONE_MINUTE ticks to a compact "42h 7m" (or "7m" under an hour). */
    public static String formatPlaytime(long ticks) {
        long totalMinutes = Math.max(0, ticks) / 20 / 60;
        long hours = totalMinutes / 60;
        long minutes = totalMinutes % 60;
        if (hours == 0) {
            return minutes + "m";
        }
        return hours + "h " + minutes + "m";
    }

    /** Sum of *_ONE_CM statistics, shown in kilometers with one decimal. */
    public static String formatDistanceKm(long totalCm) {
        double km = Math.max(0, totalCm) / 100_000.0;
        return String.format(Locale.US, "%,.1f km", km);
    }

    public static String formatCount(long value) {
        return String.format(Locale.US, "%,d", Math.max(0, value));
    }

    public static String formatMoney(double balance) {
        return String.format(Locale.US, "$%,.0f", Math.max(0, balance));
    }

    /** Top Builder: sum USE_ITEM values over block items only. The caller
     * supplies the block test so this stays free of Bukkit types. */
    public static long sumPlacedBlocks(Map<String, Integer> useItemByMaterial,
                                       Predicate<String> isBlockItem) {
        long total = 0;
        for (Map.Entry<String, Integer> e : useItemByMaterial.entrySet()) {
            if (isBlockItem.test(e.getKey())) {
                total += Math.max(0, e.getValue());
            }
        }
        return total;
    }

    /** Top Miner adjustment: raw mined total minus Build Mode session mined
     * blocks, never below zero. */
    public static long adjustedMined(long rawMined, long sessionAdjustment) {
        return Math.max(0, rawMined - Math.max(0, sessionAdjustment));
    }

    /** Session delta between a start snapshot and an end snapshot. A stat
     * that goes DOWN between snapshots (stat reset) contributes zero, never
     * a negative adjustment. */
    public static long sessionDelta(long startValue, long endValue) {
        return Math.max(0, endValue - startValue);
    }
}

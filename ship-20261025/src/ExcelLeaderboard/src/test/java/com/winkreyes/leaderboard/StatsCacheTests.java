package com.winkreyes.leaderboard;

import com.winkreyes.leaderboard.core.VanillaStatsCache;
import com.winkreyes.leaderboard.core.VanillaStatsCache.Totals;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/** 1.3.1: offline stats are computed once per stats-file version. */
public final class StatsCacheTests {
    private static int assertions;

    public static void main(String[] args) {
        UUID a = UUID.fromString("00000000-0000-0000-0000-00000000000a");
        UUID b = UUID.fromString("00000000-0000-0000-0000-00000000000b");
        VanillaStatsCache cache = new VanillaStatsCache();
        AtomicInteger runs = new AtomicInteger();
        long s1 = VanillaStatsCache.stamp(1_000_000L, 500L);

        Totals t1 = cache.get(a, s1, () -> { runs.incrementAndGet(); return new Totals(10, 2, 3, 4, 5, 6); });
        Totals t2 = cache.get(a, s1, () -> { runs.incrementAndGet(); return new Totals(99, 0, 0, 0, 0, 0); });
        check(runs.get() == 1, "same stamp: computed once");
        check(t2.equals(t1) && t2.mined() == 10, "same stamp: cached totals returned");

        long s2 = VanillaStatsCache.stamp(1_000_000L, 501L);
        check(s2 != s1, "same mtime, different size: different stamp");
        Totals t3 = cache.get(a, s2, () -> { runs.incrementAndGet(); return new Totals(11, 2, 3, 4, 5, 6); });
        check(runs.get() == 2 && t3.mined() == 11, "file rewritten: recomputed");

        cache.get(b, -1, () -> { runs.incrementAndGet(); return new Totals(1, 1, 1, 1, 1, 1); });
        cache.get(b, -1, () -> { runs.incrementAndGet(); return new Totals(2, 1, 1, 1, 1, 1); });
        check(runs.get() == 4, "negative stamp (online / no file): never cached");

        Totals t4 = cache.get(a, -1, () -> { runs.incrementAndGet(); return new Totals(12, 2, 3, 4, 5, 6); });
        check(runs.get() == 5 && t4.mined() == 12, "player comes online: live value, old slot dropped");
        cache.get(a, s2, () -> { runs.incrementAndGet(); return new Totals(13, 2, 3, 4, 5, 6); });
        check(runs.get() == 6, "after going online, a later offline read recomputes");

        check(VanillaStatsCache.stamp(0L, 10L) == -1L, "no mtime: no stamp");
        check(cache.computations() == 6, "computations counter matches");
        try {
            java.nio.file.Path root = java.nio.file.Files.createTempDirectory("lbstats");
            java.nio.file.Path ow = root.resolve("world/dimensions/minecraft/overworld");
            java.nio.file.Files.createDirectories(ow);
            check(VanillaStatsCache.findStatsDir(ow) == null, "no stats folder anywhere: null");
            java.nio.file.Path st = root.resolve("world/players/stats");
            java.nio.file.Files.createDirectories(st);
            check(st.toAbsolutePath().normalize().equals(VanillaStatsCache.findStatsDir(ow)), "26.x layout: world/players/stats found from the overworld dimension folder");
            java.nio.file.Path legacy = root.resolve("legacy/world");
            java.nio.file.Files.createDirectories(legacy.resolve("stats"));
            check(legacy.resolve("stats").toAbsolutePath().normalize().equals(VanillaStatsCache.findStatsDir(legacy)), "legacy layout: world/stats found");
            check(VanillaStatsCache.findStatsDir(null) == null, "null world folder: null");
        } catch (java.io.IOException e) {
            throw new AssertionError(e);
        }
        System.out.println("StatsCacheTests: " + assertions + " assertions passed");
    }

    private static void check(boolean ok, String what) {
        if (!ok) {
            throw new AssertionError(what);
        }
        assertions++;
    }
}

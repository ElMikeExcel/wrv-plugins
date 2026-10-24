package com.winkreyes.leaderboard.core;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

/** 1.3.1: caches an OFFLINE player's six vanilla board totals, keyed on a
 * stamp of their stats file (last-modified time and size).
 *
 * Why: on Paper an offline player's getStatistic call builds a fresh
 * ServerStatsCounter from the stats JSON on disk, for EVERY call. The
 * Miner and Builder boards ask once per block material and once per item
 * material, so 1.3.0 re-read and re-parsed each offline player's file a
 * few thousand times per refresh, every refresh, for every player who has
 * ever joined. Offline stats cannot change until the file is rewritten,
 * so one computation per file version is enough.
 *
 * A negative stamp means "do not cache" (online player, whose live stats
 * are in memory and cheap, or a file that could not be read). */
public final class VanillaStatsCache {

    public record Totals(long mined, long placed, long playtimeTicks,
                         long mobKills, long distanceCm, long deaths) {
    }

    private record Slot(long stamp, Totals totals) {
    }

    private final Map<UUID, Slot> slots = new HashMap<>();
    private long computations;

    /** Finds the vanilla stats folder starting from the main world's folder.
     * Paper 26.x reports world/dimensions/minecraft/overworld as the world
     * folder while stats live in world/players/stats, so this checks
     * players/stats and stats at the folder and up to three parents.
     * Returns null when none exists. */
    public static Path findStatsDir(Path worldFolder) {
        Path p = worldFolder == null ? null : worldFolder.toAbsolutePath().normalize();
        for (int up = 0; up <= 3 && p != null; up++, p = p.getParent()) {
            for (Path candidate : List.of(p.resolve("players").resolve("stats"), p.resolve("stats"))) {
                if (Files.isDirectory(candidate)) {
                    return candidate;
                }
            }
        }
        return null;
    }

    /** Stamp for a stats file: mixes size into the mtime so a same-second
     * rewrite with a different length still invalidates. -1 = no file. */
    public static long stamp(long lastModifiedMillis, long sizeBytes) {
        if (lastModifiedMillis <= 0 || sizeBytes < 0) {
            return -1L;
        }
        return lastModifiedMillis * 31L + sizeBytes;
    }

    public synchronized Totals get(UUID player, long stamp, Supplier<Totals> compute) {
        if (stamp >= 0) {
            Slot slot = slots.get(player);
            if (slot != null && slot.stamp() == stamp) {
                return slot.totals();
            }
        }
        Totals totals = compute.get();
        computations++;
        if (stamp >= 0) {
            slots.put(player, new Slot(stamp, totals));
        } else {
            slots.remove(player);
        }
        return totals;
    }

    /** How many times compute ran (tests and the debug log). */
    public synchronized long computations() {
        return computations;
    }
}

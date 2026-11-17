package com.winkreyes.leaderboard.core;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

/** Cache for one player's vanilla statistic totals (1.3.1): a refresh
 * used to re-read and re-parse every known player's stats file on
 * every cycle, which grows with the player count. A player's totals
 * are recomputed only when their stats file's stamp (last-modified
 * time combined with size) changes; an online player, a missing
 * file, or an unknown stats folder yields a negative stamp, which
 * never caches, so those reads stay exactly as uncached as 1.3.0.
 * All access is synchronized: refreshes run off the main thread. */
public final class VanillaStatsCache {

    /** One player's six vanilla totals, in board order. */
    public record Totals(long mined, long placed, long playtimeTicks,
                         long mobKills, long distanceCm, long deaths) {}

    private record Slot(long stamp, Totals totals) {}

    private final Map<UUID, Slot> slots = new HashMap<>();
    private long computations;

    public VanillaStatsCache() {}

    /** Finds the vanilla stats folder for a world folder: the folder
     * itself or an ancestor (up to three levels up) may carry it
     * directly ("stats") or under "players/stats" (the layout some
     * hosts use). Returns null when no candidate is a directory. */
    public static Path findStatsDir(Path worldFolder) {
        Path current = worldFolder == null ? null : worldFolder.toAbsolutePath().normalize();
        for (int depth = 0; depth <= 3 && current != null; depth++, current = current.getParent()) {
            for (Path candidate : List.of(current.resolve("players").resolve("stats"),
                    current.resolve("stats"))) {
                if (Files.isDirectory(candidate)) {
                    return candidate;
                }
            }
        }
        return null;
    }

    /** The cache stamp for a stats file: -1 when either input says
     * the file cannot be trusted (no positive mtime, negative size). */
    public static long stamp(long modifiedMillis, long size) {
        if (modifiedMillis <= 0 || size < 0) {
            return -1L;
        }
        return modifiedMillis * 31L + size;
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

    public synchronized long computations() {
        return computations;
    }
}

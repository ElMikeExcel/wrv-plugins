package com.winkreyes.excelclaims;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * ExcelClaims — the GriefPrevention flat-file importer. Reads the old
 * store's on-disk shapes (ClaimData/*.yml, PlayerData text files,
 * _nextClaimID) as documented in INVENTORY.md §4. File logic first;
 * the one Bukkit consult (an owner's last-played time, used to seed
 * lastSeenAt) is guarded exactly like Core.nameOf, so headless runs
 * (unit tests) degrade to file timestamps and the tests exercise the
 * exact code the migration runs. Anything the importer cannot carry
 * is counted and reported, never silently dropped.
 *
 * DATE SEEDING (0.1.1, the Nov 7 expiration incident): GP's files
 * carry no dates at all, and 0.1.0 imported every claim and player
 * with created/modified/last-seen all zero — the expiration sweep
 * then read every owner as away since the epoch and expired all
 * nine live player claims at the first post-import boot. The
 * importer now seeds what GP never wrote: claim created/modified
 * from the claim file's on-disk mtime (only where the file itself
 * carried no date), and player last-seen from Bukkit's last-played
 * record, falling back to the player file's mtime. Seeding can only
 * ever make an imported claim YOUNGER in the sweep's eyes than the
 * raw files suggest — never older.
 * Excel — Iris / Mike.
 */
public final class GpImporter {

    public static final class ImportResult {
        public final List<ClaimRecord> claims = new ArrayList<>();
        public final List<PlayerRecord> players = new ArrayList<>();
        public long nextClaimId;
        public int orphanedSubdivisions;
        public int unreadableFiles;
        public final List<String> notes = new ArrayList<>();
    }

    public ImportResult importFrom(File gpDataDir) throws Exception {
        ImportResult result = new ImportResult();
        File claimDir = new File(gpDataDir, "ClaimData");
        File playerDir = new File(gpDataDir, "PlayerData");

        Set<Long> ids = new HashSet<>();
        File[] files = claimDir.listFiles((d, n) -> n.endsWith(".yml"));
        if (files != null) {
            java.util.Arrays.sort(files);
            for (File f : files) {
                String base = f.getName().substring(0, f.getName().length() - 4);
                long id;
                try {
                    id = Long.parseLong(base);
                } catch (NumberFormatException e) {
                    result.unreadableFiles++;
                    result.notes.add("skipped non-claim file " + f.getName());
                    continue;
                }
                ClaimRecord r = ClaimStore.readClaimFile(f, id);
                if (r == null) {
                    result.unreadableFiles++;
                    result.notes.add("skipped unreadable claim file " + f.getName());
                    continue;
                }
                // GP claim files carry no dates (readClaimFile yields
                // 0 for both); seed the missing ones from the file's
                // own mtime — the last moment GP itself touched the
                // claim. Carried dates (our own format) always win.
                long mtime = fileMtime(f);
                if (r.createdAt <= 0) r.createdAt = mtime;
                if (r.lastModifiedAt <= 0) r.lastModifiedAt = mtime;
                result.claims.add(r);
                ids.add(id);
            }
        }
        // Parent links must point at imported claims; a subdivision
        // whose parent is missing is re-rooted as a top-level claim
        // and reported (it keeps its land and its trust lists).
        for (ClaimRecord r : result.claims) {
            if (r.parentId >= 0 && !ids.contains(r.parentId)) {
                r.parentId = -1;
                result.orphanedSubdivisions++;
                result.notes.add("claim " + r.id + " had a missing parent; imported as top-level");
            }
        }
        File next = new File(claimDir, "_nextClaimID");
        if (next.isFile()) {
            result.nextClaimId = Long.parseLong(Files.readString(next.toPath(), StandardCharsets.UTF_8).trim());
        } else {
            result.nextClaimId = ids.stream().mapToLong(Long::longValue).max().orElse(-1L) + 1;
        }

        File[] pfiles = playerDir.listFiles();
        if (pfiles != null) {
            for (File f : pfiles) {
                UUID id;
                try {
                    id = UUID.fromString(f.getName());
                } catch (IllegalArgumentException e) {
                    result.unreadableFiles++;
                    result.notes.add("skipped non-player file " + f.getName());
                    continue;
                }
                int[] ab = ClaimStore.readPlayerFile(f);
                PlayerRecord pr = new PlayerRecord(id, ab[0], ab[1]);
                // GP's player files hold block counts only; the
                // owner's last-seen comes from Bukkit's last-played
                // record when a server is running, else the file's
                // mtime (the last time GP saved the account).
                pr.lastSeenAt = seedLastSeenAt(bukkitLastPlayed(id), fileMtime(f));
                result.players.add(pr);
            }
        }
        return result;
    }

    // ------------------------------------------------------------ date seeding

    /** On-disk mtime of a file, or 0 when it cannot be read. */
    static long fileMtime(File f) {
        try {
            return Files.getLastModifiedTime(f.toPath()).toMillis();
        } catch (Exception e) {
            return 0L;
        }
    }

    /**
     * Bukkit's last-played time for a player, or 0 when no server is
     * running (headless tests) or Bukkit has no record. Guarded the
     * same way as Core.nameOf — a lookup failure must never fail an
     * import.
     */
    static long bukkitLastPlayed(UUID id) {
        if (id == null) return 0L;
        try {
            if (Bukkit.getServer() == null) return 0L;
            OfflinePlayer op = Bukkit.getOfflinePlayer(id);
            return op == null ? 0L : Math.max(0L, op.getLastPlayed());
        } catch (Throwable t) {
            return 0L;
        }
    }

    /**
     * The last-seen seed for an imported player: Bukkit's record when
     * it has one, else the player file's mtime, else 0 (unknown — the
     * expiration sweep never expires on an unknown basis, so a 0 here
     * is safe, merely uninformative).
     */
    static long seedLastSeenAt(long bukkitLastPlayed, long fileMtime) {
        if (bukkitLastPlayed > 0) return bukkitLastPlayed;
        return Math.max(0L, fileMtime);
    }
}

// ExcelClaims — Iris / Mike, for Wink Reyes Valley

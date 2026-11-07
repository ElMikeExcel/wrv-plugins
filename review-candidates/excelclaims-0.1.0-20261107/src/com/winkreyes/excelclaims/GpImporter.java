package com.winkreyes.excelclaims;

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
 * _nextClaimID) as documented in INVENTORY.md §4. Pure file logic —
 * no Bukkit runtime beyond the YAML reader the store already uses —
 * so the unit tests exercise the exact code the migration runs.
 * Anything the importer cannot carry is counted and reported, never
 * silently dropped. Excel — Iris / Mike.
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
                result.players.add(new PlayerRecord(id, ab[0], ab[1]));
            }
        }
        return result;
    }
}

// ExcelClaims — Iris / Mike, for Wink Reyes Valley

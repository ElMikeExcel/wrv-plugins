package com.winkreyes.exceldeathrecord.core;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Set;

/**
 * Snapshot file naming: one file per death in the player's own
 * folder, named by the death's local second —
 * "2026-10-22_212404.yml". The fixed-width format sorts
 * lexicographically in chronological order, which is what makes
 * the listing logic trivial and testable. Two deaths inside the
 * same second for the same player are vanishingly rare but must
 * never overwrite each other: unique() walks "-2", "-3", ...
 * suffixes against the names already on disk.
 */
public final class RecordName {

    private static final DateTimeFormatter FILE =
            DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmmss", Locale.ROOT);
    private static final DateTimeFormatter TEXT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.ROOT);

    private RecordName() {}

    /** "2026-10-22_212404" for the instant, in the given zone. */
    public static String baseName(long epochMillis, ZoneId zone) {
        return FILE.format(Instant.ofEpochMilli(epochMillis).atZone(zone));
    }

    /** "2026-10-22 21:24:04" — the human timestamp stored in the record. */
    public static String timeText(long epochMillis, ZoneId zone) {
        return TEXT.format(Instant.ofEpochMilli(epochMillis).atZone(zone));
    }

    /** First free file name for the base: "base.yml", else
     *  "base-2.yml", "base-3.yml", ... against the names that
     *  already exist in the player's folder. */
    public static String unique(String base, Set<String> existingFileNames) {
        String plain = base + ".yml";
        if (!existingFileNames.contains(plain)) return plain;
        for (int n = 2; ; n++) {
            String candidate = base + "-" + n + ".yml";
            if (!existingFileNames.contains(candidate)) return candidate;
        }
    }
}

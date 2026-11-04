package com.winkreyes.excelguildgate;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

/**
 * Persistence for NarratorPackPrompt records: a tiny line-based text
 * format, one record per line - the same house style as
 * ExcelQuartermaster's PackStateStore:
 *
 *   &lt;uuid&gt; &lt;OUTCOME&gt; &lt;attempts&gt; &lt;lastChangeMillis&gt;
 *
 * No Bukkit (and no YAML) dependency, so the format is unit-testable
 * on a plain JVM. Serialization is sorted by UUID string, so the file
 * is stable and diff-friendly. Parsing is forgiving: blank lines,
 * comments (#), unknown outcome names, and malformed lines are
 * skipped rather than failing the load, and negative numbers clamp
 * to 0.
 */
public final class NarratorPackStateStore {

    private NarratorPackStateStore() {}

    /** Serialize records to the on-disk text form (empty map -> ""). */
    public static String serialize(Map<UUID, NarratorPackPrompt.Record> records) {
        if (records == null || records.isEmpty()) {
            return "";
        }
        TreeMap<String, NarratorPackPrompt.Record> sorted = new TreeMap<>();
        for (Map.Entry<UUID, NarratorPackPrompt.Record> e : records.entrySet()) {
            if (e.getKey() != null && e.getValue() != null && e.getValue().outcome() != null) {
                sorted.put(e.getKey().toString(), e.getValue());
            }
        }
        StringBuilder out = new StringBuilder();
        for (Map.Entry<String, NarratorPackPrompt.Record> e : sorted.entrySet()) {
            out.append(e.getKey()).append(' ')
               .append(e.getValue().outcome().name()).append(' ')
               .append(Math.max(0, e.getValue().attempts())).append(' ')
               .append(Math.max(0L, e.getValue().lastChangeMillis())).append('\n');
        }
        return out.toString();
    }

    /** Parse the on-disk text form. Never throws on bad content. */
    public static Map<UUID, NarratorPackPrompt.Record> parse(String text) {
        Map<UUID, NarratorPackPrompt.Record> out = new LinkedHashMap<>();
        if (text == null || text.isBlank()) {
            return out;
        }
        for (String line : text.split("\\R")) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue;
            }
            String[] parts = trimmed.split("\\s+");
            if (parts.length != 4) {
                continue;
            }
            try {
                UUID id = UUID.fromString(parts[0]);
                NarratorPackPrompt.Outcome outcome = NarratorPackPrompt.Outcome.valueOf(parts[1]);
                int attempts = Integer.parseInt(parts[2]);
                long lastMillis = Long.parseLong(parts[3]);
                out.put(id, new NarratorPackPrompt.Record(outcome,
                        Math.max(0, attempts), Math.max(0L, lastMillis)));
            } catch (IllegalArgumentException badLine) {
                // Skip the malformed line; one bad record must not lose the rest.
            }
        }
        return out;
    }

    /** Load records from a file; a missing file reads as an empty map. */
    public static Map<UUID, NarratorPackPrompt.Record> load(Path file) throws IOException {
        if (file == null || !Files.exists(file)) {
            return new LinkedHashMap<>();
        }
        return parse(Files.readString(file, StandardCharsets.UTF_8));
    }

    /** Save records to a file, creating parent folders as needed. */
    public static void save(Path file, Map<UUID, NarratorPackPrompt.Record> records) throws IOException {
        if (file == null) {
            return;
        }
        Path parent = file.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.writeString(file, serialize(records), StandardCharsets.UTF_8);
    }
}

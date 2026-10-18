package com.winkreyes.excelquartermaster;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

/**
 * Persistence for TaleRotation records: a tiny line-based text format, one
 * record per line:
 *
 *   &lt;uuid&gt; &lt;nextIndex&gt; &lt;lastTaleMillis&gt;
 *
 * No Bukkit (and no YAML) dependency, so the format is unit-testable on a
 * plain JVM. Serialization is sorted by UUID string, so the file is stable
 * and diff-friendly. Parsing is forgiving: blank lines, comments (#), and
 * malformed lines are skipped rather than failing the load, and negative
 * numbers clamp to 0.
 */
public final class TaleStore {

    private TaleStore() {}

    /** Serialize records to the on-disk text form (empty map -> ""). */
    public static String serialize(Map<UUID, TaleRotation.PlayerTale> records) {
        if (records == null || records.isEmpty()) {
            return "";
        }
        TreeMap<String, TaleRotation.PlayerTale> sorted = new TreeMap<>();
        for (Map.Entry<UUID, TaleRotation.PlayerTale> e : records.entrySet()) {
            if (e.getKey() != null && e.getValue() != null) {
                sorted.put(e.getKey().toString(), e.getValue());
            }
        }
        StringBuilder out = new StringBuilder();
        for (Map.Entry<String, TaleRotation.PlayerTale> e : sorted.entrySet()) {
            out.append(e.getKey()).append(' ')
               .append(Math.max(0, e.getValue().nextIndex())).append(' ')
               .append(Math.max(0L, e.getValue().lastTaleMillis())).append('\n');
        }
        return out.toString();
    }

    /** Parse the on-disk text form. Never throws on bad content. */
    public static Map<UUID, TaleRotation.PlayerTale> parse(String text) {
        Map<UUID, TaleRotation.PlayerTale> out = new LinkedHashMap<>();
        if (text == null || text.isBlank()) {
            return out;
        }
        for (String line : text.split("\\R")) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue;
            }
            String[] parts = trimmed.split("\\s+");
            if (parts.length != 3) {
                continue;
            }
            try {
                UUID id = UUID.fromString(parts[0]);
                int nextIndex = Integer.parseInt(parts[1]);
                long lastMillis = Long.parseLong(parts[2]);
                out.put(id, new TaleRotation.PlayerTale(
                        Math.max(0, nextIndex), Math.max(0L, lastMillis)));
            } catch (IllegalArgumentException badLine) {
                // Skip the malformed line; one bad record must not lose the rest.
            }
        }
        return out;
    }

    /** Load records from a file; a missing file reads as an empty map. */
    public static Map<UUID, TaleRotation.PlayerTale> load(Path file) throws IOException {
        if (file == null || !Files.exists(file)) {
            return new LinkedHashMap<>();
        }
        return parse(Files.readString(file, StandardCharsets.UTF_8));
    }

    /** Save records to a file, creating parent folders as needed. */
    public static void save(Path file, Map<UUID, TaleRotation.PlayerTale> records) throws IOException {
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

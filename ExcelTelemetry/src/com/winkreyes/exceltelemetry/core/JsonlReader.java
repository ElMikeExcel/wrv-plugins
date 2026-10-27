package com.winkreyes.exceltelemetry.core;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Fail-soft JSONL reader used by /telemetry status: counts lines by
 * their "type" field, tallies corrupt lines, and NEVER throws on bad
 * content - a torn last line from a hard server stop, or a line a
 * human mangled in the panel, must not break the status command or
 * (in the Python tooling, which mirrors this contract) the weekly
 * report.
 */
public final class JsonlReader {
    private JsonlReader() {}

    public record Counts(long total, long corrupt, Map<String, Long> byType) {}

    public static Counts countLines(List<String> lines) {
        long total = 0;
        long corrupt = 0;
        Map<String, Long> byType = new LinkedHashMap<>();
        for (String raw : lines) {
            if (raw == null) continue;
            String line = raw.trim();
            if (line.isEmpty()) continue;
            String type = extractType(line);
            if (type == null) {
                corrupt++;
            } else {
                total++;
                byType.merge(type, 1L, Long::sum);
            }
        }
        return new Counts(total, corrupt, byType);
    }

    public static Counts countFile(Path file) throws IOException {
        if (file == null || !Files.exists(file)) return new Counts(0, 0, new LinkedHashMap<>());
        return countLines(Files.readAllLines(file, StandardCharsets.UTF_8));
    }

    /**
     * Light structural check (not a full parse): line must be a JSON
     * object carrying a "type":"<word>" field. Returns the type or
     * null when the line is corrupt for our purposes.
     */
    static String extractType(String line) {
        if (!line.startsWith("{") || !line.endsWith("}")) return null;
        int i = line.indexOf("\"type\"");
        if (i < 0) return null;
        int colon = line.indexOf(':', i + 6);
        if (colon < 0) return null;
        int q1 = line.indexOf('"', colon);
        if (q1 < 0) return null;
        int q2 = line.indexOf('"', q1 + 1);
        if (q2 < 0) return null;
        String type = line.substring(q1 + 1, q2);
        return type.isEmpty() ? null : type;
    }
}

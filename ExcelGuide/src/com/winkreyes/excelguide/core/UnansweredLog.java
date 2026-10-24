package com.winkreyes.excelguide.core;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * The unanswered-question log (SPEC 6.3): the Guide's growth list.
 * One line per unanswered question:
 *
 *   &lt;ISO instant&gt; | &lt;player&gt; | &lt;uuid&gt; | &lt;door&gt; | &lt;question&gt; | best=&lt;chunk key&gt; | score=&lt;n&gt;
 *
 * Appends are synchronized; when the file grows past maxEntries the
 * oldest lines are dropped (the close-out pass rotates real candidates
 * into the Route Sheet rhythm long before the cap matters).
 */
public final class UnansweredLog {

    /** One unanswered question. door: COMMAND, SUMMON, or AMBIENT. */
    public record Entry(Instant when, String playerName, String playerUuid,
                        String door, String question, String bestChunkKey, double bestScore) {
        public String toLine() {
            String q = question == null ? "" : question.replace('|', '/').replace('\n', ' ');
            return when + " | " + playerName + " | " + playerUuid + " | " + door
                    + " | " + q + " | best=" + (bestChunkKey == null ? "-" : bestChunkKey)
                    + " | score=" + String.format(java.util.Locale.ROOT, "%.1f", bestScore);
        }
    }

    private final Path file;
    private final int maxEntries;
    private int count;

    public UnansweredLog(Path file, int maxEntries) {
        this.file = file;
        this.maxEntries = Math.max(10, maxEntries);
        this.count = countLines(file);
    }

    public synchronized int count() {
        return count;
    }

    public synchronized void append(Entry entry) {
        try {
            if (file.getParent() != null) {
                Files.createDirectories(file.getParent());
            }
            String line = entry.toLine() + "\n";
            if (Files.exists(file)) {
                Files.writeString(file, line, StandardCharsets.UTF_8,
                        java.nio.file.StandardOpenOption.APPEND);
            } else {
                Files.writeString(file, line, StandardCharsets.UTF_8);
            }
            count++;
            if (count > maxEntries) {
                trim();
            }
        } catch (IOException e) {
            throw new UncheckedIOException("writing unanswered log " + file, e);
        }
    }

    /** All lines currently in the log (oldest first). */
    public synchronized List<String> readAll() {
        if (!Files.exists(file)) {
            return List.of();
        }
        try {
            return Files.readAllLines(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("reading unanswered log " + file, e);
        }
    }

    private void trim() throws IOException {
        List<String> lines = new ArrayList<>(Files.readAllLines(file, StandardCharsets.UTF_8));
        int excess = lines.size() - maxEntries;
        if (excess > 0) {
            lines = lines.subList(excess, lines.size());
            Files.writeString(file, String.join("\n", lines) + "\n", StandardCharsets.UTF_8);
            count = lines.size();
        }
    }

    private static int countLines(Path file) {
        if (!Files.exists(file)) {
            return 0;
        }
        try {
            return (int) Files.lines(file, StandardCharsets.UTF_8).count();
        } catch (IOException e) {
            return 0;
        }
    }
}

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
 * The AI engagement log (0.2.0): one line per AI call the fallback
 * engaged, so the weekly Guide report can count how often the AI
 * was needed and how often it actually answered:
 *
 *   &lt;ISO instant&gt; | &lt;player&gt; | &lt;uuid&gt; | &lt;door&gt; | answered=&lt;true|false&gt; | ms=&lt;latency&gt; | &lt;question&gt;
 *
 * Same append + trim discipline as the unanswered log. A call whose
 * model answered "don't know", errored, or timed out is
 * answered=false; the question ALSO lands in unanswered.log in that
 * case, exactly as it did before the AI existed.
 *
 * 0.2.1: learn-cache activity shares this file as cache=&lt;kind&gt;
 * lines (see appendCacheEvent) so the Monday report reads one file
 * for the whole AI story without cache serves inflating the
 * answered counts.
 */
public final class AiLog {

    /** One AI engagement. door: COMMAND or SUMMON (ambient never engages the AI). */
    public record Entry(Instant when, String playerName, String playerUuid, String door,
                        boolean answered, long latencyMillis, String question) {
        public String toLine() {
            String q = question == null ? "" : question.replace('|', '/').replace('\n', ' ');
            return when + " | " + playerName + " | " + playerUuid + " | " + door
                    + " | answered=" + answered + " | ms=" + latencyMillis + " | " + q;
        }
    }

    private final Path file;
    private final int maxEntries;
    private int count;

    public AiLog(Path file, int maxEntries) {
        this.file = file;
        this.maxEntries = Math.max(10, maxEntries);
        this.count = countLines(file);
    }

    public synchronized int count() {
        return count;
    }

    public synchronized void append(Entry entry) {
        appendLine(entry.toLine());
        count++;
        if (count > maxEntries) {
            trimQuietly();
        }
    }

    /**
     * A learn-cache event line (0.2.1): same pipe shape as an
     * engagement, but the answered= field is replaced by cache=&lt;kind&gt;
     * ("hit" or "stale-dropped") so the weekly report's answered=
     * counts stay pure AI numbers while cache activity is still
     * visible in this file. ms is always 0: no provider was called.
     */
    public synchronized void appendCacheEvent(Instant when, String playerName, String playerUuid,
                                              String door, String kind, String question) {
        String q = question == null ? "" : question.replace('|', '/').replace('\n', ' ');
        appendLine(when + " | " + playerName + " | " + playerUuid + " | " + door
                + " | cache=" + kind + " | ms=0 | " + q);
        count++;
        if (count > maxEntries) {
            trimQuietly();
        }
    }

    private void appendLine(String line) {
        try {
            if (file.getParent() != null) {
                Files.createDirectories(file.getParent());
            }
            if (Files.exists(file)) {
                Files.writeString(file, line + "\n", StandardCharsets.UTF_8,
                        java.nio.file.StandardOpenOption.APPEND);
            } else {
                Files.writeString(file, line + "\n", StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("writing ai log " + file, e);
        }
    }

    private void trimQuietly() {
        try {
            trim();
        } catch (IOException e) {
            throw new UncheckedIOException("writing ai log " + file, e);
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
            throw new UncheckedIOException("reading ai log " + file, e);
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

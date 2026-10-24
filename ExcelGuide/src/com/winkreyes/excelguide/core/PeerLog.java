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
 * The peer-learning event log (0.2.3): one line per peer-learning
 * event, so the weekly Guide report can tell Mike everything the
 * valley taught the Guide, what is still contested, and what could
 * not be settled. Line shapes (pipe fields, question last, exactly
 * as the report counts them):
 *
 *   &lt;instant&gt; | peer=captured | author=&lt;name&gt; | &lt;question&gt;
 *   &lt;instant&gt; | peer=learned | verdict=figured | author=&lt;name&gt; | &lt;question&gt;
 *   &lt;instant&gt; | peer=learned | verdict=was-right | author=&lt;name&gt; | &lt;question&gt;
 *   &lt;instant&gt; | peer=contested | author=&lt;name&gt; | challenger=&lt;name&gt; | &lt;question&gt;
 *   &lt;instant&gt; | peer=unresolved | reason=&lt;slug&gt; | &lt;question&gt;
 *   &lt;instant&gt; | peer=served | author=&lt;name&gt; | &lt;question&gt;
 *   &lt;instant&gt; | peer=stale-dropped | &lt;question&gt;
 *   &lt;instant&gt; | peer=rejected | reason=&lt;slug&gt; | by=&lt;name&gt; | &lt;question&gt;
 *   &lt;instant&gt; | peer=no-answer | &lt;question&gt;
 *
 * Same append + trim discipline as the AI log. Durable peer state
 * (what can be served, what awaits Mike's cull) lives in
 * peer-answers.yml; this file is the week's story of how it got
 * there.
 *
 * 0.3.0: the engine writes only the name-free {@link #event} lines
 * below (no player names, no UUIDs). The 0.2.3 methods above are kept
 * so older lines still parse, but nothing calls them any more.
 *
 *   &lt;instant&gt; | peer=window | &lt;question&gt;                       (capture window opened)
 *   &lt;instant&gt; | peer=skipped | reason=&lt;q-slug&gt; | &lt;question&gt;    (question gate said no)
 *   &lt;instant&gt; | peer=candidate | &lt;question&gt;                    (an answer passed the gate)
 *   &lt;instant&gt; | peer=rejected | reason=&lt;slug&gt; | &lt;question&gt;     (a substantive answer refusal)
 *   &lt;instant&gt; | peer=agreed | count=&lt;n&gt; | &lt;question&gt;
 *   &lt;instant&gt; | peer=confirmed | &lt;question&gt;                    (the asker said thanks / it worked)
 *   &lt;instant&gt; | peer=disputed | &lt;question&gt;
 *   &lt;instant&gt; | peer=dropped | reason=&lt;slug&gt; | &lt;question&gt;      (window closed, nothing queued)
 *   &lt;instant&gt; | peer=no-answer | &lt;question&gt;
 *   &lt;instant&gt; | peer=queued | id=&lt;id&gt; | &lt;question&gt;            (waiting for /guide review)
 *   &lt;instant&gt; | peer=approved | id=&lt;id&gt; | by=owner|auto | &lt;question&gt;
 *   &lt;instant&gt; | peer=owner-rejected | id=&lt;id&gt; | &lt;question&gt;
 *   &lt;instant&gt; | peer=expired | id=&lt;id&gt; | &lt;question&gt;
 *   &lt;instant&gt; | peer=served | id=&lt;id&gt; | &lt;question&gt;
 *   &lt;instant&gt; | peer=serve-blocked | reason=&lt;slug&gt; | id=&lt;id&gt; | &lt;question&gt;
 */
public final class PeerLog {

    private final Path file;
    private final int maxEntries;
    private int count;

    public PeerLog(Path file, int maxEntries) {
        this.file = file;
        this.maxEntries = Math.max(10, maxEntries);
        this.count = countLines(file);
    }

    public synchronized int count() {
        return count;
    }

    public synchronized void captured(Instant when, String author, String question) {
        line(when + " | peer=captured | author=" + clean(author) + " | " + cleanQ(question));
    }

    public synchronized void learned(Instant when, String verdict, String author, String question) {
        line(when + " | peer=learned | verdict=" + clean(verdict) + " | author=" + clean(author)
                + " | " + cleanQ(question));
    }

    public synchronized void contested(Instant when, String author, String challenger, String question) {
        line(when + " | peer=contested | author=" + clean(author) + " | challenger="
                + clean(challenger) + " | " + cleanQ(question));
    }

    public synchronized void unresolved(Instant when, String reason, String question) {
        line(when + " | peer=unresolved | reason=" + clean(reason) + " | " + cleanQ(question));
    }

    public synchronized void served(Instant when, String author, String question) {
        line(when + " | peer=served | author=" + clean(author) + " | " + cleanQ(question));
    }

    public synchronized void staleDropped(Instant when, String question) {
        line(when + " | peer=stale-dropped | " + cleanQ(question));
    }

    public synchronized void rejected(Instant when, String reason, String by, String question) {
        line(when + " | peer=rejected | reason=" + clean(reason) + " | by=" + clean(by)
                + " | " + cleanQ(question));
    }

    public synchronized void noAnswer(Instant when, String question) {
        line(when + " | peer=no-answer | " + cleanQ(question));
    }

    /**
     * 0.3.0 name-free event line: kind, then key=value fields (already
     * formatted, e.g. "reason=spoiler"), then the question.
     */
    public synchronized void event(Instant when, String kind, String question, String... fields) {
        StringBuilder sb = new StringBuilder();
        sb.append(when).append(" | peer=").append(clean(kind));
        for (String f : fields) {
            if (f != null && !f.isBlank()) {
                sb.append(" | ").append(clean(f));
            }
        }
        sb.append(" | ").append(cleanQ(question));
        line(sb.toString());
    }

    /** All lines currently in the log (oldest first). */
    public synchronized List<String> readAll() {
        if (!Files.exists(file)) {
            return List.of();
        }
        try {
            return Files.readAllLines(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("reading peer log " + file, e);
        }
    }

    private static String clean(String field) {
        return field == null ? "" : field.replace('|', '/').replace('\n', ' ');
    }

    private static String cleanQ(String question) {
        return clean(question);
    }

    private void line(String line) {
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
            throw new UncheckedIOException("writing peer log " + file, e);
        }
        count++;
        if (count > maxEntries) {
            trimQuietly();
        }
    }

    private void trimQuietly() {
        try {
            List<String> lines = new ArrayList<>(Files.readAllLines(file, StandardCharsets.UTF_8));
            int excess = lines.size() - maxEntries;
            if (excess > 0) {
                lines = lines.subList(excess, lines.size());
                Files.writeString(file, String.join("\n", lines) + "\n", StandardCharsets.UTF_8);
                count = lines.size();
            }
        } catch (IOException e) {
            throw new UncheckedIOException("writing peer log " + file, e);
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

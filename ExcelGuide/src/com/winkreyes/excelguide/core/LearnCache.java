package com.winkreyes.excelguide.core;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.yaml.snakeyaml.Yaml;

/**
 * The AI learn-cache (0.2.1): answers the AI produced once, saved so
 * a repeat question is served instantly with no AI call and no AI
 * budget spent. Backed by a YAML file in the plugin data folder
 * (default ai-cache.yml):
 *
 *   entries:
 *     - question: "how do i tame a fox"     (normalized form)
 *       answer: "Crouch near a fox ..."     (the cleaned AI answer)
 *       stamp: "0f40df75"                  (knowledge version at answer time)
 *       created: 1760000000000              (epoch millis)
 *       last-hit: 1760000000000             (epoch millis)
 *       hits: 2
 *
 * The ENGINE owns the policy (when to look up, the stamp check, the
 * privacy gate before a put); this class is storage: lookup, hit
 * recording, LRU-capped inserts, and durable write-through saves.
 *
 * Discipline, in the family style:
 * - Never throws. A missing file is an empty cache; a malformed file
 *   is an empty cache plus a load warning the glue logs - a corrupt
 *   cache can never take the Guide down.
 * - Saves write a temp file and move it over the cache file, so a
 *   crash mid-save cannot leave a torn cache behind.
 * - Puts save immediately (write-through). Hit counters advance in
 *   memory and are persisted by flush() - the glue flushes on
 *   disable and before a reload swap - and by the next put.
 * - The cap evicts the least-recently-hit entry (ties: the oldest
 *   created), so one-hit wonders from a busy night age out first.
 */
public final class LearnCache {

    /** One cached answer. question is the normalized question (the key). */
    public record Entry(String question, String answer, String stamp,
                        long created, long lastHit, int hits) {}

    private final Path file;
    private final int maxEntries;
    private final Map<String, Entry> entries = new LinkedHashMap<>();
    private final String loadWarning;
    private boolean dirty;

    public LearnCache(Path file, int maxEntries) {
        this.file = file;
        this.maxEntries = Math.max(1, maxEntries);
        this.loadWarning = load();
    }

    /** Why the on-disk cache could not be used, or null when it loaded clean. */
    public String loadWarning() {
        return loadWarning;
    }

    public synchronized int size() {
        return entries.size();
    }

    /** Sum of every entry's hit counter (the file's own bookkeeping). */
    public synchronized long totalHits() {
        long total = 0;
        for (Entry e : entries.values()) {
            total += e.hits();
        }
        return total;
    }

    /** The entry for a normalized question, or null. Stamp policy is the engine's. */
    public synchronized Entry get(String normalizedQuestion) {
        return normalizedQuestion == null ? null : entries.get(normalizedQuestion);
    }

    /** Record a serve: hit counter + last-hit advance in memory; flush persists them. */
    public synchronized void recordHit(String normalizedQuestion, long now) {
        Entry e = entries.get(normalizedQuestion);
        if (e != null) {
            entries.put(normalizedQuestion, new Entry(e.question(), e.answer(), e.stamp(),
                    e.created(), now, e.hits() + 1));
            dirty = true;
        }
    }

    /**
     * Store (or replace) the answer for a normalized question and
     * save immediately. Replacing resets the hit counter and stamps
     * the entry fresh - a re-cached answer starts a new life.
     */
    public synchronized void put(String normalizedQuestion, String answer, String stamp, long now) {
        if (normalizedQuestion == null || normalizedQuestion.isBlank()
                || answer == null || answer.isBlank()) {
            return;
        }
        entries.put(normalizedQuestion, new Entry(normalizedQuestion, answer,
                stamp == null ? "" : stamp, now, now, 0));
        evictOverflow();
        dirty = true;
        save();
    }

    /** Drop an entry (the engine's stale-stamp path) and save. */
    public synchronized void remove(String normalizedQuestion) {
        if (entries.remove(normalizedQuestion) != null) {
            dirty = true;
            save();
        }
    }

    /** Persist pending hit-counter movement. Safe to call any time. */
    public synchronized void flush() {
        if (dirty) {
            save();
        }
    }

    // ---------- file ----------

    private String load() {
        if (file == null || !Files.exists(file)) {
            return null;
        }
        try {
            Object root = new Yaml().load(Files.readString(file, StandardCharsets.UTF_8));
            if (!(root instanceof Map)) {
                return "cache file root is not a map; starting with an empty cache";
            }
            Object raw = ((Map<?, ?>) root).get("entries");
            if (raw == null) {
                return null; // an empty (or comments-only) cache file is clean
            }
            if (!(raw instanceof List)) {
                return "cache file 'entries' is not a list; starting with an empty cache";
            }
            int skipped = 0;
            for (Object o : (List<?>) raw) {
                if (!(o instanceof Map)) {
                    skipped++;
                    continue;
                }
                Map<?, ?> m = (Map<?, ?>) o;
                String q = str(m.get("question"));
                String a = str(m.get("answer"));
                if (q.isBlank() || a.isBlank()) {
                    skipped++;
                    continue;
                }
                entries.put(q, new Entry(q, a, str(m.get("stamp")),
                        num(m.get("created")), num(m.get("last-hit")),
                        (int) num(m.get("hits"))));
            }
            evictOverflow();
            return skipped == 0 ? null
                    : "skipped " + skipped + " malformed cache entries";
        } catch (Exception e) {
            entries.clear();
            return "cache file unreadable (" + e.getMessage() + "); starting with an empty cache";
        }
    }

    private void evictOverflow() {
        while (entries.size() > maxEntries) {
            String victim = null;
            Entry worst = null;
            for (Entry e : entries.values()) {
                if (worst == null
                        || e.lastHit() < worst.lastHit()
                        || (e.lastHit() == worst.lastHit() && e.created() < worst.created())) {
                    victim = e.question();
                    worst = e;
                }
            }
            if (victim == null) {
                return;
            }
            entries.remove(victim);
        }
    }

    private void save() {
        if (file == null) {
            return;
        }
        try {
            List<Map<String, Object>> list = new ArrayList<>();
            for (Entry e : entries.values()) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("question", e.question());
                m.put("answer", e.answer());
                m.put("stamp", e.stamp());
                m.put("created", e.created());
                m.put("last-hit", e.lastHit());
                m.put("hits", e.hits());
                list.add(m);
            }
            Map<String, Object> root = new LinkedHashMap<>();
            root.put("entries", list);
            String text = new Yaml().dump(root);
            if (file.getParent() != null) {
                Files.createDirectories(file.getParent());
            }
            Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
            Files.writeString(tmp, text, StandardCharsets.UTF_8);
            try {
                Files.move(tmp, file, StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (java.nio.file.AtomicMoveNotSupportedException e) {
                Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
            }
            dirty = false;
        } catch (IOException e) {
            // A failed save loses the cache, never the Guide: the next
            // put retries, and the in-memory entries keep serving.
        }
    }

    private static String str(Object o) {
        return o == null ? "" : String.valueOf(o);
    }

    private static long num(Object o) {
        return o instanceof Number n ? n.longValue() : 0L;
    }
}

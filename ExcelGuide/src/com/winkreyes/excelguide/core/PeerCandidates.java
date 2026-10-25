package com.winkreyes.excelguide.core;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.yaml.snakeyaml.Yaml;

/**
 * The owner review queue for player-taught answers (0.3.0). It lives
 * in its own YAML file in the plugin data folder (default
 * peer-review.yml). The 0.2.3 store (peer-answers.yml) is no longer
 * read or served.
 *
 *   entries:
 *     - id: "3f9a1c"                       (short, stable: hash of the topic key)
 *       question: "how do i tame a fox"    (normalized)
 *       key: "fox tame"                    (meaningful words, sorted: the match key)
 *       state: review                      (review | approved)
 *       answer: "use sweet berries ..."    (the text that would be served)
 *       how: "2 players agreed"            (why it reached the queue)
 *       agreements: 2                      (distinct other players who agreed)
 *       asker-confirmed: false             (the asker said thanks / that worked)
 *       created: 1760000000000
 *       last-asked: 1760000000000          (decay clock: refreshed when the topic is asked again)
 *       asks: 1
 *       decided: 0                         (when approved)
 *       decided-by: ""                     (owner | auto)
 *       hits: 0
 *       last-hit: 0
 *
 * NO player names or UUIDs are stored: who taught what is not kept,
 * and served answers say only "A player said: ...".
 *
 * STATES: review = waiting for Mike, NEVER served; approved = Mike
 * (or auto-approve, off by default) said yes, served. Reject deletes
 * the entry; the peer log keeps the line.
 *
 * Discipline (same as PeerAnswers / LearnCache): never throws; a
 * missing file is an empty store, a malformed one is an empty store
 * plus a load warning; saves write a temp file and move it over.
 */
public final class PeerCandidates {

    public static final String REVIEW = "review";
    public static final String APPROVED = "approved";

    public record Entry(String id, String question, String key, String state, String answer,
                        String how, int agreements, boolean askerConfirmed, long created,
                        long lastAsked, int asks, long decided, String decidedBy, int hits,
                        long lastHit) {

        public static Entry review(String question, String key, String answer, String how,
                                   int agreements, boolean askerConfirmed, long now) {
            return new Entry(idOf(key), question, key, REVIEW, answer, how, agreements,
                    askerConfirmed, now, now, 1, 0L, "", 0, 0L);
        }

        public Entry approved(String by, long now) {
            return new Entry(id, question, key, APPROVED, answer, how, agreements, askerConfirmed,
                    created, lastAsked, asks, now, by, hits, lastHit);
        }

        public Entry asked(long now) {
            return new Entry(id, question, key, state, answer, how, agreements, askerConfirmed,
                    created, now, asks + 1, decided, decidedBy, hits, lastHit);
        }

        public Entry hit(long now) {
            return new Entry(id, question, key, state, answer, how, agreements, askerConfirmed,
                    created, now, asks, decided, decidedBy, hits + 1, now);
        }

        public boolean isApproved() {
            return APPROVED.equals(state);
        }
    }

    /** Six hex characters of SHA-1 over the topic key: short enough to type in /guide review. */
    public static String idOf(String key) {
        try {
            byte[] d = MessageDigest.getInstance("SHA-1")
                    .digest((key == null ? "" : key).getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 3; i++) {
                sb.append(String.format("%02x", d[i] & 0xff));
            }
            return sb.toString();
        } catch (Exception e) {
            return Integer.toHexString((key == null ? "" : key).hashCode());
        }
    }

    private final Path file;
    private final int maxEntries;
    private final Map<String, Entry> byKey = new LinkedHashMap<>();
    private final String loadWarning;
    private boolean dirty;

    public PeerCandidates(Path file, int maxEntries) {
        this.file = file;
        this.maxEntries = Math.max(1, maxEntries);
        this.loadWarning = load();
    }

    public String loadWarning() {
        return loadWarning;
    }

    public synchronized int size() {
        return byKey.size();
    }

    public synchronized Entry byKey(String key) {
        return key == null ? null : byKey.get(key);
    }

    /** Find by id (case-insensitive); a unique prefix of 3+ characters also works. */
    public synchronized Entry byId(String id) {
        if (id == null || id.isBlank()) {
            return null;
        }
        String want = id.trim().toLowerCase(java.util.Locale.ROOT).replace("#", "");
        Entry prefix = null;
        int prefixHits = 0;
        for (Entry e : byKey.values()) {
            if (e.id().equals(want)) {
                return e;
            }
            if (want.length() >= 3 && e.id().startsWith(want)) {
                prefix = e;
                prefixHits++;
            }
        }
        return prefixHits == 1 ? prefix : null;
    }

    public synchronized List<Entry> inState(String state) {
        List<Entry> out = new ArrayList<>();
        for (Entry e : byKey.values()) {
            if (state.equals(e.state())) {
                out.add(e);
            }
        }
        out.sort(Comparator.comparingLong(Entry::created));
        return out;
    }

    public synchronized int count(String state) {
        int n = 0;
        for (Entry e : byKey.values()) {
            if (state.equals(e.state())) {
                n++;
            }
        }
        return n;
    }

    public synchronized List<Entry> all() {
        return List.copyOf(byKey.values());
    }

    public synchronized void put(Entry e) {
        if (e == null || e.key() == null || e.key().isBlank()) {
            return;
        }
        byKey.put(e.key(), e);
        evictOverflow();
        dirty = true;
        save();
    }

    public synchronized Entry remove(String key) {
        Entry gone = byKey.remove(key);
        if (gone != null) {
            dirty = true;
            save();
        }
        return gone;
    }

    /** In-memory only (hit and re-ask counters); flush() persists. */
    public synchronized void touch(Entry e) {
        if (e != null && byKey.containsKey(e.key())) {
            byKey.put(e.key(), e);
            dirty = true;
        }
    }

    public synchronized void flush() {
        if (dirty) {
            save();
        }
    }

    /**
     * Decay: drop review entries not asked again within reviewMs, and
     * (when approvedMs > 0) approved entries neither asked nor served
     * within approvedMs. Returns what was dropped.
     */
    public synchronized List<Entry> expire(long now, long reviewMs, long approvedMs) {
        List<Entry> gone = new ArrayList<>();
        for (Entry e : List.copyOf(byKey.values())) {
            long last = Math.max(e.lastAsked(), e.lastHit());
            boolean drop = e.isApproved()
                    ? approvedMs > 0 && now - Math.max(last, e.decided()) > approvedMs
                    : reviewMs > 0 && now - e.lastAsked() > reviewMs;
            if (drop) {
                byKey.remove(e.key());
                gone.add(e);
            }
        }
        if (!gone.isEmpty()) {
            dirty = true;
            save();
        }
        return gone;
    }

    private void evictOverflow() {
        while (byKey.size() > maxEntries) {
            Entry victim = null;
            for (Entry e : byKey.values()) { // oldest review entry first
                if (!e.isApproved() && (victim == null || e.lastAsked() < victim.lastAsked())) {
                    victim = e;
                }
            }
            if (victim == null) {
                for (Entry e : byKey.values()) { // then the least-recently-used approved one
                    long use = Math.max(e.lastHit(), e.decided());
                    if (victim == null || use < Math.max(victim.lastHit(), victim.decided())) {
                        victim = e;
                    }
                }
            }
            if (victim == null) {
                return;
            }
            byKey.remove(victim.key());
        }
    }

    // ---------- file ----------

    private String load() {
        if (file == null || !Files.exists(file)) {
            return null;
        }
        try {
            Object root = new Yaml().load(Files.readString(file, StandardCharsets.UTF_8));
            // Empty, "[]" or "{}" (the Oct 24 clear wrote "[]"): a clean
            // empty queue, no warning.
            if (root == null || (root instanceof List<?> l0 && l0.isEmpty())
                    || (root instanceof Map<?, ?> m0 && m0.isEmpty())) {
                return null;
            }
            if (!(root instanceof Map)) {
                return "review file root is not a map; starting with an empty queue";
            }
            Object raw = ((Map<?, ?>) root).get("entries");
            if (raw == null) {
                return null;
            }
            if (!(raw instanceof List)) {
                return "review file 'entries' is not a list; starting with an empty queue";
            }
            int skipped = 0;
            for (Object o : (List<?>) raw) {
                if (!(o instanceof Map<?, ?> m)) {
                    skipped++;
                    continue;
                }
                String key = str(m.get("key"));
                String state = str(m.get("state"));
                String answer = str(m.get("answer"));
                if (key.isBlank() || answer.isBlank()
                        || !(REVIEW.equals(state) || APPROVED.equals(state))) {
                    skipped++;
                    continue;
                }
                String id = str(m.get("id"));
                byKey.put(key, new Entry(id.isBlank() ? idOf(key) : id, str(m.get("question")), key,
                        state, answer, str(m.get("how")), (int) num(m.get("agreements")),
                        Boolean.TRUE.equals(m.get("asker-confirmed")), num(m.get("created")),
                        num(m.get("last-asked")), (int) num(m.get("asks")), num(m.get("decided")),
                        str(m.get("decided-by")), (int) num(m.get("hits")), num(m.get("last-hit"))));
            }
            evictOverflow();
            return skipped == 0 ? null : "skipped " + skipped + " malformed review entries";
        } catch (Exception e) {
            byKey.clear();
            return "review file unreadable (" + e.getMessage() + "); starting with an empty queue";
        }
    }

    private void save() {
        if (file == null) {
            return;
        }
        try {
            List<Map<String, Object>> list = new ArrayList<>();
            for (Entry e : byKey.values()) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id", e.id());
                m.put("question", e.question());
                m.put("key", e.key());
                m.put("state", e.state());
                m.put("answer", e.answer());
                m.put("how", e.how());
                m.put("agreements", e.agreements());
                m.put("asker-confirmed", e.askerConfirmed());
                m.put("created", e.created());
                m.put("last-asked", e.lastAsked());
                m.put("asks", e.asks());
                m.put("decided", e.decided());
                m.put("decided-by", e.decidedBy());
                m.put("hits", e.hits());
                m.put("last-hit", e.lastHit());
                list.add(m);
            }
            Map<String, Object> root = new LinkedHashMap<>();
            root.put("entries", list);
            String text = "# Player-taught Guide answers waiting for review (/guide review).\n"
                    + "# 'review' entries are never served; 'approved' ones are, as \"A player said: ...\".\n"
                    + new Yaml().dump(root);
            if (file.getParent() != null) {
                Files.createDirectories(file.getParent());
            }
            Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
            Files.writeString(tmp, text, StandardCharsets.UTF_8);
            try {
                Files.move(tmp, file, StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (java.nio.file.AtomicMoveNotSupportedException ex) {
                Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
            }
            dirty = false;
        } catch (IOException e) {
            // A failed save loses the write, never the Guide.
        }
    }

    private static String str(Object o) {
        return o == null ? "" : String.valueOf(o);
    }

    private static long num(Object o) {
        return o instanceof Number n ? n.longValue() : 0L;
    }
}

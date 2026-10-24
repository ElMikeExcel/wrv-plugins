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
 * The peer-learned answers store (0.2.3): what the valley's players
 * taught the Guide, in a YAML file in the plugin data folder
 * (default peer-answers.yml) - a SEPARATE store from the AI
 * learn-cache (ai-cache.yml) and the promoted answers (learned.yml)
 * by design: different source (players, not the AI, not a human
 * review), different rules.
 *
 *   entries:
 *     - question: "how do i tame a fox"        (normalized)
 *       key: "do fox how i tame"               (stemmed+sorted match key)
 *       state: learned                         (pending|contested|learned|unresolved)
 *       answer: "Crouch near them ..."         (the serving text; learned only)
 *       author: "ExamplePlayer"              (who taught it / who was right)
 *       author-uuid: "..."
 *       verdict: figured                       (figured|was-right)
 *       resolution: single                     (how the entry settled)
 *       stamp: "bd69277b"                      (knowledge version at learn time)
 *       created: 1760000000000
 *       last-hit: 1760000000000
 *       hits: 2
 *       sides:                                 (the candidate answers, max 2)
 *         - author: "ExamplePlayer"
 *           author-uuid: "..."
 *           answer: "..."
 *           at: 1760000000000
 *       debate: ["Quiet_Otter: no its glow berries"]   (contested history)
 *       support-a: ["testkite"]            (third players backing side 0)
 *       support-b: []
 *       asker-backed: ""                       ("" | "0" | "1")
 *
 * STATES: pending = captured inside (or awaiting) its window,
 * never served; contested = a disagreement opened adjudication,
 * never served; learned = settled, served with attribution;
 * unresolved = adjudication could not settle it - kept for the
 * weekly report and Mike's cull, never served.
 *
 * Discipline, in the family style (mirrors LearnCache):
 * - Never throws. A missing file is an empty store; a malformed
 *   file is an empty store plus a load warning the glue logs.
 * - Saves write a temp file and move it over the store, so a crash
 *   mid-save cannot tear it. State changes save immediately
 *   (write-through); hit counters advance in memory and persist on
 *   flush() / the next state change.
 * - The cap evicts the least-recently-hit LEARNED entry first,
 *   then the oldest unresolved, then the oldest pending; contested
 *   entries are never evicted (they are transient by design - if
 *   they alone overflow the cap, the store runs over until they
 *   settle, rather than silently dropping a live debate).
 */
public final class PeerAnswers {

    /** One candidate answer inside an entry: who said it, what, when. */
    public record Side(String author, String authorUuid, String answer, long at) {}

    /** One question's peer-learning record. See the class docs for the states. */
    public record Entry(String question, String key, String state,
                        String answer, String author, String authorUuid,
                        String verdict, String resolution, String stamp,
                        long created, long lastHit, int hits,
                        List<Side> sides, List<String> debate,
                        List<String> supportA, List<String> supportB,
                        String askerBacked) {

        public Entry {
            sides = sides == null ? List.of() : List.copyOf(sides);
            debate = debate == null ? List.of() : List.copyOf(debate);
            supportA = supportA == null ? List.of() : List.copyOf(supportA);
            supportB = supportB == null ? List.of() : List.copyOf(supportB);
            askerBacked = askerBacked == null ? "" : askerBacked;
        }

        /** A fresh pending entry for a just-opened capture window. */
        public static Entry pending(String question, String key, String stamp, long now) {
            return new Entry(question, key, "pending", "", "", "", "", "", stamp,
                    now, now, 0, List.of(), List.of(), List.of(), List.of(), "");
        }

        public Entry withSide(Side side) {
            List<Side> s = new ArrayList<>(sides);
            s.add(side);
            return new Entry(question, key, state, answer, author, authorUuid, verdict,
                    resolution, stamp, created, lastHit, hits, s, debate, supportA, supportB,
                    askerBacked);
        }

        public Entry withState(String newState) {
            return new Entry(question, key, newState, answer, author, authorUuid, verdict,
                    resolution, stamp, created, lastHit, hits, sides, debate, supportA, supportB,
                    askerBacked);
        }

        public Entry withDebateLine(String line) {
            List<String> d = new ArrayList<>(debate);
            d.add(line);
            return new Entry(question, key, state, answer, author, authorUuid, verdict,
                    resolution, stamp, created, lastHit, hits, sides, d, supportA, supportB,
                    askerBacked);
        }

        /** Record a third player's backing for a side (0 or 1); duplicates ignored. */
        public Entry withSupport(int side, String playerName) {
            if (side == 0 && !supportA.contains(playerName)) {
                List<String> s = new ArrayList<>(supportA);
                s.add(playerName);
                return new Entry(question, key, state, answer, author, authorUuid, verdict,
                        resolution, stamp, created, lastHit, hits, sides, debate, s, supportB,
                        askerBacked);
            }
            if (side == 1 && !supportB.contains(playerName)) {
                List<String> s = new ArrayList<>(supportB);
                s.add(playerName);
                return new Entry(question, key, state, answer, author, authorUuid, verdict,
                        resolution, stamp, created, lastHit, hits, sides, debate, supportA, s,
                        askerBacked);
            }
            return this;
        }

        /** The asker's confirmation lands on a side (0 or 1); the latest one stands. */
        public Entry withAskerBacked(int side) {
            return new Entry(question, key, state, answer, author, authorUuid, verdict,
                    resolution, stamp, created, lastHit, hits, sides, debate, supportA, supportB,
                    String.valueOf(side));
        }

        /** Settle as learned: the winning side becomes the serving answer. */
        public Entry learned(Side winner, String verdictWord, String how, String stampNow, long now) {
            return new Entry(question, key, "learned", winner.answer(), winner.author(),
                    winner.authorUuid(), verdictWord, how, stampNow, created, now, hits,
                    sides, debate, supportA, supportB, askerBacked);
        }

        /** Settle as unresolved: nothing is learned; the record stays for the report. */
        public Entry unresolved(String reason) {
            return new Entry(question, key, "unresolved", "", "", "", "", reason, stamp,
                    created, lastHit, hits, sides, debate, supportA, supportB, askerBacked);
        }

        /** A serve landed: hit counter + last-hit advance (persisted on flush). */
        public Entry hit(long now) {
            return new Entry(question, key, state, answer, author, authorUuid, verdict,
                    resolution, stamp, created, now, hits + 1, sides, debate, supportA,
                    supportB, askerBacked);
        }
    }

    private final Path file;
    private final int maxEntries;
    private final Map<String, Entry> entries = new LinkedHashMap<>();
    private final String loadWarning;
    private boolean dirty;

    public PeerAnswers(Path file, int maxEntries) {
        this.file = file;
        this.maxEntries = Math.max(1, maxEntries);
        this.loadWarning = load();
    }

    /** Why the on-disk store could not be used, or null when it loaded clean. */
    public String loadWarning() {
        return loadWarning;
    }

    public synchronized int size() {
        return entries.size();
    }

    public synchronized Entry get(String key) {
        return key == null ? null : entries.get(key);
    }

    public synchronized List<Entry> all() {
        return List.copyOf(entries.values());
    }

    public synchronized int learnedCount() {
        int n = 0;
        for (Entry e : entries.values()) {
            if ("learned".equals(e.state())) {
                n++;
            }
        }
        return n;
    }

    public synchronized int contestedCount() {
        int n = 0;
        for (Entry e : entries.values()) {
            if ("contested".equals(e.state())) {
                n++;
            }
        }
        return n;
    }

    public synchronized List<Entry> unresolved() {
        List<Entry> out = new ArrayList<>();
        for (Entry e : entries.values()) {
            if ("unresolved".equals(e.state())) {
                out.add(e);
            }
        }
        return out;
    }

    /** Insert or replace an entry and save immediately (state changes are write-through). */
    public synchronized void put(Entry entry) {
        if (entry == null || entry.key() == null || entry.key().isBlank()) {
            return;
        }
        entries.put(entry.key(), entry);
        evictOverflow();
        dirty = true;
        save();
    }

    public synchronized void remove(String key) {
        if (entries.remove(key) != null) {
            dirty = true;
            save();
        }
    }

    /** Record a serve in memory; flush() persists hit counters. */
    public synchronized void recordHit(String key, long now) {
        Entry e = entries.get(key);
        if (e != null) {
            entries.put(key, e.hit(now));
            dirty = true;
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
                return "peer file root is not a map; starting with an empty store";
            }
            Object raw = ((Map<?, ?>) root).get("entries");
            if (raw == null) {
                return null; // an empty (or comments-only) file is clean
            }
            if (!(raw instanceof List)) {
                return "peer file 'entries' is not a list; starting with an empty store";
            }
            int skipped = 0;
            for (Object o : (List<?>) raw) {
                if (!(o instanceof Map)) {
                    skipped++;
                    continue;
                }
                Map<?, ?> m = (Map<?, ?>) o;
                String q = str(m.get("question"));
                String key = str(m.get("key"));
                String state = str(m.get("state"));
                if (q.isBlank() || key.isBlank() || state.isBlank()) {
                    skipped++;
                    continue;
                }
                entries.put(key, new Entry(q, key, state,
                        str(m.get("answer")), str(m.get("author")), str(m.get("author-uuid")),
                        str(m.get("verdict")), str(m.get("resolution")), str(m.get("stamp")),
                        num(m.get("created")), num(m.get("last-hit")), (int) num(m.get("hits")),
                        sides(m.get("sides")), strings(m.get("debate")),
                        strings(m.get("support-a")), strings(m.get("support-b")),
                        str(m.get("asker-backed"))));
            }
            evictOverflow();
            return skipped == 0 ? null
                    : "skipped " + skipped + " malformed peer entries";
        } catch (Exception e) {
            entries.clear();
            return "peer file unreadable (" + e.getMessage() + "); starting with an empty store";
        }
    }

    private void evictOverflow() {
        while (entries.size() > maxEntries) {
            Entry victim = null;
            // 1) least-recently-hit learned
            for (Entry e : entries.values()) {
                if ("learned".equals(e.state())
                        && (victim == null || e.lastHit() < victim.lastHit())) {
                    victim = e;
                }
            }
            // 2) oldest unresolved
            if (victim == null) {
                for (Entry e : entries.values()) {
                    if ("unresolved".equals(e.state())
                            && (victim == null || e.created() < victim.created())) {
                        victim = e;
                    }
                }
            }
            // 3) oldest pending; contested is never evicted
            if (victim == null) {
                for (Entry e : entries.values()) {
                    if ("pending".equals(e.state())
                            && (victim == null || e.created() < victim.created())) {
                        victim = e;
                    }
                }
            }
            if (victim == null) {
                return; // only contested entries left: run over rather than drop a debate
            }
            entries.remove(victim.key());
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
                m.put("key", e.key());
                m.put("state", e.state());
                m.put("answer", e.answer());
                m.put("author", e.author());
                m.put("author-uuid", e.authorUuid());
                m.put("verdict", e.verdict());
                m.put("resolution", e.resolution());
                m.put("stamp", e.stamp());
                m.put("created", e.created());
                m.put("last-hit", e.lastHit());
                m.put("hits", e.hits());
                List<Map<String, Object>> sideList = new ArrayList<>();
                for (Side s : e.sides()) {
                    Map<String, Object> sm = new LinkedHashMap<>();
                    sm.put("author", s.author());
                    sm.put("author-uuid", s.authorUuid());
                    sm.put("answer", s.answer());
                    sm.put("at", s.at());
                    sideList.add(sm);
                }
                m.put("sides", sideList);
                m.put("debate", new ArrayList<>(e.debate()));
                m.put("support-a", new ArrayList<>(e.supportA()));
                m.put("support-b", new ArrayList<>(e.supportB()));
                m.put("asker-backed", e.askerBacked());
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
            } catch (java.nio.file.AtomicMoveNotSupportedException ex) {
                Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
            }
            dirty = false;
        } catch (IOException e) {
            // A failed save loses the store write, never the Guide.
        }
    }

    private static List<Side> sides(Object raw) {
        List<Side> out = new ArrayList<>();
        if (raw instanceof List) {
            for (Object o : (List<?>) raw) {
                if (o instanceof Map<?, ?> m) {
                    String author = str(m.get("author"));
                    String answer = str(m.get("answer"));
                    if (!author.isBlank() && !answer.isBlank()) {
                        out.add(new Side(author, str(m.get("author-uuid")), answer,
                                num(m.get("at"))));
                    }
                }
            }
        }
        return out;
    }

    private static List<String> strings(Object raw) {
        List<String> out = new ArrayList<>();
        if (raw instanceof List) {
            for (Object o : (List<?>) raw) {
                if (o != null) {
                    out.add(String.valueOf(o));
                }
            }
        }
        return out;
    }

    private static String str(Object o) {
        return o == null ? "" : String.valueOf(o);
    }

    private static long num(Object o) {
        return o instanceof Number n ? n.longValue() : 0L;
    }
}

package com.winkreyes.excelprotect;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * ExcelProtect 0.1.5 "Clarity" — player-first timeline projection
 * (SPEC §7). The canonical per-stream daily files stay the source of
 * truth; this is a DERIVED, rebuildable projection: every canonical
 * record that names a known player is also appended to
 * `players/<player-key>-<day>.jsonl` with two added fields — "role"
 * (that player's part in the record) and "stream" (its origin, so
 * consumers can re-section). A two-player record lands in BOTH files,
 * each copy with its own role. Records with no player never project.
 *
 * Failure isolation is structural: projection runs through its own
 * async writers, enqueued after the canonical write, inside a
 * catch-everything guard. The first failure latches the projector
 * broken (later copies are counted, not attempted), emits ONE system
 * marker, and surfaces in status — the canonical path never waits on,
 * retries, or even notices it.
 * Excel — Wink Reyes Valley
 */
public final class Timelines {

    /** One player's part in one record: the timeline file key + the role. */
    public record Involvement(String key, String role) {}

    /** Record types whose counterparty fields are NOT player identities. */
    private static final java.util.Set<String> NEVER_PROJECT = java.util.Set.of("fight_summary");

    private final File dir;
    private final Map<UUID, String> names = new ConcurrentHashMap<>();
    private final Map<String, DurableWriter> writers = new ConcurrentHashMap<>();
    private final AtomicLong failures = new AtomicLong();
    private final AtomicLong projected = new AtomicLong();
    private volatile boolean broken;
    private volatile boolean markerSent;
    private volatile Consumer<Json.Obj> markerSink;

    public Timelines(File dataDir) {
        this.dir = new File(dataDir, "players");
        try { if (!dir.isDirectory()) dir.mkdirs(); } catch (Throwable ignored) { }
        if (!dir.isDirectory()) { broken = true; failures.incrementAndGet(); }
    }

    public File dir() { return dir; }
    public boolean broken() { return broken; }
    public long failures() { return failures.get(); }
    public long projected() { return projected.get(); }

    /** Where the one-time failure marker goes (the system stream, via the ledger). */
    public void setMarkerSink(Consumer<Json.Obj> sink) { this.markerSink = sink; }

    public void notePlayer(UUID uuid, String name) {
        if (uuid != null && name != null && !name.isBlank()) names.put(uuid, name);
    }

    public boolean knows(UUID uuid) { return uuid != null && names.containsKey(uuid); }
    public String nameOf(UUID uuid) { return uuid == null ? null : names.get(uuid); }

    /** Filesystem-safe player key: anything outside [A-Za-z0-9_-] becomes '_'
     *  (Bedrock ".coolgirlkail" → "_coolgirlkail" — not dot-hidden, and a
     *  Java "coolgirlkail" stays a different key). */
    public static String sanitize(String name) {
        if (name == null || name.isBlank()) return "unknown";
        String k = name.replaceAll("[^A-Za-z0-9_-]", "_");
        return k.isBlank() ? "unknown" : k;
    }

    // ---------------------------------------------------------- extraction
    private record Cand(String field, String uuidField, String role, int priority) {}

    private static final List<Cand> FIELDS = List.of(
            new Cand("player", "player_uuid", "actor", 5),
            new Cand("attacker", "attacker_uuid", "actor", 5),
            new Cand("healer", "healer_uuid", "healer", 4),
            new Cand("target", "target_uuid", "target", 3),
            new Cand("victim", "victim_uuid", "victim", 2),
            new Cand("owner", "owner_uuid", "owner", 1));

    /**
     * Learn player identities from a line's identity fields (rebuild
     * pass 1 + live self-learning). Only unambiguous identities are
     * learned: the primary player pair always; counterparty pairs only
     * where the stream's construction makes them players (healer /
     * target are players by construction; attacker only when the line
     * marks attacker_kind=player or carries no kind at all, e.g. the
     * dealt stream, whose attacker is the player by construction).
     */
    public static void learn(String line, Map<UUID, String> registry) {
        learnPair(line, "player", "player_uuid", registry);
        learnPair(line, "healer", "healer_uuid", registry);
        learnPair(line, "target", "target_uuid", registry);
        String kind = Json.strField(line, "attacker_kind");
        if (kind == null || kind.equals("player")) learnPair(line, "attacker", "attacker_uuid", registry);
        if ("player".equals(Json.strField(line, "victim_kind"))) learnPair(line, "victim", "victim_uuid", registry);
    }

    private static void learnPair(String line, String nameField, String uuidField, Map<UUID, String> registry) {
        String name = Json.strField(line, nameField);
        String raw = Json.strField(line, uuidField);
        if (name == null || raw == null) return;
        try { registry.putIfAbsent(UUID.fromString(raw), name); } catch (Exception ignored) { }
    }

    /**
     * The players named by one canonical line, with each player's
     * strongest role. A field counts as a player identity only when it
     * resolves against the registry (by UUID, or by name when the line
     * carries no UUID) — a zombie victim or a mob attacker is never
     * projected. One involvement per player per record.
     */
    public static List<Involvement> involvements(String line, Map<UUID, String> registry) {
        String type = Json.strField(line, "type");
        if (type == null || NEVER_PROJECT.contains(type)) return List.of();
        Map<String, Involvement> byPlayer = new LinkedHashMap<>();
        Map<String, Integer> prio = new LinkedHashMap<>();
        for (Cand c : FIELDS) {
            String name = Json.strField(line, c.field());
            String rawUuid = Json.strField(line, c.uuidField());
            UUID uuid = null;
            if (rawUuid != null) { try { uuid = UUID.fromString(rawUuid); } catch (Exception ignored) { } }
            String resolvedName = null;
            if (uuid != null) {
                resolvedName = registry.get(uuid);
                if (resolvedName == null) continue;              // not a known player (mob, entity)
                if (name == null) name = resolvedName;
            } else if (name != null) {
                if (!registry.containsValue(name)) continue;     // name-only field, unknown player
            } else continue;
            String idKey = uuid != null ? uuid.toString() : "name:" + name;
            int p = prio.getOrDefault(idKey, -1);
            if (c.priority() > p) {
                prio.put(idKey, c.priority());
                byPlayer.put(idKey, new Involvement(sanitize(name), c.role()));
            }
        }
        return new ArrayList<>(byPlayer.values());
    }

    // ---------------------------------------------------------- live path
    /**
     * Project one canonical line. Called by the ledger AFTER the
     * canonical write is queued; guarded so a projection failure can
     * never reach the canonical path.
     */
    public void project(String stream, String line) {
        if (broken) { failures.incrementAndGet(); return; }
        try {
            learn(line, names);
            List<Involvement> inv = involvements(line, names);
            if (inv.isEmpty()) return;
            long tsMs = (long) Json.numField(line, "ts_ms", System.currentTimeMillis());
            String day = Ledger.dayOf(tsMs);
            for (Involvement iv : inv) {
                String copy = line.substring(0, line.length() - 1)
                        + ",\"role\":\"" + iv.role() + "\",\"stream\":\"" + stream + "\"}";
                writer(iv.key()).add(day, copy);
                projected.incrementAndGet();
            }
        } catch (Throwable t) {
            latchBroken();
        }
    }

    private void latchBroken() {
        failures.incrementAndGet();
        if (!broken) {
            broken = true;
            if (!markerSent) {
                markerSent = true;
                Consumer<Json.Obj> sink = markerSink;
                if (sink != null) {
                    try {
                        sink.accept(new Json.Obj().put("type", "timeline_projection_failed")
                                .put("detail", "player-timeline projection disabled after a write failure; canonical streams unaffected"));
                    } catch (Throwable ignored) { }
                }
            }
        }
    }

    private DurableWriter writer(String key) {
        return writers.computeIfAbsent(key, k -> new DurableWriter(dir, k + "-", ".jsonl", null, 20_000));
    }

    /** Flush every timeline writer; one aggregate health line for status. */
    public String flushAll() {
        writers.values().forEach(w -> { try { w.flush(); } catch (Throwable ignored) { } });
        // a writer whose batches failed has hit a real IO problem:
        // count it and latch broken (the marker fires once, here or in
        // project(), whichever sees the failure first)
        long failed = 0;
        for (DurableWriter w : writers.values()) failed += w.failedBatches();
        if (failed > 0 && !broken) latchBroken();
        long dropped = 0;
        for (DurableWriter w : writers.values()) dropped += w.dropped();
        if (projected.get() == 0 && failures.get() == 0 && dropped == 0) return "";
        return "timelines: files=" + writers.size() + " projected=" + projected.get()
                + " dropped=" + dropped + " failures=" + failures.get() + (broken ? " BROKEN" : "");
    }

    // ---------------------------------------------------------- query side
    /** The day file (plain or gz) for one player key + day, or null when absent. */
    public File fileFor(String key, String day) {
        File plain = new File(dir, key + "-" + day + ".jsonl");
        if (plain.exists()) return plain;
        File gz = new File(dir, key + "-" + day + ".jsonl.gz");
        return gz.exists() ? gz : null;
    }

    /** All day files for one player key, oldest first. */
    public File[] filesFor(String key) {
        File[] files = dir.listFiles((d, n) -> n.startsWith(key + "-")
                && (n.endsWith(".jsonl") || n.endsWith(".jsonl.gz")));
        if (files == null) return new File[0];
        java.util.Arrays.sort(files);
        return files;
    }

    public static String dayOfFile(String fileName) {
        return fileName.replaceAll("^.*-(\\d{4}-\\d{2}-\\d{2})\\.jsonl(\\.gz)?$", "$1");
    }

    // ---------------------------------------------------------- rebuild
    /**
     * Regenerate one day's timeline files from that day's canonical
     * stream lines (SPEC §7 rebuild). {@code streamLines} = [stream,
     * line] pairs for the day. Identities are learned in a first pass
     * over ALL lines, then every line is projected; existing files for
     * the day are replaced. Returns the number of copies written.
     * Runs on the caller's (async) thread; live appends for the same
     * day may interleave — it is a repair tool, run it in quiet time.
     */
    public int rebuildDay(String day, List<String[]> streamLines) throws java.io.IOException {
        Map<UUID, String> registry = new ConcurrentHashMap<>(names);
        for (String[] sl : streamLines) learn(sl[1], registry);
        Map<String, List<String>> byKey = new LinkedHashMap<>();
        for (String[] sl : streamLines) {
            for (Involvement iv : involvements(sl[1], registry)) {
                String line = sl[1];
                String copy = line.substring(0, line.length() - 1)
                        + ",\"role\":\"" + iv.role() + "\",\"stream\":\"" + sl[0] + "\"}";
                byKey.computeIfAbsent(iv.key(), k -> new ArrayList<>()).add(copy);
            }
        }
        File[] old = dir.listFiles((d, n) -> n.endsWith("-" + day + ".jsonl") || n.endsWith("-" + day + ".jsonl.gz"));
        if (old != null) for (File f : old) Files.deleteIfExists(f.toPath());
        int n = 0;
        for (Map.Entry<String, List<String>> e : byKey.entrySet()) {
            File f = new File(dir, e.getKey() + "-" + day + ".jsonl");
            Files.write(f.toPath(), e.getValue(), StandardCharsets.UTF_8);
            n += e.getValue().size();
        }
        return n;
    }
}

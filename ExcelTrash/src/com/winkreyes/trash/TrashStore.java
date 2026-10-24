package com.winkreyes.trash;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.function.LongSupplier;

/**
 * Pure-Java store: ONE recoverable batch per player (the last OK-delete), 5-minute window, one-shot take, file-backed.
 * A batch is everything deleted in one Trash screen session (every OK press in that session adds to the same batch).
 * Entries are "P:<base64>" (came from the player) or "C:<base64>" (came out of the source chest).
 *
 * File (batches.dat): header "#v2\t<savedAtMillis>", then per player
 *   uuid \t id \t updatedAt \t source \t entry1 \t entry2 ...
 * 0.1.0 lines (uuid \t createdAt \t item...) are migrated on load as player items.
 * A 0.1.0 jar reading this file sees id (a small number) as its timestamp, so it treats v2 batches as
 * expired: rollback can lose a pending recover but can never duplicate one.
 * Downtime does not eat the window: on load, timestamps are shifted forward by (now - savedAt).
 */
public final class TrashStore {
    public static final long TTL_MS = 5 * 60 * 1000L;
    public static final String INV = "inv";

    public record Batch(long id, long updatedAt, String source, List<String> entries) {
        public boolean fromChest() { return !INV.equals(source); }
    }

    private final Map<UUID, Batch> batches = new HashMap<>();
    private final Path file;
    private final LongSupplier clock;
    private long nextId = 1;

    public TrashStore(Path file, LongSupplier clock) { this.file = file; this.clock = clock; }

    public static String player(String b64) { return "P:" + b64; }
    public static String chest(String b64) { return "C:" + b64; }
    public static boolean isChest(String entry) { return entry.startsWith("C:"); }
    public static String data(String entry) { return entry.length() > 2 && entry.charAt(1) == ':' ? entry.substring(2) : entry; }

    /**
     * Add entries to the player's batch for session {@code sessionBatch} (0 = none yet). If that batch is still the
     * current one, entries are appended; otherwise a new batch replaces the old one (0.1.0 rule: only the last batch).
     * Returns the batch id to remember for the session, or 0 when nothing was stored.
     */
    public synchronized long add(UUID id, long sessionBatch, String source, List<String> entries) {
        if (entries == null || entries.isEmpty()) return sessionBatch;
        long now = clock.getAsLong();
        Batch cur = batches.get(id);
        Batch next;
        if (cur != null && sessionBatch != 0 && cur.id() == sessionBatch && !expired(cur)) {
            List<String> all = new ArrayList<>(cur.entries()); all.addAll(entries);
            next = new Batch(cur.id(), now, cur.source(), List.copyOf(all));
        } else {
            next = new Batch(nextId++, now, clean(source), List.copyOf(entries));
        }
        batches.put(id, next);
        save();
        return next.id();
    }

    /** 0.1.0 API kept for tests/compat: a fresh batch of player items. */
    public synchronized boolean put(UUID id, List<String> b64) {
        if (b64 == null || b64.isEmpty()) return false;
        add(id, 0, INV, b64.stream().map(TrashStore::player).toList());
        return true;
    }

    /** One-shot: returns and removes the batch if not expired; otherwise empty. Removal is saved before returning. */
    public synchronized Optional<Batch> take(UUID id) {
        Batch b = batches.remove(id);
        if (b == null) return Optional.empty();
        save();
        if (expired(b)) return Optional.empty();
        return Optional.of(b);
    }

    public synchronized Optional<Batch> peek(UUID id) {
        Batch b = batches.get(id);
        return b == null || expired(b) ? Optional.empty() : Optional.of(b);
    }

    public synchronized boolean has(UUID id) { return peek(id).isPresent(); }

    public synchronized long secondsLeft(UUID id) {
        return peek(id).map(b -> Math.max(0, (TTL_MS - (clock.getAsLong() - b.updatedAt())) / 1000)).orElse(0L);
    }

    private boolean expired(Batch b) { return clock.getAsLong() - b.updatedAt() >= TTL_MS; }

    public synchronized int purgeExpired() {
        int before = batches.size();
        batches.values().removeIf(this::expired);
        if (batches.size() != before) save();
        return before - batches.size();
    }

    private static String clean(String s) { return s == null || s.isBlank() ? INV : s.replace('\t', ' ').replace('\n', ' '); }

    public synchronized void save() {
        try {
            Files.createDirectories(file.toAbsolutePath().getParent());
            Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
            try (BufferedWriter w = Files.newBufferedWriter(tmp, StandardCharsets.UTF_8)) {
                w.write("#v2\t" + clock.getAsLong()); w.newLine();
                for (Map.Entry<UUID, Batch> e : batches.entrySet()) {
                    Batch b = e.getValue();
                    StringBuilder sb = new StringBuilder().append(e.getKey()).append('\t').append(b.id()).append('\t')
                            .append(b.updatedAt()).append('\t').append(b.source());
                    for (String s : b.entries()) sb.append('\t').append(s);
                    w.write(sb.toString()); w.newLine();
                }
            }
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException ex) { throw new UncheckedIOException(ex); }
    }

    public synchronized void load() {
        batches.clear();
        if (!Files.exists(file)) return;
        long now = clock.getAsLong();
        try {
            List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
            boolean v2 = !lines.isEmpty() && lines.get(0).startsWith("#v2");
            long shift = 0;
            if (v2) {
                String[] h = lines.get(0).split("\t");
                try { long saved = Long.parseLong(h[1]); if (now > saved) shift = now - saved; } catch (RuntimeException ignored) { }
            }
            for (String line : lines) {
                if (line.startsWith("#")) continue;
                String[] p = line.split("\t");
                try {
                    UUID u = UUID.fromString(p[0]);
                    Batch b;
                    if (v2) {
                        if (p.length < 5) continue;
                        long bid = Long.parseLong(p[1]);
                        b = new Batch(bid, Long.parseLong(p[2]) + shift, p[3], List.of(Arrays.copyOfRange(p, 4, p.length)));
                        nextId = Math.max(nextId, bid + 1);
                    } else { // 0.1.0 format
                        if (p.length < 3) continue;
                        List<String> es = new ArrayList<>();
                        for (int i = 2; i < p.length; i++) es.add(player(p[i]));
                        b = new Batch(nextId++, Long.parseLong(p[1]), INV, List.copyOf(es));
                    }
                    if (!expired(b)) batches.put(u, b);
                } catch (RuntimeException ignored) { }
            }
        } catch (IOException ex) { throw new UncheckedIOException(ex); }
        if (!v2Header()) save(); // migrate a 0.1.0 file to v2 at once
    }

    private boolean v2Header() {
        try (BufferedReader r = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            String l = r.readLine(); return l != null && l.startsWith("#v2");
        } catch (IOException e) { return false; }
    }
}

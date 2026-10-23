package com.winkreyes.trash;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.function.LongSupplier;

/** Pure-Java store: one batch (opaque serialized items) per player, 5-min TTL, one-shot take, file-backed. */
public final class TrashStore {
    public static final long TTL_MS = 5 * 60 * 1000L;
    public record Batch(long createdAt, List<String> items) {}

    private final Map<UUID, Batch> batches = new HashMap<>();
    private final Path file;
    private final LongSupplier clock;

    public TrashStore(Path file, LongSupplier clock) { this.file = file; this.clock = clock; }

    /** Replaces any previous batch (only the last batch is recoverable). Empty batches are ignored. */
    public synchronized boolean put(UUID id, List<String> items) {
        if (items == null || items.isEmpty()) return false;
        batches.put(id, new Batch(clock.getAsLong(), List.copyOf(items)));
        save();
        return true;
    }

    /** One-shot: returns and removes the batch if not expired; otherwise empty. */
    public synchronized Optional<List<String>> take(UUID id) {
        Batch b = batches.remove(id);
        if (b == null) return Optional.empty();
        save();
        if (expired(b)) return Optional.empty();
        return Optional.of(b.items());
    }

    public synchronized boolean has(UUID id) { Batch b = batches.get(id); return b != null && !expired(b); }

    private boolean expired(Batch b) { return clock.getAsLong() - b.createdAt() >= TTL_MS; }

    public synchronized int purgeExpired() {
        int before = batches.size();
        batches.values().removeIf(this::expired);
        if (batches.size() != before) save();
        return before - batches.size();
    }

    /** Format: one line per player: uuid \t createdAt \t item1 \t item2 ... (items are base64, tab-free). */
    public synchronized void save() {
        try {
            Files.createDirectories(file.toAbsolutePath().getParent());
            Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
            try (BufferedWriter w = Files.newBufferedWriter(tmp, StandardCharsets.UTF_8)) {
                for (Map.Entry<UUID, Batch> e : batches.entrySet()) {
                    StringBuilder sb = new StringBuilder().append(e.getKey()).append('\t').append(e.getValue().createdAt());
                    for (String s : e.getValue().items()) sb.append('\t').append(s);
                    w.write(sb.toString()); w.newLine();
                }
            }
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException ex) { throw new UncheckedIOException(ex); }
    }

    public synchronized void load() {
        batches.clear();
        if (!Files.exists(file)) return;
        try {
            for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                String[] p = line.split("\t");
                if (p.length < 3) continue;
                try {
                    Batch b = new Batch(Long.parseLong(p[1]), List.of(Arrays.copyOfRange(p, 2, p.length)));
                    if (!expired(b)) batches.put(UUID.fromString(p[0]), b);
                } catch (IllegalArgumentException ignored) { }
            }
        } catch (IOException ex) { throw new UncheckedIOException(ex); }
    }
}

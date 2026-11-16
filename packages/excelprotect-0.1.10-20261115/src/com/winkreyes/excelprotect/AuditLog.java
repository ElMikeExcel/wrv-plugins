package com.winkreyes.excelprotect;

import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * ExcelProtect 0.1.10 (P-PROTECT-WATCH-1) — the AUDIT WATCH log core: the
 * per-player gameplay log Mike commissioned Nov 14, 2026 ("We should make it
 * so we can see these things in the logs. ExcelProtect should be able to
 * watch everything").
 *
 * Layout (the per-player law, Mike, Nov 14, 2026, ~6:09 PM CST — anything
 * added to ExcelProtect is organized PER PLAYER): daily files under
 * plugins/ExcelProtect/audit/, ONE FILE PER PLAYER PER DAY —
 * audit/<player-key>-<day>.jsonl — so one player's casts, summons and mana
 * history reads as that player's own story in a single pull, never only as
 * lines in one global firehose. The same record is ALSO emitted onto the
 * canonical audit stream by the caller, so /excelprotect inspect, export
 * and the players/ timeline projection keep carrying it.
 *
 * Append-only, one DurableWriter per player (the house writer: lines are
 * removed from the queue only after they are durably written; drops are
 * counted, never silent). A record with no player identity has no
 * per-player home: it is dropped and counted (droppedNoPlayer), never
 * filed under a fake name. No Bukkit types — unit-tested off-server.
 * Excel — Wink Reyes Valley
 */
public final class AuditLog {

    static final String FILE_HEADER = new Json.Obj().put("type", "file_header")
            .put("generator", "ExcelProtect").put("version", "0.1.10")
            .put("brand", "Excel — Wink Reyes Valley").put("stream", "audit-watch").render();

    private final File dir;
    private final Map<String, DurableWriter> writers = new ConcurrentHashMap<>();
    private final AtomicLong written = new AtomicLong();
    private final AtomicLong droppedNoPlayer = new AtomicLong();

    public AuditLog(File dataDir) {
        this.dir = new File(dataDir, "audit");
        try { dir.mkdirs(); } catch (Throwable ignored) { }
    }

    public File dir() { return dir; }
    public long written() { return written.get(); }
    public long droppedNoPlayer() { return droppedNoPlayer.get(); }

    /** Filesystem-safe per-player key (the Timelines discipline). */
    public static String keyOf(String playerName) {
        return Timelines.sanitize(playerName);
    }

    /**
     * Route one gameplay record to its player's daily file. The object must
     * carry "type" and "player"; the envelope (ts / ts_ms, plus world +
     * coordinates when a location is given) is stamped here, in the ledger's
     * one timestamp dialect. Returns the player key the record was filed
     * under, or null when the record names no player (dropped + counted).
     */
    public String record(Json.Obj o, String world, Double x, Double y, Double z) {
        String player = Json.strField(o.render(), "player");
        if (player == null || player.isBlank()) { droppedNoPlayer.incrementAndGet(); return null; }
        long now = System.currentTimeMillis();
        Json.Obj head = new Json.Obj().put("ts", Ledger.isoOf(now)).put("ts_ms", now);
        if (world != null) {
            head.put("world", world);
            if (x != null) head.put("x", Ledger.round(x));
            if (y != null) head.put("y", Ledger.round(y));
            if (z != null) head.put("z", Ledger.round(z));
        }
        Json.Obj merged = new Json.Obj();
        head.forEach(merged::putRaw);
        o.forEach(merged::putRaw);
        String key = keyOf(player);
        writer(key).add(Ledger.dayOf(now), merged.render());
        written.incrementAndGet();
        return key;
    }

    private DurableWriter writer(String key) {
        return writers.computeIfAbsent(key,
                k -> new DurableWriter(dir, k + "-", ".jsonl", FILE_HEADER, 20_000));
    }

    /** Flush every per-player writer; one aggregate health line for status. */
    public String flushAll() {
        long dropped = 0, failed = 0;
        for (DurableWriter w : writers.values()) {
            try { w.flush(); } catch (Throwable ignored) { }
            dropped += w.dropped();
            failed += w.failedBatches();
        }
        if (written.get() == 0 && droppedNoPlayer.get() == 0 && dropped == 0) return "";
        return "audit-watch: files=" + writers.size() + " written=" + written.get()
                + " dropped=" + dropped + " dropped-no-player=" + droppedNoPlayer.get()
                + " failed-batches=" + failed;
    }

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
}

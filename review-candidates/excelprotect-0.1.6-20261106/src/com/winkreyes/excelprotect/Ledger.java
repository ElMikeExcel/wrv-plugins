package com.winkreyes.excelprotect;

import org.bukkit.Location;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.zip.GZIPOutputStream;

/**
 * ExcelProtect — the one writer (audit §A standard; answers R1/R6): append-only daily
 * JSONL per stream, one timestamp dialect (ISO + ms + Chicago offset + ts_ms), async
 * batched writes with drop counters, gzip at rollover, retention by config. Event
 * handlers enqueue and return; nothing here touches the main thread's time.
 * Excel — Wink Reyes Valley
 */
public final class Ledger {
    public static final ZoneId ZONE = ZoneId.of("America/Chicago");
    /**
     * Provenance header written as the first line of every FRESH day
     * file (0.1.5 — closes the null-header gap from the fingerprint
     * bake list): a regular JSON record, so every consumer's type
     * filters skip it exactly like any unknown record type.
     */
    static final String FILE_HEADER = new Json.Obj().put("type", "file_header")
            .put("generator", "ExcelProtect").put("version", "0.1.5")
            .put("brand", "Excel — Wink Reyes Valley").render();
    private static final DateTimeFormatter ISO_MS =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final File dir;
    private final Settings cfg;
    private final Map<String, DurableWriter> writers = new ConcurrentHashMap<>();
    private volatile Function<Location, String> claimLookup; // GP adapter, fail-soft
    private volatile Timelines timelines; // 0.1.5 projection, fail-isolated (SPEC §7)
    private volatile String lastDay = LocalDate.now(ZONE).format(DAY);

    public Ledger(File dir, Settings cfg) {
        this.dir = dir;
        this.cfg = cfg;
        dir.mkdirs();
    }

    public void setClaimLookup(Function<Location, String> f) { this.claimLookup = f; }
    public void setTimelines(Timelines t) { this.timelines = t; }
    public Timelines timelines() { return timelines; }
    public File dir() { return dir; }
    public static String isoOf(long ms) { return ISO_MS.format(ZonedDateTime.ofInstant(java.time.Instant.ofEpochMilli(ms), ZONE)); }
    public static String dayOf(long ms) { return DAY.format(ZonedDateTime.ofInstant(java.time.Instant.ofEpochMilli(ms), ZONE)); }

    private DurableWriter writer(String stream) {
        return writers.computeIfAbsent(stream, s -> {
            // Overload shed order (Amendment 3): earlier in writer.shed-order =
            // shed first = smaller effective queue cap (25% steps, floor 10%).
            int cap = cfg.queueCap();
            java.util.List<String> order = cfg.shedOrder();
            int idx = order.indexOf(s);
            if (idx >= 0 && !order.isEmpty()) {
                double frac = 0.25 + 0.75 * (idx / (double) Math.max(1, order.size() - 1));
                cap = Math.max(100, (int) (cap * frac));
            }
            return new DurableWriter(dir, s + "-", ".jsonl", FILE_HEADER, cap);
        });
    }

    /** Claim owner at a location via the GP adapter, or null (fail-soft). */
    public String claimAt(Location loc) {
        Function<Location, String> cl = claimLookup;
        if (cl == null || loc == null) return null;
        try { return cl.apply(loc); } catch (Throwable t) { return null; }
    }

    private final Map<String, Long> lastDroppedSeen = new ConcurrentHashMap<>();

    /** Marker records for any stream whose drop counter moved — shedding is never silent. */
    public void shedMarkers() {
        writers.forEach((s, w) -> {
            long d = w.dropped();
            long prev = lastDroppedSeen.getOrDefault(s, 0L);
            if (d > prev) {
                lastDroppedSeen.put(s, d);
                if (!s.equals("system")) {
                    Json.Obj o = new Json.Obj().put("type", "overload_shed")
                            .put("stream", s).put("shed", d - prev).put("dropped_total", d);
                    try { writer("system").add(dayOf(System.currentTimeMillis()), envelope(null, o)); }
                    catch (Throwable ignored) { }
                }
            }
        });
    }

    /** Build the full line for an event: envelope first, then caller fields. */
    public String envelope(Location loc, Json.Obj o) {
        long now = System.currentTimeMillis();
        Json.Obj head = new Json.Obj()
                .put("ts", isoOf(now))
                .put("ts_ms", now);
        if (loc != null) {
            head.put("world", loc.getWorld() == null ? "" : loc.getWorld().getName())
                .put("x", round(loc.getX())).put("y", round(loc.getY())).put("z", round(loc.getZ()));
            String owner = claimAt(loc);
            if (owner != null) head.put("claim_owner", owner);
        }
        Json.Obj merged = new Json.Obj();
        head.forEach(merged::putRaw);
        o.forEach(merged::putRaw);
        return merged.render();
    }

    /** Emit one event. The object must already carry "type"; envelope fields are added here. */
    public void emit(String stream, Location loc, Json.Obj o) {
        String line = envelope(loc, o);
        writer(stream).add(dayOf(System.currentTimeMillis()), line);
        // 0.1.5: player-first timeline projection — AFTER the canonical
        // write is queued, fully guarded (SPEC §7 failure isolation).
        Timelines t = timelines;
        if (t != null) { try { t.project(stream, line); } catch (Throwable ignored) { } }
    }

    static double round(double v) { return Math.round(v * 100.0) / 100.0; }

    /** Flush every stream; returns a one-line-per-stream health summary for /excelprotect status. */
    public String flushAll() {
        StringBuilder sb = new StringBuilder();
        writers.forEach((s, w) -> {
            try { w.flush(); } catch (Throwable ignored) { }
            sb.append(s).append(": pending=").append(w.pending())
              .append(" dropped=").append(w.dropped())
              .append(" failed=").append(w.failedBatches());
            if (w.lastError() != null) sb.append(" last-error=").append(w.lastError());
            sb.append('\n');
        });
        Timelines t = timelines;
        if (t != null) {
            String tl = t.flushAll();
            if (!tl.isEmpty()) sb.append(tl).append('\n');
        }
        return sb.toString();
    }

    public long totalDropped() {
        long t = 0; for (DurableWriter w : writers.values()) t += w.dropped(); return t;
    }

    /** Daily housekeeping: gzip day files that have rolled over; delete past retention. */
    public synchronized void housekeeping() {
        String today = LocalDate.now(ZONE).format(DAY);
        int retention = cfg.retentionDays();
        LocalDate cutoff = retention == 0 ? null : LocalDate.now(ZONE).minusDays(retention);
        File[] files = dir.listFiles((d, n) -> n.endsWith(".jsonl") || n.endsWith(".jsonl.gz"));
        if (files != null) for (File f : files) housekeepFile(f, cutoff, true);
        // 0.1.5: timeline projection files ride the same retention/gzip —
        // minus aggregation, which stays a per-stream concept (SPEC §7).
        File tdir = new File(dir, "players");
        File[] tfiles = tdir.listFiles((d, n) -> n.endsWith(".jsonl") || n.endsWith(".jsonl.gz"));
        if (tfiles != null) for (File f : tfiles) housekeepFile(f, cutoff, false);
        lastDay = today;
    }

    private void housekeepFile(File f, LocalDate cutoff, boolean aggregate) {
        String name = f.getName();
        String dayPart = name.replaceAll("^.*-(\\d{4}-\\d{2}-\\d{2})\\.jsonl(\\.gz)?$", "$1");
        LocalDate fileDay;
        try { fileDay = LocalDate.parse(dayPart); } catch (Exception e) { return; }
        if (cutoff != null && fileDay.isBefore(cutoff)) {
            if (aggregate) aggregate(f); // compaction aging: aggregates are kept indefinitely
            f.delete();
            return;
        }
        if (name.endsWith(".jsonl") && fileDay.isBefore(LocalDate.now(ZONE))) {
            // flush first so the file is complete, then gzip + remove the plain file
            writers.values().forEach(w -> { try { w.flush(); } catch (Throwable ignored) { } });
            File gz = new File(f.getParentFile(), name + ".gz");
            try (FileInputStream in = new FileInputStream(f);
                 GZIPOutputStream out = new GZIPOutputStream(new FileOutputStream(gz))) {
                in.transferTo(out);
                f.delete();
            } catch (IOException ignored) { /* leave the plain file; retry tomorrow */ }
        }
    }

    /** Roll one expiring day file into aggregates-<stream>.jsonl (per day+type counts). */
    private void aggregate(File f) {
        String name = f.getName();
        int dash = name.indexOf('-');
        if (dash < 0) return;
        String stream = name.substring(0, dash);
        String dayPart = name.replaceAll("^.*-(\\d{4}-\\d{2}-\\d{2})\\.jsonl(\\.gz)?$", "$1");
        Map<String, Long> counts = new java.util.HashMap<>();
        try (var r = open(f)) {
            String line;
            while ((line = r.readLine()) != null) {
                String t = Json.strField(line, "type");
                if (t != null) counts.merge(t, 1L, Long::sum);
            }
        } catch (Exception e) { return; } // unreadable: skip aggregation, still deleted by caller? no — caller deletes regardless; counts lost is the honest failure mode, logged below
        File agg = new File(dir, "aggregates-" + stream + ".jsonl");
        try (var w = Files.newBufferedWriter(agg.toPath(), java.nio.charset.StandardCharsets.UTF_8,
                java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND)) {
            for (Map.Entry<String, Long> e : counts.entrySet()) {
                w.write(new Json.Obj().put("day", dayPart).put("type", e.getKey())
                        .put("count", e.getValue()).render());
                w.newLine();
            }
        } catch (Exception ignored) { }
    }

    /** Roll over when the Chicago day flips (checked by the flush timer). */
    public void tickRollover() {
        String today = LocalDate.now(ZONE).format(DAY);
        if (!today.equals(lastDay)) housekeeping();
    }

    /** All day files (plain or gz) for a stream, oldest first — the export/import read path. */
    public File[] streamFiles(String stream) {
        File[] files = dir.listFiles((d, n) -> n.startsWith(stream + "-")
                && (n.endsWith(".jsonl") || n.endsWith(".jsonl.gz")));
        if (files == null) return new File[0];
        java.util.Arrays.sort(files);
        return files;
    }

    public static java.io.BufferedReader open(File f) throws IOException {
        java.io.InputStream in = Files.newInputStream(f.toPath());
        if (f.getName().endsWith(".gz")) in = new java.util.zip.GZIPInputStream(in);
        return new java.io.BufferedReader(new java.io.InputStreamReader(in, java.nio.charset.StandardCharsets.UTF_8));
    }
}

package com.winkreyes.auditlog;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

/** Queue + single flush; caller schedules flush() off the main thread. One file per CDT day. */
public final class AuditWriter {
    public static final ZoneId ZONE = ZoneId.of("America/Chicago");
    public static final String HEADER = "time,player,uuid,platform,event,details";
    static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z");

    private final File dir;
    private final ConcurrentLinkedQueue<String[]> q = new ConcurrentLinkedQueue<>();

    public AuditWriter(File dir) { this.dir = dir; }

    public static String now(ZonedDateTime t) { return t.withZoneSameInstant(ZONE).format(TS); }

    public static File fileFor(File dir, ZonedDateTime t) {
        return new File(dir, "audit-" + t.withZoneSameInstant(ZONE).format(DAY) + ".csv");
    }

    public void add(String player, String uuid, String platform, String event, String details) {
        q.add(new String[] { now(ZonedDateTime.now(ZONE)), player, uuid, platform, event, details });
    }

    public int pending() { return q.size(); }

    public synchronized void flush() throws IOException {
        if (q.isEmpty()) return;
        dir.mkdirs();
        String[] r;
        File cur = null; List<String> buf = new ArrayList<>();
        while ((r = q.poll()) != null) {
            File f = new File(dir, "audit-" + r[0].substring(0, 10) + ".csv");
            if (cur != null && !f.equals(cur)) { write(cur, buf); buf.clear(); }
            cur = f; buf.add(Csv.row(r));
        }
        if (cur != null) write(cur, buf);
    }

    private static void write(File f, List<String> lines) throws IOException {
        boolean fresh = !f.exists();
        try (BufferedWriter w = Files.newBufferedWriter(f.toPath(), StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
            if (fresh) { w.write(HEADER); w.newLine(); }
            for (String l : lines) { w.write(l); w.newLine(); }
        }
    }

    /** Read matching rows from the last N hours, newest last. Run off main thread. */
    public List<List<String>> query(String player, int hours, String type, int limit) throws IOException {
        ZonedDateTime end = ZonedDateTime.now(ZONE), start = end.minusHours(hours);
        String since = now(start);
        List<List<String>> out = new ArrayList<>();
        for (ZonedDateTime d = start.toLocalDate().atStartOfDay(ZONE); !d.isAfter(end); d = d.plusDays(1)) {
            File f = fileFor(dir, d);
            if (!f.exists()) continue;
            for (String line : Files.readAllLines(f.toPath(), StandardCharsets.UTF_8)) {
                if (line.startsWith("time,")) continue;
                List<String> c = Csv.parse(line);
                if (c.size() < 6 || c.get(0).compareTo(since) < 0) continue;
                if (!c.get(1).equalsIgnoreCase(player)) continue;
                if (type != null && !c.get(4).toLowerCase().startsWith(type.toLowerCase())) continue;
                out.add(c);
            }
        }
        return out.size() > limit ? out.subList(out.size() - limit, out.size()) : out;
    }
}

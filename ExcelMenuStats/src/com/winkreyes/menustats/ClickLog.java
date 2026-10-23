// SPDX-License-Identifier: MIT
package com.winkreyes.menustats;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

/** Append-only button click history (plugins/ExcelMenuStats/clicks.csv), one private writer thread. Mirrors ExcelShops PurchaseLog. */
public final class ClickLog implements AutoCloseable {
    public static final String FILE = "clicks.csv";
    public static final String HEADER = "timestamp,player,uuid,platform,menu,button,slot,click";
    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    public record Click(Instant at, String player, UUID uuid, String platform, String menu, String button, int slot, String click) {
        public Click { Objects.requireNonNull(at); Objects.requireNonNull(uuid); }
    }
    public record ButtonStat(String key, int clicks, int players, int java, int bedrock) { }
    public record Stats(int days, int clicks, int players, int java, int bedrock, List<ButtonStat> top, int badLines) { }

    private final Path file; private final ZoneId zone; private final Clock clock;
    private final ExecutorService writer; private final Consumer<String> warn; private volatile boolean closed;

    public ClickLog(Path folder, ZoneId zone, Clock clock, Consumer<String> warn) {
        this.file = folder.resolve(FILE); this.zone = zone; this.clock = clock; this.warn = warn;
        this.writer = Executors.newSingleThreadExecutor(r -> { Thread t = new Thread(r, "ExcelMenuStats-ClickLog"); t.setDaemon(true); return t; });
    }
    public Path file() { return file; }

    public CompletableFuture<Void> record(Click c) {
        if (closed) return CompletableFuture.completedFuture(null);
        final String line;
        try { line = toCsv(c, zone); } catch (RuntimeException bad) { warn.accept("Click line skipped: " + bad.getMessage()); return CompletableFuture.completedFuture(null); }
        try { return CompletableFuture.runAsync(() -> append(line), writer); }
        catch (RejectedExecutionException shuttingDown) { return CompletableFuture.completedFuture(null); }
    }
    private void append(String line) {
        try {
            Files.createDirectories(file.getParent());
            boolean fresh = !Files.exists(file) || Files.size(file) == 0;
            try (Writer out = Files.newBufferedWriter(file, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND, StandardOpenOption.WRITE)) {
                if (fresh) out.write(HEADER + "\n");
                out.write(line + "\n");
            }
        } catch (IOException | RuntimeException f) { warn.accept("Could not append to " + FILE + ": " + f.getMessage()); }
    }
    public CompletableFuture<Stats> stats(int days, int topN) {
        if (days < 1 || days > 3650) throw new IllegalArgumentException("days must be 1..3650");
        Instant since = clock.instant().minus(Duration.ofDays(days));
        try { return CompletableFuture.supplyAsync(() -> summarise(file, since, days, topN), writer); }
        catch (RejectedExecutionException s) { return CompletableFuture.failedFuture(s); }
    }
    static Stats summarise(Path file, Instant since, int days, int topN) {
        Map<String, int[]> by = new LinkedHashMap<>(); Map<String, Set<String>> who = new HashMap<>();
        Set<String> players = new HashSet<>(); int clicks = 0, bad = 0, java = 0, bedrock = 0;
        if (Files.exists(file)) {
            try (BufferedReader in = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                String line; boolean first = true;
                while ((line = in.readLine()) != null) {
                    if (first) { first = false; if (line.equals(HEADER)) continue; }
                    if (line.isBlank()) continue;
                    List<String> f = parseCsv(line);
                    try {
                        if (f.size() < 8) throw new IllegalArgumentException("short");
                        Instant at = OffsetDateTime.parse(f.get(0), ISO).toInstant();
                        if (at.isBefore(since)) continue;
                        String key = f.get(4) + " > " + f.get(5);
                        boolean bed = "bedrock".equalsIgnoreCase(f.get(3));
                        int[] s = by.computeIfAbsent(key, k -> new int[3]);
                        s[0]++; if (bed) s[2]++; else s[1]++;
                        who.computeIfAbsent(key, k -> new HashSet<>()).add(f.get(2));
                        clicks++; players.add(f.get(2)); if (bed) bedrock++; else java++;
                    } catch (RuntimeException m) { bad++; }
                }
            } catch (IOException e) { throw new UncheckedIOException(e); }
        }
        List<ButtonStat> top = new ArrayList<>();
        by.forEach((k, s) -> top.add(new ButtonStat(k, s[0], who.get(k).size(), s[1], s[2])));
        top.sort(Comparator.comparingInt(ButtonStat::clicks).reversed().thenComparing(ButtonStat::key));
        return new Stats(days, clicks, players.size(), java, bedrock, List.copyOf(top.subList(0, Math.min(Math.max(topN, 0), top.size()))), bad);
    }
    @Override public void close() {
        closed = true; writer.shutdown();
        try { if (!writer.awaitTermination(5, TimeUnit.SECONDS)) warn.accept("Click log did not drain within 5s."); }
        catch (InterruptedException i) { Thread.currentThread().interrupt(); }
    }
    public static String toCsv(Click c, ZoneId zone) {
        return String.join(",", ZonedDateTime.ofInstant(c.at(), zone).truncatedTo(java.time.temporal.ChronoUnit.SECONDS).format(ISO),
                esc(c.player()), c.uuid().toString(), esc(c.platform()), esc(c.menu()), esc(c.button()), Integer.toString(c.slot()), esc(c.click()));
    }
    static String esc(String v) {
        if (v == null) return "";
        String s = v.replace('\r', ' ').replace('\n', ' ');
        if (!s.isEmpty() && "=+-@".indexOf(s.charAt(0)) >= 0) s = "'" + s;
        return s.indexOf(',') >= 0 || s.indexOf('"') >= 0 ? '"' + s.replace("\"", "\"\"") + '"' : s;
    }
    static List<String> parseCsv(String line) {
        List<String> out = new ArrayList<>(); StringBuilder cur = new StringBuilder(); boolean q = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (q) { if (c == '"') { if (i + 1 < line.length() && line.charAt(i + 1) == '"') { cur.append('"'); i++; } else q = false; } else cur.append(c); }
            else if (c == '"') q = true; else if (c == ',') { out.add(cur.toString()); cur.setLength(0); } else cur.append(c);
        }
        out.add(cur.toString()); return out;
    }
}

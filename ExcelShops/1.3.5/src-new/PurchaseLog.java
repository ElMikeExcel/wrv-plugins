// SPDX-License-Identifier: MIT
package com.winkreyes.shops.log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Writer;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Append-only purchase history (plugins/ExcelShops/purchases.csv) for market-trend review.
 * Evidence only: it never gates, retries or replays a purchase. The checkout journal
 * (checkout/transactions.log) stays the authority for money/inventory reconciliation.
 *
 * Thread safety: every file operation runs on ONE private writer thread, so lines never
 * interleave and the server thread never blocks on disk. Each append opens the file in
 * APPEND mode, writes one complete line and closes it, so a crash loses at most the line
 * being written and a day boundary or a long uptime needs no rotation state.
 */
public final class PurchaseLog implements AutoCloseable {
    public static final String FILE = "purchases.csv";
    public static final String HEADER = "timestamp,player,uuid,platform,shop,item,qty,bundle_size,unit_price,total,balance_after,offer_id";
    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    public record Purchase(Instant at, String player, UUID uuid, String platform, String shop, String item,
                           int quantity, int bundleSize, long unitPriceMinor, long totalMinor,
                           Long balanceAfterMinor, String offerId) {
        public Purchase {
            Objects.requireNonNull(at); Objects.requireNonNull(uuid);
            if (quantity < 1 || bundleSize < 1) throw new IllegalArgumentException("quantity and bundle size must be positive");
        }
    }

    public record ItemStat(String key, int purchases, long items, long revenueMinor) { }
    public record Stats(int days, int purchases, long revenueMinor, long spawnRevenueMinor,
                        long playerRevenueMinor, int buyers, List<ItemStat> top, int badLines) { }

    private final Path file;
    private final ZoneId zone;
    private final Clock clock;
    private final ExecutorService writer;
    private final Consumer<String> warn;
    private volatile boolean closed;

    public PurchaseLog(Path folder, ZoneId zone, Clock clock, Consumer<String> warn) {
        this.file = Objects.requireNonNull(folder).resolve(FILE);
        this.zone = Objects.requireNonNull(zone);
        this.clock = Objects.requireNonNull(clock);
        this.warn = Objects.requireNonNull(warn);
        this.writer = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "ExcelShops-PurchaseLog");
            t.setDaemon(true);
            return t;
        });
    }

    public Path file() { return file; }

    /** Non-blocking. Safe from any thread; never throws into the checkout path. */
    public CompletableFuture<Void> record(Purchase purchase) {
        if (closed) return CompletableFuture.completedFuture(null);
        final String line;
        try { line = toCsv(purchase, zone); }
        catch (RuntimeException bad) { warn.accept("Purchase log line skipped: " + bad.getMessage()); return CompletableFuture.completedFuture(null); }
        try {
            return CompletableFuture.runAsync(() -> append(line), writer);
        } catch (RejectedExecutionException shuttingDown) {
            return CompletableFuture.completedFuture(null);
        }
    }

    private void append(String line) {
        try {
            Files.createDirectories(file.getParent());
            boolean fresh = !Files.exists(file) || Files.size(file) == 0;
            try (Writer out = Files.newBufferedWriter(file, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND, StandardOpenOption.WRITE)) {
                if (fresh) out.write(HEADER + "\n");
                out.write(line + "\n");
            }
        } catch (IOException | RuntimeException failure) {
            warn.accept("Could not append to " + FILE + ": " + failure.getMessage());
        }
    }

    /** Summarise the last {@code days} days off the server thread (same writer thread, so it sees every queued line). */
    public CompletableFuture<Stats> stats(int days, int topN) {
        if (days < 1 || days > 3650) throw new IllegalArgumentException("days must be 1..3650");
        Instant since = clock.instant().minus(Duration.ofDays(days));
        try { return CompletableFuture.supplyAsync(() -> summarise(file, since, days, topN), writer); }
        catch (RejectedExecutionException shuttingDown) { return CompletableFuture.failedFuture(shuttingDown); }
    }

    static Stats summarise(Path file, Instant since, int days, int topN) {
        Map<String, long[]> byItem = new LinkedHashMap<>();
        java.util.Set<String> buyers = new java.util.HashSet<>();
        int purchases = 0, bad = 0; long revenue = 0, spawn = 0, player = 0;
        if (Files.exists(file)) {
            try (BufferedReader in = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                String line; boolean first = true;
                while ((line = in.readLine()) != null) {
                    if (first) { first = false; if (line.equals(HEADER)) continue; }
                    if (line.isBlank()) continue;
                    List<String> f = parseCsv(line);
                    try {
                        if (f.size() < 12) throw new IllegalArgumentException("short");
                        Instant at = OffsetDateTime.parse(f.get(0), ISO).toInstant();
                        if (at.isBefore(since)) continue;
                        int qty = Integer.parseInt(f.get(6)); int bundle = Integer.parseInt(f.get(7));
                        long total = minor(f.get(9));
                        String key = f.get(11).isEmpty() ? f.get(5) : f.get(11);
                        long[] s = byItem.computeIfAbsent(key, k -> new long[3]);
                        s[0]++; s[1] += (long) qty * bundle; s[2] += total;
                        purchases++; revenue += total; buyers.add(f.get(2));
                        if ("spawn".equals(f.get(4))) spawn += total; else player += total;
                    } catch (RuntimeException malformed) { bad++; }
                }
            } catch (IOException failure) { throw new java.io.UncheckedIOException(failure); }
        }
        List<ItemStat> top = new ArrayList<>();
        byItem.forEach((k, s) -> top.add(new ItemStat(k, (int) s[0], s[1], s[2])));
        top.sort(Comparator.comparingLong(ItemStat::revenueMinor).reversed().thenComparing(ItemStat::key));
        return new Stats(days, purchases, revenue, spawn, player, buyers.size(),
                List.copyOf(top.subList(0, Math.min(Math.max(topN, 0), top.size()))), bad);
    }

    /** Drain queued lines (bounded wait) then stop. Called from onDisable. */
    @Override public void close() {
        closed = true;
        writer.shutdown();
        try {
            if (!writer.awaitTermination(5, TimeUnit.SECONDS)) warn.accept("Purchase log did not drain within 5s; remaining lines dropped.");
        } catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); }
    }

    // ---- CSV helpers (RFC 4180 quoting) ----
    public static String toCsv(Purchase p, ZoneId zone) {
        return String.join(",",
                ZonedDateTime.ofInstant(p.at(), zone).truncatedTo(java.time.temporal.ChronoUnit.SECONDS).format(ISO),
                esc(p.player()), p.uuid().toString(), esc(p.platform()), esc(p.shop()), esc(p.item()),
                Integer.toString(p.quantity()), Integer.toString(p.bundleSize()),
                money(p.unitPriceMinor()), money(p.totalMinor()),
                p.balanceAfterMinor() == null ? "" : money(p.balanceAfterMinor()), esc(p.offerId()));
    }
    static String money(long minor) { return BigDecimal.valueOf(minor, 2).toPlainString(); }
    static long minor(String money) { return new BigDecimal(money).setScale(2, java.math.RoundingMode.UNNECESSARY).movePointRight(2).longValueExact(); }
    static String esc(String v) {
        if (v == null) return "";
        String s = v.replace('\r', ' ').replace('\n', ' ');
        // Neutralise spreadsheet formula injection from player-controlled names.
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
        out.add(cur.toString());
        return out;
    }
}

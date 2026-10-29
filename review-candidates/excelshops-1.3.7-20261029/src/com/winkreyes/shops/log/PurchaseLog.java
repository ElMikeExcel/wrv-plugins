// SPDX-License-Identifier: MIT
// Excel: analytics lifecycle, exclusive writer ownership and bounded optional stats hardening.
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
 * One private writer owns an analytics-only OS lock until actual executor termination.
 * Accepted rows remain memory queued; a process crash can lose pending rows. Successful
 * completion means append/close completed, not fsync durability. Purchase admission is
 * intentionally unchanged/unbounded: no new overflow-loss or checkout-blocking policy.
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
    private final java.util.concurrent.ThreadPoolExecutor writer;
    private final Consumer<String> warn;
    private boolean closed;
    private boolean statsPending;
    private java.nio.channels.FileChannel lockChannel;
    private java.nio.channels.FileLock ownership;
    private String ownershipKey;
    // Excel: string-only JVM marker shared across classloaders, with no plugin objects.
    // A contender must not close a second descriptor: some OSes then release all JVM locks.
    private static final String REGISTRY_KEY = "com.winkreyes.ExcelShops.analytics.owner.v1:";
    private final String ownershipToken = UUID.randomUUID().toString();
    private long accepted, completed, failed, rejected;
    private long lastWarningNanos;
    public enum CloseResult { OPEN, TERMINATED, TIMED_OUT, INTERRUPTED }
    private volatile CloseResult closeResult = CloseResult.OPEN;
    public record Status(long accepted, long completed, long failed, long rejected,
                         long pending, boolean closing, boolean terminated) { }

    public PurchaseLog(Path folder, ZoneId zone, Clock clock, Consumer<String> warn) {
        this.file = Objects.requireNonNull(folder).resolve(FILE);
        this.zone = Objects.requireNonNull(zone);
        this.clock = Objects.requireNonNull(clock);
        this.warn = Objects.requireNonNull(warn);
        this.writer = new java.util.concurrent.ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS,
                new java.util.concurrent.LinkedBlockingQueue<>(), r -> {
                    Thread t = new Thread(r, "ExcelShops-PurchaseLog");
                    t.setDaemon(true); return t;
                }) {
            @Override protected void terminated() {
                releaseOwnership();
                closeResult = CloseResult.TERMINATED;
            }
        };
    }

    public Path file() { return file; }
    public synchronized Status status() {
        return new Status(accepted, completed, failed, rejected, accepted-completed-failed,
                          closed, writer.isTerminated());
    }
    public CloseResult closeResult() { return writer.isTerminated() ? CloseResult.TERMINATED : closeResult; }

    // Excel: diagnostic failures must never escape into an already committed checkout.
    private void warning(String message, boolean force) {
        synchronized (this) {
            long now = System.nanoTime();
            if (!force && lastWarningNanos != 0 && now-lastWarningNanos < TimeUnit.SECONDS.toNanos(30)) return;
            lastWarningNanos = now;
        }
        try { warn.accept(message); } catch (RuntimeException | LinkageError ignored) { }
    }
    private synchronized <T> CompletableFuture<T> rejection(String reason) {
        rejected++;
        warning(reason + "; analytics unavailable, committed purchases are not retried. " + status(), false);
        return CompletableFuture.failedFuture(new RejectedExecutionException(reason));
    }

    /** Nonblocking admission. Completion acknowledges append/close only, never checkout durability. */
    public synchronized CompletableFuture<Void> record(Purchase purchase) {
        if (closed) return rejection("Purchase log is closing");
        final String line;
        try { line = toCsv(purchase, zone); }
        catch (RuntimeException bad) { return rejection("Invalid purchase log row: " + bad.getMessage()); }
        CompletableFuture<Void> result = new CompletableFuture<>();
        try {
            writer.execute(() -> {
                try {
                    append(line);
                    synchronized (this) { completed++; }
                    result.complete(null);
                } catch (IOException | RuntimeException failure) {
                    synchronized (this) { failed++; }
                    warning("Could not append to " + FILE + ": " + failure + "; " + status(), false);
                    result.completeExceptionally(failure);
                }
            });
            accepted++;
        } catch (RejectedExecutionException stopping) { return rejection("Purchase log executor closed"); }
        return result;
    }

    // Called only on the writer. File lock also excludes another plugin classloader/process.
    private void ensureOwnership() throws IOException {
        if (ownership != null) return;
        Files.createDirectories(file.getParent());
        Path lockPath = file.getParent().toRealPath().resolve(FILE + ".analytics.lock");
        java.util.Properties registry = System.getProperties();
        String key = REGISTRY_KEY + lockPath;
        synchronized (registry) {
            if (registry.containsKey(key)) throw new IOException("Another purchase analytics writer still owns this folder");
            registry.setProperty(key, ownershipToken);
        }
        java.nio.channels.FileChannel channel = null;
        try {
            channel = java.nio.channels.FileChannel.open(lockPath, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
            java.nio.channels.FileLock acquired;
            try { acquired = channel.tryLock(); }
            catch (java.nio.channels.OverlappingFileLockException busy) { throw new IOException("Another purchase analytics writer still owns this folder", busy); }
            if (acquired == null) throw new IOException("Another purchase analytics writer still owns this folder");
            lockChannel = channel; ownership = acquired; ownershipKey = key;
        } finally {
            if (ownership == null) {
                try { if (channel != null) channel.close(); }
                finally { if (channel == null || !channel.isOpen()) registry.remove(key, ownershipToken); }
            }
        }
    }
    private void releaseOwnership() {
        if (ownership == null) return;
        try { ownership.release(); }
        catch (IOException failure) { warning("Analytics lock release failed: " + failure, true); }
        finally {
            try { lockChannel.close(); }
            catch (IOException failure) { warning("Analytics lock channel close failed: " + failure, true); }
            // Fail closed if a failed close left a live descriptor.
            if (!lockChannel.isOpen()) System.getProperties().remove(ownershipKey, ownershipToken);
        }
    }
    private void append(String line) throws IOException {
        ensureOwnership();
        boolean fresh = !Files.exists(file) || Files.size(file) == 0;
        try (Writer out = Files.newBufferedWriter(file, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND, StandardOpenOption.WRITE)) {
            if (fresh) out.write(HEADER + "\n");
            out.write(line + "\n");
        }
    }

    /** At most one optional scan pending. Refuse extras explicitly; never share stale query results. */
    public synchronized CompletableFuture<Stats> stats(int days, int topN) {
        if (days < 1 || days > 3650) throw new IllegalArgumentException("days must be 1..3650");
        if (closed) return CompletableFuture.failedFuture(new RejectedExecutionException("Purchase log is closing"));
        if (statsPending) return CompletableFuture.failedFuture(new RejectedExecutionException("Stats busy; retry after the current scan finishes"));
        Instant since = clock.instant().minus(Duration.ofDays(days));
        CompletableFuture<Stats> result = new CompletableFuture<>();
        statsPending = true;
        try { writer.execute(() -> {
            Stats value = null; Throwable error = null;
            try { ensureOwnership(); value = summarise(file, since, days, topN); }
            catch (IOException | RuntimeException failure) { error = failure; }
            finally { synchronized (this) { statsPending = false; } }
            if (error == null) result.complete(value); else result.completeExceptionally(error);
        }); } catch (RejectedExecutionException stopping) { statsPending = false; result.completeExceptionally(stopping); }
        return result;
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

    /** Bounded wait, not a guarantee of quiescence. Ownership lasts through actual drain. */
    @Override public void close() {
        synchronized (this) { closed = true; }
        writer.shutdown();
        try {
            if (writer.awaitTermination(5, TimeUnit.SECONDS)) closeResult = CloseResult.TERMINATED;
            else {
                if (!writer.isTerminated()) closeResult = CloseResult.TIMED_OUT;
                warning("Purchase analytics still draining after 5s; rows are NOT discarded. "
                        + "Ownership remains held until actual termination; not a clean backup boundary. " + status(), true);
            }
        } catch (InterruptedException interrupted) {
            if (!writer.isTerminated()) closeResult = CloseResult.INTERRUPTED;
            warning("Purchase analytics close interrupted; drain/ownership may still be active. " + status(), true);
            Thread.currentThread().interrupt();
        }
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

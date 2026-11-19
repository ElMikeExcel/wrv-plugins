// SPDX-License-Identifier: MIT
package com.winkreyes.shops.log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
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
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Purchase analytics (ExcelShops 1.3.7, reconstructed from the live
 * 1.3.7-analytics-OFFLINE jar — see the 1.3.8 build record).
 *
 * Appends every completed purchase to purchases.csv in the plugin
 * folder (config purchase-log, default on) as market-trend history.
 * Evidence only: the checkout transactions.log stays the
 * reconciliation journal, and a purchase is never retried or held
 * because analytics failed — record() is fire-and-forget from the
 * caller's side, with failures counted and rate-limited warnings.
 *
 * A single daemon writer thread owns the file. Ownership is claimed
 * per data folder (a JVM-wide registry key plus an OS file lock on
 * purchases.csv.analytics.lock) so two plugin instances can never
 * interleave rows. stats() re-reads the CSV on the writer thread
 * and summarises a trailing window: totals, the spawn/player split,
 * distinct buyers, and the top items by revenue.
 */
public final class PurchaseLog implements AutoCloseable {
    public static final String FILE = "purchases.csv";
    public static final String HEADER =
            "timestamp,player,uuid,platform,shop,item,qty,bundle_size,unit_price,total,balance_after,offer_id";
    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
    private static final String REGISTRY_KEY = "com.winkreyes.ExcelShops.analytics.owner.v1:";
    private final Path file;
    private final ZoneId zone;
    private final Clock clock;
    private final ThreadPoolExecutor writer;
    private final Consumer<String> warn;
    private boolean closed;
    private boolean statsPending;
    private FileChannel lockChannel;
    private FileLock ownership;
    private String ownershipKey;
    private final String ownershipToken = UUID.randomUUID().toString();
    private long accepted;
    private long completed;
    private long failed;
    private long rejected;
    private long lastWarningNanos;
    private volatile CloseResult closeResult = CloseResult.OPEN;

    public PurchaseLog(Path directory, ZoneId zone, Clock clock, Consumer<String> warn) {
        this.file = Objects.requireNonNull(directory).resolve(FILE);
        this.zone = Objects.requireNonNull(zone);
        this.clock = Objects.requireNonNull(clock);
        this.warn = Objects.requireNonNull(warn);
        this.writer = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(),
                runnable -> {
                    Thread thread = new Thread(runnable, "ExcelShops-PurchaseLog");
                    thread.setDaemon(true);
                    return thread;
                }) {
            @Override protected void terminated() {
                PurchaseLog.this.releaseOwnership();
                PurchaseLog.this.closeResult = CloseResult.TERMINATED;
            }
        };
    }

    public Path file() { return file; }

    public synchronized Status status() {
        return new Status(accepted, completed, failed, rejected,
                accepted - completed - failed, closed, writer.isTerminated());
    }

    public CloseResult closeResult() {
        return writer.isTerminated() ? CloseResult.TERMINATED : closeResult;
    }

    private void warning(String message, boolean force) {
        synchronized (this) {
            long now = System.nanoTime();
            if (!force && lastWarningNanos != 0L && now - lastWarningNanos < TimeUnit.SECONDS.toNanos(30L)) return;
            lastWarningNanos = now;
        }
        try {
            warn.accept(message);
        } catch (LinkageError | RuntimeException ignored) {
            // A broken warning sink must never break analytics or the caller.
        }
    }

    private synchronized <T> CompletableFuture<T> rejection(String message) {
        rejected++;
        warning(message + "; analytics unavailable, committed purchases are not retried. " + status(), false);
        return CompletableFuture.failedFuture(new RejectedExecutionException(message));
    }

    public synchronized CompletableFuture<Void> record(Purchase purchase) {
        if (closed) return rejection("Purchase log is closing");
        final String csv;
        try {
            csv = toCsv(purchase, zone);
        } catch (RuntimeException failure) {
            return rejection("Invalid purchase log row: " + failure.getMessage());
        }
        CompletableFuture<Void> done = new CompletableFuture<>();
        try {
            writer.execute(() -> {
                try {
                    append(csv);
                    synchronized (this) { completed++; }
                    done.complete(null);
                } catch (RuntimeException | IOException failure) {
                    synchronized (this) { failed++; }
                    warning("Could not append to purchases.csv: " + failure + "; " + status(), false);
                    done.completeExceptionally(failure);
                }
            });
            accepted++;
        } catch (RejectedExecutionException failure) {
            return rejection("Purchase log executor closed");
        }
        return done;
    }

    private void ensureOwnership() throws IOException {
        if (ownership != null) return;
        Files.createDirectories(file.getParent());
        Path lockFile = file.getParent().toRealPath().resolve("purchases.csv.analytics.lock");
        Properties properties = System.getProperties();
        String key = REGISTRY_KEY + lockFile;
        synchronized (properties) {
            if (properties.containsKey(key)) {
                throw new IOException("Another purchase analytics writer still owns this folder");
            }
            properties.setProperty(key, ownershipToken);
        }
        FileChannel channel = null;
        try {
            channel = FileChannel.open(lockFile, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
            FileLock lock;
            try {
                lock = channel.tryLock();
            } catch (OverlappingFileLockException overlap) {
                throw new IOException("Another purchase analytics writer still owns this folder", overlap);
            }
            if (lock == null) {
                throw new IOException("Another purchase analytics writer still owns this folder");
            }
            this.lockChannel = channel;
            this.ownership = lock;
            this.ownershipKey = key;
        } finally {
            if (this.ownership == null) {
                try {
                    if (channel != null) channel.close();
                } finally {
                    if (channel == null || !channel.isOpen()) properties.remove(key, ownershipToken);
                }
            }
        }
    }

    private void releaseOwnership() {
        if (ownership == null) return;
        try {
            ownership.release();
        } catch (IOException failure) {
            warning("Analytics lock release failed: " + failure, true);
        } finally {
            try {
                lockChannel.close();
            } catch (IOException failure) {
                warning("Analytics lock channel close failed: " + failure, true);
            }
            if (!lockChannel.isOpen()) {
                System.getProperties().remove(ownershipKey, ownershipToken);
            }
        }
    }

    private void append(String csv) throws IOException {
        ensureOwnership();
        boolean needsHeader = !Files.exists(file) || Files.size(file) == 0L;
        try (Writer out = Files.newBufferedWriter(file, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND, StandardOpenOption.WRITE)) {
            if (needsHeader) out.write(HEADER + "\n");
            out.write(csv + "\n");
        }
    }

    public synchronized CompletableFuture<Stats> stats(int days, int topLimit) {
        if (days < 1 || days > 3650) throw new IllegalArgumentException("days must be 1..3650");
        if (closed) return CompletableFuture.failedFuture(new RejectedExecutionException("Purchase log is closing"));
        if (statsPending) {
            return CompletableFuture.failedFuture(
                    new RejectedExecutionException("Stats busy; retry after the current scan finishes"));
        }
        Instant since = clock.instant().minus(Duration.ofDays(days));
        CompletableFuture<Stats> done = new CompletableFuture<>();
        statsPending = true;
        try {
            writer.execute(() -> {
                Stats result = null;
                Exception failure = null;
                try {
                    ensureOwnership();
                    result = summarise(file, since, days, topLimit);
                } catch (RuntimeException | IOException ex) {
                    failure = ex;
                } finally {
                    synchronized (this) { statsPending = false; }
                }
                if (failure == null) done.complete(result);
                else done.completeExceptionally(failure);
            });
        } catch (RejectedExecutionException failure) {
            statsPending = false;
            done.completeExceptionally(failure);
        }
        return done;
    }

    static Stats summarise(Path file, Instant since, int days, int topLimit) {
        Map<String, long[]> byItem = new LinkedHashMap<>();
        Set<String> buyers = new HashSet<>();
        int purchases = 0;
        int badLines = 0;
        long revenue = 0L;
        long spawnRevenue = 0L;
        long playerRevenue = 0L;
        if (Files.exists(file)) {
            try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                boolean first = true;
                String line;
                while ((line = reader.readLine()) != null) {
                    if (first) {
                        first = false;
                        if (line.equals(HEADER)) continue;
                    }
                    if (line.isBlank()) continue;
                    List<String> fields = parseCsv(line);
                    try {
                        if (fields.size() < 12) throw new IllegalArgumentException("short");
                        Instant at = OffsetDateTime.parse(fields.get(0), ISO).toInstant();
                        if (!at.isBefore(since)) {
                            int quantity = Integer.parseInt(fields.get(6));
                            int bundleSize = Integer.parseInt(fields.get(7));
                            long total = minor(fields.get(9));
                            String key = fields.get(11).isEmpty() ? fields.get(5) : fields.get(11);
                            long[] counts = byItem.computeIfAbsent(key, unused -> new long[3]);
                            counts[0]++;
                            counts[1] += (long) quantity * bundleSize;
                            counts[2] += total;
                            purchases++;
                            revenue += total;
                            buyers.add(fields.get(2));
                            if ("spawn".equals(fields.get(4))) spawnRevenue += total;
                            else playerRevenue += total;
                        }
                    } catch (RuntimeException bad) {
                        badLines++;
                    }
                }
            } catch (IOException failure) {
                throw new UncheckedIOException(failure);
            }
        }
        List<ItemStat> top = new ArrayList<>();
        byItem.forEach((key, counts) -> top.add(new ItemStat(key, (int) counts[0], counts[1], counts[2])));
        top.sort(Comparator.comparingLong(ItemStat::revenueMinor).reversed().thenComparing(ItemStat::key));
        return new Stats(days, purchases, revenue, spawnRevenue, playerRevenue, buyers.size(),
                List.copyOf(top.subList(0, Math.min(Math.max(topLimit, 0), top.size()))), badLines);
    }

    @Override public void close() {
        synchronized (this) { closed = true; }
        writer.shutdown();
        try {
            if (writer.awaitTermination(5L, TimeUnit.SECONDS)) {
                closeResult = CloseResult.TERMINATED;
            } else {
                if (!writer.isTerminated()) closeResult = CloseResult.TIMED_OUT;
                warning("Purchase analytics still draining after 5s; rows are NOT discarded. "
                        + "Ownership remains held until actual termination; not a clean backup boundary. "
                        + status(), true);
            }
        } catch (InterruptedException failure) {
            if (!writer.isTerminated()) closeResult = CloseResult.INTERRUPTED;
            warning("Purchase analytics close interrupted; drain/ownership may still be active. " + status(), true);
            Thread.currentThread().interrupt();
        }
    }

    public static String toCsv(Purchase purchase, ZoneId zone) {
        return String.join(",",
                ZonedDateTime.ofInstant(purchase.at(), zone).truncatedTo(ChronoUnit.SECONDS).format(ISO),
                esc(purchase.player()),
                purchase.uuid().toString(),
                esc(purchase.platform()),
                esc(purchase.shop()),
                esc(purchase.item()),
                Integer.toString(purchase.quantity()),
                Integer.toString(purchase.bundleSize()),
                money(purchase.unitPriceMinor()),
                money(purchase.totalMinor()),
                purchase.balanceAfterMinor() == null ? "" : money(purchase.balanceAfterMinor()),
                esc(purchase.offerId()));
    }

    static String money(long minorUnits) {
        return BigDecimal.valueOf(minorUnits, 2).toPlainString();
    }

    static long minor(String text) {
        return new BigDecimal(text).setScale(2, RoundingMode.UNNECESSARY).movePointRight(2).longValueExact();
    }

    static String esc(String text) {
        if (text == null) return "";
        String cleaned = text.replace('\r', ' ').replace('\n', ' ');
        if (!cleaned.isEmpty() && "=+-@".indexOf(cleaned.charAt(0)) >= 0) cleaned = "'" + cleaned;
        return cleaned.indexOf(',') < 0 && cleaned.indexOf('"') < 0
                ? cleaned : "\"" + cleaned.replace("\"", "\"\"") + "\"";
    }

    static List<String> parseCsv(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (quoted) {
                if (c == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        current.append('"');
                        i++;
                    } else {
                        quoted = false;
                    }
                } else {
                    current.append(c);
                }
            } else if (c == '"') {
                quoted = true;
            } else if (c == ',') {
                fields.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        fields.add(current.toString());
        return fields;
    }

    public enum CloseResult { OPEN, TERMINATED, TIMED_OUT, INTERRUPTED }

    public record ItemStat(String key, int purchases, long items, long revenueMinor) { }

    public record Purchase(Instant at, String player, UUID uuid, String platform, String shop, String item,
                           int quantity, int bundleSize, long unitPriceMinor, long totalMinor,
                           Long balanceAfterMinor, String offerId) {
        public Purchase {
            Objects.requireNonNull(at);
            Objects.requireNonNull(uuid);
            if (quantity < 1 || bundleSize < 1) {
                throw new IllegalArgumentException("quantity and bundle size must be positive");
            }
        }
    }

    public record Stats(int days, int purchases, long revenueMinor, long spawnRevenueMinor,
                        long playerRevenueMinor, int buyers, List<ItemStat> top, int badLines) { }

    public record Status(long accepted, long completed, long failed, long rejected, long pending,
                         boolean closing, boolean terminated) { }
}

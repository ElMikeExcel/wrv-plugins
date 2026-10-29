// Excel: assert truthful closed and failed analytics futures.
package com.winkreyes.shops.log;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PurchaseLogTest {
    @TempDir Path dir;
    static final ZoneId CHI = ZoneId.of("America/Chicago");
    static final UUID A = new UUID(0L, 0x00000000000aL);

    PurchaseLog.Purchase p(Instant at, String name, String shop, String offer, int qty, long unit, Long after) {
        return new PurchaseLog.Purchase(at, name, A, "java", shop, "minecraft:enchanted_book[mending:1]", qty, 1, unit, unit * qty, after, offer);
    }

    @Test void headerOnceAndIsoTimestampWithZone() throws Exception {
        Instant at = Instant.parse("2026-10-23T21:30:00Z");
        try (PurchaseLog log = new PurchaseLog(dir, CHI, Clock.fixed(at, ZoneOffset.UTC), m -> fail(m))) {
            log.record(p(at, "FixtureBuyerA", "spawn", "spawn:mending", 2, 200000, 3117100L)).get(5, TimeUnit.SECONDS);
            log.record(p(at, "FixtureBuyerB", "player", "shop:x", 1, 1050, null)).get(5, TimeUnit.SECONDS);
        }
        List<String> lines = Files.readAllLines(dir.resolve("purchases.csv"));
        assertEquals(PurchaseLog.HEADER, lines.get(0));
        assertEquals(3, lines.size());
        assertEquals("2026-10-23T16:30:00-05:00,FixtureBuyerA," + A + ",java,spawn,minecraft:enchanted_book[mending:1],2,1,2000.00,4000.00,31171.00,spawn:mending", lines.get(1));
        assertTrue(lines.get(2).endsWith(",10.50,10.50,,shop:x"));
    }

    @Test void concurrentAppendsNeverInterleave() throws Exception {
        Instant at = Instant.parse("2026-10-23T12:00:00Z");
        int threads = 8, each = 250;
        try (PurchaseLog log = new PurchaseLog(dir, CHI, Clock.fixed(at, ZoneOffset.UTC), m -> fail(m))) {
            ExecutorService pool = Executors.newFixedThreadPool(threads);
            List<Future<?>> fs = new ArrayList<>();
            List<CompletableFuture<Void>> writes = Collections.synchronizedList(new ArrayList<>());
            for (int t = 0; t < threads; t++) { int tt = t; fs.add(pool.submit(() -> { for (int i = 0; i < each; i++) writes.add(log.record(p(at, "P" + tt, "spawn", "spawn:o" + i, 1, 100, 1L))); })); }
            for (Future<?> f : fs) f.get();
            pool.shutdown();
            CompletableFuture.allOf(writes.toArray(CompletableFuture[]::new)).get(30, TimeUnit.SECONDS);
        }
        List<String> lines = Files.readAllLines(dir.resolve("purchases.csv"));
        assertEquals(1 + threads * each, lines.size());
        assertEquals(1, lines.stream().filter(PurchaseLog.HEADER::equals).count());
        for (String l : lines.subList(1, lines.size())) assertEquals(12, PurchaseLog.parseCsv(l).size(), l);
    }

    @Test void escapingAndFormulaNeutralised() {
        Instant at = Instant.parse("2026-10-23T12:00:00Z");
        String line = PurchaseLog.toCsv(new PurchaseLog.Purchase(at, "=cmd,\"x\"", A, "bedrock", "spawn", "minecraft:stone", 1, 64, 5, 5, 0L, "spawn:s"), CHI);
        List<String> f = PurchaseLog.parseCsv(line);
        assertEquals(12, f.size());
        assertEquals("'=cmd,\"x\"", f.get(1));
        assertEquals("bedrock", f.get(3));
    }

    @Test void statsWindowTopItemsAndBadLines() throws Exception {
        Instant now = Instant.parse("2026-10-23T22:00:00Z");
        try (PurchaseLog log = new PurchaseLog(dir, CHI, Clock.fixed(now, ZoneOffset.UTC), m -> { })) {
            log.record(p(now.minus(Duration.ofDays(10)), "Old", "spawn", "spawn:mending", 5, 200000, 0L));
            log.record(p(now.minus(Duration.ofHours(1)), "FixtureBuyerA", "spawn", "spawn:mending", 1, 200000, 0L));
            log.record(p(now.minus(Duration.ofHours(2)), "FixtureBuyerB", "spawn", "spawn:torch", 3, 1000, 0L));
            log.record(p(now.minus(Duration.ofHours(3)), "FixtureBuyerB", "player", "shop:x", 1, 500, 0L)).get(5, TimeUnit.SECONDS);
            Files.writeString(dir.resolve("purchases.csv"), "garbage,line\n", StandardCharsets.UTF_8, java.nio.file.StandardOpenOption.APPEND);
            PurchaseLog.Stats st = log.stats(7, 10).get(5, TimeUnit.SECONDS);
            assertEquals(3, st.purchases());
            assertEquals(200000 + 3000 + 500, st.revenueMinor());
            assertEquals(203000, st.spawnRevenueMinor());
            assertEquals(500, st.playerRevenueMinor());
            assertEquals(1, st.buyers());
            assertEquals("spawn:mending", st.top().get(0).key());
            assertEquals(1, st.badLines());
            assertEquals(1, log.stats(7, 1).get(5, TimeUnit.SECONDS).top().size());
            assertThrows(IllegalArgumentException.class, () -> log.stats(0, 10));
        }
    }

    @Test void statsOnMissingFileIsEmptyAndClosedLogRejectsWrites() throws Exception {
        PurchaseLog log = new PurchaseLog(dir, CHI, Clock.systemUTC(), m -> { });
        assertEquals(0, log.stats(30, 10).get(5, TimeUnit.SECONDS).purchases());
        log.close();
        assertThrows(ExecutionException.class, () -> log.record(p(Instant.now(), "X", "spawn", "spawn:a", 1, 1, 1L)).get(1, TimeUnit.SECONDS));
        assertFalse(Files.exists(dir.resolve("purchases.csv")));
    }

    @Test void writeFailureWarnsInsteadOfThrowing() throws Exception {
        Path blocker = dir.resolve("notadir"); Files.writeString(blocker, "x");
        List<String> warnings = new CopyOnWriteArrayList<>();
        try (PurchaseLog log = new PurchaseLog(blocker, CHI, Clock.systemUTC(), warnings::add)) {
            assertThrows(ExecutionException.class, () -> log.record(p(Instant.now(), "X", "spawn", "spawn:a", 1, 1, 1L)).get(5, TimeUnit.SECONDS));
        }
        assertEquals(1, warnings.size());
    }

    @Test void platformDetection() {
        ClassLoader cl = getClass().getClassLoader();
        org.geysermc.fixtures.FloodgateFixture.reset();
        assertEquals("java", Platform.detect(null).of(A));
        assertEquals("bedrock", Platform.detect(cl).of(A));
        org.geysermc.fixtures.FloodgateFixture.bedrock = false;
        assertEquals("java", Platform.detect(cl).of(A));
        org.geysermc.fixtures.FloodgateFixture.throwOnLookup = true;
        assertEquals("unknown", Platform.detect(cl).of(A));
        org.geysermc.fixtures.FloodgateFixture.reset();
    }
}

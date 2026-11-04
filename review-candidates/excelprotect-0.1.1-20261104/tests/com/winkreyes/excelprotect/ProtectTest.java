package com.winkreyes.excelprotect;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** ExcelProtect unit suite — pure logic (no server): envelope extraction, query filters, writer durability, free-first chain, payload codec, diff math. */
public class ProtectTest {

    @Test void jsonRoundTripAndFields() {
        String line = new Json.Obj().put("type", "entity_death").put("ts_ms", 1793580430740L)
                .put("name", "Kailey's \"stand\"").put("x", 8.5)
                .put("damage", new Json.Obj().put("total", 9)).render();
        assertEquals("entity_death", Json.strField(line, "type"));
        assertEquals("Kailey's \"stand\"", Json.strField(line, "name"));
        assertEquals(9, Json.numField(Json.rawField(line, "damage"), "total", 0));
        assertEquals(List.of("a", "b"), Json.strings("[\"a\",\"b\"]"));
    }

    @Test void settingsGlob() {
        assertTrue(Settings.glob("minecraft:diamond_*", "minecraft:diamond_sword"));
        assertTrue(Settings.glob("minecraft:*shulker_box", "minecraft:red_shulker_box"));
        assertFalse(Settings.glob("minecraft:diamond_*", "minecraft:iron_sword"));
    }

    @Test void exporterFilters() {
        String stand = "{\"ts\":\"t\",\"ts_ms\":2000,\"type\":\"entity_death\",\"world\":\"world\",\"x\":8.5,\"y\":-60,\"z\":8.5,\"entity\":\"armor_stand\"}";
        String egg = "{\"ts\":\"t\",\"ts_ms\":2000,\"type\":\"item_pickup\",\"world\":\"world\",\"x\":1,\"y\":2,\"z\":3,\"player\":\"Kai\",\"item\":{\"id\":\"minecraft:egg\"}}";
        Exporter.Query q = Exporter.Query.parse(new String[]{"type=entity_death", "match=armor_stand"});
        assertTrue(q.accepts(stand));
        assertFalse(q.accepts(egg));
        Exporter.Query r = Exporter.Query.parse(new String[]{"radius=5", "at=8,-60,8"});
        assertTrue(r.accepts(stand));
        assertFalse(r.accepts(egg));
        Exporter.Query p = Exporter.Query.parse(new String[]{"player=Kai"});
        assertTrue(p.accepts(egg));
        assertFalse(p.accepts(stand));
        Exporter.Query w = Exporter.Query.parse(new String[]{"hours=1"});
        assertFalse(w.accepts(stand)); // ts_ms=2000 is far outside the last hour
    }

    @Test void damageLedgerReplay() {
        DamageLedger led = new DamageLedger(100);
        UUID v = UUID.randomUUID(), z = UUID.randomUUID();
        led.record(v, 1000, "entity:zombie:" + z, "entity", z.toString(), "zombie", "ENTITY_SWEEP_ATTACK", 4.0);
        led.record(v, 2000, "env:fire_tick", "environment", null, null, "FIRE_TICK", 1.0);
        led.record(v, 3000, "env:fire_tick", "environment", null, null, "FIRE_TICK", 4.0);
        DamageLedger.Book b = led.take(v);
        assertEquals(9.0, b.total);
        assertEquals("environment", b.lastKind);
        assertEquals(4.0, b.byCause.get("ENTITY_SWEEP_ATTACK"));
        assertEquals(5.0, b.byAttacker.get("env:fire_tick"));
        assertNull(led.take(v));
    }

    @Test void durableWriterRetainsOnFailure(@TempDir File dir) throws Exception {
        File bad = new File(dir, "nope");
        Files.writeString(new File(dir, "nope").toPath(), "x", StandardCharsets.UTF_8); // a FILE where the dir must be
        DurableWriter w = new DurableWriter(bad, "s-", ".jsonl", null, 100);
        w.add("2026-11-01", "{\"a\":1}");
        assertThrows(java.io.IOException.class, w::flush); // failure is reported, never silent
        assertEquals(1, w.pending()); // retained, not lost (the AuditLog 0.2.0 defect, inverted)
        assertEquals(1, w.failedBatches());
    }

    @Test void durableWriterWrites(@TempDir File dir) throws Exception {
        DurableWriter w = new DurableWriter(dir, "blocks-", ".jsonl", null, 100);
        w.add("2026-11-01", "{\"a\":1}");
        w.add("2026-11-01", "{\"a\":2}");
        w.flush();
        List<String> lines = Files.readAllLines(new File(dir, "blocks-2026-11-01.jsonl").toPath());
        assertEquals(2, lines.size());
        assertEquals(0, w.pending());
    }

    @Test void payloadCodecRoundTrip() throws Exception {
        byte[] raw = new byte[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
        String enc = ItemFact.encode(raw);
        assertArrayEquals(raw, ItemFact.decode(enc));
        // plain base64 (DeathRecord import shape) must also decode
        String plain = java.util.Base64.getEncoder().encodeToString(raw);
        assertArrayEquals(raw, ItemFact.decode(plain));
    }

    @Test void containerDiffMath() {
        ItemFact a1 = new ItemFact("minecraft:diamond", 3, 0, null, null, null, null, null, null);
        ItemFact a2 = new ItemFact("minecraft:diamond", 2, 0, null, null, null, null, null, null);
        Map<String, ItemFact> m = Containers.merged(List.of(a1, a2));
        assertEquals(1, m.size());
        assertEquals(5, m.values().iterator().next().count());
    }

    @Test void freeFirstRuleChain(@TempDir File dir) {
        YamlConfiguration y = new YamlConfiguration();
        y.set("lostfound.free-first-recovery", true);
        y.set("lostfound.free-threshold", 10_000);
        y.set("lostfound.free-only-if-no-earlier-recovery", true);
        Settings cfg = new Settings(y);
        Ledger ledger = new Ledger(new File(dir, "data"), cfg);
        Recovery rec = new Recovery(ledger, cfg, new File(dir, "data"));
        UUID u = UUID.randomUUID();
        assertTrue(rec.freeApplies(u, 5_000));      // poor + never recovered → free
        assertFalse(rec.freeApplies(u, 20_000));    // balance at/over threshold → not free
        rec.noteRecovery(u, false);                  // a PAID recovery happened
        assertFalse(rec.freeApplies(u, 5_000));      // earlier recovery blocks free (rule chain)
        UUID v = UUID.randomUUID();
        rec.noteRecovery(v, true);                   // free used
        assertFalse(rec.freeApplies(v, 5_000));      // once ever
    }

    @Test void compactionAgingKeepsAggregates(@TempDir File dir) throws Exception {
        YamlConfiguration y = new YamlConfiguration();
        y.set("retention-days", 30);
        Settings cfg = new Settings(y);
        Ledger ledger = new Ledger(dir, cfg);
        File old = new File(dir, "blocks-2020-01-01.jsonl");
        Files.writeString(old.toPath(),
                "{\"type\":\"block_change\",\"action\":\"break\"}\n{\"type\":\"block_change\",\"action\":\"place\"}\n{\"type\":\"sign_edit\"}\n",
                StandardCharsets.UTF_8);
        ledger.housekeeping();
        assertFalse(old.exists()); // raw deleted past retention
        File agg = new File(dir, "aggregates-blocks.jsonl");
        assertTrue(agg.exists()); // aggregates kept indefinitely
        String aggText = Files.readString(agg.toPath(), StandardCharsets.UTF_8);
        assertTrue(aggText.contains("\"day\":\"2020-01-01\""));
        assertTrue(aggText.contains("\"count\":2"));
    }

    // ------------------------------------------------ 0.1.1 inspect query forms

    private static String env(long tsMs, String world, long x, long y, long z, String body) {
        return "{\"ts\":\"" + Ledger.isoOf(tsMs) + "\",\"ts_ms\":" + tsMs
                + ",\"world\":\"" + world + "\",\"x\":" + x + ",\"y\":" + y + ",\"z\":" + z + "," + body + "}";
    }

    private static final String CONTAINER_BODY = "\"type\":\"container_transaction\",\"container\":\"chest\","
            + "\"player\":\"RigPlayer\",\"player_uuid\":\"u-rig\","
            + "\"deposits\":[{\"id\":\"minecraft:ominous_bottle\",\"count\":8}],"
            + "\"withdrawals\":[{\"id\":\"minecraft:egg\",\"count\":1}],"
            + "\"contents_after\":[{\"id\":\"minecraft:ominous_bottle\",\"count\":8,\"bytes\":\"SECRETPAYLOAD\"}]";
    private static final String BLOCK_BODY = "\"type\":\"block_change\",\"action\":\"place\",\"block\":\"minecraft:chest\","
            + "\"before\":\"minecraft:air\",\"after\":\"minecraft:chest\",\"player\":\"RigPlayer\",\"player_uuid\":\"u-rig\"";
    private static final String DROP_BODY = "\"type\":\"item_drop\",\"player\":\"RigPlayer\",\"player_uuid\":\"u-rig\","
            + "\"item\":{\"id\":\"minecraft:diamond_sword\",\"count\":1,\"name\":\"Relic\"}";
    private static final String DEATH_BODY = "\"type\":\"death_snapshot\",\"player\":\"RigPlayer\",\"player_uuid\":\"u-rig\","
            + "\"death_message\":\"RigPlayer fell out of the world\"";
    private static final String DESTROYED_BODY = "\"type\":\"item_destroyed\",\"cause\":\"burn\",\"owner\":\"RigPlayer\","
            + "\"owner_uuid\":\"u-rig\",\"item\":{\"id\":\"minecraft:diamond\",\"count\":3}";

    @Test void inspectSummaryShapes() {
        String container = InspectReport.summarize(env(1_000, "world", -100, 64, -200, CONTAINER_BODY));
        assertNotNull(container);
        assertTrue(container.contains("container chest"), container);
        assertTrue(container.contains("by RigPlayer"), container);
        assertTrue(container.contains("deposited minecraft:ominous_bottle#8"), container);
        assertTrue(container.contains("withdrew minecraft:egg#1"), container);
        assertFalse(container.contains("SECRETPAYLOAD"), "summaries must never leak item payloads");

        String block = InspectReport.summarize(env(2_000, "world", -100, 64, -200, BLOCK_BODY));
        assertTrue(block.contains("block place minecraft:chest by RigPlayer"), block);
        assertTrue(block.contains("@ world -100,64,-200"), block);

        String drop = InspectReport.summarize(env(3_000, "world", -100, 64, -200, DROP_BODY));
        assertTrue(drop.contains("drop minecraft:diamond_sword#1 \"Relic\" by RigPlayer"), drop);

        String death = InspectReport.summarize(env(4_000, "world", -100, 64, -200, DEATH_BODY));
        assertTrue(death.contains("death — RigPlayer fell out of the world"), death);

        String destroyed = InspectReport.summarize(env(5_000, "world", -100, 64, -200, DESTROYED_BODY));
        assertTrue(destroyed.contains("destroyed (burn) minecraft:diamond#3 by RigPlayer"), destroyed);

        // not inspect-reportable: bulky/internal record types are skipped, never dumped
        assertNull(InspectReport.summarize(env(6_000, "world", 0, 0, 0,
                "\"type\":\"inventory_snapshot\",\"player\":\"RigPlayer\"")));
        assertNull(InspectReport.summarize(env(6_000, "world", 0, 0, 0, "\"type\":\"mcmmo_snapshot\"")));
    }

    @Test void inspectReportSortsLatestFirstAndLimits() {
        List<String> scanned = List.of(
                env(1_000, "world", -100, 64, -200, BLOCK_BODY),
                env(3_000, "world", -100, 64, -200, DROP_BODY),
                env(2_000, "world", -100, 64, -200, CONTAINER_BODY),
                env(4_000, "world", 0, 0, 0, "\"type\":\"inventory_snapshot\",\"player\":\"RigPlayer\""));
        InspectReport.Report r = InspectReport.build(scanned, 2);
        assertEquals(3, r.total()); // the snapshot line is not reportable
        assertEquals(2, r.lines().size());
        assertTrue(r.lines().get(0).contains("drop"), r.lines().get(0));       // ts 3000 first
        assertTrue(r.lines().get(1).contains("container"), r.lines().get(1));  // ts 2000 second
    }

    @Test void inspectParseLimit() {
        assertEquals(10, InspectReport.parseLimit(null));
        assertEquals(10, InspectReport.parseLimit(""));
        assertEquals(3, InspectReport.parseLimit("3"));
        assertEquals(1, InspectReport.parseLimit("0"));
        assertEquals(25, InspectReport.parseLimit("999"));
        assertEquals(10, InspectReport.parseLimit("abc"));
    }

    /** The exact scan the `inspect player` command runs, over real day files on disk. */
    private static InspectReport.Report scanPlayer(Ledger ledger, String who, int limit) {
        List<String> scanned = new java.util.ArrayList<>();
        for (String stream : new String[]{"containers", "items", "deaths", "blocks"}) {
            Exporter.Query q = Exporter.Query.parse(new String[]{"player=" + who});
            q.stream = stream;
            scanned.addAll(Exporter.scan(ledger, q));
        }
        return InspectReport.build(scanned, limit);
    }

    /** The exact scan the `inspect at` command runs, over real day files on disk. */
    private static InspectReport.Report scanAt(Ledger ledger, String world, long x, long y, long z, int limit) {
        List<String> scanned = new java.util.ArrayList<>();
        for (String stream : new String[]{"blocks", "containers"}) {
            Exporter.Query q = Exporter.Query.parse(new String[]{
                    "world=" + world, "at=" + x + "," + y + "," + z, "radius=0"});
            q.stream = stream;
            scanned.addAll(Exporter.scan(ledger, q));
        }
        return InspectReport.build(scanned, limit);
    }

    private Ledger seededLedger(File dir) throws Exception {
        long t0 = 1_793_770_800_000L; // 2026-11-04, morning UTC
        String day = Ledger.dayOf(t0);
        Files.writeString(new File(dir, "containers-" + day + ".jsonl").toPath(),
                env(t0 + 2_000, "world", -100, 64, -200, CONTAINER_BODY) + "\n"
                + env(t0 + 3_000, "world", 50, 70, 50, CONTAINER_BODY.replace("RigPlayer", "Marvin")) + "\n",
                StandardCharsets.UTF_8);
        Files.writeString(new File(dir, "items-" + day + ".jsonl").toPath(),
                env(t0 + 4_000, "world", -99, 64, -200, DROP_BODY) + "\n", StandardCharsets.UTF_8);
        Files.writeString(new File(dir, "deaths-" + day + ".jsonl").toPath(),
                env(t0 + 5_000, "world", -100, 60, -200, DEATH_BODY) + "\n"
                + env(t0 + 6_000, "world", -100, 60, -200, DESTROYED_BODY) + "\n", StandardCharsets.UTF_8);
        Files.writeString(new File(dir, "blocks-" + day + ".jsonl").toPath(),
                env(t0 + 1_000, "world", -100, 64, -200, BLOCK_BODY) + "\n"
                + env(t0 + 7_000, "world", -101, 64, -200, BLOCK_BODY) + "\n", StandardCharsets.UTF_8);
        return new Ledger(dir, new Settings(new YamlConfiguration()));
    }

    @Test void inspectPlayerFormFromLedgerFiles(@TempDir File dir) throws Exception {
        Ledger ledger = seededLedger(dir);
        InspectReport.Report r = scanPlayer(ledger, "RigPlayer", 10);
        assertEquals(6, r.total()); // container + drop + death + destroyed + 2 block places
        assertEquals(6, r.lines().size());
        assertTrue(r.lines().get(0).contains("block place"), r.lines().get(0)); // ts t0+7000 is latest
        assertTrue(r.lines().stream().anyMatch(l -> l.contains("container chest")), r.lines().toString());
        assertTrue(r.lines().stream().anyMatch(l -> l.contains("death — RigPlayer fell")), r.lines().toString());
        assertTrue(r.lines().stream().noneMatch(l -> l.contains("Marvin")), "another player's events must not appear");
        // not-found / empty case: a player with no records gets an empty report (the command prints "no recorded activity")
        InspectReport.Report ghost = scanPlayer(ledger, "Ghost", 10);
        assertEquals(0, ghost.total());
        assertTrue(ghost.lines().isEmpty());
    }

    @Test void inspectAtFormFromLedgerFiles(@TempDir File dir) throws Exception {
        Ledger ledger = seededLedger(dir);
        InspectReport.Report r = scanAt(ledger, "world", -100, 64, -200, 10);
        assertEquals(2, r.total()); // the chest place + the container transaction at that exact block
        assertTrue(r.lines().stream().anyMatch(l -> l.contains("container chest")), r.lines().toString());
        assertTrue(r.lines().stream().anyMatch(l -> l.contains("block place")), r.lines().toString());
        // empty case: a position with nothing recorded
        InspectReport.Report empty = scanAt(ledger, "world", 0, 0, 0, 10);
        assertEquals(0, empty.total());
        assertTrue(empty.lines().isEmpty());
    }

    @Test void ledgerEmitsEnvelope(@TempDir File dir) throws Exception {
        YamlConfiguration y = new YamlConfiguration();
        Settings cfg = new Settings(y);
        Ledger ledger = new Ledger(dir, cfg);
        ledger.emit("system", null, new Json.Obj().put("type", "boot_check"));
        ledger.flushAll();
        File[] files = dir.listFiles((d, n) -> n.startsWith("system-"));
        assertNotNull(files); assertEquals(1, files.length);
        String line = Files.readString(files[0].toPath(), StandardCharsets.UTF_8).trim();
        assertTrue(line.startsWith("{\"ts\":\""));
        assertEquals("boot_check", Json.strField(line, "type"));
        assertTrue(Json.numField(line, "ts_ms", 0) > 0);
    }
}

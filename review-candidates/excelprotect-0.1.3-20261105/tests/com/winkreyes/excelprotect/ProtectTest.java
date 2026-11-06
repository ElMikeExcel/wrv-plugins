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

    // ------------------------------------------------ 0.1.2 damage incidents

    private static DamageIncidents.Tick dmgTick(UUID p, long ms, String sourceKey, String cause,
            String kind, String attackerName, String attackerEntity, String attackerUuid,
            String projectile, int elite, double dmg, double hpBefore, double maxHp) {
        return new DamageIncidents.Tick(p, "RigPlayer", "world", 10, 64, -20, cause, sourceKey,
                kind, attackerName, attackerEntity, attackerUuid, projectile, elite,
                dmg, hpBefore, maxHp, ms);
    }

    private static DamageIncidents.Tick zombieTick(UUID p, long ms, UUID z, double dmg, double hp) {
        return dmgTick(p, ms, "entity:minecraft:zombie:" + z + "|ENTITY_ATTACK", "ENTITY_ATTACK",
                "mob", null, "minecraft:zombie", z.toString(), null, DamageIncidents.NOT_ELITE,
                dmg, hp, 20);
    }

    private static DamageIncidents.Tick lavaTick(UUID p, long ms, double dmg, double hp) {
        return dmgTick(p, ms, "env|LAVA", "LAVA", "environment", null, null, null, null,
                DamageIncidents.NOT_ELITE, dmg, hp, 20);
    }

    @Test void coalescerMergesWithinWindow() {
        DamageIncidents di = new DamageIncidents(2000, 0.30);
        UUID p = UUID.randomUUID(), z = UUID.randomUUID();
        assertTrue(di.record(zombieTick(p, 0, z, 4, 20)).isEmpty());
        assertTrue(di.record(zombieTick(p, 1000, z, 4, 16)).isEmpty());
        assertTrue(di.record(zombieTick(p, 2000, z, 4, 12)).isEmpty());
        assertEquals(1, di.openCount());
        assertTrue(di.sweep(4000).isEmpty()); // exactly at the gap edge: still open
        List<DamageIncidents.Incident> closed = di.sweep(4001);
        assertEquals(1, closed.size());
        DamageIncidents.Incident i = closed.get(0);
        assertEquals(3, i.tickCount);
        assertEquals(12.0, i.totalDamage);
        assertEquals(4.0, i.maxSingleHit);
        assertEquals(20.0, i.healthStart);
        assertEquals(8.0, i.healthEnd);
        assertEquals(8.0, i.healthMin);
        assertEquals("window", i.closed);
        assertFalse(i.died);
        assertEquals(0, di.openCount());
    }

    @Test void coalescerWindowEdgeAndSplit() {
        DamageIncidents di = new DamageIncidents(2000, 0.30);
        UUID p = UUID.randomUUID(), z = UUID.randomUUID();
        di.record(zombieTick(p, 0, z, 3, 20));
        di.record(zombieTick(p, 2000, z, 3, 17)); // gap == window: merges
        assertEquals(1, di.openCount());
        List<DamageIncidents.Incident> closed = di.record(zombieTick(p, 4001, z, 3, 14)); // gap > window: splits
        assertEquals(1, closed.size());
        assertEquals(2, closed.get(0).tickCount);
        assertEquals("window", closed.get(0).closed);
        assertEquals(1, di.openCount()); // the fresh incident is open
        List<DamageIncidents.Incident> rest = di.sweep(7000);
        assertEquals(1, rest.size());
        assertEquals(1, rest.get(0).tickCount);
    }

    @Test void coalescerSourceAndCauseSplit() {
        DamageIncidents di = new DamageIncidents(2000, 0.30);
        UUID p = UUID.randomUUID(), z = UUID.randomUUID(), s = UUID.randomUUID();
        di.record(zombieTick(p, 0, z, 3, 20));
        // a different attacker mid-exposure does NOT close the zombie incident; both run open
        di.record(dmgTick(p, 500, "entity:minecraft:skeleton:" + s + "|PROJECTILE", "PROJECTILE",
                "mob", null, "minecraft:skeleton", s.toString(), "minecraft:arrow",
                DamageIncidents.NOT_ELITE, 2, 17, 20));
        assertEquals(2, di.openCount());
        di.record(zombieTick(p, 1000, z, 3, 15)); // still merges into the zombie incident
        // same attacker, different cause = a different source
        di.record(dmgTick(p, 1200, "entity:minecraft:zombie:" + z + "|SONIC_BOOM", "SONIC_BOOM",
                "mob", null, "minecraft:zombie", z.toString(), null,
                DamageIncidents.NOT_ELITE, 5, 12, 20));
        assertEquals(3, di.openCount());
        List<DamageIncidents.Incident> closed = di.closePlayer(p, "quit", false);
        assertEquals(3, closed.size());
        assertTrue(closed.stream().allMatch(i -> "quit".equals(i.closed)));
        DamageIncidents.Incident zombie = closed.stream()
                .filter(i -> "ENTITY_ATTACK".equals(i.cause)).findFirst().orElseThrow();
        assertEquals(2, zombie.tickCount);
        assertEquals(6.0, zombie.totalDamage);
        DamageIncidents.Incident arrow = closed.stream()
                .filter(i -> "PROJECTILE".equals(i.cause)).findFirst().orElseThrow();
        assertEquals("minecraft:arrow", arrow.projectile);
        assertEquals("minecraft:skeleton", arrow.attackerEntity);
    }

    @Test void deathClosesAndFlagsLatest() {
        DamageIncidents di = new DamageIncidents(2000, 0.30);
        UUID p = UUID.randomUUID(), z = UUID.randomUUID();
        di.record(lavaTick(p, 0, 2, 20));
        di.record(lavaTick(p, 500, 2, 18));
        di.record(zombieTick(p, 800, z, 9, 3));
        List<DamageIncidents.Incident> closed = di.closePlayer(p, "death", true);
        assertEquals(2, closed.size());
        DamageIncidents.Incident killing = closed.stream()
                .filter(i -> "ENTITY_ATTACK".equals(i.cause)).findFirst().orElseThrow();
        assertTrue(killing.died);
        assertEquals(0.0, killing.healthEnd);
        assertEquals(0.0, killing.healthMin);
        assertEquals("death", killing.closed);
        DamageIncidents.Incident lava = closed.stream()
                .filter(i -> "LAVA".equals(i.cause)).findFirst().orElseThrow();
        assertFalse(lava.died);
        assertEquals("death", lava.closed);
        assertEquals(2, lava.tickCount);
        assertEquals(0, di.openCount());
    }

    @Test void nearDeathFlagMath() {
        DamageIncidents di = new DamageIncidents(2000, 0.30);
        UUID p = UUID.randomUUID(), z = UUID.randomUUID();
        di.record(zombieTick(p, 0, z, 14, 20)); // ends at 6 = exactly 30% of 20
        String line = di.sweep(3000).get(0).render(0.30).render();
        assertTrue(line.contains("\"near_death\":true"), line);
        assertTrue(line.contains("\"died\":false"), line);
        DamageIncidents di2 = new DamageIncidents(2000, 0.30);
        di2.record(zombieTick(p, 0, z, 13.5, 20)); // ends at 6.5 — above the line
        String line2 = di2.sweep(3000).get(0).render(0.30).render();
        assertTrue(line2.contains("\"near_death\":false"), line2);
        // a death is never ALSO near_death — died is the stronger signal
        DamageIncidents di3 = new DamageIncidents(2000, 0.30);
        di3.record(zombieTick(p, 0, z, 25, 20));
        String line3 = di3.closePlayer(p, "death", true).get(0).render(0.30).render();
        assertTrue(line3.contains("\"died\":true"), line3);
        assertTrue(line3.contains("\"near_death\":false"), line3);
    }

    @Test void eliteSerializationShapes() {
        UUID p = UUID.randomUUID(), z = UUID.randomUUID();
        DamageIncidents di = new DamageIncidents(2000, 0.30);
        di.record(dmgTick(p, 0, "entity:minecraft:zombie:" + z + "|ENTITY_ATTACK", "ENTITY_ATTACK",
                "mob", null, "minecraft:zombie", z.toString(), null, 17, 5, 20, 20));
        String elite = di.sweep(3000).get(0).render(0.30).render();
        assertTrue(elite.contains("\"elite\":true"), elite);
        assertTrue(elite.contains("\"elite_level\":17"), elite);
        assertTrue(elite.contains("\"attacker_kind\":\"mob\""), elite);
        assertTrue(elite.contains("\"tick_count\":1"), elite);

        DamageIncidents di2 = new DamageIncidents(2000, 0.30);
        di2.record(zombieTick(p, 0, z, 5, 20));
        String plain = di2.sweep(3000).get(0).render(0.30).render();
        assertTrue(plain.contains("\"elite\":false"), plain);
        assertFalse(plain.contains("elite_level"), plain);

        DamageIncidents di3 = new DamageIncidents(2000, 0.30);
        di3.record(dmgTick(p, 0, "entity:minecraft:zombie:" + z + "|ENTITY_ATTACK", "ENTITY_ATTACK",
                "mob", null, "minecraft:zombie", z.toString(), null,
                DamageIncidents.UNKNOWN, 5, 20, 20));
        String unknown = di3.sweep(3000).get(0).render(0.30).render();
        assertTrue(unknown.contains("\"elite_probe\":\"unavailable\""), unknown);
        assertFalse(unknown.contains("\"elite\":"), unknown);

        // environment incidents carry no elite keys at all
        DamageIncidents di4 = new DamageIncidents(2000, 0.30);
        di4.record(lavaTick(p, 0, 2, 20));
        String lava = di4.sweep(3000).get(0).render(0.30).render();
        assertTrue(lava.contains("\"attacker_kind\":\"environment\""), lava);
        assertFalse(lava.contains("elite"), lava);
        assertFalse(lava.contains("attacker_entity"), lava);
    }

    @Test void settingsDamageDefaultsAndClamps() {
        Settings def = new Settings(new YamlConfiguration());
        assertTrue(def.damageIncidents());
        assertEquals(2000, def.damageIncidentGapMs());
        assertEquals(0.30, def.nearDeathFraction());
        YamlConfiguration y = new YamlConfiguration();
        y.set("log.combat.damage-incidents", false);
        y.set("combat.damage-incident-gap-ms", 10);        // below the floor
        y.set("combat.near-death-health-fraction", 1.5);  // above the ceiling
        Settings clamped = new Settings(y);
        assertFalse(clamped.damageIncidents());
        assertEquals(250, clamped.damageIncidentGapMs());
        assertEquals(1.0, clamped.nearDeathFraction());
        YamlConfiguration y2 = new YamlConfiguration();
        y2.set("combat.damage-incident-gap-ms", 999_999);
        y2.set("combat.near-death-health-fraction", -0.5);
        Settings clamped2 = new Settings(y2);
        assertEquals(30_000, clamped2.damageIncidentGapMs());
        assertEquals(0.0, clamped2.nearDeathFraction());
    }

    @Test void damageReportForms() {
        assertEquals(24, DamageReport.parseHours(null));
        assertEquals(24, DamageReport.parseHours(""));
        assertEquals(6, DamageReport.parseHours("6"));
        assertEquals(24, DamageReport.parseHours("abc"));
        assertEquals(0.1, DamageReport.parseHours("0"));
        assertEquals(720, DamageReport.parseHours("9999"));
        assertEquals("24h", DamageReport.fmtHours(24));
        assertEquals("0.5h", DamageReport.fmtHours(0.5));

        UUID p = UUID.randomUUID(), z = UUID.randomUUID();
        DamageIncidents di = new DamageIncidents(2000, 0.30);
        di.record(dmgTick(p, 1_000, "entity:minecraft:zombie:" + z + "|ENTITY_ATTACK", "ENTITY_ATTACK",
                "mob", null, "minecraft:zombie", z.toString(), null, 17, 16.5, 20, 20));
        String eliteLine = di.sweep(5000).get(0).render(0.30).render();
        DamageIncidents di2 = new DamageIncidents(2000, 0.30);
        di2.record(lavaTick(p, 9_000, 3, 20));
        di2.record(lavaTick(p, 9_500, 3, 17));
        String lavaLine = di2.sweep(13_000).get(0).render(0.30).render();

        DamageReport.Report r = DamageReport.build(List.of(eliteLine, lavaLine,
                "{\"type\":\"combat_damage\",\"player\":\"RigPlayer\"}"), 10);
        assertEquals(2, r.total()); // the combat line is not an incident
        assertEquals(2, r.lines().size());
        assertTrue(r.lines().get(0).contains("lava"), r.lines().get(0)); // later start first
        assertTrue(r.lines().get(0).contains("6 dmg in 2 ticks"), r.lines().get(0));
        assertTrue(r.lines().get(1).contains("zombie ELITE Lv17"), r.lines().get(1));
        assertTrue(r.lines().get(1).contains("NEAR DEATH"), r.lines().get(1)); // 20 -> 3.5
        assertNull(DamageReport.summarize("{\"type\":\"combat_damage\"}"));
    }

    @Test void damageStreamRetention(@TempDir File dir) throws Exception {
        YamlConfiguration y = new YamlConfiguration();
        y.set("retention-days", 30);
        Ledger ledger = new Ledger(dir, new Settings(y));
        File old = new File(dir, "damage-2020-01-01.jsonl");
        Files.writeString(old.toPath(),
                "{\"type\":\"damage_incident\",\"player\":\"A\"}\n{\"type\":\"damage_incident\",\"player\":\"B\"}\n",
                StandardCharsets.UTF_8);
        String today = Ledger.dayOf(System.currentTimeMillis());
        File now = new File(dir, "damage-" + today + ".jsonl");
        Files.writeString(now.toPath(), "{\"type\":\"damage_incident\",\"player\":\"A\"}\n",
                StandardCharsets.UTF_8);
        ledger.housekeeping();
        assertFalse(old.exists()); // aged out like every stream
        File agg = new File(dir, "aggregates-damage.jsonl");
        assertTrue(agg.exists());
        String aggText = Files.readString(agg.toPath(), StandardCharsets.UTF_8);
        assertTrue(aggText.contains("\"day\":\"2020-01-01\""));
        assertTrue(aggText.contains("\"count\":2"));
        assertTrue(now.exists()); // today's file untouched
    }

    /** The exact scan the `damage` command runs, over real day files on disk. */
    @Test void damageQueryFormFromLedgerFiles(@TempDir File dir) throws Exception {
        long nowMs = System.currentTimeMillis();
        UUID p = UUID.randomUUID(), z = UUID.randomUUID();
        DamageIncidents di = new DamageIncidents(2000, 0.30);
        di.record(zombieTick(p, nowMs - 120_000, z, 5, 20));
        String body1 = di.sweep(nowMs - 100_000).get(0).render(0.30).render();
        DamageIncidents di2 = new DamageIncidents(2000, 0.30);
        di2.record(lavaTick(p, nowMs - 60_000, 2, 20));
        String body2 = di2.sweep(nowMs - 50_000).get(0).render(0.30).render();
        String day = Ledger.dayOf(nowMs);
        String line1 = "{\"ts\":\"" + Ledger.isoOf(nowMs - 100_000) + "\",\"ts_ms\":" + (nowMs - 100_000)
                + ",\"world\":\"world\",\"x\":10,\"y\":64,\"z\":-20," + body1.substring(1);
        String line2 = "{\"ts\":\"" + Ledger.isoOf(nowMs - 50_000) + "\",\"ts_ms\":" + (nowMs - 50_000)
                + ",\"world\":\"world\",\"x\":10,\"y\":64,\"z\":-20," + body2.substring(1);
        String marvin = line2.replace("RigPlayer", "Marvin");
        Files.writeString(new File(dir, "damage-" + day + ".jsonl").toPath(),
                line1 + "\n" + line2 + "\n" + marvin + "\n", StandardCharsets.UTF_8);
        Ledger ledger = new Ledger(dir, new Settings(new YamlConfiguration()));
        Exporter.Query q = Exporter.Query.parse(new String[]{"stream=damage", "player=RigPlayer", "hours=24"});
        List<String> scanned = Exporter.scan(ledger, q);
        DamageReport.Report r = DamageReport.build(scanned, 10);
        assertEquals(2, r.total()); // Marvin's incident is filtered out by the player filter
        assertTrue(r.lines().get(0).contains("lava"), r.lines().get(0));
        Exporter.Query qg = Exporter.Query.parse(new String[]{"stream=damage", "player=Ghost", "hours=24"});
        assertEquals(0, DamageReport.build(Exporter.scan(ledger, qg), 10).total());
    }

    // ---- damage DEALT coalescing (0.1.3) --------------------------------
    private DealtIncidents.Tick dealtTick(UUID attacker, String victimKey, String victimType,
                                          String cause, int elite, double dmg,
                                          double healthAfter, long ms) {
        return new DealtIncidents.Tick(attacker, "Mike", "world", 0, 64, 0,
                cause, "victim:" + victimKey + "|" + cause + "|hand",
                victimKey, victimType, UUID.randomUUID().toString(), null, null, null, elite,
                dmg, healthAfter, 850, ms);
    }

    @Test void dealtMergeOneVictimHealthArc() {
        UUID a = UUID.randomUUID();
        DealtIncidents di = new DealtIncidents(2000);
        String vk = "minecraft:zombie:" + UUID.randomUUID();
        assertTrue(di.record(
                dealtTick(a, vk, "minecraft:zombie", "ENTITY_ATTACK", DealtIncidents.NOT_ELITE, 8, 850, 1000)).isEmpty());
        List<DealtIncidents.Incident> flushed = di.record(
                dealtTick(a, vk, "minecraft:zombie", "ENTITY_ATTACK", DealtIncidents.NOT_ELITE, 3.5, 842, 2500));
        assertTrue(flushed.isEmpty()); // still inside the window
        List<DealtIncidents.Incident> swept = di.sweep(2500 + 2001);
        assertEquals(1, swept.size());
        String line = swept.get(0).render().render();
        assertEquals("window", swept.get(0).closed);
        assertEquals(2L, Math.round(Json.numField(line, "hit_count", 0)));
        assertEquals(11.5, Json.numField(line, "total_damage", 0));
        assertEquals(8.0, Json.numField(line, "max_single_hit", 0));
        assertEquals(850.0, Json.numField(line, "victim_health_start", 0));
        assertEquals(838.5, Json.numField(line, "victim_health_end", 0));
        assertEquals(838.5, Json.numField(line, "victim_health_min", 0));
        assertTrue(line.contains("\"elite\":false")); // probed non-elite says so explicitly
        assertFalse(line.contains("elite_level"));
        assertFalse(line.contains("\"died\":true"));
        DealtReport.Report r = DealtReport.build(List.of(line), 5);
        assertEquals(1, r.total());
        assertEquals(11.5, r.totalDamage());
        assertEquals(0.0, r.eliteDamage());
        assertTrue(r.lines().get(0).contains("zombie"));
        assertTrue(r.byVictim().get(0).contains("11.5"));
    }

    @Test void dealtSplitsOnVictimCauseAndDelivery() {
        UUID a = UUID.randomUUID();
        DealtIncidents di = new DealtIncidents(2000);
        String vk1 = "minecraft:zombie:" + UUID.randomUUID();
        String vk2 = "minecraft:zombie:" + UUID.randomUUID();
        di.record(dealtTick(a, vk1, "minecraft:zombie", "ENTITY_ATTACK", -1, 5, 850, 1000));
        di.record(dealtTick(a, vk2, "minecraft:zombie", "ENTITY_ATTACK", -1, 5, 850, 1100)); // other victim
        var arrow = new DealtIncidents.Tick(a, "Mike", "world", 0, 64, 0,
                "PROJECTILE", "victim:" + vk1 + "|PROJECTILE|proj:minecraft:arrow",
                vk1, "minecraft:zombie", UUID.randomUUID().toString(), null,
                "minecraft:arrow", null, -1, 7, 843, 850, 1200);
        di.record(arrow); // projectile delivery to the FIRST victim: still a separate incident
        assertEquals(3, di.openCount());
        List<DealtIncidents.Incident> all = di.closeAll("shutdown");
        assertEquals(3, all.size());
        String rendered = all.stream().map(i -> i.render().render())
                .filter(l -> l.contains("\"projectile\":\"minecraft:arrow\"")).findFirst().orElse("");
        assertFalse(rendered.isEmpty());
        assertTrue(rendered.contains("\"total_damage\":7"));
    }

    @Test void dealtEliteVictimSerializationShapes() {
        UUID a = UUID.randomUUID();
        DealtIncidents di = new DealtIncidents(2000);
        String vkE = "minecraft:elder_guardian:" + UUID.randomUUID();
        di.record(dealtTick(a, vkE, "minecraft:elder_guardian", "ENTITY_ATTACK", 25, 19, 850, 1000));
        String eliteLine = di.closeAll("shutdown").get(0).render().render();
        assertTrue(eliteLine.contains("\"elite\":true"));
        assertTrue(eliteLine.contains("\"elite_level\":25"));
        DealtReport.Report r = DealtReport.build(List.of(eliteLine), 5);
        assertEquals(19.0, r.eliteDamage());
        assertTrue(r.lines().get(0).contains("ELITE Lv25"));

        DealtIncidents di2 = new DealtIncidents(2000);
        String vkU = "minecraft:husk:" + UUID.randomUUID();
        di2.record(dealtTick(a, vkU, "minecraft:husk", "ENTITY_ATTACK", DealtIncidents.UNKNOWN, 4, 846, 1000));
        String unknownLine = di2.closeAll("shutdown").get(0).render().render();
        assertTrue(unknownLine.contains("\"elite_probe\":\"unavailable\""));
        assertFalse(unknownLine.contains("\"elite\":true"));
    }

    @Test void dealtVictimDeathKillingBlowAttribution() {
        UUID a = UUID.randomUUID(), b = UUID.randomUUID();
        DealtIncidents di = new DealtIncidents(2000);
        String vk = "minecraft:zombie:" + UUID.randomUUID();
        di.record(dealtTick(a, vk, "minecraft:zombie", "ENTITY_ATTACK", -1, 5, 850, 1000));
        di.record(dealtTick(b, vk, "minecraft:zombie", "ENTITY_ATTACK", -1, 3, 845, 1100));
        List<DealtIncidents.Incident> closed = di.closeVictim(vk, b);
        assertEquals(2, closed.size());
        int died = 0;
        for (DealtIncidents.Incident inc : closed) {
            assertEquals("victim_death", inc.closed);
            if (inc.died) died++;
            if (inc.attackerUuid.equals(b)) {
                String line = inc.render().render();
                assertEquals(0.0, Json.numField(line, "victim_health_end", -1));
                assertEquals(0.0, Json.numField(line, "victim_health_min", -1));
                assertTrue(line.contains("\"died\":true"));
                assertTrue(DealtReport.summarize(line).contains("DIED"));
            }
        }
        assertEquals(1, died); // exactly the killer's incident
    }

    @Test void dealtSpellDeliveryRecorded() {
        UUID a = UUID.randomUUID();
        DealtIncidents di = new DealtIncidents(2000);
        String vk = "minecraft:spider:" + UUID.randomUUID();
        var tick = new DealtIncidents.Tick(a, "Mike", "world", 0, 64, 0,
                "ENTITY_ATTACK", "victim:" + vk + "|ENTITY_ATTACK|spell:fireball",
                vk, "minecraft:spider", UUID.randomUUID().toString(), null,
                null, "fireball", -1, 12, 850, 850, 1000);
        di.record(tick);
        String line = di.closeAll("shutdown").get(0).render().render();
        assertTrue(line.contains("\"spell\":\"fireball\""));
        assertTrue(DealtReport.summarize(line).contains("[spell: fireball]"));
    }

    // ---- HEALING coalescing (0.1.3) --------------------------------------
    private HealingIncidents.Tick healTick(UUID target, String targetName, UUID healer, String healerName,
                                           boolean self, String source, String reason, String spell,
                                           double amount, double healthBefore, long ms) {
        String sourceKey = (healer == null ? "unknown" : healer.toString())
                + "|" + (spell != null ? "SPELL:" + spell : reason);
        return new HealingIncidents.Tick(target, targetName, "world", 0, 64, 0, sourceKey,
                healer, healerName, self, source, reason, spell, amount, healthBefore, 20, ms);
    }

    @Test void healingSelfMergeWithCapDelta() {
        UUID t = UUID.randomUUID();
        HealingIncidents hi = new HealingIncidents(2000);
        assertTrue(hi.record(
                healTick(t, "Kailey", t, "Kailey", true, "REGEN", "REGEN", null, 4, 19.5, 1000)).isEmpty());
        hi.record(healTick(t, "Kailey", t, "Kailey", true, "REGEN", "REGEN", null, 4, 19.5, 1500));
        List<HealingIncidents.Incident> swept = hi.sweep(1500 + 2001);
        assertEquals(1, swept.size());
        String line = swept.get(0).render().render();
        assertEquals(2L, Math.round(Json.numField(line, "event_count", 0)));
        assertEquals(8.0, Json.numField(line, "total_healed", 0));   // event amounts
        assertEquals(0.5, Json.numField(line, "total_effective", 0)); // capped at 20
        assertEquals(19.5, Json.numField(line, "health_start", 0));
        assertEquals(20.0, Json.numField(line, "health_end", 0));
        assertTrue(line.contains("\"self\":true"));
        HealingReport.Report r = HealingReport.build(List.of(line), "Kailey", 5);
        assertEquals(8.0, r.selfHealed());
        assertEquals(0.5, r.selfEffective());
        assertTrue(r.lines().get(0).contains("self REGEN"));
    }

    @Test void healingOtherPlayerMergeAndDirections() {
        UUID healer = UUID.randomUUID(), target = UUID.randomUUID();
        HealingIncidents hi = new HealingIncidents(2000);
        hi.record(healTick(target, "Rylie", healer, "Mike", false, "SPELL", "CUSTOM", "heal", 6, 10, 1000));
        hi.record(healTick(target, "Rylie", healer, "Mike", false, "SPELL", "CUSTOM", "heal", 6, 16, 1600));
        String line = hi.closeAll("shutdown").get(0).render().render();
        assertEquals(12.0, Json.numField(line, "total_healed", 0));
        assertEquals(10.0, Json.numField(line, "total_effective", 0)); // 10 -> 20 caps the second event
        assertTrue(line.contains("\"spell\":\"heal\""));
        assertTrue(line.contains("\"reasons\":[\"CUSTOM\"]"));
        HealingReport.Report done = HealingReport.build(List.of(line), "Mike", 5);
        assertEquals(12.0, done.doneHealed());
        assertTrue(done.lines().get(0).contains("→ Rylie"));
        HealingReport.Report recv = HealingReport.build(List.of(line), "Rylie", 5);
        assertEquals(12.0, recv.receivedHealed());
        assertTrue(recv.lines().get(0).contains("← Mike"));
    }

    @Test void healingSplitsOnHealerAndSourceAndUnattributed() {
        UUID t = UUID.randomUUID(), h1 = UUID.randomUUID(), h2 = UUID.randomUUID();
        HealingIncidents hi = new HealingIncidents(2000);
        hi.record(healTick(t, "Rylie", null, null, false, "CUSTOM", "CUSTOM", null, 3, 10, 1000)); // unattributed
        hi.record(healTick(t, "Rylie", h1, "Mike", false, "SPELL", "CUSTOM", "heal", 3, 13, 1100));
        hi.record(healTick(t, "Rylie", h2, "Kay", false, "SPELL", "CUSTOM", "heal", 3, 16, 1200));
        hi.record(healTick(t, "Rylie", t, "Rylie", true, "EATING", "EATING", null, 2, 19, 1300));
        assertEquals(4, hi.openCount());
        List<HealingIncidents.Incident> all = hi.closeAll("shutdown");
        HealingReport.Report r = HealingReport.build(
                all.stream().map(i -> i.render().render()).toList(), "Rylie", 10);
        assertEquals(3.0, r.receivedUnknown());
        assertEquals(6.0, r.receivedHealed());
        assertEquals(2.0, r.selfHealed());
    }

    // ---- 0.1.3 wiring: toggles, peek, query forms -------------------------
    @Test void newStreamTogglesDefaultOnAndHonorOff() {
        Settings on = new Settings(new YamlConfiguration());
        assertTrue(on.damageDealt());
        assertTrue(on.healingIncidents());
        YamlConfiguration y = new YamlConfiguration();
        y.set("log.combat.damage-dealt", false);
        y.set("log.combat.healing-incidents", false);
        Settings off = new Settings(y);
        assertFalse(off.damageDealt());
        assertFalse(off.healingIncidents());
        assertTrue(Exporter.STREAMS.contains("dealt"));
        assertTrue(Exporter.STREAMS.contains("healing"));
    }

    @Test void peekClaimDoesNotConsume() throws Exception {
        CombatCore core = new CombatCore(null, null, new Settings(new YamlConfiguration()));
        UUID caster = UUID.randomUUID();
        assertNull(core.peekClaim(caster));
        // Seed a claim the way onCast does (the claims map is private; reflect in).
        var claimsField = CombatCore.class.getDeclaredField("claims");
        claimsField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<UUID, Object> claims = (Map<UUID, Object>) claimsField.get(core);
        Class<?> claimCls = Class.forName("com.winkreyes.excelprotect.CombatCore$Claim");
        var claimCtor = claimCls.getDeclaredConstructor(String.class, long.class);
        claimCtor.setAccessible(true);
        claims.put(caster, claimCtor.newInstance("heal", System.currentTimeMillis()));
        assertEquals("heal", core.peekClaim(caster));
        assertEquals("heal", core.peekClaim(caster)); // still there after two peeks
        assertEquals("heal", core.consumeClaim(caster)); // consume works after peeks
        assertNull(core.peekClaim(caster));
    }

    @Test void dealtAndHealingQueryFormsFromLedgerFiles() throws Exception {
        java.nio.file.Path dir = Files.createTempDirectory("protect13-query");
        long nowMs = System.currentTimeMillis();
        String day = Ledger.dayOf(nowMs);
        UUID a = UUID.randomUUID();
        DealtIncidents di = new DealtIncidents(2000);
        String vk = "minecraft:zombie:" + UUID.randomUUID();
        di.record(dealtTick(a, vk, "minecraft:zombie", "ENTITY_ATTACK", 25, 19, 850, 1000));
        String dealtBody = di.closeAll("shutdown").get(0).render().render();
        String dealtLine = "{\"ts\":\"" + Ledger.isoOf(nowMs - 1000) + "\",\"ts_ms\":" + (nowMs - 1000)
                + ",\"world\":\"world\",\"x\":0,\"y\":64,\"z\":0," + dealtBody.substring(1);
        Files.writeString(dir.resolve("dealt-" + day + ".jsonl"), dealtLine + "\n", StandardCharsets.UTF_8);
        HealingIncidents hi = new HealingIncidents(2000);
        hi.record(healTick(a, "Mike", a, "Mike", true, "REGEN", "REGEN", null, 2, 18, 1000));
        String healBody = hi.closeAll("shutdown").get(0).render().render();
        String healLine = "{\"ts\":\"" + Ledger.isoOf(nowMs - 1000) + "\",\"ts_ms\":" + (nowMs - 1000)
                + ",\"world\":\"world\",\"x\":0,\"y\":64,\"z\":0," + healBody.substring(1);
        Files.writeString(dir.resolve("healing-" + day + ".jsonl"), healLine + "\n", StandardCharsets.UTF_8);
        Ledger ledger = new Ledger(dir.toFile(), new Settings(new YamlConfiguration()));
        Exporter.Query dq = Exporter.Query.parse(new String[]{"stream=dealt", "player=Mike", "hours=24"});
        assertEquals(1, Exporter.scan(ledger, dq).stream()
                .filter(l -> "damage_dealt_incident".equals(Json.strField(l, "type"))).count());
        Exporter.Query hq = Exporter.Query.parse(new String[]{"stream=healing", "player=Mike", "hours=24"});
        assertEquals(1, Exporter.scan(ledger, hq).stream()
                .filter(l -> "healing_incident".equals(Json.strField(l, "type"))).count());
    }
}

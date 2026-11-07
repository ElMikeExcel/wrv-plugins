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

        // 0.1.4 SUPERSESSION (deliberate): inspect covers ALL streams now, so the
        // snapshot types ARE reportable — but only as compact count lines; the
        // bulky fingerprint/skill payloads are never dumped (the 0.1.1 rule's
        // spirit: askable, not dumped). Unknown types stay unreportable.
        String invSnap = InspectReport.summarize(env(6_000, "world", 0, 0, 0,
                "\"type\":\"inventory_snapshot\",\"player\":\"RigPlayer\",\"reason\":\"join\",\"items\":[]"));
        assertNotNull(invSnap);
        assertTrue(invSnap.contains("inventory snapshot (join) — 0 item(s)"), invSnap);
        String mmSnap = InspectReport.summarize(env(6_000, "world", 0, 0, 0,
                "\"type\":\"mcmmo_snapshot\",\"player\":\"RigPlayer\",\"reason\":\"hourly\",\"power_level\":123"));
        assertNotNull(mmSnap);
        assertTrue(mmSnap.contains("mcMMO snapshot (hourly) — power level 123"), mmSnap);
        assertNull(InspectReport.summarize(env(6_000, "world", 0, 0, 0, "\"type\":\"some_future_internal\"")));
    }

    @Test void inspectReportSortsLatestFirstAndLimits() {
        List<String> scanned = List.of(
                env(1_000, "world", -100, 64, -200, BLOCK_BODY),
                env(3_000, "world", -100, 64, -200, DROP_BODY),
                env(2_000, "world", -100, 64, -200, CONTAINER_BODY),
                env(4_000, "world", 0, 0, 0, "\"type\":\"inventory_snapshot\",\"player\":\"RigPlayer\""));
        InspectReport.Report r = InspectReport.build(scanned, 2);
        assertEquals(4, r.total()); // 0.1.4: the snapshot line IS reportable now (compact count line)
        assertEquals(2, r.lines().size());
        assertTrue(r.lines().get(0).contains("inventory snapshot"), r.lines().get(0)); // ts 4000 first
        assertTrue(r.lines().get(1).contains("drop"), r.lines().get(1));       // ts 3000 second
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
        // 0.1.5 SUPERSESSION (deliberate): fresh day files now open with
        // the provenance file_header record (fingerprint bake list); the
        // emitted envelope is the SECOND line.
        List<String> lines = Files.readAllLines(files[0].toPath(), StandardCharsets.UTF_8);
        assertEquals(2, lines.size());
        assertEquals("file_header", Json.strField(lines.get(0), "type"));
        String line = lines.get(1);
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

    // ------------------------------------------------------------ 0.1.4 "Where"

    private static String locSample(long ts, String world, double x, double y, double z) {
        return new Json.Obj().put("ts", Ledger.isoOf(ts)).put("ts_ms", ts)
                .put("world", world).put("x", x).put("y", y).put("z", z)
                .put("type", "location_sample").put("player", "Mike")
                .put("player_uuid", "u-mike").put("gamemode", "survival")
                .put("health", 20.0).put("food", 20).render();
    }

    private static String locAnchor(long ts, String kind, String world, double x, double y, double z) {
        return new Json.Obj().put("ts", Ledger.isoOf(ts)).put("ts_ms", ts)
                .put("world", world).put("x", x).put("y", y).put("z", z)
                .put("type", "location_anchor").put("anchor", kind).put("player", "Mike")
                .put("player_uuid", "u-mike").put("gamemode", "survival").render();
    }

    private static String sessionLine(long ts, String type, String player) {
        return new Json.Obj().put("ts", Ledger.isoOf(ts)).put("ts_ms", ts)
                .put("world", "world").put("x", 0).put("y", 64).put("z", 0)
                .put("type", type).put("player", player)
                .put("player_uuid", "u-" + player.toLowerCase()).render();
    }

    @Test void locationSampleSchedulingMath() {
        assertEquals(1200, LocationWatch.periodTicks(60));
        assertEquals(100, LocationWatch.periodTicks(5));
        assertEquals(14_400, LocationWatch.samplesPerDay(10, 60)); // the coverage-map ceiling
        assertEquals(24, LocationWatch.samplesPerDay(1, 3600));
        assertEquals(2_880, LocationWatch.samplesPerDay(2, 60));
    }

    @Test void locationsToggleDefaultOnAndSampleSecondsClamps() {
        Settings on = new Settings(new YamlConfiguration());
        assertTrue(on.locations());                       // default ON (charter)
        assertEquals(60, on.locationsSampleSeconds());    // settled default
        assertTrue(Exporter.STREAMS.contains("locations"));
        YamlConfiguration y = new YamlConfiguration();
        y.set("log.locations", false);
        assertFalse(new Settings(y).locations());         // OFF honored -> watch never built -> no records
        YamlConfiguration lo = new YamlConfiguration();
        lo.set("locations.sample-seconds", 1);
        assertEquals(5, new Settings(lo).locationsSampleSeconds());
        YamlConfiguration hi = new YamlConfiguration();
        hi.set("locations.sample-seconds", 99_999);
        assertEquals(3600, new Settings(hi).locationsSampleSeconds());
    }

    @Test void locationAnchorSummaryShapes() {
        String tp = new Json.Obj().put("ts", Ledger.isoOf(1_000_000L)).put("ts_ms", 1_000_000L)
                .put("world", "world").put("x", 100).put("y", 64).put("z", -200)
                .put("type", "location_anchor").put("anchor", "teleport").put("cause", "plugin")
                .put("to_world", "world_nether").put("to_x", 12).put("to_y", 70).put("to_z", 40)
                .put("player", "Mike").render();
        String s = WhereReport.summarizeAnchor(tp);
        assertNotNull(s);
        assertTrue(s.contains("anchor teleport (plugin)"), s);
        assertTrue(s.contains("world_nether 12,70,40"), s);
        String gm = new Json.Obj().put("ts", Ledger.isoOf(1_060_000L)).put("ts_ms", 1_060_000L)
                .put("world", "world").put("x", 1).put("y", 64).put("z", 1)
                .put("type", "location_anchor").put("anchor", "gamemode")
                .put("gamemode_from", "survival").put("gamemode_to", "creative")
                .put("player", "Mike").render();
        assertTrue(WhereReport.summarizeAnchor(gm).contains("gamemode survival→creative"));
        assertNull(WhereReport.summarizeAnchor(locSample(1_000_000L, "world", 0, 64, 0))); // samples are not anchors
    }

    @Test void whereRouteOrderingAnchorsBeforeSamplesAtEqualTs() {
        long t0 = 1_700_000_000_000L;
        // fed in scrambled order; the anchor and a sample share t0 — anchor must lead
        List<String> lines = List.of(
                locSample(t0 + 60_000, "world", 3, 64, 0),
                locSample(t0, "world", 0, 64, 0),
                locAnchor(t0, "join", "world", 0, 64, 0));
        WhereReport.Report r = WhereReport.build(lines, 15);
        assertEquals(3, r.records());
        assertEquals(1, r.anchors());
        assertEquals(2, r.samples());
        assertTrue(r.lines().get(0).contains("start join"), r.lines().toString());
    }

    @Test void whereRouteRendersAnchorsAndTravelOnly() {
        long t0 = 1_700_000_000_000L;
        String tp = new Json.Obj().put("ts", Ledger.isoOf(t0 + 180_000)).put("ts_ms", t0 + 180_000)
                .put("world", "world").put("x", 100).put("y", 64).put("z", 0)
                .put("type", "location_anchor").put("anchor", "teleport").put("cause", "command")
                .put("to_world", "world_nether").put("to_x", 10).put("to_y", 70).put("to_z", 10)
                .put("player", "Mike").put("player_uuid", "u-mike").render();
        List<String> lines = List.of(
                locAnchor(t0, "join", "world", 0, 64, 0),
                locSample(t0 + 60_000, "world", 5, 64, 0),          // 5 blocks: no step
                locSample(t0 + 120_000, "world", 100, 64, 0),       // 100 blocks: travel step
                tp,                                                  // anchor: always a step
                locSample(t0 + 240_000, "world_nether", 12, 70, 10)); // arrival sample
        WhereReport.Report r = WhereReport.build(lines, 15);
        assertEquals(5, r.records());
        assertEquals(0, r.hiddenSteps());
        String route = String.join("\n", r.lines());
        assertTrue(route.contains("start join"), route);
        assertTrue(route.contains("moved @ world 100,64,0"), route);
        assertTrue(route.contains("teleport (command) → world_nether 10,70,10"), route);
        assertTrue(r.lines().get(r.lines().size() - 1).contains("world_nether 12,70,10"), route);
        // samples-only timeline: the last dot is tagged latest
        WhereReport.Report s = WhereReport.build(List.of(
                locSample(t0, "world", 0, 64, 0),
                locSample(t0 + 60_000, "world", 4, 64, 0)), 15);
        assertEquals(2, s.lines().size());
        assertTrue(s.lines().get(1).contains("latest"), s.lines().toString());
    }

    @Test void whereRouteCapKeepsStartAndLatest() {
        long t0 = 1_700_000_000_000L;
        List<String> lines = new java.util.ArrayList<>();
        lines.add(locAnchor(t0, "join", "world", 0, 64, 0));
        for (int i = 1; i <= 10; i++)
            lines.add(locSample(t0 + i * 60_000L, "world", i * 100, 64, 0)); // every sample travels
        WhereReport.Report r = WhereReport.build(lines, 5);
        assertEquals(11, r.steps());
        assertEquals(5, r.lines().size());
        assertEquals(6, r.hiddenSteps());
        assertTrue(r.lines().get(0).contains("start join"));
        assertTrue(r.lines().get(4).contains("world 1000,64,0"), r.lines().toString());
    }

    @Test void playtimePairsArcsAndClampsToWindow() {
        long t0 = 1_700_000_000_000L;
        List<String> lines = List.of(
                sessionLine(t0 - 3_600_000, "player_join", "Mike"),   // arc starts 1h before window
                sessionLine(t0 + 3_600_000, "player_leave", "Mike"),  // ends 1h into window
                sessionLine(t0 + 7_200_000, "player_join", "Mike"),
                sessionLine(t0 + 10_800_000, "player_leave", "Mike")); // full 1h inside window
        PlaytimeReport.Report r = PlaytimeReport.build(lines, "Mike", t0, t0 + 14_400_000, -1);
        assertEquals(7_200_000, r.totalMs()); // 1h clamped + 1h full
        assertEquals(2, r.sessions());
        assertEquals(3_600_000, r.longestMs());
        assertFalse(r.openNow());
    }

    @Test void playtimeOpenArcClosesAtNowOnlyWhenTold() {
        long t0 = 1_700_000_000_000L;
        List<String> lines = List.of(sessionLine(t0, "player_join", "Mike"));
        PlaytimeReport.Report offline = PlaytimeReport.build(lines, "Mike", t0, t0 + 7_200_000, -1);
        assertEquals(0, offline.sessions());       // open arc dropped: not known to be still online
        assertEquals(0, offline.totalMs());
        PlaytimeReport.Report online = PlaytimeReport.build(lines, "Mike", t0, t0 + 7_200_000, t0 + 7_200_000);
        assertEquals(1, online.sessions());
        assertEquals(7_200_000, online.totalMs());
        assertTrue(online.openNow());
        assertEquals(t0, online.openSinceMs());
        assertEquals(7_200_000, online.currentMs());
    }

    @Test void playtimeMissingLeaveAndOrphans() {
        long t0 = 1_700_000_000_000L;
        List<String> lines = List.of(
                sessionLine(t0 - 60_000, "player_leave", "Mike"),     // orphan leave: ignored
                sessionLine(t0, "player_join", "Mike"),
                sessionLine(t0 + 1_800_000, "player_join", "Mike"),   // missing leave: arc closes here
                sessionLine(t0 + 3_600_000, "player_leave", "Mike"),
                sessionLine(t0 + 600_000, "player_join", "Kay"),      // someone else's arc
                sessionLine(t0 + 900_000, "player_leave", "Kay"));
        PlaytimeReport.Report r = PlaytimeReport.build(lines, "Mike", t0, t0 + 7_200_000, -1);
        assertEquals(3_600_000, r.totalMs()); // 30m + 30m, Kay untouched
        assertEquals(2, r.sessions());
    }

    @Test void playtimeParseDaysAndFmtDuration() {
        assertEquals(1, PlaytimeReport.parseDays(null));
        assertEquals(1, PlaytimeReport.parseDays("abc"));
        assertEquals(7, PlaytimeReport.parseDays("7"));
        assertEquals(1, PlaytimeReport.parseDays("0"));
        assertEquals(30, PlaytimeReport.parseDays("99"));
        assertEquals("45s", PlaytimeReport.fmtDuration(45_000));
        assertEquals("12m", PlaytimeReport.fmtDuration(720_000));
        assertEquals("3h 12m", PlaytimeReport.fmtDuration(11_520_000));
        assertEquals("2d 4h", PlaytimeReport.fmtDuration(187_200_000));
    }

    @Test void inspectSectionsCoverAllStreamsWithMoreTail() {
        long t0 = 1_700_000_000_000L;
        String iso = Ledger.isoOf(t0);
        Map<String, List<String>> by = new java.util.LinkedHashMap<>();
        by.put("players", List.of(sessionLine(t0, "player_join", "Mike")));
        by.put("locations", List.of(
                locAnchor(t0 + 1000, "join", "world", 0, 64, 0),
                locSample(t0 + 2000, "world", 1, 64, 0),
                locSample(t0 + 3000, "world", 2, 64, 0)));
        by.put("combat", List.of(
                new Json.Obj().put("ts_ms", t0).put("world", "world").put("x", 0).put("y", 64).put("z", 0)
                        .put("type", "combat_damage").put("victim", "minecraft:zombie")
                        .put("attacker", "Mike").put("amount", 6.0).put("cause", "ENTITY_ATTACK").render(),
                new Json.Obj().put("ts_ms", t0 + 500).put("type", "fight_summary")
                        .put("encounter", "fight-1").put("total_damage", 42.0)
                        .put("players", new Json.Arr().add(new Json.Obj().put("player", "Mike")
                                .put("damage_dealt", 42.0))).render()));
        by.put("damage", List.of(new Json.Obj().put("ts_ms", t0).put("start_ts", iso).put("start_ts_ms", t0)
                .put("type", "damage_incident").put("attacker_kind", "mob").put("attacker_entity", "minecraft:zombie")
                .put("cause", "ENTITY_ATTACK").put("total_damage", 9.0).put("tick_count", 3)
                .put("health_start", 20.0).put("health_end", 11.0).render()));
        by.put("dealt", List.of(new Json.Obj().put("ts_ms", t0).put("start_ts", iso).put("start_ts_ms", t0)
                .put("type", "damage_dealt_incident").put("victim", "minecraft:zombie")
                .put("total_damage", 18.0).put("hit_count", 3)
                .put("victim_health_start", 200.0).put("victim_health_end", 182.0).render()));
        by.put("healing", List.of(new Json.Obj().put("ts_ms", t0).put("start_ts", iso).put("start_ts_ms", t0)
                .put("type", "healing_incident").put("target", "Mike").put("self", true)
                .put("source", "REGEN").put("total_healed", 6.0).put("total_effective", 4.5)
                .put("event_count", 3).put("health_start", 10.0).put("health_end", 14.5).render()));
        List<InspectReport.Section> secs = InspectReport.buildSections(by, "Mike", 1);
        List<String> order = secs.stream().map(InspectReport.Section::stream).toList();
        assertEquals(List.of("players", "locations", "combat", "damage", "dealt", "healing"), order);
        InspectReport.Section combat = secs.get(2);
        assertEquals(2, combat.total());
        assertEquals(1, combat.shown());
        assertEquals(1, combat.more()); // the "…and N more" tail data
        InspectReport.Section loc = secs.get(1);
        assertEquals(1, loc.total());   // anchors count; samples do not itemize
        assertEquals(2, loc.samples());
        assertTrue(loc.lines().stream().anyMatch(l -> l.contains("location sample(s)")), loc.lines().toString());
        for (InspectReport.Section sec : secs)
            assertFalse(sec.lines().isEmpty(), sec.stream());
    }

    @Test void locationsQueryFormFromLedgerFiles(@TempDir File dir) throws Exception {
        long nowMs = System.currentTimeMillis();
        String day = Ledger.dayOf(nowMs);
        List<String> lines = List.of(
                locAnchor(nowMs - 120_000, "join", "world", 0, 64, 0),
                locSample(nowMs - 60_000, "world", 50, 64, 0),
                locSample(nowMs - 1_000, "world", 55, 64, 0));
        Files.writeString(dir.toPath().resolve("locations-" + day + ".jsonl"),
                String.join("\n", lines) + "\n", StandardCharsets.UTF_8);
        Ledger ledger = new Ledger(dir, new Settings(new YamlConfiguration()));
        Exporter.Query q = Exporter.Query.parse(new String[]{"stream=locations", "player=Mike", "hours=1"});
        List<String> scanned = Exporter.scan(ledger, q);
        assertEquals(3, scanned.size());
        WhereReport.Report r = WhereReport.build(scanned, 15);
        assertEquals(3, r.records());
        assertEquals(1, r.anchors());
        assertTrue(r.lines().get(0).contains("start join"), r.lines().toString());
        // playtime reads the players stream the same way (join/leave arcs already kept)
        Files.writeString(dir.toPath().resolve("players-" + day + ".jsonl"),
                sessionLine(nowMs - 3_600_000, "player_join", "Mike") + "\n"
                        + sessionLine(nowMs - 1_800_000, "player_leave", "Mike") + "\n",
                StandardCharsets.UTF_8);
        Exporter.Query pq = Exporter.Query.parse(new String[]{
                "stream=players", "type=player_join,player_leave", "player=Mike"});
        PlaytimeReport.Report pr = PlaytimeReport.build(
                Exporter.scan(ledger, pq), "Mike", nowMs - 7_200_000, nowMs, -1);
        assertEquals(1_800_000, pr.totalMs());
        assertEquals(1, pr.sessions());
    }

    // ------------------------------------------------------------ 0.1.5 "Clarity"

    private static Chains.Sig bookSig() {
        return new Chains.Sig("minecraft:enchanted_book", Map.of("minecraft:mending", 1), null);
    }

    private static String bookJson() {
        return "{\"id\":\"minecraft:enchanted_book\",\"count\":1,\"enchants\":{\"minecraft:mending\":1}}";
    }

    @Test void deliveryClassification() {
        assertEquals(Chains.Delivered.INVENTORY, Chains.classify(true, false));
        assertEquals(Chains.Delivered.INVENTORY, Chains.classify(true, true)); // inventory wins
        assertEquals(Chains.Delivered.GROUND, Chains.classify(false, true));
        assertEquals(Chains.Delivered.UNKNOWN, Chains.classify(false, false));
    }

    @Test void sigFromItemJsonParsesEnchants() {
        Chains.Sig sig = Chains.fromItemJson(bookJson());
        assertNotNull(sig);
        assertEquals(bookSig(), sig);
        assertEquals(bookSig().key(), sig.key());
        assertNull(Chains.fromItemJson(null));
        assertNull(Chains.fromItemJson("{\"count\":2}"));
        Chains.Sig named = Chains.fromItemJson("{\"id\":\"minecraft:diamond_sword\",\"count\":1,\"name\":\"Relic\"}");
        assertEquals("Relic", named.name());
    }

    @Test void chainMintLinkAndClose() {
        Chains ch = new Chains(new Settings(new YamlConfiguration()));
        UUID p = UUID.randomUUID();
        long t = 1_700_000_000_000L;
        String c1 = ch.mint(p, bookSig(), t);
        assertNotNull(c1);
        assertTrue(c1.startsWith("ch-"), c1);
        // the Nov 5 order: drop lands before the next-tick delivery note
        String[] m = ch.match(p, bookSig(), "drop", t + 1);
        assertNotNull(m); assertEquals(c1, m[0]); assertEquals("exact", m[1]);
        ch.noteDelivery(c1, Chains.Delivered.GROUND, t + 50); // GROUND stays GROUND
        m = ch.match(p, bookSig(), "pickup", t + 540_000);
        assertNotNull(m); assertEquals(c1, m[0]); assertEquals("exact", m[1]);
        m = ch.match(p, bookSig(), "deposit", t + 600_000);
        assertNotNull(m); assertEquals(c1, m[0]);
        m = ch.match(p, bookSig(), "withdraw", t + 700_000);
        assertNotNull(m); assertEquals(c1, m[0]);
        m = ch.match(p, bookSig(), "drop", t + 800_000);
        assertNotNull(m);
        m = ch.match(p, bookSig(), "despawn", t + 900_000);
        assertNotNull(m); assertEquals(c1, m[0]);
        assertNull(ch.match(p, bookSig(), "pickup", t + 950_000)); // closed is closed
        // inventory-delivered chain: drop still resolves, from HELD
        String c2 = ch.mint(p, bookSig(), t + 1_000_000);
        ch.noteDelivery(c2, Chains.Delivered.INVENTORY, t + 1_000_050);
        m = ch.match(p, bookSig(), "drop", t + 1_100_000);
        assertNotNull(m); assertEquals(c2, m[0]); assertEquals("exact", m[1]);
        // another player's events never match
        assertNull(ch.match(UUID.randomUUID(), bookSig(), "pickup", t + 1_200_000));
    }

    @Test void chainAmbiguousDegradesToProbable() {
        Chains ch = new Chains(new Settings(new YamlConfiguration()));
        UUID p = UUID.randomUUID();
        long t = 1_700_000_000_000L;
        String a = ch.mint(p, bookSig(), t);
        String b = ch.mint(p, bookSig(), t + 60_000);
        // first drop: both chains OPEN -> probable, oldest (a) chosen
        String[] m = ch.match(p, bookSig(), "drop", t + 70_000);
        assertNotNull(m); assertEquals(a, m[0]); assertEquals("probable", m[1]);
        // second drop: only b still OPEN -> exact
        m = ch.match(p, bookSig(), "drop", t + 80_000);
        assertNotNull(m); assertEquals(b, m[0]); assertEquals("exact", m[1]);
        // pickup: both GROUND -> probable again, oldest first
        m = ch.match(p, bookSig(), "pickup", t + 90_000);
        assertNotNull(m); assertEquals(a, m[0]); assertEquals("probable", m[1]);
        // despawn: only b still GROUND -> exact
        m = ch.match(p, bookSig(), "despawn", t + 100_000);
        assertNotNull(m); assertEquals(b, m[0]); assertEquals("exact", m[1]);
    }

    @Test void chainExpiryStopsMatching() {
        YamlConfiguration y = new YamlConfiguration();
        y.set("clarity.chain-expiry-seconds", 60);
        Chains ch = new Chains(new Settings(y));
        UUID p = UUID.randomUUID();
        long t = 1_700_000_000_000L;
        assertNotNull(ch.mint(p, bookSig(), t));
        assertNull(ch.match(p, bookSig(), "drop", t + 61_000)); // past the 60 s expiry
        assertEquals(0, ch.openCount());
    }

    @Test void itemChainQueryAssemblesMendingScenario() {
        long t0 = 1_793_932_212_000L; // 2026-11-05 ~20:43 CST in spirit; only order matters
        String who = ".coolgirlkail", uuid = "00000000-0000-0000-0009-01f40ccd59de";
        String trade = "\"type\":\"villager_trade\",\"player\":\"" + who + "\",\"player_uuid\":\"" + uuid
                + "\",\"item\":" + bookJson() + ",\"price\":{\"id\":\"minecraft:emerald\",\"count\":14}";
        String trade1 = env(t0, "world", -1266, 68, -247, trade + ",\"chain\":\"ch-20261105-0001\"");
        String del1 = env(t0 + 60, "world", -1266, 68, -247,
                "\"type\":\"item_delivery\",\"player\":\"" + who + "\",\"player_uuid\":\"" + uuid
                + "\",\"chain\":\"ch-20261105-0001\",\"source\":\"villager_trade\",\"delivered\":\"ground\",\"item\":" + bookJson());
        String drop = env(t0 + 63, "world", -1266, 69, -247,
                "\"type\":\"item_drop\",\"player\":\"" + who + "\",\"player_uuid\":\"" + uuid
                + "\",\"item\":" + bookJson() + ",\"chain\":\"ch-20261105-0001\",\"link\":\"exact\"");
        String trade2 = env(t0 + 256_000, "world", -1267, 68, -248, trade + ",\"chain\":\"ch-20261105-0002\"");
        String del2 = env(t0 + 256_060, "world", -1267, 68, -248,
                "\"type\":\"item_delivery\",\"player\":\"" + who + "\",\"player_uuid\":\"" + uuid
                + "\",\"chain\":\"ch-20261105-0002\",\"source\":\"villager_trade\",\"delivered\":\"inventory\",\"item\":" + bookJson());
        String pickup = env(t0 + 547_000, "world", -1269, 68, -248,
                "\"type\":\"item_pickup\",\"player\":\"" + who + "\",\"player_uuid\":\"" + uuid
                + "\",\"item\":" + bookJson() + ",\"chain\":\"ch-20261105-0001\",\"link\":\"exact\",\"remaining\":0");
        ItemChainReport.Report r = ItemChainReport.build(
                List.of(trade2, pickup, del1, trade1, drop, del2), who, "mending", t0 - 1000, t0 + 600_000);
        assertEquals(4, r.total()); // 2 trades (deliveries folded) + drop + pickup
        assertEquals(2, r.chains());
        assertTrue(r.lines().get(0).contains("TRADE villager"), r.lines().toString());
        assertTrue(r.lines().get(0).contains("paid minecraft:emerald#14"), r.lines().get(0));
        assertTrue(r.lines().get(0).contains("delivered GROUND"), r.lines().get(0));
        assertTrue(r.lines().get(1).contains("DROP"), r.lines().get(1));
        assertTrue(r.lines().get(2).contains("delivered INVENTORY"), r.lines().get(2));
        assertTrue(r.lines().get(3).contains("PICKUP"), r.lines().get(3));
        assertTrue(r.lines().get(3).contains("chain in inventory"), r.lines().get(3));
        // item filter: a different item sees none of it
        ItemChainReport.Report none = ItemChainReport.build(
                List.of(trade1, drop, pickup), who, "diamond_sword", t0 - 1000, t0 + 600_000);
        assertEquals(0, none.total());
    }

    @Test void itemChainQueryEmptyAndStandaloneDelivery() {
        ItemChainReport.Report empty = ItemChainReport.build(List.of(), "Kai", "mending", 0, Long.MAX_VALUE);
        assertEquals(0, empty.total());
        assertTrue(empty.lines().isEmpty());
        // a delivery whose trade fell outside the window stands alone
        String del = env(5_000, "world", 0, 64, 0,
                "\"type\":\"item_delivery\",\"player\":\"Kai\",\"chain\":\"ch-20261105-0009\","
                + "\"delivered\":\"ground\",\"item\":" + bookJson());
        ItemChainReport.Report r = ItemChainReport.build(List.of(del), "Kai", "enchanted_book", 0, 10_000);
        assertEquals(1, r.total());
        assertTrue(r.lines().get(0).contains("DELIVERY GROUND"), r.lines().get(0));
    }

    @Test void inspectPairingResolvesAndFlags() {
        long t0 = 1_700_000_000_000L;
        String dropBook = env(t0, "world", 0, 64, 0,
                "\"type\":\"item_drop\",\"player\":\"Kai\",\"player_uuid\":\"u-kai\",\"item\":" + bookJson());
        String pickBook = env(t0 + 540_000, "world", 1, 64, 0,
                "\"type\":\"item_pickup\",\"player\":\"Kai\",\"player_uuid\":\"u-kai\",\"item\":" + bookJson()
                + ",\"owner_uuid\":\"u-kai\",\"remaining\":0");
        String dropGem = env(t0 + 60_000, "world", 0, 64, 0,
                "\"type\":\"item_drop\",\"player\":\"Kai\",\"player_uuid\":\"u-kai\","
                + "\"item\":{\"id\":\"minecraft:diamond\",\"count\":3}");
        Map<String, List<String>> by = Map.of("items", List.of(dropBook, pickBook, dropGem));
        List<InspectReport.Section> secs = InspectReport.buildSections(by, "Kai", 10, t0 + 7_200_000);
        assertEquals(1, secs.size());
        List<String> lines = secs.get(0).lines();
        assertTrue(lines.stream().anyMatch(l -> l.contains("enchanted_book") && l.contains("picked up")), lines.toString());
        assertTrue(lines.stream().anyMatch(l -> l.contains("minecraft:diamond") && l.contains("UNRECOVERED (1h59m")), lines.toString());
    }

    @Test void inspectPairingChainIdDecides() {
        long t0 = 1_700_000_000_000L;
        String d1 = env(t0, "world", 0, 64, 0,
                "\"type\":\"item_drop\",\"player\":\"Kai\",\"player_uuid\":\"u-kai\",\"item\":" + bookJson()
                + ",\"chain\":\"ch-20261105-0001\",\"link\":\"probable\"");
        String d2 = env(t0 + 60_000, "world", 0, 64, 0,
                "\"type\":\"item_drop\",\"player\":\"Kai\",\"player_uuid\":\"u-kai\",\"item\":" + bookJson()
                + ",\"chain\":\"ch-20261105-0002\",\"link\":\"exact\"");
        String pick = env(t0 + 120_000, "world", 0, 64, 0,
                "\"type\":\"item_pickup\",\"player\":\"Kai\",\"player_uuid\":\"u-kai\",\"item\":" + bookJson()
                + ",\"chain\":\"ch-20261105-0002\",\"link\":\"exact\",\"remaining\":0");
        Map<String, String> pairing = InspectReport.pairDrops(List.of(d1, d2, pick), t0 + 600_000);
        assertTrue(pairing.get(d2).contains("picked up"), pairing.toString());
        assertTrue(pairing.get(d1).contains("UNRECOVERED"), pairing.toString());
    }

    @Test void inspectEconomySummariesIncludeTrades() {
        String trade = InspectReport.summarize(env(1_000, "world", -1266, 68, -247,
                "\"type\":\"villager_trade\",\"player\":\"Kai\",\"item\":" + bookJson()
                + ",\"price\":{\"id\":\"minecraft:emerald\",\"count\":14}"));
        assertNotNull(trade);
        assertTrue(trade.contains("villager trade minecraft:enchanted_book#1"), trade);
        assertTrue(trade.contains("for minecraft:emerald#14"), trade);
        String del = InspectReport.summarize(env(2_000, "world", 0, 64, 0,
                "\"type\":\"item_delivery\",\"player\":\"Kai\",\"delivered\":\"ground\",\"item\":" + bookJson()));
        assertNotNull(del);
        assertTrue(del.contains("delivery ground"), del);
    }

    @Test void liveInventoryAggregates() {
        ItemFact b1 = new ItemFact("minecraft:enchanted_book", 1, 0, null,
                Map.of("minecraft:mending", 1), null, null, null, null);
        ItemFact b2 = new ItemFact("minecraft:enchanted_book", 1, 0, null,
                Map.of("minecraft:mending", 1), null, null, null, null);
        ItemFact arrows = new ItemFact("minecraft:arrow", 64, 0, null, null, null, null, null, null);
        ItemFact pick = new ItemFact("minecraft:diamond_pickaxe", 1, 12, null,
                Map.of("minecraft:efficiency", 4), null, null, null, null);
        LiveInventory.Summary s = LiveInventory.summarize(List.of(b1, b2, arrows), List.of(pick));
        assertEquals(3, s.invStacks());
        assertEquals(66, s.invItems());
        assertEquals(1, s.enderStacks());
        assertTrue(s.lines().stream().anyMatch(l -> l.contains("enchanted_book") && l.endsWith("x2")), s.lines().toString());
        assertTrue(s.lines().stream().anyMatch(l -> l.contains("minecraft:arrow") && l.endsWith("x64")), s.lines().toString());
    }

    // ---- player-first timelines (0.1.5, SPEC §7) ----

    private static String joinLine(String name, UUID uuid) {
        return "{\"ts\":\"t\",\"ts_ms\":1000,\"type\":\"player_join\",\"player\":\"" + name
                + "\",\"player_uuid\":\"" + uuid + "\"}";
    }

    @Test void timelineSanitizeKeys() {
        assertEquals("_coolgirlkail", Timelines.sanitize(".coolgirlkail"));
        assertEquals("MikeExcel", Timelines.sanitize("MikeExcel"));
        assertEquals("a_b_c", Timelines.sanitize("a b.c"));
    }

    @Test void timelineProjectionRoles() {
        UUID kai = UUID.randomUUID(), bea = UUID.randomUUID(), mob = UUID.randomUUID();
        Map<UUID, String> reg = new java.util.HashMap<>();
        Timelines.learn(joinLine("Kai", kai), reg);
        Timelines.learn(joinLine("Bea", bea), reg);
        assertEquals(2, reg.size());
        // single-player record -> one copy, actor
        String trade = "{\"type\":\"villager_trade\",\"player\":\"Kai\",\"player_uuid\":\"" + kai
                + "\",\"item\":" + bookJson() + "}";
        List<Timelines.Involvement> inv = Timelines.involvements(trade, reg);
        assertEquals(1, inv.size());
        assertEquals("Kai", inv.get(0).key());
        assertEquals("actor", inv.get(0).role());
        // two-player record -> both files, own roles
        String heal = "{\"type\":\"healing_incident\",\"healer\":\"Kai\",\"healer_uuid\":\"" + kai
                + "\",\"target\":\"Bea\",\"target_uuid\":\"" + bea + "\"}";
        inv = Timelines.involvements(heal, reg);
        assertEquals(2, inv.size());
        assertTrue(inv.stream().anyMatch(i -> i.key().equals("Kai") && i.role().equals("healer")), inv.toString());
        assertTrue(inv.stream().anyMatch(i -> i.key().equals("Bea") && i.role().equals("target")), inv.toString());
        // mob counterparty is never projected; the player victim is
        String dmg = "{\"type\":\"damage_incident\",\"victim\":\"Kai\",\"victim_uuid\":\"" + kai
                + "\",\"attacker\":\"zombie\",\"attacker_uuid\":\"" + mob + "\",\"attacker_kind\":\"mob\"}";
        inv = Timelines.involvements(dmg, reg);
        assertEquals(1, inv.size());
        assertEquals("victim", inv.get(0).role());
        // no-player record -> nothing
        String natural = "{\"type\":\"block_change\",\"action\":\"fade\",\"block\":\"minecraft:snow\"}";
        assertTrue(Timelines.involvements(natural, reg).isEmpty());
        // self-heal: ONE copy, stronger role (healer)
        String self = "{\"type\":\"healing_incident\",\"healer\":\"Kai\",\"healer_uuid\":\"" + kai
                + "\",\"target\":\"Kai\",\"target_uuid\":\"" + kai + "\"}";
        inv = Timelines.involvements(self, reg);
        assertEquals(1, inv.size());
        assertEquals("healer", inv.get(0).role());
        // fight summaries are not projected per-player (SPEC §7)
        String fight = "{\"type\":\"fight_summary\",\"players\":[{\"player\":\"Kai\",\"damage_dealt\":5}]}";
        assertTrue(Timelines.involvements(fight, reg).isEmpty());
    }

    @Test void timelineLedgerRoundTrip(@TempDir File dir) throws Exception {
        Ledger ledger = new Ledger(dir, new Settings(new YamlConfiguration()));
        Timelines tl = new Timelines(dir);
        UUID kai = UUID.randomUUID();
        tl.notePlayer(kai, ".coolgirlkail");
        ledger.setTimelines(tl);
        String today = Ledger.dayOf(System.currentTimeMillis());
        ledger.emit("economy", null, new Json.Obj().put("type", "villager_trade")
                .put("player", ".coolgirlkail").put("player_uuid", kai.toString()));
        ledger.emit("system", null, new Json.Obj().put("type", "boot_check")); // no player: not projected
        ledger.flushAll();
        File tf = new File(new File(dir, "players"), "_coolgirlkail-" + today + ".jsonl");
        assertTrue(tf.exists(), "timeline file written: " + tf);
        String text = Files.readString(tf.toPath(), StandardCharsets.UTF_8);
        assertTrue(text.contains("\"role\":\"actor\""), text);
        assertTrue(text.contains("\"stream\":\"economy\""), text);
        assertFalse(text.contains("boot_check"), text);
        assertEquals(1, tl.projected());
    }

    @Test void timelineFailureIsolation(@TempDir File dir) throws Exception {
        // timeline root UNDER a regular file: the players/ dir can never exist
        File blocker = new File(dir, "blocker");
        Files.writeString(blocker.toPath(), "not a directory", StandardCharsets.UTF_8);
        Ledger ledger = new Ledger(dir, new Settings(new YamlConfiguration()));
        Timelines tl = new Timelines(blocker);
        assertTrue(tl.broken());
        ledger.setTimelines(tl);
        UUID kai = UUID.randomUUID();
        tl.notePlayer(kai, "Kai");
        // the canonical write must land regardless, and project() must not throw
        ledger.emit("economy", null, new Json.Obj().put("type", "villager_trade")
                .put("player", "Kai").put("player_uuid", kai.toString()));
        ledger.flushAll();
        File[] files = dir.listFiles((d, n) -> n.startsWith("economy-"));
        assertNotNull(files); assertEquals(1, files.length);
        String text = Files.readString(files[0].toPath(), StandardCharsets.UTF_8);
        assertTrue(text.contains("villager_trade"), text);
        assertTrue(tl.failures() >= 1);
    }

    @Test void timelineRebuildRegenerates(@TempDir File dir) throws Exception {
        Timelines tl = new Timelines(dir);
        UUID kai = UUID.randomUUID();
        String day = "2026-11-05";
        List<String[]> pairs = List.of(
                new String[]{"players", joinLine("Kai", kai)},
                new String[]{"economy", "{\"ts_ms\":2000,\"type\":\"villager_trade\",\"player\":\"Kai\",\"player_uuid\":\"" + kai + "\",\"item\":" + bookJson() + "}"},
                new String[]{"items", "{\"ts_ms\":3000,\"type\":\"item_drop\",\"player\":\"Kai\",\"player_uuid\":\"" + kai + "\",\"item\":" + bookJson() + "}"},
                new String[]{"items", "{\"ts_ms\":4000,\"type\":\"item_pickup\",\"player\":\"Kai\",\"player_uuid\":\"" + kai + "\",\"item\":" + bookJson() + ",\"remaining\":0}"});
        int n = tl.rebuildDay(day, pairs);
        assertEquals(4, n); // join + trade + drop + pickup, all Kai's
        File tf = new File(new File(dir, "players"), "Kai-" + day + ".jsonl");
        assertTrue(tf.exists());
        List<String> lines = Files.readAllLines(tf.toPath(), StandardCharsets.UTF_8);
        assertEquals(4, lines.size());
        assertTrue(lines.get(1).contains("villager_trade"), lines.toString());
        assertTrue(lines.get(3).contains("\"stream\":\"items\""), lines.toString());
    }

    @Test void claritySettingsDefaultsAndClamps() {
        Settings def = new Settings(new YamlConfiguration());
        assertTrue(def.playerTimelines());
        assertEquals(1800, def.chainExpirySeconds());
        assertEquals(5, def.clarityGroundRadius());
        YamlConfiguration y = new YamlConfiguration();
        y.set("log.player-timelines", false);
        y.set("clarity.chain-expiry-seconds", 5);      // below the 60 s floor
        y.set("clarity.ground-radius", 99);            // above the 16 cap
        Settings clamped = new Settings(y);
        assertFalse(clamped.playerTimelines());
        assertEquals(60, clamped.chainExpirySeconds());
        assertEquals(16, clamped.clarityGroundRadius());
    }

    // ------------------------------------------------ 0.1.6 Recovery

    @Test void parseLocal_isoSpaceAndDateForms() {
        long expected = java.time.LocalDateTime.of(2026, 11, 6, 17, 41, 56)
                .atZone(Ledger.ZONE).toInstant().toEpochMilli();
        assertEquals(expected, Exporter.Query.parseLocal("2026-11-06T17:41:56"), "ISO with T (dead before 0.1.6)");
        assertEquals(expected, Exporter.Query.parseLocal("2026-11-06 17:41:56"), "space separator");
        long startOfDay = java.time.LocalDate.of(2026, 11, 6).atStartOfDay(Ledger.ZONE).toInstant().toEpochMilli();
        assertEquals(startOfDay, Exporter.Query.parseLocal("2026-11-06"), "bare date = start of day");
    }

    @Test void parseLocal_malformedThrowsSoParseRecordsTheError() {
        // parseLocal throws on total failure; Query.parse catches it and
        // records Query.error, which the commands surface LOUDLY (0.1.6).
        assertThrows(Exception.class, () -> Exporter.Query.parseLocal("not-a-date"));
        assertThrows(Exception.class, () -> Exporter.Query.parseLocal("2026-13-45T99:99:99"));
    }

    @Test void query_malformedKnownValueSurfacesError() {
        Exporter.Query q = Exporter.Query.parse(new String[]{"from=yesterday-ish"});
        assertNotNull(q.error); assertTrue(q.error.contains("from="));
        Exporter.Query q2 = Exporter.Query.parse(new String[]{"hours=abc"});
        assertNotNull(q2.error); assertTrue(q2.error.contains("hours="));
        Exporter.Query ok = Exporter.Query.parse(new String[]{"from=2026-11-06T17:41:56", "to=2026-11-07", "boguskey=1"});
        assertNull(ok.error, "unknown keys are ignored; known-good filters parse clean");
        assertTrue(ok.fromMs > 0 && ok.toMs > ok.fromMs);
    }

    @Test void query_fromToWindowAcceptsInsideOnly() {
        // Feature 1 regression: absolute bounds used to be silently empty
        // (the parse threw and the exception was swallowed).
        Exporter.Query q = Exporter.Query.parse(new String[]{"stream=items", "from=2026-11-06T10:00:00", "to=2026-11-06T12:00:00"});
        assertNull(q.error);
        long inside = java.time.LocalDateTime.of(2026, 11, 6, 11, 0, 0).atZone(Ledger.ZONE).toInstant().toEpochMilli();
        long outside = java.time.LocalDateTime.of(2026, 11, 6, 13, 0, 0).atZone(Ledger.ZONE).toInstant().toEpochMilli();
        String inLine = new Json.Obj().put("type", "item_pickup").put("ts_ms", inside)
                .put("item", new Json.Obj().put("id", "minecraft:x")).render();
        String outLine = new Json.Obj().put("type", "item_pickup").put("ts_ms", outside)
                .put("item", new Json.Obj().put("id", "minecraft:x")).render();
        assertTrue(q.accepts(inLine));
        assertFalse(q.accepts(outLine));
    }

    @Test void chooseContainerLine_picksEarlierSnapshotWhenToIsBetweenTransactions() {
        // Feature 1 container regression from the Nov 6 verdict: two
        // recorded transactions, to= between them -> the EARLIER snapshot.
        String earlier = new Json.Obj().put("type", "container_transaction").put("ts_ms", 1000)
                .put("contents_after", new Json.Arr()).render();
        String later = new Json.Obj().put("type", "container_transaction").put("ts_ms", 2000)
                .put("contents_after", new Json.Arr()).render();
        assertEquals(earlier, Rollback.chooseContainerLine(List.of(later, earlier), 1500));
        assertEquals(later, Rollback.chooseContainerLine(List.of(earlier, later), 2500));
        assertNull(Rollback.chooseContainerLine(List.of(earlier), 500));
    }

    @Test void chooseContainerLine_latestWinsWhenTargetIsNewest() {
        String l1 = new Json.Obj().put("type", "container_transaction").put("ts_ms", 100).put("contents_after", new Json.Arr()).render();
        String l2 = new Json.Obj().put("type", "container_transaction").put("ts_ms", 200).put("contents_after", new Json.Arr()).render();
        assertEquals(l2, Rollback.chooseContainerLine(List.of(l1, l2), Long.MAX_VALUE));
    }

    @Test void containerUndoLine_shapeAndSelfSelection() {
        // Feature 4: undo lines carry the pre-op contents as a
        // container_transaction with contents_after, selected by undo.
        Json.Arr contents = new Json.Arr().add(new Json.Obj().put("id", "minecraft:diamond").put("count", 3).put("bytes", "xyz"));
        String line = Rollback.containerUndoLine("world", 10, 64, -5, contents, 7777);
        assertEquals("container_transaction", Json.strField(line, "type"));
        assertEquals("world", Json.strField(line, "world"));
        assertEquals(10, (long) Json.numField(line, "x", 0));
        assertEquals(line, Rollback.chooseContainerLine(List.of(line), Long.MAX_VALUE));
        assertTrue(Json.rawField(line, "contents_after").contains("minecraft:diamond"));
    }

    @Test void chooseEntityLine_latestAtOrBeforeTarget() {
        String d1 = new Json.Obj().put("type", "entity_death").put("ts_ms", 100).put("entity", "minecraft:pig")
                .put("entity_uuid", "u1").render();
        String d2 = new Json.Obj().put("type", "entity_death").put("ts_ms", 200).put("entity", "minecraft:wolf")
                .put("entity_uuid", "u2").render();
        String other = new Json.Obj().put("type", "entity_state").put("ts_ms", 300).render();
        assertEquals(d2, Rollback.chooseEntityLine(List.of(d1, d2, other), 250));
        assertEquals(d1, Rollback.chooseEntityLine(List.of(d1, d2), 150));
        assertNull(Rollback.chooseEntityLine(List.of(d1), 50));
        assertEquals("entity:u2", Rollback.entityMark(d2));
    }

    @Test void chooseSnapshotAndCounts_deathSnapshotSelection() {
        // Feature 3: only death snapshots materialize; payloads count.
        Json.Arr inv = new Json.Arr()
                .add(new Json.Obj().put("id", "minecraft:diamond_sword").put("count", 1).put("bytes", "b64"))
                .add(new Json.Obj().put("empty", true))
                .add(new Json.Obj().put("id", "minecraft:apple").put("count", 2).put("bytes", "b64"));
        Json.Arr end = new Json.Arr().add(new Json.Obj().put("id", "minecraft:ender_pearl").put("count", 4).put("bytes", "b64"));
        String snap = new Json.Obj().put("type", "death_snapshot").put("ts_ms", 5000).put("death_id", "d1")
                .put("inventory", inv).put("ender_chest", end).render();
        String newer = new Json.Obj().put("type", "death_snapshot").put("ts_ms", 9000).put("death_id", "d2")
                .put("inventory", new Json.Arr()).render();
        assertEquals(snap, Rollback.chooseSnapshot(List.of(newer, snap), 6000), "to= between snapshots picks the earlier");
        assertEquals(newer, Rollback.chooseSnapshot(List.of(snap, newer), Long.MAX_VALUE));
        int[] counts = Rollback.snapshotCounts(snap);
        assertEquals(2, counts[0], "empty placeholder is not a restorable stack");
        assertEquals(1, counts[1]);
    }

    @Test void slotListFromJson_preservesRecordedEmpties() {
        // Feature 3 slot fidelity: {empty:true} placeholders hold their
        // slot so a REPLACE restore clears exactly the recorded-empty ones.
        Json.Arr arr = new Json.Arr()
                .add(new Json.Obj().put("id", "minecraft:apple").put("count", 1))
                .add(new Json.Obj().put("empty", true))
                .add(new Json.Obj().put("id", "minecraft:pearl").put("count", 1));
        List<ItemFact> slots = ItemFact.slotListFromJson(arr.render());
        assertEquals(3, slots.size());
        assertEquals("minecraft:apple", slots.get(0).id());
        assertNull(slots.get(1));
        assertEquals("minecraft:pearl", slots.get(2).id());
    }

    // ------------------------------------------------------------ 0.1.7 importer

    private static String b64(String s) {
        return java.util.Base64.getEncoder().encodeToString(s.getBytes(StandardCharsets.UTF_8));
    }

    private static void writeFixture(File f, String text) throws Exception {
        f.getParentFile().mkdirs();
        Files.writeString(f.toPath(), text, StandardCharsets.UTF_8);
    }

    private static String burnYml(String owner, String uuid, long epoch, String itemId, String bytes, Integer restoredTimes) {
        StringBuilder sb = new StringBuilder();
        if (owner != null) sb.append("owner: ").append(owner).append('\n');
        if (uuid != null) sb.append("owner-uuid: ").append(uuid).append('\n');
        sb.append("epoch-millis: ").append(epoch).append('\n');
        sb.append("cause: lava\nworld: minecraft:overworld\nx: 10.5\ny: 64.0\nz: -3.5\n");
        sb.append("item:\n  id: ").append(itemId).append("\n  count: 1\n  damage: 3\n  bytes: ").append(bytes).append('\n');
        if (restoredTimes != null) {
            sb.append("  restored-times: ").append(restoredTimes).append('\n');
            sb.append("  restored-to: ").append(owner).append('\n');
            sb.append("  restored-by: CONSOLE\n");
        }
        return sb.toString();
    }

    private static List<String> deathsLines(File dataDir) throws Exception {
        List<String> out = new java.util.ArrayList<>();
        File[] fs = dataDir.listFiles((d, n) -> n.startsWith("deaths-") && n.endsWith(".jsonl"));
        assertNotNull(fs);
        for (File f : fs) out.addAll(Files.readAllLines(f.toPath(), StandardCharsets.UTF_8));
        return out;
    }

    @Test void importerHonorsRestoredMarksAndLfState(@TempDir File dir) throws Exception {
        UUID u = UUID.fromString("11111111-2222-3333-4444-555555555555");
        long recent = System.currentTimeMillis() - 86_400_000L; // 1 day ago: inside the 14-day window
        File root = new File(dir, "ExcelDeathRecord");
        String udir = u.toString();
        // burns: two already restored in DeathRecord (the census re-offer hazard), one open
        writeFixture(new File(root, "burned/" + udir + "/burn-restored-1.yml"),
                burnYml("FixtureOne", udir, recent, "minecraft:netherite_sword", b64("sword-bytes"), 1));
        writeFixture(new File(root, "burned/" + udir + "/burn-restored-2.yml"),
                burnYml("FixtureOne", udir, recent, "minecraft:netherite_shovel", b64("shovel-bytes"), 2));
        writeFixture(new File(root, "burned/" + udir + "/burn-open.yml"),
                burnYml("FixtureOne", udir, recent, "minecraft:diamond_axe", b64("axe-bytes"), null));
        writeFixture(new File(root, "burned/unknown/burn-unknown.yml"),
                burnYml(null, null, recent, "minecraft:iron_pickaxe", b64("pick-bytes"), null));
        // death 1: xp NOT restored; one of three items already restored
        writeFixture(new File(root, "deaths/" + udir + "/death-1.yml"), """
                player: FixtureOne
                uuid: %s
                epoch-millis: %d
                world: minecraft:overworld
                xp-level: 27
                total-experience: 1395
                dropped-exp: 27
                gamemode: SURVIVAL
                death-message: FixtureOne fell out of the world
                inventory:
                - id: minecraft:diamond_pickaxe
                  count: 1
                  bytes: %s
                - id: minecraft:netherite_boots
                  count: 1
                  bytes: %s
                  restored-times: 1
                drops:
                - id: minecraft:apple
                  count: 3
                  bytes: %s
                """.formatted(udir, recent, b64("dpick-bytes"), b64("boots-bytes"), b64("apple-bytes")));
        // death 2: xp restored AND its only item restored -> nothing left to give
        writeFixture(new File(root, "deaths/" + udir + "/death-2.yml"), """
                player: FixtureOne
                uuid: %s
                epoch-millis: %d
                world: minecraft:overworld
                xp-level: 9
                total-experience: 171
                xp-restored-times: 1
                xp-restored-to: FixtureOne
                inventory:
                - id: minecraft:netherite_helmet
                  count: 1
                  bytes: %s
                  restored-times: 1
                """.formatted(udir, recent, b64("helmet-bytes")));
        // Lost & Found player state, Kailey-shaped (free NOT used, 3 paid recoveries)
        File stateFile = new File(root, "lostfound/players/" + udir + ".yml");
        writeFixture(stateFile, """
                free-used: false
                free-used-epoch: 0
                recoveries: 3
                last-recovery-epoch: 1793582762530
                tip-ok: true
                """);

        Settings cfg = new Settings(new YamlConfiguration());
        File dataDir = new File(dir, "data");
        Ledger ledger = new Ledger(dataDir, cfg);
        Recovery rec = new Recovery(ledger, cfg, dataDir);
        Importer imp = new Importer(ledger, rec, dataDir);

        Importer.Result r1 = imp.importDeathRecord(root);
        assertEquals(2, r1.deaths()); assertEquals(4, r1.burns());
        assertEquals(0, r1.skipped()); assertEquals(0, r1.errors());
        assertEquals(2, r1.restoredBurns());
        assertEquals(2, r1.restoredItems()); // boots + helmet
        assertEquals(1, r1.lfState());
        ledger.flushAll();

        // claim attempt refused: only the genuinely-open burn is recoverable
        List<Recovery.LedgerEntry> open = rec.recoverable(u);
        assertEquals(1, open.size());
        assertEquals("minecraft:diamond_axe", open.get(0).item().id());

        // state migration values exact
        Recovery.PlayerState st = rec.state(u);
        assertEquals(3, st.recoveries);
        assertFalse(st.freeUsed);
        assertEquals(1793582762530L, st.lastRecoveryEpoch);

        // stream shapes
        List<String> lines = deathsLines(dataDir);
        List<String> snaps = lines.stream().filter(l -> l.contains("\"death_snapshot\"")).toList();
        assertEquals(2, snaps.size());
        String d1 = snaps.stream().filter(l -> l.contains("death-1.yml")).findFirst().orElseThrow();
        assertEquals(27, Json.numField(d1, "xp_level", -1));
        assertEquals(1, Json.numField(d1, "source_restored_items", 0));
        assertEquals(2, ItemFact.listFromJson(Json.rawField(d1, "inventory")).size()); // boots excluded
        String d2 = snaps.stream().filter(l -> l.contains("death-2.yml")).findFirst().orElseThrow();
        assertFalse(d2.contains("\"xp_level\"")); // XP already granted by DeathRecord
        assertTrue(d2.contains("\"xp_restored_in_source\":true"));
        assertEquals(0, ItemFact.listFromJson(Json.rawField(d2, "inventory")).size());
        assertTrue(rec.isRestored("death:imported:death-2.yml"));   // fully restored -> marked
        assertFalse(rec.isRestored("death:imported:death-1.yml"));  // still has goods -> open
        List<String> burns = lines.stream().filter(l -> l.contains("\"item_destroyed\"")).toList();
        assertEquals(4, burns.size());
        assertEquals(2, burns.stream().filter(l -> l.contains("\"source_restored\":true")).count());

        // (c) double-import is a no-op: everything skipped, state and streams unchanged
        int lineCount = deathsLines(dataDir).size();
        Importer.Result r2 = imp.importDeathRecord(root);
        assertEquals(0, r2.deaths()); assertEquals(0, r2.burns());
        assertEquals(6, r2.skipped()); assertEquals(0, r2.errors());
        ledger.flushAll();
        assertEquals(3, rec.state(u).recoveries);
        assertEquals(lineCount, deathsLines(dataDir).size()); // deaths stream untouched by the re-import
    }

    @Test void importerLfStateAdvancesByDeltaNeverDoubleCounts(@TempDir File dir) throws Exception {
        UUID u = UUID.fromString("11111111-2222-3333-4444-555555555555");
        File root = new File(dir, "ExcelDeathRecord");
        File stateFile = new File(root, "lostfound/players/" + u + ".yml");
        writeFixture(stateFile, "free-used: false\nfree-used-epoch: 0\nrecoveries: 3\nlast-recovery-epoch: 1000\n");
        Settings cfg = new Settings(new YamlConfiguration());
        File dataDir = new File(dir, "data");
        Ledger ledger = new Ledger(dataDir, cfg);
        Recovery rec = new Recovery(ledger, cfg, dataDir);
        Importer imp = new Importer(ledger, rec, dataDir);

        imp.importDeathRecord(root);
        assertEquals(3, rec.state(u).recoveries);
        // DeathRecord keeps living: one more PAID recovery + the free one gets used
        writeFixture(stateFile, "free-used: true\nfree-used-epoch: 2222\nrecoveries: 4\nlast-recovery-epoch: 2222\n");
        imp.importDeathRecord(root);
        Recovery.PlayerState st = rec.state(u);
        assertEquals(4, st.recoveries);          // +1 delta, not 3+4
        assertTrue(st.freeUsed);
        assertEquals(2222, st.freeUsedEpoch);
        assertEquals(2222, st.lastRecoveryEpoch);
        // and again with no change: nothing moves
        imp.importDeathRecord(root);
        assertEquals(4, rec.state(u).recoveries);
        // Protect's own recovery in between is never subtracted or reset by a stale re-import
        rec.noteRecovery(u, false);
        assertEquals(5, rec.state(u).recoveries);
        imp.importDeathRecord(root);
        assertEquals(5, rec.state(u).recoveries);
        assertTrue(rec.state(u).freeUsed);
    }

    @Test void importedLfStateDeltaMath(@TempDir File dir) {
        ImportedLfState book = new ImportedLfState(dir);
        String p = "/x/lostfound/players/u.yml";
        ImportedLfState.Delta d1 = book.deltaFor(p, 5, true);
        assertEquals(5, d1.recoveriesDelta()); assertTrue(d1.applyFreeUsed());
        book.commit(p, 5, true);
        ImportedLfState.Delta d2 = book.deltaFor(p, 5, true);
        assertEquals(0, d2.recoveriesDelta()); assertFalse(d2.applyFreeUsed()); // re-import: no-op
        assertEquals(2, book.deltaFor(p, 7, true).recoveriesDelta());          // growth: delta only
        book.commit(p, 7, true);
        ImportedLfState.Delta d4 = book.deltaFor(p, 6, false);                  // source decreased: clamp, never subtract
        assertEquals(0, d4.recoveriesDelta()); assertFalse(d4.applyFreeUsed());
        book.commit(p, 6, false);                                               // re-baseline
        ImportedLfState.Delta d5 = book.deltaFor(p, 6, true);                   // free was already applied once for
        assertEquals(0, d5.recoveriesDelta()); assertFalse(d5.applyFreeUsed()); // this path; Recovery's OR-merge keeps it
        String p2 = "/x/lostfound/players/v.yml";                               // a path whose free flips only later:
        book.commit(p2, 2, false);
        ImportedLfState.Delta d6 = book.deltaFor(p2, 2, true);
        assertEquals(0, d6.recoveriesDelta()); assertTrue(d6.applyFreeUsed());  // applies exactly then
        // persistence across instances (the file is the state)
        ImportedLfState reopened = new ImportedLfState(dir);
        assertEquals(0, reopened.deltaFor(p, 6, false).recoveriesDelta());
    }

    @Test void recoveryImportedStateMergeIsMonotonic(@TempDir File dir) {
        Settings cfg = new Settings(new YamlConfiguration());
        Ledger ledger = new Ledger(dir, cfg);
        Recovery rec = new Recovery(ledger, cfg, dir);
        UUID u = UUID.randomUUID();
        rec.applyImportedLfState(u, false, 0, 3, 1000);
        assertEquals(3, rec.state(u).recoveries);
        assertEquals(1000, rec.state(u).lastRecoveryEpoch);
        rec.noteRecovery(u, false); // Protect-era recovery: 4, last = now
        long nowEpoch = rec.state(u).lastRecoveryEpoch;
        rec.applyImportedLfState(u, true, 555, 0, 500); // stale epochs must not rewind
        assertTrue(rec.state(u).freeUsed);
        assertEquals(555, rec.state(u).freeUsedEpoch);
        assertEquals(4, rec.state(u).recoveries);
        assertEquals(nowEpoch, rec.state(u).lastRecoveryEpoch);
        rec.applyImportedLfState(u, false, 0, 0, 0);    // a merge can never un-use the free recovery
        assertTrue(rec.state(u).freeUsed);
    }

    @Test void markLineRestoredMatchesRecoverableId(@TempDir File dir) {
        Settings cfg = new Settings(new YamlConfiguration());
        Ledger ledger = new Ledger(dir, cfg);
        Recovery rec = new Recovery(ledger, cfg, dir);
        UUID u = UUID.randomUUID();
        ItemFact f = new ItemFact("minecraft:diamond_axe", 1, 0, null, null, null, null, "PAYLOAD", null);
        String line = ledger.emit("deaths", null, new Json.Obj().put("type", "item_destroyed")
                .put("cause", "lava").put("owner", "Fixture").put("owner_uuid", u.toString())
                .put("item", f.toJson()));
        ledger.flushAll();
        assertEquals(1, rec.recoverable(u).size());
        rec.markLineRestored(line);
        long ts = (long) Json.numField(line, "ts_ms", 0);
        assertTrue(rec.isRestored(ts + ":minecraft:diamond_axe#1")); // recoverable()'s exact id shape
        assertEquals(0, rec.recoverable(u).size());                  // claim refused
    }

    // ------------------------------------------------------------ 0.1.7 kill parity

    @Test void bookEliteCacheMergeRules() {
        // The at-death EliteMobs probe is unreliable (EM unregisters during
        // its own death handling), so the book caches damage-time probes:
        // elite answers stick, NOT_ELITE yields to a later elite, UNKNOWN
        // never overwrites anything.
        DamageLedger.Book b = new DamageLedger.Book();
        assertEquals(EliteProbe.UNKNOWN, b.eliteLevel);
        b.noteElite(EliteProbe.UNKNOWN);
        assertEquals(EliteProbe.UNKNOWN, b.eliteLevel);
        b.noteElite(EliteProbe.NOT_ELITE);
        assertEquals(EliteProbe.NOT_ELITE, b.eliteLevel);
        b.noteElite(7);                                   // conversion seen later upgrades
        assertEquals(7, b.eliteLevel);
        b.noteElite(EliteProbe.NOT_ELITE);                // never downgraded by a late probe
        assertEquals(7, b.eliteLevel);
        b.noteElite(EliteProbe.UNKNOWN);
        assertEquals(7, b.eliteLevel);
    }

    @Test void rgOriginTaxonomy() {
        assertEquals("natural", RgOrigin.classOf("NATURAL"));
        assertEquals("natural", RgOrigin.classOf("DEFAULT"));
        assertEquals("natural", RgOrigin.classOf("CHUNK_GEN"));
        assertEquals("natural", RgOrigin.classOf("VILLAGE_DEFENSE"));
        assertEquals("natural", RgOrigin.classOf("VILLAGE_INVASION"));
        assertEquals("spawner", RgOrigin.classOf("SPAWNER"));
        assertEquals("spawner_egg", RgOrigin.classOf("SPAWNER_EGG"));
        assertEquals("nether_portal", RgOrigin.classOf("NETHER_PORTAL"));
        assertEquals("other", RgOrigin.classOf("BREEDING"));
        assertEquals("other", RgOrigin.classOf("CUSTOM"));
        assertEquals("natural", RgOrigin.classOf(null));   // RewardGuard semantics: absent mark = natural
        assertEquals("natural", RgOrigin.classOf(""));
    }

    @Test void bookDamageShares() {
        DamageLedger.Book b = new DamageLedger.Book();
        b.record(0, "env:FALL", "environment", null, null, "FALL", 30);
        b.record(0, "entity:x", "entity", null, null, "ENTITY_EXPLOSION", 10);
        b.record(0, "entity:y", "entity", null, null, "BLOCK_EXPLOSION", 10);
        b.record(0, "player:z", "player", null, null, "ENTITY_ATTACK", 50);
        assertEquals(0.3, b.fallShare(), 1e-9);
        assertEquals(0.2, b.explosionShare(), 1e-9);
        DamageLedger.Book empty = new DamageLedger.Book();
        assertEquals(0.0, empty.fallShare(), 0);
        assertEquals(0.0, empty.explosionShare(), 0);
    }

    @Test void killParityShapes() {
        // elite victim, explosion-heavy: flag fires at Telemetry's 0.60 threshold
        DamageLedger.Book boom = new DamageLedger.Book();
        boom.record(0, "entity:c", "entity", null, null, "ENTITY_EXPLOSION", 70);
        boom.record(0, "player:p", "player", null, null, "ENTITY_ATTACK", 30);
        Json.Obj elite = Entities.killParity(5, boom);
        assertTrue(elite.render().contains("\"elite\":true"));
        assertEquals(5, Json.numField(elite.render(), "elite_level", -1));
        String dmg = Json.rawField(elite.render(), "damage");
        assertEquals(0.7, Json.numField(dmg, "explosion_share", -1), 1e-9);
        assertEquals(0.0, Json.numField(dmg, "fall_share", -1), 1e-9);
        String flags = Json.rawField(elite.render(), "flags");
        assertNotNull(flags);
        assertTrue(flags.contains("\"explosion_share_high\""));
        assertTrue(flags.contains("\"threshold\":0.6"));
        assertTrue(elite.render().contains("\"fall_softened\":false"));

        // non-elite victim with the same book: no flags (elite-only), elite=false stamped
        Json.Obj plain = Entities.killParity(EliteProbe.NOT_ELITE, boom);
        assertTrue(plain.render().contains("\"elite\":false"));
        assertNull(Json.rawField(plain.render(), "flags"));

        // fall-dominated fight: fall_softened flips at >= 0.5 of the RAW share
        DamageLedger.Book fall = new DamageLedger.Book();
        fall.record(0, "env:FALL", "environment", null, null, "FALL", 60);
        fall.record(0, "player:p", "player", null, null, "ENTITY_ATTACK", 40);
        Json.Obj fell = Entities.killParity(EliteProbe.NOT_ELITE, fall);
        assertTrue(fell.render().contains("\"fall_softened\":true"));
        assertEquals(0.6, Json.numField(Json.rawField(fell.render(), "damage"), "fall_share", -1), 1e-9);

        // probe unavailable + no damage book: no elite claim, fall_softened present and false
        Json.Obj unknown = Entities.killParity(EliteProbe.UNKNOWN, null);
        assertEquals("unavailable", Json.strField(unknown.render(), "elite_probe"));
        assertNull(Json.rawField(unknown.render(), "damage"));
        assertTrue(unknown.render().contains("\"fall_softened\":false"));

        // share rounding is Telemetry's: round1(share*1000)/1000 (4dp)
        assertEquals(0.1235, Entities.roundShare(0.123456), 1e-9);
        assertEquals(0.6, Entities.roundShare(0.6), 1e-9);
    }

    @Test void worldEditChangeObj_blocksStreamShape() {
        // Feature 5: WE changes write the same line shape as Blocks.java,
        // and rollback's block selector consumes them like any change.
        UUID u = UUID.fromString("00000000-0000-0000-0000-0000000000aa");
        Json.Obj o = WorldEditHook.changeObj("minecraft:dirt", "minecraft:stone[brick=false]", "RigRoll", u);
        o.put("world", "world").put("x", 1.0d).put("y", 64.0d).put("z", 2.0d).put("ts_ms", 1000);
        String line = o.render();
        assertEquals("block_change", Json.strField(line, "type"));
        assertEquals("worldedit", Json.strField(line, "action"));
        assertEquals("minecraft:stone", Json.strField(line, "block"));
        assertEquals("minecraft:dirt", Json.strField(line, "before"));
        assertEquals("RigRoll", Json.strField(line, "player"));
        Map<String, String> chosen = Rollback.chooseBlocks(List.of(line), 1000);
        assertEquals(1, chosen.size());
        assertEquals(line, chosen.get("world@1,64,2"));
    }
}

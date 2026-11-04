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

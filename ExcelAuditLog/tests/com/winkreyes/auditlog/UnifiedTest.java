package com.winkreyes.auditlog;

import static org.junit.jupiter.api.Assertions.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.FileTime;
import java.time.*;
import java.util.*;
import java.util.zip.GZIPOutputStream;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

class UnifiedTest {
    @TempDir Path root;
    Path plugins;
    // "now" = 2026-10-23 19:00 Chicago = 2026-10-24 00:00 UTC
    final long now = ZonedDateTime.of(2026, 10, 23, 19, 0, 0, 0, AuditWriter.ZONE).toInstant().toEpochMilli();

    void w(String rel, String body) throws IOException { Path p = root.resolve(rel); Files.createDirectories(p.getParent()); Files.writeString(p, body); }

    @BeforeEach void setUp() throws IOException {
        plugins = root.resolve("plugins");
        w("plugins/ExcelAuditLog/audit-2026-10-23.csv", AuditWriter.HEADER + "\n"
                + "2026-10-23 18:30:00 CDT,Steve,u1,java,money,\"+250.00 -> 1250.00\"\n"
                + "2026-10-23 18:31:00 CDT,Alex,u2,java,money,+1\n"
                + "2026-10-23 10:00:00 CDT,Steve,u1,java,enderchest_open,\"\"\n");
        w("plugins/ExcelShops/purchases.csv", "timestamp,player,uuid,platform,shop,item,qty,bundle_size,unit_price,total,balance_after,offer_id\n"
                + "2026-10-23T18:40:00-05:00,Steve,u1,java,Blocks,OAK_LOG,2,16,1.50,48.00,1202.00,blocks-oak\n"
                + "2026-10-20T18:40:00-05:00,Steve,u1,java,Blocks,OAK_LOG,2,16,1.50,48.00,1202.00,blocks-oak\n");
        w("plugins/ExcelMenuStats/clicks.csv", "timestamp,player,uuid,platform,menu,button,slot,click\n"
                + "2026-10-23T18:41:00-05:00,.SteveBR,u3,bedrock,main,warps,13,FORM\n"
                + "2026-10-23T18:42:00-05:00,Steve,u1,java,main,shop > item:confirm,22,LEFT\n");
        w("plugins/ExcelTrash/trash.log", "2026-10-23T18:43:00.123-05:00[America/Chicago] TRASH player=Steve uuid=u1 stacks=3 items=130 DIRT x64, COBBLESTONE x66\n"
                + "2026-10-23T18:44:00.5-05:00[America/Chicago] UNDO player=Steve uuid=u1 stacks=3 items=130\n");
        w("plugins/ExcelDeathRecord/deaths/u1/2026-10-23_184500.yml", "# ExcelDeathRecord snapshot v1\nplayer: \"Steve\"\nuuid: \"u1\"\n"
                + "epoch-millis: " + (now - 15 * 60_000) + "\ntime: \"2026-10-23 18:45:00 CDT\"\nworld: \"world\"\nx: 10.7\ny: 64.0\nz: -3.2\n"
                + "gamemode: \"SURVIVAL\"\ndeath-message: \"Steve was slain by \\\"Zombie\\\"\"\ndamage-cause: \"ENTITY_ATTACK\"\nkiller: \"Zombie\"\nxp-level: 12\n"
                + "drops:\n  - slot: 0\n    id: \"minecraft:iron_sword\"\n  - slot: 1\n    id: \"minecraft:bread\"\ninventory:\n");
        w("plugins/ExcelDeathRecord/burned/u1/2026-10-23_184600.yml", "# ExcelDeathRecord burn-ledger v1\nowner: \"Steve\"\nowner-uuid: \"u1\"\n"
                + "epoch-millis: " + (now - 14 * 60_000) + "\ntime: \"x\"\ncause: \"LAVA\"\nworld: \"world_nether\"\nx: 1\ny: 30\nz: 2\nage-seconds: 4\n"
                + "item:\n  slot: -1\n  id: \"minecraft:diamond_pickaxe\"\n  count: 1\n");
        // UTC console log: 23:50 on the 23rd, then wraps past midnight to the 24th.
        w("logs/latest.log", "[23:50:00 INFO]: [SealLog] FIGHT_START player=Steve\n"
                + "[23:55:10] [Server thread/INFO]: [SealLog] BOSS_DEATH killer=Alex players=Alex,Steve\n"
                + "[23:58:00 INFO]: [SealLog] RAID_WON winners=-\n[00:00:05 INFO]: something else\n");
        Files.setLastModifiedTime(root.resolve("logs/latest.log"), FileTime.from(Instant.parse("2026-10-24T00:00:05Z")));
        try (OutputStream o = new GZIPOutputStream(Files.newOutputStream(root.resolve("logs/2026-10-23-1.log.gz")))) {
            o.write("[20:00:00 INFO]: [SealLog] RAID_START player=Steve omen=1\n".getBytes(StandardCharsets.UTF_8));
        }
    }

    List<Entry> run(String... args) { String[] e = new String[1]; Unified.Query q = Unified.parse(args, e); assertNotNull(q, e[0]); return Unified.collect(q, plugins, root, ZoneOffset.UTC, now, null); }

    @Test void mergesAllSourcesNewestFirst() {
        List<Entry> r = run("Steve", "24");
        List<String> types = r.stream().map(Entry::type).toList();
        assertEquals(List.of("seal", "seal", "burn", "death", "trash", "trash", "menu", "shop", "money", "seal", "enderchest"), types);
        for (int i = 1; i < r.size(); i++) assertTrue(r.get(i - 1).epochMs() >= r.get(i).epochMs());
        Entry d = r.stream().filter(e -> e.type().equals("death")).findFirst().orElseThrow();
        assertTrue(d.details().contains("Steve was slain by \"Zombie\""), d.details());
        assertTrue(d.details().contains("world 10 64 -4") && d.details().contains("drops 2"), d.details());
        assertTrue(r.stream().anyMatch(e -> e.details().contains("1x minecraft:diamond_pickaxe destroyed by LAVA")));
        assertTrue(r.stream().anyMatch(e -> e.details().equals("bought 2x16 OAK_LOG for $48.00 (Blocks) bal $1202.00")));
    }

    @Test void sealTimesAreUtcWithDateFromFile() {
        List<Entry> s = run("Steve", "48", "seal");
        assertEquals(3, s.size());
        assertEquals(Instant.parse("2026-10-23T23:55:10Z").toEpochMilli(), s.get(0).epochMs());
        assertEquals(Instant.parse("2026-10-23T20:00:00Z").toEpochMilli(), s.get(2).epochMs());
        assertTrue(run("Bob", "48", "seal").isEmpty());
    }

    @Test void typeFilterWindowAndBedrockName() {
        assertEquals(1, run("Steve", "24", "shop").size());
        assertEquals(2, run("Steve", "720", "shop").size());
        assertEquals(1, run("SteveBR", "24", "menu").size());
        assertEquals(2, run("steve", "2", "trash").size());
        assertTrue(run("Steve", "1", "enderchest").isEmpty());
    }

    @Test void pagingAndArgs() {
        String[] e = new String[1];
        Unified.Query q = Unified.parse(new String[]{"Steve", "24", "all", "page", "2"}, e);
        List<String> out = Unified.render(q, run("Steve", "24"));
        assertTrue(out.get(0).contains("11 entries, page 2/2"), out.get(0));
        assertEquals(2, out.size());
        List<String> p1 = Unified.render(Unified.parse(new String[]{"Steve"}, e), run("Steve"));
        assertEquals(12, p1.size());
        assertEquals("Next: /audit Steve 24 all page 2", p1.get(11));
        assertNull(Unified.parse(new String[]{"Steve", "bogus"}, e));
        assertTrue(e[0].contains("/audit types"));
        assertEquals("block", Unified.parse(new String[]{"Steve", "block"}, e).type());
    }

    @Test void missingSourcesAreEmptyNotErrors() throws IOException {
        Path empty = Files.createTempDirectory("al");
        String[] e = new String[1];
        assertTrue(Unified.collect(Unified.parse(new String[]{"Steve", "all"}, e), empty.resolve("plugins"), empty, ZoneOffset.UTC, now, null).isEmpty());
        assertEquals(List.of("auditlog"), Unified.available(empty.resolve("plugins"), empty));
        assertEquals(List.of("auditlog", "shop", "menu", "trash", "death", "burn", "seal"), Unified.available(plugins, root));
    }

    @Test void coreProtectRowsMergeAndFilter() {
        String[] e = new String[1];
        Unified.Query q = Unified.parse(new String[]{"Steve", "24", "chat"}, e);
        List<Entry> r = Unified.collect(q, plugins, root, ZoneOffset.UTC, now, () -> List.of(
                new Entry(now - 1000, "chat", "Steve", "chat hi [CoreProtect]"), new Entry(now - 2000, "block", "Steve", "x")));
        assertEquals(1, r.size());
        assertEquals("chat", r.get(0).type());
    }
}

package com.winkreyes.menustats;
import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.*; import java.time.*; import java.util.*;
import org.junit.jupiter.api.Test;
class MenuStatsTest {
    @Test void indexRealServerMenuAndTieBreak() throws Exception {
        Path d = Files.createTempDirectory("m");
        Files.copy(Path.of("/workspace/batch1023/menu/server_menu.fixed.yml"), d.resolve("server_menu.yml"));
        MenuIndex i = MenuIndex.load(d);
        assertEquals(0, i.failed().size()); MenuIndex.Menu m = i.byTitle("\u00a76\u00a7lServer Menu", 27);
        assertEquals("server_menu", m.id());
        assertEquals("spawn", MenuIndex.button(m, 10, "\u00a7a\u00a7lSpawn"));
        assertEquals("guild_locked", MenuIndex.button(m, 12, "\u00a78\u00a7lAdventurers Guild"));
        assertEquals("guild_open", MenuIndex.button(m, 12, "\u00a7a\u00a7lAdventurers Guild"));
        assertEquals("slot0", MenuIndex.button(m, 0, null));
    }
    @Test void logAndStats() throws Exception {
        Path d = Files.createTempDirectory("c");
        Clock clk = Clock.fixed(Instant.parse("2026-10-24T00:00:00Z"), ZoneOffset.UTC);
        ClickLog l = new ClickLog(d, ZoneId.of("America/Chicago"), clk, s -> fail(s));
        UUID a = UUID.randomUUID(), b = new UUID(0, 5);
        l.record(new ClickLog.Click(clk.instant(), "Marvin", a, "java", "server_menu", "shop", 11, "LEFT")).get();
        l.record(new ClickLog.Click(clk.instant(), ".Kailey", b, "bedrock", "server_menu", "shop", 11, "LEFT")).get();
        l.record(new ClickLog.Click(clk.instant(), "=evil", a, "java", "server_menu", "spawn", 10, "LEFT")).get();
        l.record(new ClickLog.Click(clk.instant().minus(Duration.ofDays(30)), "Old", a, "java", "x", "y", 1, "LEFT")).get();
        ClickLog.Stats s = l.stats(7, 10).get(); l.close();
        assertEquals(3, s.clicks()); assertEquals(2, s.players()); assertEquals(1, s.bedrock());
        assertEquals("server_menu > shop", s.top().get(0).key()); assertEquals(2, s.top().get(0).clicks());
        assertTrue(Files.readString(d.resolve("clicks.csv")).startsWith(ClickLog.HEADER + "\n"));
        assertTrue(Files.readString(d.resolve("clicks.csv")).contains(",'=evil,"));
    }
    @Test void formTapRowsCountAsBedrock() throws Exception {
        Path d = Files.createTempDirectory("f");
        Clock clk = Clock.fixed(Instant.parse("2026-10-24T00:00:00Z"), ZoneOffset.UTC);
        ClickLog l = new ClickLog(d, ZoneId.of("America/Chicago"), clk, s -> fail(s));
        l.record(new ClickLog.Click(clk.instant(), ".Kailey", new UUID(0, 9), "bedrock", MenuIndex.plain("\u00a76Server Menu"), "Utility Pouch", 3, "FORM")).get();
        ClickLog.Stats s = l.stats(1, 5).get(); l.close();
        assertEquals(1, s.bedrock()); assertEquals("Server Menu > Utility Pouch", s.top().get(0).key());
    }
}

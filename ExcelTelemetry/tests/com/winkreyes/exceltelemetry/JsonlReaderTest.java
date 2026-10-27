package com.winkreyes.exceltelemetry;

import com.winkreyes.exceltelemetry.core.Json;
import com.winkreyes.exceltelemetry.core.JsonlReader;
import java.util.List;
import java.util.Map;

/**
 * JSONL reader fail-soft contract: corrupt lines are counted and
 * skipped, never thrown on - a torn final line after a hard stop
 * must not break status or the weekly report.
 */
public class JsonlReaderTest {
    static int fails = 0;
    static void check(boolean c, String m) {
        if (!c) { fails++; System.out.println("FAIL " + m); } else System.out.println("ok   " + m);
    }

    public static void main(String[] args) {
        List<String> lines = List.of(
                "{\"type\":\"kill\",\"victim\":{\"type\":\"zombie\"}}",
                "{\"type\":\"kill\",\"victim\":{\"type\":\"creeper\"}}",
                "{\"type\":\"player_death\",\"player\":\"x\"}",
                "",
                "   ",
                "{\"type\":\"boss_encounter\",\"boss_kind\":\"ender_dragon\"",
                "not json at all",
                "{\"no_type\":true}",
                "{\"type\":\"\"}",
                "{\"type\":\"mcmmo_snapshot\",\"power\":379}");
        JsonlReader.Counts c = JsonlReader.countLines(lines);
        check(c.total() == 4, "valid lines counted (" + c.total() + ")");
        check(c.corrupt() == 4, "corrupt lines counted, not thrown (" + c.corrupt() + ")");
        check(c.byType().get("kill") == 2L, "per-type counts");
        check(c.byType().get("player_death") == 1L, "death counted");

        // Round-trip: every line our own writer shapes must read back clean.
        Map<String, Object> shaped = com.winkreyes.exceltelemetry.core.EventShaper.mcmmoLevelUp(
                "uuid-1", "PlayerB", "Mining", 233, 1);
        String json = Json.write(shaped);
        JsonlReader.Counts rt = JsonlReader.countLines(List.of("{\"ts\":\"x\",\"ts_ms\":1," + json.substring(1)));
        check(rt.total() == 1 && rt.corrupt() == 0, "writer-shaped lines round-trip through the reader");

        JsonlReader.Counts none = JsonlReader.countLines(List.of());
        check(none.total() == 0 && none.corrupt() == 0, "empty input is a clean zero");

        System.out.println(fails == 0 ? "ALL PASS" : "FAILED " + fails);
        if (fails > 0) System.exit(1);
    }
}

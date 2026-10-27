package com.winkreyes.exceltelemetry;

import com.winkreyes.exceltelemetry.core.EventShaper;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * mcMMO shaping: level-up lines carry skill + new level; snapshots
 * carry the full skill map; and when mcMMO is ABSENT (bridge null)
 * the shaper yields null - Telemetry must emit nothing rather than
 * fabricate numbers (the Bukkit layer passes null straight through).
 */
public class McMmoShapingTest {
    static int fails = 0;
    static void check(boolean c, String m) {
        if (!c) { fails++; System.out.println("FAIL " + m); } else System.out.println("ok   " + m);
    }

    public static void main(String[] args) {
        Map<String, Object> up = EventShaper.mcmmoLevelUp("u1", "PlayerD", "Mining", 233, 1);
        check(up != null && "mcmmo_level_up".equals(up.get("type")), "level-up line type");
        check(Integer.valueOf(233).equals(up.get("level")), "level-up carries the NEW level");
        check("Mining".equals(up.get("skill")), "level-up skill name matches flatfile naming");

        check(EventShaper.mcmmoLevelUp(null, "x", "Mining", 1, 1) == null, "no uuid -> no line");
        check(EventShaper.mcmmoLevelUp("u", "x", " ", 1, 1) == null, "blank skill -> no line");
        check(EventShaper.mcmmoLevelUp("u", "x", "Mining", -1, 1) == null, "negative level -> no line");

        Map<String, Integer> skills = new LinkedHashMap<>();
        skills.put("Mining", 232);
        skills.put("Archery", 0);
        skills.put("Swords", 34);
        Map<String, Object> snap = EventShaper.mcmmoSnapshot("u1", "PlayerD", "join", skills, 481);
        check(snap != null && "mcmmo_snapshot".equals(snap.get("type")), "snapshot line type");
        check(Integer.valueOf(481).equals(snap.get("power")), "snapshot power level");
        check("join".equals(snap.get("reason")), "snapshot reason (join/quit/hourly)");

        check(EventShaper.mcmmoSnapshot("u1", "x", "quit", null, 0) == null,
                "mcMMO unreadable (null skills) -> NO snapshot line, never a fake zero sheet");

        Map<String, Object> adv = EventShaper.advancement("u1", "PlayerC", "minecraft:end/kill_dragon");
        check(adv != null && "minecraft:end/kill_dragon".equals(adv.get("key")), "advancement line");
        check(EventShaper.advancement("u1", "x", null) == null, "keyless advancement dropped");

        System.out.println(fails == 0 ? "ALL PASS" : "FAILED " + fails);
        if (fails > 0) System.exit(1);
    }
}

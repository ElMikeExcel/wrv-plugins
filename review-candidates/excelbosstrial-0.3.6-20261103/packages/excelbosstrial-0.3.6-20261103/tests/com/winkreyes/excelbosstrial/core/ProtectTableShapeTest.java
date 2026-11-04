/* Excel WRV maintenance; existing authorship retained. */ package com.winkreyes.excelbosstrial.core;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * The ExcelProtect publish shapes (0.3.4): the verdict table's
 * rows carry EXACTLY the contract keys at the fight record's
 * one-decimal precision; the boss_state schema (version 1) is
 * stable per state; the elder progress shape carries its rows
 * without posing as a verdict; and the edge the rig cannot
 * force - a verdict with ZERO players on the table - shapes to
 * an empty row list, not an error. Also pins the NOOP sink: the
 * adapter-off state accepts every publish shape and drops it.
 */
public final class ProtectTableShapeTest {

    private static int checks;
    private static int failures;

    private static void check(boolean ok, String label) {
        checks++;
        if (!ok) {
            failures++;
            System.out.println("FAIL: " + label);
        }
    }

    private static FightTracker.FightRecord record(List<FightTracker.FightRecord.Row> rows,
                                                   String killerName,
                                                   List<String> champions) {
        UUID boss = UUID.randomUUID();
        UUID killer = killerName == null ? null : UUID.randomUUID();
        return new FightTracker.FightRecord(BossKind.WITHER, boss, "CREDIT",
                killer, killerName, 480.0, 470.24, 460.06, 97.93,
                List.of(), champions, 1_000_000L, 1_060_400L, rows,
                0.0, Map.of(), null);
    }

    private static FightTracker.FightRecord.Row row(String name, double dealt, double taken,
                                                    double done, double received) {
        return new FightTracker.FightRecord.Row(UUID.randomUUID(), name,
                dealt, taken, done, received);
    }

    public static void main(String[] args) {
        // ---- rows: the contract keys, exactly, at record precision ----
        FightTracker.FightRecord rec = record(List.of(
                row("Kailey", 300.04, 45.56, 12.34, 60.0),
                row("Mike", 160.02, 90.0, 0.0, 12.34)), "Kailey",
                List.of("Kailey", "Mike"));
        List<Map<String, Object>> rows = ProtectTableShape.rows(rec);
        check(rows.size() == 2, "two rows shaped");
        check(rows.get(0).keySet().equals(Set.of("name", "uuid", "damage_dealt",
                "damage_taken", "healing_done", "healing_received")),
                "row keys are exactly the contract six");
        check("Kailey".equals(rows.get(0).get("name")), "row name carried");
        check(rows.get(0).get("uuid") instanceof String
                && ((String) rows.get(0).get("uuid")).length() == 36, "uuid is a string");
        check(((Number) rows.get(0).get("damage_dealt")).doubleValue() == 300.0,
                "damage_dealt at one decimal (300.04 -> 300.0)");
        check(((Number) rows.get(0).get("damage_taken")).doubleValue() == 45.6,
                "damage_taken rounds (45.56 -> 45.6)");
        check(((Number) rows.get(0).get("healing_done")).doubleValue() == 12.3,
                "healing_done rounds (12.34 -> 12.3)");
        check(!rows.get(0).containsKey("deaths"),
                "no deaths key is invented (BossTrial tracks none in-fight)");

        // ---- the zero-player edge: an untouched boss's verdict ----
        FightTracker.FightRecord empty = record(List.of(), null, List.of());
        check(ProtectTableShape.rows(empty).isEmpty(),
                "zero players on the table shapes to zero rows");
        check(ProtectTableShape.rows(null).isEmpty(), "null record shapes to zero rows");
        check("".equals(ProtectTableShape.creditOf(empty)),
                "no killer reads as empty credit");
        check("Kailey".equals(ProtectTableShape.creditOf(rec)),
                "credit is the killer's name");

        // ---- rounding guards ----
        check(ProtectTableShape.round1(Double.NaN) == 0.0, "NaN rounds to 0");
        check(ProtectTableShape.round1(Double.POSITIVE_INFINITY) == 0.0,
                "infinity rounds to 0");
        check(ProtectTableShape.round1(-3.26) == -3.3, "negatives round normally");

        // ---- boss_state: fight_started ----
        Map<String, Object> started = ProtectTableShape.fightStarted(
                BossKind.WARDEN, "boss-uuid-1", 21600.04);
        check(started.get("schema").equals(ProtectTableShape.SCHEMA)
                && "bosstrial".equals(started.get("source"))
                && "warden".equals(started.get("chapter"))
                && "The Warden".equals(started.get("boss"))
                && "fight_started".equals(started.get("state"))
                && "boss-uuid-1".equals(started.get("boss_uuid"))
                && ((Number) started.get("pool")).doubleValue() == 21600.0,
                "fight_started carries the schema head + pool");

        // ---- boss_state: fight_ended (the verdict reference) ----
        Map<String, Object> ended = ProtectTableShape.fightEnded(rec, "CREDIT",
                "fights/wither-20261102-120000-abcd1234.yml");
        check("fight_ended".equals(ended.get("state"))
                && "wither".equals(ended.get("chapter"))
                && "CREDIT".equals(ended.get("verdict"))
                && "fights/wither-20261102-120000-abcd1234.yml".equals(ended.get("record"))
                && ((Number) ended.get("duration_s")).longValue() == 60L
                && ended.get("champions").equals(List.of("Kailey", "Mike")),
                "fight_ended carries verdict + record reference + champions");
        Map<String, Object> endedBare = ProtectTableShape.fightEnded(empty,
                "DENY_SHARE", null);
        check(!endedBare.containsKey("record")
                && ((List<?>) endedBare.get("champions")).isEmpty(),
                "a bare fight_ended omits the reference, keeps empty champions");

        // ---- boss_state: the elder respawn pair ----
        Map<String, Object> sched = ProtectTableShape.respawnScheduled(
                "monument-1", 1793585545902L, 3);
        check("elder_guardian".equals(sched.get("chapter"))
                && "respawn_scheduled".equals(sched.get("state"))
                && "monument-1".equals(sched.get("monument"))
                && ((Number) sched.get("respawn_due_ms")).longValue() == 1793585545902L
                && ((Number) sched.get("elders")).intValue() == 3,
                "respawn_scheduled carries monument + due + trio size");
        Map<String, Object> schedSolo = ProtectTableShape.respawnScheduled(null, 42L, 1);
        check(!schedSolo.containsKey("monument")
                && ((Number) schedSolo.get("elders")).intValue() == 1,
                "a solo schedule omits an unknown monument");
        Map<String, Object> back = ProtectTableShape.respawned(
                "monument-1", "fresh-uuid", "world");
        check("respawned".equals(back.get("state"))
                && "fresh-uuid".equals(back.get("boss_uuid"))
                && "world".equals(back.get("world")),
                "respawned carries the fresh elder + world");

        // ---- fight_progress: the elder progress shape ----
        FightTracker.FightRecord elder = new FightTracker.FightRecord(
                BossKind.ELDER_GUARDIAN, UUID.randomUUID(), "ELDER_PROGRESS 1 of 3",
                null, null, 17100.0, 17100.0, 16900.0, 98.8,
                List.of(), List.of("Kailey"), 2_000_000L, 2_900_000L,
                List.of(row("Kailey", 16900.0, 500.0, 0.0, 0.0)),
                0.0, Map.of(), "monument-1");
        Map<String, Object> prog = ProtectTableShape.fightProgress(elder,
                "ELDER_PROGRESS 1 of 3", 1, 3, "fights/elder_guardian-x.yml");
        check("elder_guardian".equals(prog.get("encounter"))
                && "ELDER_PROGRESS 1 of 3".equals(prog.get("verdict"))
                && ((Number) prog.get("progress_now")).intValue() == 1
                && ((Number) prog.get("progress_of")).intValue() == 3
                && ((List<?>) prog.get("players")).size() == 1
                && ((Number) prog.get("duration_s")).longValue() == 900L,
                "fight_progress carries counters + its own table");

        // ---- the NOOP sink drops every shape silently ----
        ProtectEmitSink noop = ProtectEmitSink.NOOP;
        noop.fightTable("wither", "CREDIT", "Kailey", 480.0, 60.0, rows);
        noop.fightTable("wither", "DENY_SHARE", "", 0.0, 0.0, List.of());
        noop.record("combat", "boss_state", started);
        check(true, "NOOP sink accepts verdict, empty-table and state shapes");

        System.out.println("ProtectTableShapeTest: " + checks + " checks, "
                + failures + " failures");
        if (failures > 0) {
            System.exit(1);
        }
    }
}

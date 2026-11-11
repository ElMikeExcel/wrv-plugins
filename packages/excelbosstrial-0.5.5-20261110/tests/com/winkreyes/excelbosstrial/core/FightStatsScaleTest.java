/* Excel WRV maintenance; existing authorship retained. */ package com.winkreyes.excelbosstrial.core;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The stats surface at the display scale (Packet B, Item 35):
 * the post-fight pop-up and the /bosstrial stats table are
 * player-facing pool-space surfaces, so the host renders them
 * through the scaled overloads (feedback.display-scale). The
 * record's own unscaled rendering - what the fights/ files and
 * the 0.4.1 round-trip pin - is untouched: these checks prove
 * both, side by side, on the 0.4.1 fixture shape.
 */
public final class FightStatsScaleTest {

    private static int passed;
    private static int failed;

    private static void check(boolean cond, String label) {
        if (cond) {
            passed++;
        } else {
            failed++;
            System.out.println("FAIL: " + label);
        }
    }

    private static boolean anyLine(List<String> lines, String needle) {
        for (String line : lines) {
            if (line.contains(needle)) {
                return true;
            }
        }
        return false;
    }

    public static void main(String[] args) {
        UUID boss = UUID.randomUUID();
        UUID jester = UUID.randomUUID();
        UUID mike = UUID.randomUUID();
        Map<UUID, String> names = Map.of(jester, "Jester_Turtle", mike, ".MikeExcel");
        long t0 = 1_700_000_000_000L;
        EncounterLedger ledger = new EncounterLedger(BossKind.WITHER, boss, 21900, t0);
        ledger.recordDealt(jester, 11000, t0 + 1000);
        ledger.recordDealt(mike, 1580, t0 + 2000);
        ledger.recordHealed(0);
        FightTracker ft = new FightTracker();
        ft.openEncounter(boss, BossKind.WITHER, t0);
        FightTracker.FightRecord rec = ft.snapshot(boss, ledger, "CREDIT", jester,
                List.of(jester, mike), names, 1000, t0 + 60_000, null);
        check(rec != null, "fixture: record produced");

        // Unscaled (the record's own rendering) - unchanged.
        List<String> popRaw = FightStatsFormat.popupLines(rec, mike);
        check(anyLine(popRaw, "1580.0"), "unscaled pop-up keeps the record figure 1580.0");
        check(!anyLine(popRaw, "1580000.0"), "unscaled pop-up carries no scaled figure");

        // Scaled x1000 (what the host shows at the settled scale).
        List<String> pop = FightStatsFormat.popupLines(rec, mike, 1000.0);
        check(anyLine(pop, "1580000.0"), "scaled pop-up: the viewer's dealt renders x1000");
        check(anyLine(pop, "12580000.0"), "scaled pop-up: party player damage renders x1000");
        check(anyLine(pop, "21900000.0"), "scaled pop-up: the pool renders x1000");
        check(anyLine(pop, "(12.6% of the party's damage)"),
                "scaled pop-up: the share percentage is NOT scaled");
        List<String> table = FightStatsFormat.tableLines(rec, jester, 1000.0);
        check(anyLine(table, "11000000.0 dmg"), "scaled table: top dealer renders x1000");
        check(anyLine(table, "21900000.0"), "scaled table: the split's pool renders x1000");

        // Scale 1 through the scaled overload == the raw rendering.
        check(FightStatsFormat.popupLines(rec, mike, 1.0).equals(popRaw),
                "scaled overload at 1.0 is byte-identical to the record rendering");
        // A junk scale can never flatten the surface to the raw
        // numbers unnoticed: NaN reads as the settled default.
        check(FightStatsFormat.popupLines(rec, mike, Double.NaN).equals(pop),
                "NaN scale reads as the settled 1000, never as raw");

        System.out.println("FightStatsScaleTest: " + passed + " passed, " + failed + " failed");
        if (failed > 0) {
            System.exit(1);
        }
    }
}

// ExcelBossTrial - a Wink Reyes Valley Excel plugin.

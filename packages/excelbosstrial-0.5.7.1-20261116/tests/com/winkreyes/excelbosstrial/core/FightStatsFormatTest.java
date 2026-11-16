/* Excel WRV maintenance; existing authorship retained. */ package com.winkreyes.excelbosstrial.core;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The post-fight stats surface (0.4.1, ㉚.9): the readout
 * math and renderings over PM-8 fight-record fixtures - a
 * CREDIT close with a source-attributed healer (the healing
 * column the ㉚.10 gate held, now open), a verdict-less
 * close, and the healed-pool shape of the Nov 7 Wither
 * (gross dealt far past the entity's health, net near zero).
 * The PM-9 split (players / other / boss healed / net) must
 * appear whole on both renderings, the header must name the
 * pool-damage space (PM-4), and a record must survive the
 * toYaml -> parseRecord round trip with its rendered table
 * byte-identical - the /bosstrial stats lookup re-shows the
 * record the close wrote, never a second accounting.
 */
public final class FightStatsFormatTest {

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

    private static void eq(double actual, double expected, String label) {
        check(Math.abs(actual - expected) < 0.001,
                label + " (expected " + expected + ", got " + actual + ")");
    }

    /** Round-trip equality at the FILE's own precision: the
     *  record format writes every number with one decimal
     *  (FightRecord.num), so a parsed value may sit half a
     *  quantum (0.05) from the in-memory original. */
    private static void eqFile(double actual, double expected, String label) {
        check(Math.abs(actual - expected) <= 0.051,
                label + " (expected " + expected + ", got " + actual + ")");
    }

    private static boolean anyLine(List<String> lines, String needle) {
        for (String line : lines) {
            if (line.contains(needle)) {
                return true;
            }
        }
        return false;
    }

    private static FightTracker.FightRecord.Row row(
            FightTracker.FightRecord rec, UUID who) {
        return FightStatsFormat.rowOf(rec, who);
    }

    public static void main(String[] args) {
        UUID boss = UUID.randomUUID();
        UUID jester = UUID.randomUUID();
        UUID kailey = UUID.randomUUID();
        UUID marvin = UUID.randomUUID();
        UUID mike = UUID.randomUUID();
        Map<UUID, String> names = Map.of(
                jester, "Jester_Turtle",
                kailey, ".coolgirlkail",
                marvin, "GumGumJetPistol",
                mike, ".MikeExcel");
        long t0 = 1_700_000_000_000L;

        // ---- Fixture A: a CREDIT close, four champions, a
        // healer whose casts name him (the 17.0 of the ㉚.10
        // rig proof, rebuilt at desk scale), the boss healed
        // 1,250 of its pool back, and 470 of the damage was
        // nobody's (PM-9's "other"). Gross 23,150 - healed
        // 1,250 = net 21,900 = the pool, exactly spent. ----
        FightTracker.FightRecord recA;
        {
            EncounterLedger ledger = new EncounterLedger(BossKind.WITHER, boss, 21900, t0);
            ledger.recordDealt(jester, 11000, t0 + 1000);
            ledger.recordDealt(kailey, 5800, t0 + 2000);
            ledger.recordDealt(marvin, 4300, t0 + 3000);
            ledger.recordDealt(mike, 1580, t0 + 4000);
            ledger.recordDealt(null, 470, t0 + 5000); // other: mob-on-boss
            ledger.recordHealed(1250);
            ledger.recordTaken(jester, 1204, t0 + 6000);
            ledger.recordTaken(kailey, 860, t0 + 6000);
            ledger.recordTaken(marvin, 977, t0 + 6000);
            ledger.recordTaken(mike, 412, t0 + 6000);
            FightTracker ft = new FightTracker();
            ft.openEncounter(boss, BossKind.WITHER, t0);
            ft.recordClaim(boss, mike, jester, "heal", t0 + 10_000);
            ft.recordHealing(boss, jester, 20, 10, 20, "CUSTOM", t0 + 10_010);
            ft.recordHealing(boss, jester, 7, 13, 20, "CUSTOM", t0 + 10_020);
            ft.recordHealing(boss, marvin, 4, 16, 20, "CUSTOM", t0 + 20_000); // no claim
            ft.recordHealing(boss, kailey, 5, 15, 20, "SATIATED", t0 + 30_000);
            recA = ft.snapshot(boss, ledger, "CREDIT", jester,
                    List.of(jester, kailey, marvin, mike), names, 1000,
                    t0 + 1_411_000, null);
        }
        check(recA != null, "A: record produced");
        eq(recA.playerDamage, 22680, "A: player damage sums the four rows");
        eq(recA.otherDamage, 470, "A: other damage is the unattributed rest");
        eq(recA.netDamage, 21900, "A: net = gross 23150 - healed 1250");
        eq(row(recA, mike).healingDone, 17.0,
                "A: the healer's two claimed regains credit him 17.0 effective");
        eq(recA.unattributedSpellHealing, 4.0, "A: the unclaimed CUSTOM heal is unattributed");
        eq(recA.otherUnattributed(), 5.0, "A: the SATIATED heal is unattributed by cause");

        // ---- Readout math --------------------------------
        eq(FightStatsFormat.shareOfParty(11000, 22680), 48.501,
                "math: top dealer share of the party");
        eq(FightStatsFormat.shareOfParty(1580, 22680), 6.966,
                "math: the healer's small damage share");
        eq(FightStatsFormat.shareOfParty(0, 22680), 0, "math: zero dealt is zero share");
        eq(FightStatsFormat.shareOfParty(500, 0), 0, "math: a zero party is zero share, never NaN");
        double shareSum = 0;
        for (FightTracker.FightRecord.Row r : recA.rows) {
            shareSum += FightStatsFormat.shareOfParty(r.damageDealt, recA.playerDamage);
        }
        check(Math.abs(shareSum - 100.0) < 0.01,
                "math: the four shares sum to 100 (got " + shareSum + ")");
        check(FightStatsFormat.durationText(0).equals("0s"), "math: duration 0s");
        check(FightStatsFormat.durationText(45).equals("45s"), "math: duration 45s");
        check(FightStatsFormat.durationText(1411).equals("23m 31s"), "math: duration 23m 31s");
        check(FightStatsFormat.durationText(3725).equals("1h 2m 5s"), "math: duration 1h 2m 5s");

        // ---- Outcome words -------------------------------
        check(FightStatsFormat.outcomeText("CREDIT").equals("Seal earned"),
                "words: CREDIT reads as the seal earned");
        check(FightStatsFormat.outcomeText("CLOSED_NO_VERDICT").contains("no verdict"),
                "words: a verdict-less close says so");
        check(FightStatsFormat.outcomeText("SEAL_GRANTED").contains("Seal granted"),
                "words: the seal-grant close names itself");
        check(FightStatsFormat.outcomeText("ELDER_PROGRESS 2 of 3").startsWith("Elder down"),
                "words: elder progress reads as progress");
        check(FightStatsFormat.outcomeText("SOME_FUTURE_LABEL").equals("SOME_FUTURE_LABEL"),
                "words: an unknown label renders as itself, never invented");

        // ---- The pop-up (the healer's view) ---------------
        List<String> popMike = FightStatsFormat.popupLines(recA, mike);
        check(anyLine(popMike, "The Wither") && anyLine(popMike, "Seal earned"),
                "popup: header names the boss and the outcome");
        check(anyLine(popMike, "pool damage"),
                "popup: the header names the number space (PM-4)");
        check(anyLine(popMike, "1580.0") && anyLine(popMike, "7.0%"),
                "popup: own line carries his damage and his share");
        check(anyLine(popMike, "17.0"),
                "popup: own line carries his healing done (the opened gate's column)");
        check(anyLine(popMike, "412.0"), "popup: own line carries his damage taken");
        check(anyLine(popMike, "21900.0") && anyLine(popMike, "22680.0")
                        && anyLine(popMike, "470.0") && anyLine(popMike, "1250.0"),
                "popup: party line carries the whole split (net/pool/players/other/healed)");
        check(anyLine(popMike, "23m 31s"), "popup: party line carries the duration");
        check(anyLine(popMike, "/bosstrial stats"), "popup: the door to the full table is named");
        List<String> popStranger = FightStatsFormat.popupLines(recA, UUID.randomUUID());
        check(!anyLine(popStranger, "You dealt"),
                "popup: a viewer with no row gets no fabricated own line");
        check(anyLine(popStranger, "Party:"), "popup: ...but still the party line");

        // ---- The full table -------------------------------
        List<String> tableA = FightStatsFormat.tableLines(recA, jester);
        check(anyLine(tableA, "fight record"), "table: header names the surface");
        check(anyLine(tableA, "pool damage"), "table: header names the number space (PM-4)");
        check(anyLine(tableA, "Killer:") && anyLine(tableA, "Jester_Turtle"),
                "table: the killer is named");
        check(anyLine(tableA, "standing at the close") && anyLine(tableA, ".coolgirlkail"),
                "table: the standing party is named");
        int jesterLine = -1;
        int mikeLine = -1;
        for (int i = 0; i < tableA.size(); i++) {
            if (tableA.get(i).contains("#1") && tableA.get(i).contains("Jester_Turtle")) {
                jesterLine = i;
            }
            if (tableA.get(i).contains(".MikeExcel")) {
                mikeLine = i;
            }
        }
        check(jesterLine >= 0, "table: the top dealer is ranked #1");
        check(mikeLine > jesterLine, "table: rows keep the record's dealt-descending order");
        check(tableA.get(jesterLine).contains("\u00bb"),
                "table: the viewer's own row is marked");
        check(anyLine(tableA, "17.0 healed"),
                "table: the healing-done column renders the healer's 17.0");
        check(anyLine(tableA, "The split:") && anyLine(tableA, "22680.0")
                        && anyLine(tableA, "470.0") && anyLine(tableA, "1250.0")
                        && anyLine(tableA, "103.6%"),
                "table: the split line carries players/other/healed/net and the verdict share"
                        + " (players dealt 103.6% of the pool - the healed 1250 was dealt twice)");
        check(anyLine(tableA, "no named source") && anyLine(tableA, "9.0"),
                "table: unattributed healing is shown whole (4.0 spell + 5.0 other)");

        // ---- Fixture B: a verdict-less close --------------
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        Map<UUID, String> namesB = Map.of(a, "Alpha", b, "Bravo");
        FightTracker.FightRecord recB;
        {
            EncounterLedger ledger = new EncounterLedger(BossKind.WITHER, boss, 6600, t0);
            ledger.recordDealt(a, 800, t0 + 1000);
            ledger.recordDealt(b, 300, t0 + 2000);
            ledger.recordDealt(null, 50, t0 + 3000);
            FightTracker ft = new FightTracker();
            ft.openEncounter(boss, BossKind.WITHER, t0);
            recB = ft.snapshot(boss, ledger, "CLOSED_NO_VERDICT", null,
                    List.of(a, b), namesB, 1000, t0 + 95_000, "north-monument");
        }
        List<String> popB = FightStatsFormat.popupLines(recB, a);
        check(anyLine(popB, "no verdict"), "B: the pop-up names the verdict-less close");
        check(anyLine(popB, "1150.0") && anyLine(popB, "6600.0"),
                "B: the party line carries the partial net against the full pool");
        List<String> tableB = FightStatsFormat.tableLines(recB, a);
        check(anyLine(tableB, "none recorded"), "B: no killer is recorded as none, not invented");
        check(anyLine(tableB, "72.7%"), "B: the leader's share of a two-player party");
        check(!anyLine(tableB, "no named source"),
                "B: no unattributed line when there is no unattributed healing");
        check(anyLine(tableB, "north-monument"), "B: the record's context renders");

        // ---- Fixture C: the healed-pool (zombie) shape ----
        {
            EncounterLedger ledger = new EncounterLedger(BossKind.WITHER, boss, 21900, t0);
            ledger.recordDealt(a, 1079, t0 + 1000); // a kill's worth of real damage
            ledger.recordHealed(1024);              // ...healed back off the entity
            FightTracker ft = new FightTracker();
            ft.openEncounter(boss, BossKind.WITHER, t0);
            FightTracker.FightRecord recC = ft.snapshot(boss, ledger, "SEAL_GRANTED", null,
                    List.of(a), namesB, 1000, t0 + 3_234_000, null);
            eq(recC.netDamage, 55, "C: net is 55 of 21900 - the boss out-healed the fight");
            List<String> popC = FightStatsFormat.popupLines(recC, a);
            check(anyLine(popC, "55.0") && anyLine(popC, "1024.0"),
                    "C: the party line shows net 55.0 beside the 1024.0 the boss healed");
            check(anyLine(popC, "100.0%"), "C: a solo fighter's share is 100.0%");
            check(anyLine(popC, "Seal granted"), "C: the seal-grant close names itself");
        }

        // ---- Round trip: the record the command re-shows --
        for (FightTracker.FightRecord rec : new FightTracker.FightRecord[] {recA, recB}) {
            FightTracker.FightRecord back = FightTracker.parseRecord(rec.toYaml());
            check(back != null, "roundtrip: " + rec.verdict + " parses back");
            if (back == null) {
                continue;
            }
            check(back.kind == rec.kind && back.verdict.equals(rec.verdict),
                    "roundtrip: kind + verdict survive");
            check(back.bossUuid.equals(rec.bossUuid), "roundtrip: boss uuid survives");
            check(back.killerName == null ? rec.killerName == null
                            : back.killerName.equals(rec.killerName),
                    "roundtrip: killer name survives (incl. null)");
            eqFile(back.pool, rec.pool, "roundtrip: pool");
            eqFile(back.totalDamage, rec.totalDamage, "roundtrip: total damage");
            eqFile(back.playerDamage, rec.playerDamage, "roundtrip: player damage");
            eqFile(back.otherDamage, rec.otherDamage, "roundtrip: other damage (PM-9)");
            eqFile(back.healedPool, rec.healedPool, "roundtrip: healed pool (PM-1)");
            eqFile(back.netDamage, rec.netDamage, "roundtrip: net damage");
            eqFile(back.sharePercent, rec.sharePercent, "roundtrip: verdict share");
            check(back.startedMs == rec.startedMs && back.endedMs == rec.endedMs,
                    "roundtrip: the fight's timestamps survive");
            check(back.championsStandingNames.equals(rec.championsStandingNames),
                    "roundtrip: the standing names survive");
            check(back.rows.size() == rec.rows.size(), "roundtrip: every row survives");
            boolean rowsEqual = back.rows.size() == rec.rows.size();
            for (int i = 0; i < rec.rows.size() && rowsEqual; i++) {
                FightTracker.FightRecord.Row x = rec.rows.get(i);
                FightTracker.FightRecord.Row y = back.rows.get(i);
                rowsEqual = x.uuid.equals(y.uuid) && x.name.equals(y.name)
                        && Math.abs(x.damageDealt - y.damageDealt) <= 0.051
                        && Math.abs(x.damageTaken - y.damageTaken) <= 0.051
                        && Math.abs(x.healingDone - y.healingDone) <= 0.051
                        && Math.abs(x.healingReceived - y.healingReceived) <= 0.051;
            }
            check(rowsEqual, "roundtrip: every row's four numbers survive");
            eqFile(back.unattributedSpellHealing, rec.unattributedSpellHealing,
                    "roundtrip: unattributed spell healing survives");
            eqFile(back.otherUnattributed(), rec.otherUnattributed(),
                    "roundtrip: unattributed by-cause healing survives");
            check(java.util.Objects.equals(back.context, rec.context),
                    "roundtrip: context survives (incl. null)");
        }
        // The re-shown table is the table the close wrote.
        FightTracker.FightRecord backA = FightTracker.parseRecord(recA.toYaml());
        check(FightStatsFormat.tableLines(backA, mike)
                        .equals(FightStatsFormat.tableLines(recA, mike)),
                "roundtrip: the parsed record renders the identical table");
        check(FightTracker.parseRecord(null) == null, "parse: null in, null out");
        check(FightTracker.parseRecord("not a fight record\nat: all") == null,
                "parse: foreign text is refused, never guessed");
        check(FightTracker.parseRecord(recA.toYaml().replace(
                        recA.bossUuid.toString(), "not-a-uuid")) == null,
                "parse: a record whose boss uuid does not parse is refused");

        // ---- Plain text for the Bedrock form --------------
        String plain = FightStatsFormat.plainText(FightStatsFormat.popupLines(recA, mike));
        check(!plain.contains("&6") && !plain.contains("&7") && !plain.contains("&a"),
                "plain: the form text carries no colour codes");
        check(plain.contains("The Wither") && plain.contains("17.0"),
                "plain: the form text keeps the numbers");

        System.out.println("FightStatsFormatTest: " + passed + " passed, " + failed + " failed");
        if (failed > 0) {
            System.exit(1);
        }
    }
}

// ExcelBossTrial - a Wink Reyes Valley Excel plugin.

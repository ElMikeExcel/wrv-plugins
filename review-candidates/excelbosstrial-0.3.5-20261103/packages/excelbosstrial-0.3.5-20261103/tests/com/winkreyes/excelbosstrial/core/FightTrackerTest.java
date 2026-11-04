/* Excel WRV maintenance; existing authorship retained. */ package com.winkreyes.excelbosstrial.core;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The fight record (0.3.1): verdict snapshots carry the ledger's
 * per-player damage table; healing counts effective only; healing
 * DONE is attributed only through a matched heal-spell cast claim;
 * everything else lands in received + the unattributed buckets;
 * healing with no encounter open is ignored; elder progress
 * points snapshot like verdicts; a DENY writes a record too.
 */
public final class FightTrackerTest {

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

    private static FightTracker.FightRecord.Row row(
            FightTracker.FightRecord rec, UUID who) {
        for (FightTracker.FightRecord.Row r : rec.rows) {
            if (r.uuid.equals(who)) {
                return r;
            }
        }
        return null;
    }

    public static void main(String[] args) {
        UUID boss = UUID.randomUUID();
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        UUID healer = UUID.randomUUID();
        Map<UUID, String> names = Map.of(a, "Alpha", b, "Bravo", healer, "Healer");
        long t0 = 1_000_000L;

        // ---- Solo fight table ------------------------------------
        {
            EncounterLedger ledger = new EncounterLedger(BossKind.WITHER, boss, 1000, t0);
            ledger.recordDealt(a, 400, t0 + 1000);
            ledger.recordDealt(a, 200, t0 + 2000);
            ledger.recordTaken(a, 25, t0 + 3000);
            FightTracker ft = new FightTracker();
            ft.openEncounter(boss, BossKind.WITHER, t0);
            FightTracker.FightRecord rec = ft.snapshot(boss, ledger, "CREDIT", a,
                    List.of(a), names, 1000, t0 + 65_000, null);
            check(rec != null, "solo: a record is produced");
            check(rec.rows.size() == 1, "solo: exactly one row");
            eq(row(rec, a).damageDealt, 600, "solo: dealt sums both hits");
            eq(row(rec, a).damageTaken, 25, "solo: taken recorded");
            eq(rec.sharePercent, 60.0, "solo: aggregate share carried");
            eq(rec.pool, 1000, "solo: pool carried");
            check(rec.durationSeconds() == 65, "solo: duration from the ledger start");
            check(rec.fileName().startsWith("wither-") && rec.fileName().endsWith(".yml"),
                    "solo: file name is fights/<seal>-<timestamp>-<boss>.yml shaped");
            check(rec.toYaml().contains("damage-dealt: 600.0"),
                    "solo: the YAML carries the per-player row");
            check(rec.logLines(rec.fileName()).size() == 2,
                    "solo: header + one player line, no unattributed line");
            check(rec.logLines(rec.fileName()).get(0).contains("verdict=CREDIT"),
                    "solo: the log header carries the verdict");
        }

        // ---- Mid-fight joiner with partial numbers ---------------
        {
            EncounterLedger ledger = new EncounterLedger(BossKind.WARDEN, boss, 2000, t0);
            ledger.recordDealt(a, 500, t0 + 1000);
            ledger.recordDealt(b, 120, t0 + 50_000); // joins late
            FightTracker ft = new FightTracker();
            ft.openEncounter(boss, BossKind.WARDEN, t0);
            FightTracker.FightRecord rec = ft.snapshot(boss, ledger, "CREDIT", b,
                    List.of(a, b), names, 1000, t0 + 60_000, null);
            check(rec.rows.size() == 2, "joiner: both players appear");
            check(rec.rows.get(0).uuid.equals(a), "joiner: rows sort by damage dealt");
            eq(row(rec, b).damageDealt, 120, "joiner: partial numbers stand as dealt");
        }

        // ---- Zero-damage healer still appears, with healing ------
        {
            EncounterLedger ledger = new EncounterLedger(BossKind.WITHER, boss, 1000, t0);
            ledger.recordDealt(a, 800, t0 + 1000);
            FightTracker ft = new FightTracker();
            ft.openEncounter(boss, BossKind.WITHER, t0);
            check(ft.recordClaim(boss, healer, a, "heal", t0 + 5000),
                    "healer: the cast claim is accepted");
            eq(ft.recordHealing(boss, a, 50, 30, 100, "CUSTOM", t0 + 5100), 50,
                    "healer: the full heal lands (50 missing, 50 healed)");
            FightTracker.FightRecord rec = ft.snapshot(boss, ledger, "CREDIT", a,
                    List.of(a, healer), names, 1000, t0 + 9000, null);
            FightTracker.FightRecord.Row h = row(rec, healer);
            check(h != null, "healer: a zero-damage healer still gets a row");
            eq(h.damageDealt, 0, "healer: dealt is zero");
            eq(h.healingDone, 50, "healer: the claimed heal attributes to the caster");
            eq(row(rec, a).healingReceived, 50, "healer: the target's received matches");
            eq(rec.unattributedSpellHealing, 0, "healer: nothing left unattributed");
        }

        // ---- Overheal counts effective only ----------------------
        {
            eq(FightTracker.effectiveHealing(100, 95, 100), 5,
                    "overheal: capped at the missing health");
            eq(FightTracker.effectiveHealing(10, 20, 20), 0,
                    "overheal: a full target heals for zero");
            eq(FightTracker.effectiveHealing(-5, 10, 20), 0,
                    "overheal: negative amounts are nothing");
            eq(FightTracker.effectiveHealing(7, 10, 20), 7,
                    "overheal: an honest heal passes through");
            FightTracker ft = new FightTracker();
            ft.openEncounter(boss, BossKind.WITHER, t0);
            eq(ft.recordHealing(boss, a, 100, 95, 100, "CUSTOM", t0), 5,
                    "overheal: the tracker records the effective 5, not the raw 100");
            eq(ft.recordHealing(boss, a, 100, 100, 100, "CUSTOM", t0), 0,
                    "overheal: healing a full player records nothing");
        }

        // ---- Heal outside an encounter is ignored ----------------
        {
            FightTracker ft = new FightTracker();
            eq(ft.recordHealing(boss, a, 50, 0, 100, "CUSTOM", t0), 0,
                    "outside: no open encounter, no record");
            check(!ft.recordClaim(boss, healer, a, "heal", t0),
                    "outside: no open encounter, no claim");
            ft.openEncounter(boss, BossKind.WITHER, t0);
            ft.closeEncounter(boss);
            eq(ft.recordHealing(boss, a, 50, 0, 100, "CUSTOM", t0), 0,
                    "outside: a closed encounter ignores heals too");
        }

        // ---- Claim window: inside attributes, outside doesn't ----
        {
            FightTracker ft = new FightTracker();
            ft.openEncounter(boss, BossKind.WITHER, t0);
            ft.recordClaim(boss, healer, a, "heal", t0 + 10_000);
            ft.recordHealing(boss, a, 40, 0, 100, "CUSTOM", t0 + 10_900); // +900ms
            ft.recordHealing(boss, a, 30, 0, 100, "CUSTOM", t0 + 12_500); // +2500ms
            ft.recordClaim(boss, healer, a, "superheal", t0 + 20_000);
            ft.recordHealing(boss, a, 20, 0, 100, "CUSTOM", t0 + 19_500); // -500ms
            FightTracker.FightRecord rec = ft.snapshot(boss, null, "CREDIT", a,
                    List.of(a, healer), names, 1000, t0 + 30_000, null);
            eq(row(rec, healer).healingDone, 60,
                    "window: heals within the window attribute (either side of the cast)");
            eq(rec.unattributedSpellHealing, 30,
                    "window: a heal outside the window stays unattributed");
            eq(row(rec, a).healingReceived, 90,
                    "window: received counts every effective heal regardless");
        }

        // ---- Non-spell causes are never source-attributed --------
        {
            FightTracker ft = new FightTracker();
            ft.openEncounter(boss, BossKind.WITHER, t0);
            ft.recordHealing(boss, a, 30, 0, 100, "MAGIC_REGEN", t0 + 1000);
            ft.recordHealing(boss, a, 10, 0, 100, "SATIATED", t0 + 2000);
            FightTracker.FightRecord rec = ft.snapshot(boss, null, "CREDIT", a,
                    List.of(a), names, 1000, t0 + 3000, null);
            eq(rec.unattributedSpellHealing, 0, "causes: no spell bucket without CUSTOM");
            eq(rec.unattributedByCause.getOrDefault("MAGIC_REGEN", 0.0), 30,
                    "causes: potion regen lands in its labeled bucket");
            eq(rec.otherUnattributed(), 40, "causes: the other total sums the buckets");
            eq(row(rec, a).healingReceived, 40, "causes: received still counts them");
            boolean anyDone = rec.rows.stream().anyMatch(r -> r.healingDone > 0);
            check(!anyDone, "causes: nobody is credited with healing done");
        }

        // ---- Elder trio: a snapshot at each of the 3 kills -------
        {
            FightTracker ft = new FightTracker();
            String[] labels = {"ELDER_PROGRESS 1 of 3", "ELDER_PROGRESS 2 of 3", "CREDIT"};
            java.util.Set<String> fileNames = new java.util.HashSet<>();
            for (int i = 0; i < 3; i++) {
                UUID elder = UUID.randomUUID();
                EncounterLedger ledger = new EncounterLedger(
                        BossKind.ELDER_GUARDIAN, elder, 17_100, t0);
                ledger.recordDealt(a, 9000 + i * 1000, t0 + 1000);
                ft.openEncounter(elder, BossKind.ELDER_GUARDIAN, t0);
                FightTracker.FightRecord rec = ft.snapshot(elder, ledger, labels[i], a,
                        List.of(a), names, 1000, t0 + 60_000, "monument-1");
                check(rec != null && rec.verdict.equals(labels[i]),
                        "trio: kill " + (i + 1) + " snapshots with its own label");
                check("monument-1".equals(rec.context),
                        "trio: kill " + (i + 1) + " carries its monument context");
                fileNames.add(rec.fileName());
                // A snapshot does not close the encounter; the
                // adapter closes it explicitly at the death.
                eq(ft.recordHealing(elder, a, 10, 0, 100, "CUSTOM", t0 + 61_000), 10,
                        "trio: tracking stays open until the adapter closes it");
                ft.closeEncounter(elder);
                eq(ft.recordHealing(elder, a, 10, 0, 100, "CUSTOM", t0 + 62_000), 0,
                        "trio: after the close, heals are ignored");
            }
            check(fileNames.size() == 3, "trio: three kills, three distinct record files");
        }

        // ---- A DENY verdict writes a record too ------------------
        {
            EncounterLedger ledger = new EncounterLedger(BossKind.WITHER, boss, 1000, t0);
            ledger.recordDealt(a, 100, t0 + 1000); // far under the share gate
            FightTracker ft = new FightTracker();
            ft.openEncounter(boss, BossKind.WITHER, t0);
            FightTracker.FightRecord rec = ft.snapshot(boss, ledger, "DENY_SHARE", null,
                    List.of(), names, 1000, t0 + 5000, null);
            check(rec != null, "deny: a record is produced");
            check("DENY_SHARE".equals(rec.verdict), "deny: the verdict label stands");
            check(rec.killerName == null, "deny: no killer is invented");
            eq(row(rec, a).damageDealt, 100, "deny: the partial table is kept");
        }

        // ---- Degenerate snapshots --------------------------------
        {
            FightTracker ft = new FightTracker();
            check(ft.snapshot(boss, null, "CREDIT", null, List.of(), names,
                    1000, t0, null) == null,
                    "degenerate: no ledger and no encounter - no record, no crash");
            ft.openEncounter(boss, BossKind.WARDEN, t0);
            FightTracker.FightRecord rec = ft.snapshot(boss, null, "DENY_PRESENCE",
                    null, List.of(), names, 1000, t0 + 1000, null);
            check(rec != null && rec.rows.isEmpty() && rec.pool == 0,
                    "degenerate: an encounter with no ledger still records, empty");
        }

        System.out.println("FightTrackerTest: " + passed + " passed, " + failed + " failed");
        if (failed > 0) {
            System.exit(1);
        }
    }
}

// ExcelBossTrial - a Wink Reyes Valley Excel plugin.

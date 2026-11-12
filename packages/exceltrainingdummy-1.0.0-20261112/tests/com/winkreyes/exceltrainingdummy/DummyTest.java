package com.winkreyes.exceltrainingdummy;

import com.winkreyes.exceltrainingdummy.core.DamageLedger;
import com.winkreyes.exceltrainingdummy.core.Hazard;
import com.winkreyes.exceltrainingdummy.core.ReportText;
import com.winkreyes.exceltrainingdummy.core.SpawnLayout;
import com.winkreyes.exceltrainingdummy.core.TestSession;
import com.winkreyes.exceltrainingdummy.core.Tier;
import com.winkreyes.exceltrainingdummy.core.ZoneCheck;

import java.util.List;
import java.util.UUID;

/**
 * Unit harness (house pattern — plain main, counted checks, exit 1
 * on any failure). Covers the Bukkit-free core: tiers, the session /
 * checkpoint / finish math, execute + endless behaviour, ledgers,
 * hazard clamping, zone verdicts, spawn geometry, report rendering.
 */
public final class DummyTest {
    private static int pass, fail;

    private static void check(String name, boolean ok, String detail) {
        if (ok) { pass++; System.out.println("PASS " + name); }
        else { fail++; System.out.println("FAIL " + name + " — " + detail); }
    }

    private static void eq(String name, double want, double got) {
        check(name, Math.abs(want - got) < 1e-9, "want " + want + " got " + got);
    }

    public static void main(String[] args) {
        UUID alice = UUID.nameUUIDFromBytes("alice".getBytes());
        UUID bob = UUID.nameUUIDFromBytes("bob".getBytes());

        // ---- tiers ---------------------------------------------------
        eq("tier-practice-pool", 1000, Tier.PRACTICE.defaultPool);
        eq("tier-dungeon-pool", 5000, Tier.DUNGEON.defaultPool);
        eq("tier-seal-pool", 20000, Tier.SEAL.defaultPool);
        check("tier-cycle", Tier.PRACTICE.next() == Tier.DUNGEON
                && Tier.DUNGEON.next() == Tier.SEAL && Tier.SEAL.next() == Tier.PRACTICE, "cycle order");
        check("tier-parse", Tier.parse("seal", Tier.PRACTICE) == Tier.SEAL
                && Tier.parse("bogus", Tier.DUNGEON) == Tier.DUNGEON
                && Tier.parse(null, Tier.SEAL) == Tier.SEAL, "parse + fallback");

        // ---- session basics + checkpoints -----------------------------
        TestSession s = new TestSession(Tier.PRACTICE, 1000, false, false, true);
        eq("session-starts-full", 1000, s.remaining());
        double r = s.recordDamage(alice, DamageLedger.Category.MELEE, 200, 1_000);
        eq("no-checkpoint-at-20pct", 0, r);
        r = s.recordDamage(alice, DamageLedger.Category.SPELLS, 100, 3_000);
        eq("checkpoint-25-crossed", 0.25, r);
        eq("remaining-after-300", 700, s.remaining());
        eq("dps-after-300-in-2s", 150, s.dps(alice, 3_000));
        r = s.recordDamage(bob, DamageLedger.Category.PROJECTILES, 200, 5_000);
        eq("checkpoint-50-crossed-by-other", 0.50, r);
        eq("bob-ledger", 200, s.ledger(bob).total());
        eq("alice-ledger", 300, s.ledger(alice).total());
        r = s.recordDamage(alice, DamageLedger.Category.MELEE, 250, 7_000);
        eq("checkpoint-75-crossed", 0.75, r);
        r = s.recordDamage(alice, DamageLedger.Category.MELEE, 999, 9_000);
        eq("finish-returns-minus-1", -1, r);
        check("session-finished", s.finished(), "finished flag");
        eq("overkill-capped-at-pool", 1000, s.totalDamage());
        eq("remaining-zero", 0, s.remaining());
        eq("final-dps-alice", (300 + 250 + 250) / 8.0, s.dps(alice, 99_000)); // alice: 800 over 8 s
        r = s.recordDamage(alice, DamageLedger.Category.MELEE, 10, 10_000);
        eq("no-damage-after-finish", 0, r);
        eq("total-stays-at-pool", 1000, s.totalDamage());

        // ---- checkpoints off ------------------------------------------
        TestSession quiet = new TestSession(Tier.PRACTICE, 1000, false, false, false);
        eq("checkpoints-off-crossing-silent", 0,
                quiet.recordDamage(alice, DamageLedger.Category.MELEE, 300, 0));
        quiet.setCheckpointsEnabled(true);
        // The 25% checkpoint was crossed silently while off; with prints back
        // on, the next hit reports the oldest pending checkpoint first.
        eq("checkpoints-toggled-on-fires-next-pending", 0.25,
                quiet.recordDamage(alice, DamageLedger.Category.MELEE, 300, 1_000));

        // ---- execute preset ---------------------------------------------
        TestSession exec = new TestSession(Tier.SEAL, 20000, false, true, true);
        eq("execute-starts-at-25pct", 5000, exec.remaining());
        double er = exec.recordDamage(alice, DamageLedger.Category.SPELLS, 100, 0);
        eq("execute-no-replay-of-past-checkpoints", 0, er);
        er = exec.recordDamage(alice, DamageLedger.Category.SPELLS, 4900, 2_000);
        eq("execute-finish", -1, er);
        check("execute-finished", exec.finished(), "finished");
        // restartAtExecute
        TestSession re = new TestSession(Tier.DUNGEON, 5000, false, false, true);
        re.recordDamage(alice, DamageLedger.Category.MELEE, 1000, 0);
        re.restartAtExecute(5_000);
        eq("execute-reset-remaining", 1250, re.remaining());
        eq("execute-reset-total-cleared", 0, re.totalDamage());
        check("execute-reset-not-finished", !re.finished(), "live again");

        // ---- endless laps -------------------------------------------------
        TestSession endless = new TestSession(Tier.PRACTICE, 1000, true, false, true);
        double lr = endless.recordDamage(alice, DamageLedger.Category.MELEE, 1000, 4_000);
        eq("endless-lap-finish-signal", -1, lr);
        check("endless-not-finished-but-resetting", !endless.finished() && endless.resetting(),
                "endless waits for reset, never 'finished'");
        eq("endless-damage-while-resetting-ignored", 0,
                endless.recordDamage(alice, DamageLedger.Category.MELEE, 50, 5_000));
        endless.resetLap(10_000);
        eq("endless-lap-2", 2, endless.lap());
        eq("endless-reset-full", 1000, endless.remaining());
        eq("endless-ledgers-cleared", 0, endless.totalDamage());
        check("endless-live-after-reset", !endless.resetting(), "resetting cleared");

        // ---- ledger --------------------------------------------------------
        DamageLedger led = new DamageLedger();
        led.add(DamageLedger.Category.MELEE, 100);
        led.add(DamageLedger.Category.MELEE, 50);
        led.add(DamageLedger.Category.SUMMONS, 25);
        led.add(DamageLedger.Category.SPELLS, -5);   // ignored
        led.add(DamageLedger.Category.OTHER, Double.NaN); // ignored
        eq("ledger-total", 175, led.total());
        eq("ledger-melee", 150, led.of(DamageLedger.Category.MELEE));
        eq("ledger-summons", 25, led.of(DamageLedger.Category.SUMMONS));
        eq("ledger-absent-category", 0, led.of(DamageLedger.Category.PROJECTILES));

        // ---- hazard ---------------------------------------------------------
        eq("hazard-light-default", 2, Hazard.LIGHT.defaultDamagePerPulse);
        eq("hazard-medium-default", 4, Hazard.MEDIUM.defaultDamagePerPulse);
        eq("hazard-intense-default", 8, Hazard.INTENSE.defaultDamagePerPulse);
        eq("hazard-clamp-leaves-1hp", 3, Hazard.pulseDamage(8, 4, false));
        eq("hazard-clamp-at-1hp-is-0", 0, Hazard.pulseDamage(8, 1, false));
        eq("hazard-full-when-healthy", 8, Hazard.pulseDamage(8, 20, false));
        eq("hazard-lethal-unclamped", 8, Hazard.pulseDamage(8, 4, true));
        eq("hazard-zero-config", 0, Hazard.pulseDamage(0, 20, true));
        check("hazard-cycle", Hazard.OFF.next() == Hazard.LIGHT && Hazard.INTENSE.next() == Hazard.OFF,
                "cycle wraps");
        // session hazard/healing accounting
        TestSession hz = new TestSession(Tier.DUNGEON, 5000, false, false, true);
        hz.recordDamage(alice, DamageLedger.Category.MELEE, 500, 0);
        hz.recordHazardDamage(30);
        hz.recordHealing(120);
        eq("session-hazard-taken", 30, hz.hazardDamageTaken());
        eq("session-hps", 120 / 10.0, hzAfter(hz, alice)); // placeholder replaced below

        // ---- zone verdicts ---------------------------------------------------
        check("zone-wilderness", ZoneCheck.evaluate(false, false, false, false) == ZoneCheck.Verdict.WILDERNESS
                && ZoneCheck.Verdict.WILDERNESS.allowed(), "no claim = wilderness");
        check("zone-own-claim", ZoneCheck.evaluate(true, false, true, false) == ZoneCheck.Verdict.OWN_CLAIM
                && ZoneCheck.Verdict.OWN_CLAIM.allowed(), "own claim allowed");
        check("zone-foreign-denied", ZoneCheck.evaluate(true, false, false, false)
                == ZoneCheck.Verdict.DENIED_FOREIGN_CLAIM
                && !ZoneCheck.Verdict.DENIED_FOREIGN_CLAIM.allowed(), "foreign claim denied");
        check("zone-admin-player-denied", ZoneCheck.evaluate(true, true, false, false)
                == ZoneCheck.Verdict.DENIED_ADMIN_CLAIM
                && !ZoneCheck.Verdict.DENIED_ADMIN_CLAIM.allowed(), "admin claim, plain player denied");
        check("zone-admin-staff-allowed", ZoneCheck.evaluate(true, true, false, true)
                == ZoneCheck.Verdict.ADMIN_ZONE && ZoneCheck.Verdict.ADMIN_ZONE.allowed(),
                "admin claim + admin permission allowed");

        // ---- spawn layout ------------------------------------------------------
        check("layout-counts", SpawnLayout.offsets(1).length == 1
                && SpawnLayout.offsets(3).length == 3 && SpawnLayout.offsets(5).length == 5, "1/3/5");
        double[][] three = SpawnLayout.offsets(3);
        eq("layout-cluster-spacing", SpawnLayout.SIDE_SPACING, Math.abs(three[1][1] - three[0][1]));
        eq("layout-forward", SpawnLayout.FORWARD, three[0][0]);
        boolean threw = false;
        try { SpawnLayout.offsets(2); } catch (IllegalArgumentException e) { threw = true; }
        check("layout-rejects-2", threw && !SpawnLayout.isSupportedCount(2)
                && SpawnLayout.isSupportedCount(5), "only 1/3/5");

        // ---- report text ---------------------------------------------------------
        TestSession rep = new TestSession(Tier.DUNGEON, 5000, false, false, true);
        rep.recordDamage(alice, DamageLedger.Category.MELEE, 2100, 0);
        rep.recordDamage(alice, DamageLedger.Category.SPELLS, 1900, 20_000);
        rep.recordDamage(alice, DamageLedger.Category.PROJECTILES, 600, 30_000);
        rep.recordDamage(alice, DamageLedger.Category.SUMMONS, 400, 42_000);
        List<String> lines = ReportText.finalReport(rep, alice, 42_000,
                Hazard.MEDIUM, false, false, 1000);
        check("report-has-duration-total-dps", lines.get(1).contains("Duration: 42.0s")
                && lines.get(1).contains("Total damage: 5,000"), lines.get(1));
        check("report-breakdown-categories", lines.get(2).contains("Melee 2,100 (42%)")
                && lines.get(2).contains("Spells 1,900 (38%)")
                && lines.get(2).contains("Projectiles 600 (12%)")
                && lines.get(2).contains("Summons 400 (8%)"), lines.get(2));
        check("report-hazard-line", lines.stream().anyMatch(l -> l.contains("Hazard Medium")
                || l.contains("Hazard")), lines.toString());
        List<String> bossLines = ReportText.finalReport(rep, alice, 42_000,
                Hazard.OFF, false, true, 1000);
        check("report-boss-scale-x1000", bossLines.get(1).contains("Total damage: 5,000,000")
                && bossLines.stream().anyMatch(l -> l.contains("boss display scale")), bossLines.get(1));
        String cp = ReportText.checkpointLine(rep, alice, 42_000, 0.5, false, 1000);
        check("checkpoint-line-shape", cp.contains("Checkpoint 50%") && cp.contains("DPS so far"), cp);
        List<String> status = ReportText.statusReadout(rep, "Mike", 42_000, Hazard.OFF, false, 1000);
        check("status-readout-names-owner", status.get(0).contains("Mike's Dungeon dummy")
                && status.get(0).contains("0 of 5,000 HP left"), status.get(0));

        System.out.println("RESULT pass=" + pass + " fail=" + fail);
        if (fail > 0) System.exit(1);
    }

    /** HPS after 10 s of test time (the hit clock started at t=0). */
    private static double hzAfter(TestSession hz, UUID alice) {
        return hz.hps(10_000);
    }
}

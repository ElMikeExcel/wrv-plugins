/* Excel WRV maintenance; existing authorship retained. */ package com.winkreyes.excelbosstrial.core;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The 0.2.1 seal-integrity package (the seal-difficulty audit,
 * SEAL-DIFFICULTY-AUDIT-2026-10-20, plus the same night's
 * adversarial triple check; Mike, Oct 20, 2026 late: "fix
 * everything else that we can"):
 *
 * 1. WITHER "fought free" check - the CAGED-AT-DEATH test
 *    (Mike's strict variant): judged on the fight's last
 *    stretch - the final wither.fought-free-window fraction
 *    (0.25) of the rallied pool, in damage dealt - from the
 *    ledger's damage-stamped position samples and hit stamps.
 *    Pins the money case (fought early, caged for the final
 *    stretch = denied), the false-positive guards (a hit or a
 *    move INSIDE the window = fought, credits), the air-pocket
 *    case (no suffocation anywhere in the rule - triple-check
 *    FLAG 2), and the window-boundary arithmetic.
 * 2. WARDEN range band - the horizontal shooting gallery. Pins
 *    both-band semantics, the Euclidean edge (14 counts, the
 *    10+10 diagonal does not), band-0 disable, and the precise
 *    per-band exclusion bookkeeping the denial texts read.
 *    Plus FLAG 4's launch-snapshot store (the bands judge the
 *    shooter's LAUNCH position, not the land-time position).
 * 3. DRAGON share gate - the phase-aware denominator pattern
 *    (ledger opened at the phase-1 bar, grown to 250 + 500 at
 *    the flip) plus the config defaults that arm it.
 * 4. ELDER latch validation (FLAG 1) - the latch truth table:
 *    a GuildGate-side revoke makes the latch STALE and the
 *    adapter clears it; unverifiable reads trust the latch.
 *
 * Plus the denial-slot selection for the two new named reasons.
 */
public final class SealIntegrityFixTest {

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

    private static void eq(double got, double want, String label) {
        check(Math.abs(got - want) < 1e-9, label + " (got " + got + ", want " + want + ")");
    }

    public static void main(String[] args) {
        witherPredicate();
        witherWindowFacts();
        wardenBand();
        wardenLedgerBanded();
        launchSnapshots();
        elderLatch();
        denialSelection();
        dragonDenominator();
        configDefaultsAndKeys();
        System.out.println("SealIntegrityFixTest: " + passed + " passed, " + failed + " failed");
        if (failed > 0) {
            System.exit(1);
        }
    }

    // ----------------------------------------------------------
    // 1a. The fought-free predicate on window facts directly
    // ----------------------------------------------------------

    private static void witherPredicate() {
        // The cage: no player hit in the window, never strayed.
        check(CreditJudge.witherNeverFought(false, 0.0, 4.0),
                "motionless + hitless in the window = caged");
        check(CreditJudge.witherNeverFought(false, 3.9, 4.0),
                "jitter inside the radius still caged");
        check(CreditJudge.witherNeverFought(false, 4.0, 4.0),
                "displacement exactly at the radius still caged");
        // The false-positive guards.
        check(!CreditJudge.witherNeverFought(true, 0.0, 4.0),
                "one player hit inside the window = fought");
        check(!CreditJudge.witherNeverFought(false, 4.1, 4.0),
                "strayed past the radius inside the window = fought");
        check(!CreditJudge.witherNeverFought(true, 30.0, 4.0),
                "hit + roam = fought");
    }

    // ----------------------------------------------------------
    // 1b. The ledger facts: the whole-life anchor (kept from the
    // first build) and the damage-stamped window facts the
    // caged-at-death test reads. Choreography helper: note a
    // position, then deal damage, in fight order (the adapter's
    // own order). Pool 750, window 0.25 x 750 = 187.5, so the
    // window in a full kill begins at totalDamage 562.5.
    // ----------------------------------------------------------

    private static final UUID P1 = UUID.randomUUID();
    private static final UUID P2 = UUID.randomUUID();

    private static EncounterLedger witherLedger() {
        return new EncounterLedger(BossKind.WITHER, UUID.randomUUID(), 750, 1_000L);
    }

    private static void step(EncounterLedger l, double x, double y, double z, double dmg) {
        l.noteBossPosition(x, y, z);
        if (dmg > 0) {
            l.recordDealt(P1, dmg, 2_000L, true);
        }
    }

    private static boolean caged(EncounterLedger l) {
        double window = 0.25 * l.maxHealth();
        return CreditJudge.witherNeverFought(
                l.windowBossHitPlayer(window), l.windowMaxDisplacement(window), 4.0);
    }

    private static void witherWindowFacts() {
        // Position anchor: first note anchors; displacement is
        // the MAX 3D distance from the anchor, never shrinking.
        EncounterLedger anchor = witherLedger();
        anchor.noteBossPosition(100.0, 64.0, 100.0);
        eq(anchor.maxDisplacement(), 0.0, "first note anchors: displacement 0");
        anchor.noteBossPosition(103.0, 64.0, 100.0);
        eq(anchor.maxDisplacement(), 3.0, "displacement tracks the move");
        anchor.noteBossPosition(100.0, 68.0, 100.0);
        eq(anchor.maxDisplacement(), 4.0, "vertical movement counts (3D)");
        anchor.noteBossPosition(100.5, 64.0, 100.0);
        eq(anchor.maxDisplacement(), 4.0, "displacement is a ratchet: returning changes nothing");

        // The cage story's share side: honest pocket swings PASS
        // the raw share - which is why the veto must exist.
        EncounterLedger share = witherLedger();
        share.recordDealt(P1, 400.0, 10L, true);
        check(share.shareMet(0.5), "cage story: the raw share passes (400 of 750)");

        // THE AIR-POCKET CAGE (triple-check FLAG 2): head in air,
        // zero suffocation anywhere - the rule has no suffocation
        // input at all. Immobilized the whole life, never hit a
        // player -> caged.
        EncounterLedger cage = witherLedger();
        step(cage, 100, 64, 100, 190);
        step(cage, 100.4, 64, 99.7, 190);
        step(cage, 99.8, 64, 100.3, 190);
        step(cage, 100.2, 64, 100, 180);
        check(!cage.bossEverHitPlayer(), "cage: never hit a player, whole life");
        check(caged(cage), "air-pocket cage, whole life: CAGED");

        // MIKE'S CASE: fought hard early (roamed, hit a player at
        // total 200), then caged for the final stretch. A
        // whole-life test would CREDIT this; the strict
        // caged-at-death rule DENIES it.
        EncounterLedger late = witherLedger();
        step(late, 0, 64, 0, 100);
        step(late, 10, 64, 0, 100);
        late.recordTaken(P1, 12, 2_500L);          // hit lands at total 200
        step(late, 25, 64, 5, 100);
        step(late, 40, 64, 0, 100);                 // total 400, still free
        step(late, 100, 64, 100, 100);              // total 500 - the cage
        step(late, 100.3, 64, 99.8, 100);           // total 600
        step(late, 99.9, 64, 100.2, 100);           // total 700
        step(late, 100.1, 64, 100, 50);             // total 750 - dead
        check(late.bossEverHitPlayer(), "late-cage: it DID fight early (whole-life fact)");
        check(!late.windowBossHitPlayer(187.5),
                "late-cage: no hit inside the final window");
        check(late.windowMaxDisplacement(187.5) <= 4.0,
                "late-cage: never strayed inside the final window");
        check(caged(late), "fought early + caged for the last stretch: CAGED (Mike's rule)");

        // CAGED EARLY, FREE AT THE END: roaming + a hit inside
        // the window -> fought -> credits.
        EncounterLedger freed = witherLedger();
        step(freed, 0, 64, 0, 200);
        step(freed, 0.3, 64, 0.2, 200);
        step(freed, 0, 64, 0, 200);                 // total 600, caged so far
        step(freed, 8, 64, 3, 100);                 // total 700 - it broke out
        freed.recordTaken(P2, 9, 3_000L);           // hit at total 700, in window
        step(freed, 20, 64, 10, 50);                // total 750
        check(freed.windowBossHitPlayer(187.5), "freed: hit inside the window reads");
        check(!caged(freed), "caged early + fought at the end: NOT caged, credits");

        // CAGED THE WHOLE WAY but lands ONE skull in the final
        // stretch -> fought -> credits (the other guard).
        EncounterLedger fighter = witherLedger();
        step(fighter, 5, 64, 5, 300);
        step(fighter, 5.2, 64, 4.9, 300);
        fighter.recordTaken(P1, 7, 3_500L);          // hit at total 600, in window
        step(fighter, 4.9, 64, 5.1, 150);
        check(!caged(fighter), "caged but landed a skull in the final stretch: NOT caged");

        // Window-start arithmetic: a huge EARLY roam must not
        // leak into the window's displacement.
        EncounterLedger roam = witherLedger();
        step(roam, 0, 64, 0, 100);
        step(roam, 60, 64, 0, 100);                 // the big roam, total 200
        step(roam, 200, 64, 200, 200);              // relocated, total 400
        step(roam, 200, 64, 200, 200);              // total 600
        step(roam, 202.5, 64, 201, 150);            // total 750, moved 2.7 in window
        check(roam.maxDisplacement() > 100, "roam: whole-life displacement is huge");
        eq(roam.windowMaxDisplacement(187.5),
                CreditJudge.distance(200, 64, 200, 202.5, 64, 201),
                "roam: window displacement measures from the window's start only");

        // A fight shorter than the window: the window covers the
        // whole fight (the boundary reads before the first
        // sample; the anchor is the first sample).
        EncounterLedger mini = witherLedger();
        step(mini, 0, 64, 0, 50);
        step(mini, 0.5, 64, 0.4, 50);               // total 100 < window 187.5
        check(caged(mini), "short caged fight: the window covers it whole, CAGED");

        // Boundary: a hit stamped exactly AT the window's start
        // counts as inside.
        EncounterLedger edge = witherLedger();
        step(edge, 0, 64, 0, 562.5);
        edge.recordTaken(P1, 5, 4_000L);            // stamped exactly 562.5
        step(edge, 0.2, 64, 0.1, 187.5);
        check(edge.windowBossHitPlayer(187.5), "hit exactly at the window start counts");

        // A mob victim never enters taken-records at all (the
        // adapter only records player victims) - the ledger API
        // has no other intake, so a skull clipping a wolf can
        // never read as fought back. Pinned as the empty case.
        check(!witherLedger().windowBossHitPlayer(187.5),
                "no player hits recorded: window reads hitless");
    }

    // ----------------------------------------------------------
    // 2c. FLAG 4: launch snapshots.
    // ----------------------------------------------------------

    private static void launchSnapshots() {
        LaunchSnapshotLog log = new LaunchSnapshotLog();
        UUID arrow1 = UUID.randomUUID();
        UUID arrow2 = UUID.randomUUID();
        check(log.lookup(arrow1) == null, "snapshots: unknown projectile reads null");
        log.record(arrow1, 30, 70, -5, 10_000L);
        double[] s = log.lookup(arrow1);
        check(s != null && s[0] == 30 && s[1] == 70 && s[2] == -5,
                "snapshots: recorded launch position reads back");
        // The pearl-in evasion, at the data level: the snapshot
        // is the LAUNCH spot (x=30, outside the 14 band of a
        // warden at origin) no matter where the shooter stands
        // when the arrow lands.
        check(!CreditJudge.withinRangeBand(s[0], s[2], 0, 0, 14.0),
                "snapshots: the out-of-band launch stays out of band");
        log.record(arrow1, 1, 64, 1, 10_500L);
        check(log.lookup(arrow1)[0] == 1, "snapshots: re-record overwrites");
        log.record(arrow2, 2, 64, 2, 39_500L);
        log.prune(50_000L, 30_000L);   // drops entries older than 30 s at t=50 s
        check(log.size() == 1 && log.lookup(arrow2) != null && log.lookup(arrow1) == null,
                "snapshots: prune drops the old, keeps the young");
    }

    // ----------------------------------------------------------
    // 4. FLAG 1: the elder latch truth table.
    // ----------------------------------------------------------

    private static void elderLatch() {
        check(CreditJudge.elderLatch(false, Boolean.TRUE) == CreditJudge.LatchState.NOT_SET,
                "latch: unset latch is NOT_SET whatever GuildGate says");
        check(CreditJudge.elderLatch(false, null) == CreditJudge.LatchState.NOT_SET,
                "latch: unset + unverifiable is NOT_SET");
        check(CreditJudge.elderLatch(true, Boolean.TRUE) == CreditJudge.LatchState.VALID,
                "latch: set + GuildGate broken = VALID");
        check(CreditJudge.elderLatch(true, Boolean.FALSE) == CreditJudge.LatchState.STALE,
                "latch: set + GuildGate UNBROKEN (revoked) = STALE - cleared, verdict path");
        check(CreditJudge.elderLatch(true, null) == CreditJudge.LatchState.UNVERIFIED,
                "latch: set + unverifiable (pre-0.3.3 GuildGate) = trusted, as 0.2.0");
    }

    // ----------------------------------------------------------
    // 2. The warden range band
    // ----------------------------------------------------------

    private static void wardenBand() {
        double bx = 0.0, by = 64.0, bz = 0.0;
        // Gallery sniper: same level (height fine), 20 blocks out.
        check(!CreditJudge.countsTowardShare(BossKind.WARDEN,
                        20.0, by, 0.0, bx, by, bz, 12.0, 14.0),
                "gallery sniper at 20 blocks, same level: excluded by range");
        // Pit fighter inside boom reach: counts.
        check(CreditJudge.countsTowardShare(BossKind.WARDEN,
                        10.0, by, 0.0, bx, by, bz, 12.0, 14.0),
                "fighter at 10 blocks, same level: counts");
        // Tower sniper moved in horizontally: still excluded by height.
        check(!CreditJudge.countsTowardShare(BossKind.WARDEN,
                        5.0, by + 25.0, 0.0, bx, by, bz, 12.0, 14.0),
                "tower sniper at 5 horizontal but +25 up: excluded by height");
        // Both bands must pass - failing either excludes.
        check(!CreditJudge.countsTowardShare(BossKind.WARDEN,
                        30.0, by + 30.0, 0.0, bx, by, bz, 12.0, 14.0),
                "far AND high: excluded");
        // Range edge: exactly 14 counts (the boom's own reach),
        // 14.5 does not.
        check(CreditJudge.withinRangeBand(14.0, 0.0, 0.0, 0.0, 14.0),
                "edge: exactly 14 horizontal counts");
        check(!CreditJudge.withinRangeBand(14.5, 0.0, 0.0, 0.0, 14.0),
                "edge: 14.5 horizontal does not count");
        // The distance is Euclidean: 10 + 10 diagonal is 14.14 -
        // out; 9 + 9 is 12.7 - in.
        check(!CreditJudge.withinRangeBand(10.0, 10.0, 0.0, 0.0, 14.0),
                "diagonal 10+10 (14.14) does not count");
        check(CreditJudge.withinRangeBand(9.0, 9.0, 0.0, 0.0, 14.0),
                "diagonal 9+9 (12.7) counts");
        // Band 0 disables the horizontal rule (height still works).
        check(CreditJudge.countsTowardShare(BossKind.WARDEN,
                        500.0, by + 2.0, 0.0, bx, by, bz, 12.0, 0.0),
                "range band 0 disables the horizontal rule");
        check(!CreditJudge.countsTowardShare(BossKind.WARDEN,
                        500.0, by + 25.0, 0.0, bx, by, bz, 12.0, 0.0),
                "range band 0: height band still excludes the tower");
        // Other kinds ignore both bands entirely.
        check(CreditJudge.countsTowardShare(BossKind.WITHER,
                        500.0, by + 90.0, 0.0, bx, by, bz, 12.0, 14.0),
                "wither: unaffected by either band");
        check(CreditJudge.countsTowardShare(BossKind.ELDER_GUARDIAN,
                        500.0, by - 40.0, 0.0, bx, by, bz, 12.0, 14.0),
                "elder guardian: unaffected by either band");
        // The pre-0.2.1 predicate shape is untouched.
        check(!CreditJudge.countsTowardShare(BossKind.WARDEN, by + 25.0, by, 12.0),
                "legacy predicate: tower hit still excluded");
        check(CreditJudge.countsTowardShare(BossKind.WARDEN, by + 5.0, by, 12.0),
                "legacy predicate: ground hit still counts");
    }

    // ----------------------------------------------------------
    // 2b. Banded ledger records: precise per-band bookkeeping
    // ----------------------------------------------------------

    private static void wardenLedgerBanded() {
        UUID boss = UUID.randomUUID();
        UUID sniper = UUID.randomUUID();
        UUID fighter = UUID.randomUUID();
        EncounterLedger l = new EncounterLedger(BossKind.WARDEN, boss, 800, 0L);

        // Range-only failure: raises the total, earns no share,
        // tallied under RANGE only, attacker not a participant.
        boolean nd = l.recordDealtBanded(sniper, 30.0, 1L, true, false);
        check(!nd, "range-excluded first hit registers no damager");
        eq(l.totalDamage(), 30.0, "range-excluded hit still lands in the total");
        eq(l.playerDamage(), 0.0, "range-excluded hit earns no share");
        check(l.shareExcludedHits() == 1, "exclusion total counts the hit");
        check(l.rangeExcludedHits() == 1, "range counter counts the hit");
        check(l.heightExcludedHits() == 0, "height counter untouched by a range miss");
        check(!l.isParticipant(sniper), "range-excluded attacker is not a participant");

        // Height-only failure: the mirror image.
        l.recordDealtBanded(sniper, 20.0, 2L, false, true);
        check(l.heightExcludedHits() == 1, "height counter counts its hit");
        check(l.rangeExcludedHits() == 1, "range counter untouched by a height miss");
        check(l.shareExcludedHits() == 2, "exclusion total counts both hits");

        // Both bands failed on one hit: tallied under both, once
        // in the total exclusion count.
        l.recordDealtBanded(sniper, 10.0, 3L, false, false);
        check(l.heightExcludedHits() == 2 && l.rangeExcludedHits() == 2,
                "a both-bands miss tallies under both bands");
        check(l.shareExcludedHits() == 3, "a both-bands miss is ONE excluded hit");

        // In-band hit: the ordinary attributed record.
        nd = l.recordDealtBanded(fighter, 50.0, 4L, true, true);
        check(nd, "in-band first hit registers the damager");
        eq(l.playerDamage(), 50.0, "in-band hit earns its share");
        eq(l.totalDamage(), 110.0, "total is every hit that landed");
        check(l.isParticipant(fighter), "in-band attacker is a participant");
    }

    // ----------------------------------------------------------
    // Denial slot selection for the new reasons
    // ----------------------------------------------------------

    private static void denialSelection() {
        CreditJudge.Verdict denyShare = CreditJudge.Verdict.DENY_SHARE;
        CreditJudge.Verdict denyPresence = CreditJudge.Verdict.DENY_PRESENCE;
        CreditJudge.Verdict credit = CreditJudge.Verdict.CREDIT;

        // Wither cage: the cage fact reroutes the share text.
        check(DenialMessage.select(BossKind.WITHER, denyShare, 0, 0, true)
                        == DenialMessage.Slot.SHARE_CAGE,
                "wither share denial + cage facts: cage text");
        check(DenialMessage.select(BossKind.WITHER, denyShare, 0, 0, false)
                        == DenialMessage.Slot.SHARE,
                "wither share denial without cage facts: generic text");
        // Warden range-only exclusion: the range text.
        check(DenialMessage.select(BossKind.WARDEN, denyShare, 0, 7, false)
                        == DenialMessage.Slot.SHARE_RANGE,
                "warden share denial, range exclusions only: range text");
        // Both bands failed: the settled height text wins.
        check(DenialMessage.select(BossKind.WARDEN, denyShare, 3, 7, false)
                        == DenialMessage.Slot.SHARE_HEIGHT,
                "warden share denial, both bands: height text (settled first)");
        check(DenialMessage.select(BossKind.WARDEN, denyShare, 3, 0, false)
                        == DenialMessage.Slot.SHARE_HEIGHT,
                "warden share denial, height only: height text (unchanged)");
        check(DenialMessage.select(BossKind.WARDEN, denyShare, 0, 0, false)
                        == DenialMessage.Slot.SHARE,
                "warden share denial, no exclusions: generic text (unchanged)");
        // The bands never reroute a presence denial.
        check(DenialMessage.select(BossKind.WARDEN, denyPresence, 3, 7, false)
                        == DenialMessage.Slot.PRESENCE,
                "warden presence denial with exclusions: presence text");
        // The dragon's share denial is the generic text with her
        // real percent (the adapter fills {pct} from the ledger).
        check(DenialMessage.select(BossKind.ENDER_DRAGON, denyShare, 0, 0, false)
                        == DenialMessage.Slot.SHARE,
                "dragon share denial: generic share text");
        // A credit never selects a denial, whatever the facts.
        check(DenialMessage.select(BossKind.WITHER, credit, 0, 0, true)
                        == DenialMessage.Slot.NONE,
                "credit + cage-shaped facts: no denial text (selection never fires)");
        // The cage flag means nothing for the warden.
        check(DenialMessage.select(BossKind.WARDEN, denyShare, 0, 0, true)
                        == DenialMessage.Slot.SHARE,
                "cage flag on the warden: ignored, generic text");
    }

    // ----------------------------------------------------------
    // 3. Dragon phase-aware denominator (the adapter's pattern)
    // ----------------------------------------------------------

    private static void dragonDenominator() {
        UUID dragon = UUID.randomUUID();
        UUID champion = UUID.randomUUID();
        UUID poker = UUID.randomUUID();

        // The staged-wither story: the ledger opens at the
        // phase-1 bar (250), a participant pokes her once, and a
        // staged wither deals most of the damage as unattributed
        // explosions across both phases.
        EncounterLedger l = new EncounterLedger(BossKind.ENDER_DRAGON, dragon, 250, 0L);
        l.recordDealt(poker, 10.0, 1L, true);            // the one poke
        l.recordDealt(null, 240.0, 2L, true);            // phase 1 finished by the wither
        check(l.shareMet(0.5) == false,
                "pre-flip sanity: 10 of 250 already fails the gate");
        // The flip fires; the host syncs the denominator to the
        // phase-aware pool (250 + 500).
        l.growTo(750);
        l.recordDealt(null, 460.0, 3L, true);            // phase 2 ground down by the wither
        l.recordDealt(poker, 15.0, 4L, true);            // a little more presence
        eq(l.maxHealth(), 750.0, "denominator is the phase-aware pool after the flip");
        eq(l.playerSharePercent(), 25.0 / 750.0 * 100.0,
                "share percent is the real dealt share of the whole fight");
        check(!l.shareMet(0.5),
                "staged-wither kill: players dealt 25 of 750 -> gate refuses");
        check(l.isParticipant(poker),
                "the poker WAS a participant (presence passes) - the gate, not presence, refuses");

        // The honest fight: players deal across both phases.
        EncounterLedger honest = new EncounterLedger(BossKind.ENDER_DRAGON,
                UUID.randomUUID(), 250, 0L);
        honest.recordDealt(champion, 200.0, 1L, true);   // phase 1, fought
        honest.growTo(750);                              // the flip
        honest.recordDealt(champion, 320.0, 2L, true);   // phase 2, fought
        honest.recordDealt(null, 230.0, 3L, true);       // beds + misc unattributed
        check(honest.shareMet(0.5),
                "honest two-phase fight: 520 of 750 (69%) credits");
        // And the sync is a ratchet: a later smaller pool (a
        // mid-fight read in phase 2 alone) never shrinks it.
        honest.growTo(500);
        eq(honest.maxHealth(), 750.0, "denominator ratchet: never shrinks");

        // An UNTRACKED dragon (vanilla respawn, chapter pool 0):
        // the host skips the sync and the dragon's own bar stays
        // the denominator - a vanilla fight stays winnable.
        EncounterLedger vanilla = new EncounterLedger(BossKind.ENDER_DRAGON,
                UUID.randomUUID(), 200, 0L);
        vanilla.recordDealt(champion, 150.0, 1L, true);
        check(vanilla.shareMet(0.5),
                "untracked vanilla dragon: her own 200 bar stays the denominator");
    }

    // ----------------------------------------------------------
    // Config: the new knobs at their settled defaults
    // ----------------------------------------------------------

    private static void configDefaultsAndKeys() {
        BossTrialConfig d = BossTrialConfig.defaults();
        check(d.dragonShareGate, "defaults: dragon share gate ON (the audit fix)");
        eq(d.wardenShareRangeBand, 14.0, "defaults: warden range band 14 (the boom's reach)");
        eq(d.wardenShareHeightBand, 12.0, "defaults: warden height band still 12 (unchanged)");
        check(d.witherFoughtFreeCheck, "defaults: wither fought-free check ON");
        eq(d.witherFoughtFreeRadius, 4.0, "defaults: fought-free radius 4");
        eq(d.witherFoughtFreeWindow, 0.25, "defaults: fought-free window = the final quarter");

        // fromMap reads the new keys...
        Map<String, Object> m = new HashMap<>();
        m.put("dragon.share-gate", false);
        m.put("warden.share-range-band", 20.0);
        m.put("wither.fought-free-check", false);
        m.put("wither.fought-free-radius", 6.0);
        m.put("wither.fought-free-window", 0.5);
        BossTrialConfig c = BossTrialConfig.fromMap(m);
        check(!c.dragonShareGate, "fromMap: dragon.share-gate false honored");
        eq(c.wardenShareRangeBand, 20.0, "fromMap: warden.share-range-band honored");
        check(!c.witherFoughtFreeCheck, "fromMap: wither.fought-free-check false honored");
        eq(c.witherFoughtFreeRadius, 6.0, "fromMap: wither.fought-free-radius honored");
        eq(c.witherFoughtFreeWindow, 0.5, "fromMap: wither.fought-free-window honored");
        Map<String, Object> wild = new HashMap<>();
        wild.put("wither.fought-free-window", 9.0);
        eq(BossTrialConfig.fromMap(wild).witherFoughtFreeWindow, 1.0,
                "fromMap: fought-free window clamps to the whole fight at most");

        // ...and an empty map (a 0.2.0 config carried over) runs
        // the settled 0.2.1 defaults, fixes ON.
        BossTrialConfig carried = BossTrialConfig.fromMap(Map.of());
        check(carried.dragonShareGate && carried.witherFoughtFreeCheck,
                "carried-over config: both new gates default ON");
        eq(carried.wardenShareRangeBand, 14.0, "carried-over config: range band defaults 14");

        // NO TUNING DRIFT (hard constraint): the rally pools and
        // thresholds are exactly the 0.2.0 settled numbers.
        eq(carried.shareThreshold, 0.5, "no drift: share threshold 0.5");
        eq(carried.witherScaling.baseHealth(), 750.0, "no drift: wither pool base 750");
        eq(carried.witherScaling.perExtraDamager(), 200.0, "no drift: wither step 200");
        eq(carried.elderScaling.baseHealth(), 400.0, "no drift: elder pool base 400");
        eq(carried.wardenScaling.baseHealth(), 800.0, "no drift: warden pool base 800");
        eq(carried.wardenScaling.perExtraDamager(), 150.0, "no drift: warden step 150");
        eq(carried.presenceRange, 128.0, "no drift: presence range 128");
    }
}

// ExcelBossTrial - a Wink Reyes Valley Excel plugin.


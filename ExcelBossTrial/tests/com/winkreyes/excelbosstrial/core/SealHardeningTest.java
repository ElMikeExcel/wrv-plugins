package com.winkreyes.excelbosstrial.core;

import java.util.Map;
import java.util.UUID;

/**
 * The 0.2.2 seal-hardening pair (Mike, Oct 22, 2026, from the
 * seal-hardening analysis of PlayerB's Oct 20-22 attempts):
 *
 * 1. THE WARDEN GIVES UP (option 1B): a warden whose ledger
 *    records ONLY excluded (band) or unattributed damage for a
 *    sustained 45 s window burrows away - no death, no drops,
 *    no verdict, no cooldown. These checks pin the predicate's
 *    truth table (WardenGiveUp.due) and the ledger stamps it
 *    reads: counted damage resets the clock, environmental /
 *    unattributed damage does NOT, the warden's own landed
 *    blows are fight evidence too, and an encounter with zero
 *    dealt damage can never trigger.
 *
 * 2. CRYSTAL + ANCHOR ATTRIBUTION (option 3A): the placement
 *    registry that lets end crystals and respawn anchors answer
 *    to their placer, band-judged like player-lit TNT. These
 *    checks pin the registry's lifecycle (place -> resolve ->
 *    forget), the detonation log's radius/window edges and
 *    most-recent-wins rule, and the unregistered case staying
 *    unattributed (the natural explosion, the pre-restart
 *    placement - the safe direction).
 *
 * Interpretation pinned here, per the approved wording ("only
 * excluded damage for a sustained window; in-band damage
 * resets"): the clock measures time since the last FIGHT
 * EVIDENCE (a counted blow, or a blow the warden landed).
 * Silence is no-evidence too - a fight that simply stopped is
 * also a fight that never came, and the warden does not stand
 * wounded forever. An honest fighter lands evidence constantly
 * and never sees the window lapse mid-fight.
 */
public final class SealHardeningTest {

    private static int passed;
    private static int failed;

    private static final long W = WardenGiveUp.DEFAULT_WINDOW_MS; // 45_000

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
        giveUpPredicate();
        giveUpLedger();
        registryCrystals();
        registryAnchors();
        registryDetonations();
        configKeys();
        System.out.println("SealHardeningTest: " + passed + " passed, " + failed + " failed");
        if (failed > 0) {
            System.exit(1);
        }
    }

    // ----------------------------------------------------------
    // 1a. The give-up predicate, on raw stamps
    // ----------------------------------------------------------

    private static void giveUpPredicate() {
        // The rule disabled never fires, however stale the fight.
        check(!WardenGiveUp.due(500, 1_000, 0, 0, 999_999_999, 0),
                "window 0 disables the rule");
        check(!WardenGiveUp.due(500, 1_000, 0, 0, 999_999_999, -5),
                "negative window disables the rule");

        // Zero dealt damage never triggers (no encounter, no non-fight).
        check(!WardenGiveUp.due(0, 0, 0, 0, 999_999_999, W),
                "zero damage never gives up");
        check(!WardenGiveUp.due(500, 0, 0, 0, 999_999_999, W),
                "no first-dealt stamp never gives up");

        // The Oct 22 shape: excluded-only fire from t=10s.
        check(!WardenGiveUp.due(900, 10_000, 0, 0, 10_000 + W - 1, W),
                "excluded-only: not yet at window - 1ms");
        check(WardenGiveUp.due(900, 10_000, 0, 0, 10_000 + W, W),
                "excluded-only: gives up exactly at the window");

        // A counted blow restarts the clock from ITS moment.
        check(!WardenGiveUp.due(900, 10_000, 30_000, 0, 30_000 + W - 1, W),
                "counted blow resets: not yet 45s after it");
        check(WardenGiveUp.due(900, 10_000, 30_000, 0, 30_000 + W, W),
                "counted blow resets: gives up 45s after it");

        // Unattributed damage after a counted blow does NOT
        // restart anything (its stamp is not an input at all -
        // the same stamps without it give the same answer).
        check(WardenGiveUp.due(900, 10_000, 30_000, 0, 30_000 + W, W)
                        == WardenGiveUp.due(960, 10_000, 30_000, 0, 30_000 + W, W),
                "unattributed damage never moves the clock");

        // The warden's own landed blow is fight evidence.
        check(!WardenGiveUp.due(900, 10_000, 0, 40_000, 40_000 + W - 1, W),
                "boss landed a blow: clock runs from the blow");
        check(WardenGiveUp.due(900, 10_000, 0, 40_000, 40_000 + W, W),
                "boss landed a blow: gives up 45s after it");

        // The LATEST evidence wins when several kinds exist.
        check(!WardenGiveUp.due(900, 10_000, 20_000, 35_000, 35_000 + W - 1, W),
                "latest evidence (taken after counted) starts the clock");
        check(WardenGiveUp.due(900, 10_000, 20_000, 35_000, 35_000 + W, W),
                "latest evidence: gives up 45s after it");
    }

    // ----------------------------------------------------------
    // 1b. The ledger stamps the predicate reads
    // ----------------------------------------------------------

    private static void giveUpLedger() {
        UUID boss = UUID.randomUUID();
        UUID perch = UUID.randomUUID();

        // A fresh ledger has dealt nothing and can never be due.
        EncounterLedger fresh = new EncounterLedger(BossKind.WARDEN, boss, 1300.0, 5_000L);
        check(fresh.firstDealtMs() == 0 && fresh.lastCountedMs() == 0
                        && fresh.lastTakenMs() == 0,
                "fresh ledger: all fight stamps empty");
        check(!fresh.wardenGiveUpDue(999_999_999L, W),
                "fresh ledger: never due");

        // The Oct 22 replay in ledger form: 261 band-excluded
        // hits (height-banded here), zero counted, zero taken.
        EncounterLedger l = new EncounterLedger(BossKind.WARDEN, boss, 1300.0, 0L);
        for (int i = 0; i < 261; i++) {
            l.recordDealtBanded(perch, 5.0, 10_000L + i * 100L, false, true);
        }
        eq(l.totalDamage(), 1305.0, "replay: every excluded hit still lands");
        eq(l.playerDamage(), 0.0, "replay: counted damage stays 0.0%");
        check(l.firstDealtMs() == 10_000L, "replay: clock starts at the first hit");
        check(l.lastCountedMs() == 0 && l.lastTakenMs() == 0,
                "replay: no fight evidence ever lands");
        check(!l.wardenGiveUpDue(10_000L + W - 1, W), "replay: not due at window - 1");
        check(l.wardenGiveUpDue(10_000L + W, W),
                "replay: due at the window - burrows instead of a 5th kill");

        // Mixed excluded -> counted resets the clock.
        EncounterLedger mixed = new EncounterLedger(BossKind.WARDEN, boss, 1300.0, 0L);
        mixed.recordDealtBanded(perch, 9.0, 10_000L, false, true); // excluded
        mixed.recordDealtBanded(perch, 9.0, 30_000L, true, true);  // counted
        check(mixed.lastCountedMs() == 30_000L, "mixed: counted hit stamps the clock");
        check(!mixed.wardenGiveUpDue(30_000L + W - 1, W),
                "mixed: not due 45s after the FIRST hit - the reset held");
        check(mixed.wardenGiveUpDue(30_000L + W, W),
                "mixed: due 45s after the counted hit");

        // Environmental damage does not reset: unattributed
        // records after the counted hit leave its stamp alone.
        EncounterLedger env = new EncounterLedger(BossKind.WARDEN, boss, 1300.0, 0L);
        env.recordDealt(perch, 9.0, 10_000L, true);        // counted
        env.recordDealt(null, 60.0, 50_000L);               // environment
        env.recordDealtBanded(perch, 9.0, 54_000L, false, false); // excluded
        check(env.lastCountedMs() == 10_000L,
                "environment + excluded hits never restamp the counted clock");
        check(env.wardenGiveUpDue(10_000L + W, W),
                "due 45s after the counted hit despite later noise");

        // A re-adopted wounded warden (a FRESH ledger over a
        // wounded boss - the E4a shape) starts a clean clock:
        // no stale stamps, no instant burrow, no cooldown facts
        // live in a ledger at all.
        EncounterLedger adopted = new EncounterLedger(BossKind.WARDEN, boss, 1300.0, 0L);
        adopted.recordDealt(null, 3.3, 200_000L); // one unattributed nick
        check(adopted.firstDealtMs() == 200_000L && adopted.lastCountedMs() == 0,
                "re-adopted: clean stamps, clock from the new first hit");
        check(!adopted.wardenGiveUpDue(200_000L + W - 1, W),
                "re-adopted: no instant burrow from a stale clock");

        // The warden landing a blow restarts via recordTaken.
        EncounterLedger fought = new EncounterLedger(BossKind.WARDEN, boss, 1300.0, 0L);
        fought.recordDealtBanded(perch, 9.0, 10_000L, false, true);
        fought.recordTaken(perch, 12.0, 40_000L);
        check(fought.lastTakenMs() == 40_000L, "boss blow stamps the taken clock");
        check(!fought.wardenGiveUpDue(40_000L + W - 1, W),
                "boss blow: not due inside its window");
        check(fought.wardenGiveUpDue(40_000L + W, W),
                "boss blow: due a full window after it");

        // Window 0 disables at the ledger level too.
        check(!l.wardenGiveUpDue(999_999_999L, 0), "ledger: window 0 never due");
    }

    // ----------------------------------------------------------
    // 2a. Placement registry - end crystals
    // ----------------------------------------------------------

    private static void registryCrystals() {
        PlacementRegistry r = new PlacementRegistry();
        UUID crystal = UUID.randomUUID();
        UUID other = UUID.randomUUID();
        UUID placerA = UUID.randomUUID();
        UUID mike = UUID.randomUUID();

        check(r.crystalPlacer(crystal) == null,
                "unregistered crystal (a natural End pillar) attributes nobody");
        check(r.crystalPlacer(null) == null, "null crystal attributes nobody");

        r.recordCrystal(crystal, placerA, 1_000L);
        check(placerA.equals(r.crystalPlacer(crystal)), "registered crystal names its placer");
        check(r.crystalPlacer(other) == null, "a different crystal stays unregistered");
        check(r.crystalCount() == 1, "one crystal held");

        // Whoever detonates it is irrelevant: the entry names
        // the placer until the crystal is gone.
        r.forgetCrystal(crystal);
        check(r.crystalPlacer(crystal) == null,
                "exploded/broken crystal leaves the registry");
        check(r.crystalCount() == 0, "registry empty after forget");

        // Null placer can never register (a placement with no
        // player is a natural spawn, not a placement).
        r.recordCrystal(crystal, null, 2_000L);
        check(r.crystalPlacer(crystal) == null, "null placer never registers");

        // Age prune is leak insurance only: inside the cap the
        // entry survives, past it the entry goes.
        r.recordCrystal(crystal, mike, 10_000L);
        r.pruneCrystals(10_000L + 86_400_000L, 86_400_000L);
        check(mike.equals(r.crystalPlacer(crystal)), "entry at exactly the cap survives");
        r.pruneCrystals(10_000L + 86_400_001L, 86_400_000L);
        check(r.crystalPlacer(crystal) == null, "entry past the cap is pruned");
    }

    // ----------------------------------------------------------
    // 2b. Placement registry - respawn anchors
    // ----------------------------------------------------------

    private static void registryAnchors() {
        PlacementRegistry r = new PlacementRegistry();
        UUID placerA = UUID.randomUUID();
        UUID mike = UUID.randomUUID();

        check(r.anchorPlacer("world", 10, 64, -3) == null,
                "unregistered anchor attributes nobody");
        r.recordAnchor("world", 10, 64, -3, placerA, 1_000L);
        check(placerA.equals(r.anchorPlacer("world", 10, 64, -3)),
                "registered anchor names its placer");
        check(r.anchorPlacer("world", 11, 64, -3) == null,
                "the next block over is a different anchor");
        check(r.anchorPlacer("world_nether", 10, 64, -3) == null,
                "same coords in another world are a different anchor");

        // Re-placing the same spot (break + replace) overwrites.
        r.recordAnchor("world", 10, 64, -3, mike, 2_000L);
        check(mike.equals(r.anchorPlacer("world", 10, 64, -3)),
                "re-placement overwrites the placer");

        r.forgetAnchor("world", 10, 64, -3);
        check(r.anchorPlacer("world", 10, 64, -3) == null,
                "broken anchor leaves the registry");
        check(r.anchorCount() == 0, "no anchors held");

        r.recordAnchor(null, 1, 2, 3, placerA, 0L);
        r.recordAnchor("world", 1, 2, 3, null, 0L);
        check(r.anchorCount() == 0, "null world/placer never registers");
    }

    // ----------------------------------------------------------
    // 2c. Detonation log - the anchor blast resolver
    // ----------------------------------------------------------

    private static void registryDetonations() {
        PlacementRegistry r = new PlacementRegistry();
        UUID placerA = UUID.randomUUID();
        UUID mike = UUID.randomUUID();

        // The pure use rule behind the adapter's interact feed:
        // a use charges when it is glowstone below the cap;
        // every other use of a charged anchor detonates it
        // where anchors explode; the nether use never does.
        check(PlacementRegistry.anchorUseDetonates(false, 1, 4, true),
                "anchorUse: empty-hand use of a charged anchor detonates");
        check(!PlacementRegistry.anchorUseDetonates(true, 1, 4, true),
                "anchorUse: glowstone below the cap charges instead");
        check(PlacementRegistry.anchorUseDetonates(true, 4, 4, true),
                "anchorUse: glowstone at the cap detonates");
        check(!PlacementRegistry.anchorUseDetonates(false, 0, 4, true),
                "anchorUse: an uncharged anchor never detonates");
        check(!PlacementRegistry.anchorUseDetonates(false, 4, 4, false),
                "anchorUse: the nether use never detonates");
        check(PlacementRegistry.anchorUseDetonates(false, 3, 4, true),
                "anchorUse: a sword use of a charged anchor detonates");

        // Nothing registered, nothing detonated: a natural
        // explosion near a boss stays unattributed.
        check(r.anchorDetonationNear("world", 0, 70, 0, 5_000L) == null,
                "no detonation recorded: blast attributes nobody");

        // PlayerC's anchor blows at (0, 70, 0), t=5000. A boss 8
        // blocks off takes its blast the same tick.
        r.recordAnchorDetonation(placerA, "world", 0.5, 70.5, 0.5, 5_000L);
        PlacementRegistry.Detonation d =
                r.anchorDetonationNear("world", 8.5, 70.5, 0.5, 5_000L);
        check(d != null && placerA.equals(d.placer()),
                "same-tick blast near the anchor names the placer");
        check(d != null && Math.abs(d.x() - 0.5) < 1e-9 && Math.abs(d.z() - 0.5) < 1e-9,
                "the detonation reports the blast's own position");

        // Window edges: exactly 1000 ms counts, 1001 does not;
        // before the detonation never counts.
        check(r.anchorDetonationNear("world", 8.5, 70.5, 0.5, 6_000L) != null,
                "blast at the window edge (1000 ms) still names her");
        check(r.anchorDetonationNear("world", 8.5, 70.5, 0.5, 6_001L) == null,
                "blast past the window is unattributed");
        check(r.anchorDetonationNear("world", 8.5, 70.5, 0.5, 4_999L) == null,
                "a blast BEFORE the detonation is not hers");

        // Radius edges: exactly 12 counts, beyond does not.
        check(r.anchorDetonationNear("world", 12.5, 70.5, 0.5, 5_000L) != null,
                "blast at exactly 12 blocks counts");
        check(r.anchorDetonationNear("world", 13.0, 70.5, 0.5, 5_000L) == null,
                "blast beyond 12 blocks is unattributed");

        // World must match.
        check(r.anchorDetonationNear("world_nether", 8.5, 70.5, 0.5, 5_000L) == null,
                "a detonation in another world explains nothing here");

        // Two anchors blow in the same window: the most recent
        // one explains the blast.
        r.recordAnchorDetonation(mike, "world", 2.5, 70.5, 0.5, 5_400L);
        PlacementRegistry.Detonation d2 =
                r.anchorDetonationNear("world", 8.5, 70.5, 0.5, 5_500L);
        check(d2 != null && mike.equals(d2.placer()),
                "two detonations in range: the most recent wins");

        // Prune: far outside the window the entries are gone.
        r.pruneDetonations(5_400L + PlacementRegistry.DETONATION_PRUNE_MS + 1);
        check(r.detonationCount() == 0, "stale detonations are pruned");

        // Clear (admin reset) empties every store.
        r.recordCrystal(UUID.randomUUID(), placerA, 0L);
        r.recordAnchor("world", 1, 2, 3, placerA, 0L);
        r.recordAnchorDetonation(placerA, "world", 0, 0, 0, 0L);
        r.clear();
        check(r.crystalCount() == 0 && r.anchorCount() == 0 && r.detonationCount() == 0,
                "clear empties crystals, anchors, and detonations");
    }

    // ----------------------------------------------------------
    // 2d. Config: the give-up window key
    // ----------------------------------------------------------

    private static void configKeys() {
        eq(BossTrialConfig.defaults().wardenGiveUpWindowMs, 45_000.0,
                "default: give-up window is the settled 45 s");
        eq(BossTrialConfig.fromMap(Map.of()).wardenGiveUpWindowMs, 45_000.0,
                "carried-over config (no key): 45 s from code");
        eq(BossTrialConfig.fromMap(Map.of(
                        "warden.give-up-window-seconds", 90)).wardenGiveUpWindowMs,
                90_000.0, "config 90 s reads as 90,000 ms");
        eq(BossTrialConfig.fromMap(Map.of(
                        "warden.give-up-window-seconds", 0)).wardenGiveUpWindowMs,
                0.0, "config 0 disables the rule");
        eq(BossTrialConfig.fromMap(Map.of(
                        "warden.give-up-window-seconds", -30)).wardenGiveUpWindowMs,
                0.0, "negative window clamps to disabled, never crashes");
        eq(BossTrialConfig.fromMap(Map.of(
                        "warden.give-up-window-seconds", 999_999)).wardenGiveUpWindowMs,
                3_600_000.0, "absurd window clamps to one hour");
    }
}

// ExcelBossTrial - a Wink Reyes Valley Excel plugin.

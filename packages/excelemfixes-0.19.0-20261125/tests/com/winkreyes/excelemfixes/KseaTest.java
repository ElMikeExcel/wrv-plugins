import excelemfixes.mcmmo.AwardRates;
import excelemfixes.mcmmo.CeilingLedger;
import excelemfixes.mcmmo.DummyFiles;
import excelemfixes.mcmmo.HitTokens;
import excelemfixes.mcmmo.WatchdogLedger;
import excelemfixes.mcmmo.WeaponSkills;
import java.util.List;
import java.util.UUID;

/**
 * mcMMO XP Connector pure logic: award pricing, the hourly
 * ceiling ledger, hit-token provenance, weapon-skill routing,
 * the watchdog's rolling gain ledger, and the
 * training-dummy matcher.
 */
public final class KseaTest {

    private static final UUID ALICE =
            UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID BOB =
            UUID.fromString("22222222-2222-2222-2222-222222222222");

    public static void run() {
        System.out.println("KseaTest");
        awardPricing();
        ceilingLedger();
        hitTokens();
        weaponSkills();
        watchdogLedger();
        dummyFiles();
    }

    private static void awardPricing() {
        AwardRates.Rates rates = AwardRates.Rates.defaults();
        // The design benchmarks at the approved rates.
        Check.eq(1_000, AwardRates.award(
                AwardRates.RankClass.BOSS, 10, rates),
                "Lv.10 BOSS kill pays 1,000");
        Check.eq(10_000, AwardRates.award(
                AwardRates.RankClass.BOSS, 100, rates),
                "Lv.100 BOSS kill pays 10,000");
        Check.eq(400, AwardRates.award(
                AwardRates.RankClass.MINIBOSS, 10, rates),
                "Lv.10 MINIBOSS kill pays 400");
        Check.eq(200, AwardRates.award(
                AwardRates.RankClass.NATURAL, 10, rates),
                "Lv.10 natural elite pays 200");
        Check.eq(3_000, AwardRates.award(
                AwardRates.RankClass.NATURAL, 150, rates),
                "Lv.150 natural elite pays 3,000");
        Check.eq(100, AwardRates.award(
                AwardRates.RankClass.TRASH, 10, rates),
                "Lv.10 TRASH kill pays 100");
        // Fail-low edges.
        Check.eq(0, AwardRates.award(
                AwardRates.RankClass.BOSS, 0, rates),
                "level 0 pays 0 (unreadable level fails low)");
        Check.eq(0, AwardRates.award(
                AwardRates.RankClass.BOSS, -3, rates),
                "negative level pays 0");
        Check.eq(AwardRates.RankClass.BOSS,
                AwardRates.fromEmRankName("BOSS"), "rank BOSS maps");
        Check.eq(AwardRates.RankClass.MINIBOSS,
                AwardRates.fromEmRankName("miniboss"),
                "rank name matching ignores case");
        Check.eq(AwardRates.RankClass.TRASH,
                AwardRates.fromEmRankName("AUTO"),
                "unresolved rank name prices TRASH (fail-low)");
        Check.eq(AwardRates.RankClass.TRASH,
                AwardRates.fromEmRankName(null),
                "null rank prices TRASH (fail-low)");
        Check.eq(AwardRates.RankClass.TRASH,
                AwardRates.fromEmRankName("GIGA"),
                "unknown rank prices TRASH (fail-low)");
    }

    private static void ceilingLedger() {
        CeilingLedger ledger = new CeilingLedger();
        long t0 = 1_700_000_000_000L;
        Check.eq(1_000, ledger.fit(ALICE, "SWORDS", 1_000, 60_000, t0),
                "full award fits under an empty ceiling");
        ledger.debit(ALICE, "SWORDS", 59_500, t0);
        Check.eq(500, ledger.fit(ALICE, "SWORDS", 1_000, 60_000, t0),
                "award is fitted to what remains under the ceiling");
        ledger.debit(ALICE, "SWORDS", 500, t0);
        Check.eq(0, ledger.fit(ALICE, "SWORDS", 1_000, 60_000, t0),
                "ceiling reached: nothing fits (hard stop)");
        Check.close(60_000.0, ledger.grantedInWindow(ALICE, "SWORDS", t0),
                "window total is the granted sum");
        // Per-skill and per-player independence.
        Check.eq(1_000, ledger.fit(ALICE, "AXES", 1_000, 60_000, t0),
                "ceiling is per skill");
        Check.eq(1_000, ledger.fit(BOB, "SWORDS", 1_000, 60_000, t0),
                "ceiling is per player");
        // Post-multiplier counting: a grant observed at 1.5x debits 1.5x.
        CeilingLedger scaled = new CeilingLedger();
        scaled.debit(ALICE, "SWORDS", 1_500, t0);
        Check.close(1_500.0, scaled.grantedInWindow(ALICE, "SWORDS", t0),
                "ledger counts the post-multiplier granted amount");
        // The window rolls: an hour later the old grants fall out.
        long t1 = t0 + CeilingLedger.WINDOW_MS + 1;
        Check.eq(1_000, ledger.fit(ALICE, "SWORDS", 1_000, 60_000, t1),
                "window rolls after 60 minutes");
        // Persistence round-trip.
        CeilingLedger source = new CeilingLedger();
        source.debit(ALICE, "SWORDS", 1_000, t0);
        source.debit(ALICE, "TAMING", 250.5, t0);
        source.debit(BOB, "ARCHERY", 42, t0);
        CeilingLedger loaded =
                CeilingLedger.deserialize(source.serialize(t0));
        Check.close(1_000.0, loaded.grantedInWindow(ALICE, "SWORDS", t0),
                "ledger survives a serialize round-trip (restart)");
        Check.close(250.5, loaded.grantedInWindow(ALICE, "TAMING", t0),
                "fractional (post-multiplier) amounts persist exactly");
        Check.close(42.0, loaded.grantedInWindow(BOB, "ARCHERY", t0),
                "second player's entries persist");
        // Malformed lines are skipped, never fatal.
        CeilingLedger messy = CeilingLedger.deserialize(
                "garbage line\n" + ALICE + "\tSWORDS\t" + t0 + "\t10\n"
                        + "\t\t\t\n" + ALICE + "\tSWORDS\tNaN\t5\n");
        Check.close(10.0, messy.grantedInWindow(ALICE, "SWORDS", t0),
                "malformed ledger lines are skipped");
        // Stale entries are dropped on save.
        CeilingLedger stale = new CeilingLedger();
        stale.debit(ALICE, "SWORDS", 999, t0);
        CeilingLedger afterSave = CeilingLedger.deserialize(
                stale.serialize(t0 + CeilingLedger.WINDOW_MS + 1));
        Check.close(0.0, afterSave.grantedInWindow(ALICE, "SWORDS",
                t0 + CeilingLedger.WINDOW_MS + 1),
                "entries outside the window are dropped on save");
    }

    private static void hitTokens() {
        HitTokens tokens = new HitTokens();
        tokens.plant(ALICE, "SWORDS", 100, UUID.randomUUID(), 8);
        Check.ok(tokens.consumeForGain(ALICE, "SWORDS", 103, 6),
                "a gain within the TTL matches its hit token");
        Check.ok(!tokens.consumeForGain(ALICE, "SWORDS", 103, 6),
                "a consumed token cannot match twice");
        tokens.plant(ALICE, "SWORDS", 100, UUID.randomUUID(), 8);
        Check.ok(!tokens.consumeForGain(ALICE, "SWORDS", 107, 6),
                "a gain after the TTL does not match (token expired)");
        tokens.plant(ALICE, "SWORDS", 100, UUID.randomUUID(), 8);
        Check.ok(!tokens.consumeForGain(ALICE, "AXES", 101, 6),
                "tokens are skill-keyed (Axes gain ignores a Swords hit)");
        Check.ok(!tokens.consumeForGain(BOB, "SWORDS", 101, 6),
                "tokens are earner-keyed");
        // FIFO: two hits, two gains, oldest consumed first.
        HitTokens fifo = new HitTokens();
        fifo.plant(ALICE, "SWORDS", 100, UUID.randomUUID(), 8);
        fifo.plant(ALICE, "SWORDS", 101, UUID.randomUUID(), 8);
        Check.ok(fifo.consumeForGain(ALICE, "SWORDS", 102, 6),
                "FIFO: first gain consumes a token");
        Check.ok(fifo.consumeForGain(ALICE, "SWORDS", 102, 6),
                "FIFO: second gain consumes the second token");
        Check.ok(!fifo.consumeForGain(ALICE, "SWORDS", 102, 6),
                "FIFO: third gain has nothing to match");
        // Shares: candidates carry budget; spending does not consume.
        HitTokens shares = new HitTokens();
        UUID target = UUID.randomUUID();
        shares.plant(ALICE, "SWORDS", 100, target, 2);
        List<HitTokens.Token> candidates =
                shares.shareCandidates("SWORDS", 101, 6);
        Check.eq(1, candidates.size(), "share candidates find the token");
        shares.spendShareBudget(candidates.get(0));
        shares.spendShareBudget(candidates.get(0));
        Check.eq(0, shares.shareCandidates("SWORDS", 101, 6).size(),
                "share budget exhausts (bounded shares per hit)");
        Check.ok(shares.consumeForGain(ALICE, "SWORDS", 102, 6),
                "the earner's own gain still matches after share vetoes");
    }

    private static void weaponSkills() {
        Check.eq("SWORDS", WeaponSkills.byMaterial("DIAMOND_SWORD"),
                "sword -> SWORDS");
        Check.eq("AXES", WeaponSkills.byMaterial("IRON_AXE"),
                "axe -> AXES");
        Check.eq("MACES", WeaponSkills.byMaterial("MACE"),
                "mace -> MACES");
        Check.eq("SPEARS", WeaponSkills.byMaterial("NETHERITE_SPEAR"),
                "spear -> SPEARS");
        Check.eq("TRIDENTS", WeaponSkills.byMaterial("TRIDENT"),
                "trident in hand -> TRIDENTS");
        Check.eq("ARCHERY", WeaponSkills.byMaterial("BOW"),
                "bow -> ARCHERY");
        Check.eq("CROSSBOWS", WeaponSkills.byMaterial("CROSSBOW"),
                "crossbow -> CROSSBOWS");
        Check.eq(null, WeaponSkills.byMaterial("BLAZE_ROD"),
                "a wand is not an mcMMO weapon (null; caller falls back)");
        Check.eq("UNARMED", WeaponSkills.forMelee("BLAZE_ROD"),
                "non-weapon in hand prices UNARMED (mcMMO's own rule)");
        Check.eq("UNARMED", WeaponSkills.forMelee("AIR"),
                "empty hand prices UNARMED");
        Check.eq("ARCHERY", WeaponSkills.byProjectile("ARROW"),
                "arrow -> ARCHERY");
        Check.eq("TRIDENTS", WeaponSkills.byProjectile("TRIDENT"),
                "thrown trident -> TRIDENTS");
        Check.eq("CROSSBOWS", WeaponSkills.forShot("CROSSBOW", "ARROW"),
                "a crossbow's arrow prices CROSSBOWS (weapon wins)");
        Check.eq("ARCHERY", WeaponSkills.forShot("BOW", "ARROW"),
                "a bow's arrow prices ARCHERY");
    }

    private static void watchdogLedger() {
        WatchdogLedger ledger = new WatchdogLedger();
        long t0 = 1_000_000_000L;
        Check.eq(0.0, ledger.totalLast(ALICE, "SWORDS", t0,
                WatchdogLedger.HOUR_MS), "watchdog: empty totals 0");
        ledger.record(ALICE, "SWORDS", t0, 30_000);
        ledger.record(ALICE, "SWORDS", t0 + 10_000, 30_000);
        ledger.record(ALICE, "AXES", t0 + 10_000, 5_000);
        ledger.record(BOB, "SWORDS", t0 + 10_000, 7_500);
        Check.eq(60_000.0, ledger.totalLast(ALICE, "SWORDS", t0 + 20_000,
                WatchdogLedger.MINUTE_MS),
                "watchdog: minute window sums the player's skill only");
        Check.eq(60_000.0, ledger.totalLast(ALICE, "SWORDS", t0 + 20_000,
                WatchdogLedger.HOUR_MS), "watchdog: hour window sums");
        Check.eq(5_000.0, ledger.totalLast(ALICE, "AXES", t0 + 20_000,
                WatchdogLedger.HOUR_MS), "watchdog: skills are separate");
        Check.eq(7_500.0, ledger.totalLast(BOB, "SWORDS", t0 + 20_000,
                WatchdogLedger.HOUR_MS), "watchdog: players are separate");
        // Roll-off: the first gain ages out of the minute window
        // while staying inside the hour; both age out of the hour.
        Check.eq(30_000.0, ledger.totalLast(ALICE, "SWORDS", t0 + 61_000,
                WatchdogLedger.MINUTE_MS),
                "watchdog: minute window rolls off the oldest gain");
        Check.eq(60_000.0, ledger.totalLast(ALICE, "SWORDS", t0 + 61_000,
                WatchdogLedger.HOUR_MS),
                "watchdog: hour window still holds both gains");
        // Non-positive and null gains are ignored.
        ledger.record(ALICE, "SWORDS", t0, 0);
        ledger.record(ALICE, "SWORDS", t0, -50);
        ledger.record(null, "SWORDS", t0, 100);
        Check.eq(60_000.0, ledger.totalLast(ALICE, "SWORDS", t0 + 20_000,
                WatchdogLedger.HOUR_MS),
                "watchdog: junk records change nothing");
        // Persistence: serialize -> deserialize keeps the totals.
        String saved = ledger.serialize(t0 + 20_000);
        WatchdogLedger restored = WatchdogLedger.deserialize(saved);
        Check.eq(60_000.0, restored.totalLast(ALICE, "SWORDS", t0 + 20_000,
                WatchdogLedger.HOUR_MS),
                "watchdog: serialize round-trips the totals");
        Check.eq(0.0, restored.totalLast(ALICE, "SWORDS",
                t0 + WatchdogLedger.HOUR_MS + 20_000,
                WatchdogLedger.HOUR_MS),
                "watchdog: restored entries still roll off");
        // (Checked last: reading at a far-future time prunes the
        // live ledger's entries, by design.)
        Check.eq(0.0, ledger.totalLast(ALICE, "SWORDS",
                t0 + WatchdogLedger.HOUR_MS + 20_000,
                WatchdogLedger.HOUR_MS),
                "watchdog: hour window rolls off completely");
        // Hit-token flag context counter.
        HitTokens tokens = new HitTokens();
        tokens.plant(ALICE, "SWORDS", 100L, BOB, 8);
        tokens.plant(ALICE, "SWORDS", 101L, BOB, 8);
        tokens.plant(ALICE, "AXES", 101L, BOB, 8);
        Check.eq(2, tokens.liveCountFor(ALICE, "SWORDS", 102L, 6L),
                "tokens: liveCountFor counts the player's skill");
        Check.eq(0, tokens.liveCountFor(ALICE, "SWORDS", 1_000L, 6L),
                "tokens: liveCountFor respects the TTL");
    }

    private static void dummyFiles() {
        Check.ok(DummyFiles.isTrainingDummyFilename(
                "training_dummy_lv1.yml"), "guild dummy lv1 matches");
        Check.ok(DummyFiles.isTrainingDummyFilename(
                "training_dummy_lv100.yml"), "guild dummy lv100 matches");
        Check.ok(DummyFiles.isTrainingDummyFilename(
                "adventurers_guild/training_dummy_lv50.yml"),
                "directory prefixes are ignored");
        Check.ok(DummyFiles.isTrainingDummyFilename(
                "TRAINING_DUMMY_LV75.YML"), "matching ignores case");
        Check.ok(DummyFiles.isTrainingDummyFilename(
                "damage_test_dummy.yml"), "the damage-test dummy matches");
        Check.ok(!DummyFiles.isTrainingDummyFilename(
                "training_dummy.yml"), "a generic training_dummy is not the set");
        Check.ok(!DummyFiles.isTrainingDummyFilename(
                "primis_boss.yml"), "ordinary bosses do not match");
        Check.ok(!DummyFiles.isTrainingDummyFilename(null),
                "null filename does not match");
    }
}

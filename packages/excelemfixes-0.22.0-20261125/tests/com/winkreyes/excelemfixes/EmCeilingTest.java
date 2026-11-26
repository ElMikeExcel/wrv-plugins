import excelemfixes.mcmmo.EmWorlds;
import excelemfixes.mcmmo.WatchdogLedger;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * EM-world gain ceiling pure logic (mcmmo-xp-connector part 5):
 * the EliteMobs world classifier and the headroom arithmetic
 * the engine performs over its ceiling ledger. The engine's
 * own wiring (KseaEngine.emWorldHeadroom over the live
 * settings) is exercised on the rig; here the classifier and
 * the ledger math are pinned directly, in the same shape as
 * the watchdog-ledger checks in KseaTest.
 */
public final class EmCeilingTest {

    private static final UUID ALICE =
            UUID.fromString("11111111-1111-1111-1111-111111111111");

    /** The skills the shipped config covers. */
    private static final Set<String> COVERED = Set.of("ACROBATICS");

    public static void run() {
        System.out.println("EmCeilingTest");
        emWorlds();
        emHeadroom();
    }

    /**
     * Mirrors KseaEngine.emWorldHeadroom's decision over a
     * bare ledger: -1 when the ceiling does not cover the
     * combination, else the remaining rolling-hour budget.
     */
    private static double headroom(WatchdogLedger ledger, UUID player,
                                   String skill, String worldName,
                                   int cap, long now) {
        if (!COVERED.contains(skill)
                || !EmWorlds.isEmWorld(worldName, List.of(), List.of())) {
            return -1.0;
        }
        double used = ledger.totalLast(player, skill, now,
                WatchdogLedger.HOUR_MS);
        return Math.max(0.0, cap - used);
    }

    private static void emWorlds() {
        // Built-in matches.
        Check.ok(EmWorlds.isEmWorld("em_primis"),
                "emworlds: em_primis is an EM world");
        Check.ok(EmWorlds.isEmWorld("em_adventurers_guild"),
                "emworlds: em_adventurers_guild is an EM world");
        Check.ok(EmWorlds.isEmWorld("em_id_primis_wolf_den_1"),
                "emworlds: instanced em_id_ copy is an EM world");
        Check.ok(EmWorlds.isEmWorld("GoblinKingdom_vf"),
                "emworlds: GoblinKingdom_vf is an EM world");
        Check.ok(EmWorlds.isEmWorld("goblinkingdom_vf_2"),
                "emworlds: Goblin Kingdom instance copy is an EM world");
        Check.ok(EmWorlds.isEmWorld("EM_PRIMIS"),
                "emworlds: matching is case-insensitive");
        // Near-misses that must NOT match.
        Check.ok(!EmWorlds.isEmWorld("world"),
                "emworlds: overworld is not an EM world");
        Check.ok(!EmWorlds.isEmWorld("world_nether"),
                "emworlds: nether is not an EM world");
        Check.ok(!EmWorlds.isEmWorld("em"),
                "emworlds: bare 'em' is not an EM world");
        Check.ok(!EmWorlds.isEmWorld("emx_world"),
                "emworlds: emx_world lookalike is not an EM world");
        Check.ok(!EmWorlds.isEmWorld("some_em_world"),
                "emworlds: em_ mid-name is not an EM world");
        Check.ok(!EmWorlds.isEmWorld(""),
                "emworlds: empty name never matches");
        Check.ok(!EmWorlds.isEmWorld(null),
                "emworlds: null never matches");
        // Config extras, case-insensitive both ways.
        Check.ok(EmWorlds.isEmWorld("Custom_Keep",
                        List.of("custom_keep"), List.of()),
                "emworlds: extra exact name matches case-insensitively");
        Check.ok(!EmWorlds.isEmWorld("custom_keep_2",
                        List.of("custom_keep"), List.of()),
                "emworlds: extra exact name does not prefix-match");
        Check.ok(EmWorlds.isEmWorld("arena_zone_1",
                        List.of(), List.of("Arena_")),
                "emworlds: extra prefix matches case-insensitively");
        Check.ok(!EmWorlds.isEmWorld("the_arena_zone",
                        List.of(), List.of("arena_")),
                "emworlds: extra prefix must anchor at the start");
    }

    private static void emHeadroom() {
        WatchdogLedger ledger = new WatchdogLedger();
        long t0 = 1_000_000_000L;
        int cap = 2_500;
        // Nothing recorded: full budget in an EM world.
        Check.eq(2_500.0, headroom(ledger, ALICE, "ACROBATICS",
                        "em_primis", cap, t0),
                "emceiling: fresh ledger leaves the full budget");
        // Feed 1,000 + 1,000 of in-world Acrobatics.
        ledger.record(ALICE, "ACROBATICS", t0, 1_000);
        ledger.record(ALICE, "ACROBATICS", t0 + 5_000, 1_000);
        Check.eq(500.0, headroom(ledger, ALICE, "ACROBATICS",
                        "em_primis", cap, t0 + 10_000),
                "emceiling: 1,000 + 1,000 against 2,500 leaves 500");
        // Not the ceiling's business: -1 both ways.
        Check.eq(-1.0, headroom(ledger, ALICE, "ACROBATICS",
                        "world", cap, t0 + 10_000),
                "emceiling: a non-EM world returns -1");
        Check.eq(-1.0, headroom(ledger, ALICE, "SWORDS",
                        "em_primis", cap, t0 + 10_000),
                "emceiling: an unlisted skill returns -1");
        // Spend the rest: headroom floors at 0, never negative.
        ledger.record(ALICE, "ACROBATICS", t0 + 6_000, 500);
        Check.eq(0.0, headroom(ledger, ALICE, "ACROBATICS",
                        "em_primis", cap, t0 + 10_000),
                "emceiling: a spent budget reads exactly 0");
        ledger.record(ALICE, "ACROBATICS", t0 + 7_000, 250);
        Check.eq(0.0, headroom(ledger, ALICE, "ACROBATICS",
                        "em_primis", cap, t0 + 10_000),
                "emceiling: an over-spent budget still floors at 0");
        // Persistence: serialize -> deserialize keeps the totals.
        String saved = ledger.serialize(t0 + 10_000);
        WatchdogLedger restored = WatchdogLedger.deserialize(saved);
        Check.eq(0.0, headroom(restored, ALICE, "ACROBATICS",
                        "em_primis", cap, t0 + 10_000),
                "emceiling: serialize round-trips the spent budget");
        // Malformed lines are skipped, never fatal.
        WatchdogLedger messy = WatchdogLedger.deserialize(
                "not a ledger line\n"
                        + ALICE + "\tACROBATICS\t" + t0 + "\t1000.0\n"
                        + "\t\t\t\n");
        Check.eq(1_500.0, headroom(messy, ALICE, "ACROBATICS",
                        "em_primis", cap, t0 + 10_000),
                "emceiling: malformed lines skipped on load");
        // Window roll: once every entry is older than the
        // rolling hour, the full budget is free again.
        Check.eq(2_500.0, headroom(ledger, ALICE, "ACROBATICS",
                        "em_primis", cap, t0 + WatchdogLedger.HOUR_MS + 7_001),
                "emceiling: the rolling hour frees the budget");
    }
}

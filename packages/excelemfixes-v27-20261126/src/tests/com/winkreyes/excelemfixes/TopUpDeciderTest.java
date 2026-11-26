import excelemfixes.witherskulls.TopUpDecider;
import excelemfixes.witherskulls.TopUpDecider.Verdict;

/**
 * Every edge case in the spec as a decider row, with pinned rolls:
 * decide(enabled, witherSkeleton, elite, playerKill,
 * skullAlreadyPresent, targetPercent, lootingLevel, rollPercent).
 */
public final class TopUpDeciderTest {

    private static Verdict d(boolean enabled, boolean wither, boolean elite, boolean player,
                             boolean skull, double target, int looting, double roll) {
        return TopUpDecider.decide(enabled, wither, elite, player, skull, target, looting, roll);
    }

    public static void run() {
        System.out.println("TopUpDeciderTest");

        // The happy path and its roll boundary.
        Check.eq(Verdict.ADD_SKULL, d(true, true, true, true, false, 25.0, 0, 10.0),
                "elite player kill, roll under top-up -> ADD");
        Check.eq(Verdict.ADD_SKULL, d(true, true, true, true, false, 25.0, 0, 22.4999),
                "roll just under 22.5 top-up -> ADD");
        Check.eq(Verdict.ROLL_MISS, d(true, true, true, true, false, 25.0, 0, 22.5),
                "roll == top-up chance -> MISS");
        Check.eq(Verdict.ROLL_MISS, d(true, true, true, true, false, 25.0, 0, 90.0),
                "high roll -> MISS");
        Check.eq(Verdict.ADD_SKULL, d(true, true, true, true, false, 25.0, 3, 19.4999),
                "Looting III: roll under 19.5 top-up -> ADD");
        Check.eq(Verdict.ROLL_MISS, d(true, true, true, true, false, 25.0, 3, 19.5),
                "Looting III: roll == 19.5 -> MISS");

        // SPEC EDGE: vanilla already dropped the skull — never two,
        // even on a roll that would have succeeded.
        Check.eq(Verdict.SKULL_ALREADY_PRESENT, d(true, true, true, true, true, 25.0, 0, 0.0),
                "skull already in drops, roll 0 -> no double");

        // SPEC EDGE: killer is not a player (dispenser projectile,
        // environment, another mob) — no boost, roll irrelevant.
        Check.eq(Verdict.NOT_PLAYER_KILL, d(true, true, true, false, false, 25.0, 0, 0.0),
                "elite wither, non-player kill, roll 0 -> no boost");

        // SPEC EDGE: elite but not a wither skeleton.
        Check.eq(Verdict.NOT_WITHER_SKELETON, d(true, false, true, true, false, 25.0, 0, 0.0),
                "elite zombie, player kill -> untouched");

        // SPEC EDGE: wither skeleton but not elite — the whole
        // point of the plugin is that regular ones stay vanilla.
        Check.eq(Verdict.NOT_ELITE, d(true, true, false, true, false, 25.0, 0, 0.0),
                "regular wither skeleton, player kill, roll 0 -> no boost");
        Check.eq(Verdict.NOT_ELITE, d(true, true, false, true, true, 25.0, 3, 0.0),
                "regular wither skeleton with skull present -> NOT_ELITE (plugin never involved)");

        // SPEC EDGE: target below vanilla's effective chance for
        // this kill — the plugin only ever adds.
        Check.eq(Verdict.TARGET_NOT_ABOVE_VANILLA, d(true, true, true, true, false, 3.0, 3, 0.0),
                "target 3 vs Looting III 5.5 -> no-op even on roll 0");
        Check.eq(Verdict.TARGET_NOT_ABOVE_VANILLA, d(true, true, true, true, false, 2.5, 0, 0.0),
                "target == vanilla base -> no-op");
        Check.eq(Verdict.TARGET_NOT_ABOVE_VANILLA, d(true, true, true, true, false, 0.0, 0, 0.0),
                "target 0 -> no-op");

        // Disabled beats everything.
        Check.eq(Verdict.DISABLED, d(false, true, true, true, false, 25.0, 0, 0.0),
                "disabled + perfect kill -> DISABLED");
        Check.eq(Verdict.DISABLED, d(false, false, false, false, false, 25.0, 0, 0.0),
                "disabled + nothing -> DISABLED first");

        // Verdict order pins (the glue and the debug log rely on
        // these): type before elite before player-kill; skull
        // present before the target comparison.
        Check.eq(Verdict.NOT_WITHER_SKELETON, d(true, false, false, false, false, 25.0, 0, 0.0),
                "non-wither non-elite -> type verdict first");
        Check.eq(Verdict.NOT_ELITE, d(true, true, false, false, false, 25.0, 0, 0.0),
                "non-elite non-player-kill -> elite verdict first");
        Check.eq(Verdict.SKULL_ALREADY_PRESENT, d(true, true, true, true, true, 3.0, 3, 0.0),
                "skull present beats target-below-vanilla");

        // Certainty at target 100: any roll in [0,100) adds.
        Check.eq(Verdict.ADD_SKULL, d(true, true, true, true, false, 100.0, 0, 97.4999),
                "target 100, roll 97.4999 -> ADD");
        Check.eq(Verdict.ROLL_MISS, d(true, true, true, true, false, 100.0, 0, 97.5),
                "target 100, roll == 97.5 top-up -> MISS (boundary is exact)");
        Check.eq(Verdict.ADD_SKULL, d(true, true, true, true, false, 100.0, 0, 0.0),
                "target 100, roll 0 -> ADD");
    }
}

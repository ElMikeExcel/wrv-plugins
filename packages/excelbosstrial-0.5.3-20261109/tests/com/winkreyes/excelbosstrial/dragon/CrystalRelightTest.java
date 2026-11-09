/* Excel WRV maintenance; existing authorship retained. */ package com.winkreyes.excelbosstrial.dragon;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Crystal re-light at the flip (Packet B, D-2): the pure
 * choice - bare towers only, the design count, the live
 * trial-crystal ceiling, and a reproducible random draw.
 */
public final class CrystalRelightTest {

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

    private static List<CrystalRelight.Tower> towers(int total, int... withCrystal) {
        Set<Integer> lit = new HashSet<>();
        for (int i : withCrystal) {
            lit.add(i);
        }
        List<CrystalRelight.Tower> out = new ArrayList<>();
        for (int i = 0; i < total; i++) {
            out.add(new CrystalRelight.Tower(i, lit.contains(i)));
        }
        return out;
    }

    public static void main(String[] args) {
        // The design case: 10 towers, 3 still lit by the
        // original fight, flip re-lights exactly 4 bare ones.
        List<Integer> picks = CrystalRelight.choose(
                towers(10, 1, 4, 7), 4, 6, 0, new Random(42));
        check(picks.size() == 4, "design case: exactly 4 re-lit (got " + picks.size() + ")");
        check(new HashSet<>(picks).size() == 4, "design case: no tower picked twice");
        check(!picks.contains(1) && !picks.contains(4) && !picks.contains(7),
                "design case: towers already carrying a crystal are never picked");

        // Fewer bare towers than the count: re-light what exists.
        check(CrystalRelight.choose(towers(10, 0, 1, 2, 3, 4, 5, 6, 7), 4, 6, 0,
                new Random(1)).size() == 2, "only 2 bare towers: exactly 2 re-lit");
        // No bare towers: nothing.
        check(CrystalRelight.choose(towers(3, 0, 1, 2), 4, 6, 0,
                new Random(1)).isEmpty(), "no bare towers: empty draw");
        // No towers at all / null: nothing, never an exception.
        check(CrystalRelight.choose(List.of(), 4, 6, 0, new Random(1)).isEmpty(),
                "no towers: empty draw");
        check(CrystalRelight.choose(null, 4, 6, 0, new Random(1)).isEmpty(),
                "null towers: empty draw");

        // The ceiling: 5 trial crystals already live of a 6
        // ceiling leaves room for exactly 1, however many bare.
        check(CrystalRelight.choose(towers(10), 4, 6, 5, new Random(7)).size() == 1,
                "ceiling: 5 live of 6 leaves room for 1");
        check(CrystalRelight.choose(towers(10), 4, 6, 6, new Random(7)).isEmpty(),
                "ceiling: 6 live of 6 re-lights nothing");
        check(CrystalRelight.choose(towers(10), 4, 6, 9, new Random(7)).isEmpty(),
                "ceiling: over-ceiling live count re-lights nothing (never negative)");

        // Count 0 retires the feature by config.
        check(CrystalRelight.choose(towers(10), 0, 6, 0, new Random(7)).isEmpty(),
                "count 0: the feature is off");

        // Sanitizers: junk reads as the design values, extremes clamp.
        check(CrystalRelight.sanitizedCount(-1) == CrystalRelight.DEFAULT_COUNT,
                "sanitize: negative count reads as the design 4");
        check(CrystalRelight.sanitizedCount(99) == 10, "sanitize: count clamps to 10");
        check(CrystalRelight.sanitizedMaxLive(-1) == CrystalRelight.DEFAULT_MAX_LIVE,
                "sanitize: negative ceiling reads as the design 6");
        check(CrystalRelight.sanitizedMaxLive(99) == 12, "sanitize: ceiling clamps to 12");

        // The draw is random but reproducible under a seed (the
        // rig's property): same seed, same picks; the SET of
        // possible picks is exactly the bare towers.
        List<Integer> a = CrystalRelight.choose(towers(10, 2), 4, 6, 0, new Random(99));
        List<Integer> b = CrystalRelight.choose(towers(10, 2), 4, 6, 0, new Random(99));
        check(a.equals(b), "draw: same seed reproduces the same picks");
        Set<Integer> seen = new HashSet<>();
        for (long seed = 0; seed < 200; seed++) {
            seen.addAll(CrystalRelight.choose(towers(10, 2), 1, 6, 0, new Random(seed)));
        }
        check(seen.size() == 9 && !seen.contains(2),
                "draw: across seeds every bare tower is reachable, the lit one never");

        System.out.println("CrystalRelightTest: " + passed + " passed, " + failed + " failed");
        if (failed > 0) {
            System.exit(1);
        }
    }
}

// ExcelBossTrial - a Wink Reyes Valley Excel plugin.

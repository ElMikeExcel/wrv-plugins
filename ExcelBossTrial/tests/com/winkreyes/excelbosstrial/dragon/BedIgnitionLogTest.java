package com.winkreyes.excelbosstrial.dragon;

import java.util.UUID;

/**
 * Bed-ignition attribution (0.2.1, triple-check FLAG 3): the
 * dragon share gate must not count sanctioned bed damage
 * against the family. A lit bed explodes as BLOCK_EXPLOSION
 * with no damager; the chapter's ignition log names the lighter
 * when the blast lands near the bed within a few ticks. These
 * checks pin the window/radius edges, the most-recent-wins
 * rule for two players' sequential beds, and the prune.
 */
public final class BedIgnitionLogTest {

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

    public static void main(String[] args) {
        UUID placerA = UUID.randomUUID();
        UUID mike = UUID.randomUUID();

        BedIgnitionLog log = new BedIgnitionLog();
        check(log.lighterNear(0, 70, 0, 100) == null, "empty log attributes nobody");

        // PlayerC lights the perch bed at tick 100; it blows the same
        // tick at the dragon's head, 6 blocks away.
        log.record(placerA, 3, 74, 4, 100);
        check(placerA.equals(log.lighterNear(0, 70, 0, 100)),
                "same-tick blast near the bed names the lighter");
        check(placerA.equals(log.lighterNear(0, 70, 0, 105)),
                "blast at the window's edge (5 ticks) still names her");
        check(log.lighterNear(0, 70, 0, 106) == null,
                "blast past the window (6 ticks) is unattributed");
        check(log.lighterNear(30, 70, 30, 100) == null,
                "blast far from every lit bed is unattributed");
        check(log.lighterNear(0, 70, 0, 99) == null,
                "a blast BEFORE the ignition is not hers");

        // Radius edges: exactly 10 counts, 10.1 does not.
        BedIgnitionLog r = new BedIgnitionLog();
        r.record(mike, 10, 64, 0, 200);
        check(mike.equals(r.lighterNear(0, 64, 0, 200)), "blast exactly 10 away counts");
        check(r.lighterNear(-0.1, 64, 0, 200) == null, "blast 10.1 away does not");

        // Two players, sequential beds near the same spot: the
        // most recent ignition wins, so each bed charges its own
        // lighter.
        BedIgnitionLog two = new BedIgnitionLog();
        two.record(placerA, 2, 70, 2, 300);
        two.record(mike, 3, 70, 1, 302);
        check(mike.equals(two.lighterNear(0, 70, 0, 303)),
                "of two lit beds, the most recent names the blast");
        check(placerA.equals(two.lighterNear(0, 70, 0, 300)),
                "before Mike's bed, the same blast was PlayerC's");

        // Prune: ignitions older than 40 ticks fall away on the
        // next record.
        BedIgnitionLog p = new BedIgnitionLog();
        p.record(placerA, 0, 64, 0, 0);
        p.record(mike, 0, 64, 0, 100);
        check(p.size() == 1, "recording prunes stale ignitions");

        // A null lighter never records.
        BedIgnitionLog n = new BedIgnitionLog();
        n.record(null, 0, 64, 0, 0);
        check(n.size() == 0, "null lighter records nothing");

        System.out.println("BedIgnitionLogTest: " + passed + " passed, " + failed + " failed");
        if (failed > 0) {
            System.exit(1);
        }
    }
}

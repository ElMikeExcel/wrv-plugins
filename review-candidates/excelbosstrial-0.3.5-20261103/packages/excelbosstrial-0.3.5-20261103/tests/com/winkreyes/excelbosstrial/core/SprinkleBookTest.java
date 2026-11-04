/* Excel WRV maintenance; existing authorship retained. */ package com.winkreyes.excelbosstrial.core;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Elite sprinkle registry (0.3.3, study section 6): per-encounter
 * add tracking, the max-per-fight accounting that survives adds
 * dying mid-fight, per-chapter independence (two bosses' books
 * never mix), and the cleanup contract - close returns exactly
 * the adds to remove and drains the entry; closeAll drains
 * everything (the verdict / prune / give-up / reset / disable
 * paths all ride these two calls).
 */
public final class SprinkleBookTest {

    private static int checks;
    private static int failures;

    private static void check(boolean ok, String label) {
        checks++;
        if (!ok) {
            failures++;
            System.out.println("FAIL: " + label);
        }
    }

    private static UUID id() {
        return UUID.randomUUID();
    }

    public static void main(String[] args) {
        SprinkleBook book = new SprinkleBook();
        UUID wither = id();
        UUID warden = id();
        UUID a1 = id(), a2 = id(), a3 = id(), b1 = id();

        check(book.isEmpty() && book.spawnedTotal(wither) == 0
                && book.close(wither).isEmpty(), "an unknown boss closes empty");

        // --- spawns register per encounter, independently
        book.recordSpawn(wither, a1);
        book.recordSpawn(wither, a2);
        book.recordSpawn(warden, b1);
        check(book.spawnedTotal(wither) == 2 && book.spawnedTotal(warden) == 1,
                "spawn totals are per-encounter");
        check(book.adds(wither).equals(Set.of(a1, a2))
                && book.adds(warden).equals(Set.of(b1)), "add sets are per-encounter");
        book.recordSpawn(wither, a1);
        check(book.spawnedTotal(wither) == 2,
                "re-recording the same add does not double-count the fight total");

        // --- alive counting and dead pruning keep the total
        check(book.aliveCount(wither, Set.of(a1)) == 1, "alive count reads the world set");
        check(book.pruneDead(wither, Set.of(a1)) == 1, "pruneDead reports the pruned add");
        check(book.adds(wither).equals(Set.of(a1)), "the dead add is forgotten");
        check(book.spawnedTotal(wither) == 2,
                "the spawned total SURVIVES its adds dying (cap accounting)");
        book.recordSpawn(wither, a3);
        check(book.spawnedTotal(wither) == 3 && book.adds(wither).equals(Set.of(a1, a3)),
                "later stamps keep counting against the same fight total");

        // --- close returns exactly the adds and drains the entry
        Set<UUID> drained = book.close(wither);
        check(drained.equals(Set.of(a1, a3)), "close returns exactly the live adds");
        check(book.spawnedTotal(wither) == 0 && book.adds(wither).isEmpty()
                && !book.openBosses().contains(wither), "close drains the encounter");
        check(book.adds(warden).equals(Set.of(b1)),
                "closing one encounter leaves the other untouched");

        // --- forget removes one add without touching the total
        book.forget(warden, b1);
        check(book.adds(warden).isEmpty() && book.spawnedTotal(warden) == 1,
                "forget drops the add, keeps the total");

        // --- closeAll drains everything (disable / reset)
        UUID elder = id(), c1 = id(), c2 = id();
        book.recordSpawn(warden, b1);
        book.recordSpawn(elder, c1);
        book.recordSpawn(elder, c2);
        Map<UUID, Set<UUID>> all = book.closeAll();
        check(all.get(warden).equals(Set.of(b1)) && all.get(elder).equals(Set.of(c1, c2)),
                "closeAll returns every encounter's adds");
        check(book.isEmpty() && book.openBosses().isEmpty(), "closeAll empties the book");

        System.out.println("SprinkleBookTest: " + checks + " checks, " + failures + " failures");
        if (failures > 0) {
            System.exit(1);
        }
    }
}

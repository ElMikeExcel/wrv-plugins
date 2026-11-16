/* Excel WRV maintenance; existing authorship retained. */ package com.winkreyes.excelbosstrial.core;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Counter seeding from the record (0.5.6; Mike, Nov 11, 2026):
 * Marvin + Jester at Elder clear #2 (next Elder pays $2,000);
 * the Wither's Nov 7 manual grant seeds NOTHING; Dragon/Raid
 * clear #1 already stands in the paid book, so no seed exists
 * for them either.
 */
public final class CounterSeedingTest {

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
        // The settled table of record.
        var seeds = CounterSeeding.settled();
        check(seeds.size() == 2, "exactly two settled seeds");
        check(seeds.stream().anyMatch(sd -> sd.playerName().equals("Marvin")
                && sd.sealId().equals("elder_guardian") && sd.lifetimeCount() == 2),
                "Marvin seeded at elder clear #2");
        check(seeds.stream().anyMatch(sd -> sd.playerName().equals("Jester")
                && sd.sealId().equals("elder_guardian") && sd.lifetimeCount() == 2),
                "Jester seeded at elder clear #2");
        check(seeds.stream().noneMatch(sd -> sd.sealId().equals("wither")),
                "no wither seed: the Nov 7 break was a manual grant, no paid clear #1");
        check(seeds.stream().noneMatch(sd -> sd.sealId().equals("ender_dragon")),
                "no dragon seed: clear #1 already stands in the paid book");
        check(seeds.stream().noneMatch(sd -> sd.sealId().equals("raid")),
                "no raid seed: clear #1 already stands in GuildGate's book");

        // MAX merge: raises, never lowers, idempotent.
        Map<String, Map<UUID, Integer>> counts = new HashMap<>();
        UUID marvin = UUID.randomUUID();
        String elder = BossKind.ELDER_GUARDIAN.sealId();
        check(CounterSeeding.applySeed(counts, elder, marvin, 2) == 2, "seed lands at 2");
        check(CounterSeeding.applySeed(counts, elder, marvin, 2) == 2, "seed re-apply is idempotent");
        counts.get(elder).put(marvin, 4);
        check(CounterSeeding.applySeed(counts, elder, marvin, 2) == 4, "seed never lowers a higher book count");
        check(CounterSeeding.applySeed(counts, elder, marvin, 0) == 4, "zero seed never lowers");

        // The seeded consequence: the next Elder clear is
        // lifetime #3 and pays the taper floor.
        Map<String, Map<UUID, Integer>> book = new HashMap<>();
        CounterSeeding.applySeed(book, elder, marvin, 2);
        int next = book.get(elder).get(marvin) + 1;
        check(next == 3, "seeded player's next elder clear is #3");
        check(CashTaper.cashFor(elder, next) == 2000L, "seeded player's next elder clear pays $2,000");
        // And a wither first PAID clear still pays clear #1,
        // because the manual grant seeded nothing.
        check(CashTaper.cashFor(BossKind.WITHER.sealId(), 1) == 5000L,
                "first PAID wither clear pays clear #1 $5,000");

        System.out.println("CounterSeedingTest: " + passed + " passed, " + failed + " failed");
        if (failed > 0) {
            System.exit(1);
        }
    }
}

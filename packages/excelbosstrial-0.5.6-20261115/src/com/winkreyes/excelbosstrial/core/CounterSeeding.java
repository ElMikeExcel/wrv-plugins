/* Excel WRV maintenance; existing authorship retained. */ package com.winkreyes.excelbosstrial.core;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * COUNTER SEEDING from the record (ExcelBossTrial 0.5.6; Mike,
 * Nov 11, 2026). The lifetime clear counts behind the cash taper
 * must start from what the record actually shows, not from zero:
 *
 * - Marvin and Jester stand at Elder clear #2 (the repeat slate
 *   listing of record, checked Nov 11, 2026): their NEXT Elder
 *   clear is lifetime clear #3 and pays the taper floor, $2,000.
 * - The Wither's Nov 7 break was a MANUAL GRANT: no paid clear
 *   #1 stands on record for anyone from it, so NO Wither seed
 *   exists — the first PAID Wither clear pays clear #1.
 * - Dragon and Raid parties have clear #1 used: those counts
 *   already stand in the live repeat-clears book (they were paid
 *   clears, recorded by the repeat layer itself), so they need
 *   no seed either — a seed only exists where the record and
 *   the book can disagree, which is the Elder pair.
 *
 * Seeds are keyed by UUID in state.yml's {@code counter-seeds}
 * section (written at install from the live usercache/state —
 * player NAMES are never resolved by this plugin, so a seed can
 * never land on a name-hash UUID). The merge rule is MAX: a
 * seed raises a count to at least the seeded value and never
 * lowers a count the book already holds, and applying a seed
 * is idempotent. The settled name table below is the review
 * surface: it names WHO the install-time UUID seeds are for.
 */
public final class CounterSeeding {

    /** One settled seed, by player name (review surface only —
     *  the applied seed is keyed by the player's real UUID). */
    public record Seed(String playerName, String sealId, int lifetimeCount) {
    }

    /** The settled seed table of record (Nov 11, 2026). */
    public static List<Seed> settled() {
        return List.of(
                new Seed("Marvin", BossKind.ELDER_GUARDIAN.sealId(), 2),
                new Seed("Jester", BossKind.ELDER_GUARDIAN.sealId(), 2));
    }

    private CounterSeeding() {
    }

    /**
     * Apply one UUID-keyed seed to a clear-count book
     * ({sealId -> {player -> lifetime count}}) under the MAX
     * rule.
     *
     * @return the count now standing for that player + seal
     *         (the seeded value, or the higher book value).
     */
    public static int applySeed(Map<String, Map<UUID, Integer>> clearCounts,
                                String sealId, UUID player, int seededCount) {
        if (clearCounts == null || sealId == null || player == null) {
            return 0;
        }
        int seed = Math.max(0, seededCount);
        Map<UUID, Integer> per = clearCounts.computeIfAbsent(sealId,
                k -> new java.util.HashMap<>());
        int current = per.getOrDefault(player, 0);
        int merged = Math.max(current, seed);
        per.put(player, merged);
        return merged;
    }
}

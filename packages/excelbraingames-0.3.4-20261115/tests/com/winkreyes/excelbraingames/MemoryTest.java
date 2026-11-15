package com.winkreyes.excelbraingames;

import com.winkreyes.excelbraingames.games.memory.MemoryRound;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/** Memory Match logic: deck integrity, flip/pair/miss/win, serialization. */
public final class MemoryTest {

    public static void main(String[] args) {
        Check ck = new Check("MemoryTest");
        deckIntegrity(ck);
        flipLogic(ck);
        missFlow(ck);
        winFlow(ck);
        serialization(ck);
        ck.finish();
    }

    static void deckIntegrity(Check ck) {
        for (int pairs : new int[]{6, 8, 12, 18}) {
            for (int seed = 0; seed < 250; seed++) {
                MemoryRound r = MemoryRound.deal("t", pairs, 18, new Random(seed * 31L + pairs), 0L);
                ck.eq(pairs * 2L, r.cardCount(), "card count for " + pairs + " pairs");
                Map<Integer, Integer> counts = new HashMap<>();
                for (int i = 0; i < r.cardCount(); i++) counts.merge(r.symbolAt(i), 1, Integer::sum);
                ck.eq(pairs, counts.size(), "distinct symbols for " + pairs + " pairs");
                ck.that(counts.values().stream().allMatch(n -> n == 2),
                        "each symbol exactly twice (" + pairs + " pairs, seed " + seed + ")");
            }
        }
    }

    static void flipLogic(Check ck) {
        MemoryRound r = MemoryRound.deal("standard", 8, 18, new Random(5), 0L);
        int a = 0;
        int b = mateOf(r, a);
        ck.that(r.flip(a) == MemoryRound.FlipResult.FIRST, "first flip is FIRST");
        ck.that(r.flip(a) == MemoryRound.FlipResult.IGNORED, "flipping the face-up card is IGNORED");
        ck.eq(1, r.flips(), "ignored flip does not count");
        ck.that(r.flip(b) == MemoryRound.FlipResult.MATCH, "flipping the mate is MATCH");
        ck.eq(1, r.matchedPairs(), "one pair matched");
        ck.that(r.flip(a) == MemoryRound.FlipResult.IGNORED, "matched card cannot flip again");
        ck.that(!r.isComplete(), "not complete after one pair");
        ck.that(r.flip(-1) == MemoryRound.FlipResult.IGNORED, "negative card ignored");
        ck.that(r.flip(999) == MemoryRound.FlipResult.IGNORED, "out-of-range card ignored");
    }

    static void missFlow(Check ck) {
        MemoryRound r = MemoryRound.deal("standard", 8, 18, new Random(11), 0L);
        int a = 0;
        int notMate = -1;
        for (int i = 1; i < r.cardCount(); i++) if (r.symbolAt(i) != r.symbolAt(a)) { notMate = i; break; }
        ck.that(r.flip(a) == MemoryRound.FlipResult.FIRST, "miss flow: first flip");
        ck.that(r.flip(notMate) == MemoryRound.FlipResult.MISS, "miss flow: second flip misses");
        ck.that(r.missPending(), "miss is pending");
        ck.that(r.stateAt(a) == MemoryRound.UP && r.stateAt(notMate) == MemoryRound.UP,
                "missed cards stay face-up during the pause");
        int c = (notMate + 1) % r.cardCount();
        if (r.stateAt(c) != MemoryRound.DOWN) c = (c + 1) % r.cardCount();
        ck.that(r.flip(c) == MemoryRound.FlipResult.IGNORED, "flips blocked while a miss shows");
        ck.that(r.resolveMiss(), "resolveMiss flips the pair back");
        ck.that(r.stateAt(a) == MemoryRound.DOWN && r.stateAt(notMate) == MemoryRound.DOWN,
                "missed cards are face-down after resolve");
        ck.that(!r.resolveMiss(), "second resolveMiss is a no-op");
        ck.eq(2, r.flips(), "miss cost exactly two flips");
    }

    static void winFlow(Check ck) {
        MemoryRound r = MemoryRound.deal("kid", 6, 18, new Random(3), 0L);
        boolean[] done = new boolean[r.cardCount()];
        for (int i = 0; i < r.cardCount(); i++) {
            if (done[i]) continue;
            int mate = mateOf(r, i);
            ck.that(r.flip(i) == MemoryRound.FlipResult.FIRST, "win flow first flip");
            ck.that(r.flip(mate) == MemoryRound.FlipResult.MATCH, "win flow match");
            done[i] = done[mate] = true;
        }
        ck.that(r.isComplete(), "round completes when all pairs matched");
        ck.eq(12, r.flips(), "perfect game is exactly 2 flips per pair");
        ck.eq(6, r.matchedPairs(), "all pairs counted");
    }

    static void serialization(Check ck) {
        MemoryRound r = MemoryRound.deal("big", 12, 18, new Random(8), 777L);
        int a = 0, mate = mateOf(r, a);
        r.flip(a); r.flip(mate);
        MemoryRound back = MemoryRound.fromMap(r.toMap());
        ck.that(back.tierId().equals("big"), "round trip: tier");
        ck.eq(777L, back.startedMillis(), "round trip: started");
        ck.eq(r.flips(), back.flips(), "round trip: flips");
        ck.eq(1, back.matchedPairs(), "round trip: matched pairs");
        ck.that(back.stateAt(a) == MemoryRound.MATCHED, "round trip: matched state kept");
        boolean deckSame = true;
        for (int i = 0; i < r.cardCount(); i++) if (back.symbolAt(i) != r.symbolAt(i)) deckSame = false;
        ck.that(deckSame, "round trip: deck order kept");
    }

    static int mateOf(MemoryRound r, int card) {
        for (int i = 0; i < r.cardCount(); i++) {
            if (i != card && r.symbolAt(i) == r.symbolAt(card)) return i;
        }
        throw new IllegalStateException("no mate");
    }
}

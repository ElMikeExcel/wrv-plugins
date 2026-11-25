/* Excel WRV maintenance; existing authorship retained. */ package com.winkreyes.teamrewards.ui;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 0.2.11 (B-2): the presented-set contract — a batch auto-presents
 * once, any open marks it presented, a later batch with a new claim
 * presents again, and the sidecar encoding round-trips (a damaged
 * sidecar degrades to "present once more", never to a lost reward).
 */
public final class ReviewPresentationTest {

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
        UUID ana = UUID.randomUUID();
        UUID bo = UUID.randomUUID();
        String run1 = "ender_dragon:world-uuid:boss-uuid";
        String claimA = run1 + "/" + ana;
        String claimB = "wither:world-uuid:boss-uuid/" + ana;

        ReviewPresentation p = new ReviewPresentation();

        // --- A fresh batch triggers; an empty pending list never does.
        check(p.hasUnpresented(ana, List.of(claimA)), "new claim triggers a present");
        check(!p.hasUnpresented(ana, List.of()), "empty pending never triggers");
        check(!p.hasUnpresented(ana, null), "null pending never triggers");

        // --- Marking (what ANY open does) makes the same batch quiet.
        p.markPresented(ana, List.of(claimA));
        check(!p.hasUnpresented(ana, List.of(claimA)), "the same batch never re-presents");
        p.markPresented(ana, List.of(claimA));
        check(!p.hasUnpresented(ana, List.of(claimA)), "marking is idempotent");

        // --- A later batch with one new ID triggers again; the old
        // ID riding along does not matter.
        check(p.hasUnpresented(ana, List.of(claimA, claimB)), "a batch with a new claim triggers");
        p.markPresented(ana, List.of(claimA, claimB));
        check(!p.hasUnpresented(ana, List.of(claimA, claimB)), "the second batch settles too");

        // --- Players are independent.
        check(p.hasUnpresented(bo, List.of(claimA)), "another player is unaffected");
        check(p.presented(bo).isEmpty(), "nothing on record for the other player");
        check(p.presented(ana).equals(Set.of(claimA, claimB)), "the record holds both claims");

        // --- The sidecar round-trips.
        ReviewPresentation q = ReviewPresentation.decode(p.encode());
        check(!q.hasUnpresented(ana, List.of(claimA, claimB)), "round-trip keeps the batch settled");
        check(q.hasUnpresented(ana, List.of("new:run/" + ana)), "round-trip still spots a new claim");
        check(q.presented(ana).equals(p.presented(ana)), "round-trip preserves the set exactly");

        // --- Damage tolerance: malformed lines are skipped, valid
        // lines survive, null/empty text decodes to an empty record.
        String damaged = "not-a-uuid some:id/x\n"
                + "\n"
                + "garbage-line-without-separator\n"
                + ana + " " + claimA + "\n"
                + ana + "\n";
        ReviewPresentation r = ReviewPresentation.decode(damaged);
        check(r.presented(ana).equals(Set.of(claimA)), "valid lines survive a damaged sidecar");
        check(ReviewPresentation.decode(null).presented(ana).isEmpty(), "null text decodes empty");
        check(ReviewPresentation.decode("").presented(ana).isEmpty(), "empty text decodes empty");
        check(new ReviewPresentation().encode().isEmpty(), "an empty record encodes empty");

        System.out.println("ReviewPresentationTest: " + passed + " passed, " + failed + " failed");
        if (failed > 0) {
            System.exit(1);
        }
    }
}

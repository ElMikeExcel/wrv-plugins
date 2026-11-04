package com.winkreyes.excelguildgate;

/**
 * Plain main-method test harness for NarratorPackPrompt +
 * NarratorPackStateStore (same style as the other ExcelGuildGate
 * harnesses: no test framework, every check counted, any failure
 * prints and exits the JVM with status 1).
 *
 * The discipline under test is TestPlayerA's Oct 19, 2026 rule: five
 * lifetime offers per player, counted as they are made; an accept
 * stops the asking permanently at any point; the 5th offer without
 * an accept stops it permanently; there is NO offer #6 under any
 * path (rejoin, restart, config reload - the count lives in the
 * persisted records); Bedrock players are never offered.
 *
 * Run via run-tests.sh.
 */
public final class NarratorPackPromptTest {

    private static final java.util.UUID A =
            java.util.UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final java.util.UUID B =
            java.util.UUID.fromString("22222222-2222-2222-2222-222222222222");

    private static final int MAX = 5;

    private static int checks = 0;
    private static int failures = 0;

    private static void check(String name, boolean condition) {
        checks++;
        if (!condition) {
            failures++;
            System.out.println("FAIL: " + name);
        }
    }

    private static void checkEq(String name, Object expected, Object actual) {
        checks++;
        if (!java.util.Objects.equals(expected, actual)) {
            failures++;
            System.out.println("FAIL: " + name + " expected=" + expected + " actual=" + actual);
        }
    }

    private static NarratorPackPrompt.Record rec(NarratorPackPrompt.Outcome o,
                                                 int attempts, long when) {
        return new NarratorPackPrompt.Record(o, attempts, when);
    }

    private static NarratorPackPrompt.Action offer(NarratorPackPrompt.Platform p,
                                                   NarratorPackPrompt.Record r) {
        return NarratorPackPrompt.decideOffer(p, r, MAX);
    }

    public static void main(String[] args) {
        platformGate();
        offersClimbToTheCap();
        acceptStopsForever();
        offerNumbering();
        lifecycles();
        resetSemantics();
        sendAndStatusFlow();
        storeRoundTrip();
        storeForgivingParse();
        System.out.println("NarratorPackPromptTest: " + (checks - failures) + "/" + checks
                + " checks passed" + (failures == 0 ? " - ALL PASS" : " - " + failures + " FAILED"));
        if (failures > 0) {
            System.exit(1);
        }
    }

    /** Bedrock players are never offered, whatever their record says;
     *  an unknown platform is never guessed at either. */
    private static void platformGate() {
        checkEq("bedrock, no record: skip", NarratorPackPrompt.Action.SKIP,
                offer(NarratorPackPrompt.Platform.BEDROCK, null));
        checkEq("bedrock, chances left: still skip", NarratorPackPrompt.Action.SKIP,
                offer(NarratorPackPrompt.Platform.BEDROCK,
                        rec(NarratorPackPrompt.Outcome.DECLINED, 1, 0L)));
        checkEq("bedrock, loaded record: still skip", NarratorPackPrompt.Action.SKIP,
                offer(NarratorPackPrompt.Platform.BEDROCK,
                        rec(NarratorPackPrompt.Outcome.LOADED, 1, 0L)));
        checkEq("unknown, no record: skip", NarratorPackPrompt.Action.SKIP,
                offer(NarratorPackPrompt.Platform.UNKNOWN, null));
        checkEq("unknown, chances left: skip", NarratorPackPrompt.Action.SKIP,
                offer(NarratorPackPrompt.Platform.UNKNOWN,
                        rec(NarratorPackPrompt.Outcome.SENT, 2, 0L)));
        checkEq("null platform: skip", NarratorPackPrompt.Action.SKIP,
                NarratorPackPrompt.decideOffer(null, null, MAX));
    }

    /** Offers 1-5 are made (one per join); every non-accept outcome
     *  simply spends the chance - no waiting period, no reset. At 5
     *  the asking stops permanently, for every outcome. */
    private static void offersClimbToTheCap() {
        checkEq("never offered: offer 1 sends", NarratorPackPrompt.Action.SEND,
                offer(NarratorPackPrompt.Platform.JAVA, null));
        for (NarratorPackPrompt.Outcome o : new NarratorPackPrompt.Outcome[]{
                NarratorPackPrompt.Outcome.SENT,
                NarratorPackPrompt.Outcome.DECLINED,
                NarratorPackPrompt.Outcome.FAILED}) {
            for (int used = 1; used < MAX; used++) {
                checkEq(o + " after " + used + " offer(s): next sends",
                        NarratorPackPrompt.Action.SEND,
                        offer(NarratorPackPrompt.Platform.JAVA, rec(o, used, 0L)));
            }
            checkEq(o + " at the cap of 5: skip forever", NarratorPackPrompt.Action.SKIP,
                    offer(NarratorPackPrompt.Platform.JAVA, rec(o, MAX, 0L)));
            checkEq(o + " past the cap: skip forever", NarratorPackPrompt.Action.SKIP,
                    offer(NarratorPackPrompt.Platform.JAVA, rec(o, MAX + 2, 0L)));
        }
        // No clock is involved any more: a decline neither waits out
        // a window nor earns a reset, however much time passes.
        checkEq("declined at the cap, ages later: still skip",
                NarratorPackPrompt.Action.SKIP,
                offer(NarratorPackPrompt.Platform.JAVA,
                        rec(NarratorPackPrompt.Outcome.DECLINED, MAX, 0L)));
        // A maxOffers below 1 behaves as 1, never as "offer forever".
        checkEq("maxOffers 0 clamps to 1: one spent offer skips",
                NarratorPackPrompt.Action.SKIP,
                NarratorPackPrompt.decideOffer(NarratorPackPrompt.Platform.JAVA,
                        rec(NarratorPackPrompt.Outcome.SENT, 1, 0L), 0));
    }

    /** An accept at any point - offer 1 or offer 5 - means never
     *  offered again, ever. */
    private static void acceptStopsForever() {
        for (int used = 0; used <= MAX; used++) {
            checkEq("loaded after " + used + " offer(s): skip",
                    NarratorPackPrompt.Action.SKIP,
                    offer(NarratorPackPrompt.Platform.JAVA,
                            rec(NarratorPackPrompt.Outcome.LOADED, used, 0L)));
        }
    }

    /** The counter the prompt shows: the next offer's number. */
    private static void offerNumbering() {
        checkEq("no record: next is offer 1", 1, NarratorPackPrompt.nextOfferNumber(null));
        checkEq("0 attempts: next is offer 1", 1,
                NarratorPackPrompt.nextOfferNumber(rec(NarratorPackPrompt.Outcome.SENT, 0, 0L)));
        checkEq("1 attempt: next is offer 2", 2,
                NarratorPackPrompt.nextOfferNumber(rec(NarratorPackPrompt.Outcome.DECLINED, 1, 0L)));
        checkEq("4 attempts: next is offer 5", 5,
                NarratorPackPrompt.nextOfferNumber(rec(NarratorPackPrompt.Outcome.FAILED, 4, 0L)));
    }

    /** Full player lifecycles through the real record-keeping. */
    private static void lifecycles() {
        // The tap-spammer: declines every time. Five joins, five
        // offers with the counters 1..5, then silence forever.
        NarratorPackPrompt spam = new NarratorPackPrompt();
        for (int join = 1; join <= MAX; join++) {
            NarratorPackPrompt.Record before = spam.record(A);
            checkEq("decliner, join " + join + ": offered",
                    NarratorPackPrompt.Action.SEND,
                    offer(NarratorPackPrompt.Platform.JAVA, before));
            checkEq("decliner, join " + join + ": counter shows " + join,
                    join, NarratorPackPrompt.nextOfferNumber(before));
            spam.applySend(A, join * 1_000L);
            spam.applyStatus(A, NarratorPack.Status.DECLINED, join * 1_000L + 1);
        }
        checkEq("decliner, join 6: no offer #6", NarratorPackPrompt.Action.SKIP,
                offer(NarratorPackPrompt.Platform.JAVA, spam.record(A)));
        checkEq("decliner spent exactly 5 offers", MAX, spam.record(A).attempts());

        // The same player after a "restart": the persisted records
        // restore into a fresh instance and the cap still holds -
        // no offer #6 via restart or config reload either.
        NarratorPackPrompt afterRestart = new NarratorPackPrompt();
        afterRestart.restore(NarratorPackStateStore.parse(
                NarratorPackStateStore.serialize(spam.snapshot())));
        checkEq("decliner after restart: still no offer #6",
                NarratorPackPrompt.Action.SKIP,
                offer(NarratorPackPrompt.Platform.JAVA, afterRestart.record(A)));

        // The acceptor: declines twice, accepts on offer 3 - and is
        // never offered again, even though 2 chances remained.
        NarratorPackPrompt acceptor = new NarratorPackPrompt();
        acceptor.applySend(A, 0L);
        acceptor.applyStatus(A, NarratorPack.Status.DECLINED, 1L);
        acceptor.applySend(A, 2L);
        acceptor.applyStatus(A, NarratorPack.Status.DECLINED, 3L);
        checkEq("acceptor: offer 3 still comes", NarratorPackPrompt.Action.SEND,
                offer(NarratorPackPrompt.Platform.JAVA, acceptor.record(A)));
        acceptor.applySend(A, 4L);
        acceptor.applyStatus(A, NarratorPack.Status.LOADED, 5L);
        checkEq("acceptor: after the accept, no offer 4",
                NarratorPackPrompt.Action.SKIP,
                offer(NarratorPackPrompt.Platform.JAVA, acceptor.record(A)));
        checkEq("acceptor: record kept at 3 offers", 3, acceptor.record(A).attempts());

        // The ghost: never answers at all (leaves mid-download every
        // time). Unanswered offers still spend the chances.
        NarratorPackPrompt ghost = new NarratorPackPrompt();
        for (int join = 1; join <= MAX; join++) {
            checkEq("ghost, join " + join + ": offered", NarratorPackPrompt.Action.SEND,
                    offer(NarratorPackPrompt.Platform.JAVA, ghost.record(A)));
            ghost.applySend(A, join * 1_000L);
        }
        checkEq("ghost, join 6: no offer #6", NarratorPackPrompt.Action.SKIP,
                offer(NarratorPackPrompt.Platform.JAVA, ghost.record(A)));
    }

    /** The manual escape hatch (/guildgate packreset): a reset
     *  starts the offers again at 1 of 5 - except for a player who
     *  already has the pack, whose record is left exactly as it was
     *  and reported as such. */
    private static void resetSemantics() {
        // Mid-count: 3 offers spent declining -> back to zero, and the
        // next join offers "1 of 5" again.
        NarratorPackPrompt mid = new NarratorPackPrompt();
        mid.restore(java.util.Map.of(A, rec(NarratorPackPrompt.Outcome.DECLINED, 3, 0L)));
        checkEq("mid-count reset reports RESET", NarratorPackPrompt.ResetResult.RESET,
                mid.reset(A));
        checkEq("mid-count record is gone", null, mid.record(A));
        checkEq("mid-count next join offers again", NarratorPackPrompt.Action.SEND,
                offer(NarratorPackPrompt.Platform.JAVA, mid.record(A)));
        checkEq("mid-count counter restarts at 1", 1,
                NarratorPackPrompt.nextOfferNumber(mid.record(A)));

        // Exhausted: all 5 spent -> reset makes them offerable again.
        NarratorPackPrompt spent = new NarratorPackPrompt();
        spent.restore(java.util.Map.of(A, rec(NarratorPackPrompt.Outcome.FAILED, MAX, 0L)));
        checkEq("exhausted player is a hard skip before reset",
                NarratorPackPrompt.Action.SKIP,
                offer(NarratorPackPrompt.Platform.JAVA, spent.record(A)));
        checkEq("exhausted reset reports RESET", NarratorPackPrompt.ResetResult.RESET,
                spent.reset(A));
        checkEq("exhausted player offers again after reset",
                NarratorPackPrompt.Action.SEND,
                offer(NarratorPackPrompt.Platform.JAVA, spent.record(A)));

        // Accepted: untouched, and reported as already-has-pack.
        NarratorPackPrompt accepted = new NarratorPackPrompt();
        accepted.restore(java.util.Map.of(A, rec(NarratorPackPrompt.Outcome.LOADED, 2, 7L)));
        checkEq("accepted reset reports ALREADY_HAS_PACK",
                NarratorPackPrompt.ResetResult.ALREADY_HAS_PACK, accepted.reset(A));
        checkEq("accepted record untouched",
                rec(NarratorPackPrompt.Outcome.LOADED, 2, 7L), accepted.record(A));
        checkEq("accepted player still never offered",
                NarratorPackPrompt.Action.SKIP,
                offer(NarratorPackPrompt.Platform.JAVA, accepted.record(A)));

        // Nobody home: unknown and null players are a clean NO_RECORD.
        NarratorPackPrompt empty = new NarratorPackPrompt();
        checkEq("no record reset reports NO_RECORD",
                NarratorPackPrompt.ResetResult.NO_RECORD, empty.reset(A));
        checkEq("null player reset reports NO_RECORD",
                NarratorPackPrompt.ResetResult.NO_RECORD, empty.reset(null));

        // Resetting one player never touches another.
        NarratorPackPrompt two = new NarratorPackPrompt();
        two.restore(java.util.Map.of(
                A, rec(NarratorPackPrompt.Outcome.DECLINED, 4, 0L),
                B, rec(NarratorPackPrompt.Outcome.DECLINED, 1, 0L)));
        two.reset(A);
        checkEq("the other player's record survives a reset",
                rec(NarratorPackPrompt.Outcome.DECLINED, 1, 0L), two.record(B));

        // The all variant: every non-accepted record clears, accepted
        // records are kept, and the counts say exactly what happened.
        NarratorPackPrompt all = new NarratorPackPrompt();
        java.util.Map<java.util.UUID, NarratorPackPrompt.Record> mix =
                new java.util.LinkedHashMap<>();
        mix.put(A, rec(NarratorPackPrompt.Outcome.DECLINED, 3, 0L));
        mix.put(B, rec(NarratorPackPrompt.Outcome.LOADED, 1, 0L));
        mix.put(java.util.UUID.fromString("33333333-3333-3333-3333-333333333333"),
                rec(NarratorPackPrompt.Outcome.SENT, MAX, 0L));
        mix.put(java.util.UUID.fromString("44444444-4444-4444-4444-444444444444"),
                rec(NarratorPackPrompt.Outcome.FAILED, 2, 0L));
        all.restore(mix);
        NarratorPackPrompt.ResetSummary summary = all.resetAll();
        checkEq("all: 3 non-accepted reset", 3, summary.resetCount());
        checkEq("all: 1 accepted kept", 1, summary.keptAcceptedCount());
        checkEq("all: only the accepted record remains", 1, all.trackedCount());
        checkEq("all: the accepted record is the one kept",
                rec(NarratorPackPrompt.Outcome.LOADED, 1, 0L), all.record(B));
        checkEq("all: a reset player offers again", NarratorPackPrompt.Action.SEND,
                offer(NarratorPackPrompt.Platform.JAVA, all.record(A)));
        NarratorPackPrompt.ResetSummary second = all.resetAll();
        checkEq("all again: nothing left to reset", 0, second.resetCount());
        checkEq("all again: accepted still kept", 1, second.keptAcceptedCount());
    }

    private static void sendAndStatusFlow() {
        NarratorPackPrompt pp = new NarratorPackPrompt();
        checkEq("fresh instance tracks nobody", 0, pp.trackedCount());
        checkEq("unknown player has no record", null, pp.record(A));

        NarratorPackPrompt.Record sent = pp.applySend(A, 1_000L);
        checkEq("first send: SENT x1",
                rec(NarratorPackPrompt.Outcome.SENT, 1, 1_000L), sent);
        checkEq("record visible after send", sent, pp.record(A));
        pp.applySend(A, 2_000L);
        checkEq("second send: attempts 2, restamped",
                rec(NarratorPackPrompt.Outcome.SENT, 2, 2_000L), pp.record(A));

        check("LOADED status changes the record",
                pp.applyStatus(A, NarratorPack.Status.LOADED, 3_000L));
        checkEq("after load",
                rec(NarratorPackPrompt.Outcome.LOADED, 2, 3_000L), pp.record(A));
        check("same LOADED again is not a change",
                !pp.applyStatus(A, NarratorPack.Status.LOADED, 3_000L));
        check("IGNORED never changes anything",
                !pp.applyStatus(A, NarratorPack.Status.IGNORED, 4_000L));
        checkEq("record survives IGNORED",
                rec(NarratorPackPrompt.Outcome.LOADED, 2, 3_000L), pp.record(A));
        check("null status is a no-op", !pp.applyStatus(A, null, 4_000L));
        check("null player send is a no-op", pp.applySend(null, 4_000L) == null);
        check("null player status is a no-op",
                !pp.applyStatus(null, NarratorPack.Status.LOADED, 4_000L));

        check("DECLINED lands", pp.applyStatus(B, NarratorPack.Status.DECLINED, 5_000L));
        checkEq("decline without a prior send keeps attempts 0",
                rec(NarratorPackPrompt.Outcome.DECLINED, 0, 5_000L), pp.record(B));
        check("FAILED lands over a decline",
                pp.applyStatus(B, NarratorPack.Status.FAILED, 6_000L));
        checkEq("players are independent",
                rec(NarratorPackPrompt.Outcome.LOADED, 2, 3_000L), pp.record(A));
        checkEq("two players tracked", 2, pp.trackedCount());
    }

    private static void storeRoundTrip() {
        java.util.Map<java.util.UUID, NarratorPackPrompt.Record> in =
                new java.util.LinkedHashMap<>();
        in.put(B, rec(NarratorPackPrompt.Outcome.DECLINED, 5, 123_456L));
        in.put(A, rec(NarratorPackPrompt.Outcome.LOADED, 2, 999L));
        String text = NarratorPackStateStore.serialize(in);
        check("serialization is sorted by uuid",
                text.indexOf(A.toString()) < text.indexOf(B.toString()));
        checkEq("round trip preserves every record", in, NarratorPackStateStore.parse(text));
        checkEq("empty map serializes to empty text", "",
                NarratorPackStateStore.serialize(java.util.Map.of()));
        checkEq("null map serializes to empty text", "", NarratorPackStateStore.serialize(null));
        checkEq("empty text parses to empty map", java.util.Map.of(),
                NarratorPackStateStore.parse(""));
        checkEq("null text parses to empty map", java.util.Map.of(),
                NarratorPackStateStore.parse(null));
    }

    private static void storeForgivingParse() {
        String messy = "# a comment\n"
                + "\n"
                + A + " LOADED 2 999\n"
                + "not-a-uuid SENT 1 1\n"
                + B + " BOGUS 1 1\n"
                + B + " SENT notanumber 1\n"
                + B + " SENT 1\n"
                + B + " FAILED -3 -50\n";
        java.util.Map<java.util.UUID, NarratorPackPrompt.Record> out =
                NarratorPackStateStore.parse(messy);
        checkEq("good line survives the mess",
                rec(NarratorPackPrompt.Outcome.LOADED, 2, 999L), out.get(A));
        checkEq("negative numbers clamp to 0",
                rec(NarratorPackPrompt.Outcome.FAILED, 0, 0L), out.get(B));
        checkEq("only the two valid records parse", 2, out.size());

        NarratorPackPrompt restored = new NarratorPackPrompt();
        restored.restore(out);
        checkEq("restore lands both records", 2, restored.trackedCount());
        restored.restore(null);
        checkEq("restore of null clears to empty", 0, restored.trackedCount());
        java.util.Map<java.util.UUID, NarratorPackPrompt.Record> withNulls =
                new java.util.HashMap<>();
        withNulls.put(null, rec(NarratorPackPrompt.Outcome.SENT, 1, 1L));
        withNulls.put(A, null);
        restored.restore(withNulls);
        checkEq("restore skips null keys and values", 0, restored.trackedCount());
    }
}

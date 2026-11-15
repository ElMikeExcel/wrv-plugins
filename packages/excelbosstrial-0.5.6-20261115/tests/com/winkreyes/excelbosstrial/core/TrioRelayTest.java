/* Excel WRV maintenance; existing authorship retained. */ package com.winkreyes.excelbosstrial.core;

import java.util.List;
import java.util.UUID;

/**
 * The monument answers (Packet B, E-1): the relay decision
 * and the tracker's sibling set. A counted kill at progress
 * 1 or 2 relays to every still-TRACKED sibling; a completing
 * kill (progress 3) and an uncounted kill relay nothing.
 */
public final class TrioRelayTest {

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
        // The firing window.
        check(!TrioRelay.fires(-1), "relay: an uncounted kill (-1) fires nothing");
        check(!TrioRelay.fires(0), "relay: progress 0 fires nothing");
        check(TrioRelay.fires(1), "relay: progress 1 fires (elder #2 starts hot)");
        check(TrioRelay.fires(2), "relay: progress 2 fires (elder #3 opens roused)");
        check(!TrioRelay.fires(3), "relay: the completing kill fires nothing (verdict owns it)");
        check(TrioRelay.relayTargets(1, 2) == 2, "relay: two tracked siblings, two targets");
        check(TrioRelay.relayTargets(2, 1) == 1, "relay: one tracked sibling, one target");
        check(TrioRelay.relayTargets(1, 0) == 0, "relay: no siblings, no targets");
        check(TrioRelay.relayTargets(3, 2) == 0, "relay: progress 3 targets nobody");
        check(TrioRelay.DEFAULT_TEXT.contains("The monument answers"),
                "relay: the settled line is the design's chapter voice");

        // The tracker sibling set, through a real trio.
        MonumentTracker mt = new MonumentTracker();
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        UUID c = UUID.randomUUID();
        UUID other = UUID.randomUUID();
        mt.register(a, "world_the_end", 0, 70, 0);
        mt.register(b, "world_the_end", 20, 70, 10);
        mt.register(c, "world_the_end", -15, 70, 25);
        mt.register(other, "world_the_end", 500, 70, 500); // a different monument
        String monument = mt.monumentOf(a);
        check(mt.trackedSiblings(a).size() == 2,
                "tracker: a fresh elder has two tracked siblings");
        check(mt.trackedSiblings(other).isEmpty(),
                "tracker: a lone elder of another monument has no siblings here");

        int progress = mt.markCounted(a);
        check(progress == 1, "tracker: the first kill counts to progress 1");
        List<MonumentTracker.ElderRecord> sibs = mt.trackedSiblings(a);
        check(sibs.size() == 2, "tracker: both survivors still tracked after the kill");
        check(TrioRelay.relayTargets(progress, sibs.size()) == 2,
                "relay plan: progress 1 relays to both survivors");

        // A sibling that dies uncounted goes PENDING_RESPAWN and
        // leaves the sibling set - it cannot be relayed to.
        mt.markPendingRespawn(b, System.currentTimeMillis() + 1000);
        check(mt.trackedSiblings(a).size() == 1,
                "tracker: a pending-respawn sibling leaves the relay set");
        check(mt.trackedSiblings(a).get(0).uuid.equals(c),
                "tracker: the remaining sibling is the third elder");

        progress = mt.markCounted(c);
        check(progress == 2, "tracker: the second counted kill reaches progress 2");
        check(mt.trackedSiblings(c).isEmpty(),
                "tracker: after two counted, no tracked sibling remains (b is pending)");
        check(TrioRelay.relayTargets(progress, mt.trackedSiblings(c).size()) == 0,
                "relay plan: progress 2 with no live tracked sibling relays zero - "
                        + "the adapter logs the empty relay, never invents one");

        System.out.println("TrioRelayTest: " + passed + " passed, " + failed + " failed");
        if (failed > 0) {
            System.exit(1);
        }
    }
}

// ExcelBossTrial - a Wink Reyes Valley Excel plugin.

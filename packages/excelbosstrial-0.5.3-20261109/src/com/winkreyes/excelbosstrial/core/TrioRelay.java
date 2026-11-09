/* Excel WRV maintenance; existing authorship retained. */ package com.winkreyes.excelbosstrial.core;

/**
 * The monument answers (Item 35 Packet B, E-1 - the elder trio
 * relay): when an elder of a monument trio dies at trio
 * progress 1 of 3 or 2 of 3, each SURVIVING elder of the same
 * monument immediately owes a sprinkle wave and fires its
 * milestone surge, under a broadcast line in the chapter's
 * voice. Elder #2's fight starts hot; #3 - already the x1.3
 * elder - opens with the monument fully roused. Design finding
 * F-E4's invisible escalation (a thicker number on elder #3,
 * identical behavior) becomes a thing the party watches happen.
 *
 * This class is the pure decision: given the monument's
 * progress after the kill and the candidate siblings (the
 * tracker's still-TRACKED elders of the same monument - see
 * MonumentTracker.trackedSiblings), who relays. The adapter
 * (the host) resolves those records to live entities with open
 * encounters, queues each sibling ledger's surge, and hands
 * each sibling encounter to the sprinkle's relay entry point.
 * A sibling with no open encounter is skipped by the adapter,
 * never invented: the relay pressures fights that are actually
 * being fought.
 *
 * The relay fires at progress 1 and 2 only. At progress 3 the
 * trio is complete - the verdict path (seal credit, generation
 * turnover) owns that moment, and there is no surviving trio
 * sibling left to answer. At progress 0 or -1 (an uncounted or
 * unknown kill) nothing counted, so nothing answers.
 */
public final class TrioRelay {

    /** The settled broadcast line (design E-1, the chapter's
     *  voice). Config key messages.elder-relay overrides it. */
    public static final String DEFAULT_TEXT =
            "&3The monument answers &7- its remaining guardians swell with rage.";

    private TrioRelay() {
    }

    /** True when a kill at this trio progress triggers the
     *  relay: a COUNTED kill (progress advanced) that does not
     *  complete the trio. */
    public static boolean fires(int progressAfterKill) {
        return progressAfterKill > 0
                && progressAfterKill < MonumentTracker.TRIO_SIZE;
    }

    /**
     * The relay plan for one counted elder kill.
     *
     * @param progressAfterKill the monument's progress after
     *        the kill (MonumentTracker.markCounted's return;
     *        -1 when the kill did not count)
     * @param siblingCount how many still-TRACKED siblings the
     *        tracker holds for the dead elder's monument
     * @return how many siblings the relay owes a wave + surge
     *         to (0 when the relay does not fire)
     */
    public static int relayTargets(int progressAfterKill, int siblingCount) {
        if (!fires(progressAfterKill)) {
            return 0;
        }
        return Math.max(0, siblingCount);
    }
}

// ExcelBossTrial - a Wink Reyes Valley Excel plugin.

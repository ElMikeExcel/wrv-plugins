package com.winkreyes.excelwandbars.core;

/**
 * 0.3.11: the join-guard eviction sweep ("A-lite" of the
 * compass-on-wand-bar fix). Pure (no Bukkit/Magic types), unit-tested on
 * the plain JVM.
 *
 * The quit close (QuitClosePlan) stops NEW compasses from being baked
 * into wand-bar pages, but pages saved before 0.3.11 can already hold
 * one. The exit sweep (ExitSweep) evicts foreign items from the pages,
 * yet on the restored-at-join path it never ran: TapBarHook.tryOpen
 * returns "already open" before its sweep step when the bar comes back
 * restored. So when the join guard's FINAL check leaves a bar up — the
 * player engaged, or the window closed — the sweep runs once from the
 * guard itself. That is the one moment the pages are guaranteed loaded
 * and the player is present, and it covers every already-baked page the
 * first time its owner joins on 0.3.11.
 *
 * Once per join only: the early checks (1.5 s / 4 s) never sweep — a
 * restored bar the guard is about to close needs no sweep (the close
 * restores the real items, and CompassGuard's tidy then sees the truth),
 * and sweeping mid-grace would race the guard's own judgement.
 */
public final class JoinSweepPlan {
    private JoinSweepPlan() {}

    /**
     * @param guardEnabled the "joins start on your items" switch (the guard's own policy)
     * @param sweepEnabled the exit-sweep switch
     * @param finalCheck   this is the guard's final (past-grace) check for the join
     * @param guardLeft    the join guard's verdict was LEAVE (it is not closing the bar)
     * @param barOpen      the wand's spell bar is open right now
     * @param alreadySwept the sweep already ran for this join
     */
    public static boolean shouldSweep(boolean guardEnabled, boolean sweepEnabled, boolean finalCheck,
                                      boolean guardLeft, boolean barOpen, boolean alreadySwept) {
        return guardEnabled && sweepEnabled && finalCheck && guardLeft && barOpen && !alreadySwept;
    }
}

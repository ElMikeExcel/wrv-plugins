/* Excel WRV maintenance; existing authorship retained. */ package com.winkreyes.teamrewards.ui;

/**
 * 0.2.11 (B-2): the immunity-frame boundaries. Frames exist for AUTO
 * sessions only; the manual-open exclusion is the anti-exploit
 * boundary and is pinned first. Then the window: a hard cap from the
 * FIRST open (paging cannot extend it), a close grace that blocks
 * damage-in only, and dealing damage ending the grace instantly.
 * Defaults: 1200 ticks framed (60 s), 20 ticks grace (1 s).
 */
public final class ReviewFramesPolicyTest {

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
        // --- THE boundary: a manual open never earns frames, ever.
        ReviewFramesPolicy manual = new ReviewFramesPolicy();
        manual.sessionOpened(false, 100);
        check(!manual.framedAt(100) && !manual.damageInBlocked(100) && !manual.actionsBlocked(100),
                "manual open: no frames at open");
        check(!manual.damageInBlocked(100 + 10_000), "manual open: no frames ever appear later");
        manual.sessionClosed(200);
        check(!manual.inGrace(200) && !manual.damageInBlocked(200),
                "manual open: closing earns no grace either");

        // --- An AUTO session frames: in, out, and interact blocked.
        ReviewFramesPolicy auto = new ReviewFramesPolicy();
        auto.sessionOpened(true, 1_000);
        check(auto.framedAt(1_000) && auto.damageInBlocked(1_000) && auto.actionsBlocked(1_000),
                "auto open: framed from the first tick");
        check(auto.framedAt(1_000 + 1199), "auto open: still framed one tick before the cap");
        check(!auto.framedAt(1_000 + 1200) && !auto.damageInBlocked(1_000 + 1200),
                "auto open: the cap is absolute at 1200 ticks");

        // --- Closing after the cap earns NO grace.
        auto.sessionClosed(1_000 + 1200);
        check(!auto.inGrace(1_000 + 1200) && !auto.damageInBlocked(1_000 + 1200),
                "close at the cap: no grace");

        // --- Closing inside the window: damage-in-only grace, 20 ticks.
        ReviewFramesPolicy closed = new ReviewFramesPolicy();
        closed.sessionOpened(true, 500);
        closed.sessionClosed(600);
        check(closed.inGrace(600) && closed.damageInBlocked(600) && !closed.actionsBlocked(600),
                "grace: damage-in blocked, actions free");
        check(closed.inGrace(619) && !closed.inGrace(620) && !closed.damageInBlocked(620),
                "grace: exactly 20 ticks");

        // --- Dealing damage ends the grace on the spot.
        ReviewFramesPolicy fighter = new ReviewFramesPolicy();
        fighter.sessionOpened(true, 0);
        fighter.sessionClosed(100);
        check(fighter.inGrace(105), "grace running before the hit");
        fighter.damageDealt(105);
        check(!fighter.inGrace(105) && !fighter.damageInBlocked(105),
                "dealing damage ends the grace instantly");

        // --- Paging inside the session cannot extend the window:
        // a second AUTO open while FRAMED keeps the original start.
        ReviewFramesPolicy pager = new ReviewFramesPolicy();
        pager.sessionOpened(true, 0);
        pager.sessionOpened(true, 1100);
        check(pager.framedAt(1199) && !pager.framedAt(1200),
                "paging at tick 1100 does not move the 1200 cap");

        // --- A manual open ends live frames outright (the player
        // took the menu into their own hands; protection is over).
        ReviewFramesPolicy taken = new ReviewFramesPolicy();
        taken.sessionOpened(true, 0);
        check(taken.framedAt(10), "framed before the manual open");
        taken.sessionOpened(false, 10);
        check(!taken.framedAt(10) && !taken.damageInBlocked(10),
                "manual open ends frames outright, no grace");

        // --- After a full settle, a NEW auto session frames afresh.
        ReviewFramesPolicy again = new ReviewFramesPolicy();
        again.sessionOpened(true, 0);
        again.sessionClosed(100);          // grace 100..119
        again.sessionOpened(true, 5_000);  // a later verdict/join batch
        check(again.framedAt(5_000) && again.framedAt(5_000 + 1199) && !again.framedAt(5_000 + 1200),
                "a new batch frames a full new window");

        // --- Config-sized windows behave the same at their own scale.
        ReviewFramesPolicy small = new ReviewFramesPolicy(100, 5);
        small.sessionOpened(true, 0);
        check(small.framedAt(99) && !small.framedAt(100), "custom cap honoured");
        small.sessionClosed(50);
        check(small.inGrace(54) && !small.inGrace(55), "custom grace honoured");
        check(small.maxTicks() == 100 && small.graceTicks() == 5, "parameters retained");
        check(new ReviewFramesPolicy().maxTicks() == 1200
                        && new ReviewFramesPolicy().graceTicks() == 20,
                "defaults are the settled 1200 / 20 ticks");

        System.out.println("ReviewFramesPolicyTest: " + passed + " passed, " + failed + " failed");
        if (failed > 0) {
            System.exit(1);
        }
    }
}

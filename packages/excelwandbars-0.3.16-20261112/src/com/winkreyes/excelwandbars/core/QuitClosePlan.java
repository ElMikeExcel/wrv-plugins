package com.winkreyes.excelwandbars.core;

/**
 * 0.3.11: the deterministic logout close (the compass-on-wand-bar fix).
 * Pure (no Bukkit/Magic types), unit-tested on the plain JVM.
 *
 * The defect (see COMPASS-ON-WANDBAR-INVESTIGATION.md): a player who logs
 * out with the spell bar open is restored by Magic at the next join with
 * the bar open again. While the bar is presented, the player's real
 * inventory is hidden inside the wand, so the menu-compass give at join
 * (ExcelMenuJoin inspects the PRESENTED inventory for an existing compass)
 * cannot see the real compass and hands out a duplicate — which lands in
 * the presented bar and is saved into the wand's page contents, where it
 * re-renders at every later open.
 *
 * The 0.3.8 join guard was the backstop, but it races the restore: it can
 * only judge checks at 1.5 s / 4 s / 8 s after join, and Magic decides
 * whether to restore the wand open from the wand's open state at ITS quit
 * handling — which runs before any PlayerQuitEvent handler WandBars can
 * register (Magic enables first; its default logout_delay is 0). So the
 * deterministic fix is to close the bar BEFORE that decision is read:
 * TapBarHook.closeBarAtExit runs from Paper's connection-close event
 * (fired synchronously from the connection teardown, before
 * PlayerQuitEvent dispatch, while the mage is still active) and, as a
 * belt for logout_delay &gt; 0 setups, again at the top of the quit
 * handler. With the bar closed at every logout, the restore decision
 * always reads "closed": no join ever starts on the wand bar, and the
 * blind compass give loses its precondition.
 *
 * The policy switch is the existing "joins start on your items" switch
 * (join-starts-on-items): the logout close is that policy enforced at the
 * other end of the session, so it shares the switch — no new config key.
 */
public final class QuitClosePlan {
    private QuitClosePlan() {}

    public enum Decision { CLOSE, LEAVE }

    /**
     * @param enabled     the "joins start on your items" policy switch
     * @param usesTapBar  the player's style opens a spell bar on hold (Tap bar / Tap to pick)
     * @param wandPresent the player has an active wand at all
     * @param barOpen     the wand's spell bar is open right now
     */
    public static Decision decide(boolean enabled, boolean usesTapBar, boolean wandPresent, boolean barOpen) {
        if (!enabled || !usesTapBar || !wandPresent || !barOpen) return Decision.LEAVE;
        return Decision.CLOSE;
    }
}

package com.winkreyes.excelwandbars.core;

/**
 * 0.3.8: the login wand-mode trap fix (Mike, Nov 2 2026: joins must land on
 * the normal hotbar). Pure (no Bukkit/Magic types), unit-tested on the plain
 * JVM.
 *
 * The defect: Magic restores a wand whose spell bar was open at logout with
 * the bar open again at the next join, so the player lands in wand mode —
 * and the exit paths are slot-dependent (tap the empty slot) or echo-guarded
 * (the wand tap), while the menu compass other plugins hand out at join can
 * sit in the very slot the exit needs. The guard's rule: a bar that is open
 * inside the join grace window, with no sign the player has deliberately
 * engaged since joining, is a RESTORED bar, not a chosen one — close it
 * (Magic's own close, real items restored) so the join lands on the normal
 * hotbar.
 *
 * "Deliberate engagement" is a slot pick or a bar open stamped after the
 * join churn window ({@link #CHURN_MS}): in the first moments after a join
 * the server itself stamps picks (the wand-slot sweep moving the wand into
 * place), so stamps inside that window prove nothing and never exempt.
 * Past the grace window the guard stops policing entirely: whatever the
 * player has open then is their own session.
 */
public final class JoinGuardPlan {
    private JoinGuardPlan() {}

    /** Server-side join churn (wand move/select stamps) inside this window is not engagement. */
    public static final long CHURN_MS = 1000;

    public enum Decision { CLOSE, LEAVE }

    /**
     * @param enabled        the feature's config switch
     * @param usesTapBar     the player's style opens a spell bar on hold (Tap bar / Tap to pick)
     * @param barOpen        the wand's spell bar is open right now
     * @param joinedAt       when this join was stamped (0 = never stamped, e.g. plugin reload)
     * @param now            current time, same clock as joinedAt
     * @param lastActivityAt last slot pick / deliberate bar open stamp (0 = none)
     * @param graceMs        the join grace window
     */
    public static Decision decide(boolean enabled, boolean usesTapBar, boolean barOpen,
                                  long joinedAt, long now, long lastActivityAt, long graceMs) {
        if (!enabled || !usesTapBar || !barOpen) return Decision.LEAVE;
        if (joinedAt <= 0) return Decision.LEAVE;
        if (now - joinedAt > Math.max(0, graceMs)) return Decision.LEAVE;
        if (lastActivityAt > joinedAt + CHURN_MS) return Decision.LEAVE;
        return Decision.CLOSE;
    }
}

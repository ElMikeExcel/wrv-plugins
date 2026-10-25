package com.winkreyes.excelvaults.core;

/** What a right-click on a vault should do. Pure logic so it can be unit tested. */
public final class Rules {
    private Rules() {}

    public enum Action {
        /** Not locked for this player: leave it to vanilla. */
        VANILLA,
        /** Ominous vault and the seal gate is still closed: vanilla, plus a gentle one-line note. */
        GATED,
        /** Cooldown is over (or a reopen is pending): re-allow now, then let vanilla handle the same click. */
        REALLOW,
        /** Locked: show "Opens again in ..." plus the pay option. */
        HINT,
        /** Locked and sneaking: open the pay-to-reopen prompt. */
        PROMPT
    }

    /** startTimer = we have no record yet (vault was locked before install): start its cooldown now. */
    public record Decision(Action action, boolean startTimer) {}

    /**
     * @param rewarded vanilla says this player already got this vault's reward (locked for them)
     * @param openedAt when we saw them open it (ms), 0 = reopen pending (paid or admin reset), null = no record
     */
    public static Decision decide(boolean rewarded, Long openedAt, boolean ominous, boolean gateOpen, boolean sneaking,
                                  long now, long cooldownMs) {
        if (!rewarded) return new Decision(Action.VANILLA, false);
        boolean start = openedAt == null;
        if (ominous && !gateOpen) return new Decision(Action.GATED, start);
        if (!start && left(openedAt, now, cooldownMs) <= 0) return new Decision(Action.REALLOW, false);
        return new Decision(sneaking ? Action.PROMPT : Action.HINT, start);
    }

    /** Milliseconds until reopen; <= 0 means ready. openedAt 0 = pending reopen (ready). */
    public static long left(long openedAt, long now, long cooldownMs) {
        if (openedAt <= 0) return 0;
        return openedAt + cooldownMs - now;
    }

    /** Ominous vaults only reopen (by time or by paying) once this many seals are broken. Unknown seals = closed. */
    public static boolean gateOpen(Integer seals, int minSeals) {
        if (minSeals <= 0) return true;
        return seals != null && seals >= minSeals;
    }
}

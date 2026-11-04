package com.winkreyesvalley.excelmagiclevel.core;

/**
 * ExcelMagicLevel — party share copy math (SPEC §4.5). Copies are computed
 * from the earner's FINAL (post-window, post-multiplier) amount and never
 * re-share; the receiver's own windows are not applied to a copy. The share
 * itself is wired in Phase 2 (ExcelTogether adapter) — the math lives in
 * core now so it is pinned by tests from day one.
 */
public final class ShareMath {
    private ShareMath() {
    }

    /** {@code floor(earnerFinal × percent / 100)}. */
    public static int copyAmount(int earnerFinalXp, int percent) {
        if (earnerFinalXp <= 0 || percent <= 0) return 0;
        return (int) Math.floor(earnerFinalXp * (percent / 100.0));
    }
}

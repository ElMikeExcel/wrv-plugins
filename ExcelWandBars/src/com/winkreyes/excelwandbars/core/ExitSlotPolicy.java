package com.winkreyes.excelwandbars.core;
/** Tap bar capacity, independent of the player's arrangement and wand position. */
public final class ExitSlotPolicy {
    public static final int MAX_SPELLS = 7;
    private ExitSlotPolicy() {}
    public static int perPage(WandStyle style, int configuredTap, int configuredBars) {
        if (style == WandStyle.TAPBAR) return Math.min(MAX_SPELLS, PageLayout.clampPerPage(configuredTap));
        return style == WandStyle.BARS ? configuredBars : configuredTap;
    }
    public static boolean pagesRequired(WandStyle style, boolean configured) { return configured || style == WandStyle.TAPBAR; }
}

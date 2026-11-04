package com.winkreyesvalley.excelmagiclevel.adapter.mcmmo;

/**
 * ExcelMagicLevel — the ONE door any future mcMMO contact goes through
 * (SPEC §9.1, house build style: replaceable adapters, never hard welds).
 * v1 has NO runtime mcMMO touchpoint at all: parties come from
 * ExcelTogether, the weekend signal from our own clock adapter, display
 * from PlaceholderAPI. This seam exists so that if a future mcMMO change
 * ever wants a contact point (party-XP-event taps, a display hook, or a
 * true custom skill), swapping the implementation is the whole cost —
 * core, store, and surfaces do not move.
 */
public interface McmmoBridge {
    /** True if a real mcMMO connection is live behind this seam. */
    boolean isConnected();
}

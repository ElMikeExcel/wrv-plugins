package com.winkreyes.excelautosort.core;

/**
 * The Chest filter dialog's direct ON/OFF toggle (P-AUTOSORT-FILTER-1,
 * 0.2.3): the pure decision behind the toggle button both renderers
 * draw (Java selector slot 3, Bedrock main form's first button).
 *
 * The toggle is a shortcut over the existing mode semantics, never a
 * second state: ON means a filtering mode (WHITELIST or BLACKLIST),
 * OFF means Mode.OFF. Turning OFF is exactly requestMode(OFF) — it
 * applies at once and keeps the list. Turning ON returns to the
 * mode the chest last filtered with; a chest that has never filtered
 * turns on as WHITELIST ("keep only these"), the headline mode.
 * Arming a filtering mode still passes the §6.1 confirm gate in
 * SelectorFlow — the toggle changes WHERE the control lives, not
 * the safety semantics.
 */
public final class FilterToggleCore {

    private FilterToggleCore() {}

    /** True when the filter is doing anything (any filtering mode). */
    public static boolean isOn(Mode mode) {
        return mode != null && mode != Mode.OFF;
    }

    /**
     * The mode a toggle tap asks for: a filtering mode toggles to
     * OFF; OFF toggles back to the last filtering mode the chest
     * used, or WHITELIST when it never has one.
     */
    public static Mode toggleTarget(Mode current, Mode lastActive) {
        if (isOn(current)) return Mode.OFF;
        return isOn(lastActive) ? lastActive : Mode.WHITELIST;
    }

    /**
     * The last-active memory after a mode lands: a filtering mode
     * becomes the memory; OFF never erases it (turning off is a
     * pause, §5.1 — the memory is what the next ON tap restores).
     */
    public static Mode remember(Mode landed, Mode previousLast) {
        return isOn(landed) ? landed : previousLast;
    }

    /** The toggle button's state word, for both renderers' labels. */
    public static String stateWord(Mode current) {
        return isOn(current) ? "ON" : "OFF";
    }
}

// ExcelAutoSort — Iris / Mike, for Wink Reyes Valley

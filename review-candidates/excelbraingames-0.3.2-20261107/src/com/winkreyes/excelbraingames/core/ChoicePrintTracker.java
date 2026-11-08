package com.winkreyes.excelbraingames.core;

/**
 * Remembers the last choice-prompt a view already announced, so a companion
 * chat print fires exactly once per prompt: the first render of a new
 * prompt announces; re-renders of the same prompt (click feedback, wrong
 * kid-tier retries) stay silent; advancing to the next prompt announces
 * again. Pure logic, no Bukkit. Lives on the view holder (transient GUI
 * state, never persisted — the same pattern as the Sudoku palette state).
 */
public final class ChoicePrintTracker {

    private String lastKey;

    /** Returns true (and records the key) iff this key was not the last announced one. */
    public boolean markIfNew(String key) {
        if (key == null || key.equals(lastKey)) return false;
        lastKey = key;
        return true;
    }

    public String lastKey() { return lastKey; }
}

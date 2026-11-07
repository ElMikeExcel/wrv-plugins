package com.winkreyes.excelautosort.gui;

import com.winkreyes.excelautosort.core.Mode;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;

/**
 * One player's selector session (SPEC §7.3): a WORKING copy of the
 * mode + list lives here. List toggles persist the moment they are
 * tapped when they create no new rejects; an edit that would make
 * current contents into rejects is held as {@code pending*} and only
 * lands through the confirm screen (§5.8, §6.1) — cancel discards it
 * and the record keeps its persisted state exactly.
 */
public final class SelectorSession {

    public final UUID playerId;
    public List<String> targetKeys;
    public UUID chestId;              // null until a record exists

    public Mode workMode = Mode.OFF;
    public final LinkedHashSet<String> workItems = new LinkedHashSet<>();
    public Mode persistedMode = Mode.OFF;
    public final LinkedHashSet<String> persistedItems = new LinkedHashSet<>();

    public int page = 0;

    // The confirm screen's held change (null when nothing is pending).
    public Mode pendingMode;
    public LinkedHashSet<String> pendingItems;

    public SelectorSession(UUID playerId, List<String> targetKeys) {
        this.playerId = playerId;
        this.targetKeys = targetKeys;
    }

    public boolean hasPending() {
        return pendingMode != null;
    }

    public void clearPending() {
        pendingMode = null;
        pendingItems = null;
    }

    /** Cancel: the working copy snaps back to the persisted state (§5.8). */
    public void revertToPersisted() {
        workMode = persistedMode;
        workItems.clear();
        workItems.addAll(persistedItems);
        clearPending();
    }

    /** After a persist, working and persisted agree. */
    public void syncPersisted() {
        persistedMode = workMode;
        persistedItems.clear();
        persistedItems.addAll(workItems);
    }
}

// ExcelAutoSort — Iris / Mike, for Wink Reyes Valley

package com.winkreyes.excelautosort.gui;

import com.winkreyes.excelautosort.core.MaxOrgCore;
import com.winkreyes.excelautosort.engine.MaxOrgService;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * One player's Max Organization session (SPEC §15.4): the resolved
 * network and the current page of view rows. Display state only —
 * the rows name real chests and real slots; nothing an item could
 * live in is held here (the view is a window, never a warehouse).
 * A pending purchase kind lives here between the unlock tap and the
 * confirm tap; choosing nothing (or closing) clears it uncharged.
 */
public final class MaxOrgSession {

    public final UUID playerId;
    public List<MaxOrgService.NetChest> network = new ArrayList<>();
    public List<MaxOrgCore.ViewEntry> entries = new ArrayList<>();
    public int page = 0;

    /** The purchase awaiting its confirm tap: TRUE = buy, FALSE = rent, null = none. */
    public Boolean pendingBuy;

    public MaxOrgSession(UUID playerId) {
        this.playerId = playerId;
    }

    public MaxOrgService.NetChest chestOf(MaxOrgCore.ViewEntry e) {
        if (e == null) return null;
        for (MaxOrgService.NetChest nc : network) {
            if (nc.key().equals(e.chestKey())) return nc;
        }
        return null;
    }
}

// ExcelAutoSort — Iris / Mike, for Wink Reyes Valley

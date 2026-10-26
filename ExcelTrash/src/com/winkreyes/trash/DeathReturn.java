package com.winkreyes.trash;

import java.util.ArrayList;
import java.util.List;

/**
 * Pure death rule (unit-tested). Trash-slot items are still the player's when they die.
 * At LOWEST, ExcelTrash adds them to the drops (unless keepInventory is already on).
 * At MONITOR, once every other plugin is done (keep-inventory plugins such as Essentials keepinv clear the drops at HIGHEST):
 *  - inventory kept: take our stacks out of the drops (no double copy) and give them back to the player;
 *  - not kept, and we never added them (keepInventory was on at LOWEST, then turned off): add them to the drops now;
 *  - not kept, and we added them: leave the drops alone (a grave plugin may have taken them on purpose).
 * Stacks are matched by identity, never by equals, so a player's own identical items are never touched.
 */
public final class DeathReturn {
    private DeathReturn() {}

    /** Returns the stacks to give back to the player. May change drops. */
    public static <T> List<T> settle(boolean keepInventory, boolean addedToDrops, List<T> drops, List<T> ours) {
        List<T> back = new ArrayList<>();
        if (ours == null || ours.isEmpty()) return back;
        if (keepInventory) {
            for (T it : ours) { drops.removeIf(d -> d == it); back.add(it); }
        } else if (!addedToDrops) {
            drops.addAll(ours);
        }
        return back;
    }
}

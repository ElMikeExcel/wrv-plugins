package com.winkreyes.exceldeathrecord.core;

import java.util.ArrayList;
import java.util.List;

/**
 * All-or-nothing fit check for a restore, done BEFORE anything is
 * given or marked. Pure model of the target's 36 storage slots
 * plus (for a same-player restore) the armor and offhand slots:
 *
 *   1. an item that came from an equipment slot (helmet, ...,
 *      offhand) goes back into that slot when it is empty and the
 *      restore is to the same player;
 *   2. everything else first tops up existing partial stacks of
 *      the same kind (key = the item serialized at amount 1,
 *      which matches Bukkit's isSimilar), then takes new empty
 *      slots, ceil(rest / max stack) of them.
 *
 * needed = new empty storage slots the restore would take;
 * free = empty storage slots the target has now. fits = needed <= free.
 */
public final class SlotPlanner {

    /** A storage slot as the planner sees it (empty = key null). */
    public static final class Slot {
        public String key;       // null = empty
        public int amount;
        public int max;
        public Slot(String key, int amount, int max) { this.key = key; this.amount = amount; this.max = max; }
        public static Slot empty() { return new Slot(null, 0, 0); }
    }

    /** One item to restore. equipSlot = "helmet"/"chestplate"/
     *  "leggings"/"boots"/"offhand" or "" for storage. */
    public static final class Want {
        public final String key; public final int amount; public final int max; public final String equipSlot;
        public Want(String key, int amount, int max, String equipSlot) {
            this.key = key; this.amount = amount; this.max = Math.max(1, max);
            this.equipSlot = equipSlot == null ? "" : equipSlot;
        }
    }

    public static final class Plan {
        public int needed;        // new empty storage slots required
        public int free;          // empty storage slots available
        public int intoEquip;     // items going straight into armor/offhand
        public final List<String> equipPlaced = new ArrayList<>(); // per Want: slot name or ""
        public boolean fits() { return needed <= free; }
        public int short_() { return Math.max(0, needed - free); }
    }

    private SlotPlanner() {}

    /**
     * @param storage    target's storage slots (36 for a player), copied, never modified
     * @param emptyEquip equipment slot names that are empty on the target (same-player restore only; else empty list)
     */
    public static Plan plan(List<Slot> storage, List<String> emptyEquip, List<Want> wants) {
        Plan p = new Plan();
        List<Slot> sim = new ArrayList<>();
        for (Slot s : storage) {
            sim.add(new Slot(s.key, s.amount, s.max));
            if (s.key == null) p.free++;
        }
        List<String> equipLeft = new ArrayList<>(emptyEquip);
        int newSlots = 0;
        for (Want w : wants) {
            if (!w.equipSlot.isEmpty() && equipLeft.remove(w.equipSlot)) {
                p.intoEquip++;
                p.equipPlaced.add(w.equipSlot);
                continue;
            }
            p.equipPlaced.add("");
            int rest = w.amount;
            for (Slot s : sim) {
                if (rest <= 0) break;
                if (s.key != null && s.key.equals(w.key) && s.amount < s.max) {
                    int take = Math.min(rest, s.max - s.amount);
                    s.amount += take; rest -= take;
                }
            }
            while (rest > 0) {
                int take = Math.min(rest, w.max);
                // a newly opened stack can be topped up by later wants of the same kind
                sim.add(new Slot(w.key, take, w.max));
                rest -= take;
                newSlots++;
            }
        }
        p.needed = newSlots;
        return p;
    }

    /** "Can't restore: Sam needs 4 empty inventory slots and has 1. Ask them to clear 3 more, then try again." */
    public static String refusal(String target, Plan p) {
        return "Can't restore: " + target + " needs " + p.needed + " empty inventory slot" + (p.needed == 1 ? "" : "s")
                + " and has " + p.free + ". Ask them to clear " + p.short_() + " more, then try again.";
    }

    /** Confirm-screen line: "Needs 4 empty slots, Sam has 6 (OK)". */
    public static String summary(String target, Plan p) {
        String s = "Needs " + p.needed + " empty slot" + (p.needed == 1 ? "" : "s") + ", " + target + " has " + p.free;
        if (p.intoEquip > 0) s += " (+" + p.intoEquip + " straight into armor/offhand)";
        return s + (p.fits() ? " - fits" : " - DOES NOT FIT, clear " + p.short_() + " more");
    }
}

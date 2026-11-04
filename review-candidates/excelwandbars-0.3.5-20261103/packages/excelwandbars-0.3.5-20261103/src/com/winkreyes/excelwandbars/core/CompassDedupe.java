package com.winkreyes.excelwandbars.core;

/**
 * Pure plan for "exactly one menu compass". Input: amount per inventory slot
 * for slots that hold a menu compass, 0 for every other slot. Output: the new
 * amount per slot. Keeps one compass in the first slot that has one (hotbar
 * slots come first in Bukkit's storage order), sets every other one to 0.
 * Returns null when nothing needs to change.
 */
public final class CompassDedupe {
    private CompassDedupe() {}

    public static int[] plan(int[] amounts) {
        if (amounts == null) return null;
        int keep = -1, total = 0;
        for (int i = 0; i < amounts.length; i++) {
            if (amounts[i] > 0) { total += amounts[i]; if (keep < 0) keep = i; }
        }
        if (total <= 1) return null;
        int[] out = new int[amounts.length];
        out[keep] = 1;
        return out;
    }

    public static int removed(int[] amounts) {
        int total = 0;
        if (amounts != null) for (int a : amounts) if (a > 0) total += a;
        return Math.max(0, total - 1);
    }
}


package com.winkreyes.trash;

/**
 * Slot maps (pure, unit-tested).
 * Inventory mode ("Trash", 4 rows, same as 0.1.0): 0-26 red trash side, 27-35 buttons.
 * Chest mode (6 rows): 0-26 live mirror of the chest you looked at, 27-35 buttons, 36-53 red trash side.
 */
public final class Layout {
    private Layout() {}
    // inventory mode
    public static final int A_SIZE = 36, A_BAG_END = 27;
    public static final int A_CHEST = 27, A_UNDO = 31, A_OK = 33, A_CANCEL = 35;
    // chest mode
    public static final int B_SIZE = 54, B_MIRROR = 27, B_BAG_START = 36;
    public static final int B_PREV = 27, B_INFO = 28, B_INV = 29, B_UNDO = 31, B_OK = 33, B_CANCEL = 34, B_NEXT = 35;
    public static final int UNDO_SLOT = A_UNDO; // 0.1.0 compat name

    public static boolean aBag(int raw) { return raw >= 0 && raw < A_BAG_END; }
    public static boolean aButtons(int raw) { return raw >= A_BAG_END && raw < A_SIZE; }
    public static boolean bMirror(int raw) { return raw >= 0 && raw < B_MIRROR; }
    public static boolean bButtons(int raw) { return raw >= B_MIRROR && raw < B_BAG_START; }
    public static boolean bBag(int raw) { return raw >= B_BAG_START && raw < B_SIZE; }

    public static int pages(int chestSize) { return Math.max(1, (chestSize + B_MIRROR - 1) / B_MIRROR); }
    /** Real chest index shown at mirror slot, or -1 when that mirror slot is unused on this page. */
    public static int real(int page, int mirrorSlot, int chestSize) {
        if (mirrorSlot < 0 || mirrorSlot >= B_MIRROR) return -1;
        int i = page * B_MIRROR + mirrorSlot;
        return i < chestSize ? i : -1;
    }
}

package com.winkreyes.trash;

/** 4-row GUI: rows 0-2 (slots 0-26) are storage, row 3 (27-35) is buttons. */
public final class SlotPolicy {
    public static final int SIZE = 36, STORAGE = 27, UNDO_SLOT = 31;
    private SlotPolicy() {}
    public static boolean isButton(int rawSlot) { return rawSlot >= STORAGE && rawSlot < SIZE; }
    public static boolean isStorage(int rawSlot) { return rawSlot >= 0 && rawSlot < STORAGE; }
}

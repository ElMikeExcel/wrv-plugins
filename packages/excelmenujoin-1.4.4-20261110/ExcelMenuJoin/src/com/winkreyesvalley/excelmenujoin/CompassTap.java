package com.winkreyesvalley.excelmenujoin;

/**
 * 1.4.2: the compass-tap decision, kept pure so it can be unit-tested.
 * Open the menu unless another plugin denied the ITEM use. The
 * interacted-block result is deliberately not consulted: Bukkit
 * pre-denies it for every tap in the air.
 */
public final class CompassTap {
    private CompassTap() { }

    public static boolean shouldOpen(boolean itemUseDenied) {
        return !itemUseDenied;
    }
}

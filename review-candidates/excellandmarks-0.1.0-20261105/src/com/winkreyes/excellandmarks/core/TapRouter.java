package com.winkreyes.excellandmarks.core;

/**
 * Sign-tap routing (SPEC §6, Oct 15 ExcelShops lesson): the pure decision
 * the Bukkit listener delegates to. Exactly one outcome per tap; anything
 * that is not a main-hand right-click on a registered sign is IGNORE —
 * never cancelled, never a menu — so other sign handlers, MenuJoin, and
 * vanilla sign editing are untouched.
 */
public final class TapRouter {

    public enum Action { RIGHT_CLICK_BLOCK, LEFT_CLICK_BLOCK, RIGHT_CLICK_AIR, LEFT_CLICK_AIR, PHYSICAL }
    public enum Hand { HAND, OFF_HAND }
    public enum Route { IGNORE, OPEN_MENU }

    /** Debounce window for duplicate interact fires (config mirrors this default). */
    public static final long DEBOUNCE_MS = 300L;

    private TapRouter() { }

    public static Route route(Action action, Hand hand, boolean registeredSign, long msSinceLastOpen) {
        if (action != Action.RIGHT_CLICK_BLOCK) return Route.IGNORE;
        if (hand != Hand.HAND) return Route.IGNORE;
        if (!registeredSign) return Route.IGNORE;
        if (msSinceLastOpen < DEBOUNCE_MS) return Route.IGNORE;
        return Route.OPEN_MENU;
    }
}

package com.winkreyes.excellandmarks;

import com.winkreyes.excellandmarks.core.TapRouter;

/**
 * Sign-tap routing (SPEC §6, Oct 15 lesson): exactly one input opens a
 * menu — main-hand RIGHT_CLICK_BLOCK on a registered sign, outside the
 * 300 ms debounce. Everything else is IGNORE (never cancelled, never
 * touched): left clicks, off-hand duplicates, unregistered signs.
 */
public final class TapRouterTest {

    public static void main(String[] args) {
        Check ck = new Check("TapRouterTest");
        openCase(ck);
        ignoreCases(ck);
        debounce(ck);
        ck.finish();
    }

    static void openCase(Check ck) {
        ck.that(TapRouter.route(TapRouter.Action.RIGHT_CLICK_BLOCK, TapRouter.Hand.HAND, true, 10_000)
                == TapRouter.Route.OPEN_MENU, "main-hand right-click on registered sign opens the menu");
        ck.that(TapRouter.route(TapRouter.Action.RIGHT_CLICK_BLOCK, TapRouter.Hand.HAND, true, TapRouter.DEBOUNCE_MS)
                == TapRouter.Route.OPEN_MENU, "debounce boundary (exactly 300ms) opens");
    }

    static void ignoreCases(Check ck) {
        ck.that(TapRouter.route(TapRouter.Action.LEFT_CLICK_BLOCK, TapRouter.Hand.HAND, true, 10_000)
                == TapRouter.Route.IGNORE, "left-click on a sign never opens (and is not cancelled)");
        ck.that(TapRouter.route(TapRouter.Action.RIGHT_CLICK_BLOCK, TapRouter.Hand.OFF_HAND, true, 10_000)
                == TapRouter.Route.IGNORE, "off-hand echo ignored — the menu opens exactly once");
        ck.that(TapRouter.route(TapRouter.Action.RIGHT_CLICK_BLOCK, TapRouter.Hand.HAND, false, 10_000)
                == TapRouter.Route.IGNORE, "unregistered sign: no menu, no interference");
        ck.that(TapRouter.route(TapRouter.Action.RIGHT_CLICK_AIR, TapRouter.Hand.HAND, true, 10_000)
                == TapRouter.Route.IGNORE, "air clicks ignored");
        ck.that(TapRouter.route(TapRouter.Action.LEFT_CLICK_AIR, TapRouter.Hand.HAND, true, 10_000)
                == TapRouter.Route.IGNORE, "left air clicks ignored");
        ck.that(TapRouter.route(TapRouter.Action.PHYSICAL, TapRouter.Hand.HAND, true, 10_000)
                == TapRouter.Route.IGNORE, "physical interactions ignored");
    }

    static void debounce(Check ck) {
        ck.that(TapRouter.route(TapRouter.Action.RIGHT_CLICK_BLOCK, TapRouter.Hand.HAND, true, 0)
                == TapRouter.Route.IGNORE, "same-tick double fire debounced");
        ck.that(TapRouter.route(TapRouter.Action.RIGHT_CLICK_BLOCK, TapRouter.Hand.HAND, true, 299)
                == TapRouter.Route.IGNORE, "299ms debounced");
        ck.that(TapRouter.route(TapRouter.Action.RIGHT_CLICK_BLOCK, TapRouter.Hand.HAND, true, 301)
                == TapRouter.Route.OPEN_MENU, "301ms opens again");
    }
}

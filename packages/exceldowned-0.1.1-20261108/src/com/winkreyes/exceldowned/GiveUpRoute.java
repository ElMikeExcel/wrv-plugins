package com.winkreyes.exceldowned;

/**
 * Platform routing for the give-up confirm UI (0.1.1) — a pure decision
 * (unit-tested, no Bukkit): Java players get the inventory menu,
 * Bedrock players get the Cumulus form when the Geyser form API is
 * available, and anyone else falls back to the 0.1.0 chat arm/confirm
 * so the typed path never breaks.
 *
 * Excel — Wink Reyes Valley (Iris / Mike).
 */
public final class GiveUpRoute {

    public enum Route { JAVA_MENU, BEDROCK_FORM, CHAT_FALLBACK }

    private GiveUpRoute() {}

    public static Route route(boolean bedrockPlayer, boolean formApiAvailable) {
        if (!bedrockPlayer) return Route.JAVA_MENU;
        return formApiAvailable ? Route.BEDROCK_FORM : Route.CHAT_FALLBACK;
    }
}

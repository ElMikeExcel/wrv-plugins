package com.winkreyesvalley.excelmenujoin;

import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.ServicesManager;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.UUID;

/**
 * The ExcelPouch consultation (ExcelMenuJoin 1.4.0): before giving
 * a menu compass, ask ExcelPouch's PouchQuery service whether the
 * player's Utility Pouch already holds one — a parked compass must
 * never duplicate on join or respawn.
 *
 * FAIL-OPEN BY DESIGN: no ExcelPouch plugin, no service, or any
 * error along the way reads as "no answer", and the give proceeds.
 * If the pouch is ever removed, the compass self-heals on the next
 * join/respawn exactly as it did in 1.3.0. MenuJoin 1.4.0 is safe
 * to run with or without ExcelPouch installed.
 *
 * The service is invoked reflectively by method name
 * (holdsKind(UUID, String)) so this plugin has NO compile-time or
 * load-time dependency on ExcelPouch classes — Bukkit associates a
 * service registration with the plugin that registered it, so the
 * provider is found by scanning ExcelPouch's registrations, which
 * also sidesteps classloader questions entirely.
 */
public final class PouchCompassCheck {
    private PouchCompassCheck() { }

    public static final String MENU_COMPASS_KIND = "menu_compass";

    /** The give decision: give unless the player already has a compass in hand/inventory OR the pouch provably holds one. */
    public static boolean shouldGive(boolean hasCompassAlready, Boolean pouchHoldsCompass) {
        if (hasCompassAlready) return false;
        return pouchHoldsCompass == null || !pouchHoldsCompass;
    }

    /**
     * Ask ExcelPouch whether this player's pouch holds a menu compass.
     *
     * @return TRUE/FALSE when the service answered; null when there
     *         is no answer (plugin absent, no service, error) — the
     *         caller must treat null as "give" (fail-open).
     */
    public static Boolean pouchHoldsMenuCompass(ServicesManager services, Plugin pouchPlugin, UUID player) {
        if (services == null || pouchPlugin == null || player == null) return null;
        try {
            Collection<RegisteredServiceProvider<?>> registrations = services.getRegistrations(pouchPlugin);
            for (RegisteredServiceProvider<?> rsp : registrations) {
                Object provider = rsp.getProvider();
                if (provider == null) continue;
                try {
                    Method m = provider.getClass().getMethod("holdsKind", UUID.class, String.class);
                    Object answer = m.invoke(provider, player, MENU_COMPASS_KIND);
                    if (answer instanceof Boolean b) return b;
                } catch (NoSuchMethodException notTheService) {
                    // Some other service of the same plugin: keep looking.
                }
            }
        } catch (Exception e) {
            return null;
        }
        return null;
    }
}

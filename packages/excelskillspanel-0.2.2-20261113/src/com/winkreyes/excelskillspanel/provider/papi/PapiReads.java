package com.winkreyes.excelskillspanel.provider.papi;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;

/**
 * PlaceholderAPI reads by reflection against PAPI's own class
 * loader — no compile-time PAPI dependency, and a missing or
 * disabled PAPI simply yields null (the caller omits the field).
 * A placeholder that PAPI returns UNCHANGED (unregistered
 * expansion) also reads as null, so an absent expansion never
 * leaks a raw "%...%" string into the panel. Failures are the
 * caller's to log where they matter; this helper is deliberately
 * quiet-but-honest: null means "no verified value", never a guess.
 *
 * Excel — Wink Reyes Valley (Iris / Mike).
 */
public final class PapiReads {
    private PapiReads() {}

    private static volatile Method setPlaceholders;
    private static volatile boolean resolved;

    /** One placeholder's value for a player, or null when PAPI or
     *  the expansion is unavailable or the value is empty. */
    public static String get(OfflinePlayer player, String placeholder) {
        if (player == null || placeholder == null) return null;
        try {
            Method m = method();
            if (m == null) return null;
            Object out = m.invoke(null, player, placeholder);
            if (!(out instanceof String s)) return null;
            if (s.isBlank() || s.equals(placeholder)) return null;
            return s;
        } catch (Throwable t) {
            return null;
        }
    }

    /** A placeholder parsed as a long, or null. */
    public static Long getLong(OfflinePlayer player, String placeholder) {
        String s = get(player, placeholder);
        if (s == null) return null;
        try {
            return Long.parseLong(s.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** A placeholder parsed as an int, or null. */
    public static Integer getInt(OfflinePlayer player, String placeholder) {
        Long v = getLong(player, placeholder);
        return v == null ? null : v.intValue();
    }

    private static Method method() {
        if (!resolved) {
            synchronized (PapiReads.class) {
                if (!resolved) {
                    resolved = true;
                    try {
                        Plugin papi = Bukkit.getPluginManager().getPlugin("PlaceholderAPI");
                        if (papi != null && papi.isEnabled()) {
                            Class<?> api = Class.forName("me.clip.placeholderapi.PlaceholderAPI",
                                    true, papi.getClass().getClassLoader());
                            setPlaceholders = api.getMethod("setPlaceholders",
                                    OfflinePlayer.class, String.class);
                        }
                    } catch (Throwable t) {
                        setPlaceholders = null;
                    }
                }
            }
        }
        return setPlaceholders;
    }
}

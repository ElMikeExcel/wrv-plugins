package com.winkreyes.excelguide;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/**
 * Online players' chat nicknames for the Guide's "talking to a player"
 * gate (0.3.4). EssentialsX is a soft dependency read by reflection:
 * {@code Essentials#getUser(Player)} then {@code User#getNickname()}.
 * When Essentials is missing, disabled or its API changes, the
 * player's display name is used instead (Essentials normally writes
 * the nickname there too). Never throws; read-only.
 */
final class Nicknames {

    private Nicknames() {}

    private static volatile boolean essBroken;

    /** name -> nickname (or display name) for everyone online. */
    static Map<String, String> online() {
        Map<String, String> out = new LinkedHashMap<>();
        try {
            for (Player p : Bukkit.getOnlinePlayers()) {
                String nick = essentialsNick(p);
                if (nick == null || nick.isBlank()) {
                    nick = displayName(p);
                }
                out.put(p.getName(), nick);
            }
        } catch (Throwable ignored) {
            // The Guide never breaks chat over a nickname lookup.
        }
        return out;
    }

    private static String displayName(Player p) {
        try {
            Component c = p.displayName();
            return c == null ? null : PlainTextComponentSerializer.plainText().serialize(c);
        } catch (Throwable t) {
            return null;
        }
    }

    private static String essentialsNick(Player p) {
        if (essBroken) {
            return null;
        }
        try {
            Plugin ess = Bukkit.getPluginManager().getPlugin("Essentials");
            if (ess == null || !ess.isEnabled()) {
                return null;
            }
            Method getUser = ess.getClass().getMethod("getUser", Player.class);
            Object user = getUser.invoke(ess, p);
            if (user == null) {
                return null;
            }
            Object nick = user.getClass().getMethod("getNickname").invoke(user);
            return nick == null ? null : nick.toString();
        } catch (Throwable t) {
            essBroken = true; // API mismatch: stay on display names
            return null;
        }
    }
}

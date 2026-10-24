package com.winkreyes.exceldeathrecord;

import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.lang.reflect.Method;
import java.util.Locale;
import java.util.UUID;

/**
 * Name lookups for restore. Real player names always work
 * (Bedrock names with or without the leading dot). An EssentialsX
 * nickname also resolves: for online players through the display
 * name / Essentials user, for offline players through
 * plugins/Essentials/userdata/<uuid>.yml (read-only).
 */
final class Names {

    private Names() {}

    static String strip(String s) {
        if (s == null) return "";
        String t = s.replaceAll("(?i)[§&][0-9A-FK-ORX]", "").replaceAll("(?i)[§&]#[0-9A-F]{6}", "");
        t = t.trim();
        while (t.startsWith("~")) t = t.substring(1);
        return t.trim();
    }

    private static boolean sameName(String a, String b) {
        String x = a.startsWith(".") ? a.substring(1) : a;
        String y = b.startsWith(".") ? b.substring(1) : b;
        return x.equalsIgnoreCase(y);
    }

    /** An online player by real name, then by nickname; null if none. */
    static Player online(String typed) {
        if (typed == null || typed.isBlank()) return null;
        String t = typed.trim();
        Player p = Bukkit.getPlayerExact(t);
        if (p != null) return p;
        for (Player o : Bukkit.getOnlinePlayers()) if (sameName(o.getName(), t)) return o;
        String want = strip(t);
        for (Player o : Bukkit.getOnlinePlayers()) {
            String disp = "";
            try { disp = PlainTextComponentSerializer.plainText().serialize(o.displayName()); } catch (Throwable ignored) { }
            if (!disp.isEmpty() && strip(disp).equalsIgnoreCase(want)) return o;
            String nick = essentialsNick(o);
            if (!nick.isEmpty() && strip(nick).equalsIgnoreCase(want)) return o;
        }
        return null;
    }

    private static String essentialsNick(Player p) {
        try {
            Plugin ess = Bukkit.getPluginManager().getPlugin("Essentials");
            if (ess == null || !ess.isEnabled()) return "";
            Method getUser = ess.getClass().getMethod("getUser", Player.class);
            Object user = getUser.invoke(ess, p);
            if (user == null) return "";
            Object nick = user.getClass().getMethod("getNickname").invoke(user);
            return nick == null ? "" : nick.toString();
        } catch (Throwable t) {
            return "";
        }
    }

    /** Offline nickname lookup in Essentials userdata; null if none. */
    static UUID offlineNick(String typed) {
        String want = strip(typed).toLowerCase(Locale.ROOT);
        if (want.isEmpty()) return null;
        File dir = new File(Bukkit.getPluginsFolder(), "Essentials/userdata");
        File[] files = dir.listFiles((d, n) -> n.endsWith(".yml"));
        if (files == null) return null;
        for (File f : files) {
            try {
                YamlConfiguration y = YamlConfiguration.loadConfiguration(f);
                String nick = y.getString("nickname", "");
                if (nick != null && !nick.isEmpty() && strip(nick).equalsIgnoreCase(want)) {
                    return UUID.fromString(f.getName().substring(0, f.getName().length() - 4));
                }
            } catch (Throwable ignored) { }
        }
        return null;
    }
}

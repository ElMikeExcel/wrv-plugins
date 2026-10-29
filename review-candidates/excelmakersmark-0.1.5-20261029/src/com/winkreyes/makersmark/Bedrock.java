/* Excel WRV maintained source; original attribution and license preserved. */ package com.winkreyes.makersmark;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;
import java.util.function.IntConsumer;
import java.util.logging.Logger;

/**
 * Bedrock detection and simple forms through Geyser's class loader (same reflection approach as
 * ExcelWandBars / ExcelBedrockMenu), with Floodgate as a fallback for detection. No compile-time dependency.
 * Form callbacks arrive off the main thread; callers hop back before touching Bukkit.
 */
final class Bedrock {
    private final Logger log;
    Bedrock(Logger log) { this.log = log; }

    private static ClassLoader loader(String name) {
        Plugin g = Bukkit.getPluginManager().getPlugin(name);
        return g != null && g.isEnabled() ? g.getClass().getClassLoader() : null;
    }

    static boolean isBedrock(Player pl) {
        try {
            ClassLoader l = loader("Geyser-Spigot");
            if (l != null) {
                Class<?> api = Class.forName("org.geysermc.geyser.api.GeyserApi", true, l);
                Object a = api.getMethod("api").invoke(null);
                if (Boolean.TRUE.equals(api.getMethod("isBedrockPlayer", UUID.class).invoke(a, pl.getUniqueId()))) return true;
            }
        } catch (Throwable ignored) { }
        try {
            ClassLoader l = loader("floodgate");
            if (l != null) {
                Class<?> api = Class.forName("org.geysermc.floodgate.api.FloodgateApi", true, l);
                Object a = api.getMethod("getInstance").invoke(null);
                return Boolean.TRUE.equals(api.getMethod("isFloodgatePlayer", UUID.class).invoke(a, pl.getUniqueId()));
            }
        } catch (Throwable ignored) { }
        return false;
    }

    boolean simple(Player p, String title, String content, List<String> buttons, IntConsumer cb) {
        try {
            ClassLoader l = loader("Geyser-Spigot");
            if (l == null) return false;
            Class<?> api = Class.forName("org.geysermc.geyser.api.GeyserApi", true, l);
            Object a = api.getMethod("api").invoke(null);
            Object conn = api.getMethod("connectionByUuid", UUID.class).invoke(a, p.getUniqueId());
            if (conn == null) return false;
            Object b = Class.forName("org.geysermc.cumulus.form.SimpleForm", true, l).getMethod("builder").invoke(null);
            Class<?> bt = Class.forName("org.geysermc.cumulus.form.SimpleForm$Builder", true, l);
            Class<?> fb = Class.forName("org.geysermc.cumulus.form.util.FormBuilder", true, l);
            Class<?> resp = Class.forName("org.geysermc.cumulus.response.SimpleFormResponse", true, l);
            Class<?> res = Class.forName("org.geysermc.cumulus.response.result.FormResponseResult", true, l);
            Class<?> ok = Class.forName("org.geysermc.cumulus.response.result.ValidFormResponseResult", true, l);
            fb.getMethod("title", String.class).invoke(b, title);
            bt.getMethod("content", String.class).invoke(b, content);
            for (String s : buttons) bt.getMethod("button", String.class).invoke(b, s);
            Method isValid = res.getMethod("isValid"), response = ok.getMethod("response"), id = resp.getMethod("clickedButtonId");
            java.util.function.BiConsumer<Object, Object> h = (form, r) -> {
                int clicked = -1;
                try { if (Boolean.TRUE.equals(isValid.invoke(r))) clicked = (Integer) id.invoke(response.invoke(r)); }
                catch (Exception e) { clicked = -1; }
                cb.accept(clicked);
            };
            fb.getMethod("resultHandler", java.util.function.BiConsumer.class).invoke(b, h);
            Object form = fb.getMethod("build").invoke(b);
            for (Method m : conn.getClass().getMethods()) {
                if (m.getName().equals("sendForm") && m.getParameterCount() == 1 && m.getParameterTypes()[0].isAssignableFrom(form.getClass())) {
                    return Boolean.TRUE.equals(m.invoke(conn, form));
                }
            }
            return false;
        } catch (Throwable t) {
            log.warning("Bedrock form failed: " + t.getClass().getSimpleName() + ": " + t.getMessage());
            return false;
        }
    }
}

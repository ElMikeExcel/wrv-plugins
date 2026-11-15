package com.winkreyes.excelwhatsnew;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;
import java.util.function.IntConsumer;
import java.util.logging.Logger;

/**
 * Bedrock detection and the OK pop-up (a Cumulus SimpleForm), all by
 * reflection so there is no compile-time Floodgate or Geyser dependency.
 * Floodgate first (FloodgateApi.sendForm), Geyser's connection as fallback.
 * The form is built with the same class loader that sends it, because
 * Floodgate and Geyser each carry their own copy of Cumulus.
 * Form callbacks arrive off the main thread; the caller hops back.
 */
final class Bedrock {
    private final Logger log;
    Bedrock(Logger log) { this.log = log; }

    private static ClassLoader loader(String plugin) {
        Plugin p = Bukkit.getPluginManager().getPlugin(plugin);
        return p != null && p.isEnabled() ? p.getClass().getClassLoader() : null;
    }

    private static Object floodgate(ClassLoader l) throws Exception {
        Class<?> api = Class.forName("org.geysermc.floodgate.api.FloodgateApi", true, l);
        return api.getMethod("getInstance").invoke(null);
    }

    static boolean isBedrock(Player p) {
        UUID id = p.getUniqueId();
        try {
            ClassLoader l = loader("floodgate");
            if (l != null) {
                Object a = floodgate(l);
                Object r = a.getClass().getMethod("isFloodgatePlayer", UUID.class).invoke(a, id);
                if (Boolean.TRUE.equals(r)) return true;
            }
        } catch (Throwable ignored) { }
        try {
            ClassLoader l = loader("Geyser-Spigot");
            if (l != null) {
                Class<?> api = Class.forName("org.geysermc.geyser.api.GeyserApi", true, l);
                Object a = api.getMethod("api").invoke(null);
                if (Boolean.TRUE.equals(api.getMethod("isBedrockPlayer", UUID.class).invoke(a, id))) return true;
            }
        } catch (Throwable ignored) { }
        return false;
    }

    /** Build a SimpleForm with loader l. cb gets the clicked button id, or -1 when closed. */
    static Object buildSimple(ClassLoader l, String title, String content, List<String> buttons, IntConsumer cb) throws Exception {
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
        return fb.getMethod("build").invoke(b);
    }

    /** Send the pop-up. Returns "floodgate", "geyser" or null when it could not be sent. */
    String send(Player p, String title, String content, List<String> buttons, IntConsumer cb) {
        UUID id = p.getUniqueId();
        try {
            ClassLoader l = loader("floodgate");
            if (l != null) {
                Object a = floodgate(l);
                Object form = buildSimple(l, title, content, buttons, cb);
                for (Method m : a.getClass().getMethods()) {
                    if (m.getName().equals("sendForm") && m.getParameterCount() == 2 && m.getParameterTypes()[0] == UUID.class
                            && m.getParameterTypes()[1].isAssignableFrom(form.getClass())) {
                        if (Boolean.TRUE.equals(m.invoke(a, id, form))) return "floodgate";
                    }
                }
            }
        } catch (Throwable t) { log.warning("Floodgate form failed: " + t.getClass().getSimpleName() + ": " + t.getMessage()); }
        try {
            ClassLoader l = loader("Geyser-Spigot");
            if (l == null) return null;
            Class<?> api = Class.forName("org.geysermc.geyser.api.GeyserApi", true, l);
            Object a = api.getMethod("api").invoke(null);
            Object conn = api.getMethod("connectionByUuid", UUID.class).invoke(a, id);
            if (conn == null) return null;
            Object form = buildSimple(l, title, content, buttons, cb);
            for (Method m : conn.getClass().getMethods()) {
                if (m.getName().equals("sendForm") && m.getParameterCount() == 1 && m.getParameterTypes()[0].isAssignableFrom(form.getClass())) {
                    m.invoke(conn, form);
                    return "geyser";
                }
            }
        } catch (Throwable t) { log.warning("Geyser form failed: " + t.getClass().getSimpleName() + ": " + t.getMessage()); }
        return null;
    }
}

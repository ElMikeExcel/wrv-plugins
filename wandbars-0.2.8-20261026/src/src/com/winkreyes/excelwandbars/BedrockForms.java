package com.winkreyes.excelwandbars;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;
import java.util.function.IntConsumer;
import java.util.logging.Logger;

/**
 * Bedrock forms through Geyser's own class loader (same reflection approach
 * as ExcelTogether / ExcelBedrockMenu), so there is no compile-time Geyser
 * or Cumulus dependency. Callbacks arrive on Geyser's thread; the caller
 * hops back to the main thread before touching Bukkit or Magic.
 */
final class BedrockForms {
    private final Logger log;
    BedrockForms(Logger log) { this.log = log; }

    static ClassLoader geyserLoader() {
        Plugin g = Bukkit.getPluginManager().getPlugin("Geyser-Spigot");
        return g != null && g.isEnabled() ? g.getClass().getClassLoader() : null;
    }

    static boolean isBedrock(Player pl) {
        try {
            ClassLoader l = geyserLoader();
            if (l == null) return false;
            Class<?> api = Class.forName("org.geysermc.geyser.api.GeyserApi", true, l);
            Object a = api.getMethod("api").invoke(null);
            return Boolean.TRUE.equals(api.getMethod("isBedrockPlayer", UUID.class).invoke(a, pl.getUniqueId()));
        } catch (Throwable t) { return false; }
    }

    /** Build a Cumulus SimpleForm; cb gets the clicked button id, or -1 when closed. Public for the Cumulus build test. */
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
            try {
                if (Boolean.TRUE.equals(isValid.invoke(r))) clicked = (Integer) id.invoke(response.invoke(r));
            } catch (Exception e) { clicked = -1; }
            cb.accept(clicked);
        };
        fb.getMethod("resultHandler", java.util.function.BiConsumer.class).invoke(b, h);
        return fb.getMethod("build").invoke(b);
    }

    boolean simple(Player p, String title, String content, List<String> buttons, IntConsumer cb) {
        try {
            ClassLoader l = geyserLoader();
            if (l == null) return false;
            Class<?> api = Class.forName("org.geysermc.geyser.api.GeyserApi", true, l);
            Object a = api.getMethod("api").invoke(null);
            Object conn = api.getMethod("connectionByUuid", UUID.class).invoke(a, p.getUniqueId());
            if (conn == null) return false;
            Object form = buildSimple(l, title, content, buttons, cb);
            for (Method m : conn.getClass().getMethods()) {
                if (m.getName().equals("sendForm") && m.getParameterCount() == 1 && m.getParameterTypes()[0].isAssignableFrom(form.getClass())) {
                    return Boolean.TRUE.equals(m.invoke(conn, form));
                }
            }
            log.warning("No compatible sendForm on the Geyser connection");
            return false;
        } catch (Throwable t) {
            log.warning("Bedrock form failed: " + t.getClass().getSimpleName() + ": " + t.getMessage());
            return false;
        }
    }
}

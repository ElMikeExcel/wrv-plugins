package com.winkreyes.exceltrainingdummy;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.UUID;
import java.util.function.IntConsumer;
import java.util.logging.Logger;

/**
 * Bedrock form sending — the house pattern (ExcelTour / WandBars):
 * reflection-only Cumulus SimpleForm via Floodgate, then Geyser.
 * Every failure answers false and the caller falls back to chat —
 * a form failure never blocks a control. Button labels carry NO
 * legacy color codes (the WandBars 0.3.14 form-contrast lesson:
 * pale text dies on Bedrock button fills; plain dark ink survives).
 */
public final class BedrockForms {
    private BedrockForms() {}

    public static boolean sendSimpleForm(Player player, String title, String body,
                                         List<String> buttons, IntConsumer onButton,
                                         Logger log) {
        if (sendViaFloodgate(player, title, body, buttons, onButton, log)) return true;
        return sendViaGeyser(player, title, body, buttons, onButton, log);
    }

    private static boolean sendViaFloodgate(Player player, String title, String body,
                                            List<String> buttons, IntConsumer onButton, Logger log) {
        try {
            Plugin p = Bukkit.getPluginManager().getPlugin("floodgate");
            if (p == null || !p.isEnabled()) return false;
            ClassLoader cl = p.getClass().getClassLoader();
            Class<?> formClass = Class.forName("org.geysermc.cumulus.SimpleForm", true, cl);
            Object builder = formClass.getMethod("builder").invoke(null);
            Class<?> bc = builder.getClass();
            bc.getMethod("title", String.class).invoke(builder, title);
            bc.getMethod("content", String.class).invoke(builder, body);
            for (String b : buttons) bc.getMethod("button", String.class).invoke(builder, b);
            attachHandlers(bc, builder, cl, onButton);
            Object form = bc.getMethod("build").invoke(builder);
            Class<?> apiClass = Class.forName("org.geysermc.floodgate.api.FloodgateApi", true, cl);
            Object api = apiClass.getMethod("getInstance").invoke(null);
            Object r = apiClass.getMethod("sendForm", UUID.class, Class.forName("org.geysermc.cumulus.Form", true, cl))
                    .invoke(api, player.getUniqueId(), form);
            return !Boolean.FALSE.equals(r);
        } catch (Exception e) {
            log.fine("ExcelTrainingDummy: Floodgate form send failed, trying Geyser: " + e);
            return false;
        }
    }

    private static boolean sendViaGeyser(Player player, String title, String body,
                                         List<String> buttons, IntConsumer onButton, Logger log) {
        try {
            Plugin p = Bukkit.getPluginManager().getPlugin("Geyser-Spigot");
            if (p == null || !p.isEnabled()) return false;
            ClassLoader cl = p.getClass().getClassLoader();
            Class<?> apiClass = Class.forName("org.geysermc.geyser.api.GeyserApi", true, cl);
            Object api = apiClass.getMethod("api").invoke(null);
            Object conn = apiClass.getMethod("connectionByUuid", UUID.class).invoke(api, player.getUniqueId());
            if (conn == null) return false;
            Class<?> formClass = Class.forName("org.geysermc.cumulus.form.SimpleForm", true, cl);
            Object builder = formClass.getMethod("builder").invoke(null);
            Class<?> bc = builder.getClass();
            bc.getMethod("title", String.class).invoke(builder, title);
            bc.getMethod("content", String.class).invoke(builder, body);
            for (String b : buttons) bc.getMethod("button", String.class).invoke(builder, b);
            attachHandlers(bc, builder, cl, onButton);
            Object form = bc.getMethod("build").invoke(builder);
            conn.getClass().getMethod("sendForm", Class.forName("org.geysermc.cumulus.form.Form", true, cl))
                    .invoke(conn, form);
            return true;
        } catch (Exception e) {
            log.fine("ExcelTrainingDummy: Geyser form send failed, chat fallback applies: " + e);
            return false;
        }
    }

    private static void attachHandlers(Class<?> builderClass, Object builder, ClassLoader cl,
                                       IntConsumer onButton) throws Exception {
        for (Method m : builderClass.getMethods()) {
            if (!m.getName().equals("validResultHandler") || m.getParameterCount() != 1) continue;
            Class<?> param = m.getParameterTypes()[0];
            if (!param.isInterface()) continue;
            InvocationHandler h = (proxy, method, args) -> {
                if (args != null && args.length >= 1 && args[0] != null) {
                    try {
                        Object idx = args[0].getClass().getMethod("clickedButtonId").invoke(args[0]);
                        if (idx instanceof Number) onButton.accept(((Number) idx).intValue());
                    } catch (Exception ignored) { }
                }
                return null;
            };
            Object proxy = Proxy.newProxyInstance(cl, new Class<?>[]{param}, h);
            m.invoke(builder, proxy);
            return;
        }
    }
}

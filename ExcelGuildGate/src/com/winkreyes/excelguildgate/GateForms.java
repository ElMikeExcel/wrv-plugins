package com.winkreyes.excelguildgate;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.UUID;
import java.util.function.IntConsumer;
import java.util.logging.Logger;

/**
 * Reflection-based Bedrock form helpers, mirroring the GeyserForms helper in
 * ExcelBedrockMenu: everything goes through the Geyser plugin's own
 * classloader, so this plugin has no compile-time Geyser/Floodgate/Cumulus
 * dependency and enables cleanly when none of them are installed.
 *
 * Bedrock detection: Geyser API first, Floodgate API as a second check
 * (either one saying "Bedrock" is enough).
 */
final class GateForms {

    private GateForms() {}

    /** True if the player is connected via Bedrock (Geyser or Floodgate). */
    static boolean isBedrockPlayer(Player player, Logger log) {
        if (isBedrockViaGeyser(player)) {
            return true;
        }
        return isBedrockViaFloodgate(player);
    }

    private static boolean isBedrockViaGeyser(Player player) {
        try {
            Plugin geyser = Bukkit.getPluginManager().getPlugin("Geyser-Spigot");
            if (geyser == null || !geyser.isEnabled()) {
                return false;
            }
            ClassLoader loader = geyser.getClass().getClassLoader();
            Class<?> apiClass = Class.forName("org.geysermc.geyser.api.GeyserApi",
                    true, loader);
            Object api = apiClass.getMethod("api").invoke(null);
            Object result = apiClass.getMethod("isBedrockPlayer", UUID.class)
                    .invoke(api, player.getUniqueId());
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean isBedrockViaFloodgate(Player player) {
        try {
            Plugin floodgate = Bukkit.getPluginManager().getPlugin("floodgate");
            if (floodgate == null || !floodgate.isEnabled()) {
                return false;
            }
            ClassLoader loader = floodgate.getClass().getClassLoader();
            Class<?> apiClass = Class.forName("org.geysermc.floodgate.api.FloodgateApi",
                    true, loader);
            Object api = apiClass.getMethod("getInstance").invoke(null);
            Object result = apiClass.getMethod("isFloodgatePlayer", UUID.class)
                    .invoke(api, player.getUniqueId());
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Sends a Cumulus SimpleForm with the given title, body text and button
     * labels. onSelect receives the clicked button id (-1 if closed or never
     * fired). Returns true if the form was sent.
     */
    static boolean sendSimpleForm(Player player, String title, String content,
                                  String[] buttons, IntConsumer onSelect, Logger log) {
        String step = "start";
        try {
            Plugin geyserPlugin = Bukkit.getPluginManager().getPlugin("Geyser-Spigot");
            if (geyserPlugin == null || !geyserPlugin.isEnabled()) {
                return false;
            }
            ClassLoader loader = geyserPlugin.getClass().getClassLoader();
            UUID uuid = player.getUniqueId();

            step = "GeyserApi.api()";
            Class<?> apiClass = Class.forName("org.geysermc.geyser.api.GeyserApi",
                    true, loader);
            Object api = apiClass.getMethod("api").invoke(null);

            step = "connectionByUuid";
            Object connection = apiClass.getMethod("connectionByUuid", UUID.class)
                    .invoke(api, uuid);
            if (connection == null) {
                return false;
            }

            step = "SimpleForm.builder()";
            Class<?> formClass = Class.forName("org.geysermc.cumulus.form.SimpleForm",
                    true, loader);
            Object builder = formClass.getMethod("builder").invoke(null);
            Class<?> builderClass = builder.getClass();

            step = "title/content/buttons";
            builderClass.getMethod("title", String.class).invoke(builder, title);
            builderClass.getMethod("content", String.class).invoke(builder, content);
            Method buttonMethod = builderClass.getMethod("button", String.class);
            for (String b : buttons) {
                buttonMethod.invoke(builder, b);
            }

            step = "validResultHandler";
            Class<?> responseClass = Class.forName(
                    "org.geysermc.cumulus.response.SimpleFormResponse", true, loader);
            Class<?> biConsumerClass = Class.forName("java.util.function.BiConsumer",
                    true, loader);
            Object handler = Proxy.newProxyInstance(loader,
                    new Class<?>[]{biConsumerClass},
                    (proxy, method, methodArgs) -> {
                        if (method.getName().equals("accept") && methodArgs.length == 2) {
                            Object response = methodArgs[1];
                            int id = (int) responseClass.getMethod("clickedButtonId")
                                    .invoke(response);
                            onSelect.accept(id);
                        }
                        return null;
                    });
            Method vrh = findHandler(builderClass, "validResultHandler", handler);
            if (vrh == null) {
                log.warning("GateForms: no compatible validResultHandler (SimpleForm)");
                return false;
            }
            vrh.invoke(builder, handler);

            step = "build+send";
            Object form = builderClass.getMethod("build").invoke(builder);
            Method sendForm = findSendForm(connection.getClass(), form.getClass());
            if (sendForm == null) {
                log.warning("GateForms: no compatible sendForm (SimpleForm)");
                return false;
            }
            return (boolean) sendForm.invoke(connection, form);
        } catch (Exception e) {
            log.warning("GateForms SimpleForm '" + title + "' failed at [" + step + "]: "
                    + e.getClass().getSimpleName() + ": " + e.getMessage());
            return false;
        }
    }

    private static Method findHandler(Class<?> builderClass, String name, Object handler) {
        for (Method m : builderClass.getMethods()) {
            if (m.getName().equals(name) && m.getParameterCount() == 1) {
                Class<?> pt = m.getParameterTypes()[0];
                if (pt.isAssignableFrom(handler.getClass())) {
                    return m;
                }
            }
        }
        return null;
    }

    private static Method findSendForm(Class<?> connectionClass, Class<?> formClass) {
        for (Method m : connectionClass.getMethods()) {
            if (m.getName().equals("sendForm") && m.getParameterCount() == 1) {
                Class<?> pt = m.getParameterTypes()[0];
                if (pt.isAssignableFrom(formClass)) {
                    return m;
                }
            }
        }
        return null;
    }
}

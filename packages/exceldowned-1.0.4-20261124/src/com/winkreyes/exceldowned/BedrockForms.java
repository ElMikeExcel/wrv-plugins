package com.winkreyes.exceldowned;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.logging.Logger;

/**
 * Reflection-based Geyser form helpers — the ExcelBedrockMenu
 * GeyserForms pattern, carried into ExcelDowned for the give-up
 * confirm form (0.1.1). Uses Geyser's own classloader so we never
 * define Geyser API types ourselves and avoid loader constraint
 * violations. No compile-time dependency on Geyser or Cumulus: every
 * failure answers false and the caller falls back to the chat
 * arm/confirm, so the typed path never breaks.
 *
 * Excel — Wink Reyes Valley (Iris / Mike).
 */
final class BedrockForms {

    private BedrockForms() {}

    /** True when the Geyser plugin is present and enabled (pre-check). */
    static boolean isFormApiAvailable() {
        Plugin geyser = Bukkit.getPluginManager().getPlugin("Geyser-Spigot");
        return geyser != null && geyser.isEnabled();
    }

    /** True if the player is connected via Geyser (Bedrock). */
    static boolean isBedrockPlayer(Player player, Logger log) {
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

    /**
     * Sends a ModalForm (two-button) dialog. onResult receives true if
     * the first button was clicked, false for the second; onClosed runs
     * when the form is dismissed without a choice. Returns true if the
     * form was sent.
     */
    static boolean sendModalForm(Player player, String title, String content,
                                 String button1, String button2,
                                 Consumer<Boolean> onResult, Runnable onClosed,
                                 Logger log) {
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

            step = "ModalForm.builder()";
            Class<?> formClass = Class.forName("org.geysermc.cumulus.form.ModalForm",
                    true, loader);
            Object builder = formClass.getMethod("builder").invoke(null);
            Class<?> builderClass = builder.getClass();

            step = "title/content/buttons";
            builderClass.getMethod("title", String.class).invoke(builder, title);
            builderClass.getMethod("content", String.class).invoke(builder, content);
            builderClass.getMethod("button1", String.class).invoke(builder, button1);
            builderClass.getMethod("button2", String.class).invoke(builder, button2);

            step = "validResultHandler";
            Class<?> responseClass = Class.forName(
                    "org.geysermc.cumulus.response.ModalFormResponse", true, loader);
            Class<?> biConsumerClass = Class.forName("java.util.function.BiConsumer",
                    true, loader);
            Object handler = Proxy.newProxyInstance(loader,
                    new Class<?>[]{biConsumerClass},
                    (proxy, method, methodArgs) -> {
                        if (method.getName().equals("accept") && methodArgs.length == 2) {
                            Object response = methodArgs[1];
                            boolean first = (boolean) responseClass.getMethod("clickedFirst")
                                    .invoke(response);
                            onResult.accept(first);
                        }
                        return null;
                    });
            Method vrh = findHandler(builderClass, "validResultHandler", handler);
            if (vrh == null) {
                log.warning("BedrockForms: no compatible validResultHandler (ModalForm)");
                return false;
            }
            vrh.invoke(builder, handler);

            if (onClosed != null) {
                builderClass.getMethod("closedOrInvalidResultHandler", java.util.function.Consumer.class)
                        .invoke(builder, (java.util.function.Consumer<Object>) ignored -> onClosed.run());
            }

            step = "build+send";
            Object form = builderClass.getMethod("build").invoke(builder);
            Method sendForm = findSendForm(connection.getClass(), form.getClass());
            if (sendForm == null) {
                log.warning("BedrockForms: no compatible sendForm (ModalForm)");
                return false;
            }
            return (boolean) sendForm.invoke(connection, form);
        } catch (Exception e) {
            log.warning("BedrockForms ModalForm '" + title + "' failed at [" + step + "]: "
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

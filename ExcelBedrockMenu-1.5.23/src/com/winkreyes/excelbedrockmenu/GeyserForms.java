package com.winkreyes.excelbedrockmenu;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.logging.Logger;

/**
 * Reflection-based Geyser form helpers. Uses Geyser's own classloader so we
 * never define Geyser API types ourselves and avoid loader constraint
 * violations. No compile-time dependency on Geyser or Cumulus.
 */
final class GeyserForms {

    private GeyserForms() {}

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
     * Sends a SimpleForm list dialog. onSelect receives the clicked button id
     * (-1 if the form was closed). Returns true if the form was sent.
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
                            if (id >= 0 && id < buttons.length) ClickStats.tap(player, title, buttons[id], id);
                            onSelect.accept(id);
                        }
                        return null;
                    });
            Method vrh = findHandler(builderClass, "validResultHandler", handler);
            if (vrh == null) {
                log.warning("GeyserForms: no compatible validResultHandler (SimpleForm)");
                return false;
            }
            vrh.invoke(builder, handler);

            step = "build+send";
            Object form = builderClass.getMethod("build").invoke(builder);
            Method sendForm = findSendForm(connection.getClass(), form.getClass());
            if (sendForm == null) {
                log.warning("GeyserForms: no compatible sendForm (SimpleForm)");
                return false;
            }
            return (boolean) sendForm.invoke(connection, form);
        } catch (Exception e) {
            log.warning("GeyserForms SimpleForm '" + title + "' failed at [" + step + "]: "
                    + e.getClass().getSimpleName() + ": " + e.getMessage());
            return false;
        }
    }

    /**
     * Sends a ModalForm (two-button Accept/Decline style) dialog. onResult
     * receives true if the first button was clicked, false for the second.
     * Returns true if the form was sent.
     */
    static boolean sendModalForm(Player player, String title, String content,
                                 String button1, String button2,
                                 Consumer<Boolean> onResult, Logger log) {
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
                            ClickStats.tap(player, title, first ? button1 : button2, first ? 0 : 1);
                            onResult.accept(first);
                        }
                        return null;
                    });
            Method vrh = findHandler(builderClass, "validResultHandler", handler);
            if (vrh == null) {
                log.warning("GeyserForms: no compatible validResultHandler (ModalForm)");
                return false;
            }
            vrh.invoke(builder, handler);

            step = "build+send";
            Object form = builderClass.getMethod("build").invoke(builder);
            Method sendForm = findSendForm(connection.getClass(), form.getClass());
            if (sendForm == null) {
                log.warning("GeyserForms: no compatible sendForm (ModalForm)");
                return false;
            }
            return (boolean) sendForm.invoke(connection, form);
        } catch (Exception e) {
            log.warning("GeyserForms ModalForm '" + title + "' failed at [" + step + "]: "
                    + e.getClass().getSimpleName() + ": " + e.getMessage());
            return false;
        }
    }

    /**
     * v1.5.22: sends a CustomForm with one text box (Ask the Guide). onText
     * receives what the player typed (possibly empty) when they press Submit;
     * nothing is called if the form is closed. The typed text is never logged:
     * ClickStats gets only the fixed "Send" label. Returns true if sent.
     */
    static boolean sendInputForm(Player player, String title, String prompt,
                                 String placeholder, Consumer<String> onText, Logger log) {
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

            step = "CustomForm.builder()";
            Class<?> formClass = Class.forName("org.geysermc.cumulus.form.CustomForm",
                    true, loader);
            Object builder = formClass.getMethod("builder").invoke(null);
            Class<?> builderClass = builder.getClass();

            step = "title/input";
            builderClass.getMethod("title", String.class).invoke(builder, title);
            // One component only (index 0), so the response index never depends
            // on whether a Cumulus version counts labels.
            builderClass.getMethod("input", String.class, String.class)
                    .invoke(builder, prompt, placeholder);

            step = "validResultHandler";
            Class<?> responseClass = Class.forName(
                    "org.geysermc.cumulus.response.CustomFormResponse", true, loader);
            Class<?> biConsumerClass = Class.forName("java.util.function.BiConsumer",
                    true, loader);
            Object handler = Proxy.newProxyInstance(loader,
                    new Class<?>[]{biConsumerClass},
                    (proxy, method, methodArgs) -> {
                        if (method.getName().equals("accept") && methodArgs.length == 2) {
                            Object response = methodArgs[1];
                            Object typed = responseClass.getMethod("asInput", int.class)
                                    .invoke(response, 0);
                            ClickStats.tap(player, title, "Send", 0);
                            onText.accept(typed == null ? "" : typed.toString());
                        }
                        return null;
                    });
            Method vrh = findHandler(builderClass, "validResultHandler", handler);
            if (vrh == null) {
                log.warning("GeyserForms: no compatible validResultHandler (CustomForm)");
                return false;
            }
            vrh.invoke(builder, handler);

            step = "build+send";
            Object form = builderClass.getMethod("build").invoke(builder);
            Method sendForm = findSendForm(connection.getClass(), form.getClass());
            if (sendForm == null) {
                log.warning("GeyserForms: no compatible sendForm (CustomForm)");
                return false;
            }
            return (boolean) sendForm.invoke(connection, form);
        } catch (Exception e) {
            log.warning("GeyserForms CustomForm '" + title + "' failed at [" + step + "]: "
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

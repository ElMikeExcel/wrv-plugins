package com.winkreyes.exceldeathrecord;

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
 * Reflection-based Geyser form helpers (the ExcelBedrockMenu
 * pattern, reused; SimpleForm + a CustomForm of toggles added for
 * the restore picker). Uses Geyser's own classloader so we
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
                return floodgate(player);
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

    private static boolean floodgate(Player player) {
        try {
            Plugin fg = Bukkit.getPluginManager().getPlugin("floodgate");
            if (fg == null || !fg.isEnabled()) return false;
            Class<?> c = Class.forName("org.geysermc.floodgate.api.FloodgateApi", true, fg.getClass().getClassLoader());
            Object api = c.getMethod("getInstance").invoke(null);
            return Boolean.TRUE.equals(c.getMethod("isFloodgatePlayer", UUID.class).invoke(api, player.getUniqueId()));
        } catch (Throwable t) {
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
                            { final int fid = id; Bukkit.getScheduler().runTask(ExcelDeathRecord.instance(), () -> onSelect.accept(fid)); }
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
                            { final boolean ff = first; Bukkit.getScheduler().runTask(ExcelDeathRecord.instance(), () -> onResult.accept(ff)); }
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
     * Sends a CustomForm: one label (content) followed by toggles.
     * onResult gets the toggle values in order, or null if closed.
     */
    static boolean sendToggleForm(Player player, String title, String content,
                                  java.util.List<String> toggles,
                                  Consumer<java.util.List<Boolean>> onResult, Logger log) {
        String step = "start";
        try {
            Plugin geyserPlugin = Bukkit.getPluginManager().getPlugin("Geyser-Spigot");
            if (geyserPlugin == null || !geyserPlugin.isEnabled()) return false;
            ClassLoader loader = geyserPlugin.getClass().getClassLoader();
            step = "GeyserApi.api()";
            Class<?> apiClass = Class.forName("org.geysermc.geyser.api.GeyserApi", true, loader);
            Object api = apiClass.getMethod("api").invoke(null);
            step = "connectionByUuid";
            Object connection = apiClass.getMethod("connectionByUuid", UUID.class).invoke(api, player.getUniqueId());
            if (connection == null) return false;
            step = "CustomForm.builder()";
            Class<?> formClass = Class.forName("org.geysermc.cumulus.form.CustomForm", true, loader);
            Object builder = formClass.getMethod("builder").invoke(null);
            Class<?> bc = builder.getClass();
            bc.getMethod("title", String.class).invoke(builder, title);
            bc.getMethod("label", String.class).invoke(builder, content);
            Method toggle = bc.getMethod("toggle", String.class, boolean.class);
            for (String t : toggles) toggle.invoke(builder, t, false);
            step = "validResultHandler";
            Class<?> responseClass = Class.forName("org.geysermc.cumulus.response.CustomFormResponse", true, loader);
            Class<?> biConsumerClass = Class.forName("java.util.function.BiConsumer", true, loader);
            Object handler = Proxy.newProxyInstance(loader, new Class<?>[]{biConsumerClass},
                    (proxy, method, a) -> {
                        if (method.getName().equals("accept") && a.length == 2) {
                            Object response = a[1];
                            java.util.List<Boolean> vals = new java.util.ArrayList<>();
                            // labels are skipped by next() (includeLabels defaults to false)
                            Method next = responseClass.getMethod("next");
                            for (int i = 0; i < toggles.size(); i++) {
                                Object v = next.invoke(response);
                                vals.add(Boolean.TRUE.equals(v));
                            }
                            Bukkit.getScheduler().runTask(ExcelDeathRecord.instance(), () -> onResult.accept(vals));
                        }
                        return null;
                    });
            Method vrh = findHandler(bc, "validResultHandler", handler);
            if (vrh == null) { log.warning("GeyserForms: no compatible validResultHandler (CustomForm)"); return false; }
            vrh.invoke(builder, handler);
            step = "build+send";
            Object form = bc.getMethod("build").invoke(builder);
            Method sendForm = findSendForm(connection.getClass(), form.getClass());
            if (sendForm == null) { log.warning("GeyserForms: no compatible sendForm (CustomForm)"); return false; }
            return (boolean) sendForm.invoke(connection, form);
        } catch (Exception e) {
            log.warning("GeyserForms CustomForm '" + title + "' failed at [" + step + "]: "
                    + e.getClass().getSimpleName() + ": " + e.getMessage());
            return false;
        }
    }

    /** Boot probe: resolves every Cumulus/Geyser class and method the
     *  restore forms use, without sending anything. "ready", "off"
     *  (no Geyser) or "broken: <why>". */
    static String probe() {
        try {
            Plugin geyserPlugin = Bukkit.getPluginManager().getPlugin("Geyser-Spigot");
            if (geyserPlugin == null) return "off";
            ClassLoader loader = geyserPlugin.getClass().getClassLoader();
            Class.forName("org.geysermc.geyser.api.GeyserApi", true, loader).getMethod("connectionByUuid", UUID.class);
            Class<?> sf = Class.forName("org.geysermc.cumulus.form.SimpleForm", true, loader);
            sf.getMethod("builder").invoke(null).getClass().getMethod("button", String.class);
            Class<?> cf = Class.forName("org.geysermc.cumulus.form.CustomForm", true, loader);
            cf.getMethod("builder").invoke(null).getClass().getMethod("toggle", String.class, boolean.class);
            Class.forName("org.geysermc.cumulus.response.CustomFormResponse", true, loader).getMethod("next");
            Class.forName("org.geysermc.cumulus.response.SimpleFormResponse", true, loader).getMethod("clickedButtonId");
            return "ready";
        } catch (Throwable t) {
            return "broken: " + t.getClass().getSimpleName() + ": " + t.getMessage();
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

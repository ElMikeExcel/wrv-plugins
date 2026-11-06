package com.winkreyes.excelcosmetics;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.UUID;
import java.util.function.IntConsumer;
import java.util.logging.Logger;

/**
 * The Bedrock door of the picker: a Cumulus SimpleForm, reached by
 * reflection through Geyser's classloader — the exact pattern
 * ExcelQuartermaster's BedrockTales proved on this stack (no
 * compile-time Floodgate link, nothing breaks when Geyser is
 * absent). One button per held cosmetic; the callback index maps
 * back to the same PickerModel entries the Java GUI shows.
 */
final class BedrockPicker {

    private BedrockPicker() {}

    /** TRUE = Bedrock, FALSE = confirmed Java, null = undetermined.
     *  When Geyser is absent entirely, nobody can be on Bedrock. */
    static Boolean bedrockOrNull(Player player) {
        try {
            Plugin geyser = Bukkit.getPluginManager().getPlugin("Geyser-Spigot");
            if (geyser == null || !geyser.isEnabled()) return Boolean.FALSE;
            ClassLoader loader = geyser.getClass().getClassLoader();
            Class<?> apiClass = Class.forName("org.geysermc.geyser.api.GeyserApi", true, loader);
            Object api = apiClass.getMethod("api").invoke(null);
            Object result = apiClass.getMethod("isBedrockPlayer", UUID.class)
                    .invoke(api, player.getUniqueId());
            return result instanceof Boolean b ? b : null;
        } catch (Exception e) {
            return null;
        }
    }

    static boolean formsAvailable() {
        Plugin geyser = Bukkit.getPluginManager().getPlugin("Geyser-Spigot");
        if (geyser == null || !geyser.isEnabled()) return false;
        try {
            Class.forName("org.geysermc.cumulus.form.SimpleForm", true,
                    geyser.getClass().getClassLoader());
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Send the picker form. {@code onButton} receives the clicked
     * entry index; handlers run on a Geyser thread — the caller
     * re-dispatches to the main thread before touching state.
     * Returns true only when the form was actually sent.
     */
    static boolean sendPicker(Player player, String title, String content,
                              List<String> buttonLabels, IntConsumer onButton, Logger log) {
        String step = "start";
        try {
            if (!formsAvailable()) return false;
            Plugin geyserPlugin = Bukkit.getPluginManager().getPlugin("Geyser-Spigot");
            ClassLoader loader = geyserPlugin.getClass().getClassLoader();
            UUID uuid = player.getUniqueId();

            step = "GeyserApi.api()";
            Class<?> apiClass = Class.forName("org.geysermc.geyser.api.GeyserApi", true, loader);
            Object api = apiClass.getMethod("api").invoke(null);

            step = "connectionByUuid";
            Object connection = apiClass.getMethod("connectionByUuid", UUID.class).invoke(api, uuid);
            if (connection == null) return false;

            step = "SimpleForm.builder()";
            Class<?> formClass = Class.forName("org.geysermc.cumulus.form.SimpleForm", true, loader);
            Object builder = formClass.getMethod("builder").invoke(null);
            Class<?> builderClass = builder.getClass();

            step = "title/content/buttons";
            builderClass.getMethod("title", String.class).invoke(builder, title);
            builderClass.getMethod("content", String.class).invoke(builder, content);
            Method buttonMethod = builderClass.getMethod("button", String.class);
            for (String label : buttonLabels) buttonMethod.invoke(builder, label);

            step = "validResultHandler";
            Class<?> responseClass = Class.forName(
                    "org.geysermc.cumulus.response.SimpleFormResponse", true, loader);
            Class<?> biConsumerClass = Class.forName("java.util.function.BiConsumer", true, loader);
            Object validHandler = Proxy.newProxyInstance(loader,
                    new Class<?>[]{biConsumerClass},
                    (proxy, method, methodArgs) -> {
                        if (method.getName().equals("accept") && methodArgs != null
                                && methodArgs.length == 2 && methodArgs[1] != null) {
                            int id = (int) responseClass.getMethod("clickedButtonId")
                                    .invoke(methodArgs[1]);
                            onButton.accept(id);
                        }
                        return null;
                    });
            Method vrh = findHandler(builderClass, "validResultHandler", validHandler);
            if (vrh == null) {
                log.warning("ExcelCosmetics: no compatible validResultHandler (SimpleForm)");
                return false;
            }
            vrh.invoke(builder, validHandler);

            step = "build+send";
            Object form = builderClass.getMethod("build").invoke(builder);
            Method sendForm = findSendForm(connection.getClass(), form.getClass());
            if (sendForm == null) {
                log.warning("ExcelCosmetics: no compatible sendForm (SimpleForm)");
                return false;
            }
            return (boolean) sendForm.invoke(connection, form);
        } catch (Exception e) {
            log.warning("ExcelCosmetics picker form failed at [" + step + "]: "
                    + e.getClass().getSimpleName() + ": " + e.getMessage());
            return false;
        }
    }

    private static Method findHandler(Class<?> builderClass, String name, Object handler) {
        for (Method m : builderClass.getMethods()) {
            if (m.getName().equals(name) && m.getParameterCount() == 1) {
                Class<?> pt = m.getParameterTypes()[0];
                if (pt.isAssignableFrom(handler.getClass())) return m;
            }
        }
        return null;
    }

    private static Method findSendForm(Class<?> connectionClass, Class<?> formClass) {
        for (Method m : connectionClass.getMethods()) {
            if (m.getName().equals("sendForm") && m.getParameterCount() == 1) {
                Class<?> pt = m.getParameterTypes()[0];
                if (pt.isAssignableFrom(formClass)) return m;
            }
        }
        return null;
    }
}

// ExcelCosmetics — an Excel plugin by WinkReyesValley. (Excel signature — MikeExcel)

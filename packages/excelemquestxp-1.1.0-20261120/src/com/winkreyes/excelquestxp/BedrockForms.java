package com.winkreyes.excelquestxp;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.IntConsumer;
import java.util.logging.Logger;

/**
 * Optional Geyser/Cumulus bridge for the quest-XP target popup — the
 * ExcelShops BedrockForms pattern: every reflection target is a PUBLIC
 * API interface loaded through Geyser's own classloader, never an
 * implementation class, and there is no compile-time dependency on
 * Geyser or Cumulus. Any failure answers false and the caller falls back
 * (chat instructions), so the typed path never breaks.
 *
 * Excel — Wink Reyes Valley (Iris / Mike).
 */
final class BedrockForms {

    private BedrockForms() {}

    /** True when the Geyser plugin is present and enabled (pre-check). */
    static boolean isFormApiAvailable() {
        Plugin geyser = org.bukkit.Bukkit.getPluginManager().getPlugin("Geyser-Spigot");
        return geyser != null && geyser.isEnabled();
    }

    /**
     * Positive Bedrock detection only: a Geyser connection for the UUID,
     * or Floodgate's isFloodgatePlayer. Errors never identify a Java
     * player as Bedrock (they answer false and the Java menu opens).
     */
    static boolean isBedrockPlayer(Player player, Logger log) {
        UUID id = player.getUniqueId();
        var manager = org.bukkit.Bukkit.getPluginManager();
        Plugin geyser = manager.getPlugin("Geyser-Spigot");
        if (geyser != null && geyser.isEnabled()) {
            try {
                Class<?> type = publicApi("org.geysermc.geyser.api.GeyserApi",
                        geyser.getClass().getClassLoader());
                Object api = type.getMethod("api").invoke(null);
                if (type.isInstance(api)
                        && type.getMethod("connectionByUuid", UUID.class).invoke(api, id) != null) {
                    return true;
                }
            } catch (ReflectiveOperationException | RuntimeException | LinkageError unavailable) {
                // Try Floodgate independently.
            }
        }
        Plugin floodgate = manager.getPlugin("floodgate");
        if (floodgate == null) {
            floodgate = manager.getPlugin("Floodgate");
        }
        if (floodgate != null && floodgate.isEnabled()) {
            try {
                Class<?> type = publicApi("org.geysermc.floodgate.api.FloodgateApi",
                        floodgate.getClass().getClassLoader());
                Object api = type.getMethod("getInstance").invoke(null);
                return type.isInstance(api) && Boolean.TRUE.equals(
                        type.getMethod("isFloodgatePlayer", UUID.class).invoke(api, id));
            } catch (ReflectiveOperationException | RuntimeException | LinkageError unavailable) {
                return false;
            }
        }
        return false;
    }

    /**
     * Sends a Cumulus SimpleForm with one button per label. onResult
     * receives the clicked button index; onClosed runs when the form is
     * dismissed without a choice. Returns true only when the form was
     * actually sent. Exactly one of onResult/onClosed ever runs
     * (ResponseGate), and a rejected send leaves no live callbacks.
     */
    static boolean sendChoices(JavaPlugin plugin, Player player, String title, String content,
                               String[] labels, IntConsumer onResult, Runnable onClosed) {
        ResponseGate gate = new ResponseGate(plugin);
        try {
            String[] buttons = labels.clone();
            if (buttons.length == 0) {
                return false;
            }
            Plugin geyser = plugin.getServer().getPluginManager().getPlugin("Geyser-Spigot");
            if (geyser == null || !geyser.isEnabled()) {
                return false;
            }
            ClassLoader loader = geyser.getClass().getClassLoader();
            Class<?> apiType = publicApi("org.geysermc.geyser.api.GeyserApi", loader);
            Object api = apiType.getMethod("api").invoke(null);
            if (!apiType.isInstance(api)) {
                return false;
            }
            UUID id = player.getUniqueId();
            if (apiType.getMethod("connectionByUuid", UUID.class).invoke(api, id) == null) {
                return false;
            }

            Class<?> formApi = publicApi("org.geysermc.cumulus.form.SimpleForm", loader);
            Method factory = formApi.getMethod("builder");
            Class<?> builderApi = factory.getReturnType();
            if (!builderApi.isInterface() || !Modifier.isPublic(builderApi.getModifiers())) {
                return false;
            }
            // Both handlers are mandatory — a form whose dismissal cannot
            // be observed is not safe to send.
            builderApi.getMethod("validResultHandler", BiConsumer.class);
            builderApi.getMethod("closedOrInvalidResultHandler", Runnable.class);
            Object builder = factory.invoke(null);
            if (!builderApi.isInstance(builder)) {
                return false;
            }
            builderApi.getMethod("title", String.class).invoke(builder, title);
            builderApi.getMethod("content", String.class).invoke(builder, content);
            for (String label : buttons) {
                builderApi.getMethod("button", String.class).invoke(builder, label);
            }
            Class<?> responseApi = publicApi("org.geysermc.cumulus.response.SimpleFormResponse", loader);
            Method clicked = responseApi.getMethod("clickedButtonId");
            builderApi.getMethod("validResultHandler", BiConsumer.class).invoke(builder,
                    (BiConsumer<Object, Object>) (form, response) -> gate.reply(() -> {
                        final int buttonId;
                        try {
                            buttonId = (Integer) clicked.invoke(response);
                        } catch (ReflectiveOperationException | RuntimeException ex) {
                            onClosed.run();
                            return;
                        }
                        if (buttonId < 0 || buttonId >= buttons.length) {
                            onClosed.run();
                            return;
                        }
                        onResult.accept(buttonId);
                    }));
            builderApi.getMethod("closedOrInvalidResultHandler", Runnable.class).invoke(builder,
                    (Runnable) () -> gate.reply(onClosed));

            Object form = builderApi.getMethod("build").invoke(builder);
            Class<?> baseFormApi = publicApi("org.geysermc.cumulus.form.Form", loader);
            if (!baseFormApi.isInstance(form)) {
                return false;
            }
            boolean sent = Boolean.TRUE.equals(apiType
                    .getMethod("sendForm", UUID.class, baseFormApi).invoke(api, id, form));
            gate.sent(sent);
            return sent;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError ex) {
            gate.sent(false);
            plugin.getLogger().warning("BedrockForms: target form unavailable; caller falls back: "
                    + ex.getClass().getSimpleName());
            return false;
        }
    }

    private static Class<?> publicApi(String name, ClassLoader loader)
            throws ReflectiveOperationException {
        Class<?> type = Class.forName(name, true, loader);
        if (!type.isInterface() || !Modifier.isPublic(type.getModifiers())) {
            throw new NoSuchMethodException("A public API interface is required: " + name);
        }
        return type;
    }

    /** One result total, including close/invalid. Rejected sends cannot leave actionable late callbacks. */
    private static final class ResponseGate {
        private final JavaPlugin plugin;
        private boolean sendFinished;
        private boolean accepted;
        private boolean consumed;
        private Runnable pending;

        ResponseGate(JavaPlugin plugin) {
            this.plugin = plugin;
        }

        void reply(Runnable action) {
            synchronized (this) {
                if (consumed) {
                    return;
                }
                if (!sendFinished) {
                    if (pending == null) {
                        pending = action;
                    }
                    return;
                }
                if (!accepted) {
                    return;
                }
                consumed = true;
            }
            dispatch(action);
        }

        void sent(boolean success) {
            Runnable action;
            synchronized (this) {
                if (sendFinished) {
                    return;
                }
                sendFinished = true;
                accepted = success;
                action = success ? pending : null;
                pending = null;
                if (!success || action != null) {
                    consumed = true;
                }
            }
            if (action != null) {
                dispatch(action);
            }
        }

        private void dispatch(Runnable action) {
            if (!plugin.isEnabled()) {
                return;
            }
            // Form callbacks arrive off the main thread; Bukkit state
            // (the target store is in-memory + file) is main-thread only.
            org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
                try {
                    action.run();
                } catch (RuntimeException ex) {
                    plugin.getLogger().warning("BedrockForms: form callback rejected: "
                            + ex.getClass().getSimpleName());
                }
            });
        }
    }
}

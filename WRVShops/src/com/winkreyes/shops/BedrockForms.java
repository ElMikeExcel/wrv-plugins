// SPDX-License-Identifier: MIT
package com.winkreyes.shops;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Objects;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/** Optional Geyser/Cumulus bridge. All reflection targets public API interfaces, never implementation classes. */
public final class BedrockForms {
    @FunctionalInterface interface ClassLookup {
        Class<?> load(String name, ClassLoader loader) throws ClassNotFoundException;
    }
    private final WRVShops plugin;
    private final ClassLookup classes;
    public BedrockForms(WRVShops plugin) { this(plugin, (name, loader) -> Class.forName(name, true, loader)); }
    /** Package-private API-compatibility seam used by fixtures, without shipping Geyser classes. */
    BedrockForms(WRVShops plugin, ClassLookup classes) {
        this.plugin = Objects.requireNonNull(plugin); this.classes = Objects.requireNonNull(classes);
    }
    private record Connection(ClassLoader loader, Class<?> apiType, Object api, UUID playerId) { }
    private record Builder(Class<?> apiType, Object value) { }

    private Class<?> publicApi(String name, ClassLoader loader) throws ReflectiveOperationException {
        Class<?> type = classes.load(name, loader);
        if (!type.isInterface() || !Modifier.isPublic(type.getModifiers())) {
            throw new NoSuchMethodException("A public API interface is required: " + name);
        }
        return type;
    }
    /**
     * Native Java dialogs are safe only with no installed bridge, or when every
     * installed bridge positively identifies this UUID as a non-Bedrock player.
     * This check deliberately ignores the form preference and fails closed for
     * disabled bridges, unavailable APIs, and detection errors.
     */
    public boolean canUseJavaDialog(Player player) {
        try {
            UUID id = Objects.requireNonNull(player.getUniqueId());
            var manager = plugin.getServer().getPluginManager();
            Plugin geyser = manager.getPlugin("Geyser-Spigot");
            if (geyser != null) {
                if (!geyser.isEnabled()) return false;
                Class<?> apiType = publicApi("org.geysermc.geyser.api.GeyserApi", geyser.getClass().getClassLoader());
                Object api = apiType.getMethod("api").invoke(null);
                if (!apiType.isInstance(api)) return false;
                if (apiType.getMethod("connectionByUuid", UUID.class).invoke(api, id) != null) return false;
            }
            // Floodgate may be the only local bridge when Geyser runs on a proxy.
            Plugin floodgate = manager.getPlugin("floodgate");
            if (floodgate == null) floodgate = manager.getPlugin("Floodgate");
            if (floodgate != null) {
                if (!floodgate.isEnabled()) return false;
                Class<?> apiType = publicApi("org.geysermc.floodgate.api.FloodgateApi", floodgate.getClass().getClassLoader());
                Object api = apiType.getMethod("getInstance").invoke(null);
                if (!apiType.isInstance(api)) return false;
                if (!Boolean.FALSE.equals(apiType.getMethod("isFloodgatePlayer", UUID.class).invoke(api, id))) return false;
            }
            return true;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError ex) {
            return false;
        }
    }
    /** Positive detection only, independent of the native-form preference. Errors never identify Java as Bedrock. */
    public boolean isBedrock(Player player) {
        UUID id = player.getUniqueId();
        var manager = plugin.getServer().getPluginManager();
        Plugin geyser = manager.getPlugin("Geyser-Spigot");
        if (geyser != null && geyser.isEnabled()) {
            try {
                Class<?> type = publicApi("org.geysermc.geyser.api.GeyserApi", geyser.getClass().getClassLoader());
                Object api = type.getMethod("api").invoke(null);
                if (type.isInstance(api) && type.getMethod("connectionByUuid", UUID.class).invoke(api, id) != null) return true;
            } catch (ReflectiveOperationException | RuntimeException | LinkageError unavailable) { /* Try Floodgate independently. */ }
        }
        Plugin floodgate = manager.getPlugin("floodgate");
        if (floodgate == null) floodgate = manager.getPlugin("Floodgate");
        if (floodgate != null && floodgate.isEnabled()) {
            try {
                Class<?> type = publicApi("org.geysermc.floodgate.api.FloodgateApi", floodgate.getClass().getClassLoader());
                Object api = type.getMethod("getInstance").invoke(null);
                return type.isInstance(api) && Boolean.TRUE.equals(type.getMethod("isFloodgatePlayer", UUID.class).invoke(api, id));
            } catch (ReflectiveOperationException | RuntimeException | LinkageError unavailable) { return false; }
        }
        return false;
    }
    private Connection connect(Player player) throws ReflectiveOperationException {
        if (!plugin.getConfig().getBoolean("bedrock-forms", true)) return null;
        Plugin geyser = plugin.getServer().getPluginManager().getPlugin("Geyser-Spigot");
        if (geyser == null || !geyser.isEnabled()) return null;
        ClassLoader loader = geyser.getClass().getClassLoader();
        Class<?> apiType = publicApi("org.geysermc.geyser.api.GeyserApi", loader);
        Object api = apiType.getMethod("api").invoke(null);
        if (!apiType.isInstance(api)) throw new IllegalStateException("Geyser API is unavailable");
        UUID id = player.getUniqueId();
        Object connection = apiType.getMethod("connectionByUuid", UUID.class).invoke(api, id);
        return connection == null ? null : new Connection(loader, apiType, api, id);
    }
    private Builder builder(String name, Connection connection) throws ReflectiveOperationException {
        Class<?> formApi = publicApi(name, connection.loader());
        Method factory = formApi.getMethod("builder");
        Class<?> builderApi = factory.getReturnType();
        if (!builderApi.isInterface() || !Modifier.isPublic(builderApi.getModifiers())) {
            throw new NoSuchMethodException("A public form builder interface is required");
        }
        // Both are mandatory. A form whose close cannot invalidate its quote is not safe to use.
        builderApi.getMethod("validResultHandler", BiConsumer.class);
        builderApi.getMethod("closedOrInvalidResultHandler", Runnable.class);
        Object value = factory.invoke(null);
        if (!builderApi.isInstance(value)) throw new IllegalStateException("Invalid form builder");
        return new Builder(builderApi, value);
    }

    public boolean number(Player player, String title, String text, String initial, Consumer<String> result, Runnable close) {
        ResponseGate gate = new ResponseGate();
        try {
            Connection connection = connect(player); if (connection == null) return false;
            Builder builder = builder("org.geysermc.cumulus.form.CustomForm", connection);
            Class<?> responseApi = publicApi("org.geysermc.cumulus.response.CustomFormResponse", connection.loader());
            Method input = responseApi.getMethod("asInput", int.class);
            strings(builder, "title", title); strings(builder, "input", text, "Enter a number", initial);
            register(builder, "validResultHandler", BiConsumer.class, (BiConsumer<Object, Object>) (form, response) -> gate.reply(() -> {
                final String value;
                try { value = (String) input.invoke(response, 0); }
                catch (ReflectiveOperationException | RuntimeException ex) { close.run(); return; }
                if (value == null) { close.run(); return; }
                result.accept(value);
            }));
            register(builder, "closedOrInvalidResultHandler", Runnable.class, (Runnable) () -> gate.reply(close));
            boolean sent = send(connection, builder);
            gate.sent(sent);
            return sent;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError ex) {
            gate.sent(false); fallback("number", ex); return false;
        }
    }

    public boolean choices(Player player, String title, String text, String[] labels, IntConsumer result, Runnable close) {
        ResponseGate gate = new ResponseGate();
        try {
            String[] buttons = labels.clone();
            if (buttons.length == 0) return false;
            Connection connection = connect(player); if (connection == null) return false;
            Builder builder = builder("org.geysermc.cumulus.form.SimpleForm", connection);
            Class<?> responseApi = publicApi("org.geysermc.cumulus.response.SimpleFormResponse", connection.loader());
            Method selected = responseApi.getMethod("clickedButtonId");
            strings(builder, "title", title); strings(builder, "content", text);
            for (String label : buttons) strings(builder, "button", label);
            register(builder, "validResultHandler", BiConsumer.class, (BiConsumer<Object, Object>) (form, response) -> gate.reply(() -> {
                final int id;
                try { id = (Integer) selected.invoke(response); }
                catch (ReflectiveOperationException | RuntimeException ex) { close.run(); return; }
                if (id < 0 || id >= buttons.length) { close.run(); return; }
                result.accept(id);
            }));
            register(builder, "closedOrInvalidResultHandler", Runnable.class, (Runnable) () -> gate.reply(close));
            boolean sent = send(connection, builder);
            gate.sent(sent);
            return sent;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError ex) {
            gate.sent(false); fallback("review", ex); return false;
        }
    }

    private static void strings(Builder builder, String name, String... args) throws ReflectiveOperationException {
        Class<?>[] types = new Class<?>[args.length]; java.util.Arrays.fill(types, String.class);
        builder.apiType().getMethod(name, types).invoke(builder.value(), (Object[]) args);
    }
    private static void register(Builder builder, String name, Class<?> handlerType, Object callback) throws ReflectiveOperationException {
        builder.apiType().getMethod(name, handlerType).invoke(builder.value(), callback);
    }
    private boolean send(Connection connection, Builder builder) throws ReflectiveOperationException {
        Object form = builder.apiType().getMethod("build").invoke(builder.value());
        Class<?> formApi = publicApi("org.geysermc.cumulus.form.Form", connection.loader());
        if (!formApi.isInstance(form)) throw new IllegalStateException("Invalid built form");
        return Boolean.TRUE.equals(connection.apiType().getMethod("sendForm", UUID.class, formApi)
                .invoke(connection.api(), connection.playerId(), form));
    }
    private void fallback(String kind, Throwable failure) {
        plugin.getLogger().warning("Bedrock " + kind + " form unavailable; caller will use safe fallback or cancellation: " + failure.getClass().getSimpleName());
    }

    /** One result total, including close/invalid. Rejected sends cannot leave actionable late callbacks. */
    private final class ResponseGate {
        private boolean sendFinished, accepted, consumed;
        private Runnable pending;
        void reply(Runnable action) {
            synchronized (this) {
                if (consumed) return;
                if (!sendFinished) {
                    if (pending == null) pending = action;
                    return;
                }
                if (!accepted) return;
                consumed = true;
            }
            dispatch(action);
        }
        void sent(boolean success) {
            Runnable action;
            synchronized (this) {
                if (sendFinished) return;
                sendFinished = true; accepted = success;
                action = success ? pending : null;
                pending = null;
                if (!success || action != null) consumed = true;
            }
            if (action != null) dispatch(action);
        }
        private void dispatch(Runnable action) {
            if (!plugin.isEnabled()) return;
            try { action.run(); }
            catch (RuntimeException ex) { plugin.getLogger().warning("Bedrock form callback was rejected: " + ex.getClass().getSimpleName()); }
        }
    }
}

package com.winkreyes.leaderboard.paper;

import com.winkreyes.leaderboard.ExcelLeaderboard;
import com.winkreyes.leaderboard.core.BoardMath;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.IntConsumer;

/** Bedrock form menus (native Cumulus SimpleForms) for /leaderboard,
 * copied from ExcelShops 1.1's proven BedrockForms pattern: every Geyser /
 * Cumulus / Floodgate type is reached by reflection against the owning
 * plugin's classloader, public API interfaces only, so this plugin has NO
 * compile-time dependency on any of them. All text comes from
 * BoardPresentation; this class only detects, builds, sends, and routes
 * the one-shot response.
 *
 * Form callbacks arrive off the server thread: the ResponseGate hops to
 * the main thread before any callback touches plugin state, the same
 * discipline the shop uses. A failed send returns false and the caller
 * falls back to the chest menu (safe here: a leaderboard is read-only). */
public final class BedrockBoardForms {

    @FunctionalInterface
    interface ClassLookup {
        Class<?> load(String name, ClassLoader loader) throws ClassNotFoundException;
    }

    private final ExcelLeaderboard plugin;
    private final ClassLookup classes;

    public BedrockBoardForms(ExcelLeaderboard plugin) {
        this(plugin, (name, loader) -> Class.forName(name, true, loader));
    }

    /** Package-private seam mirroring the shop's fixture seam. */
    BedrockBoardForms(ExcelLeaderboard plugin, ClassLookup classes) {
        this.plugin = Objects.requireNonNull(plugin);
        this.classes = Objects.requireNonNull(classes);
    }

    private record Connection(ClassLoader loader, Class<?> apiType, Object api, UUID playerId) {}
    private record Builder(Class<?> apiType, Object value) {}

    private Class<?> publicApi(String name, ClassLoader loader) throws ReflectiveOperationException {
        Class<?> type = classes.load(name, loader);
        if (!type.isInterface() || !Modifier.isPublic(type.getModifiers())) {
            throw new NoSuchMethodException("A public API interface is required: " + name);
        }
        return type;
    }

    /** Positive detection only: Geyser connectionByUuid first, Floodgate
     * isFloodgatePlayer as fallback. Errors never identify a Java player
     * as Bedrock. (Adapted from ExcelShops' BedrockForms.isBedrock.) */
    public boolean isBedrock(Player player) {
        UUID id = player.getUniqueId();
        var manager = plugin.getServer().getPluginManager();
        Plugin geyser = manager.getPlugin("Geyser-Spigot");
        if (geyser != null && geyser.isEnabled()) {
            try {
                Class<?> type = publicApi("org.geysermc.geyser.api.GeyserApi", geyser.getClass().getClassLoader());
                Object api = type.getMethod("api").invoke(null);
                if (type.isInstance(api) && type.getMethod("connectionByUuid", UUID.class).invoke(api, id) != null) {
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
                Class<?> type = publicApi("org.geysermc.floodgate.api.FloodgateApi", floodgate.getClass().getClassLoader());
                Object api = type.getMethod("getInstance").invoke(null);
                return type.isInstance(api) && Boolean.TRUE.equals(type.getMethod("isFloodgatePlayer", UUID.class).invoke(api, id));
            } catch (ReflectiveOperationException | RuntimeException | LinkageError unavailable) {
                return false;
            }
        }
        return false;
    }

    private Connection connect(Player player) throws ReflectiveOperationException {
        if (!plugin.getConfig().getBoolean("bedrock-forms", true)) {
            return null;
        }
        Plugin geyser = plugin.getServer().getPluginManager().getPlugin("Geyser-Spigot");
        if (geyser == null || !geyser.isEnabled()) {
            return null;
        }
        ClassLoader loader = geyser.getClass().getClassLoader();
        Class<?> apiType = publicApi("org.geysermc.geyser.api.GeyserApi", loader);
        Object api = apiType.getMethod("api").invoke(null);
        if (!apiType.isInstance(api)) {
            throw new IllegalStateException("Geyser API is unavailable");
        }
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
        // Both handlers are mandatory before the form may be used.
        builderApi.getMethod("validResultHandler", BiConsumer.class);
        builderApi.getMethod("closedOrInvalidResultHandler", Runnable.class);
        Object value = factory.invoke(null);
        if (!builderApi.isInstance(value)) {
            throw new IllegalStateException("Invalid form builder");
        }
        return new Builder(builderApi, value);
    }

    /** The shop's choices(): one SimpleForm, one button per label, the
     * answer read from SimpleFormResponse.clickedButtonId(). Returns true
     * only when the form was actually sent. */
    public boolean choices(Player player, String title, String text, String[] labels,
                           IntConsumer result, Runnable close) {
        ResponseGate gate = new ResponseGate();
        try {
            String[] buttons = labels.clone();
            if (buttons.length == 0) {
                return false;
            }
            Connection connection = connect(player);
            if (connection == null) {
                return false;
            }
            Builder builder = builder("org.geysermc.cumulus.form.SimpleForm", connection);
            Class<?> responseApi = publicApi("org.geysermc.cumulus.response.SimpleFormResponse", connection.loader());
            Method selected = responseApi.getMethod("clickedButtonId");
            strings(builder, "title", title);
            strings(builder, "content", text);
            for (String label : buttons) {
                strings(builder, "button", label);
            }
            register(builder, "validResultHandler", BiConsumer.class, (BiConsumer<Object, Object>) (form, response) -> gate.reply(() -> {
                final int id;
                try {
                    id = (Integer) selected.invoke(response);
                } catch (ReflectiveOperationException | RuntimeException ex) {
                    close.run();
                    return;
                }
                if (id < 0 || id >= buttons.length) {
                    close.run();
                    return;
                }
                result.accept(id);
            }));
            register(builder, "closedOrInvalidResultHandler", Runnable.class, (Runnable) () -> gate.reply(close));
            boolean sent = send(connection, builder);
            gate.sent(sent);
            return sent;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError ex) {
            gate.sent(false);
            fallback("choices", ex);
            return false;
        }
    }

    /** A text-only form with a single Close button (the warming-up
     * notice). Returns true only when sent. */
    public boolean notice(Player player, String title, String text) {
        ResponseGate gate = new ResponseGate();
        try {
            Connection connection = connect(player);
            if (connection == null) {
                return false;
            }
            Builder builder = builder("org.geysermc.cumulus.form.SimpleForm", connection);
            strings(builder, "title", title);
            strings(builder, "content", text);
            strings(builder, "button", "Close");
            register(builder, "validResultHandler", BiConsumer.class,
                    (BiConsumer<Object, Object>) (form, response) -> gate.reply(() -> {}));
            register(builder, "closedOrInvalidResultHandler", Runnable.class,
                    (Runnable) () -> gate.reply(() -> {}));
            boolean sent = send(connection, builder);
            gate.sent(sent);
            return sent;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError ex) {
            gate.sent(false);
            fallback("notice", ex);
            return false;
        }
    }

    // ---- Leaderboard flows ------------------------------------------------

    /** Main form: one compact button per available board. With no boards
     * yet, the warming-up notice form. False = caller falls back to the
     * chest menu. */
    public boolean openMain(Player player) {
        ExcelLeaderboard.Snapshot snapshot = plugin.snapshot();
        List<Category> available = BoardPresentation.availableBoards(snapshot.boards());
        if (available.isEmpty()) {
            return notice(player, BoardPresentation.MAIN_TITLE, BoardPresentation.WARMING_UP_TEXT);
        }
        List<String> labels = BoardPresentation.mainFormButtons(snapshot.boards());
        return choices(player, BoardPresentation.MAIN_TITLE, BoardPresentation.MAIN_FORM_CONTENT,
                labels.toArray(new String[0]),
                id -> {
                    if (player.isOnline()) {
                        openBoard(player, available.get(id));
                    }
                },
                () -> {});
    }

    /** Board form: top 10 as content text, the viewer's rank line, then
     * "← All boards" and "Close" buttons. */
    public boolean openBoard(Player player, Category category) {
        ExcelLeaderboard.Snapshot snapshot = plugin.snapshot();
        List<BoardMath.Entry> board = snapshot.boards().getOrDefault(category, List.of());
        String content = BoardPresentation.boardContent(category, board, player.getUniqueId(),
                snapshot.detailsFor(category));
        return choices(player, category.title(), content, BoardPresentation.BOARD_FORM_BUTTONS,
                id -> {
                    if (id == 0 && player.isOnline()) {
                        openMain(player);
                    }
                },
                () -> {});
    }

    private static void strings(Builder builder, String name, String... args) throws ReflectiveOperationException {
        Class<?>[] types = new Class<?>[args.length];
        java.util.Arrays.fill(types, String.class);
        builder.apiType().getMethod(name, types).invoke(builder.value(), (Object[]) args);
    }

    private static void register(Builder builder, String name, Class<?> handlerType, Object callback)
            throws ReflectiveOperationException {
        builder.apiType().getMethod(name, handlerType).invoke(builder.value(), callback);
    }

    private boolean send(Connection connection, Builder builder) throws ReflectiveOperationException {
        Object form = builder.apiType().getMethod("build").invoke(builder.value());
        Class<?> formApi = publicApi("org.geysermc.cumulus.form.Form", connection.loader());
        if (!formApi.isInstance(form)) {
            throw new IllegalStateException("Invalid built form");
        }
        return Boolean.TRUE.equals(connection.apiType().getMethod("sendForm", UUID.class, formApi)
                .invoke(connection.api(), connection.playerId(), form));
    }

    private void fallback(String kind, Throwable failure) {
        plugin.getLogger().warning("Bedrock " + kind + " form unavailable; caller will use safe fallback: "
                + failure.getClass().getSimpleName());
    }

    /** One result total, including close/invalid. Rejected sends cannot
     * leave actionable late callbacks. Copied from ExcelShops, with the
     * dispatch hopping to the main thread first: form callbacks arrive
     * off-thread and must never touch plugin state directly. */
    private final class ResponseGate {
        private boolean sendFinished, accepted, consumed;
        private Runnable pending;

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
            Runnable guarded = () -> {
                try {
                    action.run();
                } catch (RuntimeException ex) {
                    plugin.getLogger().warning("Bedrock form callback was rejected: " + ex.getClass().getSimpleName());
                }
            };
            if (Bukkit.isPrimaryThread()) {
                guarded.run();
            } else {
                plugin.getServer().getScheduler().runTask(plugin, guarded);
            }
        }
    }
}

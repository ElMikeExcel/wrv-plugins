// Excel signature - WRV maintained source; original authorship and license notices preserved.
// SPDX-License-Identifier: MIT
package com.winkreyes.shops;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.LongSupplier;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import org.bukkit.entity.Player;

/**
 * Optional Paper 1.21.7+ native text input, compiled against the older supported API.
 * Only public API interfaces are reflected; no server implementation or NMS access.
 * The caller owns navigation, main-thread dispatch, page tokens and the visible timeout.
 * @see <a href="https://docs.papermc.io/paper/dev/dialogs/">Paper dialog API</a>
 */
public final class JavaNumberDialogs {
    public static final int LIFETIME_SECONDS = 60;
    private static final String INPUT_KEY = "amount";
    private static final String DATA = "io.papermc.paper.registry.data.dialog.";
    @FunctionalInterface interface ClassLookup {
        Class<?> load(String name, ClassLoader loader) throws ClassNotFoundException;
    }
    private final WRVShops plugin;
    private final ClassLookup classes;
    private final LongSupplier nanos;

    public JavaNumberDialogs(WRVShops plugin) {
        this(plugin, (name, loader) -> Class.forName(name, true, loader), System::nanoTime);
    }

    /** Compatibility fixtures can substitute public interfaces without shipping Paper classes. */
    JavaNumberDialogs(WRVShops plugin, ClassLookup classes, LongSupplier nanos) {
        this.plugin = Objects.requireNonNull(plugin);
        this.classes = Objects.requireNonNull(classes);
        this.nanos = Objects.requireNonNull(nanos);
    }

    /** Server API detection only; a protocol-translating proxy can hide an older client. */
    public boolean supported() {
        try { new Api(); return true; }
        catch (ReflectiveOperationException | RuntimeException | LinkageError unavailable) { return false; }
    }

    /**
     * Returns false without actionable callbacks if native input is unavailable or sending fails.
     * A true result means the server accepted the show call, not that the client rendered it.
     */
    public boolean number(Player player, String title, String details, String initial,
                          Consumer<String> result, Runnable close) {
        Objects.requireNonNull(player); Objects.requireNonNull(result); Objects.requireNonNull(close);
        ResponseGate gate = new ResponseGate(player);
        if (!plugin.isEnabled() || !player.isOnline()) return false;
        try {
            Api api = new Api();
            Object inputBuilder = api.input.getMethod("text", String.class, Component.class)
                    .invoke(null, INPUT_KEY, Component.text("Amount"));
            api.textBuilder.getMethod("initial", String.class).invoke(inputBuilder, initial);
            api.textBuilder.getMethod("maxLength", int.class).invoke(inputBuilder, 32);
            Object input = api.textBuilder.getMethod("build").invoke(inputBuilder);
            Object body = api.body.getMethod("plainMessage", Component.class)
                    .invoke(null, Component.text(details));
            Object baseBuilder = api.base.getMethod("builder", Component.class)
                    .invoke(null, Component.text(title));
            api.baseBuilder.getMethod("canCloseWithEscape", boolean.class).invoke(baseBuilder, false);
            api.baseBuilder.getMethod("pause", boolean.class).invoke(baseBuilder, false);
            api.baseBuilder.getMethod("afterAction", api.afterAction)
                    .invoke(baseBuilder, api.afterAction.getField("CLOSE").get(null));
            api.baseBuilder.getMethod("body", List.class).invoke(baseBuilder, List.of(body));
            api.baseBuilder.getMethod("inputs", List.class).invoke(baseBuilder, List.of(input));
            Object base = api.baseBuilder.getMethod("build").invoke(baseBuilder);

            Object use = api.button("Use amount", api.callback((response, audience) -> {
                if (!gate.isOwner(audience)) return;
                gate.reply(() -> {
                    final String value;
                    try { value = (String) api.response.getMethod("getText", String.class).invoke(response, INPUT_KEY); }
                    catch (ReflectiveOperationException | RuntimeException invalid) { close.run(); return; }
                    if (value == null || value.length() > 32) { close.run(); return; }
                    result.accept(value);
                });
            }));
            Object cancel = api.button("Cancel", api.callback((response, audience) -> {
                if (gate.isOwner(audience)) gate.reply(close);
            }));
            Object type = api.type.getMethod("confirmation", api.button, api.button).invoke(null, use, cancel);
            Consumer<Object> configure = factory -> {
                try {
                    Object builder = api.factory.getMethod("empty").invoke(factory);
                    api.registryBuilder.getMethod("base", api.base).invoke(builder, base);
                    api.registryBuilder.getMethod("type", api.type).invoke(builder, type);
                } catch (ReflectiveOperationException invalid) {
                    throw new IllegalStateException("Cannot construct native number dialog", invalid);
                }
            };
            Object dialog = api.dialog.getMethod("create", Consumer.class).invoke(null, configure);
            if (!api.dialogLike.isInstance(dialog)) throw new IllegalStateException("Invalid native dialog");
            api.show.invoke(player, dialog);
            gate.sent(true);
            return true;
        } catch (ClassNotFoundException unavailable) {
            gate.sent(false); return false;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError failure) {
            gate.sent(false);
            plugin.getLogger().warning("Java number dialog unavailable; using inventory controls: "
                    + failure.getClass().getSimpleName());
            return false;
        }
    }

    /** Call only when the current page token still owns this player's dialog. */
    public void close(Player player) {
        try { publicApi("net.kyori.adventure.audience.Audience").getMethod("closeDialog").invoke(player); }
        catch (ClassNotFoundException | NoSuchMethodException unavailable) { /* Older Paper has no dialogs. */ }
        catch (ReflectiveOperationException | RuntimeException | LinkageError failure) {
            plugin.getLogger().warning("Could not dismiss Java number dialog: " + failure.getClass().getSimpleName());
        }
    }

    private Class<?> publicApi(String name) throws ReflectiveOperationException {
        Class<?> type = classes.load(name, plugin.getClass().getClassLoader());
        if (!type.isInterface() || !Modifier.isPublic(type.getModifiers())) {
            throw new NoSuchMethodException("A public API interface is required: " + name);
        }
        return type;
    }

    @FunctionalInterface private interface Reply { void accept(Object response, Object audience); }

    private final class Api {
        final Class<?> dialog = publicApi("io.papermc.paper.dialog.Dialog");
        final Class<?> dialogLike = publicApi("net.kyori.adventure.dialog.DialogLike");
        final Class<?> factory = publicApi("io.papermc.paper.registry.RegistryBuilderFactory");
        final Class<?> registryBuilder = publicApi(DATA + "DialogRegistryEntry$Builder");
        final Class<?> base = publicApi(DATA + "DialogBase");
        final Class<?> baseBuilder = publicApi(DATA + "DialogBase$Builder");
        final Class<?> afterAction = classes.load(DATA + "DialogBase$DialogAfterAction", plugin.getClass().getClassLoader());
        final Class<?> input = publicApi(DATA + "input.DialogInput");
        final Class<?> textBuilder = publicApi(DATA + "input.TextDialogInput$Builder");
        final Class<?> body = publicApi(DATA + "body.DialogBody");
        final Class<?> button = publicApi(DATA + "ActionButton");
        final Class<?> action = publicApi(DATA + "action.DialogAction");
        final Class<?> callback = publicApi(DATA + "action.DialogActionCallback");
        final Class<?> response = publicApi("io.papermc.paper.dialog.DialogResponseView");
        final Class<?> type = publicApi(DATA + "type.DialogType");
        final Class<?> audience = publicApi("net.kyori.adventure.audience.Audience");
        final Method show = audience.getMethod("showDialog", dialogLike);

        Api() throws ReflectiveOperationException {
            if (!afterAction.isEnum() || !Modifier.isPublic(afterAction.getModifiers()))
                throw new NoSuchMethodException("A public dialog action enum is required");
            afterAction.getField("CLOSE");
            dialog.getMethod("create", Consumer.class);
            factory.getMethod("empty");
            registryBuilder.getMethod("base", base);
            registryBuilder.getMethod("type", type);
            input.getMethod("text", String.class, Component.class);
            textBuilder.getMethod("initial", String.class);
            textBuilder.getMethod("maxLength", int.class);
            textBuilder.getMethod("build");
            body.getMethod("plainMessage", Component.class);
            base.getMethod("builder", Component.class);
            baseBuilder.getMethod("canCloseWithEscape", boolean.class);
            baseBuilder.getMethod("pause", boolean.class);
            baseBuilder.getMethod("afterAction", afterAction);
            baseBuilder.getMethod("body", List.class);
            baseBuilder.getMethod("inputs", List.class);
            baseBuilder.getMethod("build");
            button.getMethod("create", Component.class, Component.class, int.class, action);
            action.getMethod("customClick", callback, ClickCallback.Options.class);
            type.getMethod("confirmation", button, button);
            callback.getMethod("accept", response, audience);
            response.getMethod("getText", String.class);
            audience.getMethod("closeDialog");
        }

        Object callback(Reply reply) {
            return Proxy.newProxyInstance(callback.getClassLoader(), new Class<?>[]{callback}, (proxy, method, args) -> {
                if (method.getDeclaringClass() == Object.class) return switch (method.getName()) {
                    case "toString" -> "ExcelShops number response";
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    default -> null;
                };
                if (method.getName().equals("accept") && args != null && args.length == 2)
                    reply.accept(args[0], args[1]);
                return null;
            });
        }

        Object button(String label, Object handler) throws ReflectiveOperationException {
            // The UUID-bound shared gate performs single use. An unrelated player must not
            // burn the legitimate player's callback by presenting a copied action identifier.
            ClickCallback.Options options = ClickCallback.Options.builder().uses(ClickCallback.UNLIMITED_USES)
                    .lifetime(Duration.ofSeconds(LIFETIME_SECONDS)).build();
            Object click = action.getMethod("customClick", callback, ClickCallback.Options.class)
                    .invoke(null, handler, options);
            return button.getMethod("create", Component.class, Component.class, int.class, action)
                    .invoke(null, Component.text(label), null, 150, click);
        }
    }

    /** Shared by both buttons, including synchronous replies during an unsuccessful send. */
    private final class ResponseGate {
        private final Player owner;
        private final UUID ownerId;
        private final long started = nanos.getAsLong();
        private boolean sendFinished, accepted, consumed;
        private Runnable pending;
        ResponseGate(Player owner) { this.owner = owner; ownerId = owner.getUniqueId(); }
        boolean isOwner(Object audience) {
            return audience instanceof Player sender && ownerId.equals(sender.getUniqueId());
        }
        private boolean expired() {
            return nanos.getAsLong() - started >= Duration.ofSeconds(LIFETIME_SECONDS).toNanos();
        }
        void reply(Runnable action) {
            synchronized (this) {
                if (consumed || expired()) return;
                if (!sendFinished) { if (pending == null) pending = action; return; }
                if (!accepted) return;
                consumed = true;
            }
            dispatch(action);
        }
        void sent(boolean success) {
            Runnable action;
            synchronized (this) {
                if (sendFinished) return;
                sendFinished = true; accepted = success && !expired();
                action = accepted ? pending : null; pending = null;
                if (!accepted || action != null) consumed = true;
            }
            if (action != null) dispatch(action);
        }
        private void dispatch(Runnable action) {
            if (!plugin.isEnabled() || !owner.isOnline() || expired()) return;
            try { action.run(); }
            catch (RuntimeException rejected) {
                plugin.getLogger().warning("Java number dialog callback was rejected: "
                        + rejected.getClass().getSimpleName());
            }
        }
    }
}

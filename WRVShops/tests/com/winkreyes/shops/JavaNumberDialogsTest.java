// SPDX-License-Identifier: MIT
package com.winkreyes.shops;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.logging.Logger;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Public-interface fixtures deliberately use hidden implementations, never fake Paper classes. */
class JavaNumberDialogsTest {
    private WRVShops plugin;
    private Player player;
    private JavaNumberDialogs dialogs;
    private final AtomicLong time = new AtomicLong();
    private final List<String> numbers = new ArrayList<>();
    private final AtomicInteger closes = new AtomicInteger();
    private final UUID id = UUID.fromString("00000000-0000-0000-0000-000000000432");
    private static State state;

    @BeforeEach void setup() {
        state = new State(); time.set(0); numbers.clear(); closes.set(0);
        plugin = mock(WRVShops.class);
        when(plugin.isEnabled()).thenReturn(true);
        when(plugin.getLogger()).thenReturn(Logger.getLogger("JavaNumberDialogsTest"));
        player = mock(Player.class, withSettings().extraInterfaces(FixtureAudience.class));
        when(player.getUniqueId()).thenReturn(id); when(player.isOnline()).thenReturn(true);
        dialogs = new JavaNumberDialogs(plugin, JavaNumberDialogsTest::lookup, time::get);
    }

    private boolean number() { return dialogs.number(player, "Exact quantity", "Items, not bundles", "4", numbers::add, closes::incrementAndGet); }
    private void use(String text) { state.use.accept(new HiddenResponse(text), (FixtureAudience) player); }
    private void cancel() { state.cancel.accept(null, (FixtureAudience) player); }

    @Test void publicInterfacesWorkWithHiddenBuildersAndPrivateNativeInput() {
        assertTrue(dialogs.supported()); assertTrue(number());
        assertEquals(Component.text("Exact quantity"), state.title);
        assertEquals(Component.text("Items, not bundles"), state.details);
        assertEquals("amount", state.key); assertEquals("4", state.initial);
        assertEquals(32, state.maxLength); assertFalse(state.escape); assertFalse(state.pause);
        assertEquals(FixtureAfterAction.CLOSE, state.after);
        assertEquals(List.of(Component.text("Use amount"), Component.text("Cancel")), state.labels);
        assertEquals(2, state.options.size());
        for (ClickCallback.Options options : state.options) {
            assertEquals(Duration.ofSeconds(60), options.lifetime());
            assertEquals(ClickCallback.UNLIMITED_USES, options.uses());
        }
        verify((FixtureAudience) player).showDialog(any(FixtureDialogLike.class));
        verify(player, never()).sendMessage(anyString());
        use("250.25"); assertEquals(List.of("250.25"), numbers); assertEquals(0, closes.get());
    }

    @Test void oneReplyAcrossBothButtonsPreventsDuplicateAndLateCancel() {
        assertTrue(number()); use("12"); use("99"); cancel();
        assertEquals(List.of("12"), numbers); assertEquals(0, closes.get());
    }

    @Test void cancellationConsumesBothButtons() {
        assertTrue(number()); cancel(); use("12"); cancel();
        assertTrue(numbers.isEmpty()); assertEquals(1, closes.get());
    }

    @Test void copiedCallbackCannotBeUsedOrConsumedByAnotherPlayer() {
        assertTrue(number());
        Player other = mock(Player.class, withSettings().extraInterfaces(FixtureAudience.class));
        when(other.getUniqueId()).thenReturn(UUID.randomUUID());
        state.use.accept(new HiddenResponse("999"), (FixtureAudience) other);
        state.cancel.accept(null, (FixtureAudience) other);
        state.use.accept(new HiddenResponse("999"), mock(FixtureAudience.class));
        assertTrue(numbers.isEmpty()); assertEquals(0, closes.get());
        use("12"); assertEquals(List.of("12"), numbers);
    }

    @ParameterizedTest @ValueSource(booleans = {true, false})
    void expiredCallbacksNeverReachCaller(boolean useButton) {
        assertTrue(number()); time.set(Duration.ofSeconds(60).toNanos());
        if (useButton) use("12"); else cancel();
        assertTrue(numbers.isEmpty()); assertEquals(0, closes.get());
    }

    @ParameterizedTest @ValueSource(strings = {"disabled", "offline"})
    void unavailablePlayerOrPluginPreventsSendAndLateReply(String condition) {
        assertTrue(number());
        if (condition.equals("disabled")) when(plugin.isEnabled()).thenReturn(false);
        else when(player.isOnline()).thenReturn(false);
        use("12"); cancel(); assertTrue(numbers.isEmpty()); assertEquals(0, closes.get());
        assertFalse(number()); verify((FixtureAudience) player, times(1)).showDialog(any());
    }

    @Test void missingApiReturnsFalseWithoutCallbacks() {
        dialogs = new JavaNumberDialogs(plugin, (name, loader) -> { throw new ClassNotFoundException(name); }, time::get);
        assertFalse(dialogs.supported()); assertFalse(number());
        assertTrue(numbers.isEmpty()); assertEquals(0, closes.get());
        verify((FixtureAudience) player, never()).showDialog(any());
    }

    @Test void implementationClassCannotBeUsedAsReflectionTarget() {
        dialogs = new JavaNumberDialogs(plugin, (name, loader) -> name.endsWith("TextDialogInput$Builder")
                ? HiddenTextBuilder.class : lookup(name, loader), time::get);
        assertFalse(dialogs.supported()); assertFalse(number());
        verify((FixtureAudience) player, never()).showDialog(any());
    }

    @Test void missingRequiredInputApiIsDetectedBeforeAnySend() {
        dialogs = new JavaNumberDialogs(plugin, (name, loader) -> name.endsWith("TextDialogInput$Builder")
                ? MissingTextBuilder.class : lookup(name, loader), time::get);
        assertFalse(dialogs.supported()); assertFalse(number());
        verify((FixtureAudience) player, never()).showDialog(any());
        assertTrue(numbers.isEmpty()); assertEquals(0, closes.get());
    }

    @Test void failedSendDisarmsAlreadyRegisteredCallbacks() {
        doThrow(new IllegalStateException("Send failed")).when((FixtureAudience) player).showDialog(any());
        assertFalse(number()); use("12"); cancel();
        assertTrue(numbers.isEmpty()); assertEquals(0, closes.get());
    }

    @ParameterizedTest @ValueSource(booleans = {true, false})
    void synchronousResponseIsDeferredUntilSendSucceeds(boolean success) {
        doAnswer(call -> {
            use("12"); cancel(); assertTrue(numbers.isEmpty()); assertEquals(0, closes.get());
            if (!success) throw new IllegalStateException("Send failed after callback");
            return null;
        }).when((FixtureAudience) player).showDialog(any());
        assertEquals(success, number());
        assertEquals(success ? List.of("12") : List.of(), numbers); assertEquals(0, closes.get());
    }

    @ParameterizedTest @ValueSource(strings = {"null", "oversized", "throws"})
    void malformedResponseCancelsOnceAndCannotBeReplayed(String kind) {
        assertTrue(number());
        if (kind.equals("throws")) state.use.accept(key -> { throw new IllegalArgumentException("Invalid payload"); }, (FixtureAudience) player);
        else use(kind.equals("null") ? null : "9".repeat(33));
        use("12"); cancel(); assertTrue(numbers.isEmpty()); assertEquals(1, closes.get());
    }

    @Test void concurrentSubmitAndCancelAreMutuallyExclusive() throws Exception {
        AtomicInteger accepted = new AtomicInteger();
        assertTrue(dialogs.number(player, "Title", "Details", "1", value -> accepted.incrementAndGet(), closes::incrementAndGet));
        try (var executor = Executors.newFixedThreadPool(2)) {
            CountDownLatch start = new CountDownLatch(1);
            Future<?> use = executor.submit(() -> { await(start); use("12"); });
            Future<?> cancel = executor.submit(() -> { await(start); cancel(); });
            start.countDown(); use.get(); cancel.get();
        }
        assertEquals(1, accepted.get() + closes.get());
    }

    @Test void callbackFailureDoesNotCauseSecondCallback() {
        AtomicInteger accepted = new AtomicInteger();
        assertTrue(dialogs.number(player, "Title", "Details", "1", value -> {
            accepted.incrementAndGet(); throw new IllegalStateException("No longer available");
        }, closes::incrementAndGet));
        assertDoesNotThrow(() -> use("12")); cancel();
        assertEquals(1, accepted.get()); assertEquals(0, closes.get());
    }

    @Test void closeUsesPublicAudienceApiOnly() {
        dialogs.close(player); verify((FixtureAudience) player).closeDialog();
        verify(player, never()).closeInventory();
    }

    private static void await(CountDownLatch latch) {
        try { latch.await(); } catch (InterruptedException ex) { Thread.currentThread().interrupt(); throw new IllegalStateException(ex); }
    }

    private static Class<?> lookup(String name, ClassLoader ignored) throws ClassNotFoundException {
        String prefix = "io.papermc.paper.registry.data.dialog.";
        Map<String, Class<?>> types = Map.ofEntries(
                Map.entry("io.papermc.paper.dialog.Dialog", FixtureDialog.class),
                Map.entry("net.kyori.adventure.dialog.DialogLike", FixtureDialogLike.class),
                Map.entry("io.papermc.paper.registry.RegistryBuilderFactory", FixtureFactory.class),
                Map.entry(prefix + "DialogRegistryEntry$Builder", FixtureRegistryBuilder.class),
                Map.entry(prefix + "DialogBase", FixtureBase.class),
                Map.entry(prefix + "DialogBase$Builder", FixtureBaseBuilder.class),
                Map.entry(prefix + "DialogBase$DialogAfterAction", FixtureAfterAction.class),
                Map.entry(prefix + "input.DialogInput", FixtureInput.class),
                Map.entry(prefix + "input.TextDialogInput$Builder", FixtureTextBuilder.class),
                Map.entry(prefix + "body.DialogBody", FixtureBody.class),
                Map.entry(prefix + "ActionButton", FixtureButton.class),
                Map.entry(prefix + "action.DialogAction", FixtureAction.class),
                Map.entry(prefix + "action.DialogActionCallback", FixtureCallback.class),
                Map.entry("io.papermc.paper.dialog.DialogResponseView", FixtureResponse.class),
                Map.entry(prefix + "type.DialogType", FixtureType.class),
                Map.entry("net.kyori.adventure.audience.Audience", FixtureAudience.class));
        Class<?> type = types.get(name); if (type == null) throw new ClassNotFoundException(name); return type;
    }

    private static final class State {
        Component title, details;
        String key, initial;
        int maxLength;
        boolean escape = true, pause = true;
        FixtureAfterAction after;
        FixtureCallback use, cancel;
        final List<Component> labels = new ArrayList<>();
        final List<ClickCallback.Options> options = new ArrayList<>();
    }
    public interface FixtureAudience { void showDialog(FixtureDialogLike dialog); void closeDialog(); }
    public interface FixtureDialogLike { }
    public interface FixtureDialog extends FixtureDialogLike {
        static FixtureDialog create(Consumer<FixtureFactory> configure) {
            configure.accept(() -> new HiddenRegistryBuilder()); return new HiddenDialog();
        }
    }
    private record HiddenDialog() implements FixtureDialog { }
    public interface FixtureFactory { FixtureRegistryBuilder empty(); }
    public interface FixtureRegistryBuilder { FixtureRegistryBuilder base(FixtureBase base); FixtureRegistryBuilder type(FixtureType type); }
    private static final class HiddenRegistryBuilder implements FixtureRegistryBuilder {
        public FixtureRegistryBuilder base(FixtureBase base) { return this; }
        public FixtureRegistryBuilder type(FixtureType type) { return this; }
    }
    public enum FixtureAfterAction { CLOSE, NONE }
    public interface FixtureBase {
        static FixtureBaseBuilder builder(Component title) { state.title = title; return new HiddenBaseBuilder(); }
    }
    public interface FixtureBaseBuilder {
        FixtureBaseBuilder canCloseWithEscape(boolean enabled); FixtureBaseBuilder pause(boolean enabled);
        FixtureBaseBuilder afterAction(FixtureAfterAction action); FixtureBaseBuilder body(List<?> body);
        FixtureBaseBuilder inputs(List<?> input); FixtureBase build();
    }
    private static final class HiddenBaseBuilder implements FixtureBaseBuilder {
        public FixtureBaseBuilder canCloseWithEscape(boolean enabled) { state.escape = enabled; return this; }
        public FixtureBaseBuilder pause(boolean enabled) { state.pause = enabled; return this; }
        public FixtureBaseBuilder afterAction(FixtureAfterAction action) { state.after = action; return this; }
        public FixtureBaseBuilder body(List<?> body) { return this; }
        public FixtureBaseBuilder inputs(List<?> input) { return this; }
        public FixtureBase build() { return new FixtureBase() { }; }
    }
    public interface FixtureInput {
        static FixtureTextBuilder text(String key, Component label) { state.key = key; return new HiddenTextBuilder(); }
    }
    public interface FixtureTextBuilder {
        FixtureTextBuilder initial(String initial); FixtureTextBuilder maxLength(int maxLength); FixtureInput build();
    }
    public interface MissingTextBuilder { FixtureInput build(); }
    private static final class HiddenTextBuilder implements FixtureTextBuilder {
        public FixtureTextBuilder initial(String initial) { state.initial = initial; return this; }
        public FixtureTextBuilder maxLength(int maximum) { state.maxLength = maximum; return this; }
        public FixtureInput build() { return new FixtureInput() { }; }
    }
    public interface FixtureBody {
        static FixtureBody plainMessage(Component details) { state.details = details; return new FixtureBody() { }; }
    }
    public interface FixtureAction {
        static FixtureAction customClick(FixtureCallback callback, ClickCallback.Options options) {
            state.options.add(options); return new HiddenAction(callback);
        }
    }
    private record HiddenAction(FixtureCallback callback) implements FixtureAction { }
    public interface FixtureButton {
        static FixtureButton create(Component label, Component tooltip, int width, FixtureAction action) {
            state.labels.add(label); return new HiddenButton(((HiddenAction) action).callback());
        }
    }
    private record HiddenButton(FixtureCallback callback) implements FixtureButton { }
    public interface FixtureType {
        static FixtureType confirmation(FixtureButton use, FixtureButton cancel) {
            state.use = ((HiddenButton) use).callback(); state.cancel = ((HiddenButton) cancel).callback();
            return new FixtureType() { };
        }
    }
    public interface FixtureCallback { void accept(FixtureResponse response, FixtureAudience audience); }
    public interface FixtureResponse { String getText(String key); }
    private record HiddenResponse(String text) implements FixtureResponse {
        public String getText(String key) { assertEquals("amount", key); return text; }
    }
}

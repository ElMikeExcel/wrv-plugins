// SPDX-License-Identifier: MIT
package com.winkreyes.shops;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.logging.Logger;
import org.bukkit.Server;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.geysermc.cumulus.form.Form;
import org.geysermc.fixtures.FormFixtures;
import org.geysermc.fixtures.GeyserFixture;
import org.geysermc.fixtures.FloodgateFixture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Tests original API fixtures only. These are not real-Geyser or phone-client acceptance tests. */
public class BedrockFormsTest {
    private WRVShops plugin;
    private Plugin geyser;
    private PluginManager plugins;
    private Player player;
    private YamlConfiguration config;
    private BedrockForms forms;
    private final List<String> numbers = new ArrayList<>();
    private final List<Integer> choices = new ArrayList<>();
    private final AtomicInteger closes = new AtomicInteger();

    @BeforeEach void setup() {
        GeyserFixture.reset(); FormFixtures.reset(); FloodgateFixture.reset();
        plugin = mock(WRVShops.class); geyser = mock(Plugin.class);
        Server server = mock(Server.class); plugins = mock(PluginManager.class);
        player = mock(Player.class); config = new YamlConfiguration();
        config.set("bedrock-forms", true);
        when(plugin.getConfig()).thenReturn(config);
        when(plugin.getLogger()).thenReturn(Logger.getLogger("BedrockFormsFixtureTest"));
        when(plugin.getServer()).thenReturn(server);
        when(plugin.isEnabled()).thenReturn(true);
        when(server.getPluginManager()).thenReturn(plugins);
        when(plugins.getPlugin("Geyser-Spigot")).thenReturn(geyser);
        when(geyser.isEnabled()).thenReturn(true);
        when(player.getUniqueId()).thenReturn(UUID.fromString("00000000-0000-0000-0000-000000000123"));
        forms = new BedrockForms(plugin);
        numbers.clear(); choices.clear(); closes.set(0);
    }
    private boolean number() { return forms.number(player, "Quantity", "How many?", "4", numbers::add, closes::incrementAndGet); }
    private boolean choices() { return forms.choices(player, "Confirm", "Buy 4 items?", new String[] {"Confirm", "Back", "Cancel"}, choices::add, closes::incrementAndGet); }

    @Test void javaDialogsAreAllowedWithoutInstalledBridges() {
        when(plugins.getPlugin("Geyser-Spigot")).thenReturn(null);
        assertTrue(forms.canUseJavaDialog(player));
    }

    @Test void javaDialogsAreAllowedForPositivelyIdentifiedJavaPlayer() {
        installFloodgate();
        GeyserFixture.online = false; FloodgateFixture.bedrock = false;
        assertTrue(forms.canUseJavaDialog(player));
        assertEquals(player.getUniqueId(), FloodgateFixture.lastPlayer);
    }

    @ParameterizedTest @ValueSource(booleans = {true, false})
    void knownBedrockNeverGetsJavaDialogRegardlessOfFormPreference(boolean enabled) {
        config.set("bedrock-forms", enabled);
        assertFalse(forms.canUseJavaDialog(player));
    }

    @Test void rejectedFormDoesNotTurnBedrockIntoJava() {
        GeyserFixture.sendAccepted = false;
        assertFalse(number());
        assertFalse(forms.canUseJavaDialog(player));
    }

    @ParameterizedTest @ValueSource(booleans = {true, false})
    void floodgateOnlyProxyPlayerNeverGetsJavaDialog(boolean enabled) {
        when(plugins.getPlugin("Geyser-Spigot")).thenReturn(null);
        installFloodgate(); config.set("bedrock-forms", enabled);
        assertFalse(forms.canUseJavaDialog(player));
        assertEquals(player.getUniqueId(), FloodgateFixture.lastPlayer);
    }

    @Test void negativeGeyserResultDoesNotOverrideFloodgateBedrockDetection() {
        installFloodgate(); GeyserFixture.online = false;
        assertFalse(forms.canUseJavaDialog(player));
    }

    @ParameterizedTest @ValueSource(strings = {"geyser-disabled", "geyser-api-null", "geyser-class-missing", "geyser-method-missing", "floodgate-disabled", "floodgate-api-null", "floodgate-class-missing", "floodgate-method-missing", "floodgate-throws", "linkage", "manager-throws"})
    void uncertainDetectionNeverEnablesJavaDialog(String failure) {
        Plugin floodgate = installFloodgate();
        GeyserFixture.online = false; FloodgateFixture.bedrock = false;
        switch (failure) {
            case "geyser-disabled" -> when(geyser.isEnabled()).thenReturn(false);
            case "geyser-api-null" -> GeyserFixture.apiAvailable = false;
            case "geyser-class-missing" -> forms = missingDetectionApi("org.geysermc.geyser.api.GeyserApi");
            case "geyser-method-missing" -> forms = replacement("org.geysermc.geyser.api.GeyserApi", MissingResponseMethod.class);
            case "floodgate-disabled" -> when(floodgate.isEnabled()).thenReturn(false);
            case "floodgate-api-null" -> FloodgateFixture.apiAvailable = false;
            case "floodgate-class-missing" -> forms = missingDetectionApi("org.geysermc.floodgate.api.FloodgateApi");
            case "floodgate-method-missing" -> forms = replacement("org.geysermc.floodgate.api.FloodgateApi", MissingResponseMethod.class);
            case "floodgate-throws" -> FloodgateFixture.throwOnLookup = true;
            case "linkage" -> forms = new BedrockForms(plugin, (name, loader) -> { throw new NoClassDefFoundError("fixture"); });
            case "manager-throws" -> when(plugins.getPlugin("Geyser-Spigot")).thenThrow(new IllegalStateException("fixture"));
            default -> fail("Unhandled fixture");
        }
        assertFalse(forms.canUseJavaDialog(player));
    }

    private Plugin installFloodgate() {
        Plugin floodgate = mock(Plugin.class);
        when(plugins.getPlugin("floodgate")).thenReturn(floodgate);
        when(floodgate.isEnabled()).thenReturn(true);
        return floodgate;
    }

    private BedrockForms missingDetectionApi(String missing) {
        return new BedrockForms(plugin, (name, loader) -> {
            if (name.equals(missing)) throw new ClassNotFoundException(name);
            return Class.forName(name, true, loader);
        });
    }

    @Test void numberUsesPublicInterfacesWithNonpublicImplementationClasses() {
        assertTrue(number());
        FormFixtures.State form = FormFixtures.last();
        assertEquals("Quantity", form.title);
        assertEquals("How many?", form.text);
        assertEquals("Enter a number", form.placeholder);
        assertEquals("4", form.initial);
        assertEquals(player.getUniqueId(), GeyserFixture.lastPlayer);
        form.number("12");
        assertEquals(List.of("12"), numbers);
        assertEquals(0, closes.get());
    }

    @Test void simpleFormUsesPublicInheritedMethodsAndPreservesLabels() {
        assertTrue(choices());
        FormFixtures.State form = FormFixtures.last();
        assertEquals("Confirm", form.title);
        assertEquals("Buy 4 items?", form.text);
        assertEquals(List.of("Confirm", "Back", "Cancel"), form.buttons);
        form.choice(0);
        assertEquals(List.of(0), choices);
        assertEquals(0, closes.get());
    }

    @ParameterizedTest @ValueSource(booleans = {true, false})
    void closedAndInvalidResultsConsumeFormAndBlockEveryLateValidReply(boolean invalid) {
        assertTrue(number());
        FormFixtures.State form = FormFixtures.last();
        if (invalid) form.invalid(); else form.close();
        form.number("999"); form.close(); form.invalid();
        assertTrue(numbers.isEmpty());
        assertEquals(1, closes.get());
    }

    @Test void repeatedValidResponseIsForwardedOnlyOnceAndLateCloseCannotCancelNewPage() {
        assertTrue(choices());
        FormFixtures.State form = FormFixtures.last();
        form.choice(0); form.choice(1); form.close(); form.invalid();
        assertEquals(List.of(0), choices);
        assertEquals(0, closes.get());
    }

    @ParameterizedTest @ValueSource(ints = {-1, 3, Integer.MAX_VALUE})
    void invalidButtonIndexBecomesOneCancellation(int id) {
        assertTrue(choices());
        FormFixtures.last().choice(id);
        FormFixtures.last().choice(0);
        assertTrue(choices.isEmpty());
        assertEquals(1, closes.get());
    }

    @Test void nullInputBecomesCancellationAndCannotBeReplayed() {
        assertTrue(number());
        FormFixtures.last().number(null);
        FormFixtures.last().number("1");
        assertTrue(numbers.isEmpty());
        assertEquals(1, closes.get());
    }

    @ParameterizedTest @ValueSource(booleans = {true, false})
    void responseApiExceptionBecomesCancellation(boolean numeric) {
        assertTrue(numeric ? number() : choices());
        FormFixtures.throwOnResponse = true;
        if (numeric) FormFixtures.last().number("1"); else FormFixtures.last().choice(0);
        assertTrue(numbers.isEmpty()); assertTrue(choices.isEmpty());
        assertEquals(1, closes.get());
    }

    @ParameterizedTest @ValueSource(booleans = {true, false})
    void rejectedSendSuppressesAllLateCallbacks(boolean numeric) {
        GeyserFixture.sendAccepted = false;
        assertFalse(numeric ? number() : choices());
        FormFixtures.State form = FormFixtures.last();
        if (numeric) form.number("1"); else form.choice(0);
        form.close(); form.invalid();
        assertTrue(numbers.isEmpty()); assertTrue(choices.isEmpty());
        assertEquals(0, closes.get());
    }

    @Test void sendExceptionAfterReceivingFormCannotLeaveActionableCallbacks() {
        GeyserFixture.throwOnSend = true;
        assertFalse(choices());
        FormFixtures.last().choice(0); FormFixtures.last().close();
        assertTrue(choices.isEmpty()); assertEquals(0, closes.get());
    }

    @Test void responseBeforeSendReturnsIsDeferredUntilAccepted() {
        GeyserFixture.duringSend = form -> {
            FormFixtures.last().number("7");
            FormFixtures.last().close();
            assertTrue(numbers.isEmpty());
        };
        assertTrue(number());
        assertEquals(List.of("7"), numbers);
        assertEquals(0, closes.get());
    }

    @Test void responseBeforeRejectedSendReturnsIsDiscarded() {
        GeyserFixture.duringSend = form -> FormFixtures.last().choice(0);
        GeyserFixture.sendAccepted = false;
        assertFalse(choices());
        assertTrue(choices.isEmpty()); assertEquals(0, closes.get());
    }

    @Test void closedResponseBeforeSendReturnsSuppressesLaterConfirmation() {
        GeyserFixture.duringSend = form -> { FormFixtures.last().close(); FormFixtures.last().choice(0); };
        assertTrue(choices());
        assertTrue(choices.isEmpty()); assertEquals(1, closes.get());
    }

    @Test void concurrentCloseAndValidResponseCannotBothReachCaller() throws Exception {
        AtomicInteger replies = new AtomicInteger();
        assertTrue(forms.choices(player, "Title", "Text", new String[] {"Confirm"}, id -> replies.incrementAndGet(), closes::incrementAndGet));
        FormFixtures.State form = FormFixtures.last();
        try (ExecutorService workers = Executors.newFixedThreadPool(2)) {
            CountDownLatch start = new CountDownLatch(1);
            Future<?> a = workers.submit(() -> { try { start.await(); form.choice(0); } catch (InterruptedException e) { Thread.currentThread().interrupt(); } });
            Future<?> b = workers.submit(() -> { try { start.await(); form.close(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); } });
            start.countDown(); a.get(); b.get();
        }
        assertEquals(1, replies.get() + closes.get());
    }

    @Test void disabledPluginCannotRunLateCallbacks() {
        assertTrue(number());
        when(plugin.isEnabled()).thenReturn(false);
        FormFixtures.last().number("1"); FormFixtures.last().close();
        assertTrue(numbers.isEmpty()); assertEquals(0, closes.get());
    }

    @Test void callbackExceptionDoesNotTriggerAnotherCloseOrReplay() {
        AtomicInteger attempts = new AtomicInteger();
        assertTrue(forms.choices(player, "Title", "Text", new String[] {"Confirm"}, id -> {
            attempts.incrementAndGet(); throw new IllegalStateException("Caller was disabled");
        }, closes::incrementAndGet));
        assertDoesNotThrow(() -> FormFixtures.last().choice(0));
        FormFixtures.last().choice(0); FormFixtures.last().close();
        assertEquals(1, attempts.get()); assertEquals(0, closes.get());
    }

    @ParameterizedTest @ValueSource(strings = {"config", "plugin-absent", "plugin-disabled", "java-player", "api-absent"})
    void unavailableNativePathFallsBackWithoutSendingOrCallingHandlers(String condition) {
        switch (condition) {
            case "config" -> config.set("bedrock-forms", false);
            case "plugin-absent" -> when(plugins.getPlugin("Geyser-Spigot")).thenReturn(null);
            case "plugin-disabled" -> when(geyser.isEnabled()).thenReturn(false);
            case "java-player" -> GeyserFixture.online = false;
            case "api-absent" -> GeyserFixture.apiAvailable = false;
        }
        assertFalse(number()); assertFalse(choices());
        assertTrue(GeyserFixture.sentForms.isEmpty());
        assertTrue(numbers.isEmpty()); assertEquals(0, closes.get());
    }

    @ParameterizedTest @ValueSource(strings = {"valid", "close", "build"})
    void handlerRegistrationOrBuildFailuresMustFallBack(String kind) {
        switch (kind) {
            case "valid" -> FormFixtures.throwOnValidHandler = true;
            case "close" -> FormFixtures.throwOnClosedHandler = true;
            case "build" -> FormFixtures.throwOnBuild = true;
        }
        assertFalse(number()); assertFalse(choices());
        assertTrue(GeyserFixture.sentForms.isEmpty());
        assertEquals(0, closes.get());
    }

    @ParameterizedTest @ValueSource(strings = {"org.geysermc.geyser.api.GeyserApi", "org.geysermc.cumulus.form.CustomForm", "org.geysermc.cumulus.response.CustomFormResponse", "org.geysermc.cumulus.form.Form"})
    void missingApiInterfacesFallBackAndDisableAnyAlreadyBuiltForm(String missing) {
        forms = new BedrockForms(plugin, (name, loader) -> {
            if (name.equals(missing)) throw new ClassNotFoundException(name);
            return Class.forName(name, true, loader);
        });
        assertFalse(number());
        for (FormFixtures.State state : FormFixtures.built) { state.number("1"); state.close(); }
        assertTrue(numbers.isEmpty()); assertEquals(0, closes.get());
    }

    @ParameterizedTest @ValueSource(booleans = {true, false})
    void missingRequiredHandlerOnPublicBuilderInterfaceFallsBack(boolean missingClose) {
        forms = replacement("org.geysermc.cumulus.form.CustomForm", missingClose ? MissingCloseForm.class : MissingValidForm.class);
        assertFalse(number());
        assertTrue(GeyserFixture.sentForms.isEmpty());
    }

    @Test void nonpublicBuilderInterfaceFallsBack() {
        forms = replacement("org.geysermc.cumulus.form.CustomForm", NonpublicBuilderForm.class);
        assertFalse(number()); assertTrue(GeyserFixture.sentForms.isEmpty());
    }

    @Test void responseInterfaceWithoutRequiredMethodFallsBack() {
        forms = replacement("org.geysermc.cumulus.response.CustomFormResponse", MissingResponseMethod.class);
        assertFalse(number()); assertTrue(GeyserFixture.sentForms.isEmpty());
    }

    @Test void implementationClassIsNotAcceptedAsPublicApi() {
        forms = replacement("org.geysermc.cumulus.form.CustomForm", String.class);
        assertFalse(number()); assertTrue(GeyserFixture.sentForms.isEmpty());
    }

    @Test void missingSendMethodFallsBackAndMakesBuiltFormInert() {
        forms = replacement("org.geysermc.geyser.api.GeyserApi", MissingSendApi.class);
        assertFalse(number());
        FormFixtures.last().number("1"); FormFixtures.last().close();
        assertTrue(numbers.isEmpty()); assertEquals(0, closes.get());
    }

    @Test void linkageFailureFallsBackInsteadOfBreakingPurchaseMenu() {
        forms = new BedrockForms(plugin, (name, loader) -> { throw new NoClassDefFoundError("fixture missing transitive API"); });
        assertFalse(number()); assertFalse(choices());
        assertTrue(GeyserFixture.sentForms.isEmpty());
    }

    @Test void emptyChoicesNeverSendAForm() {
        assertFalse(forms.choices(player, "Title", "Text", new String[0], choices::add, closes::incrementAndGet));
        assertTrue(GeyserFixture.sentForms.isEmpty());
    }

    private BedrockForms replacement(String requested, Class<?> type) {
        return new BedrockForms(plugin, (name, loader) -> name.equals(requested) ? type : Class.forName(name, true, loader));
    }

    public interface MissingCloseForm extends Form {
        static MissingCloseBuilder builder() { throw new AssertionError("Must reject API before building"); }
    }
    public interface MissingCloseBuilder {
        MissingCloseBuilder validResultHandler(BiConsumer<Object,Object> callback);
    }
    public interface MissingValidForm extends Form {
        static MissingValidBuilder builder() { throw new AssertionError("Must reject API before building"); }
    }
    public interface MissingValidBuilder {
        MissingValidBuilder closedOrInvalidResultHandler(Runnable callback);
    }
    public interface NonpublicBuilderForm extends Form {
        static HiddenBuilder builder() { throw new AssertionError("Must reject API before building"); }
    }
    private interface HiddenBuilder { }
    public interface MissingResponseMethod { }
    public interface MissingSendApi {
        static MissingSendApi api() { return id -> new Object(); }
        Object connectionByUuid(UUID id);
    }
    @Test void positiveBedrockDetectionIgnoresFormPreference(){
        config.set("bedrock-forms",false);assertTrue(forms.isBedrock(player));
    }
    @Test void positiveBedrockDetectionFallsBackToFloodgateWhenGeyserApiIsUnavailable(){
        installFloodgate();GeyserFixture.apiAvailable=false;FloodgateFixture.bedrock=true;assertTrue(forms.isBedrock(player));
    }
    @Test void positiveBedrockDetectionDoesNotClassifyJavaOrApiErrorsAsBedrock(){
        GeyserFixture.online=false;assertFalse(forms.isBedrock(player));GeyserFixture.apiAvailable=false;assertFalse(forms.isBedrock(player));
    }

}

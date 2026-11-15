package com.winkreyes.excelsummons;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.IntConsumer;
import java.util.logging.Logger;

/**
 * Bedrock form sending — reflection-only Cumulus SimpleForm
 * through GEYSER's own class loader, so there is no compile-time
 * Geyser or Cumulus dependency. This is the live-proven WandBars
 * shape (ExcelSummons 0.1.8; the 0.1.7 Floodgate-first form is
 * retired — see below).
 *
 * 0.1.7 DEFECT (Mike's live proof, Nov 12, 2026, 6:51 PM CST):
 * the main form rendered but every button was dead. The old
 * implementation tried Floodgate FIRST with the OLD Cumulus
 * (org.geysermc.cumulus.SimpleForm) and attached its response
 * handler by hunting for a 1-arg {@code validResultHandler},
 * reading {@code clickedButtonId()} inside a proxy whose
 * failures were swallowed silently. The old-Cumulus response
 * API does not match that shape, the send itself succeeded, so
 * taps vanished without a trace and the Geyser path never ran.
 *
 * THE 0.1.8 SHAPE (mirrors ExcelWandBars' BedrockForms, which is
 * proven live on this server):
 * - Geyser class loader ONLY; the Floodgate attempt is gone.
 * - NEW Cumulus: org.geysermc.cumulus.form.SimpleForm, reached
 *   via GeyserApi.connectionByUuid.
 * - Response handling via FormBuilder.resultHandler(BiConsumer):
 *   the handler receives (form, FormResponseResult); a valid
 *   result unwraps ValidFormResponseResult.response() ->
 *   SimpleFormResponse.clickedButtonId(). A closed form reports
 *   -1 to the callback, as WandBars does.
 * - The send hunts the connection's assignable 1-arg sendForm
 *   overload.
 * - NOTHING is silent anymore: a failed handler attach, an
 *   unparseable response, a missing sendForm, or a failed send
 *   logs at WARNING (the send failure also answers false, and
 *   the caller falls back to chat — a form failure never blocks
 *   a control). Button labels carry NO legacy color codes (the
 *   WandBars 0.3.14 form-contrast lesson: pale text dies on
 *   Bedrock button fills; plain dark ink survives).
 *
 * Callbacks arrive on Geyser's thread; callers hop back to the
 * main thread before touching Bukkit state (SummonsMenus does).
 *
 * 0.1.9 adds the CustomForm twin (buildCustomForm /
 * sendCustomForm) for the pet-rename text input: same Geyser
 * loader, same FormBuilder.resultHandler contract, the valid
 * result unwrapping to CustomFormResponse.asInput(0). A closed
 * rename form reports null and renames nothing.
 */
public final class BedrockForms {
    private BedrockForms() {}

    public static boolean sendSimpleForm(Player player, String title, String body,
                                         List<String> buttons, IntConsumer onButton,
                                         Logger log) {
        try {
            ClassLoader cl = geyserLoader();
            if (cl == null) {
                log.warning("ExcelSummons: Geyser-Spigot is not enabled; "
                        + "cannot send the Bedrock form (chat fallback applies)");
                return false;
            }
            Object conn = geyserConnection(cl, player, log);
            if (conn == null) return false;
            Object form = buildForm(cl, title, body, buttons, onButton, log);
            return sendBuiltForm(conn, form, log);
        } catch (Throwable t) {
            log.warning("ExcelSummons: Bedrock form send failed, chat fallback "
                    + "applies: " + t.getClass().getSimpleName() + ": " + t.getMessage());
            return false;
        }
    }

    /** 0.1.9: the rename text-input form — a Cumulus CustomForm
     *  with ONE text input, built and answered through the exact
     *  0.1.8 resultHandler shape (see buildCustomForm). {@code
     *  onText} receives the submitted text, or null when the
     *  form was closed without submitting. Returns false when
     *  the form could not be sent at all (the caller then tells
     *  the player the rename could not open — there is no chat
     *  fallback for a Bedrock player). */
    public static boolean sendCustomForm(Player player, String title, String inputLabel,
                                         String placeholder, String initial,
                                         java.util.function.Consumer<String> onText,
                                         Logger log) {
        try {
            ClassLoader cl = geyserLoader();
            if (cl == null) {
                log.warning("ExcelSummons: Geyser-Spigot is not enabled; "
                        + "cannot send the Bedrock rename form");
                return false;
            }
            Object conn = geyserConnection(cl, player, log);
            if (conn == null) return false;
            Object form = buildCustomForm(cl, title, inputLabel, placeholder, initial,
                    onText, log);
            return sendBuiltForm(conn, form, log);
        } catch (Throwable t) {
            log.warning("ExcelSummons: Bedrock rename form send failed: "
                    + t.getClass().getSimpleName() + ": " + t.getMessage());
            return false;
        }
    }

    /** The player's Geyser connection, or null (with the warning
     *  logged) when there is none. */
    private static Object geyserConnection(ClassLoader cl, Player player, Logger log)
            throws Exception {
        Class<?> apiClass = Class.forName("org.geysermc.geyser.api.GeyserApi", true, cl);
        Object api = apiClass.getMethod("api").invoke(null);
        Object conn = apiClass.getMethod("connectionByUuid", UUID.class)
                .invoke(api, player.getUniqueId());
        if (conn == null) {
            log.warning("ExcelSummons: no Geyser connection for " + player.getName()
                    + "; cannot send the Bedrock form (chat fallback applies)");
        }
        return conn;
    }

    /** Send a built form on a connection: hunt the assignable
     *  1-arg sendForm overload, as 0.1.8 established. */
    private static boolean sendBuiltForm(Object conn, Object form, Logger log)
            throws Exception {
        for (Method m : conn.getClass().getMethods()) {
            if (m.getName().equals("sendForm") && m.getParameterCount() == 1
                    && m.getParameterTypes()[0].isAssignableFrom(form.getClass())) {
                return Boolean.TRUE.equals(m.invoke(conn, form));
            }
        }
        log.warning("ExcelSummons: no compatible sendForm on the Geyser "
                + "connection; cannot send the Bedrock form (chat fallback applies)");
        return false;
    }

    static ClassLoader geyserLoader() {
        Plugin g = Bukkit.getPluginManager().getPlugin("Geyser-Spigot");
        return g != null && g.isEnabled() ? g.getClass().getClassLoader() : null;
    }

    /**
     * Build a Cumulus SimpleForm with the response handler
     * attached. Public for the harness and rig response tests
     * (the WandBars buildSimple pattern): they build through
     * this exact method against the live Geyser jar's Cumulus
     * classes and drive a synthetic response through the
     * attached handler. Any reflection failure propagates to the
     * caller — attaching the handler is part of the build, and a
     * build that cannot attach it must fail loudly, never send a
     * form whose buttons are dead.
     */
    public static Object buildForm(ClassLoader loader, String title, String body,
                                   List<String> buttons, IntConsumer onButton,
                                   Logger log) throws Exception {
        Object builder = Class.forName("org.geysermc.cumulus.form.SimpleForm", true, loader)
                .getMethod("builder").invoke(null);
        Class<?> builderType = Class.forName("org.geysermc.cumulus.form.SimpleForm$Builder", true, loader);
        Class<?> formBuilder = Class.forName("org.geysermc.cumulus.form.util.FormBuilder", true, loader);
        Class<?> simpleResponse = Class.forName("org.geysermc.cumulus.response.SimpleFormResponse", true, loader);
        Class<?> responseResult = Class.forName("org.geysermc.cumulus.response.result.FormResponseResult", true, loader);
        Class<?> validResult = Class.forName("org.geysermc.cumulus.response.result.ValidFormResponseResult", true, loader);
        formBuilder.getMethod("title", String.class).invoke(builder, title);
        builderType.getMethod("content", String.class).invoke(builder, body);
        for (String b : buttons) builderType.getMethod("button", String.class).invoke(builder, b);
        Method isValid = responseResult.getMethod("isValid");
        Method response = validResult.getMethod("response");
        Method clickedId = simpleResponse.getMethod("clickedButtonId");
        BiConsumer<Object, Object> handler = (form, result) -> {
            int clicked = -1;
            try {
                if (Boolean.TRUE.equals(isValid.invoke(result))) {
                    Object idValue = clickedId.invoke(response.invoke(result));
                    if (idValue instanceof Number) {
                        clicked = ((Number) idValue).intValue();
                    } else {
                        log.warning("ExcelSummons: unparseable Bedrock form response "
                                + "(clickedButtonId returned " + idValue + "); treating as closed");
                    }
                }
            } catch (Exception e) {
                log.warning("ExcelSummons: could not read the Bedrock form response: "
                        + e.getClass().getSimpleName() + ": " + e.getMessage());
                clicked = -1;
            }
            onButton.accept(clicked);
        };
        formBuilder.getMethod("resultHandler", BiConsumer.class).invoke(builder, handler);
        return formBuilder.getMethod("build").invoke(builder);
    }

    /**
     * Build a Cumulus CustomForm carrying one text input, with
     * the response handler attached — the CustomForm twin of
     * {@link #buildForm}, same contract: public for the harness
     * and rig, any reflection failure propagates (a rename form
     * whose handler cannot attach must fail loudly, never render
     * as a dead form). The handler unwraps a valid result to
     * CustomFormResponse.asInput(0); a closed form, an invalid
     * result, or an unreadable response reports null — rename
     * treats null as "changed my mind" and does nothing.
     */
    public static Object buildCustomForm(ClassLoader loader, String title, String inputLabel,
                                         String placeholder, String initial,
                                         java.util.function.Consumer<String> onText,
                                         Logger log) throws Exception {
        Object builder = Class.forName("org.geysermc.cumulus.form.CustomForm", true, loader)
                .getMethod("builder").invoke(null);
        Class<?> builderType = Class.forName("org.geysermc.cumulus.form.CustomForm$Builder", true, loader);
        Class<?> formBuilder = Class.forName("org.geysermc.cumulus.form.util.FormBuilder", true, loader);
        Class<?> customResponse = Class.forName("org.geysermc.cumulus.response.CustomFormResponse", true, loader);
        Class<?> responseResult = Class.forName("org.geysermc.cumulus.response.result.FormResponseResult", true, loader);
        Class<?> validResult = Class.forName("org.geysermc.cumulus.response.result.ValidFormResponseResult", true, loader);
        formBuilder.getMethod("title", String.class).invoke(builder, title);
        builderType.getMethod("input", String.class, String.class, String.class)
                .invoke(builder, inputLabel, placeholder, initial == null ? "" : initial);
        Method isValid = responseResult.getMethod("isValid");
        Method response = validResult.getMethod("response");
        Method asInput = customResponse.getMethod("asInput", int.class);
        BiConsumer<Object, Object> handler = (form, result) -> {
            String text = null;
            try {
                if (Boolean.TRUE.equals(isValid.invoke(result))) {
                    Object value = asInput.invoke(response.invoke(result), 0);
                    if (value instanceof String s) {
                        text = s;
                    } else {
                        log.warning("ExcelSummons: unparseable Bedrock rename response "
                                + "(asInput returned " + value + "); treating as closed");
                    }
                }
            } catch (Exception e) {
                log.warning("ExcelSummons: could not read the Bedrock rename response: "
                        + e.getClass().getSimpleName() + ": " + e.getMessage());
                text = null;
            }
            onText.accept(text);
        };
        formBuilder.getMethod("resultHandler", BiConsumer.class).invoke(builder, handler);
        return formBuilder.getMethod("build").invoke(builder);
    }
}

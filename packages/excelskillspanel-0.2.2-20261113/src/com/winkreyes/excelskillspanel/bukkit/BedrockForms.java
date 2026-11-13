package com.winkreyes.excelskillspanel.bukkit;

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
 * shape, carried over from ExcelSummons 0.1.8's BedrockForms
 * (itself the fix for the defect below, which is Summons history
 * but dictates this shape — it is kept verbatim as the reason
 * every rule here exists).
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
 * main thread before touching Bukkit state (PanelMenus does).
 */
public final class BedrockForms {
    private BedrockForms() {}

    public static boolean sendSimpleForm(Player player, String title, String body,
                                         List<String> buttons, IntConsumer onButton,
                                         Logger log) {
        try {
            ClassLoader cl = geyserLoader();
            if (cl == null) {
                log.warning("ExcelSkillsPanel: Geyser-Spigot is not enabled; "
                        + "cannot send the Bedrock form (chat fallback applies)");
                return false;
            }
            Class<?> apiClass = Class.forName("org.geysermc.geyser.api.GeyserApi", true, cl);
            Object api = apiClass.getMethod("api").invoke(null);
            Object conn = apiClass.getMethod("connectionByUuid", UUID.class)
                    .invoke(api, player.getUniqueId());
            if (conn == null) {
                log.warning("ExcelSkillsPanel: no Geyser connection for " + player.getName()
                        + "; cannot send the Bedrock form (chat fallback applies)");
                return false;
            }
            Object form = buildForm(cl, title, body, buttons, onButton, log);
            for (Method m : conn.getClass().getMethods()) {
                if (m.getName().equals("sendForm") && m.getParameterCount() == 1
                        && m.getParameterTypes()[0].isAssignableFrom(form.getClass())) {
                    return Boolean.TRUE.equals(m.invoke(conn, form));
                }
            }
            log.warning("ExcelSkillsPanel: no compatible sendForm on the Geyser "
                    + "connection; cannot send the Bedrock form (chat fallback applies)");
            return false;
        } catch (Throwable t) {
            log.warning("ExcelSkillsPanel: Bedrock form send failed, chat fallback "
                    + "applies: " + t.getClass().getSimpleName() + ": " + t.getMessage());
            return false;
        }
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
                        log.warning("ExcelSkillsPanel: unparseable Bedrock form response "
                                + "(clickedButtonId returned " + idValue + "); treating as closed");
                    }
                }
            } catch (Exception e) {
                log.warning("ExcelSkillsPanel: could not read the Bedrock form response: "
                        + e.getClass().getSimpleName() + ": " + e.getMessage());
                clicked = -1;
            }
            onButton.accept(clicked);
        };
        formBuilder.getMethod("resultHandler", BiConsumer.class).invoke(builder, handler);
        return formBuilder.getMethod("build").invoke(builder);
    }
}

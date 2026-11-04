// Excel signature - WRV maintained source; existing authorship notices preserved.
package com.winkreyes.excelmenu.bukkit;

import com.winkreyes.excelmenu.core.platform.Detection;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.UUID;

/**
 * THE platform bridge — the only one (DESIGN-2.0 §2.2). It merges the
 * two reflection bridges the fleet used to carry (ExcelMenu 0.1.8's
 * BedrockMenus and ExcelBedrockMenu's GeyserForms) into a single
 * isolated component behind the tri-state {@link Detection} core:
 *
 * <ul>
 *   <li>Detection reaches {@code GeyserApi.isBedrockPlayer(UUID)}
 *       through Geyser's own classloader — no compile-time Geyser or
 *       Cumulus link anywhere, so nothing breaks when Geyser is
 *       absent or mid-update.</li>
 *   <li>There is deliberately NO second Floodgate-API check: a player
 *       arriving through Geyser IS Bedrock-rendered, and two sources
 *       of truth about one fact is how the old drift started. The
 *       floodgate softdepend stays for identity conventions only.</li>
 *   <li>Every failure — Geyser absent, API moved, exception — becomes
 *       UNKNOWN in the core, and UNKNOWN renders the chest GUI. The
 *       bridge never throws into a menu open.</li>
 *   <li>The boot self-check runs once at enable and logs one loud
 *       line either way, so a broken bridge is a boot fact, not a
 *       first-tap surprise. Geyser updates therefore join the
 *       "rig before live" list with menu opens as a named check.</li>
 * </ul>
 *
 * Phase 1 adds the Cumulus SEND surface to this module (the
 * GeyserForms/BedrockMenus reflection pattern): callers hand in a
 * title, content and button labels and receive selections on the
 * main thread. Session/token discipline (§2.5) belongs to the
 * callers (FormService for registry forms, BedrockMenus for
 * definition forms) — the bridge is transport, never policy.
 */
final class PlatformBridge {

    /** A form's answer channel. Both callbacks arrive on the main
     *  thread; a close/invalid answer is reported as such and is
     *  NEVER converted into a selection (closed = cancelled). */
    interface FormHandler {
        void onSelect(int buttonIndex);
        void onClosedOrInvalid();
    }

    private final JavaPlugin plugin;

    PlatformBridge(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /** The tri-state answer for one connection, asked fresh per open. */
    Detection.State detect(Player player) {
        return Detection.detect(this::probe, player.getUniqueId());
    }

    /** Raw probe: TRUE bedrock, FALSE confirmed Java, null = cannot tell. */
    private Boolean probe(UUID uuid) throws Exception {
        Plugin geyser = geyserPlugin();
        if (geyser == null) return null;
        ClassLoader loader = geyser.getClass().getClassLoader();
        Class<?> apiClass = Class.forName("org.geysermc.geyser.api.GeyserApi", true, loader);
        Object api = apiClass.getMethod("api").invoke(null);
        if (api == null) return null;
        Object result = apiClass.getMethod("isBedrockPlayer", UUID.class).invoke(api, uuid);
        return result instanceof Boolean b ? b : null;
    }

    /**
     * Boot self-check: is the reflective surface (Geyser API + the
     * Cumulus form classes) actually there? Logs the core's verdict
     * line under the ExcelMenu prefix and returns the same fact.
     */
    boolean selfCheck() {
        boolean present = reflectiveSurfacePresent();
        plugin.getLogger().info(Detection.selfCheckLine(present));
        return present;
    }

    boolean reflectiveSurfacePresent() {
        try {
            Plugin geyser = geyserPlugin();
            if (geyser == null) return false;
            ClassLoader loader = geyser.getClass().getClassLoader();
            Class.forName("org.geysermc.geyser.api.GeyserApi", true, loader);
            Class.forName("org.geysermc.cumulus.form.SimpleForm", true, loader);
            return true;
        } catch (Exception | LinkageError e) {
            return false;
        }
    }

    private Plugin geyserPlugin() {
        Plugin geyser = Bukkit.getPluginManager().getPlugin("Geyser-Spigot");
        return geyser != null && geyser.isEnabled() ? geyser : null;
    }

    // ---------- Cumulus send surface (transport only) ----------

    /**
     * Send one Cumulus SimpleForm. Returns true when the form was
     * actually sent; false (with a warning naming the failing step)
     * when the bridge could not send it — callers fall back to the
     * chest GUI, exactly as both old bridges did.
     */
    boolean sendSimpleForm(Player player, String title, String content,
                           List<String> buttons, FormHandler handler) {
        String step = "start";
        try {
            Plugin geyserPlugin = geyserPlugin();
            if (geyserPlugin == null) return false;
            ClassLoader loader = geyserPlugin.getClass().getClassLoader();
            UUID uuid = player.getUniqueId();

            step = "GeyserApi.api()";
            Class<?> apiClass = Class.forName("org.geysermc.geyser.api.GeyserApi", true, loader);
            Object api = apiClass.getMethod("api").invoke(null);
            if (api == null) return false;

            step = "connectionByUuid";
            Object connection = apiClass.getMethod("connectionByUuid", UUID.class).invoke(api, uuid);
            if (connection == null) return false;

            step = "SimpleForm.builder()";
            Class<?> formClass = Class.forName("org.geysermc.cumulus.form.SimpleForm", true, loader);
            Object builder = formClass.getMethod("builder").invoke(null);
            Class<?> builderClass = builder.getClass();

            step = "title/content/buttons";
            builderClass.getMethod("title", String.class).invoke(builder, title);
            builderClass.getMethod("content", String.class).invoke(builder,
                    content == null ? "" : content);
            Method buttonMethod = builderClass.getMethod("button", String.class);
            for (String label : buttons) buttonMethod.invoke(builder, label);

            step = "validResultHandler";
            Class<?> responseClass = Class.forName(
                    "org.geysermc.cumulus.response.SimpleFormResponse", true, loader);
            Class<?> biConsumerClass = Class.forName("java.util.function.BiConsumer", true, loader);
            Object proxy = Proxy.newProxyInstance(loader,
                    new Class<?>[]{biConsumerClass},
                    (p, method, methodArgs) -> {
                        if (method.getName().equals("accept") && methodArgs != null
                                && methodArgs.length == 2 && methodArgs[1] != null) {
                            int id = (int) responseClass.getMethod("clickedButtonId")
                                    .invoke(methodArgs[1]);
                            schedule(() -> handler.onSelect(id));
                        }
                        return null;
                    });
            Method vrh = findHandler(builderClass, "validResultHandler", proxy);
            if (vrh == null) {
                plugin.getLogger().warning("ExcelMenu: no compatible validResultHandler"
                        + " (SimpleForm) — Bedrock forms unavailable this boot.");
                return false;
            }
            vrh.invoke(builder, proxy);
            builderClass.getMethod("closedOrInvalidResultHandler",
                            java.util.function.Consumer.class)
                    .invoke(builder, (java.util.function.Consumer<Object>) ignored ->
                            schedule(handler::onClosedOrInvalid));

            step = "build+send";
            Object form = builderClass.getMethod("build").invoke(builder);
            Method sendForm = findSendForm(connection.getClass(), form.getClass());
            if (sendForm == null) {
                plugin.getLogger().warning("ExcelMenu: no compatible sendForm (SimpleForm)"
                        + " — Bedrock forms unavailable this boot.");
                return false;
            }
            return (boolean) sendForm.invoke(connection, form);
        } catch (Exception e) {
            plugin.getLogger().warning("ExcelMenu form '" + title + "' failed at [" + step
                    + "]: " + e.getClass().getSimpleName() + ": " + e.getMessage());
            return false;
        }
    }

    /** Form callbacks arrive off-thread; menu work belongs on the
     *  main thread, and never after the plugin has disabled. */
    private void schedule(Runnable callback) {
        if (!plugin.isEnabled()) return;
        try {
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (plugin.isEnabled()) callback.run();
            });
        } catch (org.bukkit.plugin.IllegalPluginAccessException stopped) {
            // Disable won the scheduling race; drop the callback.
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

// ExcelMenu — an Excel plugin by WinkReyesValley. (Excel signature — MikeExcel)

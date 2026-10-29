// Excel signature - WRV maintained source; existing authorship notices preserved.
package com.winkreyes.excelmenu.bukkit;

import com.winkreyes.excelmenu.core.BedrockDetail;
import com.winkreyes.excelmenu.core.MenuDef;
import com.winkreyes.excelmenu.core.Selector;
import com.winkreyes.excelmenu.core.Text;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.IntConsumer;

/**
 * The Bedrock door: the same MenuDef rendered as Cumulus forms,
 * reached by reflection through Geyser's classloader — the pattern
 * ExcelBedrockMenu's GeyserForms and ExcelCosmetics' BedrockPicker
 * proved on this stack (no compile-time Geyser link; nothing breaks
 * when Geyser is absent, the chest GUI serves as fallback).
 *
 * One button per visible item, in slot order, labelled with the
 * item's name and lore so prices and descriptions survive the
 * translation. Button CONTENT text (the lore lines) is darkened by
 * Text.bedrockText — Bedrock buttons are light grey, and the pale
 * palette washes out on them (0.1.1, Mike's in-game report); item
 * names keep their authored colors. A button runs the item's LEFT
 * click actions (what a tap means on Java); an item whose left and
 * right actions differ carries a bedrock modal block and opens a
 * detail form whose buttons name the actions (e.g. "Rent 1 week -
 * $200" / "Own forever - $15,000") plus a Back button that returns
 * to the parent menu. An item with a four-label grows a third
 * action button between Own and Back that runs its shift-left
 * lines — the Bedrock mirror of the Java shift-left click (0.1.2;
 * the button/index mapping lives in core.BedrockDetail).
 * (Cumulus ModalForm has exactly two buttons, which is why the
 * 0.1.0 modal could offer no way back — the detail screen is a
 * SimpleForm since 0.1.1.) Items with no actions are display
 * pieces: tapping one re-sends the form instead of dumping the
 * player out.
 */
final class BedrockMenus {

    private final JavaPlugin plugin;
    private final MenuService service;

    BedrockMenus(JavaPlugin plugin, MenuService service) {
        this.plugin = plugin;
        this.service = service;
    }

    /** TRUE = Bedrock, FALSE = confirmed Java (incl. no Geyser). */
    boolean isBedrock(Player player) {
        try {
            Plugin geyser = Bukkit.getPluginManager().getPlugin("Geyser-Spigot");
            if (geyser == null || !geyser.isEnabled()) return false;
            ClassLoader loader = geyser.getClass().getClassLoader();
            Class<?> apiClass = Class.forName("org.geysermc.geyser.api.GeyserApi", true, loader);
            Object api = apiClass.getMethod("api").invoke(null);
            Object result = apiClass.getMethod("isBedrockPlayer", UUID.class)
                    .invoke(api, player.getUniqueId());
            return result instanceof Boolean b && b;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean formsAvailable() {
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

    /** Returns true when a form was actually sent. */
    boolean open(Player player, MenuDef def) {
        if (!formsAvailable()) return false;
        BukkitContext ctx = new BukkitContext(player);
        Map<Integer, MenuDef.ItemDef> visible = Selector.visibleItems(def, ctx);
        List<MenuDef.ItemDef> items = new ArrayList<>(visible.values());
        List<String> labels = new ArrayList<>();
        for (MenuDef.ItemDef item : items) {
            labels.add(label(player, item));
        }
        String title = Text.colorizeBedrock(Placeholders.resolve(player, def.title));
        return sendSimpleForm(player, title, "", labels, idx -> {
            if (idx < 0 || idx >= items.size()) return;
            MenuDef.ItemDef item = items.get(idx);
            if (item.bedrock() != null && "modal".equals(item.bedrock().mode())) {
                sendItemDetail(player, def, item);
                return;
            }
            List<String> lines = firstLines(item, "left", "any", "right");
            if (lines == null) {
                open(player, def); // display-only piece: stay in the menu
                return;
            }
            service.dispatcher().runAll(player, def.id + ":" + item.id(), lines,
                    service.session(player, def));
        });
    }

    /**
     * The item detail screen (a definition's "modal" bedrock block):
     * the item's name and lore as the form content, its action
     * labels as buttons, and Back last — so closing is never the
     * only way out of an item screen. The buttons and the
     * click-index map come from core.BedrockDetail: [left, right]
     * always, the four label (running the shift-left lines) when
     * the item defines one, then Back. A SimpleForm, not a
     * ModalForm: Cumulus modals have exactly two buttons, leaving
     * no room for Back (the 0.1.0 trap Mike hit).
     */
    private void sendItemDetail(Player player, MenuDef def, MenuDef.ItemDef item) {
        List<String> lore = new ArrayList<>();
        for (String line : item.lore()) lore.add(Placeholders.resolve(player, line));
        String content = Text.bedrockText(
                Placeholders.resolve(player, item.name() == null ? item.id() : item.name()),
                lore);
        String title = Text.colorizeBedrock(Placeholders.resolve(player, def.title));
        MenuDef.BedrockDef bedrock = item.bedrock();
        List<String> buttons = new ArrayList<>();
        for (String label : BedrockDetail.buttonLabels(bedrock)) {
            buttons.add(BedrockDetail.BACK_LABEL.equals(label)
                    ? label : Text.colorizeBedrock(label));
        }
        boolean sent = sendSimpleForm(player, title, content, buttons, idx -> {
            if (BedrockDetail.isBack(bedrock, idx)) { // Back: return to the parent menu
                open(player, def);
                return;
            }
            String kind = BedrockDetail.clickKind(bedrock, idx);
            if (kind == null) return;
            List<String> lines = firstLines(item, kind);
            if (lines != null) {
                service.dispatcher().runAll(player, def.id + ":" + item.id(), lines,
                        service.session(player, def));
            } else {
                open(player, def);
            }
        });
        if (!sent) open(player, def);
    }

    private List<String> firstLines(MenuDef.ItemDef item, String... kinds) {
        for (String kind : kinds) {
            List<String> lines = Selector.clickLines(item, kind);
            if (lines != null && !lines.isEmpty()) return lines;
        }
        return null;
    }

    private String label(Player player, MenuDef.ItemDef item) {
        List<String> lore = new ArrayList<>();
        for (String line : item.lore()) lore.add(Placeholders.resolve(player, line));
        return Text.bedrockText(
                Placeholders.resolve(player, item.name() == null ? item.id() : item.name()),
                lore);
    }

    // ---------- Cumulus reflection (GeyserForms pattern) ----------

    private boolean sendSimpleForm(Player player, String title, String content,
                                   List<String> buttons, IntConsumer onSelect) {
        String step = "start";
        try {
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
            for (String label : buttons) buttonMethod.invoke(builder, label);

            step = "validResultHandler";
            Class<?> responseClass = Class.forName(
                    "org.geysermc.cumulus.response.SimpleFormResponse", true, loader);
            Class<?> biConsumerClass = Class.forName("java.util.function.BiConsumer", true, loader);
            Object handler = Proxy.newProxyInstance(loader,
                    new Class<?>[]{biConsumerClass},
                    (proxy, method, methodArgs) -> {
                        if (method.getName().equals("accept") && methodArgs != null
                                && methodArgs.length == 2 && methodArgs[1] != null) {
                            int id = (int) responseClass.getMethod("clickedButtonId")
                                    .invoke(methodArgs[1]);
                            // Form callbacks arrive off-thread; menu
                            // work belongs on the main thread.
                            Bukkit.getScheduler().runTask(plugin, () -> onSelect.accept(id));
                        }
                        return null;
                    });
            Method vrh = findHandler(builderClass, "validResultHandler", handler);
            if (vrh == null) {
                plugin.getLogger().warning("ExcelMenu: no compatible validResultHandler (SimpleForm)");
                return false;
            }
            vrh.invoke(builder, handler);

            step = "build+send";
            Object form = builderClass.getMethod("build").invoke(builder);
            Method sendForm = findSendForm(connection.getClass(), form.getClass());
            if (sendForm == null) {
                plugin.getLogger().warning("ExcelMenu: no compatible sendForm (SimpleForm)");
                return false;
            }
            return (boolean) sendForm.invoke(connection, form);
        } catch (Exception e) {
            plugin.getLogger().warning("ExcelMenu form '" + title + "' failed at [" + step
                    + "]: " + e.getClass().getSimpleName() + ": " + e.getMessage());
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

// ExcelMenu — an Excel plugin by WinkReyesValley. (Excel signature — MikeExcel)

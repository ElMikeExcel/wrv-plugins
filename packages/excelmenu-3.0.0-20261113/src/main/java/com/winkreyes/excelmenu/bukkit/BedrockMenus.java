// Excel signature - WRV maintained source; existing authorship notices preserved.
package com.winkreyes.excelmenu.bukkit;

import com.winkreyes.excelmenu.core.BedrockDetail;
import com.winkreyes.excelmenu.core.MenuDef;
import com.winkreyes.excelmenu.core.Selector;
import com.winkreyes.excelmenu.core.Text;
import com.winkreyes.excelmenu.core.platform.Detection;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.IntConsumer;

/**
 * The Bedrock door for DEFINITION menus: the same MenuDef rendered
 * as Cumulus forms (registry menus have their own form service —
 * FormService — over the same bridge).
 *
 * Phase 1 port note: 0.1.8's copy of this class carried its own
 * Geyser reflection (detection + form sending). Both now go through
 * the unified {@link PlatformBridge} (DESIGN-2.0 §2.2 — one bridge,
 * one answer); what remains here is purely the definition-form
 * policy: one button per visible item in slot order, item detail
 * forms for modal items, and the token discipline (one live form
 * per player, single-use answers, revalidation before dispatch).
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
    private final PlatformBridge bridge;
    // Excel: main-thread, one live form token per player; quit/open replaces it.
    private final java.util.Map<UUID, Object> forms = new java.util.HashMap<>();

    boolean hasLiveForm(Player player) { return forms.containsKey(player.getUniqueId()); }

    void retire() { forms.clear(); }

    void invalidate(Player player) { forms.remove(player.getUniqueId()); }

    BedrockMenus(JavaPlugin plugin, MenuService service, PlatformBridge bridge) {
        this.plugin = plugin;
        this.service = service;
        this.bridge = bridge;
    }

    /** TRUE only on a confirmed Bedrock detection (tri-state core). */
    boolean isBedrock(Player player) {
        return bridge.detect(player) == Detection.State.BEDROCK;
    }

    private boolean formsAvailable() {
        return bridge.reflectiveSurfacePresent();
    }

    /** Returns true when a form was actually sent. */
    boolean open(Player player, MenuDef def) {
        if (!service.canAccess(player, def) || !formsAvailable()) return false;
        BukkitContext ctx = new BukkitContext(player);
        Map<Integer, MenuDef.ItemDef> visible = Selector.visibleItems(def, ctx);
        List<MenuDef.ItemDef> items = new ArrayList<>(visible.values());
        List<String> labels = new ArrayList<>();
        for (MenuDef.ItemDef item : items) {
            labels.add(label(player, item));
        }
        String title = Text.colorizeBedrock(Placeholders.resolve(player, def.title));
        return sendSimpleForm(player, def, title, "", labels, idx -> {
            if (idx < 0 || idx >= items.size()) return;
            MenuDef.ItemDef item = items.get(idx);
            if (!service.canAct(player, def, item)) return;
            if (item.bedrock() != null && "modal".equals(item.bedrock().mode())) {
                sendItemDetail(player, def, item);
                return;
            }
            List<String> lines = firstLines(item, "left", "any", "right");
            if (lines == null) {
                service.openMenu(player, def.id); // display-only piece: stay in the menu
                return;
            }
            StatsSink.tap(player, def.id, item.id(), idx, "bedrock");
            service.dispatcher().runAll(player, def.id + ":" + item.id(), lines,
                    service.session(player, def, true));
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
        boolean sent = sendSimpleForm(player, def, title, content, buttons, idx -> {
            if (BedrockDetail.isBack(bedrock, idx)) { // Back: return to the parent menu
                service.openMenu(player, def.id);
                return;
            }
            if (!service.canAct(player, def, item)) return;
            String kind = BedrockDetail.clickKind(bedrock, idx);
            if (kind == null) return;
            List<String> lines = firstLines(item, kind);
            if (lines != null) {
                StatsSink.tap(player, def.id, item.id(), idx, "bedrock");
                service.dispatcher().runAll(player, def.id + ":" + item.id(), lines,
                        service.session(player, def, true));
            } else {
                service.openMenu(player, def.id);
            }
        });
        if (!sent) service.openMenu(player, def.id);
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

    // ---------- form transport: the unified bridge, our tokens ----------

    private boolean sendSimpleForm(Player player, MenuDef def, String title, String content,
                                   List<String> buttons, IntConsumer onSelect) {
        Object token = new Object();
        java.util.concurrent.atomic.AtomicBoolean used =
                new java.util.concurrent.atomic.AtomicBoolean();
        UUID uuid = player.getUniqueId();
        boolean sent = bridge.sendSimpleForm(player, title, content, buttons,
                new PlatformBridge.FormHandler() {
                    @Override
                    public void onSelect(int id) {
                        if (forms.get(uuid) != token || !used.compareAndSet(false, true)) {
                            return;
                        }
                        forms.remove(uuid);
                        if (service.canAccess(player, def)) onSelect.accept(id);
                    }

                    @Override
                    public void onClosedOrInvalid() {
                        // Closing/invalidating a form also consumes
                        // its callback authority.
                        if (forms.get(uuid) == token) {
                            used.set(true);
                            forms.remove(uuid);
                        }
                    }
                });
        if (sent) forms.put(uuid, token);
        return sent;
    }
}

// ExcelMenu — an Excel plugin by WinkReyesValley. (Excel signature — MikeExcel)

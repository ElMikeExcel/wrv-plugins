// Excel signature - WRV maintained source; existing authorship notices preserved.
package com.winkreyes.excelmenu.bukkit;

import com.winkreyes.excelmenu.core.MenuEditorCore;
import com.winkreyes.excelmenu.core.platform.Platform;
import com.winkreyes.excelmenu.core.registry.PlayerMenuPrefs;
import com.winkreyes.excelmenu.core.render.FormModel;
import com.winkreyes.excelmenu.core.render.FormSession;
import com.winkreyes.excelmenu.core.render.LayoutDef;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * The Bedrock rearrange editor (ExcelMenu 3.0, DESIGN §3.5): forms
 * E1–E5 over the same PlatformBridge, under the same §2.5 session
 * discipline as FormService (single-use tokens bound to player +
 * world + TTL; closed = cancelled; stale answers dropped). Every
 * form ends with a Back button (Mike's standing rule). Saves happen
 * PER MOVE — closing any editor form loses nothing.
 */
final class MenuEditorForms {

    private final JavaPlugin plugin;
    private final PlatformBridge bridge;
    private final PrefsStore prefsStore;
    private final EntryService entries;
    private final FormSession.Store sessions = new FormSession.Store();
    private volatile long ttlMillis = 300_000L;
    private RegistryMenus registryMenus;

    MenuEditorForms(JavaPlugin plugin, PlatformBridge bridge, PrefsStore prefsStore,
                    EntryService entries) {
        this.plugin = plugin;
        this.bridge = bridge;
        this.prefsStore = prefsStore;
        this.entries = entries;
    }

    void bind(RegistryMenus registryMenus) {
        this.registryMenus = registryMenus;
    }

    void setTtlSeconds(long seconds) {
        this.ttlMillis = Math.max(30L, seconds) * 1000L;
    }

    private LayoutDef layout() {
        return registryMenus == null ? null : registryMenus.layout(EditorSupport.MENU);
    }

    private PlayerMenuPrefs prefs(Player player) {
        return prefsStore.get(player.getUniqueId());
    }

    private PlayerMenuPrefs prefsOrEmpty(Player player) {
        PlayerMenuPrefs p = prefs(player);
        return p != null ? p : PlayerMenuPrefs.empty(EditorSupport.MENU);
    }

    // ---------- E1: editor root ----------

    void openRoot(Player player) {
        LayoutDef layout = layout();
        if (layout == null) return;
        PlayerMenuPrefs prefs = prefs(player);
        List<String> groupOrder = EditorSupport.effectiveGroupOrder(entries, layout, prefs);
        StringBuilder body = new StringBuilder(
                "Move the doors in each section into your own order.\n");
        body.append(prefs != null
                ? "You're using your own order."
                : "You're using the default order.");
        List<String> labels = new ArrayList<>();
        List<Runnable> actions = new ArrayList<>();
        for (String g : groupOrder) {
            labels.add(EditorSupport.groupName(layout, g));
            actions.add(() -> openGroup(player, g));
        }
        labels.add("Section Order");
        actions.add(() -> openSectionOrder(player));
        if (prefs != null) {
            labels.add("Reset to Default");
            actions.add(() -> openResetConfirm(player, null));
        }
        labels.add(FormModel.BACK_LABEL);
        actions.add(() -> backToPicker(player));
        send(player, "Rearrange Menu", body.toString(), labels, actions);
    }

    // ---------- E2: one group ----------

    private void openGroup(Player player, String group) {
        LayoutDef layout = layout();
        if (layout == null) return;
        PlayerMenuPrefs prefs = prefs(player);
        List<String> ids = EditorSupport.effectiveGroupIds(
                entries, layout, prefs, group, Platform.BEDROCK);
        StringBuilder body = new StringBuilder();
        List<String> pinNames = new ArrayList<>();
        for (String pin : layout.pinned()) {
            if (group.equals(EditorSupport.def(entries, pin) != null
                    ? EditorSupport.def(entries, pin).group() : null)) {
                pinNames.add(EditorSupport.label(entries, pin));
            }
        }
        if (!pinNames.isEmpty()) {
            body.append("Pinned at the top of the menu: ")
                    .append(String.join(", ", pinNames)).append("\n");
        }
        for (int i = 0; i < ids.size(); i++) {
            body.append(i + 1).append(". ")
                    .append(EditorSupport.label(entries, ids.get(i))).append("\n");
        }
        body.append("Tap a door to move it.");
        List<String> labels = new ArrayList<>();
        List<Runnable> actions = new ArrayList<>();
        for (String id : ids) {
            labels.add(EditorSupport.label(entries, id));
            actions.add(() -> openMove(player, group, id, null));
        }
        if (prefs != null && prefs.hasGroup(group)) {
            labels.add("Reset This Section");
            actions.add(() -> openResetConfirm(player, group));
        }
        labels.add(FormModel.BACK_LABEL);
        actions.add(() -> openRoot(player));
        send(player, "Rearrange — " + EditorSupport.groupName(layout, group),
                body.toString(), labels, actions);
    }

    // ---------- E3: move one entry ----------

    private void openMove(Player player, String group, String id, String note) {
        LayoutDef layout = layout();
        if (layout == null) return;
        List<String> ids = EditorSupport.effectiveGroupIds(
                entries, layout, prefs(player), group, Platform.BEDROCK);
        int idx = ids.indexOf(id);
        if (idx < 0) { openGroup(player, group); return; }
        String label = EditorSupport.label(entries, id);
        String body = label + " is #" + (idx + 1) + " of " + ids.size()
                + " in " + EditorSupport.groupName(layout, group) + "."
                + (note != null ? "\n" + note : "");
        List<String> labels = new ArrayList<>(List.of(
                "Move Up", "Move Down", "Move to Top", "Move to Bottom",
                FormModel.BACK_LABEL));
        List<Runnable> actions = new ArrayList<>();
        actions.add(() -> applyMove(player, group, id,
                MenuEditorCore.moveUp(ids, id)));
        actions.add(() -> applyMove(player, group, id,
                MenuEditorCore.moveDown(ids, id)));
        actions.add(() -> applyMove(player, group, id,
                MenuEditorCore.moveToTop(ids, id)));
        actions.add(() -> applyMove(player, group, id,
                MenuEditorCore.moveToBottom(ids, id)));
        actions.add(() -> openGroup(player, group));
        send(player, label, body, labels, actions);
    }

    private void applyMove(Player player, String group, String id,
                           MenuEditorCore.MoveResult result) {
        if (result.moved()) {
            PlayerMenuPrefs prefs = prefsOrEmpty(player);
            prefsStore.save(player.getUniqueId(), prefs.withGroup(group, result.list()));
        }
        // Re-open E3 with the updated position so repeated Move Up
        // taps chain without re-walking the list (§3.5).
        openMove(player, group, id, result.note());
    }

    // ---------- E4: section order ----------

    private void openSectionOrder(Player player) {
        LayoutDef layout = layout();
        if (layout == null) return;
        List<String> order = EditorSupport.effectiveGroupOrder(entries, layout, prefs(player));
        StringBuilder body = new StringBuilder(
                "This changes the order of sections on the section picker.\n");
        for (int i = 0; i < order.size(); i++) {
            body.append(i + 1).append(". ")
                    .append(EditorSupport.groupName(layout, order.get(i))).append("\n");
        }
        body.append("Tap a section to move it.");
        List<String> labels = new ArrayList<>();
        List<Runnable> actions = new ArrayList<>();
        for (String g : order) {
            labels.add(EditorSupport.groupName(layout, g));
            actions.add(() -> openSectionMove(player, g, null));
        }
        labels.add(FormModel.BACK_LABEL);
        actions.add(() -> openRoot(player));
        send(player, "Section Order", body.toString(), labels, actions);
    }

    private void openSectionMove(Player player, String group, String note) {
        LayoutDef layout = layout();
        if (layout == null) return;
        List<String> order = EditorSupport.effectiveGroupOrder(entries, layout, prefs(player));
        int idx = order.indexOf(group);
        if (idx < 0) { openSectionOrder(player); return; }
        String name = EditorSupport.groupName(layout, group);
        String body = name + " is section #" + (idx + 1) + " of " + order.size() + "."
                + (note != null ? "\n" + note : "");
        List<String> labels = new ArrayList<>(List.of(
                "Move Up", "Move Down", "Move to Top", "Move to Bottom",
                FormModel.BACK_LABEL));
        List<Runnable> actions = new ArrayList<>();
        actions.add(() -> applySectionMove(player, group, MenuEditorCore.moveUp(order, group)));
        actions.add(() -> applySectionMove(player, group, MenuEditorCore.moveDown(order, group)));
        actions.add(() -> applySectionMove(player, group, MenuEditorCore.moveToTop(order, group)));
        actions.add(() -> applySectionMove(player, group, MenuEditorCore.moveToBottom(order, group)));
        actions.add(() -> openSectionOrder(player));
        send(player, name, body, labels, actions);
    }

    private void applySectionMove(Player player, String group,
                                  MenuEditorCore.MoveResult result) {
        if (result.moved()) {
            PlayerMenuPrefs prefs = prefsOrEmpty(player);
            prefsStore.save(player.getUniqueId(), prefs.withGroupOrder(result.list()));
        }
        openSectionMove(player, group, result.note());
    }

    // ---------- E5: reset confirm ----------

    private void openResetConfirm(Player player, String group) {
        String body = group == null
                ? "This removes your own order everywhere and goes back to the"
                        + " default menu order. Your other settings don't change."
                : "This removes your own order for the "
                        + EditorSupport.groupName(layout(), group)
                        + " section only and goes back to the default order there.";
        List<String> labels = new ArrayList<>(List.of("Reset to Default", FormModel.BACK_LABEL));
        List<Runnable> actions = new ArrayList<>();
        actions.add(() -> {
            if (group == null) {
                prefsStore.delete(player.getUniqueId());
            } else {
                PlayerMenuPrefs prefs = prefs(player);
                if (prefs != null) {
                    PlayerMenuPrefs next = prefs.withoutGroup(group);
                    if (next.isEmpty()) prefsStore.delete(player.getUniqueId());
                    else prefsStore.save(player.getUniqueId(), next);
                }
            }
            player.sendMessage("§aYour menu is back to the default order.");
            backToPicker(player);
        });
        actions.add(() -> {
            if (group == null) openRoot(player); else openGroup(player, group);
        });
        send(player, "Reset to Default", body, labels, actions);
    }

    // ---------- plumbing ----------

    private void backToPicker(Player player) {
        if (registryMenus != null) registryMenus.openMenu(player, EditorSupport.MENU);
    }

    private void send(Player player, String title, String content,
                      List<String> labels, List<Runnable> actions) {
        FormSession.Token token = sessions.issue(player.getUniqueId(),
                "rearrange-editor", null, player.getWorld().getName(),
                System.currentTimeMillis(), ttlMillis);
        boolean sent = bridge.sendSimpleForm(player, title, content, labels,
                new PlatformBridge.FormHandler() {
                    @Override
                    public void onSelect(int index) {
                        FormSession.Verdict verdict = sessions.answer(
                                player.getUniqueId(), token.id(),
                                player.getWorld().getName(), System.currentTimeMillis());
                        if (verdict != FormSession.Verdict.OK) return;
                        if (index < 0 || index >= actions.size()) return;
                        actions.get(index).run();
                    }

                    @Override
                    public void onClosedOrInvalid() {
                        sessions.invalidate(player.getUniqueId());
                    }
                });
        if (!sent && registryMenus != null) {
            // Never strand a Bedrock player: fall back to the menu.
            registryMenus.openMenu(player, EditorSupport.MENU);
        }
    }

    void invalidate(UUID uuid) {
        sessions.invalidate(uuid);
    }
}

// ExcelMenu — an Excel plugin by WinkReyesValley. (Excel signature — MikeExcel)

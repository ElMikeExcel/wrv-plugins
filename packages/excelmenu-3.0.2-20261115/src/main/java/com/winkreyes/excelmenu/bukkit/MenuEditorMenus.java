// Excel signature - WRV maintained source; existing authorship notices preserved.
package com.winkreyes.excelmenu.bukkit;

import com.winkreyes.excelmenu.core.MenuEditorCore;
import com.winkreyes.excelmenu.core.Text;
import com.winkreyes.excelmenu.core.platform.Platform;
import com.winkreyes.excelmenu.core.registry.EntryDef;
import com.winkreyes.excelmenu.core.registry.PlayerMenuPrefs;
import com.winkreyes.excelmenu.core.render.LayoutDef;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The Java rearrange editor (ExcelMenu 3.0, DESIGN §3.6): chest
 * screens J1–J4 under a MenuEditorHolder. Every click is cancelled;
 * no item ever leaves an editor inventory. Selection uses the
 * select-and-insert mechanic — the same algebra as the Bedrock
 * editor's Up/Down buttons (MenuEditorCore). Saves happen per
 * completed move; closing at any point loses nothing. Every screen
 * carries a Back item (Mike's standing rule).
 */
final class MenuEditorMenus implements Listener {

    /** Loud cap from DESIGN §3.6: far above any live group. */
    static final int MAX_MOVABLE_PER_GROUP = 36;

    private final JavaPlugin plugin;
    private final PrefsStore prefsStore;
    private final EntryService entries;
    private RegistryMenus registryMenus;

    MenuEditorMenus(JavaPlugin plugin, PrefsStore prefsStore, EntryService entries) {
        this.plugin = plugin;
        this.prefsStore = prefsStore;
        this.entries = entries;
    }

    void bind(RegistryMenus registryMenus) {
        this.registryMenus = registryMenus;
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

    // ---------- J1: root ----------

    void openRoot(Player player) {
        LayoutDef layout = layout();
        if (layout == null) return;
        PlayerMenuPrefs prefs = prefs(player);
        List<String> groups = EditorSupport.effectiveGroupOrder(entries, layout, prefs);
        Map<Integer, MenuEditorHolder.Action> actions = new LinkedHashMap<>();
        MenuEditorHolder holder = new MenuEditorHolder(player.getUniqueId(),
                MenuEditorHolder.Screen.ROOT, null, actions);
        Inventory inv = create(player, holder, 27, "Rearrange Menu");
        int slot = 0;
        for (String g : groups) {
            if (slot > 8) break;
            inv.setItem(slot, groupItem(player, layout, g, List.of(
                    "§7Click to rearrange this section",
                    prefs != null ? "§7You're using your own order."
                            : "§7You're using the default order.")));
            actions.put(slot, MenuEditorHolder.Action.group(g));
            slot++;
        }
        inv.setItem(11, simple(Material.COMPASS, "&b&lSection Order",
                List.of("§7Order of sections on the section picker")));
        actions.put(11, MenuEditorHolder.Action.of(
                MenuEditorHolder.Action.Kind.SECTION_ORDER));
        boolean has = prefs != null;
        inv.setItem(13, simple(Material.BARRIER,
                has ? "&c&lReset to Default" : "&7&lReset to Default",
                has ? List.of("§7First click arms; second click resets")
                        : List.of("§7You're already on the default order.")));
        if (has) {
            actions.put(13, MenuEditorHolder.Action.of(
                    MenuEditorHolder.Action.Kind.RESET_GLOBAL));
        }
        inv.setItem(22, simple(Material.ARROW, "&7&lBack to Server Menu", List.of()));
        actions.put(22, MenuEditorHolder.Action.of(MenuEditorHolder.Action.Kind.BACK));
        player.openInventory(inv);
    }

    // ---------- J2: group editor ----------

    private void openGroup(Player player, String group) {
        openGroup(player, group, null);
    }

    private void openGroup(Player player, String group, String selectedId) {
        LayoutDef layout = layout();
        if (layout == null) return;
        PlayerMenuPrefs prefs = prefs(player);
        List<String> ids = EditorSupport.effectiveGroupIds(
                entries, layout, prefs, group, Platform.JAVA);
        if (ids.size() > MAX_MOVABLE_PER_GROUP) {
            plugin.getLogger().severe("ExcelMenu rearrange editor: group '" + group
                    + "' has " + ids.size() + " movable entries, over the editor cap of "
                    + MAX_MOVABLE_PER_GROUP + " — refusing to truncate; pagination is owed.");
            player.sendMessage("§cThat section is too large to rearrange right now.");
            return;
        }
        Map<Integer, MenuEditorHolder.Action> actions = new LinkedHashMap<>();
        MenuEditorHolder holder = new MenuEditorHolder(player.getUniqueId(),
                MenuEditorHolder.Screen.GROUP, group, actions);
        holder.selectedId(selectedId);
        Inventory inv = create(player, holder, 54,
                "Rearrange — " + EditorSupport.groupName(layout, group));
        // Pinned members render as a display line in the lore of the
        // header note slot row: a single note item at slot 44 when
        // the group has pins, explaining their absence (§3.5's E2
        // header line, chest-shaped).
        int slot = 0;
        for (String id : ids) {
            EntryDef def = EditorSupport.def(entries, id);
            List<String> lore = new ArrayList<>();
            if (def != null) {
                for (String line : def.lore()) lore.add(line);
            }
            if (id.equals(selectedId)) lore.add("§eSelected — click where it should go");
            else if (selectedId != null) lore.add("§7Click to move " + EditorSupport.label(entries, selectedId) + " here");
            else lore.add("§7Click to select");
            inv.setItem(slot, entryItem(player, def, id, lore));
            actions.put(slot, MenuEditorHolder.Action.entry(id));
            slot++;
        }
        List<String> pins = new ArrayList<>();
        for (String pin : layout.pinned()) {
            EntryDef d = EditorSupport.def(entries, pin);
            if (d != null && group.equals(d.group())) pins.add(EditorSupport.label(entries, pin));
        }
        if (!pins.isEmpty()) {
            inv.setItem(44, simple(Material.PAPER, "&b&lPinned",
                    List.of("§7Pinned at the top of the menu: " + String.join(", ", pins))));
        }
        inv.setItem(45, simple(Material.CHEST, "&b&lMove to Top",
                List.of("§7Moves the selected door")));
        actions.put(45, MenuEditorHolder.Action.of(MenuEditorHolder.Action.Kind.MOVE_TOP));
        inv.setItem(46, simple(Material.CHEST, "&b&lMove to Bottom",
                List.of("§7Moves the selected door")));
        actions.put(46, MenuEditorHolder.Action.of(MenuEditorHolder.Action.Kind.MOVE_BOTTOM));
        if (prefs != null && prefs.hasGroup(group)) {
            inv.setItem(48, simple(Material.BARRIER, "&c&lReset This Section",
                    List.of("§7First click arms; second click resets this section")));
            actions.put(48, MenuEditorHolder.Action.of(
                    MenuEditorHolder.Action.Kind.RESET_GROUP));
        }
        inv.setItem(53, simple(Material.ARROW, "&7&lBack", List.of()));
        actions.put(53, MenuEditorHolder.Action.of(MenuEditorHolder.Action.Kind.BACK));
        player.openInventory(inv);
    }

    // ---------- J3: section order ----------

    private void openSectionOrder(Player player) {
        openSectionOrder(player, null);
    }

    private void openSectionOrder(Player player, String selectedId) {
        LayoutDef layout = layout();
        if (layout == null) return;
        List<String> order = EditorSupport.effectiveGroupOrder(entries, layout, prefs(player));
        Map<Integer, MenuEditorHolder.Action> actions = new LinkedHashMap<>();
        MenuEditorHolder holder = new MenuEditorHolder(player.getUniqueId(),
                MenuEditorHolder.Screen.SECTION_ORDER, null, actions);
        holder.selectedId(selectedId);
        Inventory inv = create(player, holder, 27, "Section Order");
        int slot = 0;
        for (String g : order) {
            List<String> lore = new ArrayList<>(List.of(
                    "§7This order applies to the section picker."));
            if (g.equals(selectedId)) lore.add("§eSelected — click where it should go");
            else if (selectedId != null) lore.add("§7Click to move the selected section here");
            else lore.add("§7Click to select");
            inv.setItem(slot, groupItem(player, layout, g, lore));
            actions.put(slot, MenuEditorHolder.Action.entry(g));
            slot++;
        }
        inv.setItem(22, simple(Material.ARROW, "&7&lBack", List.of()));
        actions.put(22, MenuEditorHolder.Action.of(MenuEditorHolder.Action.Kind.BACK));
        player.openInventory(inv);
    }

    // ---------- clicks ----------

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof MenuEditorHolder holder)) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getClickedInventory() != event.getInventory()) return;
        if (!holder.player().equals(player.getUniqueId())) return;
        MenuEditorHolder.Action action = holder.at(event.getSlot());
        if (action == null) {
            holder.resetArmed(false); // any other click disarms (J4)
            return;
        }
        LayoutDef layout = layout();
        if (layout == null) return;
        switch (holder.screen()) {
            case ROOT -> clickRoot(player, holder, action);
            case GROUP -> clickGroup(player, holder, action);
            case SECTION_ORDER -> clickSectionOrder(player, holder, action);
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof MenuEditorHolder) {
            event.setCancelled(true);
        }
    }

    private void clickRoot(Player player, MenuEditorHolder holder,
                           MenuEditorHolder.Action action) {
        switch (action.kind()) {
            case GROUP -> openGroup(player, action.id());
            case SECTION_ORDER -> openSectionOrder(player);
            case RESET_GLOBAL -> {
                if (!holder.resetArmed()) {
                    holder.resetArmed(true);
                    // Re-render armed state in place.
                    event13Armed(player, holder);
                } else {
                    prefsStore.delete(player.getUniqueId());
                    player.sendMessage("§aYour menu is back to the default order.");
                    if (registryMenus != null) registryMenus.openMenu(player, EditorSupport.MENU);
                }
            }
            case BACK -> {
                if (registryMenus != null) registryMenus.openMenu(player, EditorSupport.MENU);
            }
            default -> { }
        }
    }

    private void event13Armed(Player player, MenuEditorHolder holder) {
        Inventory inv = holder.getInventory();
        if (inv != null) {
            inv.setItem(13, simple(Material.BARRIER, "&c&lClick again to confirm reset",
                    List.of("§7This removes your own order everywhere.")));
        }
    }

    private void clickGroup(Player player, MenuEditorHolder holder,
                            MenuEditorHolder.Action action) {
        String group = holder.group();
        switch (action.kind()) {
            case ENTRY -> {
                String clicked = action.id();
                String selected = holder.selectedId();
                if (selected == null) {
                    openGroup(player, group, clicked);
                } else if (selected.equals(clicked)) {
                    openGroup(player, group, null); // deselect
                } else {
                    List<String> ids = EditorSupport.effectiveGroupIds(
                            entries, layout(), prefs(player), group, Platform.JAVA);
                    MenuEditorCore.MoveResult r =
                            MenuEditorCore.insertBefore(ids, selected, clicked);
                    if (r.moved()) {
                        prefsStore.save(player.getUniqueId(),
                                prefsOrEmpty(player).withGroup(group, r.list()));
                    }
                    openGroup(player, group, null);
                }
            }
            case MOVE_TOP, MOVE_BOTTOM -> {
                String selected = holder.selectedId();
                if (selected == null) { openGroup(player, group, null); return; }
                List<String> ids = EditorSupport.effectiveGroupIds(
                        entries, layout(), prefs(player), group, Platform.JAVA);
                MenuEditorCore.MoveResult r = action.kind() == MenuEditorHolder.Action.Kind.MOVE_TOP
                        ? MenuEditorCore.moveToTop(ids, selected)
                        : MenuEditorCore.moveToBottom(ids, selected);
                if (r.moved()) {
                    prefsStore.save(player.getUniqueId(),
                            prefsOrEmpty(player).withGroup(group, r.list()));
                } else if (r.note() != null) {
                    player.sendMessage("§e" + r.note());
                }
                openGroup(player, group, null);
            }
            case RESET_GROUP -> {
                if (!holder.resetArmed()) {
                    holder.resetArmed(true);
                    Inventory inv = holder.getInventory();
                    if (inv != null) {
                        inv.setItem(48, simple(Material.BARRIER,
                                "&c&lClick again to confirm reset",
                                List.of("§7Only this section goes back to default.")));
                    }
                } else {
                    PlayerMenuPrefs prefs = prefs(player);
                    if (prefs != null) {
                        PlayerMenuPrefs next = prefs.withoutGroup(group);
                        if (next.isEmpty()) prefsStore.delete(player.getUniqueId());
                        else prefsStore.save(player.getUniqueId(), next);
                    }
                    player.sendMessage("§aThat section is back to the default order.");
                    openRoot(player);
                }
            }
            case BACK -> openRoot(player);
            default -> { }
        }
    }

    private void clickSectionOrder(Player player, MenuEditorHolder holder,
                                   MenuEditorHolder.Action action) {
        switch (action.kind()) {
            case ENTRY -> {
                String clicked = action.id();
                String selected = holder.selectedId();
                if (selected == null) {
                    openSectionOrder(player, clicked);
                } else if (selected.equals(clicked)) {
                    openSectionOrder(player, null);
                } else {
                    List<String> order = EditorSupport.effectiveGroupOrder(
                            entries, layout(), prefs(player));
                    MenuEditorCore.MoveResult r =
                            MenuEditorCore.insertBefore(order, selected, clicked);
                    if (r.moved()) {
                        prefsStore.save(player.getUniqueId(),
                                prefsOrEmpty(player).withGroupOrder(r.list()));
                    }
                    openSectionOrder(player, null);
                }
            }
            case BACK -> openRoot(player);
            default -> { }
        }
    }

    // ---------- item building ----------

    private Inventory create(Player player, MenuEditorHolder holder, int size, String title) {
        Component t = LegacyComponentSerializer.legacySection()
                .deserialize(Text.colorize(Placeholders.resolve(player, title)));
        Inventory inv = Bukkit.createInventory(holder, size, t);
        holder.bind(inv);
        return inv;
    }

    private ItemStack groupItem(Player player, LayoutDef layout, String group,
                                List<String> lore) {
        // The layout's own header pane for the group when it has one
        // (read from the layout's fixed items, not re-typed, §3.6).
        String material = "PAPER";
        String name = "&f&l" + EditorSupport.groupName(layout, group);
        int pageIdx = layout.pageOfGroup(group);
        if (pageIdx >= 0) {
            // Header panes sit at the first slot of each row in the
            // bundled layout idiom; find the fixed item whose slot
            // precedes the group's first slot on its page.
            List<Integer> slots = layout.pages().get(pageIdx).slots().get(group);
            if (slots != null && !slots.isEmpty()) {
                int first = slots.stream().mapToInt(Integer::intValue).min().orElse(-1);
                for (LayoutDef.FixedItem f : layout.pages().get(pageIdx).fixed()) {
                    if (f.slot() < first && (material.equals("PAPER") || f.slot() >= 0)) {
                        material = f.material();
                        if (f.name() != null) name = f.name();
                    }
                }
            }
        }
        return simple(materialOf(material), name, lore);
    }

    private ItemStack entryItem(Player player, EntryDef def, String id, List<String> lore) {
        Material material = def != null ? materialOf(def.icon()) : Material.PAPER;
        String name = def != null ? def.label() : id;
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.displayName(LegacyComponentSerializer.legacySection().deserialize(
                    Text.colorize(Placeholders.resolve(player, name))));
            List<Component> lines = new ArrayList<>();
            for (String line : lore) {
                lines.add(LegacyComponentSerializer.legacySection().deserialize(
                        Text.colorize(Placeholders.resolve(player, line))));
            }
            meta.lore(lines);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private ItemStack simple(Material material, String name, List<String> lore) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.displayName(LegacyComponentSerializer.legacySection()
                    .deserialize(Text.colorize(name)));
            if (!lore.isEmpty()) {
                List<Component> lines = new ArrayList<>();
                for (String line : lore) {
                    lines.add(LegacyComponentSerializer.legacySection()
                            .deserialize(Text.colorize(line)));
                }
                meta.lore(lines);
            }
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private Material materialOf(String name) {
        try {
            return Material.valueOf(name);
        } catch (IllegalArgumentException | NullPointerException e) {
            return Material.PAPER;
        }
    }
}

// ExcelMenu — an Excel plugin by WinkReyesValley. (Excel signature — MikeExcel)

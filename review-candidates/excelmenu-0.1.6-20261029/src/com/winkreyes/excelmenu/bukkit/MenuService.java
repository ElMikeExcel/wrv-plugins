// Excel signature - WRV maintained source; existing authorship notices preserved.
package com.winkreyes.excelmenu.bukkit;

import com.winkreyes.excelmenu.core.DefinitionLoader;
import com.winkreyes.excelmenu.core.MenuDef;
import com.winkreyes.excelmenu.core.MenuValidator;
import com.winkreyes.excelmenu.core.RequirementEngine;
import com.winkreyes.excelmenu.core.Selector;
import com.winkreyes.excelmenu.core.Text;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Stream;

/**
 * The menu registry and renderer. Loading runs the strict loader
 * plus the semantic validator; a definition that fails either is
 * refused LOUDLY (one SEVERE per problem, naming menu and item)
 * while the rest of the set still serves. That asymmetry is the
 * whole reason ExcelMenu exists: under DeluxeMenus the same class
 * of failure was silent and a store simply never opened.
 */
final class MenuService {

    /** loaded: menu id -> definition. failed: file stem -> errors. */
    record LoadOutcome(Map<String, MenuDef> loaded, Map<String, List<String>> failed) {}

    private final JavaPlugin plugin;
    private final ActionDispatcher dispatcher;
    private final BedrockMenus bedrock;
    private volatile Map<String, MenuDef> menus = Map.of();

    MenuService(JavaPlugin plugin) {
        this.plugin = plugin;
        this.dispatcher = new ActionDispatcher(plugin, this);
        this.bedrock = new BedrockMenus(plugin, this);
    }

    ActionDispatcher dispatcher() { return dispatcher; }

    Map<String, MenuDef> menus() { return menus; }

    MenuDef menu(String id) { return menus.get(id); }

    // ---------- loading ----------

    LoadOutcome loadAll(Path dir) {
        Map<String, List<String>> failed = new TreeMap<>();
        List<MenuDef> defs = new ArrayList<>();
        Set<String> materials = new HashSet<>();
        for (Material m : Material.values()) materials.add(m.name());

        List<Path> files = new ArrayList<>();
        try (Stream<Path> s = Files.list(dir)) {
            s.filter(p -> p.getFileName().toString().endsWith(".yml")).sorted().forEach(files::add);
        } catch (IOException e) {
            plugin.getLogger().severe("ExcelMenu: cannot list menus folder " + dir
                    + " — " + e.getMessage());
            return new LoadOutcome(Map.of(), Map.of("<folder>", List.of(e.toString())));
        }
        for (Path file : files) {
            DefinitionLoader.Result r = DefinitionLoader.load(file);
            if (r.ok()) {
                defs.add(r.def());
            } else {
                String stem = file.getFileName().toString().replaceFirst("\\.yml$", "");
                failed.put(stem, r.errors());
            }
        }
        // Semantic validation, cascading: a refused menu can make a
        // linking menu invalid (its target vanished), so re-validate
        // after each removal until the surviving set is clean. Every
        // removal is recorded with its reasons.
        for (int pass = 0; pass < 8; pass++) {
            List<String> errors = MenuValidator.validate(defs, materials);
            if (errors.isEmpty()) break;
            Set<String> badIds = new HashSet<>();
            Map<String, List<String>> byMenu = new HashMap<>();
            for (String err : errors) {
                String id = err.contains(":") ? err.substring(0, err.indexOf(':')) : err;
                if (id.startsWith("duplicate menu id")) {
                    int a = err.indexOf('\'');
                    int b = err.indexOf('\'', a + 1);
                    id = err.substring(a + 1, b);
                }
                badIds.add(id);
                byMenu.computeIfAbsent(id, k -> new ArrayList<>()).add(err);
            }
            List<MenuDef> survivors = new ArrayList<>();
            boolean removed = false;
            for (MenuDef d : defs) {
                if (badIds.contains(d.id)) {
                    failed.put(d.id, byMenu.getOrDefault(d.id, List.of("validation failed")));
                    removed = true;
                } else {
                    survivors.add(d);
                }
            }
            defs = survivors;
            if (!removed) break;
        }

        Map<String, MenuDef> loaded = new LinkedHashMap<>();
        for (MenuDef d : defs) loaded.put(d.id, d);
        return new LoadOutcome(loaded, failed);
    }

    /** Swap in a freshly loaded set and report the self-check. */
    void installSet(LoadOutcome outcome) {
        this.menus = outcome.loaded();
        for (Map.Entry<String, List<String>> e : outcome.failed().entrySet()) {
            for (String err : e.getValue()) {
                plugin.getLogger().severe("ExcelMenu definition FAILED [" + e.getKey()
                        + "]: " + err);
            }
        }
        List<String> names = new ArrayList<>(outcome.loaded().keySet());
        names.sort(String::compareTo);
        plugin.getLogger().info("ExcelMenu self-check: loaded " + names.size()
                + " menus: " + String.join(", ", names));
        if (!outcome.failed().isEmpty()) {
            plugin.getLogger().severe("ExcelMenu self-check: " + outcome.failed().size()
                    + " definition file(s) FAILED: "
                    + String.join(", ", outcome.failed().keySet())
                    + " — those menus are CLOSED until fixed.");
        }
    }

    // ---------- opening ----------

    boolean openMenu(Player player, String menuId) {
        MenuDef def = menus.get(menuId);
        if (def == null) {
            plugin.getLogger().warning("ExcelMenu: open requested for unknown menu '"
                    + menuId + "' by " + player.getName());
            player.sendMessage(LegacyComponentSerializer.legacySection()
                    .deserialize("§cThat menu is unavailable right now."));
            return false;
        }
        BukkitContext ctx = new BukkitContext(player);
        if (!RequirementEngine.allPass(def.openRequirements, ctx)) {
            dispatcher.runAll(player, def.id + " open-requirement", def.openDeny,
                    session(player, def));
            return false;
        }
        // bedrock-render: chest (0.1.3) skips the form for this
        // menu: the Bedrock player gets the chest GUI, which Geyser
        // translates with real item icons. Every other menu keeps
        // its form. If the form send fails the chest GUI is the
        // fallback, as before.
        if (!def.bedrockChest() && bedrock.isBedrock(player) && bedrock.open(player, def)) {
            return true;
        }
        player.openInventory(renderJava(player, def));
        return true;
    }

    ActionDispatcher.Session session(Player player, MenuDef def) {
        // The session must mirror how THIS menu was actually
        // rendered for this player: a form session closes nothing
        // and refreshes by re-sending the form; an inventory
        // session (Java, or Bedrock on a bedrock-render: chest
        // menu) closes and refreshes the inventory.
        boolean formRendered = !def.bedrockChest() && bedrock.isBedrock(player);
        return new ActionDispatcher.Session() {
            @Override
            public void close(Player p) {
                if (!formRendered) p.closeInventory();
            }

            @Override
            public void refresh(Player p) {
                if (formRendered) {
                    bedrock.open(p, def);
                } else {
                    InventoryHolder holder = p.getOpenInventory().getTopInventory().getHolder();
                    if (holder instanceof MenuHolder mh && mh.menuId().equals(def.id)) {
                        fillInventory(p, def, mh.getInventory(), mh, false);
                    }
                }
            }
        };
    }

    // ---------- Java purchase confirm (0.1.4) ----------

    /** Opens the Confirm/Cancel chest for a spending click; runs nothing. */
    void openConfirm(Player player, MenuDef def, MenuDef.ItemDef item, String kind,
                     List<String> lines) {
        ConfirmHolder holder = new ConfirmHolder(player.getUniqueId(), def.id, item.id(),
                lines, System.currentTimeMillis());
        String action = com.winkreyes.excelmenu.core.JavaConfirm.actionLabel(item, kind);
        Component title = LegacyComponentSerializer.legacySection()
                .deserialize("\u00a72\u00a7lConfirm purchase?");
        Inventory inv = Bukkit.createInventory(holder, ConfirmHolder.SIZE, title);
        holder.bind(inv);
        ItemStack preview = buildStack(player, item);
        ItemMeta pm = preview.getItemMeta();
        if (pm != null) {
            List<Component> lore = pm.lore() == null ? new ArrayList<>() : new ArrayList<>(pm.lore());
            lore.add(Component.empty());
            lore.add(legacy(action == null ? "\u00a7eYou picked this item." : "\u00a7eYou picked: \u00a7f" + Text.colorize(action)));
            lore.add(legacy("\u00a77Nothing is charged until you click Confirm."));
            pm.lore(lore);
            preview.setItemMeta(pm);
        }
        inv.setItem(ConfirmHolder.SLOT_PREVIEW, preview);
        inv.setItem(ConfirmHolder.SLOT_CONFIRM, button(Material.LIME_CONCRETE,
                "\u00a7a\u00a7l" + Text.colorize(com.winkreyes.excelmenu.core.JavaConfirm.confirmLabel(item, kind)),
                "\u00a77This button spends money.", "\u00a77Balance: \u00a7f" + Placeholders.resolve(player, "$%vault_eco_balance_2dp%")));
        inv.setItem(ConfirmHolder.SLOT_CANCEL, button(Material.RED_CONCRETE,
                "\u00a7c\u00a7l" + com.winkreyes.excelmenu.core.JavaConfirm.CANCEL_LABEL,
                "\u00a77Back to the menu. Nothing is charged."));
        player.openInventory(inv);
    }

    /** Confirm clicked: return to the parent menu, then run the original click lines once. */
    void confirmed(Player player, MenuDef def, String itemId, List<String> lines) {
        openMenu(player, def.id);
        dispatcher.runAll(player, def.id + ":" + itemId, lines, session(player, def));
    }

    private static Component legacy(String s) {
        return LegacyComponentSerializer.legacySection().deserialize(s);
    }

    private static ItemStack button(Material material, String name, String... lore) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.displayName(legacy(name));
            List<Component> l = new ArrayList<>();
            for (String line : lore) l.add(legacy(line));
            meta.lore(l);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    // ---------- Java chest rendering ----------

    Inventory renderJava(Player player, MenuDef def) {
        BukkitContext ctx = new BukkitContext(player);
        Map<Integer, MenuDef.ItemDef> visible = Selector.visibleItems(def, ctx);
        Map<Integer, String> slotItems = new LinkedHashMap<>();
        for (Map.Entry<Integer, MenuDef.ItemDef> e : visible.entrySet()) {
            slotItems.put(e.getKey(), e.getValue().id());
        }
        MenuHolder holder = new MenuHolder(def.id, slotItems);
        Component title = LegacyComponentSerializer.legacySection()
                .deserialize(Text.colorize(Placeholders.resolve(player, def.title)));
        Inventory inv;
        if ("dispenser".equals(def.type)) {
            inv = Bukkit.createInventory(holder,
                    org.bukkit.event.inventory.InventoryType.DISPENSER, title);
        } else {
            inv = Bukkit.createInventory(holder, def.size, title);
        }
        holder.bind(inv);
        holder.markRendered();
        for (Map.Entry<Integer, MenuDef.ItemDef> e : visible.entrySet()) {
            inv.setItem(e.getKey(), buildStack(player, e.getValue()));
        }
        return inv;
    }

    /** Re-fill an open view (refresh action / placeholder tick). */
    void fillInventory(Player player, MenuDef def, Inventory inv, MenuHolder holder,
                       boolean onlyUpdates) {
        BukkitContext ctx = new BukkitContext(player);
        Map<Integer, MenuDef.ItemDef> visible = Selector.visibleItems(def, ctx);
        for (Map.Entry<Integer, MenuDef.ItemDef> e : visible.entrySet()) {
            if (onlyUpdates && !e.getValue().update()) continue;
            inv.setItem(e.getKey(), buildStack(player, e.getValue()));
            if (onlyUpdates) holder.replaceSlotItem(e.getKey(), e.getValue().id());
        }
        if (!onlyUpdates) {
            for (int slot = 0; slot < inv.getSize(); slot++) {
                if (!visible.containsKey(slot)) inv.setItem(slot, null);
            }
            holder.replaceSlotItems(visible);
        }
        holder.markRendered();
    }

    ItemStack buildStack(Player player, MenuDef.ItemDef item) {
        ItemStack stack = new ItemStack(Material.valueOf(item.material()),
                Math.max(1, item.amount()));
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            if (item.name() != null) {
                meta.displayName(LegacyComponentSerializer.legacySection().deserialize(
                        Text.colorize(Placeholders.resolve(player, item.name()))));
            }
            if (!item.lore().isEmpty()) {
                List<Component> lore = new ArrayList<>();
                for (String line : item.lore()) {
                    lore.add(LegacyComponentSerializer.legacySection().deserialize(
                            Text.colorize(Placeholders.resolve(player, line))));
                }
                meta.lore(lore);
            }
            if (item.data() != null && meta instanceof Damageable dmg) {
                dmg.setDamage(item.data());
            }
            stack.setItemMeta(meta);
        }
        return stack;
    }
}

// ExcelMenu — an Excel plugin by WinkReyesValley. (Excel signature — MikeExcel)

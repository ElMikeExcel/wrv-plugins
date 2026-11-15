// Excel signature - WRV maintained source; existing authorship notices preserved.
package com.winkreyes.excelmenu.bukkit;

import com.winkreyes.excelmenu.core.Text;
import com.winkreyes.excelmenu.core.registry.PlayerMenuPrefs;
import com.winkreyes.excelmenu.core.registry.ResolvedEntry;
import com.winkreyes.excelmenu.core.render.ChestModel;
import com.winkreyes.excelmenu.core.render.LayoutDef;
import com.winkreyes.excelmenu.core.render.LayoutLoader;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The registry menus' Bukkit home: loads the layout definitions,
 * resolves entries per viewer per open, and renders them — chest
 * GUI for Java (and as the universal fallback), Cumulus form for
 * Bedrock through FormService.
 *
 * A menu id with a layout is REGISTRY-OWNED: the definition loader
 * skips a same-named definition file loudly, and every open route
 * (/dm, [openguimenu], per-menu commands) lands here. Resolution is
 * per open, never cached (DESIGN-2.0 §2.2) — the click path
 * re-resolves before dispatching, so a door whose state changed
 * between render and tap dispatches its CURRENT action.
 */
final class RegistryMenus {

    private final JavaPlugin plugin;
    private final PlatformBridge bridge;
    private final EntryService entries;
    private final ActionDispatcher dispatcher;
    private final FormService forms;
    private volatile Map<String, LayoutDef> layouts = Map.of();
    private volatile Map<String, List<String>> lastFailures = Map.of();
    private final Map<UUID, Long> generations = new ConcurrentHashMap<>();
    /** 3.0 rearrange prefs (set by ExcelMenu). */
    private PrefsStore prefsStore;

    void setPrefsStore(PrefsStore prefsStore) {
        this.prefsStore = prefsStore;
    }

    /** Per-player resolution WITH rearrange prefs applied (Java
     *  takes the within-group order; §3.3). */
    List<ResolvedEntry> resolvedFor(Player player, String menuId) {
        PlayerMenuPrefs prefs = prefsStore != null
                ? prefsStore.get(player.getUniqueId()) : null;
        return entries.entriesFor(menuId, new BukkitViewerContext(player, bridge), prefs);
    }

    RegistryMenus(JavaPlugin plugin, PlatformBridge bridge, EntryService entries,
                  ActionDispatcher dispatcher, FormService forms) {
        this.plugin = plugin;
        this.bridge = bridge;
        this.entries = entries;
        this.dispatcher = dispatcher;
        this.forms = forms;
    }

    // ---------- layouts ----------

    boolean handles(String menuId) {
        return layouts.containsKey(menuId);
    }

    Set<String> layoutIds() {
        return layouts.keySet();
    }

    Map<String, List<String>> lastFailures() {
        return lastFailures;
    }

    LayoutDef layout(String menuId) {
        return layouts.get(menuId);
    }

    /** Load (or reload) the layouts folder. A reload that would
     *  empty a non-empty layout set is rejected, previous set live. */
    synchronized void reloadLayouts(CommandSender sender) {
        File dir = new File(plugin.getDataFolder(), "layouts");
        Map<String, LayoutDef> loaded = new LinkedHashMap<>();
        Map<String, List<String>> failed = new TreeMap<>();
        if (dir.isDirectory()) {
            File[] files = dir.listFiles((d, name) -> name.endsWith(".yml"));
            if (files != null) {
                java.util.Arrays.sort(files);
                for (File f : files) {
                    LayoutLoader.Result r = LayoutLoader.load(f.toPath());
                    if (r.ok()) {
                        loaded.put(r.def().id(), r.def());
                    } else {
                        String stem = f.getName().replaceFirst("\\.yml$", "");
                        failed.put(stem, r.errors());
                    }
                }
            }
        }
        if (loaded.isEmpty() && !layouts.isEmpty()) {
            plugin.getLogger().severe("ExcelMenu layout reload REJECTED: the new set"
                    + " loaded zero layouts; the previous " + layouts.size() + " stay live.");
            if (sender != null) {
                sender.sendMessage("§cExcelMenu layout reload rejected — new set is empty;"
                        + " previous layouts stay live. See console.");
            }
            lastFailures = failed;
            return;
        }
        layouts = loaded;
        lastFailures = failed;
        for (Map.Entry<String, List<String>> e : failed.entrySet()) {
            for (String err : e.getValue()) {
                plugin.getLogger().severe("ExcelMenu layout FAILED [" + e.getKey()
                        + "]: " + err);
            }
        }
        // Pin cross-check (3.0, DESIGN §2.2): the layout loader is
        // registry-blind, so a pinned id with no registry entry is
        // named SEVERE here — loud, and the menu still serves.
        for (LayoutDef def : loaded.values()) {
            for (String pin : def.pinned()) {
                if (entries.registry().entry(pin) == null) {
                    plugin.getLogger().severe("ExcelMenu layout '" + def.id()
                            + "': pinned entry '" + pin + "' has no registry entry"
                            + " — the pin will not render until the entry exists.");
                }
            }
        }
        List<String> names = new ArrayList<>(loaded.keySet());
        names.sort(String::compareTo);
        plugin.getLogger().info("ExcelMenu layouts: " + loaded.size() + " live: "
                + String.join(", ", names));
        if (sender != null) {
            sender.sendMessage("§aExcelMenu layouts reloaded: " + loaded.size()
                    + " menus, " + failed.size() + " failed.");
        }
    }

    // ---------- opening ----------

    boolean openMenu(Player player, String menuId) {
        return openMenu(player, menuId, 1);
    }

    boolean openMenu(Player player, String menuId, int page) {
        LayoutDef layout = layouts.get(menuId);
        if (layout == null) return false;
        forms.invalidate(player);
        List<ResolvedEntry> resolved = resolvedFor(player, menuId);
        if (bridge.detect(player)
                == com.winkreyes.excelmenu.core.platform.Detection.State.BEDROCK) {
            return forms.open(player, layout, resolved);
        }
        return openChest(player, layout, resolved, page);
    }

    /** The chest renderer: also FormService's fallback. */
    boolean openChest(Player player, String menuId) {
        LayoutDef layout = layouts.get(menuId);
        if (layout == null) return false;
        List<ResolvedEntry> resolved = resolvedFor(player, menuId);
        return openChest(player, layout, resolved, 1);
    }

    private boolean openChest(Player player, LayoutDef layout,
                              List<ResolvedEntry> resolved, int page) {
        ChestModel.Result result = ChestModel.build(layout, resolved, page - 1);
        if (!result.ok()) {
            plugin.getLogger().severe("ExcelMenu: cannot render '" + layout.id()
                    + "' for " + player.getName() + " — " + result.error());
            player.sendMessage(LegacyComponentSerializer.legacySection()
                    .deserialize("§cThat menu is unavailable right now."));
            return false;
        }
        ChestModel.Page model = result.page();
        long generation = generations.merge(player.getUniqueId(), 1L, Long::sum);
        RegistryMenuHolder holder = new RegistryMenuHolder(player.getUniqueId(),
                layout.id(), page, generation, model.slots());
        Component title = LegacyComponentSerializer.legacySection()
                .deserialize(Text.colorize(Placeholders.resolve(player, model.title())));
        Inventory inv = Bukkit.createInventory(holder, model.size(), title);
        holder.bind(inv);
        for (Map.Entry<Integer, ChestModel.Placed> e : model.slots().entrySet()) {
            inv.setItem(e.getKey(), buildStack(player, e.getValue()));
        }
        player.openInventory(inv);
        return true;
    }

    private ItemStack buildStack(Player player, ChestModel.Placed placed) {
        String materialName;
        String name;
        List<String> lore;
        if (placed instanceof ChestModel.Placed.EntryItem entry) {
            materialName = entry.entry().icon();
            name = entry.entry().label();
            lore = entry.entry().lore();
        } else if (placed instanceof ChestModel.Placed.FixedItem fixed) {
            materialName = fixed.fixed().material();
            name = fixed.fixed().name();
            lore = fixed.fixed().lore();
        } else {
            return new ItemStack(Material.AIR);
        }
        Material material;
        try {
            material = Material.valueOf(materialName);
        } catch (IllegalArgumentException e) {
            material = Material.PAPER;
        }
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            if (name != null) {
                meta.displayName(LegacyComponentSerializer.legacySection().deserialize(
                        Text.colorize(Placeholders.resolve(player, name))));
            }
            if (lore != null && !lore.isEmpty()) {
                List<Component> lines = new ArrayList<>();
                for (String line : lore) {
                    lines.add(LegacyComponentSerializer.legacySection().deserialize(
                            Text.colorize(Placeholders.resolve(player, line))));
                }
                meta.lore(lines);
            }
            stack.setItemMeta(meta);
        }
        return stack;
    }

    // ---------- clicks ----------

    /** Chest clicks for registry views (the MenuListener rule: the
     *  holder's map is trusted, never the ItemStack). */
    final class ClickListener implements Listener {

        @EventHandler
        public void onClick(InventoryClickEvent event) {
            if (!(event.getInventory().getHolder() instanceof RegistryMenuHolder holder)) {
                return;
            }
            event.setCancelled(true);
            if (!(event.getWhoClicked() instanceof Player player)) return;
            if (event.getClickedInventory() != event.getInventory()) return;
            if (!holder.player().equals(player.getUniqueId())) return;
            ChestModel.Placed placed = holder.at(event.getSlot());
            if (placed == null) return;
            LayoutDef layout = layouts.get(holder.menuId());
            if (layout == null) return;

            if (placed instanceof ChestModel.Placed.FixedItem fixed) {
                if (fixed.fixed().page() != null) {
                    openMenu(player, holder.menuId(), fixed.fixed().page());
                } else if (!fixed.fixed().action().isEmpty()) {
                    dispatcher.runAll(player, holder.menuId() + ":chrome",
                            fixed.fixed().action(),
                            session(player, holder));
                }
                return;
            }
            if (placed instanceof ChestModel.Placed.EntryItem entryItem) {
                // Re-resolve before dispatching: the CURRENT action
                // for the CURRENT state runs, not the rendered one.
                List<ResolvedEntry> fresh = resolvedFor(player, holder.menuId());
                ResolvedEntry current = null;
                for (ResolvedEntry r : fresh) {
                    if (r.id().equals(entryItem.entry().id())) { current = r; break; }
                }
                if (current == null) {
                    // The door vanished since render (date window,
                    // guard): explain, don't dispatch a ghost.
                    player.sendMessage(LegacyComponentSerializer.legacySection()
                            .deserialize("§eThat isn't available right now."));
                    return;
                }
                StatsSink.tap(player, holder.menuId(), current.id(), event.getSlot(), "java");
                dispatcher.runAll(player, holder.menuId() + ":" + current.id(),
                        current.action(), session(player, holder));
            }
        }

        @EventHandler
        public void onDrag(InventoryDragEvent event) {
            if (event.getInventory().getHolder() instanceof RegistryMenuHolder) {
                event.setCancelled(true);
            }
        }
    }

    private ActionDispatcher.Session session(Player player, RegistryMenuHolder holder) {
        return new ActionDispatcher.Session() {
            @Override
            public boolean valid(Player p) {
                return plugin.isEnabled() && p.isOnline()
                        && generations.getOrDefault(p.getUniqueId(), 0L)
                                == holder.generation()
                        && layouts.containsKey(holder.menuId());
            }

            @Override
            public void close(Player p) {
                p.closeInventory();
            }

            @Override
            public void refresh(Player p) {
                openMenu(p, holder.menuId(), holder.page());
            }
        };
    }
}

// ExcelMenu — an Excel plugin by WinkReyesValley. (Excel signature — MikeExcel)

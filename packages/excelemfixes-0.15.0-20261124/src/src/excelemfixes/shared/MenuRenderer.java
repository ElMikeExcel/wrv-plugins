package excelemfixes.shared;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.logging.Logger;

/**
 * The one renderer for every companion module. Turns shared Views
 * into a Java chest GUI or a Bedrock Cumulus form (identical
 * content), routes button actions to the module that owns the
 * action's prefix, and keeps a per-player view stack so core:back
 * and core:close work in every menu with no module code.
 *
 * Bedrock notes (established server pattern): Cumulus forms have no
 * item icons and no hover lore, so a button's detail screen carries
 * what the Java hover lore shows; if a form cannot be delivered the
 * player gets the chest GUI instead of nothing.
 */
public final class MenuRenderer implements Listener {

    /** Marker holder so the click listener only handles our menus. */
    static final class FixesHolder implements InventoryHolder {
        @Override
        public Inventory getInventory() {
            return null;
        }
    }

    private final JavaPlugin plugin;
    private final Logger log;
    private final NamespacedKey actionKey;
    private final Map<String, BiConsumer<Player, String>> handlers =
            new HashMap<>();
    private final Map<UUID, Deque<Views.View>> stacks = new HashMap<>();
    private final Map<UUID, Views.View> current = new HashMap<>();

    public MenuRenderer(JavaPlugin plugin) {
        this.plugin = plugin;
        this.log = plugin.getLogger();
        this.actionKey = new NamespacedKey(plugin, "action");
    }

    /** Modules register the handler for their action prefix. */
    public void registerHandler(String prefix,
                                BiConsumer<Player, String> handler) {
        handlers.put(prefix, handler);
    }

    public void unregisterHandler(String prefix) {
        handlers.remove(prefix);
    }

    // ------------------------------------------------------------ opening

    /** Opens a view as a new navigation root (clears the stack). */
    public void openRoot(Player player, Views.View view) {
        Deque<Views.View> stack =
                stacks.computeIfAbsent(player.getUniqueId(),
                        k -> new ArrayDeque<>());
        stack.clear();
        stack.push(view);
        render(player, view);
    }

    /** Opens a view on top of the current stack (forward navigation). */
    public void open(Player player, Views.View view) {
        Deque<Views.View> stack =
                stacks.computeIfAbsent(player.getUniqueId(),
                        k -> new ArrayDeque<>());
        stack.push(view);
        render(player, view);
    }

    /**
     * Replaces the current top view with a fresh one — the
     * refresh after an action changed what the top view shows
     * (a toggle inside the view itself). The stack depth is
     * unchanged, so Back still lands on the view below; on
     * Bedrock this also keeps the flow alive, since a form
     * closes on selection and only a re-render reopens it.
     */
    public void openReplace(Player player, Views.View view) {
        Deque<Views.View> stack =
                stacks.computeIfAbsent(player.getUniqueId(),
                        k -> new ArrayDeque<>());
        if (!stack.isEmpty()) {
            stack.pop();
        }
        stack.push(view);
        render(player, view);
    }

    /** Opens a detail text screen; Back returns to the current view. */
    public void openDetail(Player player, Views.Detail detail) {
        if (isBedrock(player)) {
            StringBuilder body = new StringBuilder();
            for (String line : detail.lines()) {
                if (body.length() > 0) body.append('\n');
                body.append(TextUtil.plain(line));
            }
            boolean sent = GeyserForms.sendSimpleForm(player, detail.title(),
                    body.toString(), new String[]{"Back"},
                    id -> {
                        if (id == 0) {
                            Views.View v = current.get(player.getUniqueId());
                            if (v != null) render(player, v);
                        }
                    }, log);
            if (!sent) {
                for (String line : detail.lines()) {
                    player.sendMessage("§7" + TextUtil.plain(line));
                }
            }
            return;
        }
        Inventory inv = Bukkit.createInventory(new FixesHolder(), 27,
                Component.text(detail.title()));
        ItemStack item = new ItemStack(materialOf(detail.icon()));
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(TextUtil.component("&f" + detail.title()));
            List<Component> lore = new ArrayList<>();
            for (String line : detail.lines()) {
                lore.add(TextUtil.component(line));
            }
            meta.lore(lore);
            meta.getPersistentDataContainer().set(actionKey,
                    PersistentDataType.STRING, "core:noop");
            item.setItemMeta(meta);
        }
        inv.setItem(13, item);
        inv.setItem(22, navItem("core:detail-back", "&fBack", "ARROW"));
        player.openInventory(inv);
    }

    // ------------------------------------------------------------- routing

    /** The single action entry point for clicks and form selections. */
    public void dispatch(Player player, String action) {
        if (action == null || action.isBlank()) return;
        try {
            if (action.equals("core:noop")) {
                return;
            }
            if (action.equals("core:close")) {
                stacks.remove(player.getUniqueId());
                current.remove(player.getUniqueId());
                player.closeInventory();
                return;
            }
            if (action.equals("core:detail-back")) {
                // Leaving a detail screen re-renders the view under
                // it; the navigation stack is untouched.
                Views.View v = current.get(player.getUniqueId());
                if (v != null) {
                    render(player, v);
                } else {
                    player.closeInventory();
                }
                return;
            }
            if (action.equals("core:back")) {
                Deque<Views.View> stack = stacks.get(player.getUniqueId());
                if (stack != null && stack.size() > 1) {
                    stack.pop();
                    render(player, stack.peek());
                } else {
                    Views.View v = current.get(player.getUniqueId());
                    if (v != null && stack != null && !stack.isEmpty()) {
                        render(player, v);
                    } else {
                        player.closeInventory();
                    }
                }
                return;
            }
            if (action.startsWith("core:detail:")) {
                int idx = Integer.parseInt(
                        action.substring("core:detail:".length()));
                Views.View v = current.get(player.getUniqueId());
                if (v != null && idx >= 0 && idx < v.buttons().size()) {
                    Views.Detail d = v.buttons().get(idx).detail();
                    if (d != null) openDetail(player, d);
                }
                return;
            }
            String prefix = action.split(":", 2)[0];
            BiConsumer<Player, String> handler = handlers.get(prefix);
            if (handler != null) {
                handler.accept(player, action);
            }
        } catch (RuntimeException e) {
            log.warning("ExcelEMFixes: action '" + action
                    + "' failed: " + e);
        }
    }

    // -------------------------------------------------------- Java renderer

    private void render(Player player, Views.View view) {
        current.put(player.getUniqueId(), view);
        if (isBedrock(player) && renderBedrock(player, view)) {
            return;
        }
        renderJava(player, view);
    }

    private void renderJava(Player player, Views.View view) {
        Inventory inv = Bukkit.createInventory(new FixesHolder(), 54,
                Component.text(view.title()));
        int slot = 0;
        int navSlot = 45;
        List<Views.Button> buttons = view.buttons();
        for (int i = 0; i < buttons.size(); i++) {
            Views.Button b = buttons.get(i);
            ItemStack item = menuItem(b, i);
            if (b.nav()) {
                if (navSlot < 54) inv.setItem(navSlot++, item);
            } else if (slot < 45) {
                inv.setItem(slot++, item);
            }
        }
        player.openInventory(inv);
    }

    private ItemStack menuItem(Views.Button b, int index) {
        ItemStack item = new ItemStack(materialOf(b.icon()));
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(TextUtil.component(b.name()));
            List<Component> lore = new ArrayList<>();
            for (String line : b.lore()) {
                lore.add(TextUtil.component(line));
            }
            meta.lore(lore);
            String action = b.detail() != null
                    ? "core:detail:" + index : b.action();
            meta.getPersistentDataContainer().set(actionKey,
                    PersistentDataType.STRING, action);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack navItem(String action, String name, String icon) {
        ItemStack item = new ItemStack(materialOf(icon));
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(TextUtil.component(name));
            meta.getPersistentDataContainer().set(actionKey,
                    PersistentDataType.STRING, action);
            item.setItemMeta(meta);
        }
        return item;
    }

    private static Material materialOf(String icon) {
        Material mat = icon == null ? null : Material.matchMaterial(icon);
        return mat == null ? Material.STONE : mat;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder()
                instanceof FixesHolder)) {
            return;
        }
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;
        String action = clicked.getItemMeta().getPersistentDataContainer()
                .get(actionKey, PersistentDataType.STRING);
        if (action != null) dispatch(player, action);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        stacks.remove(event.getPlayer().getUniqueId());
        current.remove(event.getPlayer().getUniqueId());
    }

    // ----------------------------------------------------- Bedrock renderer

    private boolean renderBedrock(Player player, Views.View view) {
        List<Views.Button> all = view.buttons();
        // Content buttons first, then nav (Back/pages/Close) — the
        // same order a player scans in the Java GUI.
        List<Views.Button> ordered = new ArrayList<>();
        for (Views.Button b : all) if (!b.nav()) ordered.add(b);
        for (Views.Button b : all) if (b.nav()) ordered.add(b);
        String[] labels = new String[ordered.size()];
        for (int i = 0; i < ordered.size(); i++) {
            labels[i] = TextUtil.plain(ordered.get(i).name());
        }
        String intro = view.intro() == null || view.intro().isBlank()
                ? "Pick one:" : TextUtil.plain(view.intro());
        return GeyserForms.sendSimpleForm(player, view.title(), intro,
                labels, id -> {
                    if (id >= 0 && id < ordered.size()) {
                        Views.Button b = ordered.get(id);
                        if (b.detail() != null) {
                            openDetail(player, b.detail());
                        } else {
                            dispatch(player, b.action());
                        }
                    }
                }, log);
    }

    private boolean isBedrock(Player player) {
        return GeyserForms.isBedrockPlayer(player, log);
    }
}

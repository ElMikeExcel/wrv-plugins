package com.winkreyesvalley.excelhalloweentoggles;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.function.BiFunction;

/**
 * ExcelHalloweenToggles — the Java /scares chest GUI (0.1.1).
 *
 * A 27-slot chest in the ExcelMenu state style: one item per scare,
 * its material/name/lore driven by the player's CURRENT state (the
 * ScareView model — identical wording and icons to the ExcelMenu
 * halloween_scares definition), so ON/OFF reads at a glance. A click
 * flips through the exact same path /scares &lt;feature&gt; uses (the
 * BiFunction handed in by ScaresCommand) and the GUI re-renders in
 * place, so the player watches the state change.
 *
 * Ownership is proven by a marker InventoryHolder, never by title —
 * titles are display text, holders are identity.
 */
public final class ScaresGui implements Listener {

    /** Marker holder: an inventory whose holder is this class is ours. */
    static final class Holder implements InventoryHolder {
        @Override
        public Inventory getInventory() {
            return null; // identity marker only; Bukkit tracks the real inventory
        }
    }

    private final ExcelHalloweenToggles plugin;
    private final FeatureRegistry registry;
    private final ToggleStore store;
    private final BiFunction<Player, FeatureDef, Boolean> flipper;

    public ScaresGui(ExcelHalloweenToggles plugin, FeatureRegistry registry, ToggleStore store,
                     BiFunction<Player, FeatureDef, Boolean> flipper) {
        this.plugin = plugin;
        this.registry = registry;
        this.store = store;
        this.flipper = flipper;
    }

    /** Open (or re-open, after a flip) the GUI with fresh states. */
    public void open(Player player) {
        ScareView.GuiModel model = ScareView.gui(registry,
                key -> store.isEnabled(player.getUniqueId(), key));
        Inventory inv = Bukkit.createInventory(new Holder(), ScareView.GUI_SIZE,
                plugin.text("messages.gui-title", "&6&lHalloween Scares"));
        for (ScareView.GuiItem item : model.items()) {
            ItemStack stack = new ItemStack(item.material());
            ItemMeta meta = stack.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(item.name());
                meta.setLore(item.lore());
                stack.setItemMeta(meta);
            }
            inv.setItem(item.slot(), stack);
        }
        ItemStack close = new ItemStack(Material.BARRIER);
        ItemMeta closeMeta = close.getItemMeta();
        if (closeMeta != null) {
            closeMeta.setDisplayName(plugin.text("messages.gui-close", "&7&lClose"));
            close.setItemMeta(closeMeta);
        }
        inv.setItem(ScareView.CLOSE_SLOT, close);
        player.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof Holder)) {
            return;
        }
        // Ours: nothing may move while the switch panel is open.
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (event.getClickedInventory() != event.getView().getTopInventory()) {
            return; // a click in the player's own inventory — cancelled, not a switch
        }
        int slot = event.getSlot();
        if (slot == ScareView.CLOSE_SLOT) {
            player.closeInventory();
            return;
        }
        ScareView.GuiModel model = ScareView.gui(registry,
                key -> store.isEnabled(player.getUniqueId(), key));
        String key = model.keyAt(slot);
        if (key == null) {
            return;
        }
        FeatureDef feature = registry.byKey(key);
        if (feature == null) {
            return;
        }
        flipper.apply(player, feature);
        if (player.isOnline()) {
            open(player); // re-render in place with the new state
        }
    }
}

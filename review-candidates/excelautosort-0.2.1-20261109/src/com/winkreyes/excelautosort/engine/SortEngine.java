package com.winkreyes.excelautosort.engine;

import com.winkreyes.excelautosort.ExcelAutoSort;
import com.winkreyes.excelautosort.core.SortCore;
import org.bukkit.Material;
import org.bukkit.Registry;
import org.bukkit.block.Container;
import org.bukkit.block.DoubleChest;
import org.bukkit.entity.ChestBoat;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.minecart.StorageMinecart;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * The parity sort engine (SPEC §2 P1–P8, §4): tidies the container a
 * player opened/closed, for players whose toggle is ON. The ordering
 * and merging run through {@link SortCore} — the unit-tested pure
 * model — so the rig and the unit suite test the very code that runs.
 */
public final class SortEngine {

    private final ExcelAutoSort plugin;
    private final Map<Material, Integer> materialOrder = new HashMap<>();

    public SortEngine(ExcelAutoSort plugin) {
        this.plugin = plugin;
        int n = 0;
        for (Material m : Registry.MATERIAL) materialOrder.put(m, n++);
    }

    public boolean isOn(UUID player) {
        return plugin.store().sortOn.contains(player);
    }

    public String statusOf(UUID player) {
        return isOn(player) ? "ON" : "OFF";
    }

    /** Flip a player's toggle; returns the new state. Persists immediately (P8). */
    public boolean toggle(Player player) {
        UUID uuid = player.getUniqueId();
        boolean now;
        if (plugin.store().sortOn.remove(uuid)) {
            now = false;
        } else {
            plugin.store().sortOn.add(uuid);
            now = true;
        }
        plugin.store().savePlayers();
        return now;
    }

    private Set<InventoryType> sortableTypes() {
        Set<InventoryType> out = EnumSet.noneOf(InventoryType.class);
        for (String s : plugin.getConfig().getStringList("sort.types")) {
            try {
                out.add(InventoryType.valueOf(s.trim().toUpperCase()));
            } catch (IllegalArgumentException ignored) {
                // A bad config line drops one type, never the engine.
            }
        }
        if (out.isEmpty()) {
            out.addAll(Set.of(InventoryType.CHEST, InventoryType.BARREL,
                    InventoryType.ENDER_CHEST, InventoryType.SHULKER_BOX));
        }
        return out;
    }

    /** Display names for the toggle feedback line, sorted, from config (§9). */
    public String typeListForMessage() {
        List<String> names = new ArrayList<>();
        for (InventoryType t : sortableTypes()) {
            names.add(plugin.getConfig().getString("sort.type-names." + t.name(), titleCase(t.name())));
        }
        names.sort(String::compareTo);
        return String.join(", ", names);
    }

    private static String titleCase(String enumName) {
        String[] parts = enumName.toLowerCase().split("_");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (!sb.isEmpty()) sb.append(' ');
            if (!p.isEmpty()) sb.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1));
        }
        return sb.toString();
    }

    public void sortOnOpen(Inventory inv, HumanEntity player) {
        sort(inv, player);
    }

    public void sortOnClose(Inventory inv, HumanEntity player) {
        sort(inv, player);
    }

    private void sort(Inventory inventory, HumanEntity player) {
        if (!sortableTypes().contains(inventory.getType())) return;
        if (!isWorldStorage(inventory, player)) return;
        if (!isOn(player.getUniqueId())) return;
        boolean hasViewers = inventory.getViewers().stream()
                .anyMatch(v -> !v.getUniqueId().equals(player.getUniqueId()));
        if (hasViewers) return;

        List<SortCore.StackView> views = new ArrayList<>();
        Map<String, ItemStack> templates = new HashMap<>();
        for (ItemStack stack : inventory.getContents()) {
            if (stack == null || stack.getType() == Material.AIR || stack.getAmount() <= 0) continue;
            ItemMeta meta = stack.getItemMeta();
            String metaString = meta == null ? "{}" : meta.getAsString();
            SortCore.StackView v = new SortCore.StackView(
                    materialOrder.getOrDefault(stack.getType(), 0),
                    stack.getType().getKey().getKey(),
                    metaString,
                    stack.getAmount(),
                    stack.getType().getMaxStackSize());
            views.add(v);
            templates.putIfAbsent(v.similarityKey(), stack);
        }
        List<SortCore.StackView> plan = SortCore.sortAndMerge(views);
        inventory.clear();
        int slot = 0;
        for (SortCore.StackView v : plan) {
            ItemStack template = templates.get(v.similarityKey());
            if (template == null) continue;
            ItemStack placed = template.clone();
            placed.setAmount(v.amount());
            inventory.setItem(slot++, placed);
        }
    }

    /**
     * Only real world storage is sorted (P5): block containers, double
     * chests, storage minecarts, chest boats. An ender chest sorts only
     * when it is the viewing player's own. Plugin GUIs — whose holder is
     * null, a player, or a plugin class — are never touched.
     */
    private boolean isWorldStorage(Inventory inventory, HumanEntity player) {
        if (inventory.getType() == InventoryType.ENDER_CHEST) {
            return inventory.equals(player.getEnderChest());
        }
        InventoryHolder holder = inventory.getHolder(false);
        return holder instanceof Container
                || holder instanceof DoubleChest
                || holder instanceof StorageMinecart
                || holder instanceof ChestBoat;
    }
}

// ExcelAutoSort — Iris / Mike, for Wink Reyes Valley

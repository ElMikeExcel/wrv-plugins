package com.winkreyes.excelitems.api;

import com.winkreyes.excelitems.core.ItemDefinition;
import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;

import java.util.Collection;
import java.util.Optional;
import java.util.OptionalInt;

/**
 * The public API other Excel plugins call (ExcelShops, ExcelDowned
 * and future plugins hard-depend on ExcelItems via plugin.yml
 * depend, so this class is always present when they enable).
 *
 * <p>Static access goes through the Bukkit services manager:
 * <pre>
 *   ExcelItemsAPI api = ExcelItemsAPI.get();
 *   ItemStack stack = api.create(4);            // by permanent ID
 *   ItemStack same  = api.create("revive_potion"); // by key
 *   OptionalInt id  = api.identify(stack);      // from the item's own data
 * </pre>
 * Shops, menus and loot reference items by ID (or key) in config —
 * adding an existing custom item to a shop is a config line naming
 * the ID, never a code change.
 */
public interface ExcelItemsAPI {

    /** The live API instance. Throws if ExcelItems is not enabled. */
    static ExcelItemsAPI get() {
        ExcelItemsAPI api = Bukkit.getServicesManager().load(ExcelItemsAPI.class);
        if (api == null) throw new IllegalStateException("ExcelItems is not enabled");
        return api;
    }

    /** Mint one item by permanent registry ID. Throws on unknown ID. */
    ItemStack create(int id);

    /** Mint {@code amount} (1..64) of the item with this ID. */
    ItemStack create(int id, int amount);

    /** Mint one item by registry key. Throws on unknown key. */
    ItemStack create(String key);

    /** The registry ID this stack carries, from its own PDC data. */
    OptionalInt identify(ItemStack stack);

    /** The full definition this stack identifies as, if any. */
    Optional<ItemDefinition> identifyDefinition(ItemStack stack);

    /** True when the stack identifies as any registered custom item. */
    boolean isCustomItem(ItemStack stack);

    /** True when the stack identifies as exactly this registry ID. */
    boolean isItem(ItemStack stack, int id);

    Optional<ItemDefinition> definition(int id);

    Optional<ItemDefinition> definition(String key);

    Collection<ItemDefinition> definitions();

    /**
     * Register a new custom item definition (IDs are permanent and
     * never reused; a duplicate ID or key is rejected). Owning
     * plugins call this at their own enable, after ExcelItems.
     */
    ItemDefinition register(ItemDefinition definition);

    /** Register the behavior handler for one registry ID. */
    void registerBehavior(int id, ItemBehavior behavior);

    /** The behavior handler registered for this ID, if any. */
    Optional<ItemBehavior> behavior(int id);

    /** The next never-used registry ID. */
    int nextId();
}

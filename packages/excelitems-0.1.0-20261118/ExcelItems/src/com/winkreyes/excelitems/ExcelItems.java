package com.winkreyes.excelitems;

import com.winkreyes.excelitems.api.ExcelItemsAPI;
import com.winkreyes.excelitems.api.ItemBehavior;
import com.winkreyes.excelitems.core.BuiltinItems;
import com.winkreyes.excelitems.core.ItemDefinition;
import com.winkreyes.excelitems.core.ItemRegistry;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;

/**
 * ExcelItems — the standalone custom-item registry (CUSTOM ITEM
 * REGISTRY design law, Mike, Nov 17, 2026). FREE, small, stable and
 * boring by design: it owns the registry, the mint and the identify
 * seam; every feature lives in the product plugins on top, which
 * hard-depend on this plugin (plugin.yml depend) exactly like
 * plugins depend on Vault or PlaceholderAPI.
 *
 * <p>Enable order of business: build the registry from the builtin
 * definitions, then validate the config's ID assignments against it.
 * Assignments are the persisted shape of "IDs are permanent, never
 * reused" — a drift between config.yml and the code definitions
 * disables the plugin (fail closed) rather than letting shops hand
 * out the wrong item under a familiar ID.
 */
public final class ExcelItems extends JavaPlugin implements ExcelItemsAPI {

    private ItemRegistry registry;
    private ItemFactory factory;
    private final Map<Integer, ItemBehavior> behaviors = new HashMap<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        registry = BuiltinItems.registry();
        List<String> problems = registry.validateAssignments(readAssignments());
        if (!problems.isEmpty()) {
            for (String problem : problems) getLogger().severe("ID assignment: " + problem);
            getLogger().severe("ExcelItems disabled: config ID assignments disagree with the "
                    + "registered definitions. IDs are permanent — fix config.yml, never renumber.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        factory = new ItemFactory(this, registry);
        getServer().getServicesManager().register(ExcelItemsAPI.class, this, this, ServicePriority.Normal);
        ItemsCommand command = new ItemsCommand(this);
        if (getCommand("excelitems") != null) {
            getCommand("excelitems").setExecutor(command);
            getCommand("excelitems").setTabCompleter(command);
        }
        getLogger().info("ExcelItems enabled: " + registry.size()
                + " custom item(s) registered, next free id " + registry.nextId() + ".");
    }

    private Map<String, String> readAssignments() {
        Map<String, String> raw = new HashMap<>();
        ConfigurationSection section = getConfig().getConfigurationSection("items");
        if (section == null) return raw;
        for (String key : section.getKeys(false)) {
            raw.put(key, String.valueOf(section.get(key)));
        }
        return raw;
    }

    // ------------------------------------------------ ExcelItemsAPI

    @Override
    public ItemStack create(int id) {
        return create(id, 1);
    }

    @Override
    public ItemStack create(int id, int amount) {
        return factory.create(registry.requireId(id), amount);
    }

    @Override
    public ItemStack create(String key) {
        return factory.create(registry.requireKey(key), 1);
    }

    @Override
    public OptionalInt identify(ItemStack stack) {
        Optional<ItemDefinition> def = factory.identify(stack);
        return def.isPresent() ? OptionalInt.of(def.get().id()) : OptionalInt.empty();
    }

    @Override
    public Optional<ItemDefinition> identifyDefinition(ItemStack stack) {
        return factory.identify(stack);
    }

    @Override
    public boolean isCustomItem(ItemStack stack) {
        return factory.identify(stack).isPresent();
    }

    @Override
    public boolean isItem(ItemStack stack, int id) {
        OptionalInt found = identify(stack);
        return found.isPresent() && found.getAsInt() == id;
    }

    @Override
    public Optional<ItemDefinition> definition(int id) {
        return registry.byId(id);
    }

    @Override
    public Optional<ItemDefinition> definition(String key) {
        return registry.byKey(key);
    }

    @Override
    public Collection<ItemDefinition> definitions() {
        return registry.all();
    }

    @Override
    public ItemDefinition register(ItemDefinition definition) {
        return registry.register(definition);
    }

    @Override
    public void registerBehavior(int id, ItemBehavior behavior) {
        registry.requireId(id);
        if (behavior == null) throw new IllegalArgumentException("behavior required");
        behaviors.put(id, behavior);
    }

    @Override
    public Optional<ItemBehavior> behavior(int id) {
        return Optional.ofNullable(behaviors.get(id));
    }

    @Override
    public int nextId() {
        return registry.nextId();
    }

    /** Registry access for the admin command. */
    ItemRegistry registry() {
        return registry;
    }
}

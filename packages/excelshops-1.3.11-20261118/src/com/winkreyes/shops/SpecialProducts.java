// SPDX-License-Identifier: MIT
package com.winkreyes.shops;

import com.winkreyes.excelitems.api.ExcelItemsAPI;
import com.winkreyes.excelitems.core.BuiltinItems;
import com.winkreyes.excelitems.core.ItemDefinition;
import com.winkreyes.excelitems.core.ItemRegistry;
import com.winkreyes.shops.checkout.Money;
import java.io.File;
import java.util.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

/**
 * The special-product listings, resolved through the ExcelItems
 * registry (M4 migration, P-ITEM-REGISTRY-1). A listing is CONFIG,
 * not code: each entry in the {@code special-products} config
 * section names a shop product, its category, and the permanent
 * registry ID it sells ({@code excelitems_id}); the item's name and
 * lore come from the registry definition for that ID, and the price
 * stays a shop fact in the same config entry (design law D123 D3:
 * price is never part of the item definition). Delivery stays the
 * proven owner console command (supervisor decision D124), but the
 * command template and the owning plugin are config data too, so
 * MobileCatalog and ShopUi run ONE registry-resolved delivery path
 * for every special instead of a hardcoded per-product switch.
 *
 * <p>Adding an already-registered item to the shop is therefore a
 * config change only: one listing here plus the matching menu
 * entry. No code patch, no count-pin edit, no rebuild of this
 * plugin's classes.
 *
 * <p>Definition lookup goes through the live ExcelItemsAPI when a
 * server is present (ExcelShops hard-depends on ExcelItems, so the
 * API is always enabled first on a real server); when the API
 * cannot be reached (the headless JVM suites) it falls back to the
 * registry's own builtin table, which is the same data of record.
 */
public final class SpecialProducts {

    /** One config listing, fully resolved against the registry. */
    public record Listing(String productId, String category, int excelitemsId, long price,
                          String delivery, String owner, ItemDefinition definition) {
        public Listing {
            Objects.requireNonNull(productId, "productId");
            Objects.requireNonNull(category, "category");
            Objects.requireNonNull(delivery, "delivery");
            Objects.requireNonNull(owner, "owner");
            Objects.requireNonNull(definition, "definition");
        }
        /** The registry key this listing sells (definition of record). */
        public String registryKey() { return definition.key(); }
        /** Menu form of the delivery command, as the menus pin it. */
        public String menuCommand() { return "[console] " + delivery; }
        /** The console command for one buyer, player name substituted. */
        public String deliveryCommand(String playerName) {
            return delivery.replace("%player_name%", playerName);
        }
    }

    /** Where registry definitions come from (live API or builtin table). */
    public interface DefinitionLookup {
        Optional<ItemDefinition> byId(int id);
        Collection<ItemDefinition> all();
    }

    private final List<Listing> listings;
    private final Map<String, Listing> byProductId = new LinkedHashMap<>();
    private final Map<String, List<Listing>> byCategory = new LinkedHashMap<>();
    private final Set<String> protectedKeys = new LinkedHashSet<>();

    private SpecialProducts(List<Listing> listings, DefinitionLookup lookup) {
        this.listings = List.copyOf(listings);
        for (Listing listing : listings) {
            if (byProductId.put(listing.productId(), listing) != null)
                throw new IllegalArgumentException("Duplicate special product listing: " + listing.productId());
            byCategory.computeIfAbsent(listing.category(), c -> new ArrayList<>()).add(listing);
            protectedKeys.add(listing.productId());
            protectedKeys.add(listing.registryKey());
        }
        // The never-shadowed guard is expressed against the registry
        // itself: every registered key is protected, listed or not.
        for (ItemDefinition definition : lookup.all()) protectedKeys.add(definition.key());
    }

    public List<Listing> listings() { return listings; }
    public Optional<Listing> byProduct(String productId) {
        return Optional.ofNullable(byProductId.get(productId));
    }
    public List<Listing> inCategory(String category) {
        return List.copyOf(byCategory.getOrDefault(category, List.of()));
    }
    /** Product ids and registry keys a catalog offer may never shadow. */
    public Set<String> protectedKeys() { return Collections.unmodifiableSet(protectedKeys); }
    /** Every baseline registry ID (1..4) must stay listed; a dropped listing fails closed. */
    public void requireBaselineIds() {
        for (int id = 1; id <= 4; id++) {
            int required = id;
            if (listings.stream().noneMatch(l -> l.excelitemsId() == required))
                throw new IllegalArgumentException(
                    "Special-products config no longer lists baseline registry id " + required);
        }
    }

    /** Parse + resolve a {@code special-products} config section. Fail-closed on any drift. */
    public static SpecialProducts parse(ConfigurationSection section, DefinitionLookup lookup) {
        Objects.requireNonNull(section, "special-products section");
        Objects.requireNonNull(lookup, "lookup");
        List<Listing> parsed = new ArrayList<>();
        Set<String> seenSlots = new HashSet<>();
        for (String productId : section.getKeys(false)) {
            ConfigurationSection entry = section.getConfigurationSection(productId);
            if (entry == null) throw new IllegalArgumentException("Special product entry is not a section: " + productId);
            String category = entry.getString("category");
            int registryId = entry.getInt("excelitems_id", -1);
            String delivery = entry.getString("delivery");
            String owner = entry.getString("owner");
            if (category == null || category.isBlank())
                throw new IllegalArgumentException("Special product has no category: " + productId);
            if (registryId <= 0)
                throw new IllegalArgumentException("Special product has no excelitems_id: " + productId);
            if (delivery == null || !delivery.contains("%player_name%"))
                throw new IllegalArgumentException("Special product delivery must name %player_name%: " + productId);
            if (owner == null || owner.isBlank())
                throw new IllegalArgumentException("Special product has no owner plugin: " + productId);
            if (!seenSlots.add(category + "/" + productId))
                throw new IllegalArgumentException("Duplicate special product listing: " + category + "/" + productId);
            ItemDefinition definition = lookup.byId(registryId).orElseThrow(() ->
                new IllegalArgumentException("Special product " + productId
                    + " names unknown ExcelItems registry id " + registryId));
            long price = Money.parse(entry.getString("price", ""));
            parsed.add(new Listing(productId, category, registryId, price, delivery, owner, definition));
        }
        if (parsed.isEmpty()) throw new IllegalArgumentException("Special-products config is empty");
        return new SpecialProducts(parsed, lookup);
    }

    /** Live lookup: the ExcelItems API on a running server; builtin table when headless. */
    public static DefinitionLookup liveLookup() {
        try {
            ExcelItemsAPI api = ExcelItemsAPI.get();
            return new DefinitionLookup() {
                @Override public Optional<ItemDefinition> byId(int id) { return api.definition(id); }
                @Override public Collection<ItemDefinition> all() { return api.definitions(); }
            };
        } catch (Throwable unavailable) {
            return builtinLookup();
        }
    }

    /** The registry's own builtin table (the same definitions the API serves). */
    public static DefinitionLookup builtinLookup() {
        ItemRegistry registry = BuiltinItems.registry();
        return new DefinitionLookup() {
            @Override public Optional<ItemDefinition> byId(int id) { return registry.byId(id); }
            @Override public Collection<ItemDefinition> all() { return registry.all(); }
        };
    }

    /**
     * The shipped listings, for the headless suites and other callers
     * without a plugin instance: the {@code special-products} section
     * of the working directory's config.yml when present, else the
     * bundled special-products.yml beside it. Both files ship with
     * identical listings (a test pins the agreement).
     */
    public static SpecialProducts shipped() {
        File config = new File("config.yml");
        if (config.isFile()) {
            ConfigurationSection section = YamlConfiguration.loadConfiguration(config)
                .getConfigurationSection("special-products");
            if (section != null) return parse(section, builtinLookup());
        }
        File resource = new File("special-products.yml");
        if (resource.isFile()) {
            ConfigurationSection section = YamlConfiguration.loadConfiguration(resource)
                .getConfigurationSection("special-products");
            if (section != null) return parse(section, builtinLookup());
        }
        throw new IllegalStateException("No special-products config found (config.yml or special-products.yml)");
    }
}

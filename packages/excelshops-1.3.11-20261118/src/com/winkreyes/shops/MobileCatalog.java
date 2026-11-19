// SPDX-License-Identifier: MIT
package com.winkreyes.shops;

import com.winkreyes.shops.checkout.Money;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Pattern;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

/** Read-only copies of the six installed menus. Never installs a catalog or evaluates YAML commands. */
public final class MobileCatalog {
    /**
     * ORDINARY products are priced from the spawn catalog. SPECIAL
     * products are the ExcelItems-registry listings: each one names a
     * permanent registry ID in config (see SpecialProducts) and is
     * delivered by its owning plugin's console command. There is one
     * SPECIAL route, not one route per product — which product a
     * SPECIAL entry sells is data (its registry ID), never a code
     * branch.
     */
    public enum Route { ORDINARY, SPECIAL }
    public record Product(String id, String category, String name, int bundleSize, long price,
                          List<String> lore, Route route, String originalCommand,
                          int excelitemsId, String deliveryTemplate, String ownerPlugin) {
        public Product { lore = List.copyOf(lore); }
        /** Ordinary catalog product (no registry identity). */
        static Product ordinary(String id, String category, String name, int bundleSize, long price,
                                List<String> lore, String originalCommand) {
            return new Product(id, category, name, bundleSize, price, lore, Route.ORDINARY,
                originalCommand, 0, null, null);
        }
        /** The one registry-resolved delivery path: the listing's console command for one buyer. */
        public String deliveryCommand(String playerName) {
            if (route != Route.SPECIAL || deliveryTemplate == null)
                throw new IllegalStateException("Only a special product has a delivery command: " + id);
            return deliveryTemplate.replace("%player_name%", playerName);
        }
    }
    public record Category(String id, String name, List<String> lore, List<Product> products) {
        public Category { lore = List.copyOf(lore); products = List.copyOf(products); }
    }
    private record CategorySpec(String id, List<String> products) {}
    private static final List<CategorySpec> EXPECTED = List.of(
        new CategorySpec("food", List.of("bread", "steak", "gcarrot", "gapple")),
        new CategorySpec("building", List.of("torch", "logs", "glass", "iron", "water")),
        new CategorySpec("travel_animals", List.of("saddle", "nametag", "lead", "pearl", "rocket")),
        new CategorySpec("enchanting", List.of("xp", "mending", "unbreaking", "efficiency")),
        // The potions & protection shelf's ordinary list is empty by
        // design: every product on it is a registry special, listed
        // in config (special-products), never pinned in code.
        new CategorySpec("potions_protection", List.of()));
    private static final Pattern PRICE = Pattern.compile("(?:Bundle price:|Price:)\\s*\\$([0-9,]+(?:\\.[0-9]{1,2})?)");
    private final Map<String, Category> categories = new LinkedHashMap<>();
    private final Map<String, Product> products = new LinkedHashMap<>();
    private final SpecialProducts specials;

    public static MobileCatalog load(WRVShops plugin) {
        SpecialProducts resolved;
        ConfigurationSection fromConfig = plugin.getConfig().getConfigurationSection("special-products");
        if (fromConfig != null && !fromConfig.getKeys(false).isEmpty()) {
            resolved = SpecialProducts.parse(fromConfig, SpecialProducts.liveLookup());
        } else {
            // The live data-folder config predates the migration and
            // carries no special-products section; the bundled
            // resource shipped in the jar is the fallback of record.
            var stream = plugin.getResource("special-products.yml");
            if (stream == null) throw new IllegalArgumentException("Missing special-products resource");
            try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                ConfigurationSection section = YamlConfiguration.loadConfiguration(reader)
                    .getConfigurationSection("special-products");
                if (section == null) throw new IllegalArgumentException("Bundled special-products.yml has no section");
                resolved = SpecialProducts.parse(section, SpecialProducts.liveLookup());
            } catch (java.io.IOException failure) { throw new IllegalArgumentException("Unreadable special-products resource", failure); }
        }
        return new MobileCatalog(name -> {
            var stream = plugin.getResource("mobile-menus/" + name + ".yml");
            if (stream == null) throw new IllegalArgumentException("Missing mobile menu resource: " + name);
            try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                return YamlConfiguration.loadConfiguration(reader);
            } catch (java.io.IOException failure) { throw new IllegalArgumentException("Unreadable mobile menu", failure); }
        }, plugin.getCatalog() == null ? null : plugin.getCatalog().offerData(), resolved);
    }
    MobileCatalog(Function<String, YamlConfiguration> source) { this(source, null, SpecialProducts.shipped()); }
    MobileCatalog(Function<String, YamlConfiguration> source, List<SpawnCatalog.OfferData> offers) {
        this(source, offers, SpecialProducts.shipped());
    }
    MobileCatalog(Function<String, YamlConfiguration> source, List<SpawnCatalog.OfferData> offers,
                  SpecialProducts specials) {
        this.specials = specials;
        specials.requireBaselineIds();
        ConfigurationSection root = required(source.apply("spawnshop"), "items");
        Set<String> rootKeys = new HashSet<>(Set.of("balance", "back"));
        for (CategorySpec spec : EXPECTED) rootKeys.add("category_" + spec.id());
        if (!root.getKeys(false).equals(rootKeys)) throw new IllegalArgumentException("Unexpected mobile root items");
        for (CategorySpec spec : EXPECTED) {
            ConfigurationSection category = required(root, "category_" + spec.id());
            expectCommands(category, "[openguimenu] spawnshop_" + spec.id());
            ConfigurationSection items = required(source.apply("spawnshop_" + spec.id()), "items");
            List<SpecialProducts.Listing> shelfSpecials = specials.inCategory(spec.id());
            Set<String> keys = new HashSet<>(spec.products()); keys.addAll(Set.of("balance", "back"));
            for (SpecialProducts.Listing listing : shelfSpecials) keys.add(listing.productId());
            if (!items.getKeys(false).equals(keys)) throw new IllegalArgumentException("Unexpected mobile category items");
            List<Product> entries = new ArrayList<>();
            for (String id : spec.products()) {
                ConfigurationSection item = required(items, id);
                String command = "[player] excelshop browse " + id;
                expectCommands(item, command);
                List<String> lore = item.getStringList("lore");
                Long price = null;
                for (String line : lore) {
                    var match = PRICE.matcher(plain(line));
                    if (match.find()) {
                        if (price != null) throw new IllegalArgumentException("Duplicate product price: " + id);
                        price = Money.parse(match.group(1).replace(",", ""));
                    }
                }
                int bundle = item.getInt("amount", 1);
                if (price == null || bundle < 1 || bundle > 64 || lore.isEmpty())
                    throw new IllegalArgumentException("Invalid mobile product: " + id);
                Product product = Product.ordinary(id, spec.id(), plain(item.getString("display_name", id)),
                    bundle, price, lore, command);
                entries.add(product); products.put(id, product);
            }
            for (SpecialProducts.Listing listing : shelfSpecials) {
                ConfigurationSection item = required(items, listing.productId());
                // The menu must dispatch exactly the command the
                // listing resolves to — the expected value is data
                // (config + registry), not a hardcoded string.
                expectCommands(item, listing.menuCommand());
                Long displayed = null;
                for (String line : item.getStringList("lore")) {
                    var match = PRICE.matcher(plain(line));
                    if (match.find()) {
                        if (displayed != null) throw new IllegalArgumentException("Duplicate product price: " + listing.productId());
                        displayed = Money.parse(match.group(1).replace(",", ""));
                    }
                }
                int bundle = item.getInt("amount", 1);
                if (displayed == null || displayed != listing.price() || bundle < 1 || bundle > 64)
                    throw new IllegalArgumentException("Special product menu drift (price or bundle): " + listing.productId());
                // Name and lore are the registry definition's — the
                // one construction of record for this registry ID.
                Product product = new Product(listing.productId(), spec.id(),
                    plain(listing.definition().displayName()), bundle, listing.price(),
                    listing.definition().lore(), Route.SPECIAL, listing.menuCommand(),
                    listing.excelitemsId(), listing.delivery(), listing.owner());
                entries.add(product); products.put(product.id(), product);
            }
            categories.put(spec.id(), new Category(spec.id(), plain(category.getString("display_name", spec.id())),
                category.getStringList("lore"), entries));
        }
        if (offers != null) derive(offers);
    }
    /**
     * Derives the mobile shop from the live spawn catalog, the same data Java browses.
     * The menu products keep their names, lore, order, categories and routes; ordinary
     * products take bundle size and price from the catalog (one price list, no drift),
     * and every catalog offer with no menu product joins the shelves as an ordinary
     * product in its catalog category, or in "More at the Shop" when it names none.
     * Special products are not catalog offers and are never created, repriced or shadowed:
     * the guard is expressed against the registry — an offer whose id is a special
     * listing's product id or ANY registered item key fails closed.
     */
    private void derive(List<SpawnCatalog.OfferData> offers) {
        Map<String, SpawnCatalog.OfferData> byId = new LinkedHashMap<>();
        for (SpawnCatalog.OfferData offer : offers) byId.put(offer.id(), offer);
        for (String protectedId : specials.protectedKeys())
            if (byId.containsKey(protectedId))
                throw new IllegalArgumentException("Catalog offer collides with a protected special product: " + protectedId);
        Set<String> legacyOrdinary = new HashSet<>();
        for (Category category : List.copyOf(categories.values())) {
            List<Product> reconciled = new ArrayList<>(category.products().size());
            for (Product product : category.products()) {
                SpawnCatalog.OfferData offer = product.route() == Route.ORDINARY ? byId.get(product.id()) : null;
                if (offer == null) { reconciled.add(product); continue; }
                legacyOrdinary.add(product.id());
                Product priced = new Product(product.id(), product.category(), product.name(),
                    offer.bundleSize(), offer.bundlePrice(), product.lore(), product.route(), product.originalCommand(),
                    product.excelitemsId(), product.deliveryTemplate(), product.ownerPlugin());
                reconciled.add(priced); products.put(priced.id(), priced);
            }
            categories.put(category.id(), new Category(category.id(), category.name(), category.lore(), reconciled));
        }
        Map<String, List<Product>> additions = new LinkedHashMap<>();
        for (SpawnCatalog.OfferData offer : offers) {
            if (legacyOrdinary.contains(offer.id())) continue;
            String categoryId = offer.category() == null ? "more" : offer.category();
            Product product = Product.ordinary(offer.id(), categoryId, offer.name(), offer.bundleSize(),
                offer.bundlePrice(), derivedLore(offer), "[player] excelshop browse " + offer.id());
            additions.computeIfAbsent(categoryId, id -> new ArrayList<>()).add(product);
            products.put(product.id(), product);
        }
        List<String> order = new ArrayList<>(additions.keySet());
        order.sort(Comparator.comparingInt(id -> id.equals("more") ? 1 : 0)); // The catch-all shelf renders last.
        for (String categoryId : order) {
            List<Product> extra = additions.get(categoryId);
            Category existing = categories.get(categoryId);
            if (existing != null) {
                List<Product> merged = new ArrayList<>(existing.products()); merged.addAll(extra);
                categories.put(categoryId, new Category(existing.id(), existing.name(), existing.lore(), merged));
            } else categories.put(categoryId, new Category(categoryId,
                categoryId.equals("more") ? "More at the Shop" : prettify(categoryId),
                List.of("&7Items from the spawn shop catalog.", "&8Tap to browse. No charge yet."), extra));
        }
    }
    private static List<String> derivedLore(SpawnCatalog.OfferData offer) {
        String price = "$" + formatDollars(offer.bundlePrice());
        return List.of(
            offer.bundleSize() > 1 ? "&7Sold in bundles of " + offer.bundleSize() : "&7Sold one at a time",
            offer.bundleSize() > 1 ? "&eBundle price: &f" + price : "&ePrice: &f" + price,
            "&8Tap to choose quantity. No charge yet.");
    }
    /** Menu-style dollars: thousands separators, no decimals on whole dollars. */
    static String formatDollars(long minorUnits) {
        long dollars = minorUnits / 100, cents = Math.abs(minorUnits % 100);
        String grouped = String.format(Locale.US, "%,d", dollars);
        return cents == 0 ? grouped : grouped + "." + (cents < 10 ? "0" : "") + cents;
    }
    private static String prettify(String categoryId) {
        StringBuilder name = new StringBuilder();
        for (String word : categoryId.split("_")) {
            if (word.isEmpty()) continue;
            if (name.length() > 0) name.append(' ');
            name.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return name.length() == 0 ? categoryId : name.toString();
    }
    private static ConfigurationSection required(ConfigurationSection yaml, String path) {
        return Objects.requireNonNull(yaml.getConfigurationSection(path), "Missing mobile section: " + path);
    }
    private static void expectCommands(ConfigurationSection section, String expected) {
        if (!section.getStringList("left_click_commands").equals(List.of(expected))
                || !section.getStringList("right_click_commands").equals(List.of(expected)))
            throw new IllegalArgumentException("Unapproved mobile route");
    }
    public static String labelName(Product product) {
        String prefix = product.bundleSize() + " ";
        return product.name().startsWith(prefix) ? product.name().substring(prefix.length()) : product.name();
    }
    public static String plain(String text) { return ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', text)); }
    public Collection<Category> categories() { return Collections.unmodifiableCollection(categories.values()); }
    public Category category(String id) { return Objects.requireNonNull(categories.get(id), "Unknown mobile category"); }
    public Product product(String id) { return Objects.requireNonNull(products.get(id), "Unknown mobile product"); }
    /** The registry listings this catalog was built from (config of record). */
    public SpecialProducts specials() { return specials; }
    /** Selection comes from the existing live catalog, not the resource's prices. Mismatch fails closed. */
    public void validate(Product product, ShopAdapter.Selection selection) {
        if (product.route() != Route.ORDINARY || selection.shop() != null
                || !selection.offerId().equals("spawn:" + product.id())
                || selection.bundleSize() != product.bundleSize() || selection.price() != product.price())
            throw new IllegalArgumentException("The shop listing changed. Please ask an administrator to refresh the mobile display.");
    }
    public static int maximum(ShopAdapter.Selection selection) {
        return (int) Math.min(Math.min(selection.available(), 2304 / selection.bundleSize()), 10000000000L / selection.price());
    }
    public static int quantity(String value, int maximum) {
        String text = value == null ? "" : value.trim();
        if (!text.matches("[0-9]{1,4}")) throw new IllegalArgumentException("Enter a whole number from 1 to " + maximum + ".");
        int amount = Integer.parseInt(text);
        if (amount < 1 || amount > maximum) throw new IllegalArgumentException("Enter a whole number from 1 to " + maximum + ".");
        return amount;
    }
}

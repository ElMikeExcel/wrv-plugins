// Excel signature - WRV maintained source; original authorship and license notices preserved.
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
    public enum Route { ORDINARY, VEIL, BASIC_PROTECTION, PROTECTION_POTION }
    public record Product(String id, String category, String name, int bundleSize, long price,
                          List<String> lore, Route route, String originalCommand) {
        public Product { lore = List.copyOf(lore); }
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
        new CategorySpec("potions_protection", List.of("veil_potion", "deathprotection_basic", "deathpotion")));
    private static final Pattern PRICE = Pattern.compile("(?:Bundle price:|Price:)\\s*\\$([0-9,]+(?:\\.[0-9]{1,2})?)");
    private final Map<String, Category> categories = new LinkedHashMap<>();
    private final Map<String, Product> products = new LinkedHashMap<>();

    public static MobileCatalog load(WRVShops plugin) {
        return new MobileCatalog(name -> {
            var stream = plugin.getResource("mobile-menus/" + name + ".yml");
            if (stream == null) throw new IllegalArgumentException("Missing mobile menu resource: " + name);
            try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                return YamlConfiguration.loadConfiguration(reader);
            } catch (java.io.IOException failure) { throw new IllegalArgumentException("Unreadable mobile menu", failure); }
        }, plugin.getCatalog() == null ? null : plugin.getCatalog().offerData());
    }
    MobileCatalog(Function<String, YamlConfiguration> source) { this(source, null); }
    MobileCatalog(Function<String, YamlConfiguration> source, List<SpawnCatalog.OfferData> offers) {
        ConfigurationSection root = required(source.apply("spawnshop"), "items");
        Set<String> rootKeys = new HashSet<>(Set.of("balance", "back"));
        for (CategorySpec spec : EXPECTED) rootKeys.add("category_" + spec.id());
        if (!root.getKeys(false).equals(rootKeys)) throw new IllegalArgumentException("Unexpected mobile root items");
        for (CategorySpec spec : EXPECTED) {
            ConfigurationSection category = required(root, "category_" + spec.id());
            expectCommands(category, "[openguimenu] spawnshop_" + spec.id());
            ConfigurationSection items = required(source.apply("spawnshop_" + spec.id()), "items");
            Set<String> keys = new HashSet<>(spec.products()); keys.addAll(Set.of("balance", "back"));
            if (!items.getKeys(false).equals(keys)) throw new IllegalArgumentException("Unexpected mobile category items");
            List<Product> entries = new ArrayList<>();
            for (String id : spec.products()) {
                ConfigurationSection item = required(items, id);
                Route route = switch (id) {
                    case "veil_potion" -> Route.VEIL;
                    case "deathprotection_basic" -> Route.BASIC_PROTECTION;
                    case "deathpotion" -> Route.PROTECTION_POTION;
                    default -> Route.ORDINARY;
                };
                String command = switch (route) {
                    case ORDINARY -> "[player] excelshop browse " + id;
                    case VEIL -> "[console] mobveil buy %player_name%";
                    case BASIC_PROTECTION -> "[console] deathprotection buy basic %player_name%";
                    case PROTECTION_POTION -> "[console] deathprotection buy premium %player_name%";
                };
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
                Product product = new Product(id, spec.id(), plain(item.getString("display_name", id)),
                    bundle, price, lore, route, command);
                entries.add(product); products.put(id, product);
            }
            categories.put(spec.id(), new Category(spec.id(), plain(category.getString("display_name", spec.id())),
                category.getStringList("lore"), entries));
        }
        if (products.size() != 21) throw new IllegalArgumentException("Mobile catalog must preserve all 21 products");
        if (offers != null) derive(offers);
    }
    /**
     * Derives the mobile shop from the live spawn catalog, the same data Java browses.
     * The 21 menu products keep their names, lore, order, categories and routes; ordinary
     * products take bundle size and price from the catalog (one price list, no drift),
     * and every catalog offer with no menu product joins the shelves as an ordinary
     * product in its catalog category, or in "More at the Shop" when it names none.
     * Special products are not catalog offers and are never created, repriced or shadowed.
     */
    private void derive(List<SpawnCatalog.OfferData> offers) {
        Map<String, SpawnCatalog.OfferData> byId = new LinkedHashMap<>();
        for (SpawnCatalog.OfferData offer : offers) byId.put(offer.id(), offer);
        for (Product product : List.copyOf(products.values()))
            if (product.route() != Route.ORDINARY && byId.containsKey(product.id()))
                throw new IllegalArgumentException("Catalog offer collides with a protected special product: " + product.id());
        Set<String> legacyOrdinary = new HashSet<>();
        for (Category category : List.copyOf(categories.values())) {
            List<Product> reconciled = new ArrayList<>(category.products().size());
            for (Product product : category.products()) {
                SpawnCatalog.OfferData offer = product.route() == Route.ORDINARY ? byId.get(product.id()) : null;
                if (offer == null) { reconciled.add(product); continue; }
                legacyOrdinary.add(product.id());
                Product priced = new Product(product.id(), product.category(), product.name(),
                    offer.bundleSize(), offer.bundlePrice(), product.lore(), product.route(), product.originalCommand());
                reconciled.add(priced); products.put(priced.id(), priced);
            }
            categories.put(category.id(), new Category(category.id(), category.name(), category.lore(), reconciled));
        }
        Map<String, List<Product>> additions = new LinkedHashMap<>();
        for (SpawnCatalog.OfferData offer : offers) {
            if (legacyOrdinary.contains(offer.id())) continue;
            String categoryId = offer.category() == null ? "more" : offer.category();
            Product product = new Product(offer.id(), categoryId, offer.name(), offer.bundleSize(),
                offer.bundlePrice(), derivedLore(offer), Route.ORDINARY, "[player] excelshop browse " + offer.id());
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

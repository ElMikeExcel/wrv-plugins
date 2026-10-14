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
        });
    }
    MobileCatalog(Function<String, YamlConfiguration> source) {
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

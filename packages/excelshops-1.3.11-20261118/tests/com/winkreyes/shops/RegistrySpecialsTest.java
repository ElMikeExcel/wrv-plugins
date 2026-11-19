// SPDX-License-Identifier: MIT
package com.winkreyes.shops;

import static org.junit.jupiter.api.Assertions.*;

import com.winkreyes.excelitems.core.BuiltinItems;
import com.winkreyes.excelitems.core.ItemDefinition;
import java.io.File;
import java.nio.file.Files;
import java.util.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

/**
 * M4 (ExcelShops -> ExcelItems): the four special products resolve
 * by registry ID from config, name/lore come from the registry
 * definition, delivery is the one registry-resolved console path,
 * and the never-created/repriced/shadowed guard fails closed
 * against registry IDs and keys.
 */
class RegistrySpecialsTest {
    private MobileCatalog catalog() {
        return new MobileCatalog(name -> YamlConfiguration.loadConfiguration(new File("mobile-menus/" + name + ".yml")));
    }
    private List<SpawnCatalog.OfferData> baseOffers() {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(new File("spawn-catalog.yml"));
        ConfigurationSection root = Objects.requireNonNull(yaml.getConfigurationSection("offers"));
        List<SpawnCatalog.OfferData> offers = new ArrayList<>();
        for (String id : root.getKeys(false)) {
            ConfigurationSection s = Objects.requireNonNull(root.getConfigurationSection(id));
            offers.add(new SpawnCatalog.OfferData(id, s.getString("name", id), s.getInt("bundle-size"),
                com.winkreyes.shops.checkout.Money.parse(s.getString("bundle-price", "")), s.getString("category")));
        }
        return offers;
    }

    @Test void catalogInvariantIsTwentyTwoWithFourRegistrySpecials() {
        MobileCatalog catalog = catalog();
        assertEquals(22, catalog.categories().stream().mapToInt(c -> c.products().size()).sum());
        assertEquals(18, catalog.categories().stream().flatMap(c -> c.products().stream())
            .filter(p -> p.route() == MobileCatalog.Route.ORDINARY).count());
        assertEquals(4, catalog.categories().stream().flatMap(c -> c.products().stream())
            .filter(p -> p.route() == MobileCatalog.Route.SPECIAL).count());
        assertEquals(List.of("veil_potion", "deathprotection_basic", "deathpotion", "revive_potion"),
            catalog.category("potions_protection").products().stream().map(MobileCatalog.Product::id).toList());
    }

    @Test void specialsResolveByRegistryIdWithDefinitionNameAndLore() {
        MobileCatalog catalog = catalog();
        Object[][] expected = {
            {"veil_potion", 1, 200000L, "ExcelMobVeil", "mobveil buy Steve"},
            {"deathprotection_basic", 2, 1500000L, "ExcelDeathProtection", "deathprotection buy basic Steve"},
            {"deathpotion", 3, 3000000L, "ExcelDeathProtection", "deathprotection buy premium Steve"},
            {"revive_potion", 4, 500000L, "ExcelDowned", "downed revivepotion buy Steve"},
        };
        for (Object[] row : expected) {
            MobileCatalog.Product product = catalog.product((String) row[0]);
            ItemDefinition definition = BuiltinItems.registry().requireId((Integer) row[1]);
            assertEquals(MobileCatalog.Route.SPECIAL, product.route(), product.id());
            assertEquals(row[1], product.excelitemsId(), product.id());
            assertEquals(definition.key(), catalog.specials().byProduct(product.id()).orElseThrow().registryKey());
            assertEquals(MobileCatalog.plain(definition.displayName()), product.name(), product.id());
            assertEquals(definition.lore(), product.lore(), product.id());
            assertEquals(row[2], product.price(), product.id());
            assertEquals(row[3], product.ownerPlugin(), product.id());
            assertEquals(row[4], product.deliveryCommand("Steve"), product.id());
            assertEquals("[console] " + ((String) row[4]).replace("Steve", "%player_name%"),
                product.originalCommand(), product.id());
        }
        // ID 2 is ACTIVATION in the registry: Shops lists it for
        // activation delivery only; it never mints an item for it.
        assertEquals(ItemDefinition.Kind.ACTIVATION,
            BuiltinItems.registry().requireId(2).kind());
    }

    @Test void pluginYmlHardDependsOnExcelItemsAtNewVersion() throws Exception {
        YamlConfiguration yml = YamlConfiguration.loadConfiguration(new File("plugin.yml"));
        assertEquals("1.3.11", yml.getString("version"));
        assertEquals(List.of("ExcelItems"), yml.getStringList("depend"));
    }

    @Test void configAndBundledResourceListingsAgree() {
        SpecialProducts fromConfig = SpecialProducts.shipped();
        YamlConfiguration resource = YamlConfiguration.loadConfiguration(new File("special-products.yml"));
        SpecialProducts fromResource = SpecialProducts.parse(
            Objects.requireNonNull(resource.getConfigurationSection("special-products")),
            SpecialProducts.builtinLookup());
        assertEquals(fromConfig.listings(), fromResource.listings());
    }

    @Test void catalogOfferCollidingWithAnyRegistrySpecialFailsClosed() {
        for (String clash : List.of("veil_potion", "deathprotection_basic", "deathpotion", "revive_potion")) {
            List<SpawnCatalog.OfferData> offers = new ArrayList<>(baseOffers());
            offers.add(new SpawnCatalog.OfferData(clash, "Fake", 1, 1, null));
            IllegalArgumentException failure = assertThrows(IllegalArgumentException.class, () ->
                new MobileCatalog(name -> YamlConfiguration.loadConfiguration(new File("mobile-menus/" + name + ".yml")), offers),
                clash);
            assertTrue(failure.getMessage().contains("protected special product"), clash);
        }
    }

    @Test void unknownRegistryIdInConfigFailsClosed() {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(new File("config.yml"));
        config.set("special-products.veil_potion.excelitems_id", 99);
        assertThrows(IllegalArgumentException.class, () -> SpecialProducts.parse(
            Objects.requireNonNull(config.getConfigurationSection("special-products")),
            SpecialProducts.builtinLookup()));
    }

    @Test void droppedBaselineListingFailsClosed() {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(new File("config.yml"));
        config.set("special-products.revive_potion", null);
        SpecialProducts specials = SpecialProducts.parse(
            Objects.requireNonNull(config.getConfigurationSection("special-products")),
            SpecialProducts.builtinLookup());
        assertThrows(IllegalArgumentException.class, () -> new MobileCatalog(
            name -> YamlConfiguration.loadConfiguration(new File("mobile-menus/" + name + ".yml")), null, specials));
    }

    @Test void menuCommandDriftAgainstListingFailsClosed() {
        assertThrows(IllegalArgumentException.class, () -> new MobileCatalog(name -> {
            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(new File("mobile-menus/" + name + ".yml"));
            if (name.equals("spawnshop_potions_protection"))
                yaml.set("items.revive_potion.left_click_commands", List.of("[console] downed revivepotion give %player_name%"));
            return yaml;
        }));
    }

    @Test void menuPriceDriftAgainstConfigPriceFailsClosed() {
        assertThrows(IllegalArgumentException.class, () -> new MobileCatalog(name -> {
            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(new File("mobile-menus/" + name + ".yml"));
            if (name.equals("spawnshop_potions_protection")) {
                List<String> lore = new ArrayList<>(yaml.getStringList("items.veil_potion.lore"));
                lore.replaceAll(line -> line.contains("$2,000") ? line.replace("$2,000", "$9,999") : line);
                yaml.set("items.veil_potion.lore", lore);
            }
            return yaml;
        }));
    }

    /**
     * The M4 proof-of-pattern gate, as a desk harness: an
     * already-registered item (registry ID 1) listed in a SECOND
     * category (food) by CONFIG ONLY — one extra listing in the
     * special-products config plus the matching menu entry; the
     * catalog code under test is byte-for-byte the shipped code.
     * The shipped tree is not modified: the modified configs live
     * only in this test's in-memory YAML.
     */
    @Test void configOnlyAdditionDemo_secondCategoryListingResolves() throws Exception {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(new File("config.yml"));
        config.set("special-products.veil_potion_food.category", "food");
        config.set("special-products.veil_potion_food.excelitems_id", 1);
        config.set("special-products.veil_potion_food.price", "2000.00");
        config.set("special-products.veil_potion_food.delivery", "mobveil buy %player_name%");
        config.set("special-products.veil_potion_food.owner", "ExcelMobVeil");
        SpecialProducts specials = SpecialProducts.parse(
            Objects.requireNonNull(config.getConfigurationSection("special-products")),
            SpecialProducts.builtinLookup());
        MobileCatalog demo = new MobileCatalog(name -> {
            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(new File("mobile-menus/" + name + ".yml"));
            if (name.equals("spawnshop_food")) {
                YamlConfiguration potions = YamlConfiguration.loadConfiguration(
                    new File("mobile-menus/spawnshop_potions_protection.yml"));
                ConfigurationSection veil = Objects.requireNonNull(
                    potions.getConfigurationSection("items.veil_potion"));
                for (String key : veil.getKeys(true)) {
                    if (!veil.isConfigurationSection(key)) yaml.set("items.veil_potion_food." + key, veil.get(key));
                }
                yaml.set("items.veil_potion_food.left_click_commands", List.of("[console] mobveil buy %player_name%"));
                yaml.set("items.veil_potion_food.right_click_commands", List.of("[console] mobveil buy %player_name%"));
            }
            return yaml;
        }, null, specials);
        MobileCatalog.Product added = demo.product("veil_potion_food");
        assertEquals("food", added.category());
        assertEquals(1, added.excelitemsId());
        assertEquals(MobileCatalog.Route.SPECIAL, added.route());
        assertEquals("Veil Potion", added.name());
        assertEquals(BuiltinItems.registry().requireId(1).lore(), added.lore());
        assertEquals(200000, added.price());
        assertEquals("mobveil buy Steve", added.deliveryCommand("Steve"));
        assertEquals(23, demo.categories().stream().mapToInt(c -> c.products().size()).sum());
        assertEquals(22, catalog().categories().stream().mapToInt(c -> c.products().size()).sum(),
            "the shipped catalog is unchanged after the demo (demo config was in-memory only)");
        String treeConfig = Files.readString(new File("config.yml").toPath());
        assertFalse(treeConfig.contains("veil_potion_food"), "demo listing must not leak into the shipped config");
    }
}

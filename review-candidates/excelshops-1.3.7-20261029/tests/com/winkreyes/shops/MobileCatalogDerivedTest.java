// Excel signature - WRV maintained source; original authorship and license notices preserved.
// SPDX-License-Identifier: MIT
package com.winkreyes.shops;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.winkreyes.shops.checkout.Money;
import java.io.File;
import java.util.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;

/**
 * The 1.3 derivation: the Bedrock/mobile shop is built from the same spawn
 * catalog data Java browses. Offer data is fed as SpawnCatalog.OfferData
 * records, so these tests run headless; the rig proves the wired plugin.
 */
class MobileCatalogDerivedTest {
    private MobileCatalog legacy() { return new MobileCatalog(name -> YamlConfiguration.loadConfiguration(new File("mobile-menus/" + name + ".yml"))); }
    private MobileCatalog derived(List<SpawnCatalog.OfferData> offers) {
        return new MobileCatalog(name -> YamlConfiguration.loadConfiguration(new File("mobile-menus/" + name + ".yml")), offers);
    }
    /** The 18 live offers, read from the project's staged base catalog (no Material resolution needed). */
    private List<SpawnCatalog.OfferData> baseOffers() {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(new File("spawn-catalog.yml"));
        ConfigurationSection root = Objects.requireNonNull(yaml.getConfigurationSection("offers"));
        List<SpawnCatalog.OfferData> offers = new ArrayList<>();
        for (String id : root.getKeys(false)) {
            ConfigurationSection s = Objects.requireNonNull(root.getConfigurationSection(id));
            offers.add(new SpawnCatalog.OfferData(id, s.getString("name", id), s.getInt("bundle-size"),
                Money.parse(s.getString("bundle-price", "")), s.getString("category")));
        }
        return offers;
    }
    /** Wave A additions exactly as staged in catalog-wave/spawn-catalog.yml (WRV-098). */
    private List<SpawnCatalog.OfferData> waveA() {
        List<SpawnCatalog.OfferData> wave = new ArrayList<>(List.of(
            new SpawnCatalog.OfferData("stone_bricks", "Stone Bricks", 64, 22000, null),
            new SpawnCatalog.OfferData("deepslate_bricks", "Deepslate Bricks", 64, 28000, null),
            new SpawnCatalog.OfferData("deepslate_tiles", "Deepslate Tiles", 64, 32000, null),
            new SpawnCatalog.OfferData("tuff_bricks", "Tuff Bricks", 64, 22000, null),
            new SpawnCatalog.OfferData("light_block", "Light Blocks", 4, 30000, null),
            new SpawnCatalog.OfferData("petrified_slab", "Petrified Oak Slabs", 64, 24000, null)));
        for (String disc : List.of("13", "cat", "blocks", "chirp", "far", "mall", "mellohi", "stal", "strad", "ward", "11", "wait"))
            wave.add(new SpawnCatalog.OfferData("disc_" + disc, "Music Disc " + disc, 1, 50000, null));
        wave.add(new SpawnCatalog.OfferData("disc_5", "Music Disc 5", 1, 65000, null));
        for (String disc : List.of("otherside", "relic", "tears"))
            wave.add(new SpawnCatalog.OfferData("disc_" + disc, "Music Disc " + disc, 1, 90000, null));
        for (String disc : List.of("creator", "creator_music_box", "precipice", "lava_chicken"))
            wave.add(new SpawnCatalog.OfferData("disc_" + disc, "Music Disc " + disc, 1, 100000, null));
        wave.add(new SpawnCatalog.OfferData("disc_pigstep", "Music Disc Pigstep", 1, 120000, null));
        return wave;
    }
    private ShopAdapter.Selection selection(String id, int bundle, long price) {
        ItemStack item = mock(ItemStack.class); when(item.clone()).thenReturn(item);
        return new ShopAdapter.Selection("spawn:" + id, 1, null, id, item, bundle, price, 2304 / bundle, "fixture", null);
    }

    @Test void baseCatalogReconcilesAndMenuDriftSelfHeals() {
        MobileCatalog legacy = legacy(), derived = derived(baseOffers());
        assertEquals(21, derived.categories().stream().mapToInt(c -> c.products().size()).sum());
        assertEquals(List.of("food", "building", "travel_animals", "enchanting", "potions_protection"),
            derived.categories().stream().map(MobileCatalog.Category::id).toList());
        for (MobileCatalog.Category category : legacy.categories())
            assertEquals(category.products().stream().map(MobileCatalog.Product::id).toList(),
                derived.category(category.id()).products().stream().map(MobileCatalog.Product::id).toList());
        // The staged base prices torches at $60 while the bundled menu lore still says $40:
        // the catalog is the one price list now, for both platforms.
        assertEquals(4000, legacy.product("torch").price());
        assertEquals(6000, derived.product("torch").price());
        assertEquals(64, derived.product("torch").bundleSize());
        derived.validate(derived.product("torch"), selection("torch", 64, 6000));
        assertThrows(IllegalArgumentException.class,
            () -> derived.validate(derived.product("torch"), selection("torch", 64, 4000)));
        assertEquals(4000, derived.product("bread").price()); // In-sync prices are unchanged.
    }
    @Test void specialProductsAreByteIdenticalAfterDerivation() {
        MobileCatalog legacy = legacy(), derived = derived(baseOffers());
        for (String id : List.of("veil_potion", "deathprotection_basic", "deathpotion"))
            assertEquals(legacy.product(id), derived.product(id), id);
        assertEquals(1500000, derived.product("deathprotection_basic").price());
        assertEquals("[console] mobveil buy %player_name%", derived.product("veil_potion").originalCommand());
    }
    @Test void waveAOffersAllAppearAtCatalogPrices() {
        List<SpawnCatalog.OfferData> offers = new ArrayList<>(baseOffers()); offers.addAll(waveA());
        assertEquals(45, offers.size());
        MobileCatalog derived = derived(offers);
        assertEquals(48, derived.categories().stream().mapToInt(c -> c.products().size()).sum());
        assertEquals(45, derived.categories().stream().flatMap(c -> c.products().stream())
            .filter(p -> p.route() == MobileCatalog.Route.ORDINARY).count());
        for (SpawnCatalog.OfferData offer : offers) {
            MobileCatalog.Product product = derived.product(offer.id());
            assertEquals(offer.bundlePrice(), product.price(), offer.id());
            assertEquals(offer.bundleSize(), product.bundleSize(), offer.id());
            assertEquals(MobileCatalog.Route.ORDINARY, product.route(), offer.id());
            derived.validate(product, selection(offer.id(), offer.bundleSize(), offer.bundlePrice()));
        }
        assertEquals(120000, derived.product("disc_pigstep").price());
        assertEquals(30000, derived.product("light_block").price());
        assertEquals(4, derived.product("light_block").bundleSize());
        List<MobileCatalog.Category> categories = List.copyOf(derived.categories());
        MobileCatalog.Category more = categories.get(categories.size() - 1);
        assertEquals("more", more.id());
        assertEquals("More at the Shop", more.name());
        assertEquals(waveA().stream().map(SpawnCatalog.OfferData::id).toList(),
            more.products().stream().map(MobileCatalog.Product::id).toList());
    }
    @Test void catalogCategoryRoutesOffersAndCreatesShelves() {
        List<SpawnCatalog.OfferData> offers = new ArrayList<>(baseOffers());
        offers.add(new SpawnCatalog.OfferData("honey", "Honey Bottles", 4, 12000, "food"));
        offers.add(new SpawnCatalog.OfferData("disc_bounce", "Music Disc Bounce", 1, 90000, "music_discs"));
        MobileCatalog derived = derived(offers);
        assertEquals(List.of("food", "building", "travel_animals", "enchanting", "potions_protection", "music_discs"),
            derived.categories().stream().map(MobileCatalog.Category::id).toList());
        MobileCatalog.Category food = derived.category("food");
        assertEquals("honey", food.products().get(food.products().size() - 1).id());
        assertEquals("Music Discs", derived.category("music_discs").name());
        assertThrows(NullPointerException.class, () -> derived.category("more")); // No catch-all when unused.
    }
    @Test void catalogOfferCannotShadowASpecialProduct() {
        List<SpawnCatalog.OfferData> offers = new ArrayList<>(baseOffers());
        offers.add(new SpawnCatalog.OfferData("veil_potion", "Fake Veil", 1, 1, null));
        IllegalArgumentException failure = assertThrows(IllegalArgumentException.class, () -> derived(offers));
        assertTrue(failure.getMessage().contains("protected special product"));
    }
    @Test void legacyProductMissingFromCatalogKeepsItsMenuContract() {
        List<SpawnCatalog.OfferData> offers = new ArrayList<>(baseOffers());
        offers.removeIf(o -> o.id().equals("water"));
        MobileCatalog derived = derived(offers);
        assertEquals(legacy().product("water"), derived.product("water"));
        assertEquals(21, derived.categories().stream().mapToInt(c -> c.products().size()).sum());
    }
    @Test void largeCatalogKeepsFormIndexContractPastFortyFive() {
        List<SpawnCatalog.OfferData> offers = new ArrayList<>(baseOffers());
        for (int i = 0; i < 102; i++)
            offers.add(new SpawnCatalog.OfferData(String.format("bulk_%03d", i), "Bulk " + i, 1, 100 + i, null));
        MobileCatalog derived = derived(offers);
        MobileCatalog.Category more = derived.category("more");
        assertEquals(102, more.products().size());
        // ShopUi builds one form button per product index, then Back/Cancel: index i must be product i.
        for (int i = 0; i < more.products().size(); i++)
            assertEquals(String.format("bulk_%03d", i), more.products().get(i).id());
        assertEquals("bulk_101", derived.product("bulk_101").id());
        assertEquals(123, derived.categories().stream().mapToInt(c -> c.products().size()).sum());
    }
    @Test void derivedLoreCarriesTheCatalogPriceInMenuStyle() {
        MobileCatalog derived = derived(waveA());
        assertTrue(derived.product("disc_pigstep").lore().stream().anyMatch(l -> l.contains("Price:") && l.contains("$1,200")));
        assertTrue(derived.product("stone_bricks").lore().stream().anyMatch(l -> l.contains("Bundle price:") && l.contains("$220")));
        assertEquals("1,200", MobileCatalog.formatDollars(120000));
        assertEquals("2.50", MobileCatalog.formatDollars(250));
        assertEquals("15,000", MobileCatalog.formatDollars(1500000));
    }
}

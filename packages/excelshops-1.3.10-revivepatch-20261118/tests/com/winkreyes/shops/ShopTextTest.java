// SPDX-License-Identifier: MIT
package com.winkreyes.shops;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;

/** Wording edge cases for ExcelShops 1.3. Display text only; prices and limits are unchanged. */
class ShopTextTest {
    private static MobileCatalog.Product product(String name, int bundle, long price, MobileCatalog.Route route) {
        return new MobileCatalog.Product("x", "c", name, bundle, price, List.of("Price: $1"), route, "cmd");
    }
    private static MobileCatalog.Product ordinary(String name, int bundle, long price) {
        return product(name, bundle, price, MobileCatalog.Route.ORDINARY);
    }

    @Test void stackWording() {
        MobileCatalog.Product torch = ordinary("64 Torches", 64, 4000);
        assertEquals("Buy 1 stack (64 Torches)", ShopText.buyPhrase(torch, 1));
        assertEquals("Buy 2 stacks (128 Torches)", ShopText.buyPhrase(torch, 2));
        assertEquals("Buy 36 stacks (2,304 Torches)", ShopText.buyPhrase(torch, 36));
    }
    @Test void packWording() {
        MobileCatalog.Product bread = ordinary("16 Bread", 16, 4000);
        assertEquals("Buy 16 Bread", ShopText.buyPhrase(bread, 1));
        assertEquals("Buy 3 packs (48 Bread)", ShopText.buyPhrase(bread, 3));
        assertEquals("Buy 144 packs (2,304 Bread)", ShopText.buyPhrase(bread, 144));
        assertEquals("Buy 2 Leads", ShopText.buyPhrase(ordinary("2 Leads", 2, 15000), 1));
        assertEquals("Buy 4 Ender Pearls", ShopText.buyPhrase(ordinary("4 Ender Pearls", 4, 40000), 1));
        assertEquals("Buy 3 packs (6 Leads)", ShopText.buyPhrase(ordinary("2 Leads", 2, 15000), 3));
    }
    @Test void singlesAndPluralHandling() {
        MobileCatalog.Product apple = ordinary("Golden Apple", 1, 30000);
        assertEquals("Buy 1 Golden Apple", ShopText.buyPhrase(apple, 1));
        assertEquals("Buy 2 Golden Apples", ShopText.buyPhrase(apple, 2));
        assertEquals("Buy 3 Saddles", ShopText.buyPhrase(ordinary("Saddle", 1, 40000), 3));
        assertEquals("Buy 1 Saddle", ShopText.buyPhrase(ordinary("Saddle", 1, 40000), 1));
        assertEquals("Buy 2304 Name Tags".replace("2304", "2,304"), ShopText.buyPhrase(ordinary("Name Tag", 1, 35000), 2304));
        assertEquals("Buy 5 Efficiency IV Books", ShopText.buyPhrase(ordinary("Efficiency IV Book", 1, 45000), 5));
    }
    @Test void uncountablesKeepTheirWord() {
        assertEquals("Buy 1 stack (64 Glass)", ShopText.buyPhrase(ordinary("64 Glass", 64, 18000), 1));
        assertEquals("Buy 2 stacks (128 Glass)", ShopText.buyPhrase(ordinary("64 Glass", 64, 18000), 2));
        assertEquals("Buy 2 packs (32 Steak)", ShopText.buyPhrase(ordinary("16 Steak", 16, 8000), 2));
    }
    @Test void singularPluralLookupWorksFromEitherForm() {
        assertEquals("Torch", ShopText.noun("Torches", false));
        assertEquals("Torches", ShopText.noun("Torch", true));
        assertEquals("Bottle o' Enchanting", ShopText.noun("Bottles o' Enchanting", false));
        assertEquals("Bread", ShopText.noun("Bread", false));
        assertEquals("Mystery thing", ShopText.noun("Mystery thing", true));
    }
    @Test void everyCatalogProductBuildsWithoutLeakingBundleWords() {
        for (String[] row : new String[][] {{"16 Bread","16"},{"16 Steak","16"},{"16 Golden Carrots","16"},{"Golden Apple","1"},
                {"64 Torches","64"},{"64 Oak Logs","64"},{"64 Glass","64"},{"16 Iron Ingots","16"},{"Water Bucket","1"},{"Saddle","1"},
                {"Name Tag","1"},{"2 Leads","2"},{"4 Ender Pearls","4"},{"16 Fireworks","16"},{"16 Bottles o' Enchanting","16"},
                {"Mending Book","1"},{"Unbreaking III Book","1"},{"Efficiency IV Book","1"}}) {
            MobileCatalog.Product p = ordinary(row[0], Integer.parseInt(row[1]), 12345);
            for (int n : new int[] {1, 2, 7}) {
                String text = ShopText.buyPhrase(p, n);
                assertFalse(text.contains("bundle") || text.contains("×") || text.contains("—") || text.contains("(s)"), text);
                assertTrue(text.startsWith("Buy "), text);
            }
            assertFalse(ShopText.listLabel(p).contains("—"));
        }
    }
    @Test void listLabelPutsSizeAndPriceOnTheButton() {
        assertEquals("64 Torches (1 stack)\n$40", ShopText.listLabel(ordinary("64 Torches", 64, 4000)));
        assertEquals("16 Bread\n$40", ShopText.listLabel(ordinary("16 Bread", 16, 4000)));
        assertEquals("Golden Apple\n$300", ShopText.listLabel(ordinary("Golden Apple", 1, 30000)));
        assertEquals("4 Ender Pearls\n$400", ShopText.listLabel(ordinary("4 Ender Pearls", 4, 40000)));
        assertEquals("Veil Potion\n$2,000", ShopText.listLabel(product("Veil Potion", 1, 200000, MobileCatalog.Route.VEIL)));
    }
    @Test void priceDropsDotZeroZeroAndKeepsCommasAndRealCents() {
        assertEquals("$40", ShopText.price(4000));
        assertEquals("$0", ShopText.price(0));
        assertEquals("$0.50", ShopText.price(50));
        assertEquals("$0.05", ShopText.price(5));
        assertEquals("$1.25", ShopText.price(125));
        assertEquals("$2,000", ShopText.price(200000));
        assertEquals("$15,000", ShopText.price(1500000));
        assertEquals("$30,000", ShopText.price(3000000));
        assertEquals("$2,560", ShopText.price(256000));
        assertEquals("$1,234,567.89", ShopText.price(123456789));
        assertEquals("$100,000,000", ShopText.price(10000000000L));
        assertEquals("-$3.50", ShopText.price(-350));
    }
    @Test void zeroAndNegativeQuantitiesAreRejected() {
        MobileCatalog.Product torch = ordinary("64 Torches", 64, 4000);
        assertThrows(IllegalArgumentException.class, () -> ShopText.buyPhrase(torch, 0));
        assertThrows(IllegalArgumentException.class, () -> ShopText.buyPhrase(torch, -1));
        assertThrows(IllegalArgumentException.class, () -> ShopText.quantity("Bread", 16, -5));
        assertThrows(IllegalArgumentException.class, () -> ShopText.quantity("Bread", 0, 1));
    }
    @Test void unitWords() {
        assertEquals("each", ShopText.unit(1));
        assertEquals("per stack", ShopText.unit(64));
        assertEquals("per pack of 16", ShopText.unit(16));
    }
    @Test void typedQuantityLimitsAreUnchanged() {
        assertEquals(2304, 36 * 64); // 36 stacks is the item cap for 64-bundles
        assertEquals(144, 2304 / 16);
        assertThrows(IllegalArgumentException.class, () -> MobileCatalog.quantity("0", 36));
        assertThrows(IllegalArgumentException.class, () -> MobileCatalog.quantity("-1", 36));
        assertThrows(IllegalArgumentException.class, () -> MobileCatalog.quantity("37", 36));
        assertEquals(36, MobileCatalog.quantity("36", 36));
    }
}

// Excel signature - WRV maintained source; original authorship and license notices preserved.
// SPDX-License-Identifier: MIT
package com.winkreyes.shops;

import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Player-facing wording helpers for the shop buttons and screens (ExcelShops 1.3.1).
 * Display only: never used for prices, quantities, or authorization.
 */
public final class ShopText {
    private ShopText() {}

    /** Items in a full Minecraft stack. Bundles of exactly this size are called "stacks". */
    static final int STACK = 64;

    /** Known catalog names: {singular, plural}. Uncountables repeat the same word. */
    private static final Map<String, String[]> FORMS = new HashMap<>();
    private static void form(String singular, String plural) {
        String[] pair = {singular, plural};
        FORMS.put(singular.toLowerCase(Locale.ROOT), pair);
        FORMS.put(plural.toLowerCase(Locale.ROOT), pair);
    }
    static {
        form("Torch", "Torches"); form("Oak Log", "Oak Logs"); form("Glass", "Glass");
        form("Bread", "Bread"); form("Steak", "Steak");
        form("Golden Carrot", "Golden Carrots"); form("Golden Apple", "Golden Apples");
        form("Iron Ingot", "Iron Ingots"); form("Water Bucket", "Water Buckets");
        form("Saddle", "Saddles"); form("Name Tag", "Name Tags"); form("Lead", "Leads");
        form("Ender Pearl", "Ender Pearls"); form("Firework", "Fireworks");
        form("Bottle o' Enchanting", "Bottles o' Enchanting");
        form("Mending Book", "Mending Books"); form("Unbreaking III Book", "Unbreaking III Books");
        form("Efficiency IV Book", "Efficiency IV Books");
        form("Veil Potion", "Veil Potions"); form("Death Protection", "Death Protection");
        form("Death Protection Potion", "Death Protection Potions");
    }

    /** Whole dollars lose the ".00"; cents stay only when non-zero. Thousands use commas. */
    public static String price(long cents) {
        String sign = cents < 0 ? "-" : "";
        long abs = Math.abs(cents);
        NumberFormat dollars = NumberFormat.getIntegerInstance(Locale.US);
        String text = sign + "$" + dollars.format(abs / 100);
        return abs % 100 == 0 ? text : text + "." + String.format(Locale.US, "%02d", abs % 100);
    }

    /** Singular or plural form of a known catalog name; unknown names are returned unchanged. */
    public static String noun(String name, boolean plural) {
        String bare = name.startsWith("+") ? name.substring(1) : name;
        String[] pair = FORMS.get(bare.toLowerCase(Locale.ROOT));
        return pair == null ? name : pair[plural ? 1 : 0];
    }

    /** "1 stack (64 Torches)", "2 stacks (128 Torches)", "16 Bread", "3 packs (48 Bread)", "1 Golden Apple". */
    public static String quantity(String name, int bundleSize, int bundles) {
        if (bundleSize < 1) throw new IllegalArgumentException("Bundle size must be at least 1.");
        if (bundles < 1) throw new IllegalArgumentException("Quantity must be at least 1.");
        long total = Math.multiplyExact((long) bundles, (long) bundleSize);
        NumberFormat count = NumberFormat.getIntegerInstance(Locale.US);
        if (bundleSize == 1) return count.format(total) + " " + noun(name, total != 1);
        String items = count.format(total) + " " + noun(name, true);
        if (bundleSize == STACK) return count.format(bundles) + (bundles == 1 ? " stack (" : " stacks (") + items + ")";
        if (bundles == 1) return items;
        return count.format(bundles) + " packs (" + items + ")";
    }

    /** The word for one bundle in price lines: "stack", "pack", or "each". */
    public static String unit(int bundleSize) {
        return bundleSize == 1 ? "each" : bundleSize == STACK ? "per stack" : "per pack of " + bundleSize;
    }

    /** Central buy wording: "Buy 1 stack (64 Torches)". Callers add " for $X". */
    public static String buyPhrase(MobileCatalog.Product product, int bundles) {
        return "Buy " + quantity(MobileCatalog.labelName(product), product.bundleSize(), bundles);
    }

    /** Same wording for a trusted catalog selection (name and bundle size). */
    public static String buyPhrase(String name, int bundleSize, int bundles) {
        return "Buy " + quantity(name, bundleSize, bundles);
    }

    /** Two-line list label: item and size on line one, price on line two. */
    public static String listLabel(MobileCatalog.Product product) {
        String name = MobileCatalog.labelName(product);
        if (product.route() != MobileCatalog.Route.ORDINARY || product.bundleSize() == 1)
            return product.name() + "\n" + price(product.price());
        String items = quantity(name, product.bundleSize(), 1);
        if (product.bundleSize() == STACK) items = product.bundleSize() + " " + noun(name, true) + " (1 stack)";
        return items + "\n" + price(product.price());
    }
}

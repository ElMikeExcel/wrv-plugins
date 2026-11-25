package excelemfixes.fieldmedic;

import java.util.List;
import java.util.Optional;

/**
 * The buy flows' goods table — pure data, no Bukkit (materials
 * are names; the Bukkit layer resolves them at delivery). Two
 * command surfaces share this one table and one purchase engine
 * ({@link VendorBuyFlow}):
 *
 * <ul>
 *   <li>{@code /fieldmedic buy} sells the Strong Healing Potion
 *       — a registry item (ExcelItems ID 7, minted by the
 *       registry at delivery) — at the ExcelShops list price
 *       (SHOP venue) or the guild vendor price (NPC venue). The
 *       ExcelShops special-products listing's delivery command
 *       is {@code fieldmedic buy %player_name%}; the vendor
 *       NPC's interaction runs {@code fieldmedic buy
 *       %player_name% npc}.</li>
 *   <li>{@code /guildvendor buy <good>} is the generic vendor
 *       flow for plain goods the guild vendor sells at the
 *       standing 25% vendor discount (D200): ender pearls x16
 *       and the empty map. EliteMobs' own NPC shops are
 *       engine-driven menus with no per-item price files, so a
 *       discounted fixed listing for a vanilla item cannot be
 *       expressed in EliteMobs content — the NPC interaction
 *       runs this console command instead (the pattern the
 *       summon-mount module proved with {@code mountcharm buy
 *       %player_name% npc}). NPC venue only: at list price these
 *       goods are ordinary ExcelShops catalog offers, already
 *       sold by the shop itself.</li>
 * </ul>
 *
 * Prices are config facts of this module (the ExcelShops
 * listing carries the potion's $150 as a shop fact too, and the
 * shop menu refuses to load on a mismatch — the two must agree).
 * Registry IDs and material names are pinned here, never config:
 * a knob that could move them could silently re-point a sale.
 */
public final class VendorGoods {

    /** Where a purchase was initiated: the shop, or the guild vendor NPC. */
    public enum Venue { SHOP, NPC }

    /** How a good is minted at delivery time. */
    public enum Delivery {
        /** Minted through the ExcelItems registry by permanent ID. */
        REGISTRY_ITEM,
        /** A plain vanilla stack, material + amount. */
        VANILLA_STACK
    }

    /**
     * One sellable good. {@code registryId} is meaningful only
     * for REGISTRY_ITEM goods; {@code material}/{@code amount}
     * only for VANILLA_STACK goods. A venue flag says whether
     * the good is offered there at all (price lookup returns -1
     * for an unoffered venue).
     */
    public record Good(String key, String displayName, Delivery delivery,
                       int registryId, String material, int amount,
                       boolean shopVenue, boolean npcVenue) {}

    /** The Strong Healing Potion (ExcelItems ID 7), both venues. */
    public static final Good POTION = new Good(
            "potion", "Splash Potion of Strong Healing",
            Delivery.REGISTRY_ITEM, PotionItem.REGISTRY_ID, null, 1,
            true, true);

    /** Ender Pearls x16, guild vendor only ($300 = 25% under the $400 list). */
    public static final Good PEARL = new Good(
            "pearl", "16 Ender Pearls",
            Delivery.VANILLA_STACK, 0, "ENDER_PEARL", 16,
            false, true);

    /** Empty Map x1, guild vendor only ($75 = 25% under the $100 list). */
    public static final Good EMPTYMAP = new Good(
            "emptymap", "Empty Map",
            Delivery.VANILLA_STACK, 0, "MAP", 1,
            false, true);

    private static final List<Good> ALL = List.of(POTION, PEARL, EMPTYMAP);

    public static List<Good> all() {
        return ALL;
    }

    public static Optional<Good> byKey(String key) {
        if (key == null) return Optional.empty();
        String wanted = key.toLowerCase(java.util.Locale.ROOT);
        return ALL.stream().filter(g -> g.key().equals(wanted)).findFirst();
    }

    /**
     * The four prices, read from the module's config section.
     * Defaults are the D195/D200 batch figures: potion $150 list
     * / $112.50 vendor; pearls $300 vendor (list $400); empty
     * map $75 vendor (list $100). Every price must be finite and
     * non-negative — a corrupt knob fails the whole record at
     * construction, and the caller falls back to these defaults
     * rather than selling at a garbage price.
     */
    public record Prices(double potionShop, double potionNpc,
                         double pearlNpc, double emptymapNpc) {

        public static final Prices DEFAULTS = new Prices(150.0, 112.5, 300.0, 75.0);

        public Prices {
            requirePrice(potionShop, "potionShop");
            requirePrice(potionNpc, "potionNpc");
            requirePrice(pearlNpc, "pearlNpc");
            requirePrice(emptymapNpc, "emptymapNpc");
        }

        private static void requirePrice(double price, String name) {
            if (!Double.isFinite(price) || price < 0.0 || price > 1_000_000.0) {
                throw new IllegalArgumentException(
                        name + " must be a finite price in [0, 1000000]: " + price);
            }
        }
    }

    /**
     * The price of one good at one venue, or -1 when the good is
     * not offered at that venue (the flow refuses the arm rather
     * than inventing a price).
     */
    public static double price(Good good, Venue venue, Prices prices) {
        return switch (good.key()) {
            case "potion" -> venue == Venue.NPC
                    ? (good.npcVenue() ? prices.potionNpc() : -1.0)
                    : (good.shopVenue() ? prices.potionShop() : -1.0);
            case "pearl" -> venue == Venue.NPC && good.npcVenue()
                    ? prices.pearlNpc() : -1.0;
            case "emptymap" -> venue == Venue.NPC && good.npcVenue()
                    ? prices.emptymapNpc() : -1.0;
            default -> -1.0;
        };
    }

    private VendorGoods() {}
}

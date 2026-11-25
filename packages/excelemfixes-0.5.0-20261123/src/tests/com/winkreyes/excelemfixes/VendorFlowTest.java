import com.winkreyes.excelitems.core.BuiltinItems;
import com.winkreyes.excelitems.core.ItemDefinition;
import com.winkreyes.excelitems.core.ItemRegistry;
import excelemfixes.fieldmedic.PotionItem;
import excelemfixes.fieldmedic.VendorArms;
import excelemfixes.fieldmedic.VendorGoods;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Vendor buy-flow suites: the Strong Healing Potion's registry
 * contract (definition, registration into the real ExcelItems
 * registry classes beside the bandage, and the 0.1.0
 * config-validation trap that forbids an ExcelItems config line
 * for ID 7), the goods/price table (D200 venue math), and the
 * pure arm ledger the two purchase commands share (snapshot,
 * replace, expiry).
 */
public class VendorFlowTest {

    private static final UUID PLAYER = UUID.fromString("00000000-0000-0000-0000-00000000b001");
    private static final long T0 = 1_700_000_000_000L;

    private static boolean throwsIAE(Runnable r) {
        try {
            r.run();
            return false;
        } catch (IllegalArgumentException expected) {
            return true;
        }
    }

    public static void run() {
        System.out.println("VendorFlowTest:");
        definition();
        registryContract();
        goodsAndPrices();
        armLedger();
    }

    // ------------------------------------------------------------ definition

    private static void definition() {
        ItemDefinition def = PotionItem.DEFINITION;
        Check.eq(7, def.id(), "potion registry id is 7");
        Check.eq("strong_healing_potion", def.key(), "potion registry key");
        Check.eq(ItemDefinition.Kind.ITEM, def.kind(), "potion is a physical item");
        Check.eq("SPLASH_POTION", def.material(), "potion material is SPLASH_POTION");
        Check.eq("STRONG_HEALING", def.basePotionType(),
                "potion base type is STRONG_HEALING (Instant Health II, not a dud)");
        Check.eq("excelemfixes:field-medic", def.handlerId(), "potion handler label");
        Check.ok(def.lore().stream().anyMatch(l -> l.contains("$150")),
                "potion lore carries the $150 shop line");
        Check.ok(def.lore().stream().anyMatch(l -> l.contains("$112.50")),
                "potion lore carries the $112.50 vendor line");
        Check.ok(!def.hasLegacyMark(), "potion has no legacy mark (born in the registry era)");
    }

    // ------------------------------------------------------------ registry

    private static void registryContract() {
        // The builtins hold 1-4; the bandage takes 6, the potion 7.
        ItemRegistry registry = BuiltinItems.registry();
        registry.register(excelemfixes.fieldmedic.BandageItem.DEFINITION);
        registry.register(PotionItem.DEFINITION);
        Check.eq(6, registry.size(), "builtins + bandage + potion = 6 definitions");
        Check.eq("strong_healing_potion", registry.requireId(7).key(),
                "id 7 resolves to the potion");
        Check.eq("field_bandage", registry.requireId(6).key(),
                "id 6 still resolves to the bandage");
        Check.eq(8, registry.nextId(), "next free id after 1-7 is 8");
        Check.ok(throwsIAE(() -> registry.register(new ItemDefinition(
                        7, "other_item", "&fOther", "SPLASH_POTION", null, null, false,
                        List.of(), ItemDefinition.Kind.ITEM, null, null, null, 0, "x:y"))),
                "a second claim on id 7 is rejected");
        Check.ok(throwsIAE(() -> registry.register(new ItemDefinition(
                        9, "strong_healing_potion", "&fClone", "SPLASH_POTION", null, null,
                        false, List.of(), ItemDefinition.Kind.ITEM, null, null, null, 0, "x:y"))),
                "a second claim on the potion key is rejected");

        // THE 0.1.0 TRAP, pinned for ID 7 (same trap as the
        // bandage's): ExcelItems validates its config assignments
        // at ITS enable, when the registry holds only the
        // builtins — so a "7: strong_healing_potion" config line
        // reads as an unknown ID and would disable ExcelItems.
        // This is why the packet ships NO ExcelItems config line.
        ItemRegistry builtinsOnly = BuiltinItems.registry();
        List<String> problems = builtinsOnly.validateAssignments(Map.of(
                "1", "veil_potion",
                "2", "deathprotection_basic",
                "3", "deathpotion",
                "4", "revive_potion",
                "7", "strong_healing_potion"));
        Check.eq(1, problems.size(), "a config line for id 7 produces exactly one problem");
        Check.ok(problems.get(0).contains("unknown id 7"),
                "the problem is the unknown-id trap: " + problems.get(0));
    }

    // ------------------------------------------------------------ goods + prices

    private static void goodsAndPrices() {
        VendorGoods.Prices prices = VendorGoods.Prices.DEFAULTS;
        Check.close(150.0, VendorGoods.price(VendorGoods.POTION,
                VendorGoods.Venue.SHOP, prices), "potion shop price is the $150 list");
        Check.close(112.5, VendorGoods.price(VendorGoods.POTION,
                VendorGoods.Venue.NPC, prices), "potion vendor price is $112.50 (25% off, D200)");
        Check.close(300.0, VendorGoods.price(VendorGoods.PEARL,
                VendorGoods.Venue.NPC, prices), "pearls vendor price is $300 (25% off $400)");
        Check.close(75.0, VendorGoods.price(VendorGoods.EMPTYMAP,
                VendorGoods.Venue.NPC, prices), "empty map vendor price is $75 (25% off $100)");
        Check.close(-1.0, VendorGoods.price(VendorGoods.PEARL,
                VendorGoods.Venue.SHOP, prices), "pearls are not sold at shop venue by this flow");
        Check.close(-1.0, VendorGoods.price(VendorGoods.EMPTYMAP,
                VendorGoods.Venue.SHOP, prices), "empty map is not sold at shop venue by this flow");

        Check.eq(VendorGoods.POTION, VendorGoods.byKey("potion").orElse(null),
                "good lookup: potion");
        Check.eq(VendorGoods.PEARL, VendorGoods.byKey("PEARL").orElse(null),
                "good lookup is case-insensitive");
        Check.eq(VendorGoods.EMPTYMAP, VendorGoods.byKey("emptymap").orElse(null),
                "good lookup: emptymap");
        Check.ok(VendorGoods.byKey("nonsense").isEmpty(), "unknown good resolves empty");
        Check.ok(VendorGoods.byKey(null).isEmpty(), "null good resolves empty");

        Check.eq(16, VendorGoods.PEARL.amount(), "pearl bundle is a stack of 16");
        Check.eq("ENDER_PEARL", VendorGoods.PEARL.material(), "pearl material");
        Check.eq(1, VendorGoods.EMPTYMAP.amount(), "empty map sells one at a time");
        Check.eq("MAP", VendorGoods.EMPTYMAP.material(), "empty map material is MAP");
        Check.eq(VendorGoods.Delivery.REGISTRY_ITEM, VendorGoods.POTION.delivery(),
                "potion delivery is a registry mint");
        Check.eq(7, VendorGoods.POTION.registryId(), "potion good points at registry id 7");

        Check.ok(throwsIAE(() -> new VendorGoods.Prices(-1.0, 112.5, 300.0, 75.0)),
                "negative price rejected");
        Check.ok(throwsIAE(() -> new VendorGoods.Prices(150.0, Double.NaN, 300.0, 75.0)),
                "NaN price rejected");
        VendorGoods.Prices tuned = new VendorGoods.Prices(200.0, 150.0, 320.0, 80.0);
        Check.close(200.0, VendorGoods.price(VendorGoods.POTION,
                VendorGoods.Venue.SHOP, tuned), "config prices drive the table");
    }

    // ------------------------------------------------------------ arm ledger

    private static void armLedger() {
        VendorArms arms = new VendorArms();
        Check.ok(arms.peek(PLAYER, T0) == null, "no review armed at first");
        Check.ok(arms.consume(PLAYER, T0) == null, "confirm with nothing armed is NOT_ARMED");

        VendorArms.Arm arm = arms.arm(PLAYER, VendorGoods.POTION,
                VendorGoods.Venue.SHOP, 150.0, T0);
        Check.eq("potion", arm.goodKey(), "armed review names the good");
        Check.close(150.0, arm.price(), "armed review snapshots the price");
        Check.eq(arm, arms.peek(PLAYER, T0 + 29_999L), "review live inside its window");
        Check.ok(arms.peek(PLAYER, T0 + 30_001L) == null, "review expires after 30 s");
        Check.ok(arms.consume(PLAYER, T0 + 30_001L) == null,
                "an expired review cannot be confirmed");

        // Re-arming replaces: one live review per player, latest wins.
        arms.arm(PLAYER, VendorGoods.POTION, VendorGoods.Venue.SHOP, 150.0, T0);
        VendorArms.Arm replaced = arms.arm(PLAYER, VendorGoods.PEARL,
                VendorGoods.Venue.NPC, 300.0, T0 + 1000L);
        Check.eq(replaced, arms.peek(PLAYER, T0 + 1000L),
                "arming a second review replaces the first");
        Check.eq("pearl", arms.peek(PLAYER, T0 + 1000L).goodKey(),
                "the replacement is the live review");

        // Consume takes it exactly once.
        Check.eq(replaced, arms.consume(PLAYER, T0 + 2000L), "consume returns the review");
        Check.ok(arms.consume(PLAYER, T0 + 2000L) == null,
                "a consumed review cannot be confirmed twice");
        Check.ok(arms.peek(PLAYER, T0 + 2000L) == null, "nothing armed after consume");

        arms.arm(PLAYER, VendorGoods.EMPTYMAP, VendorGoods.Venue.NPC, 75.0, T0);
        arms.forget(PLAYER);
        Check.ok(arms.peek(PLAYER, T0) == null, "forget drops the review");
    }
}

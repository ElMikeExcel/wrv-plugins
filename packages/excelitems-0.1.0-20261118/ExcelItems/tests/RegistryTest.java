import com.winkreyes.excelitems.core.BuiltinItems;
import com.winkreyes.excelitems.core.ItemDefinition;
import com.winkreyes.excelitems.core.ItemRegistry;

import java.util.List;
import java.util.NoSuchElementException;

/**
 * Register / look-up / identify round-trips, duplicate rejection and
 * unknown handling — the task's named coverage. "Create" at this
 * layer is the definition-to-identity round-trip: a definition is
 * registered, fetched back by ID and by key, and an item carrying
 * only its marks (primary or legacy) identifies back to it. The
 * Bukkit mint itself is exercised on the rig, not the JVM suite.
 */
public final class RegistryTest {

    private static ItemDefinition custom(int id, String key) {
        return new ItemDefinition(id, key, "&f" + key, "STONE", null, null, false,
                List.of(), ItemDefinition.Kind.ITEM, null, null, null, 0, "test:handler");
    }

    private static boolean throwsIAE(Runnable r) {
        try { r.run(); return false; } catch (IllegalArgumentException e) { return true; }
    }

    public static void run() {
        System.out.println("RegistryTest:");

        // --- register + create/identify round-trip (builtins) ---
        ItemRegistry reg = BuiltinItems.registry();
        Check.eq(4, reg.size(), "builtin registry holds exactly 4 items");
        for (ItemDefinition def : BuiltinItems.ALL) {
            Check.eq(def, reg.byId(def.id()).orElse(null), "round-trip byId " + def.id());
            Check.eq(def, reg.byKey(def.key()).orElse(null), "round-trip byKey " + def.key());
            Check.eq(def, reg.identify(def.id(), null, null).orElse(null),
                    "identify by primary mark " + def.key());
            if (def.hasLegacyMark()) {
                Check.eq(def, reg.identify(null, def.legacyNamespace(), def.legacyKey()).orElse(null),
                        "identify by legacy mark " + def.key());
                Check.eq(def, reg.identifyLegacy(def.legacyNamespace(), def.legacyKey()).orElse(null),
                        "identifyLegacy " + def.key());
            }
        }
        Check.eq(5, reg.nextId(), "next free id after builtins is 5");

        // --- registration of a new item, then round-trip ---
        ItemRegistry fresh = new ItemRegistry();
        Check.eq(1, fresh.nextId(), "empty registry next id is 1");
        ItemDefinition a = custom(7, "test_widget");
        fresh.register(a);
        Check.eq(a, fresh.byId(7).orElse(null), "new item round-trip byId");
        Check.eq(a, fresh.byKey("test_widget").orElse(null), "new item round-trip byKey");
        Check.eq(8, fresh.nextId(), "next id follows the max, not the count");

        // --- duplicate-ID / duplicate-key rejection ---
        Check.ok(throwsIAE(() -> fresh.register(custom(7, "other_key"))), "duplicate id rejected");
        Check.ok(throwsIAE(() -> fresh.register(custom(9, "test_widget"))), "duplicate key rejected");
        Check.eq(1, fresh.size(), "rejected duplicates changed nothing");
        ItemDefinition legacyA = new ItemDefinition(1, "legacy_a", "&fA", "POTION", null, null,
                false, List.of(), ItemDefinition.Kind.ITEM, "excelmobveil", "potion", null, 0, "t:a");
        ItemDefinition legacyB = new ItemDefinition(2, "legacy_b", "&fB", "POTION", null, null,
                false, List.of(), ItemDefinition.Kind.ITEM, "excelmobveil", "potion", null, 0, "t:b");
        ItemRegistry legacyReg = new ItemRegistry();
        legacyReg.register(legacyA);
        Check.ok(throwsIAE(() -> legacyReg.register(legacyB)), "duplicate legacy mark rejected");

        // --- definition validation ---
        Check.ok(throwsIAE(() -> custom(0, "zero_id")), "id 0 rejected");
        Check.ok(throwsIAE(() -> custom(-3, "neg_id")), "negative id rejected");
        Check.ok(throwsIAE(() -> custom(1, "Bad Key")), "malformed key rejected");
        Check.ok(throwsIAE(() -> new ItemDefinition(1, "half_legacy", "&fX", "STONE", null, null,
                false, List.of(), ItemDefinition.Kind.ITEM, "excelmobveil", null, null, 0, "t:x")),
                "half a legacy mark rejected");

        // --- unknown-ID / unknown-key handling ---
        Check.ok(reg.byId(999).isEmpty(), "unknown id looks up empty");
        Check.ok(reg.byKey("no_such_item").isEmpty(), "unknown key looks up empty");
        Check.ok(reg.byKey(null).isEmpty(), "null key looks up empty");
        Check.ok(reg.identify(999, null, null).isEmpty(), "unknown primary mark identifies empty");
        Check.ok(reg.identify(null, "excelmobveil", "nope").isEmpty(), "unknown legacy key identifies empty");
        Check.ok(reg.identify(null, "no_plugin", "potion").isEmpty(), "unknown legacy namespace identifies empty");
        Check.ok(reg.identify(null, null, null).isEmpty(), "no marks identifies empty");
        boolean threw = false;
        try { reg.requireId(999); } catch (NoSuchElementException e) { threw = true; }
        Check.ok(threw, "requireId on unknown id throws");
        threw = false;
        try { reg.requireKey("no_such_item"); } catch (NoSuchElementException e) { threw = true; }
        Check.ok(threw, "requireKey on unknown key throws");

        // --- a primary mark never falls through to a legacy identity ---
        Check.ok(reg.identify(999, "excelmobveil", "potion").isEmpty(),
                "unknown primary id does not fall through to legacy");
        Check.eq(BuiltinItems.VEIL_POTION,
                reg.identify(1, "exceldowned", "revive_potion").orElse(null),
                "known primary id wins over a foreign legacy mark");
    }
}

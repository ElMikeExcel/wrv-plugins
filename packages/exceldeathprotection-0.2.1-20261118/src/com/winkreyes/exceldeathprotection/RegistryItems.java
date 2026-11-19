package com.winkreyes.exceldeathprotection;

import com.winkreyes.excelitems.core.ItemDefinition;

import java.util.OptionalInt;

/**
 * The ExcelItems registry seam for this plugin (migration M2,
 * P-ITEM-REGISTRY-1). Pure JVM — no Bukkit types — so the plain-JVM
 * suite can pin the two rules that matter:
 *
 * <ul>
 *   <li>Premium (registry ID 3, key "deathpotion") is a physical
 *       ITEM: minting is FULLY delegated to
 *       {@code ExcelItemsAPI.create(3)} (supervisor decision D124).
 *       The registry mint dual-stamps the primary mark
 *       (excelitems:id = 3) and this plugin's legacy marks
 *       (exceldeathprotection:potion BYTE 1 +
 *       exceldeathprotection:lore_version INTEGER 2), so freshly
 *       minted potions and already-held legacy potions are the
 *       same item.</li>
 *   <li>Basic (registry ID 2, key "deathprotection_basic") is an
 *       ACTIVATION: no physical item exists and none may be
 *       invented. {@code create(2)} yields the CHEST
 *       display/representative stack ONLY; it must never be wired
 *       as a giveable, tradeable or drinkable item. Buying basic
 *       activates Tier.BASIC through deliverBasic, exactly as in
 *       0.2.0.</li>
 * </ul>
 */
public final class RegistryItems {

    private RegistryItems() {}

    public static final int PREMIUM_ID = 3;
    public static final String PREMIUM_KEY = "deathpotion";
    public static final String PREMIUM_HANDLER = "exceldeathprotection:premium-drink";

    public static final int BASIC_ID = 2;
    public static final String BASIC_KEY = "deathprotection_basic";
    public static final String BASIC_HANDLER = "exceldeathprotection:basic-activation";

    /** True only for definitions that mint a real, giveable item. */
    public static boolean isGiveable(ItemDefinition def) {
        return def != null && def.kind() == ItemDefinition.Kind.ITEM;
    }

    /**
     * Fail-closed guard for any give/mint-by-ID path: returns the
     * definition only when it is a physical ITEM. An ACTIVATION
     * definition (ID 2) is refused — wiring it as a giveable item
     * would invent a transferable protection token that was never
     * designed (decision D123 D1).
     */
    public static ItemDefinition requireGiveable(ItemDefinition def) {
        if (!isGiveable(def)) {
            throw new IllegalStateException("Registry "
                    + (def == null ? "definition is missing"
                            : "id " + def.id() + " (" + def.key() + ") is an ACTIVATION")
                    + " — no giveable item exists; delivery stays with the owning plugin.");
        }
        return def;
    }

    /** Fail-closed guard for the basic activation path (ID 2). */
    public static ItemDefinition requireActivation(ItemDefinition def) {
        if (def == null || def.kind() != ItemDefinition.Kind.ACTIVATION) {
            throw new IllegalStateException("Registry id " + BASIC_ID + " (" + BASIC_KEY
                    + ") must be an ACTIVATION definition; got "
                    + (def == null ? "none" : def.id() + " (" + def.key() + ", " + def.kind() + ")"));
        }
        return def;
    }

    /**
     * Resolve a stack's raw identity marks to the premium registry
     * ID, mirroring the ExcelItems ItemFactory rule (primary mark
     * first; legacy fallback ONLY when no primary mark is present —
     * a primary mark naming another item never falls through to the
     * legacy mark) plus this plugin's 0.2.0 recognition rule (the
     * legacy potion BYTE alone identifies a premium potion; the
     * lore_version stamp rides along on every minted bottle and the
     * lore refresh upgrades older bottles in place).
     *
     * @param primaryId        the excelitems:id value, or null
     * @param legacyPotionMark true when exceldeathprotection:potion BYTE is present
     * @return 3 when the marks identify the premium potion, else empty
     */
    public static OptionalInt resolvePremiumIdentity(Integer primaryId, boolean legacyPotionMark) {
        if (primaryId != null) {
            return primaryId == PREMIUM_ID ? OptionalInt.of(PREMIUM_ID) : OptionalInt.empty();
        }
        return legacyPotionMark ? OptionalInt.of(PREMIUM_ID) : OptionalInt.empty();
    }
}

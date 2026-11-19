package com.winkreyes.exceldeathprotection;

import com.winkreyes.excelitems.core.BuiltinItems;
import com.winkreyes.excelitems.core.ItemDefinition;
import com.winkreyes.excelitems.core.ItemRegistry;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.OptionalInt;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Migration M2 gates (W3-MIGRATION-SCOPE §4.2), plain-JVM against
 * the REAL ExcelItems registry definitions (BuiltinItems — the
 * same definitions the live ExcelItems 0.1.0 registers):
 *
 *  - M2a: premium ID 3 is a giveable ITEM whose construction in the
 *    registry matches the 0.2.0 owner mint exactly (material,
 *    colour, glint, name, lore, legacy marks + lore_version 2), so
 *    delegating the mint to ExcelItemsAPI.create(3) changes no
 *    player-visible fact.
 *  - M2 gate 1 (ID-2 negative test): the ACTIVATION definition
 *    cannot pass the giveable guard — no create/give-by-ID path in
 *    this plugin can produce a tradeable/drinkable "basic" item —
 *    while the basic activation guard accepts it, so the shop's
 *    basic entry still activates Tier.BASIC through deliverBasic.
 *  - M2 gate 2 (credit seam): the reflective creditPremium shapes
 *    BossTrial probes are unchanged, and a premium credit's
 *    product identity resolves through the registry identify rule
 *    (primary-first, legacy fallback, no fall-through).
 */
class RegistryMigrationTest {

    private static ItemRegistry registry() { return BuiltinItems.registry(); }

    // ---- M2a: premium definition fidelity ----

    @Test void premiumIsRegistryId3GiveableItem() {
        ItemDefinition def = registry().requireId(RegistryItems.PREMIUM_ID);
        assertEquals("deathpotion", def.key());
        assertEquals(ItemDefinition.Kind.ITEM, def.kind());
        assertSame(def, RegistryItems.requireGiveable(def));
        assertEquals("exceldeathprotection:premium-drink", def.handlerId());
        assertEquals(RegistryItems.PREMIUM_HANDLER, def.handlerId());
    }

    @Test void premiumDefinitionMatchesThe020MintExactly() {
        ItemDefinition def = registry().requireId(3);
        assertEquals("POTION", def.material());
        assertEquals(Integer.valueOf(0x7D3C98), def.potionColorRgb());
        assertTrue(def.glint());
        assertEquals("&d&l+Death Protection Potion", def.displayName());
        assertEquals(8, def.lore().size());
        // Legacy marks the 0.2.0 mint stamps (ExcelDeathProtection
        // createPotion/updatePotionText): potion BYTE + lore_version 2.
        assertEquals("exceldeathprotection", def.legacyNamespace());
        assertEquals("potion", def.legacyKey());
        assertEquals("lore_version", def.legacyLoreKey());
        assertEquals(2, def.loreVersion());
    }

    // ---- M2 gate 1: ID-2 negative test ----

    @Test void basicIsRegistryId2ActivationResolvedFromRegistry() {
        ItemDefinition def = registry().requireId(RegistryItems.BASIC_ID);
        assertEquals("deathprotection_basic", def.key());
        assertEquals(ItemDefinition.Kind.ACTIVATION, def.kind());
        assertSame(def, RegistryItems.requireActivation(def));
        assertEquals("exceldeathprotection:basic-activation", def.handlerId());
        assertEquals(RegistryItems.BASIC_HANDLER, def.handlerId());
        // Single source for name/lore/display: the CHEST stack is
        // the display representative only.
        assertEquals("CHEST", def.material());
        assertEquals("&a&lDeath Protection", def.displayName());
        assertEquals(5, def.lore().size());
        assertFalse(def.hasLegacyMark(), "ID 2 has no legacy mark — no pre-registry basic item exists");
    }

    @Test void id2NegativeTest_activationCanNeverBeGiven() {
        ItemDefinition basic = registry().requireId(2);
        assertFalse(RegistryItems.isGiveable(basic));
        IllegalStateException refused = assertThrows(IllegalStateException.class,
                () -> RegistryItems.requireGiveable(basic),
                "create/give-by-ID for ID 2 must be refused, not mint a display CHEST as an item");
        assertTrue(refused.getMessage().contains("ACTIVATION"));
        // And the reverse guards fail closed too: premium is not an
        // activation, a missing definition is neither.
        assertThrows(IllegalStateException.class, () -> RegistryItems.requireActivation(registry().requireId(3)));
        assertThrows(IllegalStateException.class, () -> RegistryItems.requireGiveable(null));
        assertThrows(IllegalStateException.class, () -> RegistryItems.requireActivation(null));
    }

    @Test void id2HasNoIdentifiableItem() {
        // No marks resolve to ID 2: it owns no legacy mark, and the
        // premium identity resolver never returns it.
        assertTrue(registry().identifyLegacy("exceldeathprotection", "potion").isPresent());
        assertEquals(3, registry().identifyLegacy("exceldeathprotection", "potion").get().id());
        assertEquals(OptionalInt.empty(), RegistryItems.resolvePremiumIdentity(2, false));
        assertEquals(OptionalInt.empty(), RegistryItems.resolvePremiumIdentity(2, true));
    }

    // ---- Identify rule (premium, primary-first / legacy fallback) ----

    @Test void premiumIdentity_primaryMarkWinsAndNeverFallsThrough() {
        assertEquals(OptionalInt.of(3), RegistryItems.resolvePremiumIdentity(3, false));
        assertEquals(OptionalInt.of(3), RegistryItems.resolvePremiumIdentity(3, true));
        // A stack carrying ANOTHER item's primary mark is not a
        // premium potion even if it also carries the legacy mark.
        assertEquals(OptionalInt.empty(), RegistryItems.resolvePremiumIdentity(4, true));
        assertEquals(OptionalInt.empty(), RegistryItems.resolvePremiumIdentity(1, true));
        assertEquals(OptionalInt.empty(), RegistryItems.resolvePremiumIdentity(99, true));
    }

    @Test void premiumIdentity_legacyFallbackOnlyWithoutPrimary() {
        assertEquals(OptionalInt.of(3), RegistryItems.resolvePremiumIdentity(null, true));
        assertEquals(OptionalInt.empty(), RegistryItems.resolvePremiumIdentity(null, false));
        // The registry core agrees on the legacy mark.
        assertEquals(3, registry().identify(null, "exceldeathprotection", "potion").get().id());
        assertTrue(registry().identify(99, "exceldeathprotection", "potion").isEmpty(),
                "stale/forged primary must not fall through to legacy");
    }

    // ---- M2 gate 2: premium credit seam ----

    @Test void creditSeamReflectiveShapesUnchanged() throws Exception {
        Method two = ExcelDeathProtectionAPI.class.getMethod("creditPremium", Player.class, int.class);
        assertTrue(Modifier.isPublic(two.getModifiers()) && Modifier.isStatic(two.getModifiers()));
        assertEquals(boolean.class, two.getReturnType());
        Method one = ExcelDeathProtectionAPI.class.getMethod("creditPremium", Player.class);
        assertTrue(Modifier.isPublic(one.getModifiers()) && Modifier.isStatic(one.getModifiers()));
        // Plugin down in the unit environment: clean false, never throws.
        assertFalse(ExcelDeathProtectionAPI.creditPremium(null, 1));
        assertEquals(0, ExcelDeathProtectionAPI.bankedPremiumCredits(null));
    }

    @Test void creditSeamPremiumIdentityResolvesThroughRegistry() {
        // A banked premium credit is one premium protection (ID 3).
        // The identity it stands for resolves through the same
        // registry identify rule the drink path uses: a stack
        // carrying the registry-minted marks (primary 3 + legacy
        // potion + lore_version 2) and a legacy-only stack both
        // resolve to ID 3; nothing else does.
        assertEquals(OptionalInt.of(RegistryItems.PREMIUM_ID),
                RegistryItems.resolvePremiumIdentity(3, true));
        assertEquals(OptionalInt.of(RegistryItems.PREMIUM_ID),
                RegistryItems.resolvePremiumIdentity(null, true));
        ItemDefinition def = registry().requireId(RegistryItems.PREMIUM_ID);
        assertEquals(2, def.loreVersion(), "registry mint stamps lore_version 2 like the 0.2.0 mint");
        // Bank math itself is untouched by the migration (existing
        // PremiumCredits suite pins it); one credit activates now.
        PremiumCredits.CreditResult r = PremiumCredits.applyCredit(false, 0, 1, true, 5, 0);
        assertTrue(r.credited() && r.activateNow());
    }
}

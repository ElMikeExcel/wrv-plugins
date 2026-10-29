// Excel signature - WRV maintained source; original authorship and license notices preserved.
// SPDX-License-Identifier: MIT
package com.winkreyes.shops;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.junit.jupiter.api.Test;

/**
 * The Bukkit event shell in WRVShops cannot run in this harness, so the
 * compass identification and routing rules live in CompassShortcut as pure
 * functions and are covered here. The handler only gathers these facts.
 */
class CompassShortcutTest {
    private static ItemStack stamped(Material type, boolean current, boolean legacy) {
        ItemStack item = mock(ItemStack.class);
        ItemMeta meta = mock(ItemMeta.class);
        PersistentDataContainer pdc = mock(PersistentDataContainer.class);
        when(item.getType()).thenReturn(type);
        when(item.hasItemMeta()).thenReturn(true);
        when(item.getItemMeta()).thenReturn(meta);
        when(meta.getPersistentDataContainer()).thenReturn(pdc);
        when(pdc.getOrDefault(CompassShortcut.COMPASS_KEY, PersistentDataType.BOOLEAN, false)).thenReturn(current);
        when(pdc.getOrDefault(CompassShortcut.LEGACY_COMPASS_KEY, PersistentDataType.BOOLEAN, false)).thenReturn(legacy);
        return item;
    }

    @Test void stampsUseExcelMenuJoinAndLegacyNamespaces() {
        assertEquals(new NamespacedKey("excelmenujoin", "menu_compass"), CompassShortcut.COMPASS_KEY);
        assertEquals(new NamespacedKey("menujoin", "menu_compass"), CompassShortcut.LEGACY_COMPASS_KEY);
    }
    @Test void currentStampAloneIdentifiesMenuCompass() {
        assertTrue(CompassShortcut.isMenuCompass(stamped(Material.COMPASS, true, false)));
    }
    @Test void legacyStampAloneIdentifiesMenuCompass() {
        assertTrue(CompassShortcut.isMenuCompass(stamped(Material.COMPASS, false, true)));
    }
    @Test void bothStampsIdentifyMenuCompass() {
        assertTrue(CompassShortcut.isMenuCompass(stamped(Material.COMPASS, true, true)));
    }
    @Test void unstampedCompassIsNotMenuCompass() {
        assertFalse(CompassShortcut.isMenuCompass(stamped(Material.COMPASS, false, false)));
    }
    @Test void stampOnWrongItemTypeIsNotMenuCompass() {
        assertFalse(CompassShortcut.isMenuCompass(stamped(Material.STICK, true, true)));
    }
    @Test void compassWithoutMetaIsNotMenuCompass() {
        ItemStack item = mock(ItemStack.class);
        when(item.getType()).thenReturn(Material.COMPASS);
        when(item.hasItemMeta()).thenReturn(false);
        assertFalse(CompassShortcut.isMenuCompass(item));
    }
    @Test void nullItemIsNotMenuCompass() {
        assertFalse(CompassShortcut.isMenuCompass(null));
    }

    private static CompassShortcut.Route route(boolean enabled, boolean compass, boolean sign,
                                               boolean shopFound, boolean owned, boolean attachedToChest) {
        return CompassShortcut.route(enabled, compass, sign, shopFound, owned, attachedToChest);
    }

    @Test void ownShopSignRoutesToManage() {
        assertEquals(CompassShortcut.Route.MANAGE, route(true, true, true, true, true, true));
        // Whether the sign also resolves an attached chest does not matter once the shop is found.
        assertEquals(CompassShortcut.Route.MANAGE, route(true, true, true, true, true, false));
    }
    @Test void otherPlayersShopIsNeverIntercepted() {
        assertEquals(CompassShortcut.Route.NONE, route(true, true, true, true, false, true));
        assertEquals(CompassShortcut.Route.NONE, route(true, true, true, true, false, false));
    }
    @Test void ownChestSignWithoutShopRoutesToSetup() {
        assertEquals(CompassShortcut.Route.SETUP, route(true, true, true, false, false, true));
    }
    @Test void signWithoutAttachedChestIsNotIntercepted() {
        assertEquals(CompassShortcut.Route.NONE, route(true, true, true, false, false, false));
    }
    @Test void nonSignClicksAreNotIntercepted() {
        assertEquals(CompassShortcut.Route.NONE, route(true, true, false, true, true, true));
        assertEquals(CompassShortcut.Route.NONE, route(true, true, false, false, false, true));
    }
    @Test void nonCompassItemsAreNotIntercepted() {
        assertEquals(CompassShortcut.Route.NONE, route(true, false, true, true, true, true));
        assertEquals(CompassShortcut.Route.NONE, route(true, false, true, false, false, true));
    }
    @Test void disabledToggleDisablesEveryRoute() {
        assertEquals(CompassShortcut.Route.NONE, route(false, true, true, true, true, true));
        assertEquals(CompassShortcut.Route.NONE, route(false, true, true, false, false, true));
        assertEquals(CompassShortcut.Route.NONE, route(false, false, false, false, false, false));
    }
}

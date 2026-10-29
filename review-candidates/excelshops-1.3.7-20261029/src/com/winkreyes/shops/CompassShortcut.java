// Excel signature - WRV maintained source; original authorship and license notices preserved.
// SPDX-License-Identifier: MIT
package com.winkreyes.shops;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

/**
 * Routing rules for the menu-compass shop shortcut, kept free of event and
 * world state so they can be unit tested directly. The Bukkit handler in
 * WRVShops only gathers these facts and executes the returned route.
 */
public final class CompassShortcut {
    /** Where a compass tap on a block should go. */
    public enum Route { NONE, MANAGE, SETUP }

    /** Stamp written by ExcelMenuJoin 1.3.0+ (its compasses carry both stamps). */
    public static final NamespacedKey COMPASS_KEY = new NamespacedKey("excelmenujoin", "menu_compass");
    /** Stamp written by MenuJoin up to 1.2.0; older compasses carry only this one. */
    public static final NamespacedKey LEGACY_COMPASS_KEY = new NamespacedKey("menujoin", "menu_compass");

    private CompassShortcut() {}

    /** Same dual-key identification ExcelMenuJoin uses for its own compass. */
    public static boolean isMenuCompass(ItemStack item) {
        if (item == null || item.getType() != Material.COMPASS || !item.hasItemMeta()) return false;
        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        return pdc.getOrDefault(COMPASS_KEY, PersistentDataType.BOOLEAN, false)
                || pdc.getOrDefault(LEGACY_COMPASS_KEY, PersistentDataType.BOOLEAN, false);
    }

    /**
     * Routes one right-click on a block. Only a menu-compass tap on a sign can
     * route anywhere. A shop owned by another player is never intercepted, so
     * the normal browse path proceeds untouched.
     */
    public static Route route(boolean enabled, boolean menuCompass, boolean signClicked,
                              boolean shopFound, boolean shopOwnedByPlayer, boolean attachedToChest) {
        if (!enabled || !menuCompass || !signClicked) return Route.NONE;
        if (shopFound) return shopOwnedByPlayer ? Route.MANAGE : Route.NONE;
        return attachedToChest ? Route.SETUP : Route.NONE;
    }
}

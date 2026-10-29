// Excel signature - WRV maintained source; original authorship and license notices preserved.
/* SPDX-License-Identifier: MIT */
package com.winkreyes.shops.store;

import java.util.Objects;
import java.util.UUID;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;

/** Exact metadata is preserved. stockLimit is a safety cap, not reserved or virtual stock. */
public record ShopOffer(UUID id, ItemStack item, long unitPriceCents, int stockLimit) {
    public ShopOffer {
        Objects.requireNonNull(id, "id"); Objects.requireNonNull(item, "item");
        if (air(item.getType()) || unitPriceCents <= 0 || stockLimit < 1 || stockLimit > 2304)
            throw new IllegalArgumentException("Offer must have an item, a positive price, and a stock cap from 1 to 2304.");
        item = item.clone(); item.setAmount(1);
    }
    static boolean air(Material material) {
        return material == Material.AIR || material == Material.CAVE_AIR || material == Material.VOID_AIR;
    }
    @Override public ItemStack item() { return item.clone(); }
}

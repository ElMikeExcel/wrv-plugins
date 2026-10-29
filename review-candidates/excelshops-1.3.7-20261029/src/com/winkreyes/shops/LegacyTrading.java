// Excel signature - WRV maintained source; original authorship and license notices preserved.
// SPDX-License-Identifier: MIT
package com.winkreyes.shops;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/** Server-owned UI contract; the player never supplies prices, stock or a recipient. */
public interface LegacyTrading {
    enum Direction { BUY, SELL }
    record Offer(UUID id, String owner, ItemStack item, int bundleQuantity,
                 String buyPrice, String sellPrice, boolean canBuy, boolean canSell) {
        public Offer { Objects.requireNonNull(id); item=item.clone(); }
        @Override public ItemStack item() { return item.clone(); }
    }
    record Quote(UUID id, Direction direction, int requestedQuantity, int quantity,
                 long totalCents, ItemStack item, String description, Instant expiresAt) {
        public Quote { Objects.requireNonNull(id); item=item.clone(); }
        @Override public ItemStack item() { return item.clone(); }
    }
    record Outcome(boolean success, boolean recoveryRequired, String message) { }
    Quote quote(Player player, UUID offerId, Direction direction, int requestedQuantity) throws Exception;
    Outcome confirm(Player player, UUID quoteId);
    void cancel(UUID playerId);
    boolean available();
}

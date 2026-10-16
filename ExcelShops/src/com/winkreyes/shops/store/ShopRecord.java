/* SPDX-License-Identifier: MIT */
package com.winkreyes.shops.store;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public record ShopRecord(UUID id, UUID ownerId, String ownerName, BlockPos chest, BlockPos sign,
                         long revision, List<ShopOffer> offers, OwnershipProof proof) {
    public ShopRecord {
        Objects.requireNonNull(id); Objects.requireNonNull(ownerId); Objects.requireNonNull(ownerName);
        Objects.requireNonNull(chest); Objects.requireNonNull(sign); Objects.requireNonNull(proof);
        offers = List.copyOf(offers);
        if (revision < 1 || ownerName.isBlank() || offers.isEmpty() || offers.size() > 27 || !chest.adjacent(sign))
            throw new IllegalArgumentException("Invalid shop record.");
    }
    public Optional<ShopOffer> offer(UUID id) { return offers.stream().filter(o -> o.id().equals(id)).findFirst(); }
}

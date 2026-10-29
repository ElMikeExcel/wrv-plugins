// Excel signature - WRV maintained source; original authorship and license notices preserved.
/* SPDX-License-Identifier: MIT */
package com.winkreyes.shops.store;

import java.util.UUID;
import org.bukkit.inventory.ItemStack;

/** Review snapshot. Only the registry can issue one; all mutable stacks are defensive copies. */
public final class SetupProposal {
    private final UUID shopId, ownerId;
    private final String ownerName, signSnapshot, chestTagSnapshot;
    private final BlockPos chest, sign;
    private final long revision, epoch;
    private final OwnershipProof proof;
    private final ItemStack[] contents;
    SetupProposal(UUID shopId, UUID ownerId, String ownerName, BlockPos chest, BlockPos sign,
                  long revision, long epoch, OwnershipProof proof, ItemStack[] contents, String signSnapshot, String chestTagSnapshot) {
        this.shopId = shopId; this.ownerId = ownerId; this.ownerName = ownerName; this.chest = chest;
        this.sign = sign; this.revision = revision; this.epoch = epoch; this.proof = proof;
        this.contents = copy(contents); this.signSnapshot = signSnapshot; this.chestTagSnapshot = chestTagSnapshot;
    }
    public UUID shopId() { return shopId; }
    public UUID ownerId() { return ownerId; }
    public String ownerName() { return ownerName; }
    public BlockPos chest() { return chest; }
    public BlockPos sign() { return sign; }
    /** Zero for a new shop; otherwise the revision that was reviewed. */
    public long revision() { return revision; }
    public OwnershipProof proof() { return proof; }
    public ItemStack[] contents() { return copy(contents); }
    long epoch() { return epoch; }
    String signSnapshot() { return signSnapshot; }
    String chestTagSnapshot() { return chestTagSnapshot; }
    static ItemStack[] copy(ItemStack[] items) {
        ItemStack[] result = new ItemStack[items.length];
        for (int i = 0; i < items.length; i++) result[i] = items[i] == null ? null : items[i].clone();
        return result;
    }
}

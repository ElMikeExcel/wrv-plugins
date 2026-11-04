/* Excel WRV maintenance; existing authorship retained. */ package com.winkreyes.teamrewards.ui;

import java.util.List;
import java.util.UUID;

/** Durable owner ledger boundary. UI icons are not rewards and never leave the GUI. */
public interface ClaimGateway {
    record Pending(String id, String label) {
        public Pending {
            if (id == null || id.isBlank() || label == null) throw new IllegalArgumentException();
        }
    }
    enum Result { DELIVERED, FULL_INVENTORY, UNSAFE_INVENTORY, OFFLINE, ALREADY_CLAIMED, NOT_OWNER, PENDING_REVIEW, UNAVAILABLE }
    List<Pending> pending(UUID owner);
    /** Must revalidate UUID ownership, online status and capacity and durably deduplicate delivery.
     * Full/offline requests must retain the claim; GUI success is never delivery evidence. */
    Result claim(UUID owner, String claimId);
}

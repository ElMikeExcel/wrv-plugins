// Excel signature - WRV maintained source; original authorship and license notices preserved.
/* SPDX-License-Identifier: MIT */
package com.winkreyes.shops.store;

import java.util.UUID;
import org.bukkit.Location;

/** Read-only integration seam. Present+null claim means wilderness, never ownership. */
@FunctionalInterface
public interface ClaimOwnership {
    ClaimStatus at(Location location) throws RegistryException;
    record ClaimStatus(boolean available, String claimId, UUID ownerId) {
        public ClaimStatus {
            if (!available && (claimId != null || ownerId != null)) throw new IllegalArgumentException("Unavailable claim provider.");
            if (claimId == null && ownerId != null) throw new IllegalArgumentException("Owner without a claim.");
        }
        public static ClaimStatus unavailable() { return new ClaimStatus(false, null, null); }
        public static ClaimStatus wilderness() { return new ClaimStatus(true, null, null); }
    }
}

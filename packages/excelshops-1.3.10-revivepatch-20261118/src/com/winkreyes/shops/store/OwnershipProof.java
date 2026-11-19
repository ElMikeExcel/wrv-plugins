/* SPDX-License-Identifier: MIT */
package com.winkreyes.shops.store;

import java.util.Objects;
import java.util.UUID;

/** Actual placement/claim ownership, never build trust or container trust. */
public record OwnershipProof(UUID chestIdentity, UUID recordedPlacer, boolean griefPreventionPresent,
                             String chestClaimId, String signClaimId) {
    public OwnershipProof { Objects.requireNonNull(chestIdentity, "chestIdentity"); }
}

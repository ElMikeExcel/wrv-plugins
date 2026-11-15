/* Excel WRV maintenance; existing authorship retained. */ package com.winkreyes.teamrewards.ui;

import java.util.*;

/**
 * 0.2.11 (B-2): which claim IDs have already been presented to each
 * player, so the rewards menu auto-opens for a batch ONCE — at the
 * verdict that stored it, or at the first join that finds it waiting —
 * and never nags again for the same batch. Pure state plus the
 * sidecar text encoding; TeamClaims owns the file
 * (team-rewards-presented.dat, written through AtomicStateFile).
 *
 * Marking rule (spec): ANY open of the menu — auto-present, the
 * /bossrewards command, or a personal chest marker — marks the
 * then-pending claim IDs as presented for that player. A batch
 * triggers an auto-present only while at least one of its IDs is
 * unpresented; new claims in a later batch trigger again.
 */
public final class ReviewPresentation {
    private final Map<UUID, Set<String>> presented = new HashMap<>();

    /** True when at least one of the pending IDs was never presented. */
    public synchronized boolean hasUnpresented(UUID owner, Collection<String> pendingIds) {
        if (owner == null || pendingIds == null || pendingIds.isEmpty()) {
            return false;
        }
        Set<String> seen = presented.getOrDefault(owner, Set.of());
        for (String id : pendingIds) {
            if (id != null && !seen.contains(id)) {
                return true;
            }
        }
        return false;
    }

    /** Record claim IDs as presented to this player. Idempotent. */
    public synchronized void markPresented(UUID owner, Collection<String> claimIds) {
        if (owner == null || claimIds == null || claimIds.isEmpty()) {
            return;
        }
        Set<String> seen = presented.computeIfAbsent(owner, k -> new LinkedHashSet<>());
        for (String id : claimIds) {
            if (id != null && !id.isBlank()) {
                seen.add(id);
            }
        }
    }

    /** The presented IDs on record for one player (never null). */
    public synchronized Set<String> presented(UUID owner) {
        return Set.copyOf(presented.getOrDefault(owner, Set.of()));
    }

    /**
     * Sidecar text: one "uuid claimId" line per pair, sorted so the
     * file is stable and diffable. Claim IDs never contain spaces
     * (they are ledger paths), so the first space is the separator.
     */
    public synchronized String encode() {
        StringBuilder out = new StringBuilder();
        List<UUID> owners = new ArrayList<>(presented.keySet());
        owners.sort(Comparator.comparing(UUID::toString));
        for (UUID owner : owners) {
            List<String> ids = new ArrayList<>(presented.get(owner));
            Collections.sort(ids);
            for (String id : ids) {
                out.append(owner).append(' ').append(id).append('\n');
            }
        }
        return out.toString();
    }

    /**
     * Parse sidecar text. Malformed lines are skipped, never fatal —
     * a damaged sidecar costs at most one repeated presentation,
     * never a lost reward (the claims themselves live in the ledger).
     */
    public static ReviewPresentation decode(String text) {
        ReviewPresentation out = new ReviewPresentation();
        if (text == null) {
            return out;
        }
        for (String line : text.split("\n")) {
            String trimmed = line.strip();
            if (trimmed.isEmpty()) {
                continue;
            }
            int split = trimmed.indexOf(' ');
            if (split <= 0) {
                continue;
            }
            try {
                UUID owner = UUID.fromString(trimmed.substring(0, split));
                String id = trimmed.substring(split + 1).strip();
                if (!id.isEmpty()) {
                    out.markPresented(owner, List.of(id));
                }
            } catch (IllegalArgumentException malformed) {
                // Skip the line; see the javadoc for why this is safe.
            }
        }
        return out;
    }
}

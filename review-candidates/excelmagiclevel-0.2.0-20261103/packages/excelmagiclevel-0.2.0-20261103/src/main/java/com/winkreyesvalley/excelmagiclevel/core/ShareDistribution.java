package com.winkreyesvalley.excelmagiclevel.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * ExcelMagicLevel — WHO receives a party-share copy (SPEC §4.5), as pure
 * data so the gates are unit-testable without a server. The Bukkit layer
 * snapshots every candidate (party member or proximity neighbour) into a
 * {@link MemberView}; this function applies the settled rules:
 *
 * <ul>
 *   <li>the earner never receives their own copy;</li>
 *   <li>same world only — a copy never crosses a world boundary;</li>
 *   <li>distance ≤ radius pays; one step past it does not ("within 50
 *       blocks" — the boundary itself is in range);</li>
 *   <li>malformed views (null fields, NaN coordinates) never pay.</li>
 * </ul>
 *
 * Copy AMOUNTS come from {@link ShareMath} applied to the earner's FINAL
 * amount — copies are terminal: this function is only ever consulted by
 * the earner's own award path, and a copy never re-enters distribution
 * (SPEC §4.5's re-entry guard, structural rather than a flag).
 *
 * <p>Pure Java: no Bukkit, no Magic, no mcMMO imports (SPEC §9.1).</p>
 */
public final class ShareDistribution {
    /** A positioned player, reduced to what the share rules need. */
    public record MemberView(UUID id, String world, double x, double y, double z) {
    }

    private ShareDistribution() {
    }

    /**
     * The recipients of a share copy from {@code earner}, in deterministic
     * (UUID string) order. {@code candidates} may include the earner and
     * out-of-range or cross-world players — all are filtered here.
     */
    public static List<UUID> recipients(MemberView earner, Collection<MemberView> candidates, double radius) {
        List<UUID> out = new ArrayList<>();
        if (earner == null || candidates == null || !(radius > 0)) return out;
        double radiusSq = radius * radius;
        for (MemberView candidate : candidates) {
            if (candidate == null || candidate.id() == null) continue;
            if (candidate.id().equals(earner.id())) continue;
            if (earner.world() == null || !earner.world().equals(candidate.world())) continue;
            double dx = candidate.x() - earner.x();
            double dy = candidate.y() - earner.y();
            double dz = candidate.z() - earner.z();
            double distSq = dx * dx + dy * dy + dz * dz;
            if (Double.isNaN(distSq) || distSq > radiusSq) continue;
            out.add(candidate.id());
        }
        out.sort(Comparator.comparing(UUID::toString));
        return out;
    }
}

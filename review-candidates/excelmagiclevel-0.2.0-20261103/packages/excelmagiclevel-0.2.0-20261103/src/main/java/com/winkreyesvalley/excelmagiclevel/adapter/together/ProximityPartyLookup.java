package com.winkreyesvalley.excelmagiclevel.adapter.together;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * ExcelMagicLevel — the DEGRADED party source (SPEC §0.3's named
 * fallback): with no party service to read, "party" becomes "every
 * online player within the share radius in the same world" — Together's
 * own numbers (50 blocks, same world), no party check. This mode is
 * config-gated ({@code share.fallback-proximity}) and LABELLED wherever
 * the mode is shown (awake line, /magiclevel) — it exists because
 * ExcelTogether 0.4.3 publishes no party API, and it retires itself the
 * moment a real PartyLookup service registers.
 *
 * <p>The radius/world test is duplicated in core.ShareDistribution when
 * copies are distributed: this lookup proposes candidates, the core
 * function disposes — a candidate list can never pay someone the core
 * rules exclude.</p>
 */
public final class ProximityPartyLookup implements PartyLookup {
    private final double radius;

    public ProximityPartyLookup(double radius) {
        this.radius = radius;
    }

    @Override
    public Set<UUID> partyMembers(UUID player) {
        Set<UUID> out = new HashSet<>();
        out.add(player);
        Player earner = Bukkit.getPlayer(player);
        if (earner == null) return out;
        double radiusSq = radius * radius;
        for (Player other : Bukkit.getOnlinePlayers()) {
            if (other.getUniqueId().equals(player)) continue;
            if (!other.getWorld().equals(earner.getWorld())) continue;
            if (other.getLocation().distanceSquared(earner.getLocation()) <= radiusSq) {
                out.add(other.getUniqueId());
            }
        }
        return out;
    }
}

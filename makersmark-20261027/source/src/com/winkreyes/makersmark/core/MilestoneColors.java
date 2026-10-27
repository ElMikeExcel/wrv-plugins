package com.winkreyes.makersmark.core;

import java.util.List;
import net.kyori.adventure.text.format.NamedTextColor;

/** Offline configurable cosmetic selector. Not connected to the live renderer.
 * No approved production thresholds yet. Never writes or changes item data. */
public final class MilestoneColors {
    private MilestoneColors() {}
    public record Tier(long minimum, NamedTextColor color) {
        public Tier {
            if (minimum < 0 || color == null) throw new IllegalArgumentException("Invalid cosmetic tier");
        }
    }
    public static NamedTextColor tier(long count, List<Tier> tiers) {
        NamedTextColor result=NamedTextColor.GRAY;
        long best=-1;
        java.util.Set<Long> seen=new java.util.HashSet<>();
        for(Tier t:tiers) {
            if(!seen.add(t.minimum())) throw new IllegalArgumentException("Duplicate cosmetic tier minimum");
            if(t.minimum()<=count && t.minimum()>best) { best=t.minimum();result=t.color(); }
        }
        return result;
    }
    /** Only plugin-generated lines; existing weapon total remains mob + player kills. */
    public static NamedTextColor forLine(String line, int mobKills, int playerKills, int hits,
            List<Tier> weaponTiers, List<Tier> armorTiers) {
        if (line.startsWith("Kills: "))
            return tier((long)Math.max(0,mobKills)+Math.max(0,playerKills),weaponTiers);
        if (line.startsWith("Hits blocked: ")) return tier(Math.max(0,hits),armorTiers);
        return NamedTextColor.GRAY;
    }
}

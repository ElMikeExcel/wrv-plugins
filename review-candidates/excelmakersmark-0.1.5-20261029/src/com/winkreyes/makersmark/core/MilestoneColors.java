/* Excel WRV maintained source; original attribution and license preserved. */ package com.winkreyes.makersmark.core;

import java.util.List;
import net.kyori.adventure.text.format.NamedTextColor;

/** Cosmetic text colors approved 2026-10-28. Never changes item stats or properties. */
public final class MilestoneColors {
    private MilestoneColors() {}
    private static final List<Tier> APPROVED = List.of(
        new Tier(100, NamedTextColor.GREEN), new Tier(500, NamedTextColor.BLUE),
        new Tier(1000, NamedTextColor.DARK_PURPLE), new Tier(5000, NamedTextColor.GOLD));
    public static NamedTextColor weapon(long mobCreditUnits, int playerKills) {
        // Existing kills include PvP; it remains full credit, never farm-discounted.
        long units = Math.max(0L, mobCreditUnits), pvp = KillCredit.baseline(playerKills);
        long total = units > Long.MAX_VALUE - pvp ? Long.MAX_VALUE : units + pvp;
        return tier(total / 5L, APPROVED); // floor: 99.8 stays gray, exactly100 turns green
    }
    public static NamedTextColor armor(int hits) { return tier(Math.max(0, hits), APPROVED); }
    public static NamedTextColor forCreditLine(String line, long units, int pvp, int hits) {
        if (line.startsWith("Kills: ")) return weapon(units, pvp);
        if (line.startsWith("Hits blocked: ")) return armor(hits);
        return NamedTextColor.GRAY;
    }
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

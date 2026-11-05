package com.winkreyes.excellandmarks.core;

import java.util.EnumMap;
import java.util.Map;

/**
 * Plain settings values (SPEC §9), built by the Bukkit layer from
 * config.yml. All prices/sizes/periods live here — never in code.
 */
public final class Settings {

    public final Map<Spot.Tier, Long> rentCents = new EnumMap<>(Spot.Tier.class);
    public final Map<Spot.Tier, Long> buyCents = new EnumMap<>(Spot.Tier.class);
    public final Map<Spot.Tier, Integer> plotSizes = new EnumMap<>(Spot.Tier.class);
    public Spot.Type defaultType = Spot.Type.LANDMARK;
    public Spot.Tier defaultTier = Spot.Tier.SPOT;
    public int rentPeriodDays = 30;
    public int graceDays = 7;
    public int clockTickMinutes = 15;
    public int plotYBelow = 3;
    public int plotYAbove = 12;
    public int redemptionSearchRadius = 3;
    public String protectLogsDir = "../ExcelProtect";

    /** Code defaults = the config.yml defaults = Mike's adopted sketch (Nov 5, 2026). */
    public Settings() {
        rentCents.put(Spot.Tier.SPOT, 50_000L);        // $500/mo
        rentCents.put(Spot.Tier.LANDMARK, 150_000L);   // $1,500/mo
        rentCents.put(Spot.Tier.HEART, 300_000L);      // $3,000/mo
        buyCents.put(Spot.Tier.SPOT, 1_000_000L);      // $10,000
        buyCents.put(Spot.Tier.LANDMARK, 2_500_000L);  // $25,000
        buyCents.put(Spot.Tier.HEART, 5_000_000L);     // $50,000
        plotSizes.put(Spot.Tier.SPOT, 3);
        plotSizes.put(Spot.Tier.LANDMARK, 5);
        plotSizes.put(Spot.Tier.HEART, 8);
    }

    public long rentFor(Spot.Tier t) { return rentCents.getOrDefault(t, 0L); }
    public long buyFor(Spot.Tier t) { return buyCents.getOrDefault(t, 0L); }
    public int sizeFor(Spot.Tier t) { return plotSizes.getOrDefault(t, 3); }
}

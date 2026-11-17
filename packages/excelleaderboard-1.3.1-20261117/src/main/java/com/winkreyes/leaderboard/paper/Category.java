package com.winkreyes.leaderboard.paper;

import org.bukkit.Material;

import java.util.function.Function;

/** The board's categories, in menu order. Format decides how a raw long
 * value is displayed. */
public enum Category {
    MINER("miner", "Top Miner", Material.IRON_PICKAXE, Format.COUNT),
    BUILDER("builder", "Top Builder", Material.BRICKS, Format.COUNT),
    PLAYTIME("playtime", "Most Playtime", Material.CLOCK, Format.PLAYTIME),
    SLAYER("slayer", "Top Slayer", Material.IRON_SWORD, Format.COUNT),
    TRAVELER("traveler", "Top Traveler", Material.COMPASS, Format.DISTANCE),
    DEATHS("deaths", "Most Deaths", Material.SKELETON_SKULL, Format.COUNT),
    RICHEST("richest", "Richest", Material.GOLD_INGOT, Format.MONEY),
    MCMMO("mcmmo", "mcMMO Power", Material.EXPERIENCE_BOTTLE, Format.COUNT),
    JOBS("jobs", "Jobs Level", Material.ANVIL, Format.COUNT),
    ELITE("elite", "Elite Coins", Material.GOLD_NUGGET, Format.COUNT),
    CLAIMS("claims", "Land Claimed", Material.GOLDEN_SHOVEL, Format.COUNT);

    public enum Format { COUNT, PLAYTIME, DISTANCE, MONEY }

    private final String id;
    private final String title;
    private final Material icon;
    private final Format format;

    Category(String id, String title, Material icon, Format format) {
        this.id = id;
        this.title = title;
        this.icon = icon;
        this.format = format;
    }

    public String id() { return id; }
    public String title() { return title; }
    public Material icon() { return icon; }

    public String formatValue(long value) {
        return switch (format) {
            case COUNT -> com.winkreyes.leaderboard.core.BoardMath.formatCount(value);
            case PLAYTIME -> com.winkreyes.leaderboard.core.BoardMath.formatPlaytime(value);
            case DISTANCE -> com.winkreyes.leaderboard.core.BoardMath.formatDistanceKm(value);
            case MONEY -> com.winkreyes.leaderboard.core.BoardMath.formatMoney(value);
        };
    }

    public static Category byId(String id) {
        for (Category c : values()) {
            if (c.id.equals(id)) {
                return c;
            }
        }
        return null;
    }

    /** Display hook kept for future per-category value sources. */
    public Function<Long, String> formatter() {
        return this::formatValue;
    }
}

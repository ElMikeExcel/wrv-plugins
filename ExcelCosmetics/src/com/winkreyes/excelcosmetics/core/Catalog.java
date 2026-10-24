package com.winkreyes.excelcosmetics.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The sellable catalog, built from plain maps (the Bukkit glue
 * parses config.yml into these) and validated here so the exact
 * same rules are unit-testable on the plain JVM. Colors are a
 * curated palette — never a free hex picker — because a near-black
 * color on the dark chat background is a readability defect, and
 * because a fixed list is what makes each color a sellable SKU.
 */
public final class Catalog {

    /** A single chat color SKU. rgb is a 0xRRGGBB int. */
    public record ColorItem(String id, String name, int rgb, boolean forSale) {
        /** A color on sale in the shop (the normal case). */
        public ColorItem(String id, String name, int rgb) { this(id, name, rgb, true); }
    }

    /** Effect kinds the renderer knows how to paint. */
    public enum EffectKind { GRADIENT, RAINBOW, BOLD, ITALIC, SPARKLE }

    /** A text-effect SKU, applied to the sender's NAME only (the
     *  feasibility call: message bodies stay readable). gradientFrom/
     *  gradientTo are used by GRADIENT effects; -1 = unused. */
    public record EffectItem(String id, String name, EffectKind kind, int gradientFrom, int gradientTo) {}

    /** An archetype set: one bundled color + one effect + flair text.
     *  rgb is the set's own chat color (it need not be a standalone
     *  color SKU). effectId may be null for a color-only set. */
    public record SetItem(String id, String name, int rgb, String effectId, String flair) {}

    /** Prices in whole dollars (Vault doubles underneath). Own-it
     *  prices are FLAT per kind — a whale tier (Mike, Oct 20, 2026:
     *  "Own is whale purchase. Rent is the ideal."): colors 15000,
     *  emote pack 20000, effects 25000, sets 30000. The 4-week
     *  commitment tier rents at fourWeekMultiplier x one week's
     *  rent (3.5x — a discount against 4 single weeks). */
    public record Prices(double colorWeek, double setWeek, double effectWeek,
                         double emotesWeek, double fourWeekMultiplier,
                         double colorOwn, double setOwn, double effectOwn,
                         double emotesOwn, int rentalDaysPerWeek) {

        /** Weeks in the commitment tier (the menus' shift-left buy). */
        public static final int COMMITMENT_WEEKS = 4;

        public double weekPrice(Kind kind) {
            switch (kind) {
                case COLOR: return colorWeek;
                case SET: return setWeek;
                case EFFECT: return effectWeek;
                case EMOTES: return emotesWeek;
                default: throw new IllegalArgumentException("kind " + kind);
            }
        }
        /** The flat own-it price for a kind. */
        public double ownPrice(Kind kind) {
            switch (kind) {
                case COLOR: return colorOwn;
                case SET: return setOwn;
                case EFFECT: return effectOwn;
                case EMOTES: return emotesOwn;
                default: throw new IllegalArgumentException("kind " + kind);
            }
        }
        /** Price for a rental of {@code weeks} weeks, or the flat
         *  own-it price when {@code own}. Exactly COMMITMENT_WEEKS
         *  rents at the commitment multiplier; every other count
         *  is linear in weeks. */
        public double price(Kind kind, int weeks, boolean own) {
            if (own) return ownPrice(kind);
            if (weeks == COMMITMENT_WEEKS) return weekPrice(kind) * fourWeekMultiplier;
            return weekPrice(kind) * Math.max(1, weeks);
        }
        public long rentalMillis(int weeks) {
            return (long) rentalDaysPerWeek * Math.max(1, weeks) * 24L * 3600L * 1000L;
        }
    }

    private final Map<String, ColorItem> colors;
    private final Map<String, EffectItem> effects;
    private final Map<String, SetItem> sets;
    private final Prices prices;
    private final Map<String, String> basicTokens;
    private final Map<String, String> premiumTokens;
    private final boolean premiumTokensEnabled;

    private Catalog(Map<String, ColorItem> colors, Map<String, EffectItem> effects,
                    Map<String, SetItem> sets, Prices prices,
                    Map<String, String> basicTokens, Map<String, String> premiumTokens,
                    boolean premiumTokensEnabled) {
        this.colors = Collections.unmodifiableMap(new LinkedHashMap<>(colors));
        this.effects = Collections.unmodifiableMap(new LinkedHashMap<>(effects));
        this.sets = Collections.unmodifiableMap(new LinkedHashMap<>(sets));
        this.prices = prices;
        this.basicTokens = Collections.unmodifiableMap(new LinkedHashMap<>(basicTokens));
        this.premiumTokens = Collections.unmodifiableMap(new LinkedHashMap<>(premiumTokens));
        this.premiumTokensEnabled = premiumTokensEnabled;
    }

    public Map<String, ColorItem> colors() { return colors; }
    public Map<String, EffectItem> effects() { return effects; }
    public Map<String, SetItem> sets() { return sets; }
    public Prices prices() { return prices; }
    public Map<String, String> basicTokens() { return basicTokens; }
    public Map<String, String> premiumTokens() { return premiumTokens; }
    public boolean premiumTokensEnabled() { return premiumTokensEnabled; }

    public boolean exists(Kind kind, String id) {
        if (id == null) return false;
        switch (kind) {
            case COLOR: return colors.containsKey(id);
            case EFFECT: return effects.containsKey(id);
            case SET: return sets.containsKey(id);
            case EMOTES: return EmoteDefs.PACK_ID.equals(id);
            default: return false;
        }
    }

    /** False for retired colors (for-sale: false): still held, worn
     *  and equippable by players who bought them, but no longer sold. */
    public boolean forSale(Kind kind, String id) {
        if (!exists(kind, id)) return false;
        if (kind == Kind.COLOR) return colors.get(id).forSale();
        return true;
    }

    /** Human display name for any SKU id, or the id itself. */
    public String displayName(Kind kind, String id) {
        switch (kind) {
            case COLOR: { ColorItem c = colors.get(id); return c != null ? c.name() : id; }
            case EFFECT: { EffectItem e = effects.get(id); return e != null ? e.name() : id; }
            case SET: { SetItem s = sets.get(id); return s != null ? s.name() : id; }
            case EMOTES: return "Emote Pack";
            default: return id;
        }
    }

    /**
     * Build + validate. Throws IllegalArgumentException listing every
     * problem found (the plugin refuses to enable on a broken catalog:
     * better a clean boot failure in the log than half-sold SKUs).
     */
    public static Catalog build(Map<String, ColorItem> colors, Map<String, EffectItem> effects,
                                Map<String, SetItem> sets, Prices prices,
                                Map<String, String> basicTokens, Map<String, String> premiumTokens,
                                boolean premiumTokensEnabled) {
        List<String> errors = new ArrayList<>();
        if (colors == null || colors.isEmpty()) errors.add("catalog has no colors");
        if (prices == null) {
            errors.add("prices missing");
        } else {
            if (prices.colorWeek() <= 0) errors.add("color week price must be > 0");
            if (prices.setWeek() <= 0) errors.add("set week price must be > 0");
            if (prices.effectWeek() <= 0) errors.add("effect week price must be > 0");
            if (prices.emotesWeek() <= 0) errors.add("emotes week price must be > 0");
            if (prices.fourWeekMultiplier() <= 0) errors.add("four-week multiplier must be > 0");
            if (prices.colorOwn() <= 0) errors.add("color own price must be > 0");
            if (prices.setOwn() <= 0) errors.add("set own price must be > 0");
            if (prices.effectOwn() <= 0) errors.add("effect own price must be > 0");
            if (prices.emotesOwn() <= 0) errors.add("emotes own price must be > 0");
            if (prices.rentalDaysPerWeek() < 1) errors.add("rental days per week must be >= 1");
        }
        if (colors != null) {
            for (ColorItem c : colors.values()) {
                if (c.id() == null || c.id().isBlank()) errors.add("color with blank id");
                if (c.rgb() < 0 || c.rgb() > 0xFFFFFF) errors.add("color " + c.id() + " rgb out of range");
            }
        }
        if (effects != null) {
            for (EffectItem e : effects.values()) {
                if (e.kind() == EffectKind.GRADIENT && (e.gradientFrom() < 0 || e.gradientTo() < 0)) {
                    errors.add("gradient effect " + e.id() + " needs from/to colors");
                }
            }
        }
        if (sets != null) {
            for (SetItem s : sets.values()) {
                if (s.rgb() < 0 || s.rgb() > 0xFFFFFF) errors.add("set " + s.id() + " rgb out of range");
                if (s.effectId() != null && (effects == null || !effects.containsKey(s.effectId()))) {
                    errors.add("set " + s.id() + " references unknown effect " + s.effectId());
                }
            }
        }
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException("catalog invalid: " + String.join("; ", errors));
        }
        return new Catalog(colors, effects == null ? Map.of() : effects,
                sets == null ? Map.of() : sets, prices,
                basicTokens == null ? Map.of() : basicTokens,
                premiumTokens == null ? Map.of() : premiumTokens,
                premiumTokensEnabled);
    }
}

// ExcelCosmetics — an Excel plugin by WinkReyesValley. (Excel signature — MikeExcel)

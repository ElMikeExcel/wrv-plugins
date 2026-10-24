package com.winkreyes.excelcosmetics.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Turns (name, message, appearance) into a render plan: lists of
 * styled segments the Bukkit glue converts 1:1 into Adventure
 * components. Keeping this pure means the exact painted output —
 * the product being sold — is unit-testable character by character.
 *
 * Painting rules (feasibility §1.1/§1.4):
 *  - The equipped color paints BOTH the name and the message.
 *  - An effect decorates the NAME only; message bodies stay solid
 *    and readable. GRADIENT/RAINBOW recolor the name per character
 *    (overriding the solid color on the name, never the message).
 *  - SPARKLE flanks the name with obfuscated glyphs.
 *  - Token replacement (:heart:, <3, ...) happens on the message
 *    BEFORE coloring; basic tokens are free for everyone, premium
 *    tokens only while the emote pack is held.
 */
public final class ChatFormatter {

    public record Segment(String text, Integer rgb, boolean bold, boolean italic, boolean obfuscated) {
        public static Segment plain(String text, Integer rgb) {
            return new Segment(text, rgb, false, false, false);
        }
    }

    public record Plan(List<Segment> name, List<Segment> message) {}

    /** Gold used for sparkle flanks when no color is equipped. */
    public static final int SPARKLE_DEFAULT_RGB = 0xFFD75E;

    private ChatFormatter() {}

    public static Plan format(Catalog catalog, PlayerState.Appearance appearance,
                              String playerName, String rawMessage,
                              boolean premiumTokensHeld) {
        String message = replaceTokens(catalog, rawMessage, premiumTokensHeld);
        List<Segment> messageSegs = List.of(Segment.plain(message, appearance.rgb()));

        List<Segment> nameSegs = new ArrayList<>();
        Catalog.EffectItem effect = appearance.effectId() != null
                ? catalog.effects().get(appearance.effectId()) : null;
        if (effect == null) {
            nameSegs.add(Segment.plain(playerName, appearance.rgb()));
            return new Plan(nameSegs, messageSegs);
        }
        switch (effect.kind()) {
            case BOLD -> nameSegs.add(new Segment(playerName, appearance.rgb(), true, false, false));
            case ITALIC -> nameSegs.add(new Segment(playerName, appearance.rgb(), false, true, false));
            case GRADIENT -> {
                int[] cols = ColorMath.gradient(effect.gradientFrom(), effect.gradientTo(), playerName.length());
                for (int i = 0; i < playerName.length(); i++) {
                    nameSegs.add(Segment.plain(String.valueOf(playerName.charAt(i)), cols[i]));
                }
            }
            case RAINBOW -> {
                for (int i = 0; i < playerName.length(); i++) {
                    nameSegs.add(Segment.plain(String.valueOf(playerName.charAt(i)),
                            ColorMath.rainbow(i, playerName.length())));
                }
            }
            case SPARKLE -> {
                int flank = appearance.rgb() != null ? appearance.rgb() : SPARKLE_DEFAULT_RGB;
                nameSegs.add(new Segment("✦", flank, false, false, true));
                nameSegs.add(Segment.plain(playerName, appearance.rgb()));
                nameSegs.add(new Segment("✦", flank, false, false, true));
            }
        }
        return new Plan(nameSegs, messageSegs);
    }

    /** Replace chat tokens with glyphs. Longest-token-first so a
     *  premium token can never be shadowed by a shorter basic one
     *  sharing a prefix. Unknown tokens pass through untouched. */
    public static String replaceTokens(Catalog catalog, String message, boolean premiumTokensHeld) {
        if (message == null || message.isEmpty()) return message;
        String out = message;
        if (premiumTokensHeld && catalog.premiumTokensEnabled()) {
            out = applyMap(out, catalog.premiumTokens());
        }
        out = applyMap(out, catalog.basicTokens());
        return out;
    }

    private static String applyMap(String text, Map<String, String> tokens) {
        if (tokens.isEmpty()) return text;
        List<String> keys = new ArrayList<>(tokens.keySet());
        keys.sort((a, b) -> Integer.compare(b.length(), a.length()));
        String out = text;
        for (String key : keys) {
            if (!key.isEmpty()) out = out.replace(key, tokens.get(key));
        }
        return out;
    }
}

// ExcelCosmetics — an Excel plugin by WinkReyesValley. (Excel signature — MikeExcel)

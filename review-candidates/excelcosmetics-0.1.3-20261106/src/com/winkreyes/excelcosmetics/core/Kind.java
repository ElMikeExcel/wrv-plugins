package com.winkreyes.excelcosmetics.core;

/** The four cosmetic grant families. Colors paint chat, effects
 *  decorate the name, sets bundle a color + an effect as one
 *  archetype product, and EMOTES unlocks the emote command pack
 *  (plus any premium glyph tokens once verified). */
public enum Kind {
    COLOR, EFFECT, SET, EMOTES;

    public static Kind parse(String s) {
        if (s == null) return null;
        switch (s.trim().toLowerCase()) {
            case "color": case "colors": return COLOR;
            case "effect": case "effects": return EFFECT;
            case "set": case "sets": return SET;
            case "emote": case "emotes": return EMOTES;
            default: return null;
        }
    }
}

// ExcelCosmetics — an Excel plugin by WinkReyesValley. (Excel signature — MikeExcel)

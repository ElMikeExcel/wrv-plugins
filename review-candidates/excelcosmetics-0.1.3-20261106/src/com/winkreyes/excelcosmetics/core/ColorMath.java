package com.winkreyes.excelcosmetics.core;

/** Color arithmetic for the renderer: hex parsing, gradient
 *  interpolation, rainbow cycling, and the readability floor the
 *  curated palette is tested against. Pure math, no AWT. */
public final class ColorMath {

    private ColorMath() {}

    /** Parse "#RRGGBB" or "RRGGBB" (case-insensitive). -1 if invalid. */
    public static int parseHex(String s) {
        if (s == null) return -1;
        String t = s.trim();
        if (t.startsWith("#")) t = t.substring(1);
        if (t.length() != 6) return -1;
        try {
            return Integer.parseInt(t, 16);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    public static String toHex(int rgb) {
        return String.format("#%06X", rgb & 0xFFFFFF);
    }

    public static int r(int rgb) { return (rgb >> 16) & 0xFF; }
    public static int g(int rgb) { return (rgb >> 8) & 0xFF; }
    public static int b(int rgb) { return rgb & 0xFF; }

    /** sRGB relative luminance in [0,1] (WCAG formula). The shipped
     *  palette is unit-tested to sit at or above the readability
     *  floor against the dark chat background. */
    public static double luminance(int rgb) {
        double rl = linear(r(rgb)), gl = linear(g(rgb)), bl = linear(b(rgb));
        return 0.2126 * rl + 0.7152 * gl + 0.0722 * bl;
    }

    private static double linear(int channel) {
        double c = channel / 255.0;
        return c <= 0.03928 ? c / 12.92 : Math.pow((c + 0.055) / 1.055, 2.4);
    }

    /** Readability floor for palette colors on the dark chat box.
     *  0.1.2: 0.10 (about 3:1 contrast against black), so the classic
     *  Blue (&9) and Grape (&5) codes qualify; black, dark blue, dark
     *  red, dark gray, netherite, redstone and lapis stay out. */
    public static final double MIN_PALETTE_LUMINANCE = 0.10;

    /** 0.1.2: every color Bedrock can show in chat, as Geyser defines
     *  them (code char -> exact RGB). Bedrock has no hex: Geyser rounds
     *  any other RGB to the nearest of these. A palette hex that IS
     *  one of these lands on exactly that code, so Java and Bedrock
     *  see the same color. */
    public static final java.util.Map<Character, Integer> BEDROCK_CODES;
    static {
        java.util.Map<Character, Integer> m = new java.util.LinkedHashMap<>();
        char[] c = {'0','1','2','3','4','5','6','7','8','9','a','b','c','d','e','f',
                'g','h','i','j','m','n','p','q','s','t','u','v'};
        int[] v = {0x000000,0x0000AA,0x00AA00,0x00AAAA,0xAA0000,0xAA00AA,0xFFAA00,0xAAAAAA,
                0x555555,0x5555FF,0x55FF55,0x55FFFF,0xFF5555,0xFF55FF,0xFFFF55,0xFFFFFF,
                0xDDD605,0xE3D4D1,0xCECACA,0x443A3B,0x971607,0xB4684D,0xDEB12D,0x11A036,
                0x2CBAA8,0x21497B,0x9A5CC6,0xEB7214};
        for (int i = 0; i < c.length; i++) m.put(c[i], v[i]);
        BEDROCK_CODES = java.util.Collections.unmodifiableMap(m);
    }

    /** The Bedrock code whose RGB is exactly this color, or null. */
    public static Character exactBedrockCode(int rgb) {
        for (java.util.Map.Entry<Character, Integer> e : BEDROCK_CODES.entrySet()) {
            if (e.getValue() == (rgb & 0xFFFFFF)) return e.getKey();
        }
        return null;
    }

    /** Interpolate {@code steps} colors from {@code from} to {@code to}
     *  inclusive. steps == 1 yields exactly {from}. */
    public static int[] gradient(int from, int to, int steps) {
        if (steps <= 1) return new int[]{from};
        int[] out = new int[steps];
        for (int i = 0; i < steps; i++) {
            double t = (double) i / (steps - 1);
            out[i] = lerp(from, to, t);
        }
        return out;
    }

    static int lerp(int from, int to, double t) {
        int rr = (int) Math.round(r(from) + (r(to) - r(from)) * t);
        int gg = (int) Math.round(g(from) + (g(to) - g(from)) * t);
        int bb = (int) Math.round(b(from) + (b(to) - b(from)) * t);
        return (rr << 16) | (gg << 8) | bb;
    }

    /** Rainbow color for character {@code index} of a string of
     *  {@code length}: hues spread across the full wheel so even a
     *  short name shows a spread, cycling for long ones. */
    public static int rainbow(int index, int length) {
        int spread = Math.max(length, 6);
        double hue = (index % spread) * (360.0 / spread);
        return hsvToRgb(hue, 0.72, 1.0);
    }

    static int hsvToRgb(double h, double s, double v) {
        double c = v * s;
        double x = c * (1 - Math.abs(((h / 60.0) % 2) - 1));
        double m = v - c;
        double rp, gp, bp;
        if (h < 60)       { rp = c; gp = x; bp = 0; }
        else if (h < 120) { rp = x; gp = c; bp = 0; }
        else if (h < 180) { rp = 0; gp = c; bp = x; }
        else if (h < 240) { rp = 0; gp = x; bp = c; }
        else if (h < 300) { rp = x; gp = 0; bp = c; }
        else              { rp = c; gp = 0; bp = x; }
        int rr = (int) Math.round((rp + m) * 255);
        int gg = (int) Math.round((gp + m) * 255);
        int bb = (int) Math.round((bp + m) * 255);
        return (rr << 16) | (gg << 8) | bb;
    }
}

// ExcelCosmetics — an Excel plugin by WinkReyesValley. (Excel signature — MikeExcel)

package com.winkreyes.excelmenu.core;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Legacy color handling, matching what players already see from
 * DeluxeMenus: '&' codes (0-9a-f, k-o, r, x) plus '&#RRGGBB' hex,
 * translated to the section-sign form renderers consume. Pure
 * string work so the unit tests pin it exactly.
 *
 * Bedrock forms render button text on a light-grey background, so
 * the pale end of the palette (light grey, white, yellow, light
 * green, aqua) washes out there — the 0.1.1 fix darkens those tones
 * to black in CONTENT text (lore lines) on the Bedrock door only.
 * Titles keep their authored colors on both platforms, and the
 * Java chest GUI is untouched.
 */
public final class Text {

    private static final Pattern HEX = Pattern.compile("&#([0-9a-fA-F]{6})");
    private static final Pattern AMP_CODE = Pattern.compile("&([0-9a-fk-orx])", Pattern.CASE_INSENSITIVE);
    private static final Pattern CODES = Pattern.compile("[&§][0-9a-fk-orx]", Pattern.CASE_INSENSITIVE);
    /** One pass over §-form text: a full §x hex color, or one pale legacy color code. */
    private static final Pattern PALE = Pattern.compile(
            "§x((?:§[0-9a-fA-F]){6})|§([7feabFEAB])");
    /** Hex colors at least this bright (0-255 perceived) count as pale. §7 grey is 170. */
    private static final int PALE_BRIGHTNESS = 160;

    /** 0.1.5: a lore line that starts with this marker is a color
     *  SAMPLE (e.g. a chat-color preview). Java shows it as authored
     *  (the marker is removed); the Bedrock door skips darkenPale for
     *  it, so the sample keeps its real color instead of turning black,
     *  and only downsamples hex to the Bedrock code chat will use. */
    public static final String SAMPLE = "{sample}";

    private Text() {}

    private static String unmark(String s) {
        return s.startsWith(SAMPLE) ? s.substring(SAMPLE.length()) : s;
    }

    /** True when a raw lore line carries the {sample} marker. */
    public static boolean isSample(String s) {
        return s != null && s.startsWith(SAMPLE);
    }

    /** '&aHello &#FF85C0!' -> '§aHello §x§f§f§8§5§c§0!' */
    public static String colorize(String s) {
        if (s == null) return "";
        Matcher m = HEX.matcher(unmark(s));
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            String hex = m.group(1).toLowerCase();
            StringBuilder rep = new StringBuilder("§x");
            for (char c : hex.toCharArray()) rep.append('§').append(c);
            m.appendReplacement(sb, Matcher.quoteReplacement(rep.toString()));
        }
        m.appendTail(sb);
        // Only '&' that introduces a real code becomes '§' — a bare
        // '&' in text ("Sets & Effects") stays literal, as in game.
        Matcher am = AMP_CODE.matcher(sb.toString());
        return am.replaceAll("§$1");
    }

    /**
     * Darken the pale palette in §-form CONTENT text for Bedrock's
     * light form background: legacy §7 (light grey), §f (white),
     * §e (yellow), §a (light green) and §b (aqua) become §0 (black),
     * and a §x hex color whose perceived brightness is at least
     * PALE_BRIGHTNESS becomes §0 as a whole. Saturated mid-tones
     * (§6 gold, §8 dark grey, §9 blue, §c red, §d pink) and every
     * format code (§l, §o, §r, ...) pass through untouched. Single
     * pass, so digits inside a surviving hex color are never
     * mistaken for legacy codes. Input is colorize() output.
     */
    public static String darkenPale(String s) {
        if (s == null) return "";
        Matcher m = PALE.matcher(s);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            String rep;
            if (m.group(1) != null) { // §x hex color
                String digits = m.group(1).replace("§", "");
                int r = Integer.parseInt(digits.substring(0, 2), 16);
                int g = Integer.parseInt(digits.substring(2, 4), 16);
                int b = Integer.parseInt(digits.substring(4, 6), 16);
                int brightness = (299 * r + 587 * g + 114 * b) / 1000;
                rep = brightness >= PALE_BRIGHTNESS ? "§0" : m.group(0);
            } else { // pale legacy color code
                rep = "§0";
            }
            m.appendReplacement(sb, Matcher.quoteReplacement(rep));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    /**
     * The text of one Bedrock form button (or one detail screen's
     * content): the item's name colorized exactly as authored —
     * titles keep their colors — followed by each lore line
     * colorized and darkened for the light form background. Raw
     * '&'-form strings in, §-form text out, lines joined by '\n'.
     */
    public static String bedrockText(String rawName, List<String> rawLore) {
        StringBuilder sb = new StringBuilder(colorize(rawName));
        if (rawLore != null) {
            for (String line : rawLore) {
                if (sb.length() > 0) sb.append('\n');
                // {sample} lines keep their real color (0.1.5)
                sb.append(isSample(line) ? colorize(line) : darkenPale(colorize(line)));
            }
        }
        return bedrockDownsample(sb.toString());
    }


    // ---- Bedrock hex downsample (0.1.5) ----
    // Bedrock forms are sent raw (Geyser does not translate form text)
    // and Bedrock has no hex: a §x§R§R§G§G§B§B run would be read as six
    // separate codes and land on whatever the LAST digit means. So the
    // Bedrock door replaces each §x run with the single nearest Bedrock
    // code, using the SAME palette and distance Geyser uses for chat
    // (Geyser MessageTranslator + Adventure TextColor.nearestColorTo),
    // so the shop preview matches what Bedrock chat will really show.
    private static final Pattern HEX_RUN = Pattern.compile("§x((?:§[0-9a-fA-F]){6})");
    private static final char[] BEDROCK_CODES = {
            '0','1','2','3','4','5','6','7','8','9','a','b','c','d','e','f',
            'g','h','i','j','m','n','p','q','s','t','u','v'};
    private static final int[] BEDROCK_RGB = {
            0x000000,0x0000AA,0x00AA00,0x00AAAA,0xAA0000,0xAA00AA,0xFFAA00,0xAAAAAA,
            0x555555,0x5555FF,0x55FF55,0x55FFFF,0xFF5555,0xFF55FF,0xFFFF55,0xFFFFFF,
            0xDDD605,0xE3D4D1,0xCECACA,0x443A3B,0x971607,0xB4684D,0xDEB12D,0x11A036,
            0x2CBAA8,0x21497B,0x9A5CC6,0xEB7214};

    /** HSV in [0,1], same math as Adventure's HSVLike.fromRGB. */
    static float[] hsv(int rgb) {
        float r = ((rgb >> 16) & 0xFF) / 255f, g = ((rgb >> 8) & 0xFF) / 255f, b = (rgb & 0xFF) / 255f;
        float max = Math.max(r, Math.max(g, b)), min = Math.min(r, Math.min(g, b)), d = max - min;
        float s = max != 0 ? d / max : 0;
        if (s == 0) return new float[]{0, 0, max};
        float h;
        if (r == max) h = (g - b) / d;
        else if (g == max) h = 2 + (b - r) / d;
        else h = 4 + (r - g) / d;
        h *= 60;
        if (h < 0) h += 360;
        return new float[]{h / 360f, s, max};
    }

    /** The Bedrock code (one char) Geyser would pick for this RGB. */
    public static char nearestBedrockCode(int rgb) {
        float[] a = hsv(rgb);
        float best = Float.MAX_VALUE;
        char code = '0';
        for (int i = 0; i < BEDROCK_RGB.length; i++) {
            float[] o = hsv(BEDROCK_RGB[i]);
            float hd = 3 * Math.min(Math.abs(a[0] - o[0]), 1f - Math.abs(a[0] - o[0]));
            float sd = a[1] - o[1], vd = a[2] - o[2];
            float dist = hd * hd + sd * sd + vd * vd;
            if (dist < best) { best = dist; code = BEDROCK_CODES[i]; }
            if (dist == 0) break;
        }
        return code;
    }

    /** Replace every §x hex run with its nearest single Bedrock code. */
    public static String bedrockDownsample(String s) {
        if (s == null) return "";
        Matcher m = HEX_RUN.matcher(s);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            int rgb = Integer.parseInt(m.group(1).replace("§", ""), 16);
            m.appendReplacement(sb, Matcher.quoteReplacement("§" + nearestBedrockCode(rgb)));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    /** colorize() for the Bedrock door: hex becomes a Bedrock code. */
    public static String colorizeBedrock(String s) {
        return bedrockDownsample(colorize(s));
    }

    /** Plain text with every color/format code removed (form labels). */
    public static String strip(String s) {
        if (s == null) return "";
        String noHex = HEX.matcher(unmark(s)).replaceAll("");
        return CODES.matcher(noHex).replaceAll("");
    }
}

// ExcelMenu — an Excel plugin by WinkReyesValley. (Excel signature — MikeExcel)

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

    private Text() {}

    /** '&aHello &#FF85C0!' -> '§aHello §x§f§f§8§5§c§0!' */
    public static String colorize(String s) {
        if (s == null) return "";
        Matcher m = HEX.matcher(s);
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
                sb.append(darkenPale(colorize(line)));
            }
        }
        return sb.toString();
    }

    /** Plain text with every color/format code removed (form labels). */
    public static String strip(String s) {
        if (s == null) return "";
        String noHex = HEX.matcher(s).replaceAll("");
        return CODES.matcher(noHex).replaceAll("");
    }
}

// ExcelMenu — an Excel plugin by WinkReyesValley. (Excel signature — MikeExcel)

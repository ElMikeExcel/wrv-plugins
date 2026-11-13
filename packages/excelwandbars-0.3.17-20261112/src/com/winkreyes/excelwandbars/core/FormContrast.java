package com.winkreyes.excelwandbars.core;

/**
 * 0.3.14: Bedrock form contrast (the Oct 20, 2026 standard, applied to
 * WandBars' own forms). A Bedrock SimpleForm button sits on a LIGHT fill
 * when idle and on a GREEN fill while it is the selected row, so button
 * text has to stay readable on both. Legacy ink luminance says only
 * black does: section 7 light grey (#AAAAAA) measures about 2.0:1 on the
 * light fill and about 1.1:1 on the green fill (the pale-on-green and
 * pale-on-light locked rows Mike photographed on Nov 9, 2026), and
 * section 8 dark grey still fails the green fill at about 2.9:1. Black
 * (section 0) measures about 17:1 on the light fill and about 8:1 on
 * the green fill.
 *
 * The rule for button labels, enforced by the form renderer
 * (BedrockForms) so no caller can ship an unreadable button: every
 * legacy foreground colour code in a button label is rewritten to
 * {@link #INK}; formatting codes (bold, italic, underline, strikethrough,
 * obfuscated, reset) pass through untouched, and so does the visible
 * text itself. Meaning is never carried by button hue alone: a locked
 * spell's label says "unlocks at ..." in words. Titles and body content
 * are NOT rewritten here; they do not sit on button fills, and no
 * WandBars title or content line carries a colour code.
 *
 * Pure text work (no Bukkit), so the whole rule is unit tested.
 */
public final class FormContrast {
    private FormContrast() {}

    /** The one ink a form button may carry: black, readable on both fills. */
    public static final String INK = "\u00a70";

    private static boolean isColour(char c) {
        return (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
    }

    /**
     * One button label, made safe for both button fills: each legacy
     * colour code (section sign or ampersand) becomes {@link #INK};
     * formatting codes and all other characters pass through unchanged.
     * Null passes through as null (the renderer never sends one).
     */
    public static String buttonLabel(String s) {
        if (s == null || s.isEmpty()) return s;
        StringBuilder out = null;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if ((c == '\u00a7' || c == '&') && i + 1 < s.length() && isColour(s.charAt(i + 1))) {
                if (out == null) { out = new StringBuilder(s.length()); out.append(s, 0, i); }
                out.append(INK);
                i++; // the colour character is consumed by the rewrite
            } else if (out != null) {
                out.append(c);
            }
        }
        return out == null ? s : out.toString();
    }
}

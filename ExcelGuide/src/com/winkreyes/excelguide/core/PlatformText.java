package com.winkreyes.excelguide.core;

import java.util.regex.Pattern;

/**
 * Platform-correct control wording (0.3.3, Mike's parity rule, Oct 26,
 * 2026 12:10 AM CDT: "always do onto one as you would do the other").
 *
 * Answers come from the website knowledge, banked player answers, the
 * AI cache and public chat, so one answer text is shared by Java and
 * Bedrock players. Rather than rewrite the answer (which would break
 * the cache and the public line everyone sees), the Guide adds ONE
 * short control note per delivery, chosen per reader:
 *  - a Bedrock reader whose answer only speaks mouse and keyboard
 *    (right-click, left-click, Shift, sneak-click) gets the touch and
 *    controller words;
 *  - a Java reader whose answer only speaks touch (tap, press and
 *    hold, LT/RT) gets the mouse words.
 * Answers that already give both ways get nothing extra. Pure Java,
 * no Bukkit: tested by PlatformTextTest.
 */
public final class PlatformText {
    private PlatformText() { }

    public static final String BEDROCK_NOTE =
            "On a phone or tablet: right-click means press and hold, left-click means tap, "
            + "and sneak means the crouch button. On a controller: right-click is LT and left-click is RT.";
    public static final String JAVA_NOTE =
            "On a computer: tap means click, and press and hold means hold right-click.";

    private static final Pattern MOUSE = Pattern.compile(
            "(?i)\\b(right[- ]?click\\w*|left[- ]?click\\w*|shift[- ]?click\\w*|sneak[- ]?click\\w*"
            + "|sneak ?\\+ ?(right|left)|click (the|a|an|on|it)\\b|\\[[a-z ]+\\] button)");
    private static final Pattern TOUCH_WORDS = Pattern.compile(
            "(?i)\\b(tap|taps|tapped|tapping|press and hold|long[- ]press|touch|controller)\\b");
    private static final Pattern PAD_KEYS = Pattern.compile("\\b(LT|RT)\\b");

    public static boolean mentionsMouse(String text) {
        return text != null && MOUSE.matcher(text).find();
    }

    public static boolean mentionsTouch(String text) {
        return text != null && (TOUCH_WORDS.matcher(text).find() || PAD_KEYS.matcher(text).find());
    }

    /** The note this reader needs for this answer text, or null for none. */
    public static String noteFor(String text, boolean bedrockReader) {
        if (text == null || text.isBlank()) {
            return null;
        }
        boolean mouse = mentionsMouse(text);
        boolean touch = mentionsTouch(text);
        if (bedrockReader && mouse && !touch) {
            return BEDROCK_NOTE;
        }
        if (!bedrockReader && touch && !mouse) {
            return JAVA_NOTE;
        }
        return null;
    }

    /** The AI rule added to the system prompt, so fresh answers give both ways. */
    public static final String AI_RULE =
            "6. Players use computers (Java) and phones, tablets and consoles (Bedrock). When you "
            + "describe a control, give both ways in one short phrase, for example: right-click "
            + "(press and hold on a phone, LT on a controller), or sneak (the crouch button). "
            + "Menu buttons are clicked on a computer and tapped on a phone.";
}

package excelemfixes.casterxp;

import excelemfixes.shared.Views;

import java.util.ArrayList;
import java.util.List;

/**
 * The class-xp-sharing view model: plain data in, shared
 * Views out — the module reads EliteMobs' state through its
 * bridge and the player's choice through the state store,
 * and this class shapes the screen, so the screen's shape
 * is unit-testable on a plain JVM, the pack's standing
 * pattern. One view definition renders as a Java chest GUI
 * or a Bedrock form through the shared renderer; state a
 * Bedrock player cannot see in hover lore is carried in the
 * button names themselves.
 */
public final class CasterXpView {

    private CasterXpView() {}

    public static final String ACTION_OPEN = "casterxp:open";
    public static final String ACTION_SET_PREFIX = "casterxp:set:";

    /** What the screen shows: the player's active form and
     *  its two foundation tracks (names, XP totals and
     *  levels), their sharing mode, and whether the active
     *  form's tree catch-up is consumed. {@code formName}
     *  is null when no form is selected (the track fields
     *  are then meaningless). */
    public record Snapshot(CasterXpMath.Mode mode,
                           String formName,
                           String skillA, long aXp, int aLevel,
                           String skillB, long bXp, int bLevel,
                           boolean catchUpDone) {}

    public static String setAction(CasterXpMath.Mode mode) {
        return ACTION_SET_PREFIX + mode.name();
    }

    /** Parses a set action into the mode, or null. */
    public static CasterXpMath.Mode parseSet(String action) {
        if (action == null || !action.startsWith(ACTION_SET_PREFIX)) {
            return null;
        }
        String name = action.substring(ACTION_SET_PREFIX.length());
        try {
            return CasterXpMath.Mode.valueOf(name);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public static boolean isOpen(String action) {
        return ACTION_OPEN.equals(action);
    }

    /** Player-facing name for a mode. */
    public static String modeName(CasterXpMath.Mode mode) {
        return switch (mode) {
            case STOCK -> "Separate (EliteMobs default)";
            case SHARED -> "Shared pool";
            case CARRY -> "Main weapon carries";
        };
    }

    /** One-line explanation of a mode, in the words the
     *  screen and the command both use. */
    public static String modeBlurb(CasterXpMath.Mode mode) {
        return switch (mode) {
            case STOCK -> "Your class's two foundation skills level"
                    + " on their own; your class follows the lower one.";
            case SHARED -> "Every gain on either of your class's two"
                    + " foundation skills is split between both, so"
                    + " they rise together.";
            case CARRY -> "Gains on your stronger foundation skill"
                    + " also raise the weaker one, so your class keeps"
                    + " up with your main weapon.";
        };
    }

    /** The sharing screen: the player's numbers, one button
     *  per mode (the current one marked), Back chrome. */
    public static Views.View view(Snapshot s) {
        List<Views.Button> buttons = new ArrayList<>();
        for (CasterXpMath.Mode mode : CasterXpMath.Mode.values()) {
            boolean current = mode == s.mode();
            List<String> lore = new ArrayList<>();
            lore.add("&7" + modeBlurb(mode));
            if (current) {
                lore.add("&aThis is your current setting");
            } else {
                lore.add("&eTap to switch to this setting");
            }
            buttons.add(Views.Button.content(setAction(mode),
                    (current ? "&a[ON] " : "&f") + modeName(mode),
                    current ? "LIME_DYE" : "ENCHANTED_BOOK", lore));
        }
        buttons.add(Views.Button.nav("core:back", "&fBack", "ARROW"));
        String intro;
        if (s.formName() == null) {
            intro = "No class is selected right now. This setting"
                    + " applies to whichever class you have selected:"
                    + " pick a class in EliteMobs' class menu, then"
                    + " choose how its two foundation skills share XP.";
        } else {
            intro = s.formName() + " — " + pretty(s.skillA()) + " Lv "
                    + s.aLevel() + " — " + formatXp(s.aXp()) + " XP  ·  "
                    + pretty(s.skillB()) + " Lv " + s.bLevel() + " — "
                    + formatXp(s.bXp()) + " XP. A class follows its"
                    + " LOWER foundation skill, so pick how your"
                    + " class's two foundation skills share XP:";
        }
        return new Views.View("Class XP Sharing", buttons, intro);
    }

    /** ARMOR -> Armor (display only). */
    static String pretty(String type) {
        if (type == null || type.isEmpty()) return "";
        return type.charAt(0) + type.substring(1).toLowerCase();
    }

    /** XP with thousands separators, locale-free (same
     *  formatting as the skills menu). */
    public static String formatXp(long xp) {
        String digits = Long.toString(xp);
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < digits.length(); i++) {
            if (i > 0 && (digits.length() - i) % 3 == 0) out.append(',');
            out.append(digits.charAt(i));
        }
        return out.toString();
    }
}

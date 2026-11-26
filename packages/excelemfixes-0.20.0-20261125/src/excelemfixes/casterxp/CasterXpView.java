package excelemfixes.casterxp;

import excelemfixes.shared.Views;

import java.util.ArrayList;
import java.util.List;

/**
 * The caster-xp-sharing view model: plain data in, shared
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

    /** What the screen shows: the player's two caster tracks
     *  (XP totals and levels) and their sharing state. */
    public record Snapshot(CasterXpMath.Mode mode,
                           long wandsXp, int wandsLevel,
                           long stavesXp, int stavesLevel,
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
            case STOCK -> "Wands and Staves level on their own;"
                    + " your class follows the lower skill.";
            case SHARED -> "Every Wands or Staves gain is split"
                    + " between both skills, so they rise together.";
            case CARRY -> "Gains on your stronger skill also raise"
                    + " the weaker one, so your class keeps up with"
                    + " your main weapon.";
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
        String intro = "Wands Lv " + s.wandsLevel() + " — "
                + formatXp(s.wandsXp()) + " XP  ·  Staves Lv "
                + s.stavesLevel() + " — " + formatXp(s.stavesXp())
                + " XP. A caster class follows its LOWER skill, so"
                + " pick how your Wands and Staves XP is shared:";
        return new Views.View("Caster XP Sharing", buttons, intro);
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

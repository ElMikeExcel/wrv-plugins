package excelemfixes.shared;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.regex.Pattern;

/**
 * EliteMobs configs write display text with legacy '&' color codes.
 * Java menus render them as Adventure components (italic off, as with
 * every menu item); Bedrock forms take the same text with the codes
 * stripped to plain text.
 */
public final class TextUtil {

    private static final LegacyComponentSerializer LEGACY =
            LegacyComponentSerializer.legacyAmpersand();
    private static final Pattern CODES =
            Pattern.compile("(?i)[&§][0-9a-fk-orx]");

    private TextUtil() {}

    public static Component component(String raw) {
        if (raw == null || raw.isEmpty()) return Component.empty();
        return LEGACY.deserialize(raw)
                .decoration(TextDecoration.ITALIC, false);
    }

    public static String plain(String raw) {
        if (raw == null) return "";
        return CODES.matcher(raw).replaceAll("").strip();
    }

    private static final Pattern NAMEPLATE_BRACKET =
            Pattern.compile("^\\s*[\\[『][^\\]』]*[\\]』]\\s*");

    /**
     * Cleans an in-world nameplate (as kill records store it, e.g.
     * "§4『§c11§4』§f §5The Shredder") or a config display name down to
     * the bare boss name ("The Shredder") so the two can be matched.
     * Strips color codes, a leading level bracket and extra spaces.
     */
    public static String cleanNameplate(String raw) {
        if (raw == null) return "";
        String s = CODES.matcher(raw).replaceAll("");
        s = NAMEPLATE_BRACKET.matcher(s).replaceAll("");
        return s.replaceAll("\\s+", " ").strip();
    }
}

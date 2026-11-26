package excelemfixes.questxp;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Pure D159 target policy: turning what a player typed into a weapon-skill
 * enum name. No Bukkit, no EliteMobs — the caller supplies the live skill
 * table (enum names + display names, in EliteMobs' weapon-skill order),
 * so this stays unit-testable on a plain JVM.
 *
 * <p>Accepted input (case-insensitive; spaces and underscores ignored):
 * <ul>
 *   <li>the enum name — {@code SWORDS}, {@code WANDS}, ...</li>
 *   <li>the display name — {@code Swords}, {@code Wands}, ...</li>
 *   <li>the singular alias — {@code sword}, {@code wand}, {@code axe},
 *       {@code bow}, {@code crossbow}, {@code trident}, {@code hoe},
 *       {@code mace}, {@code spear}, {@code staff} / {@code stave}</li>
 * </ul>
 * ARMOR is never resolvable here: it is not a weapon skill, and its 1/3
 * split is paid automatically with every grant.
 */
public final class TargetPolicy {

    private TargetPolicy() {}

    /** True for the words that select HIGHEST (auto) mode. */
    public static boolean isHighestWord(String input) {
        String n = normalize(input);
        return n.equals("highest") || n.equals("auto") || n.equals("default");
    }

    /**
     * Resolve typed input to a weapon-skill ENUM name from the supplied
     * table, or null when nothing matches (the caller refuses with a
     * helpful message — an unknown skill is never guessed).
     *
     * @param enumNames    weapon-skill enum names, EliteMobs order
     * @param displayNames matching display names (may be null/short —
     *                     enum names alone still resolve)
     */
    public static String resolveSkill(String input, String[] enumNames, String[] displayNames) {
        String n = normalize(input);
        if (n.isEmpty()) {
            return null;
        }
        String nSingular = stripTrailingS(n);
        for (int i = 0; i < enumNames.length; i++) {
            Set<String> forms = new HashSet<>();
            String e = normalize(enumNames[i]);
            String d = displayNames != null && i < displayNames.length
                    ? normalize(displayNames[i]) : e;
            forms.add(e);
            forms.add(d);
            forms.add(stripTrailingS(e));
            forms.add(stripTrailingS(d));
            if ("STAVES".equals(enumNames[i])) {
                forms.add("staff"); // the one irregular singular
            }
            if (forms.contains(n) || forms.contains(nSingular)) {
                return enumNames[i];
            }
        }
        return null;
    }

    private static String normalize(String s) {
        return s == null ? "" : s.toLowerCase(Locale.ROOT).replaceAll("[^a-z]", "");
    }

    private static String stripTrailingS(String s) {
        return s.length() > 1 && s.endsWith("s") ? s.substring(0, s.length() - 1) : s;
    }
}

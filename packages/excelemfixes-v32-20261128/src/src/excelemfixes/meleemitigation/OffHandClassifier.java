package excelemfixes.meleemitigation;

import java.util.Locale;
import java.util.Set;

/**
 * Which off-hand items earn a mitigation bonus, and how
 * much — as a pure, string-based decision.
 *
 * The ruling splits the bonus BY ITEM (Nov 23, 2026,
 * ~12:40 PM amendment):
 *  - a TOTEM pays 2% — it already carries so many other
 *    benefits that its mitigation slice stays small;
 *  - a SHIELD or shield-like defensive item pays 7%.
 *
 * The audit behind the shipped lists (EliteMobs
 * custom-items tree, 849 files): every EliteMobs shield —
 * all 37 of them, including every Primis shield drop and
 * reward — is built on the vanilla SHIELD material, and
 * the only totem-material items are TOTEM_OF_UNDYING
 * charms (2 files). NO other shield-like off-hand material
 * exists in the content tree today. The shield-like list
 * is therefore a config list shipped as [SHIELD]: a future
 * custom defensive item on another material joins by data,
 * never by code.
 *
 * This classifier only answers "what bonus does this
 * off-hand item carry?" The rule that the bonus pays ONLY
 * while the main hand holds a melee weapon lives in the
 * module glue and in MitigationMath's contract — a totem
 * next to a wand earns nothing here in practice because
 * the glue never asks.
 */
public final class OffHandClassifier {

    private OffHandClassifier() {}

    public enum Kind { NONE, TOTEM, SHIELD_LIKE }

    public static final Set<String> DEFAULT_TOTEM_MATERIALS =
            Set.of("TOTEM_OF_UNDYING");
    public static final Set<String> DEFAULT_SHIELD_LIKE_MATERIALS =
            Set.of("SHIELD");

    public static Kind kindOf(String materialName,
                              Set<String> totemMaterials,
                              Set<String> shieldLikeMaterials) {
        if (materialName == null) return Kind.NONE;
        String m = materialName.trim().toUpperCase(Locale.ROOT);
        if (m.isEmpty() || m.equals("AIR")) return Kind.NONE;
        if (totemMaterials != null && totemMaterials.contains(m)) return Kind.TOTEM;
        if (shieldLikeMaterials != null && shieldLikeMaterials.contains(m)) {
            return Kind.SHIELD_LIKE;
        }
        return Kind.NONE;
    }

    /** The bonus percentage a kind pays (sanitized by the caller/math). */
    public static double bonusFor(Kind kind, double totemPercent, double shieldPercent) {
        if (kind == null) return 0.0;
        return switch (kind) {
            case TOTEM -> totemPercent;
            case SHIELD_LIKE -> shieldPercent;
            case NONE -> 0.0;
        };
    }
}

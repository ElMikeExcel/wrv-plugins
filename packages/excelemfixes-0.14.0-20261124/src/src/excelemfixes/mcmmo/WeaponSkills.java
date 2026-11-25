package excelemfixes.mcmmo;

import java.util.Locale;

/**
 * Which mcMMO weapon skill a hit or a killing blow belongs to,
 * mirroring mcMMO's own routing: the skill follows the weapon
 * that dealt the damage (a sword hit prices Swords, a bow shot
 * prices Archery, a wolf's bite prices its owner's Taming —
 * the pet direction is resolved by the caller, not here).
 *
 * <p>String-based (Bukkit material / projectile names in,
 * mcMMO PrimarySkillType names out) so the mapping is unit
 * testable without a server. Anything that is not a recognised
 * weapon maps to null here; the caller falls back to UNARMED —
 * mcMMO's own answer for a hit dealt with a non-weapon in hand
 * (a wand included: mcMMO has no wand skill, and its hand-to-hand
 * skill is the honest home for a spell kill's award).
 */
public final class WeaponSkills {

    public static final String UNARMED = "UNARMED";
    public static final String TAMING = "TAMING";

    private WeaponSkills() {
    }

    /** Skill for a held item's material name, or null if not a weapon. */
    public static String byMaterial(String materialName) {
        if (materialName == null) return null;
        String m = materialName.trim().toUpperCase(Locale.ROOT);
        if (m.endsWith("_SWORD")) return "SWORDS";
        if (m.endsWith("_AXE")) return "AXES";
        if (m.equals("MACE")) return "MACES";
        if (m.endsWith("_SPEAR")) return "SPEARS";
        if (m.equals("TRIDENT")) return "TRIDENTS";
        if (m.equals("BOW")) return "ARCHERY";
        if (m.equals("CROSSBOW")) return "CROSSBOWS";
        return null;
    }

    /** Skill for a projectile's type name, or null if not a weapon shot. */
    public static String byProjectile(String projectileName) {
        if (projectileName == null) return null;
        return switch (projectileName.trim().toUpperCase(Locale.ROOT)) {
            case "ARROW", "SPECTRAL_ARROW", "TIPPED_ARROW" -> "ARCHERY";
            case "TRIDENT" -> "TRIDENTS";
            default -> null;
        };
    }

    /**
     * The skill for a shot: the shooter's held weapon wins when
     * it is a recognised weapon (a crossbow's arrow prices
     * Crossbows, not Archery — mcMMO routes by the weapon used),
     * otherwise the projectile's own mapping, otherwise null.
     */
    public static String forShot(String heldMaterialName,
                                 String projectileName) {
        String held = byMaterial(heldMaterialName);
        if (held != null) return held;
        return byProjectile(projectileName);
    }

    /** The skill for a direct hit: held weapon, else UNARMED. */
    public static String forMelee(String heldMaterialName) {
        String held = byMaterial(heldMaterialName);
        return held != null ? held : UNARMED;
    }
}

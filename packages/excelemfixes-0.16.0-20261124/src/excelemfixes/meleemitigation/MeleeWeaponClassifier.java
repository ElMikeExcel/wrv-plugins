package excelemfixes.meleemitigation;

import java.util.Locale;
import java.util.Set;

/**
 * Which held items count as a MELEE WEAPON — as a pure,
 * string-based decision (material name in, verdict out), so
 * it is unit-testable without a server and fully data-driven
 * from config.
 *
 * The audit behind the shipped lists (full audit in the
 * build record; EliteMobs custom-items tree, 849 files):
 *  - Swords: every tier (*_SWORD) — the largest melee family.
 *  - Axes: every tier (*_AXE).
 *  - Maces: the MACE material (31 EliteMobs items).
 *  - Spears: every *_SPEAR — WITH the trap below.
 *  - Scythes: EliteMobs sells scythes on the *_HOE material
 *    family (19 DIAMOND_HOE items are scythe loot), so *_HOE
 *    counts as melee.
 *  - Tridents: TRIDENT held in hand is a melee weapon (its
 *    melee poke is a hand attack; while it is thrown it is
 *    not in hand at all, so no mitigation can pay then).
 *
 * THE STAFF TRAP: EliteMobs staves are built on the
 * WOODEN_SPEAR material and its wands on BLAZE_ROD — a
 * material-only rule would call a staff a spear and pay
 * mitigation to a magic build, the exact wand+totem case
 * the ruling excludes. So the magic check wins FIRST: when
 * EliteMobs' own weapon identity says the held item is a
 * magic weapon (wand/staff), it is never melee, whatever
 * its material says. BLAZE_ROD and STICK are also simply
 * absent from the melee lists.
 *
 * Deliberately NOT melee: BOW / CROSSBOW (ranged), fishing
 * rods, tools (*_PICKAXE, *_SHOVEL), empty hand, and any
 * item a config exclusion names. Config can add materials
 * and suffixes, so a future expansion weapon family is a
 * data row, never a code change.
 */
public final class MeleeWeaponClassifier {

    private MeleeWeaponClassifier() {}

    /** Shipped exact-material melee list. */
    public static final Set<String> DEFAULT_MELEE_MATERIALS =
            Set.of("MACE", "TRIDENT");

    /** Shipped melee material suffixes. */
    public static final Set<String> DEFAULT_MELEE_SUFFIXES =
            Set.of("_SWORD", "_AXE", "_SPEAR", "_HOE");

    /**
     * @param materialName     Bukkit material name of the main-hand item (null/blank = empty hand)
     * @param magicWeapon      EliteMobs' own identity verdict for the item (wand/staff)
     * @param meleeMaterials   config exact-material set
     * @param meleeSuffixes    config suffix set
     * @param excludedMaterials config exclusion set (wins over both lists)
     */
    public static boolean isMeleeWeapon(String materialName,
                                        boolean magicWeapon,
                                        Set<String> meleeMaterials,
                                        Set<String> meleeSuffixes,
                                        Set<String> excludedMaterials) {
        if (magicWeapon) return false;
        if (materialName == null) return false;
        String m = materialName.trim().toUpperCase(Locale.ROOT);
        if (m.isEmpty() || m.equals("AIR")) return false;
        if (excludedMaterials != null && excludedMaterials.contains(m)) return false;
        if (meleeMaterials != null && meleeMaterials.contains(m)) return true;
        if (meleeSuffixes != null) {
            for (String suffix : meleeSuffixes) {
                if (suffix != null && !suffix.isBlank()
                        && m.endsWith(suffix.trim().toUpperCase(Locale.ROOT))) {
                    return true;
                }
            }
        }
        return false;
    }

    /** Convenience overload using the shipped lists. */
    public static boolean isMeleeWeapon(String materialName, boolean magicWeapon) {
        return isMeleeWeapon(materialName, magicWeapon,
                DEFAULT_MELEE_MATERIALS, DEFAULT_MELEE_SUFFIXES, Set.of());
    }
}

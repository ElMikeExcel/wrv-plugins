package com.winkreyes.excelbosstrial.core;

/**
 * Which governed hits plant an XP band verdict (0.2.4): exactly the
 * hit shapes the installed mcMMO fork pays combat XP for, mirrored
 * from its CombatUtils.processCombatAttack / resolveMeleeSkill
 * (source-verified Oct 22, 2026 against the WRV fork, build
 * 2.3.003-WRV-XP-2):
 *
 * <pre>
 *   Wolf damager with a Player owner  -&gt; owner gains TAMING XP   (plants)
 *   Trident with a Player shooter     -&gt; shooter gains TRIDENTS  (plants)
 *   other arrow, Player shooter       -&gt; ARCHERY or CROSSBOWS    (plants)
 *   Player melee, spear damage type   -&gt; SPEARS                  (plants)
 *   Player melee, sword/axe/unarmed/
 *     trident/mace in main hand       -&gt; that melee skill        (plants)
 *   Player melee, anything else held  -&gt; NO gain event fires     (no plant)
 *   TNT / crystal / anchor / cloud /
 *     any other damager shape         -&gt; NO gain event fires     (no plant)
 * </pre>
 *
 * Planting only for shapes that actually produce a gain event is
 * what keeps the per-player FIFO aligned: a planted token with no
 * matching gain would sit in the queue and shift every later verdict
 * by one. The adapter classifies the damager by entity type alone;
 * that is sound because BossTrial's attribution resolves the shooter
 * or owner to exactly the credited player whenever it attributes a
 * projectile or pet hit, and the only registry-attributed shapes
 * (end crystals, respawn anchors) classify as OTHER here.
 *
 * Pure (names, not Bukkit types), so the mirror table is unit-tested
 * directly.
 */
public final class XpBandPlant {

    /** The damager shapes the adapter distinguishes, by entity type. */
    public enum Damager {
        /** The damager IS the player (a melee swing). */
        MELEE,
        /** A wolf (tameable) damager. */
        WOLF,
        /** A thrown trident entity. */
        TRIDENT,
        /** Any other AbstractArrow (arrow, spectral arrow). */
        ARROW,
        /** Everything else: explosions, clouds, dispensers' output, mobs. */
        OTHER
    }

    private XpBandPlant() {
    }

    /**
     * True when a hit of this damager shape, by a player holding
     * mainHandMaterialName, makes mcMMO fire a combat XP gain for
     * that player. mainHandMaterialName is the Bukkit Material name
     * of the credited player's main-hand item (e.g. "DIAMOND_SWORD"),
     * or null when the hand state is unknown; only the MELEE row
     * consults it.
     */
    public static boolean paysCombatXp(Damager damager, String mainHandMaterialName) {
        if (damager == null) {
            return false;
        }
        return switch (damager) {
            case WOLF, TRIDENT, ARROW -> true;
            case MELEE -> meleePays(mainHandMaterialName);
            case OTHER -> false;
        };
    }

    /**
     * The melee half of the mirror: mcMMO's resolveMeleeSkill claims
     * the hit when the damage type is spear (the adapter cannot see
     * damage types, and a spear hit requires holding a spear-class
     * item, so the item test covers it), or the held item is a sword,
     * an axe, an unarmed-eligible item (mcMMO's isUnarmed covers the
     * empty hand and non-weapon items are NOT unarmed - see below), a
     * trident, or a mace.
     *
     * Conservative asymmetry, deliberate: an unknown hand (null) is
     * treated as paying, because the adapter reads the actual main
     * hand at hit time and null should never occur; if it somehow
     * did, planting matches the overwhelmingly common case (a player
     * fighting a seal boss bare-handed or with a combat item).
     *
     * Config dependency, stated plainly: mcMMO's isUnarmed widens to
     * "any held non-tool item" only when Skills.Unarmed
     * .Items_As_Unarmed is set true; the fork's coded default is
     * false (empty hand only) and WRV runs the default, so a melee
     * swing holding a torch, a block or a bow fires NO gain and this
     * returns false for it, matching. If that config were ever
     * flipped, those swings would pay Unarmed XP unplanted - full
     * pay, the fail-safe direction, never an over-scale.
     *
     * Known mirror limits, accepted and documented: mcMMO's
     * ItemUtils.isSword/isAxe also accept configured custom items
     * (items.yml overrides) - WRV runs stock items.yml, so the
     * material-name rules are exact for this server.
     */
    static boolean meleePays(String mainHandMaterialName) {
        if (mainHandMaterialName == null || mainHandMaterialName.equals("AIR")) {
            return true; // empty hand = Unarmed
        }
        String n = mainHandMaterialName;
        return n.endsWith("_SWORD")
                || n.endsWith("_AXE")
                || n.equals("TRIDENT")
                || n.equals("MACE")
                || n.contains("SPEAR");
    }
}


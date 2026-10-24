package com.winkreyes.exceldeathrecord.core;

/**
 * Maps a Bukkit DamageCause name onto a Burn Ledger cause, or
 * null when the damage is not a destruction the ledger tracks.
 *
 *   LAVA              -> LAVA  (in lava, or lava's own damage)
 *   FIRE, FIRE_TICK   -> FIRE  (standing in fire / burning)
 *   VOID              -> VOID  (fell out of the world)
 *
 * Everything else (a sword swung at an item does nothing, an
 * explosion leaves items alone in this version, cactus is a
 * CONTACT loss the owner did not ask to track in v1) maps to
 * null: not recorded from the damage path.
 */
public final class BurnCause {

    public static final String LAVA = "LAVA";
    public static final String FIRE = "FIRE";
    public static final String VOID = "VOID";
    public static final String DESPAWN = "DESPAWN";

    private BurnCause() {}

    public static String fromDamageCause(String damageCauseName) {
        if (damageCauseName == null) return null;
        switch (damageCauseName) {
            case "LAVA": return LAVA;
            case "FIRE":
            case "FIRE_TICK": return FIRE;
            case "VOID": return VOID;
            default: return null;
        }
    }
}

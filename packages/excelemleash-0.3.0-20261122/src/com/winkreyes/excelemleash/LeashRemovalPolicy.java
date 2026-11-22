package com.winkreyes.excelemleash;

/**
 * Pure leash-resolution policy for ExcelEMLeash 0.2.0 (D182).
 *
 * Mike removed the aggro reset ENTIRELY (Nov 21, 2026): there is no
 * leash anymore. Every boss that has a leash radius is unleashed —
 * the effective target is always 0, EliteMobs' disabled sentinel.
 * With a radius of 0, EliteMobs attaches no soft/hard leash AI at
 * spawn (bytecode: the addSoftLeashAI/addHardLeashAI calls sit under
 * a leashRadius > 0 guard) and its fallback checkLeash() returns
 * immediately (leashRadius < 1), so no elite ever walks or teleports
 * home and no return-end event can fire for it.
 *
 * There are deliberately NO exceptions: no floor, no per-boss
 * overrides, no boss-only carve-out (D182 superseded the D181
 * boss-only scope and the D167 floor). A boss with no leash
 * (current <= 0) is already in the target state and is untouched.
 */
public final class LeashRemovalPolicy {

    public enum ChangeKind { NONE, UNLEASH }

    private LeashRemovalPolicy() {
    }

    /**
     * The radius that should be in force for a boss: always 0
     * (unleashed), whatever it currently carries.
     *
     * @param current the boss's current (file/loaded) leash radius; 0 = none
     * @return the effective target radius
     */
    public static double effectiveTarget(double current) {
        return current > 0 ? 0.0 : current;
    }

    /** How applying the policy to this boss would change it. */
    public static ChangeKind classify(double current) {
        return current > 0 ? ChangeKind.UNLEASH : ChangeKind.NONE;
    }
}

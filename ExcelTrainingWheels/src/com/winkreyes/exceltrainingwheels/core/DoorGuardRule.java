package com.winkreyes.exceltrainingwheels.core;

/**
 * Door Guard decision (Training Wheels Protocol): a door break is
 * cancelled ONLY when the breaking mob is hunting a protected
 * player. Breaks aimed at anyone else — veterans, villagers, no
 * target at all — proceed exactly as vanilla Hard intends.
 * Protection follows the listed player; the world is untouched.
 */
public final class DoorGuardRule {

    private DoorGuardRule() {}

    public static boolean shouldCancel(boolean targetIsPlayer, boolean targetProtected) {
        return targetIsPlayer && targetProtected;
    }
}

package com.winkreyes.excelautosort.hook;

import java.util.UUID;

/**
 * Economy access as a JDK-only seam (SPEC §15.2). The single Vault
 * adapter lives in the {@code economy} package — the only place Vault
 * classes are named — so Max Organization binds or fails soft without
 * Vault ever touching the rest of the plugin. This mirrors the house
 * isolation pattern (GpRightsHook, ProtectGateway, Landmark economy).
 */
public interface EconomyPort {

    /** The standing instance when no economy is bound: everything fails soft. */
    EconomyPort UNAVAILABLE = new EconomyPort() {
        @Override public boolean available() { return false; }
        @Override public double balance(UUID player) { return -1; }
        @Override public boolean withdraw(UUID player, double amount) { return false; }
    };

    /** Is an economy bound and answering? */
    boolean available();

    /** Current balance; negative when the economy is unavailable. */
    double balance(UUID player);

    /**
     * Take {@code amount} from the player. True ONLY when the money
     * actually moved — a failed or unavailable withdrawal changes no
     * entitlement state anywhere (§15.2 confirm-before-charge).
     */
    boolean withdraw(UUID player, double amount);
}

// ExcelAutoSort — Iris / Mike, for Wink Reyes Valley

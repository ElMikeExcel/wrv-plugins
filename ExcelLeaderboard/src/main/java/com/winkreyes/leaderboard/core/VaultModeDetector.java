package com.winkreyes.leaderboard.core;

import java.util.List;

/** Reads EliteMobs' EconomySettings the way a careful human would: the
 * top-level useVault key. When EliteMobs runs on Vault, its "Elite Coins"
 * balance IS the Vault balance, so a separate Elite Coins board would
 * just duplicate Richest. Mike's one-currency decision (Oct 14, 2026):
 * in that mode the Elite Coins board hides itself. */
public final class VaultModeDetector {

    private VaultModeDetector() {}

    public static boolean useVaultEnabled(List<String> economySettingsLines) {
        for (String raw : economySettingsLines) {
            if (raw == null) {
                continue;
            }
            String line = raw.strip();
            if (line.startsWith("#")) {
                continue;
            }
            if (line.startsWith("useVault:")) {
                String value = line.substring("useVault:".length()).strip().toLowerCase();
                return value.equals("true");
            }
        }
        return false;
    }
}

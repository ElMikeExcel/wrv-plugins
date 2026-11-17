package com.winkreyes.leaderboard;

import com.winkreyes.leaderboard.core.VaultModeDetector;

import java.util.List;

public final class VaultModeTests {
    private static int checks = 0;

    private static void eq(Object actual, Object expected, String label) {
        checks++;
        if (!actual.equals(expected)) {
            throw new AssertionError(label + ": expected <" + expected + "> but was <" + actual + ">");
        }
    }

    public static void main(String[] args) {
        eq(VaultModeDetector.useVaultEnabled(List.of("useVault: true")), true, "plain true");
        eq(VaultModeDetector.useVaultEnabled(List.of("useVault: false")), false, "plain false");
        eq(VaultModeDetector.useVaultEnabled(List.of("enableEconomy: true", "useVault: true")),
                true, "found among other keys");
        eq(VaultModeDetector.useVaultEnabled(List.of("# useVault: true (commented example)")),
                false, "comment does not count");
        eq(VaultModeDetector.useVaultEnabled(List.of("currencyName: Elite Coins")),
                false, "missing key is false");
        eq(VaultModeDetector.useVaultEnabled(List.of("  useVault: TRUE  ")),
                true, "indentation and case tolerated");
        System.out.println("VaultModeTests: " + checks + " assertions passed");
    }
}

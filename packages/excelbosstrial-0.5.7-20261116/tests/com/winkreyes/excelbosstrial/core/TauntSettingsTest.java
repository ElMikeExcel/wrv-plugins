/* Excel WRV maintenance; existing authorship retained. */ package com.winkreyes.excelbosstrial.core;

import java.util.List;

/**
 * Boss taunt settings (0.4.0 rebuild, Mike Nov 8, 2026):
 * the Wither ships with its ten lines and the 25-second
 * cooldown; every other chapter ships silent; sanitization
 * keeps the config-first semantics (absent = default,
 * present-but-empty = honored silence).
 */
public final class TauntSettingsTest {

    private static int checks;
    private static int failures;

    private static void check(boolean ok, String label) {
        checks++;
        if (!ok) {
            failures++;
            System.out.println("FAIL: " + label);
        }
    }

    public static void main(String[] args) {
        // --- the shipped defaults
        TauntSettings wither = TauntSettings.defaults(BossKind.WITHER);
        check(wither.enabled() && wither.lines().size() == 10
                        && wither.cooldownSeconds() == 25,
                "the wither ships armed: ten lines, 25 s cooldown");
        check(wither.lines().get(0).equals("MORE.")
                        && wither.lines().contains("Yes… feed the storm."),
                "the wither lines are Mike's slate, first to last");
        for (BossKind kind : BossKind.values()) {
            if (kind != BossKind.WITHER) {
                check(!TauntSettings.defaults(kind).enabled(),
                        kind + " ships silent (no taunts configured)");
            }
        }

        // --- sanitization semantics
        TauntSettings absent = TauntSettings.sanitized(null, 25, wither);
        check(absent.lines().equals(wither.lines()),
                "an absent taunts key keeps the chapter default lines");
        TauntSettings silenced = TauntSettings.sanitized(List.of(), 25, wither);
        check(!silenced.enabled(),
                "a present-but-empty taunts list silences the chapter");
        TauntSettings messy = TauntSettings.sanitized(
                List.of("  MORE.  ", "", "  ", "Feed me."), 25, wither);
        check(messy.lines().equals(List.of("MORE.", "Feed me.")),
                "lines trim and blanks drop");
        check(TauntSettings.sanitized(null, -5, wither).cooldownSeconds() == 0
                        && TauntSettings.sanitized(null, 99_999, wither)
                        .cooldownSeconds() == 3600,
                "the cooldown clamps to [0, 3600]");

        System.out.println("TauntSettingsTest: " + checks + " checks, "
                + failures + " failures");
        if (failures > 0) {
            System.exit(1);
        }
    }
}

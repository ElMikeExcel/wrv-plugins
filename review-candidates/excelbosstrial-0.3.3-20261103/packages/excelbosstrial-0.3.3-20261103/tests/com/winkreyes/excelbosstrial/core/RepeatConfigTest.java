/* Excel WRV maintenance; existing authorship retained. */ package com.winkreyes.excelbosstrial.core;

import java.util.List;
import java.util.Map;

/**
 * The 0.3.0 config surface: every new key parses from the flat
 * map BossTrialConfig consumes, the settled numbers are the
 * code defaults (adjustability law), and per-chapter repeat
 * windows override the global one.
 */
public final class RepeatConfigTest {

    private static int passed;
    private static int failed;

    private static void check(boolean cond, String label) {
        if (cond) {
            passed++;
        } else {
            failed++;
            System.out.println("FAIL: " + label);
        }
    }

    public static void main(String[] args) {
        // Settled defaults, no config at all.
        BossTrialConfig d = BossTrialConfig.defaults();
        check(d.intakeEntityCap == 1024, "default intake cap is the rig-measured 1,024");
        check(d.repeatEnabled, "repeat layer defaults on");
        check(d.repeatResetHours == 24.0, "repeat window defaults to 24h");
        check(d.repeatCashPerChampion == 5000, "repeat cash defaults to the settled $5,000");
        check(d.repeatMcMmoXp == 5000, "mcMMO accelerant defaults to the settled 5,000");
        check(d.repeatMagicLevelXp == 250, "MagicLevel accelerant defaults to the settled 250");
        check(d.repeatClaimflyHours == 2.0, "ClaimFly row defaults to the settled 2 hours");
        check(d.repeatJackpotExtraRolls == 2, "jackpot defaults to the settled +2 rolls");
        check(d.repeatDeathProtectionPremium, "DeathProtection premium row defaults on (adapter-gated)");
        check(!d.repeatLostFoundVoucherEnabled, "L&F voucher defaults OFF (ships disabled, SPEC 4.4)");
        check(d.elderRepeatRespawnMs == 6L * 3600 * 1000, "elder generation respawn defaults to 6h");
        check(d.pressure(BossKind.WITHER).outgoingMultiplier() == 1.25,
                "wither pressure defaults to 1.25 outgoing");
        check(d.pressure(BossKind.WARDEN).outgoingMultiplier() == 1.25,
                "warden pressure defaults to 1.25 outgoing");
        check(d.pressure(BossKind.ELDER_GUARDIAN).outgoingMultiplier() == 1.0,
                "elder pressure defaults to 1.0 outgoing (numbers do the work)");
        check(d.pressure(BossKind.ENDER_DRAGON).pulseDamage() == 2.0,
                "pulse base defaults to 2.0");
        check(!d.repeatTitles.isEmpty(), "the flex title table has its settled defaults");

        // A config carrying the epic slate + overrides parses whole.
        Map<String, Object> m = new java.util.HashMap<>();
        m.put("intake.entity-cap", 1024);
        m.put("wither.scaling.enabled", true);
        m.put("wither.scaling.base-health", 6600);
        m.put("wither.scaling.per-extra-damager", 5100);
        m.put("repeat.reset-hours", 24);
        m.put("wither.repeat.reset-hours", 12);
        m.put("repeat.rewards.cash-per-champion", 5000);
        m.put("wither.pressure.outgoing-multiplier", 1.25);
        m.put("repeat.titles", List.of(
                Map.of("seal", "wither", "clears", 3, "title", "Witherworn"),
                Map.of("seal", "raid", "clears", 5, "title", "Twice-Triumphant")));
        BossTrialConfig cfg = BossTrialConfig.fromMap(m);
        check(cfg.scaling(BossKind.WITHER).baseHealth() == 6600,
                "epic wither base parses (6,600)");
        check(cfg.scaling(BossKind.WITHER).perExtraDamager() == 5100,
                "epic wither per-extra parses (5,100)");
        check(cfg.repeatWindowMs(BossKind.WITHER) == 12L * 3600 * 1000,
                "per-chapter repeat window override wins (12h)");
        check(cfg.repeatWindowMs(BossKind.WARDEN) == 24L * 3600 * 1000,
                "chapters without an override use the global window (24h)");
        check(cfg.repeatTitles.size() == 2
                        && cfg.repeatTitles.get(0).sealId().equals("wither")
                        && cfg.repeatTitles.get(0).clears() == 3
                        && cfg.repeatTitles.get(1).title().equals("Twice-Triumphant"),
                "the config title table replaces the default table whole");

        System.out.println("RepeatConfigTest: " + passed + " passed, " + failed + " failed");
        if (failed > 0) {
            System.exit(1);
        }
    }
}

/* Excel WRV maintenance; existing authorship retained. */ package com.winkreyes.excelbosstrial.core;

import java.util.List;
import java.util.Map;

/**
 * The 0.5.5 config surface: the first-break block parses from
 * the flat map BossTrialConfig consumes (the EM kit as a
 * list-of-maps, the titlesFrom shape), the settled numbers are
 * the code defaults, a config can retune every row (the
 * adjustability law - including the cash number, whose settled
 * value is D6's $10,000 total), and malformed kit rows degrade
 * to skipped instead of breaking the parse.
 */
public final class FirstBreakConfigTest {

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
        check(d.firstBreakWarden.enabled(), "first-break layer defaults on");
        check(d.firstBreakWarden.cashPerChampion() == 10000L,
                "first-break cash defaults to the settled $10,000 (D6 total)");
        check(d.firstBreakWarden.jackpotExtraRolls() == 2, "first-break jackpot defaults to +2");
        check(d.firstBreakWarden.mcMmoXp() == 5000L, "first-break mcMMO defaults to 5,000");
        check(d.firstBreakWarden.magicLevelXp() == 250L, "first-break MagicLevel defaults to 250");
        check(d.firstBreakWarden.claimflyHours() == 2.0, "first-break claim fly defaults to 2 hours");
        check(d.firstBreakWarden.deathProtectionPremium(), "first-break premium defaults on");
        check(d.firstBreakWarden.healerRecognitionEnabled(), "first-break healer recognition defaults on");
        check("ECHO_SHARD".equals(d.firstBreakWarden.cosmeticMaterial()),
                "first-break cosmetic defaults to ECHO_SHARD");
        check("Sealbreakers".equals(d.firstBreakWarden.title()),
                "first-break title defaults to Sealbreakers");
        check(d.firstBreakWarden.emKit().size() == 9, "first-break EM kit defaults to the 9 settled rows");
        check(d.firstBreakWarden.equals(FirstBreakRewards.Spec.settled()),
                "config defaults ARE the settled spec, whole");

        // A config carrying overrides parses whole.
        Map<String, Object> m = new java.util.HashMap<>();
        m.put("first-break.warden.cash-per-champion", 12000);
        m.put("first-break.warden.jackpot-extra-rolls", 3);
        m.put("first-break.warden.em-kit", List.of(
                Map.of("material", "GOLDEN_CARROT", "count", 8),
                Map.of("material", "POTION", "count", 1, "potion", "STRENGTH", "level", 1),
                Map.of("count", 5),
                Map.of("material", "MILK_BUCKET")));
        BossTrialConfig cfg = BossTrialConfig.fromMap(m);
        check(cfg.firstBreakWarden.cashPerChampion() == 12000L,
                "config cash override parses (the number is config-owned)");
        check(cfg.firstBreakWarden.jackpotExtraRolls() == 3, "config jackpot override parses");
        check(cfg.firstBreakWarden.mcMmoXp() == 5000L, "absent rows keep the settled value");
        check(cfg.firstBreakWarden.emKit().size() == 3,
                "kit list parses; the material-less row is skipped, not fatal");
        check(cfg.firstBreakWarden.emKit().get(0).count() == 8, "kit row count parses");
        check("STRENGTH".equals(cfg.firstBreakWarden.emKit().get(1).potion())
                        && cfg.firstBreakWarden.emKit().get(1).level() == 1,
                "kit potion row parses (effect + tier)");
        check(cfg.firstBreakWarden.emKit().get(2).count() == 1
                        && cfg.firstBreakWarden.emKit().get(2).potion() == null,
                "kit row without count/potion defaults to a plain x1");

        // The repeat block is untouched by the new parse.
        check(cfg.repeatCashPerChampion == 5000L, "repeat cash still defaults to $5,000");
        check(cfg.repeatJackpotExtraRolls == 2, "repeat jackpot still defaults to +2");

        System.out.println("FirstBreakConfigTest: " + passed + " passed, " + failed + " failed");
        if (failed > 0) {
            System.exit(1);
        }
    }
}

// ExcelBossTrial - a Wink Reyes Valley Excel plugin.

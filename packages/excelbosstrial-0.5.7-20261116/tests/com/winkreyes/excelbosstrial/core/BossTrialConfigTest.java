/* Excel WRV maintenance; existing authorship retained. */ package com.winkreyes.excelbosstrial.core;

import java.util.Map;

/**
 * The shipped defaults ARE the settled design (Mike, Oct 18, 2026):
 * 50% share, 128-block presence, 6-hour cooldowns, spoils cap 5,
 * caps wither 60/75, elder 35/40, warden 60/75, elder respawn 6h.
 * Oct 19, 2026 evening amendment: warden share height band 12.
 * If a default ever drifts, this suite is the tripwire.
 */
public final class BossTrialConfigTest {

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
        BossTrialConfig d = BossTrialConfig.defaults();
        check(d.enabled, "master switch defaults on");
        check(d.shareThreshold == 0.5, "share threshold 0.5");
        check(d.presenceRange == 128.0, "presence range 128");
        check(d.cooldownMs == 6L * 3_600_000L, "cooldown 6h");
        check(d.spoilsEnabled && d.spoilsCap == 5, "spoils on, cap 5");
        check(d.elderRespawnMs == 6L * 3_600_000L, "elder respawn 6h");
        check(d.wither.caps().perEventDamage() == 60 && d.wither.caps().explosionPerTick() == 75,
                "wither caps 60/75");
        check(d.elderGuardian.caps().perEventDamage() == 35
                && d.elderGuardian.caps().explosionPerTick() == 40, "elder caps 35/40");
        check(d.warden.caps().perEventDamage() == 60 && d.warden.caps().explosionPerTick() == 75,
                "warden caps 60/75");
        check(d.wither.enabled() && d.elderGuardian.enabled() && d.warden.enabled(),
                "all chapters default on");
        check(d.wither.shareGate() && d.wither.presence() && d.elderGuardian.shareGate()
                && d.warden.presence(), "gates default on");
        check(d.dragonPresence, "dragon presence rule defaults on");
        check(d.wardenShareHeightBand == 12.0, "warden share height band defaults to 12");
        check(d.chapter(BossKind.WITHER) == d.wither, "chapter lookup: wither");
        check(d.chapter(BossKind.ENDER_DRAGON) == BossTrialConfig.Chapter.OFF,
                "chapter lookup: dragon carries no BossCaps chapter (its own machine governs)");

        // fromMap: empty and null both yield the defaults.
        BossTrialConfig empty = BossTrialConfig.fromMap(Map.of());
        check(empty.shareThreshold == 0.5 && empty.spoilsCap == 5, "empty map = defaults");
        check(BossTrialConfig.fromMap(null).cooldownMs == d.cooldownMs, "null map = defaults");

        // fromMap overrides land where they should.
        BossTrialConfig custom = BossTrialConfig.fromMap(Map.of(
                "credit.share-threshold", 0.75,
                "credit.cooldown-hours", 2.0,
                "elder-guardian.caps.per-event-damage", 0,
                "wither.enabled", false,
                "spoils.max-multiplier", 3));
        check(custom.shareThreshold == 0.75, "share override");
        check(custom.cooldownMs == 2L * 3_600_000L, "cooldown hours convert to ms");
        check(!custom.elderGuardian.caps().perEventEnabled(), "a 0 cap disables that cap from config");
        check(!custom.wither.enabled(), "chapter switch off from config");
        check(custom.spoilsCap == 3, "spoils cap override");

        // The warden height band rides its own key (Oct 19 amendment).
        BossTrialConfig band = BossTrialConfig.fromMap(Map.of(
                "warden.share-height-band", 20));
        check(band.wardenShareHeightBand == 20.0, "height band override respected (20)");
        BossTrialConfig bandOff = BossTrialConfig.fromMap(Map.of(
                "warden.share-height-band", 0));
        check(bandOff.wardenShareHeightBand == 0.0, "height band 0 kept (rule off)");

        // Junk values clamp toward safety, never crash.
        BossTrialConfig junk = BossTrialConfig.fromMap(Map.of(
                "credit.share-threshold", 7.0,
                "credit.presence-range", -50.0,
                "spoils.max-multiplier", 0,
                "warden.share-height-band", -4.0));
        check(junk.shareThreshold == 1.0, "share threshold clamps to 1.0");
        check(junk.presenceRange == 0.0, "presence range clamps to 0");
        check(junk.spoilsCap == 1, "spoils cap clamps to at least 1");
        check(junk.wardenShareHeightBand == 0.0, "negative height band clamps to 0 (rule off)");

        System.out.println("BossTrialConfigTest: " + passed + " passed, " + failed + " failed");
        if (failed > 0) {
            System.exit(1);
        }
    }
}


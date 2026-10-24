package com.winkreyes.excelraidbuff.core;

import java.util.HashMap;
import java.util.Map;

/**
 * Config parsing (BossTrialConfig pattern): the settled defaults
 * with no file at all, per-key overrides, and clamp-never-throw
 * validation — a typo'd config degrades toward vanilla and can
 * never weaken a raider below vanilla.
 */
public final class RaidBuffConfigTest {

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

    private static void eq(double actual, double expected, String label) {
        check(Math.abs(actual - expected) < 1e-9,
                label + " (expected " + expected + ", got " + actual + ")");
    }

    private static Map<String, Object> nested(Object... kv) {
        Map<String, Object> m = new HashMap<>();
        for (int i = 0; i < kv.length; i += 2) m.put((String) kv[i], kv[i + 1]);
        return m;
    }

    public static void main(String[] args) {
        // Defaults are the settled table.
        RaidBuffConfig d = RaidBuffConfig.defaults();
        check(d.enabled && d.announceEnabled && d.elitesEnabled, "defaults all on");
        check(d.announceMinOmen == 3, "default announce min omen 3");
        eq(d.healthMultiplier(1), 1.00, "default hp I");
        eq(d.healthMultiplier(2), 1.25, "default hp II");
        eq(d.healthMultiplier(3), 1.50, "default hp III");
        eq(d.healthMultiplier(4), 1.80, "default hp IV");
        eq(d.healthMultiplier(5), 2.20, "default hp V");
        eq(d.damageMultiplier(1), 1.00, "default dmg I");
        eq(d.damageMultiplier(2), 1.08, "default dmg II");
        eq(d.damageMultiplier(3), 1.15, "default dmg III");
        eq(d.damageMultiplier(4), 1.22, "default dmg IV");
        eq(d.damageMultiplier(5), 1.30, "default dmg V");
        eq(d.eliteShare(1), 0.00, "default share I");
        eq(d.eliteShare(2), 0.05, "default share II");
        eq(d.eliteShare(3), 0.10, "default share III");
        eq(d.eliteShare(4), 0.15, "default share IV");
        eq(d.eliteShare(5), 0.18, "default share V");
        check(d.eliteLevelBonus(3) == 5 && d.eliteLevelBonus(4) == 10 && d.eliteLevelBonus(5) == 15,
                "default level bonuses III/IV/V");
        check(d.eliteLevelBonus(1) == 0 && d.eliteLevelBonus(2) == 0, "default level bonuses I/II");
        eq(d.lateWaveHealthRamp, 1.15, "default late ramp");
        check(d.lateWaveCount == 2, "default late count");
        check(d.eliteLevelCapFallback == 150, "default cap fallback");
        check(d.sweepPeriodTicks == 40, "default sweep period");

        // Out-of-range omen reads degrade safely.
        eq(d.healthMultiplier(0), 1.0, "hp omen 0 degrades to vanilla");
        eq(d.healthMultiplier(6), 1.0, "hp omen 6 degrades to vanilla");
        eq(d.eliteShare(9), 0.0, "share omen 9 degrades to none");
        check(d.eliteLevelBonus(-1) == 0, "bonus omen -1 degrades to 0");

        // Null map = defaults, untouched.
        RaidBuffConfig n = RaidBuffConfig.fromMap(null);
        eq(n.healthMultiplier(5), 2.20, "null map keeps default hp V");
        check(n.enabled, "null map keeps enabled");

        // Overrides land per key; untouched keys keep defaults.
        RaidBuffConfig o = RaidBuffConfig.fromMap(nested(
                "enabled", false,
                "announce", nested("min-omen-level", 4, "line", "&eCustom line"),
                "scaling", nested(
                        "health-multiplier", nested("5", 2.50, "3", 1.60),
                        "damage-multiplier", nested("5", 1.45),
                        "late-wave-health-ramp", 1.25,
                        "late-wave-count", 3),
                "elites", nested(
                        "share", nested("5", 0.25),
                        "level-bonus", nested("5", 20),
                        "wave-ramp-from", 0.5,
                        "wave-ramp-to", 1.5),
                "sweep-period-ticks", 60));
        check(!o.enabled, "override enabled=false");
        check(o.announceMinOmen == 4, "override announce min omen");
        check("&eCustom line".equals(o.announceLine), "override announce line");
        eq(o.healthMultiplier(5), 2.50, "override hp V");
        eq(o.healthMultiplier(3), 1.60, "override hp III");
        eq(o.healthMultiplier(4), 1.80, "untouched hp IV keeps default");
        eq(o.damageMultiplier(5), 1.45, "override dmg V");
        eq(o.lateWaveHealthRamp, 1.25, "override late ramp");
        check(o.lateWaveCount == 3, "override late count");
        eq(o.eliteShare(5), 0.25, "override share V");
        check(o.eliteLevelBonus(5) == 20, "override bonus V");
        eq(o.eliteWaveRampFrom, 0.5, "override ramp from");
        eq(o.eliteWaveRampTo, 1.5, "override ramp to");
        check(o.sweepPeriodTicks == 60, "override sweep period");

        // Clamps: nothing below vanilla, nothing absurd.
        RaidBuffConfig c = RaidBuffConfig.fromMap(nested(
                "announce", nested("min-omen-level", 9),
                "scaling", nested(
                        "health-multiplier", nested("5", 0.50, "4", 99.0),
                        "damage-multiplier", nested("5", 0.10),
                        "late-wave-health-ramp", 0.5,
                        "late-wave-count", 99),
                "elites", nested(
                        "share", nested("5", 2.0, "4", -0.5),
                        "level-bonus", nested("5", -5),
                        "level-cap-fallback", 0),
                "sweep-period-ticks", 1));
        eq(c.healthMultiplier(5), 1.0, "hp can never weaken below vanilla");
        eq(c.healthMultiplier(4), 5.0, "hp clamped at the 5x ceiling");
        eq(c.damageMultiplier(5), 1.0, "dmg can never weaken below vanilla");
        eq(c.lateWaveHealthRamp, 1.0, "late ramp floored at identity");
        check(c.lateWaveCount == 8, "late count clamped at 8");
        check(c.announceMinOmen == 5, "announce min omen clamped at 5");
        eq(c.eliteShare(5), 1.0, "share clamped at 1");
        eq(c.eliteShare(4), 0.0, "share floored at 0");
        check(c.eliteLevelBonus(5) == 0, "bonus floored at 0");
        check(c.eliteLevelCapFallback == 1, "cap fallback floored at 1");
        check(c.sweepPeriodTicks == 10, "sweep period floored at 10");

        // Garbage types are ignored, never thrown on.
        RaidBuffConfig g = RaidBuffConfig.fromMap(nested(
                "enabled", "yes",
                "scaling", nested("health-multiplier", nested("5", "lots")),
                "elites", "not-a-map"));
        check(g.enabled, "garbage enabled falls back to default true");
        eq(g.healthMultiplier(5), 2.20, "garbage hp falls back to default");
        eq(g.eliteShare(5), 0.18, "garbage elites block falls back to default");

        // --- The reveal block (0.1.1): defaults, overrides, clamps.
        check(d.revealEnabled && d.revealAnnounce, "reveal defaults on (feature + line)");
        check(d.revealQuietSeconds == 180, "reveal default quiet 180s");
        check(d.revealLine.equals(RaidBuffConfig.DEFAULT_REVEAL_LINE), "reveal default line");

        RaidBuffConfig r = RaidBuffConfig.fromMap(nested(
                "reveal", nested("enabled", false, "quiet-seconds", 45,
                        "announce", false, "line", "&aCustom reveal.")));
        check(!r.revealEnabled, "reveal override: feature off");
        check(r.revealQuietSeconds == 45, "reveal override: quiet 45s");
        check(!r.revealAnnounce, "reveal override: announce off");
        check(r.revealLine.equals("&aCustom reveal."), "reveal override: custom line");

        RaidBuffConfig lo = RaidBuffConfig.fromMap(nested("reveal", nested("quiet-seconds", 5)));
        check(lo.revealQuietSeconds == 30, "reveal quiet floored at 30");
        RaidBuffConfig hi = RaidBuffConfig.fromMap(nested("reveal", nested("quiet-seconds", 5000)));
        check(hi.revealQuietSeconds == 900, "reveal quiet capped at 900");
        RaidBuffConfig gr = RaidBuffConfig.fromMap(nested(
                "reveal", nested("quiet-seconds", "soon", "line", "  ")));
        check(gr.revealQuietSeconds == 180, "reveal garbage quiet falls back to 180");
        check(gr.revealLine.equals(RaidBuffConfig.DEFAULT_REVEAL_LINE),
                "reveal blank line falls back to default");
        RaidBuffConfig bare = RaidBuffConfig.fromMap(nested("enabled", true));
        check(bare.revealEnabled && bare.revealQuietSeconds == 180 && bare.revealAnnounce,
                "a config with no reveal block runs the settled reveal defaults");

        // --- The expiry block (0.1.2): defaults, overrides, clamps.
        check(d.expiryEnabled, "expiry defaults on");
        eq(d.expiryHours, 6.0, "expiry default cap 6h");
        check(d.expiryCapMs() == 21_600_000L, "expiry default cap in ms");
        check(d.expiryLine.equals(RaidBuffConfig.DEFAULT_EXPIRY_LINE), "expiry default line");
        check(bare.expiryEnabled && bare.expiryCapMs() == 21_600_000L,
                "a config with no expiry block runs the settled expiry defaults");

        RaidBuffConfig x = RaidBuffConfig.fromMap(nested(
                "expiry", nested("enabled", false, "hours", 2.5, "line", "&aCustom expiry.")));
        check(!x.expiryEnabled, "expiry override: feature off");
        eq(x.expiryHours, 2.5, "expiry override: cap 2.5h");
        check(x.expiryCapMs() == 9_000_000L, "expiry override: cap in ms");
        check(x.expiryLine.equals("&aCustom expiry."), "expiry override: custom line");

        RaidBuffConfig xlo = RaidBuffConfig.fromMap(nested("expiry", nested("hours", 0)));
        eq(xlo.expiryHours, RaidBuffConfig.EXPIRY_HOURS_MIN, "expiry cap floored at the min");
        check(xlo.expiryCapMs() == 36_000L, "expiry min cap is 36s (rig-sized values stay legal)");
        RaidBuffConfig xhi = RaidBuffConfig.fromMap(nested("expiry", nested("hours", 10_000)));
        eq(xhi.expiryHours, RaidBuffConfig.EXPIRY_HOURS_MAX, "expiry cap capped at a week");
        RaidBuffConfig xg = RaidBuffConfig.fromMap(nested(
                "expiry", nested("hours", "six", "line", "  ")));
        eq(xg.expiryHours, 6.0, "expiry garbage hours falls back to 6");
        check(xg.expiryLine.equals(RaidBuffConfig.DEFAULT_EXPIRY_LINE),
                "expiry blank line falls back to default");

        System.out.println("RaidBuffConfigTest: " + passed + " passed, " + failed + " failed");
        if (failed > 0) System.exit(1);
    }
}

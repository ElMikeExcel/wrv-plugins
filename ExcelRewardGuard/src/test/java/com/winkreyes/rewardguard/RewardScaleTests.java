package com.winkreyes.rewardguard;

import java.util.UUID;

/**
 * Policy tests for the 1.1 reduced-reward math and config parsing (pure
 * logic, no server). The percent under test is Mike's chosen default, 65.
 * Rounding contract: scaled doubles are rounded half-up to 2 decimal
 * places; percent 0 yields exactly 0 (the 1.0 behavior the handlers pair
 * with cancellation); percent 100 is an exact pass-through with NO
 * rounding, so a misconfigured 100 can never shave a reward by dust.
 */
public final class RewardScaleTests {
    private static int assertions;
    private static void check(boolean condition) { assertions++; if (!condition) throw new AssertionError("check " + assertions); }

    private static void checkParse(Object raw, int percent, boolean adjusted) {
        RewardScale.ParsedPercent parsed = RewardScale.parsePercent(raw);
        check(parsed.percent() == percent);
        check(parsed.adjusted() == adjusted);
    }

    public static void main(String[] args) {
        // Scaling math at 65%: Jobs-style doubles (money, points, XP).
        check(RewardScale.scale(10.0, 65) == 6.5);
        check(RewardScale.scale(20.0, 65) == 13.0);
        check(RewardScale.scale(15.0, 65) == 9.75);
        check(RewardScale.scale(45.0, 65) == 29.25);
        check(RewardScale.scale(12.0, 65) == 7.8);
        check(RewardScale.scale(8.0, 65) == 5.2);
        check(RewardScale.scale(1.0, 65) == 0.65);
        check(RewardScale.scale(0.0, 65) == 0.0);
        // Rounding behavior: half-up to cents, never floating-point dust.
        check(RewardScale.scale(0.1, 65) == 0.07);   // 0.065 -> 0.07
        check(RewardScale.scale(0.01, 65) == 0.01);  // 0.0065 -> 0.01, not silently zero
        check(RewardScale.scale(3.33, 65) == 2.16);  // 2.1645 -> 2.16
        check(RewardScale.scale(2.5, 65) == 1.63);   // 1.625 -> 1.63 (half-up)

        // Scaling math at 65%: mcMMO-style floats.
        check(RewardScale.scaleFloat(100.0f, 65) == 65.0f);
        check(RewardScale.scaleFloat(7.0f, 65) == 4.55f);
        check(RewardScale.scaleFloat(3.0f, 65) == 1.95f);
        check(RewardScale.scaleFloat(50.0f, 65) == 32.5f);

        // Percent 0 equivalence with 1.0: everything scales to exactly zero
        // (the handlers additionally cancel at 0, as 1.0 did).
        check(RewardScale.scale(10.0, 0) == 0.0);
        check(RewardScale.scale(0.01, 0) == 0.0);
        check(RewardScale.scaleFloat(100.0f, 0) == 0.0f);

        // Percent 100 is a true no-op: exact values back, no rounding.
        check(RewardScale.scale(10.0, 100) == 10.0);
        check(RewardScale.scale(0.065, 100) == 0.065);
        check(RewardScale.scale(3.333, 100) == 3.333);
        check(RewardScale.scaleFloat(4.55f, 100) == 4.55f);

        // Config parsing: missing, valid, out-of-range, invalid.
        checkParse(null, 65, false);        // missing key -> shipped default, normal path
        checkParse(65, 65, false);
        checkParse(0, 0, false);
        checkParse(100, 100, false);
        checkParse(150, 100, true);        // clamped down
        checkParse(-5, 0, true);           // clamped up
        checkParse("65", 65, false);       // numeric string counts
        checkParse(" 42 ", 42, false);     // whitespace tolerated
        checkParse("abc", 65, true);       // unparseable -> default, flagged
        checkParse(65.9, 65, true);        // fractional -> truncated, flagged
        checkParse(Boolean.TRUE, 65, true); // wrong type -> default, flagged

        // Window + scale, the way McMmoVeto combines them: a party member's
        // SHARED_PVE-style gain inside the window is SCALED to 65%, not
        // zeroed; a gain just outside the window is not matched at all, so
        // the handler leaves it at full value.
        UUID killer = UUID.randomUUID();
        UUID party = UUID.randomUUID();
        UUID stranger = UUID.randomUUID();
        var window = new XpSuppression(40.0, 2L);
        window.plant(killer, "world", 0, 64, 0, 50L);
        check(window.shouldSuppress(party, "world", 20, 64, 0, 50L, true));
        float partyGain = RewardScale.scaleFloat(100.0f, 65);
        check(partyGain == 65.0f);
        check(partyGain != 0.0f);
        check(window.shouldSuppress(killer, "world", 100, 64, 0, 50L, true));
        check(!window.shouldSuppress(stranger, "world", 41, 64, 0, 50L, true));
        check(!window.shouldSuppress(party, "world", 0, 64, 0, 50L, false));

        System.out.println("PASS " + assertions + " reward-scale assertions (65% math, rounding, 0/100 endpoints, config parsing, window+party scaling)");
    }
}

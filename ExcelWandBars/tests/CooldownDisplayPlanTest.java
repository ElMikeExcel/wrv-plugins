package com.winkreyes.excelwandbars;
import com.winkreyes.excelwandbars.core.CooldownDisplayPlan;

/**
 * Unit checks for CooldownDisplayPlan (0.3.13 accurate-cooldown display).
 * Plain-JVM, no JUnit — run by the build's test loop like the other
 * suites. Every case is a shape from the Nov 8, 2026 investigation:
 *  - Heal active (time-to-cast null, cooldown running),
 *  - mana-wait conflation (Wither shape: cooldown short, mana wait long),
 *  - Familiar on Bedrock (cooldown past the Geyser 64 cap),
 *  - familiar-length on Java (past Magic's own 99),
 *  - the truthful middle (number already equals the cooldown),
 *  - cooldown expired with a mana wait still running (tag must clear),
 *  - legacy mode = the 0.3.10 rule, unchanged.
 */
public final class CooldownDisplayPlanTest {
    static int checks = 0, failures = 0;
    static void expect(CooldownDisplayPlan.Decision d, boolean tag, long sec, String label) {
        checks++;
        if (d.tag != tag || (tag && d.seconds != sec)) {
            failures++;
            System.out.println("FAIL " + label + ": got tag=" + d.tag + " sec=" + d.seconds + " want tag=" + tag + " sec=" + sec);
        }
    }
    public static void main(String[] args) {
        // Heal specimen: active HoT, ttc null (Magic writes 1), cooldown 6 s running -> tag shows 6.
        expect(CooldownDisplayPlan.decide(true, 6000L, null, 99, 60), true, 6, "heal active ttc null java");
        expect(CooldownDisplayPlan.decide(true, 6000L, null, 64, 60), true, 6, "heal active ttc null bedrock");
        // Mana-wait conflation (Wither shape): cooldown 6 s, mana wait makes ttc 30 s -> tag shows the COOLDOWN, 6.
        expect(CooldownDisplayPlan.decide(true, 6000L, 30000L, 99, 60), true, 6, "mana wait java");
        expect(CooldownDisplayPlan.decide(true, 6000L, 30000L, 64, 60), true, 6, "mana wait bedrock");
        // Familiar 240 s: Bedrock cap 64 -> tag 4:00 (240); Java cap 99 -> tag too.
        expect(CooldownDisplayPlan.decide(true, 240000L, 240000L, 64, 60), true, 240, "familiar bedrock");
        expect(CooldownDisplayPlan.decide(true, 240000L, 240000L, 99, 60), true, 240, "familiar java");
        // Truthful middle: number == cooldown, under both caps -> no tag (name stays clean).
        expect(CooldownDisplayPlan.decide(true, 8000L, 8000L, 99, 60), false, 0, "truthful 8s java");
        expect(CooldownDisplayPlan.decide(true, 8000L, 8000L, 64, 60), false, 0, "truthful 8s bedrock");
        expect(CooldownDisplayPlan.decide(true, 60000L, 60000L, 64, 60), false, 0, "truthful 60s bedrock at cap edge");
        expect(CooldownDisplayPlan.decide(true, 65000L, 65000L, 64, 60), true, 65, "65s bedrock past cap");
        expect(CooldownDisplayPlan.decide(true, 65000L, 65000L, 99, 60), false, 0, "65s java under cap");
        // Cooldown expired, mana wait remains -> NO tag (the tag names cooldown; mana has its own bar).
        expect(CooldownDisplayPlan.decide(true, 0L, 45000L, 99, 60), false, 0, "cd 0 mana wait java");
        expect(CooldownDisplayPlan.decide(true, 0L, 45000L, 64, 60), false, 0, "cd 0 mana wait bedrock");
        // Unreadable cooldown -> no tag (never guess a number).
        expect(CooldownDisplayPlan.decide(true, null, 30000L, 99, 60), false, 0, "cooldown unreadable");
        // Sub-second cooldown: ceil to 1; ttc equal -> truthful; ttc null -> tag 1.
        expect(CooldownDisplayPlan.decide(true, 400L, 400L, 99, 60), false, 0, "sub-second truthful");
        expect(CooldownDisplayPlan.decide(true, 400L, null, 99, 60), true, 1, "sub-second ttc null");
        // Legacy mode reproduces 0.3.10 exactly: threshold on time-to-cast, value from time-to-cast.
        expect(CooldownDisplayPlan.decide(false, 6000L, null, 99, 60), false, 0, "legacy ttc null");
        expect(CooldownDisplayPlan.decide(false, 6000L, 30000L, 99, 60), false, 0, "legacy ttc 30 under threshold");
        expect(CooldownDisplayPlan.decide(false, 6000L, 61000L, 99, 60), true, 61, "legacy ttc 61 over threshold");
        expect(CooldownDisplayPlan.decide(false, 240000L, 240000L, 64, 60), true, 240, "legacy familiar");
        System.out.println("SUITE core.CooldownDisplayPlanTest checks=" + checks + " failures=" + failures);
        if (failures > 0) System.exit(1);
    }
}

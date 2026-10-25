package com.winkreyes.excelclaimfly;

import com.winkreyes.excelclaimfly.core.CombatClock;
import com.winkreyes.excelclaimfly.core.FlightRules;
import com.winkreyes.excelclaimfly.core.FlightRules.Block;
import com.winkreyes.excelclaimfly.core.FlightRules.Facts;
import java.util.UUID;

public final class TestMain {
    static int pass, fail;
    static void check(boolean c, String w) { if (c) pass++; else { fail++; System.out.println("FAIL " + w); } }
    public static void main(String[] a) {
        // decide: order of reasons
        check(FlightRules.decide(new Facts(true, true, false, true, false, true)) == Block.NONE, "in own claim, no fight = fly");
        check(FlightRules.decide(new Facts(false, true, false, true, false, true)) == Block.NO_PASS, "no pass");
        check(FlightRules.decide(new Facts(true, false, false, true, false, true)) == Block.OTHER_MODE, "creative left alone");
        check(FlightRules.decide(new Facts(true, true, true, true, false, true)) == Block.BLOCKED_WORLD, "blocked world");
        check(FlightRules.decide(new Facts(true, true, false, false, false, true)) == Block.OUTSIDE_CLAIM, "outside claim");
        check(FlightRules.decide(new Facts(true, true, false, true, true, true)) == Block.COMBAT, "combat pauses");
        check(FlightRules.decide(new Facts(true, true, false, true, true, false)) == Block.NONE, "combat rule off = fly in fight");
        check(FlightRules.decide(new Facts(true, true, false, false, true, true)) == Block.OUTSIDE_CLAIM, "outside wins over combat");
        // clock/words
        check(FlightRules.clock(30 * 60_000L).equals("30:00"), "clock 30:00");
        check(FlightRules.clock(61_500L).equals("1:02"), "clock rounds up");
        check(FlightRules.clock(3_725_000L).equals("1:02:05"), "clock hours");
        check(FlightRules.clock(-5).equals("0:00"), "clock never negative");
        check(FlightRules.words(22 * 60_000L + 5000).equals("23 min"), "words min rounds up");
        check(FlightRules.words(30_000).equals("1 min"), "words <1 says 1 min"); check(FlightRules.words(4 * 60_000L + 59_000).equals("5 min"), "words 4:59 says 5 min"); check(FlightRules.words(0).equals("0 min"), "words zero");
        check(FlightRules.words(65 * 60_000L).equals("1 hr 5 min"), "words hr");
        check(FlightRules.words(120 * 60_000L).equals("2 hr"), "words 2 hr");
        // warnings
        int[] w = {5, 1};
        check(FlightRules.warningDue(6 * 60_000L, Integer.MAX_VALUE, w) == 0, "no warning at 6 min");
        check(FlightRules.warningDue(5 * 60_000L, Integer.MAX_VALUE, w) == 5, "5 min warning");
        check(FlightRules.warningDue(4 * 60_000L, 5, w) == 0, "5 min warning only once");
        check(FlightRules.warningDue(60_000L, 5, w) == 1, "1 min warning");
        check(FlightRules.warningDue(30_000L, 1, w) == 0, "1 min only once");
        check(FlightRules.warningDue(50_000L, Integer.MAX_VALUE, w) == 1, "joining at 50 s gives only the 1 min warning");
        check(FlightRules.warningDue(0, Integer.MAX_VALUE, w) == 0, "no warning after end");
        // extend cap
        long m = 60_000L;
        check(FlightRules.canExtend(0, 30 * m, 120 * m), "first buy ok");
        check(FlightRules.canExtend(90 * m, 30 * m, 120 * m), "extend to exactly cap ok");
        check(!FlightRules.canExtend(91 * m, 30 * m, 120 * m), "over cap refused");
        // combat clock
        CombatClock c = new CombatClock(10_000);
        UUID id = UUID.randomUUID();
        check(!c.tagged(id, 0), "untagged");
        c.tag(id, 1000);
        check(c.tagged(id, 5000), "tagged within 10 s");
        check(!c.tagged(id, 11_000), "quiet after 10 s");
        c.tag(id, 20_000); c.tag(id, 28_000);
        check(c.tagged(id, 37_000), "a new hit restarts the 10 s");
        c.clear(id); check(!c.tagged(id, 37_001), "death clears");
        // money
        check(ExcelClaimFly.money(1000).equals("1,000"), "money 1,000");
        check(ExcelClaimFly.money(1234.5).equals("1,234.50"), "money cents");
        System.out.println("UNIT pass=" + pass + " fail=" + fail);
        if (fail > 0) System.exit(1);
    }
}

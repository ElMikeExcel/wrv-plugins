package com.winkreyes.exceltogether;

import com.gamingmesh.jobs.api.JobsPrePaymentEvent;
import org.bukkit.OfflinePlayer;
import java.lang.reflect.Proxy;

/** Dependency-free test runner (no JUnit needed). Exit code 1 on any failure. */
public class PartyTest {
    static int fail = 0, pass = 0;
    static void check(String n, boolean ok) { if (ok) pass++; else { fail++; System.out.println("FAIL " + n); } }
    static boolean eq(double a, double b) { return Math.abs(a - b) < 1e-9; }

    static OfflinePlayer fake() {
        return (OfflinePlayer) Proxy.newProxyInstance(PartyTest.class.getClassLoader(),
                new Class<?>[]{OfflinePlayer.class}, (p, m, a) -> {
                    Class<?> r = m.getReturnType();
                    if (r == boolean.class) return false;
                    if (r == int.class) return 0;
                    if (r == long.class) return 0L;
                    if (r == double.class) return 0.0;
                    return null;
                });
    }

    static JobsPrePaymentEvent ev(double money, double exp) {
        return new JobsPrePaymentEvent(fake(), null, money, exp, 0.0, null, null, null, null);
    }

    public static void main(String[] a) {
        PartyBonus b = new PartyBonus(1.25, 50);
        check("apply", eq(b.apply(10), 12.5));
        check("zero", eq(b.apply(0), 0));
        check("negative untouched", eq(b.apply(-4), -4));
        check("within edge 50", b.within(true, 30, 40, 0));
        check("outside 50.1", !b.within(true, 50.1, 0, 0));
        check("3d distance", !b.within(true, 40, 40, 40));
        check("other world", !b.within(false, 0, 0, 0));
        PartyBonus bad = new PartyBonus(Double.NaN, -3);
        check("bad cfg defaults", eq(bad.multiplier(), 1.25) && eq(bad.radius(), 50));
        PartyBonus c = new PartyBonus(2.0, 10);
        check("custom", eq(c.apply(3), 6) && c.within(true, 10, 0, 0) && !c.within(true, 10.5, 0, 0));

        // Event level
        PartyListener yes = new PartyListener(b, p -> true);
        JobsPrePaymentEvent e = ev(8.0, 4.0);
        yes.onPay(e);
        check("event money", eq(e.getAmount(), 10.0));
        check("event exp", eq(e.getExp(), 5.0));
        check("event points untouched", eq(e.getPoints(), 0.0));
        PartyListener no = new PartyListener(b, p -> false);
        JobsPrePaymentEvent e2 = ev(8.0, 4.0);
        no.onPay(e2);
        check("no partner money", eq(e2.getAmount(), 8.0));
        check("no partner exp", eq(e2.getExp(), 4.0));
        // Stacking model: Jobs applies amt + amt*boost after the event. weekend boost +20% example.
        double base = 8.0, jobsBoost = 0.20;
        double afterEvent = ev(base, 1).getAmount();
        yes.onPay(e2 = ev(base, 1));
        double finalPay = e2.getAmount() + e2.getAmount() * jobsBoost;
        check("stack multiplicative", eq(finalPay, base * 1.25 * 1.20));
        check("not replaced", finalPay > base * 1.25);
        JobsPrePaymentEvent e3 = ev(-5, 0);
        yes.onPay(e3);
        check("penalty untouched", eq(e3.getAmount(), -5));
        JobsPrePaymentEvent e4 = ev(5, 0);
        yes.onPay(e4);
        check("money only", eq(e4.getAmount(), 6.25) && eq(e4.getExp(), 0));

        System.out.println(pass + " passed, " + fail + " failed");
        System.exit(fail == 0 ? 0 : 1);
    }
}

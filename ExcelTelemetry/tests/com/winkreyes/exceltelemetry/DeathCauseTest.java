package com.winkreyes.exceltelemetry;

import com.winkreyes.exceltelemetry.core.LastDamage;
import java.util.UUID;

/** Death-cause memory: fresh hits attribute; stale hits do not. */
public class DeathCauseTest {
    static int fails = 0;
    static void check(boolean c, String m) {
        if (!c) { fails++; System.out.println("FAIL " + m); } else System.out.println("ok   " + m);
    }

    public static void main(String[] args) {
        LastDamage ld = new LastDamage();
        UUID playerA = UUID.randomUUID();
        check(ld.fresh(playerA, 1_000) == null, "no hit recorded -> no cause (honest unknown)");

        ld.record(playerA, new LastDamage.Hit("ENTITY_ATTACK", "mob", "ZOMBIE", "z-uuid", 10_000));
        LastDamage.Hit fresh = ld.fresh(playerA, 25_000);
        check(fresh != null && "ZOMBIE".equals(fresh.damagerType()), "fresh hit attributes the death");
        check(ld.fresh(playerA, 40_001) == null, "hit older than the freshness window is not blamed");

        ld.record(playerA, new LastDamage.Hit("FALL", "environment", null, null, 50_000));
        LastDamage.Hit fall = ld.fresh(playerA, 51_000);
        check(fall != null && "environment".equals(fall.damagerKind()), "latest hit wins (fall finish)");

        ld.clear(playerA);
        check(ld.fresh(playerA, 51_500) == null && ld.size() == 0, "clear on death/quit");

        System.out.println(fails == 0 ? "ALL PASS" : "FAILED " + fails);
        if (fails > 0) System.exit(1);
    }
}

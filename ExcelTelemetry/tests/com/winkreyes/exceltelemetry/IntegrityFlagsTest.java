package com.winkreyes.exceltelemetry;

import com.winkreyes.exceltelemetry.core.IntegrityFlags;
import java.util.List;
import java.util.Map;

/** Flag thresholds: at, above, below - flags are data, never verdicts. */
public class IntegrityFlagsTest {
    static int fails = 0;
    static void check(boolean c, String m) {
        if (!c) { fails++; System.out.println("FAIL " + m); } else System.out.println("ok   " + m);
    }

    static boolean hasType(List<Map<String, Object>> flags, String type) {
        return flags.stream().anyMatch(f -> type.equals(f.get("type")));
    }

    public static void main(String[] args) {
        var t = IntegrityFlags.Thresholds.defaults();

        var bedKill = IntegrityFlags.forBossKill("ender_dragon", 300, 0.85, 550, t);
        check(hasType(bedKill, "explosion_share_high"), "bed-heavy dragon kill is flagged for a look");
        check(!hasType(bedKill, "implausibly_fast_kill"), "a 300 s bed kill is not 'fast'");

        var honest = IntegrityFlags.forBossKill("ender_dragon", 300, 0.10, 550, t);
        check(honest.isEmpty(), "an ordinary sword-and-bow kill raises no flags");

        var edge = IntegrityFlags.forBossKill("wither", 90, 0.60, 300, t);
        check(hasType(edge, "explosion_share_high"), "share exactly AT the threshold flags (>=)");

        var under = IntegrityFlags.forBossKill("wither", 90, 0.59, 300, t);
        check(under.isEmpty(), "share just under the threshold does not flag");

        var fast = IntegrityFlags.forBossKill("wither", 8, 0.0, 300, t);
        check(hasType(fast, "implausibly_fast_kill"), "an 8-second wither kill is flagged");
        var fastEdge = IntegrityFlags.forBossKill("wither", 20, 0.0, 300, t);
        check(!hasType(fastEdge, "implausibly_fast_kill"), "exactly at the floor is not flagged (<)");

        var noDamage = IntegrityFlags.forBossKill("warden", 5, 0.0, 0, t);
        check(!hasType(noDamage, "explosion_share_high"), "zero-damage edge never flags share");

        var elite = IntegrityFlags.forEliteKill(0.75, 120, t);
        check(hasType(elite, "explosion_share_high"), "elite kills get the explosion flag too");
        var eliteMelee = IntegrityFlags.forEliteKill(0.0, 120, t);
        check(eliteMelee.isEmpty(), "melee elite kill: no flags");

        System.out.println(fails == 0 ? "ALL PASS" : "FAILED " + fails);
        if (fails > 0) System.exit(1);
    }
}

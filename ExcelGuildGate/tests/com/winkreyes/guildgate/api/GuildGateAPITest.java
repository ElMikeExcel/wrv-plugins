package com.winkreyes.guildgate.api;

import com.winkreyes.excelguildgate.SealState;

import java.util.LinkedHashSet;
import java.util.List;

/**
 * Plain main-method checks for GuildGateAPI null-safety and live readouts.
 * Same harness style as SealStateTest: counted checks, exit 1 on failure.
 */
public final class GuildGateAPITest {

    private static int checks = 0;
    private static int failures = 0;

    private static void check(String name, boolean condition) {
        checks++;
        if (!condition) {
            failures++;
            System.out.println("FAIL: " + name);
        }
    }

    private static void checkEq(String name, Object expected, Object actual) {
        checks++;
        if (!expected.equals(actual)) {
            failures++;
            System.out.println("FAIL: " + name + " expected=" + expected + " actual=" + actual);
        }
    }

    public static void main(String[] args) {
        List<String> five = List.of("ender_dragon", "wither", "elder_guardian", "warden", "raid");

        // Before any bind (plugin not enabled yet): safe locked defaults.
        GuildGateAPI.unbind();
        check("unbound: locked", !GuildGateAPI.isUnlocked());
        checkEq("unbound: broken", 0, GuildGateAPI.sealsBrokenCount());
        checkEq("unbound: total", 0, GuildGateAPI.sealsTotalCount());

        // Bound to a fresh state: real counts, still locked.
        SealState s = new SealState(new LinkedHashSet<>(five), new LinkedHashSet<>(five));
        GuildGateAPI.bind(s);
        check("bound fresh: locked", !GuildGateAPI.isUnlocked());
        checkEq("bound fresh: broken", 0, GuildGateAPI.sealsBrokenCount());
        checkEq("bound fresh: total", 5, GuildGateAPI.sealsTotalCount());

        // Live view: the API reflects state changes without rebinding.
        s.credit("wither", "Mike", 1L);
        s.credit("raid", null, 2L);
        checkEq("live: broken tracks state", 2, GuildGateAPI.sealsBrokenCount());
        check("live: still locked", !GuildGateAPI.isUnlocked());
        s.openAll(3L);
        check("live: unlocked at completion", GuildGateAPI.isUnlocked());
        checkEq("live: broken at completion", 5, GuildGateAPI.sealsBrokenCount());

        // Enabled-subset state: totals count enabled seals only.
        SealState sub = new SealState(new LinkedHashSet<>(five),
                new LinkedHashSet<>(List.of("ender_dragon", "wither")));
        GuildGateAPI.bind(sub);
        checkEq("subset: total is enabled count", 2, GuildGateAPI.sealsTotalCount());
        sub.credit("ender_dragon", "Mike", 1L);
        sub.credit("wither", "Mike", 2L);
        check("subset: unlocked", GuildGateAPI.isUnlocked());

        // Unbind returns to the safe defaults (plugin disabled).
        GuildGateAPI.unbind();
        check("after unbind: locked", !GuildGateAPI.isUnlocked());
        checkEq("after unbind: broken", 0, GuildGateAPI.sealsBrokenCount());
        checkEq("after unbind: total", 0, GuildGateAPI.sealsTotalCount());

        System.out.println("GuildGateAPITest: " + (checks - failures) + "/" + checks
                + " checks passed" + (failures == 0 ? " - ALL PASS" : " - " + failures + " FAILED"));
        if (failures > 0) {
            System.exit(1);
        }
    }
}

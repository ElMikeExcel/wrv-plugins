package com.winkreyes.rewardguard;

import java.util.UUID;

/**
 * Policy tests for the mcMMO suppression window (pure logic, no server).
 * Window semantics under test: radius 40, window 2 ticks, kill-reason only.
 */
public final class XpSuppressionTests {
    private static int assertions;
    private static void check(boolean condition) { assertions++; if (!condition) throw new AssertionError("check " + assertions); }

    public static void main(String[] args) {
        UUID killer = UUID.randomUUID();
        UUID party = UUID.randomUUID();
        UUID stranger = UUID.randomUUID();

        // Killer match: any distance (bow kill), same tick and inside the window.
        var s = new XpSuppression(40.0, 2L);
        s.plant(killer, "world", 0, 64, 0, 100L);
        check(s.shouldSuppress(killer, "world", 100, 64, 0, 100L, true));
        check(s.shouldSuppress(killer, "world", 100, 64, 0, 101L, true));
        check(s.shouldSuppress(killer, "world", 100, 64, 0, 102L, true));
        // Window expired.
        check(!s.shouldSuppress(killer, "world", 100, 64, 0, 103L, true));
        // Killer match still needs the same world.
        check(!s.shouldSuppress(killer, "world_nether", 0, 64, 0, 100L, true));

        // Party share: a nearby non-killer inside the radius is suppressed.
        var s2 = new XpSuppression(40.0, 2L);
        s2.plant(killer, "world", 0, 64, 0, 50L);
        check(s2.shouldSuppress(party, "world", 20, 64, 0, 50L, true));
        check(s2.shouldSuppress(party, "world", 0, 64, 39, 50L, true));
        // Just outside the radius: not suppressed.
        check(!s2.shouldSuppress(party, "world", 41, 64, 0, 50L, true));
        check(!s2.shouldSuppress(stranger, "world", 0, 64, -41, 50L, true));
        // Wrong world: not suppressed even at the same coordinates.
        check(!s2.shouldSuppress(party, "world_the_end", 0, 64, 0, 50L, true));

        // Non-kill gains are never suppressed, even for the killer at the spot.
        check(!s2.shouldSuppress(killer, "world", 0, 64, 0, 50L, false));
        check(!s2.shouldSuppress(party, "world", 0, 64, 0, 50L, false));

        // Null inputs never suppress.
        check(!s2.shouldSuppress(null, "world", 0, 64, 0, 50L, true));
        check(!s2.shouldSuppress(party, null, 0, 64, 0, 50L, true));

        // Killer unknown (mob died to a tamed pet or unattributed damage):
        // the radius rule still applies around the death spot.
        var s3 = new XpSuppression(40.0, 2L);
        s3.plant(null, "world", 500, 70, 500, 10L);
        check(s3.shouldSuppress(party, "world", 505, 70, 500, 10L, true));
        check(!s3.shouldSuppress(party, "world", 600, 70, 500, 10L, true));

        // Multiple markers: a later, different death can match when the first cannot.
        var s4 = new XpSuppression(40.0, 2L);
        s4.plant(killer, "world", 0, 64, 0, 10L);
        s4.plant(killer, "world", 1000, 64, 1000, 20L);
        check(s4.shouldSuppress(party, "world", 1005, 64, 1000, 20L, true));
        check(!s4.shouldSuppress(party, "world", 1005, 64, 1000, 11L, true));

        // Expiry purges markers instead of leaking them.
        var s5 = new XpSuppression(40.0, 2L);
        s5.plant(killer, "world", 0, 64, 0, 10L);
        check(s5.activeMarkers(12L) == 1);
        check(s5.activeMarkers(13L) == 0);
        check(!s5.shouldSuppress(killer, "world", 0, 64, 0, 13L, true));

        // A gain timestamped before the death never matches it.
        var s6 = new XpSuppression(40.0, 2L);
        s6.plant(killer, "world", 0, 64, 0, 100L);
        check(!s6.shouldSuppress(killer, "world", 0, 64, 0, 99L, true));

        System.out.println("PASS " + assertions + " suppression-window assertions (killer/radius/world/reason/expiry/multi-marker)");
    }
}

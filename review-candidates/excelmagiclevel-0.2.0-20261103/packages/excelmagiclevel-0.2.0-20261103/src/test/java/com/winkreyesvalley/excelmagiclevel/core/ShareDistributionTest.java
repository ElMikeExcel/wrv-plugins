package com.winkreyesvalley.excelmagiclevel.core;

import org.junit.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * ExcelMagicLevel — the share gates (SPEC §4.5): who may receive a copy.
 * The amounts themselves are ShareMath's (pinned in TablesTest); these
 * tests pin the RECIPIENT rules — range, world, the earner exclusion,
 * and the solo case — including the exact-boundary behaviour.
 */
public class ShareDistributionTest {
    private static final UUID EARNER = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID BUDDY = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID THIRD = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final String WORLD = "world";

    private static ShareDistribution.MemberView view(UUID id, String world, double x, double y, double z) {
        return new ShareDistribution.MemberView(id, world, x, y, z);
    }

    private static ShareDistribution.MemberView earner() {
        return view(EARNER, WORLD, 0.5, 100, 10.5);
    }

    @Test
    public void earnerAloneReceivesNothing() {
        // Earner solo (SPEC §4.5 edge): the candidate set holds only the
        // earner and the distribution is empty — share can never self-pay.
        List<UUID> out = ShareDistribution.recipients(earner(), List.of(earner()), 50.0);
        assertTrue(out.isEmpty());
    }

    @Test
    public void nearbyMemberInSameWorldReceives() {
        List<UUID> out = ShareDistribution.recipients(earner(),
                List.of(earner(), view(BUDDY, WORLD, 5.5, 100, 10.5)), 50.0);
        assertEquals(List.of(BUDDY), out);
    }

    @Test
    public void exactBoundaryIsInRange() {
        // 30-40-50 triangle: distance exactly 50.0 — "within 50 blocks"
        // includes the boundary (SPEC §4.5, Together's own comparison).
        List<UUID> out = ShareDistribution.recipients(earner(),
                List.of(view(BUDDY, WORLD, 30.5, 100, 50.5)), 50.0);
        assertEquals(List.of(BUDDY), out);
    }

    @Test
    public void justPastTheBoundaryPaysNothing() {
        List<UUID> out = ShareDistribution.recipients(earner(),
                List.of(view(BUDDY, WORLD, 30.5, 100, 50.6)), 50.0);
        assertTrue(out.isEmpty());
    }

    @Test
    public void verticalDistanceCounts() {
        // 51 blocks straight up is out of range — the distance is 3D.
        List<UUID> out = ShareDistribution.recipients(earner(),
                List.of(view(BUDDY, WORLD, 0.5, 151, 10.5)), 50.0);
        assertTrue(out.isEmpty());
    }

    @Test
    public void crossWorldPaysNothing() {
        List<UUID> out = ShareDistribution.recipients(earner(),
                List.of(view(BUDDY, "world_the_end", 0.5, 100, 10.5)), 50.0);
        assertTrue(out.isEmpty());
    }

    @Test
    public void earnerIsExcludedEvenWhenListedAsCandidate() {
        List<UUID> out = ShareDistribution.recipients(earner(),
                List.of(view(EARNER, WORLD, 0.5, 100, 10.5), view(BUDDY, WORLD, 1.5, 100, 10.5)), 50.0);
        assertEquals(List.of(BUDDY), out);
    }

    @Test
    public void malformedViewsNeverPay() {
        List<UUID> out = ShareDistribution.recipients(earner(),
                List.of(view(BUDDY, WORLD, Double.NaN, 100, 10.5),
                        view(THIRD, null, 1.5, 100, 10.5)), 50.0);
        assertTrue(out.isEmpty());
    }

    @Test
    public void zeroRadiusPaysNobody() {
        List<UUID> out = ShareDistribution.recipients(earner(),
                List.of(view(BUDDY, WORLD, 0.5, 100, 10.5)), 0.0);
        assertTrue(out.isEmpty());
    }

    @Test
    public void orderIsDeterministic() {
        List<UUID> out = ShareDistribution.recipients(earner(),
                List.of(view(THIRD, WORLD, 2.5, 100, 10.5), view(BUDDY, WORLD, 1.5, 100, 10.5)), 50.0);
        assertEquals(List.of(BUDDY, THIRD), out);
    }

    @Test
    public void nullInputsAreSafe() {
        assertTrue(ShareDistribution.recipients(null, List.of(earner()), 50.0).isEmpty());
        assertTrue(ShareDistribution.recipients(earner(), null, 50.0).isEmpty());
        assertTrue(ShareDistribution.recipients(earner(), List.of(), 50.0).isEmpty());
    }
}

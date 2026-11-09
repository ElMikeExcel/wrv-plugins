package com.winkreyes.excelclaims;

import org.bukkit.Location;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** Claim creation/overlap/subdivision/block-charge rules. */
public class ClaimRulesTest {

    static final UUID ALICE = UUID.randomUUID();
    static final UUID BOB = UUID.randomUUID();

    @TempDir Path dir;
    Core core;
    ClaimStore store;

    @BeforeEach
    void setup() throws Exception {
        core = TestKit.newCore(dir);
        store = core.store();
    }

    private ClaimService.CreateResult create(UUID owner, int x1, int z1, int x2, int z2) {
        return core.service().createClaim("world", x1, 64, z1, x2, 80, z2, owner, null, null);
    }

    @Test
    void createChargesAreaBlocks() {
        // Fresh accounts start with the initial 100 blocks.
        ClaimService.CreateResult res = create(ALICE, 0, 0, 9, 9); // 10x10 = 100
        assertEquals(ClaimService.CreateOutcome.OK, res.outcome);
        assertEquals(0, store.remainingBlocks(ALICE));
        assertEquals(100, store.usedArea(ALICE));
    }

    @Test
    void tooSmallDenied() {
        assertEquals(ClaimService.CreateOutcome.TOO_SMALL, create(ALICE, 0, 0, 3, 29).outcome); // width 4
        assertEquals(ClaimService.CreateOutcome.TOO_SMALL, create(ALICE, 0, 0, 8, 8).outcome);   // 9x9 = 81 < 100
    }

    @Test
    void notEnoughBlocksDenied() {
        assertEquals(ClaimService.CreateOutcome.NOT_ENOUGH_BLOCKS, create(ALICE, 0, 0, 19, 19).outcome); // 400 > 100
    }

    @Test
    void overlapDeniedButAdjacencyFine() {
        store.playerData(ALICE).accrued = 100000;
        store.playerData(BOB).accrued = 100000;
        assertEquals(ClaimService.CreateOutcome.OK, create(ALICE, 0, 0, 19, 19).outcome);
        assertEquals(ClaimService.CreateOutcome.OVERLAP, create(BOB, 10, 10, 29, 29).outcome);
        // The live-server pattern: claims packed edge-to-edge (Jester's towers).
        assertEquals(ClaimService.CreateOutcome.OK, create(BOB, 20, 0, 39, 19).outcome);
    }

    @Test
    void worldDisabledDenied() {
        ClaimService.CreateResult res = core.service().createClaim("world_nether",
                0, 64, 0, 9, 80, 9, ALICE, null, null);
        assertEquals(ClaimService.CreateOutcome.WORLD_DISABLED, res.outcome);
    }

    @Test
    void subdivisionRules() {
        store.playerData(ALICE).accrued = 100000;
        ClaimService.CreateResult parent = create(ALICE, 0, 0, 39, 39);
        assertEquals(ClaimService.CreateOutcome.OK, parent.outcome);
        int remainingBefore = store.remainingBlocks(ALICE);
        // Inside: fine, and free.
        ClaimService.CreateResult sub = core.service().createClaim("world",
                5, 64, 5, 14, 80, 14, BOB, parent.claim, null);
        assertEquals(ClaimService.CreateOutcome.OK, sub.outcome);
        assertEquals(ALICE, sub.claim.owner, "subdivisions keep the parent's owner");
        assertEquals(remainingBefore, store.remainingBlocks(ALICE), "subdivisions consume no blocks");
        // Straddling the border: refused.
        ClaimService.CreateResult straddle = core.service().createClaim("world",
                35, 64, 5, 44, 80, 14, BOB, parent.claim, null);
        assertEquals(ClaimService.CreateOutcome.OUTSIDE_PARENT, straddle.outcome);
        // Sibling overlap: refused.
        ClaimService.CreateResult overlap = core.service().createClaim("world",
                10, 64, 10, 19, 80, 19, BOB, parent.claim, null);
        assertEquals(ClaimService.CreateOutcome.SUBDIVISION_OVERLAP, overlap.outcome);
        // Adjacent sibling: fine.
        ClaimService.CreateResult adjacent = core.service().createClaim("world",
                15, 64, 5, 24, 80, 14, BOB, parent.claim, null);
        assertEquals(ClaimService.CreateOutcome.OK, adjacent.outcome);
    }

    @Test
    void deleteParentRemovesSubdivisions() {
        store.playerData(ALICE).accrued = 100000;
        ClaimService.CreateResult parent = create(ALICE, 0, 0, 39, 39);
        core.service().createClaim("world", 5, 64, 5, 14, 80, 14, ALICE, parent.claim, null);
        assertEquals(2, store.claimCount());
        int removed = core.service().deleteClaim(parent.claim);
        assertEquals(2, removed);
        assertEquals(0, store.claimCount());
        assertEquals(100000, store.remainingBlocks(ALICE), "blocks return on deletion (ratio 1.0)");
    }

    @Test
    void adminClaimsAreFreeAndOwnerless() {
        ClaimService.CreateResult res = create(null, 0, 0, 49, 49);
        assertEquals(ClaimService.CreateOutcome.OK, res.outcome);
        assertTrue(res.claim.isAdmin());
        assertNull(res.claim.owner);
    }

    @Test
    void transferRules() {
        store.playerData(ALICE).accrued = 100000;
        store.playerData(BOB).accrued = 100000;
        ClaimService.CreateResult parent = create(ALICE, 0, 0, 39, 39);
        ClaimService.CreateResult sub = core.service().createClaim("world",
                5, 64, 5, 14, 80, 14, ALICE, parent.claim, null);
        assertFalse(core.service().transfer(sub.claim, BOB, null), "subdivisions can't transfer");
        assertTrue(core.service().transfer(parent.claim, BOB, null));
        assertEquals(BOB, parent.claim.owner);
        assertEquals(BOB, sub.claim.owner, "subdivisions follow the parent's title");
    }

    @Test
    void transferDeniedWhenNewOwnerCantAfford() {
        store.playerData(ALICE).accrued = 100000;
        ClaimService.CreateResult parent = create(ALICE, 0, 0, 39, 39); // 1600 blocks
        // BOB has only the initial 100.
        assertFalse(core.service().transfer(parent.claim, BOB, null));
        assertEquals(ALICE, parent.claim.owner);
    }

    @Test
    void resizeRules() {
        store.playerData(ALICE).accrued = 100000;
        store.playerData(BOB).accrued = 100000;
        ClaimService.CreateResult a = create(ALICE, 0, 0, 19, 19);
        create(BOB, 30, 0, 49, 19);
        // Grow into the gap: fine.
        assertEquals(ClaimService.ResizeOutcome.OK,
                core.service().resizeTo(a.claim, 0, 0, 29, 19, null));
        // Grow into the neighbor: refused.
        assertEquals(ClaimService.ResizeOutcome.OVERLAP,
                core.service().resizeTo(a.claim, 0, 0, 35, 19, null));
        // Shrink below minimum: refused.
        assertEquals(ClaimService.ResizeOutcome.TOO_SMALL,
                core.service().resizeTo(a.claim, 0, 0, 3, 19, null));
    }

    @Test
    void autoClaimIsNineByNineAndFreeOfSizeRules() {
        Location chest = new Location(TestKit.fakeWorld("world"), 100, 64, 100);
        ClaimService.CreateResult res = core.service().createAutoClaim(chest,
                TestKit.fakePlayer(ALICE, "Alice", java.util.Set.of()));
        assertEquals(ClaimService.CreateOutcome.OK, res.outcome);
        assertEquals(81, res.claim.area(), "radius 4 = 9x9 = 81 blocks");
        assertTrue(res.claim.chestClaim);
        assertEquals(19, store.remainingBlocks(ALICE), "100 initial - 81");
        // Second auto-claim shape (another player, overlapping): refused silently.
        Location chest2 = new Location(TestKit.fakeWorld("world"), 102, 64, 102);
        ClaimService.CreateResult res2 = core.service().createAutoClaim(chest2,
                TestKit.fakePlayer(BOB, "Bob", java.util.Set.of()));
        assertEquals(ClaimService.CreateOutcome.OVERLAP, res2.outcome);
    }

    @Test
    void claimCountLimit() throws Exception {
        // Rebuild a core whose config caps claims at 1 per player.
        Core limited = TestKit.newCore(dir.resolve("limited"));
        limited.config().maxClaimsPerPlayer = 1;
        limited.store().playerData(ALICE).accrued = 100000;
        assertEquals(ClaimService.CreateOutcome.OK, limited.service()
                .createClaim("world", 0, 64, 0, 19, 80, 19, ALICE, null, null).outcome);
        assertEquals(ClaimService.CreateOutcome.CLAIM_LIMIT, limited.service()
                .createClaim("world", 30, 64, 0, 49, 80, 19, ALICE, null, null).outcome);
    }
}

// ExcelClaims tests — Iris / Mike

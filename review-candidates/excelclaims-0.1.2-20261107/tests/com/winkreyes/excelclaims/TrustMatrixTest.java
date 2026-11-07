package com.winkreyes.excelclaims;

import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.ClaimPermission;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The trust-tier matrix, including the ExcelAutoSort §6.4 configure
 * case: owner and PermissionTrust (managers) configure; Build,
 * Container and Access trust never do.
 */
public class TrustMatrixTest {

    static final UUID OWNER = UUID.randomUUID();
    static final UUID MANAGER = UUID.randomUUID();
    static final UUID BUILDER = UUID.randomUUID();
    static final UUID CONTAINER = UUID.randomUUID();
    static final UUID ACCESS = UUID.randomUUID();
    static final UUID INVENTORY = UUID.randomUUID();
    static final UUID STRANGER = UUID.randomUUID();

    @TempDir Path dir;
    Core core;
    ClaimRecord record;
    Claim claim;

    @BeforeEach
    void setup() throws Exception {
        core = TestKit.newCore(dir);
        ClaimStore store = core.store();
        store.playerData(OWNER).accrued = 100000;
        ClaimService.CreateResult res = core.service().createClaim("world",
                0, 64, 0, 39, 80, 39, OWNER, null, null);
        assertEquals(ClaimService.CreateOutcome.OK, res.outcome);
        record = res.claim;
        core.service().setTrust(record, MANAGER.toString(), ClaimPermission.Manage, null);
        core.service().setTrust(record, BUILDER.toString(), ClaimPermission.Build, null);
        core.service().setTrust(record, CONTAINER.toString(), ClaimPermission.Container, null);
        core.service().setTrust(record, ACCESS.toString(), ClaimPermission.Access, null);
        core.service().setTrust(record, INVENTORY.toString(), ClaimPermission.Inventory, null);
        claim = core.facadeFor(record);
    }

    private Player player(UUID id) {
        return TestKit.fakePlayer(id, "P-" + id.toString().substring(0, 4), Set.of());
    }

    private boolean allowed(UUID id, ClaimPermission tier) {
        return claim.checkPermission(player(id), tier, null) == null;
    }

    @Test
    void ownerAndManagerPassEverything() {
        for (ClaimPermission tier : ClaimPermission.values()) {
            assertTrue(allowed(OWNER, tier), "owner should pass " + tier);
            assertTrue(allowed(MANAGER, tier), "manager should pass " + tier);
        }
    }

    @Test
    void builderTier() {
        assertTrue(allowed(BUILDER, ClaimPermission.Build));
        assertTrue(allowed(BUILDER, ClaimPermission.Container));
        assertTrue(allowed(BUILDER, ClaimPermission.Access));
        assertTrue(allowed(BUILDER, ClaimPermission.Inventory));
        assertFalse(allowed(BUILDER, ClaimPermission.Manage));
        assertFalse(allowed(BUILDER, ClaimPermission.Edit));
    }

    @Test
    void containerTier() {
        assertTrue(allowed(CONTAINER, ClaimPermission.Container));
        assertTrue(allowed(CONTAINER, ClaimPermission.Access));
        assertFalse(allowed(CONTAINER, ClaimPermission.Build));
        assertFalse(allowed(CONTAINER, ClaimPermission.Manage));
    }

    @Test
    void accessTier() {
        assertTrue(allowed(ACCESS, ClaimPermission.Access));
        assertFalse(allowed(ACCESS, ClaimPermission.Container));
        assertFalse(allowed(ACCESS, ClaimPermission.Build));
    }

    @Test
    void inventoryIsViewOnly() {
        assertTrue(allowed(INVENTORY, ClaimPermission.Inventory));
        assertTrue(allowed(INVENTORY, ClaimPermission.Access));
        // View-only trust must not open containers for taking:
        assertFalse(allowed(INVENTORY, ClaimPermission.Container));
        assertNotNull(claim.allowContainers(player(INVENTORY)));
    }

    @Test
    void strangerDeniedEverything() {
        for (ClaimPermission tier : ClaimPermission.values()) {
            if (tier == ClaimPermission.Manage) continue;
            assertFalse(allowed(STRANGER, tier), "stranger must fail " + tier);
        }
        assertNotNull(claim.allowBuild(player(STRANGER), null));
    }

    /** The AutoSort §6.4 configure rule, evaluated exactly as its GP hook evaluates it. */
    private boolean canConfigureLikeAutoSort(UUID me, boolean adminPerm) {
        // GpRightsHook: admin claim -> excelautosort.admin holders; else
        // claim owner; else managers membership (PermissionTrust).
        if (claim.isAdminClaim()) return adminPerm;
        if (me.equals(claim.getOwnerID())) return true;
        return claim.managers.contains(me.toString());
    }

    @Test
    void autoSortConfigureCase() {
        assertTrue(canConfigureLikeAutoSort(OWNER, false));
        assertTrue(canConfigureLikeAutoSort(MANAGER, false));
        assertFalse(canConfigureLikeAutoSort(BUILDER, false), "Build trust must NOT configure");
        assertFalse(canConfigureLikeAutoSort(CONTAINER, false), "ContainerTrust must NOT configure (Mike's rule)");
        assertFalse(canConfigureLikeAutoSort(ACCESS, false), "AccessTrust must NOT configure");
        assertFalse(canConfigureLikeAutoSort(STRANGER, false));
    }

    @Test
    void inheritanceIntoSubdivision() {
        ClaimService.CreateResult sub = core.service().createClaim("world",
                5, 64, 5, 14, 80, 14, OWNER, record, null);
        assertEquals(ClaimService.CreateOutcome.OK, sub.outcome);
        Claim subClaim = core.facadeFor(sub.claim);
        // Parent's builder is a builder in the subdivision by inheritance.
        assertNull(subClaim.checkPermission(player(BUILDER), ClaimPermission.Build, null));
        assertEquals(ClaimPermission.Build, subClaim.getPermission(BUILDER.toString()));
        // inheritNothing cuts the inheritance.
        sub.claim.inheritNothing = true;
        assertNotNull(subClaim.checkPermission(player(BUILDER), ClaimPermission.Build, null));
        // ...but the owner still owns.
        assertNull(subClaim.checkPermission(player(OWNER), ClaimPermission.Build, null));
    }

    @Test
    void publicTrust() {
        core.service().setTrust(record, "public", ClaimPermission.Access, null);
        assertTrue(allowed(STRANGER, ClaimPermission.Access));
        assertFalse(allowed(STRANGER, ClaimPermission.Container));
    }

    /**
     * The Landmarks dual-membership contract, exactly as rig-proven
     * against the old plugin (Nov 5, 2026): the buyer is granted Build
     * via setPermission AND added to the managers list directly.
     * getPermission must then report Build (Landmarks' post-grant
     * verification), the managers membership must hold (AutoSort's
     * renter rule), and Manage-tier rights checks must still pass
     * (the buyer can trust friends in their plot).
     */
    @Test
    void landmarksDualMembershipContract() {
        UUID buyer = UUID.randomUUID();
        claim.setPermission(buyer.toString(), ClaimPermission.Build);
        if (!claim.managers.contains(buyer.toString())) claim.managers.add(buyer.toString());
        assertEquals(ClaimPermission.Build, claim.getPermission(buyer.toString()),
                "getPermission reports the Build grant, as the old API did");
        assertTrue(claim.managers.contains(buyer.toString()), "AutoSort reads the managers list");
        assertTrue(canConfigureLikeAutoSort(buyer, false), "the plot renter configures");
        assertNull(claim.checkPermission(player(buyer), ClaimPermission.Manage, null),
                "manager rights checks still pass in full");
        assertNull(claim.checkPermission(player(buyer), ClaimPermission.Build, null));
    }

    /**
     * isAdminClaim() is a top-level-only concept for API callers
     * (AutoSort SPEC §6.4): the spawn admin claim reports true; a
     * Landmarks plot inside it — admin-titled, owner null — reports
     * false, so the renter-as-manager rule (not the staff rule)
     * governs inside plots.
     */
    @Test
    void adminClaimFacadeSemantics() {
        ClaimService.CreateResult admin = core.service().createClaim("world",
                200, 64, 200, 299, 80, 299, null, null, null);
        assertEquals(ClaimService.CreateOutcome.OK, admin.outcome);
        Claim adminFacade = core.facadeFor(admin.claim);
        assertTrue(adminFacade.isAdminClaim());
        assertNull(adminFacade.getOwnerID());

        ClaimService.CreateResult plot = core.service().createClaim("world",
                210, 64, 210, 219, 80, 219, UUID.randomUUID(), admin.claim, null);
        assertEquals(ClaimService.CreateOutcome.OK, plot.outcome);
        Claim plotFacade = core.facadeFor(plot.claim);
        assertTrue(plot.claim.isAdmin(), "engine-side the plot still carries the admin title");
        assertFalse(plotFacade.isAdminClaim(), "facade: a subdivision is not an admin claim");
        assertFalse(claim.isAdminClaim(), "an ordinary player claim is not an admin claim");
    }

    @Test
    void allowEditMatchesBuildTier() {
        // The documented interpretation Magic relies on: Build-trusted
        // players pass the edit gate, untrusted players do not.
        assertNull(claim.allowEdit(player(BUILDER)));
        assertNull(claim.allowEdit(player(OWNER)));
        assertNotNull(claim.allowEdit(player(CONTAINER)));
        assertNotNull(claim.allowEdit(player(STRANGER)));
    }
}

// ExcelClaims tests — Iris / Mike

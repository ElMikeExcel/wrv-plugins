package com.winkreyes.excelclaims;

import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.ClaimPermission;
import me.ryanhamshire.GriefPrevention.CreateClaimResult;
import me.ryanhamshire.GriefPrevention.DataStore;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import me.ryanhamshire.GriefPrevention.PlayerData;
import me.ryanhamshire.GriefPrevention.events.ClaimChangeEvent;
import me.ryanhamshire.GriefPrevention.events.ClaimCreatedEvent;
import me.ryanhamshire.GriefPrevention.events.ClaimDeletedEvent;
import me.ryanhamshire.GriefPrevention.events.ClaimModifiedEvent;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.UUID;
import java.util.Vector;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The facade signature contract: every probe the inventoried
 * consumers run against the claims API, replayed reflectively —
 * Guide/Leaderboard's field+method probe, TreeCleanup's link,
 * Shops'/BossTrial's store field, the childproof screens'
 * checkPermission probe, Magic's instanceof + allowEdit + pvp field,
 * and Landmarks'/AutoSort's compile-time surface (those two are also
 * compile-checked against these classes in the build script).
 */
public class FacadeSignatureTest {

    @Test
    void mainClassShape() throws Exception {
        Field instance = GriefPrevention.class.getField("instance");
        assertEquals(GriefPrevention.class, instance.getType());
        assertTrue(java.lang.reflect.Modifier.isStatic(instance.getModifiers()));
        Field store = GriefPrevention.class.getField("dataStore");
        assertEquals(DataStore.class, store.getType());
        // Magic: the plugin instance must BE a GriefPrevention (JavaPlugin subclass).
        assertTrue(org.bukkit.plugin.java.JavaPlugin.class.isAssignableFrom(GriefPrevention.class));
        // Magic reads this field directly.
        Field pvp = GriefPrevention.class.getField("config_pvp_noCombatInPlayerLandClaims");
        assertEquals(boolean.class, pvp.getType());
        Method enabled = GriefPrevention.class.getMethod("claimsEnabledForWorld", World.class);
        assertEquals(boolean.class, enabled.getReturnType());
    }

    @Test
    void dataStoreShape() throws Exception {
        assertEquals(Claim.class, DataStore.class
                .getMethod("getClaimAt", Location.class, boolean.class, Claim.class).getReturnType());
        assertEquals(Claim.class, DataStore.class.getMethod("getClaim", long.class).getReturnType());
        Method create = DataStore.class.getMethod("createClaim", World.class,
                int.class, int.class, int.class, int.class, int.class, int.class,
                UUID.class, Claim.class, Long.class, Player.class);
        assertEquals(CreateClaimResult.class, create.getReturnType());
        DataStore.class.getMethod("saveClaim", Claim.class);
        DataStore.class.getMethod("deleteClaim", Claim.class);
        assertEquals(PlayerData.class, DataStore.class.getMethod("getPlayerData", UUID.class).getReturnType());
        DataStore.class.getMethod("changeClaimOwner", Claim.class, UUID.class);
        // The transfer exception is a DataStore inner class, as documented by Landmarks.
        Class<?> nte = Class.forName("me.ryanhamshire.GriefPrevention.DataStore$NoTransferException");
        assertTrue(Exception.class.isAssignableFrom(nte));
        CreateClaimResult r = new CreateClaimResult();
        assertFalse(r.succeeded);
        assertNull(r.claim);
    }

    @Test
    void claimShape() throws Exception {
        Field parent = Claim.class.getField("parent");
        assertEquals(Claim.class, parent.getType());
        Field managers = Claim.class.getField("managers");
        assertEquals(ArrayList.class, managers.getType());
        assertEquals(Long.class, Claim.class.getMethod("getID").getReturnType());
        assertEquals(int.class, Claim.class.getMethod("getArea").getReturnType());
        assertEquals(UUID.class, Claim.class.getMethod("getOwnerID").getReturnType());
        assertEquals(String.class, Claim.class.getMethod("getOwnerName").getReturnType());
        assertEquals(boolean.class, Claim.class.getMethod("isAdminClaim").getReturnType());
        assertEquals(Location.class, Claim.class.getMethod("getLesserBoundaryCorner").getReturnType());
        assertEquals(Location.class, Claim.class.getMethod("getGreaterBoundaryCorner").getReturnType());
        Claim.class.getMethod("setPermission", String.class, ClaimPermission.class);
        assertEquals(ClaimPermission.class, Claim.class.getMethod("getPermission", String.class).getReturnType());
        // TreeCleanup + Magic:
        assertEquals(String.class, Claim.class.getMethod("allowBuild", Player.class, Material.class).getReturnType());
        assertEquals(String.class, Claim.class.getMethod("allowEdit", Player.class).getReturnType());
        // Childproof screens: checkPermission(UUID, ClaimPermission, Event) -> Supplier (null = allowed).
        Method check = Claim.class.getMethod("checkPermission", UUID.class, ClaimPermission.class, Event.class);
        assertEquals(Supplier.class, check.getReturnType());
        Claim.class.getMethod("checkPermission", Player.class, ClaimPermission.class, Event.class);
    }

    @Test
    void playerDataShape() throws Exception {
        assertEquals(int.class, PlayerData.class.getMethod("getAccruedClaimBlocks").getReturnType());
        assertEquals(int.class, PlayerData.class.getMethod("getBonusClaimBlocks").getReturnType());
        assertEquals(Vector.class, PlayerData.class.getMethod("getClaims").getReturnType());
        assertEquals(int.class, PlayerData.class.getMethod("getRemainingClaimBlocks").getReturnType());
    }

    @Test
    void claimPermissionConstants() {
        assertNotNull(ClaimPermission.valueOf("Build"));
        assertNotNull(ClaimPermission.valueOf("Container"));
        assertNotNull(ClaimPermission.valueOf("Access"));
        assertNotNull(ClaimPermission.valueOf("Manage"));
        assertNotNull(ClaimPermission.valueOf("Edit"));
        assertNotNull(ClaimPermission.valueOf("Inventory"));
    }

    @Test
    void eventShapes() throws Exception {
        assertTrue(Event.class.isAssignableFrom(ClaimCreatedEvent.class));
        assertTrue(Cancellable.class.isAssignableFrom(ClaimCreatedEvent.class));
        ClaimCreatedEvent.class.getMethod("getCreator");
        ClaimCreatedEvent.class.getMethod("getClaim");
        ClaimDeletedEvent.class.getMethod("getClaim");
        ClaimModifiedEvent.class.getMethod("getClaim");
        ClaimChangeEvent.class.getMethod("getFrom");
        ClaimChangeEvent.class.getMethod("getTo");
        assertTrue(Cancellable.class.isAssignableFrom(ClaimChangeEvent.class));
    }
}

// ExcelClaims tests — Iris / Mike

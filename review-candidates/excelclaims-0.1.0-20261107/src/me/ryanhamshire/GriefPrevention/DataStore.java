package me.ryanhamshire.GriefPrevention;

import com.winkreyes.excelclaims.ClaimRecord;
import com.winkreyes.excelclaims.ClaimService;
import com.winkreyes.excelclaims.Core;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * ExcelClaims facade — the claim data store, API-shaped like the
 * public GriefPrevention DataStore type it stands in for (clean-room:
 * signatures and semantics from the API's observable behavior). All
 * operations delegate to the ExcelClaims engine, so API-made claims
 * and shovel-made claims are the same claims.
 * Part of ExcelClaims (Iris / Mike).
 */
public class DataStore {

    /** Thrown when a subdivision transfer is attempted — subdivisions can't change owners. */
    public static class NoTransferException extends Exception {
        public NoTransferException(String message) { super(message); }
    }

    private final Core core;

    public DataStore(Core core) {
        this.core = core;
    }

    // ------------------------------------------------------------ lookup

    public synchronized Claim getClaimAt(Location location, boolean ignoreHeight, Claim cachedClaim) {
        if (location == null || location.getWorld() == null) return null;
        if (cachedClaim != null && cachedClaim.contains(location, ignoreHeight, false)) return cachedClaim;
        return lookup(location, ignoreHeight, false);
    }

    public synchronized Claim getClaimAt(Location location, boolean ignoreHeight,
                                         boolean ignoreSubclaims, Claim cachedClaim) {
        if (location == null || location.getWorld() == null) return null;
        if (cachedClaim != null && cachedClaim.contains(location, ignoreHeight, ignoreSubclaims)) return cachedClaim;
        return lookup(location, ignoreHeight, ignoreSubclaims);
    }

    private Claim lookup(Location location, boolean ignoreHeight, boolean ignoreSubclaims) {
        String world = location.getWorld().getName();
        int x = location.getBlockX(), y = location.getBlockY(), z = location.getBlockZ();
        List<ClaimRecord> candidates = new ArrayList<>();
        for (ClaimRecord r : core.store().all()) {
            if (!r.world.equals(world) || !r.containsColumn(x, z)) continue;
            if (ignoreSubclaims && r.isSubdivision()) continue;
            if (!ignoreHeight && !r.contains(x, y, z)) continue;
            candidates.add(r);
        }
        if (candidates.isEmpty()) return null;
        candidates.sort(Comparator.comparingInt(r -> depthOf(r)));
        return core.facadeFor(candidates.get(candidates.size() - 1));
    }

    private int depthOf(ClaimRecord r) {
        int d = 0;
        ClaimRecord c = r;
        while (c != null && c.parentId >= 0) { d++; c = core.store().byId(c.parentId); }
        return d;
    }

    public synchronized Claim getClaim(long claimID) {
        ClaimRecord r = core.store().byId(claimID);
        return r == null ? null : core.facadeFor(r);
    }

    public Collection<Claim> getClaims() {
        List<Claim> out = new ArrayList<>();
        for (ClaimRecord r : core.store().all()) out.add(core.facadeFor(r));
        return out;
    }

    /** Claims intersecting the given chunk (the old store's chunk query). */
    public Collection<Claim> getClaims(int chunkX, int chunkZ) {
        List<Claim> out = new ArrayList<>();
        int minX = chunkX << 4, maxX = minX + 15, minZ = chunkZ << 4, maxZ = minZ + 15;
        for (ClaimRecord r : core.store().all()) {
            if (r.gx >= minX && r.lx <= maxX && r.gz >= minZ && r.lz <= maxZ) out.add(core.facadeFor(r));
        }
        return out;
    }

    // ------------------------------------------------------------ mutation

    public synchronized CreateClaimResult createClaim(World world, int lesserX, int greaterX,
            int lesserY, int greaterY, int lesserZ, int greaterZ, UUID ownerID,
            Claim parentClaim, Long claimID, Player creator) {
        CreateClaimResult result = new CreateClaimResult();
        if (world == null) { result.succeeded = false; return result; }
        ClaimRecord parent = parentClaim == null ? null : parentClaim.record;
        ClaimService.CreateResult res = core.service().createClaim(world.getName(),
                lesserX, lesserY, lesserZ, greaterX, greaterY, greaterZ,
                ownerID, parent, creator);
        result.succeeded = res.outcome == ClaimService.CreateOutcome.OK;
        result.claim = result.succeeded ? core.facadeFor(res.claim) : null;
        return result;
    }

    public synchronized CreateClaimResult createClaim(World world, int lesserX, int greaterX,
            int lesserY, int greaterY, int lesserZ, int greaterZ, UUID ownerID,
            Claim parentClaim, Long claimID, Player creator, boolean fireEvent) {
        // The extra flag exists in the old API for internal callers; our
        // engine always fires the (cancellable) creation event.
        return createClaim(world, lesserX, greaterX, lesserY, greaterY, lesserZ, greaterZ,
                ownerID, parentClaim, claimID, creator);
    }

    public synchronized void saveClaim(Claim claim) {
        if (claim == null) return;
        // The facade managers list writes through to the record on every
        // edit; fold the list back wholesale anyway so any list-side
        // change is what gets saved, then persist.
        claim.record.managers.clear();
        claim.record.managers.addAll(claim.managers);
        claim.record.lastModifiedAt = System.currentTimeMillis();
        core.store().saveClaim(claim.record);
    }

    public synchronized void deleteClaim(Claim claim) {
        if (claim == null) return;
        core.service().deleteClaim(claim.record);
    }

    public synchronized void deleteClaim(Claim claim, boolean deleteSubclaims) {
        deleteClaim(claim);
    }

    public synchronized void deleteClaimsForPlayer(UUID playerID, boolean deleteSubclaims) {
        core.service().deleteAllClaimsFor(playerID);
    }

    public synchronized void changeClaimOwner(Claim claim, UUID newOwner) throws NoTransferException {
        if (claim == null) return;
        if (claim.record.parentId >= 0) {
            throw new NoTransferException("Subdivisions can't be transferred.");
        }
        core.service().transfer(claim.record, newOwner, null);
    }

    // ------------------------------------------------------------ player data

    public synchronized PlayerData getPlayerData(UUID playerID) {
        if (playerID == null) return null;
        return new PlayerData(core.store().playerData(playerID), core);
    }

    public void savePlayerData(UUID playerID, PlayerData playerData) {
        if (playerID == null || playerData == null) return;
        core.store().savePlayer(playerData.record);
    }

    public void savePlayerDataSync(UUID playerID, PlayerData playerData) {
        savePlayerData(playerID, playerData);
    }
}

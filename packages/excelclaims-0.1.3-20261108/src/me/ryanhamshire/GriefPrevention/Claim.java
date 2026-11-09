package me.ryanhamshire.GriefPrevention;

import com.winkreyes.excelclaims.ClaimRecord;
import com.winkreyes.excelclaims.Core;
import com.winkreyes.excelclaims.Trust;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;

import java.util.ArrayList;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * ExcelClaims facade — a claim, API-shaped like the public
 * GriefPrevention Claim type it stands in for (clean-room: signatures
 * and semantics from the API's observable behavior, verified against
 * the installed jar's public surface). Wraps the engine's ClaimRecord;
 * one facade exists per record, so {@link #parent} identity is stable.
 * Part of ExcelClaims (Iris / Mike).
 */
public class Claim {

    /** Claim managers (PermissionTrust), as a live list: edits land on the claim. */
    public final ArrayList<String> managers;
    /** Parent claim for subdivisions; null for top-level claims. */
    public Claim parent;
    public boolean inDataStore = true;
    public boolean areExplosivesAllowed = false;
    public boolean doorsOpen = false;

    final ClaimRecord record;
    final Core core;
    private boolean refreshingManagers;

    public Claim(ClaimRecord record, Core core) {
        this.record = record;
        this.core = core;
        this.managers = new LiveManagers();
        refreshManagersFromRecord();
        if (record.parentId >= 0) {
            ClaimRecord p = core.store().byId(record.parentId);
            if (p != null) this.parent = core.facadeFor(p);
        }
    }

    /** A managers list that writes through to the record's manager set. */
    private final class LiveManagers extends ArrayList<String> {
        @Override public boolean add(String s) {
            boolean changed = super.add(s);
            if (changed && !refreshingManagers) record.managers.add(s);
            return changed;
        }
        @Override public void add(int i, String s) {
            super.add(i, s);
            if (!refreshingManagers) record.managers.add(s);
        }
        @Override public boolean addAll(java.util.Collection<? extends String> c) {
            boolean changed = super.addAll(c);
            if (changed && !refreshingManagers) record.managers.addAll(c);
            return changed;
        }
        @Override public boolean remove(Object o) {
            boolean changed = super.remove(o);
            if (changed && !refreshingManagers) record.managers.remove(o);
            return changed;
        }
        @Override public boolean removeAll(java.util.Collection<?> c) {
            boolean changed = super.removeAll(c);
            if (changed && !refreshingManagers) record.managers.removeAll(c);
            return changed;
        }
        @Override public void clear() {
            super.clear();
            if (!refreshingManagers) record.managers.clear();
        }
    }

    /** Rebuild the live managers list from the record (engine-side trust changes). */
    public void refreshManagersFromRecord() {
        refreshingManagers = true;
        try {
            managers.clear();
            managers.addAll(record.managers);
        } finally {
            refreshingManagers = false;
        }
    }

    // ------------------------------------------------------------ identity

    public Long getID() { return record.id; }

    /**
     * True only for a TOP-LEVEL claim with no owner. A subdivision of
     * an admin claim carries the admin title (owner null) but is NOT
     * itself an admin claim for API callers — ExcelAutoSort's §6.4
     * rule depends on exactly this: inside a Landmarks plot the
     * renter-as-manager rule governs, while inside the top-level
     * admin claim (spawn) staff permission governs. (The engine's own
     * protection code reads ClaimRecord.isAdmin() plus the top-level
     * claim explicitly, so admin subdivisions stay admin-protected.)
     */
    public boolean isAdminClaim() { return record.isAdmin() && !record.isSubdivision(); }

    public UUID getOwnerID() { return record.owner; }

    public String getOwnerName() { return core.nameOf(record.owner); }

    public int getArea() { return record.area(); }

    public int getWidth() { return record.widthX(); }

    public int getHeight() { return record.height(); }

    private org.bukkit.World worldOrNull() {
        try {
            return Bukkit.getServer() == null ? null : Bukkit.getWorld(record.world);
        } catch (Throwable t) {
            return null;
        }
    }

    public Location getLesserBoundaryCorner() {
        return new Location(worldOrNull(), record.lx, record.ly, record.lz);
    }

    public Location getGreaterBoundaryCorner() {
        return new Location(worldOrNull(), record.gx, record.gy, record.gz);
    }

    public boolean contains(Location location, boolean ignoreHeight, boolean ignoreSubclaims) {
        if (location == null || location.getWorld() == null) return false;
        if (!location.getWorld().getName().equals(record.world)) return false;
        if (ignoreSubclaims && record.isSubdivision()) return false;
        int x = location.getBlockX(), y = location.getBlockY(), z = location.getBlockZ();
        return ignoreHeight ? record.containsColumn(x, z) : record.contains(x, y, z);
    }

    // ------------------------------------------------------------ permissions

    public Supplier<String> checkPermission(Player player, ClaimPermission permission, Event event) {
        if (player == null) return denial();
        if (core.isIgnoring(player)) return null;
        boolean ok = Trust.check(record, player.getUniqueId(), player.getName(), permission,
                core.adminOverride(player, record), core.store()::byId);
        return ok ? null : denial();
    }

    public Supplier<String> checkPermission(UUID playerID, ClaimPermission permission, Event event) {
        if (playerID == null) return denial();
        Player online = null;
        try { if (Bukkit.getServer() != null) online = Bukkit.getPlayer(playerID); }
        catch (Throwable ignored) { }
        boolean admin = false;
        if (online != null) {
            if (core.isIgnoring(online)) return null;
            admin = core.adminOverride(online, record);
        }
        String name = offlineName(playerID);
        boolean ok = Trust.check(record, playerID, name, permission, admin, core.store()::byId);
        return ok ? null : denial();
    }

    private Supplier<String> denial() {
        String owner = getOwnerName();
        return () -> "You don't have permission in " + owner + "'s claim.";
    }

    /** Build gate (null = allowed). The old API's spell/build integrations key off this. */
    public String allowBuild(Player player, Material material) {
        return checkPermission(player, ClaimPermission.Build, null) == null ? null
                : "You can't build in " + getOwnerName() + "'s claim.";
    }

    /** Edit gate (null = allowed): owner, managers, and Build-trusted players. */
    public String allowEdit(Player player) {
        return checkPermission(player, ClaimPermission.Build, null) == null ? null
                : "You can't edit " + getOwnerName() + "'s claim.";
    }

    public String allowAccess(Player player) {
        return checkPermission(player, ClaimPermission.Access, null) == null ? null
                : "You can't access " + getOwnerName() + "'s claim.";
    }

    public String allowContainers(Player player) {
        return checkPermission(player, ClaimPermission.Container, null) == null ? null
                : "You can't use containers in " + getOwnerName() + "'s claim.";
    }

    /** The entry's effective tier here (lists + inheritance; ownership is not a tier). */
    public ClaimPermission getPermission(String playerID) {
        if (playerID == null) return null;
        UUID uuid = null;
        try { uuid = UUID.fromString(playerID); } catch (IllegalArgumentException ignored) { }
        ClaimRecord cursor = record;
        while (cursor != null) {
            // The reported tier (Trust.reportedOn), NOT the rights
            // ladder: see Trust for the Landmarks dual-membership case.
            ClaimPermission p = Trust.reportedOn(cursor, uuid == null ? null : uuid.toString(), playerID);
            if (p != null) return p;
            if (cursor.inheritNothing || cursor.parentId < 0) break;
            cursor = core.store().byId(cursor.parentId);
        }
        return null;
    }

    /** Offline name lookup that tolerates a headless (test) runtime. */
    private static String offlineName(UUID playerID) {
        try {
            if (Bukkit.getServer() == null) return null;
            return Bukkit.getOfflinePlayer(playerID).getName();
        } catch (Throwable t) {
            return null;
        }
    }

    public boolean hasExplicitPermission(UUID playerID, ClaimPermission permission) {
        if (playerID == null) return false;
        String name = offlineName(playerID);
        ClaimPermission held = Trust.explicitOn(record, playerID.toString(), name);
        return held != null && (held == ClaimPermission.Manage || permission.isGrantedBy(held));
    }

    public void setPermission(String playerID, ClaimPermission permission) {
        core.service().setTrust(record, playerID, permission, null);
    }

    public void dropPermission(String playerID) {
        core.service().setTrust(record, playerID, null, null);
    }

    public void clearPermissions() {
        core.service().clearAllTrust(record, null);
    }

    public void getPermissions(ArrayList<String> builders, ArrayList<String> containers,
                               ArrayList<String> accessors, ArrayList<String> managers) {
        if (builders != null) { builders.clear(); builders.addAll(record.builders); }
        if (containers != null) { containers.clear(); containers.addAll(record.containers); }
        if (accessors != null) { accessors.clear(); accessors.addAll(record.accessors); }
        if (managers != null) { managers.clear(); managers.addAll(record.managers); }
    }

    public boolean getSubclaimRestrictions() { return record.subclaimRestricted; }

    public void setSubclaimRestrictions(boolean restricted) {
        record.subclaimRestricted = restricted;
        core.service().touch(record);
        core.store().saveClaim(record);
    }

    @Override public boolean equals(Object o) {
        return o instanceof Claim other && other.record.id == record.id;
    }

    @Override public int hashCode() {
        return Long.hashCode(record.id);
    }
}

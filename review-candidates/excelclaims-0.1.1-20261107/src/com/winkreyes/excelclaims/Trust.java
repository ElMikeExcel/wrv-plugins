package com.winkreyes.excelclaims;

import me.ryanhamshire.GriefPrevention.ClaimPermission;

import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

/**
 * ExcelClaims — trust resolution. One place decides what a player may
 * do in a claim, so the protection listeners, the commands, and the
 * API facade can never drift apart.
 *
 * The ladder (the shape players know): Edit &gt; Build &gt; Container
 * &gt; Access; Manage (PermissionTrust) stands apart and implies every
 * tier inside the claim; Inventory is view-only container trust.
 * Subdivisions inherit their parent's trust unless the subdivision
 * says inheritNothing. The claim owner and the claim's managers pass
 * every check. "public" in a list trusts everyone at that tier.
 *
 * AutoSort's configure rule (SPEC §6.4) is expressible exactly on top
 * of this: configure = owner OR manager OR staff-in-admin-claim —
 * Container and Access tiers are read from different lists and can
 * never satisfy it. Excel — Iris / Mike.
 */
public final class Trust {

    private Trust() { }

    /** The lists one claim contributes, before inheritance. */
    public enum Source { OWN, INHERITED, NONE }

    /**
     * The player's explicit tier on ONE claim record (no inheritance):
     * the strongest list they appear in, or null. Managers report as
     * Manage; the owner is NOT reported here (ownership is checked
     * separately, as the old API's getPermission did).
     */
    public static ClaimPermission explicitOn(ClaimRecord claim, String playerId, String playerName) {
        if (claim == null || playerId == null) return null;
        if (matches(claim.managers, playerId, playerName)) return ClaimPermission.Manage;
        if (matches(claim.builders, playerId, playerName)) return ClaimPermission.Build;
        if (matches(claim.containers, playerId, playerName)) {
            return claim.inventoryOnly.contains(playerId) ? ClaimPermission.Inventory : ClaimPermission.Container;
        }
        if (matches(claim.accessors, playerId, playerName)) return ClaimPermission.Access;
        return null;
    }

    /**
     * The tier the API's getPermission REPORTS for one claim record.
     * This deliberately diverges from the ladder above in exactly one
     * state: an entry sitting in several lists at once. The observed
     * behavior of the old API (Landmarks rig, Nov 5, 2026, against the
     * real thing): Landmarks grants a plot buyer Build via
     * setPermission, then adds the buyer to the managers list
     * directly — and getPermission afterwards reports Build, not
     * Manage, while the managers membership itself (and every rights
     * check) still counts in full. So the reported tier reads the
     * grant lists in Build, Container, Access, Manage order, while
     * check() keeps using the ladder. Pure single-list states — every
     * normal claim — report identically under both.
     */
    public static ClaimPermission reportedOn(ClaimRecord claim, String playerId, String playerName) {
        if (claim == null || playerId == null) return null;
        if (matches(claim.builders, playerId, playerName)) return ClaimPermission.Build;
        if (matches(claim.containers, playerId, playerName)) {
            return claim.inventoryOnly.contains(playerId) ? ClaimPermission.Inventory : ClaimPermission.Container;
        }
        if (matches(claim.accessors, playerId, playerName)) return ClaimPermission.Access;
        if (matches(claim.managers, playerId, playerName)) return ClaimPermission.Manage;
        return null;
    }

    /** Effective explicit tier including inheritance from ancestors. */
    public static ClaimPermission effective(ClaimRecord claim, UUID player, String playerName,
                                            Function<Long, ClaimRecord> byId) {
        String pid = player == null ? null : player.toString();
        ClaimRecord cursor = claim;
        while (cursor != null) {
            ClaimPermission p = explicitOn(cursor, pid, playerName);
            if (p != null) return p;
            if (cursor.inheritNothing || cursor.parentId < 0) break;
            cursor = byId.apply(cursor.parentId);
        }
        return null;
    }

    public static boolean isOwner(ClaimRecord claim, UUID player) {
        return claim != null && player != null && player.equals(claim.owner);
    }

    /** Owner of this claim or of any ancestor (subdivisions share their owner's title). */
    public static boolean isOwnerChain(ClaimRecord claim, UUID player, Function<Long, ClaimRecord> byId) {
        ClaimRecord cursor = claim;
        while (cursor != null) {
            if (isOwner(cursor, player)) return true;
            if (cursor.parentId < 0) break;
            cursor = byId.apply(cursor.parentId);
        }
        return false;
    }

    public static boolean isManager(ClaimRecord claim, UUID player, String playerName,
                                    Function<Long, ClaimRecord> byId) {
        return effective(claim, player, playerName, byId) == ClaimPermission.Manage;
    }

    /**
     * The one permission question every caller asks. Admin permission
     * (staff) is the caller's to add: pass adminOverride = true when
     * the player holds griefprevention.adminclaims and the claim is an
     * admin claim (or its subdivision).
     */
    public static boolean check(ClaimRecord claim, UUID player, String playerName,
                                ClaimPermission needed, boolean adminOverride,
                                Function<Long, ClaimRecord> byId) {
        if (claim == null || needed == null) return false;
        if (adminOverride) return true;
        if (player == null) return false;
        if (isOwnerChain(claim, player, byId)) return true;
        ClaimPermission held = effective(claim, player, playerName, byId);
        if (held == null) {
            // "public" trust: anyone at the tier the public entry grants.
            held = publicTier(claim, byId);
            if (held == null) return false;
        }
        if (held == ClaimPermission.Manage) return true;
        return needed.isGrantedBy(held);
    }

    /** The tier granted to "public" on this claim (with inheritance), or null. */
    public static ClaimPermission publicTier(ClaimRecord claim, Function<Long, ClaimRecord> byId) {
        ClaimRecord cursor = claim;
        while (cursor != null) {
            ClaimPermission p = explicitOnPublic(cursor);
            if (p != null) return p;
            if (cursor.inheritNothing || cursor.parentId < 0) break;
            cursor = byId.apply(cursor.parentId);
        }
        return null;
    }

    private static ClaimPermission explicitOnPublic(ClaimRecord claim) {
        if (claim.managers.contains("public")) return ClaimPermission.Manage;
        if (claim.builders.contains("public")) return ClaimPermission.Build;
        if (claim.containers.contains("public")) {
            return claim.inventoryOnly.contains("public") ? ClaimPermission.Inventory : ClaimPermission.Container;
        }
        if (claim.accessors.contains("public")) return ClaimPermission.Access;
        return null;
    }

    /** Which list a tier lives in (null for Edit — Edit is owner/manager-only here). */
    public static Set<String> listFor(ClaimRecord claim, ClaimPermission tier) {
        switch (tier) {
            case Build: return claim.builders;
            case Container: case Inventory: return claim.containers;
            case Access: return claim.accessors;
            case Manage: return claim.managers;
            default: return null;
        }
    }

    static boolean matches(Set<String> list, String playerId, String playerName) {
        if (playerId != null && list.contains(playerId)) return true;
        if (playerName != null) {
            for (String entry : list) {
                if (entry.equalsIgnoreCase(playerName)) return true;
            }
        }
        return false;
    }
}

// ExcelClaims — Iris / Mike, for Wink Reyes Valley

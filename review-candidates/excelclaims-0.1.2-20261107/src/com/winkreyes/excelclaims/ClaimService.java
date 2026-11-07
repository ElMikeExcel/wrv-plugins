package com.winkreyes.excelclaims;

import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.ClaimPermission;
import me.ryanhamshire.GriefPrevention.events.ClaimChangeEvent;
import me.ryanhamshire.GriefPrevention.events.ClaimCreatedEvent;
import me.ryanhamshire.GriefPrevention.events.ClaimDeletedEvent;
import me.ryanhamshire.GriefPrevention.events.ClaimModifiedEvent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * ExcelClaims — claim operations: create, delete, resize, trust,
 * transfer, expiration. Commands and the API facade both call these,
 * so a claim made by a shovel, a command, and another plugin all obey
 * the same rules and fire the same events. Firing the facade events
 * here is what lets ExcelProtect and ExcelAuditLog keep recording
 * claim activity with no changes on their side.
 * Excel — Iris / Mike.
 */
public final class ClaimService {

    public enum CreateOutcome {
        OK, WORLD_DISABLED, TOO_SMALL, OVERLAP, OUTSIDE_PARENT,
        SUBDIVISION_OVERLAP, NOT_ENOUGH_BLOCKS, CLAIM_LIMIT, EVENT_CANCELLED
    }

    public static final class CreateResult {
        public final CreateOutcome outcome;
        public final ClaimRecord claim;
        CreateResult(CreateOutcome o, ClaimRecord c) { outcome = o; claim = c; }
    }

    private final Core core;
    private long lastExpirationRun = 0L;

    public ClaimService(Core core) {
        this.core = core;
    }

    private ClaimStore store() { return core.store(); }
    private ClaimsConfig cfg() { return core.config(); }

    /** Fire an event when a server is running; headless (tests) is a no-op. */
    private static void fire(org.bukkit.event.Event event) {
        try {
            if (Bukkit.getServer() != null) Bukkit.getPluginManager().callEvent(event);
        } catch (Throwable ignored) { }
    }

    /** Fire the cancellable creation event; returns true when a listener cancelled it. */
    private static boolean fireCreated(ClaimCreatedEvent event) {
        try {
            if (Bukkit.getServer() == null) return false;
            Bukkit.getPluginManager().callEvent(event);
            return event.isCancelled();
        } catch (Throwable ignored) {
            return false;
        }
    }

    // ------------------------------------------------------------ create

    /**
     * The one creation path. Corners may arrive in any order; Y is
     * normalized, then the lesser Y drops by the extend-into-ground
     * distance (except for subdivisions, whose Y is kept as given).
     * Top-level player claims are charged area blocks; subdivisions
     * and admin claims are free.
     */
    public CreateResult createClaim(String world, int x1, int y1, int z1, int x2, int y2, int z2,
                                    UUID owner, ClaimRecord parent, CommandSender creator) {
        if (!cfg().claimsEnabled(world)) return new CreateResult(CreateOutcome.WORLD_DISABLED, null);
        ClaimRecord r = new ClaimRecord();
        r.world = world;
        r.lx = Math.min(x1, x2); r.gx = Math.max(x1, x2);
        r.lz = Math.min(z1, z2); r.gz = Math.max(z1, z2);
        r.ly = Math.min(y1, y2); r.gy = Math.max(y1, y2);
        r.parentId = parent == null ? -1 : parent.id;
        r.owner = parent != null ? parent.owner : owner;
        if (parent == null) {
            r.ly = Math.max(-64, r.ly - cfg().extendIntoGround);
            if (r.widthX() < cfg().minimumWidth || r.widthZ() < cfg().minimumWidth
                    || r.area() < cfg().minimumArea) {
                return new CreateResult(CreateOutcome.TOO_SMALL, null);
            }
            for (ClaimRecord other : store().all()) {
                if (other.parentId < 0 && other.world.equals(world) && r.overlapsColumn(other)) {
                    return new CreateResult(CreateOutcome.OVERLAP, null);
                }
            }
            if (r.owner != null) {
                if (cfg().maxClaimsPerPlayer > 0) {
                    long count = store().claimsOwnedBy(r.owner).stream().filter(c -> c.parentId < 0).count();
                    if (count >= cfg().maxClaimsPerPlayer) return new CreateResult(CreateOutcome.CLAIM_LIMIT, null);
                }
                if (store().remainingBlocks(r.owner) < r.area()) {
                    return new CreateResult(CreateOutcome.NOT_ENOUGH_BLOCKS, null);
                }
            }
        } else {
            if (!r.fullyInside(parent)) return new CreateResult(CreateOutcome.OUTSIDE_PARENT, null);
            for (ClaimRecord sibling : store().childrenOf(parent.id)) {
                if (r.overlapsColumn(sibling)) return new CreateResult(CreateOutcome.SUBDIVISION_OVERLAP, null);
            }
        }
        r.id = store().allocateId();
        r.createdAt = System.currentTimeMillis();
        r.lastModifiedAt = r.createdAt;
        // The creation event is cancellable, like the old API's.
        ClaimCreatedEvent event = new ClaimCreatedEvent(core.facadeFor(r), creator);
        if (fireCreated(event)) {
            return new CreateResult(CreateOutcome.EVENT_CANCELLED, null);
        }
        store().put(r);
        store().saveClaim(r);
        return new CreateResult(CreateOutcome.OK, r);
    }

    /**
     * The first-chest automatic claim: a square of the configured
     * radius around the chest. Exempt from the minimum-size rules (the
     * radius IS the size rule), silent on overlap or insufficient
     * blocks — a new player is never scolded by their own first chest.
     */
    public CreateResult createAutoClaim(org.bukkit.Location chest, org.bukkit.entity.Player player) {
        int radius = cfg().autoClaimRadius;
        String world = chest.getWorld().getName();
        if (!cfg().claimsEnabled(world)) return new CreateResult(CreateOutcome.WORLD_DISABLED, null);
        ClaimRecord r = new ClaimRecord();
        r.world = world;
        r.lx = chest.getBlockX() - radius; r.gx = chest.getBlockX() + radius;
        r.lz = chest.getBlockZ() - radius; r.gz = chest.getBlockZ() + radius;
        r.ly = Math.max(-64, chest.getBlockY() - cfg().extendIntoGround);
        r.gy = chest.getBlockY();
        r.parentId = -1;
        r.owner = player.getUniqueId();
        for (ClaimRecord other : store().all()) {
            if (other.parentId < 0 && other.world.equals(world) && r.overlapsColumn(other)) {
                return new CreateResult(CreateOutcome.OVERLAP, null);
            }
        }
        if (store().remainingBlocks(r.owner) < r.area()) {
            return new CreateResult(CreateOutcome.NOT_ENOUGH_BLOCKS, null);
        }
        r.id = store().allocateId();
        r.createdAt = System.currentTimeMillis();
        r.lastModifiedAt = r.createdAt;
        r.chestClaim = true;
        ClaimCreatedEvent event = new ClaimCreatedEvent(core.facadeFor(r), player);
        if (fireCreated(event)) return new CreateResult(CreateOutcome.EVENT_CANCELLED, null);
        store().put(r);
        store().saveClaim(r);
        return new CreateResult(CreateOutcome.OK, r);
    }

    // ------------------------------------------------------------ delete

    /** Delete a claim and (always) its subdivision subtree. Returns claims removed. */
    public int deleteClaim(ClaimRecord r) {
        if (r == null) return 0;
        List<ClaimRecord> subtree = new ArrayList<>();
        collectSubtree(r, subtree);
        int removed = 0;
        // Children first, so no orphan ever points at a dead parent.
        for (int i = subtree.size() - 1; i >= 0; i--) {
            ClaimRecord c = subtree.get(i);
            fire(new ClaimDeletedEvent(core.facadeFor(c)));
            store().remove(c.id);
            store().deleteClaimFile(c);
            c.facade = null;
            removed++;
        }
        return removed;
    }

    private void collectSubtree(ClaimRecord r, List<ClaimRecord> out) {
        out.add(r);
        for (ClaimRecord child : store().childrenOf(r.id)) collectSubtree(child, out);
    }

    public int deleteAllClaimsFor(UUID owner) {
        int removed = 0;
        for (ClaimRecord r : store().claimsOwnedBy(owner)) {
            if (r.parentId < 0) removed += deleteClaim(r);
        }
        return removed;
    }

    // ------------------------------------------------------------ resize

    public enum ResizeOutcome { OK, TOO_SMALL, OVERLAP, NOT_ENOUGH_BLOCKS, OUTSIDE_PARENT, EVENT_CANCELLED }

    /** Move one corner of a claim (shovel resize / extend). axis: which corner moves is derived from the new corner point. */
    public ResizeOutcome resizeTo(ClaimRecord r, int nx1, int nz1, int nx2, int nz2, CommandSender actor) {
        int olx = r.lx, ogx = r.gx, olz = r.lz, ogz = r.gz;
        int nlx = Math.min(nx1, nx2), ngx = Math.max(nx1, nx2);
        int nlz = Math.min(nz1, nz2), ngz = Math.max(nz1, nz2);
        ClaimRecord probe = new ClaimRecord();
        probe.world = r.world; probe.lx = nlx; probe.gx = ngx; probe.lz = nlz; probe.gz = ngz;
        probe.ly = r.ly; probe.gy = r.gy; probe.parentId = r.parentId;
        if (r.parentId < 0) {
            if (probe.widthX() < cfg().minimumWidth || probe.widthZ() < cfg().minimumWidth
                    || probe.area() < cfg().minimumArea) return ResizeOutcome.TOO_SMALL;
            for (ClaimRecord other : store().all()) {
                if (other.id != r.id && other.parentId < 0 && other.world.equals(r.world)
                        && probe.overlapsColumn(other)) return ResizeOutcome.OVERLAP;
            }
            if (r.owner != null) {
                int delta = probe.area() - r.area();
                if (delta > 0 && store().remainingBlocks(r.owner) < delta) return ResizeOutcome.NOT_ENOUGH_BLOCKS;
            }
        } else {
            ClaimRecord parent = store().byId(r.parentId);
            if (parent == null || !probe.fullyInside(parent)) return ResizeOutcome.OUTSIDE_PARENT;
            for (ClaimRecord sibling : store().childrenOf(r.parentId)) {
                if (sibling.id != r.id && probe.overlapsColumn(sibling)) return ResizeOutcome.OVERLAP;
            }
        }
        Claim before = snapshotFacade(r);
        r.lx = nlx; r.gx = ngx; r.lz = nlz; r.gz = ngz;
        ClaimChangeEvent change = new ClaimChangeEvent(before, core.facadeFor(r));
        fire(change);
        if (change.isCancelled()) {
            r.lx = olx; r.gx = ogx; r.lz = olz; r.gz = ogz;
            return ResizeOutcome.EVENT_CANCELLED;
        }
        touch(r);
        store().saveClaim(r);
        fire(new ClaimModifiedEvent(before, core.facadeFor(r), actor));
        return ResizeOutcome.OK;
    }

    /** A detached facade copy of the record as it stands (for change events). */
    private Claim snapshotFacade(ClaimRecord r) {
        ClaimRecord copy = new ClaimRecord();
        copy.id = r.id; copy.world = r.world;
        copy.lx = r.lx; copy.ly = r.ly; copy.lz = r.lz;
        copy.gx = r.gx; copy.gy = r.gy; copy.gz = r.gz;
        copy.owner = r.owner; copy.parentId = r.parentId;
        copy.builders.addAll(r.builders); copy.containers.addAll(r.containers);
        copy.accessors.addAll(r.accessors); copy.managers.addAll(r.managers);
        copy.inventoryOnly.addAll(r.inventoryOnly);
        copy.inheritNothing = r.inheritNothing;
        copy.subclaimRestricted = r.subclaimRestricted;
        copy.createdAt = r.createdAt; copy.lastModifiedAt = r.lastModifiedAt;
        if (r.parentId >= 0) {
            ClaimRecord p = store().byId(r.parentId);
            if (p != null) copy.facade = null;
        }
        Claim f = new Claim(copy, core);
        if (r.parentId >= 0) {
            ClaimRecord p = store().byId(r.parentId);
            if (p != null) f.parent = core.facadeFor(p);
        }
        return f;
    }

    // ------------------------------------------------------------ trust

    /** Set (tier != null) or clear (tier == null) one entry's trust on one claim. */
    public void setTrust(ClaimRecord r, String entry, ClaimPermission tier, CommandSender actor) {
        if (r == null || entry == null) return;
        ClaimPermission before = Trust.explicitOn(r, entry, null);
        r.builders.remove(entry); r.containers.remove(entry);
        r.accessors.remove(entry); r.managers.remove(entry);
        r.inventoryOnly.remove(entry);
        // Legacy name entries may differ in case from a re-grant; clear those too.
        r.builders.removeIf(e -> e.equalsIgnoreCase(entry));
        r.containers.removeIf(e -> e.equalsIgnoreCase(entry));
        r.accessors.removeIf(e -> e.equalsIgnoreCase(entry));
        r.managers.removeIf(e -> e.equalsIgnoreCase(entry));
        if (tier != null) {
            switch (tier) {
                case Build: case Edit: r.builders.add(entry); break;
                case Container: r.containers.add(entry); break;
                case Inventory: r.containers.add(entry); r.inventoryOnly.add(entry); break;
                case Access: r.accessors.add(entry); break;
                case Manage: r.managers.add(entry); break;
            }
        }
        touch(r);
        store().saveClaim(r);
        syncLiveLists(r);
        fire(new ClaimModifiedEvent(core.facadeFor(r), core.facadeFor(r), actor));
        publishTrust(r, entry, before, tier, actor);
    }

    public void clearAllTrust(ClaimRecord r, CommandSender actor) {
        r.builders.clear(); r.containers.clear(); r.accessors.clear(); r.managers.clear();
        r.inventoryOnly.clear();
        touch(r);
        store().saveClaim(r);
        syncLiveLists(r);
        fire(new ClaimModifiedEvent(core.facadeFor(r), core.facadeFor(r), actor));
    }

    /** The facade Claim exposes managers as a live list; keep it mirroring the record. */
    public void syncLiveLists(ClaimRecord r) {
        if (r.facade != null) r.facade.refreshManagersFromRecord();
    }

    private void publishTrust(ClaimRecord r, String entry, ClaimPermission before,
                              ClaimPermission after, CommandSender actor) {
        ProtectGateway sink = core.protectSink();
        if (sink == null) return;
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("claim", r.id);
        fields.put("claim_owner", r.owner == null ? null : r.owner.toString());
        fields.put("target", entry);
        fields.put("level_before", before == null ? null : before.name());
        fields.put("level_after", after == null ? null : after.name());
        fields.put("actor", actor == null ? null : actor.getName());
        Location lo = core.facadeFor(r).getLesserBoundaryCorner();
        fields.put("world", r.world);
        fields.put("x", lo.getBlockX()); fields.put("y", lo.getBlockY()); fields.put("z", lo.getBlockZ());
        sink.trustChanged("claim_trust_changed", fields);
    }

    // ------------------------------------------------------------ transfer

    /** Transfer a top-level claim. Subdivisions refuse (facade throws there; here we just refuse). */
    public boolean transfer(ClaimRecord r, UUID newOwner, CommandSender actor) {
        if (r == null || r.parentId >= 0 || newOwner == null) return false;
        int usedByNew = store().usedArea(newOwner);
        PlayerRecord pr = store().playerData(newOwner);
        if (pr.accrued + pr.bonus - usedByNew < r.area()) return false;
        r.owner = newOwner;
        for (ClaimRecord child : subtreeOf(r)) child.owner = newOwner;
        touch(r);
        store().saveClaim(r);
        for (ClaimRecord child : subtreeOf(r)) store().saveClaim(child);
        fire(new ClaimModifiedEvent(core.facadeFor(r), core.facadeFor(r), actor));
        return true;
    }

    private List<ClaimRecord> subtreeOf(ClaimRecord r) {
        List<ClaimRecord> out = new ArrayList<>();
        collectSubtree(r, out);
        out.remove(r);
        return out;
    }

    public void touch(ClaimRecord r) {
        r.lastModifiedAt = System.currentTimeMillis();
        if (r.chestClaim) r.chestClaim = false; // any deliberate change graduates the claim
    }

    // ------------------------------------------------------------ expiration

    /**
     * Bukkit's last-played time for a player, or 0 when no server is
     * running (headless tests) or Bukkit has no record. Guarded the
     * same way as Core.nameOf — a lookup failure reads as "unknown",
     * and unknown is never a basis for expiring a claim.
     */
    static long bukkitLastPlayed(UUID id) {
        if (id == null) return 0L;
        try {
            if (Bukkit.getServer() == null) return 0L;
            OfflinePlayer op = Bukkit.getOfflinePlayer(id);
            return op == null ? 0L : Math.max(0L, op.getLastPlayed());
        } catch (Throwable t) {
            return 0L;
        }
    }

    /**
     * The last-seen basis for one claim's expiration threshold: the
     * NEWEST positive evidence of the owner's presence across every
     * source — the stored last-seen stamp, Bukkit's last-played
     * record consulted live, and the claim's own created/modified
     * stamps. A basis of 0 means every source is silent.
     *
     * 0.1.1 (the Nov 7 incident): 0.1.0 used the stored stamp when
     * present and the claim stamps otherwise, with no Bukkit consult
     * and no unknown case — imported claims carried all-zero stamps,
     * so the basis came out 0 and "days away" computed to ~20,000,
     * expiring nine live claims at the first due tick. Taking the
     * newest evidence across all sources can only ever keep a claim
     * LONGER than 0.1.0's chain did, never shorter — the deliberate
     * conservative direction: expire on proof of absence, never on
     * absence of proof.
     */
    static long lastSeenBasis(long playerLastSeenAt, long bukkitLastPlayed,
                              long createdAt, long lastModifiedAt) {
        long basis = Math.max(createdAt, lastModifiedAt);
        if (playerLastSeenAt > basis) basis = playerLastSeenAt;
        if (bukkitLastPlayed > basis) basis = bukkitLastPlayed;
        return basis;
    }

    /**
     * The expiration sweep, run on the config interval.
     *
     * 0.1.2 (Mike's ruling, Nov 7, 2026): the three global clocks
     * and the block-count exemptions are replaced by ONE inactivity
     * threshold per owner — their per-player override when one is
     * set, else the server default. `never` (or any negative value)
     * means the owner's claims never expire; a per-player `never`
     * IS the exemption mechanism now. Expiration is a policy tool
     * for large public servers; on a family server it only needs
     * to exist as an option, which is exactly what this is.
     *
     * The 0.1.1 safety floor is unchanged: the basis is the newest
     * positive evidence across the stored stamp, the live Bukkit
     * consult, and the claim's own stamps, and a claim whose basis
     * is UNKNOWN (every source silent) never expires under ANY
     * setting; those are counted and reported once per run, not
     * spammed. Owners whose threshold is `never` are skipped by
     * setting before any evidence is consulted, so the skipped-
     * unknown count only ever names claims a finite setting would
     * otherwise have judged — the count stays a pure safety-floor
     * signal (named note, BUILD-RECORD 0.1.2).
     */
    public void runExpirationIfDue() {
        long now = System.currentTimeMillis();
        if (now - lastExpirationRun < cfg().expirationCheckMinutes * 60_000L) return;
        lastExpirationRun = now;
        long day = 86_400_000L;
        List<ClaimRecord> doomed = new ArrayList<>();
        int skippedUnknown = 0;
        for (ClaimRecord r : store().all()) {
            if (r.parentId >= 0 || r.owner == null) continue; // subdivisions die with parents; admin claims never expire
            int threshold = cfg().effectiveExpirationDays(r.owner);
            if (threshold < 0) continue; // `never` — skipped by setting, not by the safety floor
            PlayerRecord pr = store().playerDataIfPresent(r.owner);
            long basis = lastSeenBasis(pr != null ? pr.lastSeenAt : 0L,
                    bukkitLastPlayed(r.owner), r.createdAt, r.lastModifiedAt);
            if (basis <= 0) { skippedUnknown++; continue; } // unknown basis: never expires
            long awayDays = (now - basis) / day;
            if (awayDays >= threshold) doomed.add(r);
        }
        if (skippedUnknown > 0) {
            core.log().info("ExcelClaims: expiration check skipped " + skippedUnknown
                    + " claim(s) — owner last-seen unknown (a claim never expires on unknown data)");
        }
        for (ClaimRecord r : doomed) {
            core.log().info("ExcelClaims: expiring claim " + r.id + " of " + core.nameOf(r.owner)
                    + " (owner inactive)");
            deleteClaim(r);
        }
    }
}

// ExcelClaims — Iris / Mike, for Wink Reyes Valley

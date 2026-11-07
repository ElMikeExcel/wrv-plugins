package com.winkreyes.excelclaims;

import me.ryanhamshire.GriefPrevention.ClaimPermission;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

/**
 * ExcelClaims — the golden-shovel flow and the stick investigation:
 * right-click two corners to claim, click your own claim's corner to
 * resize it, subdivision mode carves plots inside a claim, admin mode
 * makes server claims. The stick reports who owns what you click.
 * Excel — Iris / Mike.
 */
public final class ToolListener implements Listener {

    private final Core core;

    public ToolListener(Core core) {
        this.core = core;
    }

    private ClaimsConfig cfg() { return core.config(); }
    private ClaimStore store() { return core.store(); }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = false)
    public void onInteract(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Block block = e.getClickedBlock();
        if (block == null) return;
        Player p = e.getPlayer();
        ItemStack hand = e.getItem();
        if (hand == null) return;
        Location loc = block.getLocation();
        if (!cfg().claimsEnabled(loc.getWorld().getName())) return;

        if (hand.getType() == cfg().investigationTool) {
            e.setCancelled(true);
            investigate(p, loc);
        } else if (hand.getType() == cfg().modificationTool) {
            e.setCancelled(true);
            shovelClick(p, loc);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        core.shovelFirstCorner.remove(e.getPlayer().getUniqueId());
    }

    // ------------------------------------------------------------ investigation

    private void investigate(Player p, Location loc) {
        ClaimRecord claim = store().claimAt(loc.getWorld().getName(), loc.getBlockX(), loc.getBlockZ());
        if (claim == null) {
            p.sendMessage(ProtectionListener.color(cfg().msg("investigate-none")));
            return;
        }
        Map<String, String> vars = new HashMap<>();
        vars.put("owner", core.nameOf(claim.owner));
        vars.put("area", String.valueOf(claim.area()));
        vars.put("builders", names(claim.builders));
        vars.put("containers", names(claim.containers));
        vars.put("accessors", names(claim.accessors));
        vars.put("managers", names(claim.managers));
        p.sendMessage(ProtectionListener.color(cfg().msg("investigate", vars)));
        core.visualizer().showCorners(p, claim);
    }

    private String names(java.util.Set<String> entries) {
        if (entries.isEmpty()) return "nobody";
        java.util.List<String> out = new java.util.ArrayList<>();
        for (String entry : entries) {
            if (entry.equals("public")) { out.add("the public"); continue; }
            java.util.UUID id = core.resolveEntry(entry);
            out.add(id == null ? entry : core.nameOf(id));
        }
        return String.join(", ", out);
    }

    // ------------------------------------------------------------ shovel

    private void shovelClick(Player p, Location loc) {
        String mode = core.shovelModes.getOrDefault(p.getUniqueId(), "basic");
        Core.ShovelState first = core.shovelFirstCorner.get(p.getUniqueId());

        if (first == null) {
            // Resize start? Clicking a corner column of a claim you own
            // or manage anchors a resize at the opposite corner.
            ClaimRecord claim = store().claimAt(loc.getWorld().getName(), loc.getBlockX(), loc.getBlockZ());
            if (claim != null && "basic".equals(mode) && isCorner(claim, loc)
                    && (Trust.isOwnerChain(claim, p.getUniqueId(), store()::byId)
                        || Trust.isManager(claim, p.getUniqueId(), p.getName(), store()::byId)
                        || core.adminOverride(p, claim))) {
                Core.ShovelState state = new Core.ShovelState();
                state.world = loc.getWorld().getName();
                state.x = loc.getBlockX() == claim.lx ? claim.gx : claim.lx;
                state.z = loc.getBlockZ() == claim.lz ? claim.gz : claim.lz;
                state.y = loc.getBlockY();
                state.resizingClaimId = claim.id;
                core.shovelFirstCorner.put(p.getUniqueId(), state);
                p.sendMessage(ProtectionListener.color(cfg().msg("resizing")));
                core.visualizer().showCorners(p, claim);
                return;
            }
            Core.ShovelState state = new Core.ShovelState();
            state.world = loc.getWorld().getName();
            state.x = loc.getBlockX(); state.y = loc.getBlockY(); state.z = loc.getBlockZ();
            state.resizingClaimId = -1;
            core.shovelFirstCorner.put(p.getUniqueId(), state);
            p.sendMessage(ProtectionListener.color(cfg().msg("corner-one")));
            return;
        }

        core.shovelFirstCorner.remove(p.getUniqueId());
        if (!first.world.equals(loc.getWorld().getName())) {
            p.sendMessage(ProtectionListener.color(cfg().msg("corner-one")));
            return;
        }

        if (first.resizingClaimId >= 0) {
            ClaimRecord claim = store().byId(first.resizingClaimId);
            if (claim == null) return;
            ClaimService.ResizeOutcome out = core.service().resizeTo(claim,
                    first.x, first.z, loc.getBlockX(), loc.getBlockZ(), p);
            reportResize(p, out, claim);
            return;
        }

        if ("subdivision".equals(mode)) {
            ClaimRecord parent = store().claimAt(first.world, first.x, first.z);
            if (parent == null) {
                p.sendMessage(ProtectionListener.color(cfg().msg("claim-outside-parent")));
                return;
            }
            if (parent.isSubdivision()) parent = store().topLevel(parent);
            boolean allowed = Trust.isOwnerChain(parent, p.getUniqueId(), store()::byId)
                    || (!parent.subclaimRestricted
                        && Trust.isManager(parent, p.getUniqueId(), p.getName(), store()::byId))
                    || core.adminOverride(p, parent)
                    || (parent.isAdmin() && Trust.check(parent, p.getUniqueId(), p.getName(),
                            ClaimPermission.Build, false, store()::byId));
            if (!allowed) {
                p.sendMessage(ProtectionListener.color(cfg().msg("not-your-claim")));
                return;
            }
            ClaimService.CreateResult res = core.service().createClaim(first.world,
                    first.x, first.y, first.z, loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(),
                    p.getUniqueId(), parent, p);
            reportCreate(p, res);
            return;
        }

        // Basic (or admin) claim creation.
        if (!p.hasPermission("griefprevention.createclaims")) {
            p.sendMessage(ProtectionListener.color(cfg().msg("no-permission")));
            return;
        }
        java.util.UUID owner = "admin".equals(mode) ? null : p.getUniqueId();
        ClaimService.CreateResult res = core.service().createClaim(first.world,
                first.x, first.y, first.z, loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(),
                owner, null, p);
        reportCreate(p, res);
    }

    private boolean isCorner(ClaimRecord claim, Location loc) {
        int x = loc.getBlockX(), z = loc.getBlockZ();
        return (x == claim.lx || x == claim.gx) && (z == claim.lz || z == claim.gz);
    }

    private void reportCreate(Player p, ClaimService.CreateResult res) {
        Map<String, String> vars = new HashMap<>();
        switch (res.outcome) {
            case OK -> {
                vars.put("remaining", String.valueOf(store().remainingBlocks(p.getUniqueId())));
                p.sendMessage(ProtectionListener.color(cfg().msg("claim-created", vars)));
                core.visualizer().showCorners(p, res.claim);
            }
            case OVERLAP -> p.sendMessage(ProtectionListener.color(cfg().msg("claim-overlap")));
            case TOO_SMALL -> {
                vars.put("width", String.valueOf(cfg().minimumWidth));
                vars.put("area", String.valueOf(cfg().minimumArea));
                p.sendMessage(ProtectionListener.color(cfg().msg("claim-too-small", vars)));
            }
            case NOT_ENOUGH_BLOCKS -> p.sendMessage(ProtectionListener.color(cfg().msg("not-enough-blocks",
                    Map.of("needed", String.valueOf(missingBlocks(p))))));
            case OUTSIDE_PARENT -> p.sendMessage(ProtectionListener.color(cfg().msg("claim-outside-parent")));
            case SUBDIVISION_OVERLAP -> p.sendMessage(ProtectionListener.color(cfg().msg("claim-subdivision-overlap")));
            case CLAIM_LIMIT -> p.sendMessage(ProtectionListener.color(cfg().msg("claim-limit")));
            case WORLD_DISABLED -> p.sendMessage(ProtectionListener.color(cfg().msg("no-claims-here")));
            case EVENT_CANCELLED -> p.sendMessage(ProtectionListener.color(cfg().msg("no-permission")));
        }
    }

    private int missingBlocks(Player p) {
        return Math.max(0, -store().remainingBlocks(p.getUniqueId()));
    }

    private void reportResize(Player p, ClaimService.ResizeOutcome out, ClaimRecord claim) {
        Map<String, String> vars = new HashMap<>();
        switch (out) {
            case OK -> {
                vars.put("remaining", String.valueOf(store().remainingBlocks(p.getUniqueId())));
                p.sendMessage(ProtectionListener.color(cfg().msg("claim-resized", vars)));
                core.visualizer().showCorners(p, claim);
            }
            case TOO_SMALL -> {
                vars.put("width", String.valueOf(cfg().minimumWidth));
                vars.put("area", String.valueOf(cfg().minimumArea));
                p.sendMessage(ProtectionListener.color(cfg().msg("claim-too-small", vars)));
            }
            case OVERLAP -> p.sendMessage(ProtectionListener.color(cfg().msg("claim-overlap")));
            case NOT_ENOUGH_BLOCKS -> p.sendMessage(ProtectionListener.color(cfg().msg("not-enough-blocks",
                    Map.of("needed", String.valueOf(missingBlocks(p))))));
            case OUTSIDE_PARENT -> p.sendMessage(ProtectionListener.color(cfg().msg("claim-outside-parent")));
            case EVENT_CANCELLED -> p.sendMessage(ProtectionListener.color(cfg().msg("no-permission")));
        }
    }
}

// ExcelClaims — Iris / Mike, for Wink Reyes Valley

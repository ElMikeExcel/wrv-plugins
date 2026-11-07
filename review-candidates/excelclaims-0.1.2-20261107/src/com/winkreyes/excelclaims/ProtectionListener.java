package com.winkreyes.excelclaims;

import me.ryanhamshire.GriefPrevention.ClaimPermission;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.entity.Animals;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Enderman;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Rabbit;
import org.bukkit.entity.Silverfish;
import org.bukkit.entity.Tameable;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityInteractEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.raid.RaidTriggerEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * ExcelClaims — claim protection, rule-for-rule the live configuration:
 * build/container/access trust gates, creature and PvP protection in
 * claims, explosion/fire/piston boundaries, ender-pearl and villager
 * rules, the access-trust command list, combat-logout punishment, and
 * the first-chest automatic claim. Every rule reads the config live,
 * so tuning is a config edit. Excel — Iris / Mike.
 */
public final class ProtectionListener implements Listener {

    private final Core core;
    private final Map<UUID, Long> lastDenial = new HashMap<>();

    public ProtectionListener(Core core) {
        this.core = core;
    }

    private ClaimsConfig cfg() { return core.config(); }
    private ClaimStore store() { return core.store(); }

    ClaimRecord claimAt(Location loc) {
        if (loc == null || loc.getWorld() == null) return null;
        if (!cfg().claimsEnabled(loc.getWorld().getName())) return null;
        ClaimRecord r = store().claimAt(loc.getWorld().getName(), loc.getBlockX(), loc.getBlockZ());
        if (r == null) return null;
        return r.contains(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ()) ? r : topIfContains(r, loc);
    }

    private ClaimRecord topIfContains(ClaimRecord r, Location loc) {
        // The deepest column match failed the Y test; an ancestor may still hold it.
        ClaimRecord cursor = store().byId(r.parentId);
        while (cursor != null) {
            if (cursor.contains(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ())) return cursor;
            cursor = store().byId(cursor.parentId);
        }
        return null;
    }

    boolean can(Player p, ClaimRecord claim, ClaimPermission tier) {
        if (claim == null) return true;
        if (core.isIgnoring(p)) return true;
        return Trust.check(claim, p.getUniqueId(), p.getName(), tier,
                core.adminOverride(p, claim), store()::byId);
    }

    private void deny(Player p, String key, ClaimRecord claim) {
        long now = System.currentTimeMillis();
        Long last = lastDenial.get(p.getUniqueId());
        if (last != null && now - last < 2000) return;
        lastDenial.put(p.getUniqueId(), now);
        Map<String, String> vars = new HashMap<>();
        vars.put("owner", claim == null ? "" : core.nameOf(claim.owner));
        p.sendMessage(color(cfg().msg(key, vars)));
    }

    static String color(String s) {
        return s.replace('&', '§');
    }

    // ------------------------------------------------------------ build / break

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent e) {
        ClaimRecord claim = claimAt(e.getBlock().getLocation());
        if (claim == null) return;
        if (!can(e.getPlayer(), claim, ClaimPermission.Build)) {
            e.setCancelled(true);
            deny(e.getPlayer(), "build-denied", claim);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent e) {
        ClaimRecord claim = claimAt(e.getBlock().getLocation());
        if (claim == null) return;
        if (!can(e.getPlayer(), claim, ClaimPermission.Build)) {
            e.setCancelled(true);
            deny(e.getPlayer(), "build-denied", claim);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChestPlaced(BlockPlaceEvent e) {
        if (e.getBlock().getType() != Material.CHEST) return;
        Player p = e.getPlayer();
        if (cfg().autoClaimRadius <= 0) return;
        if (!store().claimsOwnedBy(p.getUniqueId()).isEmpty()) return;
        Location loc = e.getBlock().getLocation();
        ClaimService.CreateResult res = core.service().createAutoClaim(loc, p);
        if (res.outcome == ClaimService.CreateOutcome.OK) {
            Map<String, String> vars = new HashMap<>();
            vars.put("remaining", String.valueOf(store().remainingBlocks(p.getUniqueId())));
            p.sendMessage(color(cfg().msg("claim-created-auto", vars)));
            core.visualizer().showCorners(p, res.claim);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBucketEmpty(PlayerBucketEmptyEvent e) {
        ClaimRecord claim = claimAt(e.getBlock().getLocation());
        if (claim != null && !can(e.getPlayer(), claim, ClaimPermission.Build)) {
            e.setCancelled(true);
            deny(e.getPlayer(), "build-denied", claim);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBucketFill(PlayerBucketFillEvent e) {
        ClaimRecord claim = claimAt(e.getBlock().getLocation());
        if (claim != null && !can(e.getPlayer(), claim, ClaimPermission.Build)) {
            e.setCancelled(true);
            deny(e.getPlayer(), "build-denied", claim);
        }
    }

    // ------------------------------------------------------------ use / access

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK && e.getAction() != Action.PHYSICAL) return;
        Block block = e.getClickedBlock();
        if (block == null) return;
        ClaimRecord claim = claimAt(block.getLocation());
        if (claim == null) return;
        Player p = e.getPlayer();
        Material type = block.getType();

        if (block.getState() instanceof Container && type != Material.ENDER_CHEST) {
            if (cfg().preventTheft && !can(p, claim, ClaimPermission.Container)) {
                e.setCancelled(true);
                deny(p, "container-denied", claim);
            }
            return;
        }
        if (type == Material.LECTERN && cfg().lecternReadingRequiresAccess) {
            if (!can(p, claim, ClaimPermission.Access)) {
                e.setCancelled(true);
                deny(p, "access-denied", claim);
            }
            return;
        }
        boolean doorLike = (cfg().lockWoodenDoors && Tag.DOORS.isTagged(type))
                || (cfg().lockTrapDoors && Tag.TRAPDOORS.isTagged(type))
                || (cfg().lockFenceGates && Tag.FENCE_GATES.isTagged(type));
        if (doorLike) {
            if (!can(p, claim, ClaimPermission.Access)) {
                e.setCancelled(true);
                deny(p, "access-denied", claim);
            }
            return;
        }
        if (cfg().preventButtonsSwitches
                && (Tag.BUTTONS.isTagged(type) || type == Material.LEVER || Tag.PRESSURE_PLATES.isTagged(type)
                    || Tag.BEDS.isTagged(type))) {
            if (!can(p, claim, ClaimPermission.Access)) {
                e.setCancelled(true);
                deny(p, "access-denied", claim);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryOpen(InventoryOpenEvent e) {
        if (!(e.getPlayer() instanceof Player p)) return;
        if (e.getInventory().getType() == InventoryType.ENDER_CHEST) return;
        if (!(e.getInventory().getHolder() instanceof Container container)) return;
        ClaimRecord claim = claimAt(container.getLocation());
        if (claim == null || !cfg().preventTheft) return;
        if (!can(p, claim, ClaimPermission.Container)) {
            e.setCancelled(true);
            deny(p, "container-denied", claim);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteractEntity(PlayerInteractEntityEvent e) {
        Entity target = e.getRightClicked();
        ClaimRecord claim = claimAt(target.getLocation());
        if (claim == null) return;
        Player p = e.getPlayer();
        if (target instanceof Villager && cfg().villagerTradingRequiresBuild) {
            if (!can(p, claim, ClaimPermission.Build)) {
                e.setCancelled(true);
                deny(p, "build-denied", claim);
            }
        } else if (target instanceof ItemFrame || target instanceof ArmorStand) {
            if (!can(p, claim, ClaimPermission.Build)) {
                e.setCancelled(true);
                deny(p, "build-denied", claim);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onArmorStand(PlayerArmorStandManipulateEvent e) {
        ClaimRecord claim = claimAt(e.getRightClicked().getLocation());
        if (claim != null && !can(e.getPlayer(), claim, ClaimPermission.Build)) {
            e.setCancelled(true);
            deny(e.getPlayer(), "build-denied", claim);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onHangingBreak(HangingBreakByEntityEvent e) {
        if (!(e.getRemover() instanceof Player p)) return;
        ClaimRecord claim = claimAt(e.getEntity().getLocation());
        if (claim != null && !can(p, claim, ClaimPermission.Build)) {
            e.setCancelled(true);
            deny(p, "build-denied", claim);
        }
    }

    // ------------------------------------------------------------ damage

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamageByEntity(EntityDamageByEntityEvent e) {
        Entity victim = e.getEntity();
        Player attacker = null;
        if (e.getDamager() instanceof Player p) attacker = p;
        else if (e.getDamager() instanceof Projectile proj && proj.getShooter() instanceof Player p) attacker = p;
        if (attacker == null) return; // mob-caused damage is never claim-gated (matches the old behavior)

        if (victim instanceof Player vp) {
            ClaimRecord claim = claimAt(vp.getLocation());
            if (claim != null && pvpProtectedHere(claim)) {
                e.setCancelled(true);
                return;
            }
            // Outside protected claims: combat-tag both parties.
            long until = System.currentTimeMillis() + cfg().combatTimeoutSeconds * 1000L;
            core.combatUntil.put(vp.getUniqueId(), until);
            core.combatUntil.put(attacker.getUniqueId(), until);
            return;
        }
        if (cfg().protectCreatures && (victim instanceof Animals || victim instanceof Tameable || victim instanceof Villager)) {
            ClaimRecord claim = claimAt(victim.getLocation());
            if (claim != null && !can(attacker, claim, ClaimPermission.Build)) {
                e.setCancelled(true);
                deny(attacker, "build-denied", claim);
            }
        }
    }

    private boolean pvpProtectedHere(ClaimRecord claim) {
        ClaimRecord top = store().topLevel(claim);
        if (claim.isSubdivision() && top != null && top.isAdmin()) return cfg().pvpProtectAdminSubdivisions;
        if (claim.isAdmin()) return cfg().pvpProtectAdminClaims;
        return cfg().pvpProtectPlayerClaims;
    }

    // ------------------------------------------------------------ nature

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent e) {
        boolean creeper = e.getEntity() instanceof Creeper;
        if (!cfg().blockLandClaimExplosions && !(creeper && cfg().blockSurfaceCreeperExplosions)
                && !(!creeper && cfg().blockSurfaceOtherExplosions)) return;
        e.blockList().removeIf(b -> claimAt(b.getLocation()) != null);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent e) {
        if (!cfg().blockLandClaimExplosions) return;
        e.blockList().removeIf(b -> claimAt(b.getLocation()) != null);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBurn(BlockBurnEvent e) {
        ClaimRecord claim = claimAt(e.getBlock().getLocation());
        if (claim != null) {
            if (!cfg().fireDamagesInClaims) e.setCancelled(true);
        } else if (!cfg().fireDestroys) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onIgnite(BlockIgniteEvent e) {
        if (e.getCause() != BlockIgniteEvent.IgniteCause.SPREAD) return;
        ClaimRecord claim = claimAt(e.getBlock().getLocation());
        if (claim != null) {
            if (!cfg().fireSpreadsInClaims) e.setCancelled(true);
        } else if (!cfg().fireSpreads) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent e) {
        if (!"CLAIMS_ONLY".equalsIgnoreCase(cfg().pistonMovement)) return;
        ClaimRecord pistonClaim = claimAt(e.getBlock().getLocation());
        for (Block b : e.getBlocks()) {
            Location dest = b.getRelative(e.getDirection()).getLocation();
            // A piston may only move blocks that stay within its own
            // claim (or fully outside claims): no pushing across a border.
            if (claimAt(b.getLocation()) != pistonClaim || claimAt(dest) != pistonClaim) {
                e.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent e) {
        if (!"CLAIMS_ONLY".equalsIgnoreCase(cfg().pistonMovement)) return;
        ClaimRecord pistonClaim = claimAt(e.getBlock().getLocation());
        for (Block b : e.getBlocks()) {
            Location dest = b.getRelative(e.getDirection()).getLocation();
            if (claimAt(b.getLocation()) != pistonClaim || claimAt(dest) != pistonClaim) {
                e.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityChangeBlock(EntityChangeBlockEvent e) {
        Entity ent = e.getEntity();
        if (ent instanceof Enderman && !cfg().endermenMoveBlocks) { e.setCancelled(true); return; }
        if (ent instanceof Silverfish && !cfg().silverfishBreakBlocks) { e.setCancelled(true); return; }
        if (ent instanceof Rabbit && e.getTo() == Material.AIR
                && e.getBlock().getType().name().contains("CARROT") && !cfg().rabbitsEatCrops) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onTrample(EntityInteractEvent e) {
        if (e.getBlock().getType() == Material.FARMLAND && !(e.getEntity() instanceof Player)
                && !cfg().creaturesTrampleCrops) {
            e.setCancelled(true);
        }
    }

    // ------------------------------------------------------------ pearls, raids, commands, sessions

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPearl(ProjectileLaunchEvent e) {
        if (!cfg().enderPearlsRequireAccess) return;
        if (!(e.getEntity() instanceof org.bukkit.entity.EnderPearl pearl)) return;
        if (!(pearl.getShooter() instanceof Player p)) return;
        ClaimRecord claim = claimAt(p.getLocation());
        if (claim != null && !can(p, claim, ClaimPermission.Access)) {
            e.setCancelled(true);
            deny(p, "access-denied", claim);
            if (cfg().refundDeniedEnderPearls) {
                p.getInventory().addItem(new ItemStack(Material.ENDER_PEARL, 1));
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onRaidTrigger(RaidTriggerEvent e) {
        if (!cfg().raidTriggersRequireBuild) return;
        ClaimRecord claim = claimAt(e.getPlayer().getLocation());
        if (claim != null && !can(e.getPlayer(), claim, ClaimPermission.Build)) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent e) {
        String token = e.getMessage().split("\\s+")[0].toLowerCase();
        if (!cfg().commandsRequiringAccess.contains(token)) return;
        ClaimRecord claim = claimAt(e.getPlayer().getLocation());
        if (claim != null && !can(e.getPlayer(), claim, ClaimPermission.Access)) {
            e.setCancelled(true);
            deny(e.getPlayer(), "access-denied", claim);
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        PlayerRecord pr = store().playerData(e.getPlayer().getUniqueId());
        pr.lastSeenAt = System.currentTimeMillis();
        store().savePlayer(pr);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        Player p = e.getPlayer();
        PlayerRecord pr = store().playerData(p.getUniqueId());
        pr.lastSeenAt = System.currentTimeMillis();
        store().savePlayer(pr);
        Long until = core.combatUntil.remove(p.getUniqueId());
        if (cfg().punishLogout && until != null && until > System.currentTimeMillis()) {
            // Combat logging: gear drops where they stood (the old rule).
            for (ItemStack item : p.getInventory().getContents()) {
                if (item != null) p.getWorld().dropItemNaturally(p.getLocation(), item);
            }
            for (ItemStack item : p.getInventory().getArmorContents()) {
                if (item != null) p.getWorld().dropItemNaturally(p.getLocation(), item);
            }
            p.getInventory().clear();
        }
        core.trappedPending.remove(p.getUniqueId());
        core.visualizer().clear(p);
    }
}

// ExcelClaims — Iris / Mike, for Wink Reyes Valley

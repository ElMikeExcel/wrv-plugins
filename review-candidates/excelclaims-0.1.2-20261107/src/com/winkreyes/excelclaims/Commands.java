package com.winkreyes.excelclaims;

import me.ryanhamshire.GriefPrevention.ClaimPermission;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * ExcelClaims — the player and staff command surface, at parity for
 * the inventoried set (INVENTORY.md §3): the claim/trust family the
 * menus offer, subdivision mode, trapped, the (live-disabled) economy
 * pair, and the staff set, plus /excelclaims for status, migration,
 * and reload. Excel — Iris / Mike.
 */
public final class Commands implements CommandExecutor {

    static final String[] NAMES = {
            "claim", "claimslist", "trust", "untrust", "untrustall", "containertrust",
            "accesstrust", "permissiontrust", "trustlist", "extendclaim", "abandonclaim",
            "abandontopclaim", "abandonallclaims", "subdivideclaims", "restrictsubclaim",
            "trapped", "buyclaimblocks", "sellclaimblocks", "ignoreclaims", "adminclaims",
            "adjustclaimblocks", "adjustbonusclaimblocks", "deleteclaim", "deleteallclaims",
            "transferclaim", "excelclaims"
    };

    private final Core core;

    public Commands(Core core) {
        this.core = core;
    }

    private ClaimsConfig cfg() { return core.config(); }
    private ClaimStore store() { return core.store(); }

    private static String c(String s) { return ProtectionListener.color(s); }

    private void msg(CommandSender to, String key) {
        to.sendMessage(c(cfg().msg(key)));
    }

    private void msg(CommandSender to, String key, Map<String, String> vars) {
        to.sendMessage(c(cfg().msg(key, vars)));
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String name = command.getName().toLowerCase();
        try {
            switch (name) {
                case "excelclaims" -> { return excelClaims(sender, args); }
                case "buyclaimblocks" -> { return buySell(sender, args, true); }
                case "sellclaimblocks" -> { return buySell(sender, args, false); }
                case "adjustclaimblocks" -> { return adjust(sender, args, true); }
                case "adjustbonusclaimblocks" -> { return adjust(sender, args, false); }
                case "deleteallclaims" -> { return deleteAll(sender, args); }
                default -> { }
            }
            if (!(sender instanceof Player p)) {
                sender.sendMessage("ExcelClaims: that command is for players.");
                return true;
            }
            switch (name) {
                case "claim" -> claim(p, args);
                case "claimslist" -> claimsList(p);
                case "trust" -> trust(p, args, ClaimPermission.Build);
                case "containertrust" -> trust(p, args, ClaimPermission.Container);
                case "accesstrust" -> trust(p, args, ClaimPermission.Access);
                case "permissiontrust" -> trust(p, args, ClaimPermission.Manage);
                case "untrust" -> untrust(p, args);
                case "untrustall" -> untrustAll(p);
                case "trustlist" -> trustList(p);
                case "extendclaim" -> extendClaim(p, args);
                case "abandonclaim" -> abandon(p, false);
                case "abandontopclaim" -> abandon(p, true);
                case "abandonallclaims" -> {
                    int n = core.service().deleteAllClaimsFor(p.getUniqueId());
                    if (n > 0) msg(p, "claim-deleted"); else msg(p, "claimslist-none");
                }
                case "subdivideclaims" -> toggleMode(p, "subdivision", "subdivision-mode-on", "subdivision-mode-off");
                case "adminclaims" -> toggleMode(p, "admin", "admin-mode-on", "admin-mode-off");
                case "restrictsubclaim" -> restrictSubclaim(p);
                case "trapped" -> trapped(p);
                case "ignoreclaims" -> {
                    if (core.ignoringClaims.remove(p.getUniqueId())) {
                        msg(p, "ignoreclaims-off");
                    } else {
                        core.ignoringClaims.add(p.getUniqueId());
                        msg(p, "ignoreclaims-on");
                    }
                }
                case "deleteclaim" -> {
                    ClaimRecord claim = stoodIn(p);
                    if (claim == null) { msg(p, "no-claims-here"); break; }
                    core.service().deleteClaim(claim);
                    msg(p, "claim-deleted");
                }
                case "transferclaim" -> transfer(p, args);
                default -> { return false; }
            }
            return true;
        } catch (Exception ex) {
            core.log().warning("ExcelClaims: /" + name + " failed for " + sender.getName() + " (" + ex + ")");
            sender.sendMessage("§cExcelClaims hit a snag with that command — it's been logged.");
            return true;
        }
    }

    // ------------------------------------------------------------ helpers

    private ClaimRecord stoodIn(Player p) {
        Location loc = p.getLocation();
        return store().claimAt(loc.getWorld().getName(), loc.getBlockX(), loc.getBlockZ());
    }

    private boolean canManage(Player p, ClaimRecord claim) {
        return Trust.isOwnerChain(claim, p.getUniqueId(), store()::byId)
                || Trust.isManager(claim, p.getUniqueId(), p.getName(), store()::byId)
                || core.adminOverride(p, claim);
    }

    /** Entry string for a typed player name: UUID when the player is known, else the name itself. */
    private String entryFor(String typed) {
        if (typed.equalsIgnoreCase("public")) return "public";
        Player online = Bukkit.getPlayerExact(typed);
        if (online != null) return online.getUniqueId().toString();
        OfflinePlayer op = Bukkit.getOfflinePlayer(typed);
        if (op.hasPlayedBefore()) return op.getUniqueId().toString();
        return typed;
    }

    private String displayName(String entry) {
        if (entry.equals("public")) return "the public";
        UUID id = core.resolveEntry(entry);
        return id == null ? entry : core.nameOf(id);
    }

    /** Claims a trust command applies to: the claim stood in (if manageable), else all own top-level claims. */
    private List<ClaimRecord> trustTargets(Player p) {
        List<ClaimRecord> out = new ArrayList<>();
        ClaimRecord here = stoodIn(p);
        if (here != null && canManage(p, here)) { out.add(here); return out; }
        for (ClaimRecord r : store().claimsOwnedBy(p.getUniqueId())) {
            if (r.parentId < 0) out.add(r);
        }
        return out;
    }

    // ------------------------------------------------------------ claim commands

    private void claim(Player p, String[] args) {
        int radius = cfg().claimDefaultRadius;
        if (args.length > 0) {
            try { radius = Math.max(1, Integer.parseInt(args[0])); }
            catch (NumberFormatException e) { msg(p, "no-permission"); return; }
        }
        Location loc = p.getLocation();
        ClaimService.CreateResult res = core.service().createClaim(loc.getWorld().getName(),
                loc.getBlockX() - radius, loc.getBlockY(), loc.getBlockZ() - radius,
                loc.getBlockX() + radius, loc.getBlockY(), loc.getBlockZ() + radius,
                p.getUniqueId(), null, p);
        reportCreate(p, res);
    }

    private void reportCreate(Player p, ClaimService.CreateResult res) {
        switch (res.outcome) {
            case OK -> {
                msg(p, "claim-created", Map.of("remaining", String.valueOf(store().remainingBlocks(p.getUniqueId()))));
                core.visualizer().showCorners(p, res.claim);
            }
            case OVERLAP -> msg(p, "claim-overlap");
            case TOO_SMALL -> msg(p, "claim-too-small", Map.of(
                    "width", String.valueOf(cfg().minimumWidth), "area", String.valueOf(cfg().minimumArea)));
            case NOT_ENOUGH_BLOCKS -> msg(p, "not-enough-blocks",
                    Map.of("needed", String.valueOf(Math.max(0, -store().remainingBlocks(p.getUniqueId())))));
            case OUTSIDE_PARENT -> msg(p, "claim-outside-parent");
            case SUBDIVISION_OVERLAP -> msg(p, "claim-subdivision-overlap");
            case CLAIM_LIMIT -> msg(p, "claim-limit");
            case WORLD_DISABLED -> msg(p, "no-claims-here");
            case EVENT_CANCELLED -> msg(p, "no-permission");
        }
    }

    private void claimsList(Player p) {
        PlayerRecord pr = store().playerData(p.getUniqueId());
        int used = store().usedArea(p.getUniqueId());
        Map<String, String> head = new LinkedHashMap<>();
        head.put("accrued", String.valueOf(pr.accrued));
        head.put("bonus", String.valueOf(pr.bonus));
        head.put("used", String.valueOf(used));
        head.put("remaining", String.valueOf(pr.accrued + pr.bonus - used));
        msg(p, "claimslist-header", head);
        List<ClaimRecord> mine = store().claimsOwnedBy(p.getUniqueId());
        if (mine.isEmpty()) { msg(p, "claimslist-none"); return; }
        for (ClaimRecord r : mine) {
            if (r.parentId >= 0) continue;
            int subs = store().childrenOf(r.id).size();
            Map<String, String> vars = new LinkedHashMap<>();
            vars.put("world", r.world);
            vars.put("x", String.valueOf(r.lx));
            vars.put("z", String.valueOf(r.lz));
            vars.put("area", String.valueOf(r.area()));
            vars.put("admin", subs > 0 ? " (+" + subs + " subdivision" + (subs == 1 ? "" : "s") + ")" : "");
            msg(p, "claimslist-line", vars);
        }
    }

    // ------------------------------------------------------------ trust commands

    private void trust(Player p, String[] args, ClaimPermission tier) {
        if (args.length == 0) { p.sendMessage("§7Usage: /" + label(tier) + " <player>"); return; }
        String entry = entryFor(args[0]);
        if (entry.equals(p.getUniqueId().toString())) { msg(p, "trust-self"); return; }
        List<ClaimRecord> targets = trustTargets(p);
        if (targets.isEmpty()) { msg(p, "not-your-claim"); return; }
        for (ClaimRecord r : targets) core.service().setTrust(r, entry, tier, p);
        msg(p, "trust-added", Map.of("name", displayName(entry), "level", tierName(tier)));
    }

    private String label(ClaimPermission tier) {
        return switch (tier) {
            case Build -> "trust";
            case Container -> "containertrust";
            case Access -> "accesstrust";
            case Manage -> "permissiontrust";
            default -> "trust";
        };
    }

    private String tierName(ClaimPermission tier) {
        return switch (tier) {
            case Build -> "build";
            case Container -> "container";
            case Access -> "access";
            case Manage -> "permission";
            default -> tier.name().toLowerCase();
        };
    }

    private void untrust(Player p, String[] args) {
        if (args.length == 0) { p.sendMessage("§7Usage: /untrust <player>"); return; }
        String entry = entryFor(args[0]);
        List<ClaimRecord> targets = trustTargets(p);
        if (targets.isEmpty()) { msg(p, "not-your-claim"); return; }
        for (ClaimRecord r : targets) core.service().setTrust(r, entry, null, p);
        msg(p, "trust-removed", Map.of("name", displayName(entry)));
    }

    private void untrustAll(Player p) {
        List<ClaimRecord> targets = trustTargets(p);
        if (targets.isEmpty()) { msg(p, "not-your-claim"); return; }
        for (ClaimRecord r : targets) core.service().clearAllTrust(r, p);
        msg(p, "trust-cleared");
    }

    private void trustList(Player p) {
        ClaimRecord claim = stoodIn(p);
        if (claim == null) { msg(p, "no-claims-here"); return; }
        p.sendMessage("§aTrust in this claim (owner: " + core.nameOf(claim.owner) + "):");
        sendTier(p, "Build", claim.builders);
        sendTier(p, "Containers", claim.containers);
        sendTier(p, "Access", claim.accessors);
        sendTier(p, "Managers", claim.managers);
    }

    private void sendTier(Player p, String label, java.util.Set<String> entries) {
        if (entries.isEmpty()) { p.sendMessage("§7 " + label + ": nobody"); return; }
        List<String> names = new ArrayList<>();
        for (String e : entries) names.add(displayName(e));
        p.sendMessage("§7 " + label + ": " + String.join(", ", names));
    }

    // ------------------------------------------------------------ shape commands

    private void extendClaim(Player p, String[] args) {
        if (args.length == 0) { p.sendMessage("§7Usage: /extendclaim <blocks>"); return; }
        int n;
        try { n = Integer.parseInt(args[0]); } catch (NumberFormatException e) { return; }
        ClaimRecord claim = stoodIn(p);
        if (claim == null) { msg(p, "no-claims-here"); return; }
        if (!canManage(p, claim)) { msg(p, "not-your-claim"); return; }
        int lx = claim.lx, lz = claim.lz, gx = claim.gx, gz = claim.gz;
        float yaw = p.getLocation().getYaw();
        // Facing: 0 south(+z), 90 west(-x), 180 north(-z), 270 east(+x).
        int dir = Math.floorMod(Math.round(yaw / 90f), 4);
        switch (dir) {
            case 0 -> gz += n;
            case 1 -> lx -= n;
            case 2 -> lz -= n;
            case 3 -> gx += n;
            default -> { }
        }
        ClaimService.ResizeOutcome out = core.service().resizeTo(claim, lx, lz, gx, gz, p);
        switch (out) {
            case OK -> {
                msg(p, "claim-resized", Map.of("remaining", String.valueOf(store().remainingBlocks(p.getUniqueId()))));
                core.visualizer().showCorners(p, claim);
            }
            case TOO_SMALL -> msg(p, "claim-too-small", Map.of(
                    "width", String.valueOf(cfg().minimumWidth), "area", String.valueOf(cfg().minimumArea)));
            case OVERLAP -> msg(p, "claim-overlap");
            case NOT_ENOUGH_BLOCKS -> msg(p, "not-enough-blocks",
                    Map.of("needed", String.valueOf(Math.max(0, -store().remainingBlocks(p.getUniqueId())))));
            case OUTSIDE_PARENT -> msg(p, "claim-outside-parent");
            case EVENT_CANCELLED -> msg(p, "no-permission");
        }
    }

    private void abandon(Player p, boolean topOnly) {
        ClaimRecord claim = stoodIn(p);
        if (claim == null) { msg(p, "no-claims-here"); return; }
        ClaimRecord target = topOnly ? store().topLevel(claim) : claim;
        if (target == null) target = claim;
        if (!Trust.isOwnerChain(target, p.getUniqueId(), store()::byId)
                && !p.hasPermission("griefprevention.adminclaims")) {
            msg(p, "not-your-claim");
            return;
        }
        core.service().deleteClaim(target);
        msg(p, "claim-deleted");
    }

    private void restrictSubclaim(Player p) {
        ClaimRecord claim = stoodIn(p);
        if (claim == null) { msg(p, "no-claims-here"); return; }
        ClaimRecord top = store().topLevel(claim);
        if (top == null) top = claim;
        if (!Trust.isOwnerChain(top, p.getUniqueId(), store()::byId)) { msg(p, "not-your-claim"); return; }
        top.subclaimRestricted = !top.subclaimRestricted;
        core.service().touch(top);
        store().saveClaim(top);
        msg(p, top.subclaimRestricted ? "restrictsubclaim-on" : "restrictsubclaim-off");
    }

    private void toggleMode(Player p, String mode, String onKey, String offKey) {
        String current = core.shovelModes.get(p.getUniqueId());
        if (mode.equals(current)) {
            core.shovelModes.remove(p.getUniqueId());
            msg(p, offKey);
        } else {
            core.shovelModes.put(p.getUniqueId(), mode);
            msg(p, onKey);
        }
    }

    private void trapped(Player p) {
        ClaimRecord claim = stoodIn(p);
        boolean hasAccess = claim != null && Trust.check(claim, p.getUniqueId(), p.getName(),
                ClaimPermission.Access, core.adminOverride(p, claim), store()::byId);
        if (claim == null || hasAccess) {
            msg(p, "trapped-not-stuck");
            return;
        }
        Location start = p.getLocation().clone();
        msg(p, "trapped-wait", Map.of("seconds", "10"));
        Bukkit.getScheduler().runTaskLater(core.plugin(), () -> {
            if (!p.isOnline()) return;
            if (p.getLocation().distanceSquared(start) > 4) return; // they moved — they found a way
            ClaimRecord still = stoodIn(p);
            if (still == null || still.id != claim.id) return;
            // Step out: nearest point just past the nearest edge, at the surface.
            World world = p.getWorld();
            int x = p.getLocation().getBlockX(), z = p.getLocation().getBlockZ();
            int[] candidates = {
                    claim.lx - 2, z, claim.gx + 2, z, x, claim.lz - 2, x, claim.gz + 2
            };
            int bestX = candidates[0], bestZ = candidates[1];
            double best = Double.MAX_VALUE;
            for (int i = 0; i < candidates.length; i += 2) {
                double d = Math.pow(candidates[i] - x, 2) + Math.pow(candidates[i + 1] - z, 2);
                if (d < best) { best = d; bestX = candidates[i]; bestZ = candidates[i + 1]; }
            }
            int y = world.getHighestBlockYAt(bestX, bestZ) + 1;
            p.teleport(new Location(world, bestX + 0.5, y, bestZ + 0.5));
            msg(p, "trapped-moved");
        }, 200L);
    }

    private void transfer(Player p, String[] args) {
        if (args.length == 0) { p.sendMessage("§7Usage: /transferclaim <player>"); return; }
        ClaimRecord claim = stoodIn(p);
        if (claim == null) { msg(p, "no-claims-here"); return; }
        if (claim.parentId >= 0) {
            p.sendMessage("§cSubdivisions can't change owners — they belong to their parent claim.");
            return;
        }
        UUID target = core.resolveEntry(entryFor(args[0]));
        if (target == null) { msg(p, "player-not-found", Map.of("name", args[0])); return; }
        if (!core.service().transfer(claim, target, p)) {
            p.sendMessage("§cThat transfer didn't go through — the new owner may not have enough claim blocks for this claim.");
            return;
        }
        msg(p, "transfer-done", Map.of("name", core.nameOf(target)));
    }

    // ------------------------------------------------------------ economy (live: disabled)

    private boolean buySell(CommandSender sender, String[] args, boolean buy) {
        if (!(sender instanceof Player p)) { sender.sendMessage("ExcelClaims: players only."); return true; }
        double price = buy ? cfg().purchaseCost : cfg().sellValue;
        if (price <= 0.0) { msg(p, "economy-disabled"); return true; }
        int amount;
        try { amount = Integer.parseInt(args[0]); } catch (Exception e) {
            p.sendMessage("§7Usage: /" + (buy ? "buyclaimblocks" : "sellclaimblocks") + " <amount>");
            return true;
        }
        if (amount <= 0) return true;
        PlayerRecord pr = store().playerData(p.getUniqueId());
        if (buy) {
            if (cfg().maxBonus > 0 && pr.bonus + amount > cfg().maxBonus) {
                p.sendMessage("§cThat would put you over the bonus claim-block cap.");
                return true;
            }
            if (!VaultBridge.charge(p, amount * price)) {
                p.sendMessage("§cYou can't afford that (or no economy is available).");
                return true;
            }
            pr.bonus += amount;
        } else {
            if (pr.bonus < amount) { p.sendMessage("§cYou don't have that many bonus blocks to sell."); return true; }
            pr.bonus -= amount;
            VaultBridge.pay(p, amount * price);
        }
        store().savePlayer(pr);
        msg(p, "adjust-done", Map.of("name", p.getName(),
                "accrued", String.valueOf(pr.accrued), "bonus", String.valueOf(pr.bonus)));
        return true;
    }

    // ------------------------------------------------------------ staff

    private boolean adjust(CommandSender sender, String[] args, boolean accrued) {
        if (args.length < 2) {
            sender.sendMessage("§7Usage: /adjust" + (accrued ? "" : "bonus") + "claimblocks <player> <amount>");
            return true;
        }
        UUID target = core.resolveEntry(entryFor(args[0]));
        if (target == null) { msg(sender, "player-not-found", Map.of("name", args[0])); return true; }
        int delta;
        try { delta = Integer.parseInt(args[1]); } catch (NumberFormatException e) { return true; }
        PlayerRecord pr = store().playerData(target);
        if (accrued) pr.accrued = Math.max(0, pr.accrued + delta);
        else pr.bonus = Math.max(0, pr.bonus + delta);
        store().savePlayer(pr);
        msg(sender, "adjust-done", Map.of("name", core.nameOf(target),
                "accrued", String.valueOf(pr.accrued), "bonus", String.valueOf(pr.bonus)));
        return true;
    }

    private boolean deleteAll(CommandSender sender, String[] args) {
        if (args.length == 0) { sender.sendMessage("§7Usage: /deleteallclaims <player>"); return true; }
        UUID target = core.resolveEntry(entryFor(args[0]));
        if (target == null) { msg(sender, "player-not-found", Map.of("name", args[0])); return true; }
        int n = core.service().deleteAllClaimsFor(target);
        sender.sendMessage("§aDeleted " + n + " claim(s) belonging to " + core.nameOf(target) + ".");
        return true;
    }

    private boolean excelClaims(CommandSender sender, String[] args) {
        String sub = args.length == 0 ? "status" : args[0].toLowerCase();
        switch (sub) {
            case "status" -> {
                sender.sendMessage("§aExcelClaims " + BuildInfo.VERSION + " — build " + BuildInfo.BUILD_ID);
                long admins = store().all().stream().filter(c -> c.isAdmin() && !c.isSubdivision()).count();
                long subs = store().all().stream().filter(ClaimRecord::isSubdivision).count();
                sender.sendMessage("§7Claims: " + store().claimCount() + " (" + admins + " admin, "
                        + subs + " subdivisions) · Player accounts: "
                        + store().allPlayers().size() + " · Data: " + core.dataFolder().getPath());
                sender.sendMessage("§7Registered as 'GriefPrevention' for API compatibility with the other Excel plugins and Magic.");
            }
            case "reload" -> {
                if (!sender.hasPermission("griefprevention.adminclaims") && sender instanceof Player) {
                    msg(sender, "no-permission");
                    return true;
                }
                core.reload();
                sender.sendMessage("§aExcelClaims config reloaded.");
            }
            case "import" -> {
                if (sender instanceof Player && !sender.hasPermission("griefprevention.adminclaims")) {
                    msg(sender, "no-permission");
                    return true;
                }
                boolean confirm = args.length > 1 && args[1].equalsIgnoreCase("confirm");
                File gpDir = new File(core.dataFolder().getParentFile(), "GriefPreventionData");
                if (!gpDir.isDirectory()) {
                    sender.sendMessage("§cNo GriefPreventionData folder found beside the ExcelClaims data folder.");
                    return true;
                }
                if (store().claimCount() > 0 && !confirm) {
                    sender.sendMessage("§cThe store already holds " + store().claimCount()
                            + " claims. Import REPLACES them. Run /excelclaims import confirm to proceed.");
                    return true;
                }
                try {
                    GpImporter.ImportResult result = new GpImporter().importFrom(gpDir);
                    store().replaceAll(result.claims, result.players, result.nextClaimId);
                    sender.sendMessage("§aImported " + result.claims.size() + " claims and "
                            + result.players.size() + " player accounts from GriefPreventionData."
                            + (result.orphanedSubdivisions > 0 ? " (" + result.orphanedSubdivisions + " orphaned subdivisions re-rooted)" : "")
                            + (result.unreadableFiles > 0 ? " (" + result.unreadableFiles + " files skipped — see log)" : ""));
                    for (String note : result.notes) core.log().warning("ExcelClaims import: " + note);
                } catch (Exception e) {
                    sender.sendMessage("§cImport failed: " + e.getMessage());
                    core.log().warning("ExcelClaims import failed (" + e + ")");
                }
            }
            case "export" -> {
                if (sender instanceof Player && !sender.hasPermission("griefprevention.adminclaims")) {
                    msg(sender, "no-permission");
                    return true;
                }
                File target = new File(core.dataFolder().getParentFile(), "GriefPreventionData-restore");
                try {
                    GpExporter.ExportResult result = new GpExporter().exportTo(
                            store().all(), store().allPlayers(), store().nextClaimId(), target);
                    sender.sendMessage("§aExported " + result.claims + " claims and " + result.players
                            + " player accounts in GriefPrevention format to " + target.getPath());
                } catch (Exception e) {
                    sender.sendMessage("§cExport failed: " + e.getMessage());
                }
            }
            case "expire" -> {
                // 0.1.2: per-player expiration — report or set one
                // player's inactivity threshold. Same permission
                // as import/export; console always allowed.
                if (sender instanceof Player && !sender.hasPermission("griefprevention.adminclaims")) {
                    msg(sender, "no-permission");
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage("§7Usage: /excelclaims expire <player> [days|never]"
                            + " — with no value, reports the player's effective setting.");
                    return true;
                }
                UUID target = null;
                try {
                    target = UUID.fromString(args[1]);
                } catch (IllegalArgumentException ignored) {
                    // not a UUID — resolve as a player name below
                }
                if (target == null) target = core.resolveEntry(entryFor(args[1]));
                if (target == null) {
                    msg(sender, "player-not-found", Map.of("name", args[1]));
                    return true;
                }
                String name = core.nameOf(target);
                if (args.length < 3) {
                    int effective = core.config().effectiveExpirationDays(target);
                    String source = core.config().hasExpirationOverride(target)
                            ? "per-player override" : "server default";
                    sender.sendMessage("§aClaim expiration for " + name + ": §f"
                            + expireText(effective) + " §7(" + source + ")");
                    return true;
                }
                Integer value = parseExpireValue(args[2]);
                if (value == null) {
                    sender.sendMessage("§cUse 'never' or a day count of 1 or more."
                            + " Example: /excelclaims expire " + args[1] + " 30");
                    return true;
                }
                core.config().applyExpirationOverride(core.plugin().getConfig(), target, value);
                core.plugin().saveConfig();
                sender.sendMessage("§aClaim expiration for " + name + " set to §f" + expireText(value)
                        + "§a — per-player override saved to config; applies now, no restart needed.");
            }
            default -> sender.sendMessage("§7Usage: /excelclaims [status|import|export|reload|expire]");
        }
        return true;
    }

    /** Player-facing spelling of an expiration threshold: "never" or "N days". */
    private static String expireText(int days) {
        return days < 0 ? "never" : days + (days == 1 ? " day" : " days");
    }

    /**
     * Parse a /excelclaims expire value: "never", or a positive day
     * count. Zero and negative counts are rejected on purpose — a
     * fat-fingered 0 must not silently become "expire at the next
     * sweep" or a quiet never; the admin spells `never` out.
     * Returns null for anything unparseable.
     */
    static Integer parseExpireValue(String raw) {
        if (raw == null) return null;
        String s = raw.trim();
        if (s.equalsIgnoreCase("never")) return ClaimsConfig.EXPIRATION_NEVER;
        try {
            int v = Integer.parseInt(s);
            return v >= 1 ? v : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }
}

// ExcelClaims — Iris / Mike, for Wink Reyes Valley

package com.winkreyes.excellandmarks;

import com.winkreyes.excellandmarks.core.RentClock;
import com.winkreyes.excellandmarks.core.Spot;
import com.winkreyes.excellandmarks.core.SpotMenuModel;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

/**
 * /landmarks — the command twin of the compass door (SPEC §7, §8).
 * Every state-changing support operation is console-runnable; only the
 * menu opens and the look-at-the-sign register need an in-game player.
 */
public final class LandmarksCommand implements CommandExecutor {

    private final ExcelLandmarks plugin;

    public LandmarksCommand(ExcelLandmarks plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length >= 1 && args[0].equalsIgnoreCase("admin")) {
            return admin(sender, args);
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Console: use /landmarks admin [status|inspect|remove|release|setprice|setpaid|sweep|reload].");
            return true;
        }
        if (!player.hasPermission("landmarks.play")) {
            player.sendMessage("§cYou don't have permission to use Landmarks.");
            return true;
        }
        if (args.length >= 2 && args[0].equalsIgnoreCase("open")) {
            Spot s = plugin.registry().byId(args[1]);
            if (s == null) {
                player.sendMessage("§cNo landmark spot with id " + args[1] + ".");
                return true;
            }
            plugin.menus().openSpot(player, s);
            return true;
        }
        plugin.menus().openMyLandmarks(player);
        return true;
    }

    private boolean canAdmin(CommandSender sender) {
        return !(sender instanceof Player) || sender.hasPermission("landmarks.admin");
    }

    private boolean admin(CommandSender sender, String[] args) {
        if (!canAdmin(sender)) {
            sender.sendMessage("§cYou don't have permission for Landmarks admin commands.");
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage("Admin: status | inspect <id> | register <TYPE>:<TIER> [size] | remove <id> | release <id> | setprice <id> <rent|buy> <dollars> | setpaid <id> <yyyy-MM-dd> | sweep | reload");
            return true;
        }
        switch (args[1].toLowerCase()) {
            case "status" -> {
                long rented = plugin.registry().all().stream().filter(s -> s.mode == Spot.Mode.RENTED).count();
                long owned = plugin.registry().all().stream().filter(s -> s.mode == Spot.Mode.OWNED).count();
                sender.sendMessage("ExcelLandmarks " + plugin.getDescription().getVersion()
                        + ": spots=" + plugin.registry().size() + " (rented " + rented + ", owned " + owned
                        + ", available " + (plugin.registry().size() - rented - owned) + ")"
                        + ", gp=" + (plugin.gp().available() ? "ON" : "OFF")
                        + ", protect-attribution=" + (plugin.protect().available() ? "ON" : "OFF")
                        + ", floodgate-forms=" + (plugin.forms().available() ? "ON" : "OFF")
                        + ", redemptions=" + plugin.store().boxes.size() + " boxes / "
                        + plugin.store().virtual.size() + " virtual");
            }
            case "inspect" -> {
                Spot s = needSpot(sender, args, 2);
                if (s == null) return true;
                RentClock.State st = RentClock.stateAt(s, System.currentTimeMillis(), plugin.settings().graceDays);
                sender.sendMessage("Spot " + s.id + " \"" + s.name + "\" " + s.type + ":" + s.tier
                        + " at " + s.world + " " + s.x + "," + s.y + "," + s.z
                        + " | rent " + s.rentCents / 100 + " buy " + s.buyCents / 100
                        + (s.isPlot() ? " size " + s.size : ""));
                sender.sendMessage("  state=" + st + " owner=" + (s.ownerName == null ? "-" : s.ownerName)
                        + " mode=" + s.mode
                        + (s.mode == Spot.Mode.RENTED ? " paid-through=" + SpotMenuModel.date(s.paidThroughMs)
                                + " grace-end=" + SpotMenuModel.date(RentClock.graceEnd(s.paidThroughMs, plugin.settings().graceDays)) : "")
                        + " claim-id=" + (s.claimId == null ? "-" : s.claimId));
            }
            case "register" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("Register needs an in-game admin looking at the sign (or just write [Landmark] on it).");
                    return true;
                }
                Block b = player.getTargetBlockExact(6);
                if (b == null || !(b.getState() instanceof Sign sign)) {
                    player.sendMessage("§cLook at the sign to register (within 6 blocks).");
                    return true;
                }
                if (plugin.registry().bySign(b.getWorld().getName(), b.getX(), b.getY(), b.getZ()) != null) {
                    player.sendMessage("§cThat sign is already registered.");
                    return true;
                }
                Spot.Type type = plugin.settings().defaultType;
                Spot.Tier tier = plugin.settings().defaultTier;
                Integer size = null;
                if (args.length >= 3) {
                    String[] parts = args[2].toUpperCase().split(":");
                    try {
                        if (!parts[0].isEmpty()) type = Spot.Type.valueOf(parts[0]);
                        if (parts.length > 1) tier = Spot.Tier.valueOf(parts[1]);
                    } catch (IllegalArgumentException ex) {
                        player.sendMessage("§cType:tier must look like PLOT:HEART.");
                        return true;
                    }
                }
                if (args.length >= 4) {
                    try {
                        size = Integer.parseInt(args[3]);
                    } catch (NumberFormatException ex) {
                        player.sendMessage("§cSize must be a number.");
                        return true;
                    }
                }
                String existing = sign.getSide(org.bukkit.block.sign.Side.FRONT).getLine(1);
                Spot s = plugin.service().register(b.getWorld(), b.getX(), b.getY(), b.getZ(),
                        existing == null || existing.isBlank() ? "Unnamed Spot" : existing.trim(), type, tier, size);
                plugin.service().refreshSign(s);
                player.sendMessage("§aRegistered landmark spot " + s.id + " (" + s.name + ").");
            }
            case "remove" -> {
                Spot s = needSpot(sender, args, 2);
                if (s == null) return true;
                plugin.service().removeSpot(s);
                sender.sendMessage("Removed spot " + s.id + " (a rented plot was finalized first, if any). The sign block itself is untouched.");
            }
            case "release" -> {
                Spot s = needSpot(sender, args, 2);
                if (s == null) return true;
                if (s.isAvailable()) {
                    sender.sendMessage("Spot " + s.id + " is not taken.");
                    return true;
                }
                plugin.service().finalize(s, "admin-release");
                sender.sendMessage("Force-released spot " + s.id + ".");
            }
            case "setprice" -> {
                Spot s = needSpot(sender, args, 2);
                if (s == null || args.length < 5) {
                    if (s != null) sender.sendMessage("Usage: /landmarks admin setprice <id> <rent|buy> <dollars>");
                    return true;
                }
                try {
                    long cents = Math.round(Double.parseDouble(args[4]) * 100.0);
                    if (args[3].equalsIgnoreCase("rent")) s.rentCents = cents;
                    else if (args[3].equalsIgnoreCase("buy")) s.buyCents = cents;
                    else {
                        sender.sendMessage("Price kind must be rent or buy.");
                        return true;
                    }
                    plugin.store().saveSpots(plugin.registry());
                    plugin.service().refreshSign(s);
                    sender.sendMessage("Spot " + s.id + " " + args[3].toLowerCase() + " price set to $" + (cents / 100) + ".");
                } catch (NumberFormatException ex) {
                    sender.sendMessage("Dollars must be a number.");
                }
            }
            case "setpaid" -> {
                Spot s = needSpot(sender, args, 2);
                if (s == null || args.length < 4) {
                    if (s != null) sender.sendMessage("Usage: /landmarks admin setpaid <id> <yyyy-MM-dd>");
                    return true;
                }
                if (s.mode != Spot.Mode.RENTED) {
                    sender.sendMessage("Spot " + s.id + " is not rented.");
                    return true;
                }
                try {
                    LocalDate d = LocalDate.parse(args[3]);
                    s.paidThroughMs = d.atStartOfDay(ZoneId.of("America/Chicago")).toInstant().toEpochMilli();
                    plugin.store().saveSpots(plugin.registry());
                    plugin.service().refreshSign(s);
                    sender.sendMessage("Spot " + s.id + " paid-through set to " + SpotMenuModel.date(s.paidThroughMs) + ".");
                } catch (RuntimeException ex) {
                    sender.sendMessage("Date must be yyyy-MM-dd.");
                }
            }
            case "sweep" -> {
                List<String> done = plugin.service().sweepClock();
                sender.sendMessage("Rent clock swept: " + done.size() + " spot(s) finalized" + (done.isEmpty() ? "." : ": " + String.join(", ", done)));
            }
            case "reload" -> {
                plugin.reloadAll();
                sender.sendMessage("ExcelLandmarks config reloaded.");
            }
            default -> sender.sendMessage("Unknown admin command. Try: status, inspect, register, remove, release, setprice, setpaid, sweep, reload.");
        }
        return true;
    }

    private Spot needSpot(CommandSender sender, String[] args, int idx) {
        if (args.length <= idx) {
            sender.sendMessage("Missing spot id.");
            return null;
        }
        Spot s = plugin.registry().byId(args[idx]);
        if (s == null) sender.sendMessage("No landmark spot with id " + args[idx] + ".");
        return s;
    }
}

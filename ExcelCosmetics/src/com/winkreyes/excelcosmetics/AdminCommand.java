package com.winkreyes.excelcosmetics;

import com.winkreyes.excelcosmetics.core.Grant;
import com.winkreyes.excelcosmetics.core.Kind;
import com.winkreyes.excelcosmetics.core.PlayerState;
import com.winkreyes.excelcosmetics.core.PurchaseService;
import com.winkreyes.excelcosmetics.core.TimeText;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

import java.util.Locale;

/**
 * /excelcosmetics — admin + the ExcelShops dispatch surface.
 * Console senders are always allowed (the shop dispatches as
 * console); players need excelcosmetics.admin.
 *
 *   grant <player> <kind> <id> [weeks|own]   — no charge (Mike by hand)
 *   buy   <player> <kind> <id> [weeks|own]   — Vault charge + grant, atomic
 *   buy   <player> <kind>:<id>[:<weeks>|own] — same, single-token SKU form
 *   revoke <player> <kind> <id>
 *   status <player>
 *   setexpiry <player> <kind> <id> <seconds-from-now>   (rig/support)
 *   reload
 *
 * Targets resolve by exact online name first, then by their
 * stored UUID — a player who has never joined is refused, so a
 * typo can never grant a phantom account.
 */
final class AdminCommand implements CommandExecutor {

    private final ExcelCosmetics plugin;

    AdminCommand(ExcelCosmetics plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof ConsoleCommandSender) && !sender.hasPermission("excelcosmetics.admin")) {
            sender.sendMessage("You need the excelcosmetics.admin permission for that.");
            return true;
        }
        if (args.length == 0) {
            sender.sendMessage("Usage: /excelcosmetics grant|buy|revoke|status|setexpiry|reload ...");
            return true;
        }
        switch (args[0].toLowerCase()) {
            case "grant" -> grant(sender, args, false);
            case "buy" -> grant(sender, args, true);
            case "revoke" -> revoke(sender, args);
            case "status" -> status(sender, args);
            case "setexpiry" -> setexpiry(sender, args);
            case "reload" -> {
                plugin.reloadAll();
                sender.sendMessage("ExcelCosmetics reloaded.");
            }
            default -> sender.sendMessage("Unknown subcommand: " + args[0]);
        }
        return true;
    }

    /** Shared parser for grant and buy. args after the subcommand:
     *  <player> (<kind> <id> [tail] | <kind>:<id>[:<tail>]) where
     *  tail is a week count or "own". */
    private void grant(CommandSender sender, String[] args, boolean charge) {
        if (args.length < 3) {
            sender.sendMessage("Usage: /excelcosmetics " + (charge ? "buy" : "grant")
                    + " <player> <color|effect|set|emotes> <id> [weeks|own]");
            return;
        }
        OfflinePlayer target = resolveTarget(sender, args[1]);
        if (target == null) return;

        Kind kind;
        String id;
        String tail = null;
        if (args[2].contains(":")) {
            String[] parts = args[2].split(":");
            kind = Kind.parse(parts[0]);
            id = parts.length > 1 ? parts[1].toLowerCase() : null;
            if (parts.length > 2) tail = parts[2];
        } else {
            kind = Kind.parse(args[2]);
            id = args.length > 3 ? args[3].toLowerCase() : null;
            if (args.length > 4) tail = args[4];
        }
        if (kind == null || id == null || !plugin.catalog().exists(kind, id)) {
            sender.sendMessage("Unknown cosmetic SKU: " + args[2] + (id != null ? " " + id : ""));
            return;
        }
        boolean own = "own".equalsIgnoreCase(tail);
        int weeks = 1;
        if (tail != null && !own) {
            try {
                weeks = Integer.parseInt(tail);
            } catch (NumberFormatException e) {
                sender.sendMessage("Weeks must be a number or 'own': '" + tail + "'");
                return;
            }
            if (weeks < 1 || weeks > 52) {
                sender.sendMessage("Weeks must be between 1 and 52.");
                return;
            }
        }

        long now = System.currentTimeMillis();
        if (charge) {
            PurchaseService.Result result = plugin.purchases().buy(
                    target.getUniqueId(), kind, id, weeks, own);
            String line = String.format(Locale.US,
                    "ExcelCosmetics buy: %s %s:%s %s — %s ($%.2f)",
                    target.getName(), kind.name().toLowerCase(), id,
                    own ? "own" : weeks + "w", result.code(), result.price());
            plugin.getLogger().info(line);
            sender.sendMessage(line);
            tellBuyer(target, kind, id, weeks, own, result);
            if (result.code() == PurchaseService.Code.GRANT_FAILED_REFUND_LOST) {
                plugin.getLogger().severe("ExcelCosmetics: REFUND LOST for " + target.getName()
                        + " — " + result.detail());
            }
        } else {
            Grant g = plugin.store().grant(target.getUniqueId(), kind, id, now,
                    plugin.catalog().prices().rentalMillis(weeks), own);
            boolean saved = plugin.stateStore().save();
            String line = "ExcelCosmetics grant: " + target.getName() + " " + kind.name().toLowerCase()
                    + ":" + id + " " + (own ? "own" : weeks + "w")
                    + " -> " + TimeText.remaining(g, now) + (saved ? "" : " (WARNING: state save failed)");
            plugin.getLogger().info(line);
            sender.sendMessage(line);
        }
    }

    /** 0.1.2: the buyer hears the outcome (0.1.1 only told the console). */
    private void tellBuyer(OfflinePlayer target, Kind kind, String id, int weeks, boolean own,
                           PurchaseService.Result result) {
        if (result.code() == PurchaseService.Code.SUCCESS) {
            plugin.equipAfterPurchase(target.getUniqueId(), kind, id, weeks, own);
            return;
        }
        Player p = target.getPlayer();
        if (p == null) return;
        String what = plugin.catalog().displayName(kind, id);
        String msg = switch (result.code()) {
            case ALREADY_OWNED -> "You already own " + what + " forever, so nothing was charged. Wear it with /cosmetics.";
            case INSUFFICIENT_FUNDS -> String.format(Locale.US, "Not enough money for %s ($%,.0f). Nothing was charged.", what, result.price());
            case NOT_FOR_SALE -> what + " is retired and no longer sold. Nothing was charged.";
            case NO_ECONOMY, CHARGE_FAILED -> "The shop could not take payment right now. Nothing was charged.";
            case GRANT_FAILED_REFUNDED -> "Something went wrong saving " + what + ". Your money was refunded.";
            case GRANT_FAILED_REFUND_LOST -> "Something went wrong with " + what + ". An admin has been alerted.";
            default -> null;
        };
        if (msg != null) p.sendMessage(net.kyori.adventure.text.Component.text(msg)
                .color(net.kyori.adventure.text.format.TextColor.color(0xFF5555)));   // Bedrock &c
    }

    private void revoke(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sender.sendMessage("Usage: /excelcosmetics revoke <player> <kind> <id>");
            return;
        }
        OfflinePlayer target = resolveTarget(sender, args[1]);
        if (target == null) return;
        Kind kind = Kind.parse(args[2]);
        if (kind == null) {
            sender.sendMessage("Unknown kind: " + args[2]);
            return;
        }
        boolean removed = plugin.store().revoke(target.getUniqueId(), kind, args[3].toLowerCase());
        if (removed) plugin.stateStore().save();
        sender.sendMessage("ExcelCosmetics revoke: " + target.getName() + " " + args[2] + ":" + args[3]
                + (removed ? " removed" : " — nothing held"));
    }

    private void status(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("Usage: /excelcosmetics status <player>");
            return;
        }
        OfflinePlayer target = resolveTarget(sender, args[1]);
        if (target == null) return;
        PlayerState state = plugin.store().peek(target.getUniqueId());
        long now = System.currentTimeMillis();
        if (state == null || state.grants().isEmpty()) {
            sender.sendMessage(target.getName() + " holds no cosmetics.");
            return;
        }
        sender.sendMessage(target.getName() + " cosmetics:");
        for (Grant g : state.grants()) {
            sender.sendMessage(" - " + g.kind().name().toLowerCase() + ":" + g.id()
                    + " — " + TimeText.remaining(g, now)
                    + (g.isActiveAt(now) ? "" : " (inactive)"));
        }
        sender.sendMessage(" equipped: color=" + orNone(state.equippedColorId())
                + " effect=" + orNone(state.equippedEffectId())
                + " set=" + orNone(state.equippedSetId()));
    }

    private void setexpiry(CommandSender sender, String[] args) {
        if (args.length < 5) {
            sender.sendMessage("Usage: /excelcosmetics setexpiry <player> <kind> <id> <seconds-from-now>");
            return;
        }
        OfflinePlayer target = resolveTarget(sender, args[1]);
        if (target == null) return;
        Kind kind = Kind.parse(args[2]);
        if (kind == null) {
            sender.sendMessage("Unknown kind: " + args[2]);
            return;
        }
        long seconds;
        try {
            seconds = Long.parseLong(args[4]);
        } catch (NumberFormatException e) {
            sender.sendMessage("Seconds must be a number: '" + args[4] + "'");
            return;
        }
        PlayerState state = plugin.store().peek(target.getUniqueId());
        if (state == null) {
            sender.sendMessage(target.getName() + " holds no cosmetics.");
            return;
        }
        boolean ok = state.setExpiry(kind, args[3].toLowerCase(),
                System.currentTimeMillis() + seconds * 1000L);
        if (ok) plugin.stateStore().save();
        sender.sendMessage("ExcelCosmetics setexpiry: " + target.getName() + " " + args[2]
                + ":" + args[3] + (ok ? " now expires in " + seconds + "s"
                : " — refused (missing grant or permanent)"));
    }

    private String orNone(String s) { return s == null ? "none" : s; }

    /** Exact online name, else the stored UUID of someone who has
     *  actually played here. Never-played names are refused. */
    private OfflinePlayer resolveTarget(CommandSender sender, String name) {
        Player online = Bukkit.getPlayerExact(name);
        if (online != null) return online;
        OfflinePlayer cached = Bukkit.getOfflinePlayerIfCached(name);
        if (cached != null) return cached;
        OfflinePlayer op = Bukkit.getOfflinePlayer(name);
        if (!op.hasPlayedBefore()) {
            sender.sendMessage("Player '" + name + "' has never joined this server — refused.");
            return null;
        }
        return op;
    }
}

// ExcelCosmetics — an Excel plugin by WinkReyesValley. (Excel signature — MikeExcel)

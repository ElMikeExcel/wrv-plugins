package com.winkreyes.exceldowned;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

/**
 * /giveup (two-step, DESIGN §4 — 0.1.1: the first step opens the
 * button confirm UI; the typed two-step remains as the fallback and
 * shares the same arm state) and /downed (status + staff reload;
 * 0.1.2: console-admin revive/clear — see AdminPolicy).
 *
 * Excel — Wink Reyes Valley (Iris / Mike).
 */
public final class DownedCommands implements CommandExecutor {

    private final ExcelDowned plugin;

    public DownedCommands(ExcelDowned plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("giveup")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("Only a player can give up.");
                return true;
            }
            Player p = (Player) sender;
            DownedSession s = plugin.sessionOf(p.getUniqueId());
            if (s == null) {
                p.sendMessage(ChatColor.GRAY + "You are not downed.");
                return true;
            }
            long now = System.currentTimeMillis();
            // One arm state, two doors (0.1.1): a typed /giveup while
            // the give-up stands armed — by an open menu, an open form,
            // or a chat arm — CONFIRMS, exactly as the second typed
            // /giveup did in 0.1.0.
            if (s.isGiveUpArmed(now, plugin.giveUpWindowMs) || s.isGiveUpConfirmed()) {
                DownedSession.GiveUpResult r = s.giveUp(now, plugin.giveUpWindowMs);
                if (r == DownedSession.GiveUpResult.DIED) {
                    p.sendMessage(ChatColor.DARK_RED + "✝ You let go.");
                    plugin.finalizeDeath(p, DownedSession.Exit.GIVE_UP);
                }
                return true;
            }
            // First step: arm, then present the button confirm (the
            // house way). If no UI can be presented, the 0.1.0 chat
            // prompt is the fallback and the arm stands on its own.
            s.armGiveUp(now);
            if (plugin.giveUpUi().openConfirm(p)) {
                p.sendMessage(ChatColor.RED + "✝ Let go, or keep fighting? You have "
                        + (plugin.giveUpWindowMs / 1000) + "s.");
            } else {
                p.sendMessage(ChatColor.RED + "✝ Are you sure? Type " + ChatColor.BOLD + "/giveup"
                        + ChatColor.RESET + ChatColor.RED + " again within "
                        + (plugin.giveUpWindowMs / 1000) + "s to let go and die where you lie.");
            }
            return true;
        }
        if (cmd.getName().equalsIgnoreCase("downed")) {
            // 0.1.2 console admin (audit §4): revive/clear are routed
            // and gated by AdminPolicy BEFORE the legacy surface; the
            // gate (console explicit, otherwise exceldowned.admin)
            // precedes all target handling, and the legacy branches
            // below are the pre-0.1.2 behavior unchanged.
            AdminPolicy.Parsed parsed = AdminPolicy.route(args,
                    sender instanceof ConsoleCommandSender,
                    sender.hasPermission(AdminPolicy.PERMISSION));
            switch (parsed.route()) {
                case ADMIN_DENIED -> {
                    sender.sendMessage("ExcelDowned: /downed " + args[0].toLowerCase()
                            + " needs the " + AdminPolicy.PERMISSION + " permission.");
                    return true;
                }
                case ADMIN_USAGE -> {
                    sender.sendMessage("Usage: /downed revive <player> | /downed clear <player>");
                    return true;
                }
                case ADMIN_REVIVE -> { adminRevive(sender, parsed.target()); return true; }
                case ADMIN_CLEAR -> { adminClear(sender, parsed.target()); return true; }
                default -> { /* LEGACY: fall through to the pre-0.1.2 surface */ }
            }
            if (args.length >= 1 && args[0].equalsIgnoreCase("reload")
                    && sender.hasPermission("exceldowned.admin")) {
                plugin.reloadConfig();
                plugin.loadSettings();
                sender.sendMessage("ExcelDowned settings reloaded.");
                return true;
            }
            if (args.length >= 1 && sender.hasPermission("exceldowned.admin")) {
                Player t = Bukkit.getPlayer(args[0]);
                if (t == null) { sender.sendMessage("No such player online."); return true; }
                DownedSession s = plugin.sessionOf(t.getUniqueId());
                sender.sendMessage(t.getName() + ": " + (s == null ? "not downed"
                        : "DOWNED, bleed out in "
                        + ExcelDowned.formatClock(s.remainingMs(System.currentTimeMillis()))
                        + " (cause " + s.cause() + ")"));
                return true;
            }
            if (sender instanceof Player) {
                DownedSession s = plugin.sessionOf(((Player) sender).getUniqueId());
                sender.sendMessage(s == null ? "You are not downed."
                        : "DOWNED — bleed out in "
                        + ExcelDowned.formatClock(s.remainingMs(System.currentTimeMillis())));
            }
            return true;
        }
        return false;
    }

    // ---------- 0.1.2 console admin handlers (thin Bukkit mechanics;
    // ---------- every decision is AdminPolicy's) ----------

    private static String actorOf(CommandSender sender) {
        return sender instanceof ConsoleCommandSender ? "CONSOLE" : sender.getName();
    }

    /** Classify a named target: online player, its live session, and
     *  any durable (offline) session recorded under that name. */
    private AdminPolicy.TargetKind classify(String target, Player[] onlineOut,
                                            DownedSession[] sessionOut) {
        Player online = Bukkit.getPlayerExact(target);
        DownedSession live = online == null ? null : plugin.sessionOf(online.getUniqueId());
        DownedSession durable = live == null ? plugin.sessionByName(target) : null;
        onlineOut[0] = online;
        sessionOut[0] = live != null ? live : durable;
        return AdminPolicy.targetKind(online != null, live != null, durable != null);
    }

    private void adminRevive(CommandSender sender, String target) {
        Player[] online = new Player[1];
        DownedSession[] session = new DownedSession[1];
        AdminPolicy.TargetKind kind = classify(target, online, session);
        switch (AdminPolicy.decideRevive(kind)) {
            case PROCEED -> {
                if (plugin.adminRevive(actorOf(sender), online[0])) {
                    sender.sendMessage("Downed admin: " + online[0].getName()
                            + " revived — downed state resolved at 50% health, no death.");
                } else {
                    sender.sendMessage("Downed admin: " + target + " is no longer downed; nothing was changed.");
                }
            }
            case NOT_DOWNED -> sender.sendMessage("Downed admin: " + online[0].getName()
                    + " is not downed; nothing to revive.");
            case OFFLINE_ONLY -> sender.sendMessage("Downed admin: " + session[0].playerName()
                    + " is DOWNED but offline (bleed out in "
                    + ExcelDowned.formatClock(session[0].remainingMs(System.currentTimeMillis()))
                    + ") — revive restores live health and needs the player online, so nothing was changed. "
                    + "Use /downed clear " + session[0].playerName() + " to remove the offline session instead.");
            case UNKNOWN -> sender.sendMessage("Downed admin: no player named '" + target
                    + "' is online and no downed session exists for that name. Nothing was changed.");
        }
    }

    private void adminClear(CommandSender sender, String target) {
        Player[] online = new Player[1];
        DownedSession[] session = new DownedSession[1];
        AdminPolicy.TargetKind kind = classify(target, online, session);
        switch (AdminPolicy.decideClear(kind)) {
            case PROCEED -> {
                boolean wasOnline = kind == AdminPolicy.TargetKind.ONLINE_DOWNED;
                String name = session[0].playerName();
                if (plugin.adminClear(actorOf(sender), session[0].playerId())) {
                    sender.sendMessage("Downed admin: " + name + "'s downed session cleared ("
                            + (wasOnline ? "online" : "offline record")
                            + "). No health was restored and no death was run.");
                } else {
                    sender.sendMessage("Downed admin: " + target + " is no longer downed; nothing was changed.");
                }
            }
            case NOT_DOWNED -> sender.sendMessage("Downed admin: " + online[0].getName()
                    + " is not downed; nothing to clear.");
            case UNKNOWN -> sender.sendMessage("Downed admin: no player named '" + target
                    + "' is online and no downed session exists for that name. Nothing was changed.");
        }
    }
}

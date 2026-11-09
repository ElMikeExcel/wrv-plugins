package com.winkreyes.exceldowned;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * /giveup (two-step, DESIGN §4 — 0.1.1: the first step opens the
 * button confirm UI; the typed two-step remains as the fallback and
 * shares the same arm state) and /downed (status + staff reload).
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
}

package com.winkreyes.exceldetails;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * /details — toggle, window, clear, reset, status, reload.
 *
 * Excel — Wink Reyes Valley (Iris / Mike).
 */
public final class DetailsCommands implements CommandExecutor, TabCompleter {

    private final ExcelDetails plugin;

    public DetailsCommands(ExcelDetails plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("ExcelDetails: the meter lives on a player's sidebar — run it in game.");
            return true;
        }
        String sub = args.length == 0 ? "status" : args[0].toLowerCase();
        long now = System.currentTimeMillis();
        switch (sub) {
            case "on" -> {
                plugin.prefs().setEnabled(player.getUniqueId(), true);
                plugin.savePrefs();
                player.sendMessage("§6Details! §fon — the meter is watching this fight.");
            }
            case "off" -> {
                plugin.prefs().setEnabled(player.getUniqueId(), false);
                plugin.savePrefs();
                plugin.renderer().cleanup(player);
                player.sendMessage("§6Details! §foff — sidebar handed back.");
            }
            case "toggle" -> {
                boolean on = plugin.prefs().toggle(player.getUniqueId());
                plugin.savePrefs();
                if (!on) plugin.renderer().cleanup(player);
                player.sendMessage(on
                        ? "§6Details! §fon — the meter is watching this fight."
                        : "§6Details! §foff — sidebar handed back.");
            }
            case "window" -> {
                CombatStats.Window w = args.length >= 2 ? CombatStats.Window.parse(args[1]) : null;
                if (w == null) {
                    player.sendMessage("§6Details! §fwindows: damage, taken, healing — or /details next.");
                } else {
                    plugin.prefs().setWindow(player.getUniqueId(), w);
                    plugin.savePrefs();
                    player.sendMessage("§6Details! §fwindow: " + w.title() + ".");
                }
            }
            case "next" -> {
                CombatStats.Window w = plugin.prefs().window(player.getUniqueId()).next();
                plugin.prefs().setWindow(player.getUniqueId(), w);
                plugin.savePrefs();
                player.sendMessage("§6Details! §fwindow: " + w.title() + ".");
            }
            case "clear" -> {
                plugin.tracker().clearCurrent(now);
                player.sendMessage("§6Details! §fsegment cleared — the next hit starts a fresh one.");
            }
            case "reset" -> {
                plugin.tracker().resetAll(now);
                player.sendMessage("§6Details! §fsegment and overall totals wiped clean.");
            }
            case "reload" -> {
                if (!player.hasPermission("exceldetails.admin")) {
                    player.sendMessage("§6Details! §fthat one is staff-side.");
                } else {
                    plugin.reloadConfig();
                    player.sendMessage("§6Details! §fsettings reloaded.");
                }
            }
            case "status" -> {
                CombatStats s = plugin.tracker().currentStats(player.getUniqueId());
                CombatStats.Window w = plugin.prefs().window(player.getUniqueId());
                player.sendMessage("§6§lDetails! §7build " + BuildInfo.BUILD_ID);
                player.sendMessage("§fWindow: §e" + w.title()
                        + " §f• Segment: §e" + MeterMath.clock(plugin.tracker().elapsedMs(now))
                        + (plugin.tracker().isActive() ? " §7(live)" : " §7(closed)"));
                player.sendMessage("§fDone §e" + MeterMath.compact(s.damageDone())
                        + " §f• Taken §e" + MeterMath.compact(s.damageTaken())
                        + " §f• Healing §e" + MeterMath.compact(s.healingDone()));
                player.sendMessage("§7/details on|off • window damage|taken|healing • next • clear • reset");
            }
            default -> player.sendMessage("§6Details! §f/details on|off • window damage|taken|healing • next • clear • reset");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return List.of("on", "off", "toggle", "window", "next", "clear", "reset", "status", "reload");
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("window")) {
            return List.of("damage", "taken", "healing");
        }
        return List.of();
    }
}

package com.winkreyes.exceldetails;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;

/**
 * /details — on/off, show (the 0.2.0 checkbox mix), window + next
 * (single-section quick paths), clear, reset, status, reload.
 *
 * Semantics (DESIGN build-0.2.0 §4):
 *   show <metric>  toggles that one metric in the player's mix
 *   window <metric> shows EXACTLY that metric (and focuses it)
 *   next           focus advances; shown becomes exactly that window
 *
 * Excel — Wink Reyes Valley (Iris / Mike).
 */
public final class DetailsCommands implements CommandExecutor, TabCompleter {

    private static final String HELP =
            "§7/details on|off • show damage|taken|healing • window damage|taken|healing • next • clear • reset";

    private final ExcelDetails plugin;

    public DetailsCommands(ExcelDetails plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("ExcelDetails: the meter lives on a player's sidebar — run it in game.");
            return true;
        }
        UUID id = player.getUniqueId();
        String sub = args.length == 0 ? "status" : args[0].toLowerCase();
        long now = System.currentTimeMillis();
        switch (sub) {
            case "on" -> {
                plugin.prefs().setEnabled(id, true);
                plugin.savePrefs();
                player.sendMessage("§6Details! §fon — the meter is watching this fight.");
            }
            case "off" -> {
                plugin.prefs().setEnabled(id, false);
                plugin.savePrefs();
                plugin.renderer().cleanup(player);
                player.sendMessage("§6Details! §foff — sidebar handed back.");
            }
            case "toggle" -> {
                boolean on = plugin.prefs().toggle(id);
                plugin.savePrefs();
                if (!on) plugin.renderer().cleanup(player);
                player.sendMessage(on
                        ? "§6Details! §fon — the meter is watching this fight."
                        : "§6Details! §foff — sidebar handed back.");
            }
            case "show" -> {
                CombatStats.Window w = args.length >= 2 ? CombatStats.Window.parse(args[1]) : null;
                if (w == null) {
                    player.sendMessage("§6Details! §fyour mix: " + mixStates(id)
                            + " §7— /details show damage|taken|healing ticks one.");
                } else {
                    boolean on = plugin.prefs().toggleShown(id, w);
                    plugin.savePrefs();
                    player.sendMessage("§6Details! §f" + w.title() + ": " + (on ? "§aON" : "§7OFF")
                            + " §f• showing: §e" + mixSummary(id));
                }
            }
            case "window" -> {
                CombatStats.Window w = args.length >= 2 ? CombatStats.Window.parse(args[1]) : null;
                if (w == null) {
                    player.sendMessage("§6Details! §fwindows: damage, taken, healing — or /details next.");
                } else {
                    plugin.prefs().showOnly(id, w);
                    plugin.savePrefs();
                    player.sendMessage("§6Details! §fwindow: " + w.title()
                            + " §7(only this section — /details show adds more).");
                }
            }
            case "next" -> {
                CombatStats.Window w = plugin.prefs().window(id).next();
                plugin.prefs().showOnly(id, w);
                plugin.savePrefs();
                player.sendMessage("§6Details! §fwindow: " + w.title()
                        + " §7(only this section — /details show adds more).");
            }
            case "clear" -> {
                plugin.tracker().clearCurrent(now);
                player.sendMessage("§6Details! §fsegment cleared — the next hit starts a fresh one.");
            }
            case "reset" -> {
                plugin.tracker().resetAll(now);
                plugin.savePrefs(); // 0.2.0: the wipe persists like everything else
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
                CombatStats s = plugin.tracker().currentStats(id);
                player.sendMessage("§6§lDetails! §7build " + BuildInfo.BUILD_ID);
                player.sendMessage("§fShowing: §e" + mixSummary(id)
                        + " §f• Segment: §e" + MeterMath.clock(plugin.tracker().elapsedMs(now))
                        + (plugin.tracker().isActive() ? " §7(live)" : " §7(closed)"));
                player.sendMessage("§fDone §e" + MeterMath.compact(s.damageDone())
                        + " §f• Taken §e" + MeterMath.compact(s.damageTaken())
                        + " §f• Healing §e" + MeterMath.compact(s.healingDone()));
                player.sendMessage(HELP);
            }
            default -> player.sendMessage("§6Details! §f" + HELP.replace("§7", ""));
        }
        return true;
    }

    /** "Damage Done + Healing Done", or the all-off wording. */
    private String mixSummary(UUID id) {
        var shown = plugin.prefs().shownWindows(id);
        if (shown.isEmpty()) return "nothing — all sections off";
        StringBuilder sb = new StringBuilder();
        for (CombatStats.Window w : CombatStats.Window.values()) {
            if (shown.contains(w)) {
                if (sb.length() > 0) sb.append(" + ");
                sb.append(w.title());
            }
        }
        return sb.toString();
    }

    /** Per-metric ON/OFF states, for the bare /details show readout. */
    private String mixStates(UUID id) {
        StringBuilder sb = new StringBuilder();
        for (CombatStats.Window w : CombatStats.Window.values()) {
            if (sb.length() > 0) sb.append(" §f• ");
            sb.append("§e").append(w.title()).append(" ")
                    .append(plugin.prefs().shows(id, w) ? "§aON" : "§7OFF");
        }
        return sb.toString();
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return List.of("on", "off", "toggle", "show", "window", "next", "clear", "reset", "status", "reload");
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("window") || args[0].equalsIgnoreCase("show"))) {
            return List.of("damage", "taken", "healing");
        }
        return List.of();
    }
}

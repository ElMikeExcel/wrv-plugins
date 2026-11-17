package com.winkreyes.excelwitherskulls;

import com.winkreyes.excelwitherskulls.core.SkullSettings;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.List;

/** /witherskulls status|reload — the whole admin surface. */
final class WitherSkullsCommand implements CommandExecutor, TabCompleter {

    private final ExcelWitherSkulls plugin;

    WitherSkullsCommand(ExcelWitherSkulls plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission(ExcelWitherSkulls.PERM_ADMIN)) {
            sender.sendMessage("ExcelWitherSkulls: you need the excelwitherskulls.admin permission.");
            return true;
        }
        String sub = args.length > 0 ? args[0].toLowerCase() : "status";
        switch (sub) {
            case "status" -> {
                SkullSettings s = plugin.settings();
                sender.sendMessage("ExcelWitherSkulls: "
                        + (s.enabled() ? "top-up ON" : "top-up DISABLED by config")
                        + ", target " + s.targetChancePercent() + "%"
                        + ", debug log " + (s.debugLog() ? "on" : "off")
                        + ", EliteMobs seam " + (plugin.seamAvailable() ? "BOUND" : "UNAVAILABLE"));
            }
            case "reload" -> {
                boolean ok = plugin.reloadAll();
                SkullSettings s = plugin.settings();
                sender.sendMessage(ok
                        ? "ExcelWitherSkulls: reloaded - top-up "
                                + (s.enabled() ? "ON" : "DISABLED") + ", target "
                                + s.targetChancePercent() + "%."
                        : "ExcelWitherSkulls: reload FAILED - previous settings kept.");
            }
            default -> sender.sendMessage("ExcelWitherSkulls: usage /witherskulls <status|reload>");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1 && sender.hasPermission(ExcelWitherSkulls.PERM_ADMIN)) {
            return List.of("status", "reload").stream()
                    .filter(x -> x.startsWith(args[0].toLowerCase()))
                    .toList();
        }
        return List.of();
    }
}

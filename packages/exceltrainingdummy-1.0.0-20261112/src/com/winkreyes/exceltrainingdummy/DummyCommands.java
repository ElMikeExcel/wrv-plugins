package com.winkreyes.exceltrainingdummy;

import com.winkreyes.exceltrainingdummy.core.ReportText;
import com.winkreyes.exceltrainingdummy.core.Tier;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * /dummy — the command is a door, not the controls: with no argument
 * it opens the menu (the compass menu's "Training Dummy" entry runs
 * this same command). Subcommands exist as the typed fallback the
 * house keeps beside every menu: spawn / removeall / status /
 * version / reload.
 */
public final class DummyCommands implements CommandExecutor, TabCompleter {
    private final ExcelTrainingDummy plugin;

    public DummyCommands(ExcelTrainingDummy plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("menu")) {
            if (sender instanceof Player p) DummyMenus.openSpawnMenu(plugin, p);
            else sender.sendMessage("[Training Dummy] The menu is for players; console: dummy version / dummy reload.");
            return true;
        }
        switch (args[0].toLowerCase()) {
            case "spawn" -> {
                if (!(sender instanceof Player p)) { sender.sendMessage("[Training Dummy] Players only."); return true; }
                int count = 1;
                if (args.length >= 2) {
                    try { count = Integer.parseInt(args[1]); }
                    catch (NumberFormatException e) {
                        p.sendMessage(Component.text("[Training Dummy] Count must be 1, 3 or 5.", NamedTextColor.RED));
                        return true;
                    }
                }
                if (args.length >= 3) {
                    plugin.setup(p.getUniqueId()).tier = Tier.parse(args[2], plugin.setup(p.getUniqueId()).tier);
                }
                plugin.spawn(p, count);
                return true;
            }
            case "removeall" -> {
                if (sender instanceof Player p) plugin.removeAllOf(p.getUniqueId(), true);
                else sender.sendMessage("[Training Dummy] Players only.");
                return true;
            }
            case "status" -> {
                if (!(sender instanceof Player p)) { sender.sendMessage("[Training Dummy] Players only."); return true; }
                List<Dummy> mine = plugin.dummiesOf(p.getUniqueId());
                if (mine.isEmpty()) {
                    p.sendMessage(Component.text("[Training Dummy] You have no dummies up. /dummy to spawn some.",
                            NamedTextColor.GRAY));
                    return true;
                }
                long now = System.currentTimeMillis();
                for (Dummy d : mine) {
                    for (String line : ReportText.statusReadout(d.session, d.ownerName, now,
                            d.hazard, d.bossArmor, plugin.scaleFactor())) {
                        p.sendMessage(Component.text(line, NamedTextColor.AQUA));
                    }
                }
                return true;
            }
            case "version" -> {
                sender.sendMessage("[Training Dummy] ExcelTrainingDummy " + BuildInfo.VERSION
                        + " (build " + BuildInfo.BUILD_ID + ") — "
                        + plugin.totalDummyCount() + " dummy(s) up server-wide.");
                return true;
            }
            case "reload" -> {
                if (!sender.hasPermission("exceltrainingdummy.admin")) {
                    sender.sendMessage(Component.text("[Training Dummy] That one is admin-only.", NamedTextColor.RED));
                    return true;
                }
                plugin.loadSettings();
                sender.sendMessage("[Training Dummy] Config reloaded (build " + BuildInfo.BUILD_ID + ").");
                return true;
            }
            default -> {
                sender.sendMessage("[Training Dummy] /dummy (menu) · /dummy spawn 1|3|5 [practice|dungeon|seal]"
                        + " · /dummy removeall · /dummy status · /dummy version");
                return true;
            }
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) return List.of("menu", "spawn", "removeall", "status", "version", "reload");
        if (args.length == 2 && args[0].equalsIgnoreCase("spawn")) return List.of("1", "3", "5");
        if (args.length == 3 && args[0].equalsIgnoreCase("spawn"))
            return List.of("practice", "dungeon", "seal");
        return List.of();
    }
}

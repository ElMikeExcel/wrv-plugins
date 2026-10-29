// Excel signature - WRV maintained source; existing authorship notices preserved.
package com.winkreyes.excelmenu.bukkit;

import com.winkreyes.excelmenu.core.MenuDef;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.defaults.BukkitCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * /excelmenu (list | open | reload) and the /dm compatibility
 * alias. The alias exists so the rest of the fleet keeps working
 * untouched the day DeluxeMenus leaves: ExcelBedrockMenu routes
 * Java players with "dm open server_menu <player>", the shop
 * villager's open-command defaults to "dm open spawnshop
 * %player%", and MenuJoin opens the same way. Same subcommands,
 * same argument shapes.
 */
final class MenuCommands implements CommandExecutor {

    static final String ADMIN = "excelmenu.admin";

    private final JavaPlugin plugin;
    private final MenuService service;
    private final ExcelMenu main;

    MenuCommands(JavaPlugin plugin, MenuService service, ExcelMenu main) {
        this.plugin = plugin;
        this.service = service;
        this.main = main;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("§eExcelMenu — /" + label + " list | open <menu> [player] | reload");
            return true;
        }
        switch (args[0].toLowerCase()) {
            case "list" -> list(sender);
            case "reload" -> {
                if (!sender.hasPermission(ADMIN) && sender instanceof Player) {
                    sender.sendMessage("§cYou do not have permission to do that!");
                    return true;
                }
                main.reloadMenus(sender);
            }
            case "open" -> open(sender, args);
            default -> sender.sendMessage("§cUnknown subcommand — list | open | reload");
        }
        return true;
    }

    private void list(CommandSender sender) {
        Map<String, MenuDef> menus = service.menus();
        List<String> names = new ArrayList<>(menus.keySet());
        names.sort(String::compareTo);
        sender.sendMessage("§eExcelMenu: " + names.size() + " menus loaded:");
        for (String name : names) {
            MenuDef def = menus.get(name);
            String cmd = def.command == null ? "" : " §7(command /" + def.command + ")";
            sender.sendMessage("§a- " + name + "§7 (" + def.items.size() + " items)" + cmd);
        }
        Map<String, List<String>> failed = main.lastFailures();
        if (!failed.isEmpty()) {
            sender.sendMessage("§cFailed definitions: " + String.join(", ", failed.keySet()));
        }
    }

    private void open(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§cUsage: open <menu> [player]");
            return;
        }
        String menuId = args[1];
        Player target;
        if (args.length >= 3) {
            if (sender instanceof Player && !sender.hasPermission(ADMIN)) {
                sender.sendMessage("§cYou do not have permission to do that!");
                return;
            }
            target = Bukkit.getPlayerExact(args[2]);
            if (target == null) {
                sender.sendMessage("§cPlayer '" + args[2] + "' is not online.");
                return;
            }
        } else if (sender instanceof Player p) {
            target = p;
        } else {
            sender.sendMessage("§cConsole must name a player: open <menu> <player>");
            return;
        }
        service.openMenu(target, menuId);
    }

    /** A menu's own open command (e.g. /shop, /basicsmenu). */
    static final class MenuOpenCommand extends BukkitCommand {
        private final MenuService service;
        private final String menuId;

        MenuOpenCommand(MenuService service, String menuId, String command) {
            super(command);
            this.service = service;
            this.menuId = menuId;
            setDescription("Open the " + menuId + " menu (ExcelMenu).");
            setUsage("/" + command + " [player]");
        }

        @Override
        public boolean execute(CommandSender sender, String label, String[] args) {
            if (args.length >= 1) {
                if (sender instanceof Player && !sender.hasPermission(ADMIN)) {
                    sender.sendMessage("§cYou do not have permission to do that!");
                    return true;
                }
                Player target = Bukkit.getPlayerExact(args[0]);
                if (target == null) {
                    sender.sendMessage("§cPlayer '" + args[0] + "' is not online.");
                    return true;
                }
                service.openMenu(target, menuId);
                return true;
            }
            if (sender instanceof Player p) {
                service.openMenu(p, menuId);
            } else {
                sender.sendMessage("§cConsole must name a player: /" + label + " <player>");
            }
            return true;
        }
    }
}

// ExcelMenu — an Excel plugin by WinkReyesValley. (Excel signature — MikeExcel)

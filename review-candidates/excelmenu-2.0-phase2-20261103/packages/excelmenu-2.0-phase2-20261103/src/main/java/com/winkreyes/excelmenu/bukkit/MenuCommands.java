// Excel signature - WRV maintained source; existing authorship notices preserved.
package com.winkreyes.excelmenu.bukkit;

import com.winkreyes.excelmenu.core.MenuDef;
import com.winkreyes.excelmenu.core.registry.EntryDef;
import com.winkreyes.excelmenu.core.registry.ResolvedEntry;
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
import java.util.TreeMap;

/**
 * /excelmenu and the /dm compatibility alias — ONE executor for the
 * merged Phase 1 surface:
 *
 * <ul>
 *   <li>{@code list} — the definition menus (0.1.8's contract) AND
 *       the registry entries with their platform flags, date windows
 *       and guards (Phase 0's parity audit as a command: a
 *       one-platform or expiring entry is DECLARED, never
 *       discovered).</li>
 *   <li>{@code open <menu> [player]} — opens any menu, definition or
 *       registry (MenuService routes). The /dm alias keeps the
 *       fleet's existing "dm open ..." dispatches working untouched:
 *       ExcelBedrockMenu routes Java players with "dm open
 *       server_menu <player>", the shop villager's open-command
 *       defaults to "dm open spawnshop %player%".</li>
 *   <li>{@code entries [menu]} — resolve the registry for the sender
 *       (Phase 0 surface).</li>
 *   <li>{@code reload} — config + registry (with diff) + layouts +
 *       definitions, one pass.</li>
 * </ul>
 */
final class MenuCommands implements CommandExecutor {

    static final String ADMIN = "excelmenu.admin";

    private final JavaPlugin plugin;
    private final MenuService menus;
    private final EntryService entries;
    private final RegistryMenus registryMenus;
    private final PlatformBridge bridge;
    private final ExcelMenu main;

    MenuCommands(JavaPlugin plugin, MenuService menus, EntryService entries,
                 RegistryMenus registryMenus, PlatformBridge bridge) {
        this.plugin = plugin;
        this.menus = menus;
        this.entries = entries;
        this.registryMenus = registryMenus;
        this.bridge = bridge;
        this.main = (ExcelMenu) plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label,
                             String[] args) {
        if (args.length == 0) {
            sender.sendMessage("§eExcelMenu 2.0 — /" + label
                    + " list | open <menu> [player] | entries [menu] | reload");
            return true;
        }
        switch (args[0].toLowerCase()) {
            case "list" -> list(sender);
            case "entries" -> entries(sender, args.length >= 2 ? args[1] : "server_menu");
            case "open" -> open(sender, args);
            case "reload" -> {
                if (sender instanceof Player && !sender.hasPermission(ADMIN)) {
                    sender.sendMessage("§cYou do not have permission to do that!");
                    return true;
                }
                main.reloadAll(sender);
            }
            default -> sender.sendMessage(
                    "§cUnknown subcommand — list | open | entries | reload");
        }
        return true;
    }

    private void list(CommandSender sender) {
        Map<String, MenuDef> defs = menus.menus();
        List<String> names = new ArrayList<>(defs.keySet());
        names.sort(String::compareTo);
        sender.sendMessage("§eExcelMenu 2.0: " + names.size()
                + " definition menus loaded:");
        for (String name : names) {
            MenuDef def = defs.get(name);
            String cmd = def.command == null ? "" : " §7(command /" + def.command + ")";
            sender.sendMessage("§a- " + name + "§7 (" + def.items.size() + " items)" + cmd);
        }
        Map<String, List<String>> failedDefs = main.lastMenuFailures();
        if (!failedDefs.isEmpty()) {
            sender.sendMessage("§cFailed definitions: "
                    + String.join(", ", failedDefs.keySet()));
        }
        List<String> layoutNames = new ArrayList<>(registryMenus.layoutIds());
        layoutNames.sort(String::compareTo);
        sender.sendMessage("§eRegistry menus (layouts): "
                + String.join(", ", layoutNames));
        Map<String, EntryDef> all = new TreeMap<>(entries.entries());
        sender.sendMessage("§eRegistry: " + all.size() + " entries:");
        for (EntryDef e : all.values()) {
            StringBuilder sb = new StringBuilder("§a- ").append(e.id())
                    .append(" §7[").append(e.menu()).append(" / ").append(e.group())
                    .append(" · order ").append(e.order());
            if (e.orderBedrock() != null) sb.append(" · bedrock ").append(e.orderBedrock());
            sb.append(" · ");
            List<String> plats = new ArrayList<>();
            e.platforms().forEach(p -> plats.add(p.token()));
            sb.append(String.join("+", plats));
            if (e.visibleFrom() != null || e.visibleUntil() != null) {
                sb.append(" · window ").append(e.visibleFrom() == null ? "…"
                        : e.visibleFrom().toString())
                        .append("→").append(e.visibleUntil() == null ? "…"
                                : e.visibleUntil().toString());
            }
            if (e.requiresPlugin() != null) sb.append(" · needs ").append(e.requiresPlugin());
            if (e.requiresPermission() != null) {
                sb.append(" · perm ").append(e.requiresPermission());
            }
            if (e.statusPlaceholder() != null) {
                sb.append(" · states ").append(e.statusPlaceholder());
            }
            if (e.origin() == EntryDef.Origin.API) sb.append(" · api");
            sb.append("]");
            sender.sendMessage(sb.toString());
        }
        Map<String, List<String>> failed = entries.lastFailures();
        if (!failed.isEmpty()) {
            sender.sendMessage("§cFailed entry files: " + String.join(", ", failed.keySet()));
        }
    }

    private void entries(CommandSender sender, String menuId) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§eConsole view is the raw registry — use /excelmenu list."
                    + " (entries <menu> resolves for a player sender.)");
            return;
        }
        BukkitViewerContext ctx = new BukkitViewerContext(player, bridge);
        List<ResolvedEntry> resolved = entries.entriesFor(menuId, ctx);
        sender.sendMessage("§eExcelMenu 2.0 — " + menuId + " for you (platform "
                + ctx.platform().token() + ", detection " + ctx.detection() + "): "
                + resolved.size() + " entries");
        for (ResolvedEntry r : resolved) {
            StringBuilder sb = new StringBuilder("§a- ").append(r.id())
                    .append(" §7").append(com.winkreyes.excelmenu.core.registry
                            .EntryResolver.stripLegacy(r.label()));
            if (r.stateValue() != null) sb.append(" §8(state ").append(r.stateValue()).append(")");
            if (r.guarded()) sb.append(" §e(guarded: ").append(r.guardFailure()).append(")");
            sender.sendMessage(sb.toString());
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
        menus.openMenu(target, menuId);
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

// ExcelMenu — an Excel plugin by WinkReyesValley. (Excel signature — MikeExcel)
// Excel signature - WRV maintained source; existing authorship notices preserved.
package com.winkreyes.excelmenu.bukkit;

import com.winkreyes.excelmenu.core.registry.EntryResolver;
import com.winkreyes.excelmenu.core.registry.ResolvedEntry;
import com.winkreyes.excelmenu.core.render.LayoutDef;
import com.winkreyes.excelmenu.core.screens.CommandsPanel;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The /menu family, in-house at last (DESIGN-2.0 §1.5 + §3.3):
 *
 *   /menu           opens the Server Menu through MenuService —
 *                   the exact contract ExcelMenuJoin depends on
 *                   (one player command, no args; it funnels its
 *                   join taps through performCommand("menu")).
 *   /servercommands (alias /cmds) renders the commands_menu
 *                   registry like any other menu; with a group
 *                   argument (number or name prefix) it prints
 *                   that group's chat panel, built from the same
 *                   registry rows the viewer resolves.
 *   /serverwebsite  the WebsiteScreen.
 *
 * The player-only rejections are ExcelBedrockMenu's own lines.
 */
final class MenuFamilyCommands implements CommandExecutor {

    private final MenuService menuService;
    private final EntryService entries;
    private final RegistryMenus registryMenus;
    private final PlatformBridge bridge;
    private final WebsiteScreen website;

    MenuFamilyCommands(MenuService menuService, EntryService entries,
                       RegistryMenus registryMenus, PlatformBridge bridge,
                       WebsiteScreen website) {
        this.menuService = menuService;
        this.entries = entries;
        this.registryMenus = registryMenus;
        this.bridge = bridge;
        this.website = website;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label,
                             String[] args) {
        String name = cmd.getName().toLowerCase(Locale.ROOT);
        switch (name) {
            case "servercommands" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("Only players can use /servercommands.");
                    return true;
                }
                if (args.length > 0) {
                    sendCommandsPanel(player, args[0]);
                } else {
                    menuService.openMenu(player, "commands_menu");
                }
                return true;
            }
            case "serverwebsite" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("Only players can use /serverwebsite.");
                    return true;
                }
                website.show(player);
                return true;
            }
            default -> { // "menu"
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("Only players can use /menu.");
                    return true;
                }
                menuService.openMenu(player, "server_menu");
                return true;
            }
        }
    }

    // ---------- the group panel (registry rows, BedrockMenu shape) ----------

    private void sendCommandsPanel(Player player, String arg) {
        LayoutDef layout = registryMenus.layout("commands_menu");
        if (layout == null) {
            player.sendMessage("§eThat isn't available right now.");
            return;
        }
        List<String> groupNames = new ArrayList<>(layout.groups().values());
        int g = CommandsPanel.groupIndexFor(arg, groupNames);
        if (g < 0) {
            player.sendMessage("§cUnknown group '" + arg
                    + "'.§7 Groups: " + String.join(", ", groupNames));
            return;
        }
        List<ResolvedEntry> resolved = entries.entriesFor("commands_menu",
                new BukkitViewerContext(player, bridge));
        Map<String, List<CommandsPanel.Row>> byGroup = new LinkedHashMap<>();
        for (String groupId : layout.groups().keySet()) {
            byGroup.put(groupId, new ArrayList<>());
        }
        for (ResolvedEntry r : resolved) {
            List<CommandsPanel.Row> rows = byGroup.get(r.entry().group());
            if (rows == null) continue;
            String desc = r.lore().isEmpty() ? ""
                    : EntryResolver.stripLegacy(r.lore().get(0));
            rows.add(new CommandsPanel.Row(
                    EntryResolver.stripLegacy(r.label()), desc));
        }
        List<List<CommandsPanel.Row>> rowsByGroup =
                new ArrayList<>(byGroup.values());
        for (String line : CommandsPanel.panelLines(g, groupNames, rowsByGroup)) {
            player.sendMessage(line);
        }
    }
}

// Excel signature - WRV maintained source; existing authorship notices preserved.
package com.winkreyes.excelmenu.bukkit;

import com.winkreyes.excelmenu.core.platform.Detection;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * /rearrange (ExcelMenu 3.0, DESIGN §3.4 door 2): the universal
 * editor entry — Bedrock players route to the forms editor, Java
 * players to the chest editor. Player-only; the §3.7 config
 * kill-switch refuses politely.
 */
final class RearrangeCommand implements CommandExecutor {

    private final EntryService entries;
    private final PlatformBridge bridge;
    private final MenuEditorForms forms;
    private final MenuEditorMenus menus;

    RearrangeCommand(EntryService entries, PlatformBridge bridge,
                     MenuEditorForms forms, MenuEditorMenus menus) {
        this.entries = entries;
        this.bridge = bridge;
        this.forms = forms;
        this.menus = menus;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label,
                             String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§e/rearrange is a player command.");
            return true;
        }
        if (!entries.config().rearrangeServes(EditorSupport.MENU)) {
            player.sendMessage("§eRearranging the menu is turned off right now.");
            return true;
        }
        if (bridge.detect(player) == Detection.State.BEDROCK) {
            forms.openRoot(player);
        } else {
            menus.openRoot(player);
        }
        return true;
    }
}

// ExcelMenu — an Excel plugin by WinkReyesValley. (Excel signature — MikeExcel)

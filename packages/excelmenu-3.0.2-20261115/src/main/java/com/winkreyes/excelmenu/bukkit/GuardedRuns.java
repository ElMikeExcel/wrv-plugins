// ExcelMenu — an Excel plugin by WinkReyesValley. (Excel signature — MikeExcel)
// Excel signature - WRV maintained source; existing authorship notices preserved.
package com.winkreyes.excelmenu.bukkit;

import com.winkreyes.excelmenu.core.screens.AskCore;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * The guarded run, ported from ExcelBedrockMenu 1.5.28's runGuarded:
 * run a command owned by another plugin as the player. Missing
 * command: the "missing" message. Player lacks the command's own
 * permission node: the "denied" message. Otherwise performCommand —
 * the same path the Java menu's [player] click line uses. In 2.0
 * the registry guard (Amendment A) already covers absence for
 * guarded entries; this remains for the screen ports whose owner
 * renders its own form behind the command (Maker's Mark).
 */
final class GuardedRuns {

    private GuardedRuns() {}

    static void runGuarded(Player player, String label, String missing, String denied) {
        org.bukkit.command.PluginCommand cmd = Bukkit.getPluginCommand(label);
        switch (AskCore.guardOutcome(cmd != null,
                cmd == null || cmd.testPermissionSilent(player))) {
            case 0 -> player.sendMessage(missing);
            case 1 -> player.sendMessage(denied);
            default -> player.performCommand(label);
        }
    }

    /** The Maker's Mark door (D6): bare /mark on Bedrock opens the
     *  owner's own two-switch form (Maker's mark, Gear stats). */
    static void runMark(Player player) {
        runGuarded(player, "mark",
                "§eMaker's mark isn't available right now. Ask an adult to check the server.",
                "§eYou can't use Maker's mark right now. Ask an adult to check the server.");
    }
}

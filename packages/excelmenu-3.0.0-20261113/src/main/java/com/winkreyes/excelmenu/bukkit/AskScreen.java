// ExcelMenu — an Excel plugin by WinkReyesValley. (Excel signature — MikeExcel)
// Excel signature - WRV maintained source; existing authorship notices preserved.
package com.winkreyes.excelmenu.bukkit;

import com.winkreyes.excelmenu.core.screens.AskCore;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * The Ask the Guide text box (DESIGN-2.0 §2.4; the D7 screen port).
 * Java's compass button prints the hint; Bedrock gets the same
 * thing as a form: one text box, and Send runs /ask <question> as
 * the player — the exact command Java players type. Reached from
 * the registry form layer via ScreenHooks; the entry's own
 * [message] action remains the Java behaviour and the fallback
 * wording if the box cannot open.
 *
 * Ported from ExcelBedrockMenu 1.5.28's openAskGuide, including its
 * guard: ExcelGuide's /ask has no permission on the command itself
 * (it checks excelguide.ask inside), so the guard checks that node
 * too — no text box for a player the Guide would refuse.
 */
final class AskScreen {

    private final JavaPlugin plugin;
    private final PlatformBridge bridge;

    AskScreen(JavaPlugin plugin, PlatformBridge bridge) {
        this.plugin = plugin;
        this.bridge = bridge;
    }

    /** Open the text box (or speak the guard/hint line). Always
     *  handles the tap: callers must not also run the entry action. */
    void open(Player player) {
        org.bukkit.command.PluginCommand cmd = Bukkit.getPluginCommand("ask");
        int outcome = AskCore.guardOutcome(cmd != null, cmd == null
                || (cmd.testPermissionSilent(player)
                        && player.hasPermission(AskCore.ASK_PERMISSION)));
        if (outcome == 0) {
            player.sendMessage(AskCore.GUIDE_MISSING_MESSAGE);
            return;
        }
        if (outcome == 1) {
            player.sendMessage(AskCore.GUIDE_DENIED_MESSAGE);
            return;
        }
        boolean sent = bridge.sendInputForm(player, AskCore.ASK_TITLE,
                AskCore.ASK_PROMPT, AskCore.ASK_PLACEHOLDER,
                new PlatformBridge.InputHandler() {
                    @Override
                    public void onText(String typed) {
                        if (!player.isOnline()) return;
                        String line = AskCore.askCommandFor(typed);
                        if (line == null) {
                            player.sendMessage(AskCore.ASK_EMPTY);
                        } else {
                            player.performCommand(line);
                        }
                    }

                    @Override
                    public void onClosedOrInvalid() {
                        // Closed = cancelled: nothing is asked.
                    }
                });
        if (!sent) {
            player.sendMessage(AskCore.ASK_HINT);
        }
    }
}

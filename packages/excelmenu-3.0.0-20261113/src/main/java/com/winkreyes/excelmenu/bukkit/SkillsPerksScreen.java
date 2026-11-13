// ExcelMenu — an Excel plugin by WinkReyesValley. (Excel signature — MikeExcel)
// Excel signature - WRV maintained source; existing authorship notices preserved.
package com.winkreyes.excelmenu.bukkit;

import com.winkreyes.excelmenu.core.screens.SkillsPerksCore;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

/**
 * The Skills & Perks form (DESIGN-2.0 §2.4; the D6 screen port),
 * ported from ExcelBedrockMenu 1.5.28's openSkillsPerks: the
 * MENU-SPEC section 2 copy with the live seal count in the Magic
 * line, "My mcMMO stats" first (it runs /mcstats, like the Java
 * Skills & Perks menu's items), then the Skill Trees website
 * follow-on. If the form cannot be sent at all, the same copy goes
 * to chat so the button never dead-ends. Reached from the registry
 * form layer via ScreenHooks; the skills_perks definition menu
 * remains the Java surface and the fallback.
 */
final class SkillsPerksScreen {

    private final JavaPlugin plugin;
    private final PlatformBridge bridge;

    SkillsPerksScreen(JavaPlugin plugin, PlatformBridge bridge) {
        this.plugin = plugin;
        this.bridge = bridge;
    }

    /** Open the form (or speak the copy). Always handles the tap. */
    void open(Player player) {
        boolean sent = bridge.sendSimpleForm(player, SkillsPerksCore.TITLE,
                SkillsPerksCore.skillsPerksContent(GuildGateReads.sealsBrokenCount()),
                List.of(SkillsPerksCore.BUTTONS),
                new PlatformBridge.FormHandler() {
                    @Override
                    public void onSelect(int index) {
                        if (!player.isOnline()) return;
                        if (index == SkillsPerksCore.MCSTATS) {
                            // Same command as the Java Skills & Perks items.
                            Bukkit.getScheduler().runTask(plugin,
                                    () -> player.performCommand("mcstats"));
                        } else if (index == SkillsPerksCore.WEBSITE) {
                            Bukkit.getScheduler().runTask(plugin,
                                    () -> openWebsiteFollowOn(player));
                        }
                        // "Close", or the form was closed: nothing to do
                    }

                    @Override
                    public void onClosedOrInvalid() {
                        // Closed = cancelled: nothing to do.
                    }
                });
        if (!sent) {
            for (String line : SkillsPerksCore
                    .skillsPerksContent(GuildGateReads.sealsBrokenCount())
                    .split("\n")) {
                player.sendMessage(line);
            }
        }
    }

    /** The follow-on form: the website address as plain text. */
    private void openWebsiteFollowOn(Player player) {
        boolean sent = bridge.sendSimpleForm(player, SkillsPerksCore.WEBSITE_FORM_TITLE,
                SkillsPerksCore.skillsPerksWebsiteContent(),
                List.of("Close"),
                new PlatformBridge.FormHandler() {
                    @Override
                    public void onSelect(int index) {
                        // Close: nothing to do
                    }

                    @Override
                    public void onClosedOrInvalid() {
                        // Closed: nothing to do.
                    }
                });
        if (!sent) {
            player.sendMessage(SkillsPerksCore.skillsPerksWebsiteContent());
        }
    }
}

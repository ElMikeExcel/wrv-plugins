// ExcelMenu — an Excel plugin by WinkReyesValley. (Excel signature — MikeExcel)
// Excel signature - WRV maintained source; existing authorship notices preserved.
package com.winkreyes.excelmenu.bukkit;

import com.winkreyes.excelmenu.core.registry.ResolvedEntry;
import org.bukkit.entity.Player;

/**
 * The form-layer screen hooks (DESIGN-2.0 §2.4): three compass
 * doors keep a bespoke Bedrock screen BEHIND their registry entry,
 * exactly where ExcelBedrockMenu had them — the D6/D7 acceptances
 * recorded the definition/[message] dispatch as the interim state
 * "until the Phase 2 screen port"; this is that port. FormService
 * asks here before running a form-tapped entry's actions:
 *
 *   ask                 -> the Guide text box (AskScreen); the
 *                          entry's [message] hint stays the Java
 *                          behaviour and the box's own fallback.
 *   skills              -> the Skills & Perks form (SkillsPerksScreen);
 *                          the skills_perks definition stays the
 *                          Java surface and the form's fallback.
 *   makersmark_settings -> the owner's own /mark form, through the
 *                          guarded run (GuardedRuns); the
 *                          makersmark_settings definition stays the
 *                          Java surface.
 *
 * Java chest taps never come here (the hook lives in the form
 * service), so the Java column is untouched. A hook that fires
 * always handles the tap — each screen speaks its own guard,
 * fallback or screen, and the entry action must not also run.
 */
final class ScreenHooks implements FormService.ScreenHook {

    private final AskScreen ask;
    private final SkillsPerksScreen skillsPerks;

    ScreenHooks(AskScreen ask, SkillsPerksScreen skillsPerks) {
        this.ask = ask;
        this.skillsPerks = skillsPerks;
    }

    @Override
    public boolean tryOpen(Player player, String menuId, ResolvedEntry entry) {
        if (!"server_menu".equals(menuId)) return false;
        switch (entry.id()) {
            case "ask" -> {
                ask.open(player);
                return true;
            }
            case "skills" -> {
                skillsPerks.open(player);
                return true;
            }
            case "makersmark_settings" -> {
                GuardedRuns.runMark(player);
                return true;
            }
            default -> {
                return false;
            }
        }
    }
}

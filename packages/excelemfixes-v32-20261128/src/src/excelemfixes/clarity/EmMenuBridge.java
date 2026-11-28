package excelemfixes.clarity;

import com.magmaguy.elitemobs.menus.SkillBonusMenu;
import org.bukkit.entity.Player;

/**
 * Bridge to EliteMobs' own menu openers. EliteMobs is a
 * compile-time-only dependency (the pack's libs jar), and this
 * class is only loaded when a clarity route actually opens an
 * EliteMobs screen; the caller checks that EliteMobs is present
 * and enabled first and catches linkage failures, so a future
 * EliteMobs that moves these classes degrades to the fallback
 * hint instead of breaking the module — the same isolation as
 * the quest-destinations module's EmQuestData reader.
 *
 * Why this exists: EliteMobs' normal /em menu is a dialog whose
 * Skills page renders levels and XP as plain text — there is
 * nothing to click, and no way from it to the skill-bonus claim
 * screens (verified against the EliteMobs 10.9.14 jar, Nov 24,
 * 2026: the dialog Skills page builder never opens a menu; the
 * claim screens live only in the chest-style menu tree). The
 * public static below is the exact opener EliteMobs' own
 * chest-style status menu uses for its Skills item.
 */
public final class EmMenuBridge {

    private EmMenuBridge() {}

    /** Opens EliteMobs' skill-bonus weapon-select screen; the
     *  player then picks a skill to reach that skill's bonus
     *  screen, where [UNLOCKED] picks are clicked on. */
    public static void openSkillBonusMenu(Player player) {
        SkillBonusMenu.openWeaponSelectMenu(player);
    }
}

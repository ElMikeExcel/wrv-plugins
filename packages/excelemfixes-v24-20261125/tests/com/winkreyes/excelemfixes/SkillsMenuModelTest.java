import excelemfixes.shared.Views;
import excelemfixes.skillsmenu.SkillsMenuModel;
import excelemfixes.skillsmenu.SkillsMenuModel.BonusState;
import excelemfixes.skillsmenu.SkillsMenuModel.SkillState;
import excelemfixes.skillsmenu.SkillsMenuModel.TogglePlan;

import java.util.List;

/**
 * Skills-menu view model: the toggle plan mirrors EliteMobs'
 * own menu rules (locked above level, full set refuses,
 * active switches off), the overview carries every skill
 * with its level and active count plus Close chrome, each
 * bonus screen carries Back, bonus state is readable from
 * the button names alone (the Bedrock form rule — no hover
 * lore there), and bonuses sort the way EliteMobs' screen
 * sorts them (tier, then required level, then name).
 */
public final class SkillsMenuModelTest {

    private static BonusState bonus(String id, String name, int req,
                                    int tier, boolean active) {
        return new BonusState(id, name, List.of("&7Does a thing."),
                "+5% thing", req, tier, active);
    }

    private static List<Views.Button> content(Views.View v) {
        return v.buttons().stream().filter(b -> !b.nav()).toList();
    }

    private static List<Views.Button> nav(Views.View v) {
        return v.buttons().stream().filter(Views.Button::nav).toList();
    }

    public static void run() {
        System.out.println("SkillsMenuModelTest");

        // ------------------------------------------------ toggle plan
        BonusState off = bonus("a", "Alpha", 10, 10, false);
        BonusState on = bonus("a", "Alpha", 10, 10, true);
        Check.eq(TogglePlan.LOCKED,
                SkillsMenuModel.planToggle(9, off, 0, 3),
                "level below requirement -> LOCKED");
        Check.eq(TogglePlan.ACTIVATE,
                SkillsMenuModel.planToggle(10, off, 0, 3),
                "level exactly at requirement -> ACTIVATE");
        Check.eq(TogglePlan.ACTIVATE,
                SkillsMenuModel.planToggle(50, off, 2, 3),
                "one slot free -> ACTIVATE");
        Check.eq(TogglePlan.CAPPED,
                SkillsMenuModel.planToggle(50, off, 3, 3),
                "all slots used -> CAPPED");
        Check.eq(TogglePlan.DEACTIVATE,
                SkillsMenuModel.planToggle(50, on, 3, 3),
                "active bonus switches off even at the cap");
        Check.eq(TogglePlan.LOCKED,
                SkillsMenuModel.planToggle(9, on, 1, 3),
                "locked check runs before the active check (EM order)");

        // ---------------------------------------------------- fixtures
        SkillState armor = new SkillState("ARMOR", "Armor", 30, 123456L, 3,
                List.of(
                        bonus("armor_thorns", "Thorns", 50, 50, false),
                        bonus("armor_evasion", "Evasion", 10, 10, true),
                        bonus("armor_fortify", "Fortify", 25, 25, false),
                        bonus("armor_bulwark", "Bulwark", 25, 25, false)));
        SkillState swords = new SkillState("SWORDS", "Swords", 4, 900L, 3,
                List.of(bonus("sword_edge", "Edge", 10, 10, false)));
        Check.eq(1, armor.activeCount(), "armor active count");
        Check.eq(2, armor.waitingCount(),
                "armor has two unlocked bonuses waiting");
        Check.eq(0, swords.waitingCount(),
                "level 4 swords has nothing unlocked yet");

        // ---------------------------------------------------- overview
        Views.View overview = SkillsMenuModel.overviewView(
                List.of(armor, swords));
        Check.eq("My Skills & Skill Bonuses", overview.title(),
                "overview title");
        Check.eq(2, content(overview).size(),
                "one button per skill");
        Check.ok(nav(overview).stream().anyMatch(
                        b -> b.action().equals("core:close")),
                "overview carries Close (it is the root)");
        Check.ok(nav(overview).stream().noneMatch(
                        b -> b.action().equals("core:back")),
                "overview has no Back");
        Views.Button armorBtn = content(overview).get(0);
        Check.eq("skillsmenu:open:ARMOR", armorBtn.action(),
                "skill button action");
        Check.ok(armorBtn.name().contains("Armor")
                        && armorBtn.name().contains("Lv 30"),
                "overview button names the skill and its level");
        String armorLore = String.join("\n", armorBtn.lore());
        Check.ok(armorLore.contains("123,456 XP"),
                "overview lore carries formatted XP");
        Check.ok(armorLore.contains("1 of 3"),
                "overview lore carries the active count");
        Check.ok(armorLore.contains("2 unlocked"),
                "overview lore flags bonuses waiting");
        Check.ok(overview.intro() != null
                        && overview.intro().contains("2 unlocked"),
                "overview intro counts waiting bonuses");
        String swordsLore = String.join("\n",
                content(overview).get(1).lore());
        Check.ok(swordsLore.contains("level 10"),
                "low-level skill lore points at the first unlock");

        // ----------------------------------------------- per-skill view
        Views.View view = SkillsMenuModel.skillView(armor);
        Check.eq("Armor Bonuses", view.title(), "skill view title");
        Check.ok(nav(view).stream().anyMatch(
                        b -> b.action().equals("core:back")),
                "skill view carries Back");
        Check.ok(nav(view).stream().noneMatch(
                        b -> b.action().equals("core:close")),
                "skill view has no Close (submenu)");
        List<Views.Button> bs = content(view);
        Check.eq(4, bs.size(), "one button per bonus");
        Check.eq(List.of("Evasion", "Bulwark", "Fortify", "Thorns"),
                bs.stream().map(b -> b.name()
                        .replace("&a[ON] ", "").replace("&f", "")
                        .replace("&8", "")
                        .replace(" (level 50)", "")).toList(),
                "bonuses sort by tier, then level, then name");
        Check.ok(bs.get(0).name().startsWith("&a[ON]"),
                "active bonus name carries [ON] (Bedrock-readable)");
        Check.eq("skillsmenu:toggle:ARMOR:armor_evasion",
                bs.get(0).action(), "toggle action");
        Check.ok(String.join("\n", bs.get(0).lore())
                        .contains("switch off"),
                "active bonus lore offers switch-off");
        Check.ok(bs.get(1).name().startsWith("&f"),
                "unlocked bonus name is plain");
        Check.ok(String.join("\n", bs.get(1).lore())
                        .contains("switch on"),
                "unlocked bonus lore offers switch-on");
        Check.ok(bs.get(3).name().contains("(level 50)"),
                "locked bonus name carries its level (Bedrock-readable)");
        Check.ok(String.join("\n", bs.get(3).lore())
                        .contains("Needs Armor level 50"),
                "locked bonus lore names the requirement");
        Check.ok(String.join("\n", bs.get(1).lore())
                        .contains("Does a thing."),
                "bonus description lines reach the lore");
        Check.ok(String.join("\n", bs.get(1).lore())
                        .contains("+5% thing"),
                "EliteMobs' formatted effect reaches the lore");
        Check.ok(view.intro() != null
                        && view.intro().contains("1 of 3"),
                "skill view intro carries the slot count");

        // Empty skill: a placeholder row, still with Back.
        Views.View empty = SkillsMenuModel.skillView(
                new SkillState("HOES", "Hoes", 0, 0L, 3, List.of()));
        Check.eq(1, content(empty).size(),
                "empty skill shows one placeholder row");
        Check.ok(nav(empty).stream().anyMatch(
                        b -> b.action().equals("core:back")),
                "empty skill view still carries Back");

        // ------------------------------------------------ action codec
        Check.eq("ARMOR", SkillsMenuModel.parseOpen(
                "skillsmenu:open:ARMOR"), "open action parses");
        Check.ok(SkillsMenuModel.parseOpen(
                "skillsmenu:toggle:ARMOR:x") == null,
                "toggle action is not an open action");
        String[] t = SkillsMenuModel.parseToggle(
                "skillsmenu:toggle:SWORDS:sword_edge");
        Check.ok(t != null && t[0].equals("SWORDS")
                        && t[1].equals("sword_edge"),
                "toggle action round-trips");
        Check.ok(SkillsMenuModel.parseToggle(
                "clarity:page:skills") == null,
                "foreign action is not a toggle");
        Check.eq("IRON_CHESTPLATE", SkillsMenuModel.iconOf("ARMOR"),
                "armor icon");
        Check.eq("ENCHANTED_BOOK", SkillsMenuModel.iconOf("FUTURE"),
                "unknown skill falls back to a book icon");
    }
}

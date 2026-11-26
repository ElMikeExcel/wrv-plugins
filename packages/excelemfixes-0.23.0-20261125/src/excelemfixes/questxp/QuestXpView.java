package excelemfixes.questxp;

import excelemfixes.shared.Views;

import java.util.ArrayList;
import java.util.List;

/**
 * The quest-xp target popup as a shared Views view model —
 * plain data in, Views out, so the screen's shape is
 * unit-testable on a plain JVM (the pack's standing pattern).
 *
 * This is the 1.1.0 popup (ExcelEMQuestXP's TargetMenu)
 * re-expressed through the pack's shared renderer, per the
 * D244 build brief: 1.1.0's popup was its own code (a Java
 * chest GUI in the Downed holder pattern + a Cumulus
 * SimpleForm through its own BedrockForms reflection
 * bridge), NOT the shared renderer — so the fold ports its
 * CONTENT and ORDER as-is into the shared model, and the
 * renderer's Java chest GUI / Bedrock Cumulus form pair
 * supplies the platform parity. Contents, same order as
 * 1.1.0: "Highest weapon skill (auto)", then one entry per
 * EliteMobs weapon skill showing the player's current level
 * (and XP), the active choice marked, then Close.
 *
 * Actions: questxp:open (re-render), questxp:highest,
 * questxp:set:&lt;ENUM&gt;.
 */
public final class QuestXpView {

    private QuestXpView() {}

    public static final String ACTION_OPEN = "questxp:open";
    public static final String ACTION_HIGHEST = "questxp:highest";
    public static final String ACTION_SET_PREFIX = "questxp:set:";

    /** One weapon skill's row data, in EliteMobs order. */
    public record SkillRow(String enumName, String displayName,
                           int level, long xp) {}

    public static String setAction(String enumName) {
        return ACTION_SET_PREFIX + enumName;
    }

    /** Parses a set action into the skill enum name, or null. */
    public static String parseSet(String action) {
        if (action == null || !action.startsWith(ACTION_SET_PREFIX)) {
            return null;
        }
        String name = action.substring(ACTION_SET_PREFIX.length());
        return name.isBlank() ? null : name;
    }

    public static boolean isOpen(String action) {
        return ACTION_OPEN.equals(action);
    }

    public static boolean isHighest(String action) {
        return ACTION_HIGHEST.equals(action);
    }

    /** Java icon for a skill (the 1.1.0 popup's icon map). */
    static String iconFor(String enumName) {
        return switch (enumName) {
            case "SWORDS" -> "IRON_SWORD";
            case "AXES" -> "IRON_AXE";
            case "BOWS" -> "BOW";
            case "CROSSBOWS" -> "CROSSBOW";
            case "TRIDENTS" -> "TRIDENT";
            case "HOES" -> "IRON_HOE";
            case "MACES" -> "MACE";
            case "SPEARS" -> "SPEAR";
            case "STAVES" -> "STICK";
            case "WANDS" -> "BLAZE_ROD";
            default -> "PAPER";
        };
    }

    /**
     * The target screen. {@code pinnedEnumName} is the
     * player's pinned skill enum name, or null for HIGHEST.
     * Button names carry the state a Bedrock player cannot
     * see in hover lore (level + the current marker), the
     * same content 1.1.0's form labels carried.
     */
    public static Views.View view(String pinnedEnumName,
                                  List<SkillRow> skills) {
        List<Views.Button> buttons = new ArrayList<>();
        boolean highestActive = pinnedEnumName == null;
        List<String> highestLore = new ArrayList<>();
        highestLore.add("&7Quest XP pays your current highest");
        highestLore.add("&7weapon skill. Plus Armor at 1/3, always.");
        if (highestActive) {
            highestLore.add("&a► Currently selected");
        } else {
            highestLore.add("&eClick to choose");
        }
        buttons.add(Views.Button.content(ACTION_HIGHEST,
                (highestActive ? "&a✔ " : "&6&l") + "Highest weapon skill (auto)",
                "COMPASS", highestLore));
        for (SkillRow skill : skills) {
            boolean active = skill.enumName().equals(pinnedEnumName);
            List<String> lore = new ArrayList<>();
            lore.add("&7Level " + skill.level() + " — "
                    + formatXp(skill.xp()) + " XP");
            if (active) {
                lore.add("&a► Currently selected");
            } else {
                lore.add("&eClick to choose");
            }
            buttons.add(Views.Button.content(setAction(skill.enumName()),
                    (active ? "&a✔ " : "&f") + skill.displayName()
                            + " — Lv " + skill.level(),
                    iconFor(skill.enumName()), lore));
        }
        buttons.add(Views.Button.nav("core:close", "&c&lClose", "ARROW"));
        return new Views.View("Quest XP Target", buttons,
                "Where should your quest XP go?\n"
                        + "Armor always gains its 1/3 share on top.");
    }

    /** XP with thousands separators, locale-free (same
     *  formatting as the caster-xp-sharing view). */
    public static String formatXp(long xp) {
        String digits = Long.toString(xp);
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < digits.length(); i++) {
            if (i > 0 && (digits.length() - i) % 3 == 0) out.append(',');
            out.append(digits.charAt(i));
        }
        return out.toString();
    }
}

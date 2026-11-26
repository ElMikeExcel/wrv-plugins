package excelemfixes.skillsmenu;

import excelemfixes.shared.Views;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * The skills-menu view model: plain player skill data in,
 * shared Views out. No Bukkit and no EliteMobs types live
 * here — the bridge (EmSkillsBridge) reads EliteMobs' state
 * into these records and performs the toggles this model
 * plans, so the menu's shape and its toggle rules are
 * unit-testable on a plain JVM, the pack's standing pattern.
 *
 * The screens follow the pack's menu law: the overview is a
 * root (Close chrome), every per-skill screen carries Back,
 * and one view definition renders identically as a Java
 * chest GUI and as a Bedrock form through the shared
 * renderer — the state a Bedrock player cannot see in hover
 * lore is carried in the button names themselves.
 */
public final class SkillsMenuModel {

    private SkillsMenuModel() {}

    // ------------------------------------------------------------ data

    /** One skill bonus as the menu shows it. {@code effect} is
     *  EliteMobs' own formatted effect line for the viewer's
     *  level (may be blank). */
    public record BonusState(String id, String name,
                             List<String> description, String effect,
                             int requiredLevel, int tier,
                             boolean active) {
        public BonusState {
            description = description == null
                    ? List.of() : List.copyOf(description);
            effect = effect == null ? "" : effect;
        }

        /** Unlocked for a player of the given skill level. */
        public boolean unlocked(int playerLevel) {
            return playerLevel >= requiredLevel;
        }
    }

    /** One skill (Armor, Swords, ...) with the viewer's state. */
    public record SkillState(String type, String displayName, int level,
                             long xp, int maxActive,
                             List<BonusState> bonuses) {
        public SkillState {
            bonuses = bonuses == null ? List.of() : List.copyOf(bonuses);
        }

        public int activeCount() {
            int n = 0;
            for (BonusState b : bonuses) if (b.active()) n++;
            return n;
        }

        /** Bonuses the player has unlocked but not switched on. */
        public int waitingCount() {
            int n = 0;
            for (BonusState b : bonuses) {
                if (!b.active() && b.unlocked(level)) n++;
            }
            return n;
        }

        /** Bonuses in menu order: tier, then required level,
         *  then name — the order EliteMobs' own screen uses
         *  (its registry sorts by tier; level breaks ties the
         *  way its tiers ascend). */
        public List<BonusState> sortedBonuses() {
            List<BonusState> out = new ArrayList<>(bonuses);
            out.sort(Comparator.comparingInt(BonusState::tier)
                    .thenComparingInt(BonusState::requiredLevel)
                    .thenComparing(BonusState::name,
                            String.CASE_INSENSITIVE_ORDER));
            return out;
        }
    }

    /**
     * Whether the skill screens carry the "Class XP Sharing"
     * row. Since 0.21.0 the row sits on EVERY skill screen:
     * the sharing setting is global and applies to the two
     * foundation skills of whichever class form the player
     * has selected, so any skill page is a fair door to it.
     * (In 0.20.0 the module covered only the Spellcaster
     * pair and the row sat on the Wands and Staves screens
     * only.) The class-xp-sharing module owns the row's
     * behavior (its handler answers the casterxp:open
     * action) and sets this flag while it is enabled; with
     * the module off or EliteMobs' data unanswered, the flag
     * is down and the skill screens are exactly as before —
     * a row whose handler does not exist is never shown.
     */
    private static volatile boolean casterSharingRowVisible = false;

    public static void setCasterSharingRowVisible(boolean visible) {
        casterSharingRowVisible = visible;
    }

    public static boolean casterSharingRowVisible() {
        return casterSharingRowVisible;
    }

    /** Java-GUI icon per skill type (the renderer falls back
     *  to STONE for a name this Minecraft version lacks). */
    private static final Map<String, String> ICONS = Map.ofEntries(
            Map.entry("ARMOR", "IRON_CHESTPLATE"),
            Map.entry("SWORDS", "IRON_SWORD"),
            Map.entry("AXES", "IRON_AXE"),
            Map.entry("BOWS", "BOW"),
            Map.entry("CROSSBOWS", "CROSSBOW"),
            Map.entry("TRIDENTS", "TRIDENT"),
            Map.entry("HOES", "IRON_HOE"),
            Map.entry("MACES", "MACE"),
            Map.entry("SPEARS", "IRON_SPEAR"),
            Map.entry("STAVES", "BLAZE_ROD"),
            Map.entry("WANDS", "STICK"));

    public static String iconOf(String type) {
        return ICONS.getOrDefault(type, "ENCHANTED_BOOK");
    }

    // --------------------------------------------------------- actions

    public static String openAction(String type) {
        return "skillsmenu:open:" + type;
    }

    public static String toggleAction(String type, String bonusId) {
        return "skillsmenu:toggle:" + type + ":" + bonusId;
    }

    /** Parses a toggle action into {type, bonusId}, or null. */
    public static String[] parseToggle(String action) {
        String[] parts = action.split(":", 4);
        if (parts.length != 4 || !parts[0].equals("skillsmenu")
                || !parts[1].equals("toggle")) {
            return null;
        }
        return new String[]{parts[2], parts[3]};
    }

    /** Parses an open action into the skill type, or null. */
    public static String parseOpen(String action) {
        String[] parts = action.split(":", 3);
        if (parts.length != 3 || !parts[0].equals("skillsmenu")
                || !parts[1].equals("open")) {
            return null;
        }
        return parts[2];
    }

    // ----------------------------------------------------- toggle plan

    public enum TogglePlan { ACTIVATE, DEACTIVATE, LOCKED, CAPPED }

    /**
     * The toggle decision, mirroring EliteMobs' own menu
     * click handler (SkillBonusMenu's events, bytecode-read
     * Nov 24, 2026): a bonus above the player's level refuses;
     * an active bonus switches off; a full set of slots
     * refuses; anything else switches on. The bridge performs
     * the plan through EliteMobs' own selection calls.
     */
    public static TogglePlan planToggle(int playerLevel,
                                        BonusState bonus,
                                        int activeCount,
                                        int maxActive) {
        if (!bonus.unlocked(playerLevel)) return TogglePlan.LOCKED;
        if (bonus.active()) return TogglePlan.DEACTIVATE;
        if (activeCount >= maxActive) return TogglePlan.CAPPED;
        return TogglePlan.ACTIVATE;
    }

    // ------------------------------------------------------------ views

    /** The overview: one button per skill, Close chrome. */
    public static Views.View overviewView(List<SkillState> skills) {
        List<Views.Button> buttons = new ArrayList<>();
        int waitingTotal = 0;
        for (SkillState s : skills) {
            waitingTotal += s.waitingCount();
            List<String> lore = new ArrayList<>();
            lore.add("&7Level &f" + s.level() + " &7— &f"
                    + formatXp(s.xp()) + " XP");
            lore.add("&7Bonuses on: &f" + s.activeCount()
                    + " of " + s.maxActive());
            if (s.waitingCount() > 0) {
                lore.add("&e" + s.waitingCount()
                        + " unlocked bonus"
                        + (s.waitingCount() == 1 ? "" : "es")
                        + " waiting — tap to pick");
            } else if (s.activeCount() == 0 && s.level() < 10) {
                lore.add("&8First bonuses unlock at level 10");
            }
            buttons.add(Views.Button.content(openAction(s.type()),
                    "&f" + s.displayName() + " &7— Lv " + s.level(),
                    iconOf(s.type()), lore));
        }
        buttons.add(Views.Button.nav("core:close", "&fClose", "BARRIER"));
        String intro = waitingTotal > 0
                ? "You have " + waitingTotal + " unlocked skill bonus"
                        + (waitingTotal == 1 ? "" : "es")
                        + " waiting to be switched on. Pick a skill:"
                : "Pick a skill to see its bonuses:";
        return new Views.View("My Skills & Skill Bonuses", buttons, intro);
    }

    /** One skill's bonus screen: one button per bonus, Back. */
    public static Views.View skillView(SkillState skill) {
        List<Views.Button> buttons = new ArrayList<>();
        for (BonusState b : skill.sortedBonuses()) {
            buttons.add(bonusButton(skill, b));
        }
        if (skill.bonuses().isEmpty()) {
            buttons.add(Views.Button.content("core:noop",
                    "&7No bonuses are configured for this skill",
                    "PAPER", List.of()));
        }
        if (casterSharingRowVisible) {
            buttons.add(Views.Button.content("casterxp:open",
                    "&dClass XP Sharing", "EXPERIENCE_BOTTLE",
                    List.of("&7How your class's two foundation",
                            "&7skills share XP — tap to view or change")));
        }
        buttons.add(Views.Button.nav("core:back", "&fBack", "ARROW"));
        String intro = skill.displayName() + " level " + skill.level()
                + " — " + skill.activeCount() + " of " + skill.maxActive()
                + " bonus slots used. Tap a bonus to switch it on or off.";
        return new Views.View(skill.displayName() + " Bonuses",
                buttons, intro);
    }

    private static Views.Button bonusButton(SkillState skill,
                                            BonusState b) {
        List<String> lore = new ArrayList<>();
        for (String line : b.description()) {
            lore.add(line);
        }
        if (!b.effect().isBlank()) {
            lore.add("&7Effect: &f" + b.effect());
        }
        String name;
        if (b.active()) {
            name = "&a[ON] " + b.name();
            lore.add("&aSwitched ON — tap to switch off");
        } else if (b.unlocked(skill.level())) {
            name = "&f" + b.name();
            lore.add("&eUnlocked — tap to switch on");
            lore.add("&7Bonus slots used: " + skill.activeCount()
                    + " of " + skill.maxActive());
        } else {
            name = "&8" + b.name() + " (level " + b.requiredLevel() + ")";
            lore.add("&cNeeds " + skill.displayName() + " level "
                    + b.requiredLevel() + " — you are level "
                    + skill.level());
        }
        return Views.Button.content(
                toggleAction(skill.type(), b.id()), name,
                b.active() ? "LIME_DYE"
                        : b.unlocked(skill.level()) ? "ENCHANTED_BOOK"
                        : "GRAY_DYE",
                lore);
    }

    /** XP with thousands separators, locale-free. */
    static String formatXp(long xp) {
        String digits = Long.toString(xp);
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < digits.length(); i++) {
            if (i > 0 && (digits.length() - i) % 3 == 0) out.append(',');
            out.append(digits.charAt(i));
        }
        return out.toString();
    }
}

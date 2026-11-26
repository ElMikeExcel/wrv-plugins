package excelemfixes.questxp;

import com.magmaguy.elitemobs.playerdata.database.PlayerData;
import com.magmaguy.elitemobs.skills.ArmorSkillHealthBonus;
import com.magmaguy.elitemobs.skills.CombatLevelDisplay;
import com.magmaguy.elitemobs.skills.SkillType;
import com.magmaguy.elitemobs.skills.SkillXPCalculator;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.text.NumberFormat;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * The ONLY quest-xp class that touches EliteMobs types — the
 * port of ExcelEMQuestXP 1.1.0's SkillGrant grant mechanics and
 * its EliteMobs reads, isolated behind plain-Java signatures so
 * the rest of the module (and its unit tests) never link
 * EliteMobs. EliteMobs is a compile-time-only dependency (the
 * pack's libs jar); the module probes before calling in, so a
 * future EliteMobs that moves these classes degrades the
 * module to idle instead of breaking — the same isolation as
 * the caster-xp-sharing module's EmCasterBridge.
 *
 * GRANT MECHANICS (verified against EliteMobs 10.9.14, the
 * live jar file named 10.9.10, during the 1.0.x/1.1.0 builds
 * and carried over verbatim): {@code PlayerData.addSkillXP} is
 * a RAW additive setter (get + add + set; no display refresh,
 * no level-up notify, no EliteSkillXpGainEvent). The grant
 * therefore refreshes the display itself
 * ({@code CombatLevelDisplay} + {@code ArmorSkillHealthBonus},
 * the same two calls EliteMobs' own /em skill set makes) and
 * sends the grant + level-up lines itself.
 *
 * OFFLINE LIMIT (bytecode-proven in the 1.0.1 build):
 * {@code addSkillXP} returns 0 and grants NOTHING when
 * {@code PlayerData.isDataLoaded(uuid)} is false. The backpay
 * command therefore REFUSES to pay an offline player instead
 * of silently granting 0; its counter guard runs after that
 * online check, so it only ever reads a loaded player (whose
 * database row exists — {@code getQuestsCompleted} throws NPE
 * for a player with no row at all).
 */
public final class EmQuestXpBridge {

    private EmQuestXpBridge() {}

    /** EliteMobs version this module was compiled and
     *  rig-proven against (the live jar's internal version;
     *  its file is named 10.9.10). */
    public static final String EM_PIN = "10.9.14";

    private static final String[] PROBE_CLASSES = {
            "com.magmaguy.elitemobs.playerdata.database.PlayerData",
            "com.magmaguy.elitemobs.api.QuestCompleteEvent",
            "com.magmaguy.elitemobs.quests.CustomQuest",
            "com.magmaguy.elitemobs.skills.SkillType",
            "com.magmaguy.elitemobs.skills.SkillXPCalculator",
            "com.magmaguy.elitemobs.skills.CombatLevelDisplay",
            "com.magmaguy.elitemobs.skills.ArmorSkillHealthBonus",
    };

    private static final NumberFormat NUMBERS =
            NumberFormat.getIntegerInstance(Locale.US);

    /** Receives one audit record per grant (the module writes
     *  the console line's data to grants.log through this). */
    public interface Audit {
        void log(Player player, String filename, String questName,
                 String matchedBy, String skillName, long xp, long armorXp,
                 int weaponBefore, int weaponAfter,
                 int armorBefore, int armorAfter, String targetTag,
                 boolean backpay);
    }

    /** Result of one grant: skill paid, amounts, levels
     *  before/after, target tag. */
    public record Outcome(String skillName, long xp, long armorXp,
                          int weaponBefore, int weaponAfter,
                          int armorBefore, int armorAfter,
                          String targetTag) {}

    /**
     * Linkage probe: true when every EliteMobs class the
     * 1.1.0 build pinned to is present. Call only with
     * EliteMobs enabled. (1.1.0 disabled itself when a probe
     * class was missing; as a module this one idles instead —
     * the pack must never go down for one module.)
     */
    public static boolean probe() {
        try {
            for (String name : PROBE_CLASSES) {
                Class.forName(name, false,
                        EmQuestXpBridge.class.getClassLoader());
            }
            SkillXPCalculator.levelFromTotalXP(0);
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    /** Live weapon-skill enum names (EliteMobs order), as a set. */
    public static Set<String> weaponSkillNames() {
        Set<String> names = new HashSet<>();
        for (SkillType skill : SkillType.getWeaponSkills()) {
            names.add(skill.name());
        }
        return names;
    }

    /** Live weapon-skill enum names + display names, EliteMobs
     *  order (TargetPolicy input). */
    public static String[][] weaponSkillTable() {
        SkillType[] weapons = SkillType.getWeaponSkills();
        String[] enumNames = new String[weapons.length];
        String[] displayNames = new String[weapons.length];
        for (int i = 0; i < weapons.length; i++) {
            enumNames[i] = weapons[i].name();
            displayNames[i] = weapons[i].getDisplayName();
        }
        return new String[][]{enumNames, displayNames};
    }

    /** Display name for a weapon-skill enum name, or the enum
     *  name itself when EliteMobs does not know it. */
    public static String displayName(String enumName) {
        try {
            return SkillType.valueOf(enumName).getDisplayName();
        } catch (Throwable t) {
            return enumName;
        }
    }

    /** True when the enum name is a current EliteMobs skill. */
    public static boolean isSkill(String enumName) {
        try {
            SkillType.valueOf(enumName);
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    public static boolean isDataLoaded(UUID uuid) {
        return PlayerData.isDataLoaded(uuid);
    }

    /** The player's live QuestsCompleted counter (backpay's
     *  pre-pay guard source). Throws for a player with no
     *  database row — callers run the online check first. */
    public static int questsCompleted(UUID uuid) {
        return PlayerData.getQuestsCompleted(uuid);
    }

    public static int skillLevel(UUID uuid, String enumName) {
        try {
            return PlayerData.getSkillLevel(uuid, SkillType.valueOf(enumName));
        } catch (Throwable t) {
            return 0;
        }
    }

    public static long skillXp(UUID uuid, String enumName) {
        try {
            return PlayerData.getSkillXP(uuid, SkillType.valueOf(enumName));
        } catch (Throwable t) {
            return 0;
        }
    }

    /**
     * The one shared grant path, used verbatim by the live
     * QuestCompleteEvent listener and by the /questxp backpay
     * command (the 1.1.0 SkillGrant.grant, ported unchanged in
     * behavior): grants {@code xp} weapon XP (+ floor(xp/3)
     * Armor XP) to the player's chosen target — the pinned
     * weapon skill when {@code pinnedEnumName} names one,
     * otherwise their highest weapon skill. Caller must have
     * checked exactly-once guards already (session claim
     * ledger for live grants, persisted BackpayLedger for
     * backpay) and that the player's data is loaded. Never
     * call for xp &lt;= 0.
     *
     * @param backpay true when this grant is a backpay payment:
     *                console + audit lines are tagged BACKPAY
     *                and the player line is prefixed "Backpay: "
     */
    public static Outcome grant(Player player, UUID uuid,
                                String filename, String questName, long xp,
                                String matchedBy, boolean backpay,
                                String pinnedEnumName,
                                Logger logger, Audit audit) {
        long armor = GrantPolicy.armorXp(xp);

        SkillType[] weapons = SkillType.getWeaponSkills();
        long[] weaponXp = new long[weapons.length];
        for (int i = 0; i < weapons.length; i++) {
            weaponXp[i] = PlayerData.getSkillXP(uuid, weapons[i]);
        }
        // D159: resolve the player's chosen target at grant time. A pin
        // wins even over a higher skill; no pin = the pre-1.1.0 rule.
        SkillType pinned = null;
        if (pinnedEnumName != null) {
            try {
                pinned = SkillType.valueOf(pinnedEnumName);
            } catch (IllegalArgumentException stale) {
                pinned = null;
            }
        }
        int pinnedIndex = -1;
        if (pinned != null) {
            for (int i = 0; i < weapons.length; i++) {
                if (weapons[i] == pinned) {
                    pinnedIndex = i;
                    break;
                }
            }
        }
        int best = GrantPolicy.resolveTargetIndex(weaponXp, pinnedIndex);
        SkillType weaponSkill = weapons[best];
        String targetTag = pinned != null && weaponSkill == pinned
                ? "pinned:" + pinned.name() : "highest";

        int weaponLevelBefore = PlayerData.getSkillLevel(uuid, weaponSkill);
        int armorLevelBefore = PlayerData.getSkillLevel(uuid, SkillType.ARMOR);

        long weaponTotal = PlayerData.addSkillXP(uuid, weaponSkill, xp);
        PlayerData.addSkillXP(uuid, SkillType.ARMOR, armor);

        int weaponLevelAfter = SkillXPCalculator.levelFromTotalXP(weaponTotal);
        int armorLevelAfter = PlayerData.getSkillLevel(uuid, SkillType.ARMOR);

        // addSkillXP refreshes nothing — do the display + health refresh
        // EliteMobs' own /em skill set path does, each failure-tolerant.
        try {
            CombatLevelDisplay.updateDisplay(player);
        } catch (Throwable t) {
            logger.warning("Quest XP: CombatLevelDisplay.updateDisplay failed for "
                    + player.getName() + ": " + t);
        }
        try {
            ArmorSkillHealthBonus.updateHealthBonus(player);
        } catch (Throwable t) {
            logger.warning("Quest XP: ArmorSkillHealthBonus.updateHealthBonus failed for "
                    + player.getName() + ": " + t);
        }

        String skillName = weaponSkill.getDisplayName();
        String prefix = backpay ? "Backpay: " : "";
        player.sendMessage(ChatColor.GOLD + prefix + "+" + NUMBERS.format(xp) + " EliteMobs XP ("
                + skillName + ")" + ChatColor.GRAY + "  +" + NUMBERS.format(armor) + " Armor XP");
        if (weaponLevelAfter > weaponLevelBefore) {
            player.sendMessage(ChatColor.GREEN + "Your " + skillName + " skill reached level "
                    + weaponLevelAfter + "!");
        }
        if (armorLevelAfter > armorLevelBefore) {
            player.sendMessage(ChatColor.GREEN + "Your Armor skill reached level "
                    + armorLevelAfter + "!");
        }

        String tag = backpay ? "BACKPAY " : "";
        logger.info("Quest XP " + tag + "grant: " + player.getName() + " — "
                + filename + " (" + questName + ", " + matchedBy + ", target=" + targetTag + ") +"
                + xp + " XP to " + skillName + " (level " + weaponLevelBefore + " -> "
                + weaponLevelAfter + "), +" + armor + " Armor XP (level "
                + armorLevelBefore + " -> " + armorLevelAfter + ")");
        if (audit != null) {
            audit.log(player, filename, questName, matchedBy, skillName,
                    xp, armor, weaponLevelBefore, weaponLevelAfter,
                    armorLevelBefore, armorLevelAfter, targetTag, backpay);
        }
        return new Outcome(skillName, xp, armor,
                weaponLevelBefore, weaponLevelAfter, armorLevelBefore, armorLevelAfter,
                targetTag);
    }
}

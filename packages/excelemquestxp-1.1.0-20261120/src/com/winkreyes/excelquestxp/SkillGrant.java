package com.winkreyes.excelquestxp;

import com.magmaguy.elitemobs.playerdata.database.PlayerData;
import com.magmaguy.elitemobs.skills.ArmorSkillHealthBonus;
import com.magmaguy.elitemobs.skills.CombatLevelDisplay;
import com.magmaguy.elitemobs.skills.SkillType;
import com.magmaguy.elitemobs.skills.SkillXPCalculator;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.text.NumberFormat;
import java.util.Locale;
import java.util.UUID;

/**
 * The one shared grant path, used verbatim by the live
 * {@link QuestCompleteEvent} listener and by the {@code /questxp backpay}
 * command. Any difference between a live grant and a backpay grant would
 * be a defect: same skill target (the player's D159 choice — a pinned
 * weapon skill, or their highest weapon skill when unpinned — + Armor
 * at 1/3), same additive setter, same display + health refresh, same
 * player messages, same audit lines. The resolved target is named in
 * both audit lines ({@code target=highest} / {@code target=pinned:SKILL}).
 *
 * <p>Grant mechanics, verified against EliteMobs 10.9.14 (live file, named
 * 10.9.10): {@code PlayerData.addSkillXP} is a RAW additive setter (get +
 * add + set; no display refresh, no level-up notify, no
 * {@code EliteSkillXpGainEvent}). This method therefore refreshes the
 * display itself ({@code CombatLevelDisplay} +
 * {@code ArmorSkillHealthBonus}, the same two calls {@code /em skill set}
 * makes) and sends the grant + level-up lines itself.
 *
 * <p>OFFLINE LIMIT (bytecode-proven, see BUILD-RECORD.md): {@code addSkillXP}
 * returns 0 and grants NOTHING when {@code PlayerData.isDataLoaded(uuid)}
 * is false — i.e. when the player's data is not in memory, which for a
 * player who is not online is the normal state. The backpay command
 * therefore REFUSES to pay an offline player instead of silently granting
 * 0; the paying lane pays while each player is online. The counter guard
 * runs after that online check, so it only ever reads a loaded player
 * (whose database row exists — {@code getQuestsCompleted} throws NPE for
 * a player with no row at all).
 */
public final class SkillGrant {

    private static final NumberFormat NUMBERS = NumberFormat.getIntegerInstance(Locale.US);

    private SkillGrant() {}

    /** Result of one grant: skill paid, amounts, levels before/after, target tag. */
    public record Outcome(String skillName, long xp, long armorXp,
                          int weaponBefore, int weaponAfter,
                          int armorBefore, int armorAfter, String targetTag) {}

    /**
     * Grants {@code xp} weapon XP (+ floor(xp/3) Armor XP) to {@code player}'s
     * chosen target skill: their pinned weapon skill when one is set
     * (D159), otherwise their highest weapon skill. Caller must have
     * checked exactly-once guards already (session claim ledger for live
     * grants, persisted {@link BackpayLedger} for backpay) and that the
     * player's data is loaded. Never call for xp &lt;= 0.
     *
     * @param backpay true when this grant is a backpay payment: console +
     *                audit lines are tagged BACKPAY and the player line is
     *                prefixed "Backpay: " so it is distinguishable in logs.
     */
    public static Outcome grant(ExcelQuestXP plugin, Player player, UUID uuid,
                                String filename, String questName, long xp,
                                String matchedBy, boolean backpay) {
        long armor = GrantPolicy.armorXp(xp);

        SkillType[] weapons = SkillType.getWeaponSkills();
        long[] weaponXp = new long[weapons.length];
        for (int i = 0; i < weapons.length; i++) {
            weaponXp[i] = PlayerData.getSkillXP(uuid, weapons[i]);
        }
        // D159: resolve the player's chosen target at grant time. A pin
        // wins even over a higher skill; no pin = the pre-1.1.0 rule.
        SkillType pinned = plugin.pinnedSkill(uuid);
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
            plugin.getLogger().warning("Quest XP: CombatLevelDisplay.updateDisplay failed for "
                    + player.getName() + ": " + t);
        }
        try {
            ArmorSkillHealthBonus.updateHealthBonus(player);
        } catch (Throwable t) {
            plugin.getLogger().warning("Quest XP: ArmorSkillHealthBonus.updateHealthBonus failed for "
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
        plugin.getLogger().info("Quest XP " + tag + "grant: " + player.getName() + " — "
                + filename + " (" + questName + ", " + matchedBy + ", target=" + targetTag + ") +"
                + xp + " XP to " + skillName + " (level " + weaponLevelBefore + " -> "
                + weaponLevelAfter + "), +" + armor + " Armor XP (level "
                + armorLevelBefore + " -> " + armorLevelAfter + ")");
        if (backpay) {
            plugin.logBackpayGrant(player, filename, questName, matchedBy, skillName,
                    xp, armor, weaponLevelBefore, weaponLevelAfter,
                    armorLevelBefore, armorLevelAfter, targetTag);
        } else {
            plugin.logGrant(player, filename, questName, matchedBy, skillName,
                    xp, armor, weaponLevelBefore, weaponLevelAfter,
                    armorLevelBefore, armorLevelAfter, targetTag);
        }
        return new Outcome(skillName, xp, armor,
                weaponLevelBefore, weaponLevelAfter, armorLevelBefore, armorLevelAfter,
                targetTag);
    }
}

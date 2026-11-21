package com.winkreyes.excelquestxp;

import com.magmaguy.elitemobs.api.QuestCompleteEvent;
import com.magmaguy.elitemobs.quests.CustomQuest;
import com.magmaguy.elitemobs.quests.Quest;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * Listens for EliteMobs quest completions and pays EliteMobs XP (D136).
 *
 * <p>Grant path, verified against the EliteMobs 10.9.14 jar (live file, named 10.9.10): the public
 * {@link QuestCompleteEvent} carries the completing player and the quest;
 * the quest's config filename comes from {@link CustomQuest} (authored
 * quests are CustomQuests); XP is granted with the internal
 * {@code PlayerData.addSkillXP} — see the build record for the coupling
 * flag. The grant itself lives in {@link SkillGrant}, shared verbatim with
 * the {@code /questxp backpay} command.
 *
 * <p>Party ownership is inherent to the event: EliteMobs fires one
 * QuestCompleteEvent per completing player, and quest ownership is personal
 * (party-shared kill credit never completes a quest for anyone else), so a
 * grant here can only ever land on the event's own player.
 */
public final class QuestXpListener implements Listener {

    private final ExcelQuestXP plugin;
    private final Supplier<QuestXpTable> tableSupplier;
    private final GrantPolicy.ClaimLedger ledger;

    public QuestXpListener(ExcelQuestXP plugin, Supplier<QuestXpTable> tableSupplier,
                           GrantPolicy.ClaimLedger ledger) {
        this.plugin = plugin;
        this.tableSupplier = tableSupplier;
        this.ledger = ledger;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onQuestComplete(QuestCompleteEvent event) {
        Player player = event.getPlayer();
        Quest quest = event.getQuest();
        if (player == null || quest == null) {
            return;
        }
        String filename = null;
        if (quest instanceof CustomQuest customQuest) {
            filename = customQuest.getConfigurationFilename();
        }
        String questName = quest.getQuestName();
        QuestXpTable table = tableSupplier.get();
        QuestXpTable.Result result = table.lookup(filename, questName);
        if (result.xp() <= 0) {
            if (filename != null && table.hasFileKey(filename)) {
                // Configured zero: an excluded/held quest (test_quest, xmas_quest_0).
                plugin.getLogger().fine("Quest XP 0 (configured) for " + player.getName()
                        + " / " + filename + " — no grant.");
            } else {
                plugin.getLogger().fine("Quest XP: no table row for " + player.getName()
                        + " / filename=" + filename + " name=" + questName + " — no grant.");
            }
            return;
        }

        UUID uuid = player.getUniqueId();
        String claimKey = result.matchedBy();
        if (!ledger.tryClaim(uuid, claimKey)) {
            plugin.getLogger().info("Quest XP duplicate suppressed for " + player.getName()
                    + " / " + claimKey + " — already granted this session.");
            return;
        }

        try {
            SkillGrant.grant(plugin, player, uuid, filename, questName,
                    result.xp(), claimKey, false);
        } catch (Throwable t) {
            // A failed grant must never break the quest turn-in itself.
            ledger.release(uuid, claimKey);
            plugin.getLogger().severe("Quest XP grant FAILED for " + player.getName()
                    + " / " + filename + " (" + result.matchedBy() + ", " + result.xp()
                    + " XP): " + t);
        }
    }
}

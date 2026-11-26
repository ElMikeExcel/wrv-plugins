package excelemfixes.questxp;

import com.magmaguy.elitemobs.api.QuestCompleteEvent;
import com.magmaguy.elitemobs.quests.CustomQuest;
import com.magmaguy.elitemobs.quests.Quest;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.UUID;
import java.util.function.Supplier;
import java.util.logging.Logger;

/**
 * Listens for EliteMobs quest completions and pays EliteMobs
 * XP (D136) — the 1.1.0 QuestXpListener, ported unchanged in
 * behavior.
 *
 * Grant path, verified against the EliteMobs 10.9.14 jar
 * (live file, named 10.9.10): the public
 * {@link QuestCompleteEvent} carries the completing player
 * and the quest; the quest's config filename comes from
 * {@link CustomQuest} (authored quests are CustomQuests); XP
 * is granted through {@link EmQuestXpBridge}, the port of
 * the 1.1.0 shared SkillGrant path, shared verbatim with the
 * {@code /questxp backpay} command.
 *
 * Party ownership is inherent to the event: EliteMobs fires
 * one QuestCompleteEvent per completing player, and quest
 * ownership is personal (party-shared kill credit never
 * completes a quest for anyone else), so a grant here can
 * only ever land on the event's own player.
 */
public final class QuestXpListener implements Listener {

    private final QuestXpModule module;
    private final Supplier<QuestXpTable> tableSupplier;
    private final GrantPolicy.ClaimLedger ledger;
    private final Logger logger;

    public QuestXpListener(QuestXpModule module,
                           Supplier<QuestXpTable> tableSupplier,
                           GrantPolicy.ClaimLedger ledger,
                           Logger logger) {
        this.module = module;
        this.tableSupplier = tableSupplier;
        this.ledger = ledger;
        this.logger = logger;
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
                logger.fine("Quest XP 0 (configured) for " + player.getName()
                        + " / " + filename + " — no grant.");
            } else {
                logger.fine("Quest XP: no table row for " + player.getName()
                        + " / filename=" + filename + " name=" + questName + " — no grant.");
            }
            return;
        }

        UUID uuid = player.getUniqueId();
        String claimKey = result.matchedBy();
        if (!ledger.tryClaim(uuid, claimKey)) {
            logger.info("Quest XP duplicate suppressed for " + player.getName()
                    + " / " + claimKey + " — already granted this session.");
            return;
        }

        try {
            module.grantLive(player, uuid, filename, questName,
                    result.xp(), claimKey);
        } catch (Throwable t) {
            // A failed grant must never break the quest turn-in itself.
            ledger.release(uuid, claimKey);
            logger.severe("Quest XP grant FAILED for " + player.getName()
                    + " / " + filename + " (" + result.matchedBy() + ", " + result.xp()
                    + " XP): " + t);
        }
    }
}

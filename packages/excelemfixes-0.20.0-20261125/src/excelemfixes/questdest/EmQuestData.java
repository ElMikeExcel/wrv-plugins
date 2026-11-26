package excelemfixes.questdest;

import com.magmaguy.elitemobs.playerdata.database.PlayerData;
import com.magmaguy.elitemobs.quests.CustomQuest;
import com.magmaguy.elitemobs.quests.Quest;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Reads a player's active EliteMobs quests. This is the only
 * quest-destinations class that touches EM types (the same read
 * path a production tracker reads:
 * PlayerData.getQuests -> CustomQuest.getConfigurationFilename).
 * The module checks EliteMobs is installed before calling, and
 * every read is guarded so an EM hiccup degrades to an empty list
 * instead of an error in a player's face.
 */
public final class EmQuestData {

    private EmQuestData() {}

    /** The player's active custom quests (filename + display name). */
    public static List<QuestDestinations.ActiveQuest> read(UUID uuid,
                                                           Logger log) {
        try {
            List<Quest> quests = PlayerData.getQuests(uuid);
            if (quests == null) return List.of();
            List<QuestDestinations.ActiveQuest> out = new ArrayList<>();
            for (Quest q : quests) {
                if (q instanceof CustomQuest custom) {
                    String file = custom.getConfigurationFilename();
                    if (file != null && !file.isBlank()) {
                        out.add(new QuestDestinations.ActiveQuest(
                                file, custom.getQuestName()));
                    }
                }
            }
            return out;
        } catch (Throwable t) {
            log.warning("ExcelEMFixes quest-destinations: EM quest "
                    + "read failed for " + uuid + ": " + t);
            return List.of();
        }
    }
}

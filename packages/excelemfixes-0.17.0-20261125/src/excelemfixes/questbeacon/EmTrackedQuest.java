package excelemfixes.questbeacon;

import com.magmaguy.elitemobs.quests.CustomQuest;
import com.magmaguy.elitemobs.quests.QuestTracking;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.logging.Logger;

/**
 * Reads which quest a player has TRACKED (selected) in
 * EliteMobs' own tracker, through EM's public QuestTracking API
 * (isTracking / getPlayerTrackingQuests / getQuest — the same
 * state EM's /em quest track command toggles). This is the only
 * quest-beacon class that touches EM types; the module checks
 * EliteMobs is installed before calling, and every read is
 * guarded so an EM hiccup degrades to "no tracked quest" instead
 * of an error in a player's face.
 *
 * The quest FILE (CustomQuest.getConfigurationFilename, the same
 * accessor the quest-destinations reader uses) is the join key
 * into the destination table.
 */
public final class EmTrackedQuest {

    private EmTrackedQuest() {}

    /** A tracked quest: configuration filename + display name. */
    public record Tracked(String file, String name) {}

    /** The player's tracked quest, or null when none is tracked. */
    public static Tracked read(UUID uuid, Logger log) {
        try {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null) return null;
            if (!QuestTracking.isTracking(player)) return null;
            QuestTracking tracking =
                    QuestTracking.getPlayerTrackingQuests().get(uuid);
            if (tracking == null || tracking.getQuest() == null) {
                return null;
            }
            if (tracking.getQuest() instanceof CustomQuest custom) {
                String file = custom.getConfigurationFilename();
                if (file == null || file.isBlank()) return null;
                return new Tracked(file, custom.getQuestName());
            }
            return null;
        } catch (Throwable t) {
            log.warning("ExcelEMFixes quest-beacon: EM tracked-quest "
                    + "read failed for " + uuid + ": " + t);
            return null;
        }
    }
}

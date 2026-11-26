package excelemfixes.api;

import excelemfixes.questdest.QuestDestinations;

/**
 * The quest-destination surface for other modules — the
 * quest-beacon module reads the destinations the
 * quest-destinations module computes through this facade
 * instead of loading the table a second time.
 *
 * Two resolutions live here:
 * - destinationFor(questFile): the hand-written table only
 *   (quest-destinations.yml). Covering a tracker-blank kill
 *   quest stays a data row there, never a code change.
 * - destinationFor(questFile, player): the player-aware
 *   resolution (0.19.0). When the quest-destinations module
 *   registers its dialog provider, a carried quest with an
 *   incomplete DIALOG objective resolves to that objective's
 *   target NPC (derived from EliteMobs' own quest/NPC data —
 *   see DialogDerivation for the precedence rules); anything
 *   else falls back to the table row.
 *
 * Everything returns null / unavailable when ExcelEMFixes'
 * quest-destinations module is off, so a consumer stands down
 * cleanly.
 */
public final class QuestDestinationApi {

    /**
     * The player-aware half of the resolution, supplied by the
     * quest-destinations module (it owns the EliteMobs reads).
     * Returns the FULL resolution for the player's quest — the
     * derived dialog destination when one applies, otherwise
     * the table row, otherwise null.
     */
    public interface DialogDestinationProvider {
        QuestDestinations.Destination resolve(String questFile,
                                              java.util.UUID playerUuid);
    }

    private static volatile QuestDestinations table;
    private static volatile DialogDestinationProvider dialogProvider;

    private QuestDestinationApi() {}

    /** Called by the quest-destinations module as it (re)loads. */
    public static void register(QuestDestinations destinations) {
        table = destinations;
    }

    /** Called by the quest-destinations module as it disables. */
    public static void unregister(QuestDestinations destinations) {
        if (table == destinations) table = null;
    }

    public static boolean isAvailable() {
        return table != null;
    }

    /** The destination row for a quest file, or null. */
    public static QuestDestinations.Destination destinationFor(
            String questFile) {
        QuestDestinations t = table;
        if (t == null || questFile == null) return null;
        return t.byFile(questFile);
    }

    /** Called by the quest-destinations module as it (re)loads. */
    public static void registerDialogProvider(
            DialogDestinationProvider provider) {
        dialogProvider = provider;
    }

    /** Called by the quest-destinations module as it disables. */
    public static void unregisterDialogProvider(
            DialogDestinationProvider provider) {
        if (dialogProvider == provider) dialogProvider = null;
    }

    /**
     * The player-aware destination for a quest file: the
     * current incomplete DIALOG objective's target NPC when the
     * dialog provider derives one, else the table row, else
     * null. Falls back to the plain table lookup when no
     * provider is registered or no player is given.
     */
    public static QuestDestinations.Destination destinationFor(
            String questFile, java.util.UUID playerUuid) {
        DialogDestinationProvider p = dialogProvider;
        if (p == null || questFile == null || playerUuid == null) {
            return destinationFor(questFile);
        }
        return p.resolve(questFile, playerUuid);
    }
}

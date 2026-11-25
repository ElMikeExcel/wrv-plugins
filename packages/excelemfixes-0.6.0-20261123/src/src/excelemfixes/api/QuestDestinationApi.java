package excelemfixes.api;

import excelemfixes.questdest.QuestDestinations;

/**
 * The quest-destination table surface for other modules — the
 * quest-beacon module reads the destinations the
 * quest-destinations module computes (its quest-destinations.yml
 * table) through this facade instead of loading the table a
 * second time.
 *
 * Everything returns null / unavailable when ExcelEMFixes' 
 * quest-destinations module is off, so a consumer stands down
 * cleanly; covering a quest stays a data row in
 * quest-destinations.yml, never a code change.
 */
public final class QuestDestinationApi {

    private static volatile QuestDestinations table;

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
}

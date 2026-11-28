package excelemfixes.tracker;

import excelemfixes.shared.TextUtil;
import org.yaml.snakeyaml.Yaml;

import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The tracker's content catalog, loaded from tracker-catalog.yml
 * (generated from the live EliteMobs configs — see tools/). Every
 * denominator the tracker shows (111 quests, 31 main-chain quests,
 * 14 dungeons) derives from this catalog, never from a hardcoded
 * number, so the menus stay true as content changes.
 */
public final class TrackerCatalog {

    public record QuestInfo(String file, String name) {}

    public record DungeonInfo(String id, String name, String world,
                              List<String> objectives,
                              List<String> bosses,
                              List<String> bossFiles) {
        public DungeonInfo {
            objectives = List.copyOf(objectives);
            bosses = List.copyOf(bosses);
            bossFiles = List.copyOf(bossFiles);
        }
    }

    public record BossInfo(String name, String level, String file) {
        /** Bare name for nameplate matching ("The Shredder"). */
        public String cleanedName() {
            return TextUtil.cleanNameplate(name);
        }
    }

    public record PlaceInfo(String id, String name, String world,
                            List<BossInfo> bosses) {
        public PlaceInfo {
            bosses = List.copyOf(bosses);
        }
    }

    private final Map<String, QuestInfo> questsByFile;
    private final List<String> mainChain;
    private final List<DungeonInfo> dungeons;
    private final List<PlaceInfo> places;
    private final Map<String, PlaceInfo> placesById;
    private final Map<String, Integer> bossNameCounts;

    private TrackerCatalog(Map<String, QuestInfo> questsByFile,
                           List<String> mainChain,
                           List<DungeonInfo> dungeons,
                           List<PlaceInfo> places) {
        this.questsByFile = questsByFile;
        this.mainChain = List.copyOf(mainChain);
        this.dungeons = List.copyOf(dungeons);
        this.places = List.copyOf(places);
        this.placesById = new LinkedHashMap<>();
        this.bossNameCounts = new HashMap<>();
        for (PlaceInfo p : places) {
            placesById.put(p.id(), p);
            for (BossInfo b : p.bosses()) {
                bossNameCounts.merge(b.cleanedName(), 1, Integer::sum);
            }
        }
    }

    public int questTotal() {
        return questsByFile.size();
    }

    /** All quest filenames, in catalog (filename) order. */
    public List<String> questFiles() {
        return List.copyOf(questsByFile.keySet());
    }

    public QuestInfo quest(String file) {
        return questsByFile.get(file);
    }

    public List<String> mainChain() {
        return mainChain;
    }

    public List<DungeonInfo> dungeons() {
        return dungeons;
    }

    public List<PlaceInfo> places() {
        return places;
    }

    public PlaceInfo placeById(String id) {
        return placesById.get(id);
    }

    /** True when a cleaned boss name belongs to exactly one catalog
     *  boss — only then may a name-only kill match be trusted for a
     *  place whose world is unknown. */
    public boolean bossNameUnique(String cleanedName) {
        return bossNameCounts.getOrDefault(cleanedName, 0) == 1;
    }

    @SuppressWarnings("unchecked")
    public static TrackerCatalog load(Reader reader) {
        Object root = new Yaml().load(reader);
        if (!(root instanceof Map)) {
            throw new IllegalArgumentException("tracker-catalog: not a map");
        }
        Map<String, Object> map = (Map<String, Object>) root;

        Map<String, QuestInfo> quests = new LinkedHashMap<>();
        for (Object o : listOf(map.get("quests"))) {
            Map<String, Object> q = (Map<String, Object>) o;
            String file = str(q.get("file"));
            quests.put(file, new QuestInfo(file, str(q.get("name"))));
        }
        List<String> chain = new ArrayList<>();
        for (Object o : listOf(map.get("mainChain"))) chain.add(str(o));

        List<DungeonInfo> dungeons = new ArrayList<>();
        for (Object o : listOf(map.get("dungeons"))) {
            Map<String, Object> d = (Map<String, Object>) o;
            dungeons.add(new DungeonInfo(str(d.get("id")), str(d.get("name")),
                    str(d.get("world")), strList(d.get("objectives")),
                    strList(d.get("bosses")), strList(d.get("bossFiles"))));
        }
        List<PlaceInfo> places = new ArrayList<>();
        for (Object o : listOf(map.get("places"))) {
            Map<String, Object> p = (Map<String, Object>) o;
            List<BossInfo> bosses = new ArrayList<>();
            for (Object bo : listOf(p.get("bosses"))) {
                Map<String, Object> b = (Map<String, Object>) bo;
                bosses.add(new BossInfo(str(b.get("name")),
                        str(b.get("level")), str(b.get("file"))));
            }
            places.add(new PlaceInfo(str(p.get("id")), str(p.get("name")),
                    str(p.get("world")), bosses));
        }
        return new TrackerCatalog(quests, chain, dungeons, places);
    }

    private static List<Object> listOf(Object o) {
        return o instanceof List ? (List<Object>) o : List.of();
    }

    private static List<String> strList(Object o) {
        List<String> out = new ArrayList<>();
        for (Object e : listOf(o)) out.add(str(e));
        return out;
    }

    private static String str(Object o) {
        return o == null ? "" : o.toString();
    }
}

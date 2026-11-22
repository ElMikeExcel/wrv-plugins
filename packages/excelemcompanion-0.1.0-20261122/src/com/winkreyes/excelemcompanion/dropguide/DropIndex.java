package com.winkreyes.excelemcompanion.dropguide;

import org.yaml.snakeyaml.Yaml;

import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The generated drop index: places -> mobs -> full drop lists.
 *
 * The index is produced at build time by tools/generate_index.py from
 * the live EliteMobs configs (see SPEC.md). It never contains drop
 * chances: the generator parses them only to discard them.
 *
 * Loading goes through SnakeYAML directly (Paper always provides it),
 * so the exact same code path runs in the plain-JVM unit tests and on
 * the server.
 */
public final class DropIndex {

    /** One drop entry in a mob's drop list. */
    public record Drop(String name, String material, String kind,
                       List<String> lore, List<String> enchantments,
                       String category, String rank) {
        public Drop {
            lore = List.copyOf(lore);
            enchantments = List.copyOf(enchantments);
        }
    }

    /** One mob/boss (phases merged) and everything it drops. */
    public record Mob(String id, String name, String level, String entity,
                      String icon, boolean standardLoot, List<Drop> drops) {
        public Mob {
            drops = List.copyOf(drops);
        }
    }

    /** One top-level place: a dungeon, arena, adventure region or event. */
    public record Place(String id, String name, String kind, String icon,
                        String parent, List<Mob> mobs) {
        public Place {
            mobs = List.copyOf(mobs);
        }

        public Mob mobById(String mobId) {
            for (Mob m : mobs) {
                if (m.id().equals(mobId)) return m;
            }
            return null;
        }
    }

    private final List<Place> places;
    private final Map<String, Place> byId;

    private DropIndex(List<Place> places) {
        this.places = List.copyOf(places);
        Map<String, Place> map = new LinkedHashMap<>();
        for (Place p : places) map.put(p.id(), p);
        this.byId = Collections.unmodifiableMap(map);
    }

    public List<Place> places() {
        return places;
    }

    public Place placeById(String id) {
        return byId.get(id);
    }

    public int mobCount() {
        int n = 0;
        for (Place p : places) n += p.mobs().size();
        return n;
    }

    public int dropCount() {
        int n = 0;
        for (Place p : places) for (Mob m : p.mobs()) n += m.drops().size();
        return n;
    }

    // ------------------------------------------------------------ loading

    public static DropIndex load(Reader reader) {
        Object root = new Yaml().load(reader);
        if (!(root instanceof Map<?, ?> rootMap)) {
            throw new IllegalArgumentException("drop index: root is not a map");
        }
        List<Place> places = new ArrayList<>();
        for (Object po : listOf(rootMap.get("places"))) {
            if (!(po instanceof Map<?, ?> pm)) continue;
            List<Mob> mobs = new ArrayList<>();
            for (Object mo : listOf(pm.get("mobs"))) {
                if (!(mo instanceof Map<?, ?> mm)) continue;
                List<Drop> drops = new ArrayList<>();
                for (Object dro : listOf(mm.get("drops"))) {
                    if (!(dro instanceof Map<?, ?> dm)) continue;
                    drops.add(new Drop(
                            str(dm.get("name")),
                            str(dm.get("material")),
                            str(dm.get("kind")),
                            strList(dm.get("lore")),
                            strList(dm.get("enchantments")),
                            str(dm.get("category")),
                            str(dm.get("rank"))));
                }
                mobs.add(new Mob(
                        str(mm.get("id")),
                        str(mm.get("name")),
                        str(mm.get("level")),
                        str(mm.get("entity")),
                        str(mm.get("icon")),
                        Boolean.TRUE.equals(mm.get("standardLoot")),
                        drops));
            }
            places.add(new Place(
                    str(pm.get("id")),
                    str(pm.get("name")),
                    str(pm.get("kind")),
                    str(pm.get("icon")),
                    str(pm.get("parent")),
                    mobs));
        }
        return new DropIndex(places);
    }

    private static String str(Object o) {
        return o == null ? "" : String.valueOf(o);
    }

    private static List<?> listOf(Object o) {
        return o instanceof List<?> l ? l : List.of();
    }

    private static List<String> strList(Object o) {
        List<String> out = new ArrayList<>();
        for (Object e : listOf(o)) out.add(str(e));
        return out;
    }
}

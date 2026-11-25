package excelemfixes.jobsdummy;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Pure data model for the jobs-dummy-gate module: the set of
 * EliteMobs boss files whose deaths must pay nothing in Jobs.
 * Parsed from the module's config list so it is unit-testable
 * without a server.
 *
 * <p>Matching is by boss FILENAME (EliteMobs' own identity for
 * the entity), never by entity type: a real husk is not in the
 * list and keeps paying normally. Entries and filenames are
 * normalized the same way — trimmed, lower-cased, and a trailing
 * ".yml" is optional on either side — so an owner can write
 * "training_dummy_lv1" or "training_dummy_lv1.yml".
 */
public final class DummyBossList {

    private final Set<String> ids = new LinkedHashSet<>();

    private DummyBossList() {
    }

    /** Parse the {@code boss-files} list from the module config. */
    public static DummyBossList parse(Object rawList) {
        DummyBossList list = new DummyBossList();
        if (!(rawList instanceof List<?> raw)) return list;
        for (Object entry : raw) {
            if (entry == null) continue;
            String id = normalize(String.valueOf(entry));
            if (!id.isEmpty()) list.ids.add(id);
        }
        return list;
    }

    /** Trim, lower-case, and drop one trailing ".yml". */
    public static String normalize(String bossId) {
        if (bossId == null) return "";
        String s = bossId.trim().toLowerCase(Locale.ROOT);
        if (s.endsWith(".yml")) s = s.substring(0, s.length() - 4);
        return s.trim();
    }

    /** True when this EliteMobs boss filename is on the list. */
    public boolean isVetoed(String bossFilename) {
        if (bossFilename == null) return false;
        String id = normalize(bossFilename);
        return !id.isEmpty() && ids.contains(id);
    }

    /** The normalized boss ids on the list. */
    public Set<String> ids() {
        return Set.copyOf(ids);
    }

    public int size() {
        return ids.size();
    }

    public boolean isEmpty() {
        return ids.isEmpty();
    }
}

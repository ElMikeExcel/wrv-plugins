import com.winkreyes.excelemcompanion.dropguide.DropGuideMenus;
import com.winkreyes.excelemcompanion.dropguide.DropIndex;
import com.winkreyes.excelemcompanion.shared.Views;

import java.io.FileReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Drop-guide navigation in the shared view model: three levels,
 * Back on every submenu, Close on the root, pagination that neither
 * loses nor repeats entries, graceful fallbacks, and a Bedrock
 * detail screen behind every drop button.
 */
public final class DropGuideMenusTest {

    private static List<Views.Button> content(Views.View v) {
        return v.buttons().stream().filter(b -> !b.nav()).toList();
    }

    private static List<Views.Button> nav(Views.View v) {
        return v.buttons().stream().filter(Views.Button::nav).toList();
    }

    public static void run() {
        System.out.println("DropGuideMenusTest");
        DropIndex idx;
        try (FileReader r = new FileReader("src/drops-index.yml",
                StandardCharsets.UTF_8)) {
            idx = DropIndex.load(r);
        } catch (Exception e) {
            Check.ok(false, "index loads (" + e + ")");
            return;
        }
        DropGuideMenus menus = new DropGuideMenus(idx);

        // Places: root — every place listed, Close present, no Back.
        Views.View places = menus.placesView(0, 45);
        Check.eq(idx.places().size(), content(places).size(),
                "places view lists every place");
        Check.ok(nav(places).stream()
                        .anyMatch(b -> b.action().equals("core:close")),
                "places root has a Close button");
        Check.ok(nav(places).stream()
                        .noneMatch(b -> b.action().equals("core:back")),
                "places root has no Back (it is the root)");
        Check.ok(content(places).stream().allMatch(b -> b.action()
                        .startsWith("drops:place:") && b.action().endsWith(":0")),
                "place buttons open their mobs at page 0");

        // Mobs: Back present; Sewers lists its 3 mobs.
        Views.View mobs = menus.mobsView("primis_sewers", 0, 45);
        Check.ok(nav(mobs).stream()
                        .anyMatch(b -> b.action().equals("core:back")),
                "mobs view has a Back button");
        Check.eq(3, content(mobs).size(), "Sewers lists its 3 mobs");

        // Drops: Back present; Shredder specimen incl. quest item.
        DropIndex.Mob shredder = idx.placeById("primis_sewers").mobs()
                .stream().filter(m -> m.name().contains("Shredder"))
                .findFirst().orElseThrow();
        Views.View drops = menus.dropsView("primis_sewers", shredder.id(),
                0, 45);
        Check.ok(nav(drops).stream()
                        .anyMatch(b -> b.action().equals("core:back")),
                "drops view has a Back button");
        Views.Button keys = content(drops).stream()
                .filter(b -> b.name().contains("Sewage Keys"))
                .findFirst().orElse(null);
        Check.ok(keys != null && keys.icon().equals("BONE_MEAL"),
                "Shredder drops view shows Sewage Keys icon");
        Check.ok(keys != null && keys.detail() != null
                        && keys.detail().lines().toString()
                                .contains("Quest item"),
                "Sewage Keys detail marks it a quest item (Bedrock)");
        Check.ok(keys != null && keys.detail() != null
                        && keys.detail().lines().toString()
                                .contains("Dropped by"),
                "drop detail names the dropping mob");
        Check.ok(content(drops).stream().anyMatch(b -> b.name()
                        .contains("EliteMobs Loot") && b.detail() != null),
                "standard-loot footer shows on the last drops page");

        // Every drop button carries a detail screen (Bedrock parity).
        Check.ok(content(drops).stream().allMatch(b -> b.detail() != null),
                "every drop button carries a detail screen");

        // Pagination over the biggest place: no loss, no repeats,
        // Prev/Next appear only where they should.
        DropIndex.Place biggest = idx.places().stream()
                .max((a, b) -> Integer.compare(a.mobs().size(), b.mobs().size()))
                .orElseThrow();
        Check.ok(biggest.mobs().size() > 45,
                "biggest place pages (mobs=" + biggest.mobs().size() + ")");
        Set<String> seen = new HashSet<>();
        boolean overlap = false;
        int pages = DropGuideMenus.pageCount(biggest.mobs().size(), 45);
        for (int pg = 0; pg < pages; pg++) {
            Views.View v = menus.mobsView(biggest.id(), pg, 45);
            for (Views.Button b : content(v)) {
                if (!seen.add(b.action())) overlap = true;
            }
            boolean hasPrev = nav(v).stream().anyMatch(b -> b.name()
                    .contains("Previous"));
            boolean hasNext = nav(v).stream().anyMatch(b -> b.name()
                    .contains("Next"));
            if (pg == 0) {
                Check.ok(!hasPrev && hasNext, "first page: Next only");
            }
            if (pg == pages - 1) {
                Check.ok(hasPrev && !hasNext, "last page: Prev only");
            }
        }
        Check.ok(!overlap, "pagination never repeats a mob");
        Check.eq(biggest.mobs().size(), seen.size(),
                "pagination covers every mob exactly once");

        // Fallbacks land somewhere sane instead of crashing.
        Check.ok(nav(menus.mobsView("no_such_place", 0, 45)).stream()
                        .anyMatch(b -> b.action().equals("core:close")),
                "unknown place falls back to the places view");
        Check.ok(nav(menus.dropsView("primis_sewers", "no_such_mob", 0, 45))
                        .stream().anyMatch(b -> b.action().equals("core:back")),
                "unknown mob falls back to the mobs view");
        Check.ok(!content(menus.placesView(99, 45)).isEmpty(),
                "out-of-range page clamps to a real page");
    }
}

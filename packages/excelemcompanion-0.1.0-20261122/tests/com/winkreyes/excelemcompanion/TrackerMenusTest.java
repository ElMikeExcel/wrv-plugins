import com.winkreyes.excelemcompanion.shared.Views;
import com.winkreyes.excelemcompanion.tracker.ProtectKills;
import com.winkreyes.excelemcompanion.tracker.TrackerCatalog;
import com.winkreyes.excelemcompanion.tracker.TrackerMenus;
import com.winkreyes.excelemcompanion.tracker.TrackerSnapshot;

import java.io.FileReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Tracker menus against the verified Jester fixture (the mirror
 * proof bar): 63 completions / 61 distinct of 111 / main chain
 * 26 of 31, now on main quest 27 / 20 dungeon runs / 1,622 kills /
 * highest elite 25 — plus per-boss counts colored by expertise and
 * the legend present in the bosses and dungeons menus.
 */
public final class TrackerMenusTest {

    private static TrackerSnapshot jesterFixture(TrackerCatalog catalog) {
        Set<String> distinct = new HashSet<>();
        // Main chain 1..26 done (26), plus 35 other quests = 61.
        for (int i = 1; i <= 26; i++) {
            distinct.add("primis_main_quest_" + i + ".yml");
        }
        int added = 0;
        for (String file : catalog.questFiles()) {
            if (added >= 35) break;
            if (!catalog.mainChain().contains(file)) {
                distinct.add(file);
                added++;
            }
        }
        Set<String> lockouts = new HashSet<>();
        lockouts.add("primis_sewer_the_shredder_p1.yml");
        int dungeons = 1;
        for (TrackerCatalog.DungeonInfo d : catalog.dungeons()) {
            if (dungeons >= 8) break;
            if (!d.bossFiles().isEmpty()
                    && !d.id().equals("primis_sewers")) {
                lockouts.add(d.bossFiles().get(0));
                dungeons++;
            }
        }
        return new TrackerSnapshot(true, 63, distinct,
                List.of("primis_main_quest_27.yml"), 20, lockouts,
                1622, 25, 1, 20,
                List.of(new TrackerSnapshot.FormProgress(
                                "spellcaster", 12014, 34, true),
                        new TrackerSnapshot.FormProgress(
                                "adventurer", 17210, 40, false)),
                List.of(new TrackerSnapshot.SkillProgress("WANDS", 129505, 60),
                        new TrackerSnapshot.SkillProgress("ARMOR", 58163, 48)));
    }

    private static Map<String, Integer> sewersKills() {
        // The independent ExcelProtect count (Nov 20+21 records).
        Map<String, Integer> kills = new HashMap<>();
        kills.put(ProtectKills.key("The Shredder", "em_id_primis_sewers"), 2);
        kills.put(ProtectKills.key("Noxious Ninja", "em_id_primis_sewers"), 21);
        kills.put(ProtectKills.key("Spore Spreader", "em_id_primis_sewers"), 34);
        return kills;
    }

    private static String allText(Views.View view) {
        StringBuilder sb = new StringBuilder(view.title());
        for (Views.Button b : view.buttons()) {
            sb.append('\n').append(b.name());
            for (String line : b.lore()) sb.append('\n').append(line);
            if (b.detail() != null) {
                for (String line : b.detail().lines()) {
                    sb.append('\n').append(line);
                }
            }
        }
        return sb.toString();
    }

    public static void run() {
        System.out.println("TrackerMenusTest");
        TrackerCatalog catalog;
        try (FileReader r = new FileReader("src/tracker-catalog.yml",
                StandardCharsets.UTF_8)) {
            catalog = TrackerCatalog.load(r);
        } catch (Exception e) {
            Check.ok(false, "catalog loads (" + e + ")");
            return;
        }
        TrackerMenus menus = new TrackerMenus(catalog);
        TrackerSnapshot snap = jesterFixture(catalog);
        Map<String, Integer> kills = sewersKills();

        // Figure reproduction (the proof bar).
        Check.eq(61, menus.distinctCount(snap), "61 distinct quests");
        Check.eq(26, menus.mainChainDone(snap), "main chain 26 done");
        Check.eq(8, menus.dungeonsDowned(snap), "8 dungeons downed");
        String current = menus.currentMainQuestLine(snap);
        Check.ok(current != null && current.contains("Main quest 27")
                        && current.contains("The Four Guardians"),
                "currently on main quest 27 (The Four Guardians)");

        // Hub carries the headline figures.
        String hub = allText(menus.hubView(snap));
        Check.ok(hub.contains("63"), "hub shows 63 completions");
        Check.ok(hub.contains("61 of 111"), "hub shows 61 of 111 distinct");
        Check.ok(hub.contains("26 of 31"), "hub shows main chain 26 of 31");
        Check.ok(hub.contains("20"), "hub shows 20 dungeon runs");
        Check.ok(hub.contains("1,622"), "hub shows 1,622 kills");
        Check.ok(hub.contains("Level 25"), "hub shows highest elite 25");

        // Quests panel is honest about counter vs distinct.
        String quests = allText(menus.questsView(snap));
        Check.ok(quests.contains("Repeats count"),
                "completions labeled as a repeats-counting total");
        Check.ok(quests.contains("cooldown record"),
                "distinct labeled as from the cooldown record");
        Views.View doneList = menus.questListView(snap, true, 0, 45);
        long doneButtons = doneList.buttons().stream()
                .filter(b -> !b.nav()).count();
        Check.ok(doneButtons >= 45, "completed list pages (61 entries)");
        Views.View donePage2 = menus.questListView(snap, true, 1, 45);
        Check.eq(61, (int) (doneButtons + donePage2.buttons().stream()
                        .filter(b -> !b.nav()).count()),
                "completed list totals exactly 61 across pages");
        Views.View openList = menus.questListView(snap, false, 0, 100);
        Check.eq(50, (int) openList.buttons().stream()
                        .filter(b -> !b.nav()).count(),
                "remaining list totals 50 (111 - 61)");

        // Dungeons: Sewers shows clears ×2 in the Familiar color,
        // the runs counter is labeled, the legend is present.
        String dungeons = allText(menus.dungeonsView(snap, kills));
        Check.ok(dungeons.contains("&f×2"),
                "Sewers clears show ×2 in Familiar white");
        Check.ok(dungeons.contains("Expertise"),
                "dungeons menu carries the expertise legend");
        Check.ok(dungeons.contains("×75+") && dungeons.contains("Mastered"),
                "dungeons legend lists the top tier and its range");
        Check.ok(dungeons.contains("20 total runs")
                        || dungeons.contains("&f20 &7total runs"),
                "dungeon runs labeled as a total");

        // Bosses: per-boss counts colored by expertise; legend here too.
        Views.View sewersBosses = menus.bossPlaceView("primis_sewers",
                snap, kills, 0, 45);
        String bossText = allText(sewersBosses);
        Check.ok(bossText.contains("&f×2"),
                "Shredder shows ×2 kills in Familiar white");
        Check.ok(bossText.contains("&5×21"),
                "Noxious Ninja shows ×21 in Expert purple");
        Check.ok(bossText.contains("&5×34"),
                "Spore Spreader shows ×34 in Expert purple");
        Check.ok(bossText.contains("Expertise Legend"),
                "bosses menu carries the expertise legend");

        // Downed fallback: a locked-out boss with no Protect count
        // shows Downed, never a fabricated number.
        Map<String, Integer> noKills = Map.of();
        Views.View fallbackView = menus.bossPlaceView("primis_sewers",
                snap, noKills, 0, 45);
        Views.Button shredderBtn = fallbackView.buttons().stream()
                .filter(b -> b.name().contains("Shredder"))
                .findFirst().orElseThrow();
        String shredderLore = String.join("\n", shredderBtn.lore());
        Check.ok(shredderLore.contains("Downed ✓"),
                "lockout-only boss shows Downed, not a count");
        Check.ok(!shredderLore.contains("×"),
                "no count is fabricated when records are absent");

        // Counting state: null kills map shows the counting line.
        String counting = allText(menus.dungeonsView(snap, null));
        Check.ok(counting.contains("Counting server records"),
                "menus show a counting state while records aggregate");

        // Combat + classes panels carry their figures.
        String combat = allText(menus.combatView(snap));
        Check.ok(combat.contains("1,622") && combat.contains("Level 25"),
                "combat panel shows kills and highest elite");
        String classes = allText(menus.classesView(snap));
        Check.ok(classes.contains("Spellcaster")
                        && classes.contains("12,014"),
                "classes panel shows the current class and its XP");
        Check.ok(classes.contains("Wands") && classes.contains("129,505"),
                "classes panel shows top skill XP");

        // Unavailable snapshot degrades to one honest panel.
        String unavail = allText(menus.hubView(TrackerSnapshot.unavailable()));
        Check.ok(unavail.contains("unavailable"),
                "unavailable data shows the unavailable panel");

        // Every tracker view carries Back (root hub carries Close).
        List<Views.View> subs = List.of(menus.questsView(snap),
                menus.dungeonsView(snap, kills),
                menus.bossPlacesView(0, 45), sewersBosses,
                menus.combatView(snap), menus.classesView(snap));
        Check.ok(subs.stream().allMatch(v -> v.buttons().stream()
                        .anyMatch(b -> b.nav()
                                && b.action().equals("core:back"))),
                "every tracker submenu has a Back button");
        Check.ok(menus.hubView(snap).buttons().stream()
                        .anyMatch(b -> b.nav()
                                && b.action().equals("core:close")),
                "tracker hub (root) has a Close button");
    }
}

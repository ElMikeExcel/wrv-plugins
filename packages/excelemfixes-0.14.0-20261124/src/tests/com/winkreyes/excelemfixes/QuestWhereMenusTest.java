import excelemfixes.questdest.QuestDestinations;
import excelemfixes.questdest.QuestWhereMenus;
import excelemfixes.shared.Views;

import java.util.List;

/**
 * The /questwhere screens at the model level — the same views the
 * renderer turns into the Java GUI and the Bedrock form: a player
 * on quest 15 sees the Pontis Mille destination, a player with no
 * tracker-blank quests sees the all-clear screen, and every screen
 * carries its nav chrome (Close on the root; the detail screen is
 * renderer chrome with Back).
 */
public class QuestWhereMenusTest {

    static String viewText(Views.View view) {
        StringBuilder sb = new StringBuilder(view.title());
        sb.append('\n').append(view.intro());
        for (Views.Button b : view.buttons()) {
            sb.append('\n').append(b.name());
            for (String line : b.lore()) sb.append('\n').append(line);
        }
        return sb.toString();
    }

    static String detailText(Views.Detail d) {
        StringBuilder sb = new StringBuilder(d.title());
        for (String line : d.lines()) sb.append('\n').append(line);
        return sb.toString();
    }

    public static void run() {
        System.out.println("== QuestWhereMenusTest");
        QuestDestinations table;
        try {
            table = QuestDestinationsTest.load();
        } catch (Exception e) {
            Check.ok(false, "table loads for menus: " + e);
            return;
        }
        QuestWhereMenus menus = new QuestWhereMenus(table);
        QuestDestinations.ActiveQuest q15 = new QuestDestinations.ActiveQuest(
                "primis_main_quest_15.yml", "&2Severance payback!");
        QuestDestinations.ActiveQuest q18 = new QuestDestinations.ActiveQuest(
                "primis_main_quest_18.yml", "&2The trial of ice");
        QuestDestinations.ActiveQuest q19 = new QuestDestinations.ActiveQuest(
                "primis_main_quest_19.yml", "&2Chill out");
        QuestDestinations.ActiveQuest q3 = new QuestDestinations.ActiveQuest(
                "primis_main_quest_3.yml", "&2Some other quest");

        // A player on quest 15 gets exactly the Pontis destination.
        Views.View root = menus.rootView(List.of(q15, q3), true);
        String text = viewText(root);
        Check.ok(text.contains("Severance payback!"),
                "root names quest 15");
        Check.ok(!text.contains("Some other quest"),
                "root omits quests the tracker covers");
        Check.ok(text.contains("Counselor Machiavelli"),
                "root names the target");
        Check.ok(text.contains("512.5, 42, 724.5"),
                "root shows the wormhole coordinates");
        long content = root.buttons().stream().filter(b -> !b.nav()).count();
        Check.eq(1L, content, "one destination button");
        Check.ok(root.buttons().stream().anyMatch(b -> b.nav()
                        && b.action().equals("core:close")),
                "root carries Close (nav law)");
        Views.Button only = root.buttons().stream()
                .filter(b -> !b.nav()).findFirst().orElseThrow();
        Check.eq("questwhere:quest:primis_main_quest_15.yml", only.action(),
                "button routes to the quest 15 screen");

        // The per-quest screen: directions + glow guidance + Back.
        Views.View quest = menus.questView(
                table.byFile("primis_main_quest_15.yml"), true, 90);
        String questText = viewText(quest);
        Check.ok(questText.contains("Guide me there"),
                "quest screen offers glow guidance");
        Check.ok(questText.contains("90 seconds"),
                "quest screen states the glow duration");
        Views.Button guideBtn = quest.buttons().stream()
                .filter(b -> b.action() != null
                        && b.action().startsWith("questwhere:guide:"))
                .findFirst().orElseThrow();
        Check.eq("questwhere:guide:primis_main_quest_15.yml",
                guideBtn.action(), "guide button fires the guide action");
        Views.Button dirBtn = quest.buttons().stream()
                .filter(b -> b.detail() != null)
                .findFirst().orElseThrow();
        Check.ok(detailText(dirBtn.detail()).contains("Advisor wormhole"),
                "directions detail sits behind the directions button");
        Check.ok(quest.buttons().stream().anyMatch(b -> b.nav()
                        && b.action().equals("core:back")),
                "quest screen carries Back (nav law)");
        Views.View questNoGlow = menus.questView(
                table.byFile("primis_main_quest_15.yml"), false, 90);
        Check.ok(!viewText(questNoGlow).contains("Guide me there"),
                "glow off removes the guide button");

        // The detail screen carries the full directions.
        String detail = detailText(menus.detailFor(
                table.byFile("primis_main_quest_15.yml")));
        Check.ok(detail.contains("Pontis Mille"), "detail names the place");
        Check.ok(detail.contains("Advisor wormhole"),
                "detail names the entry");
        Check.ok(detail.contains("512.5, 42, 724.5"),
                "detail repeats the coordinates");
        Check.ok(detail.contains("Stone of Far Speech"),
                "detail covers the fetch objective");
        Check.ok(detail.contains("information only"),
                "detail states the no-teleport rule");

        // All three covered quests at once.
        Views.View all = menus.rootView(List.of(q15, q18, q19), true);
        Check.eq(3L, all.buttons().stream().filter(b -> !b.nav()).count(),
                "three destination buttons");
        String d18 = detailText(menus.detailFor(
                table.byFile("primis_main_quest_18.yml")));
        Check.ok(d18.contains("1209.5, 212, 1399.5")
                        && d18.contains("Violet"),
                "quest 18 detail: peak wormhole + path start");
        String d19 = detailText(menus.detailFor(
                table.byFile("primis_main_quest_19.yml")));
        Check.ok(d19.contains("975.4, 42, 1655.5")
                        && d19.contains("Ice Elemental"),
                "quest 19 detail: arena wormhole + target");

        // No tracker-blank quests: the all-clear screen.
        Views.View clear = menus.rootView(List.of(q3), true);
        String clearText = viewText(clear);
        Check.ok(clearText.contains("all set"), "all-clear headline");
        Check.ok(clearText.contains("Some other quest"),
                "all-clear lists the player's active quests by name");
        Views.View none = menus.rootView(List.of(), true);
        Check.ok(viewText(none).contains("no active quests"),
                "no-quest player told plainly");

        // EliteMobs unavailable: honest panel, no fabricated data.
        Views.View down = menus.rootView(List.of(q15), false);
        Check.ok(viewText(down).contains("unavailable"),
                "EM-down panel says so");
    }
}

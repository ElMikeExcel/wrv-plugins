import excelemfixes.clarity.ClarityMenus;
import excelemfixes.clarity.ClarityText;
import excelemfixes.shared.Views;

import java.io.FileReader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Clarity-menus view model: the text file loads and validates,
 * the five pages build, navigation chrome follows the standing
 * law (Close on the root, Back on every other page), every route
 * points at the verified EliteMobs command, and the wording
 * keeps the canonical terms (D216: "skill bonuses", never
 * "passive skills").
 */
public final class ClarityMenusTest {

    private static List<Views.Button> content(Views.View v) {
        return v.buttons().stream().filter(b -> !b.nav()).toList();
    }

    private static List<Views.Button> nav(Views.View v) {
        return v.buttons().stream().filter(Views.Button::nav).toList();
    }

    private static String allText(ClarityText text) {
        StringBuilder sb = new StringBuilder();
        for (ClarityText.Page p : text.pages().values()) {
            sb.append(p.title()).append('\n').append(p.intro()).append('\n');
            for (ClarityText.ButtonDef b : p.buttons()) {
                sb.append(b.name()).append('\n');
                b.lore().forEach(l -> sb.append(l).append('\n'));
                if (b.detail() != null) {
                    sb.append(b.detail().title()).append('\n');
                    b.detail().lines().forEach(l -> sb.append(l).append('\n'));
                }
            }
        }
        return sb.toString();
    }

    public static void run() {
        System.out.println("ClarityMenusTest");
        ClarityText text;
        try (FileReader r = new FileReader("deploy/clarity-menus.yml",
                StandardCharsets.UTF_8)) {
            text = ClarityText.load(r);
        } catch (Exception e) {
            Check.ok(false, "clarity text loads (" + e + ")");
            return;
        }
        Check.ok(true, "clarity text loads");
        ClarityMenus menus = new ClarityMenus(text);

        // The five pages exist.
        for (String id : List.of("home", "skills", "quests", "shops", "gear")) {
            Check.ok(text.page(id) != null, "page exists: " + id);
            Check.ok(menus.pageView(id) != null, "view builds: " + id);
        }
        Check.ok(menus.pageView("nope") == null, "unknown page id -> null view");

        // Home: four page doors, Close chrome, no Back.
        Views.View home = menus.pageView("home");
        Check.eq(4, content(home).size(), "home has four page doors");
        Check.ok(content(home).stream().allMatch(
                        b -> b.action().startsWith("clarity:page:")),
                "home doors are page actions");
        Check.ok(nav(home).stream().anyMatch(
                        b -> b.action().equals("core:close")),
                "home carries Close");
        Check.ok(nav(home).stream().noneMatch(
                        b -> b.action().equals("core:back")),
                "home has no Back (it is the root)");

        // Every other page: Back chrome, no Close; every button
        // resolves (page target builds, route target exists,
        // detail present for detail buttons).
        for (String id : List.of("skills", "quests", "shops", "gear")) {
            Views.View v = menus.pageView(id);
            Check.ok(nav(v).stream().anyMatch(
                            b -> b.action().equals("core:back")),
                    id + " carries Back");
            Check.ok(nav(v).stream().noneMatch(
                            b -> b.action().equals("core:close")),
                    id + " has no Close (submenu)");
            for (Views.Button b : content(v)) {
                if (b.action().startsWith("clarity:page:")) {
                    String target = b.action().substring("clarity:page:".length());
                    Check.ok(menus.pageView(target) != null,
                            id + " page target resolves: " + target);
                } else if (b.action().startsWith("clarity:route:")) {
                    String target = b.action().substring("clarity:route:".length());
                    Check.ok(text.route(target) != null,
                            id + " route target exists: " + target);
                } else {
                    Check.ok(b.detail() != null && !b.detail().lines().isEmpty(),
                            id + " detail button carries text: " + b.name());
                }
            }
        }

        // Routes run the verified EliteMobs commands (jar-checked
        // against EliteMobs 10.9.x command classes, Nov 24, 2026).
        Check.eq("em", text.route("skills-screen").command(),
                "skills-screen route runs bare /em (status screen)");
        Check.eq("em quest check", text.route("my-quests").command(),
                "my-quests route runs /em quest check");
        Check.eq("em money check", text.route("coin-balance").command(),
                "coin-balance route runs /em money check");
        Check.eq("em shop custom", text.route("shop-set").command(),
                "shop-set route runs /em shop custom");
        Check.eq("em shop dynamic", text.route("shop-rolling").command(),
                "shop-rolling route runs /em shop dynamic");
        Check.eq("em repair", text.route("repair").command(),
                "repair route runs /em repair");
        Check.eq("em enchant", text.route("enchant").command(),
                "enchant route runs /em enchant");
        Check.eq("em scroll", text.route("scroll").command(),
                "scroll route runs /em scroll");
        Check.eq("em unbind", text.route("unbind").command(),
                "unbind route runs /em unbind");
        Check.eq("questwhere", text.route("questwhere").command(),
                "questwhere route runs /questwhere");

        // The specimen fix is in the text: the claiming steps name
        // the click-the-skill-itself step, and the quests page
        // carries the Accepted-line warning.
        String all = allText(text);
        Check.ok(all.contains("skill itself"),
                "skills text names the click-the-skill-itself step");
        Check.ok(all.contains("Accepted!"),
                "quests text carries the Accepted-line warning");
        Check.ok(all.contains("skill bonuses"),
                "canonical term 'skill bonuses' is used");
        Check.ok(!all.contains("passive skills"),
                "retired term 'passive skills' never appears");
        Check.ok(all.contains("soulbound"),
                "canonical term 'soulbound' is used");
        Check.ok(all.contains("Elite Coins"),
                "currency is named 'Elite Coins'");

        // Loader discipline: a button with two kinds is rejected.
        String bad = """
                routes: {}
                pages:
                  home:
                    title: "x"
                    buttons:
                      - name: "bad"
                        icon: "PAPER"
                        page: "home"
                        route: "none"
                """;
        boolean rejected = false;
        try {
            ClarityText.load(new StringReader(bad));
        } catch (IllegalStateException e) {
            rejected = true;
        }
        Check.ok(rejected, "loader rejects a button with two action kinds");

        // Loader discipline: a dangling page target is rejected.
        String dangling = """
                routes: {}
                pages:
                  home:
                    title: "x"
                    buttons:
                      - name: "bad"
                        icon: "PAPER"
                        page: "missing"
                """;
        rejected = false;
        try {
            ClarityText.load(new StringReader(dangling));
        } catch (IllegalStateException e) {
            rejected = true;
        }
        Check.ok(rejected, "loader rejects a dangling page target");
    }
}

package excelemfixes.clarity;

import org.yaml.snakeyaml.Yaml;

import java.io.Reader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The clarity-menus text model, loaded from clarity-menus.yml
 * (bundled in the jar; a copy in the plugin data folder overrides
 * it, the drop-guide pattern). All wording lives in that file —
 * this class only parses and validates its shape:
 * every button carries exactly one of page / detail / route,
 * every route carries exactly one of command / em-menu,
 * every referenced page and route exists, and a "home" page is
 * present. A malformed file fails the load loudly so the module
 * never serves half a menu tree.
 */
public final class ClarityText {

    /** The EliteMobs screens a route may open directly (through
     *  EliteMobs' own menu openers) instead of running a command.
     *  "skill-bonus" is EliteMobs' skill-bonus weapon-select
     *  screen — the claim screens. EliteMobs' normal /em Skills
     *  page only lists levels and XP (it is a text page with
     *  nothing to click), so no command route can land a player
     *  on a working claim screen; this kind exists for that. */
    public static final java.util.Set<String> EM_MENU_TARGETS =
            java.util.Set.of("skill-bonus");

    /** A route: exactly one of command (run as the player) or
     *  emMenu (an EM_MENU_TARGETS key, opened by the module). */
    public record Route(String command, String emMenu, String hint) {}

    public record DetailDef(String title, List<String> lines) {
        public DetailDef {
            lines = lines == null ? List.of() : List.copyOf(lines);
        }
    }

    /** kind is one of "page", "detail", "route"; target is the
     *  page id or route id for those kinds, null for detail. */
    public record ButtonDef(String name, String icon, List<String> lore,
                            String kind, String target, DetailDef detail) {
        public ButtonDef {
            lore = lore == null ? List.of() : List.copyOf(lore);
        }
    }

    public record Page(String id, String title, String intro,
                       List<ButtonDef> buttons) {
        public Page {
            buttons = buttons == null ? List.of() : List.copyOf(buttons);
            intro = intro == null ? "" : intro;
        }
    }

    private final Map<String, Route> routes;
    private final Map<String, Page> pages;

    private ClarityText(Map<String, Route> routes, Map<String, Page> pages) {
        this.routes = routes;
        this.pages = pages;
    }

    public Route route(String id) {
        return routes.get(id);
    }

    public Page page(String id) {
        return pages.get(id);
    }

    public Map<String, Page> pages() {
        return pages;
    }

    public Map<String, Route> routes() {
        return routes;
    }

    // ------------------------------------------------------------ loading

    public static ClarityText load(Reader reader) {
        Object root = new Yaml().load(reader);
        if (!(root instanceof Map<?, ?> rootMap)) {
            throw new IllegalStateException("clarity-menus.yml: root is not a map");
        }
        Map<String, Route> routes = new LinkedHashMap<>();
        Object routesObj = rootMap.get("routes");
        if (routesObj instanceof Map<?, ?> routesMap) {
            for (Map.Entry<?, ?> e : routesMap.entrySet()) {
                String id = String.valueOf(e.getKey());
                if (!(e.getValue() instanceof Map<?, ?> rm)) {
                    throw new IllegalStateException(
                            "clarity-menus.yml: route '" + id + "' is not a map");
                }
                String command = str(rm.get("command"));
                String emMenu = str(rm.get("em-menu"));
                if ((command == null || command.isBlank())
                        == (emMenu == null || emMenu.isBlank())) {
                    throw new IllegalStateException(
                            "clarity-menus.yml: route '" + id
                                    + "' must carry exactly one of"
                                    + " command / em-menu");
                }
                if (emMenu != null && !emMenu.isBlank()
                        && !EM_MENU_TARGETS.contains(emMenu)) {
                    throw new IllegalStateException(
                            "clarity-menus.yml: route '" + id
                                    + "' names unknown em-menu '"
                                    + emMenu + "'");
                }
                routes.put(id, new Route(
                        command == null || command.isBlank() ? null : command,
                        emMenu == null || emMenu.isBlank() ? null : emMenu,
                        str(rm.get("hint"))));
            }
        }
        Map<String, Page> pages = new LinkedHashMap<>();
        Object pagesObj = rootMap.get("pages");
        if (!(pagesObj instanceof Map<?, ?> pagesMap)) {
            throw new IllegalStateException("clarity-menus.yml: no 'pages' map");
        }
        for (Map.Entry<?, ?> e : pagesMap.entrySet()) {
            String id = String.valueOf(e.getKey());
            if (!(e.getValue() instanceof Map<?, ?> pm)) {
                throw new IllegalStateException(
                        "clarity-menus.yml: page '" + id + "' is not a map");
            }
            List<ButtonDef> buttons = new ArrayList<>();
            Object btns = pm.get("buttons");
            if (btns instanceof List<?> list) {
                for (Object o : list) {
                    buttons.add(parseButton(id, o));
                }
            }
            pages.put(id, new Page(id, str(pm.get("title")),
                    str(pm.get("intro")), buttons));
        }
        if (!pages.containsKey("home")) {
            throw new IllegalStateException(
                    "clarity-menus.yml: no 'home' page");
        }
        // Referential integrity: every page/route target resolves.
        for (Page page : pages.values()) {
            for (ButtonDef b : page.buttons()) {
                if (b.kind().equals("page") && !pages.containsKey(b.target())) {
                    throw new IllegalStateException("clarity-menus.yml: page '"
                            + page.id() + "' points at missing page '"
                            + b.target() + "'");
                }
                if (b.kind().equals("route") && !routes.containsKey(b.target())) {
                    throw new IllegalStateException("clarity-menus.yml: page '"
                            + page.id() + "' points at missing route '"
                            + b.target() + "'");
                }
            }
        }
        return new ClarityText(routes, pages);
    }

    private static ButtonDef parseButton(String pageId, Object o) {
        if (!(o instanceof Map<?, ?> bm)) {
            throw new IllegalStateException(
                    "clarity-menus.yml: a button on page '" + pageId + "' is not a map");
        }
        String name = str(bm.get("name"));
        String icon = str(bm.get("icon"));
        List<String> lore = strList(bm.get("lore"));
        String page = str(bm.get("page"));
        String route = str(bm.get("route"));
        Object detailObj = bm.get("detail");
        int kinds = (page != null ? 1 : 0) + (route != null ? 1 : 0)
                + (detailObj != null ? 1 : 0);
        if (name == null || name.isBlank()) {
            throw new IllegalStateException(
                    "clarity-menus.yml: a button on page '" + pageId + "' has no name");
        }
        if (kinds != 1) {
            throw new IllegalStateException("clarity-menus.yml: button '" + name
                    + "' on page '" + pageId + "' must carry exactly one of"
                    + " page / detail / route (found " + kinds + ")");
        }
        if (page != null) {
            return new ButtonDef(name, icon, lore, "page", page, null);
        }
        if (route != null) {
            return new ButtonDef(name, icon, lore, "route", route, null);
        }
        if (!(detailObj instanceof Map<?, ?> dm)) {
            throw new IllegalStateException("clarity-menus.yml: button '" + name
                    + "' detail is not a map");
        }
        String dTitle = str(dm.get("title"));
        DetailDef detail = new DetailDef(dTitle == null ? name : dTitle,
                strList(dm.get("lines")));
        return new ButtonDef(name, icon, lore, "detail", null, detail);
    }

    private static String str(Object o) {
        return o == null ? null : String.valueOf(o);
    }

    private static List<String> strList(Object o) {
        if (!(o instanceof List<?> list)) return List.of();
        List<String> out = new ArrayList<>();
        for (Object e : list) out.add(String.valueOf(e));
        return out;
    }
}

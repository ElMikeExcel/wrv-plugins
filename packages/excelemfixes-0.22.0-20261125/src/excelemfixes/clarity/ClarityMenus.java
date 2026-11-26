package excelemfixes.clarity;

import excelemfixes.shared.Views;

import java.util.ArrayList;
import java.util.List;

/**
 * Turns the clarity-menus text model into shared Views. The
 * navigation chrome is NOT in the text file, on purpose: this
 * builder appends Close to the home page and Back to every other
 * page, so the standing law (every menu and submenu carries a
 * Back button; roots carry Close) holds no matter how the
 * wording file is edited.
 *
 * Button actions use the module's "clarity" prefix:
 *   clarity:page:<page id>    — open another clarity page
 *   clarity:route:<route id>  — run the route's command as the
 *                               player (handled by the module)
 * Detail buttons carry a Views.Detail; the shared renderer
 * turns their clicks into detail screens on both platforms.
 */
public final class ClarityMenus {

    private final ClarityText text;

    public ClarityMenus(ClarityText text) {
        this.text = text;
    }

    public ClarityText text() {
        return text;
    }

    /** The view for a page id, or null when the id is unknown. */
    public Views.View pageView(String pageId) {
        ClarityText.Page page = text.page(pageId);
        if (page == null) return null;
        List<Views.Button> buttons = new ArrayList<>();
        for (ClarityText.ButtonDef def : page.buttons()) {
            switch (def.kind()) {
                case "page" -> buttons.add(Views.Button.content(
                        "clarity:page:" + def.target(), def.name(),
                        def.icon(), def.lore()));
                case "route" -> buttons.add(Views.Button.content(
                        "clarity:route:" + def.target(), def.name(),
                        def.icon(), def.lore()));
                case "detail" -> buttons.add(new Views.Button(
                        "clarity:detail", def.name(), def.icon(), def.lore(),
                        new Views.Detail(def.detail().title(),
                                def.icon(), def.detail().lines()), false));
                default -> { /* loader guarantees the three kinds */ }
            }
        }
        if (page.id().equals("home")) {
            buttons.add(Views.Button.nav("core:close", "&fClose", "BARRIER"));
        } else {
            buttons.add(Views.Button.nav("core:back", "&fBack", "ARROW"));
        }
        return new Views.View(page.title(), buttons, page.intro());
    }
}

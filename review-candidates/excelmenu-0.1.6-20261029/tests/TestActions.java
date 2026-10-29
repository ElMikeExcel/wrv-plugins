// Excel signature - WRV maintained source; existing authorship notices preserved.
import com.winkreyes.excelmenu.core.ActionParser;
import com.winkreyes.excelmenu.core.MenuDef;
import com.winkreyes.excelmenu.core.Selector;

import java.util.List;

/**
 * Click-action parsing and routing: which list a click kind runs.
 * Real dispatch (console/player commands actually firing) is
 * exercised on the rig; here the parse + routing decisions are
 * pinned as pure logic.
 */
public final class TestActions {

    private static ActionParser.Action parse(String line) {
        try {
            return ActionParser.parse(line);
        } catch (ActionParser.ActionParseException e) {
            throw new IllegalStateException(e);
        }
    }

    private static boolean throwsOn(String line) {
        try {
            ActionParser.parse(line);
            return false;
        } catch (ActionParser.ActionParseException e) {
            return true;
        }
    }

    public static void run() {
        ActionParser.Action a = parse("[console] excelcosmetics buy %player_name% color pink");
        Check.eq(ActionParser.Kind.CONSOLE, a.kind(), "console tag routes to CONSOLE");
        Check.eq("excelcosmetics buy %player_name% color pink", a.body(), "console body is exact");
        Check.eq(0, a.delayTicks(), "no delay by default");

        a = parse("[message] &7Done<delay=100>");
        Check.eq(ActionParser.Kind.MESSAGE, a.kind(), "message tag routes to MESSAGE");
        Check.eq("&7Done", a.body(), "delay suffix is stripped from the body");
        Check.eq(100, a.delayTicks(), "delay parsed in ticks");

        a = parse("[openguimenu] spawnshop_cosmetics_colors");
        Check.eq(ActionParser.Kind.OPEN_MENU, a.kind(), "openguimenu routes to OPEN_MENU");
        Check.eq("spawnshop_cosmetics_colors", a.body(), "open target is the body");

        a = parse("[close]");
        Check.eq(ActionParser.Kind.CLOSE, a.kind(), "bare [close] is valid");
        Check.eq("", a.body(), "close body is empty");

        Check.ok(throwsOn("[bogus] x"), "unknown tag throws");
        Check.ok(throwsOn("player shop"), "missing brackets throws");
        Check.ok(throwsOn("[player]"), "command action without argument throws");
        Check.ok(throwsOn("[console"), "unclosed tag throws");

        // Routing against the real definitions.
        List<MenuDef> defs = MenuFixtures.loadShipped();
        MenuDef server = MenuFixtures.byId(defs, "server_menu");
        MenuDef.ItemDef shop = server.item("shop");
        Check.eq(List.of("[player] shop"), Selector.clickLines(shop, "left"),
                "server menu shop: left runs [player] shop");
        Check.eq(List.of("[player] shop"), Selector.clickLines(shop, "right"),
                "server menu shop: right runs [player] shop");
        Check.eq(null, Selector.clickLines(shop, "middle"),
                "server menu shop: middle has no list and no 'any' -> nothing");
        MenuDef.ItemDef pouch = server.item("utility_pouch");
        Check.eq(List.of("[player] pouch", "[close]"), Selector.clickLines(pouch, "left"),
                "utility pouch: 'any' list serves left clicks");
        Check.eq(List.of("[player] pouch", "[close]"), Selector.clickLines(pouch, "right"),
                "utility pouch: 'any' list serves right clicks");

        MenuDef colors = MenuFixtures.byId(defs, "spawnshop_cosmetics_colors");
        MenuDef.ItemDef pink = colors.item("pink");
        Check.eq(List.of("[console] excelcosmetics buy %player_name% color pink"),
                Selector.clickLines(pink, "left"), "pink left = rent");
        Check.eq(List.of("[console] excelcosmetics buy %player_name% color pink own"),
                Selector.clickLines(pink, "right"), "pink right = own");

        // Bedrock modal blocks: every differing left/right item in the
        // cosmetics menus carries one, with labels from its own lore.
        int modals = 0;
        for (String menuId : List.of("spawnshop_cosmetics_colors", "spawnshop_cosmetics_sets")) {
            MenuDef def = MenuFixtures.byId(defs, menuId);
            for (MenuDef.ItemDef item : def.items) {
                if (item.bedrock() != null) {
                    modals++;
                    Check.ok(item.bedrock().leftLabel().contains("$")
                                    && item.bedrock().rightLabel().contains("$"),
                            menuId + ":" + item.id() + " modal labels carry the prices");
                }
            }
        }
        Check.eq(34, modals, "34 cosmetics products carry bedrock modals (21 colors + 13 sets/effects/emotes)");
        Check.eq("Rent 1 week - $200", pink.bedrock().leftLabel(), "pink modal rent label");
        Check.eq("Own forever - $15,000", pink.bedrock().rightLabel(), "pink modal own label");
    }
}

// ExcelMenu — an Excel plugin by WinkReyesValley. (Excel signature — MikeExcel)

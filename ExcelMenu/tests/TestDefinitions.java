import com.winkreyes.excelmenu.core.MenuDef;
import com.winkreyes.excelmenu.core.MenuValidator;

import java.util.List;

/**
 * The shipped set: all 17 definitions (16 migrated + the 0.1.3
 * Safe Blocks shelf) load through the
 * real loader and validate clean against the real Material enum —
 * including the cross-menu check that every [openguimenu] target
 * exists (the check whose absence under DeluxeMenus killed the
 * Java cosmetics store).
 */
public final class TestDefinitions {

    public static void run() {
        List<MenuDef> defs = MenuFixtures.loadShipped();
        Check.eq(17, defs.size(), "17 definitions load");

        int totalItems = 0;
        for (MenuDef d : defs) totalItems += d.items.size();
        Check.eq(156, totalItems, "156 items across the set (151 migrated + Safe Blocks shelf)");

        List<String> errors = MenuValidator.validate(defs, MenuFixtures.materialNames());
        Check.eq(List.of(), errors, "shipped set validates with zero errors: " + errors);

        MenuDef shop = MenuFixtures.byId(defs, "spawnshop");
        Check.eq("shop", shop.command, "spawnshop claims /shop");
        Check.eq("dispenser", shop.type, "spawnshop is a dispenser menu");
        Check.eq(9, shop.size, "spawnshop size 9");
        Check.eq(1, shop.updateInterval, "spawnshop refresh interval 1s");
        Check.eq(9, shop.items.size(), "spawnshop has 9 items (7 categories + balance + back)");

        MenuDef basics = MenuFixtures.byId(defs, "basics_menu");
        Check.eq("basicsmenu", basics.command, "basics_menu claims /basicsmenu");
        Check.eq(1, basics.openRequirements.size(), "basics open requirement present");
        Check.eq("has-permission", basics.openRequirements.get(0).type(),
                "basics open requirement is has-permission");
        Check.eq("deluxemenus.admin", basics.openRequirements.get(0).options().get("permission"),
                "basics open requirement node carried over");
        Check.eq(1, basics.openDeny.size(), "basics deny line carried over");

        MenuDef commands = MenuFixtures.byId(defs, "commands_menu");
        Check.eq(54, commands.size, "commands_menu size 54");
        Check.eq(48, commands.items.size(), "commands_menu 48 items (fix-batch repack)");

        MenuDef server = MenuFixtures.byId(defs, "server_menu");
        Check.eq(27, server.size, "server_menu size 27");
        Check.eq(13, server.items.size(), "server_menu 13 items");

        MenuDef colors = MenuFixtures.byId(defs, "spawnshop_cosmetics_colors");
        Check.eq(16, colors.items.size(), "cosmetics colors 16 items");
        MenuDef.ItemDef pink = colors.item("pink");
        Check.eq(List.of("[console] excelcosmetics buy %player_name% color pink"),
                pink.clicks().get("left"), "pink left click = rent dispatch, byte-exact");
        Check.eq(List.of("[console] excelcosmetics buy %player_name% color pink own"),
                pink.clicks().get("right"), "pink right click = own dispatch, byte-exact");

        for (String t : List.of("teleport_confirm_guild", "teleport_confirm_home",
                "teleport_confirm_spawn", "teleport_confirm_spawncmd")) {
            MenuDef tp = MenuFixtures.byId(defs, t);
            Check.eq(3, tp.items.size(), t + " has yes/info/cancel");
            Check.ok(tp.title.contains("Teleport"), t + " title asks 'Teleport ... ?'");
            Check.ok(tp.item("yes") != null, t + " keeps its 'yes' item id (YAML 1.1 trap)");
        }
    }
}

// ExcelMenu — an Excel plugin by WinkReyesValley. (Excel signature — MikeExcel)

import com.winkreyes.excelmenu.core.MenuDef;
import com.winkreyes.excelmenu.core.Selector;

import java.util.List;
import java.util.Map;

/**
 * Slot selection: the priority + view-requirement pairs from the
 * live server menu (guild locked/open, auto-open on/off) resolve to
 * exactly one item per slot per viewer state — the same map both
 * renderers consume.
 */
public final class TestSelector {

    public static void run() {
        List<MenuDef> defs = MenuFixtures.loadShipped();
        MenuDef server = MenuFixtures.byId(defs, "server_menu");

        MenuFixtures.FakeContext locked = new MenuFixtures.FakeContext();
        locked.placeholders.put("%guildgate_unlocked%", "false");
        locked.placeholders.put("%menujoin_status%", "ON");
        Map<Integer, MenuDef.ItemDef> vis = Selector.visibleItems(server, locked);
        Check.eq("guild_locked", vis.get(12).id(), "sealed guild -> locked item at slot 12");
        Check.eq("autojoin_on", vis.get(16).id(), "menujoin ON -> on item at slot 16");
        Check.eq(11, vis.size(), "server menu renders 11 slots (13 items, 2 pairs)");

        MenuFixtures.FakeContext open = new MenuFixtures.FakeContext();
        open.placeholders.put("%guildgate_unlocked%", "true");
        open.placeholders.put("%menujoin_status%", "OFF");
        vis = Selector.visibleItems(server, open);
        Check.eq("guild_open", vis.get(12).id(), "unlocked guild -> open item at slot 12");
        Check.eq("autojoin_off", vis.get(16).id(), "menujoin OFF -> off item at slot 16");

        MenuDef shop = MenuFixtures.byId(defs, "spawnshop");
        vis = Selector.visibleItems(shop, new MenuFixtures.FakeContext());
        Check.eq(9, vis.size(), "spawnshop renders all 9 slots");
        Check.eq("category_safe_blocks", vis.get(5).id(), "safe blocks category sits at slot 5");
        Check.eq("category_cosmetics", vis.get(6).id(), "cosmetics category sits at slot 6");
        Check.eq("back", vis.get(8).id(), "back sits last at slot 8 (form order: content, info, Back)");

        MenuDef colors = MenuFixtures.byId(defs, "spawnshop_cosmetics_colors");
        vis = Selector.visibleItems(colors, new MenuFixtures.FakeContext());
        Check.eq(16, vis.size(), "cosmetics colors renders all 16 slots");
        Check.eq("to_sets", vis.get(17).id(), "sets cross-link at slot 17");
        Check.eq("back", vis.get(26).id(), "back button at slot 26");
    }
}

// ExcelMenu — an Excel plugin by WinkReyesValley. (Excel signature — MikeExcel)

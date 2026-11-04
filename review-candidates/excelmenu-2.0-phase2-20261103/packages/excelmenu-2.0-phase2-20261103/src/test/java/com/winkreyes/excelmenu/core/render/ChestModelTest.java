package com.winkreyes.excelmenu.core.render;

import com.winkreyes.excelmenu.core.platform.Platform;
import com.winkreyes.excelmenu.core.registry.ResolvedEntry;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * The chest renderer against the PRODUCTION layout + entries: the
 * Java Server Menu must come out slot-for-slot identical to the
 * live 0.1.8 menu (including the deliberate hole at slot 39), and
 * the catalogue pages must carry every row.
 */
public class ChestModelTest {

    private Map<Integer, String> entrySlots(ChestModel.Page page) {
        Map<Integer, String> out = new HashMap<>();
        for (Map.Entry<Integer, ChestModel.Placed> e : page.slots().entrySet()) {
            if (e.getValue() instanceof ChestModel.Placed.EntryItem entry) {
                out.put(e.getKey(), entry.entry().id());
            }
        }
        return out;
    }

    @Test
    public void serverMenuMatchesLiveJavaSlotsExactly() throws Exception {
        LayoutDef layout = RenderFixtures.layout("server_menu");
        List<ResolvedEntry> resolved = RenderFixtures.resolve(
                "server_menu", Platform.JAVA, RenderFixtures.IN_WINDOW);
        ChestModel.Result r = ChestModel.build(layout, resolved, 0);
        assertTrue(String.valueOf(r.error()), r.ok());
        Map<Integer, String> slots = entrySlots(r.page());
        // The expected map is the LIVE layout: the Nov 1, 2026
        // 'me'-group fix (54 slots; 'me' widened to two rows so
        // magiclevel fits), folded into the bundled layout at the
        // Phase 2 baseline — before the fold-in this test pinned the
        // pre-fix bundled layout the live server no longer carries.
        Map<Integer, String> expected = Map.ofEntries(
                Map.entry(1, "spawn"), Map.entry(2, "guild"),
                Map.entry(3, "tpmenu"), Map.entry(4, "party"),
                Map.entry(10, "skills"), Map.entry(11, "magiclevel"),
                Map.entry(12, "spellbook"), Map.entry(13, "jobs"),
                Map.entry(14, "leaderboard"), Map.entry(15, "lostfound"),
                Map.entry(16, "utility_pouch"), Map.entry(17, "trash"),
                Map.entry(18, "boss_rewards"),
                Map.entry(28, "shop"), Map.entry(29, "cosmetics"),
                Map.entry(37, "whatsnew"), Map.entry(38, "ask"),
                Map.entry(39, "commands"), Map.entry(40, "info"),
                Map.entry(41, "website"), Map.entry(42, "halloween"),
                Map.entry(43, "halloween_scares"),
                Map.entry(46, "autosort"), Map.entry(47, "makersmark_settings"),
                Map.entry(49, "autojoin"), Map.entry(50, "wandstyle"),
                Map.entry(51, "silkwarnings"), Map.entry(52, "aimassist"));
        assertEquals(expected, slots);
        // The hole: nothing at 48 (the SCREEN settings slot) or 53
        // (the settings row's spare), exactly like the live menu.
        assertFalse(slots.containsKey(48));
        assertFalse(slots.containsKey(53));
        // Chrome: 5 header panes. (28 entries + 5 chrome: the 27 of
        // the Nov 1, 2026 re-base plus magiclevel at slot 11.)
        assertEquals(33, r.page().slots().size());
        assertEquals("&6&lServer Menu", r.page().title());
        assertEquals(54, r.page().size());
    }

    @Test
    public void serverMenuOutOfWindowDropsHalloweenOnly() throws Exception {
        LayoutDef layout = RenderFixtures.layout("server_menu");
        List<ResolvedEntry> resolved = RenderFixtures.resolve(
                "server_menu", Platform.JAVA, RenderFixtures.OUT_WINDOW);
        ChestModel.Result r = ChestModel.build(layout, resolved, 0);
        assertTrue(r.ok());
        assertFalse(entrySlots(r.page()).containsValue("halloween"));
        assertFalse(entrySlots(r.page()).containsValue("halloween_scares"));
        assertEquals(31, r.page().slots().size());
    }

    @Test
    public void guardedEntryRendersDefaultVariantInItsSlot() throws Exception {
        LayoutDef layout = RenderFixtures.layout("server_menu");
        List<ResolvedEntry> resolved = com.winkreyes.excelmenu.core.registry
                .EntryResolver.resolve("server_menu",
                        RenderFixtures.allEntries().values(),
                        RenderFixtures.viewer(Platform.JAVA,
                                java.util.Set.of("ExcelSilkWatch")),
                        RenderFixtures.IN_WINDOW, RenderFixtures.CONFIG);
        ChestModel.Result r = ChestModel.build(layout, resolved, 0);
        assertTrue(r.ok());
        ChestModel.Placed placed = r.page().slots().get(51);
        assertTrue(placed instanceof ChestModel.Placed.EntryItem);
        ResolvedEntry e = ((ChestModel.Placed.EntryItem) placed).entry();
        assertEquals("silkwarnings", e.id());
        assertTrue(e.guarded());
        assertEquals(List.of("[message] &eThat isn't available right now."),
                e.action());
    }

    @Test
    public void commandsMenuPagesCarryEveryRow() throws Exception {
        LayoutDef layout = RenderFixtures.layout("commands_menu");
        List<ResolvedEntry> resolved = RenderFixtures.resolve(
                "commands_menu", Platform.JAVA, RenderFixtures.IN_WINDOW);
        assertEquals(56, resolved.size());

        ChestModel.Result p1 = ChestModel.build(layout, resolved, 0);
        assertTrue(String.valueOf(p1.error()), p1.ok());
        assertEquals(39, p1.page().slots().size()); // 33 entries + 6 chrome
        Map<Integer, String> s1 = entrySlots(p1.page());
        assertEquals("ga_menu", s1.get(1));
        assertEquals("cl_abandonclaim", s1.get(37));
        // Page turn is chrome with a page target, not an entry.
        ChestModel.Placed nav = p1.page().slots().get(53);
        assertTrue(nav instanceof ChestModel.Placed.FixedItem);
        assertEquals(Integer.valueOf(2),
                ((ChestModel.Placed.FixedItem) nav).fixed().page());

        ChestModel.Result p2 = ChestModel.build(layout, resolved, 1);
        assertTrue(String.valueOf(p2.error()), p2.ok());
        assertEquals(28, p2.page().slots().size()); // 23 entries + 5 chrome
        Map<Integer, String> s2 = entrySlots(p2.page());
        assertEquals("gq_ag", s2.get(1));
        assertEquals("yi_ask", s2.get(40));
        assertEquals("&6&lMore Server Commands", p2.page().title());
    }

    @Test
    public void overflowIsAnErrorNeverASilentDrop() throws Exception {
        LayoutDef tiny = new LayoutDef("tiny", "T", LayoutDef.Form.FLAT,
                null, null, null, null, Map.of(),
                List.of(new LayoutDef.Page(9, null, List.of(),
                        Map.of("getting_around", List.of(0)))));
        List<ResolvedEntry> resolved = RenderFixtures.resolve(
                "commands_menu", Platform.JAVA, RenderFixtures.IN_WINDOW);
        ChestModel.Result r = ChestModel.build(tiny, resolved, 0);
        assertFalse(r.ok());
        assertTrue(r.error(), r.error().contains("getting_around"));
    }

    @Test
    public void missingPageIsAnError() throws Exception {
        LayoutDef layout = RenderFixtures.layout("server_menu");
        ChestModel.Result r = ChestModel.build(layout, List.of(), 5);
        assertFalse(r.ok());
    }
}

// ExcelMenu — an Excel plugin by WinkReyesValley. (Excel signature — MikeExcel)

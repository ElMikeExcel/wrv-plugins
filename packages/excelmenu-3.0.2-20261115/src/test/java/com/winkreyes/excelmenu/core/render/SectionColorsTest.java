// Excel signature - WRV maintained source; existing authorship notices preserved.
package com.winkreyes.excelmenu.core.render;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.winkreyes.excelmenu.core.platform.Platform;
import com.winkreyes.excelmenu.core.registry.RegistryConfig;
import com.winkreyes.excelmenu.core.registry.ResolvedEntry;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

/**
 * ExcelMenu 3.0.1 — Bedrock Server Menu section colours (Mike,
 * Nov 13, 2026, palette approved ~12:38 PM CST): Travel §a green,
 * Me §d light purple, Shops §6 gold, Help §c red; Settings §e
 * yellow and Chest Organization §8 dark gray are the build's picks
 * for the sections Mike did not name (BUILD-RECORD.md, for veto).
 * Blue (§9/§1) is owned by the Spawn + Teleport pins and must
 * never appear on a section button.
 *
 * <p>The palette rides as config data ({@code group-colors} +
 * {@code pin-color} in config.yml), so this suite builds its
 * RegistryConfig from the APPROVED values rather than the old
 * fixture palette — a palette regression fails here first.
 */
public class SectionColorsTest {

    /** The approved 3.0.1 palette, as config data. */
    static final RegistryConfig APPROVED = new RegistryConfig(
            RegistryConfig.SERVER_ZONE,
            "&eThat isn't available right now.",
            Map.of("server_menu", List.of("travel", "me", "shops", "help",
                    "settings", "chest_tools")),
            Map.of("travel", "&a", "me", "&d", "shops", "&6",
                    "help", "&c", "settings", "&e", "chest_tools", "&8"));

    /** The live grouped server_menu shape: GROUPED form, the six
     *  section names, pins [spawn, tpmenu] (as placed live). */
    private static LayoutDef groupedServerMenu() throws Exception {
        LayoutDef flatLayout = RenderFixtures.layout("server_menu");
        Map<String, String> groups = new LinkedHashMap<>();
        groups.put("travel", "Travel");
        groups.put("me", "Me");
        groups.put("shops", "Shops");
        groups.put("help", "Help");
        groups.put("settings", "Settings");
        groups.put("chest_tools", "Chest Organization");
        return new LayoutDef(flatLayout.id(), flatLayout.title(),
                LayoutDef.Form.GROUPED, flatLayout.formTitle(),
                flatLayout.formContent(), "Pick a section:", null,
                groups, flatLayout.pages(), List.of("spawn", "tpmenu"));
    }

    private static List<ResolvedEntry> resolved() throws Exception {
        return RenderFixtures.resolve("server_menu", Platform.BEDROCK,
                RenderFixtures.IN_WINDOW);
    }

    /** No section button may carry blue (§9 or §1). */
    private static void assertNoBlue(String label) {
        assertTrue("button must not carry blue: " + label,
                !label.contains("§9") && !label.contains("§1"));
    }

    @Test
    public void pickerSectionButtonsCarryApprovedColours() throws Exception {
        LayoutDef layout = groupedServerMenu();
        FormModel.FormPage picker = FormModel.picker(layout, resolved(),
                APPROVED, true);
        List<FormModel.Button> b = picker.buttons();
        // Pins first, still dark blue — blue is owned by the pins.
        assertEquals("§1Spawn", b.get(0).label());
        assertEquals("§1Teleport to a player", b.get(1).label());
        // Then one button per section, each its approved colour.
        // chest_tools has no entries in this tree, so it is absent.
        List<String> sections = b.subList(2, b.size() - 1).stream()
                .map(FormModel.Button::label).toList();
        assertEquals(List.of("§aTravel", "§dMe", "§6Shops", "§cHelp",
                "§eSettings"), sections);
        for (String label : sections) assertNoBlue(label);
        // Rearrange chrome stays plain; no Back on the root picker.
        assertEquals("Rearrange Menu", b.get(b.size() - 1).label());
        assertTrue(b.stream().noneMatch(x -> "Back".equals(x.label())));
    }

    @Test
    public void groupFormsCarryTheirSectionColour() throws Exception {
        LayoutDef layout = groupedServerMenu();
        List<ResolvedEntry> res = resolved();
        Map<String, String> expected = Map.of(
                "travel", "§a", "me", "§d", "shops", "§6",
                "help", "§c", "settings", "§e");
        for (Map.Entry<String, String> e : expected.entrySet()) {
            FormModel.FormPage form = FormModel.group(layout, res,
                    e.getKey(), APPROVED);
            List<FormModel.Button> buttons = form.buttons();
            // Every entry button carries the section colour; the
            // trailing Back button stays plain.
            for (FormModel.Button btn : buttons.subList(0, buttons.size() - 1)) {
                assertTrue("entry button must be an entry: " + btn.label(),
                        btn.target() instanceof FormModel.Target.EntryTarget);
                assertTrue("button must carry " + e.getValue() + ": " + btn.label(),
                        btn.label().startsWith(e.getValue()));
                assertNoBlue(btn.label());
            }
            FormModel.Button back = buttons.get(buttons.size() - 1);
            assertEquals("Back", back.label());
            assertTrue(back.target() instanceof FormModel.Target.PickerTarget);
            // The body listing stays plain (only buttons are coloured).
            assertTrue(!form.content().contains("§"));
        }
        // Root-only pins do not also list in the Travel group form.
        FormModel.FormPage travel = FormModel.group(layout, res, "travel", APPROVED);
        assertTrue(travel.buttons().stream().noneMatch(btn ->
                btn.target() instanceof FormModel.Target.EntryTarget t
                        && (t.entryId().equals("spawn") || t.entryId().equals("tpmenu"))));
    }

    @Test
    public void flatFormPinsStayBlueAndEntriesTakeSectionColours() throws Exception {
        // The bundled server_menu layout carries no pins; use the
        // live grouped variant (same layout + pinned list) so the
        // flat pin-first rule is exercised on the compass's pins.
        LayoutDef layout = groupedServerMenu();
        List<ResolvedEntry> res = resolved();
        FormModel.FormPage flat = FormModel.flat(layout, res, APPROVED);
        assertEquals(res.size(), flat.buttons().size());
        // Pins first, still dark blue.
        assertEquals("§1Spawn", flat.buttons().get(0).label());
        assertEquals("§1Teleport to a player", flat.buttons().get(1).label());
        // Every other button carries its own group's approved colour.
        Map<String, String> byId = new LinkedHashMap<>();
        for (ResolvedEntry r : res) byId.put(r.id(), r.entry().group());
        Map<String, String> colour = Map.of("travel", "§a", "me", "§d",
                "shops", "§6", "help", "§c", "settings", "§e");
        for (int i = 2; i < flat.buttons().size(); i++) {
            FormModel.Button btn = flat.buttons().get(i);
            String id = ((FormModel.Target.EntryTarget) btn.target()).entryId();
            String group = byId.get(id);
            assertTrue("flat button must carry its group colour: " + btn.label(),
                    btn.label().startsWith(colour.get(group)));
            assertNoBlue(btn.label());
        }
    }

    @Test
    public void pinColourRidesAsConfigData() throws Exception {
        RegistryConfig blue9 = new RegistryConfig(
                RegistryConfig.SERVER_ZONE,
                "&eThat isn't available right now.",
                Map.of("server_menu", List.of("travel", "me", "shops",
                        "help", "settings")),
                Map.of("travel", "&a", "me", "&d", "shops", "&6",
                        "help", "&c", "settings", "&e"),
                true, List.of("server_menu"), "&9");
        FormModel.FormPage picker = FormModel.picker(groupedServerMenu(),
                resolved(), blue9, false);
        assertEquals("§9Spawn", picker.buttons().get(0).label());
        assertEquals("§9Teleport to a player", picker.buttons().get(1).label());
        // ...while the section buttons still take the group colours.
        assertEquals("§aTravel", picker.buttons().get(2).label());
    }

    @Test
    public void cataloguePickerAndGroupFormsStayPlain() throws Exception {
        // The commands catalogue's groups have no colour configured,
        // so its picker and group forms are unchanged by 3.0.1.
        LayoutDef layout = RenderFixtures.layout("commands_menu");
        List<ResolvedEntry> res = RenderFixtures.resolve(
                "commands_menu", Platform.BEDROCK, RenderFixtures.IN_WINDOW);
        FormModel.FormPage picker = FormModel.picker(layout, res,
                RenderFixtures.CONFIG);
        List<String> labels = picker.buttons().stream()
                .map(FormModel.Button::label).toList();
        assertEquals(List.of("Getting Around", "Money & Jobs", "Skills & Levels",
                "Shops & Trading", "Claims & Land", "Guild & Quests", "Server Info",
                "Fixes & Help", "Your Items & Help"), labels);
        assertTrue(labels.stream().noneMatch(l -> l.contains("§")));
        FormModel.FormPage claims = FormModel.group(layout, res,
                "claims_land", RenderFixtures.CONFIG);
        assertEquals("/claim", claims.buttons().get(0).label());
        assertTrue(claims.buttons().stream()
                .noneMatch(btn -> btn.label().contains("§")));
    }

    @Test
    public void noConfigKeepsThePre301PlainShape() throws Exception {
        // The null-config overloads are the old behaviour, kept for
        // callers and tests that never pass a config.
        LayoutDef layout = groupedServerMenu();
        FormModel.FormPage picker = FormModel.picker(layout, resolved());
        assertTrue(picker.buttons().stream()
                .noneMatch(btn -> btn.label().contains("§")));
        FormModel.FormPage me = FormModel.group(layout, resolved(), "me");
        assertTrue(me.buttons().stream()
                .noneMatch(btn -> btn.label().contains("§")));
    }
}

// ExcelMenu — an Excel plugin by WinkReyesValley. (Excel signature — MikeExcel)

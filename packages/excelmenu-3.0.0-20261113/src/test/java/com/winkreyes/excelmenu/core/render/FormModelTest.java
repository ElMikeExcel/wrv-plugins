package com.winkreyes.excelmenu.core.render;

import com.winkreyes.excelmenu.core.platform.Platform;
import com.winkreyes.excelmenu.core.registry.ResolvedEntry;
import org.junit.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * The form renderer against the PRODUCTION layout + entries: the
 * flat form must reproduce the 1.5.27 compass button sequence
 * exactly (order-bedrock weights), the grouped catalogue must
 * reproduce the catalogue's picker + group shape, and every entry —
 * including [message]-only rows — is a button (the hint speaks on
 * tap, the Java behaviour).
 */
public class FormModelTest {

    @Test
    public void flatCompassFormMatchesBedrockButtonSequence() throws Exception {
        LayoutDef layout = RenderFixtures.layout("server_menu");
        List<ResolvedEntry> resolved = RenderFixtures.resolve(
                "server_menu", Platform.BEDROCK, RenderFixtures.IN_WINDOW);
        FormModel.FormPage form = FormModel.flat(layout, resolved, RenderFixtures.CONFIG);
        assertEquals("Server Menu", form.title());
        assertEquals("What would you like to do?", form.content());
        // 27 buttons since 3.0 retired magiclevel (DESIGN §5 — the
        // door's job moved to the Skills panel's Magic header); the
        // 1.5.27 sequence minus that one door.
        assertEquals(27, form.buttons().size());
        List<String> ids = form.buttons().stream()
                .map(b -> ((FormModel.Target.EntryTarget) b.target()).entryId())
                .toList();
        assertEquals(List.of(
                "spawn", "tpmenu", "party", "utility_pouch", "spellbook", "shop",
                "cosmetics", "guild", "jobs", "skills", "leaderboard",
                "lostfound",
                "trash", "boss_rewards", "info", "commands", "website", "whatsnew",
                "ask", "halloween", "halloween_scares", "wandstyle", "autosort",
                "makersmark_settings",
                "autojoin", "silkwarnings", "aimassist"), ids);
        // Group colour + stripped label (1.5.27's colouring rule).
        assertEquals("§1Spawn", form.buttons().get(0).label());
        assertEquals("§8Aim Assist: ON", form.buttons().get(26).label());
    }

    @Test
    public void flatFormOutOfWindowHas26Buttons() throws Exception {
        LayoutDef layout = RenderFixtures.layout("server_menu");
        List<ResolvedEntry> resolved = RenderFixtures.resolve(
                "server_menu", Platform.BEDROCK, RenderFixtures.OUT_WINDOW);
        FormModel.FormPage form = FormModel.flat(layout, resolved, RenderFixtures.CONFIG);
        assertEquals(25, form.buttons().size());
    }

    @Test
    public void pickerShowsNineGroupsInLayoutOrder() throws Exception {
        LayoutDef layout = RenderFixtures.layout("commands_menu");
        List<ResolvedEntry> resolved = RenderFixtures.resolve(
                "commands_menu", Platform.BEDROCK, RenderFixtures.IN_WINDOW);
        FormModel.FormPage picker = FormModel.picker(layout, resolved);
        assertEquals("Server Commands", picker.title());
        assertEquals("Every command you can use, by group. Pick a group:",
                picker.content());
        List<String> labels = picker.buttons().stream().map(FormModel.Button::label)
                .toList();
        assertEquals(List.of("Getting Around", "Money & Jobs", "Skills & Levels",
                "Shops & Trading", "Claims & Land", "Guild & Quests", "Server Info",
                "Fixes & Help", "Your Items & Help"), labels);
    }

    @Test
    public void groupFormListsRowsAsBodyAndButtons() throws Exception {
        LayoutDef layout = RenderFixtures.layout("commands_menu");
        List<ResolvedEntry> resolved = RenderFixtures.resolve(
                "commands_menu", Platform.BEDROCK, RenderFixtures.IN_WINDOW);
        FormModel.FormPage form = FormModel.group(layout, resolved, "claims_land");
        // 9 entry buttons + the 2.0.1 Back button, always last.
        assertEquals(10, form.buttons().size());
        String[] lines = form.content().split("\n");
        assertEquals("/claim - Claim the land around you", lines[0]);
        assertEquals("/abandonclaim - Delete the claim you stand in (no undo)",
                lines[8]);
        assertEquals("", lines[9]);
        assertTrue(lines[10].startsWith("Buttons run the command"));
        // The typed-argument rows ARE buttons (their [message] hint
        // speaks on tap) — including the Bedrock-only rows.
        List<String> ids = form.buttons().subList(0, form.buttons().size() - 1)
                .stream()
                .map(b -> ((FormModel.Target.EntryTarget) b.target()).entryId())
                .toList();
        assertTrue(ids.contains("cl_childproofmenu"));
        assertTrue(ids.contains("cl_unchildproof_player"));
        assertEquals("/childproofmenu", form.buttons().get(3).label());
        // Back is the trailing button, labelled from the single
        // wording source, and targets the picker — not an entry.
        FormModel.Button back = form.buttons().get(form.buttons().size() - 1);
        assertEquals("Back", back.label());
        assertEquals(FormModel.BACK_LABEL, back.label());
        assertTrue(back.target() instanceof FormModel.Target.PickerTarget);
    }

    @Test
    public void groupFormForJavaOnlyGroupRenders() throws Exception {
        LayoutDef layout = RenderFixtures.layout("commands_menu");
        List<ResolvedEntry> resolved = RenderFixtures.resolve(
                "commands_menu", Platform.BEDROCK, RenderFixtures.IN_WINDOW);
        FormModel.FormPage form = FormModel.group(layout, resolved, "fixes_help");
        // 4 entry buttons + the 2.0.1 Back button, always last.
        assertEquals(5, form.buttons().size());
        assertTrue(form.content().startsWith("/chunks - Fix dropped items"));
        FormModel.Button back = form.buttons().get(form.buttons().size() - 1);
        assertEquals("Back", back.label());
        assertTrue(back.target() instanceof FormModel.Target.PickerTarget);
    }

    @Test
    public void groupedServerMenuGroupFormEndsWithBackAndPickerHasNone()
            throws Exception {
        // The server_menu layout bundled in the jar is FLAT; the
        // grouped shape it carries live (menu-grouped-20261112) is
        // the same LayoutDef with form=GROUPED and a groups map.
        // Build that grouped variant from the production layout so
        // the 2.0.1 Back rule is proven on the compass's own groups.
        LayoutDef flatLayout = RenderFixtures.layout("server_menu");
        Map<String, String> groups = new LinkedHashMap<>();
        groups.put("travel", "Travel");
        groups.put("me", "Me");
        groups.put("shops", "Shops");
        groups.put("help", "Help");
        groups.put("settings", "Settings");
        LayoutDef grouped = new LayoutDef(flatLayout.id(), flatLayout.title(),
                LayoutDef.Form.GROUPED, flatLayout.formTitle(),
                flatLayout.formContent(), "Pick a section:", null,
                groups, flatLayout.pages());
        List<ResolvedEntry> resolved = RenderFixtures.resolve(
                "server_menu", Platform.BEDROCK, RenderFixtures.IN_WINDOW);

        // The picker IS the root: no Back button anywhere on it,
        // every button a GroupTarget.
        FormModel.FormPage picker = FormModel.picker(grouped, resolved);
        assertTrue(picker.buttons().size() > 0);
        assertTrue(picker.buttons().stream()
                .allMatch(b -> b.target() instanceof FormModel.Target.GroupTarget));
        assertTrue(picker.buttons().stream()
                .noneMatch(b -> "Back".equals(b.label())));

        // A server_menu section form ('me') ends with Back ->
        // PickerTarget; every earlier button is an entry button.
        FormModel.FormPage me = FormModel.group(grouped, resolved, "me");
        long meEntries = resolved.stream()
                .filter(r -> r.entry().group().equals("me")).count();
        assertEquals(meEntries + 1, me.buttons().size());
        assertTrue(me.buttons().subList(0, me.buttons().size() - 1).stream()
                .allMatch(b -> b.target() instanceof FormModel.Target.EntryTarget));
        FormModel.Button back = me.buttons().get(me.buttons().size() - 1);
        assertEquals("Back", back.label());
        assertTrue(back.target() instanceof FormModel.Target.PickerTarget);
    }
}

// ExcelMenu — an Excel plugin by WinkReyesValley. (Excel signature — MikeExcel)

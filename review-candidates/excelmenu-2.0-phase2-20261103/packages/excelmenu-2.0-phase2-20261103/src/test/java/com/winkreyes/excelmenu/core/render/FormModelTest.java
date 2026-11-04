package com.winkreyes.excelmenu.core.render;

import com.winkreyes.excelmenu.core.platform.Platform;
import com.winkreyes.excelmenu.core.registry.ResolvedEntry;
import org.junit.Test;

import java.util.List;

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
        // 28 buttons since the Phase 2 baseline folded magiclevel in
        // (order-bedrock 95, between skills and leaderboard) — the
        // 1.5.27 sequence plus that one live door.
        assertEquals(28, form.buttons().size());
        List<String> ids = form.buttons().stream()
                .map(b -> ((FormModel.Target.EntryTarget) b.target()).entryId())
                .toList();
        assertEquals(List.of(
                "spawn", "tpmenu", "party", "utility_pouch", "spellbook", "shop",
                "cosmetics", "guild", "jobs", "skills", "magiclevel", "leaderboard",
                "lostfound",
                "trash", "boss_rewards", "info", "commands", "website", "whatsnew",
                "ask", "halloween", "halloween_scares", "wandstyle", "autosort",
                "makersmark_settings",
                "autojoin", "silkwarnings", "aimassist"), ids);
        // Group colour + stripped label (1.5.27's colouring rule).
        assertEquals("§1Spawn", form.buttons().get(0).label());
        assertEquals("§8Aim Assist: ON", form.buttons().get(27).label());
    }

    @Test
    public void flatFormOutOfWindowHas26Buttons() throws Exception {
        LayoutDef layout = RenderFixtures.layout("server_menu");
        List<ResolvedEntry> resolved = RenderFixtures.resolve(
                "server_menu", Platform.BEDROCK, RenderFixtures.OUT_WINDOW);
        FormModel.FormPage form = FormModel.flat(layout, resolved, RenderFixtures.CONFIG);
        assertEquals(26, form.buttons().size());
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
        assertEquals(9, form.buttons().size());
        String[] lines = form.content().split("\n");
        assertEquals("/claim - Claim the land around you", lines[0]);
        assertEquals("/abandonclaim - Delete the claim you stand in (no undo)",
                lines[8]);
        assertEquals("", lines[9]);
        assertTrue(lines[10].startsWith("Buttons run the command"));
        // The typed-argument rows ARE buttons (their [message] hint
        // speaks on tap) — including the Bedrock-only rows.
        List<String> ids = form.buttons().stream()
                .map(b -> ((FormModel.Target.EntryTarget) b.target()).entryId())
                .toList();
        assertTrue(ids.contains("cl_childproofmenu"));
        assertTrue(ids.contains("cl_unchildproof_player"));
        assertEquals("/childproofmenu", form.buttons().get(3).label());
    }

    @Test
    public void groupFormForJavaOnlyGroupRenders() throws Exception {
        LayoutDef layout = RenderFixtures.layout("commands_menu");
        List<ResolvedEntry> resolved = RenderFixtures.resolve(
                "commands_menu", Platform.BEDROCK, RenderFixtures.IN_WINDOW);
        FormModel.FormPage form = FormModel.group(layout, resolved, "fixes_help");
        assertEquals(4, form.buttons().size());
        assertTrue(form.content().startsWith("/chunks - Fix dropped items"));
    }
}

// ExcelMenu — an Excel plugin by WinkReyesValley. (Excel signature — MikeExcel)

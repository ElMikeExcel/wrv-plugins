package com.winkreyes.excelmenu.core.render;

import com.winkreyes.excelmenu.core.platform.Platform;
import com.winkreyes.excelmenu.core.registry.EntryDef;
import com.winkreyes.excelmenu.core.registry.ResolvedEntry;
import org.junit.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * 3.0 pins (DESIGN §2.5): the grouped server_menu variant with
 * {@code pinned: [spawn, tpmenu]} — picker order and pin labels,
 * group-form exclusion, flat dedupe, guard-failed pins, and the
 * empty-pins shape staying exactly the 2.0.1 picker.
 */
public class PinsModelTest {

    private LayoutDef groupedServerMenu(List<String> pinned) throws Exception {
        LayoutDef flat = RenderFixtures.layout("server_menu");
        Map<String, String> groups = new LinkedHashMap<>();
        groups.put("travel", "Travel");
        groups.put("me", "Me");
        groups.put("shops", "Shops");
        groups.put("help", "Help");
        groups.put("settings", "Settings");
        groups.put("chest_tools", "Chest Organization");
        return new LayoutDef(flat.id(), flat.title(), LayoutDef.Form.GROUPED,
                flat.formTitle(), flat.formContent(), "Pick a section:", null,
                groups, flat.pages(), pinned);
    }

    private List<ResolvedEntry> resolved() {
        return RenderFixtures.resolve("server_menu", Platform.BEDROCK,
                RenderFixtures.IN_WINDOW);
    }

    private static List<String> entryIds(FormModel.FormPage page) {
        List<String> out = new ArrayList<>();
        for (FormModel.Button b : page.buttons()) {
            if (b.target() instanceof FormModel.Target.EntryTarget e) out.add(e.entryId());
        }
        return out;
    }

    @Test
    public void pickerRendersPinsFirstThenGroupsThenRearrange() throws Exception {
        LayoutDef layout = groupedServerMenu(List.of("spawn", "tpmenu"));
        FormModel.FormPage picker = FormModel.picker(layout, resolved(),
                RenderFixtures.CONFIG, true);
        List<FormModel.Button> b = picker.buttons();
        // Pins first, EntryTargets, labels by the PIN colour rule
        // (3.0.1: dark blue — blue is owned by the pins).
        assertEquals("spawn", ((FormModel.Target.EntryTarget) b.get(0).target()).entryId());
        assertEquals("§1Spawn", b.get(0).label());
        assertEquals("tpmenu", ((FormModel.Target.EntryTarget) b.get(1).target()).entryId());
        assertEquals("§1Teleport to a player", b.get(1).label());
        // Then group buttons (Travel still present — it has non-pins).
        assertTrue(b.get(2).target() instanceof FormModel.Target.GroupTarget);
        assertEquals("travel", ((FormModel.Target.GroupTarget) b.get(2).target()).group());
        // Rearrange LAST; no Back anywhere on the root picker.
        FormModel.Button last = b.get(b.size() - 1);
        assertEquals("Rearrange Menu", last.label());
        assertTrue(last.target() instanceof FormModel.Target.EditorTarget);
        assertTrue(b.stream().noneMatch(x -> "Back".equals(x.label())));
    }

    @Test
    public void pickerWithoutRearrangeHasNoEditorButton() throws Exception {
        LayoutDef layout = groupedServerMenu(List.of("spawn", "tpmenu"));
        FormModel.FormPage picker = FormModel.picker(layout, resolved(),
                RenderFixtures.CONFIG, false);
        assertTrue(picker.buttons().stream().noneMatch(
                x -> x.target() instanceof FormModel.Target.EditorTarget));
    }

    @Test
    public void groupFormExcludesPinsAndEndsWithBack() throws Exception {
        LayoutDef layout = groupedServerMenu(List.of("spawn", "tpmenu"));
        FormModel.FormPage travel = FormModel.group(layout, resolved(), "travel");
        List<String> ids = entryIds(travel);
        assertFalse(ids.contains("spawn"));
        assertFalse(ids.contains("tpmenu"));
        assertTrue(ids.contains("party"));
        assertTrue(travel.buttons().get(travel.buttons().size() - 1).target()
                instanceof FormModel.Target.PickerTarget);
        assertFalse(travel.content().contains("Spawn -"));
    }

    @Test
    public void flatDedupesPins() throws Exception {
        LayoutDef layout = groupedServerMenu(List.of("spawn", "tpmenu"));
        List<ResolvedEntry> resolved = resolved();
        FormModel.FormPage flat = FormModel.flat(layout, resolved, RenderFixtures.CONFIG);
        List<String> ids = entryIds(flat);
        assertEquals(resolved.size(), ids.size());
        assertEquals("spawn", ids.get(0));
        assertEquals("tpmenu", ids.get(1));
        assertEquals(1, ids.stream().filter("spawn"::equals).count());
    }

    @Test
    public void guardFailedPinStillTargetsItsEntry() throws Exception {
        LayoutDef layout = groupedServerMenu(List.of("spawn", "tpmenu"));
        EntryDef spawn = RenderFixtures.allEntries().get("spawn");
        ResolvedEntry guarded = new ResolvedEntry(spawn, spawn.icon(), spawn.label(),
                spawn.lore(), RenderFixtures.CONFIG.unavailableAction(), null,
                "plugin ExcelMagicLevel absent");
        List<ResolvedEntry> resolved = new ArrayList<>();
        resolved.add(guarded);
        resolved.addAll(resolved().stream().filter(r -> !r.id().equals("spawn")).toList());
        FormModel.FormPage picker = FormModel.picker(layout, resolved,
                RenderFixtures.CONFIG, false);
        assertEquals("spawn", ((FormModel.Target.EntryTarget)
                picker.buttons().get(0).target()).entryId());
    }

    @Test
    public void emptyPinsPickerIsThe201Shape() throws Exception {
        LayoutDef layout = groupedServerMenu(List.of());
        FormModel.FormPage picker = FormModel.picker(layout, resolved(),
                RenderFixtures.CONFIG, false);
        assertTrue(picker.buttons().stream().allMatch(
                x -> x.target() instanceof FormModel.Target.GroupTarget));
    }

    @Test
    public void commandsMenuPickerHasNoPinsAndNoRearrange() throws Exception {
        LayoutDef layout = RenderFixtures.layout("commands_menu");
        List<ResolvedEntry> resolved = RenderFixtures.resolve(
                "commands_menu", Platform.BEDROCK, RenderFixtures.IN_WINDOW);
        // Even with rearrange "enabled" at the call, the caller only
        // passes true for rearrange-served layouts; here the model
        // is exercised with false (the EntryService/config scope),
        // and commands_menu carries no pinned key at all.
        FormModel.FormPage picker = FormModel.picker(layout, resolved,
                RenderFixtures.CONFIG, false);
        assertTrue(picker.buttons().stream().allMatch(
                x -> x.target() instanceof FormModel.Target.GroupTarget));
        assertTrue(layout.pinned().isEmpty());
    }

    @Test
    public void groupOrderOverrideReordersPickerGroups() throws Exception {
        LayoutDef layout = groupedServerMenu(List.of("spawn", "tpmenu"));
        FormModel.FormPage picker = FormModel.picker(layout, resolved(),
                RenderFixtures.CONFIG, false, List.of("me", "travel"));
        List<String> groups = new ArrayList<>();
        for (FormModel.Button b : picker.buttons()) {
            if (b.target() instanceof FormModel.Target.GroupTarget g) groups.add(g.group());
        }
        assertEquals("me", groups.get(0));
        assertEquals("travel", groups.get(1));
    }
}

// ExcelMenu — an Excel plugin by WinkReyesValley. (Excel signature — MikeExcel)

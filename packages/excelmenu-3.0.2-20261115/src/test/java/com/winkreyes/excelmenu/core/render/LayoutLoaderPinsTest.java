package com.winkreyes.excelmenu.core.render;

import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** 3.0 pinned-key schema discipline (DESIGN §2.2/§2.5). */
public class LayoutLoaderPinsTest {

    private LayoutLoader.Result load(String extra) throws Exception {
        Path dir = Files.createTempDirectory("layout-pins-test");
        Path f = dir.resolve("test_menu.yml");
        String body = """
                id: test_menu
                title: '&6&lT'
                form: grouped
                pages:
                  - size: 45
                    slots:
                      g1: [1, 2]
                """ + extra;
        Files.writeString(f, body);
        return LayoutLoader.load(f);
    }

    @Test
    public void pinnedParsesInDeclarationOrder() throws Exception {
        LayoutLoader.Result r = load("pinned: [spawn, tpmenu]\n");
        assertTrue(r.errors().toString(), r.ok());
        assertEquals(List.of("spawn", "tpmenu"), r.def().pinned());
    }

    @Test
    public void absentPinnedIsEmpty() throws Exception {
        LayoutLoader.Result r = load("");
        assertTrue(r.errors().toString(), r.ok());
        assertEquals(List.of(), r.def().pinned());
    }

    @Test
    public void duplicatePinRefused() throws Exception {
        LayoutLoader.Result r = load("pinned: [spawn, spawn]\n");
        assertFalse(r.ok());
        assertTrue(r.errors().stream().anyMatch(e -> e.contains("duplicate pin")));
    }

    @Test
    public void moreThanFourPinsRefused() throws Exception {
        LayoutLoader.Result r = load("pinned: [a, b, c, d, e]\n");
        assertFalse(r.ok());
        assertTrue(r.errors().stream().anyMatch(e -> e.contains("cap is 4")));
    }

    @Test
    public void nonListPinnedRefused() throws Exception {
        assertFalse(load("pinned: spawn\n").ok());
    }

    @Test
    public void blankPinRefused() throws Exception {
        assertFalse(load("pinned: ['']\n").ok());
    }
}

// ExcelMenu — an Excel plugin by WinkReyesValley. (Excel signature — MikeExcel)

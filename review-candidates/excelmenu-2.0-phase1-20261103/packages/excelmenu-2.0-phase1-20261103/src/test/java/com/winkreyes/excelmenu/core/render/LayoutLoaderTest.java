package com.winkreyes.excelmenu.core.render;

import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Layout schema discipline: the production layouts load, and the
 *  classic authoring mistakes are loud refusals. */
public class LayoutLoaderTest {

    @Test
    public void productionLayoutsLoad() throws Exception {
        LayoutDef server = RenderFixtures.layout("server_menu");
        assertEquals("server_menu", server.id());
        assertEquals(LayoutDef.Form.FLAT, server.form());
        assertEquals(1, server.pages().size());
        assertEquals(45, server.pages().get(0).size());
        assertEquals(5, server.pages().get(0).fixed().size());

        LayoutDef commands = RenderFixtures.layout("commands_menu");
        assertEquals(LayoutDef.Form.GROUPED, commands.form());
        assertEquals(2, commands.pages().size());
        assertEquals(9, commands.groups().size());
        assertEquals(0, commands.pageOfGroup("getting_around"));
        assertEquals(1, commands.pageOfGroup("your_items_help"));
    }

    private LayoutLoader.Result load(String body) throws Exception {
        Path dir = Files.createTempDirectory("layout-test");
        Path f = dir.resolve("test_menu.yml");
        Files.writeString(f, body.replace("ID", "test_menu"));
        return LayoutLoader.load(f);
    }

    private static final String BASE = """
            id: ID
            title: '&6&lT'
            form: flat
            pages:
              - size: 45
                slots:
                  g1: [1, 2]
            """;

    @Test
    public void unknownKeyRefused() throws Exception {
        assertFalse(load(BASE + "bogus: 1\n").ok());
    }

    @Test
    public void idMustMatchFilename() throws Exception {
        assertFalse(load(BASE.replace("id: ID", "id: other")).ok());
    }

    @Test
    public void groupOnTwoPagesRefused() throws Exception {
        String body = BASE + """
              - size: 45
                slots:
                  g1: [3]
            """;
        // The BASE has one page; appending a second page list item at
        // the wrong indent would not parse as intended, so build the
        // two-page body directly.
        body = """
                id: ID
                title: '&6&lT'
                pages:
                  - size: 45
                    slots:
                      g1: [1]
                  - size: 45
                    slots:
                      g1: [2]
                """;
        LayoutLoader.Result r = load(body);
        assertFalse(r.ok());
        assertTrue(r.errors().toString(), r.errors().stream()
                .anyMatch(e -> e.contains("already has slots")));
    }

    @Test
    public void fixedSlotCollidingWithGroupSlotRefused() throws Exception {
        String body = """
                id: ID
                title: '&6&lT'
                pages:
                  - size: 45
                    fixed:
                      - slot: 1
                        material: STONE
                        name: 'H'
                    slots:
                      g1: [1, 2]
                """;
        LayoutLoader.Result r = load(body);
        assertFalse(r.ok());
        assertTrue(r.errors().toString(), r.errors().stream()
                .anyMatch(e -> e.contains("collides")));
    }

    @Test
    public void badSizeRefused() throws Exception {
        assertFalse(load(BASE.replace("size: 45", "size: 40")).ok());
    }

    @Test
    public void pageAndActionBothSetRefused() throws Exception {
        String body = """
                id: ID
                title: '&6&lT'
                pages:
                  - size: 45
                    fixed:
                      - slot: 3
                        material: ARROW
                        name: 'N'
                        page: 2
                        action: ['[close]']
                    slots:
                      g1: [1]
                """;
        assertFalse(load(body).ok());
    }

    @Test
    public void unknownActionTagInFixedRefused() throws Exception {
        String body = """
                id: ID
                title: '&6&lT'
                pages:
                  - size: 45
                    fixed:
                      - slot: 3
                        material: ARROW
                        name: 'N'
                        action: ['[teleport] home']
                    slots:
                      g1: [1]
                """;
        assertFalse(load(body).ok());
    }
}

// ExcelMenu — an Excel plugin by WinkReyesValley. (Excel signature — MikeExcel)

// ExcelMenu — an Excel plugin by WinkReyesValley. (Excel signature — MikeExcel)
package com.winkreyes.excelmenu.bukkit;

import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * The regression guard owed since the Nov 1, 2026 registry incident
 * (ROOT-CAUSE-20261101.md): ExcelMenu's hand-maintained BUNDLED_*
 * extraction lists must equal the resources tree, exactly — the
 * incident was an entry file (halloween_scares) present in the jar
 * but missing from the list, so first boot extracted 82 of 83 and
 * the registry read 82. magiclevel joined the same way at the
 * Phase 2 baseline; this test is what makes the class of bug fail
 * the build instead of a boot.
 */
public class BundledEntriesTest {

    private static Set<String> stems(Path dir) throws Exception {
        Set<String> out = new HashSet<>();
        try (Stream<Path> s = Files.list(dir)) {
            s.filter(p -> p.getFileName().toString().endsWith(".yml"))
                    .forEach(p -> out.add(p.getFileName().toString()
                            .replaceFirst("\\.yml$", "")));
        }
        return out;
    }

    private static void assertListEqualsTree(String what, String[] bundled,
                                             Path dir) throws Exception {
        Set<String> listed = new HashSet<>();
        for (String name : bundled) {
            assertTrue(what + ": duplicate in BUNDLED list: " + name,
                    listed.add(name));
        }
        assertEquals(what + ": BUNDLED list must equal the resources tree",
                stems(dir), listed);
    }

    /**
     * The jar's own YAML must parse (the Phase 2 rig caught an
     * unquoted ": " inside plugin.yml's long description — the jar
     * built and tested green and Paper refused to load it).
     */
    @Test
    public void jarYamlParses() throws Exception {
        Path resources = Path.of(System.getProperty(
                "harness.entries", "src/main/resources/entries")).getParent();
        org.yaml.snakeyaml.Yaml yaml = new org.yaml.snakeyaml.Yaml();
        for (String name : new String[]{"plugin.yml", "config.yml"}) {
            try (java.io.InputStream in = Files.newInputStream(resources.resolve(name))) {
                Object parsed = yaml.load(in);
                assertTrue(name + " must parse to a map", parsed instanceof java.util.Map);
            }
        }
    }

    @Test
    public void bundledListsEqualTheResourcesTree() throws Exception {
        assertListEqualsTree("entries", ExcelMenu.BUNDLED_ENTRIES,
                Path.of(System.getProperty(
                        "harness.entries", "src/main/resources/entries")));
        assertListEqualsTree("layouts", ExcelMenu.BUNDLED_LAYOUTS,
                Path.of(System.getProperty(
                        "harness.layouts", "src/main/resources/layouts")));
        assertListEqualsTree("menus", ExcelMenu.BUNDLED_MENUS,
                Path.of(System.getProperty(
                        "harness.menus", "src/main/resources/menus")));
    }
}

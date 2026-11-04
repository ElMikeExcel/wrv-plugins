package com.winkreyesvalley.excelmagiclevel;

import org.junit.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * ExcelMagicLevel — the §9.1 boundary, ENFORCED not just intended: core
 * and store import ZERO mcMMO types (the test fails the build if a
 * {@code com.gmail.nossr50} reference ever appears there), and core
 * additionally imports no Bukkit and no Magic API — it stays pure and
 * unit-testable forever.
 */
public class PackageDependencyTest {
    @Test
    public void coreAndStoreNeverTouchMcMmo() throws Exception {
        assertNoForbidden("core", List.of("com.gmail.nossr50"));
        assertNoForbidden("store", List.of("com.gmail.nossr50", "com.elmakers"));
    }

    @Test
    public void coreStaysPure() throws Exception {
        assertNoForbidden("core", List.of("org.bukkit", "com.elmakers", "net.kyori"));
    }

    private void assertNoForbidden(String pkg, List<String> forbidden) throws Exception {
        Path srcRoot = Path.of(System.getProperty("magiclevel.srcdir", "src/main/java"));
        Path dir = srcRoot.resolve("com/winkreyesvalley/excelmagiclevel").resolve(pkg);
        if (!Files.isDirectory(dir)) {
            fail("source package not found: " + dir.toAbsolutePath());
        }
        List<String> violations = new ArrayList<>();
        try (Stream<Path> walk = Files.walk(dir)) {
            for (Path file : walk.filter(p -> p.toString().endsWith(".java")).toList()) {
                String text = Files.readString(file, StandardCharsets.UTF_8);
                for (String bad : forbidden) {
                    if (text.contains(bad)) {
                        violations.add(file.getFileName() + " references " + bad);
                    }
                }
            }
        }
        assertTrue("package '" + pkg + "' boundary violated: " + violations, violations.isEmpty());
        assertTrue("expected at least one source file in " + pkg,
                new File(dir.toUri()).listFiles((d, n) -> n.endsWith(".java")) != null);
    }
}

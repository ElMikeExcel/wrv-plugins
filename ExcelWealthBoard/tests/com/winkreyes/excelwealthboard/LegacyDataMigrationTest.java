package com.winkreyes.excelwealthboard;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Migration contract: first-boot copies are byte-for-byte, the legacy
 * folder is never modified, and an already-populated new folder is
 * never overwritten.
 */
class LegacyDataMigrationTest {

    @TempDir
    Path temp;

    private final List<String> logs = new ArrayList<>();

    @Test
    void copiesNestedFilesByteForByteAndLeavesLegacyUntouched() throws IOException {
        Path legacy = temp.resolve("WealthBoard");
        Path fresh = temp.resolve("ExcelWealthBoard");
        Files.createDirectories(legacy.resolve("sub/deep"));
        byte[] binary = new byte[] {0, 1, 2, (byte) 0xFF, 42};
        Files.write(legacy.resolve("config.yml"), "key: value\n".getBytes());
        Files.write(legacy.resolve("sub/data.bin"), binary);
        Files.write(legacy.resolve("sub/deep/note.txt"), "hello".getBytes());

        int copied = LegacyDataMigration.migrate(legacy.toFile(), fresh.toFile(), logs::add);

        assertEquals(3, copied);
        assertArrayEquals("key: value\n".getBytes(), Files.readAllBytes(fresh.resolve("config.yml")));
        assertArrayEquals(binary, Files.readAllBytes(fresh.resolve("sub/data.bin")));
        assertArrayEquals("hello".getBytes(), Files.readAllBytes(fresh.resolve("sub/deep/note.txt")));
        // Legacy untouched
        assertArrayEquals(binary, Files.readAllBytes(legacy.resolve("sub/data.bin")));
        assertTrue(Files.exists(legacy.resolve("config.yml")));
        assertEquals(1, logs.size());
        assertTrue(logs.get(0).startsWith("Migrated legacy data folder plugins/WealthBoard -> plugins/ExcelWealthBoard"), logs.get(0));
    }

    @Test
    void skipsWhenNewFolderAlreadyHasFiles() throws IOException {
        Path legacy = temp.resolve("WealthBoard");
        Path fresh = temp.resolve("ExcelWealthBoard");
        Files.createDirectories(legacy);
        Files.createDirectories(fresh);
        Files.write(legacy.resolve("config.yml"), "legacy".getBytes());
        Files.write(fresh.resolve("config.yml"), "current".getBytes());

        int copied = LegacyDataMigration.migrate(legacy.toFile(), fresh.toFile(), logs::add);

        assertEquals(0, copied);
        assertArrayEquals("current".getBytes(), Files.readAllBytes(fresh.resolve("config.yml")));
        assertTrue(logs.isEmpty());
    }

    @Test
    void missingLegacyFolderIsNoOp() {
        int copied = LegacyDataMigration.migrate(temp.resolve("nope").toFile(),
                temp.resolve("ExcelWealthBoard").toFile(), logs::add);
        assertEquals(0, copied);
        assertFalse(Files.exists(temp.resolve("ExcelWealthBoard")));
        assertTrue(logs.isEmpty());
    }

    @Test
    void emptyLegacyFolderCopiesNothing() throws IOException {
        Path legacy = temp.resolve("WealthBoard");
        Files.createDirectories(legacy);
        int copied = LegacyDataMigration.migrate(legacy.toFile(),
                temp.resolve("ExcelWealthBoard").toFile(), logs::add);
        assertEquals(0, copied);
        assertTrue(logs.isEmpty());
    }

    @Test
    void emptyExistingNewFolderStillMigrates() throws IOException {
        Path legacy = temp.resolve("WealthBoard");
        Path fresh = temp.resolve("ExcelWealthBoard");
        Files.createDirectories(legacy);
        Files.createDirectories(fresh.resolve("emptydir"));
        Files.write(legacy.resolve("a.txt"), "a".getBytes());

        int copied = LegacyDataMigration.migrate(legacy.toFile(), fresh.toFile(), logs::add);

        assertEquals(1, copied);
        assertArrayEquals("a".getBytes(), Files.readAllBytes(fresh.resolve("a.txt")));
    }
}

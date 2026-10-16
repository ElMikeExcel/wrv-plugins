package com.winkreyes.excelnophantoms;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * One-time, non-destructive migration of the legacy NoPhantoms data folder.
 * Copies (never moves or deletes) legacy files into this plugin's data
 * folder on first enable, but only while the new folder still holds no
 * files at all, so a returning install is never overwritten. The legacy
 * folder is always left in place as part of the rollback record.
 */
final class LegacyDataMigration {

    private LegacyDataMigration() {
    }

    /**
     * Returns the number of files copied. Returns 0 when there is nothing
     * to migrate: no legacy folder, an empty legacy folder, or a new
     * folder that already contains files.
     */
    static int migrate(File legacyDir, File newDir, Consumer<String> log) {
        if (legacyDir == null || !legacyDir.isDirectory()) {
            return 0;
        }
        if (newDir.exists() && containsAnyFile(newDir)) {
            return 0;
        }
        int copied = 0;
        try (Stream<java.nio.file.Path> walk = Files.walk(legacyDir.toPath())) {
            for (java.nio.file.Path source : walk.filter(Files::isRegularFile).toList()) {
                java.nio.file.Path target = newDir.toPath().resolve(legacyDir.toPath().relativize(source));
                try {
                    Files.createDirectories(target.getParent());
                    Files.copy(source, target, StandardCopyOption.COPY_ATTRIBUTES);
                    copied++;
                } catch (IOException e) {
                    log.accept("Could not copy legacy file " + source + ": " + e.getMessage());
                }
            }
        } catch (IOException | RuntimeException e) {
            // RuntimeException included on purpose: a mid-walk I/O failure
            // surfaces as UncheckedIOException, and migration is best-effort
            // - it must never stop the plugin from enabling.
            log.accept("Could not read legacy data folder " + legacyDir + ": " + e.getMessage());
        }
        if (copied > 0) {
            log.accept("Migrated legacy data folder plugins/" + legacyDir.getName()
                    + " -> plugins/" + newDir.getName() + " (" + copied + " files)");
        }
        return copied;
    }

    private static boolean containsAnyFile(File dir) {
        File[] entries = dir.listFiles();
        if (entries == null) {
            return false;
        }
        for (File entry : entries) {
            if (entry.isFile() || (entry.isDirectory() && containsAnyFile(entry))) {
                return true;
            }
        }
        return false;
    }
}

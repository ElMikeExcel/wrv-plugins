package com.winkreyes.excelcosmetics.core;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Atomic file replacement (0.1.3): write the full content to a
 * sibling .tmp file, then move it over the target, so a crash or
 * failure mid-write can never truncate the last good file.
 * ATOMIC_MOVE falls back to a plain REPLACE_EXISTING move only
 * on AtomicMoveNotSupportedException (filesystems without atomic
 * move must not fail every save). Any other IOException returns
 * false with the target untouched (the .tmp may be left behind;
 * readers only ever read the target).
 */
public final class AtomicWrite {

    private AtomicWrite() {}

    public static Path tmpPath(Path target) {
        return target.resolveSibling(target.getFileName().toString() + ".tmp");
    }

    public static boolean writeAtomically(Path target, String content) {
        Path tmp = tmpPath(target);
        try {
            Path parent = target.getParent();
            if (parent != null) Files.createDirectories(parent);
            Files.writeString(tmp, content, StandardCharsets.UTF_8);
        } catch (IOException e) {
            return false;
        }
        try {
            Files.move(tmp, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            return true;
        } catch (AtomicMoveNotSupportedException e) {
            try {
                Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
                return true;
            } catch (IOException e2) {
                return false;
            }
        } catch (IOException e) {
            return false;
        }
    }
}

// ExcelCosmetics — an Excel plugin by WinkReyesValley. (Excel signature — MikeExcel)

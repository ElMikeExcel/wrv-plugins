package com.winkreyes.trash;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.ZonedDateTime;

/** Append-only dispute log: plugins/ExcelTrash/trash.log */
public final class TrashLog {
    private final Path file;
    public TrashLog(Path file) { this.file = file; }
    public synchronized void write(String action, String player, java.util.UUID id, int stacks, int items, String detail) {
        String line = ZonedDateTime.now() + " " + action + " player=" + player + " uuid=" + id
                + " stacks=" + stacks + " items=" + items + (detail == null || detail.isEmpty() ? "" : " " + detail) + System.lineSeparator();
        try {
            Files.createDirectories(file.toAbsolutePath().getParent());
            Files.writeString(file, line, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException ignored) { }
    }
}

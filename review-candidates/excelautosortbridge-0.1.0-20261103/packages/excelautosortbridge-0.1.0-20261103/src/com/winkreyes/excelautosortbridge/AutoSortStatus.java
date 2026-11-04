// Excel signature — an Excel plugin by WinkReyesValley. (Excel signature — MikeExcel)
package com.winkreyes.excelautosortbridge;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Pure status logic for the AutoSort bridge — no Bukkit types, so the
 * whole contract is unit-testable off-server.
 *
 * AutoSort (by nothub, live 0.1.17) keeps its per-player state as a
 * set of UUIDs that have sorting ENABLED (everyone else is OFF — there
 * is no explicit OFF record), persisted one UUID per line in
 * plugins/autosort/active.txt, rewritten synchronously on every
 * toggle. This class owns: parsing that file's lines, and mapping
 * membership to the menu's plain ON/OFF strings.
 */
public final class AutoSortStatus {

    private AutoSortStatus() {}

    public static final String ON = "ON";
    public static final String OFF = "OFF";

    /** The owner's own state-file location, relative to the server root
     *  (the owner builds the identical relative path in its Plugin class). */
    public static final Path ACTIVES_FILE = Path.of("plugins", "autosort", "active.txt");

    /**
     * Parse active.txt lines into the actives set. Blank lines and
     * malformed lines are skipped (the owner's own loader is equally
     * tolerant per-line); a null list yields an empty set.
     */
    public static Set<UUID> parseActives(List<String> lines) {
        Set<UUID> out = new HashSet<>();
        if (lines == null) return out;
        for (String line : lines) {
            if (line == null) continue;
            String t = line.trim();
            if (t.isEmpty()) continue;
            try {
                out.add(UUID.fromString(t));
            } catch (IllegalArgumentException skipped) {
                // One bad line must not void the whole file.
            }
        }
        return out;
    }

    /**
     * Read the state file. Returns null when the file cannot be read
     * at all (missing or IO error) — the caller treats null as
     * "state unknown", never as OFF.
     */
    public static Set<UUID> readActivesFile(Path file) {
        try {
            return parseActives(Files.readAllLines(file));
        } catch (IOException | RuntimeException e) {
            return null;
        }
    }

    /** Map set membership to the menu string. Null set = unknown = null. */
    public static String statusOf(Set<UUID> actives, UUID player) {
        if (actives == null || player == null) return null;
        return actives.contains(player) ? ON : OFF;
    }
}

// ExcelAutoSortBridge — Excel (Iris), for Wink Reyes Valley

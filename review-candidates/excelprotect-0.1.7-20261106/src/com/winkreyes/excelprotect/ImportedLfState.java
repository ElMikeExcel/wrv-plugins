package com.winkreyes.excelprotect;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * ExcelProtect 0.1.7 — the DeathRecord importer's Lost & Found STATE delta book.
 *
 * DeathRecord record files (deaths/, burned/) are immutable, so the importer's
 * path marks (imported-sources.json) make them idempotent. Its per-player
 * state files (lostfound/players/<uuid>.yml) are MUTABLE — DeathRecord keeps
 * writing them until it retires — so path marks would freeze the first read
 * and lose later recoveries, while re-applying the full values on every import
 * would double-count. This book stores, per state-file path, the DeathRecord
 * values last applied; the importer merges only the DELTA:
 *   recoveries: DeathRecord count minus last-applied count (never negative —
 *               a decreased source count re-baselines without subtracting
 *               recoveries Protect already credited);
 *   free-used:  applied at most once per path, and the merge itself is an OR.
 * The book is small JSON in the data folder (restore-grade state, exempt from
 * retention), same convention as restore-marks.json. Excel — Wink Reyes Valley
 */
public final class ImportedLfState {
    /** What one merge should apply: recoveries to ADD, and whether the free flag applies now. */
    public record Delta(int recoveriesDelta, boolean applyFreeUsed) { }

    private record Applied(int recoveries, boolean freeUsed) { }

    private final File file;
    private final Map<String, Applied> applied = new LinkedHashMap<>();

    public ImportedLfState(File dataDir) {
        file = new File(dataDir, "imported-lf-state.json");
        try {
            if (file.exists()) {
                String all = Files.readString(file.toPath(), StandardCharsets.UTF_8);
                for (String obj : splitObjects(all)) {
                    String path = Json.strField(obj, "path");
                    if (path == null) continue;
                    applied.put(path, new Applied((int) Json.numField(obj, "recoveries", 0),
                            obj.contains("\"free_used\":true")));
                }
            }
        } catch (Exception ignored) { }
    }

    /** The delta between a state file's current DeathRecord values and what was last applied. */
    public synchronized Delta deltaFor(String path, int drRecoveries, boolean drFreeUsed) {
        Applied prev = applied.get(path);
        int prevRec = prev == null ? 0 : prev.recoveries();
        boolean prevFree = prev != null && prev.freeUsed();
        return new Delta(Math.max(0, drRecoveries - prevRec), drFreeUsed && !prevFree);
    }

    /** Record a state file's DeathRecord values as applied (persists immediately). */
    public synchronized void commit(String path, int drRecoveries, boolean drFreeUsed) {
        Applied prev = applied.get(path);
        boolean free = drFreeUsed || (prev != null && prev.freeUsed());
        applied.put(path, new Applied(drRecoveries, free));
        try {
            Json.Arr arr = new Json.Arr();
            applied.forEach((p, a) -> arr.add(new Json.Obj().put("path", p)
                    .put("recoveries", a.recoveries()).put("free_used", a.freeUsed())));
            Files.writeString(file.toPath(), arr.render(), StandardCharsets.UTF_8);
        } catch (Exception ignored) { }
    }

    private static List<String> splitObjects(String arr) {
        List<String> out = new ArrayList<>();
        int depth = 0, start = -1; boolean str = false, esc = false;
        for (int i = 0; i < arr.length(); i++) {
            char c = arr.charAt(i);
            if (str) { if (esc) esc = false; else if (c == '\\') esc = true; else if (c == '"') str = false; continue; }
            if (c == '"') str = true;
            else if (c == '{') { if (depth++ == 0) start = i; }
            else if (c == '}') { if (--depth == 0 && start >= 0) { out.add(arr.substring(start, i + 1)); start = -1; } }
        }
        return out;
    }
}

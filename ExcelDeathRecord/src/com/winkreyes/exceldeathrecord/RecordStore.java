package com.winkreyes.exceldeathrecord;

import com.winkreyes.exceldeathrecord.core.BurnFormat;
import com.winkreyes.exceldeathrecord.core.BurnIndex;
import com.winkreyes.exceldeathrecord.core.DeathRecord;
import com.winkreyes.exceldeathrecord.core.DestructionRecord;
import com.winkreyes.exceldeathrecord.core.RecordFormat;
import com.winkreyes.exceldeathrecord.core.RecordIndex;
import com.winkreyes.exceldeathrecord.core.RecordName;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * The on-disk ledgers, under the plugin data folder:
 *
 *   deaths/<player-uuid>/<yyyy-MM-dd_HHmmss>.yml   one file per death
 *   burned/<owner-uuid|unknown>/<...>.yml          one file per destruction
 *
 * Writers run async with ALREADY-FORMATTED text (serialization
 * happens on the main thread at the event; only the file write
 * leaves it). One lock serializes (list dir -> pick unique name
 * -> write) so two deaths in the same second — or a death and a
 * burn in the same second — can never collide on a file name.
 * Readers run on the command thread; the files are small and
 * the server is family-scale.
 */
final class RecordStore {

    private final File deathsRoot;
    private final File burnedRoot;
    private final Object writeLock = new Object();
    private final ZoneId zone = ZoneId.systemDefault();

    RecordStore(File dataFolder) {
        this.deathsRoot = new File(dataFolder, "deaths");
        this.burnedRoot = new File(dataFolder, "burned");
    }

    ZoneId zone() { return zone; }

    // ---- writes (async callers) ----

    void writeDeath(DeathRecord record, String text) throws IOException {
        File dir = new File(deathsRoot, record.playerUuid);
        writeUnique(dir, record.epochMillis, text);
    }

    void writeBurn(DestructionRecord record, String text) throws IOException {
        String folder = record.ownerUuid.isEmpty() ? "unknown" : record.ownerUuid;
        File dir = new File(burnedRoot, folder);
        writeUnique(dir, record.epochMillis, text);
    }

    private void writeUnique(File dir, long epochMillis, String text) throws IOException {
        synchronized (writeLock) {
            Files.createDirectories(dir.toPath());
            Set<String> existing = new HashSet<>();
            String[] names = dir.list();
            if (names != null) for (String n : names) existing.add(n);
            String fileName = RecordName.unique(RecordName.baseName(epochMillis, zone), existing);
            Files.writeString(new File(dir, fileName).toPath(), text, StandardCharsets.UTF_8);
        }
    }

    // ---- reads (command thread) ----

    /** Find the UUID whose death records belong to a typed name.
     *  Matches the STORED player name (case-insensitive, with or
     *  without the Floodgate leading dot), then falls back to
     *  Bukkit's name->UUID knowledge. Null when nothing matches. */
    UUID resolveDeathsPlayer(String typedName) {
        String typed = typedName.trim().toLowerCase();
        File[] dirs = deathsRoot.listFiles(File::isDirectory);
        if (dirs != null) {
            for (File dir : dirs) {
                List<RecordIndex.Entry> entries = readDeathFolder(dir);
                if (!entries.isEmpty()) {
                    String stored = entries.get(0).record.playerName;
                    if (stored.equalsIgnoreCase(typed) || nameMatchesDots(stored, typed)) {
                        try { return UUID.fromString(dir.getName()); } catch (IllegalArgumentException ignored) { }
                    }
                }
            }
        }
        String undeleted = typed.startsWith(".") ? typed.substring(1) : typed;
        OfflinePlayer op = Bukkit.getOfflinePlayer(undeleted);
        File dir = new File(deathsRoot, op.getUniqueId().toString());
        return dir.isDirectory() ? op.getUniqueId() : null;
    }

    List<RecordIndex.Entry> readDeaths(UUID uuid) {
        return readDeathFolder(new File(deathsRoot, uuid.toString()));
    }

    private List<RecordIndex.Entry> readDeathFolder(File dir) {
        List<RecordIndex.Entry> out = new ArrayList<>();
        File[] files = dir.listFiles((d, n) -> n.endsWith(".yml"));
        if (files == null) return out;
        for (File f : files) {
            try {
                DeathRecord r = RecordFormat.read(Files.readString(f.toPath(), StandardCharsets.UTF_8));
                out.add(new RecordIndex.Entry(f.getName(), r));
            } catch (IOException ignored) {
                // A half-written or unreadable file is skipped, never fatal.
            }
        }
        return out;
    }

    /** Destruction records, optionally restricted to one owner
     *  (same name matching as the death lookup). */
    List<BurnIndex.Entry> readBurns(String typedNameOrNull) {
        List<BurnIndex.Entry> out = new ArrayList<>();
        File[] dirs = burnedRoot.listFiles(File::isDirectory);
        if (dirs == null) return out;
        for (File dir : dirs) {
            File[] files = dir.listFiles((d, n) -> n.endsWith(".yml"));
            if (files == null) continue;
            for (File f : files) {
                try {
                    DestructionRecord r = BurnFormat.read(Files.readString(f.toPath(), StandardCharsets.UTF_8));
                    if (typedNameOrNull != null && !r.ownerName.equalsIgnoreCase(typedNameOrNull.trim())
                            && !nameMatchesDots(r.ownerName, typedNameOrNull.trim())) {
                        continue;
                    }
                    BurnIndex.Entry be = new BurnIndex.Entry(f.getName(), r);
                    be.folder = dir.getName();
                    out.add(be);
                } catch (IOException ignored) {
                    // skip unreadable, as with deaths
                }
            }
        }
        return out;
    }

    // ---- 0.2.0: restore support ----

    File dataFolder() { return deathsRoot.getParentFile(); }

    File deathFile(UUID uuid, String fileName) { return new File(new File(deathsRoot, uuid.toString()), fileName); }

    File burnFile(String folder, String fileName) { return new File(new File(burnedRoot, folder), fileName); }

    /** Path relative to the plugin folder, for logs and the CSV. */
    String relative(File f) {
        return dataFolder().toPath().relativize(f.toPath()).toString().replace('\\', '/');
    }

    DeathRecord readDeathFile(File f) throws IOException {
        return RecordFormat.read(Files.readString(f.toPath(), StandardCharsets.UTF_8));
    }

    DestructionRecord readBurnFile(File f) throws IOException {
        return BurnFormat.read(Files.readString(f.toPath(), StandardCharsets.UTF_8));
    }

    /** Rewrite a record in place (restore marks). Write to a temp
     *  file then move over, under the write lock, so a crash never
     *  leaves a half-written record. */
    void rewrite(File f, String text) throws IOException {
        synchronized (writeLock) {
            File tmp = new File(f.getParentFile(), f.getName() + ".tmp");
            Files.writeString(tmp.toPath(), text, StandardCharsets.UTF_8);
            try {
                Files.move(tmp.toPath(), f.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING,
                        java.nio.file.StandardCopyOption.ATOMIC_MOVE);
            } catch (java.nio.file.AtomicMoveNotSupportedException e) {
                Files.move(tmp.toPath(), f.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

    /** Append rows to restores.csv (header on first write). */
    void appendRestores(List<String> rows) throws IOException {
        if (rows.isEmpty()) return;
        synchronized (writeLock) {
            File csv = new File(dataFolder(), "restores.csv");
            Files.createDirectories(dataFolder().toPath());
            StringBuilder sb = new StringBuilder();
            if (!csv.exists() || csv.length() == 0) sb.append(com.winkreyes.exceldeathrecord.core.RestoreCsv.HEADER).append('\n');
            for (String r : rows) sb.append(r).append('\n');
            Files.writeString(csv.toPath(), sb.toString(), StandardCharsets.UTF_8,
                    java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
        }
    }

    // ---- 0.3.0: Lost & Found support ----

    /** One owner folder only (burned/<uuid>/): the Lost & Found list. */
    List<BurnIndex.Entry> readBurnFolder(String folder) {
        List<BurnIndex.Entry> out = new ArrayList<>();
        if (folder == null || folder.isEmpty() || folder.contains("/") || folder.contains("\\") || folder.contains("..")) return out;
        File dir = new File(burnedRoot, folder);
        File[] files = dir.listFiles((d, n) -> n.endsWith(".yml"));
        if (files == null) return out;
        for (File f : files) {
            try {
                DestructionRecord r = BurnFormat.read(Files.readString(f.toPath(), StandardCharsets.UTF_8));
                BurnIndex.Entry be = new BurnIndex.Entry(f.getName(), r);
                be.folder = dir.getName();
                out.add(be);
            } catch (IOException ignored) { }
        }
        return out;
    }

    String readText(File f) throws IOException {
        return Files.readString(f.toPath(), StandardCharsets.UTF_8);
    }

    File lostFoundStateFile(UUID uuid) {
        return new File(new File(new File(dataFolder(), "lostfound"), "players"), uuid.toString() + ".yml");
    }

    com.winkreyes.exceldeathrecord.core.LostFoundState readLostFoundState(UUID uuid) throws IOException {
        File f = lostFoundStateFile(uuid);
        if (!f.exists()) return new com.winkreyes.exceldeathrecord.core.LostFoundState();
        return com.winkreyes.exceldeathrecord.core.LostFoundState.read(Files.readString(f.toPath(), StandardCharsets.UTF_8));
    }

    void writeLostFoundState(UUID uuid, com.winkreyes.exceldeathrecord.core.LostFoundState st) throws IOException {
        File f = lostFoundStateFile(uuid);
        Files.createDirectories(f.getParentFile().toPath());
        rewrite(f, com.winkreyes.exceldeathrecord.core.LostFoundState.write(st));
    }

    /** Append rows to lostfound.csv (header on first write). */
    void appendLostFound(List<String> rows) throws IOException {
        if (rows.isEmpty()) return;
        synchronized (writeLock) {
            File csv = new File(dataFolder(), "lostfound.csv");
            Files.createDirectories(dataFolder().toPath());
            StringBuilder sb = new StringBuilder();
            if (!csv.exists() || csv.length() == 0) sb.append(com.winkreyes.exceldeathrecord.core.LostFoundCsv.HEADER).append('\n');
            for (String r : rows) sb.append(r).append('\n');
            Files.writeString(csv.toPath(), sb.toString(), StandardCharsets.UTF_8,
                    java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
        }
    }

    private static boolean nameMatchesDots(String stored, String typedLower) {
        String s = stored.toLowerCase();
        return s.equals("." + typedLower) || ("." + s).equals(typedLower) || s.replaceFirst("^\\.", "").equals(typedLower);
    }
}

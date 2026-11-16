package com.winkreyes.exceltogether;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.logging.Logger;

/**
 * ExcelTogether 0.6.0 (P-PARTY-REBUILD-1, SCOPING.md §2.1) - the on-disk half
 * of the roster of record: every party (id, leader, members, last-known
 * names, mutation-origin tag) in `roster.yml` inside the plugin's data
 * folder. Written through on every roster mutation (PartyManager's change
 * listener) and again at disable; read once at enable, before any sync
 * starts, and rebuilt into PartyManager via {@link PartyManager#restore}.
 * This file is what replaces the pre-0.6.0 login adoption for the stored
 * case: after a restart the roster comes from here, not from whatever the
 * EliteMobs/mcMMO faces still hold. A MISSING file is the first-boot case
 * (Loaded.filePresent == false) and the caller runs the one-shot EM-first
 * seed instead (SCOPING.md §2.5, Q6); an existing-but-empty file is a real
 * stored state (no parties) and is NOT a seed trigger.
 *
 * House pattern: follows RepairCooldownStore (Bukkit YamlConfiguration,
 * fail-soft by construction) with the durable-write discipline of the
 * fleet's fixed stores on top - the YAML is written to a temp file in the
 * same folder, the FILE is fsynced (mandatory; legal on every platform),
 * the temp file is atomically moved over roster.yml, and the DIRECTORY
 * sync is best-effort only: guarded, never fatal, because Java NIO on
 * Windows refuses directory channels outright (the settled fsync fix
 * class - an unguarded directory fsync is the bug, not the cure).
 *
 * Recovery: load never throws. A wholly unreadable file yields an empty
 * roster with parseOk == false (one warning). A partially corrupt file
 * recovers every party that still parses: entries with an unparseable id,
 * no valid leader, a duplicate party id, or fewer than MIN_SIZE distinct
 * valid members are skipped and counted in Loaded.skipped; a member listed
 * in two parties stays in the first; an unknown origin tag falls back to
 * LOGIN_REBUILD with a warning (the party itself is real - only its tag
 * was lost). A failed save only logs: the in-memory roster keeps governing
 * until the process ends.
 * Excel - Wink Reyes Valley.
 */
public final class RosterStore {

    public static final String FILE_NAME = "roster.yml";
    public static final int FORMAT_VERSION = 1;

    /** What {@link #load} found. filePresent distinguishes first boot from a stored empty roster. */
    public static final class Loaded {
        public final boolean filePresent;
        public final boolean parseOk;
        public final List<PartyManager.RestoredParty> parties;
        public final Map<UUID, String> names;
        public final long nextId;
        public final int skipped;
        Loaded(boolean filePresent, boolean parseOk, List<PartyManager.RestoredParty> parties,
               Map<UUID, String> names, long nextId, int skipped) {
            this.filePresent = filePresent; this.parseOk = parseOk;
            this.parties = parties; this.names = names; this.nextId = nextId; this.skipped = skipped;
        }
        static Loaded absent() { return new Loaded(false, true, List.of(), Map.of(), 1L, 0); }
    }

    /** Load the roster snapshot; an absent file is a clean first boot, anything unreadable is fail-soft. */
    public Loaded load(File file, Logger log) {
        if (file == null || !file.isFile()) return Loaded.absent();
        YamlConfiguration yml;
        try {
            yml = YamlConfiguration.loadConfiguration(file);
        } catch (Throwable t) {
            if (log != null) log.warning("ExcelTogether: could not read " + file.getName()
                    + " (" + t.getClass().getSimpleName() + ") - roster starts empty; the file was left untouched.");
            return new Loaded(true, false, List.of(), Map.of(), 1L, 0);
        }
        // SnakeYAML hands back an empty config for garbage it cannot map at all;
        // a config with none of our keys and a non-empty file is treated as corrupt, not as an empty roster.
        if (yml.get("version") == null && yml.getConfigurationSection("parties") == null
                && yml.getConfigurationSection("names") == null && file.length() > 0) {
            if (log != null) log.warning("ExcelTogether: " + file.getName()
                    + " does not look like a roster file - roster starts empty; the file was left untouched.");
            return new Loaded(true, false, List.of(), Map.of(), 1L, 0);
        }
        Map<UUID, String> names = new LinkedHashMap<>();
        ConfigurationSection nameSec = yml.getConfigurationSection("names");
        if (nameSec != null) {
            for (String key : nameSec.getKeys(false)) {
                try {
                    String v = nameSec.getString(key);
                    if (v != null && !v.isEmpty()) names.put(UUID.fromString(key), v);
                } catch (IllegalArgumentException bad) {
                    if (log != null) log.warning("ExcelTogether: skipping a malformed roster name entry ('" + key + "').");
                }
            }
        }
        List<PartyManager.RestoredParty> parties = new ArrayList<>();
        Set<Long> seenIds = new HashSet<>();
        Set<UUID> claimed = new HashSet<>();
        int skipped = 0;
        ConfigurationSection partySec = yml.getConfigurationSection("parties");
        if (partySec != null) {
            List<String> keys = new ArrayList<>(partySec.getKeys(false));
            keys.sort(Comparator.comparing(RosterStore::idKey, Comparator.nullsLast(Comparator.naturalOrder())));
            for (String key : keys) {
                ConfigurationSection one = partySec.getConfigurationSection(key);
                Long pid = idKey(key);
                if (one == null || pid == null || pid <= 0 || !seenIds.add(pid)) {
                    skipped++;
                    if (log != null) log.warning("ExcelTogether: skipping a malformed roster party entry ('" + key + "').");
                    continue;
                }
                UUID leader = uuidOrNull(one.getString("leader"));
                List<UUID> ms = new ArrayList<>();
                for (String s : one.getStringList("members")) {
                    UUID u = uuidOrNull(s);
                    if (u == null) {
                        if (log != null) log.warning("ExcelTogether: skipping a malformed member id ('" + s + "') in roster party " + pid + ".");
                        continue;
                    }
                    if (!ms.contains(u)) ms.add(u);
                }
                ms.removeIf(claimed::contains);          // a member listed twice stays in the first party
                if (leader == null || ms.size() < PartyManager.MIN_SIZE) {
                    skipped++;
                    if (log != null) log.warning("ExcelTogether: skipping roster party " + pid
                            + " (no valid leader or fewer than " + PartyManager.MIN_SIZE + " members).");
                    continue;
                }
                if (!ms.contains(leader)) {
                    if (log != null) log.warning("ExcelTogether: roster party " + pid + "'s stored leader is not a member; the longest-standing member leads instead.");
                    leader = ms.get(0);
                }
                RosterOrigin origin = RosterOrigin.parse(one.getString("origin"));
                if (origin == null) {
                    if (log != null) log.warning("ExcelTogether: roster party " + pid + " has an unknown origin tag ('"
                            + one.getString("origin") + "'); recording it as LOGIN_REBUILD.");
                    origin = RosterOrigin.LOGIN_REBUILD;
                }
                claimed.addAll(ms);
                parties.add(new PartyManager.RestoredParty(pid, leader, ms, origin));
            }
        }
        long nextId = yml.getLong("next-id", 1L);
        return new Loaded(true, true, parties, names, nextId, skipped);
    }

    private static Long idKey(String key) {
        try { return Long.parseLong(key.trim()); } catch (RuntimeException bad) { return null; }
    }
    private static UUID uuidOrNull(String s) {
        if (s == null) return null;
        try { return UUID.fromString(s.trim()); } catch (IllegalArgumentException bad) { return null; }
    }

    /** Persist the manager's current roster snapshot (write-through on mutation, and once more at disable). */
    public void save(File file, PartyManager mgr, Logger log) {
        if (file == null || mgr == null) return;
        Path target = file.toPath();
        Path tmp = null;
        try {
            YamlConfiguration yml = new YamlConfiguration();
            yml.set("version", FORMAT_VERSION);
            yml.set("next-id", mgr.nextId());
            ConfigurationSection nameSec = yml.createSection("names");
            for (Map.Entry<UUID, String> e : mgr.namesSnapshot().entrySet()) {
                nameSec.set(e.getKey().toString(), e.getValue());
            }
            ConfigurationSection partySec = yml.createSection("parties");
            for (PartyManager.RestoredParty p : mgr.snapshotAll()) {
                ConfigurationSection one = partySec.createSection(Long.toString(p.id));
                one.set("leader", p.leader.toString());
                one.set("origin", p.origin.name());
                List<String> ms = new ArrayList<>();
                for (UUID u : p.members) ms.add(u.toString());
                one.set("members", ms);
            }
            Path parent = target.toAbsolutePath().getParent();
            if (parent != null) Files.createDirectories(parent);
            tmp = Files.createTempFile(parent, ".roster-", ".tmp");
            yml.save(tmp.toFile());
            // File-level fsync is mandatory and legal everywhere; it is the directory sync that needs the guard.
            try (FileChannel channel = FileChannel.open(tmp, StandardOpenOption.WRITE)) { channel.force(true); }
            try {
                Files.move(tmp, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (java.nio.file.AtomicMoveNotSupportedException notAtomic) {
                Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
            }
            tmp = null;   // moved: nothing left to clean up
            syncDirectoryBestEffort(parent);
        } catch (Throwable t) {
            if (log != null) log.warning("ExcelTogether: could not write " + file.getName()
                    + " (" + t.getClass().getSimpleName() + ") - the roster holds in memory until the next save.");
        } finally {
            if (tmp != null) { try { Files.deleteIfExists(tmp); } catch (Throwable ignored) { } }
        }
    }

    /**
     * Best-effort directory durability sync (the fleet's settled pattern,
     * DirectorySync in the BuildMode fsync fix): exactly as durable as the
     * platform allows, skipped - never fatal - where the platform refuses
     * directory channels (Windows). The file itself was already forced.
     */
    static void syncDirectoryBestEffort(Path directory) {
        if (directory == null) return;
        try (FileChannel channel = FileChannel.open(directory, StandardOpenOption.READ)) { channel.force(true); }
        catch (Throwable refused) { /* platform refuses directory channels; the file-level sync already landed */ }
    }
}

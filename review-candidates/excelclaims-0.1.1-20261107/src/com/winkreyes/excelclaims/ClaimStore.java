package com.winkreyes.excelclaims;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * ExcelClaims — the claim store: in-memory index + flat-file
 * persistence. Claim files deliberately keep the old store's core
 * fields (corner strings, Owner, the four trust lists, Parent Claim
 * ID, inheritNothing) so the importer and exporter are near-copies
 * and a rollback export is lossless; ExcelClaims adds its own keys
 * alongside (Created At, Last Modified At, Inventory Only,
 * Subclaim Restricted, Chest Claim). PlayerData files keep the old
 * plain-text shape byte-for-byte; ExcelClaims-only player fields live
 * in player-extra.yml so the shared files never grow foreign lines.
 * Excel — Iris / Mike.
 */
public final class ClaimStore {

    private final Logger log;
    private final ClaimsConfig config;
    private File claimDir;
    private File playerDir;
    private File extraFile;

    private final Map<Long, ClaimRecord> claims = new LinkedHashMap<>();
    private final Map<UUID, PlayerRecord> players = new LinkedHashMap<>();
    private long nextClaimId = 0;

    public ClaimStore(Logger log, ClaimsConfig config) {
        this.log = log;
        this.config = config;
    }

    // ------------------------------------------------------------ loading

    public void load(File dataFolder) throws IOException {
        claimDir = new File(dataFolder, "ClaimData");
        playerDir = new File(dataFolder, "PlayerData");
        extraFile = new File(dataFolder, "player-extra.yml");
        if (!claimDir.isDirectory()) claimDir.mkdirs();
        if (!playerDir.isDirectory()) playerDir.mkdirs();

        claims.clear();
        File[] files = claimDir.listFiles((d, n) -> n.endsWith(".yml"));
        if (files != null) {
            java.util.Arrays.sort(files);
            for (File f : files) {
                try {
                    long id = Long.parseLong(f.getName().substring(0, f.getName().length() - 4));
                    ClaimRecord r = readClaimFile(f, id);
                    if (r != null) claims.put(id, r);
                } catch (NumberFormatException ignored) {
                    // not a claim file (e.g. _nextClaimID has no .yml twin)
                } catch (Exception e) {
                    log.warning("ExcelClaims: could not read claim file " + f.getName() + " (" + e + ") — skipped");
                }
            }
        }
        File next = new File(claimDir, "_nextClaimID");
        if (next.isFile()) {
            try {
                nextClaimId = Long.parseLong(Files.readString(next.toPath(), StandardCharsets.UTF_8).trim());
            } catch (Exception ignored) { }
        }
        long maxId = claims.keySet().stream().mapToLong(Long::longValue).max().orElse(-1L);
        if (nextClaimId <= maxId) nextClaimId = maxId + 1;

        players.clear();
        File[] pfiles = playerDir.listFiles();
        if (pfiles != null) {
            for (File f : pfiles) {
                try {
                    UUID id = UUID.fromString(f.getName());
                    int[] ab = readPlayerFile(f);
                    PlayerRecord pr = new PlayerRecord(id, ab[0], ab[1]);
                    players.put(id, pr);
                } catch (Exception ignored) {
                    // not a player file
                }
            }
        }
        if (extraFile.isFile()) {
            YamlConfiguration y = YamlConfiguration.loadConfiguration(extraFile);
            for (String key : y.getKeys(false)) {
                try {
                    UUID id = UUID.fromString(key);
                    PlayerRecord pr = players.get(id);
                    if (pr != null) {
                        pr.accruedLimitOverride = y.getInt(key + ".accrued-limit-override", -1);
                        pr.lastSeenAt = y.getLong(key + ".last-seen-at", 0L);
                    }
                } catch (IllegalArgumentException ignored) { }
            }
        }
    }

    static ClaimRecord readClaimFile(File f, long id) {
        YamlConfiguration y = YamlConfiguration.loadConfiguration(f);
        String lesser = y.getString("Lesser Boundary Corner");
        String greater = y.getString("Greater Boundary Corner");
        if (lesser == null || greater == null) return null;
        String[] lo = lesser.split(";");
        String[] hi = greater.split(";");
        if (lo.length < 4 || hi.length < 4) return null;
        ClaimRecord r = new ClaimRecord();
        r.id = id;
        r.world = lo[0];
        r.lx = Integer.parseInt(lo[1]); r.ly = Integer.parseInt(lo[2]); r.lz = Integer.parseInt(lo[3]);
        r.gx = Integer.parseInt(hi[1]); r.gy = Integer.parseInt(hi[2]); r.gz = Integer.parseInt(hi[3]);
        String owner = y.getString("Owner", "");
        r.owner = (owner == null || owner.isBlank()) ? null : UUID.fromString(owner.trim());
        r.builders.addAll(y.getStringList("Builders"));
        r.containers.addAll(y.getStringList("Containers"));
        r.accessors.addAll(y.getStringList("Accessors"));
        r.managers.addAll(y.getStringList("Managers"));
        r.inventoryOnly.addAll(y.getStringList("Inventory Only"));
        r.parentId = y.getLong("Parent Claim ID", -1L);
        r.inheritNothing = y.getBoolean("inheritNothing", false);
        r.subclaimRestricted = y.getBoolean("Subclaim Restricted", false);
        r.chestClaim = y.getBoolean("Chest Claim", false);
        r.createdAt = y.getLong("Created At", 0L);
        r.lastModifiedAt = y.getLong("Last Modified At", 0L);
        return r;
    }

    /** Old player-file shape: line 2 = accrued, line 3 = bonus (line 1 is a legacy blank). */
    static int[] readPlayerFile(File f) throws IOException {
        List<String> lines = Files.readAllLines(f.toPath(), StandardCharsets.UTF_8);
        List<Integer> nums = new ArrayList<>();
        for (String line : lines) {
            String t = line.trim();
            if (t.isEmpty()) continue;
            try { nums.add(Integer.parseInt(t)); } catch (NumberFormatException ignored) { }
        }
        int accrued = nums.size() > 0 ? nums.get(0) : 0;
        int bonus = nums.size() > 1 ? nums.get(1) : 0;
        return new int[]{accrued, bonus};
    }

    // ------------------------------------------------------------ saving

    public synchronized void saveClaim(ClaimRecord r) {
        YamlConfiguration y = new YamlConfiguration();
        y.set("Lesser Boundary Corner", r.world + ";" + r.lx + ";" + r.ly + ";" + r.lz);
        y.set("Greater Boundary Corner", r.world + ";" + r.gx + ";" + r.gy + ";" + r.gz);
        y.set("Owner", r.owner == null ? "" : r.owner.toString());
        y.set("Builders", new ArrayList<>(r.builders));
        y.set("Containers", new ArrayList<>(r.containers));
        y.set("Accessors", new ArrayList<>(r.accessors));
        y.set("Managers", new ArrayList<>(r.managers));
        y.set("Parent Claim ID", r.parentId);
        y.set("inheritNothing", r.inheritNothing);
        y.set("Inventory Only", new ArrayList<>(r.inventoryOnly));
        y.set("Subclaim Restricted", r.subclaimRestricted);
        y.set("Chest Claim", r.chestClaim);
        y.set("Created At", r.createdAt);
        y.set("Last Modified At", r.lastModifiedAt);
        try {
            y.save(new File(claimDir, r.id + ".yml"));
        } catch (IOException e) {
            log.warning("ExcelClaims: could not save claim " + r.id + " (" + e + ")");
        }
    }

    public synchronized void deleteClaimFile(ClaimRecord r) {
        File f = new File(claimDir, r.id + ".yml");
        if (f.exists() && !f.delete()) {
            log.warning("ExcelClaims: could not delete claim file " + f.getName());
        }
    }

    public synchronized void savePlayer(PlayerRecord pr) {
        try {
            Files.writeString(new File(playerDir, pr.id.toString()).toPath(),
                    "\n" + pr.accrued + "\n" + pr.bonus + "\n", StandardCharsets.UTF_8);
            YamlConfiguration y = extraFile.isFile()
                    ? YamlConfiguration.loadConfiguration(extraFile) : new YamlConfiguration();
            y.set(pr.id + ".accrued-limit-override", pr.accruedLimitOverride);
            y.set(pr.id + ".last-seen-at", pr.lastSeenAt);
            y.save(extraFile);
        } catch (IOException e) {
            log.warning("ExcelClaims: could not save player data " + pr.id + " (" + e + ")");
        }
    }

    private synchronized void saveNextId() {
        try {
            Files.writeString(new File(claimDir, "_nextClaimID").toPath(),
                    Long.toString(nextClaimId), StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.warning("ExcelClaims: could not save _nextClaimID (" + e + ")");
        }
    }

    // ------------------------------------------------------------ index

    public synchronized long allocateId() {
        long id = nextClaimId++;
        saveNextId();
        return id;
    }

    public synchronized void put(ClaimRecord r) { claims.put(r.id, r); }
    public synchronized ClaimRecord byId(long id) { return claims.get(id); }
    public synchronized ClaimRecord remove(long id) { return claims.remove(id); }
    public synchronized Collection<ClaimRecord> all() { return new ArrayList<>(claims.values()); }
    public synchronized int claimCount() { return claims.size(); }

    public synchronized List<ClaimRecord> childrenOf(long parentId) {
        List<ClaimRecord> out = new ArrayList<>();
        for (ClaimRecord r : claims.values()) if (r.parentId == parentId) out.add(r);
        return out;
    }

    /** Deepest claim whose column contains the point (subdivisions win over parents). */
    public synchronized ClaimRecord claimAt(String world, int x, int z) {
        ClaimRecord best = null;
        for (ClaimRecord r : claims.values()) {
            if (!r.world.equals(world) || !r.containsColumn(x, z)) continue;
            if (best == null || depth(r) > depth(best)) best = r;
        }
        return best;
    }

    private int depth(ClaimRecord r) {
        int d = 0;
        ClaimRecord c = r;
        while (c != null && c.parentId >= 0) { d++; c = claims.get(c.parentId); }
        return d;
    }

    public synchronized ClaimRecord topLevel(ClaimRecord r) {
        ClaimRecord c = r;
        while (c != null && c.parentId >= 0) {
            ClaimRecord p = claims.get(c.parentId);
            if (p == null) break;
            c = p;
        }
        return c;
    }

    public synchronized List<ClaimRecord> claimsOwnedBy(UUID owner) {
        List<ClaimRecord> out = new ArrayList<>();
        for (ClaimRecord r : claims.values()) {
            if (owner == null ? r.owner == null : owner.equals(r.owner)) out.add(r);
        }
        return out;
    }

    /** Area of TOP-LEVEL claims only — subdivisions carve up a parent and consume nothing. */
    public synchronized int usedArea(UUID owner) {
        int used = 0;
        for (ClaimRecord r : claims.values()) {
            if (r.parentId < 0 && owner != null && owner.equals(r.owner)
                    && config.claimsEnabled(r.world)) {
                used += r.area();
            }
        }
        return used;
    }

    public synchronized PlayerRecord playerData(UUID id) {
        PlayerRecord pr = players.get(id);
        if (pr == null) {
            pr = new PlayerRecord(id, config.initialBlocks, 0);
            players.put(id, pr);
        }
        return pr;
    }

    public synchronized PlayerRecord playerDataIfPresent(UUID id) { return players.get(id); }
    public synchronized Collection<PlayerRecord> allPlayers() { return new ArrayList<>(players.values()); }

    public synchronized int remainingBlocks(UUID id) {
        PlayerRecord pr = playerData(id);
        return pr.accrued + pr.bonus - usedArea(id);
    }

    public synchronized long nextClaimId() { return nextClaimId; }

    /**
     * Replace the whole store with imported data (the migration):
     * old claim/player files are removed, the imported records are
     * written out in our format, and the id counter continues from
     * the imported store's counter.
     */
    public synchronized void replaceAll(List<ClaimRecord> newClaims, List<PlayerRecord> newPlayers, long newNextId) {
        for (ClaimRecord r : claims.values()) deleteClaimFile(r);
        if (playerDir != null) {
            File[] old = playerDir.listFiles();
            if (old != null) for (File f : old) f.delete();
        }
        claims.clear();
        players.clear();
        for (ClaimRecord r : newClaims) { claims.put(r.id, r); saveClaim(r); }
        for (PlayerRecord pr : newPlayers) { players.put(pr.id, pr); savePlayer(pr); }
        nextClaimId = newNextId;
        saveNextId();
    }

    public ClaimsConfig config() { return config; }
    public File claimDir() { return claimDir; }
    public File playerDir() { return playerDir; }
}

// ExcelClaims — Iris / Mike, for Wink Reyes Valley

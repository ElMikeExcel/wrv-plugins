package com.winkreyes.excellandmarks.store;

import com.winkreyes.excellandmarks.core.Registry;
import com.winkreyes.excellandmarks.core.Spot;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Persistence (SPEC §9): spots.yml is the registry source of truth,
 * redemptions.yml holds redemption boxes + virtual holdings and is
 * NEVER pruned, registry-export.json is rewritten on every change in
 * the house export pattern for the website + Guide to consume later.
 */
public final class Store {

    /** One physical redemption box: chests in the world, owned by one player. */
    public static final class Box {
        public UUID owner;
        public String ownerName;
        public String spotId;
        public String world;
        public final List<int[]> chests = new ArrayList<>();
        public long createdMs;
        public boolean isChest(String w, int x, int y, int z) {
            if (!world.equals(w)) return false;
            for (int[] c : chests) if (c[0] == x && c[1] == y && c[2] == z) return true;
            return false;
        }
    }

    public static final class Virtual {
        public String ownerName;
        public final List<String> itemBytesB64 = new ArrayList<>();
    }

    private final File spotsFile;
    private final File redemptionsFile;
    private final File exportFile;
    private final Logger log;

    public final List<Box> boxes = new ArrayList<>();
    public final Map<UUID, Virtual> virtual = new LinkedHashMap<>();

    public Store(File dataFolder, Logger log) {
        this.spotsFile = new File(dataFolder, "spots.yml");
        this.redemptionsFile = new File(dataFolder, "redemptions.yml");
        this.exportFile = new File(dataFolder, "registry-export.json");
        this.log = log;
    }

    // ------------------------------------------------------------ spots

    public void loadSpots(Registry reg) {
        if (!spotsFile.exists()) return;
        YamlConfiguration y = YamlConfiguration.loadConfiguration(spotsFile);
        reg.restoreSeq(y.getInt("seq", 0));
        for (Map<?, ?> m : y.getMapList("spots")) {
            try {
                Spot s = new Spot(
                        str(m, "id"), str(m, "world"),
                        num(m, "x"), num(m, "y"), num(m, "z"),
                        str(m, "name"),
                        Spot.Type.valueOf(str(m, "type")),
                        Spot.Tier.valueOf(str(m, "tier")),
                        lng(m, "rent-cents"), lng(m, "buy-cents"),
                        num(m, "size"), lng(m, "created"));
                String owner = str(m, "owner");
                if (owner != null && !owner.isEmpty()) {
                    s.ownerId = UUID.fromString(owner);
                    s.ownerName = str(m, "owner-name");
                    s.mode = Spot.Mode.valueOf(str(m, "mode"));
                    s.paidThroughMs = lng(m, "paid-through");
                    String cid = str(m, "claim-id");
                    if (cid != null && !cid.isEmpty()) s.claimId = Long.parseLong(cid);
                }
                reg.add(s);
                String idNum = s.id.replaceAll("\\D+", "");
                if (!idNum.isEmpty()) reg.restoreSeq(Integer.parseInt(idNum));
            } catch (Throwable t) {
                log.warning("ExcelLandmarks: skipped an unreadable spot record (" + t + ")");
            }
        }
    }

    public void saveSpots(Registry reg) {
        YamlConfiguration y = new YamlConfiguration();
        List<Map<String, Object>> list = new ArrayList<>();
        for (Spot s : reg.all()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", s.id);
            m.put("world", s.world);
            m.put("x", s.x); m.put("y", s.y); m.put("z", s.z);
            m.put("name", s.name);
            m.put("type", s.type.name());
            m.put("tier", s.tier.name());
            m.put("rent-cents", s.rentCents);
            m.put("buy-cents", s.buyCents);
            m.put("size", s.size);
            m.put("created", s.createdMs);
            if (s.ownerId != null) {
                m.put("owner", s.ownerId.toString());
                m.put("owner-name", s.ownerName);
                m.put("mode", s.mode.name());
                m.put("paid-through", s.paidThroughMs);
                if (s.claimId != null) m.put("claim-id", s.claimId.toString());
            }
            list.add(m);
        }
        y.set("spots", list);
        try {
            spotsFile.getParentFile().mkdirs();
            y.save(spotsFile);
        } catch (Throwable t) {
            log.severe("ExcelLandmarks: could not save spots.yml (" + t + ")");
        }
        writeExport(reg);
    }

    // ------------------------------------------------------ redemptions

    public void loadRedemptions() {
        boxes.clear();
        virtual.clear();
        if (!redemptionsFile.exists()) return;
        YamlConfiguration y = YamlConfiguration.loadConfiguration(redemptionsFile);
        for (Map<?, ?> m : y.getMapList("boxes")) {
            try {
                Box b = new Box();
                b.owner = UUID.fromString(str(m, "owner"));
                b.ownerName = str(m, "owner-name");
                b.spotId = str(m, "spot");
                b.world = str(m, "world");
                b.createdMs = lng(m, "created");
                Object locs = m.get("chests");
                if (locs instanceof List<?> ls) {
                    for (Object o : ls) {
                        String[] p = String.valueOf(o).split(",");
                        if (p.length == 3) b.chests.add(new int[]{
                                Integer.parseInt(p[0].trim()), Integer.parseInt(p[1].trim()), Integer.parseInt(p[2].trim())});
                    }
                }
                boxes.add(b);
            } catch (Throwable t) {
                log.warning("ExcelLandmarks: skipped an unreadable redemption box (" + t + ")");
            }
        }
        if (y.isConfigurationSection("virtual")) {
            for (String key : y.getConfigurationSection("virtual").getKeys(false)) {
                try {
                    Virtual v = new Virtual();
                    v.ownerName = y.getString("virtual." + key + ".owner-name");
                    v.itemBytesB64.addAll(y.getStringList("virtual." + key + ".items"));
                    virtual.put(UUID.fromString(key), v);
                } catch (Throwable t) {
                    log.warning("ExcelLandmarks: skipped an unreadable virtual holding (" + t + ")");
                }
            }
        }
    }

    public void saveRedemptions() {
        YamlConfiguration y = new YamlConfiguration();
        List<Map<String, Object>> list = new ArrayList<>();
        for (Box b : boxes) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("owner", b.owner.toString());
            m.put("owner-name", b.ownerName);
            m.put("spot", b.spotId);
            m.put("world", b.world);
            m.put("created", b.createdMs);
            List<String> locs = new ArrayList<>();
            for (int[] c : b.chests) locs.add(c[0] + "," + c[1] + "," + c[2]);
            m.put("chests", locs);
            list.add(m);
        }
        y.set("boxes", list);
        for (Map.Entry<UUID, Virtual> e : virtual.entrySet()) {
            if (e.getValue().itemBytesB64.isEmpty()) continue;
            y.set("virtual." + e.getKey() + ".owner-name", e.getValue().ownerName);
            y.set("virtual." + e.getKey() + ".items", e.getValue().itemBytesB64);
        }
        try {
            redemptionsFile.getParentFile().mkdirs();
            y.save(redemptionsFile);
        } catch (Throwable t) {
            log.severe("ExcelLandmarks: could not save redemptions.yml (" + t + ")");
        }
    }

    public Box boxAt(String world, int x, int y, int z) {
        for (Box b : boxes) if (b.isChest(world, x, y, z)) return b;
        return null;
    }

    public static String encodeItem(ItemStack item) {
        return Base64.getEncoder().encodeToString(item.serializeAsBytes());
    }

    public static ItemStack decodeItem(String b64) {
        return ItemStack.deserializeBytes(Base64.getDecoder().decode(b64));
    }

    // ------------------------------------------------------------ export

    private void writeExport(Registry reg) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n  \"plugin\": \"ExcelLandmarks\",\n  \"spots\": [");
        boolean first = true;
        for (Spot s : reg.all()) {
            if (!first) sb.append(",");
            first = false;
            sb.append("\n    {\"id\": \"").append(esc(s.id))
                    .append("\", \"name\": \"").append(esc(s.name))
                    .append("\", \"type\": \"").append(s.type.name())
                    .append("\", \"tier\": \"").append(s.tier.name())
                    .append("\", \"world\": \"").append(esc(s.world))
                    .append("\", \"x\": ").append(s.x)
                    .append(", \"y\": ").append(s.y)
                    .append(", \"z\": ").append(s.z)
                    .append(", \"state\": \"").append(s.isAvailable() ? "AVAILABLE" : s.mode.name())
                    .append("\", \"owner\": ").append(s.ownerName == null ? "null" : "\"" + esc(s.ownerName) + "\"")
                    .append(", \"paidThrough\": ").append(s.paidThroughMs)
                    .append("}");
        }
        sb.append(first ? "]\n}\n" : "\n  ]\n}\n");
        try {
            exportFile.getParentFile().mkdirs();
            Files.writeString(exportFile.toPath(), sb.toString(), StandardCharsets.UTF_8);
        } catch (Throwable t) {
            log.warning("ExcelLandmarks: could not write registry-export.json (" + t + ")");
        }
    }

    private static String esc(String s) {
        return s == null ? "" : s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static String str(Map<?, ?> m, String k) {
        Object v = m.get(k);
        return v == null ? null : String.valueOf(v);
    }

    private static int num(Map<?, ?> m, String k) {
        Object v = m.get(k);
        return v instanceof Number n ? n.intValue() : 0;
    }

    private static long lng(Map<?, ?> m, String k) {
        Object v = m.get(k);
        return v instanceof Number n ? n.longValue() : 0L;
    }
}

package com.winkreyes.playermap;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.map.MapView;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;

/** Records vanilla IDs with proven activation/zoom ancestry. Never creates or changes a map. */
final class MapRegistry {
    record Entry(int id, UUID world, int scale, int centerX, int centerZ, int root) {}
    private final Path path;
    private final Map<Integer, Entry> entries = new HashMap<>();
    private boolean writeFailed;
    MapRegistry(Path path) { this.path = path; }
    void load() throws IOException, InvalidConfigurationException {
        if (!Files.exists(path)) return;
        YamlConfiguration yaml = new YamlConfiguration(); yaml.load(path.toFile());
        if (!Integer.valueOf(2).equals(yaml.get("format")) || !(yaml.get("maps") instanceof List<?> rows))
            throw new InvalidConfigurationException("Expected format 2 map registry. Preserve any older prototype registry separately; do not overwrite world map data.");
        Map<Integer, Entry> loaded = new HashMap<>();
        for (Object row : rows) {
            if (!(row instanceof Map<?, ?> raw)) throw new InvalidConfigurationException("Every map entry must be a mapping");
            try {
                Entry entry = new Entry(integer(raw,"id"), UUID.fromString(raw.get("world").toString()), integer(raw,"scale"), integer(raw,"center-x"), integer(raw,"center-z"), integer(raw,"root"));
                if (entry.id < 0 || entry.root < 0 || entry.scale < 0 || entry.scale > 4 || loaded.putIfAbsent(entry.id,entry) != null)
                    throw new IllegalArgumentException("Duplicate or invalid map entry");
            } catch (RuntimeException error) { throw new InvalidConfigurationException("Invalid map registry entry; refusing to claim map IDs", error); }
        }
        for (Entry entry : loaded.values()) {
            Entry root = loaded.get(entry.root);
            if (root == null || root.root != root.id || root.scale != 0 || root.centerX % 128 != 0 || root.centerZ % 128 != 0
                    || !root.world.equals(entry.world) || (entry.id != root.id && (entry.id <= root.id || entry.scale <= root.scale
                    || entry.centerX != MapGeometry.vanillaCenter(root.centerX,entry.scale)
                    || entry.centerZ != MapGeometry.vanillaCenter(root.centerZ,entry.scale))))
                throw new InvalidConfigurationException("Missing or impossible native map ancestry");
        }
        entries.clear(); entries.putAll(loaded);
    }
    private static int integer(Map<?,?> raw, String key) {
        Object value = raw.get(key);
        if (!(value instanceof Integer number)) throw new IllegalArgumentException("Expected integer " + key);
        return number;
    }
    Collection<Entry> entries() { return List.copyOf(entries.values()); }
    Entry entry(int id) { return entries.get(id); }
    boolean matches(MapView view) {
        Entry entry = entry(view.getId());
        return entry != null && !view.isLocked() && !view.isVirtual() && view.getWorld() != null
                && view.getWorld().getUID().equals(entry.world) && view.getScale().getValue() == entry.scale
                && view.getCenterX() == entry.centerX && view.getCenterZ() == entry.centerZ;
    }
    void trackInitial(MapView view) throws IOException {
        if (view.getWorld() == null || view.isLocked() || view.isVirtual() || view.getScale() != MapView.Scale.CLOSEST
                || view.getCenterX() % 128 != 0 || view.getCenterZ() % 128 != 0) throw new IOException("Not a newly activated unlocked native map");
        record(view,view.getId());
    }
    boolean trackZoom(MapView view, int rootId) throws IOException {
        if (matches(view)) return true;
        if (entry(view.getId()) != null) return false; // Changed registered metadata never replaces an old claim.
        Entry root = entry(rootId);
        if (root == null || root.root != root.id || view.getId() <= root.id || view.getWorld() == null || view.isLocked() || view.isVirtual()
                || !root.world.equals(view.getWorld().getUID()) || view.getScale().getValue() <= root.scale
                || view.getCenterX() != MapGeometry.vanillaCenter(root.centerX,view.getScale().getValue())
                || view.getCenterZ() != MapGeometry.vanillaCenter(root.centerZ,view.getScale().getValue())) return false;
        record(view,rootId); return true;
    }
    private void record(MapView view, int root) throws IOException {
        if (entry(view.getId()) != null) {
            if (!matches(view)) throw new IOException("Registered map changed; refusing to overwrite its identity");
            return;
        }
        if (writeFailed) throw new IOException("Map registry write failed; repair storage and restart");
        Entry entry = new Entry(view.getId(),view.getWorld().getUID(),view.getScale().getValue(),view.getCenterX(),view.getCenterZ(),root);
        entries.put(entry.id,entry);
        try { save(); } catch (IOException error) { entries.remove(entry.id); writeFailed = true; throw error; }
    }
    private void save() throws IOException {
        Files.createDirectories(path.getParent());
        YamlConfiguration yaml = new YamlConfiguration(); yaml.set("format",2);
        yaml.set("maps",entries.values().stream().sorted(Comparator.comparingInt(Entry::id)).map(entry -> {
            Map<String,Object> row = new LinkedHashMap<>(); row.put("id",entry.id); row.put("world",entry.world.toString());
            row.put("scale",entry.scale); row.put("center-x",entry.centerX); row.put("center-z",entry.centerZ); row.put("root",entry.root); return row;
        }).toList());
        Path temporary = Files.createTempFile(path.getParent(),"maps-",".tmp");
        try {
            Files.writeString(temporary,yaml.saveToString());
            try { Files.move(temporary,path,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING); }
            catch (AtomicMoveNotSupportedException ignored) { Files.move(temporary,path,StandardCopyOption.REPLACE_EXISTING); }
        } finally { Files.deleteIfExists(temporary); }
    }
}

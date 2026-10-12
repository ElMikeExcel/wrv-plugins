package com.winkreyes.playermap;

import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.map.MapView;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;

/** Only maps explicitly recorded by this plugin may receive its overlay. */
final class MapRegistry {
    record Key(UUID owner, UUID world, int scale) {
        Key { if (scale < 0 || scale > 4) throw new IllegalArgumentException("scale"); }
        String storageKey() { return owner + "." + world + "." + scale; }
    }
    record Entry(Key key, int id, int centerX, int centerZ) {}
    private final Server server;
    private final Path path;
    private final Map<Key, Entry> entries = new HashMap<>();
    private final Map<Integer, Entry> byId = new HashMap<>();
    private boolean writeFailed;
    MapRegistry(Server server, Path path) { this.server = server; this.path = path; }
    void load() throws IOException, InvalidConfigurationException {
        if (!Files.exists(path)) return;
        YamlConfiguration yaml = new YamlConfiguration(); yaml.load(path.toFile());
        if (!(yaml.get("maps") instanceof List<?>)) throw new InvalidConfigurationException("maps must be a list");
        for (Object row : (List<?>) yaml.get("maps")) {
            if (!(row instanceof Map<?, ?> raw)) throw new InvalidConfigurationException("Every maps entry must be a mapping");
            try {
                Key key = new Key(UUID.fromString(raw.get("owner").toString()), UUID.fromString(raw.get("world").toString()), ((Number) raw.get("scale")).intValue());
                Entry entry = new Entry(key, ((Number) raw.get("id")).intValue(), ((Number) raw.get("center-x")).intValue(), ((Number) raw.get("center-z")).intValue());
                if (entry.id < 0 || entries.containsKey(key) || byId.containsKey(entry.id)) throw new IllegalArgumentException("Duplicate/negative map entry");
                entries.put(key, entry); byId.put(entry.id, entry);
            } catch (RuntimeException error) { throw new InvalidConfigurationException("Invalid maps.yml entry; refusing to claim map IDs", error); }
        }
    }
    Collection<Entry> entries() { return List.copyOf(entries.values()); }
    Entry entry(int id) { return byId.get(id); }
    boolean matches(MapView view) {
        Entry entry = entry(view.getId());
        return entry != null && view.getWorld() != null && view.getWorld().getUID().equals(entry.key.world)
                && view.getScale().getValue() == entry.key.scale && view.getCenterX() == entry.centerX && view.getCenterZ() == entry.centerZ;
    }
    MapView getOrCreate(UUID owner, World world, int scale, double x, double z) throws IOException {
        Key key = new Key(owner, world.getUID(), scale);
        Entry old = entries.get(key);
        if (old != null) {
            MapView map = server.getMap(old.id);
            if (map == null || !matches(map)) throw new IOException("Stored map " + old.id + " is missing or changed; restore maps.yml together with world map data");
            return map;
        }
        if (writeFailed) throw new IOException("Map registry write failed earlier; repair storage and restart before allocating more maps");
        MapView map = server.createMap(world);
        map.setScale(MapView.Scale.values()[scale]);
        map.setCenterX(MapGeometry.vanillaCenter(x, scale)); map.setCenterZ(MapGeometry.vanillaCenter(z, scale));
        map.setTrackingPosition(false); // Draw own/player arrows in pixels, including Bedrock.
        map.setUnlimitedTracking(false);
        Entry entry = new Entry(key, map.getId(), map.getCenterX(), map.getCenterZ());
        entries.put(key, entry); byId.put(entry.id, entry);
        try { save(); }
        catch (IOException error) { entries.remove(key); byId.remove(entry.id); writeFailed = true; throw error; }
        return map;
    }
    private void save() throws IOException {
        Files.createDirectories(path.getParent());
        YamlConfiguration yaml = new YamlConfiguration();
        List<Map<String, Object>> rows = entries.values().stream().sorted(Comparator.comparingInt(Entry::id)).map(entry -> {
            Map<String, Object> row = new LinkedHashMap<>(); row.put("owner", entry.key.owner.toString()); row.put("world", entry.key.world.toString());
            row.put("scale", entry.key.scale); row.put("id", entry.id); row.put("center-x", entry.centerX); row.put("center-z", entry.centerZ); return row;
        }).toList();
        yaml.set("maps", rows);
        Path temporary = Files.createTempFile(path.getParent(), "maps-", ".tmp");
        try {
            Files.writeString(temporary, yaml.saveToString());
            try { Files.move(temporary, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
            catch (AtomicMoveNotSupportedException ignored) { Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING); }
        } finally { Files.deleteIfExists(temporary); }
    }
}

package com.winkreyes.excelwaypoints;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.function.Consumer;

/** Saved places in one small YAML file. Written atomically after every change. */
public final class PlaceStore {
    private final File file;
    private final Consumer<String> warn;
    private final Map<String, Place> byId = new LinkedHashMap<>();
    private final Random rnd = new Random();

    public PlaceStore(File file, Consumer<String> warn) { this.file = file; this.warn = warn; }

    public synchronized void load() {
        byId.clear();
        if (!file.exists()) return;
        YamlConfiguration y = new YamlConfiguration();
        try {
            y.load(file);
        } catch (Exception e) {
            File bak = new File(file.getParentFile(), file.getName() + ".corrupt-" + System.currentTimeMillis());
            try { Files.copy(file.toPath(), bak.toPath(), StandardCopyOption.REPLACE_EXISTING); } catch (IOException ignored) { }
            warn.accept("places.yml could not be read (" + e.getClass().getSimpleName() + "). Starting empty. Copy kept as " + bak.getName());
            return;
        }
        ConfigurationSection s = y.getConfigurationSection("places");
        if (s == null) return;
        int bad = 0;
        for (String id : s.getKeys(false)) {
            try {
                ConfigurationSection c = s.getConfigurationSection(id);
                if (c == null) throw new IllegalArgumentException("not a section");
                Place p = new Place(id, UUID.fromString(c.getString("owner", "")), c.getString("ownerName", "Someone"),
                        Names.clean(c.getString("name", "")), Place.Type.parse(c.getString("type")), c.getString("world", ""),
                        num(c, "x"), num(c, "y"), num(c, "z"), c.getBoolean("shareable"), c.getBoolean("allowVisit"), c.getLong("created"));
                if (p.name.isEmpty() || p.world.isEmpty()) throw new IllegalArgumentException("missing name or world");
                byId.put(id, p);
            } catch (Exception e) { bad++; }
        }
        if (bad > 0) warn.accept("Skipped " + bad + " unreadable place entries in places.yml");
    }

    private static double num(ConfigurationSection c, String k) {
        if (!c.isSet(k)) throw new IllegalArgumentException("missing " + k);
        double d = c.getDouble(k);
        if (Double.isNaN(d) || Double.isInfinite(d)) throw new IllegalArgumentException("bad " + k);
        return d;
    }

    public synchronized boolean save() {
        YamlConfiguration y = new YamlConfiguration();
        for (Place p : byId.values()) {
            String k = "places." + p.id + ".";
            y.set(k + "owner", p.owner.toString()); y.set(k + "ownerName", p.ownerName); y.set(k + "name", p.name);
            y.set(k + "type", p.type.name()); y.set(k + "world", p.world);
            y.set(k + "x", p.x); y.set(k + "y", p.y); y.set(k + "z", p.z);
            y.set(k + "shareable", p.shareable); y.set(k + "allowVisit", p.allowVisit); y.set(k + "created", p.created);
        }
        try {
            File dir = file.getParentFile();
            if (dir != null) dir.mkdirs();
            File tmp = new File(dir, file.getName() + ".tmp");
            y.save(tmp);
            try {
                Files.move(tmp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (java.nio.file.AtomicMoveNotSupportedException e) {
                Files.move(tmp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
            return true;
        } catch (IOException e) {
            warn.accept("Could not save places.yml: " + e.getMessage());
            return false;
        }
    }

    public synchronized String newId() {
        String id;
        do { id = Long.toString(java.util.concurrent.ThreadLocalRandom.current().nextLong(2176782336L), 36); while (id.length() < 6) id = "0" + id; } while (byId.containsKey(id));
        return id;
    }
    public synchronized void add(Place p) { byId.put(p.id, p); }
    public synchronized Place remove(String id) { return byId.remove(id); }
    public synchronized Place get(String id) { return id == null ? null : byId.get(id.toLowerCase(Locale.ROOT)); }
    public synchronized List<Place> all() { return new ArrayList<>(byId.values()); }
    public synchronized List<Place> ownedBy(UUID u) {
        List<Place> l = new ArrayList<>();
        for (Place p : byId.values()) if (p.owner.equals(u)) l.add(p);
        l.sort(Comparator.comparingLong((Place p) -> p.created).thenComparing(p -> p.id));
        return l;
    }
}

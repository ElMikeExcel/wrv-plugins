// ExcelSilkWatch — an Excel plugin by WinkReyesValley. (Excel signature — MikeExcel)
package com.winkreyes.silkstorage;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** Plugin-owned UUID choices; command, warning, Java menu and Bedrock menu share this store. */
public final class PreferenceStore {
    @FunctionalInterface interface AtomicWriter { void write(Path file, String text) throws IOException; }
    private final Path file;
    private final AtomicWriter writer;
    private Map<UUID, Boolean> choices = new HashMap<>();

    public PreferenceStore(Path file) throws IOException { this(file, PreferenceStore::writeAtomic); }

    // Package-private failure seam: writers must either atomically commit or throw before replacement.
    PreferenceStore(Path file, AtomicWriter writer) throws IOException {
        this.file = Objects.requireNonNull(file).toAbsolutePath().normalize();
        this.writer = Objects.requireNonNull(writer);
        load();
    }

    public synchronized boolean enabled(UUID id) {
        return choices.getOrDefault(Objects.requireNonNull(id), true);
    }

    /** Synchronous commit: caller must not announce success if this throws. */
    public synchronized void setEnabled(UUID id, boolean enabled) throws IOException {
        Objects.requireNonNull(id);
        if (choices.containsKey(id) && choices.get(id) == enabled) return;
        Map<UUID, Boolean> updated = new HashMap<>(choices);
        updated.put(id, enabled);
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.options().setHeader(List.of("ExcelSilkWatch player choices. Default ON; UUID keys survive reconnects.",
                "Do not edit while the server is running. format-version is required."));
        yaml.set("format-version", 1);
        yaml.createSection("players");
        updated.entrySet().stream().sorted(Map.Entry.comparingByKey())
                .forEach(entry -> yaml.set("players." + entry.getKey(), entry.getValue()));
        writer.write(file, yaml.saveToString());
        choices = updated;
    }

    private void load() throws IOException {
        if (Files.isSymbolicLink(file)) throw new IOException("Preference file must not be a symbolic link: " + file);
        final String text;
        try { text = Files.readString(file, StandardCharsets.UTF_8); }
        catch (NoSuchFileException missing) { return; }
        YamlConfiguration yaml = new YamlConfiguration();
        try {
            // Bukkit's convenience parser otherwise accepts duplicate UUID keys (last value wins).
            LoaderOptions options = new LoaderOptions();
            options.setAllowDuplicateKeys(false);
            options.setMaxAliasesForCollections(0);
            options.setNestingDepthLimit(8);
            new Yaml(new SafeConstructor(options)).load(text);
            yaml.loadFromString(text);
        }
        catch (InvalidConfigurationException | RuntimeException invalid) {
            throw new IOException("Invalid preference YAML; original file left unchanged: " + file, invalid);
        }
        if (!yaml.getKeys(false).equals(Set.of("format-version", "players")) ||
                !Integer.valueOf(1).equals(yaml.get("format-version")))
            throw new IOException("Unsupported preference schema; original file left unchanged: " + file);
        ConfigurationSection players = yaml.getConfigurationSection("players");
        if (players == null) throw new IOException("Preference players must be a UUID-to-boolean mapping");
        Map<UUID, Boolean> loaded = new HashMap<>();
        for (String key : players.getKeys(false)) {
            UUID id;
            try { id = UUID.fromString(key); }
            catch (IllegalArgumentException invalid) { throw new IOException("Invalid preference UUID: " + key, invalid); }
            if (!id.toString().equals(key) || !(players.get(key) instanceof Boolean value))
                throw new IOException("Invalid preference key/value: " + key);
            loaded.put(id, value);
        }
        choices = loaded;
    }

    private static void writeAtomic(Path file, String text) throws IOException {
        Path parent = file.getParent();
        Files.createDirectories(parent);
        if (Files.isSymbolicLink(file)) throw new IOException("Preference target must not be a symbolic link");
        Path temp = Files.createTempFile(parent, ".players-", ".tmp");
        try {
            try (FileChannel output = FileChannel.open(temp, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)) {
                ByteBuffer data = StandardCharsets.UTF_8.encode(text);
                while (data.hasRemaining()) output.write(data);
                output.force(true);
            }
            // No truncate-in-place fallback: an unsupported atomic move is a reported failure.
            Files.move(temp, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            // Cleanup cannot turn a completed atomic replacement into a reported failed save.
            try { Files.deleteIfExists(temp); } catch (IOException ignored) { }
        }
    }
}

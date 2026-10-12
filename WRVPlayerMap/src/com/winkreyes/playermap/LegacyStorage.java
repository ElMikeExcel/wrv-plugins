package com.winkreyes.playermap;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import java.io.IOException;
import java.nio.file.*;

/** One-time, non-destructive storage handoff from the old plugin name. */
final class LegacyStorage {
    static final String MARKER = ".excelplayermap-storage-v1";
    private static final String MARKER_CONTENT = "ExcelPlayerMap storage v1\n";
    enum Result { FRESH, MIGRATED, CURRENT }

    private LegacyStorage() {}

    static Result prepare(Path dataFolder) throws IOException, InvalidConfigurationException {
        Path current = dataFolder.toAbsolutePath().normalize();
        Path legacy = current.resolveSibling("WRVPlayerMap");
        if (current.equals(legacy)) throw new IOException("ExcelPlayerMap must use its own data folder");
        requireDirectoryIfPresent(current);

        // The marker belongs to the destination, so deleting maps.yml never reimports a stale backup.
        Path marker = current.resolve(MARKER);
        if (exists(marker)) {
            requireRegularFile(marker);
            if (!Files.readString(marker).equals(MARKER_CONTENT))
                throw new IOException("Unrecognized ExcelPlayerMap storage marker; refusing migration");
            validate(current);
            return Result.CURRENT;
        }

        requireDirectoryIfPresent(legacy);
        boolean hasLegacy = exists(legacy);
        if (exists(current) && !isEmpty(current)) {
            if (hasLegacy)
                throw new IOException("Both WRVPlayerMap and unmarked ExcelPlayerMap data exist. Preserve both and resolve the storage conflict before restarting");
            // An existing destination with no legacy source is safe to adopt without replacing it.
            validate(current);
            Files.writeString(marker, MARKER_CONTENT, StandardOpenOption.CREATE_NEW);
            return Result.CURRENT;
        }

        Files.createDirectories(current.getParent());
        Path staging = Files.createTempDirectory(current.getParent(), ".excelplayermap-migration-");
        try {
            if (hasLegacy) {
                copyIfPresent(legacy.resolve("config.yml"), staging.resolve("config.yml"));
                copyIfPresent(legacy.resolve("maps.yml"), staging.resolve("maps.yml"));
            }
            // Validate the actual copied bytes before making any destination data visible.
            validate(staging);
            Files.writeString(staging.resolve(MARKER), MARKER_CONTENT, StandardOpenOption.CREATE_NEW);
            if (exists(current)) Files.delete(current); // Only succeeds while the destination is still empty.
            try { Files.move(staging, current, StandardCopyOption.ATOMIC_MOVE); }
            catch (AtomicMoveNotSupportedException ignored) { Files.move(staging, current); }
            return hasLegacy ? Result.MIGRATED : Result.FRESH;
        } finally {
            // Only the private staging directory is ever removed. The legacy folder is read-only.
            if (exists(staging)) {
                Files.deleteIfExists(staging.resolve("config.yml"));
                Files.deleteIfExists(staging.resolve("maps.yml"));
                Files.deleteIfExists(staging.resolve(MARKER));
                Files.delete(staging);
            }
        }
    }

    private static void validate(Path folder) throws IOException, InvalidConfigurationException {
        Path config = folder.resolve("config.yml");
        if (exists(config)) {
            requireRegularFile(config);
            YamlConfiguration yaml = new YamlConfiguration();
            yaml.load(config.toFile());
            Object interval = yaml.get("update-interval-ticks");
            if (interval != null && !(interval instanceof Integer) && !(interval instanceof Long))
                throw new InvalidConfigurationException("update-interval-ticks must be an integer");
        }
        Path maps = folder.resolve("maps.yml");
        if (exists(maps)) {
            requireRegularFile(maps);
            new MapRegistry(maps).load();
        }
    }

    private static void copyIfPresent(Path source, Path destination) throws IOException {
        if (!exists(source)) return; // A legacy install may never have enrolled a map.
        requireRegularFile(source);
        Files.copy(source, destination, LinkOption.NOFOLLOW_LINKS);
    }

    private static void requireDirectoryIfPresent(Path path) throws IOException {
        if (exists(path) && !Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS))
            throw new IOException("Expected a real data directory, not a file or symbolic link: " + path);
    }

    private static void requireRegularFile(Path path) throws IOException {
        if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS))
            throw new IOException("Expected a regular storage file, not a directory or symbolic link: " + path);
    }

    private static boolean exists(Path path) { return Files.exists(path, LinkOption.NOFOLLOW_LINKS); }

    private static boolean isEmpty(Path directory) throws IOException {
        try (var files = Files.list(directory)) { return files.findAny().isEmpty(); }
    }
}

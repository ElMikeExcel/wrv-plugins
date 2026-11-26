package excelemfixes.questxp;

import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;

/**
 * Per-player quest-XP target store (D159), persisted as {@code targets.yml}
 * in the plugin data folder ({@code plugins/ExcelEMQuestXP/targets.yml}):
 *
 * <pre>
 * targets:
 *   7401bc22-...: WANDS
 * </pre>
 *
 * A player with no entry is in HIGHEST mode — the default, and exactly the
 * pre-1.1.0 behavior. Parsed and written with SnakeYAML directly, like the
 * quest table (Bukkit's loader mangles dotted keys; UUID keys are safe
 * either way, but the house pattern here is SnakeYAML throughout). Saved
 * on every change, so a crash can never lose a choice the player already
 * saw confirmed. Entries whose UUID does not parse, or whose skill is not
 * a current weapon skill, are dropped at load and counted in
 * {@link #skipped()} — that player simply falls back to HIGHEST.
 *
 * <p>No Bukkit types: load/save run against plain files so the store is
 * unit-testable on a plain JVM.
 */
public final class TargetStore {

    private final Map<UUID, String> pins = new HashMap<>();
    private int skipped;

    /** The pinned weapon-skill enum name for this player, or null = HIGHEST. */
    public synchronized String pinnedSkill(UUID uuid) {
        return pins.get(uuid);
    }

    public synchronized void setPin(UUID uuid, String skillEnumName) {
        pins.put(uuid, skillEnumName);
    }

    public synchronized void clearPin(UUID uuid) {
        pins.remove(uuid);
    }

    public synchronized int size() {
        return pins.size();
    }

    /** Entries dropped at load (bad UUID key or unknown skill name). */
    public int skipped() {
        return skipped;
    }

    /** Parse an already-loaded YAML root (null-safe). */
    public static TargetStore parse(Object yamlRoot, Set<String> validSkills) {
        TargetStore store = new TargetStore();
        if (yamlRoot instanceof Map<?, ?> root && root.get("targets") instanceof Map<?, ?> targets) {
            for (Map.Entry<?, ?> e : targets.entrySet()) {
                UUID uuid;
                try {
                    uuid = UUID.fromString(String.valueOf(e.getKey()));
                } catch (IllegalArgumentException badKey) {
                    store.skipped++;
                    continue;
                }
                String skill = e.getValue() == null ? null : String.valueOf(e.getValue());
                if (skill == null || !validSkills.contains(skill)) {
                    store.skipped++;
                    continue;
                }
                store.pins.put(uuid, skill);
            }
        }
        return store;
    }

    /** Load from disk; a missing file is an empty store, not an error. */
    public static TargetStore load(File file, Set<String> validSkills) throws IOException {
        if (!file.exists()) {
            return new TargetStore();
        }
        Object root;
        try (Reader reader = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)) {
            root = new Yaml().load(reader);
        }
        return parse(root, validSkills);
    }

    /** Write the whole store (sorted by UUID for a stable, diffable file). */
    public synchronized void save(File file) throws IOException {
        Map<String, String> targets = new TreeMap<>();
        for (Map.Entry<UUID, String> e : pins.entrySet()) {
            targets.put(e.getKey().toString(), e.getValue());
        }
        Map<String, Object> root = new TreeMap<>();
        root.put("targets", targets);
        String text = "# ExcelEMFixes quest-xp module — player quest-XP targets (D159).\n"
                + "# No entry = HIGHEST (auto): quest XP pays the player's highest weapon skill.\n"
                + "# Written by the plugin on every change; edit only while the server is stopped.\n"
                + new Yaml().dump(root);
        Files.writeString(file.toPath(), text, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }
}

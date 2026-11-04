package com.winkreyesvalley.excelhalloweentoggles;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * ExcelHalloweenToggles — the feature registry.
 *
 * Built once at enable from the companion config's {@code features:}
 * section. The registry is the single source of truth for which scares
 * exist, what they are called, and which Halloween event / config paths
 * drive them — the scheduler, the command, the store and the menu
 * placeholders all read from here, so nothing per-scare is hardcoded in
 * behaviour code (modularity law).
 *
 * Validation is strict on purpose: a malformed registry disables the
 * companion at enable with a named error rather than half-working.
 */
public final class FeatureRegistry {

    /** The Halloween 3.4.0 triggerEvent() names a companion may drive as
     *  ambient scares (read from the jar: the trigger dispatch handles
     *  pumpkin / slenderman / fire / lightning / darkness / herobrine;
     *  batwitch and the two bosses are deliberately NOT companion-driven). */
    public static final Set<String> KNOWN_AMBIENT_EVENTS =
            Set.of("pumpkin", "slenderman", "fire", "lightning", "darkness", "herobrine");

    private final List<FeatureDef> ordered;
    private final Map<String, FeatureDef> byKey;
    private final Map<String, FeatureDef> byEvent;
    private final FeatureDef loginFeature;

    private FeatureRegistry(List<FeatureDef> defs) {
        this.ordered = Collections.unmodifiableList(new ArrayList<>(defs));
        Map<String, FeatureDef> k = new LinkedHashMap<>();
        Map<String, FeatureDef> e = new LinkedHashMap<>();
        FeatureDef login = null;
        for (FeatureDef d : defs) {
            k.put(d.key(), d);
            if (d.isLogin()) {
                login = d;
            } else {
                e.put(d.halloweenEvent(), d);
            }
        }
        this.byKey = Collections.unmodifiableMap(k);
        this.byEvent = Collections.unmodifiableMap(e);
        this.loginFeature = login;
    }

    public List<FeatureDef> all() { return ordered; }

    public List<FeatureDef> ambient() {
        List<FeatureDef> out = new ArrayList<>();
        for (FeatureDef d : ordered) {
            if (!d.isLogin()) {
                out.add(d);
            }
        }
        return out;
    }

    public FeatureDef loginFeature() { return loginFeature; }

    public FeatureDef byKey(String key) {
        return key == null ? null : byKey.get(key.toLowerCase(java.util.Locale.ROOT));
    }

    /** Resolve a player-typed token: a toggle key, or the Halloween event
     *  name as an alias (e.g. "pumpkin" finds the pumpkin_curse toggle). */
    public FeatureDef resolve(String token) {
        if (token == null) {
            return null;
        }
        String t = token.toLowerCase(java.util.Locale.ROOT);
        FeatureDef d = byKey.get(t);
        if (d != null) {
            return d;
        }
        return byEvent.get(t);
    }

    public boolean knows(String key) { return byKey(key) != null; }

    /**
     * Build the registry from the companion config.
     *
     * @throws IllegalArgumentException naming the exact defect, on any
     *         malformed entry (unknown kind, duplicate key, ambient
     *         feature naming an event the Halloween plugin cannot fire,
     *         a second login feature, missing paths).
     */
    public static FeatureRegistry fromConfig(FileConfiguration config) {
        ConfigurationSection root = config.getConfigurationSection("features");
        if (root == null) {
            throw new IllegalArgumentException("config.yml has no 'features:' section");
        }
        List<FeatureDef> defs = new ArrayList<>();
        java.util.Set<String> seenKeys = new java.util.HashSet<>();
        java.util.Set<String> seenEvents = new java.util.HashSet<>();
        boolean seenLogin = false;
        for (String key : root.getKeys(false)) {
            if (!key.matches("[a-z0-9_]+")) {
                throw new IllegalArgumentException("feature key '" + key + "' must be lowercase [a-z0-9_]");
            }
            if (!seenKeys.add(key)) {
                throw new IllegalArgumentException("duplicate feature key '" + key + "'");
            }
            ConfigurationSection f = root.getConfigurationSection(key);
            if (f == null) {
                throw new IllegalArgumentException("feature '" + key + "' is not a section");
            }
            String display = f.getString("display", key);
            String kindRaw = f.getString("kind", "ambient");
            String enabledPath = f.getString("enabled-path");
            if (enabledPath == null || enabledPath.isBlank()) {
                throw new IllegalArgumentException("feature '" + key + "' has no enabled-path");
            }
            if (kindRaw.equalsIgnoreCase("login")) {
                if (seenLogin) {
                    throw new IllegalArgumentException("more than one login feature ('" + key + "')");
                }
                seenLogin = true;
                String cooldownPath = f.getString("cooldown-path");
                if (cooldownPath == null || cooldownPath.isBlank()) {
                    throw new IllegalArgumentException("login feature '" + key + "' has no cooldown-path");
                }
                int cd = f.getInt("fallback-cooldown-minutes", 30);
                defs.add(new FeatureDef(key, display, FeatureDef.Kind.LOGIN,
                        null, enabledPath, null, null, 0, 0, cooldownPath, cd));
            } else if (kindRaw.equalsIgnoreCase("ambient")) {
                String event = f.getString("event");
                if (event == null || !KNOWN_AMBIENT_EVENTS.contains(event)) {
                    throw new IllegalArgumentException("feature '" + key + "' names unknown halloween event '"
                            + event + "' (known: " + KNOWN_AMBIENT_EVENTS + ")");
                }
                if (!seenEvents.add(event)) {
                    throw new IllegalArgumentException("halloween event '" + event + "' claimed by two features");
                }
                String minPath = f.getString("min-path");
                String maxPath = f.getString("max-path");
                if (minPath == null || maxPath == null) {
                    throw new IllegalArgumentException("feature '" + key + "' lacks min-path/max-path");
                }
                int fbMin = f.getInt("fallback-min", 5);
                int fbMax = f.getInt("fallback-max", 20);
                if (fbMin < 0 || fbMax < fbMin) {
                    throw new IllegalArgumentException("feature '" + key + "' has bad fallback min/max");
                }
                defs.add(new FeatureDef(key, display, FeatureDef.Kind.AMBIENT,
                        event, enabledPath, minPath, maxPath, fbMin, fbMax, null, 0));
            } else {
                throw new IllegalArgumentException("feature '" + key + "' has unknown kind '" + kindRaw + "'");
            }
        }
        if (defs.isEmpty()) {
            throw new IllegalArgumentException("features: registry is empty");
        }
        return new FeatureRegistry(defs);
    }
}

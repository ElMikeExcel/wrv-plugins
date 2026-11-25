package excelemfixes.mount;

import excelemfixes.mount.core.QuestCensus;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The Ghast Charm's own settings, read from the summon-mount
 * config's {@code ghast} subsection with the shipped defaults.
 * Immutable; a reload builds a fresh instance. Module policy
 * the ghast shares with the horse (combat window, blocked
 * worlds, follow rule, retirement) stays in {@link MountSettings}
 * — this class carries only what is the ghast's own: its
 * prices, its loss cooldown, its entity dressing, and the
 * purchase gate (D204).
 *
 * <p>The gate's package rules are data, not code: each entry
 * names a content package and the quest-filename prefix(es)
 * that belong to it. The shipped config defines NO packages —
 * quest packages are a server's content, not the pack's — so
 * on a stock install the gate cannot pass until the owner
 * defines at least one package (the module logs exactly that
 * at enable). Wink Reyes Valley's deploy config defines the
 * Primis package (prefix {@code primis_}).
 */
public final class GhastSettings {

    private final boolean enabled;
    private final double price;
    private final double npcPrice;
    private final long cooldownMillis;
    private final long cooldownSeconds;
    private final String entityType;
    private final String harnessMaterial;
    private final double flyingSpeed;
    private final double spawnHeightOffset;
    private final boolean gateEnabled;
    private final double gateFraction;
    private final List<QuestCensus.PackageRule> gatePackages;

    private GhastSettings(boolean enabled, double price, double npcPrice,
                          long cooldownMillis, long cooldownSeconds,
                          String entityType, String harnessMaterial,
                          double flyingSpeed, double spawnHeightOffset,
                          boolean gateEnabled, double gateFraction,
                          List<QuestCensus.PackageRule> gatePackages) {
        this.enabled = enabled;
        this.price = price;
        this.npcPrice = npcPrice;
        this.cooldownMillis = cooldownMillis;
        this.cooldownSeconds = cooldownSeconds;
        this.entityType = entityType;
        this.harnessMaterial = harnessMaterial;
        this.flyingSpeed = flyingSpeed;
        this.spawnHeightOffset = spawnHeightOffset;
        this.gateEnabled = gateEnabled;
        this.gateFraction = gateFraction;
        this.gatePackages = gatePackages;
    }

    /** Read from the ghast subsection; null = all defaults. */
    public static GhastSettings from(ConfigurationSection cfg) {
        long cooldownSeconds = cfg == null ? 60L
                : cfg.getLong("lost-cooldown-seconds", 60);
        ConfigurationSection gate = cfg == null ? null
                : cfg.getConfigurationSection("gate");
        double fraction = gate == null ? 0.5
                : gate.getDouble("fraction", 0.5);
        if (fraction <= 0.0 || fraction > 1.0) fraction = 0.5;
        return new GhastSettings(
                cfg == null || cfg.getBoolean("enabled", true),
                cfg == null ? 30000.0 : cfg.getDouble("price", 30000.0),
                cfg == null ? 22500.0 : cfg.getDouble("npc-price", 22500.0),
                cooldownSeconds * 1000L,
                cooldownSeconds,
                cfg == null ? "HAPPY_GHAST"
                        : cfg.getString("entity-type", "HAPPY_GHAST"),
                cfg == null ? "RED_HARNESS"
                        : cfg.getString("harness-material", "RED_HARNESS"),
                cfg == null ? 0.0 : cfg.getDouble("flying-speed", 0.0),
                cfg == null ? 3.0 : cfg.getDouble("spawn-height-offset", 3.0),
                gate == null || gate.getBoolean("enabled", true),
                fraction,
                parsePackages(gate));
    }

    /** gate.packages: a list of {id, prefix} / {id, prefixes}
     *  maps. Malformed entries are skipped — the module's
     *  enable log reports the effective package set, so a typo
     *  is visible rather than silent. */
    private static List<QuestCensus.PackageRule> parsePackages(
            ConfigurationSection gate) {
        if (gate == null) return List.of();
        List<QuestCensus.PackageRule> out = new ArrayList<>();
        for (Map<?, ?> raw : gate.getMapList("packages")) {
            if (raw == null) continue;
            Object id = raw.get("id");
            if (id == null || id.toString().isBlank()) continue;
            List<String> prefixes = new ArrayList<>();
            Object single = raw.get("prefix");
            if (single != null && !single.toString().isBlank()) {
                prefixes.add(single.toString());
            }
            Object many = raw.get("prefixes");
            if (many instanceof List<?> list) {
                for (Object p : list) {
                    if (p != null && !p.toString().isBlank()) {
                        prefixes.add(p.toString());
                    }
                }
            }
            if (!prefixes.isEmpty()) {
                out.add(new QuestCensus.PackageRule(
                        id.toString(), List.copyOf(prefixes)));
            }
        }
        return List.copyOf(out);
    }

    public boolean enabled() { return enabled; }
    public double price() { return price; }
    public double npcPrice() { return npcPrice; }
    public long cooldownMillis() { return cooldownMillis; }
    public long cooldownSeconds() { return cooldownSeconds; }
    public String entityType() { return entityType; }
    public String harnessMaterial() { return harnessMaterial; }
    /** 0 = leave the entity's vanilla flying speed alone. */
    public double flyingSpeed() { return flyingSpeed; }
    public double spawnHeightOffset() { return spawnHeightOffset; }
    public boolean gateEnabled() { return gateEnabled; }
    public double gateFraction() { return gateFraction; }
    public List<QuestCensus.PackageRule> gatePackages() { return gatePackages; }
}

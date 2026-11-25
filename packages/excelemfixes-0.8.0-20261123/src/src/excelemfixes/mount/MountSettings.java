package excelemfixes.mount;

import org.bukkit.configuration.ConfigurationSection;

import java.util.List;

/**
 * The summon-mount module's settings, read from its config
 * section (modules.summon-mount) with the shipped defaults.
 * Immutable; a reload builds a fresh instance and the module
 * swaps it into the service and the buy flow.
 */
public final class MountSettings {

    private final boolean retired;
    private final String retirementMessage;
    private final double price;
    private final double npcPrice;
    private final long combatWindowMillis;
    private final long cooldownMillis;
    private final long cooldownSeconds;
    private final double horseSpeed;
    private final double horseJump;
    private final double horseHealth;
    private final List<String> blockedPrefixes;
    private final List<String> blockedNames;
    private final boolean followTeleport;
    private final double followDistanceSquared;

    private MountSettings(boolean retired, String retirementMessage,
                          double price, double npcPrice,
                          long combatWindowMillis,
                          long cooldownMillis, long cooldownSeconds,
                          double horseSpeed, double horseJump,
                          double horseHealth,
                          List<String> blockedPrefixes,
                          List<String> blockedNames,
                          boolean followTeleport,
                          double followDistanceSquared) {
        this.retired = retired;
        this.retirementMessage = retirementMessage;
        this.price = price;
        this.npcPrice = npcPrice;
        this.combatWindowMillis = combatWindowMillis;
        this.cooldownMillis = cooldownMillis;
        this.cooldownSeconds = cooldownSeconds;
        this.horseSpeed = horseSpeed;
        this.horseJump = horseJump;
        this.horseHealth = horseHealth;
        this.blockedPrefixes = blockedPrefixes;
        this.blockedNames = blockedNames;
        this.followTeleport = followTeleport;
        this.followDistanceSquared = followDistanceSquared;
    }

    /** Read from the module's config section; null = all defaults. */
    public static MountSettings from(ConfigurationSection cfg) {
        double followDistance = cfg == null ? 48.0
                : cfg.getDouble("follow-distance", 48.0);
        long cooldownSeconds = cfg == null ? 60L
                : cfg.getLong("lost-horse-cooldown-seconds", 60);
        return new MountSettings(
                cfg != null && cfg.getBoolean("retired", false),
                cfg == null ? "Mounts have arrived — the charm rests now."
                        + " Your horse answers a new call."
                        : cfg.getString("retirement-message",
                                "Mounts have arrived — the charm rests"
                                        + " now. Your horse answers a"
                                        + " new call."),
                cfg == null ? 7500.0 : cfg.getDouble("price", 7500.0),
                cfg == null ? 5625.0 : cfg.getDouble("npc-price", 5625.0),
                (cfg == null ? 20L
                        : cfg.getLong("combat-window-seconds", 20)) * 1000L,
                cooldownSeconds * 1000L,
                cooldownSeconds,
                cfg == null ? 0.29
                        : cfg.getDouble("horse.movement-speed", 0.29),
                cfg == null ? 0.85
                        : cfg.getDouble("horse.jump-strength", 0.85),
                cfg == null ? 24.0
                        : cfg.getDouble("horse.max-health", 24.0),
                cfg == null ? List.of("em_id_")
                        : cfg.getStringList("worlds.blocked-prefixes"),
                cfg == null ? List.of("em_adventurers_guild")
                        : cfg.getStringList("worlds.blocked-names"),
                cfg == null || cfg.getBoolean("follow-teleport", true),
                followDistance * followDistance);
    }

    public boolean retired() { return retired; }
    public String retirementMessage() { return retirementMessage; }
    public double price() { return price; }
    public double npcPrice() { return npcPrice; }
    public long combatWindowMillis() { return combatWindowMillis; }
    public long cooldownMillis() { return cooldownMillis; }
    public long cooldownSeconds() { return cooldownSeconds; }
    public double horseSpeed() { return horseSpeed; }
    public double horseJump() { return horseJump; }
    public double horseHealth() { return horseHealth; }
    public List<String> blockedPrefixes() { return blockedPrefixes; }
    public List<String> blockedNames() { return blockedNames; }
    public boolean followTeleport() { return followTeleport; }
    public double followDistanceSquared() { return followDistanceSquared; }
}

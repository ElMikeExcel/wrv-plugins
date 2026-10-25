package com.winkreyes.exceltrainingwheels;

import com.winkreyes.exceltrainingwheels.core.BedrockGrace;
import com.winkreyes.exceltrainingwheels.core.CombatDivisor;
import com.winkreyes.exceltrainingwheels.core.CompensationTable;
import com.winkreyes.exceltrainingwheels.core.CopperDoors;
import com.winkreyes.exceltrainingwheels.core.DoorGuardRule;
import com.winkreyes.exceltrainingwheels.core.LifestealRule;
import com.winkreyes.exceltrainingwheels.core.LogThrottle;
import com.winkreyes.exceltrainingwheels.core.NightHearing;
import com.winkreyes.exceltrainingwheels.core.ProtectedRegistry;
import com.winkreyes.exceltrainingwheels.core.RaidDivision;
import com.winkreyes.exceltrainingwheels.core.ShieldRule;
import com.winkreyes.exceltrainingwheels.core.SiegeBook;
import com.winkreyes.exceltrainingwheels.core.ZombieDoorRule;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.type.Door;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Enemy;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Villager;
import org.bukkit.entity.Zombie;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.event.entity.EntityBreakDoorEvent;
import org.bukkit.event.world.EntitiesLoadEvent;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.UUID;

/**
 * ExcelTrainingWheels — the Training Wheels Protocol (Mike's design,
 * approved Oct 19, 2026; named for Stark's suit protocol in
 * Spider-Man: Homecoming: limits in place until the wearer is
 * ready, and Mike holds the switch).
 *
 * Three pieces, one principle: PROTECTION FOLLOWS THE PLAYER.
 *  1. DAMAGE SHIELD — listed players take reduced damage from hostile
 *     mobs only (melee / mob-shot projectiles / creeper blasts),
 *     scaled at HIGH priority on the victim axis. Invisible: no
 *     messages, titles, particles or sounds, ever.
 *     0.1.1 adds SEAL COMPENSATION (Mike's absolute line): the
 *     factor holds against the mob's UNSCALED damage, so for
 *     ELITE damagers (EliteMobs gateway seam) the player's base
 *     factor is divided by the current ladder damage-dealt
 *     multiplier (seal count read reflectively from GuildGate,
 *     cached; config table mirrors the ladder). Non-elite mobs
 *     always use the flat base factor. See core/CompensationTable.
 *     0.1.2 adds RAID COMPENSATION (Mike's rule, Oct 21, 2026 —
 *     the same absolute line, against ExcelRaidBuff): a raid
 *     buff raises a raider's damage through its attack
 *     ATTRIBUTE, so on MELEE causes only (ENTITY_ATTACK /
 *     ENTITY_SWEEP_ATTACK) the effective factor is divided by
 *     the buff factor stored on the damager (reflective
 *     ExcelRaidBuffAPI read, mirroring the EliteMobs seam).
 *     The read is 1.0 for unbuffed damagers, for elites (never
 *     raid-buffed), and whenever the seam is unbound — the only
 *     states where 1.0 is correct, because there nothing is
 *     buffed. Projectiles and potions were never attribute-
 *     scaled and are never divided, or the protected player
 *     would drop BELOW the guaranteed quarter. See
 *     core/RaidDivision.
 *  2. DOOR GUARD — a mob breaking a door to get at a listed player
 *     is stopped (EntityBreakDoorEvent cancelled when the mob's
 *     target is protected). Every other break proceeds: veterans
 *     keep vanilla Hard's door drama.
 *  2b. DOOR TIERS — the better the door, the longer it buys you
 *     (Mike's ladder): wood = vanilla pace, copper = ~45s, iron =
 *     the 60s siege. Copper rides the same bespoke siege machinery
 *     as iron at a shorter threshold: vanilla NEVER completes a
 *     copper break on its own (the completion is suppressed in
 *     the door handler), so a copper door falls only to an
 *     accumulated ~45s of at-the-door hunting, cracks and banging
 *     included. (An earlier draft stretched vanilla instead —
 *     holding each completed break a counted number of times —
 *     but the rig proved a cancelled zombie does not reliably
 *     re-attempt, so the deterministic siege won; see BUILD.md.)
 *     Tiers are a property of the door, not the mob or its
 *     target — but the door guard above outranks them absolutely.
 *  2c. LIFESTEAL ON KILL (0.1.4, Mike Oct 24, 2026) — a listed
 *     player who kills a hostile mob regains 2 HP (an EliteMobs
 *     elite: 4 HP), capped at max health; never from players,
 *     passive mobs or spawner-cage mobs. See core/LifestealRule.
 *  3. IRON SIEGE — vanilla zombies never touch iron doors, so this
 *     is bespoke: a zombie-family mob hunting an UNPROTECTED player
 *     and path-blocked by an iron door besieges it (crack stages +
 *     banging as warning); at the threshold the door is destroyed
 *     with no drop. Sieges never start or continue against a
 *     protected target.
 *
 * The protected list lives in config.yml (UUID -> factor + name) so
 * it survives restarts; /trainingwheels edits persist immediately.
 */
public final class ExcelTrainingWheels extends JavaPlugin implements Listener {

    private boolean enabledMaster;
    private boolean debug;
    private double defaultFactor;
    // Seal compensation (0.1.1): config table + cached GuildGate state.
    private CompensationTable compTable = CompensationTable.of(CompensationTable.DEFAULT_DEALT);
    private boolean compEnabled;
    private int assumeSeal;
    private java.util.Set<String> compExcludedWorlds = java.util.Set.of();
    private volatile int cachedSeals;
    private volatile boolean sealCountKnown;
    private boolean sealStateLoggedOnce;
    // EliteMobs gateway seam (reflective; resolved lazily, guarded).
    private Method eliteCheckMethod;
    private boolean eliteSeamDead;
    private boolean eliteSeamLogged;
    // ExcelRaidBuff compensation seam (0.1.2; reflective; resolved
    // at enable for the boot-verify line, guarded on every read).
    private Method raidDivisorMethod;
    private boolean raidSeamDead;
    private boolean raidSeamLogged;
    private int siegeThresholdSeconds;
    private int siegeGraceSeconds;
    private int siegeMaxConcurrent;
    private int cadenceTicks;
    private ProtectedRegistry registry = new ProtectedRegistry();
    private SiegeBook siegeBook;
    private int copperTargetSeconds;
    private SiegeBook copperSiegeBook;
    private long tickCounter = 0L;
    private final Map<String, Location> siegeDoors = new HashMap<>();
    // All zombies break doors (0.1.3).
    private boolean allZombiesBreakDoors;
    private java.util.Set<String> breakDoorTypes = new java.util.LinkedHashSet<>(ZombieDoorRule.DEFAULT_TYPES);
    private static final NamespacedKey EM_ELITE_KEY = new NamespacedKey("elitemobs", "eliteentity");
    // Lifesteal on kill (0.1.4).
    private boolean lifestealEnabled;
    private double lifestealMobHp = LifestealRule.DEFAULT_MOB_HP;
    private double lifestealEliteHp = LifestealRule.DEFAULT_ELITE_HP;
    private boolean lifestealActionBar;
    // Door log throttle (0.1.5): one console line per door per window.
    private long doorLogSeconds = LogThrottle.DEFAULT_SECONDS;
    private LogThrottle doorLog = new LogThrottle(LogThrottle.DEFAULT_SECONDS * 1000L);
    // Live EliteMobs multiplier seam (0.1.6; reflective, lazily
    // resolved, guarded). source=live divides elite hits by the
    // multiplier EliteMobs actually used; source=table = 0.1.5.
    private boolean compUseLive = true;
    private Method emScaledMethod;
    private Method emNormalizedMethod;
    private Method emOtherMethod;
    private Method emTrackerMethod;
    private Method emIsScaledMethod;
    private Method emIsNormalizedMethod;
    private Class<?> emCustomBossClass;
    private boolean emLiveSeamDead;
    private boolean emLiveSeamLogged;
    private String lastMismatchKey = "";
    // Bedrock loading-screen grace window (0.1.6).
    private boolean graceEnabled;
    private long graceSeconds = BedrockGrace.DEFAULT_SECONDS;
    private boolean graceAllowLava;
    private boolean graceActionBar;
    private String graceMessage = "Loading in... protected";
    private BedrockGrace grace = new BedrockGrace(BedrockGrace.DEFAULT_SECONDS * 1000L);
    private Method floodgateIsPlayerMethod;
    private Object floodgateApi;
    private boolean floodgateSeamDead;
    // Night Hearing (0.1.7). cachedSealsRaw is the UNCLAMPED GuildGate
    // count (the compensation cache is clamped to the table size).
    private volatile int cachedSealsRaw;
    private boolean nhEnabled;
    private int nhMinSeals = NightHearing.DEFAULT_MIN_SEALS;
    private double nhRange = NightHearing.DEFAULT_RANGE;
    private boolean nhFootsteps;
    private java.util.Set<String> nhTypes = new java.util.LinkedHashSet<>(NightHearing.DEFAULT_TYPES);
    private int nhMaxPerNoise = NightHearing.DEFAULT_MAX_PER_NOISE;
    private int nhMaxPerPlayer = NightHearing.DEFAULT_MAX_PER_PLAYER;
    private long nhCooldownSeconds = NightHearing.DEFAULT_COOLDOWN_SECONDS;
    private long nhGiveUpSeconds = NightHearing.DEFAULT_GIVE_UP_SECONDS;
    private boolean nhBed = true;
    private java.util.Set<String> nhExcludedWorlds = java.util.Set.of();
    private List<String> nhExcludedPrefixes = List.of("em_");
    private boolean nhLogKeys;
    private NightHearing.Cooldown nhCooldown = new NightHearing.Cooldown(NightHearing.DEFAULT_COOLDOWN_SECONDS * 1000L);
    private NightHearing.Hunters nhHunters = new NightHearing.Hunters(NightHearing.DEFAULT_GIVE_UP_SECONDS * 1000L);
    private boolean nhSteerDoors = true;
    private double nhSteerRadius = NightHearing.DEFAULT_STEER_RADIUS;
    /** A heard zombie walking to (or standing at) the outside face of a door near the player. */
    private record Steer(UUID player, Block door, Location spot, NightHearing.DoorKind kind) {}
    private final Map<UUID, Steer> nhSteer = new HashMap<>();
    private boolean nhTipEnabled = true;
    private String nhTipTitle = "Night hearing";
    private String nhTipText = "The dead can hear you at night. Sneak, or sleep.";
    private static final String NH_TIP_ID = "night-hearing";
    private boolean nhTipRegistered;
    private boolean nhTipSeamLogged;
    private final Map<UUID, String> nhTipShown = new HashMap<>();
    private Method veilMethod;
    private Plugin veilOwner;
    // Wood doors off Hard (0.1.7 safety net): vanilla breaks wood only on Hard.
    private boolean woodFallback = true;
    private int woodFallbackSeconds = 12;
    private SiegeBook woodSiegeBook;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        readSettings();
        getServer().getPluginManager().registerEvents(this, this);
        Bukkit.getScheduler().runTaskTimer(this, this::siegeTick, cadenceTicks, cadenceTicks);
        getLogger().info("ExcelTrainingWheels enabled: shield " + (enabledMaster ? "ON" : "OFF (config)")
                + " (" + registry.size() + " protected), door guard ON, iron siege ON (threshold "
                + siegeThresholdSeconds + "s, cap " + siegeMaxConcurrent + "), door tiers "
                + (copperTargetSeconds > 0 ? "ON (copper " + copperTargetSeconds + "s)" : "OFF") + ".");
        int granted = sweepLoadedZombies();
        getLogger().info("all zombies break doors: " + (allZombiesBreakDoors ? "ON " + breakDoorTypes
                + " (EliteMobs elites left alone; " + granted + " loaded zombie(s) granted at enable)" : "OFF (vanilla spawn roll)"));
        logCompensationState();
        logRaidCompensationState();
        getLogger().info("lifesteal on kill: " + lifestealState());
        getLogger().info("door log throttle: " + doorLogState());
        getLogger().info("bedrock loading-screen grace: " + graceState());
        getLogger().info("night hearing: " + hearingState());
        Bukkit.getScheduler().runTaskTimer(this, this::hearingTick, 20L, 20L);
        // Slow seal-count refresh (join events refresh it too); the
        // cached value is all the damage handler ever reads.
        Bukkit.getScheduler().runTaskTimer(this, this::refreshSealCount, 600L, 600L);
    }

    // ---------- settings / registry ----------

    private void readSettings() {
        enabledMaster = getConfig().getBoolean("enabled", true);
        debug = getConfig().getBoolean("debug", false);
        defaultFactor = getConfig().getDouble("shield.default-factor", 0.5);
        if (!ProtectedRegistry.validFactor(defaultFactor)) {
            getLogger().warning("shield.default-factor " + defaultFactor + " is out of [0,1] - using 0.5");
            defaultFactor = 0.5;
        }
        siegeThresholdSeconds = Math.max(1, getConfig().getInt("siege.threshold-seconds", 60));
        siegeGraceSeconds = Math.max(0, getConfig().getInt("siege.grace-seconds", 3));
        siegeMaxConcurrent = Math.max(1, getConfig().getInt("siege.max-concurrent", 8));
        cadenceTicks = Math.max(1, getConfig().getInt("siege.cadence-ticks", 10));
        long steps = Math.max(1L, Math.round(siegeThresholdSeconds * 20.0 / cadenceTicks));
        siegeBook = new SiegeBook(siegeMaxConcurrent, steps, siegeGraceSeconds * 20L);
        copperTargetSeconds = Math.max(0, getConfig().getInt("doors.copper-target-seconds", 45));
        long copperSteps = Math.max(1L, Math.round(copperTargetSeconds * 20.0 / cadenceTicks));
        copperSiegeBook = new SiegeBook(siegeMaxConcurrent, copperSteps, siegeGraceSeconds * 20L);
        siegeDoors.clear();
        tickCounter = 0L;
        // 0.1.3: missing key -> default true (older live configs carry no key).
        allZombiesBreakDoors = getConfig().getBoolean("doors.all-zombies-break-doors", true);
        breakDoorTypes = ZombieDoorRule.parseTypes(getConfig().isList("doors.break-door-types")
                ? getConfig().getStringList("doors.break-door-types") : null);
        // 0.1.4: missing keys -> defaults (ON, 2 HP / 4 HP, action bar ON).
        lifestealEnabled = getConfig().getBoolean("lifesteal.enabled", true);
        lifestealMobHp = LifestealRule.sanitizeHp(
                getConfig().getDouble("lifesteal.mob-hp", LifestealRule.DEFAULT_MOB_HP), LifestealRule.DEFAULT_MOB_HP);
        lifestealEliteHp = LifestealRule.sanitizeHp(
                getConfig().getDouble("lifesteal.elite-hp", LifestealRule.DEFAULT_ELITE_HP), LifestealRule.DEFAULT_ELITE_HP);
        lifestealActionBar = getConfig().getBoolean("lifesteal.show-actionbar", true);
        // 0.1.5: missing key -> 300s. 0 = off (every door line logged, the 0.1.4 behavior).
        doorLogSeconds = LogThrottle.sanitizeSeconds(getConfig().getLong("log.door-throttle-seconds", LogThrottle.DEFAULT_SECONDS));
        doorLog = new LogThrottle(doorLogSeconds * 1000L);
        // 0.1.6: missing keys -> defaults (ON, 4 s, lava still applies, action bar ON).
        graceEnabled = getConfig().getBoolean("bedrock-grace.enabled", true);
        graceSeconds = BedrockGrace.sanitizeSeconds(getConfig().getLong("bedrock-grace.seconds", BedrockGrace.DEFAULT_SECONDS));
        graceAllowLava = getConfig().getBoolean("bedrock-grace.allow-lava", true);
        graceActionBar = getConfig().getBoolean("bedrock-grace.show-actionbar", true);
        graceMessage = getConfig().getString("bedrock-grace.actionbar-message", "Loading in... protected");
        grace = new BedrockGrace(graceSeconds * 1000L);
        // 0.1.6: missing key -> live.
        String source = getConfig().getString("shield.compensation.source", "live");
        compUseLive = !"table".equalsIgnoreCase(source == null ? "live" : source.trim());
        if (source != null && !"live".equalsIgnoreCase(source.trim()) && !"table".equalsIgnoreCase(source.trim())) {
            getLogger().warning("shield.compensation.source '" + source + "' is not live|table - using live");
        }
        lastMismatchKey = "";
        // 0.1.7 Night Hearing: missing keys -> defaults (feature ON but gated by the seal count).
        nhEnabled = getConfig().getBoolean("night-hearing.enabled", true);
        nhMinSeals = Math.max(0, getConfig().getInt("night-hearing.min-seals", NightHearing.DEFAULT_MIN_SEALS));
        nhRange = NightHearing.sanitizeRange(getConfig().getDouble("night-hearing.range", NightHearing.DEFAULT_RANGE));
        nhFootsteps = getConfig().getBoolean("night-hearing.hear-footsteps", false);
        nhTypes = NightHearing.parseTypes(getConfig().isList("night-hearing.types")
                ? getConfig().getStringList("night-hearing.types") : null);
        nhMaxPerNoise = NightHearing.sanitizeCount(getConfig().getInt("night-hearing.max-per-noise", NightHearing.DEFAULT_MAX_PER_NOISE), 10);
        nhMaxPerPlayer = NightHearing.sanitizeCount(getConfig().getInt("night-hearing.max-per-player", NightHearing.DEFAULT_MAX_PER_PLAYER), 20);
        nhCooldownSeconds = NightHearing.sanitizeSeconds(getConfig().getLong("night-hearing.cooldown-seconds", NightHearing.DEFAULT_COOLDOWN_SECONDS), 600L);
        nhGiveUpSeconds = Math.max(5L, NightHearing.sanitizeSeconds(getConfig().getLong("night-hearing.give-up-seconds", NightHearing.DEFAULT_GIVE_UP_SECONDS), 600L));
        nhBed = getConfig().getBoolean("night-hearing.bed-always-works", true);
        nhExcludedWorlds = new java.util.HashSet<>(getConfig().getStringList("night-hearing.excluded-worlds"));
        nhExcludedPrefixes = getConfig().isList("night-hearing.excluded-world-prefixes")
                ? getConfig().getStringList("night-hearing.excluded-world-prefixes") : List.of("em_");
        nhLogKeys = getConfig().getBoolean("night-hearing.log-event-keys", false);
        nhSteerDoors = getConfig().getBoolean("night-hearing.steer-to-doors", true);
        nhSteerRadius = NightHearing.sanitizeSteerRadius(getConfig().getDouble("night-hearing.steer-radius", NightHearing.DEFAULT_STEER_RADIUS));
        nhCooldown = new NightHearing.Cooldown(nhCooldownSeconds * 1000L);
        if (nhHunters != null) {
            for (UUID zid : nhHunters.releaseAll()) {
                Entity e = Bukkit.getEntity(zid);
                if (e instanceof Mob m) m.setTarget(null);
            }
        }
        clearSteers();
        nhHunters = new NightHearing.Hunters(nhGiveUpSeconds * 1000L);
        nhTipEnabled = getConfig().getBoolean("night-hearing.tip.enabled", true);
        nhTipTitle = getConfig().getString("night-hearing.tip.title", "Night hearing");
        nhTipText = getConfig().getString("night-hearing.tip.text", "The dead can hear you at night. Sneak, or sleep.");
        nhTipRegistered = false;
        woodFallback = getConfig().getBoolean("doors.wood-fallback-off-hard", true);
        woodFallbackSeconds = Math.max(1, getConfig().getInt("doors.wood-fallback-seconds", 12));
        long woodSteps = Math.max(1L, Math.round(woodFallbackSeconds * 20.0 / cadenceTicks));
        woodSiegeBook = new SiegeBook(siegeMaxConcurrent, woodSteps, siegeGraceSeconds * 20L);

        Map<String, String[]> raw = new LinkedHashMap<>();
        ConfigurationSection section = getConfig().getConfigurationSection("protected.players");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                Object factor = section.get(key + ".factor");
                Object name = section.get(key + ".name");
                raw.put(key, new String[]{
                        factor == null ? null : String.valueOf(factor),
                        name == null ? null : String.valueOf(name)});
            }
        }
        ProtectedRegistry.ParseResult parsed = ProtectedRegistry.fromRaw(raw);
        registry = parsed.registry();
        for (String error : parsed.errors()) getLogger().warning(error);

        // Seal compensation (0.1.1): table + fallback, then a fresh
        // seal read so a reload picks up a seal that just fell.
        compEnabled = getConfig().getBoolean("shield.compensation.enabled", true);
        Map<String, String> rawTable = new HashMap<>();
        ConfigurationSection tableSection = getConfig().getConfigurationSection("shield.compensation.dealt-multipliers");
        if (tableSection != null) {
            for (String key : tableSection.getKeys(false)) {
                Object value = tableSection.get(key);
                rawTable.put(key, value == null ? null : String.valueOf(value));
            }
        }
        CompensationTable.ParseResult parsedTable =
                CompensationTable.fromRaw(rawTable, CompensationTable.DEFAULT_DEALT);
        compTable = parsedTable.table();
        for (String error : parsedTable.errors()) getLogger().warning(error);
        int rawAssume = getConfig().getInt("shield.compensation.assume-seal", 0);
        assumeSeal = Math.max(0, Math.min(rawAssume, compTable.size() - 1));
        if (assumeSeal != rawAssume) {
            getLogger().warning("shield.compensation.assume-seal " + rawAssume
                    + " is outside the table (0.." + (compTable.size() - 1) + ") - using " + assumeSeal);
        }
        // Worlds where the flat factor governs (content there is
        // not ladder-scaled — the future EM guild world; see config).
        compExcludedWorlds = new java.util.HashSet<>(
                getConfig().getStringList("shield.compensation.excluded-worlds"));
        refreshSealCount();
    }

    // ---------- seal state (GuildGate reflective read, cached) ----------

    /**
     * Refresh the cached seal count from ExcelGuildGate's public API
     * (the same reflective read ExcelBedrockMenu uses). On any
     * failure the cache falls back to the config's assume-seal and
     * the transition is logged — the shield never silently runs on
     * a guessed-weaker state.
     */
    private void refreshSealCount() {
        Integer read = readSealsFromGuildGate();
        if (read != null) {
            cachedSealsRaw = Math.max(0, read);
            int seals = Math.max(0, Math.min(read, compTable.size() - 1));
            if (!sealCountKnown && sealStateLoggedOnce) {
                getLogger().info("shield compensation: GuildGate seal count reachable again (seal "
                        + seals + ")");
            }
            cachedSeals = seals;
            sealCountKnown = true;
        } else {
            if (sealCountKnown || !sealStateLoggedOnce) {
                getLogger().warning("shield compensation: GuildGate seal count unavailable - using assume-seal "
                        + assumeSeal + " (dealt x" + String.format("%.2f", compTable.dealtMultiplier(assumeSeal))
                        + ", elite factor " + String.format("%.4f", compTable.eliteFactor(defaultFactor, assumeSeal))
                        + " for base factor " + String.format("%.2f", defaultFactor) + ")");
            }
            cachedSeals = assumeSeal;
            cachedSealsRaw = 0;
            sealCountKnown = false;
        }
        sealStateLoggedOnce = true;
        checkTableAgainstLive();
    }

    /**
     * 0.1.6: warn (once per change) when the seal table row for the
     * current seal disagrees with the live EliteMobs ladder value.
     * With source=live the shield already follows EliteMobs; the
     * warning says the table (the fallback) needs the same edit.
     */
    private void checkTableAgainstLive() {
        if (!compEnabled) return;
        Double live = liveScaled();
        if (live == null) return;
        double row = compTable.dealtMultiplier(cachedSeals);
        String key = cachedSeals + "|" + String.format("%.3f", row) + "|" + String.format("%.3f", live);
        if (CombatDivisor.tableMatches(row, live)) {
            lastMismatchKey = "";
            return;
        }
        if (key.equals(lastMismatchKey)) return;
        lastMismatchKey = key;
        getLogger().warning("shield compensation: table row seal " + cachedSeals + " = x"
                + String.format("%.2f", row) + " but EliteMobs scaledDamageToPlayerMultiplier is x"
                + String.format("%.2f", live) + (compUseLive ? " - shield follows the live value; update the table (fallback) to match"
                : " - source=table: shield uses the TABLE value"));
    }

    /** GuildGateAPI.sealsBrokenCount() via the plugin's own classloader; null on any failure. */
    private Integer readSealsFromGuildGate() {
        try {
            Plugin guildGate = Bukkit.getPluginManager().getPlugin("ExcelGuildGate");
            if (guildGate == null || !guildGate.isEnabled()) return null;
            Class<?> apiClass = Class.forName("com.winkreyes.guildgate.api.GuildGateAPI",
                    true, guildGate.getClass().getClassLoader());
            Object count = apiClass.getMethod("sealsBrokenCount").invoke(null);
            return count instanceof Number number ? number.intValue() : null;
        } catch (Throwable t) {
            return null;
        }
    }

    /** The compensation state in force, for the enable log (packet verifies this line). */
    private void logCompensationState() {
        if (!compEnabled) {
            getLogger().info("shield compensation: OFF (config) - flat base factor against all damagers.");
            return;
        }
        getLogger().info("shield compensation: seal " + cachedSeals + ", elite factor "
                + String.format("%.4f", compTable.eliteFactor(defaultFactor, cachedSeals))
                + " (dealt x" + String.format("%.2f", compTable.dealtMultiplier(cachedSeals))
                + ", base factor " + String.format("%.2f", defaultFactor) + ", source: "
                + (sealCountKnown ? "GuildGate" : "assume-seal fallback") + ")");
        Double ls = liveScaled();
        Double ln = liveEmNamed("getNormalizedDamageToPlayerMultiplier");
        Double lo = liveEmNamed("getDamageToPlayerMultiplier");
        getLogger().info("shield compensation divisor source: " + (compUseLive ? "LIVE EliteMobs" : "TABLE (config)")
                + " - live scaled x" + fmt(ls) + ", normalized x" + fmt(ln) + ", other custom bosses x" + fmt(lo)
                + (ls == null ? " ('?' = EliteMobs not loaded yet; read per hit, so it binds by itself)" : "")
                + (compUseLive ? " (elite factor now " + (ls == null ? "table fallback" : String.format("%.4f",
                CombatDivisor.factor(defaultFactor, CombatDivisor.divisor(CombatDivisor.Path.SCALED, true, ls, null, null,
                        compTable.dealtMultiplier(cachedSeals))))) + " vs scaled elites)" : ""));
    }

    private static String fmt(Double v) {
        return v == null ? "?" : String.format("%.2f", v);
    }

    @EventHandler
    public void onJoinRefreshSeals(PlayerJoinEvent event) {
        refreshSealCount();
    }

    // ---------- EliteMobs gateway seam (elite detection) ----------

    /**
     * True when EliteMobs itself tags this entity as an elite
     * (PersistentTagger.isEliteEntity, resolved reflectively
     * against the EliteMobs classloader and cached). ANY failure —
     * plugin absent, class moved, invoke error — is treated as
     * NON-elite (the flat base factor then applies, exactly the
     * 0.1.0 behavior) and logged once; the seam never throws into
     * the damage handler.
     */
    private boolean isElite(Entity entity) {
        if (entity == null || eliteSeamDead) return false;
        try {
            Method method = eliteCheckMethod;
            if (method == null) {
                Plugin em = Bukkit.getPluginManager().getPlugin("EliteMobs");
                if (em == null || !em.isEnabled()) {
                    markEliteSeamDead("EliteMobs plugin not present");
                    return false;
                }
                Class<?> tagger = Class.forName("com.magmaguy.elitemobs.tagger.PersistentTagger",
                        true, em.getClass().getClassLoader());
                method = tagger.getMethod("isEliteEntity", Entity.class);
                eliteCheckMethod = method;
            }
            return Boolean.TRUE.equals(method.invoke(null, entity));
        } catch (Throwable t) {
            markEliteSeamDead("EliteMobs elite check failed: " + t);
            return false;
        }
    }

    private void markEliteSeamDead(String reason) {
        eliteSeamDead = true;
        if (!eliteSeamLogged) {
            eliteSeamLogged = true;
            getLogger().warning("shield compensation: elite detection unavailable (" + reason
                    + ") - elite damagers will be treated as non-elite (flat base factor)");
        }
    }

    // ---------- ExcelRaidBuff compensation seam (raid divisor) ----------

    /**
     * The raid-buff damage factor stored on this damager
     * (ExcelRaidBuffAPI.damageMultiplierOf, resolved reflectively
     * against the ExcelRaidBuff classloader and cached — the same
     * seam shape as the EliteMobs elite check above). ANY failure —
     * plugin absent, class moved, invoke error — returns 1.0 and
     * is logged once; the seam never throws into the damage
     * handler. 1.0 is the CORRECT divisor in every state where
     * the read can fail, because there nothing is buffed: the
     * plugin's absence means no raider carries a buff. The read
     * is also 1.0 for unbuffed damagers and for elites (never
     * raid-buffed), so their shield math is numerically today's.
     */
    private double raidDamageDivisor(Entity entity) {
        if (entity == null || raidSeamDead) return 1.0;
        try {
            Method method = raidDivisorMethod;
            if (method == null) {
                Plugin rb = Bukkit.getPluginManager().getPlugin("ExcelRaidBuff");
                if (rb == null || !rb.isEnabled()) {
                    markRaidSeamDead("ExcelRaidBuff plugin not present");
                    return 1.0;
                }
                Class<?> api = Class.forName("com.winkreyes.excelraidbuff.api.ExcelRaidBuffAPI",
                        true, rb.getClass().getClassLoader());
                method = api.getMethod("damageMultiplierOf", Entity.class);
                raidDivisorMethod = method;
            }
            Object value = method.invoke(null, entity);
            return value instanceof Number number ? RaidDivision.sanitize(number.doubleValue()) : 1.0;
        } catch (Throwable t) {
            markRaidSeamDead("ExcelRaidBuff damage read failed: " + t);
            return 1.0;
        }
    }

    private void markRaidSeamDead(String reason) {
        raidSeamDead = true;
        if (!raidSeamLogged) {
            raidSeamLogged = true;
            getLogger().warning("shield raid compensation: divisor read unavailable (" + reason
                    + ") - buffed-raider melee will NOT be divided (divisor 1.0)");
        }
    }

    /**
     * The raid seam state, for the enable log (the pair install's
     * boot-verify reads this line): BOUND when ExcelRaidBuff is
     * present and its API resolves NOW, UNBOUND when the plugin
     * is absent — the healthy standalone state, divisor 1.0 with
     * nothing buffed. A present-but-unresolvable API is routed
     * through markRaidSeamDead instead: its warning stands in for
     * this line, and the verify fails on the missing BOUND.
     */
    private void logRaidCompensationState() {
        Plugin rb = Bukkit.getPluginManager().getPlugin("ExcelRaidBuff");
        if (rb == null || !rb.isEnabled()) {
            getLogger().info("raid compensation: ExcelRaidBuff seam UNBOUND (plugin not present)"
                    + " - raid divisor 1.0; nothing is buffed, so the flat shield is exact.");
            return;
        }
        try {
            Class<?> api = Class.forName("com.winkreyes.excelraidbuff.api.ExcelRaidBuffAPI",
                    true, rb.getClass().getClassLoader());
            raidDivisorMethod = api.getMethod("damageMultiplierOf", Entity.class);
            getLogger().info("raid compensation: ExcelRaidBuff seam BOUND - buffed-raider melee"
                    + " is divided by the stored buff factor (the quarter holds at every omen level).");
        } catch (Throwable t) {
            markRaidSeamDead("ExcelRaidBuff API not resolvable at enable: " + t);
        }
    }

    private void persistRegistry() {
        getConfig().set("protected.players", null);
        for (Map.Entry<String, String[]> e : registry.toRaw().entrySet()) {
            getConfig().set("protected.players." + e.getKey() + ".factor",
                    Double.parseDouble(e.getValue()[0]));
            if (e.getValue()[1] != null) {
                getConfig().set("protected.players." + e.getKey() + ".name", e.getValue()[1]);
            }
        }
        saveConfig();
    }

    // ---------- 1. damage shield ----------

    private static ShieldRule.Cause mapCause(EntityDamageByEntityEvent event) {
        return switch (event.getCause()) {
            case ENTITY_ATTACK -> ShieldRule.Cause.ENTITY_ATTACK;
            case ENTITY_SWEEP_ATTACK -> ShieldRule.Cause.ENTITY_SWEEP_ATTACK;
            case PROJECTILE -> ShieldRule.Cause.PROJECTILE;
            case ENTITY_EXPLOSION -> ShieldRule.Cause.ENTITY_EXPLOSION;
            default -> ShieldRule.Cause.OTHER;
        };
    }

    /** HIGH: scale damage taken by a listed player from a hostile mob. Never cancels. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onShieldDamage(EntityDamageByEntityEvent event) {
        if (!enabledMaster) return;
        if (!(event.getEntity() instanceof Player victim)) return;
        OptionalDouble factor = registry.factor(victim.getUniqueId());
        if (factor.isEmpty()) return;
        if (event.getDamage() <= 0.0) return;
        Entity damager = event.getDamager();
        boolean damagerHostile = damager instanceof Enemy;
        boolean shooterHostile = false;
        // The damage SOURCE for elite detection: the melee attacker,
        // the creeper itself, or a projectile's shooter (never the
        // arrow). 0.1.0 early-exited at factor >= 1.0 before this
        // point; 0.1.1 exits on the EFFECTIVE factor below instead,
        // so a listed player at base 1.0 (graduated) still gets the
        // elite compensation — never more than the mob's normal
        // damage. Non-elite behavior is unchanged.
        Entity source = damager;
        if (damager instanceof Projectile projectile) {
            // Dispenser/block sources are not Entities -> never hostile.
            shooterHostile = projectile.getShooter() instanceof Entity shooter && shooter instanceof Enemy;
            if (projectile.getShooter() instanceof Entity shooter) source = shooter;
        }
        boolean damagerCreeper = damager instanceof Creeper;
        ShieldRule.Cause cause = mapCause(event);
        if (ShieldRule.shouldScale(cause, damagerHostile, shooterHostile, damagerCreeper)) {
            boolean elite = compEnabled
                    && !compExcludedWorlds.contains(victim.getWorld().getName())
                    && isElite(source);
            // 0.1.6: an elite's hit is divided by the multiplier
            // EliteMobs actually applied to it (scaled ladder for
            // natural elites, normalized for dungeon / instanced
            // bosses, damageToPlayer for other custom bosses), read
            // live; any seam failure -> the seal table (0.1.5).
            double divisor = 1.0;
            CombatDivisor.Path path = null;
            if (elite) {
                path = compUseLive ? combatPath(source) : null;
                divisor = CombatDivisor.divisor(path, compUseLive,
                        path == CombatDivisor.Path.SCALED ? liveEm(emScaledMethod) : null,
                        path == CombatDivisor.Path.NORMALIZED ? liveEm(emNormalizedMethod) : null,
                        path == CombatDivisor.Path.OTHER ? liveEm(emOtherMethod) : null,
                        compTable.dealtMultiplier(cachedSeals));
            }
            double effective = elite
                    ? CombatDivisor.factor(factor.getAsDouble(), divisor)
                    : factor.getAsDouble();
            // RAID COMPENSATION (0.1.2): on melee causes only,
            // divide by the damager's stored raid-buff factor so
            // the composed take stays base x VANILLA damage at
            // every omen level. Elites read 1.0 (never buffed),
            // unbuffed damagers read 1.0, an unbound seam reads
            // 1.0 — the division is then a no-op by construction.
            // Projectiles and explosions never reach this line
            // with a melee cause, so they are never divided. Runs
            // BEFORE the >= 1.0 exit for the same reason the elite
            // division does: a graduated (base 1.0) player still
            // gets the compensation against a buffed raider.
            double raidDivisor = 1.0;
            if (RaidDivision.isMelee(cause)) {
                raidDivisor = raidDamageDivisor(source);
                effective = RaidDivision.dividedFactor(effective, cause, raidDivisor);
            }
            if (effective >= 1.0) return;
            double before = event.getDamage();
            event.setDamage(ShieldRule.scaled(before, effective));
            if (debug) getLogger().info("SHIELD " + victim.getName() + ": " + event.getCause()
                    + " from " + damager.getType() + " " + before + " -> " + event.getDamage()
                    + " (factor " + effective + ", elite " + elite
                    + (elite ? " " + (path == null ? "TABLE" : path) + " /" + divisor : "")
                    + ", raid x" + raidDivisor
                    + ", seals " + cachedSeals + ")");
        }
    }

    // ---------- 1b. live EliteMobs multipliers (0.1.6) ----------

    /** Resolves the EliteMobs statics/methods once; false (and logs once) on any failure. */
    private boolean bindLiveSeam() {
        if (emLiveSeamDead) return false;
        if (emTrackerMethod != null) return true;
        try {
            Plugin em = Bukkit.getPluginManager().getPlugin("EliteMobs");
            if (em == null || !em.isEnabled()) {
                markLiveSeamDead("EliteMobs plugin not present");
                return false;
            }
            ClassLoader cl = em.getClass().getClassLoader();
            Class<?> mcs = Class.forName("com.magmaguy.elitemobs.config.MobCombatSettingsConfig", true, cl);
            emScaledMethod = mcs.getMethod("getScaledDamageToPlayerMultiplier");
            emNormalizedMethod = mcs.getMethod("getNormalizedDamageToPlayerMultiplier");
            emOtherMethod = mcs.getMethod("getDamageToPlayerMultiplier");
            Class<?> eliteEntity = Class.forName("com.magmaguy.elitemobs.mobconstructor.EliteEntity", true, cl);
            emIsScaledMethod = eliteEntity.getMethod("isScaledCombat");
            emCustomBossClass = Class.forName("com.magmaguy.elitemobs.mobconstructor.custombosses.CustomBossEntity", true, cl);
            emIsNormalizedMethod = emCustomBossClass.getMethod("isNormalizedCombat");
            Class<?> tracker = Class.forName("com.magmaguy.elitemobs.entitytracker.EntityTracker", true, cl);
            emTrackerMethod = tracker.getMethod("getEliteMobEntity", Entity.class);
            return true;
        } catch (Throwable t) {
            emTrackerMethod = null;
            markLiveSeamDead("EliteMobs live multiplier seam failed: " + t);
            return false;
        }
    }

    private void markLiveSeamDead(String reason) {
        emLiveSeamDead = true;
        if (!emLiveSeamLogged) {
            emLiveSeamLogged = true;
            getLogger().warning("shield compensation: live EliteMobs multipliers unavailable (" + reason
                    + ") - elite hits divide by the seal table (0.1.5 behavior)");
        }
    }

    /** Which multiplier EliteMobs used for this elite (mirrors EliteMobs 10.9.10 PDBEFilter). */
    private CombatDivisor.Path combatPath(Entity source) {
        if (source == null || !bindLiveSeam()) return CombatDivisor.Path.UNKNOWN;
        try {
            Object ee = emTrackerMethod.invoke(null, source);
            if (ee == null) return CombatDivisor.Path.UNKNOWN;
            if (Boolean.TRUE.equals(emIsScaledMethod.invoke(ee))) return CombatDivisor.Path.SCALED;
            if (emCustomBossClass.isInstance(ee) && Boolean.TRUE.equals(emIsNormalizedMethod.invoke(ee))) {
                return CombatDivisor.Path.NORMALIZED;
            }
            return CombatDivisor.Path.OTHER;
        } catch (Throwable t) {
            markLiveSeamDead("EliteMobs combat-path read failed: " + t);
            return CombatDivisor.Path.UNKNOWN;
        }
    }

    /** One live MobCombatSettings value; null when unreadable (the caller then uses the table). */
    private Double liveEm(Method m) {
        if (m == null || emLiveSeamDead) return null;
        try {
            Object v = m.invoke(null);
            return v instanceof Number n ? n.doubleValue() : null;
        } catch (Throwable t) {
            markLiveSeamDead("EliteMobs multiplier read failed: " + t);
            return null;
        }
    }

    /**
     * Live scaled ladder value, or null while unreadable. EliteMobs
     * reports 0.0 until its configs finish loading (seen on the
     * scratch boot, and briefly during /em reload); out-of-range
     * values count as unreadable, so the table governs meanwhile.
     */
    private Double liveScaled() {
        Double v = bindLiveSeam() ? liveEm(emScaledMethod) : null;
        return CombatDivisor.validLive(v) ? v : null;
    }

    private Double liveEmNamed(String getter) {
        if (!bindLiveSeam()) return null;
        Double v = switch (getter) {
            case "getNormalizedDamageToPlayerMultiplier" -> liveEm(emNormalizedMethod);
            case "getDamageToPlayerMultiplier" -> liveEm(emOtherMethod);
            default -> liveEm(emScaledMethod);
        };
        return CombatDivisor.validLive(v) ? v : null;
    }

    // ---------- 1c. Bedrock loading-screen grace window (0.1.6) ----------

    private String graceState() {
        if (!graceEnabled || graceSeconds <= 0) return "OFF";
        return "ON (" + graceSeconds + "s after join/respawn/world change, Bedrock only, VOID/KILL"
                + (graceAllowLava ? "/LAVA" : "") + " still apply, action bar " + (graceActionBar ? "ON" : "OFF")
                + ", detection " + (floodgateBound() ? "Floodgate API" : "Floodgate UUID scheme") + ")";
    }

    private boolean floodgateBound() {
        if (floodgateSeamDead) return false;
        if (floodgateIsPlayerMethod != null) return true;
        try {
            Plugin fg = Bukkit.getPluginManager().getPlugin("floodgate");
            if (fg == null || !fg.isEnabled()) return false; // may load later; retry next time
            Class<?> api = Class.forName("org.geysermc.floodgate.api.FloodgateApi", true, fg.getClass().getClassLoader());
            floodgateApi = api.getMethod("getInstance").invoke(null);
            floodgateIsPlayerMethod = api.getMethod("isFloodgatePlayer", UUID.class);
            return true;
        } catch (Throwable t) {
            floodgateSeamDead = true;
            getLogger().warning("bedrock grace: Floodgate API unavailable (" + t + ") - using the Floodgate UUID scheme");
            return false;
        }
    }

    /** Floodgate API when bound, else Floodgate's UUID scheme (most-significant bits 0). */
    private boolean isBedrock(Player player) {
        UUID id = player.getUniqueId();
        if (floodgateBound()) {
            try {
                return Boolean.TRUE.equals(floodgateIsPlayerMethod.invoke(floodgateApi, id));
            } catch (Throwable ignored) { /* fall through to the UUID scheme */ }
        }
        return BedrockGrace.looksLikeFloodgateUuid(id);
    }

    private void startGrace(Player player, String why) {
        if (!enabledMaster || !graceEnabled || graceSeconds <= 0) return;
        if (!isBedrock(player)) return;
        if (!grace.start(player.getUniqueId(), System.currentTimeMillis())) return;
        int dropped = 0;
        try {
            for (Entity e : player.getNearbyEntities(32, 16, 32)) {
                if (e instanceof Mob mob && player.equals(mob.getTarget())) {
                    mob.setTarget(null);
                    dropped++;
                }
            }
        } catch (Throwable ignored) { /* cosmetic only */ }
        if (graceActionBar && graceMessage != null && !graceMessage.isBlank()) {
            try {
                player.sendActionBar("\u00a77" + graceMessage);
            } catch (Throwable ignored) { /* cosmetic only */ }
        }
        if (debug) getLogger().info("BEDROCK GRACE " + player.getName() + ": " + why + " -> "
                + graceSeconds + "s" + (dropped > 0 ? ", " + dropped + " mob target(s) cleared" : ""));
    }

    private boolean inGrace(Player player) {
        return graceEnabled && grace.active(player.getUniqueId(), System.currentTimeMillis());
    }

    private void endGrace(Player player, String why) {
        if (grace.end(player.getUniqueId()) && debug) {
            getLogger().info("BEDROCK GRACE " + player.getName() + ": ended early (" + why + ")");
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onGraceJoin(PlayerJoinEvent event) {
        startGrace(event.getPlayer(), "join");
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onGraceRespawn(PlayerRespawnEvent event) {
        startGrace(event.getPlayer(), "respawn");
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onGraceWorldChange(PlayerChangedWorldEvent event) {
        startGrace(event.getPlayer(), "world change from " + event.getFrom().getName());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onGraceTeleport(PlayerTeleportEvent event) {
        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null || from.getWorld() == null || to.getWorld() == null) return;
        if (from.getWorld().equals(to.getWorld())) return;
        startGrace(event.getPlayer(), "cross-world teleport (" + event.getCause() + ")");
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onGraceQuit(PlayerQuitEvent event) {
        grace.end(event.getPlayer().getUniqueId());
    }

    /**
     * HIGHEST: the loading-screen hit is cancelled after every other
     * plugin has had its say (EliteMobs, the damage shield at HIGH).
     * Only cancels, never un-cancels; VOID/KILL/SUICIDE (and LAVA by
     * default) still land so a player can never get stuck.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onGraceDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!inGrace(player)) return;
        if (BedrockGrace.causeStillApplies(event.getCause().name(), graceAllowLava)) return;
        event.setCancelled(true);
        if (debug) getLogger().info("BEDROCK GRACE " + player.getName() + ": cancelled " + event.getCause()
                + " " + event.getDamage());
    }

    /** Mobs cannot pick a player in the window as a target. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onGraceTarget(EntityTargetLivingEntityEvent event) {
        if (event.getTarget() instanceof Player player && inGrace(player)) event.setCancelled(true);
    }

    /** The window ends the moment the player hits something (melee or own projectile). */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onGraceAttack(EntityDamageByEntityEvent event) {
        Entity damager = event.getDamager();
        Player attacker = null;
        if (damager instanceof Player p) attacker = p;
        else if (damager instanceof Projectile pr && pr.getShooter() instanceof Player p) attacker = p;
        if (attacker != null && attacker != event.getEntity()) endGrace(attacker, "attacked " + event.getEntity().getType());
    }

    /** ...or breaks a block. */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onGraceBreak(BlockBreakEvent event) {
        endGrace(event.getPlayer(), "broke " + event.getBlock().getType());
    }

    // ---------- 2. door guard ----------

    /**
     * HIGH (after GriefPrevention's default-priority handler). Two
     * decisions, in strict order — we only ever cancel here, never
     * un-cancel:
     *  1. DOOR GUARD (absolute precedence): the mob is hunting a
     *     protected player -> the break is cancelled outright and
     *     the tier book is never consulted; any door guards
     *     indefinitely for a protected target.
     *  2. COPPER TIER: the door is copper and the break is otherwise
     *     completing -> suppress it, always. Vanilla never fells a
     *     copper door; the copper siege in the ticker (same
     *     machinery as iron, ~45s threshold) is the only thing
     *     that does. Applies to any breaker and any target (a door
     *     is a door); an already-cancelled event is left alone.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onBreakDoor(EntityBreakDoorEvent event) {
        if (!enabledMaster) return;
        LivingEntity target = event.getEntity() instanceof Mob mob ? mob.getTarget() : null;
        boolean targetIsPlayer = target instanceof Player;
        boolean targetProtected = targetIsPlayer
                && registry.isProtected(((Player) target).getUniqueId());
        if (DoorGuardRule.shouldCancel(targetIsPlayer, targetProtected)) {
            event.setCancelled(true);
            if (debug) doorInfo("GUARD|" + doorKey(bottomHalf(event.getBlock())),
                    "DOOR GUARD: " + event.getEntity().getType() + " stopped at "
                    + event.getBlock().getLocation() + " (target " + target.getName() + " is protected)");
            return;
        }
        if (copperTargetSeconds <= 0 || event.isCancelled()) return;
        Block block = event.getBlock();
        if (!CopperDoors.isCopperDoor(block.getType().name())) return;
        event.setCancelled(true);
        if (debug) doorInfo("TIER|" + doorKey(bottomHalf(block)), "DOOR TIER: vanilla copper completion suppressed at "
                + fmt(block.getLocation()) + " (breaker " + event.getEntity().getType()
                + ") — the copper siege owns this door");
    }

    // ---------- 2b. all zombies break doors (0.1.3) ----------

    /** Elite check for the door grant: EliteMobs' PDC tag first (no seam), then the reflective seam. */
    private boolean isEliteForDoors(Entity entity) {
        try {
            if (entity.getPersistentDataContainer().has(EM_ELITE_KEY, PersistentDataType.STRING)) return true;
        } catch (Throwable ignored) { /* fall through to the seam */ }
        return isElite(entity);
    }

    /** Grant door-breaking if the rule says so. Returns true when granted. */
    private boolean maybeGrant(Entity entity) {
        if (!(entity instanceof Zombie zombie) || !zombie.isValid()) return false;
        if (!ZombieDoorRule.shouldGrant(enabledMaster && allZombiesBreakDoors, breakDoorTypes,
                zombie.getType().name(), false, zombie.canBreakDoors())) return false;
        if (isEliteForDoors(zombie)) return false;
        zombie.setCanBreakDoors(true);
        if (debug) doorInfo("GRANT", "DOORS: " + zombie.getType() + " at " + fmt(zombie.getLocation())
                + " granted door breaking");
        return true;
    }

    private int sweepLoadedZombies() {
        int n = 0;
        for (org.bukkit.World world : Bukkit.getWorlds()) {
            for (Zombie zombie : world.getEntitiesByClass(Zombie.class)) if (maybeGrant(zombie)) n++;
        }
        return n;
    }

    /**
     * Fresh spawns: decided 2 ticks later so EliteMobs has finished
     * tagging any elite conversion first (elites are left alone).
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onZombieSpawn(CreatureSpawnEvent event) {
        if (!(event.getEntity() instanceof Zombie zombie)) return;
        if (!enabledMaster || !allZombiesBreakDoors) return;
        Bukkit.getScheduler().runTaskLater(this, () -> maybeGrant(zombie), 2L);
    }

    /** Zombies already in the world (pre-0.1.3 spawns) as their chunks load. */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntitiesLoad(EntitiesLoadEvent event) {
        if (!enabledMaster || !allZombiesBreakDoors) return;
        for (Entity e : event.getEntities()) maybeGrant(e);
    }

    // ---------- 2c. lifesteal on kill (0.1.4) ----------

    private String lifestealState() {
        if (!lifestealEnabled) return "OFF (config)";
        return "ON (hostile +" + lifestealMobHp + " HP, elite +" + lifestealEliteHp
                + " HP, spawner mobs excluded, action bar " + (lifestealActionBar ? "ON" : "OFF") + ")";
    }

    /**
     * MONITOR, ignoreCancelled: the death is final by now (a plugin
     * that cancels the death, e.g. a revive, gets no heal paid out).
     * getKiller() is the player credited by vanilla: melee, the
     * player's own arrows/tridents, and other player-attributed
     * kills; null for environment/mob kills, so those pay nothing.
     * Read-only on the event; only the killer's health changes.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onLifestealKill(EntityDeathEvent event) {
        if (!enabledMaster || !lifestealEnabled) return;
        LivingEntity victim = event.getEntity();
        Player killer = victim.getKiller();
        if (killer == null || killer == victim) return;
        if (!registry.isProtected(killer.getUniqueId())) return;
        if (killer.isDead() || !killer.isValid()) return;
        boolean victimIsPlayer = victim instanceof Player;
        boolean hostile = victim instanceof Enemy;
        boolean elite = !victimIsPlayer && isEliteForDoors(victim);
        boolean fromSpawner = false;
        try {
            fromSpawner = victim.getEntitySpawnReason() == CreatureSpawnEvent.SpawnReason.SPAWNER;
        } catch (Throwable ignored) { /* API missing -> treat as natural */ }
        double heal = LifestealRule.healFor(true, true, victimIsPlayer, hostile, elite, fromSpawner,
                lifestealMobHp, lifestealEliteHp);
        if (heal <= 0.0) {
            if (debug && !victimIsPlayer && (hostile || elite)) {
                getLogger().info("LIFESTEAL " + killer.getName() + ": killed " + victim.getType()
                        + " - no heal (" + (fromSpawner ? "spawner mob" : "heal 0") + ")");
            }
            return;
        }
        double current = killer.getHealth();
        double max = maxHealth(killer);
        double gain = LifestealRule.cappedGain(current, max, heal);
        if (gain <= 0.0) {
            if (debug) getLogger().info("LIFESTEAL " + killer.getName() + ": killed " + victim.getType()
                    + (elite ? " (elite)" : "") + " - already at max " + current + "/" + max);
            return;
        }
        double next = Math.min(max, current + gain);
        try {
            killer.setHealth(next);
        } catch (Throwable t) {
            getLogger().warning("LIFESTEAL could not heal " + killer.getName() + ": " + t);
            return;
        }
        if (lifestealActionBar) {
            try {
                killer.sendActionBar("\u00a7a" + LifestealRule.heartsLabel(gain));
            } catch (Throwable ignored) { /* cosmetic only */ }
        }
        if (debug) getLogger().info("LIFESTEAL " + killer.getName() + ": killed " + victim.getType()
                + (elite ? " (elite)" : " (hostile)") + " +" + gain + " HP " + current + " -> " + next
                + " (max " + max + ", configured +" + heal + ")");
    }

    /** Max health from the attribute; 20.0 if the attribute cannot be read. */
    private static double maxHealth(Player player) {
        try {
            AttributeInstance inst = player.getAttribute(Attribute.MAX_HEALTH);
            if (inst != null && inst.getValue() > 0.0) return inst.getValue();
        } catch (Throwable ignored) { /* fall through */ }
        return 20.0;
    }

    /** The bottom half of a door block (a stable per-door identity). */
    private static Block bottomHalf(Block block) {
        if (block.getBlockData() instanceof Door doorData && doorData.getHalf() == Bisected.Half.TOP) {
            return block.getRelative(BlockFace.DOWN);
        }
        return block;
    }

    // ---------- 3. sieges: iron (60s) + the copper tier (45s) ----------

    private void siegeTick() {
        if (!enabledMaster || siegeBook == null) return;
        tickCounter += cadenceTicks;
        for (org.bukkit.World world : Bukkit.getWorlds()) {
            // Iron: zombie family hunting an unprotected player.
            for (Zombie zombie : world.getEntitiesByClass(Zombie.class)) {
                if (zombie.isDead()) continue;
                if (!(zombie.getTarget() instanceof Player target)) continue;
                if (registry.isProtected(target.getUniqueId())) continue; // never vs a protected target
                Block door = findDoor(zombie, target, mat -> mat == Material.IRON_DOOR);
                if (door == null) continue;
                observeSiege(siegeBook, false, zombie, target, door, world);
            }
            // 0.1.7 safety net: vanilla breaks wooden doors only on Hard.
            // Off Hard, the zombie family hunting an unprotected player
            // brings a wooden door down on this siege instead (same
            // machinery, doors.wood-fallback-seconds, no drop).
            if (woodSiegeBook != null && NightHearing.woodFallbackRuns(woodFallback, world.getDifficulty().name())) {
                for (Zombie zombie : world.getEntitiesByClass(Zombie.class)) {
                    if (zombie.isDead() || !zombie.canBreakDoors()) continue;
                    if (!(zombie.getTarget() instanceof Player target)) continue;
                    if (registry.isProtected(target.getUniqueId())) continue;
                    Block door = findDoor(zombie, target, mat -> org.bukkit.Tag.WOODEN_DOORS.isTagged(mat));
                    if (door == null) continue;
                    observeSiege(woodSiegeBook, false, zombie, target, door, world, "WOOD SIEGE", "wooden door");
                }
            }
            // Copper tier: any mob hunting an unprotected player or a
            // villager — tiers are a property of the door, not the mob.
            if (copperTargetSeconds > 0 && copperSiegeBook != null) {
                for (Mob mob : world.getEntitiesByClass(Mob.class)) {
                    if (mob.isDead()) continue;
                    LivingEntity target = mob.getTarget();
                    if (target == null) continue;
                    if (target instanceof Player p && registry.isProtected(p.getUniqueId())) continue;
                    if (!(target instanceof Player) && !(target instanceof Villager)) continue;
                    Block door = findDoor(mob, target, mat -> CopperDoors.isCopperDoor(mat.name()));
                    if (door == null) continue;
                    observeSiege(copperSiegeBook, true, mob, target, door, world);
                }
            }
        }
        if (!nhSteer.isEmpty()) steerSieges();
        sweepBook(siegeBook, "SIEGE");
        if (copperSiegeBook != null) sweepBook(copperSiegeBook, "COPPER SIEGE");
        if (woodSiegeBook != null) sweepBook(woodSiegeBook, "WOOD SIEGE");
    }

    private void sweepBook(SiegeBook book, String kind) {
        for (String key : book.sweep(tickCounter)) {
            clearCracks(key);
            siegeDoors.remove(key);
            if (debug) doorInfo(kind + " reset|" + key, kind + " stale-reset: " + key);
        }
    }

    /** Feed one observation to a siege book and render its effects. */
    private void observeSiege(SiegeBook book, boolean copper, LivingEntity breaker,
                              LivingEntity target, Block door, org.bukkit.World world) {
        observeSiege(book, copper, breaker, target, door, world,
                copper ? "COPPER SIEGE" : "SIEGE", copper ? "copper door" : "iron door");
    }

    private void observeSiege(SiegeBook book, boolean copper, LivingEntity breaker,
                              LivingEntity target, Block door, org.bukkit.World world,
                              String kind, String doorWord) {
        String key = doorKey(door);
        Location doorLoc = door.getLocation();
        int soundEverySteps = Math.max(1, 40 / cadenceTicks); // ~every 2 s of siege
        SiegeBook.Event ev = book.observe(key, breaker.getUniqueId(),
                target.getUniqueId(), false, tickCounter);
        switch (ev) {
            case STARTED -> {
                siegeDoors.put(key, doorLoc);
                renderCracks(key, book);
                doorInfo(kind + " started|" + key, kind + " started: " + breaker.getType() + " besieging " + doorWord
                        + " at " + fmt(doorLoc) + " (target " + targetName(target) + ")");
            }
            case PROGRESS -> {
                renderCracks(key, book);
                if (book.steps(key) % soundEverySteps == 0) {
                    world.playSound(doorLoc.clone().add(0.5, 1.0, 0.5),
                            Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, 1.0f, copper ? 1.15f : 0.95f);
                }
            }
            case COMPLETED -> {
                clearCracks(key);
                siegeDoors.remove(key);
                breakDoor(door, door.getType());
                world.playSound(doorLoc.clone().add(0.5, 1.0, 0.5),
                        Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, 1.0f, copper ? 0.75f : 0.55f);
                getLogger().info(kind + " complete: " + doorWord + " at " + fmt(doorLoc)
                        + " destroyed (target " + targetName(target) + ", no drop)");
            }
            case RESET -> {
                clearCracks(key);
                siegeDoors.remove(key);
                if (debug) doorInfo(kind + " reset|" + key, kind + " reset at " + fmt(doorLoc));
            }
            case IGNORED -> { /* busy door / cap reached */ }
        }
    }

    private String doorLogState() {
        return doorLogSeconds > 0 ? "ON (" + doorLogSeconds + "s per door)" : "OFF (config)";
    }

    /** Door console lines go through the throttle (0.1.5): one per key per window, repeats counted. */
    private void doorInfo(String key, String message) {
        long suppressed = doorLog.admit(key, System.nanoTime() / 1_000_000L);
        if (suppressed < 0) return;
        getLogger().info(message + LogThrottle.suffix(suppressed, doorLogSeconds));
    }

    private static String targetName(LivingEntity target) {
        return target instanceof Player p ? p.getName() : target.getType().name();
    }

    private static String fmt(Location loc) {
        return loc.getWorld().getName() + " " + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();
    }

    private static String doorKey(Block bottomHalf) {
        Location l = bottomHalf.getLocation();
        return l.getWorld().getName() + ";" + l.getBlockX() + ";" + l.getBlockY() + ";" + l.getBlockZ();
    }

    private static int crackId(String key) {
        return 1_500_000 + (key.hashCode() & 0x3FFFF) * 2;
    }

    /** Find the door of the given family this mob is path-blocked by, or null. Returns the BOTTOM half. */
    private Block findDoor(LivingEntity mob, LivingEntity target, java.util.function.Predicate<Material> family) {
        Location z = mob.getLocation();
        Location t = target.getLocation();
        Block best = null;
        double bestDist = Double.MAX_VALUE;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                for (int dy = 0; dy <= 1; dy++) {
                    Block b = z.getWorld().getBlockAt(z.getBlockX() + dx, z.getBlockY() + dy, z.getBlockZ() + dz);
                    if (!family.test(b.getType())) continue;
                    Location center = b.getLocation().add(0.5, 0.0, 0.5);
                    double dist = center.distance(z);
                    if (dist > 2.2) continue;
                    // Door must lie roughly between zombie and target,
                    // with the target just beyond it.
                    double dot = (center.getX() - z.getX()) * (t.getX() - z.getX())
                            + (center.getZ() - z.getZ()) * (t.getZ() - z.getZ());
                    if (dot <= 0.0) continue;
                    if (center.distance(t) > 10.0) continue;
                    if (dist < bestDist) {
                        bestDist = dist;
                        best = b;
                    }
                }
            }
        }
        if (best == null) return null;
        return bottomHalf(best);
    }

    private void renderCracks(String key, SiegeBook book) {
        Location bottom = siegeDoors.get(key);
        if (bottom == null) return;
        float progress = (float) book.progress(key);
        Location top = bottom.clone().add(0, 1, 0);
        for (Player p : bottom.getWorld().getPlayers()) {
            if (p.getLocation().distanceSquared(bottom) > 24.0 * 24.0) continue;
            p.sendBlockDamage(bottom, progress, crackId(key));
            p.sendBlockDamage(top, progress, crackId(key) + 1);
        }
    }

    private void clearCracks(String key) {
        Location bottom = siegeDoors.get(key);
        if (bottom == null) return;
        Location top = bottom.clone().add(0, 1, 0);
        for (Player p : bottom.getWorld().getPlayers()) {
            if (p.getLocation().distanceSquared(bottom) > 32.0 * 32.0) continue;
            p.sendBlockDamage(bottom, 0.0f, crackId(key));
            p.sendBlockDamage(top, 0.0f, crackId(key) + 1);
        }
    }

    /** Remove both halves with no drop (setType drops nothing; physics off so no pop-off). */
    private void breakDoor(Block bottomHalf, Material material) {
        Block top = bottomHalf.getRelative(BlockFace.UP);
        if (top.getType() == material) top.setType(Material.AIR, false);
        bottomHalf.setType(Material.AIR, false);
    }

    // ---------- 4. night hearing (0.1.7) ----------

    /** Is hearing live in this world right now (seal gate, night, overworld, not an EliteMobs world)? */
    private boolean hearingActive(org.bukkit.World world) {
        if (world == null) return false;
        boolean overworld = world.getEnvironment() == org.bukkit.World.Environment.NORMAL;
        boolean excluded = NightHearing.excludedWorld(world.getName(), nhExcludedWorlds, nhExcludedPrefixes);
        return NightHearing.active(nhEnabled, enabledMaster, sealCountKnown, cachedSealsRaw, nhMinSeals,
                overworld, excluded, world.getTime());
    }

    private String hearingState() {
        if (!nhEnabled) return "OFF (config)";
        return "ON from seal " + nhMinSeals + " (seal now " + (sealCountKnown ? String.valueOf(cachedSealsRaw) : "unknown - stays off")
                + ", " + (sealCountKnown && cachedSealsRaw >= nhMinSeals ? "ARMED" : "waiting") + "), night "
                + NightHearing.NIGHT_START + "-" + NightHearing.NIGHT_END + ", overworld, range " + fmt1(nhRange)
                + " (sneak " + fmt1(NightHearing.range(nhRange, true)) + "), footsteps " + (nhFootsteps ? "ON" : "OFF")
                + ", types " + nhTypes + ", max " + nhMaxPerNoise + "/noise " + nhMaxPerPlayer + "/player, cooldown "
                + nhCooldownSeconds + "s, give up " + nhGiveUpSeconds + "s, bed " + (nhBed ? "ALWAYS OPENS" : "vanilla")
                + ", steer to doors " + (nhSteerDoors ? "ON (" + fmt1(nhSteerRadius) + " blocks)" : "OFF")
                + ", wood fallback off Hard " + (woodFallback ? "ON (" + woodFallbackSeconds + "s)" : "OFF");
    }

    private static String fmt1(double v) {
        return String.format("%.1f", v);
    }

    /** ExcelMobVeil's public isVeiled(Player), reflective; false on any failure. */
    private boolean isVeiled(Player player) {
        try {
            Plugin veil = Bukkit.getPluginManager().getPlugin("ExcelMobVeil");
            if (veil == null || !veil.isEnabled()) return false;
            if (veilMethod == null || veilOwner != veil) {
                veilMethod = veil.getClass().getMethod("isVeiled", Player.class);
                veilOwner = veil;
            }
            return Boolean.TRUE.equals(veilMethod.invoke(veil, player));
        } catch (Throwable t) {
            return false;
        }
    }

    private boolean hearable(Player player) {
        return NightHearing.playerHearable(player.getGameMode().name(),
                registry.isProtected(player.getUniqueId()), inGrace(player), isVeiled(player),
                player.isSleeping(), player.isDead());
    }

    /** Zombie-family mobs (hearing types) currently hunting this player nearby. */
    private int huntersOf(Player player) {
        int n = 0;
        for (Zombie z : player.getWorld().getNearbyEntitiesByType(Zombie.class, player.getLocation(), 64.0)) {
            if (!z.isDead() && player.equals(z.getTarget()) && nhTypes.contains(z.getType().name())) n++;
        }
        for (Steer st : nhSteer.values()) if (st.player().equals(player.getUniqueId())) n++;
        return n;
    }

    /** A noise by this player at this spot: pull idle zombies in range. */
    private void hear(Player player, Location where, String what) {
        if (where == null || where.getWorld() == null || !where.getWorld().equals(player.getWorld())) return;
        if (!hearingActive(where.getWorld())) return;
        if (!hearable(player)) return;
        long now = System.currentTimeMillis();
        if (!nhCooldown.ready(player.getUniqueId(), now)) return;
        double r = NightHearing.range(nhRange, player.isSneaking());
        java.util.List<Zombie> candidates = new java.util.ArrayList<>();
        for (Zombie z : where.getWorld().getNearbyEntitiesByType(Zombie.class, where, r)) {
            if (z.isDead() || !z.isValid() || nhSteer.containsKey(z.getUniqueId())) continue;
            if (z.getLocation().distanceSquared(where) > r * r) continue;
            if (!NightHearing.zombieEligible(nhTypes, z.getType().name(), z.getTarget() != null, isEliteForDoors(z))) continue;
            candidates.add(z);
        }
        if (candidates.isEmpty()) {
            if (debug) getLogger().info("NIGHT HEARING: " + player.getName() + " " + what + " (range " + fmt1(r) + ") - no idle zombie in range");
            return;
        }
        candidates.sort(java.util.Comparator.comparingDouble(z -> z.getLocation().distanceSquared(where)));
        int want = NightHearing.pullCount(candidates.size(), huntersOf(player), nhMaxPerNoise, nhMaxPerPlayer);
        int pulled = 0;
        int steered = 0;
        java.util.List<Block> doors = null;
        for (Zombie z : candidates) {
            if (pulled >= want) break;
            z.setTarget(player); // fires EntityTargetEvent: grace / MobVeil / other plugins can still say no
            if (!player.equals(z.getTarget())) continue;
            nhHunters.pull(z.getUniqueId(), player.getUniqueId(), now);
            pulled++;
            if (!nhSteerDoors) continue;
            if (doors == null) doors = doorsNear(player.getLocation());
            Steer st = planSteer(z, player, doors);
            if (st == null) continue; // a walkable way in (or no usable door): vanilla takes it from here
            nhSteer.put(z.getUniqueId(), st);
            z.setTarget(null);
            z.getPathfinder().moveTo(st.spot(), 1.0);
            steered++;
            if (debug) getLogger().info("NIGHT HEARING: " + z.getType() + " has no way in to " + player.getName()
                    + ", steered to the " + st.kind().name().toLowerCase(java.util.Locale.ROOT) + " door at " + fmt(st.door().getLocation()));
        }
        if (pulled > 0) nhCooldown.start(player.getUniqueId(), now);
        if (debug || pulled > 0) {
            String line = "NIGHT HEARING: " + what + " by " + player.getName() + " at " + fmt(where)
                    + " (range " + fmt1(r) + (player.isSneaking() ? ", sneaking" : "") + ") pulled " + pulled
                    + " zombie(s)" + (steered > 0 ? " (" + steered + " sent to a door)" : "");
            if (debug) getLogger().info(line);
            else doorInfo("HEAR|" + player.getUniqueId(), line);
        }
    }

    /** Closed doors (bottom halves) we can steer to, within the steer radius of the player, nearest first, max 6. */
    private java.util.List<Block> doorsNear(Location at) {
        java.util.List<Block> out = new java.util.ArrayList<>();
        int r = (int) Math.ceil(nhSteerRadius);
        org.bukkit.World w = at.getWorld();
        int bx = at.getBlockX(), by = at.getBlockY(), bz = at.getBlockZ();
        boolean copperOn = copperTargetSeconds > 0 && copperSiegeBook != null;
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                if (dx * dx + dz * dz > r * r) continue;
                for (int dy = -3; dy <= 3; dy++) {
                    int y = by + dy;
                    if (y < w.getMinHeight() || y >= w.getMaxHeight()) continue;
                    if (!w.isChunkLoaded((bx + dx) >> 4, (bz + dz) >> 4)) continue;
                    Block b = w.getBlockAt(bx + dx, y, bz + dz);
                    if (NightHearing.steerKind(b.getType().name(), copperOn) == null) continue;
                    if (!(b.getBlockData() instanceof Door d) || d.getHalf() != Bisected.Half.BOTTOM || d.isOpen()) continue;
                    out.add(b);
                }
            }
        }
        out.sort(java.util.Comparator.comparingDouble(b -> b.getLocation().add(0.5, 0, 0.5).distanceSquared(at)));
        return out.size() > 6 ? new java.util.ArrayList<>(out.subList(0, 6)) : out;
    }

    /**
     * No walkable way to the player? Pick the door whose outside face the
     * zombie can walk to, preferring a short walk and a door close to the
     * player. Null = let vanilla handle it (it can get in, or no usable door).
     */
    private Steer planSteer(Zombie z, Player player, java.util.List<Block> doors) {
        try {
            com.destroystokyo.paper.entity.Pathfinder pf = z.getPathfinder();
            com.destroystokyo.paper.entity.Pathfinder.PathResult direct = pf.findPath(player);
            if (direct != null && direct.canReachFinalPoint()) return null;
            if (doors.isEmpty()) return null;
            Location pl = player.getLocation();
            boolean copperOn = copperTargetSeconds > 0 && copperSiegeBook != null;
            Steer best = null;
            double bestScore = Double.MAX_VALUE;
            for (Block door : doors) {
                if (!(door.getBlockData() instanceof Door d) || d.isOpen()) continue;
                NightHearing.DoorKind kind = NightHearing.steerKind(door.getType().name(), copperOn);
                if (kind == null) continue;
                for (BlockFace side : new BlockFace[] { d.getFacing(), d.getFacing().getOppositeFace() }) {
                    Block face = door.getRelative(side);
                    Block other = door.getRelative(side.getOppositeFace());
                    if (!face.isPassable() || !face.getRelative(BlockFace.UP).isPassable()) continue;
                    Location spot = face.getLocation().add(0.5, 0.0, 0.5);
                    Location inside = other.getLocation().add(0.5, 0.0, 0.5);
                    if (!NightHearing.outsideFace(spot.distanceSquared(pl), inside.distanceSquared(pl))) continue;
                    com.destroystokyo.paper.entity.Pathfinder.PathResult walk = pf.findPath(spot);
                    if (walk == null || !walk.canReachFinalPoint()) continue;
                    double score = NightHearing.steerScore(walk.getPoints().size(), inside.distance(pl));
                    if (score < bestScore) {
                        bestScore = score;
                        best = new Steer(player.getUniqueId(), door, spot, kind);
                    }
                }
            }
            return best;
        } catch (Throwable t) {
            return null; // pathfinder trouble: plain vanilla hunt
        }
    }

    private void dropSteer(UUID zid) {
        if (nhSteer.remove(zid) == null) return;
        if (Bukkit.getEntity(zid) instanceof Mob m && m.isValid()) m.getPathfinder().stopPathfinding();
    }

    private void clearSteers() {
        for (UUID zid : new java.util.ArrayList<>(nhSteer.keySet())) dropSteer(zid);
    }

    private int dropSteersOf(UUID player) {
        int n = 0;
        for (Map.Entry<UUID, Steer> me : new java.util.ArrayList<>(nhSteer.entrySet())) {
            if (me.getValue().player().equals(player)) { dropSteer(me.getKey()); n++; }
        }
        return n;
    }

    private static boolean onSpot(Entity e, Location spot, double radius) {
        Location l = e.getLocation();
        if (!l.getWorld().equals(spot.getWorld())) return false;
        double dx = l.getX() - spot.getX(), dz = l.getZ() - spot.getZ();
        return NightHearing.onSpot(dx * dx + dz * dz, l.getY() - spot.getY(), radius);
    }

    /** Siege cadence: a steered zombie standing at its door works that door's own siege. */
    private void steerSieges() {
        for (Map.Entry<UUID, Steer> me : new java.util.ArrayList<>(nhSteer.entrySet())) {
            if (!nhHunters.contains(me.getKey())) continue;
            Steer st = me.getValue();
            Player p = Bukkit.getPlayer(st.player());
            if (!(Bukkit.getEntity(me.getKey()) instanceof Zombie z) || z.isDead() || p == null || !p.isOnline()) continue;
            if (z.getTarget() != null || registry.isProtected(p.getUniqueId())) continue;
            if (!onSpot(z, st.spot(), 1.5)) continue;
            Block door = st.door();
            if (!(door.getBlockData() instanceof Door d) || d.isOpen()) continue;
            switch (st.kind()) {
                case IRON -> observeSiege(siegeBook, false, z, p, door, door.getWorld());
                case COPPER -> { if (copperSiegeBook != null) observeSiege(copperSiegeBook, true, z, p, door, door.getWorld()); }
                case WOOD -> { if (woodSiegeBook != null) observeSiege(woodSiegeBook, false, z, p, door, door.getWorld(), "WOOD SIEGE", "wooden door"); }
            }
        }
    }

    /** Vanilla vibration events (the same ones sculk sensors hear). */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onNoise(org.bukkit.event.world.GenericGameEvent event) {
        if (!nhEnabled || event.isAsynchronous()) return;
        if (!(event.getEntity() instanceof Player player)) return;
        String key;
        try {
            key = event.getEvent().getKey().getKey();
        } catch (Throwable t) {
            return;
        }
        if (nhLogKeys && hearingActive(event.getWorld())) getLogger().info("NIGHT HEARING key: " + key + " by " + player.getName());
        if (!NightHearing.isNoise(key, nhFootsteps)) return;
        hear(player, event.getLocation(), key);
    }

    /** Hitting with a sword or an own arrow (covers versions where the damage vibration names the victim). */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onNoisyHit(EntityDamageByEntityEvent event) {
        if (!nhEnabled) return;
        Entity damager = event.getDamager();
        Player attacker = null;
        if (damager instanceof Player p) attacker = p;
        else if (damager instanceof Projectile pr && pr.getShooter() instanceof Player p) attacker = p;
        if (attacker == null || attacker == event.getEntity()) return;
        hear(attacker, attacker.getLocation(), "hit");
    }

    /** Once a second: pulled zombies give up / reach / keep working; release all at dawn. */
    private void hearingTick() {
        for (UUID zid : new java.util.ArrayList<>(nhSteer.keySet())) if (!nhHunters.contains(zid)) dropSteer(zid);
        if (nhHunters.size() == 0) {
            tipTick();
            return;
        }
        long now = System.currentTimeMillis();
        for (UUID zid : nhHunters.zombies()) {
            NightHearing.Hunters.Entry e = nhHunters.get(zid);
            if (e == null) continue;
            Entity ent = Bukkit.getEntity(zid);
            Player p = Bukkit.getPlayer(e.player());
            if (!(ent instanceof Zombie z) || z.isDead() || p == null || !p.isOnline()
                    || !z.getWorld().equals(p.getWorld())) {
                if (ent instanceof Zombie z2 && p != null && p.equals(z2.getTarget())) z2.setTarget(null);
                nhHunters.remove(zid);
                dropSteer(zid);
                continue;
            }
            if (!hearingActive(z.getWorld())) {
                if (p.equals(z.getTarget())) z.setTarget(null);
                nhHunters.remove(zid);
                dropSteer(zid);
                if (debug) getLogger().info("NIGHT HEARING: released " + z.getType() + " (hearing off: day or seal)");
                continue;
            }
            Steer st = nhSteer.get(zid);
            if (st != null) {
                LivingEntity t = z.getTarget();
                if (t != null && !t.equals(p)) { // found prey of its own: no longer ours
                    dropSteer(zid);
                    nhHunters.remove(zid);
                    continue;
                }
                boolean doorClosed = st.door().getBlockData() instanceof Door d && !d.isOpen()
                        && NightHearing.steerKind(st.door().getType().name(), true) != null;
                if (t == null && doorClosed) {
                    boolean there = onSpot(z, st.spot(), 1.0);
                    if (there) {
                        z.getPathfinder().stopPathfinding();
                        z.lookAt(st.door().getLocation().add(0.5, 1.0, 0.5));
                    } else {
                        z.getPathfinder().moveTo(st.spot(), 1.0);
                    }
                    // Near the door counts as working it (a crowd at one door keeps its hunt).
                    NightHearing.Hunters.Verdict sv = nhHunters.check(zid, true, false, onSpot(z, st.spot(), 2.5), now);
                    if (sv == NightHearing.Hunters.Verdict.GIVE_UP) {
                        dropSteer(zid);
                        if (debug) getLogger().info("NIGHT HEARING: " + z.getType() + " gave up on " + p.getName()
                                + " (" + nhGiveUpSeconds + "s without reaching its door)");
                    }
                    continue;
                }
                // The door is open or gone, or it saw the player: a plain hunt from here.
                dropSteer(zid);
                if (t == null) z.setTarget(p);
                if (debug) getLogger().info("NIGHT HEARING: " + z.getType() + " door " + (doorClosed ? "still shut" : "open or down")
                        + ", now hunting " + p.getName());
            }
            boolean stillHunting = p.equals(z.getTarget());
            // Reached = within 3 blocks AND in sight. A zombie pressed against the
            // outside of a wall is close but has not reached anyone; without the
            // sight check it would drop out of the book and never give up.
            boolean reached = z.getLocation().distanceSquared(p.getLocation()) <= 9.0 && z.hasLineOfSight(p);
            boolean atDoor = stillHunting && findDoor(z, p, mat -> mat.name().endsWith("_DOOR")) != null;
            NightHearing.Hunters.Verdict v = nhHunters.check(zid, stillHunting, reached, atDoor, now);
            if (v == NightHearing.Hunters.Verdict.GIVE_UP) {
                z.setTarget(null);
                if (debug) getLogger().info("NIGHT HEARING: " + z.getType() + " gave up on " + p.getName()
                        + " (" + nhGiveUpSeconds + "s without reaching or a door)");
            }
        }
        tipTick();
    }

    /** The public tip: once per night per player in a hearing world (ExcelTips caps the shows). */
    private void tipTick() {
        if (!nhEnabled || !nhTipEnabled) return;
        for (Player p : Bukkit.getOnlinePlayers()) {
            org.bukkit.World w = p.getWorld();
            if (!hearingActive(w)) continue;
            long day = w.getFullTime() / 24000L;
            String key = w.getName() + "#" + day;
            if (key.equals(nhTipShown.get(p.getUniqueId()))) continue;
            nhTipShown.put(p.getUniqueId(), key);
            fireTip(p);
        }
    }

    /** ExcelTipsAPI by reflection; registers our default words once (a tips.yml entry overrides them). */
    private void fireTip(Player player) {
        try {
            Plugin tips = Bukkit.getPluginManager().getPlugin("ExcelTips");
            if (tips == null || !tips.isEnabled()) return;
            Class<?> api = Class.forName("com.winkreyes.exceltips.ExcelTipsAPI", true, tips.getClass().getClassLoader());
            if (!nhTipRegistered) {
                api.getMethod("register", Plugin.class, String.class, String.class, List.class)
                        .invoke(null, this, NH_TIP_ID, nhTipTitle, List.of(nhTipText));
                nhTipRegistered = true;
            }
            api.getMethod("tip", Player.class, String.class).invoke(null, player, NH_TIP_ID);
        } catch (Throwable t) {
            if (!nhTipSeamLogged) {
                nhTipSeamLogged = true;
                getLogger().warning("night hearing: ExcelTips tip unavailable (" + t + ") - feature works, no tip shown");
            }
        }
    }

    /** Bed always works while hearing is live: the "monsters nearby" refusal is overridden. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBedTry(org.bukkit.event.player.PlayerBedEnterEvent event) {
        Player p = event.getPlayer();
        if (NightHearing.forceBed(nhBed && nhEnabled, hearingActive(p.getWorld()), event.getBedEnterResult().name())
                && event.useBed() != org.bukkit.event.Event.Result.DENY) {
            event.setUseBed(org.bukkit.event.Event.Result.ALLOW);
            if (debug) getLogger().info("NIGHT HEARING: bed opened for " + p.getName() + " despite monsters nearby");
        }
    }

    /** In bed = silent: zombies hunting the sleeper let go. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBedIn(org.bukkit.event.player.PlayerBedEnterEvent event) {
        if (!nhEnabled || !nhBed) return;
        org.bukkit.event.player.PlayerBedEnterEvent.BedEnterResult r = event.getBedEnterResult();
        boolean entering = event.useBed() == org.bukkit.event.Event.Result.ALLOW
                || (event.useBed() == org.bukkit.event.Event.Result.DEFAULT && r == org.bukkit.event.player.PlayerBedEnterEvent.BedEnterResult.OK);
        if (!entering) return;
        Player p = event.getPlayer();
        if (!hearingActive(p.getWorld())) return;
        int cleared = 0;
        for (Zombie z : p.getWorld().getNearbyEntitiesByType(Zombie.class, p.getLocation(), 64.0)) {
            if (p.equals(z.getTarget()) && nhTypes.contains(z.getType().name()) && !isEliteForDoors(z)) {
                z.setTarget(null);
                cleared++;
            }
        }
        cleared += dropSteersOf(p.getUniqueId());
        nhHunters.releasePlayer(p.getUniqueId());
        if (debug || cleared > 0) doorInfo("BED|" + p.getUniqueId(), "NIGHT HEARING: " + p.getName()
                + " went to bed - " + cleared + " zombie(s) stopped hunting");
    }

    /** While asleep, the hearing zombie types cannot pick the sleeper as a target. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onTargetSleeper(EntityTargetLivingEntityEvent event) {
        if (!nhEnabled || !nhBed) return;
        if (!(event.getTarget() instanceof Player p) || !p.isSleeping()) return;
        if (!nhTypes.contains(event.getEntity().getType().name())) return;
        if (!hearingActive(p.getWorld())) return;
        if (isEliteForDoors(event.getEntity())) return;
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onHearingQuit(PlayerQuitEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        nhCooldown.forget(id);
        dropSteersOf(id);
        nhHunters.releasePlayer(id);
    }

    // ---------- /trainingwheels ----------

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("trainingwheels")) return false;
        if (args.length == 0) {
            sender.sendMessage("Usage: /trainingwheels list | add <player> [factor] | remove <player> | setfactor <player> <factor> | reload");
            return true;
        }
        switch (args[0].toLowerCase()) {
            case "list" -> {
                Map<UUID, ProtectedRegistry.Entry> snap = registry.snapshot();
                if (snap.isEmpty()) {
                    sender.sendMessage("Protected players: none");
                    return true;
                }
                StringBuilder out = new StringBuilder("Protected players (" + snap.size() + "):");
                for (Map.Entry<UUID, ProtectedRegistry.Entry> e : snap.entrySet()) {
                    String name = e.getValue().name();
                    if (name == null) {
                        OfflinePlayer op = Bukkit.getOfflinePlayer(e.getKey());
                        name = op.getName();
                    }
                    out.append("\n  ").append(name == null ? "?" : name)
                            .append(" ").append(e.getKey())
                            .append(" factor ").append(String.format("%.2f", e.getValue().factor()));
                }
                sender.sendMessage(out.toString());
                return true;
            }
            case "add" -> {
                if (args.length < 2) { sender.sendMessage("Usage: /trainingwheels add <player> [factor]"); return true; }
                double factor = defaultFactor;
                if (args.length >= 3) {
                    Double parsed = parseFactor(args[2]);
                    if (parsed == null) { sender.sendMessage("Factor must be a number in [0, 1]: " + args[2]); return true; }
                    factor = parsed;
                }
                UUID id = resolvePlayer(args[1]);
                if (id == null) { sender.sendMessage("Player not found (online, cached, listed, or a UUID): " + args[1]); return true; }
                String name = displayName(id);
                registry.put(id, factor, name);
                persistRegistry();
                sender.sendMessage("Protected: " + (name == null ? id.toString() : name)
                        + " (" + id + ") at factor " + String.format("%.2f", factor));
                return true;
            }
            case "remove" -> {
                if (args.length < 2) { sender.sendMessage("Usage: /trainingwheels remove <player>"); return true; }
                UUID id = resolvePlayer(args[1]);
                if (id == null || !registry.remove(id)) {
                    sender.sendMessage("Not on the protected list: " + args[1]);
                    return true;
                }
                persistRegistry();
                sender.sendMessage("Removed from the protected list: " + args[1] + " (" + id + ")");
                return true;
            }
            case "setfactor" -> {
                if (args.length < 3) { sender.sendMessage("Usage: /trainingwheels setfactor <player> <factor>"); return true; }
                Double parsed = parseFactor(args[2]);
                if (parsed == null) { sender.sendMessage("Factor must be a number in [0, 1]: " + args[2]); return true; }
                UUID id = resolvePlayer(args[1]);
                Optional<ProtectedRegistry.Entry> current = id == null ? Optional.empty() : registry.entry(id);
                if (current.isEmpty()) { sender.sendMessage("Not on the protected list: " + args[1]); return true; }
                registry.put(id, parsed, current.get().name());
                persistRegistry();
                sender.sendMessage("Factor for " + args[1] + " set to " + String.format("%.2f", parsed));
                return true;
            }
            case "reload" -> {
                reloadConfig();
                readSettings();
                sender.sendMessage("Training Wheels reloaded: " + registry.size()
                        + " protected, siege threshold " + siegeThresholdSeconds
                        + "s, copper tier " + copperTargetSeconds + "s, all zombies break doors "
                        + (allZombiesBreakDoors ? "ON (" + sweepLoadedZombies() + " granted)" : "OFF")
                        + ", lifesteal " + lifestealState()
                        + ", door log throttle " + doorLogState()
                        + ", compensation seal " + cachedSeals
                        + " (elite factor " + String.format("%.4f", compTable.eliteFactor(defaultFactor, cachedSeals))
                        + ", source " + (sealCountKnown ? "GuildGate" : "assume-seal fallback")
                        + ", divisor " + (compUseLive ? "LIVE scaled x" + fmt(liveScaled()) + " / normalized x"
                                + fmt(liveEmNamed("getNormalizedDamageToPlayerMultiplier")) + " / other x"
                                + fmt(liveEmNamed("getDamageToPlayerMultiplier")) : "TABLE") + ")"
                        + ", bedrock grace " + graceState()
                        + ", night hearing " + hearingState() + ".");
                return true;
            }
            default -> {
                sender.sendMessage("Usage: /trainingwheels list | add <player> [factor] | remove <player> | setfactor <player> <factor> | reload");
                return true;
            }
        }
    }

    private static Double parseFactor(String text) {
        try {
            double f = Double.parseDouble(text);
            return ProtectedRegistry.validFactor(f) ? f : null;
        } catch (Exception e) {
            return null;
        }
    }

    /** UUID literal -> online exact -> cached offline -> a listed name. */
    private UUID resolvePlayer(String arg) {
        try {
            return UUID.fromString(arg);
        } catch (Exception ignored) { /* not a UUID */ }
        Player online = Bukkit.getPlayerExact(arg);
        if (online != null) return online.getUniqueId();
        for (Map.Entry<UUID, ProtectedRegistry.Entry> e : registry.snapshot().entrySet()) {
            if (e.getValue().name() != null && e.getValue().name().equalsIgnoreCase(arg)) return e.getKey();
        }
        OfflinePlayer cached = Bukkit.getOfflinePlayerIfCached(arg);
        if (cached != null) return cached.getUniqueId();
        return null;
    }

    private String displayName(UUID id) {
        Player online = Bukkit.getPlayer(id);
        if (online != null) return online.getName();
        Optional<ProtectedRegistry.Entry> e = registry.entry(id);
        if (e.isPresent() && e.get().name() != null) return e.get().name();
        OfflinePlayer op = Bukkit.getOfflinePlayer(id);
        return op.getName();
    }
}

// ExcelTrainingWheels — an Excel plugin by WinkReyesValley. (Excel signature — MikeExcel)

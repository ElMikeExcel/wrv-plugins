package com.winkreyes.exceltrainingwheels;

import com.winkreyes.exceltrainingwheels.core.CompensationTable;
import com.winkreyes.exceltrainingwheels.core.CopperDoors;
import com.winkreyes.exceltrainingwheels.core.DoorGuardRule;
import com.winkreyes.exceltrainingwheels.core.LifestealRule;
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
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
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
 * Three pieces, one principle: PROTECTION FOLLOWS THE CHILD.
 *  1. KID SHIELD — listed players take reduced damage from hostile
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
            sealCountKnown = false;
        }
        sealStateLoggedOnce = true;
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

    // ---------- 1. kid shield ----------

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
            double effective = compEnabled
                    ? compTable.effectiveFactor(factor.getAsDouble(), elite, cachedSeals)
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
                    + " (factor " + effective + ", elite " + elite + ", raid x" + raidDivisor
                    + ", seals " + cachedSeals + ")");
        }
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
            if (debug) getLogger().info("DOOR GUARD: " + event.getEntity().getType() + " stopped at "
                    + event.getBlock().getLocation() + " (target " + target.getName() + " is protected)");
            return;
        }
        if (copperTargetSeconds <= 0 || event.isCancelled()) return;
        Block block = event.getBlock();
        if (!CopperDoors.isCopperDoor(block.getType().name())) return;
        event.setCancelled(true);
        if (debug) getLogger().info("DOOR TIER: vanilla copper completion suppressed at "
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
        if (debug) getLogger().info("DOORS: " + zombie.getType() + " at " + fmt(zombie.getLocation())
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
        sweepBook(siegeBook, "SIEGE");
        if (copperSiegeBook != null) sweepBook(copperSiegeBook, "COPPER SIEGE");
    }

    private void sweepBook(SiegeBook book, String kind) {
        for (String key : book.sweep(tickCounter)) {
            clearCracks(key);
            siegeDoors.remove(key);
            if (debug) getLogger().info(kind + " stale-reset: " + key);
        }
    }

    /** Feed one observation to a siege book and render its effects. */
    private void observeSiege(SiegeBook book, boolean copper, LivingEntity breaker,
                              LivingEntity target, Block door, org.bukkit.World world) {
        String key = doorKey(door);
        Location doorLoc = door.getLocation();
        int soundEverySteps = Math.max(1, 40 / cadenceTicks); // ~every 2 s of siege
        String kind = copper ? "COPPER SIEGE" : "SIEGE";
        String doorWord = copper ? "copper door" : "iron door";
        SiegeBook.Event ev = book.observe(key, breaker.getUniqueId(),
                target.getUniqueId(), false, tickCounter);
        switch (ev) {
            case STARTED -> {
                siegeDoors.put(key, doorLoc);
                renderCracks(key, book);
                getLogger().info(kind + " started: " + breaker.getType() + " besieging " + doorWord
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
                if (debug) getLogger().info(kind + " reset at " + fmt(doorLoc));
            }
            case IGNORED -> { /* busy door / cap reached */ }
        }
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
                        + ", compensation seal " + cachedSeals
                        + " (elite factor " + String.format("%.4f", compTable.eliteFactor(defaultFactor, cachedSeals))
                        + ", source " + (sealCountKnown ? "GuildGate" : "assume-seal fallback") + ").");
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

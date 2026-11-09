/* Excel WRV maintenance; existing authorship retained. */ package com.winkreyes.excelbosstrial.dragon;

import com.winkreyes.excelbosstrial.core.BossKind;
import com.winkreyes.excelbosstrial.core.EncounterRetention;
import com.winkreyes.excelbosstrial.core.IntakeFactor;
import com.winkreyes.excelbosstrial.core.MilestoneBeats;
import com.winkreyes.excelbosstrial.core.MilestoneSettings;
import com.winkreyes.excelbosstrial.dragon.BattlePhase;
import com.winkreyes.excelbosstrial.dragon.CrystalPlacementRule;
import com.winkreyes.excelbosstrial.dragon.DamageGovernor;
import com.winkreyes.excelbosstrial.dragon.DamagerKind;
import com.winkreyes.excelbosstrial.dragon.TrialConfig;
import com.winkreyes.excelbosstrial.dragon.TrialStateMachine;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.boss.DragonBattle;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.AreaEffectCloud;
import org.bukkit.entity.ComplexEntityPart;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Enderman;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.event.HandlerList;

/**
 * The dragon chapter of ExcelBossTrial 0.1.0: ExcelDragonTrial
 * 0.1.2's main class, ported whole. Only the lifecycle plumbing
 * changed (fields/constructor/enable-disable, config read from the
 * host's "dragon" section, state.yml in the chapter folder, the
 * adds PDC key now excelbosstrial:add, scheduler/plugin references
 * point at the host, the /dragontrial command became statusLines()
 * + reload() for the host's /bosstrial). Every fight behavior -
 * state machine calls, governor order, flip sequence, messages -
 * is the 0.1.2 original below.
 *
 * ExcelDragonTrial 0.1.2 — the two-phase Ender Dragon (Seal 1's boss).
 *
 * The whole design hangs on SPEC Step 0: ExcelGuildGate credits the
 * ender_dragon seal from EntityDeathEvent alone, so this plugin never
 * lets a phase-1 dragon die. Lethal damage in phase 1 is intercepted
 * at the damage event (HIGH priority), cancelled, and the dragon is
 * healed onto its phase-2 bar in the same handler. No health-0
 * moment, no death event, no seal credit, no loot, no portal.
 * Phase 2 permits exactly one real death and then steps aside:
 * loot, XP, egg, portal and GuildGate's credit are untouched vanilla.
 *
 * 0.1.1 adds the anti-nuke governor (DRAGON-CHEESE-RESEARCH.md
 * section 2): P1 per-event damage cap and P2 per-tick explosion
 * budget, applied to every hit BEFORE the state machine judges it,
 * plus P3 — no end-crystal placement while a trial battle is active.
 * The governed damage is the only damage the machine ever sees, so
 * the Step 0 guarantee is untouched.
 *
 * 0.1.2 is a hotfix for the flip itself: on Paper 26.2 the
 * DRAGON_BREATH particle requires a Float data argument, and the
 * old no-data call threw IllegalArgumentException inside runFlip,
 * aborting the transition and stranding the dragon invulnerable.
 * The theatre now lives in FlipEffects (which passes the Float),
 * and runFlip fences the effects call so no effects failure can
 * ever abort a flip again. Nothing else changed.
 *
 * All decisions live in the Bukkit-free core (TrialStateMachine,
 * DamageGovernor, CrystalPlacementRule); this class only translates
 * between Bukkit and the core.
 */
public final class DragonChapter implements Listener {

    /** Per-world runtime record: the machine plus persisted counters. */
    private static final class WorldTrial {
        TrialStateMachine machine = new TrialStateMachine();
        final DamageGovernor governor = new DamageGovernor();
        /** 0.3.2: milestone beats for the CURRENT phase - reset at
         *  every battle registration and every phase flip, persisted
         *  beside waves-fired so a restored battle keeps its spent
         *  beats for the phase it resumes in. */
        final MilestoneBeats phaseBeats = new MilestoneBeats();
        int completedBattles;
        int missScans;
        boolean deathSeen;
        boolean ignoreLogged;
        /** 0.2.7: the "holding while nobody is at the arena" line was logged. */
        boolean holdLogged;
    }

    private final JavaPlugin host;
    private final File dataDir;
    private ConfigurationSection section;

    private final Map<String, WorldTrial> worlds = new HashMap<>();
    /** 0.2.11 (B-4): worlds whose wiped stale trial carried our
     *  invulnerable flag; the flag is cleared off the next dragon
     *  seen there before its fresh battle registers. */
    private final java.util.Set<String> staleFlagWorlds = new java.util.HashSet<>();
    private final Random random = new Random();
    private TrialConfig cfg = TrialConfig.defaults();
    /** Bed ignitions in the End, for share attribution (0.2.1, FLAG 3). */
    private final BedIgnitionLog bedIgnitions = new BedIgnitionLog();
    private NamespacedKey addKey;
    private int scannerTask = -1;
    private int aggressionTask = -1;

    private String msgTitle;
    private String msgSubtitle;
    private String msgChat;
    private String addName;
    private boolean broadcastServerWide;
    /** 0.3.2: this chapter's milestone-beat settings, read from the
     *  dragon.milestones block (the same keys the host reads for
     *  the surge side). */
    private MilestoneSettings dragonBeats = MilestoneSettings.defaults(BossKind.ENDER_DRAGON);
    /** 0.3.2: the host's surge hand-off - the chapter fires the
     *  beat (broadcast + log) and calls this with the dragon's
     *  UUID; the host queues the pulse surge on the battle's
     *  encounter ledger, whose participants the chapter does not
     *  own. Null = no surge (the beat still speaks). */
    private java.util.function.Consumer<UUID> milestoneSurgeSink;

    /** Host wiring (0.3.2): where milestone pulse surges go. */
    public void setMilestoneSurgeSink(java.util.function.Consumer<UUID> sink) {
        this.milestoneSurgeSink = sink;
    }

    /** 0.3.3: the host's elite-sprinkle hand-off - the chapter
     *  reports the phase-2 milestone mask and the fountain
     *  anchor; the host rolls and spawns (it owns the sprinkle
     *  service and the encounter ledger). */
    public interface SprinkleSink {
        void onMilestones(UUID dragonUuid, int stampMask, Location fountainAnchor);
    }

    private SprinkleSink sprinkleSink;
    /** 0.3.3: mirrors the host's dragon.sprinkle.enabled state
     *  (pushed at wiring and on every settings reload), so the
     *  beat tracker advances for the sprinkle exactly when the
     *  chapter is armed - and 0.3.2 behavior is untouched while
     *  both the lines and the sprinkle are dark. */
    private boolean sprinkleArmed;

    /** Host wiring (0.3.3): where phase-2 sprinkle rolls go. */
    public void setSprinkleSink(SprinkleSink sink) {
        this.sprinkleSink = sink;
    }

    /** Host wiring (0.3.3): the dragon sprinkle's armed state. */
    public void setSprinkleArmed(boolean armed) {
        this.sprinkleArmed = armed;
    }

    /** A world trial carrying the CURRENT config's wave thresholds. */
    private WorldTrial newWorldTrial() {
        WorldTrial wt = new WorldTrial();
        wt.machine.setWaveThresholds(cfg.addsWaveThresholds);
        return wt;
    }

    // ------------------------------------------------------------------
    // Lifecycle
    // ------------------------------------------------------------------

    public DragonChapter(JavaPlugin host, File dataDir, ConfigurationSection section) {
        this.host = host;
        this.dataDir = dataDir;
        this.section = section;
    }

    private java.util.logging.Logger getLogger() {
        return host.getLogger();
    }

    public void enable() {
        addKey = new NamespacedKey(host, "add");
        loadSettings();
        loadState();
        Bukkit.getPluginManager().registerEvents(this, host);
        startTasks();
        getLogger().info("Dragon chapter awake (ExcelDragonTrial 0.1.2 port): two-phase dragon "
                + (cfg.enabled ? "ENABLED" : "disabled by config")
                + " (phase1 " + (int) cfg.phase1MaxHealth + " HP, phase2 "
                + (int) cfg.phase2MaxHealth + " HP, damage x" + cfg.phase2DamageMultiplier + "). "
                + capsSummary() + ".");
    }

    public void disable() {
        stopTasks();
        // Never leave a dragon invulnerable because of us.
        for (Map.Entry<String, WorldTrial> e : worlds.entrySet()) {
            WorldTrial wt = e.getValue();
            if (wt.machine.invulnerableByUs()) {
                World world = Bukkit.getWorld(e.getKey());
                EnderDragon d = world == null ? null : findDragon(world);
                if (d != null && wt.machine.tracks(d.getUniqueId().toString())) {
                    d.setInvulnerable(false);
                }
                wt.machine.setInvulnerableByUs(false);
            }
        }
        saveState();
        HandlerList.unregisterAll(this);
    }

    private String sectionString(String key, String fallback) {
        return section != null ? section.getString(key, fallback) : fallback;
    }

    private boolean sectionBool(String key, boolean fallback) {
        return section != null ? section.getBoolean(key, fallback) : fallback;
    }

    /** Host-driven reload: swap the section, re-read, restart tasks. */
    public void reload(ConfigurationSection newSection) {
        this.section = newSection;
        loadSettings();
        startTasks();
    }

    private void loadSettings() {
        Map<String, Object> flat = new HashMap<>();
        ConfigurationSection root = section;
        for (String key : new String[] {"enabled", "apply-to-resummons",
                "phase1.max-health", "phase2.max-health", "phase2.damage-multiplier",
                "transition.invulnerable-seconds", "phase2.aggression-interval-seconds",
                "phase2.aggression-chance", "adds.enabled", "adds.per-wave", "adds.max-alive",
                "adds.wave-thresholds",
                "caps.per-event-damage", "caps.explosion-per-tick",
                "caps.block-crystal-placement"}) {
            if (root != null && root.contains(key)) {
                flat.put(key, root.get(key));
            }
        }
        // 0.3.0: the intake cap is a HOST-level key (intake.entity-cap),
        // shared with the other chapters' governor; the dragon section
        // itself does not carry it.
        if (host.getConfig() != null) {
            flat.put("entity-cap", host.getConfig().getDouble("intake.entity-cap", 1024.0));
        }
        cfg = TrialConfig.fromMap(flat);
        msgTitle = sectionString("messages.title", "&5&lThe Seal Resists");
        msgSubtitle = sectionString("messages.subtitle", "&fThe dragon refuses to fall.");
        msgChat = sectionString("messages.chat",
                "&5The dragon's death-cry shatters against the first seal - and it rises again, "
                        + "wreathed in the valley's old anger.");
        addName = sectionString("adds.name", "&5Sealbound Enderman");
        broadcastServerWide = sectionBool("transition.broadcast-server-wide", false);
        // 0.3.2: milestone-beat settings for this chapter (dragon
        // .milestones.*), same keys the host mirrors for its own
        // surge gate. Missing keys keep the settled defaults.
        MilestoneSettings db = MilestoneSettings.defaults(BossKind.ENDER_DRAGON);
        dragonBeats = MilestoneSettings.sanitized(
                sectionBool("milestones.enabled", db.enabled()),
                sectionDouble("milestones.surge-multiplier", db.surgeMultiplier()),
                sectionString("milestones.text-75", db.text75()),
                sectionString("milestones.text-50", db.text50()),
                sectionString("milestones.text-25", db.text25()));
        // 0.3.2: push the configured add-wave thresholds into every
        // live machine (new trials get them at creation).
        for (WorldTrial wt : worlds.values()) {
            wt.machine.setWaveThresholds(cfg.addsWaveThresholds);
        }
    }

    private double sectionDouble(String key, double fallback) {
        return section == null ? fallback : section.getDouble(key, fallback);
    }

    private void startTasks() {
        stopTasks();
        scannerTask = Bukkit.getScheduler().scheduleSyncRepeatingTask(host, this::scan, 40L, 40L);
        long period = Math.max(1L, cfg.aggressionIntervalSeconds) * 20L;
        aggressionTask = Bukkit.getScheduler().scheduleSyncRepeatingTask(host, this::aggression, period, period);
    }

    private void stopTasks() {
        if (scannerTask >= 0) {
            Bukkit.getScheduler().cancelTask(scannerTask);
            scannerTask = -1;
        }
        if (aggressionTask >= 0) {
            Bukkit.getScheduler().cancelTask(aggressionTask);
            aggressionTask = -1;
        }
    }

    // ------------------------------------------------------------------
    // Persistence (state.yml in the data folder)
    // ------------------------------------------------------------------

    private File stateFile() {
        return new File(dataDir, "state.yml");
    }

    private void loadState() {
        worlds.clear();
        File f = stateFile();
        if (!f.exists()) {
            return;
        }
        YamlConfiguration y = YamlConfiguration.loadConfiguration(f);
        String currentBuild = host.getDescription().getVersion();
        String savedBuild = y.getString("build", null);
        ConfigurationSection section = y.getConfigurationSection("worlds");
        // 0.2.11 (B-4): wipe-on-update. First decide from the pure
        // policy whether this file holds an open battle saved under a
        // different build; only then touch anything.
        boolean anyOpen = false;
        if (section != null) {
            for (String worldName : section.getKeys(false)) {
                String base = "worlds." + worldName + ".";
                if (StaleTrialPolicy.isOpen(parsePhase(y.getString(base + "phase", "NONE")),
                        y.getString(base + "dragon-uuid", null))) {
                    anyOpen = true;
                    break;
                }
            }
        }
        boolean wipe = StaleTrialPolicy.onLoad(savedBuild, currentBuild, anyOpen)
                == StaleTrialPolicy.Action.CLOSE_STALE;
        File archive = wipe ? archiveStateFile(f, savedBuild) : null;
        if (section == null) {
            return;
        }
        for (String worldName : section.getKeys(false)) {
            String base = "worlds." + worldName + ".";
            WorldTrial wt = newWorldTrial();
            wt.completedBattles = y.getInt(base + "completed-battles", 0);
            String uuid = y.getString(base + "dragon-uuid", null);
            BattlePhase phase = parsePhase(y.getString(base + "phase", "NONE"));
            if (StaleTrialPolicy.isOpen(phase, uuid)) {
                if (wipe) {
                    // Closed out, NOT restored: the machine stays fresh
                    // (NONE, no dragon on record); the completed-battles
                    // counter above is kept, and nothing outside this
                    // chapter's state is touched.
                    if (y.getBoolean(base + "invulnerable-by-us", false)) {
                        staleFlagWorlds.add(worldName);
                    }
                    getLogger().warning("Stale trial CLOSED in " + worldName
                            + ": a " + phase + " battle (dragon " + uuid + ") saved under build "
                            + (savedBuild == null ? "<unstamped>" : savedBuild)
                            + " was NOT restored under build " + currentBuild
                            + " (wipe-on-update, 0.2.11). The next dragon in " + worldName
                            + " registers fresh; the saved state was archived to "
                            + (archive == null ? "<archive failed - see warning above>"
                                    : archive.getName()) + ".");
                } else {
                    wt.machine.restore(phase, uuid, y.getInt(base + "waves-fired", 0),
                            y.getBoolean(base + "death-consumed", false),
                            y.getBoolean(base + "invulnerable-by-us", false));
                    wt.phaseBeats.restoreMask(y.getInt(base + "beats-fired", 0));
                    getLogger().info("Restored trial state for " + worldName + ": " + wt
                            .machine.phase() + ", dragon " + uuid + ".");
                }
            }
            worlds.put(worldName, wt);
        }
        if (wipe || savedBuild == null || !savedBuild.equals(currentBuild)) {
            // Persist immediately under the current build stamp: a
            // crash cannot re-run the wipe, and the closure above is
            // the state of record from this line on.
            saveState();
        }
    }

    private static BattlePhase parsePhase(String name) {
        try {
            return BattlePhase.valueOf(name == null ? "NONE" : name);
        } catch (IllegalArgumentException e) {
            return BattlePhase.NONE;
        }
    }

    /** Copy the state file beside itself under the archive name; the
     *  original is never deleted by the wipe (saveState overwrites it
     *  with the fresh stamped state right after). */
    private File archiveStateFile(File f, String savedBuild) {
        String name = StaleTrialPolicy.archiveName(savedBuild);
        File target = new File(dataDir, name);
        int n = 2;
        while (target.exists()) {
            target = new File(dataDir, name + "-" + n++);
        }
        try {
            java.nio.file.Files.copy(f.toPath(), target.toPath());
            return target;
        } catch (IOException ex) {
            getLogger().warning("Could not archive the stale dragon state.yml to "
                    + target.getName() + ": " + ex.getMessage()
                    + " (the wipe still proceeds; the pre-wipe state survives only in this log).");
            return null;
        }
    }

    private void saveState() {
        YamlConfiguration y = new YamlConfiguration();
        // 0.2.11 (B-4): every save carries the build that wrote it, so
        // the next boot can tell its own state from a stale one.
        y.set("build", host.getDescription().getVersion());
        for (Map.Entry<String, WorldTrial> e : worlds.entrySet()) {
            String base = "worlds." + e.getKey() + ".";
            WorldTrial wt = e.getValue();
            y.set(base + "completed-battles", wt.completedBattles);
            y.set(base + "phase", wt.machine.phase().name());
            y.set(base + "dragon-uuid", wt.machine.dragonUuid());
            y.set(base + "waves-fired", wt.machine.wavesFiredMask());
            y.set(base + "beats-fired", wt.phaseBeats.firedMask());
            y.set(base + "death-consumed", wt.machine.deathConsumed());
            y.set(base + "invulnerable-by-us", wt.machine.invulnerableByUs());
        }
        try {
            dataDir.mkdirs();
            y.save(stateFile());
        } catch (IOException ex) {
            getLogger().warning("Could not save dragon state.yml: " + ex.getMessage());
        }
    }

    // ------------------------------------------------------------------
    // Battle registration (scanner is the authority)
    // ------------------------------------------------------------------

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntitySpawn(EntitySpawnEvent event) {
        if (cfg.enabled && event.getEntity() instanceof EnderDragon dragon) {
            reconcile(dragon.getWorld(), dragon);
        }
    }

    private void scan() {
        if (!cfg.enabled) {
            return;
        }
        for (World world : Bukkit.getWorlds()) {
            if (world.getEnvironment() == World.Environment.THE_END) {
                reconcile(world, findDragon(world));
            }
        }
    }

    /** The battle's dragon if linked, else any live dragon in the world. */
    private EnderDragon findDragon(World world) {
        if (world == null) {
            return null;
        }
        DragonBattle battle = world.getEnderDragonBattle();
        if (battle != null && battle.getEnderDragon() != null && !battle.getEnderDragon().isDead()) {
            return battle.getEnderDragon();
        }
        for (EnderDragon d : world.getEntitiesByClass(EnderDragon.class)) {
            if (!d.isDead()) {
                return d;
            }
        }
        return null;
    }

    private void reconcile(World world, EnderDragon dragon) {
        WorldTrial wt = worlds.computeIfAbsent(world.getName(), k -> newWorldTrial());
        TrialStateMachine m = wt.machine;
        if (dragon == null) {
            if (m.hasBattle()) {
                // 0.2.7 (Iris audit): a dragon nobody is near is
                // UNLOADED, not gone - vanilla only keeps the arena
                // loaded while a player is in it. Up to 0.2.6 three
                // empty scans ended the battle, so a wipe (everyone
                // respawning in the Overworld), everyone logging off,
                // or a restart with nobody in the End sent a phase-2
                // dragon back to a fresh phase 1 at full health
                // (rig-proven on 0.2.6). Count a miss only when the
                // death was seen or someone stands at the arena.
                if (wt.deathSeen || arenaWatched(world)) {
                    wt.missScans++;
                    if (wt.missScans >= 3) {
                        finishBattle(world, wt, wt.deathSeen);
                    }
                } else {
                    wt.missScans = 0;
                    if (!wt.holdLogged) {
                        wt.holdLogged = true;
                        getLogger().info("Nobody at the " + world.getName() + " arena - the "
                                + m.phase() + " trial is held as it stands until someone "
                                + "returns (the dragon is unloaded, not gone).");
                    }
                }
            }
            return;
        }
        wt.missScans = 0;
        wt.holdLogged = false;
        String uuid = dragon.getUniqueId().toString();
        if (!m.tracks(uuid)) {
            if (m.hasBattle()) {
                // A different dragon than the one on record: the old
                // battle ended without our seeing it. Archive, then
                // fall through and treat this as a new battle.
                finishBattle(world, wt, wt.deathSeen);
            }
            if (wt.completedBattles > 0 && !cfg.applyToResummons) {
                if (!wt.ignoreLogged) {
                    wt.ignoreLogged = true;
                    getLogger().info("Dragon in " + world.getName() + " left vanilla "
                            + "(apply-to-resummons is off; " + wt.completedBattles
                            + " trial battle(s) already completed here).");
                }
                return;
            }
            if (staleFlagWorlds.remove(world.getName()) && dragon.isInvulnerable()) {
                // 0.2.11 (B-4): the wiped battle's saved state carried
                // our invulnerable flag; if that dragon still wears it,
                // the fresh battle must not inherit it.
                dragon.setInvulnerable(false);
                getLogger().info("Cleared an invulnerable flag left by the wiped stale trial in "
                        + world.getName() + " before registering the fresh battle.");
            }
            m.startBattle(uuid);
            wt.phaseBeats.reset();
            wt.deathSeen = false;
            wt.ignoreLogged = false;
            applyPhaseSetup(dragon, BattlePhase.PHASE1, true);
            saveState();
            getLogger().info("Two-phase trial registered in " + world.getName()
                    + ": dragon " + uuid + ", phase 1 at " + (int) cfg.phase1MaxHealth + " HP.");
            return;
        }
        // Same dragon we already track: repair any drift.
        if (m.invulnerableByUs() && m.phase() != BattlePhase.TRANSITION) {
            dragon.setInvulnerable(false);
            m.setInvulnerableByUs(false);
            saveState();
            getLogger().info("Cleared a stray invulnerable flag on the " + world.getName()
                    + " dragon (state " + m.phase() + ").");
        }
        applyPhaseSetup(dragon, m.phase(), false);
    }

    /**
     * Assert the health bar for the machine's phase. On a fresh battle
     * the dragon just spawned at full vanilla health, so topping up to
     * the phase-1 bar is exact. On reconcile we never grant health
     * beyond the original buff and never top up phase 2.
     */
    private void applyPhaseSetup(EnderDragon dragon, BattlePhase phase, boolean fresh) {
        var attr = dragon.getAttribute(Attribute.MAX_HEALTH);
        if (attr == null) {
            return;
        }
        double target = phase == BattlePhase.PHASE2 || phase == BattlePhase.TRANSITION
                ? cfg.phase2MaxHealth : cfg.phase1MaxHealth;
        // 0.3.0 intake (SPEC section 0.3): the bar carries the
        // ENTITY target - the phase pool clamped to the platform
        // cap - while the pool itself lives in the state machine's
        // and the host ledger's configured space.
        double entityTarget = IntakeFactor.entityMax(cfg.entityCap, target);
        double currentMax = attr.getBaseValue();
        if (Math.abs(currentMax - entityTarget) < 0.01) {
            return;
        }
        attr.setBaseValue(entityTarget);
        if (fresh) {
            dragon.setHealth(entityTarget);
        } else if (phase == BattlePhase.PHASE1 && Math.abs(currentMax - 200.0) < 0.01) {
            // Mid-fight first sight (state lost): grant exactly the
            // buff difference, no more (SPEC).
            dragon.setHealth(Math.min(entityTarget,
                    dragon.getHealth() + (entityTarget - 200.0)));
        } else {
            dragon.setHealth(Math.min(dragon.getHealth(), entityTarget));
            getLogger().info("Reconciled dragon max health " + (int) currentMax
                    + " -> " + (int) entityTarget + " for phase " + phase + ".");
        }
    }

    /** Radius (from 0,128,0) inside which vanilla keeps the arena loaded
     *  (vanilla uses 192; a margin keeps us well inside it). */
    private static final double ARENA_WATCH_RADIUS = 160.0;

    /** True when a living player is close enough that the arena (and
     *  so a live dragon) is loaded - an empty scan then means gone. */
    private boolean arenaWatched(World world) {
        for (Player p : world.getPlayers()) {
            if (p.isDead()) {
                continue;
            }
            Location l = p.getLocation();
            double dx = l.getX();
            double dy = l.getY() - 128.0;
            double dz = l.getZ();
            if (dx * dx + dy * dy + dz * dz <= ARENA_WATCH_RADIUS * ARENA_WATCH_RADIUS) {
                return true;
            }
        }
        return false;
    }

    /**
     * 0.2.7: damage a tracked dragon already carries when the host opens
     * a fresh ledger for it (a ledger lost to a restart) - set aside
     * from the share so the remaining fight can still earn the seal.
     */
    public double unseenDamage(EnderDragon dragon) {
        if (dragon == null) {
            return 0;
        }
        String uuid = dragon.getUniqueId().toString();
        for (WorldTrial wt : worlds.values()) {
            if (!wt.machine.tracks(uuid)) {
                continue;
            }
            var attr = dragon.getAttribute(Attribute.MAX_HEALTH);
            double currentMax = attr != null ? attr.getBaseValue() : 0;
            return switch (wt.machine.phase()) {
                case PHASE1 -> EncounterRetention.dragonUnseenScaled(false,
                        cfg.phase1MaxHealth, cfg.phase2MaxHealth, cfg.entityCap,
                        currentMax, dragon.getHealth());
                case TRANSITION, PHASE2 -> EncounterRetention.dragonUnseenScaled(true,
                        cfg.phase1MaxHealth, cfg.phase2MaxHealth, cfg.entityCap,
                        currentMax, dragon.getHealth());
                default -> 0.0;
            };
        }
        return 0;
    }

    private void finishBattle(World world, WorldTrial wt, boolean sawDeath) {
        BattlePhase was = wt.machine.phase();
        wt.machine.endBattle();
        wt.completedBattles++;
        wt.missScans = 0;
        wt.deathSeen = false;
        wt.ignoreLogged = false;
        saveState();
        if (was == BattlePhase.PHASE1 || was == BattlePhase.TRANSITION) {
            getLogger().warning("Trial battle in " + world.getName() + " ended during " + was
                    + " (death event seen: " + sawDeath + "). A phase-1 dragon should be "
                    + "unable to die in combat - if an operator /kill or another plugin "
                    + "removed it, check GuildGate's seal state (remedy: /guildgate revoke).");
        } else if (!sawDeath) {
            getLogger().warning("Trial battle in " + world.getName() + " ended during " + was
                    + " without a death event (the dragon vanished from a watched arena)."
                    + " Total completed here: " + wt.completedBattles + ".");
        } else {
            getLogger().info("Trial battle in " + world.getName() + " completed (phase " + was
                    + "). Total completed here: " + wt.completedBattles + ".");
        }
    }

    // ------------------------------------------------------------------
    // The flip — damage interception (Step 0's guarantee lives here)
    // ------------------------------------------------------------------

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = false)
    public void onDragonDamage(EntityDamageEvent event) {
        if (!cfg.enabled || !(event.getEntity() instanceof EnderDragon dragon)) {
            return;
        }
        World world = dragon.getWorld();
        if (world.getEnvironment() != World.Environment.THE_END) {
            return;
        }
        WorldTrial wt = worlds.computeIfAbsent(world.getName(), k -> newWorldTrial());
        if (!wt.machine.tracks(dragon.getUniqueId().toString())) {
            reconcile(world, dragon);
            if (!wt.machine.tracks(dragon.getUniqueId().toString())) {
                return;
            }
        }
        TrialStateMachine m = wt.machine;
        if (m.phase() == BattlePhase.TRANSITION) {
            event.setCancelled(true);
            return;
        }
        // 0.1.1: the governor runs BEFORE the state machine's verdict,
        // so the flip logic judges only governed damage. A trim scales
        // the event's base damage by the same proportion the governor
        // cut from the final damage.
        double rawDamage = event.getFinalDamage();
        DamageGovernor.Result governed = wt.governor.govern(rawDamage,
                isExplosion(event.getCause()), Bukkit.getCurrentTick(), cfg);
        double judged = governed.damage;
        // 0.4.0 (PM-9): hand the governed result to the host's
        // record handler in ledger space (the chapter IS this
        // chapter's governor) - the handler must never re-derive
        // the ledger number from a final damage another plugin
        // modified after this pass.
        com.winkreyes.excelbosstrial.core.GovernorStash.put(event, judged);
        // 0.3.0 intake (SPEC section 0.3): the governed number is
        // CONFIGURED space; the state machine judges the dragon's
        // ENTITY bar, and the event must land in entity space too.
        // Scale by the phase's factor - at a cap above the pools
        // the factor is 1 and this block is the identity.
        double phasePool = m.phase() == BattlePhase.PHASE1
                ? cfg.phase1MaxHealth : cfg.phase2MaxHealth;
        double intake = IntakeFactor.factor(cfg.entityCap, phasePool);
        double judgedEntity = judged * intake;
        if (governed.trimmed() || intake < 1.0) {
            if (rawDamage > 0) {
                event.setDamage(event.getDamage() * (judgedEntity / rawDamage));
            }
            if (governed.trimmed()) {
                getLogger().info("DragonTrial cap [" + governed.ruleLabel() + "] in "
                        + world.getName() + ": dragon hit trimmed " + fmt(rawDamage)
                        + " -> " + fmt(judged) + " (dragon at " + fmt(dragon.getHealth())
                        + " HP, phase " + m.phase() + ").");
            }
        }
        BattlePhase judgedPhase = m.phase();
        TrialStateMachine.Verdict verdict = m.onDamage(dragon.getHealth(), judgedEntity);
        dangerLog("high", world, event, rawDamage, judgedEntity, dragon.getHealth(), verdict, judgedPhase);
        switch (verdict) {
            case FLIP -> flipNow(event, world, dragon, m);
            case ALLOW_DEATH -> saveState();
            case PASS -> {
                double after = Math.max(0.0, dragon.getHealth() - judgedEntity);
                double max = dragon.getAttribute(Attribute.MAX_HEALTH) != null
                        ? dragon.getAttribute(Attribute.MAX_HEALTH).getValue()
                        : (m.phase() == BattlePhase.PHASE2 ? cfg.phase2MaxHealth : cfg.phase1MaxHealth);
                if (max > 0) {
                    // 0.3.2: milestone beats run PER PHASE (the
                    // tracker resets when a phase begins), judged on
                    // the same post-judgement fraction the waves use.
                    fireMilestoneBeats(world, wt, dragon, after / max);
                }
                if (m.phase() == BattlePhase.PHASE2 && cfg.addsEnabled && cfg.addsPerWave > 0) {
                    int pending = m.pendingWaveMask(max > 0 ? after / max : 0);
                    if (pending != 0) {
                        int spawned = spawnWaves(world, dragon, pending);
                        if (spawned != 0) {
                            m.commitWaves(spawned);
                            saveState();
                        }
                    }
                }
            }
        }
    }

    /**
     * 0.2.11 (B-1): the FLIP act, shared by the HIGH judgment and the
     * HIGHEST re-judgment so both passes perform exactly the same
     * synchronous flip: cancel the blow, run the flip, persist.
     */
    private void flipNow(EntityDamageEvent event, World world, EnderDragon dragon,
            TrialStateMachine m) {
        event.setCancelled(true);
        runFlip(world, dragon, m);
        saveState();
    }

    /**
     * 0.2.11 (B-1): the second, final judgment, at HIGHEST — after
     * every other plugin's damage modifiers have run. The Oct 30,
     * 2026 kill slipped past the single HIGH verdict: the blow was
     * judged non-lethal there and grew afterwards (deathConsumed
     * stayed false on a restored PHASE1 trial, and the dragon died).
     * This pass re-reads the event's final numbers and re-judges a
     * still-PHASE1 dragon; a FLIP here cancels the event before any
     * damage is applied, so no post-HIGH growth can kill a phase-1
     * dragon. Double judgment is safe by construction: PASS mutates
     * nothing, and a HIGH FLIP has already moved the machine to
     * TRANSITION, which this pass only ever cancels, never re-judges.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDragonDamageFinal(EntityDamageEvent event) {
        if (event.isCancelled()) {
            return;
        }
        if (!cfg.enabled || !(event.getEntity() instanceof EnderDragon dragon)) {
            return;
        }
        World world = dragon.getWorld();
        if (world.getEnvironment() != World.Environment.THE_END) {
            return;
        }
        WorldTrial wt = worlds.get(world.getName());
        if (wt == null) {
            return;
        }
        TrialStateMachine m = wt.machine;
        if (!m.tracks(dragon.getUniqueId().toString())) {
            return;
        }
        switch (m.phase()) {
            case TRANSITION -> event.setCancelled(true);
            case PHASE1 -> {
                double health = dragon.getHealth();
                double judged = event.getFinalDamage();
                TrialStateMachine.Verdict verdict = m.onDamage(health, judged);
                dangerLog("highest", world, event, judged, judged, health, verdict,
                        BattlePhase.PHASE1);
                if (verdict == TrialStateMachine.Verdict.FLIP) {
                    flipNow(event, world, dragon, m);
                }
            }
            default -> {
                // PHASE2 (and NONE/DONE): deliberately nothing. The one
                // real death must stay exactly as vanilla and the HIGH
                // pass left it — post-HIGH growth of a phase-2 killing
                // blow changes nothing the seal depends on.
            }
        }
    }

    /**
     * 0.2.11 (B-1): one log line per judged blow while a PHASE1 dragon
     * sits in the danger zone (TrialStateMachine.inDangerZone — health
     * at or below the per-event cap), from EACH pass, so an amplifier
     * that outruns the HIGH verdict leaves its fingerprints: the same
     * blow appears twice, the second line carrying the grown number.
     */
    private void dangerLog(String pass, World world, EntityDamageEvent event, double raw,
            double judged, double health, TrialStateMachine.Verdict verdict,
            BattlePhase judgedPhase) {
        if (judgedPhase == BattlePhase.PHASE1
                && TrialStateMachine.inDangerZone(health, cfg)) {
            getLogger().info("DragonTrial danger [" + pass + "] in " + world.getName()
                    + ": cause " + event.getCause()
                    + ", final " + fmt(raw) + ", judged " + fmt(judged)
                    + ", health " + fmt(health) + ", verdict " + verdict
                    + ", phase PHASE1.");
        }
    }

    /** Synchronous flip: heal onto the phase-2 bar, theatre, schedule the unveil. */
    private void runFlip(World world, EnderDragon dragon, TrialStateMachine m) {
        var attr = dragon.getAttribute(Attribute.MAX_HEALTH);
        // 0.3.0 intake: the phase-2 bar is the entity clamp of the
        // configured pool; the log line below still speaks in
        // configured space, which is the pool the seal judges.
        double entityPhase2 = IntakeFactor.entityMax(cfg.entityCap, cfg.phase2MaxHealth);
        if (attr != null) {
            attr.setBaseValue(entityPhase2);
        }
        dragon.setHealth(entityPhase2);
        dragon.setInvulnerable(true);
        switch (dragon.getPhase()) {
            case FLY_TO_PORTAL, LAND_ON_PORTAL, BREATH_ATTACK,
                    SEARCH_FOR_BREATH_ATTACK_TARGET, ROAR_BEFORE_ATTACK, HOVER ->
                    dragon.setPhase(EnderDragon.Phase.CIRCLING);
            default -> { }
        }
        Location fx = dragon.getLocation();
        DragonBattle battle = world.getEnderDragonBattle();
        Location fountain = battle != null && battle.getEndPortalLocation() != null
                ? battle.getEndPortalLocation() : fx;
        String title = color(msgTitle);
        String subtitle = color(msgSubtitle);
        String chat = color(msgChat);
        // 0.1.2: the theatre is fenced. In 0.1.0/0.1.1 a throwing
        // effects call (the DRAGON_BREATH Float bug) aborted this
        // method before the unveil below was scheduled, stranding
        // the dragon in TRANSITION. Effects can now fail loudly in
        // the log but never take the flip down with them.
        try {
            FlipEffects.play(world, fx, fountain, world.getPlayers(), title, subtitle, chat);
        } catch (RuntimeException ex) {
            getLogger().warning("Flip effects failed in " + world.getName()
                    + " (the flip itself continues): " + ex);
        }
        if (broadcastServerWide) {
            Bukkit.broadcastMessage(chat);
        }
        getLogger().info("PHASE FLIP in " + world.getName() + ": the seal resists - dragon "
                + "restored to " + (int) cfg.phase2MaxHealth + " HP (no death event; "
                + "GuildGate cannot credit). Invulnerable for " + cfg.invulnerableSeconds + "s.");
        final String uuid = dragon.getUniqueId().toString();
        Bukkit.getScheduler().runTaskLater(host, () -> {
            if (m.phase() == BattlePhase.TRANSITION && m.tracks(uuid) && !dragon.isDead()) {
                dragon.setInvulnerable(false);
                m.completeTransition();
                // 0.3.2: phase 2 is a fresh fight for the beats -
                // its 75/50/25% crossings speak for themselves.
                WorldTrial cur = worlds.get(world.getName());
                if (cur != null) {
                    cur.phaseBeats.reset();
                }
                saveState();
                getLogger().info("Phase 2 begins in " + world.getName() + ".");
            }
        }, Math.max(0L, cfg.invulnerableSeconds) * 20L);
    }

    // ------------------------------------------------------------------
    // P3 — no end-crystal placement while a trial battle is active
    // ------------------------------------------------------------------

    /**
     * A player right-clicking a block with an end crystal in the End
     * is the placement action, and the only one: battle-generated
     * tower crystals never pass through a player's interact event,
     * and the resummon ritual happens while no trial battle is
     * active (phase NONE/DONE), so neither is ever blocked.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCrystalPlace(PlayerInteractEvent event) {
        if (!cfg.enabled || !cfg.blockCrystalPlacement) {
            return;
        }
        if (event.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK
                || event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        ItemStack item = event.getItem();
        if (item == null || item.getType() != Material.END_CRYSTAL) {
            return;
        }
        Player player = event.getPlayer();
        World world = player.getWorld();
        if (world.getEnvironment() != World.Environment.THE_END) {
            return;
        }
        WorldTrial wt = worlds.get(world.getName());
        if (wt == null || !CrystalPlacementRule.blocks(cfg.blockCrystalPlacement,
                wt.machine.phase())) {
            return;
        }
        event.setCancelled(true);
        player.sendMessage(color("&5[ExcelBossTrial] &fThe trial is underway - end "
                + "crystals cannot be placed while the dragon lives."));
        getLogger().info("Crystal placement blocked in " + world.getName() + " during "
                + wt.machine.phase() + " (" + player.getName() + ").");
    }

    // ------------------------------------------------------------------
    // Bed ignitions (0.2.1, FLAG 3) — attribution for the share gate
    // ------------------------------------------------------------------

    /**
     * A player right-clicking a bed in the End lights it: vanilla
     * explodes the bed in the same action, as a BLOCK_EXPLOSION
     * with no damager. Record who lit it (player + block + tick)
     * so the host's ledger can attribute the blast's damage to
     * its lighter instead of against the family. Only the End is
     * recorded - the dragon lives nowhere else, and beds in the
     * Overworld do not explode at all.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBedIgnite(PlayerInteractEvent event) {
        if (!cfg.enabled) {
            return;
        }
        if (event.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK
                || event.getClickedBlock() == null) {
            return;
        }
        if (!org.bukkit.Tag.BEDS.isTagged(event.getClickedBlock().getType())) {
            return;
        }
        World world = event.getClickedBlock().getWorld();
        if (world == null || world.getEnvironment() != World.Environment.THE_END) {
            return;
        }
        Location b = event.getClickedBlock().getLocation();
        bedIgnitions.record(event.getPlayer().getUniqueId(),
                b.getX() + 0.5, b.getY() + 0.5, b.getZ() + 0.5,
                Bukkit.getCurrentTick());
    }

    /**
     * The player who lit a bed near this location within the
     * attribution window, or null (read by the host's damage
     * ledger for BLOCK_EXPLOSION damage to the dragon).
     */
    public UUID bedLighterNear(Location location) {
        if (location == null) {
            return null;
        }
        return bedIgnitions.lighterNear(location.getX(), location.getY(),
                location.getZ(), Bukkit.getCurrentTick());
    }

    /** Spawn the waves whose bits are set; returns the mask actually spawned. */
    /**
     * 0.3.2: the dragon's milestone beats, per phase. The beat
     * fires once per threshold per phase (the tracker resets when
     * a phase begins and persists with the trial state in between):
     * each crossed threshold gets its broadcast line and a log
     * stamp, and the first crossing of a damage event hands the
     * host one pulse surge through the sink (the host owns the
     * encounter ledger and its participants). Server-wide by
     * design - a seal fight's beats are server news, like the
     * main plugin's chapters.
     */
    private void fireMilestoneBeats(World world, WorldTrial wt, EnderDragon dragon,
                                    double remainingFraction) {
        // 0.3.3: the sprinkle rolls on the same per-phase stamps,
        // armed independently of the lines - the tracker advances
        // when either feature is live for the chapter, and this
        // returns exactly as 0.3.2 did while both are dark.
        if (!dragonBeats.enabled() && !sprinkleArmed) {
            return;
        }
        int pending = wt.phaseBeats.pending(remainingFraction);
        if (pending == 0) {
            return;
        }
        wt.phaseBeats.commit(pending);
        if (dragonBeats.enabled()) {
            boolean surged = false;
            for (int i = 0; i < MilestoneBeats.THRESHOLDS.length; i++) {
                if ((pending & (1 << i)) == 0) {
                    continue;
                }
                String text = dragonBeats.textFor(i);
                if (!text.isEmpty()) {
                    Bukkit.broadcastMessage(color(text.replace("{name}", "The Ender Dragon")));
                }
                getLogger().info("Dragon milestone [" + world.getName() + "]: "
                        + MilestoneBeats.thresholdPercent(i) + "% of phase "
                        + wt.machine.phase() + " remains - beat fired"
                        + (dragonBeats.surgeMultiplier() > 0 ? ", surge handed to the host" : "")
                        + ".");
                if (!surged && dragonBeats.surgeMultiplier() > 0 && milestoneSurgeSink != null) {
                    milestoneSurgeSink.accept(dragon.getUniqueId());
                    surged = true;
                }
            }
        }
        // 0.3.3: the elite sprinkle, PHASE 2 ONLY (study section
        // 7.3: phase 1 stays exactly as the rebalance leaves it).
        // The sink rolls per stamp bit against the dragon block.
        if (sprinkleArmed && sprinkleSink != null
                && wt.machine.phase() == BattlePhase.PHASE2) {
            sprinkleSink.onMilestones(dragon.getUniqueId(), pending, fountainAnchor(world));
        }
        saveState();
    }

    /** The fountain anchor: the End island's arena centre, where
     *  the chapter's own add waves spawn and where the sprinkle's
     *  leash holds its endermen (the dragon flies - an anchor
     *  chasing it would drop adds into the void). */
    private Location fountainAnchor(World world) {
        DragonBattle battle = world.getEnderDragonBattle();
        return battle != null && battle.getEndPortalLocation() != null
                ? battle.getEndPortalLocation()
                : new Location(world, 0, world.getHighestBlockYAt(0, 0) + 1, 0);
    }

    private int spawnWaves(World world, EnderDragon dragon, int pendingMask) {
        long alive = world.getEntitiesByClass(Enderman.class).stream()
                .filter(e -> e.getPersistentDataContainer().has(addKey, PersistentDataType.BYTE))
                .count();
        Location center = fountainAnchor(world);
        int spawnedMask = 0;
        int waves = Integer.bitCount(pendingMask);
        for (int w = 0; w < waves; w++) {
            if (alive >= cfg.addsMaxAlive) {
                break;
            }
            int bit = Integer.lowestOneBit(pendingMask & ~spawnedMask);
            int count = Math.min(cfg.addsPerWave, (int) (cfg.addsMaxAlive - alive));
            for (int i = 0; i < count; i++) {
                double angle = random.nextDouble() * Math.PI * 2.0;
                double radius = 3.0 + random.nextDouble() * 4.0;
                Location loc = center.clone().add(Math.cos(angle) * radius, 1.0, Math.sin(angle) * radius);
                loc.setY(world.getHighestBlockYAt(loc) + 1);
                Enderman add = world.spawn(loc, Enderman.class);
                add.setCustomName(color(addName));
                add.setCustomNameVisible(true);
                add.getPersistentDataContainer().set(addKey, PersistentDataType.BYTE, (byte) 1);
                alive++;
            }
            spawnedMask |= bit;
            getLogger().info("Phase-2 add wave in " + world.getName() + " (threshold bit " + bit
                    + "): " + count + " Sealbound Endermen at the fountain.");
        }
        return spawnedMask;
    }

    // ------------------------------------------------------------------
    // Phase-2 outgoing damage buff
    // ------------------------------------------------------------------

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDragonDealtDamage(EntityDamageByEntityEvent event) {
        if (!cfg.enabled) {
            return;
        }
        DamagerKind kind = classify(event.getDamager());
        if (kind == DamagerKind.OTHER) {
            return;
        }
        EnderDragon dragon = dragonOf(event.getDamager());
        if (dragon == null || dragon.getWorld().getEnvironment() != World.Environment.THE_END) {
            return;
        }
        WorldTrial wt = worlds.get(dragon.getWorld().getName());
        if (wt == null || !wt.machine.tracks(dragon.getUniqueId().toString())) {
            return;
        }
        if (wt.machine.buffApplies(kind)) {
            event.setDamage(TrialStateMachine.scaledDamage(event.getDamage(), cfg));
        }
    }

    private EnderDragon dragonOf(Entity damager) {
        if (damager instanceof EnderDragon d) {
            return d;
        }
        if (damager instanceof ComplexEntityPart part && part.getParent() instanceof EnderDragon d) {
            return d;
        }
        if (damager instanceof Projectile proj && proj.getShooter() instanceof EnderDragon d) {
            return d;
        }
        if (damager instanceof AreaEffectCloud cloud && cloud.getSource() instanceof EnderDragon d) {
            return d;
        }
        return null;
    }

    private DamagerKind classify(Entity damager) {
        if (damager instanceof EnderDragon || damager instanceof ComplexEntityPart) {
            return DamagerKind.CONTACT;
        }
        if (damager instanceof Projectile proj) {
            return proj.getShooter() instanceof EnderDragon ? DamagerKind.FIREBALL : DamagerKind.OTHER;
        }
        if (damager instanceof AreaEffectCloud cloud) {
            return cloud.getSource() instanceof EnderDragon ? DamagerKind.BREATH : DamagerKind.OTHER;
        }
        return DamagerKind.OTHER;
    }

    // ------------------------------------------------------------------
    // Phase-2 aggression (requests only — vanilla AI owns the dragon)
    // ------------------------------------------------------------------

    private void aggression() {
        if (!cfg.enabled) {
            return;
        }
        for (Map.Entry<String, WorldTrial> e : worlds.entrySet()) {
            WorldTrial wt = e.getValue();
            if (wt.machine.phase() != BattlePhase.PHASE2) {
                continue;
            }
            World world = Bukkit.getWorld(e.getKey());
            if (world == null || world.getPlayers().isEmpty()) {
                continue;
            }
            EnderDragon dragon = findDragon(world);
            if (dragon == null || !wt.machine.tracks(dragon.getUniqueId().toString())) {
                continue;
            }
            EnderDragon.Phase now = dragon.getPhase();
            if (now != EnderDragon.Phase.CIRCLING && now != EnderDragon.Phase.HOVER) {
                continue;
            }
            if (random.nextDouble() >= cfg.aggressionChance) {
                continue;
            }
            dragon.setPhase(random.nextBoolean()
                    ? EnderDragon.Phase.CHARGE_PLAYER : EnderDragon.Phase.STRAFING);
        }
    }

    // ------------------------------------------------------------------
    // Death bookkeeping (observation only — vanilla does the rest)
    // ------------------------------------------------------------------

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        if (!(entity instanceof EnderDragon)) {
            return;
        }
        WorldTrial wt = worlds.get(entity.getWorld().getName());
        if (wt != null && wt.machine.tracks(entity.getUniqueId().toString())) {
            wt.deathSeen = true;
            getLogger().info("Tracked dragon died in " + entity.getWorld().getName()
                    + " during " + wt.machine.phase() + " (deathConsumed="
                    + wt.machine.deathConsumed() + "). Vanilla + GuildGate proceed as stock.");
        }
    }

    /**
     * The share denominator for a tracked dragon's encounter
     * (0.2.1 dragon share gate, read by the host's verdict): the
     * phase pools the fight actually spans - phase 1 alone until
     * the flip fires, phase 1 + phase 2 from the flip on. The
     * ledger in the host captures only the dragon's current bar
     * (250 in phase 1), so without this the share would be
     * measured against half the real fight. Returns 0 when this
     * chapter does not track the dragon (a vanilla fight, e.g.
     * apply-to-resummons off): the host then keeps the dragon's
     * own bar as the denominator.
     */
    public double trialPool(UUID dragonUuid) {
        if (dragonUuid == null) {
            return 0;
        }
        String uuid = dragonUuid.toString();
        for (WorldTrial wt : worlds.values()) {
            if (wt.machine.tracks(uuid)) {
                return switch (wt.machine.phase()) {
                    case PHASE1 -> cfg.phase1MaxHealth;
                    case TRANSITION, PHASE2, DONE ->
                            cfg.phase1MaxHealth + cfg.phase2MaxHealth;
                    case NONE -> 0.0;
                };
            }
        }
        return 0;
    }

    /**
     * 0.3.0: the pool of the phase the dragon is IN right now
     * (not the whole-trial pool {@link #trialPool} reports): the
     * host's record handler divides each hit by this phase's
     * intake factor, because the entity bar only ever carries one
     * phase at a time. 0 when untracked.
     */
    public double currentPhasePool(UUID dragonUuid) {
        if (dragonUuid == null) {
            return 0;
        }
        String uuid = dragonUuid.toString();
        for (WorldTrial wt : worlds.values()) {
            if (wt.machine.tracks(uuid)) {
                return switch (wt.machine.phase()) {
                    case PHASE1 -> cfg.phase1MaxHealth;
                    case TRANSITION, PHASE2, DONE -> cfg.phase2MaxHealth;
                    case NONE -> 0.0;
                };
            }
        }
        return 0;
    }

    // ------------------------------------------------------------------
    // Status (the host's /bosstrial renders these)
    // ------------------------------------------------------------------

    public List<String> statusLines() {
        List<String> lines = new ArrayList<>();
        lines.add("&5[Dragon chapter] &fExcelDragonTrial 0.1.2 port - enabled=" + cfg.enabled
                + ", apply-to-resummons=" + cfg.applyToResummons);
        lines.add("&7" + capsSummary());
        boolean any = false;
        for (World world : Bukkit.getWorlds()) {
            if (world.getEnvironment() != World.Environment.THE_END) {
                continue;
            }
            any = true;
            WorldTrial wt = worlds.get(world.getName());
            EnderDragon d = findDragon(world);
            String hp = d == null ? "-" : (int) d.getHealth() + "/" + (int) d.getMaxHealth();
            lines.add("&7- " + world.getName() + ": phase="
                    + (wt == null ? "NONE" : wt.machine.phase().name())
                    + ", dragon HP " + hp
                    + ", waves fired=" + (wt == null ? 0 : Integer.bitCount(wt.machine.wavesFiredMask()))
                    + ", completed battles=" + (wt == null ? 0 : wt.completedBattles));
        }
        if (!any) {
            lines.add("&7- no End world loaded.");
        }
        return lines;
    }

    /** Explosion classification for P2: block + entity explosions pool together. */
    private static boolean isExplosion(EntityDamageEvent.DamageCause cause) {
        return cause == EntityDamageEvent.DamageCause.BLOCK_EXPLOSION
                || cause == EntityDamageEvent.DamageCause.ENTITY_EXPLOSION;
    }

    /** One-line caps state for the boot line, status and reload outputs. */
    private String capsSummary() {
        return "caps: per-event " + (cfg.perEventCapEnabled()
                ? "<= " + fmt(cfg.perEventDamageCap) + " HP" : "off")
                + " | explosion " + (cfg.explosionBudgetEnabled()
                ? "<= " + fmt(cfg.explosionTickBudget) + " HP/tick" : "off")
                + " | crystal placement " + (cfg.blockCrystalPlacement
                ? "locked during battle" : "unlocked");
    }

    /** Whole numbers print without a decimal point (60, not 60.0). */
    private static String fmt(double v) {
        if (!Double.isInfinite(v) && v == Math.floor(v)) {
            return Long.toString(Math.round(v));
        }
        return Double.toString(v);
    }

    private static String color(String s) {
        return ChatColor.translateAlternateColorCodes('&', s == null ? "" : s);
    }
}


package com.winkreyes.excelbosstrial.dragon;

import com.winkreyes.excelbosstrial.core.EncounterRetention;
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
                "caps.per-event-damage", "caps.explosion-per-tick",
                "caps.block-crystal-placement"}) {
            if (root != null && root.contains(key)) {
                flat.put(key, root.get(key));
            }
        }
        cfg = TrialConfig.fromMap(flat);
        msgTitle = sectionString("messages.title", "&5&lThe Seal Resists");
        msgSubtitle = sectionString("messages.subtitle", "&fThe dragon refuses to fall.");
        msgChat = sectionString("messages.chat",
                "&5The dragon's death-cry shatters against the first seal - and it rises again, "
                        + "wreathed in the valley's old anger.");
        addName = sectionString("adds.name", "&5Sealbound Enderman");
        broadcastServerWide = sectionBool("transition.broadcast-server-wide", false);
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
        ConfigurationSection section = y.getConfigurationSection("worlds");
        if (section == null) {
            return;
        }
        for (String worldName : section.getKeys(false)) {
            String base = "worlds." + worldName + ".";
            WorldTrial wt = new WorldTrial();
            wt.completedBattles = y.getInt(base + "completed-battles", 0);
            String phaseName = y.getString(base + "phase", "NONE");
            String uuid = y.getString(base + "dragon-uuid", null);
            BattlePhase phase;
            try {
                phase = BattlePhase.valueOf(phaseName);
            } catch (IllegalArgumentException e) {
                phase = BattlePhase.NONE;
            }
            if (uuid != null && phase != BattlePhase.NONE && phase != BattlePhase.DONE) {
                wt.machine.restore(phase, uuid, y.getInt(base + "waves-fired", 0),
                        y.getBoolean(base + "death-consumed", false),
                        y.getBoolean(base + "invulnerable-by-us", false));
                getLogger().info("Restored trial state for " + worldName + ": " + wt
                        .machine.phase() + ", dragon " + uuid + ".");
            }
            worlds.put(worldName, wt);
        }
    }

    private void saveState() {
        YamlConfiguration y = new YamlConfiguration();
        for (Map.Entry<String, WorldTrial> e : worlds.entrySet()) {
            String base = "worlds." + e.getKey() + ".";
            WorldTrial wt = e.getValue();
            y.set(base + "completed-battles", wt.completedBattles);
            y.set(base + "phase", wt.machine.phase().name());
            y.set(base + "dragon-uuid", wt.machine.dragonUuid());
            y.set(base + "waves-fired", wt.machine.wavesFiredMask());
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
        WorldTrial wt = worlds.computeIfAbsent(world.getName(), k -> new WorldTrial());
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
            m.startBattle(uuid);
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
        double currentMax = attr.getBaseValue();
        if (Math.abs(currentMax - target) < 0.01) {
            return;
        }
        attr.setBaseValue(target);
        if (fresh) {
            dragon.setHealth(target);
        } else if (phase == BattlePhase.PHASE1 && Math.abs(currentMax - 200.0) < 0.01) {
            // Mid-fight first sight (state lost): grant exactly the
            // buff difference, no more (SPEC).
            dragon.setHealth(Math.min(target, dragon.getHealth() + (target - 200.0)));
        } else {
            dragon.setHealth(Math.min(dragon.getHealth(), target));
            getLogger().info("Reconciled dragon max health " + (int) currentMax
                    + " -> " + (int) target + " for phase " + phase + ".");
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
                case PHASE1 -> EncounterRetention.dragonUnseen(false, cfg.phase1MaxHealth,
                        cfg.phase2MaxHealth, currentMax, dragon.getHealth());
                case TRANSITION, PHASE2 -> EncounterRetention.dragonUnseen(true,
                        cfg.phase1MaxHealth, cfg.phase2MaxHealth, currentMax, dragon.getHealth());
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
        WorldTrial wt = worlds.computeIfAbsent(world.getName(), k -> new WorldTrial());
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
        if (governed.trimmed()) {
            if (rawDamage > 0) {
                event.setDamage(event.getDamage() * (judged / rawDamage));
            }
            getLogger().info("DragonTrial cap [" + governed.ruleLabel() + "] in "
                    + world.getName() + ": dragon hit trimmed " + fmt(rawDamage)
                    + " -> " + fmt(judged) + " (dragon at " + fmt(dragon.getHealth())
                    + " HP, phase " + m.phase() + ").");
        }
        TrialStateMachine.Verdict verdict = m.onDamage(dragon.getHealth(), judged);
        switch (verdict) {
            case FLIP -> {
                event.setCancelled(true);
                runFlip(world, dragon, m);
                saveState();
            }
            case ALLOW_DEATH -> saveState();
            case PASS -> {
                if (m.phase() == BattlePhase.PHASE2 && cfg.addsEnabled && cfg.addsPerWave > 0) {
                    double after = Math.max(0.0, dragon.getHealth() - judged);
                    double max = dragon.getAttribute(Attribute.MAX_HEALTH) != null
                            ? dragon.getAttribute(Attribute.MAX_HEALTH).getValue()
                            : cfg.phase2MaxHealth;
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

    /** Synchronous flip: heal onto the phase-2 bar, theatre, schedule the unveil. */
    private void runFlip(World world, EnderDragon dragon, TrialStateMachine m) {
        var attr = dragon.getAttribute(Attribute.MAX_HEALTH);
        if (attr != null) {
            attr.setBaseValue(cfg.phase2MaxHealth);
        }
        dragon.setHealth(cfg.phase2MaxHealth);
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
    private int spawnWaves(World world, EnderDragon dragon, int pendingMask) {
        long alive = world.getEntitiesByClass(Enderman.class).stream()
                .filter(e -> e.getPersistentDataContainer().has(addKey, PersistentDataType.BYTE))
                .count();
        DragonBattle battle = world.getEnderDragonBattle();
        Location center = battle != null && battle.getEndPortalLocation() != null
                ? battle.getEndPortalLocation()
                : new Location(world, 0, world.getHighestBlockYAt(0, 0) + 1, 0);
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


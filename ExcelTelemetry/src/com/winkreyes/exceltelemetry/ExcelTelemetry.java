package com.winkreyes.exceltelemetry;

import com.winkreyes.exceltelemetry.core.DamageLedger;
import com.winkreyes.exceltelemetry.core.Encounter;
import com.winkreyes.exceltelemetry.core.EventShaper;
import com.winkreyes.exceltelemetry.core.IntegrityFlags;
import com.winkreyes.exceltelemetry.core.Json;
import com.winkreyes.exceltelemetry.core.JsonlReader;
import com.winkreyes.exceltelemetry.core.LastDamage;
import com.winkreyes.exceltelemetry.core.LedgerStore;
import com.winkreyes.exceltelemetry.integration.EliteMobsReader;
import com.winkreyes.exceltelemetry.integration.McMmoBridge;
import com.winkreyes.exceltelemetry.integration.RewardGuardOrigin;
import com.winkreyes.exceltelemetry.integration.SealReader;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;
import org.bukkit.Bukkit;
import org.bukkit.Raid;
import org.bukkit.attribute.Attribute;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EnderDragon;
import com.winkreyes.exceltelemetry.integration.TrialCompatibility;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Raider;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.entity.Wither;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityRemoveEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.raid.RaidFinishEvent;
import org.bukkit.event.raid.RaidSpawnWaveEvent;
import org.bukkit.event.raid.RaidStopEvent;
import org.bukkit.event.raid.RaidTriggerEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * ExcelTelemetry 0.1.0 - observation-only player-power tracking for
 * seal tuning (WRV-085). Captures kills with per-victim damage
 * ledgers, player deaths with cause + gear, boss-encounter ledgers
 * (including FAILED attempts), mcMMO level-ups and snapshots, and
 * boss advancements into append-only JSONL in this plugin's data
 * folder. It enforces nothing, pays nothing, and exposes no public
 * commands (console /telemetry status only). Player data stays in
 * private local analysis storage when pulled - never on shared cloud storage.
 */
public final class ExcelTelemetry extends JavaPlugin implements Listener {

    static final ZoneId ZONE = ZoneId.of("America/Chicago");
    static final DateTimeFormatter TS = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    private static final Set<String> BOSS_KINDS = Set.of(
            "ENDER_DRAGON", "WITHER", "ELDER_GUARDIAN", "WARDEN");
    private static final Map<String, String> SEAL_BY_BOSS = Map.of(
            "ender_dragon", "ender_dragon", "wither", "wither",
            "elder_guardian", "elder_guardian", "warden", "warden", "raid", "raid");

    // config
    private boolean capKills = true, capDeaths = true, capBosses = true,
            capMcMmo = true, capAdvancements = true;
    private long ledgerTtlMs = 600_000;
    private long encounterIdleMs = 600_000;
    private double phase1Hp = 250, phase2Hp = 300;
    private int snapshotIntervalSec = 3600;
    private IntegrityFlags.Thresholds thresholds = IntegrityFlags.Thresholds.defaults();
    private Set<String> advancementWatch = Set.of(
            "minecraft:end/kill_dragon", "minecraft:nether/summon_wither", "minecraft:end/root");

    // state
    private LedgerStore ledgers;
    private final LastDamage lastDamage = new LastDamage();
    private final Map<String, Encounter> encounters = new HashMap<>();
    private final Map<String, Boolean> sealAtOpen = new HashMap<>();
    private final Map<String, Map<String, Object>> encounterExtras = new HashMap<>();
    private final Map<UUID, String> spawnReasons = new HashMap<>();
    private final Map<UUID, Long> spawnReasonAt = new HashMap<>();
    private final Map<UUID, EliteMobsReader.EliteInfo> eliteCache = new HashMap<>();
    private final Map<UUID, String> playerNames = new HashMap<>();
    // Last-known mcMMO levels per player: the quit snapshot's safety net.
    // mcMMO unloads a profile during the quit event and Bukkit already
    // reports the player offline by then, so the quit read can fail soft
    // to this cache (rig-proven 2026-10-18: without it, quit lines were
    // silently dropped). Values are real observed levels, never invented.
    private final Map<UUID, Map<String, Integer>> lastLevels = new HashMap<>();
    private final Map<UUID, Integer> lastPower = new HashMap<>();

    private final EliteMobsReader eliteReader = new EliteMobsReader();
    private final SealReader sealReader = new SealReader();
    private final RewardGuardOrigin originReader = new RewardGuardOrigin();
    private McMmoBridge mcMmo;
    private JsonlWriter writer;

    // ------------------------------------------------------------------
    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadConfigValues();
        ledgers = new LedgerStore(ledgerTtlMs);
        writer = new JsonlWriter(getDataFolder().toPath());
        writer.start();
        getServer().getPluginManager().registerEvents(this, this);

        if (capMcMmo && getServer().getPluginManager().getPlugin("mcMMO") != null) {
            try {
                mcMmo = new McMmoBridge(up -> {
                    Map<String, Object> line = EventShaper.mcmmoLevelUp(
                            up.uuid(), up.name(), up.skill(), up.level(), up.levelsGained());
                    if (line != null) emit("events", line);
                    try {
                        UUID id = UUID.fromString(up.uuid());
                        lastLevels.computeIfAbsent(id, k -> new LinkedHashMap<>())
                                .put(up.skill(), up.level());
                        lastPower.merge(id, up.levelsGained(), Integer::sum);
                    } catch (RuntimeException ignored) {
                        // cache is best-effort; the event line above is the record
                    }
                });
                getServer().getPluginManager().registerEvents(mcMmo, this);
            } catch (Throwable t) {
                getLogger().warning("mcMMO present but its bridge failed to load ("
                        + t.getMessage() + ") - mcMMO capture off, everything else on.");
                mcMmo = null;
            }
        }
        if (snapshotIntervalSec > 0) {
            getServer().getScheduler().runTaskTimer(this,
                    () -> Bukkit.getOnlinePlayers().forEach(p -> snapshot(p, "hourly")),
                    snapshotIntervalSec * 20L, snapshotIntervalSec * 20L);
        }
        getServer().getScheduler().runTaskTimer(this, this::sweep, 1200L, 1200L);

        getLogger().info("ExcelTelemetry 0.1.1 awake: observing (kills=" + capKills
                + ", deaths=" + capDeaths + ", bosses=" + capBosses
                + ", mcMMO=" + (mcMmo != null) + ", advancements=" + capAdvancements
                + "). Data folder: " + getDataFolder().getPath());
    }

    @Override
    public void onDisable() {
        long now = System.currentTimeMillis();
        for (Encounter enc : new ArrayList<>(encounters.values())) {
            if (enc.isOpen()) {
                enc.addMarker("observer_stopped", now, "plugin disabled; observation ended, not proof of defeat");
                closeEncounter(enc, Encounter.Outcome.ABANDONED, now);
            }
        }
        if (writer != null) writer.stop();
    }

    private void loadConfigValues() {
        var c = getConfig();
        capKills = c.getBoolean("capture.kills", true);
        capDeaths = c.getBoolean("capture.deaths", true);
        capBosses = c.getBoolean("capture.bosses", true);
        capMcMmo = c.getBoolean("capture.mcmmo", true);
        capAdvancements = c.getBoolean("capture.advancements", true);
        ledgerTtlMs = Math.max(30, c.getInt("ledger.ttl-minutes", 10)) * 60_000L;
        encounterIdleMs = Math.max(1, c.getInt("encounter.idle-close-minutes", 10)) * 60_000L;
        phase1Hp = c.getDouble("dragon.phase1-max-health", 250);
        phase2Hp = c.getDouble("dragon.phase2-max-health", 300);
        snapshotIntervalSec = c.getInt("mcmmo.snapshot-interval-seconds", 3600);
        double share = c.getDouble("integrity.explosion-share-threshold", 0.60);
        Map<String, Integer> fast = new LinkedHashMap<>();
        var fastSec = c.getConfigurationSection("integrity.fast-kill-seconds");
        IntegrityFlags.Thresholds d = IntegrityFlags.Thresholds.defaults();
        if (fastSec != null) {
            for (String k : fastSec.getKeys(false)) fast.put(k, fastSec.getInt(k));
        } else {
            fast.putAll(d.fastKillSeconds());
        }
        thresholds = new IntegrityFlags.Thresholds(share, fast);
        List<String> watch = c.getStringList("advancements.watch");
        if (!watch.isEmpty()) advancementWatch = new HashSet<>(watch);
    }

    // ------------------------------------------------------------------
    // Writing
    private record Line(String kind, String json) {}

    private static final class JsonlWriter implements Runnable {
        private final Path dir;
        private final BlockingQueue<Line> queue = new LinkedBlockingQueue<>(4096);
        private final AtomicLong dropped = new AtomicLong();
        private final AtomicLong written = new AtomicLong();
        private volatile boolean running = true;
        private Thread thread;

        JsonlWriter(Path dir) { this.dir = dir; }

        void start() {
            thread = new Thread(this, "ExcelTelemetry-Writer");
            thread.setDaemon(true);
            thread.start();
        }

        void offer(String kind, String json) {
            if (!queue.offer(new Line(kind, json))) dropped.incrementAndGet();
        }

        @Override
        public void run() {
            try {
                Files.createDirectories(dir);
                while (running || !queue.isEmpty()) {
                    Line line = queue.poll(500, java.util.concurrent.TimeUnit.MILLISECONDS);
                    if (line == null) continue;
                    String file = ("snapshots".equals(line.kind()) ? "snapshots-" : "events-")
                            + LocalDate.now(ZONE) + ".jsonl";
                    Files.writeString(dir.resolve(file), line.json() + "\n",
                            StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                    written.incrementAndGet();
                }
            } catch (Throwable t) {
                Bukkit.getLogger().severe("ExcelTelemetry writer died: " + t);
            }
        }

        void stop() {
            running = false;
            if (thread != null) {
                try { thread.join(3000); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
            }
        }
    }

    private void emit(String kind, Map<String, Object> payload) {
        if (writer == null || payload == null) return;
        long now = System.currentTimeMillis();
        Map<String, Object> line = new LinkedHashMap<>();
        line.put("ts", TS.format(Instant.ofEpochMilli(now).atZone(ZONE)));
        line.put("ts_ms", now);
        line.putAll(payload);
        writer.offer(kind, Json.write(line));
    }

    // ------------------------------------------------------------------
    // Attacker resolution
    private record Attacker(String key, String kind, String type, String uuid, String name) {}

    private Attacker resolveAttacker(EntityDamageEvent e) {
        if (e instanceof EntityDamageByEntityEvent by) {
            Entity damager = by.getDamager();
            if (damager instanceof Player p) {
                playerNames.put(p.getUniqueId(), p.getName());
                return new Attacker("player:" + p.getUniqueId(), "player", "PLAYER",
                        p.getUniqueId().toString(), p.getName());
            }
            if (damager instanceof Projectile proj) {
                Object shooter = proj.getShooter();
                if (shooter instanceof Player p) {
                    playerNames.put(p.getUniqueId(), p.getName());
                    return new Attacker("player:" + p.getUniqueId(), "player", "PLAYER",
                            p.getUniqueId().toString(), p.getName());
                }
                if (shooter instanceof LivingEntity le) {
                    return new Attacker("mob:" + le.getType().name(), "mob",
                            le.getType().name(), le.getUniqueId().toString(), null);
                }
                return new Attacker("env:projectile", "environment", damager.getType().name(), null, null);
            }
            if (damager instanceof TNTPrimed tnt) {
                Entity src = tnt.getSource();
                if (src instanceof Player p) {
                    playerNames.put(p.getUniqueId(), p.getName());
                    return new Attacker("player:" + p.getUniqueId(), "player", "PLAYER",
                            p.getUniqueId().toString(), p.getName());
                }
                if (src instanceof LivingEntity le) {
                    return new Attacker("mob:" + le.getType().name(), "mob",
                            le.getType().name(), le.getUniqueId().toString(), null);
                }
                return new Attacker("env:explosion", "environment", "TNT", null, null);
            }
            if (damager instanceof LivingEntity le) {
                return new Attacker("mob:" + le.getType().name(), "mob",
                        le.getType().name(), le.getUniqueId().toString(), null);
            }
            return new Attacker("mob:" + damager.getType().name(), "mob",
                    damager.getType().name(), damager.getUniqueId().toString(), null);
        }
        return new Attacker("env:" + e.getCause().name().toLowerCase(), "environment", null, null, null);
    }

    // ------------------------------------------------------------------
    // Damage: ledgers, last-damage, boss encounters
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent e) {
        double dmg = e.getFinalDamage();
        if (dmg <= 0) return;
        Entity victim = e.getEntity();
        long now = System.currentTimeMillis();
        String cause = e.getCause().name();
        Attacker atk = resolveAttacker(e);

        if (victim instanceof Player p) {
            lastDamage.record(p.getUniqueId(), new LastDamage.Hit(
                    cause, atk.kind(), atk.type(), atk.uuid(), now));
            if (capBosses) {
                Encounter enc = encounterAttackedBy(e);
                if (enc != null) enc.recordDamageTaken(p.getUniqueId().toString(), p.getName(), dmg, now);
            }
            return;
        }
        if (!(victim instanceof LivingEntity living)) return;

        // cache elite status early - EliteMobs may unregister at death
        EliteMobsReader.EliteInfo ei = eliteReader.inspect(living);
        if (ei.elite()) eliteCache.put(living.getUniqueId(), ei);

        if (capKills || capBosses) {
            ledgers.getOrCreate(living.getUniqueId(), now).record(atk.key(), cause, dmg, now);
        }
        if (!capBosses) return;

        if (isBoss(victim.getType().name())) {
            Encounter enc = encounters.get("boss:" + living.getUniqueId());
            if ((enc == null || !enc.isOpen()) && atk.key().startsWith("player:")) {
                enc = openBossEncounter(living, now); // only player damage opens a fight
            }
            if (enc != null && enc.isOpen()) {
                // once open, EVERY hit counts - the explosion share of
                // boss damage is an integrity signal, not player-only data
                enc.recordDamageToBoss(atk.key(), "player".equals(atk.kind()) ? atk.uuid() : null,
                        atk.name(), cause, dmg, phaseFor(living), now);
            }
        } else if (victim instanceof Raider raider) {
            Encounter enc = raidEncounterFor(raider.getRaid(), victim.getWorld().getName(), now, false);
            if (enc != null) {
                enc.recordDamageToBoss(atk.key(), "player".equals(atk.kind()) ? atk.uuid() : null,
                        atk.name(), cause, dmg, null, now);
            }
        }
    }

    /** The open encounter whose boss (or raid) is the damager in this event, if any. */
    private Encounter encounterAttackedBy(EntityDamageEvent e) {
        if (!(e instanceof EntityDamageByEntityEvent by)) return null;
        Entity damager = by.getDamager();
        Entity source = damager;
        if (damager instanceof Projectile proj && proj.getShooter() instanceof Entity s) source = s;
        if (source instanceof Raider raider) {
            return raidEncounterFor(raider.getRaid(), source.getWorld().getName(),
                    System.currentTimeMillis(), false);
        }
        if (source instanceof LivingEntity le && isBoss(le.getType().name())) {
            // Feeds damage-taken into an ALREADY open encounter only:
            // a fight opens when a player engages (design rule), never
            // because a boss hit a bystander first.
            Encounter enc = encounters.get("boss:" + le.getUniqueId());
            return (enc != null && enc.isOpen()) ? enc : null;
        }
        return null;
    }

    private boolean isBoss(String entityType) {
        return BOSS_KINDS.contains(entityType);
    }

    private String bossKind(String entityType) {
        return entityType.toLowerCase();
    }

    private String phaseFor(Entity boss) {
        // Read the active provider's health signatures, never its consume-on-use XP feed.
        if (boss instanceof EnderDragon) {
            try {
                var attr = ((LivingEntity) boss).getAttribute(Attribute.MAX_HEALTH);
                if (attr == null) return "unknown";
                double p1 = phase1Hp, p2 = phase2Hp;
                var trial = trialProvider();
                if (trial instanceof org.bukkit.plugin.java.JavaPlugin jp) {
                    String prefix = "ExcelBossTrial".equals(trial.getName()) ? "dragon." : "";
                    var cfg = jp.getConfig();
                    if (!TrialCompatibility.chapterActive(cfg.getBoolean("enabled", true),
                            cfg.getBoolean(prefix + "enabled", true))) return "unknown";
                    p1 = cfg.getDouble(prefix + "phase1.max-health", p1);
                    p2 = cfg.getDouble(prefix + "phase2.max-health", p2);
                }
                return TrialCompatibility.phase(attr.getValue(), p1, p2);
            } catch (Throwable t) {
                return "unknown";
            }
        }
        if (boss instanceof Wither w) return w.isCharged() ? "phase2" : "phase1";
        return null;
    }

    private Encounter openBossEncounter(LivingEntity boss, long now) {
        String key = "boss:" + boss.getUniqueId();
        Encounter enc = encounters.get(key);
        if (enc != null && enc.isOpen()) return enc;
        String kind = bossKind(boss.getType().name());
        enc = new Encounter(key, kind, boss.getUniqueId().toString(),
                boss.getWorld().getName(), now);
        encounters.put(key, enc);
        String sealId = SEAL_BY_BOSS.get(kind);
        if (sealId != null) {
            Boolean broken = sealReader.isBroken(sealId);
            if (broken != null) sealAtOpen.put(key, broken);
        }
        if ("ender_dragon".equals(kind)) {
            String trialPhase = readTrialPhase(boss.getWorld().getName(), boss.getUniqueId());
            if (trialPhase != null) {
                encounterExtras.computeIfAbsent(key, k -> new LinkedHashMap<>())
                        .put("trial_phase_at_open", trialPhase);
            }
        }
        return enc;
    }

    private Encounter raidEncounterFor(Raid raid, String world, long now, boolean create) {
        if (raid == null) return null;
        String key = "raid:" + world + ":" + raid.getId();
        Encounter enc = encounters.get(key);
        if (enc != null && enc.isOpen()) return enc;
        if (!create && !raid.isStarted()) return null;
        if (!create) {
            // auto-open on first observed raid damage (covers a missed
            // trigger, e.g. Telemetry enabled mid-raid)
            enc = new Encounter(key, "raid", null, world, now);
            encounters.put(key, enc);
            Map<String, Object> ex = encounterExtras.computeIfAbsent(key, k -> new LinkedHashMap<>());
            ex.put("bad_omen_level", raid.getBadOmenLevel());
            ex.put("auto_opened", true);
            Boolean broken = sealReader.isBroken("raid");
            if (broken != null) sealAtOpen.put(key, broken);
            return enc;
        }
        return null;
    }

    private org.bukkit.plugin.Plugin trialProvider() {
        var pm = getServer().getPluginManager();
        String name = TrialCompatibility.provider(pm.isPluginEnabled("ExcelBossTrial"),
                pm.isPluginEnabled("ExcelDragonTrial"));
        return name == null ? null : pm.getPlugin(name);
    }

    private String readTrialPhase(String worldName, UUID dragonUuid) {
        try {
            var trial = trialProvider();
            if (trial == null) return null;
            if (trial instanceof org.bukkit.plugin.java.JavaPlugin jp) {
                String prefix = "ExcelBossTrial".equals(trial.getName()) ? "dragon." : "";
                if (!TrialCompatibility.chapterActive(jp.getConfig().getBoolean("enabled", true),
                        jp.getConfig().getBoolean(prefix + "enabled", true))) return null;
            }
            File f = new File(trial.getDataFolder(), TrialCompatibility.statePath(trial.getName()));
            if (!f.exists()) return null;
            return TrialCompatibility.savedPhase(YamlConfiguration.loadConfiguration(f), worldName, dragonUuid);
        } catch (Throwable t) {
            return null;
        }
    }

    private void closeEncounter(Encounter enc, Encounter.Outcome outcome, long now) {
        enc.close(outcome);
        encounters.remove(enc.key());
        Map<String, Object> line = new LinkedHashMap<>();
        line.put("type", "boss_encounter");
        line.putAll(enc.summary(now));
        if (enc.success()) {
            List<Map<String, Object>> flags = IntegrityFlags.forBossKill(enc.bossKind(),
                    enc.durationSeconds(now), enc.explosionShare(), enc.totalDamageToBoss(), thresholds);
            if (!flags.isEmpty()) line.put("flags", flags);
        } else if (outcome != Encounter.Outcome.RAID_STOPPED) {
            line.put("failed_attempt", true);
        }
        String sealId = SEAL_BY_BOSS.get(enc.bossKind());
        if (sealId != null) {
            Map<String, Object> seal = new LinkedHashMap<>();
            seal.put("id", sealId);
            Boolean atOpen = sealAtOpen.remove(enc.key());
            if (atOpen != null) seal.put("broken_at_open", atOpen);
            Boolean atClose = sealReader.isBroken(sealId);
            if (atClose != null) seal.put("broken_at_close", atClose);
            if (seal.size() > 1) line.put("seal", seal);
        }
        Map<String, Object> extras = encounterExtras.remove(enc.key());
        if (extras != null) line.putAll(extras);
        emit("events", line);
    }

    // ------------------------------------------------------------------
    // Spawns (origin stash) and deaths
    @EventHandler(priority = EventPriority.MONITOR)
    public void onSpawn(CreatureSpawnEvent e) {
        spawnReasons.put(e.getEntity().getUniqueId(), e.getSpawnReason().name());
        spawnReasonAt.put(e.getEntity().getUniqueId(), System.currentTimeMillis());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(EntityDeathEvent e) {
        LivingEntity victim = e.getEntity();
        if (victim instanceof Player) return; // PlayerDeathEvent handles it
        long now = System.currentTimeMillis();
        UUID id = victim.getUniqueId();
        DamageLedger led = ledgers.remove(id);
        String stashed = spawnReasons.remove(id);
        spawnReasonAt.remove(id);
        EliteMobsReader.EliteInfo ei = eliteCache.remove(id);
        if (ei == null) ei = eliteReader.inspect(victim);

        if (capBosses && isBoss(victim.getType().name())) {
            Encounter enc = encounters.get("boss:" + id);
            if (enc != null && enc.isOpen()) {
                closeEncounter(enc, Encounter.Outcome.KILLED, now);
            }
            return;
        }
        if (!capKills) return;

        Player killerPlayer = victim.getKiller();
        boolean playerLed = led != null && led.hasPlayerAttacker();
        if (!playerLed && killerPlayer == null && !ei.elite()) return; // no player involvement

        String killerKind = "unknown";
        String killerUuid = null;
        String killerName = null;
        if (killerPlayer != null) {
            killerKind = "player";
            killerUuid = killerPlayer.getUniqueId().toString();
            killerName = killerPlayer.getName();
        } else if (led != null && led.topAttacker() != null) {
            String top = led.topAttacker();
            if (top.startsWith("player:")) {
                killerKind = "player";
                killerUuid = top.substring("player:".length());
                killerName = playerNames.get(UUID.fromString(killerUuid));
            } else if (top.startsWith("mob:")) {
                killerKind = "mob";
            } else if (top.startsWith("env:")) {
                killerKind = "environment";
            }
        }
        Map<String, Object> weapon = null;
        if (killerPlayer != null) {
            weapon = itemSnapshot(killerPlayer.getInventory().getItemInMainHand());
        }
        RewardGuardOrigin.Origin origin = originReader.classify(victim, stashed);
        List<Map<String, Object>> flags = ei.elite() && led != null
                ? IntegrityFlags.forEliteKill(led.explosionShare(), led.total(), thresholds)
                : List.of();
        double fight = led != null ? (now - led.firstSeenMs()) / 1000.0 : 0;
        Map<String, Object> line = EventShaper.kill(
                victim.getType().name().toLowerCase(), id.toString(), victim.getCustomName(),
                ei.elite(), ei.level(),
                killerKind, killerUuid, killerName, weapon,
                fight, led != null ? led.snapshot() : null,
                origin.originClass(), origin.raw(), origin.rgDenied(),
                led != null ? led.fallShare() : 0, flags);
        emit("events", line);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDeath(PlayerDeathEvent e) {
        Player p = e.getEntity();
        long now = System.currentTimeMillis();
        if (capDeaths) {
            LastDamage.Hit hit = lastDamage.fresh(p.getUniqueId(), now);
            String causeKind = hit != null ? hit.damagerKind() : "unknown";
            String damageCause = hit != null ? hit.damageCause()
                    : (p.getLastDamageCause() != null ? p.getLastDamageCause().getCause().name() : null);
            Map<String, Object> line = EventShaper.playerDeath(
                    p.getUniqueId().toString(), p.getName(),
                    causeKind, damageCause,
                    hit != null ? hit.damagerType() : null,
                    hit != null ? hit.damagerUuid() : null,
                    e.getDeathMessage(),
                    p.getWorld().getName(), p.getWorld().getEnvironment().name(),
                    p.getLocation().getBlockX(), p.getLocation().getBlockY(), p.getLocation().getBlockZ(),
                    gearSnapshot(p),
                    mcMmo != null ? mcMmo.combatLevels(p) : null);
            emit("events", line);
        }
        lastDamage.clear(p.getUniqueId());
        if (capBosses) {
            List<Encounter> toClose = new ArrayList<>();
            for (Encounter enc : encounters.values()) {
                if (!enc.isOpen() || !enc.hasParticipant(p.getUniqueId().toString())) continue;
                enc.recordParticipantDeath(p.getUniqueId().toString(), p.getName());
                if (enc.allParticipantsDead()) toClose.add(enc);
            }
            toClose.forEach(enc -> closeEncounter(enc, Encounter.Outcome.PARTY_WIPED, now));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRespawn(PlayerRespawnEvent e) {
        for (Encounter enc : encounters.values()) {
            if (enc.isOpen()) enc.recordParticipantAlive(e.getPlayer().getUniqueId().toString());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRemove(EntityRemoveEvent e) {
        if (!capBosses) return;
        if (!(e.getEntity() instanceof LivingEntity le) || !isBoss(le.getType().name())) return;
        String reason = e.getCause() != null ? e.getCause().name() : "";
        if ("DEATH".equals(reason)) return;
        if (TrialCompatibility.isUnload(reason)) {
            // Leave the existing encounter open, but do NOT refresh lastActivity.
            // The existing idle sweep still bounds retention when nobody returns.
            Encounter unloaded = encounters.get("boss:" + le.getUniqueId());
            if (unloaded != null && unloaded.isOpen()) {
                unloaded.addMarker("boss_unloaded", System.currentTimeMillis(), reason);
            }
            return;
        }
        Encounter enc = encounters.get("boss:" + le.getUniqueId());
        if (enc != null && enc.isOpen()) {
            enc.addMarker("boss_removed", System.currentTimeMillis(), reason);
            closeEncounter(enc, Encounter.Outcome.ABANDONED, System.currentTimeMillis());
        }
    }

    // ------------------------------------------------------------------
    // Raids
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onRaidTrigger(RaidTriggerEvent e) {
        if (!capBosses) return;
        long now = System.currentTimeMillis();
        Raid raid = e.getRaid();
        String key = "raid:" + e.getWorld().getName() + ":" + raid.getId();
        if (!encounters.containsKey(key)) {
            Encounter enc = new Encounter(key, "raid", null, e.getWorld().getName(), now);
            encounters.put(key, enc);
            Map<String, Object> ex = encounterExtras.computeIfAbsent(key, k -> new LinkedHashMap<>());
            ex.put("bad_omen_level", raid.getBadOmenLevel());
            ex.put("triggered_by", e.getPlayer().getName());
            Boolean broken = sealReader.isBroken("raid");
            if (broken != null) sealAtOpen.put(key, broken);
            enc.addMarker("raid_triggered", now, e.getPlayer().getName());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRaidWave(RaidSpawnWaveEvent e) {
        Encounter enc = encounters.get("raid:" + e.getWorld().getName() + ":" + e.getRaid().getId());
        if (enc != null && enc.isOpen()) {
            enc.addMarker("wave_spawned", System.currentTimeMillis(),
                    "groups=" + e.getRaid().getSpawnedGroups() + "/" + e.getRaid().getTotalGroups());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRaidFinish(RaidFinishEvent e) {
        if (!capBosses) return;
        String key = "raid:" + e.getWorld().getName() + ":" + e.getRaid().getId();
        Encounter enc = encounters.get(key);
        if (enc == null || !enc.isOpen()) return;
        List<String> winners = new ArrayList<>();
        e.getWinners().forEach(w -> winners.add(w.getName()));
        encounterExtras.computeIfAbsent(key, k -> new LinkedHashMap<>()).put("raid_winners", winners);
        closeEncounter(enc, Encounter.Outcome.RAID_WON, System.currentTimeMillis());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRaidStop(RaidStopEvent e) {
        if (!capBosses) return;
        String key = "raid:" + e.getWorld().getName() + ":" + e.getRaid().getId();
        Encounter enc = encounters.get(key);
        if (enc == null || !enc.isOpen()) return;
        encounterExtras.computeIfAbsent(key, k -> new LinkedHashMap<>())
                .put("stop_reason", e.getReason().name());
        closeEncounter(enc, Encounter.Outcome.RAID_STOPPED, System.currentTimeMillis());
    }

    // ------------------------------------------------------------------
    // mcMMO snapshots + advancements + join/quit
    private void snapshot(Player p, String reason) {
        snapshot(p, reason, true);
    }

    private void snapshot(Player p, String reason, boolean requireOnline) {
        if (!capMcMmo || mcMmo == null || p == null) return;
        if (requireOnline && !p.isOnline()) return;
        Map<String, Integer> levels = mcMmo.levels(p);
        int power;
        if (levels != null) {
            power = mcMmo.powerLevel(p);
            lastLevels.put(p.getUniqueId(), new LinkedHashMap<>(levels));
            lastPower.put(p.getUniqueId(), power);
        } else if ("quit".equals(reason)) {
            // mcMMO may already have unloaded the profile mid-quit; fall
            // back to the last observed levels (never zeros, never faked).
            levels = lastLevels.get(p.getUniqueId());
            power = lastPower.getOrDefault(p.getUniqueId(), 0);
        } else {
            power = 0;
        }
        Map<String, Object> line = EventShaper.mcmmoSnapshot(
                p.getUniqueId().toString(), p.getName(), reason, levels, power);
        if (line != null) emit("snapshots", line);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent e) {
        playerNames.put(e.getPlayer().getUniqueId(), e.getPlayer().getName());
        getServer().getScheduler().runTaskLater(this, () -> snapshot(e.getPlayer(), "join"), 100L);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent e) {
        snapshot(e.getPlayer(), "quit", false);
        lastDamage.clear(e.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onAdvancement(PlayerAdvancementDoneEvent e) {
        if (!capAdvancements) return;
        String key = e.getAdvancement().getKey().toString();
        if (!advancementWatch.contains(key)) return;
        Map<String, Object> line = EventShaper.advancement(
                e.getPlayer().getUniqueId().toString(), e.getPlayer().getName(), key);
        if (line != null) emit("events", line);
    }

    // ------------------------------------------------------------------
    // Periodic sweep: prune ledgers, close idle encounters
    private void sweep() {
        long now = System.currentTimeMillis();
        Set<UUID> alive = new HashSet<>();
        for (var world : Bukkit.getWorlds()) {
            for (LivingEntity le : world.getLivingEntities()) alive.add(le.getUniqueId());
        }
        ledgers.prune(alive, now);
        spawnReasons.keySet().removeIf(id -> !alive.contains(id)
                && now - spawnReasonAt.getOrDefault(id, 0L) > ledgerTtlMs);
        spawnReasonAt.keySet().retainAll(spawnReasons.keySet());
        eliteCache.keySet().removeIf(id -> !alive.contains(id));
        List<Encounter> idle = new ArrayList<>();
        for (Encounter enc : encounters.values()) {
            if (enc.isOpen() && now - enc.lastActivityMs() > encounterIdleMs) idle.add(enc);
        }
        idle.forEach(enc -> {
            enc.addMarker("idle_timeout", now, "no observed damage within configured interval");
            closeEncounter(enc, Encounter.Outcome.ABANDONED, now);
        });
    }

    // ------------------------------------------------------------------
    // Item / gear snapshots
    private Map<String, Object> itemSnapshot(ItemStack stack) {
        if (stack == null || stack.getType().name().equals("AIR")) return null;
        Map<String, Integer> enchants = new LinkedHashMap<>();
        stack.getEnchantments().forEach((ench, lvl) -> enchants.put(ench.getKey().toString(), lvl));
        String custom = null;
        if (stack.hasItemMeta() && stack.getItemMeta().hasDisplayName()) {
            custom = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
                    .plainText().serialize(stack.getItemMeta().displayName());
        }
        return EventShaper.item("minecraft:" + stack.getType().name().toLowerCase(), enchants, custom);
    }

    private Map<String, Object> gearSnapshot(Player p) {
        var inv = p.getInventory();
        Map<String, Object> armor = new LinkedHashMap<>();
        armor.put("helmet", itemSnapshot(inv.getHelmet()));
        armor.put("chestplate", itemSnapshot(inv.getChestplate()));
        armor.put("leggings", itemSnapshot(inv.getLeggings()));
        armor.put("boots", itemSnapshot(inv.getBoots()));
        Map<String, Object> gear = new LinkedHashMap<>();
        gear.put("armor", armor);
        gear.put("mainhand", itemSnapshot(inv.getItemInMainHand()));
        return gear;
    }

    // ------------------------------------------------------------------
    // Console-only status
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("telemetry")) return false;
        if (!(sender instanceof ConsoleCommandSender)) {
            sender.sendMessage("ExcelTelemetry is console-only (admin data, no in-game surface in v1).");
            return true;
        }
        String today = LocalDate.now(ZONE).toString();
        Path dir = getDataFolder().toPath();
        try {
            JsonlReader.Counts ev = JsonlReader.countFile(dir.resolve("events-" + today + ".jsonl"));
            JsonlReader.Counts sn = JsonlReader.countFile(dir.resolve("snapshots-" + today + ".jsonl"));
            sender.sendMessage("[ExcelTelemetry] status:");
            sender.sendMessage("  writer: " + writer.written.get() + " written, "
                    + writer.dropped.get() + " dropped (queue full)");
            sender.sendMessage("  today: " + ev.total() + " event lines " + ev.byType()
                    + ", " + sn.total() + " snapshot lines, " + (ev.corrupt() + sn.corrupt()) + " corrupt lines skipped");
            sender.sendMessage("  ledgers: " + ledgers.size() + " victims tracked | encounters open: "
                    + encounters.size() + " " + encounters.keySet());
            sender.sendMessage("  integrations: mcMMO=" + (mcMmo != null)
                    + ", EliteMobs=" + (getServer().getPluginManager().getPlugin("EliteMobs") != null)
                    + ", ExcelGuildGate=" + (getServer().getPluginManager().getPlugin("ExcelGuildGate") != null)
                    + ", dragonTrialProvider=" + (trialProvider() == null ? "none" : trialProvider().getName()));
        } catch (Throwable t) {
            sender.sendMessage("[ExcelTelemetry] status failed: " + t.getMessage());
        }
        return true;
    }
}

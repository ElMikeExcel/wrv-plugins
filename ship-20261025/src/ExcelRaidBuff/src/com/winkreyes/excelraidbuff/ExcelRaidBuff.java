package com.winkreyes.excelraidbuff;

import com.winkreyes.excelraidbuff.api.ExcelRaidBuffAPI;
import com.winkreyes.excelraidbuff.core.AnnounceRule;
import com.winkreyes.excelraidbuff.core.BuffMath;
import com.winkreyes.excelraidbuff.core.EliteComposition;
import com.winkreyes.excelraidbuff.core.EliteLevelRule;
import com.winkreyes.excelraidbuff.core.MembershipGate;
import com.winkreyes.excelraidbuff.core.ExpiryTracker;
import com.winkreyes.excelraidbuff.core.RaidBuffConfig;
import com.winkreyes.excelraidbuff.core.RevealTracker;
import com.winkreyes.excelraidbuff.core.ScalingTable;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Raid;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Evoker;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Pillager;
import org.bukkit.entity.Player;
import org.bukkit.entity.Raider;
import org.bukkit.entity.Ravager;
import org.bukkit.entity.Vindicator;
import org.bukkit.entity.Witch;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.raid.RaidFinishEvent;
import org.bukkit.event.raid.RaidSpawnWaveEvent;
import org.bukkit.event.raid.RaidStopEvent;
import org.bukkit.event.raid.RaidTriggerEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * ExcelRaidBuff — the raid-only buff layer of Wink Reyes Valley
 * (Mike's decision, Oct 20, 2026 late: an Excel layer over flipping
 * EliteMobs' global high-compatibility flag, so raids get meaner
 * without opening the custom-spawn surface anywhere else).
 *
 * What it does, and ONLY to members of a live raid:
 *  - attribute scaling keyed to the raid's Bad Omen level
 *    (ScalingTable: Omen I is vanilla end to end, Omen V is the
 *    wall, with an extra HP lean on the final waves), and
 *  - a config fraction of each wave converted IN PLACE into real
 *    EliteMobs elites through EM's public API (EmGateway), raid-
 *    scoped only — the global flag stays OFF and is never read or
 *    written. Converted elites are EM's to scale: this plugin puts
 *    no modifiers on them and strips nothing from them, ever.
 *
 * Membership is the raid object's own claim (Raid#getRaiders /
 * Raider#getRaid) — patrols, outposts, mansions and evoker vexes
 * never enter. The buff is attribute modifiers + a PDC marker, and
 * it is stripped when the raid ends, when the raider leaves the
 * raid, or by the janitor sweep; on disable everything strips.
 * Player-facing output is exactly one line as the first wave
 * arrives, when the buff is meaningfully on (AnnounceRule).
 * Bedrock parity is
 * automatic: attributes are server-side, both clients fight the
 * same numbers. GuildGate's credit read (winners, omen level) is
 * untouched — this plugin changes only the fight.
 *
 * The 0.1.1 stuck-wave reveal (RevealTracker) is the one
 * addition: a raid that makes no progress for the configured
 * quiet span — no member dies, takes damage, deals damage, and
 * no new wave begins — glows its living members until the fight
 * resumes, so terrain-stuck raiders can be hunted down (the
 * vanilla bell only reaches ~32 blocks). It is cosmetic end to
 * end: it marks, buffs and counts nothing, reads no credit
 * state, and writes no PDC — the glow is a plain potion effect
 * plus an in-memory record of which entities this feature lit.
 *
 * The 0.1.2 raid expiry (ExpiryTracker) caps a raid's whole
 * lifetime at the configured hours: a hard wall-clock limit
 * that fighting does NOT reset, on a clock persisted to disk
 * so the daily restart doesn't reset it either. At the cap
 * the raid dies down — the raid state is retired and its
 * members are removed where they stand (never killed: no
 * drops, no XP, no coins, no Hero of the Village; the raid
 * is neither won nor lost). One line to the raid's nearby
 * players, and a log line the nightly combat analysis reads.
 */
public final class ExcelRaidBuff extends JavaPlugin implements Listener {

    private RaidBuffConfig cfg = RaidBuffConfig.defaults();
    private EmGateway em;
    private int sweepTask = -1;

    private NamespacedKey keyBuffed;
    private NamespacedKey keyHealth;
    private NamespacedKey keyDamage;
    private NamespacedKey keyRaid;
    private NamespacedKey keyOmen;
    private NamespacedKey modHealthKey;
    private NamespacedKey modDamageKey;

    /** Raid keys (see raidKey) that already got their one announce line. */
    private final Set<Integer> announcedRaids = new HashSet<>();

    /** Per-raid stall clocks for the stuck-wave reveal (0.1.1). */
    private final RevealTracker revealTracker = new RevealTracker();

    /** Per-raid lifetime clocks for the raid expiry (0.1.2), persisted below. */
    private final ExpiryTracker expiryTracker = new ExpiryTracker();

    /** Where the expiry clocks persist (plugin data folder / expiry-starts.yml). */
    private File expiryFile;

    /** Entities the reveal lit, UUID -> raid key: the only glow this feature may clear. */
    private final Map<UUID, Integer> ourGlow = new HashMap<>();

    // ------------------------------------------------------------------
    // Lifecycle
    // ------------------------------------------------------------------

    @Override
    public void onEnable() {
        keyBuffed = new NamespacedKey(this, "buffed");
        keyHealth = new NamespacedKey(this, "hp_factor");
        keyDamage = new NamespacedKey(this, "dmg_factor");
        keyRaid = new NamespacedKey(this, "raid_key");
        keyOmen = new NamespacedKey(this, "omen");
        modHealthKey = new NamespacedKey(this, "raid_health");
        modDamageKey = new NamespacedKey(this, "raid_damage");
        em = new EmGateway(getLogger());
        saveDefaultConfig();
        loadSettings();
        // The expiry clocks come back off disk: a restart must
        // NOT reset a raid's 6-hour lifetime (ExpiryTracker).
        loadExpiryStarts();
        ExcelRaidBuffAPI.bind(new ExcelRaidBuffAPI.Markers(keyBuffed, keyHealth, keyDamage));
        Bukkit.getPluginManager().registerEvents(this, this);
        sweepTask = Bukkit.getScheduler().scheduleSyncRepeatingTask(this, this::sweep, 60L, cfg.sweepPeriodTicks);
        getLogger().info(enableLine());
    }

    @Override
    public void onDisable() {
        if (sweepTask >= 0) {
            Bukkit.getScheduler().cancelTask(sweepTask);
            sweepTask = -1;
        }
        // The reveal's glow is ours: take it back off before going
        // dark (any effect we somehow miss expires in seconds).
        clearAllGlow();
        revealTracker.reset();
        // The expiry clocks are NOT reset — they are the one
        // thing that must survive this shutdown. Persist them.
        saveExpiryStarts();
        // Nothing outlives the plugin: every marked raider in a
        // loaded chunk goes back to vanilla before we go dark.
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (entity instanceof Raider raider && isMarked(raider)) {
                    stripBuff(raider);
                }
            }
        }
        ExcelRaidBuffAPI.unbind();
    }

    private String enableLine() {
        StringBuilder sb = new StringBuilder("ExcelRaidBuff ")
                .append(getDescription().getVersion()).append(" awake: ")
                .append(cfg.enabled ? "ENABLED" : "disabled by config")
                .append(" (HP x");
        for (int omen = 1; omen <= 5; omen++) {
            if (omen > 1) sb.append('/');
            sb.append(String.format("%.2f", cfg.healthMultiplier(omen)));
        }
        sb.append(", DMG x");
        for (int omen = 1; omen <= 5; omen++) {
            if (omen > 1) sb.append('/');
            sb.append(String.format("%.2f", cfg.damageMultiplier(omen)));
        }
        sb.append(String.format(", late-wave HP x%.2f on final %d", cfg.lateWaveHealthRamp, cfg.lateWaveCount));
        sb.append("; elites ").append(cfg.elitesEnabled ? "ON" : "OFF").append(" share ");
        for (int omen = 1; omen <= 5; omen++) {
            if (omen > 1) sb.append('/');
            sb.append(Math.round(cfg.eliteShare(omen) * 100)).append('%');
        }
        if (cfg.elitesEnabled) {
            sb.append(", EM seam ").append(em.available() ? "AVAILABLE" : "UNAVAILABLE")
                    .append(", level cap ").append(em.levelCap(cfg.eliteLevelCapFallback));
        }
        sb.append("; reveal ").append(cfg.revealEnabled
                ? "ON quiet " + cfg.revealQuietSeconds + "s" : "OFF");
        sb.append("; expiry ").append(cfg.expiryEnabled
                ? "ON cap " + hoursText(cfg.expiryHours) : "OFF");
        sb.append(')');
        return sb.toString();
    }

    /** "6h", "0.02h": whole hours bald, fractions to two decimals. */
    static String hoursText(double hours) {
        if (hours == Math.rint(hours)) return ((long) hours) + "h";
        String s = String.format(java.util.Locale.ROOT, "%.2f", hours);
        while (s.endsWith("0")) s = s.substring(0, s.length() - 1);
        if (s.endsWith(".")) s = s.substring(0, s.length() - 1);
        return s + "h";
    }

    /** "6h 0m", "12m 5s", "45s": the coarsest two units that say it. */
    static String ageText(long ageMs) {
        long totalSec = Math.max(0L, ageMs) / 1000L;
        long h = totalSec / 3600L;
        long m = (totalSec % 3600L) / 60L;
        long s = totalSec % 60L;
        if (h > 0) return h + "h " + m + "m";
        if (m > 0) return m + "m " + s + "s";
        return s + "s";
    }

    private void loadSettings() {
        reloadConfig();
        cfg = RaidBuffConfig.fromMap(toMap(getConfig()));
        // A reload that turns the reveal (or the whole plugin)
        // off mid-raid retires it cleanly: glow off, clocks gone.
        // Re-enabling starts every raid's stall clock fresh.
        if (!cfg.enabled || !cfg.revealEnabled) {
            for (int key : revealTracker.reset()) {
                clearGlow(key);
            }
        }
        // Same retirement for the expiry: a reload that turns it
        // (or the whole plugin) off drops every lifetime clock,
        // persisted file included. Re-enabling starts clocks fresh
        // at first sight, exactly like the reveal's.
        if (!cfg.enabled || !cfg.expiryEnabled) {
            if (expiryTracker.trackedRaids() > 0) {
                expiryTracker.reset();
            }
            saveExpiryStarts();
        }
    }

    /** ConfigurationSection -> nested plain maps, for RaidBuffConfig.fromMap. */
    private static Map<String, Object> toMap(ConfigurationSection section) {
        Map<String, Object> out = new LinkedHashMap<>();
        for (String key : section.getKeys(false)) {
            Object value = section.get(key);
            if (value instanceof ConfigurationSection child) {
                out.put(key, toMap(child));
            } else {
                out.put(key, value);
            }
        }
        return out;
    }

    // ------------------------------------------------------------------
    // Raid events
    // ------------------------------------------------------------------

    @EventHandler
    public void onWaveSpawn(RaidSpawnWaveEvent event) {
        if (!cfg.enabled) return;
        Raid raid = event.getRaid();
        List<Raider> raiders = event.getRaiders();
        if (raiders == null || raiders.isEmpty()) return;
        int omen = raid.getBadOmenLevel();
        int totalWaves = Math.max(1, raid.getTotalWaves());
        int wave = waveNumber(raid, raiders.get(0));
        boolean ongoing = raid.getStatus() == Raid.RaidStatus.ONGOING;

        // The one announce line rides the FIRST wave, not the raid
        // trigger: at RaidTriggerEvent time the raid's Bad Omen
        // level is not yet readable (the event fires inside
        // createOrExtendRaid before the level settles - rig-proven
        // across Boots A-D, where the trigger-time read suppressed
        // the line every time while the wave-time read was exact).
        // The first wave IS the raid's start to the village anyway.
        int key = raidKey(raid);
        // A new wave is reveal progress (and the raid's first
        // sighting starts its stall clock).
        if (cfg.revealEnabled) {
            noteProgress(key);
        }
        if (AnnounceRule.shouldAnnounce(true, cfg.announceEnabled, omen,
                cfg.announceMinOmen, announcedRaids.contains(key))) {
            announcedRaids.add(key);
            Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', cfg.announceLine));
            getLogger().info("RaidBuff announce: first wave at omen " + omen
                    + " - hardened line broadcast");
        }

        // 1) Elites first: the picked raiders become EliteMobs'
        //    problem, at EM levels, and never see an attribute
        //    modifier from this plugin (no double-scaling).
        Set<Raider> converted = pickAndConvertElites(raid, raiders, omen, wave, totalWaves);

        // 2) Everyone else gets the attribute buff.
        int buffed = 0;
        for (Raider raider : raiders) {
            if (converted.contains(raider)) continue;
            if (em.isElite(raider)) continue; // EM's already (never double-scale)
            ScalingTable.Factors factors = ScalingTable.factors(cfg, omen, wave, totalWaves);
            if (!factors.active()) continue;
            if (MembershipGate.decideBuff(true, ongoing, isMarked(raider))
                    != MembershipGate.BuffDecision.BUFF) continue;
            if (applyBuff(raider, factors, raid, omen)) buffed++;
        }
        getLogger().info("RaidBuff wave: omen=" + omen + " wave=" + wave
                + " (spawnedGroups=" + raid.getSpawnedGroups() + ", raiderWave="
                + raiders.get(0).getWave() + ") totalWaves=" + totalWaves
                + " raiders=" + raiders.size() + " buffed=" + buffed
                + " elites=" + converted.size());
    }

    /**
     * A raid's birth (0.1.2): its lifetime clock starts here, at
     * the trigger — the earliest moment the raid exists. The
     * sweep's first-sight rule is the backstop for raids already
     * in progress when the plugin enables.
     */
    @EventHandler
    public void onRaidTrigger(RaidTriggerEvent event) {
        if (!cfg.enabled || !cfg.expiryEnabled) return;
        noteExpirySeen(raidKey(event.getRaid()));
    }

    /** First sight starts a raid's lifetime clock; a new clock persists at once. */
    private void noteExpirySeen(int key) {
        if (expiryTracker.onRaidSeen(key, System.currentTimeMillis())) {
            saveExpiryStarts();
        }
    }

    @EventHandler
    public void onRaidFinish(RaidFinishEvent event) {
        stripRaid(event.getRaid());
    }

    @EventHandler
    public void onRaidStop(RaidStopEvent event) {
        stripRaid(event.getRaid());
    }

    /** Strip every marked raider of this raid, one tick after the end lands. */
    private void stripRaid(Raid raid) {
        int key = raidKey(raid);
        announcedRaids.remove(key);
        if (revealTracker.onRaidEnd(key)) {
            clearGlow(key);
        }
        // However the raid ended, its lifetime clock is done.
        if (expiryTracker.onRaidEnd(key)) {
            saveExpiryStarts();
        }
        World world = raid.getLocation().getWorld();
        if (world == null) return;
        new BukkitRunnable() {
            @Override public void run() {
                for (Entity entity : world.getEntities()) {
                    if (entity instanceof Raider raider && isMarked(raider)
                            && markedRaidKey(raider) == key) {
                        stripBuff(raider);
                    }
                }
            }
        }.runTaskLater(this, 1L);
    }

    // ------------------------------------------------------------------
    // Stuck-wave reveal: progress signals (0.1.1)
    // ------------------------------------------------------------------

    /**
     * A raid member taking damage — from anyone, for any reason —
     * is progress; so is a raid member dealing damage (the other
     * side of the same event). One handler reads both sides.
     */
    @EventHandler
    public void onMemberDamage(EntityDamageEvent event) {
        if (!cfg.enabled || !cfg.revealEnabled) return;
        if (event.getEntity() instanceof Raider member && member.getRaid() != null) {
            noteProgress(raidKey(member.getRaid()));
        }
        if (event instanceof EntityDamageByEntityEvent byEntity
                && byEntity.getDamager() instanceof Raider member && member.getRaid() != null) {
            noteProgress(raidKey(member.getRaid()));
        }
    }

    /**
     * A raid member dying is progress. In practice the lethal
     * damage event above lands first in the same tick; this is
     * the belt to its suspenders (and covers deaths dealt
     * without a damage event).
     */
    @EventHandler
    public void onMemberDeath(EntityDeathEvent event) {
        if (!cfg.enabled || !cfg.revealEnabled) return;
        if (event.getEntity() instanceof Raider member && member.getRaid() != null) {
            noteProgress(raidKey(member.getRaid()));
        }
    }

    /** Record progress for a raid; a live reveal ends the moment it lands. */
    private void noteProgress(int key) {
        if (revealTracker.onProgress(key, System.currentTimeMillis())) {
            clearGlow(key);
        }
    }

    // ------------------------------------------------------------------
    // Elite conversion
    // ------------------------------------------------------------------

    /**
     * Choose this wave's elite quota (EliteComposition: deterministic
     * quota, evenly spaced through the type-priority order) and
     * convert in place through the EM seam. A conversion that fails
     * for any reason leaves the raider to the attribute buff — the
     * wave is never held up by the seam.
     */
    private Set<Raider> pickAndConvertElites(Raid raid, List<Raider> raiders,
                                             int omen, int wave, int totalWaves) {
        Set<Raider> converted = new HashSet<>();
        if (!cfg.elitesEnabled || !em.available()) return converted;
        List<Raider> candidates = new ArrayList<>();
        List<Integer> priorities = new ArrayList<>();
        for (Raider raider : raiders) {
            if (raider.isDead() || em.isElite(raider)) continue;
            candidates.add(raider);
            priorities.add(typePriority(raider));
        }
        double share = EliteComposition.waveShare(cfg, omen, wave, totalWaves);
        int quota = EliteComposition.quota(candidates.size(), share);
        if (quota <= 0) return converted;
        int cap = em.levelCap(cfg.eliteLevelCapFallback);
        for (int index : EliteComposition.select(quota, priorities)) {
            Raider raider = candidates.get(index);
            List<Player> nearby = nearbyPlayers(raider.getLocation());
            if (nearby.isEmpty()) continue;
            int natural = em.naturalLevel(raider.getLocation(), nearby);
            if (natural < 0) continue; // seam could not price it: do not guess a level
            int level = EliteLevelRule.levelFor(natural, cfg.eliteLevelBonus(omen), cap);
            if (em.convert(raider, level)) {
                converted.add(raider);
                getLogger().info("RaidBuff elite: " + raider.getType() + " converted in place"
                        + " at level " + em.eliteLevelOf(raider) + " (natural " + natural
                        + " + omen bonus " + cfg.eliteLevelBonus(omen) + ", cap " + cap + ")");
            }
        }
        return converted;
    }

    /** Conversion preference: the raiders that make a fight go first. */
    private static int typePriority(Raider raider) {
        if (raider instanceof Vindicator) return 0;
        if (raider instanceof Witch) return 1;
        if (raider instanceof Evoker) return 2;
        if (raider instanceof Pillager) return 3;
        if (raider instanceof Ravager) return 4;
        return 5;
    }

    /** Players EM's level ruler should price: those near the raider (fallback: its world). */
    private static List<Player> nearbyPlayers(Location location) {
        List<Player> out = new ArrayList<>();
        if (location == null || location.getWorld() == null) return out;
        for (Player player : location.getWorld().getPlayers()) {
            if (player.getLocation().distanceSquared(location) <= 100.0 * 100.0) {
                out.add(player);
            }
        }
        if (out.isEmpty()) out.addAll(location.getWorld().getPlayers());
        return out;
    }

    // ------------------------------------------------------------------
    // Buff apply / strip
    // ------------------------------------------------------------------

    private boolean applyBuff(Raider raider, ScalingTable.Factors factors, Raid raid, int omen) {
        AttributeInstance maxHealth = raider.getAttribute(Attribute.MAX_HEALTH);
        if (maxHealth == null) return false;
        removeModifier(maxHealth, modHealthKey);
        maxHealth.addModifier(new AttributeModifier(modHealthKey,
                BuffMath.modifierAmount(factors.health()), AttributeModifier.Operation.MULTIPLY_SCALAR_1));
        AttributeInstance attack = raider.getAttribute(Attribute.ATTACK_DAMAGE);
        if (attack != null) {
            removeModifier(attack, modDamageKey);
            attack.addModifier(new AttributeModifier(modDamageKey,
                    BuffMath.modifierAmount(factors.damage()), AttributeModifier.Operation.MULTIPLY_SCALAR_1));
        }
        double newMax = maxHealth.getValue();
        double target = BuffMath.healthOnApply(raider.getHealth(), factors.health());
        raider.setHealth(Math.max(1.0, Math.min(target, newMax)));
        PersistentDataContainer pdc = raider.getPersistentDataContainer();
        pdc.set(keyBuffed, PersistentDataType.BYTE, (byte) 1);
        pdc.set(keyHealth, PersistentDataType.DOUBLE, factors.health());
        pdc.set(keyDamage, PersistentDataType.DOUBLE, factors.damage());
        pdc.set(keyRaid, PersistentDataType.INTEGER, raidKey(raid));
        pdc.set(keyOmen, PersistentDataType.INTEGER, omen);
        return true;
    }

    private void stripBuff(Raider raider) {
        AttributeInstance maxHealth = raider.getAttribute(Attribute.MAX_HEALTH);
        if (maxHealth != null) {
            removeModifier(maxHealth, modHealthKey);
            double clamped = BuffMath.healthOnStrip(raider.getHealth(), maxHealth.getValue());
            if (raider.getHealth() > 0) {
                raider.setHealth(Math.max(1.0, Math.min(clamped, maxHealth.getValue())));
            }
        }
        AttributeInstance attack = raider.getAttribute(Attribute.ATTACK_DAMAGE);
        if (attack != null) removeModifier(attack, modDamageKey);
        PersistentDataContainer pdc = raider.getPersistentDataContainer();
        pdc.remove(keyBuffed);
        pdc.remove(keyHealth);
        pdc.remove(keyDamage);
        pdc.remove(keyRaid);
        pdc.remove(keyOmen);
    }

    /** Re-apply at new factors after a config retune (health follows the ratio). */
    private void rescaleBuff(Raider raider, ScalingTable.Factors desired, Raid raid, int omen) {
        double oldFactor = storedFactor(raider, keyHealth, 1.0);
        AttributeInstance maxHealth = raider.getAttribute(Attribute.MAX_HEALTH);
        if (maxHealth == null) {
            stripBuff(raider);
            return;
        }
        removeModifier(maxHealth, modHealthKey);
        maxHealth.addModifier(new AttributeModifier(modHealthKey,
                BuffMath.modifierAmount(desired.health()), AttributeModifier.Operation.MULTIPLY_SCALAR_1));
        AttributeInstance attack = raider.getAttribute(Attribute.ATTACK_DAMAGE);
        if (attack != null) {
            removeModifier(attack, modDamageKey);
            attack.addModifier(new AttributeModifier(modDamageKey,
                    BuffMath.modifierAmount(desired.damage()), AttributeModifier.Operation.MULTIPLY_SCALAR_1));
        }
        double target = BuffMath.healthOnRescale(raider.getHealth(), oldFactor, desired.health());
        raider.setHealth(Math.max(1.0, Math.min(target, maxHealth.getValue())));
        PersistentDataContainer pdc = raider.getPersistentDataContainer();
        pdc.set(keyHealth, PersistentDataType.DOUBLE, desired.health());
        pdc.set(keyDamage, PersistentDataType.DOUBLE, desired.damage());
        pdc.set(keyRaid, PersistentDataType.INTEGER, raidKey(raid));
        pdc.set(keyOmen, PersistentDataType.INTEGER, omen);
    }

    private static void removeModifier(AttributeInstance attribute, NamespacedKey key) {
        for (AttributeModifier modifier : new ArrayList<>(attribute.getModifiers())) {
            if (modifier.getKey().equals(key)) {
                attribute.removeModifier(modifier);
            }
        }
    }

    private boolean hasModifier(Raider raider) {
        AttributeInstance maxHealth = raider.getAttribute(Attribute.MAX_HEALTH);
        if (maxHealth == null) return false;
        for (AttributeModifier modifier : maxHealth.getModifiers()) {
            if (modifier.getKey().equals(modHealthKey)) return true;
        }
        return false;
    }

    // ------------------------------------------------------------------
    // Janitor sweep: strip the orphaned, repair the live
    // ------------------------------------------------------------------

    private void sweep() {
        if (!cfg.enabled) return;
        // The set of raids still running, by key — the strip gate's
        // "marked raid still active" fact.
        Map<Integer, Raid> activeRaids = new HashMap<>();
        for (World world : Bukkit.getWorlds()) {
            for (Raid raid : world.getRaids()) {
                if (raid.getStatus() == Raid.RaidStatus.ONGOING) {
                    activeRaids.put(raidKey(raid), raid);
                }
            }
        }
        if (cfg.expiryEnabled) {
            // First sight starts a lifetime clock — this is also
            // how a raid already in progress at enable (or in a
            // freshly loaded chunk) gets one.
            for (int key : activeRaids.keySet()) {
                noteExpirySeen(key);
            }
            expiryStep(activeRaids);
        }
        announcedRaids.retainAll(activeRaids.keySet());
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (!(entity instanceof Raider raider) || !isMarked(raider)) continue;
                int markedKey = markedRaidKey(raider);
                Raid current = raider.getRaid();
                int currentKey = current == null ? -1 : raidKey(current);
                boolean markedActive = activeRaids.containsKey(markedKey);
                if (MembershipGate.decideStrip(true, currentKey, markedKey, markedActive)
                        == MembershipGate.StripDecision.STRIP) {
                    stripBuff(raider);
                    continue;
                }
                // Still a member of its live raid: repair a lost
                // modifier and follow a config retune.
                Raid raid = activeRaids.get(markedKey);
                if (raid == null) continue;
                int omen = raid.getBadOmenLevel();
                ScalingTable.Factors desired = ScalingTable.factors(
                        cfg, omen, waveNumber(raid, raider), Math.max(1, raid.getTotalWaves()));
                double storedHp = storedFactor(raider, keyHealth, Double.NaN);
                double storedDmg = storedFactor(raider, keyDamage, Double.NaN);
                if (!desired.active()) {
                    stripBuff(raider); // retuned down to vanilla: the buff comes off
                } else if (!hasModifier(raider)) {
                    applyBuff(raider, desired, raid, omen);
                } else if (Math.abs(storedHp - desired.health()) > 1e-9
                        || Math.abs(storedDmg - desired.damage()) > 1e-9) {
                    rescaleBuff(raider, desired, raid, omen);
                }
            }
        }
        if (cfg.revealEnabled) {
            revealStep(activeRaids);
        }
    }

    // ------------------------------------------------------------------
    // Stuck-wave reveal: the sweep step and the glow bookkeeping
    // ------------------------------------------------------------------

    /**
     * One sweep's reveal work: retire raids that ended without
     * reaching the finish/stop handlers, then judge every live
     * raid's stall clock. A stalled raid's living members glow
     * (applied on activation, refreshed while it lasts, joined
     * mid-stall by any member vanilla absorbs into the raid).
     */
    private void revealStep(Map<Integer, Raid> activeRaids) {
        long now = System.currentTimeMillis();
        for (int key : revealTracker.retainOnly(activeRaids.keySet())) {
            clearGlow(key);
        }
        long quietMs = cfg.revealQuietSeconds * 1000L;
        for (Map.Entry<Integer, Raid> entry : activeRaids.entrySet()) {
            int key = entry.getKey();
            Raid raid = entry.getValue();
            List<Raider> living = livingMembers(raid);
            RevealTracker.Activation act =
                    revealTracker.evaluate(key, now, quietMs, !living.isEmpty());
            if (act == RevealTracker.Activation.NONE) continue;
            applyGlow(key, living);
            if (act == RevealTracker.Activation.ACTIVATED) {
                boolean owed = revealTracker.announceOwed(key);
                revealTracker.markAnnounced(key);
                if (cfg.revealAnnounce && owed) {
                    int sent = sendRevealLine(raid);
                    getLogger().info("RaidBuff reveal: raid " + key + " stalled "
                            + cfg.revealQuietSeconds + "s - " + living.size()
                            + " member(s) revealed, line sent to " + sent + " nearby player(s)");
                } else {
                    getLogger().info("RaidBuff reveal: raid " + key + " stalled "
                            + cfg.revealQuietSeconds + "s - " + living.size()
                            + " member(s) revealed");
                }
            }
        }
    }

    /** The raid's living members right now (its own claim, minus the fallen). */
    private static List<Raider> livingMembers(Raid raid) {
        List<Raider> out = new ArrayList<>();
        for (Raider raider : raid.getRaiders()) {
            if (raider != null && !raider.isDead() && raider.isValid()) {
                out.add(raider);
            }
        }
        return out;
    }

    /**
     * How long one glow application lasts: long enough to span
     * several sweeps at the configured cadence, short enough
     * that a glow we somehow fail to clear dies in seconds at
     * the settled 40-tick sweep (120 ticks = 6 s).
     */
    private int glowDurationTicks() {
        return Math.max(120, cfg.sweepPeriodTicks * 3);
    }

    private void applyGlow(int key, List<Raider> members) {
        int duration = glowDurationTicks();
        for (Raider member : members) {
            member.addPotionEffect(new PotionEffect(
                    PotionEffectType.GLOWING, duration, 0, false, false, false));
            ourGlow.put(member.getUniqueId(), key);
        }
    }

    /**
     * Take the glow back off every entity this feature lit for
     * this raid. ONLY those entities, and only the effect —
     * a glow from any other source on an entity we never lit is
     * untouchable here. (If another source re-lit an entity we
     * also lit, ours and theirs are the same effect slot; the
     * brief's rule stands: when in doubt leave it — in practice
     * the overlap is the raid bell, whose glow is seconds long
     * either way.)
     */
    private void clearGlow(int key) {
        List<UUID> mine = new ArrayList<>();
        for (Map.Entry<UUID, Integer> e : ourGlow.entrySet()) {
            if (e.getValue() == key) mine.add(e.getKey());
        }
        for (UUID id : mine) {
            ourGlow.remove(id);
            Entity entity = Bukkit.getEntity(id);
            if (entity instanceof LivingEntity living && !living.isDead()
                    && living.hasPotionEffect(PotionEffectType.GLOWING)) {
                living.removePotionEffect(PotionEffectType.GLOWING);
            }
        }
    }

    private void clearAllGlow() {
        for (int key : new HashSet<>(ourGlow.values())) {
            clearGlow(key);
        }
    }

    /**
     * The one reveal line, to the players who can act on it:
     * same world as the raid, within 128 blocks of its center.
     *
     * @return how many players the line reached
     */
    private int sendRevealLine(Raid raid) {
        return sendNearbyLine(raid, cfg.revealLine);
    }

    /**
     * A line to the players near a raid: same world, within 128
     * blocks of its center (the reveal's announce radius).
     *
     * @return how many players the line reached
     */
    private int sendNearbyLine(Raid raid, String legacyLine) {
        Location center = raid.getLocation();
        World world = center == null ? null : center.getWorld();
        if (world == null) return 0;
        String line = ChatColor.translateAlternateColorCodes('&', legacyLine);
        int sent = 0;
        for (Player player : world.getPlayers()) {
            if (player.getLocation().distanceSquared(center) <= 128.0 * 128.0) {
                player.sendMessage(line);
                sent++;
            }
        }
        return sent;
    }

    // ------------------------------------------------------------------
    // Raid expiry: the lifetime cap and the persisted clocks (0.1.2)
    // ------------------------------------------------------------------

    /**
     * One sweep's expiry work: prune clocks of raids that ended
     * without an event, then die down every raid at its cap.
     * Expired raids leave {@code activeRaids} so nothing else in
     * this sweep (buff repair, the reveal) touches them again.
     */
    private void expiryStep(Map<Integer, Raid> activeRaids) {
        long now = System.currentTimeMillis();
        if (!expiryTracker.retainOnly(activeRaids.keySet()).isEmpty()) {
            saveExpiryStarts();
        }
        long capMs = cfg.expiryCapMs();
        List<Integer> expired = new ArrayList<>();
        for (Integer key : activeRaids.keySet()) {
            if (expiryTracker.isExpired(key, now, capMs)) {
                expired.add(key);
            }
        }
        for (int key : expired) {
            Raid raid = activeRaids.remove(key);
            if (raid != null) expireRaid(key, raid, now);
        }
    }

    /**
     * A raid dies down (Mike's words): it is over, neither won
     * nor lost. The raid state retires first — so vanilla cannot
     * answer an emptied wave by spawning the next one — then the
     * remaining members give up and leave: removed where they
     * stand, never killed, so no drops, no XP orbs, no EliteMobs
     * coins, no death events, no Hero of the Village. One line
     * to the raid's nearby players, and the log line the nightly
     * combat analysis reads.
     */
    private void expireRaid(int key, Raid raid, long now) {
        long ageMs = expiryTracker.ageMs(key, now);
        // The reveal's glow is this plugin's: off before anyone leaves.
        if (revealTracker.onRaidEnd(key)) {
            clearGlow(key);
        }
        // The rig-proven end (0.1.1 boot k): the NMS raid stop,
        // reached reflectively — the plugin compiles against the
        // Paper API only, the same seam discipline as EliteMobs.
        boolean stopped = stopRaid(raid);
        int removed = 0;
        for (Raider member : livingMembers(raid)) {
            member.remove();
            removed++;
        }
        announcedRaids.remove(key);
        expiryTracker.onRaidEnd(key);
        saveExpiryStarts();
        int sent = sendNearbyLine(raid, cfg.expiryLine);
        getLogger().info("RaidBuff expiry: raid " + key + " died down at the "
                + hoursText(cfg.expiryHours) + " cap (age " + ageText(ageMs) + ") - "
                + removed + " member(s) removed, no loot, line sent to " + sent
                + " nearby player(s)"
                + (stopped ? "" : " [WARNING: raid stop unavailable - the raid object may linger]"));
    }

    /**
     * Retire a raid's state through the game's own stop (the
     * mechanism the 0.1.1 rig proved end-to-end). The Bukkit API
     * has no raid stop, so this reaches CraftRaid's handle by
     * reflection; any failure degrades to a warning — the
     * members are still removed by the caller.
     *
     * @return true when the stop landed
     */
    private boolean stopRaid(Raid raid) {
        try {
            Object handle = raid.getClass().getMethod("getHandle").invoke(raid);
            handle.getClass().getMethod("stop").invoke(handle);
            return true;
        } catch (ReflectiveOperationException | RuntimeException e) {
            getLogger().warning("RaidBuff expiry: could not stop raid " + raidKey(raid)
                    + " (" + e.getClass().getSimpleName() + ": " + e.getMessage()
                    + ") - members removed anyway");
            return false;
        }
    }

    /** The saved lifetime clocks, back off disk (on enable). */
    private void loadExpiryStarts() {
        expiryFile = new File(getDataFolder(), "expiry-starts.yml");
        if (!cfg.enabled || !cfg.expiryEnabled) {
            // The feature is off: saved clocks retire with it,
            // the same rule a disabling reload applies.
            if (expiryFile.exists() && !expiryFile.delete()) {
                getLogger().warning("RaidBuff expiry: could not clear the saved raid clocks");
            }
            return;
        }
        if (!expiryFile.exists()) return;
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(expiryFile);
        ConfigurationSection section = yaml.getConfigurationSection("starts");
        if (section == null) return;
        Map<Integer, Long> saved = new HashMap<>();
        for (String raw : section.getKeys(false)) {
            try {
                long start = section.getLong(raw);
                if (start > 0) saved.put(Integer.parseInt(raw), start);
            } catch (NumberFormatException ignored) {
                // A hand-mangled entry degrades to "not tracked"
                // — the raid's clock starts at first sight —
                // never to a crash.
            }
        }
        expiryTracker.restore(saved);
        if (!saved.isEmpty()) {
            getLogger().info("RaidBuff expiry: restored " + saved.size()
                    + " raid clock(s) from disk - restarts do not reset the cap");
        }
    }

    /** Persist the lifetime clocks (called whenever they change). */
    private void saveExpiryStarts() {
        if (expiryFile == null) return;
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("starts", null);
        for (Map.Entry<Integer, Long> e : expiryTracker.snapshot().entrySet()) {
            yaml.set("starts." + e.getKey(), e.getValue());
        }
        try {
            yaml.save(expiryFile);
        } catch (IOException e) {
            getLogger().warning("RaidBuff expiry: could not persist the raid clocks ("
                    + e.getMessage() + ")");
        }
    }

    // ------------------------------------------------------------------
    // Markers / raid identity
    // ------------------------------------------------------------------

    private boolean isMarked(Raider raider) {
        return raider.getPersistentDataContainer().has(keyBuffed, PersistentDataType.BYTE);
    }

    private int markedRaidKey(Raider raider) {
        Integer key = raider.getPersistentDataContainer().get(keyRaid, PersistentDataType.INTEGER);
        return key == null ? -2 : key;
    }

    private double storedFactor(Raider raider, NamespacedKey key, double fallback) {
        Double value = raider.getPersistentDataContainer().get(key, PersistentDataType.DOUBLE);
        return value == null ? fallback : value;
    }

    /**
     * A raid's identity for marking: its center in its world. One
     * raid can hold a village area at a time, so center+world is
     * unique among live raids; hashed into the PDC as an int.
     */
    static int raidKey(Raid raid) {
        Location center = raid.getLocation();
        String world = center.getWorld() == null ? "?" : center.getWorld().getName();
        String raw = world + "@" + center.getBlockX() + "," + center.getBlockY() + "," + center.getBlockZ();
        return raw.hashCode() & 0x7fffffff;
    }

    /**
     * The wave a raider belongs to, 1-based, clamped to the raid's
     * total waves. Raider#getWave is the primary read; the raid's
     * spawned-group count is the fallback when it reads below 1.
     * The clamp matters: the BONUS wave reads as wave
     * totalWaves+1 (rig-observed, Boot A: raiderWave=4 of
     * totalWaves=3), and it IS the raid's final wave — the
     * late-wave ramp and the top of the elite ramp belong to it,
     * not to wave 1.
     */
    private static int waveNumber(Raid raid, Raider raider) {
        int total = Math.max(1, raid.getTotalWaves());
        int fromRaider = raider.getWave();
        if (fromRaider >= 1) return Math.min(fromRaider, total);
        int spawned = raid.getSpawnedGroups();
        if (spawned >= 1) return Math.min(spawned, total);
        return 1;
    }

    // ------------------------------------------------------------------
    // Command
    // ------------------------------------------------------------------

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("raidbuff")) return false;
        String sub = args.length == 0 ? "status" : args[0].toLowerCase();
        switch (sub) {
            case "reload" -> {
                loadSettings();
                sweep();
                send(sender, "&aExcelRaidBuff config reloaded (live buffs repaired/rescaled by the sweep).");
            }
            case "status" -> {
                int marked = 0;
                int active = 0;
                for (World world : Bukkit.getWorlds()) {
                    active += world.getRaids().size();
                    for (Entity entity : world.getEntities()) {
                        if (entity instanceof Raider raider && isMarked(raider)) marked++;
                    }
                }
                send(sender, "&eExcelRaidBuff " + getDescription().getVersion()
                        + "&7 - " + (cfg.enabled ? "&aENABLED" : "&cDISABLED"));
                for (int omen = 1; omen <= 5; omen++) {
                    send(sender, "&7Omen " + omen + ": HP &fx" + String.format("%.2f", cfg.healthMultiplier(omen))
                            + "&7, DMG &fx" + String.format("%.2f", cfg.damageMultiplier(omen))
                            + "&7, elite share &f" + Math.round(cfg.eliteShare(omen) * 100) + "%"
                            + "&7 (level bonus +" + cfg.eliteLevelBonus(omen) + ")");
                }
                send(sender, "&7Late-wave HP ramp &fx" + String.format("%.2f", cfg.lateWaveHealthRamp)
                        + " &7on the final &f" + cfg.lateWaveCount + " &7waves; announce at omen &f"
                        + cfg.announceMinOmen + "+");
                send(sender, "&7Stuck-wave reveal: " + (cfg.revealEnabled ? "&aON" : "&cOFF")
                        + "&7, quiet &f" + cfg.revealQuietSeconds + "s&7, announce "
                        + (cfg.revealAnnounce ? "&aON" : "&cOFF")
                        + "&7, raids tracked &f" + revealTracker.trackedRaids());
                long oldest = expiryTracker.oldestStartMs();
                send(sender, "&7Raid expiry: " + (cfg.expiryEnabled ? "&aON" : "&cOFF")
                        + "&7, cap &f" + hoursText(cfg.expiryHours)
                        + "&7, raids tracked &f" + expiryTracker.trackedRaids()
                        + (oldest < 0 ? "" : "&7, oldest age &f"
                                + ageText(System.currentTimeMillis() - oldest)));
                send(sender, "&7Elites: " + (cfg.elitesEnabled ? "&aON" : "&cOFF")
                        + "&7, EM seam " + (em.seamDead() ? "&cUNAVAILABLE" : "&aAVAILABLE")
                        + "&7, level cap &f" + em.levelCap(cfg.eliteLevelCapFallback));
                send(sender, "&7Active raids: &f" + active + "&7, buffed raiders loaded: &f" + marked);
                send(sender, "&7Compensation rule: TrainingWheels divides by ExcelRaidBuffAPI.damageMultiplierOf"
                        + " so she takes exactly 0.25 x vanilla at every omen (companion delta,"
                        + " same-window install gate; elites already compensated inside TrainingWheels).");
            }
            default -> send(sender, "&7Usage: /raidbuff [status|reload]");
        }
        return true;
    }

    private void send(CommandSender sender, String legacy) {
        sender.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(legacy));
    }
}

package com.winkreyes.excelbosstrial;

import com.winkreyes.excelbosstrial.api.BossTrialAPI;
import com.winkreyes.excelbosstrial.core.Announcements;
import com.winkreyes.excelbosstrial.core.BossKind;
import com.winkreyes.excelbosstrial.core.BossTrialConfig;
import com.winkreyes.excelbosstrial.core.CommandAccess;
import com.winkreyes.excelbosstrial.core.CooldownBook;
import com.winkreyes.excelbosstrial.core.CreditJudge;
import com.winkreyes.excelbosstrial.core.EncounterLedger;
import com.winkreyes.excelbosstrial.core.EncounterRetention;
import com.winkreyes.excelbosstrial.core.MonumentTracker;
import com.winkreyes.excelbosstrial.core.DenialMessage;
import com.winkreyes.excelbosstrial.core.LaunchSnapshotLog;
import com.winkreyes.excelbosstrial.core.PlacementRegistry;
import com.winkreyes.excelbosstrial.core.ProvenanceLore;
import com.winkreyes.excelbosstrial.core.ScalingRule;
import com.winkreyes.excelbosstrial.core.SealLoot;
import com.winkreyes.excelbosstrial.core.SpoilsMath;
import com.winkreyes.excelbosstrial.core.XpBandPlant;
import com.winkreyes.excelbosstrial.core.XpBandTokens;
import com.winkreyes.excelbosstrial.dragon.DragonChapter;
import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.boss.DragonBattle;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.block.Block;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.ElderGuardian;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Pose;
import org.bukkit.entity.Trident;
import org.bukkit.entity.Wolf;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityPlaceEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * ExcelBossTrial 0.2.2 - the single home for the Wink Reyes Valley
 * seal-boss rules (SEAL-INTEGRITY-RESEARCH.md; every rule below is
 * Mike's settled decision of Oct 18, 2026; the 0.2 additions -
 * participant growth and seal loot - are Mike's first-attempt
 * doctrine, decisions settled Oct 20, 2026, spec
 * BOSSTRIAL-0.2-SPEC-2026-10-20.md; the 0.2.1 additions are the
 * seal-difficulty audit's three integrity fixes, Mike, Oct 20,
 * 2026 late, SEAL-DIFFICULTY-AUDIT-2026-10-20/REPORT.md):
 *
 * - A per-boss DamageGovernor (caps: wither 60/75, elder guardian
 *   35/40, warden 60/75 per-event / per-tick-explosion) trims the
 *   one-shot and explosion-stack damage before it lands.
 * - A credit layer in front of ExcelGuildGate: a boss seal credits
 *   only when player-attributed damage reached 50% of the boss's
 *   max HP (no near-miss mercy) AND at least one encounter
 *   participant stands alive, same world, within 128 blocks at the
 *   death. GuildGate 0.3.1 asks via SealCreditEvent; this plugin
 *   vetoes unearned falls. The raid seal is never touched.
 * - Warden height rule (Mike, Oct 19, 2026 evening amendment):
 *   warden-seal share counts a hit only when the responsible
 *   player stood within +/-12 blocks vertically of the warden when
 *   the hit landed (the sky-tower bow closure). The damage itself
 *   is never reduced - out-of-band hits land and can kill; they
 *   simply earn no seal, exactly like unattributed damage.
 * - Warden range rule (0.2.1): the same band, horizontal twin -
 *   share counts a hit only when the responsible player stood
 *   within 14 blocks horizontally of the warden, the sonic
 *   boom's own reach ("if it couldn't boom you, it doesn't
 *   count" - the ground-level shooting-gallery closure).
 * - Wither "fought free" check (0.2.1, the CAGED-AT-DEATH test,
 *   Mike's strict variant): judged on the fight's last stretch -
 *   the final quarter of the rallied pool, in damage dealt. If
 *   across that stretch the wither never strayed beyond a small
 *   radius of where the stretch began and never landed a hit on
 *   any player, it died caged - no credit (the bedrock-cage
 *   closure), however hard it fought earlier and however honest
 *   the pocket swings. A wither still fighting at the end
 *   credits as always.
 * - Dragon share gate (0.2.1): the dragon joins the share gate,
 *   measured against the phase-aware pool (250 alone until the
 *   flip, 250 + 500 after it), so a staged second boss dealing
 *   her damage while a participant merely stands present no
 *   longer breaks her seal.
 * - A non-crediting kill starts a 6-hour server-wide credit
 *   cooldown for that seal (persisted; shown in /guildgate status);
 *   while the wither cooldown runs, wither summoning is blocked
 *   outright and a refused summon never consumes the skulls.
 * - The elder seal needs all three elder guardians of ONE monument,
 *   each fought for; progress is broadcast per monument, and an
 *   elder whose kill did not count respawns at home after 6 hours
 *   so the trio stays completable.
 * - Spoils scale with the champions in range at the death (loot
 *   xN, cap 5x; the dragon's XP scales instead) - but only on a
 *   kill that CREDITS (Mike, Oct 19, 2026: spoils follow the
 *   verdict; a denied kill drops plain vanilla). EliteMobs loot
 *   and raid drops are excluded by construction (never seen here).
 * - The dragon chapter is ExcelDragonTrial 0.1.2's two-phase fight
 *   machine, ported whole (see the dragon package), plus the shared
 *   presence rule and XP spoils at its death, and (0.2.1) the share
 *   gate above.
 * - 0.2 PARTICIPANT GROWTH ("the boss rallies"): the wither,
 *   elder guardians and warden open at a scaled health pool that
 *   grows by a fixed step each time a NEW player registers
 *   attributed dealt damage (cap 5 damagers), announced in player
 *   words at every step, never shrinking; the share denominator
 *   tracks the scaled pool. Only dealt damage grows the pool -
 *   taking damage or standing nearby never does, so a
 *   non-combatant in the arena cannot inflate the boss. No boss's
 *   outgoing damage changes.
 * - 0.2 SEAL LOOT: curated trophy-tier drops on CREDIT kills
 *   only - a signature pool once per seal (on the seal-breaking
 *   kill; the elder's on the trio-completing kill), one small
 *   bonus roll per extra standing champion (cap +4), and the
 *   dragon's breath + end crystals per credit kill. Curated
 *   stacks are added after the spoils pass and are never
 *   multiplied by it.
 * - 0.2.2 THE WARDEN GIVES UP (seal-hardening option 1B, Mike,
 *   Oct 22, 2026): if a warden's ledger records ONLY excluded
 *   (band) or unattributed damage for the give-up window
 *   (45 s settled) - no counted blow landed, and the warden
 *   landed none either - the fight never came to it, so it
 *   burrows away: no death, no drops, no verdict, no cooldown,
 *   and nothing broadcast (silent, like the fight itself).
 *   Counted damage, or a blow the warden lands on a player,
 *   restarts the clock. Closes the zero-risk perch farm
 *   (261 excluded hits, 4 kills, Oct 22) that the credit
 *   rules alone left running.
 * - 0.2.2 CRYSTAL + ANCHOR ATTRIBUTION (seal-hardening option
 *   3A, Mike, Oct 22, 2026): end crystals and respawn anchors
 *   answer to their PLACER via a placement registry - band-
 *   judged and participation-counted exactly like player-lit
 *   TNT, whoever detonated them. Bukkit ties neither source
 *   to anyone on its own, so both used to resolve to nobody:
 *   a free capped opener off every fresh boss that counted
 *   against the share and could never earn.
 * - 0.2.3 THE ANNOUNCEMENT WINDOW (Mike, Oct 22, 2026): while
 *   announcements.enabled is set and announcements.until (epoch
 *   ms) is in the future, seal attempts and their outcomes are
 *   PUBLIC: an open line names the champion who challenged a
 *   seal boss (per-seal rally throttle, so a re-opened seal
 *   cannot machine-gun chat); every verdict gets a headline
 *   (fail or BROKEN) ahead of its explanation; running credit
 *   cooldowns are announced hourly while anyone is online; and
 *   every player join rebroadcasts the live state (cooldowns +
 *   attempts under way) to ALL online players - Mike's words:
 *   "I want chat respammed whenever someone joins". Burrows
 *   (0.2.2) and verdict-less endings stay silent; mid-fight
 *   stays silent; outside the window, nothing changes at all.
 *
 * All verdict logic lives in the Bukkit-free core package; this
 * class only translates between Bukkit and the core.
 */
public final class ExcelBossTrial extends JavaPlugin implements Listener {

    /** A computed-once death verdict, shared by the death handler
     *  (consequences) and the GuildGate credit hook (veto), whichever
     *  of the two runs first. */
    static final class DeathOutcome {
        final BossKind kind;
        final CreditJudge.Verdict verdict;
        /** Elder-only: a fought kill that advanced the trio but did
         *  not complete it (credit vetoed, no cooldown). */
        final boolean elderProgress;
        final int presentParticipants;
        final int multiplier;
        final double sharePercent;
        final int elderProgressNow; // -1 when not an elder-progress kill
        /** Warden height-band exclusions recorded in this encounter
         *  (0 for every other boss); drives the height denial text. */
        final int heightExcludedHits;
        /** 0.2.1: warden RANGE-band exclusions (the horizontal
         *  shooting gallery); drives the range denial text. */
        final int rangeExcludedHits;
        /** 0.2.1: the wither "fought free" facts held for this
         *  death (the cage kill); drives the cage denial text. */
        final boolean cagedWither;
        /** 0.2: the pool the family actually faced (the ledger's
         *  final scaled maximum) and how many champions the boss
         *  counted (registered damagers) - the verdict messages
         *  state both, per the feedback doctrine (spec section 5). */
        final double poolMax;
        final int damagerCount;
        /** 0.2.5: WHO stood at the fall (the present participants,
         *  by UUID, in the judge's deterministic order) - the
         *  fairness rule pays each of them their own copy of the
         *  named signature gear. Empty on outcomes built without
         *  presence facts. */
        final List<UUID> presentChampions;
        boolean consequencesApplied;

        DeathOutcome(BossKind kind, CreditJudge.Verdict verdict, boolean elderProgress,
                     int presentParticipants, int multiplier, double sharePercent,
                     int elderProgressNow, int heightExcludedHits) {
            this(kind, verdict, elderProgress, presentParticipants, multiplier,
                    sharePercent, elderProgressNow, heightExcludedHits, 0, 0);
        }

        DeathOutcome(BossKind kind, CreditJudge.Verdict verdict, boolean elderProgress,
                     int presentParticipants, int multiplier, double sharePercent,
                     int elderProgressNow, int heightExcludedHits,
                     double poolMax, int damagerCount) {
            this(kind, verdict, elderProgress, presentParticipants, multiplier,
                    sharePercent, elderProgressNow, heightExcludedHits,
                    poolMax, damagerCount, 0, false);
        }

        DeathOutcome(BossKind kind, CreditJudge.Verdict verdict, boolean elderProgress,
                     int presentParticipants, int multiplier, double sharePercent,
                     int elderProgressNow, int heightExcludedHits,
                     double poolMax, int damagerCount,
                     int rangeExcludedHits, boolean cagedWither) {
            this(kind, verdict, elderProgress, presentParticipants, multiplier,
                    sharePercent, elderProgressNow, heightExcludedHits,
                    poolMax, damagerCount, rangeExcludedHits, cagedWither,
                    List.of());
        }

        DeathOutcome(BossKind kind, CreditJudge.Verdict verdict, boolean elderProgress,
                     int presentParticipants, int multiplier, double sharePercent,
                     int elderProgressNow, int heightExcludedHits,
                     double poolMax, int damagerCount,
                     int rangeExcludedHits, boolean cagedWither,
                     List<UUID> presentChampions) {
            this.kind = kind;
            this.verdict = verdict;
            this.elderProgress = elderProgress;
            this.presentParticipants = presentParticipants;
            this.multiplier = multiplier;
            this.sharePercent = sharePercent;
            this.elderProgressNow = elderProgressNow;
            this.heightExcludedHits = heightExcludedHits;
            this.poolMax = poolMax;
            this.damagerCount = damagerCount;
            this.rangeExcludedHits = rangeExcludedHits;
            this.cagedWither = cagedWither;
            this.presentChampions = presentChampions == null
                    ? List.of() : List.copyOf(presentChampions);
        }
    }

    private BossTrialConfig cfg = BossTrialConfig.defaults();
    private final Map<UUID, EncounterLedger> encounters = new HashMap<>();
    private final Map<UUID, DeathOutcome> deathOutcomes = new HashMap<>();
    private final CooldownBook cooldowns = new CooldownBook();
    private final MonumentTracker monuments = new MonumentTracker();
    private final Map<UUID, Long> summonMsgThrottle = new HashMap<>();
    private boolean elderSealApproved;
    /** FLAG 4 (0.2.1): launch-time shooter positions for the
     *  warden's share bands, keyed by projectile UUID. */
    private final LaunchSnapshotLog launchSnapshots = new LaunchSnapshotLog();
    /** 0.2.2 (option 3A): who placed each end crystal and
     *  respawn anchor, so their blasts answer to the placer. */
    private final PlacementRegistry placements = new PlacementRegistry();
    /** 0.2.3: one live seal attempt - who opened it (the first
     *  attributed damager) and at which boss. Runtime-only;
     *  pruned against the encounter map in maintenance. */
    private record Attempt(UUID starter, String starterName, BossKind kind, long startedAtMs) {
    }

    private final Map<UUID, Attempt> attempts = new LinkedHashMap<>();
    private final Set<UUID> attemptsAnnounced = new HashSet<>();
    private final Announcements.RallyThrottle rallyThrottle = new Announcements.RallyThrottle();
    private long lastCooldownAnnounceMs;
    /** Cached GuildGateAPI.isBroken(String), read reflectively
     *  (FLAG 1) - the MagicGate idiom; null until resolved. */
    private java.lang.reflect.Method guildGateIsBrokenMethod;
    private DragonChapter dragonChapter;
    private int maintenanceTask = -1;

    // Messages (config-overridable; code fallbacks are the shipped text).
    private String msgDenyShare;
    private String msgDenyShareHeight;
    private String msgDenyShareRange;
    private String msgDenyShareCage;
    private String msgDenyPresence;
    private String msgDenyCooldown;
    private String msgSpoils;
    private String msgSpoilsDragon;
    private String msgElderProgress;
    private String msgElderTrio;
    private String msgSummonBlocked;
    private String msgRally;

    // Announcement window (0.2.3, config: announcements.*).
    // Code defaults keep the feature OFF until the install
    // packet arms it: a bare jar swap changes nothing.
    private boolean announceEnabled;
    private long announceUntilMs;
    private long announceCooldownIntervalMs = 3_600_000L;
    private long announceRallyCooldownMs = 300_000L;
    private String announceStatusCommand = "/guildgate status";
    // The XP band governor feed (0.2.4): the token store is a final
    // field so it survives /bosstrial reload (reload only re-reads
    // settings); the switch itself is a setting, read in loadSettings.
    private final XpBandTokens xpTokens = new XpBandTokens();
    private boolean xpGovernorEnabled = true;
    private String msgAttempt = "&eSeal attempt! &f{player} &ehas challenged &f{name}&e.";
    private String msgAttemptOngoing = "&eSeal attempt under way: &f{player} &eis challenging &f{name}&e right now.";
    private String msgDenyHeadline = "&cSeal attempt failed: &f{name}&c's seal holds.";
    private String msgCreditHeadline = "&aSeal attempt succeeded: &f{name}&a's seal is BROKEN!";
    private String msgCooldownStatus = "&eSeal cooldowns: &f{list}&e. Check anytime with &f{command}&e.";

    // Seal loot (0.2): parsed tables per boss kind, the master
    // switch, the dragon's breath/crystal numbers, and the
    // signature-once flags (persisted in state.yml).
    private boolean lootEnabled = true;
    private final Map<BossKind, SealLoot.LootTable> lootTables = new EnumMap<>(BossKind.class);
    private int dragonBreathBase = SealLoot.DRAGON_BREATH_BASE;
    private int dragonBreathPer = SealLoot.DRAGON_BREATH_PER_CHAMPION;
    private int dragonBreathCapN = SealLoot.DRAGON_BREATH_CAP;
    private int dragonCrystals = SealLoot.DRAGON_CRYSTALS;
    private final Set<String> signaturePaid = new HashSet<>();
    private final Random lootRng = new Random();

    // ------------------------------------------------------------------
    // Lifecycle
    // ------------------------------------------------------------------

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadSettings();
        loadState();
        cooldowns.setDurationMs(cfg.cooldownMs);
        BossTrialAPI.bind(cooldowns);
        BossTrialAPI.bindFeed(xpTokens);
        Bukkit.getPluginManager().registerEvents(this, this);
        // The GuildGate credit hook lives in its own listener class so
        // this plugin still loads cleanly when ExcelGuildGate is absent
        // (governor + spoils keep working; crediting stays vanilla).
        if (Bukkit.getPluginManager().getPlugin("ExcelGuildGate") != null) {
            Bukkit.getPluginManager().registerEvents(new SealCreditHook(this), this);
            getLogger().info("GuildGate credit hook: ON (boss seals credit only on approval).");
        } else {
            getLogger().warning("ExcelGuildGate not found: credit gating is INACTIVE. "
                    + "Governors, spoils and cooldown displays still run, but GuildGate "
                    + "is not installed to ask, so seals would credit the vanilla way.");
        }
        if (cfg.enabled && dragonEnabled()) {
            dragonChapter = new DragonChapter(this,
                    new File(getDataFolder(), "dragon"),
                    getConfig().getConfigurationSection("dragon"));
            dragonChapter.enable();
        }
        maintenanceTask = Bukkit.getScheduler().scheduleSyncRepeatingTask(
                this, this::maintenance, 600L, 600L);
        getLogger().info("ExcelBossTrial " + getDescription().getVersion()
                + " awake: " + (cfg.enabled ? "ENABLED" : "disabled by config")
                + " (share gate " + (int) (cfg.shareThreshold * 100) + "% of max HP, presence "
                + (int) cfg.presenceRange + " blocks, cooldown "
                + CooldownBook.formatRemaining(cfg.cooldownMs) + ").");
        getLogger().info("XP band governor feed: "
                + (xpGovernorEnabled
                        ? "ON (governed hits plant band verdicts; a listener such as ExcelRewardGuard may scale their mcMMO XP - out-of-band pays 25% there)"
                        : "OFF by config (xp-governor.enabled) - no verdicts planted, every gain pays full"));
    }

    @Override
    public void onDisable() {
        if (maintenanceTask >= 0) {
            Bukkit.getScheduler().cancelTask(maintenanceTask);
            maintenanceTask = -1;
        }
        if (dragonChapter != null) {
            dragonChapter.disable();
            dragonChapter = null;
        }
        saveState();
        BossTrialAPI.unbindFeed();
        xpTokens.clearAll();
        BossTrialAPI.unbind();
    }

    private boolean dragonEnabled() {
        ConfigurationSection d = getConfig().getConfigurationSection("dragon");
        return d == null || d.getBoolean("enabled", true);
    }

    private void loadSettings() {
        reloadConfig();
        Map<String, Object> flat = new HashMap<>();
        ConfigurationSection root = getConfig();
        for (String key : new String[] {"enabled", "credit.share-threshold",
                "credit.presence-range", "credit.cooldown-hours", "spoils.enabled",
                "spoils.max-multiplier", "elder-guardian.respawn-hours",
                "wither.enabled", "wither.caps.per-event-damage", "wither.caps.explosion-per-tick",
                "wither.share-gate", "wither.presence",
                "wither.fought-free-check", "wither.fought-free-radius",
                "wither.fought-free-window",
                "wither.scaling.enabled", "wither.scaling.base-health",
                "wither.scaling.per-extra-damager", "wither.scaling.max-damagers-counted",
                "elder-guardian.enabled", "elder-guardian.caps.per-event-damage",
                "elder-guardian.caps.explosion-per-tick", "elder-guardian.share-gate",
                "elder-guardian.presence",
                "elder-guardian.scaling.enabled", "elder-guardian.scaling.base-health",
                "elder-guardian.scaling.per-extra-damager",
                "elder-guardian.scaling.max-damagers-counted",
                "warden.enabled", "warden.caps.per-event-damage", "warden.caps.explosion-per-tick",
                "warden.share-gate", "warden.presence", "warden.share-height-band",
                "warden.share-range-band", "warden.give-up-window-seconds",
                "warden.scaling.enabled", "warden.scaling.base-health",
                "warden.scaling.per-extra-damager", "warden.scaling.max-damagers-counted",
                "dragon.presence", "dragon.share-gate"}) {
            if (root.contains(key)) {
                flat.put(key, root.get(key));
            }
        }
        cfg = BossTrialConfig.fromMap(flat);
        cooldowns.setDurationMs(cfg.cooldownMs);
        loadLootSettings();
        msgDenyShare = getConfig().getString("messages.deny-share",
                "&c{name} has fallen, but the seal does not break: only {pct}% of the "
                        + "killing damage was dealt by players, and a seal costs {need}%. "
                        + "The way is barred for a time. It stood at {pool} "
                        + "health, facing {champions} champion(s).");
        msgDenyShareHeight = getConfig().getString("messages.deny-share-height",
                "&c{name} has fallen, but the seal does not break: blows struck from "
                        + "too far above or below it do not count. This seal only counts "
                        + "blows landed at the warden's own level, within 12 blocks up "
                        + "or down. Counted damage was {pct}%, and a seal costs {need}%. "
                        + "The way is barred for a time. It stood at {pool} "
                        + "health, facing {champions} champion(s).");
        msgDenyShareRange = getConfig().getString("messages.deny-share-range",
                "&c{name} has fallen, but the seal does not break: blows struck from "
                        + "beyond the warden's reach do not count - if it could not "
                        + "boom you, it does not count. This seal only counts blows "
                        + "landed within 14 blocks of it. Counted damage was {pct}%, "
                        + "and a seal costs {need}%. The way is barred for a time. "
                        + "It stood at {pool} health, facing {champions} "
                        + "champion(s).");
        msgDenyShareCage = getConfig().getString("messages.deny-share-cage",
                "&c{name} has fallen, but the seal does not break: it died caged. "
                        + "For the whole final stretch it was penned where it could "
                        + "not move or strike back, and it never landed a blow - and "
                        + "a wither that dies caged earns no seal, however honest the "
                        + "blows. The way is barred for a time. It stood "
                        + "at {pool} health, facing {champions} champion(s).");
        msgDenyPresence = getConfig().getString("messages.deny-presence",
                "&c{name} has fallen, but the seal does not break: the fight was real "
                        + "({pct}% player damage), yet no champion who fought it was still "
                        + "standing near when it fell. The way is barred for a time. "
                        + "It stood at {pool} health, facing {champions} "
                        + "champion(s).");
        msgDenyCooldown = getConfig().getString("messages.deny-cooldown",
                "&e{name} has fallen, but the seal is not yet ready to break "
                        + "after an earlier unearned kill. This fight was "
                        + "real - the way is barred for a time.");
        msgRally = getConfig().getString("messages.rally",
                "&6{name} feels {ordinal} champion's blow - and swells with rage. "
                        + "Its health rises to {pool}.");
        msgSpoils = getConfig().getString("messages.spoils",
                "&6{count} champions fought - loot x{mult}!");
        msgSpoilsDragon = getConfig().getString("messages.spoils-dragon",
                "&6{count} champions fought - dragon XP x{mult}!");
        msgElderProgress = getConfig().getString("messages.elder-progress",
                "&6Elder seal: {n} of 3 - an elder guardian of this monument has "
                        + "fallen to a true fight.");
        msgElderTrio = getConfig().getString("messages.elder-trio",
                "&6Elder seal: 3 of 3 - every elder guardian of the monument has "
                        + "fallen to true fights. The curse is lifted!");
        msgSummonBlocked = getConfig().getString("messages.summon-blocked",
                "&cThe Wither cannot be summoned yet - its seal is barred for "
                        + "a time after an unearned kill. Your skulls are safe.");
        // The announcement window (0.2.3): read directly (not
        // through BossTrialConfig) - it is presentation, not
        // verdict rules, and keeping it out of the core config
        // keeps this delta off the judged surface.
        announceEnabled = getConfig().getBoolean("announcements.enabled", false);
        // The XP band governor is a standing rule (Mike, Oct 22,
        // 2026): a config without the key runs it ON, like the
        // seal-integrity fixes - absence never disables a rule.
        xpGovernorEnabled = getConfig().getBoolean("xp-governor.enabled", true);
        announceUntilMs = getConfig().getLong("announcements.until", 0L);
        announceCooldownIntervalMs = Math.max(1L, getConfig().getLong(
                "announcements.cooldown-interval-minutes", 60L)) * 60_000L;
        announceRallyCooldownMs = Math.max(0L, getConfig().getLong(
                "announcements.rally-cooldown-seconds", 300L)) * 1000L;
        announceStatusCommand = getConfig().getString("announcements.status-command",
                "/guildgate status");
        msgAttempt = getConfig().getString("messages.attempt", msgAttempt);
        msgAttemptOngoing = getConfig().getString("messages.attempt-ongoing", msgAttemptOngoing);
        msgDenyHeadline = getConfig().getString("messages.deny-headline", msgDenyHeadline);
        msgCreditHeadline = getConfig().getString("messages.credit-headline", msgCreditHeadline);
        msgCooldownStatus = getConfig().getString("messages.cooldown-status", msgCooldownStatus);
    }

    /** The 0.2.3 window check, evaluated at each fire time. */
    private boolean announceWindowActive(long nowMs) {
        return Announcements.windowActive(announceEnabled, announceUntilMs, nowMs);
    }

    // ------------------------------------------------------------------
    // Seal loot settings (0.2): config-first tables, fail-soft
    // ------------------------------------------------------------------

    /**
     * The loot tables live in config.yml under loot.<chapter>
     * (spec section 7: config-first). A missing section - or a
     * 0.1.0-era config with no loot block at all - falls back to
     * the settled code defaults in {@link SealLoot}; a malformed
     * entry is skipped with a warning, never a crash.
     */
    private void loadLootSettings() {
        ConfigurationSection loot = getConfig().getConfigurationSection("loot");
        lootEnabled = loot == null || loot.getBoolean("enabled", true);
        lootTables.clear();
        for (BossKind kind : new BossKind[] {BossKind.WITHER, BossKind.ELDER_GUARDIAN,
                BossKind.WARDEN, BossKind.ENDER_DRAGON}) {
            ConfigurationSection sec = loot == null
                    ? null : loot.getConfigurationSection(lootKey(kind));
            lootTables.put(kind, parseLootTable(kind, sec));
        }
        ConfigurationSection d = loot == null ? null : loot.getConfigurationSection("dragon");
        dragonBreathBase = d == null ? SealLoot.DRAGON_BREATH_BASE
                : Math.max(0, d.getInt("breath-base", SealLoot.DRAGON_BREATH_BASE));
        dragonBreathPer = d == null ? SealLoot.DRAGON_BREATH_PER_CHAMPION
                : Math.max(0, d.getInt("breath-per-champion", SealLoot.DRAGON_BREATH_PER_CHAMPION));
        dragonBreathCapN = d == null ? SealLoot.DRAGON_BREATH_CAP
                : Math.max(0, d.getInt("breath-cap", SealLoot.DRAGON_BREATH_CAP));
        dragonCrystals = d == null ? SealLoot.DRAGON_CRYSTALS
                : Math.max(0, d.getInt("crystals", SealLoot.DRAGON_CRYSTALS));
    }

    private static String lootKey(BossKind kind) {
        return switch (kind) {
            case WITHER -> "wither";
            case ELDER_GUARDIAN -> "elder-guardian";
            case WARDEN -> "warden";
            case ENDER_DRAGON -> "dragon";
        };
    }

    private SealLoot.LootTable parseLootTable(BossKind kind, ConfigurationSection sec) {
        SealLoot.LootTable dflt = SealLoot.defaultsFor(kind);
        if (sec == null) {
            return dflt;
        }
        List<SealLoot.LootItem> signature = dflt.signature();
        if (sec.contains("signature")) {
            signature = new ArrayList<>();
            for (Map<?, ?> entry : sec.getMapList("signature")) {
                SealLoot.LootItem item = parseLootItem(kind, entry);
                if (item != null) {
                    signature.add(item);
                }
            }
        }
        List<SealLoot.BonusEntry> bonus = dflt.bonus();
        if (sec.contains("bonus")) {
            bonus = new ArrayList<>();
            for (Map<?, ?> entry : sec.getMapList("bonus")) {
                SealLoot.BonusEntry row = parseBonusEntry(kind, entry);
                if (row != null) {
                    bonus.add(row);
                }
            }
        }
        return new SealLoot.LootTable(signature, bonus);
    }

    private SealLoot.LootItem parseLootItem(BossKind kind, Map<?, ?> entry) {
        Object mat = entry.get("material");
        if (!(mat instanceof String name) || Material.matchMaterial(name) == null) {
            getLogger().warning("Seal loot [" + kind.displayName()
                    + "]: skipping signature entry with unknown material: " + entry);
            return null;
        }
        int count = entry.get("count") instanceof Number n ? n.intValue() : 1;
        String display = entry.get("name") instanceof String s ? s : null;
        double durability = entry.get("durability") instanceof Number dn
                ? dn.doubleValue() : 1.0;
        Map<String, Integer> enchants = new LinkedHashMap<>();
        if (entry.get("enchants") instanceof Map<?, ?> em) {
            for (Map.Entry<?, ?> e : em.entrySet()) {
                if (e.getKey() instanceof String k && e.getValue() instanceof Number v) {
                    enchants.put(k, v.intValue());
                }
            }
        }
        return new SealLoot.LootItem(name, count, display, enchants, durability);
    }

    private SealLoot.BonusEntry parseBonusEntry(BossKind kind, Map<?, ?> entry) {
        Object mat = entry.get("material");
        if (!(mat instanceof String name) || Material.matchMaterial(name) == null) {
            getLogger().warning("Seal loot [" + kind.displayName()
                    + "]: skipping bonus entry with unknown material: " + entry);
            return null;
        }
        int fixed = entry.get("count") instanceof Number n ? n.intValue() : -1;
        int min = entry.get("min") instanceof Number n ? n.intValue() : Math.max(1, fixed);
        int max = entry.get("max") instanceof Number n ? n.intValue() : Math.max(min, fixed);
        Object w = entry.get("weight") != null ? entry.get("weight") : entry.get("chance");
        int weight = w instanceof Number n ? n.intValue() : 0;
        return new SealLoot.BonusEntry(name, min, max, weight);
    }

    /** Build the Bukkit stack for one curated spec (null when the
     *  material cannot resolve - already warned at parse time). */
    private ItemStack buildLootStack(SealLoot.LootItem item) {
        Material mat = Material.matchMaterial(item.material());
        if (mat == null) {
            return null;
        }
        ItemStack stack = new ItemStack(mat, item.count());
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            if (item.name() != null) {
                meta.setDisplayName(color("&f" + item.name()));
            }
            for (Map.Entry<String, Integer> e : item.enchants().entrySet()) {
                Enchantment ench = Registry.ENCHANTMENT.get(NamespacedKey.minecraft(e.getKey()));
                if (ench == null) {
                    getLogger().warning("Seal loot: unknown enchant key '" + e.getKey()
                            + "' on " + item.material() + " - skipped.");
                    continue;
                }
                if (meta instanceof EnchantmentStorageMeta stored) {
                    stored.addStoredEnchant(ench, e.getValue(), true);
                } else {
                    meta.addEnchant(ench, e.getValue(), true);
                }
            }
            if (item.remainingDurability() < 1.0 && meta instanceof Damageable dmg
                    && mat.getMaxDurability() > 0) {
                int wear = (int) Math.round(
                        (1.0 - item.remainingDurability()) * mat.getMaxDurability());
                dmg.setDamage(Math.max(0, wear));
            }
            stack.setItemMeta(meta);
        }
        return stack;
    }

    // ------------------------------------------------------------------
    // Persistence (state.yml)
    // ------------------------------------------------------------------

    private File stateFile() {
        return new File(getDataFolder(), "state.yml");
    }

    private void loadState() {
        File f = stateFile();
        if (!f.exists()) {
            return;
        }
        YamlConfiguration y = YamlConfiguration.loadConfiguration(f);
        ConfigurationSection cd = y.getConfigurationSection("cooldowns");
        if (cd != null) {
            cooldowns.restore(cd.getValues(false));
        }
        ConfigurationSection mon = y.getConfigurationSection("monuments");
        if (mon != null) {
            Map<String, Object> snap = new HashMap<>();
            snap.put("monumentSeq", mon.getInt("monumentSeq", 0));
            List<Map<?, ?>> elderMaps = mon.getMapList("elders");
            List<Object> elders = new ArrayList<>(elderMaps);
            snap.put("elders", elders);
            monuments.restore(snap);
        }
        elderSealApproved = y.getBoolean("elder-seal-approved", false);
        signaturePaid.clear();
        ConfigurationSection sig = y.getConfigurationSection("signature-paid");
        if (sig != null) {
            for (String sealId : sig.getKeys(false)) {
                if (sig.getBoolean(sealId, false)) {
                    signaturePaid.add(sealId);
                }
            }
        }
        getLogger().info("State loaded: cooldowns " + cooldowns.snapshot().keySet()
                + ", monuments " + monuments.monumentIds().size()
                + ", elder seal " + (elderSealApproved ? "approved" : "open")
                + ", signatures paid " + signaturePaid + ".");
    }

    private void saveState() {
        YamlConfiguration y = new YamlConfiguration();
        for (Map.Entry<String, Object> e : cooldowns.snapshot().entrySet()) {
            y.set("cooldowns." + e.getKey(), e.getValue());
        }
        Map<String, Object> snap = monuments.snapshot();
        y.set("monuments.monumentSeq", snap.get("monumentSeq"));
        y.set("monuments.elders", snap.get("elders"));
        y.set("elder-seal-approved", elderSealApproved);
        for (String sealId : signaturePaid) {
            y.set("signature-paid." + sealId, true);
        }
        try {
            y.save(stateFile());
        } catch (IOException ex) {
            getLogger().warning("Could not save state.yml: " + ex.getMessage());
        }
    }

    // ------------------------------------------------------------------
    // Encounter bookkeeping
    // ------------------------------------------------------------------

    /** 0.2.7: where each open encounter's boss was last seen (chunk). */
    private record SeenAt(String world, int cx, int cz) {
        static SeenAt of(Location loc) {
            return new SeenAt(loc.getWorld() != null ? loc.getWorld().getName() : "",
                    loc.getBlockX() >> 4, loc.getBlockZ() >> 4);
        }
    }

    private final Map<UUID, SeenAt> encounterSeenAt = new HashMap<>();
    private final Map<UUID, Integer> encounterGoneStreak = new HashMap<>();

    /** True when the chunk the boss was last seen in is loaded with its entities. */
    private boolean lastSeenAreaLoaded(UUID id) {
        SeenAt at = encounterSeenAt.get(id);
        if (at == null) {
            return false;
        }
        World w = Bukkit.getWorld(at.world());
        if (w == null || !w.isChunkLoaded(at.cx(), at.cz())) {
            return false;
        }
        try {
            return w.getChunkAt(at.cx(), at.cz()).isEntitiesLoaded();
        } catch (Throwable t) {
            return true; // an API without the entity flag: the streak still guards
        }
    }

    private EncounterLedger encounter(LivingEntity boss, BossKind kind) {
        EncounterLedger ledger = encounters.computeIfAbsent(boss.getUniqueId(), id -> {
            AttributeInstance attr = boss.getAttribute(Attribute.MAX_HEALTH);
            double vanillaMax = attr != null ? attr.getValue() : boss.getHealth();
            double openingMax = vanillaMax;
            ScalingRule rule = cfg.scaling(kind);
            if (rule.active()) {
                // Participant growth opens here (spec section 1):
                // the boss wakes at its full trial strength - the
                // current health rises by the same delta over
                // vanilla, so damage already taken is preserved
                // exactly (never pre-wounded, never healed).
                openingMax = rule.baseHealth();
                if (attr != null && openingMax != vanillaMax) {
                    attr.setBaseValue(openingMax);
                    double delta = openingMax - vanillaMax;
                    if (boss.getHealth() > 0) {
                        if (delta > 0) {
                            boss.setHealth(Math.min(openingMax, boss.getHealth() + delta));
                        } else {
                            boss.setHealth(Math.min(boss.getHealth(), openingMax));
                        }
                    }
                }
                getLogger().info("BossTrial rally [" + kind.displayName()
                        + "]: encounter opens at pool " + fmt(openingMax)
                        + " (vanilla " + fmt(vanillaMax) + ", +"
                        + fmt(rule.perExtraDamager()) + " per extra champion, cap "
                        + rule.maxDamagersCounted() + ").");
            }
            EncounterLedger fresh = new EncounterLedger(kind, id, openingMax,
                    System.currentTimeMillis());
            // 0.2.7: a boss already wounded under a bar this plugin
            // raised lost its ledger (server restart, or a fight
            // untouched past the retention TTL). Set the damage no
            // ledger saw aside, so the share is judged on the fight
            // that is left - never on HP nobody can deal any more.
            double unseen = 0;
            if (rule.active()) {
                unseen = EncounterRetention.unseenOnReopen(openingMax, vanillaMax,
                        rule.baseHealth(), boss.getHealth());
            } else if (kind == BossKind.ENDER_DRAGON && dragonChapter != null
                    && boss instanceof EnderDragon dragonBoss) {
                unseen = dragonChapter.unseenDamage(dragonBoss);
            }
            if (unseen > 0) {
                fresh.markUnseenDamage(unseen);
                getLogger().info("BossTrial re-open [" + kind.displayName() + "]: the boss "
                        + "already lost " + fmt(unseen) + " HP that no ledger saw (server "
                        + "restart or a fight older than "
                        + (EncounterRetention.DEFAULT_TTL_MS / 3_600_000L) + "h) - set aside; "
                        + "the share is measured on the fight that is left (boss at "
                        + fmt(boss.getHealth()) + " HP).");
            }
            if (kind == BossKind.ELDER_GUARDIAN) {
                Location loc = boss.getLocation();
                monuments.register(id, loc.getWorld().getName(), loc.getX(), loc.getY(), loc.getZ());
                saveState();
            }
            return fresh;
        });
        encounterSeenAt.put(boss.getUniqueId(), SeenAt.of(boss.getLocation()));
        if (kind == BossKind.WITHER) {
            // The fought-free check (0.2.1) samples the wither's
            // position wherever the encounter is touched - the
            // governor, the ledger and the hurt-by-boss handler
            // all pass through here, which is everywhere a
            // fighting wither's movement shows up.
            Location loc = boss.getLocation();
            ledger.noteBossPosition(loc.getX(), loc.getY(), loc.getZ());
        }
        return ledger;
    }

    /**
     * A new champion just registered dealt damage: grow the pool
     * (spec section 1). The ledger's denominator ratchets first;
     * the boss's bar follows by exactly the applied delta, and
     * every step is announced in player words (Decision 3: every
     * growth step announced - the line teaches the rule the first
     * time it fires). Growth is applied as the new damager's first
     * governed blow is recorded, so that blow's own share is
     * measured against the rallied pool.
     */
    private void maybeGrowRally(LivingEntity boss, BossKind kind, EncounterLedger ledger) {
        ScalingRule rule = cfg.scaling(kind);
        if (!rule.active()) {
            return;
        }
        int damagers = ledger.damagerCount();
        double delta = ledger.growTo(rule.poolFor(damagers));
        if (delta <= 0) {
            return;
        }
        if (boss.isDead() || boss.getHealth() <= 0) {
            // The new champion's first blow was the killing blow:
            // the denominator growth stands (the verdict is
            // measured against the rallied pool), but there is no
            // living bar left to raise and nothing to announce.
            getLogger().info("BossTrial rally [" + kind.displayName() + "]: champion #"
                    + damagers + " registered on the killing blow - pool denominator "
                    + "grows to " + fmt(ledger.maxHealth()) + " for the verdict.");
            return;
        }
        AttributeInstance attr = boss.getAttribute(Attribute.MAX_HEALTH);
        if (attr != null) {
            attr.setBaseValue(ledger.maxHealth());
        }
        boss.setHealth(Math.min(ledger.maxHealth(), boss.getHealth() + delta));
        String text = msgRally
                .replace("{name}", kind.displayName())
                .replace("{ordinal}", ordinalWord(damagers))
                .replace("{pool}", String.valueOf((int) Math.round(ledger.maxHealth())));
        broadcast(text);
        getLogger().info("BossTrial rally [" + kind.displayName() + "]: champion #"
                + damagers + " registered - pool grows by " + fmt(delta) + " to "
                + fmt(ledger.maxHealth()) + ".");
    }

    private static String ordinalWord(int damagers) {
        return switch (damagers) {
            case 2 -> "a second";
            case 3 -> "a third";
            case 4 -> "a fourth";
            case 5 -> "a fifth";
            default -> "another";
        };
    }

    private boolean chapterOn(BossKind kind) {
        if (!cfg.enabled) {
            return false;
        }
        if (kind == BossKind.ENDER_DRAGON) {
            return dragonEnabled();
        }
        return cfg.chapter(kind).enabled();
    }

    // ------------------------------------------------------------------
    // The governor (HIGH: runs before the damage lands)
    // ------------------------------------------------------------------

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBossDamageGovernor(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof LivingEntity boss)) {
            return;
        }
        if (boss.isDead() || boss.getHealth() <= 0) {
            return; // 0.2.7: never open a ledger on a corpse
        }
        BossKind kind = Attribution.kindOf(boss);
        if (kind == null || kind == BossKind.ENDER_DRAGON || !chapterOn(kind)) {
            return; // the dragon's governor lives in its chapter
        }
        BossTrialConfig.Chapter chapter = cfg.chapter(kind);
        if (!chapter.caps().anyEnabled()) {
            return;
        }
        EncounterLedger ledger = encounter(boss, kind);
        double rawFinal = event.getFinalDamage();
        var governed = ledger.governor().govern(rawFinal,
                Attribution.isExplosion(event.getCause()), Bukkit.getCurrentTick(), chapter.caps());
        if (governed.trimmed() && rawFinal > 0) {
            event.setDamage(event.getDamage() * (governed.damage / rawFinal));
            getLogger().info("BossTrial cap [" + governed.ruleLabel() + "] on " + kind.displayName()
                    + ": hit trimmed " + fmt(rawFinal) + " -> " + fmt(governed.damage)
                    + " (boss at " + fmt(boss.getHealth()) + " HP).");
        }
    }

    // ------------------------------------------------------------------
    // The ledger (MONITOR: records the governed final damage)
    // ------------------------------------------------------------------

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBossDamageRecord(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof LivingEntity boss)) {
            return;
        }
        if (boss.isDead() || boss.getHealth() <= 0) {
            // 0.2.7: a hit on a corpse (the dragon's death animation,
            // an arrow landing after the killing blow) must not re-open
            // a phantom encounter - its ledger was settled at the death.
            // MONITOR runs before damage applies, so the killing blow
            // itself still sees health > 0 and is recorded.
            return;
        }
        BossKind kind = Attribution.kindOf(boss);
        if (kind == null || !chapterOn(kind)) {
            return;
        }
        EncounterLedger ledger = encounter(boss, kind);
        UUID dealt = Attribution.dealtPlayer(event, placements);
        if (dealt != null && event instanceof EntityDamageByEntityEvent byC
                && byC.getDamager() instanceof EnderCrystal
                && event.getFinalDamage() > 0) {
            // Crystal attribution (0.2.2, option 3A): the base
            // resolution can never tie a crystal to anyone, so a
            // non-null dealer here came from the placement
            // registry - the blast answers to the placer.
            Player placer = Bukkit.getPlayer(dealt);
            getLogger().info("BossTrial crystal attribution [" + kind.displayName()
                    + "]: explosion of " + fmt(event.getFinalDamage()) + " attributed to "
                    + (placer != null ? placer.getName() : dealt.toString())
                    + " (placed the end crystal).");
        }
        if (dealt == null && kind == BossKind.ENDER_DRAGON && dragonChapter != null
                && event.getCause() == EntityDamageEvent.DamageCause.BLOCK_EXPLOSION) {
            // Bed attribution (0.2.1, triple-check FLAG 3): a lit
            // bed explodes with NO damager, so Bukkit attributes
            // its damage to nobody - and unexamined, every bed hit
            // would count AGAINST the family under the dragon
            // share gate, denying the sanctioned bed fight. The
            // chapter remembers who lit a bed near the dragon in
            // the last few ticks; that player dealt this damage.
            dealt = dragonChapter.bedLighterNear(boss.getLocation());
            if (dealt != null && event.getFinalDamage() > 0) {
                Player lighter = Bukkit.getPlayer(dealt);
                getLogger().info("BossTrial bed attribution [Dragon]: block explosion of "
                        + fmt(event.getFinalDamage()) + " attributed to "
                        + (lighter != null ? lighter.getName() : dealt.toString())
                        + " (lit the bed).");
            }
        }
        // Anchor attribution (0.2.2, option 3A): a respawn
        // anchor's blast is a bare BLOCK_EXPLOSION - no damager
        // entity exists to resolve. When a REGISTERED anchor
        // exploded, the registry's detonation log remembers who
        // placed it and where the blast was; that player dealt
        // this damage, and the blast's own position is where the
        // blow came from (the band judgment's fallback origin,
        // mirroring the offline-TNT fallback to the TNT's spot).
        double[] blowOrigin = null;
        if (dealt == null
                && event.getCause() == EntityDamageEvent.DamageCause.BLOCK_EXPLOSION) {
            Location bl = boss.getLocation();
            PlacementRegistry.Detonation det = placements.anchorDetonationNear(
                    boss.getWorld().getName(), bl.getX(), bl.getY(), bl.getZ(),
                    System.currentTimeMillis());
            if (det != null) {
                dealt = det.placer();
                blowOrigin = new double[] {det.x(), det.y(), det.z()};
                if (event.getFinalDamage() > 0) {
                    Player placer = Bukkit.getPlayer(dealt);
                    getLogger().info("BossTrial anchor attribution [" + kind.displayName()
                            + "]: explosion of " + fmt(event.getFinalDamage())
                            + " attributed to "
                            + (placer != null ? placer.getName() : dealt.toString())
                            + " (placed the respawn anchor).");
                }
            }
        }
        boolean counts = true;
        boolean banded = false;
        boolean heightOk = true;
        boolean rangeOk = true;
        if (dealt != null && kind == BossKind.WARDEN
                && (cfg.wardenShareHeightBand > 0 || cfg.wardenShareRangeBand > 0)) {
            // Height rule (Mike, Oct 19, 2026) + range rule
            // (0.2.1): judge the RESPONSIBLE PLAYER's position.
            // For a projectile, that position is the LAUNCH
            // snapshot (0.2.1, triple-check FLAG 4): judging the
            // shooter's position when the arrow LANDS would let a
            // player fire from outside the band and pearl inside
            // it before impact. Direct hits and pets judge the
            // attacker where they stand; if that player is
            // offline (a pet whose owner logged off), judge where
            // the blow itself came from instead.
            Player attacker = Bukkit.getPlayer(dealt);
            double[] snap = event instanceof EntityDamageByEntityEvent bySnap
                    && bySnap.getDamager() instanceof Projectile projSnap
                    ? launchSnapshots.lookup(projSnap.getUniqueId()) : null;
            double attackerX;
            double attackerY;
            double attackerZ;
            String attackerLabel;
            if (snap != null) {
                attackerX = snap[0];
                attackerY = snap[1];
                attackerZ = snap[2];
                attackerLabel = (attacker != null ? attacker.getName()
                        : dealt.toString()) + " (launch position)";
            } else if (attacker != null) {
                Location al = attacker.getLocation();
                attackerX = al.getX();
                attackerY = al.getY();
                attackerZ = al.getZ();
                attackerLabel = attacker.getName();
            } else if (event instanceof EntityDamageByEntityEvent by) {
                Location dl = by.getDamager().getLocation();
                attackerX = dl.getX();
                attackerY = dl.getY();
                attackerZ = dl.getZ();
                attackerLabel = dealt.toString();
            } else if (blowOrigin != null) {
                // Anchor blast (0.2.2): no damager entity and
                // the placer is offline - judge where the blow
                // itself came from, the anchor's own position.
                attackerX = blowOrigin[0];
                attackerY = blowOrigin[1];
                attackerZ = blowOrigin[2];
                attackerLabel = dealt.toString() + " (anchor blast)";
            } else {
                Location bl = boss.getLocation();
                attackerX = bl.getX();
                attackerY = bl.getY();
                attackerZ = bl.getZ();
                attackerLabel = dealt.toString();
            }
            Location bossLoc = boss.getLocation();
            heightOk = CreditJudge.withinHeightBand(attackerY, bossLoc.getY(),
                    cfg.wardenShareHeightBand);
            rangeOk = CreditJudge.withinRangeBand(attackerX, attackerZ,
                    bossLoc.getX(), bossLoc.getZ(), cfg.wardenShareRangeBand);
            counts = heightOk && rangeOk;
            banded = true;
            if (!counts && event.getFinalDamage() > 0) {
                StringBuilder why = new StringBuilder();
                if (!heightOk) {
                    why.append("vertical offset ")
                            .append(fmt(Math.abs(attackerY - bossLoc.getY())))
                            .append(" > band ").append(fmt(cfg.wardenShareHeightBand));
                }
                if (!rangeOk) {
                    if (why.length() > 0) {
                        why.append("; ");
                    }
                    why.append("horizontal offset ")
                            .append(fmt(CreditJudge.horizontalDistance(attackerX, attackerZ,
                                    bossLoc.getX(), bossLoc.getZ())))
                            .append(" > band ").append(fmt(cfg.wardenShareRangeBand));
                }
                getLogger().info("BossTrial share band [Warden]: hit from " + attackerLabel
                        + " excluded from the seal share (" + why
                        + "); the damage still lands.");
            }
        }
        boolean newDamager = banded
                ? ledger.recordDealtBanded(dealt, event.getFinalDamage(),
                        System.currentTimeMillis(), heightOk, rangeOk)
                : ledger.recordDealt(dealt, event.getFinalDamage(),
                        System.currentTimeMillis(), counts);
        // The XP band governor feed (0.2.4): plant this hit's band
        // verdict for the XP listener (ExcelRewardGuard), which
        // consumes it when the matching mcMMO PVE gain fires (~1
        // tick later). Only shapes mcMMO actually pays combat XP
        // for plant - a token without a matching gain would shift
        // every later verdict in the queue by one (see XpBandPlant
        // for the mirrored payer table). Silent by doctrine: no
        // player-facing text, here or in the listener.
        if (xpGovernorEnabled && dealt != null && event.getFinalDamage() > 0
                && event instanceof EntityDamageByEntityEvent byGov) {
            Player credited = Bukkit.getPlayer(dealt);
            if (credited != null && XpBandPlant.paysCombatXp(
                    classifyDamager(byGov.getDamager()),
                    credited.getInventory().getItemInMainHand().getType().name())) {
                xpTokens.plant(dealt, counts, Bukkit.getCurrentTick());
            }
        }
        if (newDamager) {
            maybeGrowRally(boss, kind, ledger);
        }
        if (dealt != null && event.getFinalDamage() > 0) {
            // The attempt registry (0.2.3): the first attributed
            // damaging hit opens the attempt, whoever lands it -
            // band-excluded hits included (a perch attempt IS an
            // attempt; last night's were).
            noteAttempt(boss.getUniqueId(), kind, dealt);
        }
        if (kind == BossKind.WARDEN) {
            // The give-up rule (0.2.2) is judged the moment a hit
            // lands, not only on the maintenance cadence: a perch
            // farmer's next arrow after the window is the one
            // that finds the warden already leaving.
            maybeBurrow(boss, ledger);
        }
    }

    /**
     * Register a seal attempt (0.2.3) and, inside the window,
     * announce its opening. An encounter's open line is decided
     * ONCE, at open time: the encounter is marked announced even
     * when the window is off or the per-seal rally throttle
     * suppresses the line, so a suppressed re-open never fires
     * mid-fight later (mid-fight silence), and a window that
     * opens mid-fight does not retro-announce it.
     */
    private void noteAttempt(UUID bossId, BossKind kind, UUID starter) {
        Attempt fresh = new Attempt(starter, playerName(starter), kind,
                System.currentTimeMillis());
        if (attempts.putIfAbsent(bossId, fresh) != null) {
            return; // this encounter is already registered
        }
        if (!attemptsAnnounced.add(bossId)) {
            return;
        }
        long now = System.currentTimeMillis();
        if (!announceWindowActive(now)) {
            return;
        }
        if (rallyThrottle.tryAnnounce(kind.sealId(), now, announceRallyCooldownMs)) {
            broadcast(fill(msgAttempt, kind, null).replace("{player}", fresh.starterName()));
            getLogger().info("BossTrial announce [attempt] " + fresh.starterName()
                    + " challenged " + kind.displayName() + ".");
        } else {
            getLogger().info("BossTrial announce [attempt] suppressed by the rally "
                    + "cooldown for " + kind.displayName() + " (opened by "
                    + fresh.starterName() + ").");
        }
    }

    /** A player's display name for announcements; the offline
     *  fallback covers an attributed owner who has logged off
     *  (a pet's owner, an offline crystal placer). */
    private static String playerName(UUID uuid) {
        Player online = Bukkit.getPlayer(uuid);
        if (online != null) {
            return online.getName();
        }
        String name = Bukkit.getOfflinePlayer(uuid).getName();
        return name != null ? name : "Someone";
    }

    /**
     * The warden gives up (0.2.2, seal-hardening option 1B):
     * when the ledger says the fight never came to this warden
     * for the full give-up window, end the encounter the way
     * settled escapes already end - the ledger evaporates, no
     * verdict, no cooldown - except the warden does not get to
     * stay and be farmed: it burrows away. Silent to players
     * (the feedback doctrine is silent DURING a fight, and the
     * burrow is deliberately quieter than a verdict: there was
     * no fight to explain). Console keeps the record.
     */
    private void maybeBurrow(LivingEntity boss, EncounterLedger ledger) {
        if (cfg.wardenGiveUpWindowMs <= 0 || boss.isDead()) {
            return;
        }
        long now = System.currentTimeMillis();
        if (!ledger.wardenGiveUpDue(now, cfg.wardenGiveUpWindowMs)) {
            return;
        }
        if (encounters.remove(boss.getUniqueId()) == null) {
            return; // settled by the death handler in the same pass
        }
        deathOutcomes.remove(boss.getUniqueId());
        getLogger().info("BossTrial burrow [The Warden]: only out-of-band or "
                + "unattributed damage for " + (cfg.wardenGiveUpWindowMs / 1000)
                + "s (total " + fmt(ledger.totalDamage()) + ", counted "
                + fmt(ledger.playerDamage()) + ") - the fight never came to it, "
                + "so it digs away. No death, no verdict, no cooldown.");
        digAway(boss);
    }

    /**
     * Send a warden down the way vanilla does when a fight
     * abandons it. Bukkit exposes no dig call and the NMS dig
     * is brain-internal with no public trigger, so the adapter
     * reproduces the observable vanilla behavior: the DIGGING
     * pose (which drives the vanilla digging animation and
     * particles client-side), then removal on the animation's
     * timescale. Removal fires no death event - no drops, no
     * XP, no verdict - which is the encounter semantics the
     * rule wants, exactly.
     */
    private void digAway(LivingEntity boss) {
        try {
            boss.setPose(Pose.DIGGING);
        } catch (Throwable ignored) {
            // A build without the pose still gets the removal.
        }
        Bukkit.getScheduler().runTaskLater(this, () -> {
            if (boss.isValid()) {
                boss.remove();
            }
        }, 40L);
    }

    /**
     * Launch snapshots for the warden's share bands (0.2.1,
     * triple-check FLAG 4): the shooter's position at the moment
     * of launch, keyed by the projectile's UUID. Judging bands at
     * land time would let a player fire from outside the band and
     * relocate inside it before the arrow arrives; the shot is
     * judged where it was TAKEN. Entries prune by age in the
     * maintenance pass (a hit is never forgotten on impact - the
     * hit event precedes the damage event it causes).
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        if (event.getEntity().getShooter() instanceof Player shooter) {
            Location loc = shooter.getLocation();
            launchSnapshots.record(event.getEntity().getUniqueId(),
                    loc.getX(), loc.getY(), loc.getZ(), System.currentTimeMillis());
        }
    }

    // ------------------------------------------------------------------
    // Placement registry (0.2.2, option 3A): who placed what
    // ------------------------------------------------------------------

    /** A placed end crystal answers to its placer (registry). */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityPlace(EntityPlaceEvent event) {
        if (event.getEntity() instanceof EnderCrystal crystal
                && event.getPlayer() != null) {
            placements.recordCrystal(crystal.getUniqueId(),
                    event.getPlayer().getUniqueId(), System.currentTimeMillis());
        }
    }

    // There is deliberately NO crystal-removal listener: a
    // crystal leaves the world BY exploding, and its removal
    // event races the blast's own damage events at the same
    // priority - forgetting on removal can (and in the 0.2.2
    // rig, did) erase the placer before the blast resolves.
    // The registry's 24 h age prune is the cleanup; a stale
    // entry can never mis-attribute, because entity UUIDs are
    // never reused.

    /** A placed respawn anchor answers to its placer (registry). */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onAnchorPlace(BlockPlaceEvent event) {
        Block b = event.getBlockPlaced();
        if (b.getType() == Material.RESPAWN_ANCHOR) {
            placements.recordAnchor(b.getWorld().getName(), b.getX(), b.getY(),
                    b.getZ(), event.getPlayer().getUniqueId(),
                    System.currentTimeMillis());
        }
    }

    /** A mined-out anchor leaves the registry. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onAnchorBreak(BlockBreakEvent event) {
        Block b = event.getBlock();
        if (b.getType() == Material.RESPAWN_ANCHOR) {
            placements.forgetAnchor(b.getWorld().getName(), b.getX(), b.getY(),
                    b.getZ());
        }
    }

    /**
     * The primary anchor-detonation feed (0.2.2): the USE
     * itself. BlockExplodeEvent proved too late a witness in
     * the rig - it is delivered after the blast's damage
     * events - but the interact that triggers the blast fires
     * before any of it, so a registered, charged anchor whose
     * use will detonate it (the pure
     * {@link PlacementRegistry#anchorUseDetonates} rule) is
     * converted to a detonation record HERE, in time for its
     * own damage to resolve. The nether use (respawn set)
     * records nothing and keeps the registration: the anchor
     * stays, and stays his.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onAnchorUse(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK
                || event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        Block b = event.getClickedBlock();
        if (b == null || b.getType() != Material.RESPAWN_ANCHOR) {
            return;
        }
        UUID placer = placements.anchorPlacer(b.getWorld().getName(),
                b.getX(), b.getY(), b.getZ());
        if (placer == null) {
            return;
        }
        if (!(b.getBlockData() instanceof org.bukkit.block.data.type.RespawnAnchor ra)) {
            return;
        }
        boolean glowstone = event.getItem() != null
                && event.getItem().getType() == Material.GLOWSTONE;
        boolean explodingDimension = b.getWorld().getEnvironment()
                != World.Environment.NETHER;
        if (!PlacementRegistry.anchorUseDetonates(glowstone, ra.getCharges(),
                ra.getMaximumCharges(), explodingDimension)) {
            return;
        }
        placements.forgetAnchor(b.getWorld().getName(), b.getX(), b.getY(), b.getZ());
        Location center = b.getLocation().add(0.5, 0.5, 0.5);
        placements.recordAnchorDetonation(placer, b.getWorld().getName(),
                center.getX(), center.getY(), center.getZ(),
                System.currentTimeMillis());
        Player p = Bukkit.getPlayer(placer);
        getLogger().info("BossTrial anchor registry: the respawn anchor at "
                + b.getLocation().toVector() + ", placed by "
                + (p != null ? p.getName() : placer.toString())
                + ", is detonated - its blast answers to the placer.");
    }

    /**
     * A respawn anchor exploding converts its placement into a
     * DETONATION the damage resolver can answer from (the
     * blast itself names nobody). The registry lookup is the
     * only gate: by the time this event is observable the
     * anchor block already reads AIR (proven in the 0.2.2
     * rig), so a block-type check would refuse every real
     * detonation - and an unregistered position (a bed's
     * blast, world-generated anchors, anything placed before
     * a restart) simply finds no placer and stays
     * unattributed, the safe direction. This event arrives
     * AFTER the blast's damage in this build, so it cannot
     * feed the detonation record in time (the use feed above
     * does that); what it still does is retire the destroyed
     * anchor's registration - e.g. one chain-detonated by a
     * neighbouring blast, which no use ever touched - so a
     * stale entry can never attribute a later explosion.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onAnchorExplode(BlockExplodeEvent event) {
        Block b = event.getBlock();
        UUID placer = placements.anchorPlacer(b.getWorld().getName(),
                b.getX(), b.getY(), b.getZ());
        placements.forgetAnchor(b.getWorld().getName(), b.getX(), b.getY(),
                b.getZ());
        if (placer == null) {
            return;
        }
        Location center = b.getLocation().add(0.5, 0.5, 0.5);
        placements.recordAnchorDetonation(placer, b.getWorld().getName(),
                center.getX(), center.getY(), center.getZ(),
                System.currentTimeMillis());
        Player p = Bukkit.getPlayer(placer);
        getLogger().info("BossTrial anchor registry: the respawn anchor at "
                + b.getLocation().toVector() + ", placed by "
                + (p != null ? p.getName() : placer.toString())
                + ", detonated - its blast answers to the placer.");
    }

    /** "Took damage from the boss" - the other half of participation. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerHurtByBoss(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) {
            return;
        }
        LivingEntity boss = Attribution.bossOf(event.getDamager());
        if (boss == null || boss.isDead()) {
            // A projectile already in flight when the boss died
            // must not re-open a phantom encounter on the corpse
            // (its ledger was settled at the death).
            return;
        }
        BossKind kind = Attribution.kindOf(boss);
        if (kind == null || !chapterOn(kind)) {
            return;
        }
        EncounterLedger ledger = encounter(boss, kind);
        ledger.recordTaken(victim.getUniqueId(),
                event.getFinalDamage(), System.currentTimeMillis());
    }

    // ------------------------------------------------------------------
    // Death: verdict, spoils, consequences
    // ------------------------------------------------------------------

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBossDeath(EntityDeathEvent event) {
        LivingEntity boss = event.getEntity();
        BossKind kind = Attribution.kindOf(boss);
        if (kind == null || !chapterOn(kind)) {
            return;
        }
        EncounterLedger ledger = encounters.remove(boss.getUniqueId());
        attempts.remove(boss.getUniqueId());
        attemptsAnnounced.remove(boss.getUniqueId());
        DeathOutcome outcome = outcomeFor(kind, boss, ledger);
        // Spoils follow the verdict (Mike, Oct 19, 2026): the
        // multiplier baked into the outcome is already 1 on any
        // denial, so a refused kill falls through here with plain
        // vanilla drops and no spoils broadcast, and the dragon's
        // XP stays unscaled.
        if (cfg.spoilsEnabled && outcome.multiplier > 1) {
            if (kind == BossKind.ENDER_DRAGON && boss instanceof EnderDragon dragon) {
                applyDragonXp(event, dragon, outcome.multiplier);
                broadcast(fill(msgSpoilsDragon, kind, outcome));
            } else {
                List<ItemStack> extra = new ArrayList<>();
                for (ItemStack drop : event.getDrops()) {
                    if (drop == null) {
                        continue;
                    }
                    for (int i = 1; i < outcome.multiplier; i++) {
                        extra.add(drop.clone());
                    }
                }
                event.getDrops().addAll(extra);
                broadcast(fill(msgSpoils, kind, outcome));
            }
        }
        // Seal loot (0.2) lands AFTER the spoils duplication pass,
        // so curated stacks can never be multiplied by it (spec
        // section 4.0), and only on a CREDIT verdict.
        applySealLoot(kind, event, outcome);
        applyConsequences(kind, boss, outcome);
    }

    /**
     * Curated seal loot (spec section 4): the signature pool once
     * per seal on the seal-breaking kill - since 0.2.5 its named
     * gear is granted per standing champion (the fairness rule),
     * its material stacks drop once, shared - one bonus roll per
     * extra standing champion (cap +4) on every credit kill, and
     * the dragon's breath + crystals on every dragon credit kill.
     * Denied kills get none of it - they drop plain vanilla.
     */
    private void applySealLoot(BossKind kind, EntityDeathEvent event, DeathOutcome outcome) {
        if (!lootEnabled || outcome.verdict != CreditJudge.Verdict.CREDIT) {
            return;
        }
        SealLoot.LootTable table = lootTables.getOrDefault(kind, SealLoot.defaultsFor(kind));
        boolean signature = SealLoot.paysSignature(outcome.verdict, outcome.elderProgress,
                signaturePaid.contains(kind.sealId()));
        if (signature) {
            grantSignature(kind, table, outcome, event);
            signaturePaid.add(kind.sealId());
            saveState();
        }
        if (kind == BossKind.ENDER_DRAGON) {
            int breath = Math.min(dragonBreathCapN, dragonBreathBase
                    + dragonBreathPer * Math.max(0, outcome.presentParticipants - 1));
            if (breath > 0) {
                event.getDrops().add(new ItemStack(Material.DRAGON_BREATH, breath));
            }
            if (dragonCrystals > 0) {
                event.getDrops().add(new ItemStack(Material.END_CRYSTAL, dragonCrystals));
            }
            getLogger().info("BossTrial seal loot [Dragon]: breath x" + breath
                    + ", end crystals x" + dragonCrystals + " ("
                    + outcome.presentParticipants + " champion(s) standing).");
            return;
        }
        int rolls = SealLoot.bonusRolls(outcome.presentParticipants);
        if (rolls > 0) {
            List<SealLoot.RolledBonus> rolled = SealLoot.rollBonus(table, rolls, lootRng);
            for (SealLoot.RolledBonus r : rolled) {
                Material mat = Material.matchMaterial(r.material());
                if (mat != null) {
                    event.getDrops().add(new ItemStack(mat, r.count()));
                }
            }
            if (!rolled.isEmpty()) {
                getLogger().info("BossTrial seal loot [" + kind.displayName()
                        + "]: bonus rolls " + rolled + " (" + rolls + " roll(s), "
                        + outcome.presentParticipants + " champion(s) standing).");
            }
        }
    }

    /**
     * Execute the fairness-rule grant plan (0.2.5, spec 4.6) for
     * one paying signature kill: each named-gear stack is built
     * once per standing champion, provenance-stamped for THAT
     * champion and handed straight to them (overflow at their
     * feet); each material stack drops once at the kill, shared,
     * exactly as before. The plan comes from
     * {@link SealLoot#signatureGrants}, so the plan the unit
     * tests pin is the plan the server runs - on the config path
     * and the code-default path alike, since both arrive here as
     * the same LootTable.
     */
    private void grantSignature(BossKind kind, SealLoot.LootTable table,
                                DeathOutcome outcome, EntityDeathEvent event) {
        int gearCopies = 0;
        int sharedStacks = 0;
        for (SealLoot.SignatureGrant grant : SealLoot.signatureGrants(table)) {
            SealLoot.LootItem item = grant.item();
            if (grant.perChampion()) {
                for (UUID championId : outcome.presentChampions) {
                    Player champion = Bukkit.getPlayer(championId);
                    if (champion == null) {
                        // Presence just verified them standing; a
                        // same-tick logout simply misses the
                        // hand-off. Never mail, never hold.
                        continue;
                    }
                    ItemStack stack = buildLootStack(item);
                    if (stack == null) {
                        continue;
                    }
                    stampProvenance(stack, champion.getName());
                    giveToChampion(champion, stack);
                    gearCopies++;
                }
            } else {
                ItemStack stack = buildLootStack(item);
                if (stack != null) {
                    event.getDrops().add(stack);
                    sharedStacks++;
                }
            }
        }
        getLogger().info("BossTrial seal loot [" + kind.displayName()
                + "]: SIGNATURE paid - " + gearCopies + " named-gear copies to "
                + outcome.presentChampions.size() + " champion(s), " + sharedStacks
                + " shared stack(s) - once per seal.");
    }

    /** Hand a stack to a champion; whatever does not fit drops
     *  at their feet - never lost, never held for later. */
    private void giveToChampion(Player champion, ItemStack stack) {
        Map<Integer, ItemStack> overflow = champion.getInventory().addItem(stack);
        for (ItemStack left : overflow.values()) {
            champion.getWorld().dropItemNaturally(champion.getLocation(), left);
        }
    }

    /** Stamp one champion's own copy with its provenance
     *  (0.2.5): the earner line and the date line, rendered in
     *  the vanilla lore style (gray, italic). */
    private void stampProvenance(ItemStack stack, String championName) {
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return;
        }
        List<String> lore = new ArrayList<>();
        for (String line : ProvenanceLore.lines(championName, LocalDate.now())) {
            lore.add(color("&7&o" + line));
        }
        meta.setLore(lore);
        stack.setItemMeta(meta);
    }

    /**
     * Dragon XP spoils. The dragon drops no items, so its XP scales:
     * 12,000 first kill / 500 repeat, times the participant
     * multiplier. Decided by the rig XP probe (BUILD.md): on Paper
     * 26.2 the dragon's ENTIRE XP reward rides in the death event's
     * droppedExp (probe: first kill droppedExp=12000,
     * hasBeenPreviouslyKilled=false, zero battle orbs). So a kill
     * whose event XP is the full vanilla base (12,000 first / 500
     * repeat) scales the event XP in place; if the event ever
     * carries less than the base, the difference to the target is
     * topped up with orbs at the death spot, computed from the
     * OBSERVED event XP so the delivered total is exact either way.
     */
    private void applyDragonXp(EntityDeathEvent event, EnderDragon dragon, int multiplier) {
        DragonBattle battle = dragon.getWorld().getEnderDragonBattle();
        boolean firstKill = battle == null || !battle.hasBeenPreviouslyKilled();
        int target = SpoilsMath.dragonXp(firstKill, multiplier);
        int vanilla = event.getDroppedExp();
        int base = SpoilsMath.dragonXp(firstKill, 1);
        if (vanilla >= base) {
            // The whole reward is in the death event: scale it in place.
            event.setDroppedExp(target);
        } else {
            int topUp = SpoilsMath.dragonXpTopUp(firstKill, multiplier, vanilla);
            if (topUp > 0) {
                dragon.getWorld().spawn(dragon.getLocation(),
                        org.bukkit.entity.ExperienceOrb.class, orb -> orb.setExperience(topUp));
            }
        }
        getLogger().info("Dragon XP spoils: firstKill=" + firstKill + ", death-event XP="
                + vanilla + ", target total=" + target + " (x" + multiplier + ").");
    }

    /** Consequences run once per death, from the death handler only. */
    private void applyConsequences(BossKind kind, LivingEntity boss, DeathOutcome outcome) {
        if (outcome.consequencesApplied) {
            return;
        }
        outcome.consequencesApplied = true;
        long now = System.currentTimeMillis();
        String seal = kind.displayName();
        if (outcome.elderProgress) {
            broadcast(fill(msgElderProgress, kind, outcome));
            getLogger().info("BossTrial elder progress: monument trio now "
                    + outcome.elderProgressNow + " of 3 for this monument ("
                    + fmt(outcome.sharePercent) + "% player damage, "
                    + outcome.presentParticipants + " champion(s) standing).");
            saveState();
            return;
        }
        switch (outcome.verdict) {
            case CREDIT -> {
                if (announceWindowActive(now)) {
                    broadcast(fill(msgCreditHeadline, kind, outcome));
                }
                if (kind == BossKind.ELDER_GUARDIAN) {
                    broadcast(fill(msgElderTrio, kind, outcome));
                    elderSealApproved = true;
                }
                getLogger().info("BossTrial verdict CREDIT: " + seal + " seal earned ("
                        + fmt(outcome.sharePercent) + "% player damage, "
                        + outcome.presentParticipants + " champion(s) standing).");
            }
            case DENY_SHARE, DENY_PRESENCE -> {
                cooldowns.start(kind.sealId(), now);
                if (announceWindowActive(now)) {
                    broadcast(fill(msgDenyHeadline, kind, outcome));
                }
                broadcast(fill(denialMessageFor(kind, outcome), kind, outcome));
                if (kind == BossKind.ELDER_GUARDIAN) {
                    monuments.markPendingRespawn(boss.getUniqueId(),
                            now + cfg.elderRespawnMs);
                }
                getLogger().info("BossTrial verdict " + outcome.verdict + ": " + seal
                        + " seal NOT credited (" + fmt(outcome.sharePercent)
                        + "% player damage, " + outcome.presentParticipants
                        + " present). Cooldown until "
                        + CooldownBook.formatRemaining(cfg.cooldownMs) + " from now;"
                        + " wither summons blocked / elder respawn queued as applicable.");
            }
            case DENY_COOLDOWN -> {
                if (announceWindowActive(now)) {
                    broadcast(fill(msgDenyHeadline, kind, outcome));
                }
                broadcast(fill(denialMessageFor(kind, outcome), kind, outcome));
                if (kind == BossKind.ELDER_GUARDIAN) {
                    monuments.markPendingRespawn(boss.getUniqueId(),
                            now + cfg.elderRespawnMs);
                }
                getLogger().info("BossTrial verdict DENY_COOLDOWN: " + seal
                        + " kill was fought for but the seal is cooling down ("
                        + CooldownBook.formatRemaining(
                                cooldowns.remainingMs(kind.sealId(), now)) + " left).");
            }
        }
        saveState();
    }

    /**
     * The single verdict computation, shared by the death handler and
     * the GuildGate credit hook. Computed once per dying boss and
     * cached by boss UUID.
     *
     * Package-private: SealCreditHook calls it.
     */
    DeathOutcome outcomeFor(BossKind kind, LivingEntity boss, EncounterLedger ledgerOrNull) {
        DeathOutcome cached = deathOutcomes.get(boss.getUniqueId());
        if (cached != null) {
            return cached;
        }
        EncounterLedger ledger = ledgerOrNull != null
                ? ledgerOrNull : encounters.get(boss.getUniqueId());
        long now = System.currentTimeMillis();
        // Presence facts for the shared participant definition.
        // 0.2.5: the judge returns WHO stands at the fall, not
        // just how many - the fairness rule pays each of them.
        Set<UUID> participants = ledger == null ? Set.of() : ledger.participants();
        World bossWorld = boss.getWorld();
        Location bossLoc = boss.getLocation();
        List<UUID> presentChampions = CreditJudge.presentParticipants(participants, uuid -> {
            Player p = Bukkit.getPlayer(uuid);
            if (p == null) {
                return new CreditJudge.PresenceFact(false, false, false, Double.MAX_VALUE);
            }
            boolean sameWorld = p.getWorld().equals(bossWorld);
            double d = sameWorld ? CreditJudge.distance(
                    p.getLocation().getX(), p.getLocation().getY(), p.getLocation().getZ(),
                    bossLoc.getX(), bossLoc.getY(), bossLoc.getZ()) : Double.MAX_VALUE;
            return new CreditJudge.PresenceFact(true, !p.isDead(), sameWorld, d);
        }, cfg.presenceRange);
        int present = presentChampions.size();
        boolean presenceRequired = kind == BossKind.ENDER_DRAGON
                ? cfg.dragonPresence : cfg.chapter(kind).presence();
        boolean presenceOk = !presenceRequired || present >= 1;
        // Dragon share gate (0.2.1): the denominator is the
        // PHASE-AWARE pool - the chapter knows which phase pools
        // the fight spans (250 alone until the flip, 250 + 500
        // after it), while the ledger only ever captured the
        // dragon's current bar. Sync the ledger's denominator up
        // to the chapter's pool before judging (a ratchet: it
        // never shrinks, and an untracked dragon - chapter pool
        // 0 - keeps its own bar as the denominator).
        // RESTART RULE (written choice, triple-check FLAG 3): a
        // ledger that opens MID-FIGHT (a server/plugin restart
        // adopts a live dragon) starts with an EMPTY dealt
        // record while this sync still raises its denominator to
        // the full phase pool - the share must be re-earned from
        // zero against the whole pool. Safe direction by design:
        // harder, never easier.
        if (kind == BossKind.ENDER_DRAGON && ledger != null && dragonChapter != null) {
            double trialPool = dragonChapter.trialPool(boss.getUniqueId());
            if (trialPool > 0) {
                ledger.growTo(trialPool);
            }
        }
        boolean shareRequired = kind == BossKind.ENDER_DRAGON
                ? cfg.dragonShareGate : cfg.chapter(kind).shareGate();
        boolean shareMet = !shareRequired
                || (ledger != null && ledger.shareMet(cfg.shareThreshold));
        // Wither "fought free" veto (0.2.1, the CAGED-AT-DEATH
        // test - Mike's strict variant): judged on the fight's
        // last stretch (the final fought-free-window fraction of
        // the rallied pool, in damage dealt) from the ledger's
        // damage-stamped facts. A cage kill can pass the raw
        // share percent (the pocket swings are honest player
        // damage), so the veto folds into shareMet itself - the
        // verdict, the cooldown, the vanilla drops and the summon
        // block then all follow the ordinary DENY_SHARE path
        // unchanged, and DenialMessage names the cage as the
        // reason via the outcome's cagedWither fact.
        boolean cagedWither = kind == BossKind.WITHER && cfg.witherFoughtFreeCheck
                && ledger != null
                && CreditJudge.witherNeverFought(
                        ledger.windowBossHitPlayer(
                                cfg.witherFoughtFreeWindow * ledger.maxHealth()),
                        ledger.windowMaxDisplacement(
                                cfg.witherFoughtFreeWindow * ledger.maxHealth()),
                        cfg.witherFoughtFreeRadius);
        if (cagedWither) {
            shareMet = false;
        }
        boolean cooldownActive = cooldowns.isActive(kind.sealId(), now);
        CreditJudge.Verdict verdict = CreditJudge.judge(shareMet, presenceOk, cooldownActive);
        // The elder trio folds into the verdict: a fought kill that
        // does not complete its monument's trio is progress, not
        // credit - carried as verdict CREDIT plus the elderProgress
        // flag (approvesCredit vetoes on the flag; consequences
        // broadcast progress instead of denial or triumph).
        double poolMax = ledger != null ? ledger.maxHealth()
                : (boss.getAttribute(Attribute.MAX_HEALTH) != null
                        ? boss.getAttribute(Attribute.MAX_HEALTH).getValue() : 0);
        int damagerCount = ledger != null ? ledger.damagerCount() : 0;
        int elderProgressNow = -1;
        if (kind == BossKind.ELDER_GUARDIAN && verdict == CreditJudge.Verdict.CREDIT
                && !elderLatchStands()) {
            elderProgressNow = monuments.markCounted(boss.getUniqueId());
            if (elderProgressNow >= 0 && elderProgressNow < MonumentTracker.TRIO_SIZE) {
                DeathOutcome progress = new DeathOutcome(kind, CreditJudge.Verdict.CREDIT,
                        true, present, spoilsMultiplier(verdict, present),
                        sharePercent(ledger), elderProgressNow,
                        ledger == null ? 0 : ledger.heightExcludedHits(),
                        poolMax, damagerCount,
                        ledger == null ? 0 : ledger.rangeExcludedHits(), cagedWither,
                        presentChampions);
                deathOutcomes.put(boss.getUniqueId(), progress);
                return progress;
            }
            // elderProgressNow == TRIO_SIZE: the trio completes on
            // this kill and the verdict stays a full CREDIT.
        }
        DeathOutcome outcome = new DeathOutcome(kind, verdict, false, present,
                spoilsMultiplier(verdict, present), sharePercent(ledger),
                elderProgressNow, ledger == null ? 0 : ledger.heightExcludedHits(),
                poolMax, damagerCount,
                ledger == null ? 0 : ledger.rangeExcludedHits(), cagedWither,
                presentChampions);
        deathOutcomes.put(boss.getUniqueId(), outcome);
        return outcome;
    }

    /**
     * The denial broadcast text for a death (Mike's seal feedback
     * doctrine, Oct 19, 2026): every failed seal names its specific
     * reason - and a warden share denial with height-band
     * exclusions names the level rule itself, via
     * {@link DenialMessage}'s selection.
     */
    private String denialMessageFor(BossKind kind, DeathOutcome outcome) {
        return switch (DenialMessage.select(kind, outcome.verdict,
                outcome.heightExcludedHits, outcome.rangeExcludedHits,
                outcome.cagedWither)) {
            case SHARE_HEIGHT -> msgDenyShareHeight;
            case SHARE_RANGE -> msgDenyShareRange;
            case SHARE_CAGE -> msgDenyShareCage;
            case PRESENCE -> msgDenyPresence;
            case COOLDOWN -> msgDenyCooldown;
            default -> msgDenyShare; // SHARE; NONE cannot occur on a denial
        };
    }

    private int spoilsMultiplier(CreditJudge.Verdict verdict, int present) {
        // Spoils follow the verdict (Mike, Oct 19, 2026): only a
        // kill that CREDITS multiplies its drops / scales the
        // dragon's XP. A denied kill pays vanilla 1x, however many
        // champions stood by. An elder-progress kill carries
        // verdict CREDIT into here, so each elder's own verdict
        // governs its own drops.
        return SpoilsMath.multiplierFor(cfg.spoilsEnabled, verdict, present, cfg.spoilsCap);
    }

    private static double sharePercent(EncounterLedger ledger) {
        return ledger == null ? 0.0 : ledger.playerSharePercent();
    }

    /**
     * GuildGate's word on whether a seal stands broken - read
     * reflectively through GuildGateAPI.isBroken(String), the
     * MagicGate consumption idiom (0.2.1, triple-check FLAG 1).
     * Null when unverifiable: GuildGate absent or disabled, or a
     * pre-0.3.3 GuildGate whose API has no isBroken yet - in
     * which case callers fall back to trusting local state,
     * never to pretending knowledge.
     */
    private Boolean guildGateSealBroken(String sealId) {
        Plugin gg = Bukkit.getPluginManager().getPlugin("ExcelGuildGate");
        if (gg == null || !gg.isEnabled()) {
            return null;
        }
        try {
            if (guildGateIsBrokenMethod == null) {
                Class<?> api = Class.forName("com.winkreyes.guildgate.api.GuildGateAPI",
                        true, gg.getClass().getClassLoader());
                guildGateIsBrokenMethod = api.getMethod("isBroken", String.class);
            }
            Object r = guildGateIsBrokenMethod.invoke(null, sealId);
            return r instanceof Boolean b ? b : null;
        } catch (NoSuchMethodException e) {
            // A GuildGate older than 0.3.3: the cross-check does
            // not exist yet. Silent null (not a failure).
            return null;
        } catch (Exception e) {
            getLogger().warning("Could not read GuildGateAPI.isBroken(\"" + sealId
                    + "\"): " + e.getMessage());
            guildGateIsBrokenMethod = null;
            return null;
        }
    }

    /**
     * Whether the elder approval latch stands (0.2.1, FLAG 1).
     * The latch short-circuits the per-kill verdict once the
     * trio has completed - correct only while GuildGate agrees
     * the elder seal stands broken. A /guildgate revoke
     * elder_guardian touches GuildGate state only and used to
     * desync the latch silently: the next elder kill of ANY kind
     * (even a 0%-share conduit kill) would re-break the seal on
     * the latch's say-so. Now the latch is validated against
     * GuildGate's live state at every use: shown unbroken ->
     * clear it, log the stale latch, and fall through to the
     * ordinary per-kill verdict + trio logic. Unverifiable
     * (pre-0.3.3 GuildGate) -> trust the latch, as 0.2.0 did.
     */
    private boolean elderLatchStands() {
        if (!elderSealApproved) {
            return false;
        }
        Boolean broken = guildGateSealBroken(BossKind.ELDER_GUARDIAN.sealId());
        if (CreditJudge.elderLatch(true, broken) == CreditJudge.LatchState.STALE) {
            elderSealApproved = false;
            saveState();
            getLogger().warning("BossTrial elder latch was STALE: GuildGate shows the "
                    + "elder seal unbroken (a revoke or reset landed GuildGate-side), "
                    + "so the latch is cleared - elder kills go back through the "
                    + "per-kill verdict and the monument trio.");
            return false;
        }
        return true;
    }

    /** Credit decision for SealCreditHook: true = let GuildGate credit. */
    boolean approvesCredit(BossKind kind, LivingEntity boss) {
        if (!chapterOn(kind)) {
            return true;
        }
        if (kind == BossKind.ELDER_GUARDIAN && elderLatchStands()) {
            return true;
        }
        DeathOutcome outcome = outcomeFor(kind, boss, null);
        if (outcome.elderProgress) {
            return false; // fought, counted - but the trio is not done
        }
        return outcome.verdict == CreditJudge.Verdict.CREDIT;
    }

    // ------------------------------------------------------------------
    // Wither summon block (cooldown consequence; skulls never consumed)
    // ------------------------------------------------------------------

    private boolean witherSummonBlocked() {
        return cfg.enabled && cfg.wither.enabled()
                && cooldowns.isActive(BossKind.WITHER.sealId(), System.currentTimeMillis());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onSkullPlace(BlockPlaceEvent event) {
        Material type = event.getBlockPlaced().getType();
        if (type != Material.WITHER_SKELETON_SKULL
                && type != Material.WITHER_SKELETON_WALL_SKULL) {
            return;
        }
        if (!witherSummonBlocked()) {
            return;
        }
        // Cancelling the placement keeps the skull in the player's
        // hand - nothing is ever consumed by a refused summon.
        event.setCancelled(true);
        Player p = event.getPlayer();
        long now = System.currentTimeMillis();
        Long last = summonMsgThrottle.get(p.getUniqueId());
        if (last == null || now - last > 5000L) {
            summonMsgThrottle.put(p.getUniqueId(), now);
            p.sendMessage(color(fill(msgSummonBlocked, BossKind.WITHER, null)));
        }
        getLogger().info("Wither summon blocked (skull place by " + p.getName()
                + "): seal cooldown, "
                + CooldownBook.formatRemaining(
                        cooldowns.remainingMs(BossKind.WITHER.sealId(), now)) + " left.");
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onSkullDispense(BlockDispenseEvent event) {
        if (event.getItem().getType() != Material.WITHER_SKELETON_SKULL) {
            return;
        }
        if (!witherSummonBlocked()) {
            return;
        }
        event.setCancelled(true);
        getLogger().info("Wither summon blocked (dispenser at "
                + event.getBlock().getLocation().toVector() + "): seal cooldown.");
        notifyNearby(event.getBlock().getLocation());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onWitherSpawn(CreatureSpawnEvent event) {
        if (event.getSpawnReason() != CreatureSpawnEvent.SpawnReason.BUILD_WITHER) {
            return;
        }
        if (!witherSummonBlocked()) {
            return;
        }
        event.setCancelled(true);
        getLogger().info("Wither summon blocked (BUILD_WITHER spawn): seal cooldown.");
        notifyNearby(event.getLocation());
    }

    private void notifyNearby(Location loc) {
        long now = System.currentTimeMillis();
        Long last = summonMsgThrottle.get(new UUID(0L, 1L));
        if (last != null && now - last <= 5000L) {
            return;
        }
        summonMsgThrottle.put(new UUID(0L, 1L), now);
        if (loc.getWorld() == null) {
            return;
        }
        for (Player p : loc.getWorld().getPlayers()) {
            if (p.getLocation().distanceSquared(loc) <= 32 * 32) {
                p.sendMessage(color(fill(msgSummonBlocked, BossKind.WITHER, null)));
            }
        }
    }

    // ------------------------------------------------------------------
    // Maintenance: elder respawns, stale-encounter pruning
    // ------------------------------------------------------------------

    private void maintenance() {
        long now = System.currentTimeMillis();
        // Launch snapshots outlive their hit by design (FLAG 4);
        // 30 s is far past any projectile's flight.
        launchSnapshots.prune(now, 30_000L);
        // Placement registry hygiene (0.2.2): placements are
        // recorded by the place events; crystals are never
        // forgotten on removal (the removal races the blast's
        // own damage resolution), so these age prunes are the
        // cleanup for both halves - leak insurance for entries
        // whose lifecycle simply ended offstage.
        placements.pruneCrystals(now, 24L * 60L * 60L * 1000L);
        placements.pruneAnchors(now, 7L * 24L * 60L * 60L * 1000L);
        placements.pruneDetonations(now);
        // The warden give-up (0.2.2), maintenance cadence: the
        // damage path judges the active farm the moment the
        // window lapses; this pass catches the fight that
        // simply STOPPED (a perch abandoned mid-window still
        // ends - the warden does not stand wounded forever).
        for (Map.Entry<UUID, EncounterLedger> e
                : new ArrayList<>(encounters.entrySet())) {
            if (e.getValue().kind() != BossKind.WARDEN) {
                continue;
            }
            Entity ent = Bukkit.getEntity(e.getKey());
            if (ent instanceof LivingEntity le && !le.isDead()) {
                maybeBurrow(le, e.getValue());
            }
        }
        // Prune encounters whose boss is gone (escape, despawn,
        // remove): the ledger simply evaporates - no cooldown, no
        // verdict (settled: escapes are free).
        // 0.2.7 (Iris audit): getEntity is null for an UNLOADED boss
        // too, so a wounded boss whose area unloaded (the family went
        // home, logged off, or wiped and respawned far away) is NOT
        // gone - its ledger is kept (EncounterRetention).
        for (Map.Entry<UUID, EncounterLedger> e
                : new ArrayList<>(encounters.entrySet())) {
            UUID id = e.getKey();
            Entity ent = Bukkit.getEntity(id);
            boolean found = ent != null;
            boolean dead = found && ent.isDead();
            boolean areaLoaded = false;
            int streak = 0;
            if (found && !dead) {
                encounterSeenAt.put(id, SeenAt.of(ent.getLocation()));
                encounterGoneStreak.remove(id);
            } else if (!found) {
                areaLoaded = lastSeenAreaLoaded(id);
                if (areaLoaded) {
                    streak = encounterGoneStreak.merge(id, 1, Integer::sum);
                } else {
                    encounterGoneStreak.remove(id);
                }
            }
            EncounterRetention.Decision d = EncounterRetention.decide(found, dead,
                    areaLoaded, streak, e.getValue().lastTouchMs(), now,
                    EncounterRetention.DEFAULT_TTL_MS);
            if (d == EncounterRetention.Decision.PRUNE) {
                encounters.remove(id);
                if (!found) {
                    getLogger().info("BossTrial encounter [" + e.getValue().kind().displayName()
                            + "] closed without a verdict: "
                            + (areaLoaded ? "the boss is gone from its loaded area"
                                    : "untouched and unloaded for over "
                                    + (EncounterRetention.DEFAULT_TTL_MS / 3_600_000L) + "h")
                            + " (no cooldown).");
                }
            }
        }
        encounterSeenAt.keySet().retainAll(encounters.keySet());
        encounterGoneStreak.keySet().retainAll(encounters.keySet());
        deathOutcomes.entrySet().removeIf(e -> e.getValue().consequencesApplied
                && Bukkit.getEntity(e.getKey()) == null);
        // The attempt registry (0.2.3) prunes against the live
        // encounter map: a boss that burrowed (0.2.2), escaped,
        // despawned or was removed leaves silently - no outcome
        // line for a verdict-less ending, ever.
        if (!attempts.isEmpty()) {
            attempts.keySet().retainAll(encounters.keySet());
            attemptsAnnounced.retainAll(encounters.keySet());
        }
        // The hourly cooldown announcement (0.2.3): one combined
        // line for every cooling seal, only inside the window and
        // only while someone is online to hear it. The stamp
        // moves ONLY on an actual broadcast, so an empty server
        // at the hour mark does not eat the announcement.
        if (announceWindowActive(now)
                && Announcements.hourlyDue(lastCooldownAnnounceMs,
                        announceCooldownIntervalMs, now)
                && !Bukkit.getOnlinePlayers().isEmpty()) {
            String line = cooldownStatusLine(now);
            if (line != null) {
                broadcast(line);
                lastCooldownAnnounceMs = now;
                getLogger().info("BossTrial announce [cooldown] broadcast on the "
                        + "hourly pass.");
            }
        }
        // Due elder respawns fire when the home chunk is loaded.
        boolean changed = false;
        for (MonumentTracker.ElderRecord pending : monuments.dueRespawns(now)) {
            World world = Bukkit.getWorld(pending.world);
            if (world == null) {
                continue;
            }
            int chunkX = ((int) Math.floor(pending.x)) >> 4;
            int chunkZ = ((int) Math.floor(pending.z)) >> 4;
            if (!world.isChunkLoaded(chunkX, chunkZ)) {
                continue;
            }
            Location home = new Location(world, pending.x, pending.y, pending.z);
            ElderGuardian fresh = world.spawn(home, ElderGuardian.class);
            monuments.completeRespawn(pending.uuid, fresh.getUniqueId());
            getLogger().info("Elder guardian respawned at home " + home.toVector()
                    + " (monument " + pending.monumentId
                    + ") after its unearned kill's respawn timer.");
            changed = true;
        }
        if (changed) {
            saveState();
        }
    }

    /**
     * The combined cooldown status line (0.2.3) - every seal on
     * credit cooldown with its time left and the way to check -
     * or null when no seal is cooling down. Shared by the hourly
     * broadcast and the join rebroadcast so both always read the
     * same words.
     */
    private String cooldownStatusLine(long nowMs) {
        List<Announcements.CooldownPart> parts = new ArrayList<>();
        for (BossKind kind : BossKind.values()) {
            long remaining = cooldowns.remainingMs(kind.sealId(), nowMs);
            if (remaining > 0) {
                parts.add(new Announcements.CooldownPart(kind.displayName(), remaining));
            }
        }
        String list = Announcements.cooldownListText(parts);
        if (list.isEmpty()) {
            return null;
        }
        return msgCooldownStatus.replace("{list}", list)
                .replace("{command}", announceStatusCommand);
    }

    /**
     * Join rebroadcast (0.2.3, Mike, Oct 22, 2026 - "I want chat
     * respammed whenever someone joins"): while the window is
     * live, EVERY join rebroadcasts the current seal state to
     * ALL online players, the joiner included. There is
     * deliberately NO per-player guard: repeated rejoins
     * rebroadcast every time, by Mike's direct call. A quiet
     * board (no cooldown, no attempt under way) and an expired
     * window both stay silent. The 3-second delay lets the
     * joiner actually load in before the lines land.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!announceWindowActive(System.currentTimeMillis())) {
            return;
        }
        UUID joiner = event.getPlayer().getUniqueId();
        Bukkit.getScheduler().runTaskLater(this, () -> {
            Player p = Bukkit.getPlayer(joiner);
            if (p == null || !p.isOnline()) {
                return;
            }
            long now = System.currentTimeMillis();
            if (!announceWindowActive(now)) {
                return;
            }
            List<String> lines = new ArrayList<>();
            String cooldownLine = cooldownStatusLine(now);
            if (cooldownLine != null) {
                lines.add(cooldownLine);
            }
            Set<String> seen = new HashSet<>();
            for (Attempt a : attempts.values()) {
                String line = fill(msgAttemptOngoing, a.kind(), null)
                        .replace("{player}", a.starterName());
                if (seen.add(line)) {
                    lines.add(line);
                }
            }
            for (String line : lines) {
                broadcast(line);
            }
            if (!lines.isEmpty()) {
                getLogger().info("BossTrial announce [rebroadcast] " + lines.size()
                        + " line(s) on the join of " + p.getName() + ".");
            }
        }, 60L);
    }

    // ------------------------------------------------------------------
    // Command
    // ------------------------------------------------------------------

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // 0.2.6 PERMISSION SPLIT: plugin.yml no longer gates the
        // whole command, so the split is enforced here - the
        // status view is public (any player may check seal state
        // and cooldowns; the seal announcements point players at
        // it), and every other subcommand stays behind the admin
        // node. A refused non-admin gets the same denial Bukkit's
        // plugin.yml gate used to send (CommandAccess pins both
        // the matrix and the wording).
        if (!CommandAccess.allowed(args, sender.hasPermission(CommandAccess.ADMIN_PERMISSION))) {
            sender.sendMessage(color("&c" + CommandAccess.DENIAL_MESSAGE));
            return true;
        }
        if (CommandAccess.isStatusCall(args)) {
            sender.sendMessage(color("&6ExcelBossTrial &f" + getDescription().getVersion()
                    + " &7- " + (cfg.enabled ? "enabled" : "&cdisabled by config")));
            for (BossKind kind : BossKind.values()) {
                if (kind == BossKind.ENDER_DRAGON) {
                    sender.sendMessage(color("&7Dragon chapter: "
                            + (dragonChapter != null ? "running (two-phase machine)"
                                    : "off") + ", presence rule " + (cfg.dragonPresence ? "on" : "off")
                            + ", share gate " + (cfg.dragonShareGate ? "on" : "off")));
                } else {
                    BossTrialConfig.Chapter ch = cfg.chapter(kind);
                    String line = "&7" + kind.displayName() + ": "
                            + (ch.enabled() ? "on" : "off") + ", caps "
                            + (int) ch.caps().perEventDamage() + "/"
                            + (int) ch.caps().explosionPerTick()
                            + ", share gate " + (ch.shareGate() ? "on" : "off")
                            + ", presence " + (ch.presence() ? "on" : "off");
                    ScalingRule rule = cfg.scaling(kind);
                    line += ", rally pool " + (rule.active()
                            ? (int) rule.baseHealth() + " +" + (int) rule.perExtraDamager()
                                    + " per champion (cap " + rule.maxDamagersCounted() + ")"
                            : "off");
                    if (kind == BossKind.WARDEN) {
                        line += ", share height band "
                                + (cfg.wardenShareHeightBand > 0
                                        ? "+/-" + fmt(cfg.wardenShareHeightBand) : "off");
                        line += ", share range band "
                                + (cfg.wardenShareRangeBand > 0
                                        ? fmt(cfg.wardenShareRangeBand) + " blocks" : "off");
                        line += ", give-up "
                                + (cfg.wardenGiveUpWindowMs > 0
                                        ? (cfg.wardenGiveUpWindowMs / 1000)
                                                + "s of no honest fight"
                                        : "off");
                    }
                    if (kind == BossKind.WITHER) {
                        line += ", fought-free check "
                                + (cfg.witherFoughtFreeCheck
                                        ? "on (radius " + fmt(cfg.witherFoughtFreeRadius)
                                                + ", final "
                                                + Math.round(cfg.witherFoughtFreeWindow * 100) + "%)"
                                        : "off");
                    }
                    sender.sendMessage(color(line));
                }
            }
            long now = System.currentTimeMillis();
            boolean anyCooldown = false;
            for (BossKind kind : BossKind.values()) {
                long left = cooldowns.remainingMs(kind.sealId(), now);
                if (left > 0) {
                    anyCooldown = true;
                    sender.sendMessage(color("&c" + kind.displayName()
                            + " seal credit cooldown: &f"
                            + CooldownBook.formatRemaining(left) + " &cleft"));
                }
            }
            if (!anyCooldown) {
                sender.sendMessage(color("&7No seal credit cooldowns running."));
            }
            for (String mid : monuments.monumentIds()) {
                sender.sendMessage(color("&7Monument " + mid + ": &f"
                        + monuments.progress(mid) + " &7of 3 elders counted"));
            }
            sender.sendMessage(color("&7Seal loot: " + (lootEnabled ? "on" : "off")
                    + ", signatures paid: &f"
                    + (signaturePaid.isEmpty() ? "none yet" : signaturePaid.toString())));
            sender.sendMessage(color("&7Open encounters: &f" + encounters.size()));
            sender.sendMessage(color("&7XP band governor: "
                    + (xpGovernorEnabled
                            ? "&aon&7, verdicts in flight: &f"
                                    + xpTokens.totalPending(Bukkit.getCurrentTick())
                            : "&coff&7 (xp-governor.enabled)")));
            sender.sendMessage(color("&7Announcements: " + (!announceEnabled ? "&coff"
                    : announceWindowActive(now) ? "&aOPEN until "
                            + java.time.Instant.ofEpochMilli(announceUntilMs)
                    : "&eclosed (until " + (announceUntilMs > 0
                            ? java.time.Instant.ofEpochMilli(announceUntilMs).toString()
                            : "unset") + ")")));
            if (dragonChapter != null) {
                for (String line : dragonChapter.statusLines()) {
                    sender.sendMessage(color(line));
                }
            }
            return true;
        }
        if (args[0].equalsIgnoreCase("reload")) {
            loadSettings();
            if (dragonChapter != null) {
                dragonChapter.reload(getConfig().getConfigurationSection("dragon"));
            }
            sender.sendMessage(color("&aExcelBossTrial settings reloaded."));
            return true;
        }
        if (args[0].equalsIgnoreCase("reset")) {
            cooldowns.clearAll();
            encounters.clear();
            deathOutcomes.clear();
            attempts.clear();
            attemptsAnnounced.clear();
            placements.clear();
            if (args.length > 1 && args[1].equalsIgnoreCase("all")) {
                monuments.restore(Map.of());
                elderSealApproved = false;
                signaturePaid.clear();
            }
            saveState();
            sender.sendMessage(color("&aExcelBossTrial state reset"
                    + (args.length > 1 && args[1].equalsIgnoreCase("all")
                            ? " (including monuments + elder approval)" : "") + "."));
            return true;
        }
        sender.sendMessage(color("&cUsage: /bosstrial [status|reload|reset [all]]"));
        return true;
    }

    // ------------------------------------------------------------------
    // Small helpers
    // ------------------------------------------------------------------

    /** Package-private for SealCreditHook. */
    BossKind kindOf(LivingEntity entity) {
        return Attribution.kindOf(entity);
    }

    /**
     * Governor damager shape (0.2.4), by entity type alone. Sound
     * because Attribution.dealtPlayer resolves a projectile's shooter
     * or a pet's owner to exactly the credited player whenever it
     * attributes the hit: when dealt is non-null, a Trident damager's
     * shooter IS dealt, a Wolf damager's owner IS dealt, and any
     * other arrow's shooter IS dealt. The registry-attributed shapes
     * (end crystals, respawn anchors, lit beds) never reach the plant
     * site as these types - crystals classify OTHER, and anchor/bed
     * blasts are not EntityDamageByEntityEvent at all.
     */
    private static XpBandPlant.Damager classifyDamager(Entity damager) {
        if (damager instanceof Wolf) {
            return XpBandPlant.Damager.WOLF;
        }
        if (damager instanceof Trident) {
            return XpBandPlant.Damager.TRIDENT;
        }
        if (damager instanceof AbstractArrow) {
            return XpBandPlant.Damager.ARROW;
        }
        if (damager instanceof Player) {
            return XpBandPlant.Damager.MELEE;
        }
        return XpBandPlant.Damager.OTHER;
    }

    private void broadcast(String message) {
        Bukkit.broadcastMessage(color(message));
    }

    private String fill(String template, BossKind kind, DeathOutcome outcome) {
        String out = template;
        out = out.replace("{name}", kind.displayName());
        out = out.replace("{seal}", kind.displayName());
        out = out.replace("{need}", String.valueOf((int) Math.round(cfg.shareThreshold * 100)));
        out = out.replace("{cooldown}", CooldownBook.formatRemaining(cfg.cooldownMs));
        if (outcome != null) {
            out = out.replace("{pct}", String.valueOf((int) Math.floor(outcome.sharePercent)));
            out = out.replace("{count}", String.valueOf(outcome.presentParticipants));
            out = out.replace("{mult}", String.valueOf(outcome.multiplier));
            out = out.replace("{n}", String.valueOf(Math.max(0, outcome.elderProgressNow)));
            out = out.replace("{pool}", String.valueOf((int) Math.round(outcome.poolMax)));
            out = out.replace("{champions}", String.valueOf(outcome.damagerCount));
            out = out.replace("{left}", CooldownBook.formatRemaining(
                    cooldowns.remainingMs(kind.sealId(), System.currentTimeMillis())));
        }
        return out;
    }

    private static String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    private static String fmt(double value) {
        return String.format(java.util.Locale.ROOT, "%.1f", value);
    }
}

// ExcelBossTrial - a Wink Reyes Valley Excel plugin.

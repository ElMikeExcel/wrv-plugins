/* Excel WRV maintenance; existing authorship retained. */ package com.winkreyes.excelbosstrial;

import com.winkreyes.excelbosstrial.api.BossTrialAPI;
import com.winkreyes.excelbosstrial.api.ExcelBossTrialAPI;
import com.winkreyes.excelbosstrial.core.Announcements;
import com.winkreyes.excelbosstrial.core.AtomicStateFile;
import com.winkreyes.excelbosstrial.core.BossKind;
import com.winkreyes.excelbosstrial.core.BossTrialConfig;
import com.winkreyes.excelbosstrial.core.CommandAccess;
import com.winkreyes.excelbosstrial.core.CooldownBook;
import com.winkreyes.excelbosstrial.core.CreditJudge;
import com.winkreyes.excelbosstrial.core.EncounterLedger;
import com.winkreyes.excelbosstrial.core.EncounterRetention;
import com.winkreyes.excelbosstrial.core.FightTracker;
import com.winkreyes.excelbosstrial.core.MilestoneBeats;
import com.winkreyes.excelbosstrial.core.MilestoneSettings;
import com.winkreyes.excelbosstrial.core.TrioEscalation;
import com.winkreyes.excelbosstrial.core.MonumentTracker;
import com.winkreyes.excelbosstrial.core.DenialMessage;
import com.winkreyes.excelbosstrial.core.GrantClaimPlan;
import com.winkreyes.excelbosstrial.core.LaunchSnapshotLog;
import com.winkreyes.excelbosstrial.core.PlacementRegistry;
import com.winkreyes.excelbosstrial.core.ProvenanceLore;
import com.winkreyes.excelbosstrial.core.FeedbackOptOuts;
import com.winkreyes.excelbosstrial.core.FeedbackSettings;
import com.winkreyes.excelbosstrial.core.ProtectEmitSettings;
import com.winkreyes.excelbosstrial.core.ProtectEmitSink;
import com.winkreyes.excelbosstrial.core.ProtectTableShape;
import com.winkreyes.excelbosstrial.core.ScalingRule;
import com.winkreyes.excelbosstrial.core.SealLoot;
import com.winkreyes.excelbosstrial.core.SprinkleSettings;
import com.winkreyes.excelbosstrial.core.TauntBook;
import com.winkreyes.excelbosstrial.core.TauntSettings;
import com.winkreyes.excelbosstrial.sprinkle.SprinkleService;
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
import java.util.Optional;
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
import org.bukkit.event.entity.EntityRegainHealthEvent;
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
        /** 0.3.0: whether the seal already stood broken when this
         *  kill was judged (GuildGate's word, or local credit
         *  records when GuildGate is unverifiable). Drives the
         *  generation-scoped signature gate. */
        final boolean sealWasBroken;
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
            this(kind, verdict, elderProgress, presentParticipants, multiplier,
                    sharePercent, elderProgressNow, heightExcludedHits,
                    poolMax, damagerCount, rangeExcludedHits, cagedWither,
                    presentChampions, false);
        }

        DeathOutcome(BossKind kind, CreditJudge.Verdict verdict, boolean elderProgress,
                     int presentParticipants, int multiplier, double sharePercent,
                     int elderProgressNow, int heightExcludedHits,
                     double poolMax, int damagerCount,
                     int rangeExcludedHits, boolean cagedWither,
                     List<UUID> presentChampions, boolean sealWasBroken) {
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
            this.sealWasBroken = sealWasBroken;
        }
    }

    private BossTrialConfig cfg = BossTrialConfig.defaults();
    private final Map<UUID, EncounterLedger> encounters = new HashMap<>();
    // 0.3.1: the fight record - per-player damage + healing tables
    // snapshotted at every verdict (core/FightTracker), and the
    // elder trio escalation multiplier cached per open encounter.
    private final FightTracker fightTracker = new FightTracker();
    private final Map<UUID, Double> elderTrioMult = new HashMap<>();
    private boolean fightTrackingEnabled = true;
    private int fightRecordRetention = 200;
    private long healClaimWindowMs = 1000L;
    private Set<String> magicHealSpells = Set.of("heal", "superheal");
    private List<Double> elderTrioMultipliers = TrioEscalation.DEFAULTS;
    private boolean magicHealFeedOn = false;
    // 0.3.2: milestone-beat settings per chapter, read adapter-side
    // from each chapter's milestones: block (the fight-tracking
    // precedent). The dragon's entry is the host-side surge gate;
    // the chapter reads its own copy for the lines.
    private final Map<BossKind, MilestoneSettings> milestoneSettings =
            new EnumMap<>(BossKind.class);
    // 0.3.3: the elite sprinkle (ELITE-SPRINKLE-STUDY-20261101).
    // Constructed in onEnable; every EliteMobs call lives inside
    // the service's gateway, and the service's registry is the
    // ONLY place adds are tracked - they never enter the ledger,
    // the share math, the participant sets, or the fight record.
    private SprinkleService sprinkle;
    private int sprinkleLeashTask = -1;
    // 0.3.4: the ExcelProtect emitter. The sink is the seam: it
    // stays NOOP unless the gateway in the protect package binds
    // at enable (Protect present + protect-emit.enabled), so
    // every publish site in this class is a no-op in exactly the
    // boots where 0.3.3 published nothing. Emit-only - nothing in
    // the fight, verdict or reward paths reads anything back.
    private ProtectEmitSink protectEmit = ProtectEmitSink.NOOP;
    private ProtectEmitSettings protectEmitSettings = ProtectEmitSettings.defaults();
    private boolean protectEmitBound;
    // 0.3.6: the damage-feedback display (the feel patch). The
    // display adapter owns every Bukkit bar; it only ever READS
    // the ledger, and it is constructed in onEnable (null in the
    // bare instances the recovery harness drives, so every call
    // site null-guards). The opt-out set persists in state.yml
    // beside the cooldowns; lazy-init for the same reason.
    private FeedbackDisplay feedback;
    private FeedbackSettings feedbackSettings = FeedbackSettings.defaults();
    private final Map<BossKind, FeedbackSettings> feedbackChapterSettings =
            new EnumMap<>(BossKind.class);
    private FeedbackOptOuts feedbackOptOuts;
    // 0.3.7: the TrainingWheels shield seam (TW-PRESSURE-CHECK
    // fix shape 1). Constructed in onEnable; it reads a protected
    // player's factor so the pressure pulse - source-less MAGIC
    // damage TrainingWheels' own listener never sees - can honor
    // it. Fail-soft exactly like the other seams: absent partner,
    // unprotected player or any failed read is a factor of 1.0
    // and BossTrial behaves exactly as 0.3.6 (null in the bare
    // instances the recovery harness drives, so the one call
    // site null-guards).
    private TwShieldSeam twShield;
    // 0.4.0 (the Wither-night fix train, PM-2): verdict-pending
    // state per open encounter - the pool-exhaustion judge has
    // fired and the governed lethal blow is in flight. While an
    // encounter sits here the governor and the record handler
    // pass its damage through untouched (the blow must land
    // unscaled), the bar renders its named FALLING state, and
    // the reconciliation tick retries a blow the world refused.
    private static final class VerdictPending {
        long firedMs;
        int blows;

        VerdictPending(long firedMs) {
            this.firedMs = firedMs;
        }
    }

    private final Map<UUID, VerdictPending> verdictPending = new HashMap<>();
    /** 0.4.0 (PM-12): whether each open encounter's seal already
     *  stood broken when the encounter opened - the grant watch's
     *  baseline. A seal that flips to broken mid-fight while its
     *  encounter is still open and no verdict is running is an
     *  admin grant (guildgate grant fires no event - see
     *  closeEncounterForSeal), and the encounter must close. */
    private final Map<UUID, Boolean> sealBrokenAtOpen = new HashMap<>();
    /** 0.4.0 (PM-8): the last mid-fight snapshot stamp per
     *  encounter (the 60-second record cadence). */
    private final Map<UUID, Long> midSnapshotMs = new HashMap<>();
    /** 0.4.0 (PM-9): the last unattributed-damage console line
     *  per encounter (the throttle stamp). */
    private final Map<UUID, Long> unattributedLogMs = new HashMap<>();
    /** 0.4.0 (PM-1): the last boss-heal console line per
     *  encounter (the throttle stamp). */
    private final Map<UUID, Long> bossHealLogMs = new HashMap<>();
    /** 0.4.0 rebuild (Mike, Nov 8, 2026): the boss taunts -
     *  per-chapter leaf settings read adapter-side like the
     *  milestones; only the wither ships lines. The book is
     *  the per-encounter gate (first eat always speaks, then
     *  at most one line per cooldown); it is fed ONLY from
     *  the regain handler's WITHER-reason heals, so a line
     *  can never fire without a real eat. */
    private final Map<BossKind, TauntSettings> tauntSettings =
            new EnumMap<>(BossKind.class);
    private final TauntBook tauntBook = new TauntBook();
    /** 0.4.0 leaf setting (fight-tracking.unattributed-log-threshold):
     *  unattributed hits at or above this many pool HP earn a
     *  throttled console line naming their cause class. */
    private double unattributedLogThreshold = 10.0;
    /** 0.4.0 (PM-12): the grant watch's first-sighting stamp per
     *  encounter (two consecutive maintenance passes must agree
     *  before a watched encounter closes). */
    private final Map<UUID, Long> grantWatchSince = new HashMap<>();

    private FeedbackOptOuts feedbackOptOuts() {
        if (feedbackOptOuts == null) {
            feedbackOptOuts = new FeedbackOptOuts();
        }
        return feedbackOptOuts;
    }
    private final Map<UUID, DeathOutcome> deathOutcomes = new HashMap<>();
    private final CooldownBook cooldowns = new CooldownBook();
    // 0.3.0 repeat system (SPEC section 2): the per-seal repeat
    // window book (its own book - a repeat window must never read
    // as a denial cooldown anywhere), the generation-scoped
    // signature ledger, BossTrial's own record of credited seals
    // (the fallback when GuildGate's word is unverifiable), and
    // the per-player repeat clear counts behind the flex titles.
    // (Not final: the recovery test harness drives loadState on a
    // bare instance, so these lazy-init there when unconstructed.)
    private CooldownBook repeatWindows = new CooldownBook();
    private Map<String, Integer> signatureGenerations = new HashMap<>();
    private Set<String> creditedSeals = new HashSet<>();
    private Map<String, Map<UUID, Integer>> clearCounts = new HashMap<>();
    private final Set<UUID> intakeLogged = new HashSet<>();
    private int pressurePulseTask = -1;
    private String msgRepeatCreditHeadline;
    private String msgDenyRepeatWindow;
    private RaidStandard raidStandard;
    private final MonumentTracker monuments = new MonumentTracker();
    private final Map<UUID, Long> summonMsgThrottle = new HashMap<>();
    private boolean elderSealApproved;
    private final String recoveryGeneration = UUID.randomUUID().toString();
    private final Map<String, Map<String, String>> recoveryReceipts = new LinkedHashMap<>();
    private boolean recoveryPersistenceLocked;
    private String recoveryDiskToken;
    // No external setter: fault-injection seam used by the offline persistence tests.
    private AtomicStateFile.Probe stateWriteProbe = phase -> {};

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
    /** 0.3.0: the raid repeat draw table (loot.raid) - bonus +
     *  jackpot rows only; the raid has no BossKind of its own. */
    private SealLoot.LootTable raidLootTable =
            new SealLoot.LootTable(List.of(), List.of(), List.of());
    /** 0.3.0: per-chapter flex cosmetic materials (config:
     *  repeat.cosmetics.<chapter>) - data, per the modularity law. */
    private final Map<BossKind, String> repeatCosmetics = new EnumMap<>(BossKind.class);
    /** The Lost & Found voucher row ships DISABLED (SPEC 4.4):
     *  armed only when config enables it AND a redeem path exists.
     *  ExcelDeathRecord 0.3.0 exposes no redeem API, so the load
     *  guard below keeps this false and logs why. */
    private boolean lostFoundVoucherArmed;
    private int dragonBreathBase = SealLoot.DRAGON_BREATH_BASE;
    private int dragonBreathPer = SealLoot.DRAGON_BREATH_PER_CHAMPION;
    private int dragonBreathCapN = SealLoot.DRAGON_BREATH_CAP;
    private int dragonCrystals = SealLoot.DRAGON_CRYSTALS;
    private final Set<String> signaturePaid = new HashSet<>();
    private final Random lootRng = new Random();

    // ------------------------------------------------------------------
    // Lifecycle
    // ------------------------------------------------------------------

    private TeamClaims teamClaims;
    private int teamSampleTask = -1;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        // 0.3.3: the elite sprinkle service (configured by
        // loadSettings below; its leash task is scheduled with
        // the other repeating tasks). The participants lookup is
        // the encounter ledger's own set - read-only here, used
        // solely to retarget a leashed add onto a real fighter.
        sprinkle = new SprinkleService(this, id -> {
            EncounterLedger l = encounters.get(id);
            return l == null ? Set.of() : l.participants();
        });
        loadSettings();
        // 0.3.7: the TrainingWheels shield seam for the pressure
        // pulse. Construction never touches the partner plugin;
        // the reflective resolution happens at the boot summary's
        // describeState() call below (and lazily on first use).
        twShield = new TwShieldSeam(getLogger());
        loadState();
        try { teamClaims = new TeamClaims(this, signaturePaid); }
        catch (java.io.IOException failure) {
            getLogger().severe("Team rewards unavailable; combat enforcement remains active, direct signature fallback disabled: " + failure.getMessage());
        }
        // 0.3.6: the damage-feedback display (feel patch). It
        // reads the ledger only; its dragon phase view prices
        // the dragon title per phase off the chapter's own
        // fraction basis (health over the phase's entity max,
        // the same ratio the chapter's beats judge).
        feedback = new FeedbackDisplay(getLogger(), feedbackOptOuts(), dragonUuid -> {
            if (dragonChapter == null) {
                return null;
            }
            double pool = dragonChapter.currentPhasePool(dragonUuid);
            if (pool <= 0) {
                return null;
            }
            org.bukkit.entity.Entity ent = Bukkit.getEntity(dragonUuid);
            if (!(ent instanceof EnderDragon dragon)) {
                return null;
            }
            AttributeInstance attr = dragon.getAttribute(Attribute.MAX_HEALTH);
            double entityMax = attr != null ? attr.getValue() : 0;
            if (entityMax <= 0) {
                return null;
            }
            double fraction = dragon.getHealth() / entityMax;
            if (fraction < 0) {
                fraction = 0;
            } else if (fraction > 1) {
                fraction = 1;
            }
            return new double[] {pool, pool * (1.0 - fraction)};
        }, bossId -> verdictPending.containsKey(bossId));
        pushFeedbackSettings();
        teamSampleTask = Bukkit.getScheduler().scheduleSyncRepeatingTask(this, () -> {
            for (UUID id : new ArrayList<>(encounters.keySet())) {
                org.bukkit.entity.Entity entity = Bukkit.getEntity(id);
                if (entity instanceof LivingEntity boss && !boss.isDead() && boss.isValid()) {
                    BossKind kind = Attribution.kindOf(boss);
                    if (kind != null) observeTeam(boss, kind);
                }
            }
        }, 20L, 20L);
        cooldowns.setDurationMs(cfg.cooldownMs);
        BossTrialAPI.bind(cooldowns);
        BossTrialAPI.bindFeed(xpTokens);
        BossTrialAPI.bindRepeat(repeatWindows);
        // 0.3.7: the outgoing-scale API (TW-PRESSURE-CHECK fix
        // shape 2, BossTrial half) - ExcelTrainingWheels 0.1.3's
        // compensation seam reads the same number the pressure
        // handler applies, from the shared extraction below.
        ExcelBossTrialAPI.bind(this::currentOutgoingScale);
        // 0.4.0 (PM-12): the seal-grant encounter close - the
        // public seam a seal-state writer (GuildGate's grant, an
        // admin tool) calls so a granted seal's live encounter
        // closes instead of standing as a zombie.
        ExcelBossTrialAPI.bindSealClose((sealId, cause) -> {
            BossKind kind = BossKind.fromSealId(sealId);
            return kind != null && closeEncounterForSealInternal(kind,
                    cause != null ? cause : "seal granted");
        });
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
        // 0.3.4: the ExcelProtect emitter adapter. The gateway is
        // the only class linking Protect's API and is constructed
        // only when the ExcelProtect plugin is installed - with
        // Protect absent (or protect-emit.enabled off) the sink
        // stays NOOP and this plugin behaves exactly as 0.3.3.
        if (protectEmitSettings.enabled()) {
            org.bukkit.plugin.Plugin protectPlugin =
                    Bukkit.getPluginManager().getPlugin("ExcelProtect");
            if (protectPlugin != null) {
                try {
                    protectEmit = new com.winkreyes.excelbosstrial.protect
                            .ProtectEmitGateway(getLogger());
                    protectEmitBound = true;
                } catch (Throwable bindFailure) {
                    getLogger().warning("ExcelProtect emit: OFF (adapter failed to bind: "
                            + bindFailure + ") - BossTrial runs exactly as before; "
                            + "fight records stay in fights/.");
                }
            }
        }
        // 0.3.1: the Magic heal-spell feed (healing-DONE
        // attribution for the fight record). Its listener is the
        // only class linking Magic's API and is registered only
        // when Magic is present, inside a guard: any linkage
        // failure leaves the feed off - healing received is still
        // recorded from Bukkit's regain events; only spell
        // attribution is lost (fail-soft, house build style).
        if (Bukkit.getPluginManager().getPlugin("Magic") != null) {
            try {
                Bukkit.getPluginManager().registerEvents(
                        new MagicFightHealListener(this), this);
                magicHealFeedOn = true;
            } catch (Throwable feedFailure) {
                getLogger().warning("Magic heal-spell feed unavailable "
                        + "(fight record keeps healing received + unattributed): "
                        + feedFailure);
            }
        }
        if (cfg.enabled && dragonEnabled()) {
            dragonChapter = new DragonChapter(this,
                    new File(getDataFolder(), "dragon"),
                    getConfig().getConfigurationSection("dragon"));
            dragonChapter.enable();
            // 0.3.2: the dragon chapter fires its own per-phase
            // milestone beats and hands each surge here; the surge
            // queues on the battle's encounter ledger (which this
            // plugin owns) and rides the pressure pulse task.
            dragonChapter.setMilestoneSurgeSink(dragonUuid -> {
                EncounterLedger ledger = encounters.get(dragonUuid);
                MilestoneSettings ms = milestoneSettings.get(BossKind.ENDER_DRAGON);
                if (ledger == null || ms == null || !ms.enabled()
                        || ms.surgeMultiplier() <= 0) {
                    return;
                }
                BossTrialConfig.Pressure pressure = cfg.pressure(BossKind.ENDER_DRAGON);
                if (pressure.enabled() && pressure.pulseDamage() > 0) {
                    ledger.queueSurge();
                }
            });
            // 0.3.3: the dragon's sprinkle sink - the chapter
            // calls it with the phase-2 milestone mask and the
            // fountain anchor; the roll itself is the host's,
            // against the dragon chapter's sprinkle block.
            dragonChapter.setSprinkleSink((dragonUuid, stampMask, fountain) -> {
                if (sprinkle == null) {
                    return;
                }
                Entity ent = Bukkit.getEntity(dragonUuid);
                if (!(ent instanceof LivingEntity dragonBoss)) {
                    return;
                }
                for (int i = 0; i < MilestoneBeats.THRESHOLDS.length; i++) {
                    if ((stampMask & (1 << i)) != 0) {
                        sprinkle.onMilestone(BossKind.ENDER_DRAGON, dragonBoss, i, fountain);
                    }
                }
            });
            dragonChapter.setSprinkleArmed(sprinkle != null
                    && sprinkle.armed(BossKind.ENDER_DRAGON));
        }
        // 0.3.0 raid companion: the BossTrial-side adapter pays the
        // raid signature + repeat slate off GuildGate's events.
        // Registered only when GuildGate is present AND its 0.4.0
        // event classes load - any older GuildGate leaves the raid
        // exactly as it was (fail-soft house style).
        if (RaidRewardAdapter.register(this)) {
            getLogger().info("Raid repeat adapter: ON (GuildGate raid events pay through BossTrial claims).");
        }
        raidStandard = RaidStandard.load(this);
        if (cfg.repeatEnabled) {
            getLogger().info("Repeat layer: ON (window "
                    + (cfg.repeatResetHours) + "h base, signature generations "
                    + (cfg.repeatSignatureEnabled ? "on" : "off") + ", cash $"
                    + (int) cfg.repeatCashPerChampion + "/champion, jackpot +"
                    + cfg.repeatJackpotExtraRolls + " rolls).");
            bootElderGenerationFlip();
        }
        pressurePulseTask = Bukkit.getScheduler().scheduleSyncRepeatingTask(
                this, this::pressurePulseTick, 20L, 20L);
        maintenanceTask = Bukkit.getScheduler().scheduleSyncRepeatingTask(
                this, this::maintenance, 600L, 600L);
        if (sprinkle != null) {
            sprinkleLeashTask = Bukkit.getScheduler().scheduleSyncRepeatingTask(
                    this, sprinkle::leashTick, 40L, 40L);
        }
        getLogger().info("ExcelBossTrial " + getDescription().getVersion()
                + " awake: " + (cfg.enabled ? "ENABLED" : "disabled by config")
                + " (share gate " + (int) (cfg.shareThreshold * 100) + "% of max HP, presence "
                + (int) cfg.presenceRange + " blocks, cooldown "
                + CooldownBook.formatRemaining(cfg.cooldownMs) + ").");
        getLogger().info("XP band governor feed: "
                + (xpGovernorEnabled
                        ? "ON (governed hits plant band verdicts; a listener such as ExcelRewardGuard may scale their mcMMO XP - out-of-band pays 25% there)"
                        : "OFF by config (xp-governor.enabled) - no verdicts planted, every gain pays full"));
        getLogger().info("Fight tracking: " + (fightTrackingEnabled
                ? "ON (per-player damage + healing records land in fights/ at every "
                        + "verdict and elder progress point; Magic heal-spell feed "
                        + (magicHealFeedOn ? "ON" : "unavailable - spell healing stays "
                        + "in the unattributed bucket") + ")"
                : "OFF by config (fight-tracking.enabled)"));
        getLogger().info("Milestone beats: " + milestoneSummary());
        getLogger().info("Damage feedback: " + feedbackSummary());
        if (sprinkle != null) {
            getLogger().info(sprinkle.bootSummary());
        }
        getLogger().info(tauntSummary());
        getLogger().info(protectEmitBootLine());
        getLogger().info(twShield.describeState());
    }

    /** The awake-line summary for the 0.4.0 boss taunts: which
     *  chapters can speak, with how many lines and what gap. */
    private String tauntSummary() {
        StringBuilder sb = new StringBuilder("Boss taunts: ");
        boolean first = true;
        for (BossKind kind : BossKind.values()) {
            if (!first) {
                sb.append(", ");
            }
            first = false;
            TauntSettings s = tauntSettings.get(kind);
            sb.append(kind.displayName()).append(" ");
            if (s != null && s.enabled()) {
                sb.append("ARMED (").append(s.lines().size()).append(" lines, ")
                        .append(s.cooldownSeconds()).append("s cooldown)");
            } else {
                sb.append("silent");
            }
        }
        return sb.append(".").toString();
    }

    /** The awake-line summary for the 0.3.6 damage feedback:
     *  ON when at least one chapter can show pool numbers or
     *  action-bar totals, else OFF. */
    private String feedbackSummary() {
        boolean numbers = false;
        boolean totals = false;
        for (BossKind kind : BossKind.values()) {
            FeedbackSettings s = feedbackChapterSettings.getOrDefault(kind, feedbackSettings);
            numbers |= s.poolNumbersOn();
            totals |= s.actionBarOn();
        }
        if (!numbers && !totals) {
            return "OFF by config (feedback.pool-numbers off and "
                    + "feedback.damage-total off for every chapter)";
        }
        return "ON (boss bar title carries the true pool numbers"
                + (numbers ? "" : " - off")
                + "; per-player damage totals "
                + (totals ? "on the action bar" : "off")
                + "; /bosstrial feedback off opts a player out; "
                + "display only - fight math untouched)";
    }

    /** Push the parsed feedback settings into the display
     *  adapter (loadSettings runs before the adapter exists at
     *  enable and again on every reload). */
    private void pushFeedbackSettings() {
        if (feedback != null) {
            feedback.configure(feedbackSettings, feedbackChapterSettings);
        }
    }

    /** The 0.3.4 adapter bind line - exactly one shape per state. */
    private String protectEmitBootLine() {
        if (!protectEmitSettings.enabled()) {
            return "ExcelProtect emit: OFF by config (protect-emit.enabled)"
                    + " - nothing is published; fight records stay in fights/.";
        }
        if (!protectEmitBound) {
            return "ExcelProtect emit: OFF (ExcelProtect not installed"
                    + " - fight records stay in fights/ only; BossTrial runs exactly as before)";
        }
        return "ExcelProtect emit: ON (adapter bound; fight tables "
                + (protectEmitSettings.fightTables() ? "ON" : "OFF")
                + ", state changes "
                + (protectEmitSettings.stateChanges() ? "ON" : "OFF")
                + "; verdicts and boss state publish to ExcelProtect)";
    }

    /** The awake-line summary for the 0.3.2 milestone beats: ON
     *  when at least one chapter can speak or surge, else OFF. */
    private String milestoneSummary() {
        boolean anyOn = false;
        for (BossKind kind : BossKind.values()) {
            MilestoneSettings ms = milestoneSettings.get(kind);
            if (ms != null && ms.enabled()
                    && (ms.anyText() || ms.surgeMultiplier() > 0)) {
                anyOn = true;
            }
        }
        return anyOn
                ? "ON (75/50/25% of the pool remaining: a broadcast line + one pulse "
                        + "surge per threshold, once per encounter - the warden's lines are "
                        + "its progress display; per-chapter keys under <chapter>.milestones)"
                : "OFF by config (every chapter's milestones are disabled or silenced)";
    }

    @Override
    public void onDisable() {
        // 0.4.0 (PM-8): a server stop is a RECORDED close - every
        // open encounter's fight record is written before anything
        // else tears down, so a stopped fight still leaves its
        // per-player table (the 60-second mid-fight snapshots are
        // the backstop for a crash where this never runs).
        for (Map.Entry<UUID, EncounterLedger> openEncounter
                : new ArrayList<>(encounters.entrySet())) {
            snapshotCloseRecord(openEncounter.getValue().kind(), openEncounter.getKey(),
                    openEncounter.getValue(), LABEL_SERVER_STOP, null, List.of(), null);
        }
        // 0.3.6: no feedback bar outlives the plugin (and every
        // suppressed vanilla bar is restored with its encounter).
        if (feedback != null) {
            feedback.closeAll();
        }
        if (sprinkleLeashTask >= 0) { Bukkit.getScheduler().cancelTask(sprinkleLeashTask); sprinkleLeashTask = -1; }
        // 0.3.3: no sprinkle add outlives the plugin - every open
        // encounter's elites are removed here, before state saves.
        if (sprinkle != null) {
            sprinkle.closeAll("plugin disable");
        }
        if (pressurePulseTask >= 0) { Bukkit.getScheduler().cancelTask(pressurePulseTask); pressurePulseTask = -1; }
        BossTrialAPI.unbindRepeat();
        if (teamSampleTask >= 0) { Bukkit.getScheduler().cancelTask(teamSampleTask); teamSampleTask = -1; }
        if (teamClaims != null) { try { teamClaims.close(); } catch (java.io.IOException e) { getLogger().severe("Team reward close failed: " + e.getMessage()); } teamClaims = null; }
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
        ExcelBossTrialAPI.unbind();
        ExcelBossTrialAPI.unbindSealClose();
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
                "dragon.presence", "dragon.share-gate",
                // 0.3.0: intake + attrition pressure + the repeat layer.
                "intake.entity-cap",
                "wither.pressure.enabled", "wither.pressure.outgoing-multiplier",
                "wither.pressure.pulse-damage", "wither.pressure.pulse-interval-seconds",
                "wither.pressure.pulse-hot-seconds", "wither.pressure.ramp-per-minute",
                "wither.pressure.ramp-max", "wither.pressure.pulse-respect-training-wheels",
                "elder-guardian.pressure.enabled", "elder-guardian.pressure.outgoing-multiplier",
                "elder-guardian.pressure.pulse-damage", "elder-guardian.pressure.pulse-interval-seconds",
                "elder-guardian.pressure.pulse-hot-seconds", "elder-guardian.pressure.ramp-per-minute",
                "elder-guardian.pressure.ramp-max", "elder-guardian.pressure.pulse-respect-training-wheels",
                // 0.3.5: the elder chapter's spike-reflect scale.
                "elder-guardian.reflect.multiplier",
                "warden.pressure.enabled", "warden.pressure.outgoing-multiplier",
                "warden.pressure.pulse-damage", "warden.pressure.pulse-interval-seconds",
                "warden.pressure.pulse-hot-seconds", "warden.pressure.ramp-per-minute",
                "warden.pressure.ramp-max", "warden.pressure.pulse-respect-training-wheels",
                "dragon.pressure.enabled", "dragon.pressure.outgoing-multiplier",
                "dragon.pressure.pulse-damage", "dragon.pressure.pulse-interval-seconds",
                "dragon.pressure.pulse-hot-seconds", "dragon.pressure.ramp-per-minute",
                "dragon.pressure.ramp-max", "dragon.pressure.pulse-respect-training-wheels",
                "repeat.enabled", "repeat.reset-hours", "repeat.signature-enabled",
                "repeat.announce", "repeat.rewards.jackpot.extra-rolls",
                "repeat.rewards.cash-per-champion", "repeat.rewards.accelerants.mcmmo-xp",
                "repeat.rewards.accelerants.magiclevel-xp",
                "repeat.rewards.war-chest.claimfly-hours",
                "repeat.rewards.war-chest.death-protection-premium",
                "repeat.rewards.war-chest.lost-and-found-voucher-enabled",
                "repeat.rewards.healer-recognition.enabled",
                "elder-guardian.repeat.respawn-hours",
                "wither.repeat.reset-hours", "elder-guardian.repeat.reset-hours",
                "warden.repeat.reset-hours", "dragon.repeat.reset-hours"}) {
            if (root.contains(key)) {
                flat.put(key, root.get(key));
            }
        }
        // The flex-title table is a list-of-maps, not a flat key:
        // hand it to the parser in the shape titlesFrom expects.
        if (root.contains("repeat.titles")) {
            flat.put("repeat.titles", root.getMapList("repeat.titles"));
        }
        cfg = BossTrialConfig.fromMap(flat);
        cooldowns.setDurationMs(cfg.cooldownMs);
        // 0.3.1 leaf settings, read adapter-side like the raid
        // cosmetics: no core rule consumes them, so the canonical
        // config record stays untouched. Every value is a config
        // key with its settled default (the adjustability law).
        fightTrackingEnabled = getConfig().getBoolean("fight-tracking.enabled", true);
        fightRecordRetention = Math.max(0,
                getConfig().getInt("fight-tracking.record-retention", 200));
        healClaimWindowMs = Math.max(0L,
                getConfig().getLong("fight-tracking.heal-claim-window-ms", 1000L));
        unattributedLogThreshold = Math.max(0.0,
                getConfig().getDouble("fight-tracking.unattributed-log-threshold", 10.0));
        Set<String> healSpells = new HashSet<>();
        for (String s : getConfig().getStringList("fight-tracking.magic-heal-spells")) {
            String base = baseSpellKey(s);
            if (!base.isEmpty()) {
                healSpells.add(base);
            }
        }
        magicHealSpells = healSpells.isEmpty()
                ? Set.of("heal", "superheal") : Set.copyOf(healSpells);
        elderTrioMultipliers = TrioEscalation.sanitized(getConfig().getDoubleList(
                "elder-guardian.trio.position-multipliers"));
        // 0.3.2 leaf settings, adapter-side like fight-tracking:
        // each chapter's milestones: block (enable, surge
        // multiplier, the three lines). Missing keys keep the
        // settled defaults; a present-but-empty line silences
        // that threshold's line only.
        for (BossKind kind : BossKind.values()) {
            MilestoneSettings d = MilestoneSettings.defaults(kind);
            String base = chapterSection(kind) + ".milestones.";
            milestoneSettings.put(kind, MilestoneSettings.sanitized(
                    getConfig().getBoolean(base + "enabled", d.enabled()),
                    getConfig().getDouble(base + "surge-multiplier", d.surgeMultiplier()),
                    milestoneText(base + "text-75", d.text75()),
                    milestoneText(base + "text-50", d.text50()),
                    milestoneText(base + "text-25", d.text25())));
        }
        // 0.3.3 leaf settings, adapter-side like milestones: the
        // elite sprinkle. Master keys under sprinkle:, per-chapter
        // blocks under <chapter>.sprinkle. Every chapter defaults
        // to DISABLED (SprinkleSettings.defaults) - arming is a
        // config edit, never a jar default.
        if (sprinkle != null) {
            Map<BossKind, SprinkleSettings> sprinkleChapters = new EnumMap<>(BossKind.class);
            for (BossKind kind : BossKind.values()) {
                SprinkleSettings d = SprinkleSettings.defaults(kind);
                String base = chapterSection(kind) + ".sprinkle.";
                // 0.4.0 rebuild (Mike, Nov 8, 2026): sanitizing
                // is LOUD - every value a clamp or fail-dark
                // replacement actually changes is logged as a
                // boot WARNING naming the setting, the
                // configured value, and the applied value.
                // Silence means the config loaded untouched.
                List<String> sprinkleAdjustments = new ArrayList<>();
                sprinkleChapters.put(kind, SprinkleSettings.sanitized(
                        getConfig().getBoolean(base + "enabled", d.enabled()),
                        getConfig().getDouble(base + "chance", d.chance()),
                        getConfig().getInt(base + "count", d.count()),
                        getConfig().getIntegerList(base + "levels"),
                        getConfig().getInt(base + "max-alive", d.maxAlive()),
                        getConfig().getInt(base + "max-per-fight", d.maxPerFight()),
                        getConfig().getDouble(base + "leash-radius", d.leashRadius()),
                        getConfig().getStringList(base + "types"),
                        getConfig().getInt(base + "cadence-seconds", d.cadenceSeconds()),
                        sprinkleWaves(base),
                        d, sprinkleAdjustments));
                for (String adjustment : sprinkleAdjustments) {
                    getLogger().warning("sprinkle config [" + chapterSection(kind)
                            + "] " + adjustment);
                }
            }
            sprinkle.configure(
                    getConfig().getBoolean("sprinkle.enabled", true),
                    getConfig().getDoubleList("sprinkle.stamp-multipliers"),
                    getConfig().getDouble("sprinkle.chance-ceiling",
                            SprinkleSettings.DEFAULT_CHANCE_CEILING),
                    getConfig().getString("sprinkle.announce-text",
                            SprinkleSettings.DEFAULT_ANNOUNCE_TEXT),
                    sprinkleChapters);
            // The dragon chapter mirrors the armed flag so its
            // per-phase beat tracker advances for the sprinkle
            // exactly when armed (reload path; the enable path
            // pushes it again at sink wiring).
            if (dragonChapter != null) {
                dragonChapter.setSprinkleArmed(sprinkle.armed(BossKind.ENDER_DRAGON));
            }
        }
        // 0.4.0 rebuild leaf settings, adapter-side like the
        // milestones: the boss taunts (<chapter>.taunts and
        // <chapter>.taunt-cooldown-seconds). An ABSENT list
        // keeps the chapter default (the wither's ten lines;
        // silence elsewhere); a present-but-empty list is an
        // honored silence. Only eats speak - see maybeTaunt.
        for (BossKind kind : BossKind.values()) {
            TauntSettings d = TauntSettings.defaults(kind);
            String base = chapterSection(kind) + ".";
            List<String> tauntLines = getConfig().isSet(base + "taunts")
                    ? getConfig().getStringList(base + "taunts") : null;
            tauntSettings.put(kind, TauntSettings.sanitized(tauntLines,
                    getConfig().getInt(base + "taunt-cooldown-seconds",
                            d.cooldownSeconds()), d));
        }
        // 0.3.4 leaf settings, adapter-side like fight-tracking:
        // the ExcelProtect emitter switches (protect-emit.*).
        // Every key defaults ON; no fight rule reads them.
        protectEmitSettings = ProtectEmitSettings.sanitized(
                getConfig().getBoolean("protect-emit.enabled", true),
                getConfig().getBoolean("protect-emit.fight-tables", true),
                getConfig().getBoolean("protect-emit.state-changes", true));
        // 0.3.6 leaf settings, adapter-side like protect-emit:
        // the damage-feedback display (feedback.* globals plus
        // per-chapter <chapter>.feedback overrides). Display
        // only - no fight rule reads them.
        feedbackSettings = FeedbackSettings.sanitized(
                FeedbackSettings.poolNumbersValue(getConfig().get("feedback.pool-numbers")),
                FeedbackSettings.damageTotalModeValue(getConfig().get("feedback.damage-total")),
                getConfig().getString("feedback.title-format"),
                getConfig().getString("feedback.damage-total-format"),
                getConfig().getInt("feedback.viewer-range",
                        FeedbackSettings.DEFAULT_VIEWER_RANGE),
                getConfig().getLong("feedback.damage-total-min-interval-ms",
                        FeedbackSettings.DEFAULT_MIN_INTERVAL_MS),
                getConfig().getLong("feedback.title-tick-ms",
                        FeedbackSettings.DEFAULT_TITLE_TICK_MS),
                getConfig().getString("feedback.exhausted-format"));
        feedbackChapterSettings.clear();
        for (BossKind kind : BossKind.values()) {
            String base = chapterSection(kind) + ".feedback.";
            Boolean poolOverride = getConfig().contains(base + "pool-numbers")
                    ? FeedbackSettings.poolNumbersValue(getConfig().get(base + "pool-numbers"))
                    : null;
            String modeOverride = getConfig().contains(base + "damage-total")
                    ? FeedbackSettings.damageTotalModeValue(getConfig().get(base + "damage-total"))
                    : null;
            feedbackChapterSettings.put(kind,
                    feedbackSettings.withOverrides(poolOverride, modeOverride));
        }
        pushFeedbackSettings();
        loadLootSettings();
        loadRaidSettings();
        msgRepeatCreditHeadline = getConfig().getString("messages.repeat-credit-headline",
                "&a{name} falls again - a repeat clear! The seal stands; the spoils are yours.");
        msgDenyRepeatWindow = getConfig().getString("messages.deny-repeat-window",
                "&e{name} has fallen, but its repeat spoils are still resting: "
                        + "they return in {left}. The seal stands.");
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
    /** The config section a chapter's leaf keys live under. */
    private static String chapterSection(BossKind kind) {
        return switch (kind) {
            case WITHER -> "wither";
            case ELDER_GUARDIAN -> "elder-guardian";
            case WARDEN -> "warden";
            case ENDER_DRAGON -> "dragon";
        };
    }

    /** A milestone line from config: absent keeps the default; a
     *  present-but-empty value is honored (that line is off). */
    /** Parse a chapter's sprinkle.waves list (0.4.0, PM-6):
     *  each entry is a map with types (a list), count and
     *  placement (ring|aerial). Malformed entries are skipped
     *  here and sanitized in SprinkleSettings; an absent or
     *  empty list returns null so the chapter defaults stand. */
    private List<SprinkleSettings.Wave> sprinkleWaves(String base) {
        List<Map<?, ?>> raw = getConfig().getMapList(base + "waves");
        if (raw == null || raw.isEmpty()) {
            return null;
        }
        List<SprinkleSettings.Wave> waves = new ArrayList<>();
        for (Map<?, ?> entry : raw) {
            if (entry == null) {
                continue;
            }
            List<String> types = new ArrayList<>();
            Object t = entry.get("types");
            if (t instanceof List<?> list) {
                for (Object o : list) {
                    if (o != null) {
                        types.add(o.toString());
                    }
                }
            }
            int count = 1;
            Object c = entry.get("count");
            if (c instanceof Number num) {
                count = num.intValue();
            }
            Object pl = entry.get("placement");
            waves.add(new SprinkleSettings.Wave(types, count,
                    pl != null ? pl.toString() : "ring"));
        }
        return waves;
    }

    private String milestoneText(String key, String fallback) {
        String v = getConfig().getString(key, null);
        return v == null ? fallback : v;
    }

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

    /** 0.3.0 raid + repeat surface settings: the raid draw table,
     *  the flex cosmetics, and the voucher load-guard. */
    private void loadRaidSettings() {
        ConfigurationSection loot = getConfig().getConfigurationSection("loot");
        ConfigurationSection raid = loot == null ? null : loot.getConfigurationSection("raid");
        List<SealLoot.BonusEntry> bonus = new ArrayList<>();
        List<SealLoot.BonusEntry> jackpot = new ArrayList<>();
        if (raid != null) {
            for (Map<?, ?> entry : raid.getMapList("bonus")) {
                SealLoot.BonusEntry row = parseBonusEntry("Raid", entry);
                if (row != null) {
                    bonus.add(row);
                }
            }
            for (Map<?, ?> entry : raid.getMapList("jackpot")) {
                SealLoot.BonusEntry row = parseBonusEntry("Raid", entry);
                if (row != null) {
                    jackpot.add(row);
                }
            }
        }
        raidLootTable = new SealLoot.LootTable(List.of(), bonus, jackpot);
        repeatCosmetics.clear();
        ConfigurationSection cosmetics = getConfig().getConfigurationSection("repeat.cosmetics");
        for (BossKind kind : BossKind.values()) {
            String mat = cosmetics == null ? null : cosmetics.getString(lootKey(kind));
            if (mat != null && Material.matchMaterial(mat) != null) {
                repeatCosmetics.put(kind, mat);
            }
        }
        // Load-guard (SPEC 4.4): the voucher never arms without a
        // redeem path in ExcelDeathRecord - enabled-in-config with
        // no path logs once and stays off.
        lostFoundVoucherArmed = false;
        if (cfg.repeatLostFoundVoucherEnabled) {
            getLogger().warning("Repeat rewards: lost-and-found-voucher-enabled is true, but "
                    + "ExcelDeathRecord exposes no voucher redeem path - the voucher row "
                    + "stays DISABLED until one exists (SPEC 4.4 load-guard).");
        }
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
        List<SealLoot.BonusEntry> jackpot = dflt.jackpot();
        if (sec.contains("jackpot")) {
            jackpot = new ArrayList<>();
            for (Map<?, ?> entry : sec.getMapList("jackpot")) {
                SealLoot.BonusEntry row = parseBonusEntry(kind, entry);
                if (row != null) {
                    jackpot.add(row);
                }
            }
        }
        return new SealLoot.LootTable(signature, bonus, jackpot);
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
        return parseBonusEntry(kind.displayName(), entry);
    }

    private SealLoot.BonusEntry parseBonusEntry(String label, Map<?, ?> entry) {
        Object mat = entry.get("material");
        if (!(mat instanceof String name) || Material.matchMaterial(name) == null) {
            getLogger().warning("Seal loot [" + label
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
            recoveryDiskToken="MISSING";
            return;
        }
        YamlConfiguration y = new YamlConfiguration();
        try {
            byte[] captured=java.nio.file.Files.readAllBytes(f.toPath());
            recoveryDiskToken=recoveryBytesHash(captured);
            y.loadFromString(new String(captured,java.nio.charset.StandardCharsets.UTF_8));
        }
        catch (Exception invalid) {
            recoveryPersistenceLocked=true;
            getLogger().severe("Could not parse state.yml; state writes locked until restart/inspection: "+invalid.getMessage());
            return;
        }
        recoveryReceipts.clear();
        recoveryPersistenceLocked = false;
        Object receiptsRaw = y.get("recovery.receipts");
        if (y.contains("recovery.protocol")) {
            Object protocol = y.get("recovery.protocol");
            try {
                if (!(protocol instanceof Number)
                        || new java.math.BigDecimal(protocol.toString()).compareTo(java.math.BigDecimal.ONE) != 0)
                    recoveryPersistenceLocked = true;
            } catch (NumberFormatException invalidProtocol) { recoveryPersistenceLocked = true; }
        }
        if (receiptsRaw != null && !(receiptsRaw instanceof List<?>)) recoveryPersistenceLocked = true;
        for (Map<?, ?> row : y.getMapList("recovery.receipts")) {
            try {
                Map<String,String> rec = new LinkedHashMap<>();
                for (String key : List.of("id", "digest", "seal", "afterToken")) {
                    if (!(row.get(key) instanceof String value)) throw new IllegalArgumentException();
                    rec.put(key,value);
                }
                if (!validRecoveryId(rec.get("id")) || !validRecoveryHash(rec.get("digest"))
                        || !validRecoveryHash(rec.get("afterToken"))
                        || BossKind.fromSealId(rec.get("seal")) == null
                        || recoveryReceipts.putIfAbsent(rec.get("id"),rec) != null) throw new IllegalArgumentException();
            } catch (Exception malformed) { recoveryPersistenceLocked = true; }
        }
        if (receiptsRaw instanceof List<?> all && all.size()!=recoveryReceipts.size()) recoveryPersistenceLocked=true;
        if (recoveryPersistenceLocked) getLogger().severe("Recovery receipt state is invalid; recovery and state writes locked until repaired and plugin restarted.");
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
        // 0.3.0 sections. Each restores defensively: a malformed
        // row degrades that section only, never the whole state.
        if (repeatWindows == null) {
            repeatWindows = new CooldownBook();
        }
        if (signatureGenerations == null) {
            signatureGenerations = new HashMap<>();
        }
        if (creditedSeals == null) {
            creditedSeals = new HashSet<>();
        }
        if (clearCounts == null) {
            clearCounts = new HashMap<>();
        }
        ConfigurationSection rw = y.getConfigurationSection("repeat-windows");
        if (rw != null) {
            repeatWindows.restore(rw.getValues(false));
        }
        signatureGenerations.clear();
        ConfigurationSection sg = y.getConfigurationSection("signature-generations");
        if (sg != null) {
            for (String sealId : sg.getKeys(false)) {
                signatureGenerations.put(sealId, sg.getInt(sealId, 0));
            }
        }
        // Legacy migration (SPEC 2.4): a pre-0.3.0 signature-paid
        // flag IS generation 0 paid - recorded, never re-payable.
        for (String sealId : signaturePaid) {
            signatureGenerations.putIfAbsent(sealId, 0);
        }
        creditedSeals.clear();
        creditedSeals.addAll(y.getStringList("credited-seals"));
        // A seal with any paid signature generation was credited by
        // definition; fold it into the local credit record so the
        // repeat judge's fallback never needs the legacy flags.
        creditedSeals.addAll(signatureGenerations.keySet());
        if (elderSealApproved) {
            creditedSeals.add(BossKind.ELDER_GUARDIAN.sealId());
        }
        clearCounts.clear();
        ConfigurationSection cc = y.getConfigurationSection("repeat-clears");
        if (cc != null) {
            for (String sealId : cc.getKeys(false)) {
                ConfigurationSection per = cc.getConfigurationSection(sealId);
                if (per == null) {
                    continue;
                }
                Map<UUID, Integer> counts = new HashMap<>();
                for (String raw : per.getKeys(false)) {
                    try {
                        counts.put(UUID.fromString(raw), per.getInt(raw, 0));
                    } catch (IllegalArgumentException ignored) {
                        // a malformed uuid row degrades to skipped
                    }
                }
                clearCounts.put(sealId, counts);
            }
        }
        // 0.3.6: the damage-feedback opt-outs (restored
        // defensively like every 0.3.0 section).
        feedbackOptOuts().restore(y.getStringList("feedback-opt-outs"));
        getLogger().info("State loaded: cooldowns " + cooldowns.snapshot().keySet()
                + ", monuments " + monuments.monumentIds().size()
                + ", elder seal " + (elderSealApproved ? "approved" : "open")
                + ", signatures paid " + signaturePaid
                + ", signature generations " + signatureGenerations
                + ", repeat windows " + repeatWindows.snapshot().keySet()
                + ", credited seals " + creditedSeals + ".");
    }

    private YamlConfiguration stateDocument() {
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
        // 0.3.0 sections (additive: a 0.2.11 rollback reads the
        // document fine and simply ignores these keys). Null-tolerant:
        // the recovery harness drives stateDocument on a bare
        // instance whose 0.3.0 fields were never constructed.
        if (repeatWindows != null) {
            for (Map.Entry<String, Object> e : repeatWindows.snapshot().entrySet()) {
                y.set("repeat-windows." + e.getKey(), e.getValue());
            }
        }
        if (signatureGenerations != null) {
            for (Map.Entry<String, Integer> e : signatureGenerations.entrySet()) {
                y.set("signature-generations." + e.getKey(), e.getValue());
            }
        }
        if (creditedSeals != null) {
            y.set("credited-seals", new ArrayList<>(creditedSeals));
        }
        if (clearCounts != null) {
            for (Map.Entry<String, Map<UUID, Integer>> e : clearCounts.entrySet()) {
                for (Map.Entry<UUID, Integer> c : e.getValue().entrySet()) {
                    y.set("repeat-clears." + e.getKey() + "." + c.getKey(), c.getValue());
                }
            }
        }
        // 0.3.6: the damage-feedback opt-outs (additive like the
        // 0.3.0 sections: an older rollback ignores the key).
        if (feedbackOptOuts != null) {
            y.set("feedback-opt-outs", feedbackOptOuts.snapshot());
        }
        y.set("recovery.protocol", 1);
        y.set("recovery.receipts", new ArrayList<>(recoveryReceipts.values()));
        return y;
    }

    private void saveState() {
        if (recoveryPersistenceLocked) {
            getLogger().severe("State save refused: prior uncertain/invalid recovery persistence requires restart and inspection.");
            return;
        }
        try {
            persistStateChecked(stateDocument());
        } catch (IOException ex) {
            recoveryPersistenceLocked = true;
            getLogger().severe("Could not durably save state.yml; writes locked pending restart/inspection: " + ex.getMessage());
        }
    }

    private void persistStateChecked(YamlConfiguration document) throws IOException {
        java.nio.file.Path path=stateFile().toPath();
        String actual=java.nio.file.Files.exists(path)
                ? recoveryBytesHash(java.nio.file.Files.readAllBytes(path)) : "MISSING";
        if(recoveryDiskToken==null || !recoveryDiskToken.equals(actual))
            throw new IOException("state.yml changed outside this owner; refusing overwrite");
        String encoded=document.saveToString();
        AtomicStateFile.write(path,encoded,stateWriteProbe);
        recoveryDiskToken=recoveryBytesHash(encoded.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }
    private static String recoveryBytesHash(byte[] data) {
        try{return java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(data));}
        catch(java.security.NoSuchAlgorithmException impossible){throw new IllegalStateException(impossible);}
    }

    /** Admin coordinator bridge, protocol 1. Trusted plugins only; command owner enforces admin permission. */
    public Map<String,Object> recoverySnapshot(String seal) {
        Map<String,Object> out = new LinkedHashMap<>();
        out.put("protocol",1); out.put("seal",seal == null ? "" : seal);
        out.put("generation",recoveryGeneration);
        out.put("enabled",false); out.put("active",false); out.put("deadline",0L); out.put("stateToken","");
        String reason = "";
        if (!Bukkit.isPrimaryThread()) reason="MAIN_THREAD_REQUIRED";
        BossKind kind = seal == null ? null : BossKind.fromSealId(seal);
        if (reason.isEmpty() && kind == null) reason="UNSUPPORTED_SEAL";
        if (reason.isEmpty()) {
            out.put("deadline",cooldowns.untilMs(kind.sealId()));
            out.put("stateToken",recoveryToken(kind,cooldowns.untilMs(kind.sealId()),elderSealApproved));
            boolean active = encounters.values().stream().anyMatch(l -> l.kind()==kind);
            out.put("active",active);
            if (recoveryPersistenceLocked) reason="PERSISTENCE_UNCERTAIN_RESTART_REQUIRED";
            else if (!isEnabled() || !cfg.enabled || !chapterOn(kind)) reason="PLUGIN_OR_CHAPTER_DISABLED";
            else if (kind==BossKind.ELDER_GUARDIAN && elderRecoveryPending()) reason="ELDER_PENDING_RESPAWN";
            else out.put("enabled",true);
            if (reason.isEmpty() && active) reason="ACTIVE_SELECTED_ENCOUNTER";
        }
        out.put("reason",reason);out.put("blockedReason",reason);
        return out;
    }

    /** Clears ONLY the selected cooldown; a full reviewed elder seal also establishes its approval latch. */
    public Map<String,Object> recoveryApply(String operationId,String digest,String seal,Map<String,Object> expected) {
        if (!Bukkit.isPrimaryThread()) return recoveryResult(seal,operationId,"BLOCKED","MAIN_THREAD_REQUIRED");
        if (!validRecoveryId(operationId) || !validRecoveryHash(digest) || expected==null)
            return recoveryResult(seal,operationId,"INVALID","INVALID_OPERATION_OR_DIGEST");
        BossKind kind=seal==null?null:BossKind.fromSealId(seal);
        if(kind==null)return recoveryResult(seal,operationId,"UNAVAILABLE","UNSUPPORTED_SEAL");
        Map<String,Object> current=recoverySnapshot(seal);
        if(recoveryPersistenceLocked)return recoveryResult(seal,operationId,"IO_FAILURE","PERSISTENCE_UNCERTAIN_RESTART_REQUIRED");
        // Replay is also a coordinator completion preflight: never complete a pending global grant during a new fight.
        if(!Boolean.TRUE.equals(current.get("enabled")))return recoveryResult(seal,operationId,"BLOCKED",String.valueOf(current.get("reason")));
        if(Boolean.TRUE.equals(current.get("active")))return recoveryResult(seal,operationId,"ACTIVE","ACTIVE_SELECTED_ENCOUNTER");
        Map<String,String> receipt=recoveryReceipts.get(operationId);
        if(receipt!=null) {
            if(!digest.equals(receipt.get("digest"))||!seal.equals(receipt.get("seal")))
                return recoveryResult(seal,operationId,"INVALID","OPERATION_ID_CONFLICT");
            if(!receipt.get("afterToken").equals(current.get("stateToken")))
                return recoveryResult(seal,operationId,"DIVERGED","STATE_CHANGED_AFTER_APPLY");
            return recoveryResult(seal,operationId,"REPLAYED","");
        }
        Object wantedDeadline=expected.get("deadline");
        if(!(wantedDeadline instanceof Number n) || n.doubleValue()!=n.longValue()
                || n.longValue()!=cooldowns.untilMs(seal)
                || !recoveryGeneration.equals(expected.get("generation"))
                || !current.get("stateToken").equals(expected.get("stateToken")))
            return recoveryResult(seal,operationId,"STALE","PREVIEW_STATE_CHANGED");
        boolean afterLatch=kind==BossKind.ELDER_GUARDIAN || elderSealApproved;
        String afterToken=recoveryToken(kind,0L,afterLatch);
        Map<String,String> rec=new LinkedHashMap<>();
        rec.put("id",operationId);rec.put("digest",digest);rec.put("seal",seal);rec.put("afterToken",afterToken);
        YamlConfiguration next=stateDocument();
        next.set("cooldowns."+seal,null);
        if(kind==BossKind.ELDER_GUARDIAN)next.set("elder-seal-approved",true);
        List<Map<String,String>> receiptList=new ArrayList<>(recoveryReceipts.values());receiptList.add(rec);
        next.set("recovery.receipts",receiptList);
        try { persistStateChecked(next); }
        catch(IOException failure) {
            recoveryPersistenceLocked=true;
            getLogger().severe("Recovery persistence uncertain; all state writes locked until restart/inspection: "+failure.getMessage());
            return recoveryResult(seal,operationId,"IO_FAILURE","PERSISTENCE_UNCERTAIN_RESTART_REQUIRED");
        }
        // Publish only after the owner state AND receipt were atomically committed.
        cooldowns.clear(seal);
        elderSealApproved=afterLatch;
        recoveryReceipts.put(operationId,rec);
        return recoveryResult(seal,operationId,"APPLIED","");
    }

    private Map<String,Object> recoveryResult(String seal,String id,String status,String reason) {
        Map<String,Object> out=recoverySnapshot(seal);
        out.put("status",status);out.put("operationId",id==null?"":id);
        out.put("reason",reason);out.put("blockedReason",reason);return out;
    }
    private boolean elderRecoveryPending() {
        Object rows=monuments.snapshot().get("elders");
        if(rows instanceof List<?> list) for(Object row:list)
            if(row instanceof Map<?,?> m && "PENDING_RESPAWN".equals(m.get("status"))) return true;
        return false;
    }
    private String recoveryToken(BossKind kind,long deadline,boolean latch) {
        Map<String,Object> state=new java.util.TreeMap<>();
        state.put("seal",kind.sealId());state.put("deadline",deadline);
        if(kind==BossKind.ELDER_GUARDIAN){state.put("elderLatch",latch);state.put("monuments",monuments.snapshot());}
        return recoveryHash(recoveryCanonical(state));
    }
    private static String recoveryCanonical(Object value) {
        if(value instanceof Map<?,?> m) {
            java.util.TreeMap<String,Object> sorted=new java.util.TreeMap<>();
            m.forEach((k,v)->sorted.put(String.valueOf(k),v));
            StringBuilder b=new StringBuilder("{");sorted.forEach((k,v)->b.append(recoveryCanonical(k)).append(recoveryCanonical(v)));return b.append('}').toString();
        }
        if(value instanceof List<?> list){StringBuilder b=new StringBuilder("[");for(Object v:list)b.append(recoveryCanonical(v));return b.append(']').toString();}
        String s=String.valueOf(value);return s.length()+":"+s;
    }
    private static String recoveryHash(String text) {
        try{return java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(text.getBytes(java.nio.charset.StandardCharsets.UTF_8)));}
        catch(java.security.NoSuchAlgorithmException impossible){throw new IllegalStateException(impossible);}
    }
    private static boolean validRecoveryHash(String s){return s!=null&&s.matches("[0-9a-f]{64}");}
    private static boolean validRecoveryId(String s){try{return s!=null&&UUID.fromString(s).toString().equals(s);}catch(IllegalArgumentException bad){return false;}}

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
            if (kind == BossKind.ELDER_GUARDIAN) {
                // 0.3.1: register the monument BEFORE the pool is
                // priced - the trio position (counted kills so
                // far + 1) fixes this elder's escalation
                // multiplier for the whole encounter. register()
                // is idempotent; the registration at the end of
                // this lambda still runs and still saves state.
                Location home = boss.getLocation();
                MonumentTracker.ElderRecord rec = monuments.register(id,
                        home.getWorld().getName(), home.getX(), home.getY(), home.getZ());
                int counted = monuments.progress(rec.monumentId);
                double mult = TrioEscalation.multiplierFor(counted, elderTrioMultipliers);
                elderTrioMult.put(id, mult);
                if (mult != 1.0) {
                    getLogger().info("BossTrial elder trio [" + rec.monumentId
                            + "]: elder #" + (counted + 1) + " of the trio - pool x"
                            + String.format(java.util.Locale.ROOT, "%.2f", mult)
                            + " (elder-guardian.trio.position-multipliers).");
                }
            }
            ScalingRule rule = scalingFor(kind, id);
            if (rule.active()) {
                // Participant growth opens here (spec section 1):
                // the boss wakes at its full trial strength - the
                // current health rises by the same delta over
                // vanilla, so damage already taken is preserved
                // exactly (never pre-wounded, never healed).
                openingMax = rule.baseHealth();
                double intakeF = com.winkreyes.excelbosstrial.core.IntakeFactor.factor(
                        cfg.intakeEntityCap, openingMax);
                if (intakeF < 1.0) {
                    // 0.3.0 INTAKE (SPEC 0.3): the configured pool
                    // overflows the platform's health ceiling, so
                    // the bar lives at the entity cap and damage is
                    // scaled by the intake factor instead - the
                    // setHealth that threw on 0.2.11 never sees a
                    // value above the cap. A bar already AT the cap
                    // is a re-opened encounter: its entity-space
                    // health is already right and stays untouched.
                    double entityMax = com.winkreyes.excelbosstrial.core.IntakeFactor.entityMax(
                            cfg.intakeEntityCap, openingMax);
                    boolean alreadyCapped = attr != null
                            && Math.abs(vanillaMax - entityMax) <= 0.01;
                    if (attr != null && !alreadyCapped) {
                        attr.setBaseValue(entityMax);
                        if (boss.getHealth() > 0) {
                            boss.setHealth(entityMax);
                        }
                    }
                    getLogger().info("BossTrial intake [" + kind.displayName()
                            + "]: pool " + fmt(openingMax) + " over entity cap "
                            + fmt(cfg.intakeEntityCap) + " - bar at " + fmt(entityMax)
                            + ", damage intake x" + String.format("%.4f", intakeF) + ".");
                } else if (attr != null && openingMax != vanillaMax) {
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
                double f0 = com.winkreyes.excelbosstrial.core.IntakeFactor.factor(
                        cfg.intakeEntityCap, openingMax);
                if (f0 < 1.0) {
                    // Intake reopen: judge "was this bar ours" in
                    // entity space (at the cap, below full) and the
                    // lost health in ledger space.
                    boolean raisedByUs = Math.abs(vanillaMax - cfg.intakeEntityCap) <= 0.01
                            && boss.getHealth() < cfg.intakeEntityCap - 0.01;
                    unseen = raisedByUs
                            ? Math.max(0, openingMax - com.winkreyes.excelbosstrial.core.IntakeFactor
                                    .toLedger(boss.getHealth(), f0))
                            : 0;
                } else {
                    unseen = EncounterRetention.unseenOnReopen(openingMax, vanillaMax,
                            rule.baseHealth(), boss.getHealth());
                }
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
            // 0.3.1: the fight record's healing side opens with
            // the ledger (damage rows read the ledger itself at
            // snapshot time; only healing needs its own feed).
            // 0.4.0 (PM-12): the grant watch's baseline - whether
            // this encounter's seal already stood broken when the
            // fight opened (a repeat fight never arms the watch).
            sealBrokenAtOpen.put(id, sealBroken(kind));
            fightTracker.openEncounter(id, kind, fresh.startedMs());
            // 0.3.4: the boss-state feed opens with the ledger
            // (the sink is NOOP unless the Protect adapter bound;
            // a re-opened lost ledger is a new encounter here too,
            // exactly as the fight record treats it).
            if (protectEmitSettings.stateChanges()) {
                protectEmit.record(ProtectTableShape.STREAM,
                        ProtectTableShape.TYPE_BOSS_STATE,
                        ProtectTableShape.fightStarted(kind, id.toString(), openingMax));
            }
            return fresh;
        });
        // 0.3.6: the feedback bar opens with the ledger (the
        // call is create-once; every later touch is a no-op).
        if (feedback != null) {
            feedback.onEncounterOpen(boss, kind, ledger);
        }
        observeTeam(boss, kind);
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
    /**
     * The scaling rule an encounter prices against (0.3.1): the
     * config rule as-is for every chapter except the elder trio,
     * whose pool carries the position multiplier cached when the
     * encounter opened (elder-guardian.trio.position-multipliers;
     * settled defaults 1.0 / 1.0 / 1.3). Opening and rally growth
     * both price through here, so a mid-fight joiner grows the
     * escalated pool by the escalated slope.
     */
    private ScalingRule scalingFor(BossKind kind, UUID bossId) {
        ScalingRule rule = cfg.scaling(kind);
        if (kind != BossKind.ELDER_GUARDIAN) {
            return rule;
        }
        return TrioEscalation.scaled(rule,
                elderTrioMult.getOrDefault(bossId, 1.0));
    }

    private void maybeGrowRally(LivingEntity boss, BossKind kind, EncounterLedger ledger) {
        maybeGrowRally(boss, kind, ledger, 0);
    }

    /**
     * @param pendingEntityDamage the entity-space damage of the hit
     *        that registered the new damager, not yet applied when
     *        this runs (the record handler is MONITOR) - under the
     *        intake factor the bar is re-derived from the ledger,
     *        so the pending hit is added back in entity space.
     */
    private void maybeGrowRally(LivingEntity boss, BossKind kind, EncounterLedger ledger,
                                double pendingEntityDamage) {
        ScalingRule rule = scalingFor(kind, boss.getUniqueId());
        if (!rule.active()) {
            return;
        }
        int damagers = ledger.damagerCount();
        double delta = ledger.growTo(rule.poolFor(damagers));
        if (delta <= 0) {
            return;
        }
        double intakeF = com.winkreyes.excelbosstrial.core.IntakeFactor.factor(
                cfg.intakeEntityCap, ledger.maxHealth());
        if (intakeF < 1.0) {
            // Intake growth: the entity bar does NOT grow (it is
            // pinned at the cap); it is re-derived so it still
            // mirrors the ledger's remaining fraction of the NEW
            // pool - the boss visibly gains ground exactly as the
            // configured-space rally promises.
            if (boss.isDead() || boss.getHealth() <= 0) {
                getLogger().info("BossTrial rally [" + kind.displayName() + "]: champion #"
                        + damagers + " registered on the killing blow - pool denominator "
                        + "grows to " + fmt(ledger.maxHealth()) + " for the verdict.");
                return;
            }
            AttributeInstance attr = boss.getAttribute(Attribute.MAX_HEALTH);
            double entityMax = com.winkreyes.excelbosstrial.core.IntakeFactor.entityMax(
                    cfg.intakeEntityCap, ledger.maxHealth());
            if (attr != null) {
                attr.setBaseValue(entityMax);
            }
            double target = com.winkreyes.excelbosstrial.core.IntakeFactor.entityHealthFor(
                    cfg.intakeEntityCap, ledger.maxHealth(), ledger.totalDamage(),
                    ledger.unseenDamage()) + Math.max(0, pendingEntityDamage);
            boss.setHealth(Math.min(entityMax, Math.max(0.01, target)));
            String text = msgRally
                    .replace("{name}", kind.displayName())
                    .replace("{ordinal}", ordinalWord(damagers))
                    .replace("{pool}", String.valueOf((int) Math.round(ledger.maxHealth())));
            broadcast(text);
            getLogger().info("BossTrial rally [" + kind.displayName() + "]: champion #"
                    + damagers + " registered - pool grows by " + fmt(delta) + " to "
                    + fmt(ledger.maxHealth()) + " (intake bar re-derived at "
                    + fmt(target) + " of " + fmt(entityMax) + ").");
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
        if (verdictPending.containsKey(boss.getUniqueId())) {
            return; // 0.4.0 (PM-2): the verdict blow passes unscaled
        }
        BossKind kind = Attribution.kindOf(boss);
        if (kind == null || kind == BossKind.ENDER_DRAGON || !chapterOn(kind)) {
            return; // the dragon's governor lives in its chapter
        }
        BossTrialConfig.Chapter chapter = cfg.chapter(kind);
        EncounterLedger ledger = encounter(boss, kind);
        // 0.3.0 intake: a chapter whose pool overflows the entity
        // cap processes damage even with its per-event caps off -
        // the factor IS its governor. The encounter() call moved
        // above the caps check is safe: the record handler opens
        // the same encounter on the same event either way.
        double intakeF = com.winkreyes.excelbosstrial.core.IntakeFactor.factor(
                cfg.intakeEntityCap, ledger.maxHealth());
        if (!chapter.caps().anyEnabled() && intakeF >= 1.0) {
            return;
        }
        double rawFinal = event.getFinalDamage();
        var governed = ledger.governor().govern(rawFinal,
                Attribution.isExplosion(event.getCause()), Bukkit.getCurrentTick(), chapter.caps());
        double scaled = governed.damage * intakeF;
        if ((governed.trimmed() || intakeF < 1.0) && rawFinal > 0) {
            event.setDamage(event.getDamage() * (scaled / rawFinal));
            if (governed.trimmed()) {
                getLogger().info("BossTrial cap [" + governed.ruleLabel() + "] on " + kind.displayName()
                        + ": hit trimmed " + fmt(rawFinal) + " -> " + fmt(governed.damage)
                        + (intakeF < 1.0 ? " -> intake " + fmt(scaled) : "")
                        + " (boss at " + fmt(boss.getHealth()) + " HP).");
            } else if (intakeLogged.add(boss.getUniqueId())) {
                getLogger().info("BossTrial intake [" + kind.displayName()
                        + "]: damage scaled x" + String.format("%.4f", intakeF)
                        + " (pool " + fmt(ledger.maxHealth()) + " over entity cap "
                        + fmt(cfg.intakeEntityCap) + ").");
            }
        }
        // 0.4.0 (PM-9): hand the governed result to the record
        // handler in ledger space - it must never re-derive the
        // ledger number from a final damage another plugin may
        // have modified after this pass (the x21 amplification
        // risk, closed structurally). Stashed even when the
        // event itself was left untouched: governed.damage is
        // then exactly the raw final, so the record agrees.
        com.winkreyes.excelbosstrial.core.GovernorStash.put(event, governed.damage);
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
        if (verdictPending.containsKey(boss.getUniqueId())) {
            return; // 0.4.0 (PM-2): the verdict blow is not fight damage
        }
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
        // 0.3.0 intake: the event's final damage is ENTITY space
        // (the governor already scaled it); the ledger judges in
        // configured space, so convert by the CURRENT factor. The
        // dragon's factor is its live phase pool (its governor is
        // the chapter's, but this ledger is the share judge).
        double intakeF = intakeFactorFor(boss, kind, ledger);
        // 0.4.0 (PM-9): the governor's own governed output, in
        // ledger space, whenever it ran - a post-governor
        // modification by another plugin can no longer be
        // divided by the intake factor into the ledger (the x21
        // amplification risk, closed structurally). The
        // re-derivation survives only as the fallback for the
        // passes where the governor demonstrably did not run
        // (before it is enabled, a chapter with no caps at f=1).
        Double stashed = com.winkreyes.excelbosstrial.core.GovernorStash.consume(event);
        double recordedDamage = stashed != null ? stashed
                : com.winkreyes.excelbosstrial.core.IntakeFactor
                        .toLedger(event.getFinalDamage(), intakeF);
        boolean newDamager = banded
                ? ledger.recordDealtBanded(dealt, recordedDamage,
                        System.currentTimeMillis(), heightOk, rangeOk)
                : ledger.recordDealt(dealt, recordedDamage,
                        System.currentTimeMillis(), counts);
        // 0.4.0 (PM-9): unattributed damage is legitimate (it
        // advances the pool and is accounted as 'other' in the
        // fight record - the split is persisted there now) but
        // no longer invisible: a big unattributed hit earns a
        // throttled console line naming its cause class, so a
        // future Nov 7-style mystery has its evidence in the log.
        if (dealt == null && recordedDamage >= unattributedLogThreshold) {
            long nowLog = System.currentTimeMillis();
            if (nowLog - unattributedLogMs.getOrDefault(boss.getUniqueId(), 0L)
                    >= 5_000L) {
                unattributedLogMs.put(boss.getUniqueId(), nowLog);
                getLogger().info("BossTrial unattributed [" + kind.displayName()
                        + "]: " + fmt(recordedDamage)
                        + " pool damage from no player (cause "
                        + event.getCause().name()
                        + ") - accounted as 'other' in the fight record.");
            }
        }
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
            maybeGrowRally(boss, kind, ledger, event.getFinalDamage());
        }
        // 0.3.2: milestone beats, judged AFTER the record and any
        // rally growth, so the fraction is this hit's against the
        // pool as it now stands. The dragon is skipped here - its
        // ledger spans both phases and the chapter fires its beats
        // per phase.
        if (kind != BossKind.ENDER_DRAGON) {
            checkMilestones(kind, boss, ledger);
        }
        // 0.3.6: damage feedback, AFTER the record and any rally
        // growth (and after the milestone judgment), so the title
        // renders post-record ledger truth. Display only: the
        // credited amount is what the ledger just credited this
        // damager (0 for a band-excluded blow - its total did not
        // move, and the action bar must not pretend it did).
        if (feedback != null) {
            double credited = dealt != null && counts ? recordedDamage : 0.0;
            feedback.onRecordedHit(boss, kind, ledger, dealt, credited);
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
        // 0.4.0 (PM-2): the pool-exhaustion verdict, judged the
        // moment a recorded hit spends the pool (the 1-second
        // reconciliation tick is the backstop for every other
        // route to exhaustion).
        maybeVerdict(boss, kind, ledger);
    }

    /**
     * 0.3.2: judge this encounter's milestone beats after a
     * recorded hit. The rule (stated on core/MilestoneBeats):
     * crossings are judged on the remaining fraction of the
     * CURRENT pool - 1 - totalDamage / maxHealth, with the
     * ledger's ratcheted (possibly rally-grown) pool - and the
     * tracker's fired mask dedupes: pool growth can push the
     * fraction back above a spent threshold, but the beat never
     * fires twice. Firing speaks the chapter's lines and queues
     * at most ONE pulse surge per damage event (a blow crossing
     * two thresholds speaks both lines, surges once - the surge
     * amount is the same for every threshold).
     */
    private void checkMilestones(BossKind kind, LivingEntity boss, EncounterLedger ledger) {
        MilestoneSettings ms = milestoneSettings.get(kind);
        boolean beatsOn = ms != null && ms.enabled();
        // 0.3.3: the elite sprinkle rolls on the same stamps but
        // is armed independently of the milestone lines - a
        // chapter with its lines silenced still rolls. When
        // neither feature is live for the chapter this returns
        // exactly as 0.3.2 did, tracker untouched.
        boolean sprinkleOn = sprinkle != null && sprinkle.armed(kind);
        if (!beatsOn && !sprinkleOn) {
            return;
        }
        double pool = ledger.maxHealth();
        if (pool <= 0) {
            return;
        }
        // 0.4.0 (PM-1): beats judge on NET damage - a boss that
        // heals back across a threshold has un-spent that part
        // of the fight (the beat tracker dedupes the re-crossing:
        // a beat never fires twice for one encounter).
        double remaining = 1.0 - ledger.netDamage() / pool;
        int pending = ledger.milestoneBeats().pending(remaining);
        if (pending == 0) {
            return;
        }
        ledger.milestoneBeats().commit(pending);
        // 0.4.0 (PM-9): the milestone accounting line - how the
        // pool split between players and everything else, every
        // beat, so 'other' is never a silent number again.
        getLogger().info("BossTrial milestone accounting [" + kind.displayName()
                + "]: pool spent so far - players " + fmt(ledger.playerDamage())
                + " / other " + fmt(ledger.otherDamage())
                + " (healed back " + fmt(ledger.healedPool()) + ").");
        boolean surged = false;
        for (int i = 0; i < MilestoneBeats.THRESHOLDS.length; i++) {
            if ((pending & (1 << i)) == 0) {
                continue;
            }
            if (beatsOn) {
                String text = ms.textFor(i);
                if (!text.isEmpty()) {
                    broadcast(text.replace("{name}", kind.displayName()));
                }
                getLogger().info("BossTrial milestone [" + kind.displayName() + "]: "
                        + MilestoneBeats.thresholdPercent(i) + "% of the pool remains ("
                        + fmt(ledger.netDamage()) + " net of " + fmt(pool) + " dealt)"
                        + (ms.surgeMultiplier() > 0 ? " - pulse surge queued" : "")
                        + ".");
                if (!surged && ms.surgeMultiplier() > 0) {
                    BossTrialConfig.Pressure pressure = cfg.pressure(kind);
                    if (pressure.enabled() && pressure.pulseDamage() > 0) {
                        ledger.queueSurge();
                    }
                    surged = true;
                }
            }
            if (sprinkleOn) {
                sprinkle.onMilestone(kind, boss, i, null);
            }
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
        verdictPending.remove(boss.getUniqueId());
        // 0.4.0 (PM-8): a give-up is a RECORDED close - the fight
        // record is written with its cause before anything tears
        // down (0.3.1 dropped the record unsnapshotted: no
        // verdict, no record - the gap PM-8 closes).
        snapshotCloseRecord(BossKind.WARDEN, boss.getUniqueId(), ledger,
                LABEL_GIVE_UP, null, List.of(), null);
        // 0.3.3: a give-up is an encounter close like any other -
        // its sprinkle adds are removed with it.
        if (sprinkle != null) {
            sprinkle.closeEncounter(boss.getUniqueId(), "warden give-up");
        }
        // 0.3.6: the give-up closes the feedback bar too.
        if (feedback != null) {
            feedback.onEncounterClose(boss.getUniqueId());
        }
        elderTrioMult.remove(boss.getUniqueId());
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
        // 0.4.0 (PM-2): the death settles any verdict-pending
        // state - a natural kill and a verdict blow land here
        // identically, by design.
        verdictPending.remove(boss.getUniqueId());
        // 0.3.3: the verdict closes the sprinkle too - this
        // encounter's elite adds are removed here (every elder
        // trio progress point also passes through this handler,
        // so a fallen elder's adds end with that elder, exactly
        // as the design settles).
        if (sprinkle != null) {
            sprinkle.closeEncounter(boss.getUniqueId(), "verdict");
        }
        // 0.3.6: the verdict closes the feedback bar with the
        // encounter (every elder trio progress point too).
        if (feedback != null) {
            feedback.onEncounterClose(boss.getUniqueId());
        }
        DeathOutcome outcome = outcomeFor(kind, boss, ledger);
        // 0.3.1: snapshot the fight record NOW, while the ledger
        // still holds the per-player table - every verdict and
        // every elder trio progress point passes through here.
        // The snapshot guards itself: a tracking failure logs a
        // warning and the verdict path below runs untouched.
        snapshotFightRecord(kind, boss, ledger, outcome, event);
        boolean teamVerdictReady = prepareTeamVerdict(boss, kind, outcome);
        // 0.2.11 (B-3): a CREDIT dragon pays its XP per champion —
        // the death event carries none, and every online roster
        // champion receives the full base award directly. The single
        // multiplied pool, divided by whoever scooped the orbs, is
        // retired for the dragon; when no roster is on record the
        // legacy spoils path below still pays, so XP is never voided.
        boolean dragonXpPaid = false;
        if (kind == BossKind.ENDER_DRAGON && boss instanceof EnderDragon dragon
                && (outcome.verdict == CreditJudge.Verdict.CREDIT
                        || outcome.verdict == CreditJudge.Verdict.REPEAT_CREDIT)
                && teamVerdictReady) {
            dragonXpPaid = applyDragonXpPerChampion(event, dragon,
                    outcome.verdict == CreditJudge.Verdict.REPEAT_CREDIT);
        }
        // Spoils follow the verdict (Mike, Oct 19, 2026): the
        // multiplier baked into the outcome is already 1 on any
        // denial, so a refused kill falls through here with plain
        // vanilla drops and no spoils broadcast, and the dragon's
        // XP stays unscaled.
        if (cfg.spoilsEnabled && outcome.multiplier > 1) {
            if (kind == BossKind.ENDER_DRAGON && boss instanceof EnderDragon dragon) {
                if (!dragonXpPaid) {
                    applyDragonXp(event, dragon, outcome.multiplier);
                    broadcast(fill(msgSpoilsDragon, kind, outcome));
                }
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
        try { applySealLoot(kind, event, outcome, teamVerdictReady); }
        catch (Exception rewardFailure) {
            getLogger().severe("Personal reward processing needs review; combat verdict unchanged: " + rewardFailure.getMessage());
        }
        // 0.3.0 repeat layer: the window + clear counts persist
        // BEFORE any repeat reward moves (the companion spec's
        // pay-order discipline), then the slate pays.
        if (outcome.verdict == CreditJudge.Verdict.CREDIT && cfg.repeatEnabled
                && !outcome.elderProgress) {
            // A first break is a clear too, and the repeat window
            // runs from THIS kill. (Rig catch, 2026-11-01: this
            // bookkeeping originally ran only on REPEAT_CREDIT, so
            // no window existed after a first break and the first
            // repeat inside 24h would have paid as a repeat
            // instead of judging DENY_REPEAT_WINDOW.)
            recordRepeatClear(kind, outcome);
        }
        if (outcome.verdict == CreditJudge.Verdict.REPEAT_CREDIT) {
            recordRepeatClear(kind, outcome);
            try { applyRepeatLayer(kind, event, outcome, teamVerdictReady, ledger); }
            catch (Exception repeatFailure) {
                getLogger().severe("Repeat reward layer needs review; verdict unchanged: " + repeatFailure.getMessage());
            }
        }
        applyConsequences(kind, boss, outcome);
    }

    /**
     * Curated seal loot (spec section 4): the signature pool once
     * per seal on the seal-breaking kill — since 0.2.11 (B-3, Reward
     * Law variant B) EVERY signature stack is a per-champion claim,
     * nothing signature drops at the kill — one bonus roll per extra
     * standing champion (cap +4) on every credit kill, and the
     * dragon's breath + crystals as per-champion claims on every
     * dragon credit kill. Denied kills get none of it — they drop
     * plain vanilla.
     */
    private void applySealLoot(BossKind kind, EntityDeathEvent event, DeathOutcome outcome, boolean teamVerdictReady) {
        if (!lootEnabled || (outcome.verdict != CreditJudge.Verdict.CREDIT
                && outcome.verdict != CreditJudge.Verdict.REPEAT_CREDIT)) {
            return;
        }
        SealLoot.LootTable table = lootTables.getOrDefault(kind, SealLoot.defaultsFor(kind));
        // 0.3.0: signatures are generation-scoped. A first-break
        // CREDIT pays the next unpaid generation in base form
        // (generation 0 on a fresh seal; the next generation when a
        // credited seal somehow holds no generation record - the
        // admin-grant edge); a REPEAT_CREDIT pays the next
        // generation REFORGED, while the repeat signature switch
        // is on. Elder progress kills never pay (unchanged).
        int lastGen = signatureGenerations.getOrDefault(kind.sealId(), -1);
        boolean signature = SealLoot.paysSignature(outcome.verdict, outcome.elderProgress,
                cfg.repeatSignatureEnabled, true);
        int payGen = signature ? lastGen + 1 : -1;
        boolean reforged = outcome.verdict == CreditJudge.Verdict.REPEAT_CREDIT;
        boolean signatureStored = false;
        if (signature && teamVerdictReady) {
            try {
                grantSignature(kind, table, outcome, event, payGen, reforged);
                signatureGenerations.put(kind.sealId(), payGen);
                signaturePaid.add(kind.sealId());
                saveState();
                signatureStored = true;
            } catch (Exception signatureFailure) {
                getLogger().severe("Signature claim needs owner review; ordinary credit-kill bonuses remain unchanged: " + signatureFailure.getMessage());
            }
        }
        if (kind == BossKind.ENDER_DRAGON) {
            // On the seal-breaking kill the dragon's materials already
            // ride the signature distribution (grantSignature — the
            // ledger finalizes one plan per run). Every other CREDIT
            // kill stores them on their own. Either way they are
            // per-champion claims now, never pooled drops.
            if (signatureStored) {
                getLogger().info("BossTrial seal loot [Dragon]: breath x" + dragonBreathBase
                        + ", end crystals x" + dragonCrystals + " per champion ("
                        + outcome.presentParticipants + " champion(s) standing).");
                return;
            }
            DragonMaterials stored = teamVerdictReady
                    ? storeDragonMaterials(event) : DragonMaterials.FAILED;
            if (stored == DragonMaterials.STORED) {
                getLogger().info("BossTrial seal loot [Dragon]: breath x" + dragonBreathBase
                        + ", end crystals x" + dragonCrystals + " per champion ("
                        + outcome.presentParticipants + " champion(s) standing).");
                return;
            }
            if (stored == DragonMaterials.FAILED) {
                // The store itself reported the failure (uncertain
                // write or no team claims): no direct payout, exactly
                // like a failed signature write.
                return;
            }
            // EMPTY_ROSTER: the spec's own fallback — a CREDIT kill
            // with nobody on record to claim into pays the pre-0.2.11
            // pooled drops, so the kill's materials are never voided.
            int breath = Math.min(dragonBreathCapN, dragonBreathBase
                    + dragonBreathPer * Math.max(0, outcome.presentParticipants - 1));
            if (breath > 0) {
                event.getDrops().add(new ItemStack(Material.DRAGON_BREATH, breath));
            }
            if (dragonCrystals > 0) {
                event.getDrops().add(new ItemStack(Material.END_CRYSTAL, dragonCrystals));
            }
            getLogger().warning("BossTrial seal loot [Dragon]: no roster on record for "
                    + "per-champion material claims; fell back to pooled drops (breath x"
                    + breath + ", end crystals x" + dragonCrystals + ", "
                    + outcome.presentParticipants + " champion(s) standing).");
            return;
        }
        boolean repeatKill = outcome.verdict == CreditJudge.Verdict.REPEAT_CREDIT;
        int rolls = repeatKill
                ? SealLoot.repeatRolls(outcome.presentParticipants, cfg.repeatJackpotExtraRolls)
                : SealLoot.bonusRolls(outcome.presentParticipants);
        if (rolls > 0) {
            // Repeat kills draw over bonus + jackpot combined, and
            // the underdog factor scales the stack counts (SPEC 4.1).
            SealLoot.LootTable drawTable = repeatKill
                    ? new SealLoot.LootTable(table.signature(), table.bonusAndJackpot())
                    : table;
            List<SealLoot.RolledBonus> rolled = SealLoot.rollBonus(drawTable, rolls, lootRng);
            for (SealLoot.RolledBonus r : rolled) {
                Material mat = Material.matchMaterial(r.material());
                if (mat != null) {
                    int count = repeatKill
                            ? com.winkreyes.excelbosstrial.core.Underdog.scaledCount(
                                    r.count(), outcome.presentParticipants)
                            : r.count();
                    event.getDrops().add(new ItemStack(mat, count));
                }
            }
            if (!rolled.isEmpty()) {
                getLogger().info("BossTrial seal loot [" + kind.displayName()
                        + "]: " + (repeatKill ? "repeat jackpot" : "bonus") + " rolls " + rolled
                        + " (" + rolls + " roll(s), "
                        + outcome.presentParticipants + " champion(s) standing).");
            }
        }
    }

    /** Outcome of one dragon-materials store attempt (0.2.11, B-3). */
    private enum DragonMaterials { STORED, EMPTY_ROSTER, FAILED }

    /** B-3 unit ids for the dragon's per-champion materials. */
    private static String dragonMaterialUnit(UUID owner, String material) {
        return switch (material) {
            case "DRAGON_BREATH" -> "personal:" + owner + ":breath";
            case "END_CRYSTAL" -> "personal:" + owner + ":crystals";
            default -> "personal:" + owner + ":mat:" + material;
        };
    }

    /**
     * Execute the grant plan for one paying signature kill. Since
     * 0.2.11 (B-3, Reward Law variant B) the plan is
     * {@link SealLoot#perChampionPlan}: the FULL table stored in
     * every roster member's personal claim — named gear carrying
     * its provenance stamp for that champion, material stacks
     * plain. Nothing signature drops at the kill any more. On the
     * dragon's seal-breaking kill the per-kill materials ride this
     * same distribution (the ledger finalizes exactly one plan per
     * run, so they cannot be a second store); every later CREDIT
     * kill stores them alone via {@link #storeDragonMaterials}.
     * The plan the unit tests pin is the plan the server runs — on
     * the config path and the code-default path alike, since both
     * arrive here as the same LootTable.
     */
    private void grantSignature(BossKind kind, SealLoot.LootTable table,
                                DeathOutcome outcome, EntityDeathEvent event,
                                int generation, boolean reforged) {
        if (teamClaims == null) throw new IllegalStateException("team claims unavailable");
        String run = kind == BossKind.ELDER_GUARDIAN ? trioRun(event.getEntity()) : teamRun(event.getEntity(), kind);
        // 0.3.0: the once-scope is generation-scoped
        // (signature:<seal>:<gen>). Generation 0 ALSO honors the
        // legacy unscoped scope: the TeamClaims baseline import
        // lands pre-0.3.0 payments there, and that payment IS
        // generation 0 - frozen, never re-payable.
        String scope = "signature:" + kind.sealId() + ":" + generation;
        String priorScopeOwner = teamClaims.ledger.consumedScopes().get(scope);
        if (priorScopeOwner == null && generation == 0) {
            priorScopeOwner = teamClaims.ledger.consumedScopes().get("signature:" + kind.sealId());
        }
        if (run.equals(priorScopeOwner)) return; // Frozen original plan survives later name/date/config changes.
        if (priorScopeOwner != null) throw new IllegalStateException("signature already consumed by another run or baseline");
        java.util.List<SealLoot.PlannedStack> plan = reforged
                ? SealLoot.reforgedPlan(table, vanillaMaxLevels())
                : SealLoot.perChampionPlan(table);
        java.util.Map<UUID,java.util.List<wrv.rewards.TeamRewardLedger.RewardUnit>> distribution = new java.util.TreeMap<>();
        for (UUID owner : teamClaims.ledger.roster(run)) {
            java.util.List<wrv.rewards.TeamRewardLedger.RewardUnit> units = new java.util.ArrayList<>();
            int index = 0;
            for (SealLoot.PlannedStack planned : plan) {
                ItemStack stack = buildLootStack(planned.item());
                if (stack == null) throw new IllegalStateException("invalid personal signature recipe");
                if (planned.provenanceStamped()) {
                    String name = Bukkit.getOfflinePlayer(owner).getName();
                    stampProvenance(stack, name != null ? name : owner.toString(), reforged);
                }
                units.add(new wrv.rewards.TeamRewardLedger.RewardUnit("personal:" + owner + ":" + index, TeamClaims.encode(stack)));
                index++;
            }
            if (kind == BossKind.ENDER_DRAGON) {
                for (SealLoot.LootItem mat : SealLoot.dragonMaterials(dragonBreathBase, dragonCrystals)) {
                    ItemStack stack = buildLootStack(mat);
                    if (stack == null) throw new IllegalStateException("invalid dragon material recipe");
                    units.add(new wrv.rewards.TeamRewardLedger.RewardUnit(
                            dragonMaterialUnit(owner, mat.material()), TeamClaims.encode(stack)));
                }
            }
            distribution.put(owner, java.util.List.copyOf(units));
        }
        try {
            teamClaims.ledger.winOnceScope(run, scope,
                    reforged ? "signature-reforged-v1" : "signature-v1", distribution);
        } catch (java.io.IOException failure) {
            throw new IllegalStateException("Reward ledger write uncertain; no direct payout", failure);
        }
        java.util.List<UUID> paidOwners = distribution.entrySet().stream().filter(e -> !e.getValue().isEmpty()).map(java.util.Map.Entry::getKey).toList();
        try { teamClaims.offerMarkers(paidOwners,event.getEntity().getLocation()); }
        catch (RuntimeException markerFailure) { getLogger().warning("Personal chest fallback retained in reward menu."); }
        // 0.2.11 (B-2): the verdict itself presents the menu, once per batch.
        try { teamClaims.presentToOwners(paidOwners); }
        catch (RuntimeException presentFailure) { getLogger().warning("Reward auto-present skipped: " + presentFailure.getMessage()); }
        getLogger().info("BossTrial personal signature claims stored for " + distribution.size() + " qualified teammate(s).");
    }

    /**
     * 0.2.11 (B-3): store one dragon CREDIT kill's per-champion
     * materials (breath + crystals from config, read per-champion)
     * as personal claims on this run — the non-once ledger path, so
     * every CREDIT kill pays them again while the signature scope
     * stays consumed. Never touches the drops.
     */
    private DragonMaterials storeDragonMaterials(EntityDeathEvent event) {
        if (teamClaims == null) {
            getLogger().severe("Dragon material claims unavailable (team claims not initialised); no direct payout.");
            return DragonMaterials.FAILED;
        }
        String run = teamRun(event.getEntity(), BossKind.ENDER_DRAGON);
        java.util.Set<UUID> roster = teamClaims.ledger.roster(run);
        if (roster.isEmpty()) {
            return DragonMaterials.EMPTY_ROSTER;
        }
        java.util.Map<UUID,java.util.List<wrv.rewards.TeamRewardLedger.RewardUnit>> distribution = new java.util.TreeMap<>();
        for (UUID owner : roster) {
            java.util.List<wrv.rewards.TeamRewardLedger.RewardUnit> units = new java.util.ArrayList<>();
            for (SealLoot.LootItem mat : SealLoot.dragonMaterials(dragonBreathBase, dragonCrystals)) {
                ItemStack stack = buildLootStack(mat);
                if (stack == null) throw new IllegalStateException("invalid dragon material recipe");
                units.add(new wrv.rewards.TeamRewardLedger.RewardUnit(
                        dragonMaterialUnit(owner, mat.material()), TeamClaims.encode(stack)));
            }
            distribution.put(owner, java.util.List.copyOf(units));
        }
        try {
            teamClaims.ledger.win(run, "dragon-materials-v1", distribution);
        } catch (java.io.IOException failure) {
            getLogger().severe("Dragon material claim write uncertain; no direct payout: " + failure.getMessage());
            return DragonMaterials.FAILED;
        } catch (RuntimeException failure) {
            getLogger().severe("Dragon material claims need owner review; no direct payout: " + failure.getMessage());
            return DragonMaterials.FAILED;
        }
        java.util.List<UUID> paidOwners = distribution.entrySet().stream().filter(e -> !e.getValue().isEmpty()).map(java.util.Map.Entry::getKey).toList();
        try { teamClaims.offerMarkers(paidOwners,event.getEntity().getLocation()); }
        catch (RuntimeException markerFailure) { getLogger().warning("Personal chest fallback retained in reward menu."); }
        try { teamClaims.presentToOwners(paidOwners); }
        catch (RuntimeException presentFailure) { getLogger().warning("Reward auto-present skipped: " + presentFailure.getMessage()); }
        getLogger().info("BossTrial dragon material claims stored for " + distribution.size() + " champion(s).");
        return DragonMaterials.STORED;
    }

    private String teamRun(LivingEntity boss, BossKind kind) {
        return kind.sealId() + ":" + boss.getWorld().getUID() + ":" + boss.getUniqueId();
    }
    private String trioRun(LivingEntity boss) {
        String monument = monuments.monumentOf(boss.getUniqueId());
        if (monument == null) throw new IllegalStateException("unbound elder monument");
        return "trio:" + boss.getWorld().getUID() + ":" + monument;
    }
    private boolean prepareTeamVerdict(LivingEntity boss, BossKind kind, DeathOutcome outcome) {
        if (teamClaims == null) return false;
        try {
            String run = teamRun(boss, kind);
            // Unknown historical rosters cannot be fabricated at death.
            java.util.Set<UUID> members = teamClaims.ledger.roster(run);
            if (outcome.elderProgress) {
                // 0.3.0: a trio-progress elder contributes its roster
                // whatever verdict class the trio is being fought
                // under (first break or repeat); a progress kill is
                // never closed as a loss.
                if (kind == BossKind.ELDER_GUARDIAN) {
                    String trio = trioRun(boss);
                    teamClaims.ledger.creditComponentInto(run, trio);
                }
            } else if (outcome.verdict != CreditJudge.Verdict.CREDIT
                    && outcome.verdict != CreditJudge.Verdict.REPEAT_CREDIT) {
                teamClaims.ledger.closeWithoutWin(run, wrv.rewards.TeamRewardLedger.EncounterOutcome.LOST);
            } else if (kind == BossKind.ELDER_GUARDIAN) {
                // Only a credited elder contributes its roster to the trio. Denied respawn attempts do not.
                String trio = trioRun(boss);
                teamClaims.ledger.creditComponentInto(run, trio);
            }
            return true;
        } catch (Exception failure) {
            getLogger().severe("Team verdict persistence needs review; fight verdict unchanged: " + failure.getMessage());
            return false;
        }
    }
    private void observeTeam(LivingEntity boss, BossKind kind) {
        if (teamClaims == null) return;
        try {
            com.winkreyes.teamrewards.encounter.TeamAdmission.Kind teamKind = switch(kind) {
                case WITHER -> com.winkreyes.teamrewards.encounter.TeamAdmission.Kind.WITHER;
                case WARDEN -> com.winkreyes.teamrewards.encounter.TeamAdmission.Kind.WARDEN;
                case ENDER_DRAGON -> com.winkreyes.teamrewards.encounter.TeamAdmission.Kind.DRAGON;
                case ELDER_GUARDIAN -> com.winkreyes.teamrewards.encounter.TeamAdmission.Kind.ELDER_TRIO;
            };
            teamClaims.observe(teamRun(boss, kind), teamKind, boss, cfg.presenceRange);
        } catch (Exception failure) {
            getLogger().severe("Team participation could not be persisted; owner review required: " + failure.getMessage());
        }
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
        stampProvenance(stack, championName, false);
    }

    /** 0.3.0: a reforged signature carries the extra "Reforged"
     *  provenance line - visible, cosmetic, never hidden power. */
    private void stampProvenance(ItemStack stack, String championName, boolean reforged) {
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return;
        }
        List<String> lore = new ArrayList<>();
        for (String line : ProvenanceLore.lines(championName, LocalDate.now())) {
            lore.add(color("&7&o" + line));
        }
        if (reforged) {
            lore.add(color("&7&oReforged"));
        }
        meta.setLore(lore);
        stack.setItemMeta(meta);
    }

    /** Vanilla maximum level per enchantment key, for the reforge
     *  cap (vanilla max + 1). Built lazily from the registry. */
    private Map<String, Integer> vanillaMaxLevelsCache;

    private Map<String, Integer> vanillaMaxLevels() {
        if (vanillaMaxLevelsCache == null) {
            Map<String, Integer> map = new HashMap<>();
            for (Enchantment ench : Registry.ENCHANTMENT) {
                map.put(ench.getKey().getKey(), ench.getMaxLevel());
            }
            vanillaMaxLevelsCache = map;
        }
        return vanillaMaxLevelsCache;
    }

    /**
     * 0.2.11 (B-3): per-champion dragon XP. The death event's XP is
     * zeroed and every roster champion ONLINE at the kill receives
     * the full base award (12,000 first kill / 500 repeat, from
     * SpoilsMath at multiplier 1) straight onto their bar — no orbs,
     * no scooping race, no divided pool. Offline champions receive
     * nothing: XP cannot be banked into the claims ledger, which
     * holds items only, and the old pooled event XP would have
     * reached only whoever stood at the corpse anyway. Returns
     * false when no roster is on record, leaving the caller's
     * legacy spoils path to pay the event XP exactly as before.
     */
    private boolean applyDragonXpPerChampion(EntityDeathEvent event, EnderDragon dragon,
                                             boolean repeatKill) {
        if (teamClaims == null) {
            return false;
        }
        java.util.Set<UUID> roster = teamClaims.ledger.roster(teamRun(dragon, BossKind.ENDER_DRAGON));
        if (roster.isEmpty()) {
            return false;
        }
        DragonBattle battle = dragon.getWorld().getEnderDragonBattle();
        boolean firstKill = battle == null || !battle.hasBeenPreviouslyKilled();
        // 0.3.0 (SPEC 2.3): a REPEAT_CREDIT dragon kill always pays
        // the repeat base (500 x spoils) - the vanilla first-kill
        // flag can never promote a repeat clear to the 12,000 base.
        int base = SpoilsMath.dragonXp(!repeatKill && firstKill, 1);
        int vanilla = event.getDroppedExp();
        event.setDroppedExp(0);
        int paid = 0;
        for (UUID owner : roster) {
            Player champion = Bukkit.getPlayer(owner);
            if (champion != null && champion.isOnline()) {
                champion.giveExp(base);
                paid++;
            }
        }
        getLogger().info("Dragon XP spoils: firstKill=" + firstKill + ", death-event XP="
                + vanilla + " -> 0, per-champion award=" + base + " to " + paid
                + " online champion(s) of " + roster.size() + " on the roster.");
        return true;
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

    // ------------------------------------------------------------------
    // 0.3.0 repeat layer: bookkeeping, the slate, adapters, pressure
    // ------------------------------------------------------------------

    /** Turn every monument's COUNTED trio over to respawning
     *  (SPEC 2.4): repeat elder fights need fresh elders, and in
     *  0.2.11 a counted elder NEVER respawned. Runs on every trio
     *  completion while the repeat layer is on - the first break
     *  included, so generation 1 surfaces on the settled clock. */
    private void flipCountedElderTrios(long now) {
        if (!cfg.repeatEnabled) {
            return;
        }
        for (String mid : monuments.monumentsWithCountedTrio()) {
            int flipped = monuments.completeTrioRespawn(mid, now + cfg.elderRepeatRespawnMs);
            if (flipped > 0) {
                getLogger().info("BossTrial elder generations: monument " + mid
                        + "'s counted trio turns over - fresh elders surface in "
                        + CooldownBook.formatRemaining(cfg.elderRepeatRespawnMs) + ".");
                emitRespawnScheduled(mid, now + cfg.elderRepeatRespawnMs, flipped);
            }
        }
    }

    /** The one-time first-boot flip: a trio counted under 0.2.11
     *  (the live monument-4 case) never respawned either - when the
     *  elder seal stands broken at the first 0.3.0 boot, turn it over.
     *  Desk fix (Nov 1, 2026, after the first live attempt's
     *  boot-verify): the gate is the BROKEN SEAL, not the latch alone.
     *  The latch is a verdict shortcut that only the trio-completing
     *  CREDIT consequence sets; the live state carries a
     *  GuildGate-credited elder seal (BossTrial's own signature-paid
     *  record agrees) with elder-seal-approved still false, and gating
     *  on the latch alone left that counted trio unflippable forever.
     *  sealBroken() reads GuildGate first and falls back to this
     *  plugin's credit records, and a monument's trio can only stand
     *  COMPLETE behind a credited seal (counting runs on CREDIT /
     *  repeat verdicts only), so the wider gate fires exactly on
     *  post-credit residue - never on an unbroken seal's in-progress
     *  trio, whose counted elders keep their progress. */
    private void bootElderGenerationFlip() {
        if (!elderLatchStands() && !sealBroken(BossKind.ELDER_GUARDIAN)) {
            return;
        }
        flipCountedElderTrios(System.currentTimeMillis());
        saveState();
    }

    /** Persist the repeat bookkeeping for one REPEAT_CREDIT -
     *  window started, seal recorded, clear counts incremented -
     *  BEFORE any repeat reward moves (companion-spec discipline:
     *  a crash after this point can underpay, never double-pay). */
    private void recordRepeatClear(BossKind kind, DeathOutcome outcome) {
        long now = System.currentTimeMillis();
        repeatWindows.start(kind.sealId(), now, cfg.repeatWindowMs(kind));
        creditedSeals.add(kind.sealId());
        Map<UUID, Integer> counts = clearCounts.computeIfAbsent(kind.sealId(), k -> new HashMap<>());
        for (UUID champion : outcome.presentChampions) {
            counts.merge(champion, 1, Integer::sum);
        }
        saveState();
        getLogger().info("BossTrial repeat book [" + kind.displayName() + "]: window started ("
                + CooldownBook.formatRemaining(cfg.repeatWindowMs(kind)) + "), clear counts now "
                + counts + ".");
    }

    /** The repeat slate (SPEC section 4) for one chapter clear. */
    private void applyRepeatLayer(BossKind kind, EntityDeathEvent event, DeathOutcome outcome,
                                  boolean teamVerdictReady, EncounterLedger ledger) {
        int n = Math.max(1, outcome.presentParticipants);
        String sealId = kind.sealId();
        // Cash: every PRESENT champion, online or not (Vault pays
        // offline players natively).
        int cashPaid = 0;
        for (UUID id : outcome.presentChampions) {
            if (depositCash(Bukkit.getOfflinePlayer(id), cfg.repeatCashPerChampion)) {
                cashPaid++;
            }
        }
        // The claims roster for this kill (empty when team claims
        // are down - the cash/accelerant rows still pay).
        Set<UUID> roster = Set.of();
        if (teamVerdictReady && teamClaims != null) {
            String run = kind == BossKind.ELDER_GUARDIAN
                    ? trioRun(event.getEntity()) : teamRun(event.getEntity(), kind);
            roster = teamClaims.ledger.roster(run);
        }
        // Accelerants: roster champions who are online.
        int accelPaid = 0;
        for (UUID id : roster) {
            Player p = Bukkit.getPlayer(id);
            if (p == null || !p.isOnline()) {
                continue;
            }
            boolean any = grantMcMmoXp(p, com.winkreyes.excelbosstrial.core.Underdog
                    .scaledXp(cfg.repeatMcMmoXp, n));
            any |= grantMagicLevelXp(p, com.winkreyes.excelbosstrial.core.Underdog
                    .scaledXp(cfg.repeatMagicLevelXp, n));
            if (any) {
                accelPaid++;
            }
        }
        // War chest + cosmetics + titles + healer: claims-stored.
        if (teamClaims != null && !roster.isEmpty()) {
            try {
                storeRepeatClaims(kind, event, outcome, roster, ledger);
            } catch (Exception claimsFailure) {
                getLogger().severe("Repeat claims store needs review; cash/accelerants already paid stand: "
                        + claimsFailure.getMessage());
            }
        }
        getLogger().info("BossTrial repeat layer [" + kind.displayName() + "]: cash $"
                + cfg.repeatCashPerChampion + " to " + cashPaid + " champion(s), accelerants to "
                + accelPaid + ", underdog x"
                + String.format("%.2f", com.winkreyes.excelbosstrial.core.Underdog.factor(n)) + ".");
    }

    /** Store the claims-shaped rows of the repeat slate: war-chest
     *  services that resolve to items, the chapter flex cosmetic,
     *  clear-count title crossings, and the support commendation.
     *  Adapter services (DeathProtection premium, ClaimFly hours)
     *  pay DIRECTLY to online champions instead - a service credit
     *  is not an item, and a failed adapter skips its row loudly. */
    private void storeRepeatClaims(BossKind kind, EntityDeathEvent event, DeathOutcome outcome,
                                   Set<UUID> roster, EncounterLedger ledger) throws java.io.IOException {
        String baseRun = kind == BossKind.ELDER_GUARDIAN
                ? trioRun(event.getEntity()) : teamRun(event.getEntity(), kind);
        String run = baseRun + ":repeat-layer";
        teamClaims.ledger.openEncounter(run);
        Map<UUID, List<wrv.rewards.TeamRewardLedger.RewardUnit>> distribution = new java.util.TreeMap<>();
        int n = Math.max(1, outcome.presentParticipants);
        // The support champion (recognition only, SPEC 4.6).
        UUID healer = null;
        if (cfg.healerRecognitionEnabled && ledger != null) {
            healer = com.winkreyes.excelbosstrial.core.HealingAttribution.presenceProxy()
                    .supportChampion(ledger, System.currentTimeMillis()).orElse(null);
        }
        for (UUID owner : roster) {
            teamClaims.ledger.qualify(run, owner);
            List<wrv.rewards.TeamRewardLedger.RewardUnit> units = new ArrayList<>();
            int idx = 0;
            if (lostFoundVoucherArmed) {
                ItemStack voucher = buildNamedPaper(Material.PAPER,
                        "&fLost & Found Recovery Voucher",
                        List.of("&7Present this voucher at the Lost & Found",
                                "&7to recover one lost kit, free."));
                units.add(new wrv.rewards.TeamRewardLedger.RewardUnit(
                        "repeat:" + owner + ":" + (idx++), TeamClaims.encode(voucher)));
            }
            String cosmeticMat = repeatCosmetics.get(kind);
            if (cosmeticMat != null) {
                Material mat = Material.matchMaterial(cosmeticMat);
                if (mat != null) {
                    ItemStack cosmetic = new ItemStack(mat);
                    ItemMeta meta = cosmetic.getItemMeta();
                    if (meta != null) {
                        meta.setDisplayName(color("&d" + kind.displayName() + " Tribute"));
                        cosmetic.setItemMeta(meta);
                    }
                    units.add(new wrv.rewards.TeamRewardLedger.RewardUnit(
                            "repeat:" + owner + ":" + (idx++), TeamClaims.encode(cosmetic)));
                }
            }
            int count = clearCounts.getOrDefault(kind.sealId(), Map.of())
                    .getOrDefault(owner, 0);
            for (BossTrialConfig.TitleRow row : cfg.repeatTitles) {
                if (row.sealId().equals(kind.sealId()) && row.clears() == count && count > 0) {
                    ItemStack titleItem = buildNamedPaper(Material.NAME_TAG,
                            "&6" + row.title(),
                            List.of("&7Awarded for " + count + " repeat clears of the "
                                    + kind.displayName() + " seal.",
                                    "&7A flex, earned " + java.time.LocalDate.now() + "."));
                    units.add(new wrv.rewards.TeamRewardLedger.RewardUnit(
                            "repeat:" + owner + ":" + (idx++), TeamClaims.encode(titleItem)));
                    getLogger().info("BossTrial repeat titles: " + owner + " crosses "
                            + count + " " + kind.sealId() + " clears - title stored: "
                            + row.title());
                }
            }
            if (healer != null && healer.equals(owner)) {
                ItemStack commendation = buildNamedPaper(Material.PAPER,
                        "&fSupport Commendation",
                        List.of("&7For the champion who held the line",
                                "&7while others struck. The seal remembers."));
                units.add(new wrv.rewards.TeamRewardLedger.RewardUnit(
                        "repeat:" + owner + ":" + (idx++), TeamClaims.encode(commendation)));
                getLogger().info("BossTrial healer recognition [" + kind.displayName()
                        + "]: support commendation stored for " + owner + ".");
            }
            distribution.put(owner, List.copyOf(units));
        }
        teamClaims.ledger.win(run, "repeat-layer-v1", distribution);
        // Adapter services pay online champions directly.
        for (UUID owner : roster) {
            Player p = Bukkit.getPlayer(owner);
            if (p == null || !p.isOnline()) {
                continue;
            }
            if (cfg.repeatDeathProtectionPremium) {
                creditDeathProtectionPremium(p);
            }
            if (cfg.repeatClaimflyHours > 0) {
                grantClaimFlyHours(p, cfg.repeatClaimflyHours);
            }
        }
        List<UUID> paidOwners = distribution.entrySet().stream()
                .filter(e -> !e.getValue().isEmpty()).map(Map.Entry::getKey).toList();
        try { teamClaims.presentToOwners(paidOwners); }
        catch (RuntimeException presentFailure) {
            getLogger().warning("Repeat-claims auto-present skipped: " + presentFailure.getMessage());
        }
    }

    private ItemStack buildNamedPaper(Material material, String name, List<String> lore) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(color(name));
            List<String> colored = new ArrayList<>();
            for (String line : lore) {
                colored.add(color(line));
            }
            meta.setLore(colored);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    // --- Repeat adapters (house style: reflective, fail-soft,
    // --- a missing service skips its row with a log, never fakes).

    private Object vaultEconomy;
    private boolean vaultProbed;

    private boolean depositCash(org.bukkit.OfflinePlayer target, long amount) {
        if (amount <= 0) {
            return true;
        }
        if (!vaultProbed) {
            vaultProbed = true;
            try {
                Class<?> economyClass = Class.forName("net.milkbowl.vault.economy.Economy");
                var registration = Bukkit.getServicesManager().getRegistration(economyClass);
                if (registration != null) {
                    vaultEconomy = registration.getProvider();
                }
            } catch (Throwable ignored) {
                // no Vault: the row skips below
            }
            if (vaultEconomy == null) {
                getLogger().warning("Repeat rewards: Vault economy unavailable - cash rows skip.");
            }
        }
        if (vaultEconomy == null) {
            return false;
        }
        try {
            Object response = vaultEconomy.getClass()
                    .getMethod("depositPlayer", org.bukkit.OfflinePlayer.class, double.class)
                    .invoke(vaultEconomy, target, (double) amount);
            if (response != null) {
                try {
                    Object ok = response.getClass().getMethod("transactionSuccess").invoke(response);
                    if (Boolean.FALSE.equals(ok)) {
                        return false;
                    }
                } catch (NoSuchMethodException ignored) {
                    // a response without the success probe: the invoke standing is enough
                }
            }
            return true;
        } catch (Throwable failure) {
            getLogger().warning("Repeat rewards: Vault deposit failed for "
                    + target.getName() + ": " + failure.getMessage());
            return false;
        }
    }

    private boolean mcMmoProbed;
    private java.lang.reflect.Method mcMmoAddXp;
    private Object mcMmoSkill;
    private Class<?> mcMmoAmountType;

    /** Reflective-adapter numeric shapes: the neighbor APIs this
     *  build pays through do not agree on an amount type (mcMMO's
     *  addXP takes an int, ExcelMagicLevel's addXp a double), and
     *  Method.invoke performs no widening - a Double handed to an
     *  int parameter throws IllegalArgumentException. (Caught
     *  2026-11-01 by reading the mcMMO fork's ExperienceAPI: the
     *  original probe accepted the method, then would have failed
     *  every live grant at invoke time.) Probes therefore record
     *  the real parameter type and grants convert through here. */
    private static boolean isNumericType(Class<?> t) {
        return t == int.class || t == Integer.class || t == long.class || t == Long.class
                || t == double.class || t == Double.class || t == float.class || t == Float.class;
    }

    private static Object numericArg(Class<?> t, long amount) {
        if (t == int.class || t == Integer.class) {
            return (int) Math.min(amount, Integer.MAX_VALUE);
        }
        if (t == float.class || t == Float.class) {
            return (float) amount;
        }
        if (t == long.class || t == Long.class) {
            return amount;
        }
        return (double) amount;
    }

    /** mcMMO accelerant: probes ExperienceAPI once for a static
     *  addXP shape (Player, skill, number) - enum or String skill,
     *  any numeric amount type (the fork's is int) - and grants
     *  the configured combat skill (repeat.rewards.mcmmo-skill,
     *  default SWORDS). No shape = skip with a log. */
    private boolean grantMcMmoXp(Player player, long amount) {
        if (amount <= 0) {
            return true;
        }
        if (!mcMmoProbed) {
            mcMmoProbed = true;
            try {
                Class<?> api = Class.forName("com.gmail.nossr50.api.ExperienceAPI");
                String skillName = getConfig().getString("repeat.rewards.mcmmo-skill", "SWORDS");
                for (java.lang.reflect.Method m : api.getMethods()) {
                    if (!m.getName().equals("addXP") || m.getParameterCount() != 3
                            || !java.lang.reflect.Modifier.isStatic(m.getModifiers())
                            || !m.getParameterTypes()[0].isAssignableFrom(Player.class)
                            || !isNumericType(m.getParameterTypes()[2])) {
                        continue;
                    }
                    Class<?> skillType = m.getParameterTypes()[1];
                    if (skillType.isEnum()) {
                        mcMmoSkill = Enum.valueOf((Class<Enum>) skillType, skillName.toUpperCase());
                        mcMmoAddXp = m;
                        mcMmoAmountType = m.getParameterTypes()[2];
                        break;
                    }
                    if (skillType == String.class) {
                        mcMmoSkill = skillName;
                        mcMmoAddXp = m;
                        mcMmoAmountType = m.getParameterTypes()[2];
                        break;
                    }
                }
            } catch (Throwable ignored) {
                // no mcMMO API shape: the row skips below
            }
            if (mcMmoAddXp == null) {
                getLogger().warning("Repeat rewards: mcMMO ExperienceAPI.addXP shape not found - mcMMO accelerant rows skip.");
            }
        }
        if (mcMmoAddXp == null) {
            return false;
        }
        try {
            mcMmoAddXp.invoke(null, player, mcMmoSkill, numericArg(mcMmoAmountType, amount));
            return true;
        } catch (Throwable failure) {
            getLogger().warning("Repeat rewards: mcMMO XP grant failed for "
                    + player.getName() + ": " + failure.getMessage());
            return false;
        }
    }

    private boolean magicLevelProbed;
    private java.lang.reflect.Method magicLevelAddXp;
    private Class<?> magicLevelAmountType;

    /** ExcelMagicLevel accelerant: probes its public API for a
     *  static addXp(Player, number) (the settled shape is double;
     *  any numeric type resolves and converts). Absent plugin or
     *  API = skip with a log. */
    private boolean grantMagicLevelXp(Player player, long amount) {
        if (amount <= 0) {
            return true;
        }
        if (!magicLevelProbed) {
            magicLevelProbed = true;
            Plugin ml = Bukkit.getPluginManager().getPlugin("ExcelMagicLevel");
            if (ml != null) {
                try {
                    Class<?> api = Class.forName("com.winkreyes.excelmagiclevel.ExcelMagicLevelAPI",
                            true, ml.getClass().getClassLoader());
                    for (java.lang.reflect.Method m : api.getMethods()) {
                        if (m.getName().equals("addXp") && m.getParameterCount() == 2
                                && java.lang.reflect.Modifier.isStatic(m.getModifiers())
                                && m.getParameterTypes()[0].isAssignableFrom(Player.class)
                                && isNumericType(m.getParameterTypes()[1])) {
                            magicLevelAddXp = m;
                            magicLevelAmountType = m.getParameterTypes()[1];
                            break;
                        }
                    }
                } catch (Throwable ignored) {
                    // API moved: the row skips below
                }
            }
            if (magicLevelAddXp == null) {
                getLogger().warning("Repeat rewards: ExcelMagicLevel API unavailable - MagicLevel accelerant rows skip.");
            }
        }
        if (magicLevelAddXp == null) {
            return false;
        }
        try {
            Object r = magicLevelAddXp.invoke(null, player, numericArg(magicLevelAmountType, amount));
            return !Boolean.FALSE.equals(r);
        } catch (Throwable failure) {
            getLogger().warning("Repeat rewards: MagicLevel XP grant failed for "
                    + player.getName() + ": " + failure.getMessage());
            return false;
        }
    }

    /** DeathProtection premium row: probes the DeathProtection
     *  plugin for a premium-credit seam. ExcelDeathProtection
     *  0.3.x exposes none at build time (see TEST-REPORT) - the
     *  probe resolves no method, the row skips with one log line,
     *  and the seam is ready the day the API exists. */
    private boolean deathProtectionProbed;
    private java.lang.reflect.Method deathProtectionCredit;

    private boolean creditDeathProtectionPremium(Player player) {
        if (!deathProtectionProbed) {
            deathProtectionProbed = true;
            Plugin dp = Bukkit.getPluginManager().getPlugin("ExcelDeathProtection");
            if (dp != null) {
                for (String className : new String[] {
                        "com.winkreyes.exceldeathprotection.ExcelDeathProtectionAPI",
                        "com.winkreyes.exceldeathprotection.DeathProtectionAPI"}) {
                    try {
                        Class<?> api = Class.forName(className, true, dp.getClass().getClassLoader());
                        for (java.lang.reflect.Method m : api.getMethods()) {
                            if ((m.getName().equals("creditPremium") || m.getName().equals("grantPremium")
                                    || m.getName().equals("addPremium")) && m.getParameterCount() >= 1
                                    && m.getParameterTypes()[0].isAssignableFrom(Player.class)) {
                                deathProtectionCredit = m;
                                break;
                            }
                        }
                    } catch (Throwable ignored) {
                        // try the next candidate
                    }
                    if (deathProtectionCredit != null) {
                        break;
                    }
                }
            }
            if (deathProtectionCredit == null) {
                getLogger().warning("Repeat rewards: ExcelDeathProtection exposes no premium-credit API - "
                        + "the war-chest premium row skips (adapter seam ready).");
            }
        }
        if (deathProtectionCredit == null) {
            return false;
        }
        try {
            Object r = deathProtectionCredit.getParameterCount() == 1
                    ? deathProtectionCredit.invoke(null, player)
                    : deathProtectionCredit.invoke(null, player, 1);
            return !Boolean.FALSE.equals(r);
        } catch (Throwable failure) {
            getLogger().warning("Repeat rewards: DeathProtection premium credit failed for "
                    + player.getName() + ": " + failure.getMessage());
            return false;
        }
    }

    /** ClaimFly hours row: probes the ClaimFly plugin for a
     *  grant-hours seam under its known class names; absent seam =
     *  skip with a log (same posture as the premium row). */
    private boolean claimFlyProbed;
    private java.lang.reflect.Method claimFlyGrant;
    private Object claimFlyInstance;

    private boolean grantClaimFlyHours(Player player, double hours) {
        if (!claimFlyProbed) {
            claimFlyProbed = true;
            Plugin cf = Bukkit.getPluginManager().getPlugin("ExcelClaimFly");
            if (cf == null) {
                cf = Bukkit.getPluginManager().getPlugin("ClaimFly");
            }
            if (cf != null) {
                claimFlyInstance = cf;
                for (String className : new String[] {
                        "com.winkreyes.excelclaimfly.ExcelClaimFlyAPI",
                        "com.winkreyes.excelclaimfly.ClaimFlyAPI"}) {
                    try {
                        Class<?> api = Class.forName(className, true, cf.getClass().getClassLoader());
                        for (java.lang.reflect.Method m : api.getMethods()) {
                            if ((m.getName().equals("grantHours") || m.getName().equals("addHours")
                                    || m.getName().equals("grantFlyHours"))
                                    && m.getParameterCount() == 2
                                    && m.getParameterTypes()[0].isAssignableFrom(Player.class)) {
                                claimFlyGrant = m;
                                break;
                            }
                        }
                    } catch (Throwable ignored) {
                        // try the next candidate
                    }
                    if (claimFlyGrant != null) {
                        break;
                    }
                }
            }
            if (claimFlyGrant == null) {
                getLogger().warning("Repeat rewards: ClaimFly exposes no grant-hours API - "
                        + "the war-chest ClaimFly row skips (adapter seam ready).");
            }
        }
        if (claimFlyGrant == null) {
            return false;
        }
        try {
            Object target = java.lang.reflect.Modifier.isStatic(claimFlyGrant.getModifiers())
                    ? null : claimFlyInstance;
            Object r = claimFlyGrant.invoke(target, player, hours);
            return !Boolean.FALSE.equals(r);
        } catch (Throwable failure) {
            getLogger().warning("Repeat rewards: ClaimFly hours grant failed for "
                    + player.getName() + ": " + failure.getMessage());
            return false;
        }
    }

    // --- Attrition pressure (SPEC section 3): outgoing scale at
    // --- HIGH plus the MAGIC pulse on the 1 s task.

    /** Boss -> player damage scale + ramp, judged with the same
     *  math the unit tests pin (PressureMath). The dragon chapter
     *  multiplies its own phase-2 factor natively; this handler
     *  still applies to it (its settled outgoing default is 1.0,
     *  so the composition is the chapter's own - SPEC 3.1).
     *
     *  0.3.5: spike reflect from a tracked seal elder is judged
     *  FIRST, by ReflectRule, and never reaches the pressure
     *  scale - reflect is not the boss's offense, and the ramp
     *  used to double it by about minute 17 of an elder fight.
     *  The identification is exact: guardian spikes and the
     *  Thorns enchantment share the THORNS cause, but a spike
     *  hit's damager IS the elder itself, and this handler only
     *  runs when that damager resolved to a boss kind with an
     *  open encounter ledger - so vanilla guardians outside an
     *  encounter, sprinkle adds, player Thorns armor (which
     *  fires in the opposite direction) and every other chapter
     *  are untouched.
     *
     *  0.3.7: the scale itself is computed by currentOutgoingScale
     *  below - the identical gates and arithmetic, extracted so
     *  the ExcelBossTrialAPI getter reports the exact number this
     *  handler applies (the two can never drift). */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBossPressureDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) {
            return;
        }
        if (!(event.getDamager() instanceof LivingEntity boss)) {
            return;
        }
        BossKind kind = Attribution.kindOf(boss);
        if (kind == null || !chapterOn(kind)) {
            return;
        }
        EncounterLedger ledger = encounters.get(boss.getUniqueId());
        if (ledger == null) {
            return;
        }
        if (com.winkreyes.excelbosstrial.core.ReflectRule.isSpikeReflect(
                event.getCause() == EntityDamageEvent.DamageCause.THORNS,
                true, kind)) {
            double scaled = com.winkreyes.excelbosstrial.core.ReflectRule.scaledDamage(
                    event.getDamage(), cfg.reflect(kind).multiplier());
            if (com.winkreyes.excelbosstrial.core.ReflectRule.cancels(scaled)) {
                // Cancelled, not zeroed: the MONITOR recorder
                // (ignoreCancelled) skips the event, so reflect
                // leaves the fight table's damage-taken entirely.
                event.setCancelled(true);
            } else {
                event.setDamage(scaled);
            }
            return;
        }
        double scale = currentOutgoingScale(boss);
        if (scale != 1.0) {
            event.setDamage(event.getDamage() * scale);
        }
    }

    /** BossTrial's CURRENT outgoing scale for a boss entity
     *  (0.3.7, TW-PRESSURE-CHECK fix shape 2): its chapter's
     *  outgoing-multiplier under the attrition ramp - the exact
     *  arithmetic onBossPressureDamage applied inline through
     *  0.3.6, extracted unchanged so the handler and the
     *  ExcelBossTrialAPI getter (bound to this method in
     *  onEnable) can never drift. 1.0 when no scale applies:
     *  a non-boss entity, a chapter that is off, no open
     *  encounter for the entity, or the chapter's pressure
     *  disabled. The dragon chapter's native phase-2 factor is
     *  NOT part of this number (its pressure outgoing settles
     *  at 1.0, so a dragon reads its ramp only). */
    private double currentOutgoingScale(LivingEntity boss) {
        return currentOutgoingScale(boss, System.currentTimeMillis());
    }

    /** The nowMs-parameterized form, kept package-private for
     *  tests: "current" answered against an explicit clock. */
    double currentOutgoingScale(LivingEntity boss, long nowMs) {
        if (boss == null) {
            return 1.0;
        }
        BossKind kind = Attribution.kindOf(boss);
        if (kind == null || !chapterOn(kind)) {
            return 1.0;
        }
        EncounterLedger ledger = encounters.get(boss.getUniqueId());
        if (ledger == null) {
            return 1.0;
        }
        BossTrialConfig.Pressure pressure = cfg.pressure(kind);
        if (!pressure.enabled()) {
            return 1.0;
        }
        long firstDealt = ledger.firstDealtMs();
        double elapsedMinutes = firstDealt > 0 ? (nowMs - firstDealt) / 60_000.0 : 0;
        return com.winkreyes.excelbosstrial.core.PressureMath.outgoingScale(
                pressure.outgoingMultiplier(), elapsedMinutes,
                pressure.rampPerMinute(), pressure.rampMax());
    }

    /** The 1-second attrition pulse over every open encounter:
     *  fixed MAGIC damage through the real pipeline (armor and
     *  resistances apply - it is pressure, not a bypass) to hot,
     *  in-range participants not reviewing rewards. */
    private void pressurePulseTick() {
        long now = System.currentTimeMillis();
        for (Map.Entry<UUID, EncounterLedger> entry : new ArrayList<>(encounters.entrySet())) {
            EncounterLedger ledger = entry.getValue();
            BossKind kind = ledger.kind();
            if (kind == null || !chapterOn(kind)) {
                continue;
            }
            // 0.3.6: the feedback sweep rides this same 1-second
            // walk (no new repeating task): cadence-gated title
            // refresh + audience reconciliation per open bar.
            if (feedback != null) {
                org.bukkit.entity.Entity feedbackEntity = Bukkit.getEntity(entry.getKey());
                if (feedbackEntity instanceof LivingEntity feedbackBoss) {
                    feedback.reconcile(feedbackBoss, kind, ledger, now);
                }
            }
            // 0.4.0: heal reconciliation, the verdict and the
            // mid-fight record ride this same 1-second walk, and
            // the sprinkle's timed cadence with them (PM-1/PM-2/
            // PM-6/PM-8) - still no new repeating task.
            {
                org.bukkit.entity.Entity tickEntity = Bukkit.getEntity(entry.getKey());
                if (tickEntity instanceof LivingEntity tickBoss) {
                    reconcileEncounter(tickBoss, kind, ledger, now);
                    if (sprinkle != null) {
                        sprinkle.cadenceTick(kind, tickBoss, now);
                    }
                }
            }
            BossTrialConfig.Pressure pressure = cfg.pressure(kind);
            if (!pressure.enabled() || pressure.pulseDamage() <= 0) {
                continue;
            }
            org.bukkit.entity.Entity bossEntity = Bukkit.getEntity(entry.getKey());
            if (!(bossEntity instanceof LivingEntity boss) || boss.isDead()) {
                continue;
            }
            long firstDealt = ledger.firstDealtMs();
            double elapsedMinutes = firstDealt > 0 ? (now - firstDealt) / 60_000.0 : 0;
            // 0.3.2: a queued milestone surge rides this same task -
            // one extra pulse at the chapter's pulse-damage times
            // the milestone surge multiplier, under the same ramp,
            // to the same hot participants. It does NOT touch
            // lastPulseMs: the regular cadence is undisturbed.
            if (ledger.hasPendingSurge()) {
                ledger.consumeSurge();
                MilestoneSettings ms = milestoneSettings.get(kind);
                double mult = ms == null ? 1.0 : ms.surgeMultiplier();
                if (mult > 0) {
                    double surgeAmount = com.winkreyes.excelbosstrial.core.PressureMath
                            .pulseAmount(pressure.pulseDamage() * mult, elapsedMinutes,
                                    pressure.rampPerMinute(), pressure.rampMax());
                    int hit = applyPulse(ledger, boss, pressure, surgeAmount, now);
                    getLogger().info("BossTrial milestone surge [" + kind.displayName()
                            + "]: " + fmt(surgeAmount) + " MAGIC to " + hit
                            + " hot participant(s).");
                }
            }
            if (!com.winkreyes.excelbosstrial.core.PressureMath.pulseDue(
                    ledger.lastCountedMs(), ledger.lastTakenMs(), ledger.lastPulseMs(),
                    now, pressure.pulseIntervalSeconds() * 1000L,
                    pressure.pulseHotSeconds() * 1000L)) {
                continue;
            }
            double pulseAmount = com.winkreyes.excelbosstrial.core.PressureMath.pulseAmount(
                    pressure.pulseDamage(), elapsedMinutes,
                    pressure.rampPerMinute(), pressure.rampMax());
            int hit = applyPulse(ledger, boss, pressure, pulseAmount, now);
            if (hit > 0) {
                ledger.markPulse(now);
            }
        }
    }

    /** One pulse application to every hot participant (shared by
     *  the regular attrition pulse and the 0.3.2 milestone surge):
     *  hot = seen in the fight within the window (a counted dealt
     *  blow or a taken blow stamps it), online, alive, in a real
     *  game mode, same world, within 128 blocks, and not inside a
     *  team-claims review frame. Returns how many were hit. */
    private int applyPulse(EncounterLedger ledger, LivingEntity boss,
                           BossTrialConfig.Pressure pressure, double amount, long now) {
        int hit = 0;
        for (UUID participant : ledger.participants()) {
            if (!com.winkreyes.excelbosstrial.core.PressureMath.hot(
                    ledger.lastSeenMs(participant), ledger.lastSeenMs(participant),
                    now, pressure.pulseHotSeconds() * 1000L)) {
                continue;
            }
            Player p = Bukkit.getPlayer(participant);
            if (p == null || !p.isOnline() || p.isDead()) {
                continue;
            }
            if (p.getGameMode() != org.bukkit.GameMode.SURVIVAL
                    && p.getGameMode() != org.bukkit.GameMode.ADVENTURE) {
                continue;
            }
            if (!p.getWorld().equals(boss.getWorld())
                    || p.getLocation().distanceSquared(boss.getLocation()) > 128.0 * 128.0) {
                continue;
            }
            if (teamClaims != null && teamClaims.inReviewFrame(participant)) {
                continue;
            }
            // 0.3.7: the pulse honors TrainingWheels (fix shape
            // 1). The pulse is source-less MAGIC damage, so
            // TrainingWheels' own listener never sees it - the
            // protected player's factor is applied HERE, read
            // through the seam, at this one call site (the
            // regular pulse and the milestone surge both ride
            // applyPulse, so both are fixed). Toggle off (or no
            // seam) = the exact pre-0.3.7 full pulse.
            double amt = amount;
            if (amount > 0 && pressure.pulseRespectTrainingWheels() && twShield != null) {
                amt = com.winkreyes.excelbosstrial.core.PulseShield.shieldedAmount(amount,
                        twShield.factorFor(participant));
            }
            damageMagic(p, amt);
            hit++;
        }
        return hit;
    }

    /** One MAGIC damage application: a typed DamageSource so the
     *  hit flows through the real pipeline; falls back to plain
     *  damage() if the registry path is unavailable. */
    private void damageMagic(Player player, double amount) {
        try {
            org.bukkit.damage.DamageSource source = org.bukkit.damage.DamageSource.builder(
                    io.papermc.paper.registry.RegistryAccess.registryAccess()
                            .getRegistry(io.papermc.paper.registry.RegistryKey.DAMAGE_TYPE)
                            .getOrThrow(io.papermc.paper.registry.keys.DamageTypeKeys.MAGIC))
                    .build();
            player.damage(amount, source);
        } catch (Throwable unavailable) {
            player.damage(amount);
        }
    }

    // --- Raid companion pay paths (called by RaidRewardAdapter).

    /** First-break raid signature: the minted Village Standard,
     *  one per winner, stored as a personal claim (waits out full
     *  inventories and offline winners by design). */
    void raidBreakPay(List<UUID> winners, int omenLevel) {
        if (teamClaims == null) {
            getLogger().severe("Raid signature cannot be stored: team claims unavailable.");
            return;
        }
        int stored = 0;
        for (UUID winner : winners) {
            try {
                ItemStack shield = raidStandard != null
                        ? raidStandard.buildStandard() : new ItemStack(Material.SHIELD);
                teamClaims.grantClaim(winner, List.of(shield));
                stored++;
            } catch (Exception failure) {
                getLogger().severe("Raid signature store failed for " + winner
                        + ": " + failure.getMessage());
            }
        }
        getLogger().info("Raid signature: Village Standard stored for " + stored
                + " winner(s) at omen " + omenLevel + ".");
    }

    /** Raid repeat slate (SPEC 4 as applied to the raid): cash,
     *  accelerants, the raid jackpot through claims, war-chest
     *  services, the raid flex cosmetic, and raid clear titles. */
    void raidRepeatPay(List<UUID> winners, Map<UUID, Integer> clearCountsAfter) {
        int n = Math.max(1, winners.size());
        for (UUID winner : winners) {
            depositCash(Bukkit.getOfflinePlayer(winner), cfg.repeatCashPerChampion);
            Player p = Bukkit.getPlayer(winner);
            if (p != null && p.isOnline()) {
                grantMcMmoXp(p, com.winkreyes.excelbosstrial.core.Underdog
                        .scaledXp(cfg.repeatMcMmoXp, n));
                grantMagicLevelXp(p, com.winkreyes.excelbosstrial.core.Underdog
                        .scaledXp(cfg.repeatMagicLevelXp, n));
                if (cfg.repeatDeathProtectionPremium) {
                    creditDeathProtectionPremium(p);
                }
                if (cfg.repeatClaimflyHours > 0) {
                    grantClaimFlyHours(p, cfg.repeatClaimflyHours);
                }
            }
        }
        if (teamClaims == null) {
            return;
        }
        int rolls = SealLoot.repeatRolls(n, cfg.repeatJackpotExtraRolls);
        for (UUID winner : winners) {
            List<ItemStack> items = new ArrayList<>();
            if (rolls > 0 && !raidLootTable.bonusAndJackpot().isEmpty()) {
                for (SealLoot.RolledBonus r : SealLoot.rollBonus(raidLootTable, rolls, lootRng)) {
                    Material mat = Material.matchMaterial(r.material());
                    if (mat != null) {
                        items.add(new ItemStack(mat, com.winkreyes.excelbosstrial.core.Underdog
                                .scaledCount(r.count(), n)));
                    }
                }
            }
            String cosmeticMat = getConfig().getString("repeat.cosmetics.raid");
            if (cosmeticMat != null && Material.matchMaterial(cosmeticMat) != null) {
                ItemStack cosmetic = new ItemStack(Material.matchMaterial(cosmeticMat));
                ItemMeta meta = cosmetic.getItemMeta();
                if (meta != null) {
                    meta.setDisplayName(color("&dVillage Tribute"));
                    cosmetic.setItemMeta(meta);
                }
                items.add(cosmetic);
            }
            int count = clearCountsAfter.getOrDefault(winner, 0);
            for (BossTrialConfig.TitleRow row : cfg.repeatTitles) {
                if (row.sealId().equals("raid") && row.clears() == count && count > 0) {
                    items.add(buildNamedPaper(Material.NAME_TAG, "&6" + row.title(),
                            List.of("&7Awarded for " + count + " repeat clears of the raid seal.",
                                    "&7A flex, earned " + java.time.LocalDate.now() + ".")));
                }
            }
            if (!items.isEmpty()) {
                try {
                    teamClaims.grantClaim(winner, items);
                } catch (Exception failure) {
                    getLogger().severe("Raid repeat claims store failed for " + winner
                            + ": " + failure.getMessage());
                }
            }
        }
        getLogger().info("Raid repeat slate paid: " + winners.size() + " winner(s), underdog x"
                + String.format("%.2f", com.winkreyes.excelbosstrial.core.Underdog.factor(n)) + ".");
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
                creditedSeals.add(kind.sealId());
                if (announceWindowActive(now)) {
                    broadcast(fill(msgCreditHeadline, kind, outcome));
                }
                if (kind == BossKind.ELDER_GUARDIAN) {
                    broadcast(fill(msgElderTrio, kind, outcome));
                    elderSealApproved = true;
                    flipCountedElderTrios(now);
                }
                getLogger().info("BossTrial verdict CREDIT: " + seal + " seal earned ("
                        + fmt(outcome.sharePercent) + "% player damage, "
                        + outcome.presentParticipants + " champion(s) standing).");
            }
            case REPEAT_CREDIT -> {
                // The window + clear counts were already persisted
                // by recordRepeatClear, before any reward moved.
                if (cfg.repeatAnnounce && announceWindowActive(now)) {
                    broadcast(fill(msgRepeatCreditHeadline, kind, outcome));
                }
                if (kind == BossKind.ELDER_GUARDIAN) {
                    flipCountedElderTrios(now);
                }
                getLogger().info("BossTrial verdict REPEAT_CREDIT: " + seal
                        + " repeat clear (" + fmt(outcome.sharePercent) + "% player damage, "
                        + outcome.presentParticipants + " champion(s) standing); repeat spoils "
                        + "rest for " + CooldownBook.formatRemaining(cfg.repeatWindowMs(kind)) + ".");
            }
            case DENY_REPEAT_WINDOW -> {
                String left = CooldownBook.formatRemaining(
                        repeatWindows.remainingMs(kind.sealId(), now));
                if (announceWindowActive(now)) {
                    broadcast(fill(msgDenyHeadline, kind, outcome));
                }
                broadcast(fill(msgDenyRepeatWindow, kind, outcome).replace("{left}", left));
                if (kind == BossKind.ELDER_GUARDIAN) {
                    // The trio still completed and still turns
                    // over - only the repeat payment waits.
                    flipCountedElderTrios(now);
                }
                getLogger().info("BossTrial verdict DENY_REPEAT_WINDOW: " + seal
                        + " repeat spoils still resting (" + left + " left; "
                        + fmt(outcome.sharePercent) + "% player damage). No cooldown started.");
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
                    emitRespawnScheduled(monuments.monumentOf(boss.getUniqueId()),
                            now + cfg.elderRespawnMs, 1);
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
                    emitRespawnScheduled(monuments.monumentOf(boss.getUniqueId()),
                            now + cfg.elderRespawnMs, 1);
                }
                getLogger().info("BossTrial verdict DENY_COOLDOWN: " + seal
                        + " kill was fought for but the seal is cooling down ("
                        + CooldownBook.formatRemaining(
                                cooldowns.remainingMs(kind.sealId(), now)) + " left).");
            }
        }
        saveState();
    }

    // ------------------------------------------------------------------
    // 0.3.1: the fight record (per-player damage + healing tables)
    // ------------------------------------------------------------------

    /** Close-cause labels for verdict-less endings (0.4.0,
     *  PM-8): a verdict-less close is a RECORDED close, never an
     *  evaporation - the Nov 7 reset deleted the Wither fight's
     *  only per-player table minutes after the party asked for
     *  it. CREDIT / DENIED / elder-progress labels keep coming
     *  from the verdict itself. */
    static final String LABEL_NO_VERDICT = "CLOSED_NO_VERDICT";
    static final String LABEL_GIVE_UP = "GIVE_UP";
    static final String LABEL_ADMIN_RESET = "ADMIN_RESET";
    static final String LABEL_SEAL_GRANTED = "SEAL_GRANTED";
    static final String LABEL_SERVER_STOP = "SERVER_STOP";
    static final String LABEL_IN_PROGRESS = "IN_PROGRESS";

    /**
     * Snapshot the fight record for a dying boss: the ledger's
     * per-player damage table plus the tracker's healing table,
     * written to fights/&lt;seal&gt;-&lt;timestamp&gt;-&lt;boss&gt;.yml
     * and to the log in full. Runs at every verdict and every
     * elder trio progress point (both pass through onBossDeath).
     * Self-guarding: any failure logs a warning and the encounter's
     * tracking state is still closed - the verdict path that
     * follows never hears about it.
     */
    private void snapshotFightRecord(BossKind kind, LivingEntity boss,
                                     EncounterLedger ledger, DeathOutcome outcome,
                                     EntityDeathEvent event) {
        Player killerPlayer = event.getEntity().getKiller();
        UUID killer = killerPlayer != null ? killerPlayer.getUniqueId() : null;
        String label = outcome.elderProgress
                ? "ELDER_PROGRESS " + outcome.elderProgressNow + " of 3"
                : outcome.verdict.name();
        snapshotCloseRecord(kind, boss.getUniqueId(), ledger, label, killer,
                outcome.presentChampions, outcome);
    }

    /**
     * The one close-record writer (0.4.0, PM-8): EVERY encounter
     * ending writes the fight record first - verdict (through
     * {@link #snapshotFightRecord}), prune, warden give-up,
     * admin reset, seal grant, server stop. A non-verdict close
     * passes a null outcome: the record carries its close-cause
     * label and the Protect publish is the boss_state close
     * record only - a verdict-less ending never poses as a seal
     * verdict in Protect's audit family.
     */
    private void snapshotCloseRecord(BossKind kind, UUID bossId,
                                     EncounterLedger ledger, String label,
                                     UUID killer, List<UUID> presentChampions,
                                     DeathOutcome outcome) {
        try {
            if (fightTrackingEnabled) {
                FightTracker.FightRecord rec = buildRecord(kind, bossId, ledger,
                        label, killer, presentChampions);
                if (rec != null) {
                    File dir = new File(getDataFolder(), "fights");
                    dir.mkdirs();
                    String fileName = rec.fileName();
                    java.nio.file.Files.writeString(
                            new File(dir, fileName).toPath(), rec.toYaml());
                    // The ending's record supersedes the mid-fight
                    // stable file (PM-8's 60-second cadence).
                    new File(dir, rec.stableFileName()).delete();
                    for (String line : rec.logLines(fileName)) {
                        getLogger().info(line);
                    }
                    pruneFightRecords(dir);
                    if (outcome != null) {
                        emitProtectPublish(rec, outcome, label, fileName);
                    } else {
                        emitProtectCloseState(rec, label, fileName);
                    }
                }
            }
        } catch (Exception failure) {
            getLogger().warning("Fight record failed (the encounter is unaffected): "
                    + failure);
        } finally {
            fightTracker.closeEncounter(bossId);
            elderTrioMult.remove(bossId);
        }
    }

    /**
     * The mid-fight snapshot (0.4.0, PM-8): every 60 seconds per
     * open encounter the record is re-written to the encounter's
     * STABLE filename, so even a hard crash - where no ending
     * code runs at all - leaves a current per-player table. File
     * only: no log lines (the ending's record carries those), no
     * publish, and the tracker stays open.
     */
    private void snapshotMidFight(BossKind kind, UUID bossId, EncounterLedger ledger) {
        try {
            if (!fightTrackingEnabled) {
                return;
            }
            FightTracker.FightRecord rec = buildRecord(kind, bossId, ledger,
                    LABEL_IN_PROGRESS, null, List.of());
            if (rec == null) {
                return;
            }
            File dir = new File(getDataFolder(), "fights");
            dir.mkdirs();
            java.nio.file.Files.writeString(
                    new File(dir, rec.stableFileName()).toPath(), rec.toYaml());
        } catch (Exception failure) {
            getLogger().warning("Mid-fight record snapshot failed (the fight "
                    + "is unaffected): " + failure);
        }
    }

    /** Build one fight record from the live ledger + tracker. */
    private FightTracker.FightRecord buildRecord(BossKind kind, UUID bossId,
                                                 EncounterLedger ledger, String label,
                                                 UUID killer,
                                                 List<UUID> presentChampions) {
        Set<UUID> everyone = new HashSet<>();
        if (ledger != null) {
            everyone.addAll(ledger.participants());
        }
        everyone.addAll(fightTracker.roster(bossId));
        if (presentChampions != null) {
            everyone.addAll(presentChampions);
        }
        if (killer != null) {
            everyone.add(killer);
        }
        Map<UUID, String> names = new HashMap<>();
        for (UUID u : everyone) {
            org.bukkit.OfflinePlayer op = Bukkit.getOfflinePlayer(u);
            names.put(u, op.getName() != null ? op.getName() : u.toString());
        }
        String context = kind == BossKind.ELDER_GUARDIAN
                ? monuments.monumentOf(bossId) : null;
        return fightTracker.snapshot(bossId, ledger, label, killer,
                presentChampions != null ? presentChampions : List.of(), names,
                healClaimWindowMs, System.currentTimeMillis(), context);
    }

    /** 0.4.0: a verdict-less close publishes only its boss_state
     *  close record to ExcelProtect (the fightEnded shape) - the
     *  closing reference for status readers, with no fight-table
     *  verdict attached. */
    private void emitProtectCloseState(FightTracker.FightRecord rec, String label,
                                       String fileName) {
        try {
            if (protectEmitSettings.stateChanges()) {
                protectEmit.record(ProtectTableShape.STREAM,
                        ProtectTableShape.TYPE_BOSS_STATE,
                        ProtectTableShape.fightEnded(rec, label, "fights/" + fileName));
            }
        } catch (Throwable failure) {
            getLogger().warning("Protect publish failed (the record is unaffected): "
                    + failure);
        }
    }

    /**
     * 0.3.4: publish one finished fight record to ExcelProtect.
     * A true verdict rides the contract's fight-table event
     * (Protect writes its combat fight_summary + audit
     * seal_verdict from it). An elder progress point is NOT a
     * verdict, so it rides the general publish door as a
     * fight_progress record instead - no progress point ever
     * poses as a seal verdict in Protect's audit family. The
     * closing boss_state record carries the verdict reference
     * (the fights/ file) for status readers. The verdict publish
     * rides the fight record: with fight tracking off there is
     * no table to publish, and boss_state fight_ended rides with
     * it (fight_started / respawn states are independent).
     */
    private void emitProtectPublish(FightTracker.FightRecord rec, DeathOutcome outcome,
                                    String label, String fileName) {
        try {
            String recordRef = "fights/" + fileName;
            if (protectEmitSettings.fightTables()) {
                if (outcome.elderProgress) {
                    protectEmit.record(ProtectTableShape.STREAM,
                            ProtectTableShape.TYPE_FIGHT_PROGRESS,
                            ProtectTableShape.fightProgress(rec, label,
                                    outcome.elderProgressNow, 3, recordRef));
                } else {
                    protectEmit.fightTable(rec.kind.sealId(), outcome.verdict.name(),
                            ProtectTableShape.creditOf(rec), rec.pool,
                            (double) rec.durationSeconds(),
                            ProtectTableShape.rows(rec));
                }
            }
            if (protectEmitSettings.stateChanges()) {
                protectEmit.record(ProtectTableShape.STREAM,
                        ProtectTableShape.TYPE_BOSS_STATE,
                        ProtectTableShape.fightEnded(rec, label, recordRef));
            }
        } catch (Throwable failure) {
            getLogger().warning("Protect publish failed (the verdict is unaffected): "
                    + failure);
        }
    }

    /** 0.3.4: elder(s) went on the respawn clock - an unearned
     *  kill (elders = 1) or a counted trio turning over (elders =
     *  the number flipped). The boss-state feed hears when the
     *  chapter is due back. */
    private void emitRespawnScheduled(String monumentId, long dueMs, int elders) {
        if (!protectEmitSettings.stateChanges()) {
            return;
        }
        try {
            protectEmit.record(ProtectTableShape.STREAM,
                    ProtectTableShape.TYPE_BOSS_STATE,
                    ProtectTableShape.respawnScheduled(monumentId, dueMs, elders));
        } catch (Throwable failure) {
            getLogger().warning("Protect publish failed (respawn bookkeeping "
                    + "is unaffected): " + failure);
        }
    }

    /** Keep only the newest fight-record files (the file names
     *  sort chronologically); retention 0 keeps everything. */
    private void pruneFightRecords(File dir) {
        if (fightRecordRetention <= 0) {
            return;
        }
        File[] files = dir.listFiles((d, name) -> name.endsWith(".yml"));
        if (files == null || files.length <= fightRecordRetention) {
            return;
        }
        java.util.Arrays.sort(files, java.util.Comparator.comparing(File::getName));
        for (int i = 0; i < files.length - fightRecordRetention; i++) {
            files[i].delete();
        }
    }

    /**
     * Healing received, from Bukkit's own regain events: every
     * heal that lands on a player while a governed encounter they
     * belong to is open is recorded (effective only - the tracker
     * caps at the health actually missing). Belonging means: a
     * ledger participant, a name on the tracker's roster (they
     * healed or were heal-targeted), or a heal claim's target.
     * Healing with no encounter open is ignored, by design.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerRegainHealth(EntityRegainHealthEvent event) {
        if (!fightTrackingEnabled || encounters.isEmpty()) {
            return;
        }
        if (!(event.getEntity() instanceof Player healed)) {
            return;
        }
        long now = System.currentTimeMillis();
        double maxHealth = maxHealthOf(healed);
        for (Map.Entry<UUID, EncounterLedger> e : encounters.entrySet()) {
            UUID bossId = e.getKey();
            EncounterLedger ledger = e.getValue();
            if (ledger.isParticipant(healed.getUniqueId())
                    || fightTracker.rosterContains(bossId, healed.getUniqueId())
                    || fightTracker.hasClaimFor(bossId, healed.getUniqueId())) {
                fightTracker.recordHealing(bossId, healed.getUniqueId(),
                        event.getAmount(), healed.getHealth(), maxHealth,
                        event.getRegainReason().name(), now);
            }
        }
    }

    /**
     * A heal-spell cast claim from the Magic feed (package-private:
     * MagicFightHealListener calls it). The claim is offered to
     * every open encounter the caster or the target belongs to;
     * encounters neither belongs to never see it.
     */
    void noteMagicHealCast(UUID caster, UUID target, String spellKey) {
        if (!fightTrackingEnabled || encounters.isEmpty()) {
            return;
        }
        long now = System.currentTimeMillis();
        for (Map.Entry<UUID, EncounterLedger> e : encounters.entrySet()) {
            UUID bossId = e.getKey();
            EncounterLedger ledger = e.getValue();
            if (ledger.isParticipant(caster) || ledger.isParticipant(target)
                    || fightTracker.rosterContains(bossId, caster)
                    || fightTracker.rosterContains(bossId, target)) {
                fightTracker.recordClaim(bossId, caster, target, spellKey, now);
            }
        }
    }

    /** True when a Magic spell key names a configured heal spell
     *  (package-private: MagicFightHealListener calls it). */
    boolean isHealSpellKey(String spellKey) {
        return spellKey != null && magicHealSpells.contains(baseSpellKey(spellKey));
    }

    /** A spell key's base form: lower-cased, leveled variants
     *  ("heal|2") folded to their base ("heal"). */
    private static String baseSpellKey(String key) {
        if (key == null) {
            return "";
        }
        String base = key.trim().toLowerCase(java.util.Locale.ROOT);
        int pipe = base.indexOf('|');
        return pipe >= 0 ? base.substring(0, pipe) : base;
    }

    private static double maxHealthOf(Player p) {
        AttributeInstance a = p.getAttribute(Attribute.MAX_HEALTH);
        return a != null ? a.getValue() : 20.0;
    }

    // ------------------------------------------------------------------
    // 0.4.0: boss healing, the reconciliation tick, the verdict (PM-1/PM-2)
    // ------------------------------------------------------------------

    /**
     * Healing received BY A BOSS (0.4.0, PM-1): every regain
     * reason counts (REGEN, MAGIC, satiation, End crystals, a
     * plugin's heal) and the amount lands in the LEDGER's space,
     * mirroring the damage intake conversion exactly - effective
     * heal only (capped at what the bar was actually missing),
     * converted by the same intake factor the damage path uses
     * (IntakeFactor.toEntity and HealMath.toPoolSpace are exact
     * inverses by construction). The bar is then reconciled
     * immediately: the event will add its own amount when it
     * applies, so the entity is set to (the ledger's net truth
     * minus the pending heal) and the event lands it exactly on
     * the truth - the bar visibly rises as the boss heals, and
     * the 1-second tick backstops any heal a later listener
     * modifies. The dragon is recorded but not reconciled: its
     * chapter owns its bar, and its state machine already
     * judges from entity health, so crystal healing is
     * heal-consistent there by construction.
     * MONITOR + ignoreCancelled: observe-only, never alters the
     * event itself.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBossRegainHealth(EntityRegainHealthEvent event) {
        if (!(event.getEntity() instanceof LivingEntity boss)
                || event.getEntity() instanceof Player) {
            return; // players are onPlayerRegainHealth's feed
        }
        EncounterLedger ledger = encounters.get(boss.getUniqueId());
        if (ledger == null) {
            return;
        }
        BossKind kind = ledger.kind();
        if (kind == null || !chapterOn(kind)) {
            return;
        }
        AttributeInstance attr = boss.getAttribute(Attribute.MAX_HEALTH);
        double maxHealth = attr != null ? attr.getValue() : boss.getHealth();
        double effective = com.winkreyes.excelbosstrial.core.HealMath.effectiveHeal(
                event.getAmount(), boss.getHealth(), maxHealth);
        if (effective <= 0) {
            return;
        }
        double intakeF = intakeFactorFor(boss, kind, ledger);
        double poolHeal = com.winkreyes.excelbosstrial.core.HealMath
                .toPoolSpace(effective, intakeF);
        ledger.recordHealed(poolHeal);
        // 0.4.0 rebuild (Mike, Nov 8, 2026): a WITHER-reason
        // regain IS an eat. The game fuses the kill and the
        // heal into this single event - the Wither regains
        // 5 HP when it kills an entity, and Bukkit reports
        // exactly that heal with reason WITHER (the rig's
        // PM-1 proof line: "+5.0 pool HP (WITHER)" as sprinkle
        // adds died). This is the same signal the heal-aware
        // ledger already trusts, and it is the ONLY door to
        // the boss's voice: no eat, no line, ever.
        if (event.getRegainReason()
                == EntityRegainHealthEvent.RegainReason.WITHER) {
            maybeTaunt(boss, kind, ledger);
        }
        long now = System.currentTimeMillis();
        if (poolHeal >= 1.0
                && now - bossHealLogMs.getOrDefault(boss.getUniqueId(), 0L) >= 5_000L) {
            bossHealLogMs.put(boss.getUniqueId(), now);
            getLogger().info("BossTrial boss heal [" + kind.displayName() + "]: +"
                    + fmt(poolHeal) + " pool HP (" + event.getRegainReason().name()
                    + ") - net progress is now " + fmt(ledger.netDamage()) + " of "
                    + fmt(ledger.maxHealth()) + ".");
        }
        if (kind != BossKind.ENDER_DRAGON
                && !verdictPending.containsKey(boss.getUniqueId())
                && !boss.isDead() && boss.getHealth() > 0) {
            double entityMax = com.winkreyes.excelbosstrial.core.IntakeFactor.entityMax(
                    cfg.intakeEntityCap, ledger.maxHealth());
            double target = com.winkreyes.excelbosstrial.core.IntakeFactor.entityHealthFor(
                    cfg.intakeEntityCap, ledger.maxHealth(), ledger.netDamage(),
                    ledger.unseenDamage());
            double preHeal = Math.max(0.01, target - effective);
            boss.setHealth(Math.min(entityMax, preHeal));
        }
    }

    /**
     * The boss speaks (0.4.0 rebuild, Mike, Nov 8, 2026):
     * called ONLY from the regain handler on a WITHER-reason
     * heal - a real eat. The book decides whether this eat
     * earns a line (the first eat of the fight always does;
     * then at most one per configured cooldown) and which
     * line (never the one just spoken). Delivery is chat to
     * the encounter's participants only, styled as the boss
     * speaking; the boss bar is NOT touched (the bar is the
     * boss - it carries health truth, never flavor).
     */
    private void maybeTaunt(LivingEntity boss, BossKind kind,
                            EncounterLedger ledger) {
        TauntSettings settings = tauntSettings.get(kind);
        if (settings == null || !settings.enabled()) {
            return;
        }
        Optional<String> line = tauntBook.onEat(boss.getUniqueId(),
                System.currentTimeMillis(), settings.lines(),
                settings.cooldownSeconds() * 1000L,
                () -> java.util.concurrent.ThreadLocalRandom.current()
                        .nextInt(settings.lines().size()));
        if (line.isEmpty()) {
            return;
        }
        String text = "&5" + kind.displayName() + " &8» &d" + line.get();
        for (UUID participant : ledger.participants()) {
            Player p = Bukkit.getPlayer(participant);
            if (p != null && p.isOnline()) {
                p.sendMessage(color(text));
            }
        }
        getLogger().info("BossTrial taunt [" + kind.displayName() + "]: \""
                + line.get() + "\" (eat #" + tauntBook.eats(boss.getUniqueId())
                + " this fight)");
    }

    /**
     * The 1-second reconciliation (0.4.0, PM-1 + PM-2 + PM-8),
     * riding the pressure pulse's walk of the open encounters:
     * the mid-fight record cadence, the entity-health pin to the
     * ledger's NET fraction (at most one tick of drift from any
     * heal source, then corrected), the verdict judgment, and
     * the retry of a refused verdict blow. The dragon is
     * skipped: its chapter owns its bar and its verdict.
     */
    private void reconcileEncounter(LivingEntity boss, BossKind kind,
                                    EncounterLedger ledger, long now) {
        if (kind == BossKind.ENDER_DRAGON) {
            return;
        }
        long lastMid = midSnapshotMs.getOrDefault(boss.getUniqueId(), 0L);
        if (now - lastMid >= 60_000L) {
            midSnapshotMs.put(boss.getUniqueId(), now);
            snapshotMidFight(kind, boss.getUniqueId(), ledger);
        }
        if (boss.isDead() || boss.getHealth() <= 0) {
            return;
        }
        VerdictPending pending = verdictPending.get(boss.getUniqueId());
        if (pending != null) {
            if (now - pending.firedMs >= 250L) {
                fireVerdictBlow(boss, kind, ledger, pending);
            }
            return;
        }
        double entityMax = com.winkreyes.excelbosstrial.core.IntakeFactor.entityMax(
                cfg.intakeEntityCap, ledger.maxHealth());
        double target = com.winkreyes.excelbosstrial.core.IntakeFactor.entityHealthFor(
                cfg.intakeEntityCap, ledger.maxHealth(), ledger.netDamage(),
                ledger.unseenDamage());
        if (Math.abs(boss.getHealth() - target) > 0.01) {
            boss.setHealth(Math.max(0.01, Math.min(entityMax, target)));
        }
        maybeVerdict(boss, kind, ledger);
    }

    /** Judge the pool-exhaustion verdict for one encounter
     *  (0.4.0, PM-2): NET damage at or past the pool means the
     *  fight is won - the verdict fires and the governed lethal
     *  blow carries it through the real death path. Judged from
     *  the record handler (the moment a hit spends the pool)
     *  and from the reconciliation tick (any other route to
     *  exhaustion). */
    private void maybeVerdict(LivingEntity boss, BossKind kind, EncounterLedger ledger) {
        if (kind == BossKind.ENDER_DRAGON || boss.isDead() || boss.getHealth() <= 0
                || verdictPending.containsKey(boss.getUniqueId())) {
            return;
        }
        if (!com.winkreyes.excelbosstrial.core.VerdictJudge.exhausted(
                ledger.maxHealth(), ledger.netDamage())) {
            return;
        }
        VerdictPending pending = new VerdictPending(System.currentTimeMillis());
        verdictPending.put(boss.getUniqueId(), pending);
        getLogger().info("BossTrial verdict [" + kind.displayName()
                + "]: the pool is spent (net " + fmt(ledger.netDamage()) + " of "
                + fmt(ledger.maxHealth()) + ", healed back " + fmt(ledger.healedPool())
                + ") - the verdict blow falls.");
        fireVerdictBlow(boss, kind, ledger, pending);
    }

    /**
     * Execute one verdict blow (0.4.0, PM-2): a governed lethal
     * blow through the REAL damage path, so EntityDeathEvent,
     * drops, spoils, seal loot and GuildGate's SealCreditEvent
     * all run exactly as for a natural kill. The governor and
     * the record handler pass a verdict-pending boss through
     * untouched, so the blow lands unscaled and unrecorded. The
     * dealer is the ledger's top contributor when they are
     * online - the kill credits the player who earned it;
     * otherwise the blow is sourceless (the seal loot is
     * claim-based, so a sourceless kill still spoils everyone).
     * A blow the world reduces below lethal is retried by the
     * tick at escalating force and ends in a direct health set,
     * which this platform still runs through the real death
     * path - the judged kill is never left standing as the
     * Nov 7 zombie shape.
     */
    private void fireVerdictBlow(LivingEntity boss, BossKind kind,
                                 EncounterLedger ledger, VerdictPending pending) {
        pending.blows++;
        pending.firedMs = System.currentTimeMillis();
        UUID top = ledger.topDealer();
        Player dealer = top != null ? Bukkit.getPlayer(top) : null;
        try {
            if (pending.blows >= 3) {
                boss.setHealth(0.0);
            } else {
                double amount = Math.max(1.0, boss.getHealth())
                        * (pending.blows == 2 ? 4.0 : 1.0);
                if (dealer != null) {
                    boss.damage(amount, dealer);
                } else {
                    boss.damage(amount);
                }
            }
        } catch (Throwable failure) {
            getLogger().warning("BossTrial verdict blow failed [" + kind.displayName()
                    + "]: " + failure);
        }
        if (!boss.isDead() && boss.getHealth() > 0 && pending.blows >= 3) {
            getLogger().severe("BossTrial verdict [" + kind.displayName()
                    + "]: the pool is spent but the boss would not die after "
                    + pending.blows + " blows - the encounter stays verdict-pending "
                    + "and the tick keeps retrying.");
        }
    }

    // ------------------------------------------------------------------
    // 0.4.0: the seal-grant encounter close (PM-12)
    // ------------------------------------------------------------------

    private int countEncountersOfKind(BossKind kind) {
        int count = 0;
        for (EncounterLedger ledger : encounters.values()) {
            if (ledger.kind() == kind) {
                count++;
            }
        }
        return count;
    }

    /**
     * Close every open encounter of a seal WITHOUT a verdict
     * (0.4.0, PM-12) - the seam GuildGate's admin grant (and any
     * future seal-state writer) needs: the Nov 7 manual wither
     * grant left the encounter running over a broken seal, and
     * the party had to abandon a zombie boss and reset. Bound
     * into ExcelBossTrialAPI at enable; also fired internally by
     * the maintenance grant watch, because GuildGate's grant
     * path fires no event (verified against the GuildGate
     * source of record: the admin grant calls its state store
     * directly; the only event its tree fires is an unrelated
     * narration proposal - so a BossTrial-side state watch is
     * the reliable half, and this method is the shared close).
     * Each close: the fight record is written with the
     * SEAL_GRANTED label (PM-8), the bar and the sprinkle close,
     * and the boss entity is restored to vanilla strength with
     * its current health fraction kept - no verdict, no
     * cooldown, no loot (the granted seal already delivered its
     * meaning; the encounter simply STOPS, and the boss is a
     * normal boss).
     */
    boolean closeEncounterForSealInternal(BossKind kind, String cause) {
        boolean closedAny = false;
        for (Map.Entry<UUID, EncounterLedger> e
                : new ArrayList<>(encounters.entrySet())) {
            if (e.getValue().kind() != kind) {
                continue;
            }
            Entity ent = Bukkit.getEntity(e.getKey());
            if (ent instanceof LivingEntity boss) {
                closeEncounterForSealInternal(kind, boss, e.getValue(), cause);
            } else {
                encounters.remove(e.getKey());
                verdictPending.remove(e.getKey());
                snapshotCloseRecord(kind, e.getKey(), e.getValue(),
                        LABEL_SEAL_GRANTED, null, List.of(), null);
                if (feedback != null) {
                    feedback.onEncounterClose(e.getKey());
                }
                if (sprinkle != null) {
                    sprinkle.closeEncounter(e.getKey(), "seal granted");
                }
            }
            closedAny = true;
        }
        return closedAny;
    }

    private void closeEncounterForSealInternal(BossKind kind, LivingEntity boss,
                                               EncounterLedger ledger, String cause) {
        UUID bossId = boss.getUniqueId();
        encounters.remove(bossId);
        verdictPending.remove(bossId);
        snapshotCloseRecord(kind, bossId, ledger, LABEL_SEAL_GRANTED,
                null, List.of(), null);
        if (feedback != null) {
            feedback.onEncounterClose(bossId);
        }
        if (sprinkle != null) {
            sprinkle.closeEncounter(bossId, "seal granted");
        }
        // Restore the entity to vanilla strength, keeping its
        // current health fraction: the trial scaling comes off
        // and the boss is a normal boss again (it stays in the
        // world - the party may still fight it for vanilla
        // stakes, and nothing of BossTrial's rides it anymore).
        try {
            AttributeInstance maxHealth = boss.getAttribute(Attribute.MAX_HEALTH);
            if (maxHealth != null) {
                double fraction = maxHealth.getValue() > 0
                        ? boss.getHealth() / maxHealth.getValue() : 1.0;
                maxHealth.setBaseValue(maxHealth.getDefaultValue());
                boss.setHealth(Math.max(0.01,
                        maxHealth.getValue() * Math.min(1.0, Math.max(0.0, fraction))));
            }
        } catch (Throwable failure) {
            getLogger().warning("BossTrial seal-grant close [" + kind.displayName()
                    + "]: the entity restore failed (the encounter is closed "
                    + "regardless): " + failure);
        }
        getLogger().info("BossTrial encounter [" + kind.displayName()
                + "] closed without a verdict: the seal was granted (" + cause
                + ") - the boss stands down to vanilla strength.");
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
        // 0.3.0: the repeat layer judges AFTER every 0.2.11 gate,
        // in the settled order - only a base CREDIT on an already
        // broken seal can become REPEAT_CREDIT or DENY_REPEAT_WINDOW.
        boolean sealBroken = sealBroken(kind);
        boolean repeatWindowActive = repeatWindows.isActive(kind.sealId(), now);
        CreditJudge.Verdict verdict = CreditJudge.judge(shareMet, presenceOk, cooldownActive,
                sealBroken, repeatWindowActive, cfg.repeatEnabled);
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
        // The trio runs on a first-break CREDIT (latch not yet
        // standing) and - new in 0.3.0 - on the repeat verdicts:
        // a repeat trio still assembles elder by elder, and even a
        // window-denied completion COUNTS its elders (the trio is
        // real; only the repeat payment waits).
        boolean elderTrioRuns = kind == BossKind.ELDER_GUARDIAN
                && ((verdict == CreditJudge.Verdict.CREDIT && !elderLatchStands())
                        || verdict == CreditJudge.Verdict.REPEAT_CREDIT
                        || verdict == CreditJudge.Verdict.DENY_REPEAT_WINDOW);
        if (elderTrioRuns) {
            elderProgressNow = monuments.markCounted(boss.getUniqueId());
            if (elderProgressNow >= 0 && elderProgressNow < MonumentTracker.TRIO_SIZE) {
                DeathOutcome progress = new DeathOutcome(kind, verdict,
                        true, present, spoilsMultiplier(verdict, present),
                        sharePercent(ledger), elderProgressNow,
                        ledger == null ? 0 : ledger.heightExcludedHits(),
                        poolMax, damagerCount,
                        ledger == null ? 0 : ledger.rangeExcludedHits(), cagedWither,
                        presentChampions, sealBroken);
                deathOutcomes.put(boss.getUniqueId(), progress);
                return progress;
            }
            // elderProgressNow == TRIO_SIZE: the trio completes on
            // this kill and the verdict stands as judged.
        }
        DeathOutcome outcome = new DeathOutcome(kind, verdict, false, present,
                spoilsMultiplier(verdict, present), sharePercent(ledger),
                elderProgressNow, ledger == null ? 0 : ledger.heightExcludedHits(),
                poolMax, damagerCount,
                ledger == null ? 0 : ledger.rangeExcludedHits(), cagedWither,
                presentChampions, sealBroken);
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
     * 0.3.0: whether a seal already stands broken, for the repeat
     * judge. GuildGate's live word governs when it can be read
     * (the same cross-check discipline as the elder latch);
     * unverifiable falls back to BossTrial's own credit records -
     * the seals this plugin has credited, holds a paid signature
     * generation for, or (elder) latches approved. Never pretends
     * knowledge it does not have: all sources silent reads false,
     * which fails toward a first-break path, and GuildGate's own
     * state independently refuses a duplicate credit.
     */
    private boolean sealBroken(BossKind kind) {
        Boolean gg = guildGateSealBroken(kind.sealId());
        if (gg != null) {
            return gg;
        }
        return creditedSeals.contains(kind.sealId())
                || signatureGenerations.containsKey(kind.sealId())
                || (kind == BossKind.ELDER_GUARDIAN && elderSealApproved);
    }

    /** The live intake factor for one boss: the dragon's is its
     *  current phase pool; every other chapter's is the ledger's
     *  rallied denominator. 1.0 whenever the pool fits the cap. */
    private double intakeFactorFor(LivingEntity boss, BossKind kind, EncounterLedger ledger) {
        if (kind == BossKind.ENDER_DRAGON) {
            if (dragonChapter == null) {
                return 1.0;
            }
            double pool = dragonChapter.currentPhasePool(boss.getUniqueId());
            return pool > 0
                    ? com.winkreyes.excelbosstrial.core.IntakeFactor.factor(cfg.intakeEntityCap, pool)
                    : 1.0;
        }
        return com.winkreyes.excelbosstrial.core.IntakeFactor.factor(
                cfg.intakeEntityCap, ledger.maxHealth());
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
        DeathOutcome outcome = outcomeFor(kind, boss, null);
        if (outcome.elderProgress) {
            return false; // fought, counted - but the trio is not done
        }
        // 0.3.0 (SPEC 2.5): a repeat outcome NEVER credits - the
        // seal already stands. The veto is structural, not a
        // judgment call.
        if (outcome.verdict == CreditJudge.Verdict.REPEAT_CREDIT
                || outcome.verdict == CreditJudge.Verdict.DENY_REPEAT_WINDOW) {
            return false;
        }
        if (kind == BossKind.ELDER_GUARDIAN && elderLatchStands()) {
            return true;
        }
        return outcome.verdict == CreditJudge.Verdict.CREDIT;
    }

    /** 0.3.0 (SPEC 2.5): the hook's veto line. Repeat outcomes get
     *  the repeat-aware wording - a vetoed repeat is routine, not
     *  a rules failure, and the log says so. Null = default line. */
    String vetoLogLine(BossKind kind, LivingEntity boss) {
        DeathOutcome cached = deathOutcomes.get(boss.getUniqueId());
        if (cached == null) {
            return null;
        }
        if (cached.verdict == CreditJudge.Verdict.REPEAT_CREDIT) {
            return "BossTrial repeat clear (" + kind.sealId()
                    + "): seal already stands; rewards paid by BossTrial, guild state unchanged.";
        }
        if (cached.verdict == CreditJudge.Verdict.DENY_REPEAT_WINDOW) {
            return "BossTrial repeat window (" + kind.sealId()
                    + "): seal already stands and the repeat window is still running; guild state unchanged.";
        }
        return null;
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
                verdictPending.remove(id);
                // 0.4.0 (PM-7/PM-8): a prune is a RECORDED close -
                // the fight record is written with the no-verdict
                // label (and its boss_state close record published)
                // before the encounter evaporates. The 24 h TTL
                // itself is unchanged.
                snapshotCloseRecord(e.getValue().kind(), id, e.getValue(),
                        LABEL_NO_VERDICT, null, List.of(), null);
                // 0.3.6: a pruned encounter's feedback bar ends
                // with its ledger, like its sprinkle adds below.
                if (feedback != null) {
                    feedback.onEncounterClose(id);
                }
                elderTrioMult.remove(id);
                // 0.3.3: a pruned encounter's sprinkle adds are
                // removed with it - no elite outlives its fight.
                if (sprinkle != null) {
                    sprinkle.closeEncounter(id, "prune");
                }
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
        // 0.4.0: the fix-train's per-encounter state prunes with
        // the encounter map, like every other registry here.
        sealBrokenAtOpen.keySet().retainAll(encounters.keySet());
        midSnapshotMs.keySet().retainAll(encounters.keySet());
        unattributedLogMs.keySet().retainAll(encounters.keySet());
        bossHealLogMs.keySet().retainAll(encounters.keySet());
        tauntBook.retainOnly(encounters.keySet());
        grantWatchSince.keySet().retainAll(encounters.keySet());
        // 0.4.0 (PM-12): the seal-grant watch. GuildGate's admin
        // grant fires NO event (verified in the GuildGate source
        // of record - see closeEncounterForSeal), so the state
        // seam is the only signal: an encounter whose seal stood
        // UNBROKEN at open, whose seal now stands broken, with no
        // verdict running and no sibling encounter of the same
        // chapter open, is a granted seal over a live fight -
        // close it. Two consecutive passes (the 30 s cadence)
        // must agree, so a transient read never closes a fight.
        // The elder chapter is excluded: its seal is chapter-wide
        // while its encounters are per-monument, so a trio
        // verdict at one monument would false-close another's
        // fight (the API method remains the elder seam).
        for (Map.Entry<UUID, EncounterLedger> e
                : new ArrayList<>(encounters.entrySet())) {
            BossKind kind = e.getValue().kind();
            if (kind == BossKind.ELDER_GUARDIAN
                    || verdictPending.containsKey(e.getKey())
                    || Boolean.TRUE.equals(sealBrokenAtOpen.get(e.getKey()))
                    || !sealBroken(kind)
                    || countEncountersOfKind(kind) != 1) {
                grantWatchSince.remove(e.getKey());
                continue;
            }
            Long since = grantWatchSince.get(e.getKey());
            if (since == null) {
                grantWatchSince.put(e.getKey(), now);
                continue;
            }
            if (now - since >= 30_000L) {
                grantWatchSince.remove(e.getKey());
                Entity ent = Bukkit.getEntity(e.getKey());
                if (ent instanceof LivingEntity grantedBoss) {
                    closeEncounterForSealInternal(kind, grantedBoss, e.getValue(),
                            "seal-grant watch");
                }
            }
        }
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
            // 0.3.4: the boss is available again - the
            // boss-state feed hears it at the moment it fires.
            if (protectEmitSettings.stateChanges()) {
                protectEmit.record(ProtectTableShape.STREAM,
                        ProtectTableShape.TYPE_BOSS_STATE,
                        ProtectTableShape.respawned(pending.monumentId,
                                fresh.getUniqueId().toString(), pending.world));
            }
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
            if (cfg.repeatEnabled) {
                for (BossKind kind : BossKind.values()) {
                    long left = repeatWindows.remainingMs(kind.sealId(), now);
                    int clears = clearCounts.getOrDefault(kind.sealId(), Map.of())
                            .values().stream().mapToInt(Integer::intValue).sum();
                    sender.sendMessage(color("&7Repeat [" + kind.displayName() + "]: "
                            + (sealBroken(kind)
                                    ? (left > 0 ? "&erepeat spoils rest &f"
                                            + CooldownBook.formatRemaining(left)
                                            : "&arepeat ready")
                                    : "&7seal not yet broken")
                            + " &7(sig gen " + signatureGenerations.getOrDefault(kind.sealId(), -1)
                            + ", repeat clears " + clears + ")"));
                }
            }
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
        if (args[0].equalsIgnoreCase("grantclaim")) {
            handleGrantClaim(sender, args);
            return true;
        }
        if (args[0].equalsIgnoreCase("feedback")) {
            handleFeedback(sender, args);
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
            // 0.4.0 (PM-8): an admin reset is a RECORDED close -
            // every open encounter's fight record is written
            // BEFORE its ledger is cleared (Nov 7: the reset
            // deleted the Wither fight's only per-player table
            // minutes after the party asked for it).
            for (Map.Entry<UUID, EncounterLedger> openEncounter
                    : new ArrayList<>(encounters.entrySet())) {
                snapshotCloseRecord(openEncounter.getValue().kind(), openEncounter.getKey(),
                        openEncounter.getValue(), LABEL_ADMIN_RESET, null, List.of(), null);
            }
            verdictPending.clear();
            sealBrokenAtOpen.clear();
            midSnapshotMs.clear();
            cooldowns.clearAll();
            encounters.clear();
            deathOutcomes.clear();
            attempts.clear();
            attemptsAnnounced.clear();
            // 0.3.3: a reset closes every sprinkle encounter too.
            if (sprinkle != null) {
                sprinkle.closeAll("admin reset");
            }
            // 0.3.6: and every feedback bar (encounters are gone).
            if (feedback != null) {
                feedback.closeAll();
            }
            placements.clear();
            if (args.length > 1 && args[1].equalsIgnoreCase("all")) {
                monuments.restore(Map.of());
                elderSealApproved = false;
                signaturePaid.clear();
                repeatWindows.clearAll();
                signatureGenerations.clear();
                creditedSeals.clear();
                clearCounts.clear();
            }
            saveState();
            sender.sendMessage(color("&aExcelBossTrial state reset"
                    + (args.length > 1 && args[1].equalsIgnoreCase("all")
                            ? " (including monuments + elder approval)" : "") + "."));
            return true;
        }
        sender.sendMessage(color("&cUsage: /bosstrial [status|feedback on|off|reload|reset [all]|grantclaim <player> <material> <count>|grantclaim <player> signature <chapter>]"));
        return true;
    }

    /**
     * 0.3.6: /bosstrial feedback on|off - a player's OWN damage
     * feedback setting (public in the permission split: it moves
     * nobody else's screen). Off removes their action-bar damage
     * totals and their seat in every BossTrial boss bar audience;
     * on restores both. Persisted in state.yml with the rest of
     * the plugin state. The console has no screen to set: it gets
     * the usage line, not an error.
     */
    private void handleFeedback(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(color("&cUsage (players only): /bosstrial feedback [on|off]"));
            return;
        }
        if (args.length != 2
                || !(args[1].equalsIgnoreCase("on") || args[1].equalsIgnoreCase("off"))) {
            boolean currentlyOff = feedbackOptOuts().isOptedOut(player.getUniqueId());
            sender.sendMessage(color("&cUsage: /bosstrial feedback [on|off] &7- your damage "
                    + "feedback is currently " + (currentlyOff ? "&cOFF" : "&aON")
                    + "&7 (boss bar numbers + your damage totals)."));
            return;
        }
        boolean on = args[1].equalsIgnoreCase("on");
        boolean changed = feedbackOptOuts().setOptedOut(player.getUniqueId(), !on);
        if (changed) {
            saveState();
        }
        sender.sendMessage(color(on
                ? "&aDamage feedback ON: seal boss bars show the true pool numbers, "
                        + "and your hits show your running damage total."
                : "&eDamage feedback OFF: no boss bar numbers or damage totals on your "
                        + "screen. /bosstrial feedback on brings them back anytime."));
    }

    /**
     * 0.2.11: /bosstrial grantclaim <player> <material> <count> —
     * throw an owed reward into a player's personal claims (Mike,
     * Oct 30, 2026: remediations ride the claims store because it
     * "waits out full inventories and offline players by design").
     * Access rides the 0.2.6 split: the call is a non-status
     * subcommand, so CommandAccess has already refused non-admin
     * players by the time we get here; the console passes. Offline
     * players are the primary use — the claim simply waits for
     * their next join, then presents and delivers like any earned
     * batch. Every attempt, granted or refused, is audit-logged.
     */
    private void handleGrantClaim(CommandSender sender, String[] args) {
        String joined = String.join(" ", args);
        if (!getConfig().getBoolean("commands.grantclaim-enabled", true)) {
            getLogger().warning("BossTrial grantclaim REFUSED (disabled by config) by "
                    + sender.getName() + ": " + joined);
            sender.sendMessage(color("&cgrantclaim is disabled by config (commands.grantclaim-enabled)."));
            return;
        }
        if (args.length != 4) {
            sender.sendMessage(color("&cUsage: /bosstrial grantclaim <player> <material> <count>"
                    + " - or grantclaim <player> signature <chapter>"));
            return;
        }
        // Resolve the target: a UUID, an online player's exact name,
        // or an offline player's name (the primary use).
        UUID target = null;
        String targetName = null;
        try {
            target = UUID.fromString(args[1]);
            targetName = Bukkit.getOfflinePlayer(target).getName();
        } catch (IllegalArgumentException notUuid) {
            Player online = Bukkit.getPlayerExact(args[1]);
            if (online != null) {
                target = online.getUniqueId();
                targetName = online.getName();
            } else {
                var offline = Bukkit.getOfflinePlayer(args[1]);
                target = offline.getUniqueId();
                targetName = offline.getName();
            }
        }
        if (targetName == null) {
            targetName = args[1];
        }
        // 0.4.0 (PM-10): the signature form - the chapter's
        // signature SET, built exactly as a verdict builds it
        // (SealLoot.perChampionPlan + buildLootStack +
        // stampProvenance), never a raw material stand-in.
        if (GrantClaimPlan.isSignatureForm(args[2])) {
            handleGrantClaimSignature(sender, args, target, targetName, joined);
            return;
        }
        GrantClaimPlan.Result parsed = GrantClaimPlan.parse(args[2], args[3]);
        if (!parsed.ok()) {
            getLogger().warning("BossTrial grantclaim REFUSED (" + parsed.error() + ") by "
                    + sender.getName() + ": " + joined);
            sender.sendMessage(color(parsed.error() == GrantClaimPlan.Error.MATERIAL
                    ? "&cNo such item material: " + args[2]
                    : "&cCount must be a whole number from 1 to " + GrantClaimPlan.MAX_COUNT + ": " + args[3]));
            return;
        }
        if (teamClaims == null) {
            getLogger().severe("BossTrial grantclaim FAILED (team claims unavailable) by "
                    + sender.getName() + ": " + joined);
            sender.sendMessage(color("&cTeam claims are unavailable; nothing was stored."));
            return;
        }
        GrantClaimPlan.Plan plan = parsed.plan();
        if (!plan.material().isItem()) {
            getLogger().warning("BossTrial grantclaim REFUSED (MATERIAL: not an item) by "
                    + sender.getName() + ": " + joined);
            sender.sendMessage(color("&cNot an item material: " + args[2]));
            return;
        }
        List<ItemStack> stacks = new ArrayList<>();
        for (int size : GrantClaimPlan.split(plan.count(),
                Math.max(1, plan.material().getMaxStackSize()))) {
            stacks.add(new ItemStack(plan.material(), size));
        }
        try {
            String run = teamClaims.grantClaim(target, stacks);
            getLogger().info("BossTrial grantclaim: " + sender.getName() + " granted "
                    + plan.count() + " x " + plan.material().name() + " to " + targetName
                    + " (" + target + ") as claim " + run + "/" + target
                    + " (" + stacks.size() + " stack(s)); it waits in their /bossrewards.");
            sender.sendMessage(color("&aGranted " + plan.count() + " x " + plan.material().name()
                    + " to " + targetName + " - it waits in their /bossrewards."));
            notifyClaimRecipient(target);
        } catch (Exception failure) {
            getLogger().severe("BossTrial grantclaim FAILED by " + sender.getName() + " for "
                    + targetName + " (" + target + "), " + plan.count() + " x "
                    + plan.material().name() + ": " + failure.getMessage());
            sender.sendMessage(color("&cGrant failed; nothing was stored. See the server log."));
        }
    }

    /**
     * 0.4.0 (PM-10): the signature form of grantclaim -
     * {@code /bosstrial grantclaim <player> signature <chapter>}
     * files the chapter's signature set into the player's
     * claims, built through the SAME path a verdict uses
     * (SealLoot.perChampionPlan -> buildLootStack ->
     * stampProvenance for the named pieces; the dragon adds its
     * per-champion materials; "raid" builds the RaidStandard).
     * Base (unreforged) form - a remediation restores what a
     * first clear owed. Every attempt is audit-logged, like the
     * material form.
     */
    private void handleGrantClaimSignature(CommandSender sender, String[] args,
                                           UUID target, String targetName, String joined) {
        if (teamClaims == null) {
            getLogger().severe("BossTrial grantclaim FAILED (team claims unavailable) by "
                    + sender.getName() + ": " + joined);
            sender.sendMessage(color("&cTeam claims are unavailable; nothing was stored."));
            return;
        }
        List<ItemStack> stacks = new ArrayList<>();
        String what;
        if (GrantClaimPlan.isRaidSignature(args[3])) {
            if (raidStandard == null) {
                getLogger().warning("BossTrial grantclaim REFUSED (raid standard unavailable) by "
                        + sender.getName() + ": " + joined);
                sender.sendMessage(color("&cThe raid standard is unavailable on this boot; nothing was stored."));
                return;
            }
            stacks.add(raidStandard.buildStandard());
            what = "the Raid standard";
        } else {
            BossKind chapterKind = GrantClaimPlan.signatureChapter(args[3]);
            if (chapterKind == null) {
                getLogger().warning("BossTrial grantclaim REFUSED (CHAPTER) by "
                        + sender.getName() + ": " + joined);
                sender.sendMessage(color("&cNo such signature chapter: " + args[3]
                        + " (wither, warden, dragon, elder, raid)"));
                return;
            }
            SealLoot.LootTable table = lootTables.getOrDefault(chapterKind,
                    SealLoot.defaultsFor(chapterKind));
            for (SealLoot.PlannedStack planned : SealLoot.perChampionPlan(table)) {
                ItemStack stack = buildLootStack(planned.item());
                if (stack == null) {
                    getLogger().severe("BossTrial grantclaim FAILED (invalid signature recipe) by "
                            + sender.getName() + ": " + joined);
                    sender.sendMessage(color("&cThe chapter's signature recipe is invalid; nothing was stored."));
                    return;
                }
                if (planned.provenanceStamped()) {
                    stampProvenance(stack, targetName, false);
                }
                stacks.add(stack);
            }
            if (chapterKind == BossKind.ENDER_DRAGON) {
                for (SealLoot.LootItem mat : SealLoot.dragonMaterials(
                        dragonBreathBase, dragonCrystals)) {
                    ItemStack stack = buildLootStack(mat);
                    if (stack != null) {
                        stacks.add(stack);
                    }
                }
            }
            if (stacks.isEmpty()) {
                getLogger().warning("BossTrial grantclaim REFUSED (no signature configured) by "
                        + sender.getName() + ": " + joined);
                sender.sendMessage(color("&cThat chapter has no signature set configured; nothing was stored."));
                return;
            }
            what = chapterKind.displayName() + " signature set";
        }
        try {
            String run = teamClaims.grantClaim(target, stacks);
            getLogger().info("BossTrial grantclaim: " + sender.getName() + " granted "
                    + what + " to " + targetName + " (" + target + ") as claim " + run
                    + "/" + target + " (" + stacks.size() + " stack(s)); it waits in "
                    + "their /bossrewards.");
            sender.sendMessage(color("&aGranted " + what + " to " + targetName
                    + " - it waits in their /bossrewards."));
            notifyClaimRecipient(target);
        } catch (Exception failure) {
            getLogger().severe("BossTrial grantclaim FAILED by " + sender.getName() + " for "
                    + targetName + " (" + target + "), " + what + ": " + failure.getMessage());
            sender.sendMessage(color("&cGrant failed; nothing was stored. See the server log."));
        }
    }

    /** 0.4.0 (PM-10): a successful grantclaim tells its
     *  recipient in chat when they are online - a remediation
     *  the player never hears about is a remediation that sits
     *  unclaimed (the Nov 7 lesson ran both ways). */
    private void notifyClaimRecipient(UUID target) {
        Player recipient = Bukkit.getPlayer(target);
        if (recipient != null) {
            recipient.sendMessage(color("&eBoss rewards: &fa reward was added to your "
                    + "claims - open &e/bossrewards &fto collect it."));
        }
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


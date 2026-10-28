package com.winkreyes.excelbosstrial.core;

import java.util.Map;

/**
 * Every knob of ExcelBossTrial except the dragon chapter's own
 * TrialConfig, with the settled defaults (owner, Oct 18, 2026).
 * Pure data: the Bukkit adapter fills it from config.yml via
 * {@link #fromMap}; tests fill it directly. Validation clamps
 * instead of throwing - a typo'd config degrades toward vanilla,
 * never crashes the plugin.
 *
 * Every rule is individually disable-able, DragonTrial-style:
 * a chapter's enabled=false removes the whole chapter (governor,
 * credit gate, spoils, consequences); share-gate / presence /
 * spoils / caps each switch off on their own; a cap of 0 disables
 * that cap (see BossCaps).
 */
public final class BossTrialConfig {

    /** One boss chapter's rule switches. */
    public record Chapter(boolean enabled, BossCaps caps,
                          boolean shareGate, boolean presence) {
        public static final Chapter OFF = new Chapter(false, BossCaps.OFF, false, false);
    }

    public final boolean enabled;
    public final double shareThreshold;      // fraction of max HP, 0.5 settled
    public final double presenceRange;       // blocks, 128 settled
    public final long cooldownMs;            // 6h settled
    public final boolean spoilsEnabled;
    public final int spoilsCap;              // 5 settled
    public final long elderRespawnMs;        // 6h settled

    public final Chapter wither;
    public final Chapter elderGuardian;
    public final Chapter warden;
    /** The dragon carries the presence rule + XP spoils, and -
     *  since 0.2.1 (owner, Oct 20, 2026 late, the seal-difficulty
     *  audit) - the share gate too, measured against the
     *  phase-aware pool (phase 1 + phase 2). Before 0.2.1 its
     *  two-phase machine was its only gate, which left damage
     *  PROVENANCE unpoliced: a staged second boss could deal her
     *  damage while a participant merely stood present. */
    public final boolean dragonPresence;
    /** Dragon only (0.2.1): the share gate switch for the dragon.
     *  Default on (the audit fix); off returns the dragon to its
     *  pre-0.2.1 machine-only gating. */
    public final boolean dragonShareGate;
    /** Warden only (owner, Oct 19, 2026 evening amendment): a hit
     *  counts toward the warden seal's share only when the attacker
     *  stood within this many blocks vertically of the warden when
     *  the hit landed. 0 disables the rule. See CreditJudge. */
    public final double wardenShareHeightBand;
    /** Warden only (0.2.1, the audit's horizontal shooting
     *  gallery): a hit counts toward the warden seal's share only
     *  when the attacker stood within this many blocks
     *  HORIZONTALLY of the warden when the hit landed. The settled
     *  14 mirrors the sonic boom's horizontal reach. 0 disables
     *  the rule. See CreditJudge.withinRangeBand. */
    public final double wardenShareRangeBand;
    /** Wither only (0.2.1, the audit's cage + pocket-melee hole,
     *  in owner's strict CAGED-AT-DEATH form, decided Oct 20,
     *  2026 late): the fought-free check - if the wither spent
     *  the fight's last stretch (see witherFoughtFreeWindow)
     *  penned within witherFoughtFreeRadius of where that
     *  stretch began AND landed no hit on any player during it,
     *  the kill earns no seal, however honest the counted blows
     *  and however hard it fought earlier. Off disables the
     *  check entirely. */
    public final boolean witherFoughtFreeCheck;
    /** The fought-free radius in blocks (3D, from the window's
     *  start position). Settled 4: a caged wither jitters in
     *  place; a fighting one crosses an arena. */
    public final double witherFoughtFreeRadius;
    /** The fought-free window: the final fraction of the
     *  wither's rallied health pool, measured in damage dealt,
     *  that the caged-at-death test judges. Settled 0.25 - the
     *  last quarter of the pool. 1.0 judges the whole fight
     *  (the pre-variant whole-life shape). */
    public final double witherFoughtFreeWindow;
    /** Warden only (0.2.2, seal-hardening option 1B): if the
     *  warden's ledger records ONLY excluded or unattributed
     *  damage for this long - no counted blow landed, and the
     *  warden landed none either - the fight never came to it
     *  and it burrows away (no death, no verdict, no cooldown).
     *  Settled 45 s. 0 disables the rule. See WardenGiveUp. */
    public final long wardenGiveUpWindowMs;

    /** Participant growth - "the boss rallies" (0.2, spec
     *  BOSSTRIAL-0.2-SPEC-2026-10-20.md section 1). Per chapter;
     *  the dragon never scales this way (its two-phase machine is
     *  its scaler), so it has no rule here. */
    public final ScalingRule witherScaling;
    public final ScalingRule elderScaling;
    public final ScalingRule wardenScaling;

    public BossTrialConfig(boolean enabled, double shareThreshold, double presenceRange,
                           long cooldownMs, boolean spoilsEnabled, int spoilsCap,
                           long elderRespawnMs, Chapter wither, Chapter elderGuardian,
                           Chapter warden, boolean dragonPresence,
                           double wardenShareHeightBand) {
        this(enabled, shareThreshold, presenceRange, cooldownMs, spoilsEnabled,
                spoilsCap, elderRespawnMs, wither, elderGuardian, warden,
                dragonPresence, wardenShareHeightBand,
                ScalingRule.settledDefault(BossKind.WITHER),
                ScalingRule.settledDefault(BossKind.ELDER_GUARDIAN),
                ScalingRule.settledDefault(BossKind.WARDEN));
    }

    /** Pre-0.2.1 constructor shape: the 0.2.1 rules at their settled defaults. */
    public BossTrialConfig(boolean enabled, double shareThreshold, double presenceRange,
                           long cooldownMs, boolean spoilsEnabled, int spoilsCap,
                           long elderRespawnMs, Chapter wither, Chapter elderGuardian,
                           Chapter warden, boolean dragonPresence,
                           double wardenShareHeightBand,
                           ScalingRule witherScaling, ScalingRule elderScaling,
                           ScalingRule wardenScaling) {
        this(enabled, shareThreshold, presenceRange, cooldownMs, spoilsEnabled,
                spoilsCap, elderRespawnMs, wither, elderGuardian, warden,
                dragonPresence, wardenShareHeightBand, witherScaling,
                elderScaling, wardenScaling, true, 14.0, true, 4.0, 0.25,
                WardenGiveUp.DEFAULT_WINDOW_MS);
    }

    public BossTrialConfig(boolean enabled, double shareThreshold, double presenceRange,
                           long cooldownMs, boolean spoilsEnabled, int spoilsCap,
                           long elderRespawnMs, Chapter wither, Chapter elderGuardian,
                           Chapter warden, boolean dragonPresence,
                           double wardenShareHeightBand,
                           ScalingRule witherScaling, ScalingRule elderScaling,
                           ScalingRule wardenScaling,
                           boolean dragonShareGate, double wardenShareRangeBand,
                           boolean witherFoughtFreeCheck, double witherFoughtFreeRadius,
                           double witherFoughtFreeWindow) {
        this(enabled, shareThreshold, presenceRange, cooldownMs, spoilsEnabled,
                spoilsCap, elderRespawnMs, wither, elderGuardian, warden,
                dragonPresence, wardenShareHeightBand, witherScaling,
                elderScaling, wardenScaling, dragonShareGate, wardenShareRangeBand,
                witherFoughtFreeCheck, witherFoughtFreeRadius, witherFoughtFreeWindow,
                WardenGiveUp.DEFAULT_WINDOW_MS);
    }

    public BossTrialConfig(boolean enabled, double shareThreshold, double presenceRange,
                           long cooldownMs, boolean spoilsEnabled, int spoilsCap,
                           long elderRespawnMs, Chapter wither, Chapter elderGuardian,
                           Chapter warden, boolean dragonPresence,
                           double wardenShareHeightBand,
                           ScalingRule witherScaling, ScalingRule elderScaling,
                           ScalingRule wardenScaling,
                           boolean dragonShareGate, double wardenShareRangeBand,
                           boolean witherFoughtFreeCheck, double witherFoughtFreeRadius,
                           double witherFoughtFreeWindow,
                           long wardenGiveUpWindowMs) {
        this.enabled = enabled;
        this.shareThreshold = clamp(shareThreshold, 0.0, 1.0);
        this.presenceRange = clamp(presenceRange, 0.0, 10000.0);
        this.cooldownMs = Math.max(0L, cooldownMs);
        this.spoilsEnabled = spoilsEnabled;
        this.spoilsCap = (int) clamp(spoilsCap, 1, 100);
        this.elderRespawnMs = Math.max(0L, elderRespawnMs);
        this.wither = wither;
        this.elderGuardian = elderGuardian;
        this.warden = warden;
        this.dragonPresence = dragonPresence;
        this.wardenShareHeightBand = clamp(wardenShareHeightBand, 0.0, 1000.0);
        this.witherScaling = witherScaling != null ? witherScaling : ScalingRule.OFF;
        this.elderScaling = elderScaling != null ? elderScaling : ScalingRule.OFF;
        this.wardenScaling = wardenScaling != null ? wardenScaling : ScalingRule.OFF;
        this.dragonShareGate = dragonShareGate;
        this.wardenShareRangeBand = clamp(wardenShareRangeBand, 0.0, 1000.0);
        this.witherFoughtFreeCheck = witherFoughtFreeCheck;
        this.witherFoughtFreeRadius = clamp(witherFoughtFreeRadius, 0.0, 1000.0);
        this.witherFoughtFreeWindow = clamp(witherFoughtFreeWindow, 0.05, 1.0);
        this.wardenGiveUpWindowMs = (long) clamp(wardenGiveUpWindowMs, 0.0, 3600000.0);
    }

    /** Pre-amendment constructor shape: the warden height band at its settled default. */
    public BossTrialConfig(boolean enabled, double shareThreshold, double presenceRange,
                           long cooldownMs, boolean spoilsEnabled, int spoilsCap,
                           long elderRespawnMs, Chapter wither, Chapter elderGuardian,
                           Chapter warden, boolean dragonPresence) {
        this(enabled, shareThreshold, presenceRange, cooldownMs, spoilsEnabled,
                spoilsCap, elderRespawnMs, wither, elderGuardian, warden,
                dragonPresence, 12.0);
    }

    public static BossTrialConfig defaults() {
        return new BossTrialConfig(
                true, 0.5, 128.0,
                CooldownBook.DEFAULT_DURATION_MS, true, SpoilsMath.DEFAULT_CAP,
                6L * 60L * 60L * 1000L,
                new Chapter(true, new BossCaps(60, 75), true, true),
                new Chapter(true, new BossCaps(35, 40), true, true),
                new Chapter(true, new BossCaps(60, 75), true, true),
                true, 12.0);
    }

    /** The chapter switches for a boss kind (dragon: see dragon fields). */
    public Chapter chapter(BossKind kind) {
        return switch (kind) {
            case WITHER -> wither;
            case ELDER_GUARDIAN -> elderGuardian;
            case WARDEN -> warden;
            case ENDER_DRAGON -> Chapter.OFF;
        };
    }

    /** The participant-growth rule for a boss kind (0.2). The
     *  dragon is always OFF: its two-phase machine is its scaler. */
    public ScalingRule scaling(BossKind kind) {
        return switch (kind) {
            case WITHER -> witherScaling;
            case ELDER_GUARDIAN -> elderScaling;
            case WARDEN -> wardenScaling;
            case ENDER_DRAGON -> ScalingRule.OFF;
        };
    }

    /** Build from flat config keys (the adapter passes config.yml values). */
    public static BossTrialConfig fromMap(Map<String, Object> m) {
        BossTrialConfig d = defaults();
        if (m == null) {
            return d;
        }
        long cooldownMs = (long) (num(m, "credit.cooldown-hours", d.cooldownMs / 3600000.0) * 3600000.0);
        long elderRespawnMs = (long) (num(m, "elder-guardian.respawn-hours",
                d.elderRespawnMs / 3600000.0) * 3600000.0);
        return new BossTrialConfig(
                bool(m, "enabled", d.enabled),
                num(m, "credit.share-threshold", d.shareThreshold),
                num(m, "credit.presence-range", d.presenceRange),
                cooldownMs,
                bool(m, "spoils.enabled", d.spoilsEnabled),
                (int) num(m, "spoils.max-multiplier", d.spoilsCap),
                elderRespawnMs,
                chapterFrom(m, "wither", d.wither),
                chapterFrom(m, "elder-guardian", d.elderGuardian),
                chapterFrom(m, "warden", d.warden),
                bool(m, "dragon.presence", d.dragonPresence),
                num(m, "warden.share-height-band", d.wardenShareHeightBand),
                scalingFrom(m, "wither", d.witherScaling),
                scalingFrom(m, "elder-guardian", d.elderScaling),
                scalingFrom(m, "warden", d.wardenScaling),
                bool(m, "dragon.share-gate", d.dragonShareGate),
                num(m, "warden.share-range-band", d.wardenShareRangeBand),
                bool(m, "wither.fought-free-check", d.witherFoughtFreeCheck),
                num(m, "wither.fought-free-radius", d.witherFoughtFreeRadius),
                num(m, "wither.fought-free-window", d.witherFoughtFreeWindow),
                (long) (num(m, "warden.give-up-window-seconds",
                        d.wardenGiveUpWindowMs / 1000.0) * 1000.0));
    }

    private static ScalingRule scalingFrom(Map<String, Object> m, String prefix, ScalingRule d) {
        return new ScalingRule(
                bool(m, prefix + ".scaling.enabled", d.enabled()),
                num(m, prefix + ".scaling.base-health", d.baseHealth()),
                num(m, prefix + ".scaling.per-extra-damager", d.perExtraDamager()),
                (int) num(m, prefix + ".scaling.max-damagers-counted", d.maxDamagersCounted()));
    }

    private static Chapter chapterFrom(Map<String, Object> m, String prefix, Chapter d) {
        return new Chapter(
                bool(m, prefix + ".enabled", d.enabled()),
                new BossCaps(
                        num(m, prefix + ".caps.per-event-damage", d.caps().perEventDamage()),
                        num(m, prefix + ".caps.explosion-per-tick", d.caps().explosionPerTick())),
                bool(m, prefix + ".share-gate", d.shareGate()),
                bool(m, prefix + ".presence", d.presence()));
    }

    private static double clamp(double v, double lo, double hi) {
        if (Double.isNaN(v) || Double.isInfinite(v)) {
            return lo;
        }
        return Math.max(lo, Math.min(hi, v));
    }

    private static boolean bool(Map<String, Object> m, String key, boolean fallback) {
        Object o = m.get(key);
        return o instanceof Boolean b ? b : fallback;
    }

    private static double num(Map<String, Object> m, String key, double fallback) {
        Object o = m.get(key);
        return o instanceof Number n ? n.doubleValue() : fallback;
    }
}


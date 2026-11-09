/* Excel WRV maintenance; existing authorship retained. */ package com.winkreyes.excelbosstrial.core;

import java.util.Map;

/**
 * Every knob of ExcelBossTrial except the dragon chapter's own
 * TrialConfig, with the settled defaults (Mike, Oct 18, 2026).
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
     *  since 0.2.1 (Mike, Oct 20, 2026 late, the seal-difficulty
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
    /** Warden only (Mike, Oct 19, 2026 evening amendment): a hit
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
     *  in Mike's strict CAGED-AT-DEATH form, decided Oct 20,
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

    // ------------------------------------------------------------------
    // 0.3.0 (SPEC seal-tuning): intake, pressure, the repeat system
    // ------------------------------------------------------------------

    /**
     * One chapter's attrition pressure (SPEC section 3): the
     * outgoing-damage multiplier and the sustain pulse, both under
     * the fight-length ramp. Every field is a
     * {@code <chapter>.pressure.*} config key.
     */
    public record Pressure(boolean enabled, double outgoingMultiplier,
                           double pulseDamage, int pulseIntervalSeconds,
                           int pulseHotSeconds, double rampPerMinute, double rampMax,
                           boolean pulseRespectTrainingWheels,
                           boolean applyToSonicBoom) {
        public static final Pressure OFF = new Pressure(false, 1.0, 0.0, 20, 30, 0.0, 1.0, true, false);

        /** The settled defaults (SPEC section 3.5): wither/warden
         *  lean on the group at 1.25 outgoing; elder and dragon
         *  stay at 1.0 (their own mechanics carry their pressure);
         *  the pulse and ramp are uniform. 0.3.7: the pulse
         *  respects TrainingWheels everywhere (the settled fix -
         *  the pulse is source-less MAGIC damage, so the shield
         *  is applied here, by BossTrial, through the seam).
         *  0.5.3: the sonic-boom key's code fallback is FALSE for
         *  every chapter - the Item 39 retune ships as the
         *  default behavior, and only the WARDEN chapter can
         *  deal a sonic boom, so the field is inert elsewhere
         *  (SonicBoomRule). The R2 values themselves (warden
         *  outgoing 1.0 / ramp-max 1.5) ship as the bundled
         *  config's warden defaults; the settled strengths here
         *  are unchanged. */
        public static Pressure settled(BossKind kind) {
            return switch (kind) {
                case WITHER, WARDEN -> new Pressure(true, 1.25, 2.0, 20, 30, 0.06, 2.0, true, false);
                case ELDER_GUARDIAN, ENDER_DRAGON ->
                        new Pressure(true, 1.0, 2.0, 20, 30, 0.06, 2.0, true, false);
            };
        }
    }

    /**
     * The elder chapter's spike-reflect scale (0.3.5): the
     * multiplier applied to guardian spike reflect dealt by a
     * tracked seal elder during an encounter, INSTEAD of the
     * pressure scale (reflect is never ramped). Only the
     * guardian family has spikes, so the block belongs to this
     * chapter alone: {@code elder-guardian.reflect.multiplier}.
     * 1.0 = exact vanilla; 0.0 = reflect retired for seal
     * elders. The arithmetic lives in {@link ReflectRule}.
     */
    public record Reflect(double multiplier) {

        /** The settled default (Mike, Nov 2, 2026): reflect is
         *  retired for seal elders - the governed pool made the
         *  vanilla 3 HP spike a melee-only tax that compounded
         *  over hundreds of hits, and the attrition ramp doubled
         *  it late-fight. The key stays in config so a trace of
         *  it can be restored (e.g. 0.25) without a rebuild. */
        public static Reflect settled() {
            return new Reflect(0.0);
        }
    }

    /** One clear-count title row (SPEC section 4.7): at
     *  {@code clears} recorded clears of {@code sealId}, the
     *  champion earns {@code title}. */
    public record TitleRow(String sealId, int clears, String title) {
    }

    /** The platform MAX_HEALTH clamp the intake governor works
     *  under (SPEC section 0.3, rig-measured 1,024 on Paper
     *  26.3-235). {@code intake.entity-cap}. */
    public final double intakeEntityCap;

    public final Pressure witherPressure;
    public final Pressure elderPressure;
    public final Pressure wardenPressure;
    public final Pressure dragonPressure;

    /** The elder chapter's spike-reflect scale (0.3.5). Elder
     *  chapter only - no other boss kind has spikes. */
    public final Reflect elderReflect;

    /** The repeat system (SPEC sections 2 + 4). All keys live
     *  under {@code repeat.*} unless a chapter overrides them. */
    public final boolean repeatEnabled;
    public final double repeatResetHours;
    public final boolean repeatSignatureEnabled;
    public final boolean repeatAnnounce;
    public final int repeatJackpotExtraRolls;
    public final long repeatCashPerChampion;
    public final long repeatMcMmoXp;
    public final long repeatMagicLevelXp;
    public final double repeatClaimflyHours;
    public final boolean repeatDeathProtectionPremium;
    public final boolean repeatLostFoundVoucherEnabled;
    public final boolean healerRecognitionEnabled;
    /** Elder generation turnover: a completed trio's elders
     *  respawn this long after the completing verdict.
     *  {@code elder-guardian.repeat.respawn-hours}. */
    public final long elderRepeatRespawnMs;
    /** Per-chapter repeat-window overrides in hours; <= 0 reads
     *  the global repeatResetHours. {@code <chapter>.repeat.reset-hours}. */
    public final double witherRepeatResetHours;
    public final double elderRepeatResetHours;
    public final double wardenRepeatResetHours;
    public final double dragonRepeatResetHours;
    public final java.util.List<TitleRow> repeatTitles;

    /** A chapter's pressure block. */
    public Pressure pressure(BossKind kind) {
        return switch (kind) {
            case WITHER -> witherPressure;
            case ELDER_GUARDIAN -> elderPressure;
            case WARDEN -> wardenPressure;
            case ENDER_DRAGON -> dragonPressure;
        };
    }

    /** A chapter's spike-reflect scale (0.3.5): only the elder
     *  chapter has one - the other kinds return a neutral 1.0
     *  record (exact vanilla) and their handlers never consult
     *  it, because no other boss kind deals spike reflect. */
    public Reflect reflect(BossKind kind) {
        return kind == BossKind.ELDER_GUARDIAN ? elderReflect : new Reflect(1.0);
    }

    /** A seal's repeat window in milliseconds (chapter override wins). */
    public long repeatWindowMs(BossKind kind) {
        double hours = switch (kind) {
            case WITHER -> witherRepeatResetHours;
            case ELDER_GUARDIAN -> elderRepeatResetHours;
            case WARDEN -> wardenRepeatResetHours;
            case ENDER_DRAGON -> dragonRepeatResetHours;
        };
        if (hours <= 0) {
            hours = repeatResetHours;
        }
        return (long) (Math.max(0.0, hours) * 3600000.0);
    }

    /** The raid's repeat window (GuildGate owns the raid book;
     *  this is only the BossTrial-side default for its own reads). */
    public long repeatWindowMs() {
        return (long) (Math.max(0.0, repeatResetHours) * 3600000.0);
    }

    /** The settled default title table (SPEC section 4.7). */
    public static java.util.List<TitleRow> defaultTitles() {
        return java.util.List.of(
                new TitleRow("wither", 3, "Wither's Bane (x3)"),
                new TitleRow("wither", 5, "Gravewhisper's Doom (x5)"),
                new TitleRow("elder_guardian", 2, "Monument Breaker (x2)"),
                new TitleRow("elder_guardian", 4, "Curse's End (x4)"),
                new TitleRow("warden", 3, "Deep Dark's Defier (x3)"),
                new TitleRow("warden", 5, "The Silence Broken (x5)"),
                new TitleRow("ender_dragon", 2, "Twice-Crowned (x2)"),
                new TitleRow("ender_dragon", 4, "Dragon's Doom (x4)"),
                new TitleRow("raid", 3, "Thrice the Hero (x3)"),
                new TitleRow("raid", 5, "The Village Eternal (x5)"));
    }

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
        this(enabled, shareThreshold, presenceRange, cooldownMs, spoilsEnabled,
                spoilsCap, elderRespawnMs, wither, elderGuardian, warden,
                dragonPresence, wardenShareHeightBand, witherScaling,
                elderScaling, wardenScaling, dragonShareGate, wardenShareRangeBand,
                witherFoughtFreeCheck, witherFoughtFreeRadius, witherFoughtFreeWindow,
                wardenGiveUpWindowMs,
                1024.0,
                Pressure.settled(BossKind.WITHER), Pressure.settled(BossKind.ELDER_GUARDIAN),
                Pressure.settled(BossKind.WARDEN), Pressure.settled(BossKind.ENDER_DRAGON),
                true, 24.0, true, true, 2, 5000L, 5000L, 250L, 2.0, true, false, true,
                6L * 3600_000L, -1.0, -1.0, -1.0, -1.0, defaultTitles());
    }

    /** The 0.3.0 canonical constructor shape: the 0.3.5 elder
     *  reflect block at its settled default. */
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
                           long wardenGiveUpWindowMs,
                           double intakeEntityCap,
                           Pressure witherPressure, Pressure elderPressure,
                           Pressure wardenPressure, Pressure dragonPressure,
                           boolean repeatEnabled, double repeatResetHours,
                           boolean repeatSignatureEnabled, boolean repeatAnnounce,
                           int repeatJackpotExtraRolls, long repeatCashPerChampion,
                           long repeatMcMmoXp, long repeatMagicLevelXp,
                           double repeatClaimflyHours, boolean repeatDeathProtectionPremium,
                           boolean repeatLostFoundVoucherEnabled,
                           boolean healerRecognitionEnabled, long elderRepeatRespawnMs,
                           double witherRepeatResetHours, double elderRepeatResetHours,
                           double wardenRepeatResetHours, double dragonRepeatResetHours,
                           java.util.List<TitleRow> repeatTitles) {
        this(enabled, shareThreshold, presenceRange, cooldownMs, spoilsEnabled,
                spoilsCap, elderRespawnMs, wither, elderGuardian, warden,
                dragonPresence, wardenShareHeightBand, witherScaling,
                elderScaling, wardenScaling, dragonShareGate, wardenShareRangeBand,
                witherFoughtFreeCheck, witherFoughtFreeRadius, witherFoughtFreeWindow,
                wardenGiveUpWindowMs, intakeEntityCap, witherPressure, elderPressure,
                wardenPressure, dragonPressure, repeatEnabled, repeatResetHours,
                repeatSignatureEnabled, repeatAnnounce, repeatJackpotExtraRolls,
                repeatCashPerChampion, repeatMcMmoXp, repeatMagicLevelXp,
                repeatClaimflyHours, repeatDeathProtectionPremium,
                repeatLostFoundVoucherEnabled, healerRecognitionEnabled,
                elderRepeatRespawnMs, witherRepeatResetHours, elderRepeatResetHours,
                wardenRepeatResetHours, dragonRepeatResetHours, repeatTitles,
                Reflect.settled());
    }

    /** The 0.3.5 canonical constructor: everything above, plus the
     *  elder chapter's spike-reflect block. */
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
                           long wardenGiveUpWindowMs,
                           double intakeEntityCap,
                           Pressure witherPressure, Pressure elderPressure,
                           Pressure wardenPressure, Pressure dragonPressure,
                           boolean repeatEnabled, double repeatResetHours,
                           boolean repeatSignatureEnabled, boolean repeatAnnounce,
                           int repeatJackpotExtraRolls, long repeatCashPerChampion,
                           long repeatMcMmoXp, long repeatMagicLevelXp,
                           double repeatClaimflyHours, boolean repeatDeathProtectionPremium,
                           boolean repeatLostFoundVoucherEnabled,
                           boolean healerRecognitionEnabled, long elderRepeatRespawnMs,
                           double witherRepeatResetHours, double elderRepeatResetHours,
                           double wardenRepeatResetHours, double dragonRepeatResetHours,
                           java.util.List<TitleRow> repeatTitles,
                           Reflect elderReflect) {
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
        this.intakeEntityCap = clamp(intakeEntityCap, 1.0, 100000.0);
        this.witherPressure = witherPressure != null ? witherPressure : Pressure.OFF;
        this.elderPressure = elderPressure != null ? elderPressure : Pressure.OFF;
        this.wardenPressure = wardenPressure != null ? wardenPressure : Pressure.OFF;
        this.dragonPressure = dragonPressure != null ? dragonPressure : Pressure.OFF;
        this.repeatEnabled = repeatEnabled;
        this.repeatResetHours = Math.max(0.0, repeatResetHours);
        this.repeatSignatureEnabled = repeatSignatureEnabled;
        this.repeatAnnounce = repeatAnnounce;
        this.repeatJackpotExtraRolls = Math.max(0, repeatJackpotExtraRolls);
        this.repeatCashPerChampion = Math.max(0L, repeatCashPerChampion);
        this.repeatMcMmoXp = Math.max(0L, repeatMcMmoXp);
        this.repeatMagicLevelXp = Math.max(0L, repeatMagicLevelXp);
        this.repeatClaimflyHours = Math.max(0.0, repeatClaimflyHours);
        this.repeatDeathProtectionPremium = repeatDeathProtectionPremium;
        this.repeatLostFoundVoucherEnabled = repeatLostFoundVoucherEnabled;
        this.healerRecognitionEnabled = healerRecognitionEnabled;
        this.elderRepeatRespawnMs = Math.max(0L, elderRepeatRespawnMs);
        this.witherRepeatResetHours = witherRepeatResetHours;
        this.elderRepeatResetHours = elderRepeatResetHours;
        this.wardenRepeatResetHours = wardenRepeatResetHours;
        this.dragonRepeatResetHours = dragonRepeatResetHours;
        this.repeatTitles = repeatTitles == null
                ? java.util.List.of() : java.util.List.copyOf(repeatTitles);
        this.elderReflect = elderReflect != null ? elderReflect : Reflect.settled();
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
                        d.wardenGiveUpWindowMs / 1000.0) * 1000.0),
                num(m, "intake.entity-cap", d.intakeEntityCap),
                pressureFrom(m, "wither", d.witherPressure),
                pressureFrom(m, "elder-guardian", d.elderPressure),
                pressureFrom(m, "warden", d.wardenPressure),
                pressureFrom(m, "dragon", d.dragonPressure),
                bool(m, "repeat.enabled", d.repeatEnabled),
                num(m, "repeat.reset-hours", d.repeatResetHours),
                bool(m, "repeat.signature-enabled", d.repeatSignatureEnabled),
                bool(m, "repeat.announce", d.repeatAnnounce),
                (int) num(m, "repeat.rewards.jackpot.extra-rolls", d.repeatJackpotExtraRolls),
                (long) num(m, "repeat.rewards.cash-per-champion", d.repeatCashPerChampion),
                (long) num(m, "repeat.rewards.accelerants.mcmmo-xp", d.repeatMcMmoXp),
                (long) num(m, "repeat.rewards.accelerants.magiclevel-xp", d.repeatMagicLevelXp),
                num(m, "repeat.rewards.war-chest.claimfly-hours", d.repeatClaimflyHours),
                bool(m, "repeat.rewards.war-chest.death-protection-premium",
                        d.repeatDeathProtectionPremium),
                bool(m, "repeat.rewards.war-chest.lost-and-found-voucher-enabled",
                        d.repeatLostFoundVoucherEnabled),
                bool(m, "repeat.rewards.healer-recognition.enabled",
                        d.healerRecognitionEnabled),
                (long) (num(m, "elder-guardian.repeat.respawn-hours",
                        d.elderRepeatRespawnMs / 3600000.0) * 3600000.0),
                num(m, "wither.repeat.reset-hours", d.witherRepeatResetHours),
                num(m, "elder-guardian.repeat.reset-hours", d.elderRepeatResetHours),
                num(m, "warden.repeat.reset-hours", d.wardenRepeatResetHours),
                num(m, "dragon.repeat.reset-hours", d.dragonRepeatResetHours),
                titlesFrom(m, d.repeatTitles),
                reflectFrom(m, d.elderReflect));
    }

    /** The elder chapter's reflect block (0.3.5): one key,
     *  {@code elder-guardian.reflect.multiplier}; a config file
     *  missing the block runs the settled default, exactly as
     *  pressure does. The value itself is NOT clamped here -
     *  {@link ReflectRule#clampMultiplier} owns the applied
     *  shape (negative or NaN reads as 0) at the point of use. */
    private static Reflect reflectFrom(Map<String, Object> m, Reflect d) {
        return new Reflect(
                num(m, "elder-guardian.reflect.multiplier", d.multiplier()));
    }

    private static Pressure pressureFrom(Map<String, Object> m, String prefix, Pressure d) {
        return new Pressure(
                bool(m, prefix + ".pressure.enabled", d.enabled()),
                num(m, prefix + ".pressure.outgoing-multiplier", d.outgoingMultiplier()),
                num(m, prefix + ".pressure.pulse-damage", d.pulseDamage()),
                (int) num(m, prefix + ".pressure.pulse-interval-seconds",
                        d.pulseIntervalSeconds()),
                (int) num(m, prefix + ".pressure.pulse-hot-seconds", d.pulseHotSeconds()),
                num(m, prefix + ".pressure.ramp-per-minute", d.rampPerMinute()),
                num(m, prefix + ".pressure.ramp-max", d.rampMax()),
                bool(m, prefix + ".pressure.pulse-respect-training-wheels",
                        d.pulseRespectTrainingWheels()),
                bool(m, prefix + ".pressure.apply-to-sonic-boom",
                        d.applyToSonicBoom()));
    }

    /** The title table: a config list of {seal, clears, title}
     *  maps under repeat.titles; absent or malformed rows fall
     *  back to the default table row by row is overkill - a config
     *  that provides the key at all owns the whole table, and bad
     *  rows are skipped. */
    private static java.util.List<TitleRow> titlesFrom(Map<String, Object> m,
                                                       java.util.List<TitleRow> d) {
        Object raw = m.get("repeat.titles");
        if (!(raw instanceof java.util.List<?> list)) {
            return d;
        }
        java.util.List<TitleRow> out = new java.util.ArrayList<>();
        for (Object row : list) {
            if (row instanceof Map<?, ?> map) {
                Object seal = map.get("seal");
                Object clears = map.get("clears");
                Object title = map.get("title");
                if (seal != null && clears instanceof Number n && title != null
                        && !String.valueOf(title).isBlank()) {
                    out.add(new TitleRow(String.valueOf(seal),
                            Math.max(1, n.intValue()), String.valueOf(title)));
                }
            }
        }
        return java.util.List.copyOf(out);
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


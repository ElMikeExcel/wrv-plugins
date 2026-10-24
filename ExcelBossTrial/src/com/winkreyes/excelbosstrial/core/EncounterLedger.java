package com.winkreyes.excelbosstrial.core;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * One boss encounter's damage ledger, following the ExcelTelemetry
 * per-victim DamageLedger pattern: every governed damage event
 * against the boss is split into player-attributed and
 * non-player-attributed totals, with a per-player breakdown, and
 * every player the boss hurt is remembered.
 *
 * Player-attributed (SEAL-INTEGRITY-RESEARCH.md section 5.1): direct
 * melee, player-fired projectiles, player-lit/placed explosions and
 * tamed-pet damage. Everything else - suffocation, lava/fire/fall
 * environment, conduit magic, other mobs, unattributed explosions -
 * counts AGAINST the share, never toward it.
 *
 * A PARTICIPANT is a player who dealt attributed damage to the boss
 * or took damage from it during this encounter (Mike's Q7 definition,
 * Oct 18, 2026). Participants drive both the presence rule and the
 * spoils multiplier.
 *
 * Invariant the tests pin: playerDamage + otherDamage == totalDamage,
 * and playerDamage == sum(perPlayerDealt values).
 */
public final class EncounterLedger {

    private final BossKind kind;
    private final UUID bossUuid;
    /** The share denominator. Captured when the encounter opens;
     *  in 0.2 it can GROW mid-fight (participant growth - see
     *  {@link #growTo}) but never shrink: the pool is a ratchet. */
    private double maxHealth;
    private final long startedMs;
    private long lastTouchMs;

    private double totalDamage;
    private double playerDamage;
    private int shareExcludedHits;
    private double shareExcludedDamage;
    /** 0.2.1: the exclusion split by WHICH warden band failed, so
     *  the denial message can name the real rule (DenialMessage).
     *  shareExcludedHits stays the total across both bands. */
    private int heightExcludedHits;
    private double heightExcludedDamage;
    private int rangeExcludedHits;
    private double rangeExcludedDamage;
    /** 0.2.1 wither "fought free" facts (CreditJudge.witherNeverFought):
     *  how far the boss ever strayed from the position the
     *  encounter first saw it at (the summon point, for a fresh
     *  summon). Whether it ever fought back is read straight
     *  from takenByPlayer - see {@link #bossEverHitPlayer()}.
     *  The CAGED-AT-DEATH window reads damage-stamped samples
     *  instead: every noted position and every boss-on-player
     *  hit is stamped with the ledger's totalDamage at that
     *  moment, so the fight's final stretch (a trailing slice
     *  of dealt damage) can be re-examined exactly at the
     *  verdict, with no destructive re-anchoring mid-fight.
     *  Both lists are bounded by the fight's own event count
     *  and die with the ledger. */
    private boolean anchorSet;
    private double anchorX;
    private double anchorY;
    private double anchorZ;
    private double maxDisplacement;
    /** {totalDamageStamp, x, y, z} per sampled boss position. */
    private final java.util.List<double[]> positionSamples = new java.util.ArrayList<>();
    /** totalDamage stamps of every boss-on-player hit. */
    private final java.util.List<Double> takenStamps = new java.util.ArrayList<>();
    private final Map<UUID, Double> dealtByPlayer = new LinkedHashMap<>();
    private final Map<UUID, Double> takenByPlayer = new LinkedHashMap<>();
    private final BossGovernor governor = new BossGovernor();
    /** 0.2.2 (the warden give-up rule, WardenGiveUp): fight-evidence
     *  stamps for the give-up clock. firstDealtMs is the first
     *  governed dealt damage of any kind; lastCountedMs the most
     *  recent dealt blow that COUNTED toward the share;
     *  lastTakenMs the most recent blow the BOSS landed on a
     *  player. Counted damage and the boss's own landed blows both
     *  prove a fight is actually happening; excluded and
     *  unattributed damage proves nothing but time passing. 0 =
     *  never happened. */
    private long firstDealtMs;
    private long lastCountedMs;
    private long lastTakenMs;

    public EncounterLedger(BossKind kind, UUID bossUuid, double maxHealth, long nowMs) {
        this.kind = kind;
        this.bossUuid = bossUuid;
        this.maxHealth = maxHealth;
        this.startedMs = nowMs;
        this.lastTouchMs = nowMs;
    }

    public BossKind kind() {
        return kind;
    }

    public UUID bossUuid() {
        return bossUuid;
    }

    /** The boss's max HP bar: captured when the encounter opened,
     *  then the CURRENT scaled maximum once growth lands. The
     *  share gate always measures against this - the pool actually
     *  being fought (spec section 1). */
    public double maxHealth() {
        return maxHealth;
    }

    /**
     * Grow the share denominator to {@code newMax} (participant
     * growth, 0.2). A ratchet: a target at or below the current
     * maximum changes nothing. Returns the growth actually
     * applied (0 when none) so the adapter can raise the boss's
     * health bar by exactly that amount.
     */
    public synchronized double growTo(double newMax) {
        if (Double.isNaN(newMax) || newMax <= maxHealth) {
            return 0;
        }
        double delta = newMax - maxHealth;
        maxHealth = newMax;
        return delta;
    }

    /**
     * How many players have registered attributed dealt damage in
     * this encounter - the growth count of spec section 3. Taking
     * damage from the boss never adds to this number (a
     * non-combatant standing in the arena cannot inflate the
     * boss), and neither do share-excluded hits (the warden
     * height band records those as unattributed).
     */
    public synchronized int damagerCount() {
        int n = 0;
        for (double v : dealtByPlayer.values()) {
            if (v > 0) {
                n++;
            }
        }
        return n;
    }

    public long startedMs() {
        return startedMs;
    }

    public synchronized long lastTouchMs() {
        return lastTouchMs;
    }

    public BossGovernor governor() {
        return governor;
    }

    /**
     * Record damage dealt TO the boss.
     *
     * @param player     the responsible player when the damage is
     *                   player-attributed, else null (mob/environment)
     * @param amount     the governed final damage (post-caps)
     * @return true when this record registered a NEW damager (the
     *         player's first share-counting dealt damage of the
     *         encounter) - the participant-growth trigger of 0.2
     */
    public synchronized boolean recordDealt(UUID player, double amount, long nowMs) {
        return recordDealt(player, amount, nowMs, true);
    }

    /**
     * Record damage dealt TO the boss, with the share-eligibility
     * fact resolved by the caller (CreditJudge.countsTowardShare).
     * A hit that does not count toward the share - today only a
     * warden hit landed from outside the height band - behaves
     * EXACTLY like unattributed damage for every rule downstream:
     * it raises the total (the boss really lost the HP), it adds
     * nothing to the share, and it does not make the attacker a
     * participant. The damage itself is never reduced or blocked;
     * only the seal attribution is banded.
     */
    public synchronized boolean recordDealt(UUID player, double amount, long nowMs,
                                            boolean countsTowardShare) {
        // The pre-0.2.1 entry point: the only band that existed
        // was the height band, so an exclusion here is a height
        // exclusion (preserves the 0.2.0 ledger semantics exactly).
        return recordDealtCounted(player, amount, nowMs, countsTowardShare,
                !countsTowardShare, false);
    }

    /**
     * Record damage dealt TO the boss with the warden's two share
     * bands resolved separately (0.2.1): the hit counts toward the
     * share only when BOTH bands passed, and an exclusion is
     * tallied under each band that failed, so the denial message
     * names the rule that actually refused the blow. Everything
     * else behaves exactly like {@link #recordDealt}.
     */
    public synchronized boolean recordDealtBanded(UUID player, double amount, long nowMs,
                                                  boolean heightOk, boolean rangeOk) {
        return recordDealtCounted(player, amount, nowMs, heightOk && rangeOk,
                !heightOk, !rangeOk);
    }

    private boolean recordDealtCounted(UUID player, double amount, long nowMs,
                                       boolean countsTowardShare,
                                       boolean heightFailed, boolean rangeFailed) {
        if (amount <= 0 || Double.isNaN(amount)) {
            return false;
        }
        totalDamage += amount;
        if (firstDealtMs == 0) {
            firstDealtMs = nowMs;
        }
        boolean newDamager = false;
        if (player != null && countsTowardShare) {
            lastCountedMs = nowMs;
            newDamager = dealtBy(player) <= 0;
            playerDamage += amount;
            dealtByPlayer.merge(player, amount, Double::sum);
        }
        if (!countsTowardShare) {
            // The band facts, kept as counts (not just damage) so
            // the denial message can name the rule whenever even
            // ONE blow was excluded (Mike's seal feedback
            // doctrine, Oct 19, 2026).
            shareExcludedHits++;
            shareExcludedDamage += amount;
            if (heightFailed) {
                heightExcludedHits++;
                heightExcludedDamage += amount;
            }
            if (rangeFailed) {
                rangeExcludedHits++;
                rangeExcludedDamage += amount;
            }
        }
        lastTouchMs = nowMs;
        return newDamager;
    }

    /** Hits recorded as not counting toward the share (either band). */
    public synchronized int shareExcludedHits() {
        return shareExcludedHits;
    }

    /** Total damage of the share-excluded hits. */
    public synchronized double shareExcludedDamage() {
        return shareExcludedDamage;
    }

    /** Hits excluded because the HEIGHT band failed (0.2.1 split). */
    public synchronized int heightExcludedHits() {
        return heightExcludedHits;
    }

    /** Total damage of the height-excluded hits. */
    public synchronized double heightExcludedDamage() {
        return heightExcludedDamage;
    }

    /** Hits excluded because the RANGE band failed (0.2.1). */
    public synchronized int rangeExcludedHits() {
        return rangeExcludedHits;
    }

    /** Total damage of the range-excluded hits. */
    public synchronized double rangeExcludedDamage() {
        return rangeExcludedDamage;
    }

    /**
     * Note where the boss is (0.2.1, the wither "fought free"
     * check). The first note anchors the encounter; later notes
     * ratchet the farthest 3D distance from that anchor. The
     * adapter samples on governed damage events, which is where
     * a fighting boss's movement shows up. Each note is ALSO
     * kept as a damage-stamped sample for the caged-at-death
     * window (repeats at an unchanged stamp + position are
     * folded away).
     */
    public synchronized void noteBossPosition(double x, double y, double z) {
        int n = positionSamples.size();
        if (n == 0 || positionSamples.get(n - 1)[0] != totalDamage
                || positionSamples.get(n - 1)[1] != x
                || positionSamples.get(n - 1)[2] != y
                || positionSamples.get(n - 1)[3] != z) {
            positionSamples.add(new double[] {totalDamage, x, y, z});
        }
        if (!anchorSet) {
            anchorX = x;
            anchorY = y;
            anchorZ = z;
            anchorSet = true;
            return;
        }
        double d = CreditJudge.distance(anchorX, anchorY, anchorZ, x, y, z);
        if (d > maxDisplacement) {
            maxDisplacement = d;
        }
    }

    /**
     * The totalDamage stamp where a trailing window of
     * {@code windowSize} damage begins (the caged-at-death
     * window; windowSize is the adapter's fought-free fraction
     * of the boss's rallied pool).
     */
    private double windowFrom(double windowSize) {
        return totalDamage - Math.max(0, windowSize);
    }

    /**
     * True when the boss dealt damage to a player inside the
     * trailing window (0.2.1, the caged-at-death test). Hits are
     * stamped with the ledger total at the moment they landed,
     * which orders them in damage space against the window.
     */
    public synchronized boolean windowBossHitPlayer(double windowSize) {
        double from = windowFrom(windowSize);
        for (double stamp : takenStamps) {
            if (stamp >= from - 1e-9) {
                return true;
            }
        }
        return false;
    }

    /**
     * Farthest the boss strayed, inside the trailing window,
     * from where it stood when the window began (0.2.1, the
     * caged-at-death test). The window's start position is the
     * last sample stamped at or before the window's damage
     * boundary - or the earliest sample when the whole fight is
     * shorter than the window (a fully-caged life, which the
     * window then covers entire). No samples at all reads 0:
     * nothing ever recorded it moving.
     */
    public synchronized double windowMaxDisplacement(double windowSize) {
        if (positionSamples.isEmpty()) {
            return 0;
        }
        double from = windowFrom(windowSize);
        double[] start = positionSamples.get(0);
        for (double[] s : positionSamples) {
            if (s[0] <= from + 1e-9) {
                start = s;
            } else {
                break;
            }
        }
        double max = 0;
        for (double[] s : positionSamples) {
            if (s[0] >= from - 1e-9) {
                double d = CreditJudge.distance(start[1], start[2], start[3],
                        s[1], s[2], s[3]);
                if (d > max) {
                    max = d;
                }
            }
        }
        return max;
    }

    /** Farthest the boss ever strayed from the encounter anchor. */
    public synchronized double maxDisplacement() {
        return maxDisplacement;
    }

    /**
     * True when the boss dealt damage to a PLAYER at any point in
     * this encounter (0.2.1, the wither "fought free" check -
     * triple-check FLAG 2's precise definition: read from the
     * taken-records, which hold PLAYER victims only by adapter
     * contract, so a skull clipping a wolf or a pet never counts
     * as the boss having fought back). Whole-life by decision:
     * one landed hit, early or late, means it fought.
     */
    public synchronized boolean bossEverHitPlayer() {
        for (double v : takenByPlayer.values()) {
            if (v > 0) {
                return true;
            }
        }
        return false;
    }

    /** Record damage the boss dealt TO a player (participant evidence). */
    public synchronized void recordTaken(UUID player, double amount, long nowMs) {
        if (player == null || amount <= 0 || Double.isNaN(amount)) {
            return;
        }
        takenByPlayer.merge(player, amount, Double::sum);
        // Damage-stamped for the caged-at-death window (0.2.1).
        takenStamps.add(totalDamage);
        // The boss landing a blow is fight evidence (0.2.2).
        lastTakenMs = nowMs;
        lastTouchMs = nowMs;
    }

    /** First governed dealt damage of the encounter (0 = none yet). */
    public synchronized long firstDealtMs() {
        return firstDealtMs;
    }

    /** Most recent dealt blow that COUNTED toward the share (0 = none). */
    public synchronized long lastCountedMs() {
        return lastCountedMs;
    }

    /** Most recent blow the boss landed on a player (0 = none). */
    public synchronized long lastTakenMs() {
        return lastTakenMs;
    }

    /**
     * The warden give-up test (0.2.2): this encounter has run for
     * the full give-up window with no fight evidence since the
     * clock last restarted - see {@link WardenGiveUp}.
     */
    public synchronized boolean wardenGiveUpDue(long nowMs, long windowMs) {
        return WardenGiveUp.due(totalDamage, firstDealtMs, lastCountedMs,
                lastTakenMs, nowMs, windowMs);
    }

    public synchronized double totalDamage() {
        return totalDamage;
    }

    public synchronized double playerDamage() {
        return playerDamage;
    }

    public synchronized double otherDamage() {
        return totalDamage - playerDamage;
    }

    /** Player-attributed damage as a percentage of the boss's max HP. */
    public synchronized double playerSharePercent() {
        if (maxHealth <= 0) {
            return 0;
        }
        return playerDamage / maxHealth * 100.0;
    }

    /** True when player-attributed damage reaches the threshold share of max HP. */
    public synchronized boolean shareMet(double thresholdFraction) {
        if (maxHealth <= 0) {
            return false;
        }
        return playerDamage >= thresholdFraction * maxHealth - 1e-9;
    }

    /** Damage dealt by one player in this encounter (0 when none). */
    public synchronized double dealtBy(UUID player) {
        return dealtByPlayer.getOrDefault(player, 0.0);
    }

    /** Damage taken from the boss by one player in this encounter. */
    public synchronized double takenBy(UUID player) {
        return takenByPlayer.getOrDefault(player, 0.0);
    }

    /** Every participant: dealt attributed damage OR took boss damage. */
    public synchronized Set<UUID> participants() {
        Set<UUID> out = new LinkedHashSet<>();
        for (Map.Entry<UUID, Double> e : dealtByPlayer.entrySet()) {
            if (e.getValue() > 0) {
                out.add(e.getKey());
            }
        }
        for (Map.Entry<UUID, Double> e : takenByPlayer.entrySet()) {
            if (e.getValue() > 0) {
                out.add(e.getKey());
            }
        }
        return out;
    }

    public synchronized boolean isParticipant(UUID player) {
        return dealtBy(player) > 0 || takenBy(player) > 0;
    }

    /** Deep copy of the per-player dealt map (for logs/status). */
    public synchronized Map<UUID, Double> dealtSnapshot() {
        return new LinkedHashMap<>(dealtByPlayer);
    }
}

// ExcelBossTrial - a Wink Reyes Valley Excel plugin.

package com.winkreyes.rewardguard;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Fall-softening tracker (ExcelRewardGuard 1.2, Mike's decision, Oct 16,
 * 2026: "nerf the sky farm too — not the vanilla XP, everything else").
 *
 * The 1.0/1.1 provenance marks cannot see a sky farm: its mobs spawn
 * naturally in the dark at height, fall, and are finished with one hit, so
 * their origin is NATURAL and their kills pay full Jobs and mcMMO rewards.
 * This tracker closes that gap from the kill's shape instead of its birth:
 * it accumulates the FALL damage each living entity takes, and a kill whose
 * target's cumulative fall damage is at least farm-fall-damage-percent of
 * its max health counts as farmed, exactly like an origin-DENY kill.
 *
 * The rules, stated once, here:
 *   - CUMULATIVE, never forgiven. Fall damage only ever adds up over the
 *     tracked lifetime of the entry. Healing or regenerating does NOT
 *     reduce the total: a mob that fell for 60% of its health bar, healed
 *     back to full, and is then killed still counts as softened — its fall
 *     history is a fact about how the kill was set up, not about its
 *     current health. (Pinned by FallDamageTrackerTests.)
 *   - The ratio uses the entity's max health as observed at its MOST
 *     RECENT fall (buffs and gear can change max health mid-life), and the
 *     comparison is >= the threshold: exactly at the threshold counts.
 *   - Only entities that actually took fall damage get an entry, so the
 *     map stays proportional to "mobs that fell", not "mobs alive".
 *   - Entries are removed when the entity dies, is removed from the world,
 *     or its chunk unloads, and the whole map is cleared when the plugin
 *     disables, so nothing leaks across reloads.
 *
 * Accepted false positive, inherent to the signature: a mob that falls
 * down a ravine entirely on its own and is finished off by a player at or
 * above the threshold counts as farmed. There is no signal that separates
 * "fell in a built farm" from "fell in a natural ravine"; the policy errs
 * toward the farm side, the same bias 1.0/1.1 documented for the mcMMO
 * suppression window.
 *
 * Pure logic on purpose: no Bukkit types (entity ids are UUIDs, damage is
 * doubles), so the policy is unit-testable without a server, same as
 * XpSuppression and RewardScale.
 */
public final class FallDamageTracker {
    public static final int DEFAULT_THRESHOLD_PERCENT = 50;

    /** The classification label for fall-softened kills, used in log lines and as the farmed reason. */
    public static final String FALL_SOFTENED_REASON = "fall_softened";

    private record FallRecord(double cumulativeFallDamage, double maxHealth) { }

    private final Map<UUID, FallRecord> records = new ConcurrentHashMap<>();

    /**
     * Adds one fall's final damage to the entity's cumulative total and
     * refreshes its observed max health. Calls with no real damage, a
     * non-positive (or NaN) max health, or a null id are ignored: they can
     * never create or extend an entry.
     */
    public void recordFall(UUID entityId, double fallDamage, double maxHealth) {
        if (entityId == null || !(fallDamage > 0.0) || !(maxHealth > 0.0)) return;
        records.merge(entityId, new FallRecord(fallDamage, maxHealth),
            (existing, incoming) -> new FallRecord(
                existing.cumulativeFallDamage() + incoming.cumulativeFallDamage(), incoming.maxHealth()));
    }

    public boolean hasRecord(UUID entityId) {
        return entityId != null && records.containsKey(entityId);
    }

    /** Cumulative fall damage recorded for the entity; 0 when untracked. */
    public double fallDamage(UUID entityId) {
        FallRecord record = entityId == null ? null : records.get(entityId);
        return record == null ? 0.0 : record.cumulativeFallDamage();
    }

    /** Cumulative fall damage as a percent of the most recently observed max health; 0 when untracked. */
    public double fallDamagePercent(UUID entityId) {
        FallRecord record = entityId == null ? null : records.get(entityId);
        if (record == null || record.maxHealth() <= 0.0) return 0.0;
        return record.cumulativeFallDamage() * 100.0 / record.maxHealth();
    }

    /**
     * True when the entity's cumulative fall damage reaches the threshold
     * percent of its max health. At a threshold of 0, any recorded fall
     * damage at all qualifies (entries only exist when damage was taken).
     */
    public boolean isSoftened(UUID entityId, int thresholdPercent) {
        FallRecord record = entityId == null ? null : records.get(entityId);
        if (record == null || record.maxHealth() <= 0.0) return false;
        if (thresholdPercent <= 0) return record.cumulativeFallDamage() > 0.0;
        return record.cumulativeFallDamage() * 100.0 >= record.maxHealth() * thresholdPercent;
    }

    /** Drops one entity's entry (death, removal). Removing an untracked id is a no-op. */
    public void remove(UUID entityId) {
        if (entityId != null) records.remove(entityId);
    }

    /** Drops many entries at once (chunk unload). */
    public void removeAll(Collection<UUID> entityIds) {
        if (entityIds != null) entityIds.forEach(this::remove);
    }

    /** Drops every entry (plugin disable, so a reload starts from an empty map). */
    public void clear() {
        records.clear();
    }

    public int size() {
        return records.size();
    }

    /**
     * Composes the two farm signals into one classification, so a kill is
     * farmed at most once and never scaled twice: the birth-origin verdict
     * wins the label when both signals fire (a spawner-born mob that also
     * fell is a spawner kill in the log), and the fall label applies only
     * when the origin did not already deny the kill. Returns the reason
     * string, or null when the kill is not farmed by either signal.
     */
    public static String farmedReason(boolean originDenied, String originReason, boolean fallSoftened) {
        if (originDenied) return originReason;
        return fallSoftened ? FALL_SOFTENED_REASON : null;
    }
}

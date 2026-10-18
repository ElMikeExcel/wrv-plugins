package com.winkreyes.excelguildgate;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Pure seal-progress state for the Road to the Guild campaign.
 *
 * No Bukkit dependencies, so the whole state machine is unit-testable on a
 * plain JVM (see tests/com/winkreyes/excelguildgate/SealStateTest.java).
 *
 * Rules (family-shared, forgiving by design):
 * - Only ENABLED seals are required for the unlock. Disabled seals are
 *   excluded from the required set and from the progress counts.
 * - credit() is the kill path: unknown ids and disabled seals are no-ops,
 *   and breaking an already-broken seal is a no-op (the original
 *   timestamp and killer are kept).
 * - grant()/revoke() are the admin path: they work on any known seal,
 *   including disabled ones (a record is kept, but a disabled seal never
 *   counts toward the unlock while it stays disabled).
 * - Unlocked is derived: unlocked exactly when every enabled seal is
 *   broken. With zero enabled seals the state is (vacuously) unlocked.
 * - unlockTimestampMillis tracks the start of the current unlock episode:
 *   it is set when the state flips locked -> unlocked and cleared when it
 *   flips back (revoke / reset / a newly enabled unbroken seal).
 */
public final class SealState {

    /** One broken seal's record. Killer may be null (unknown / admin grant / raid). */
    public record SealRecord(long timestampMillis, String killer) {}

    public enum CreditResult {CREDITED, ALREADY_BROKEN, NOT_A_SEAL, DISABLED}

    private final Set<String> allSeals;
    private final Set<String> enabledSeals;
    private final Map<String, SealRecord> broken = new LinkedHashMap<>();
    private long unlockTimestampMillis;

    public SealState(Set<String> allSeals, Set<String> enabledSeals) {
        this.allSeals = Collections.unmodifiableSet(new LinkedHashSet<>(allSeals));
        LinkedHashSet<String> en = new LinkedHashSet<>(enabledSeals);
        en.retainAll(this.allSeals);
        this.enabledSeals = Collections.unmodifiableSet(en);
        this.unlockTimestampMillis = 0L;
    }

    public Set<String> allSeals() {
        return allSeals;
    }

    public Set<String> enabledSeals() {
        return enabledSeals;
    }

    public boolean isKnown(String sealId) {
        return sealId != null && allSeals.contains(sealId);
    }

    public boolean isEnabled(String sealId) {
        return sealId != null && enabledSeals.contains(sealId);
    }

    public synchronized boolean isBroken(String sealId) {
        return sealId != null && broken.containsKey(sealId);
    }

    public synchronized SealRecord record(String sealId) {
        return sealId == null ? null : broken.get(sealId);
    }

    /** Snapshot of every broken seal's record (including disabled seals). */
    public synchronized Map<String, SealRecord> snapshot() {
        return new LinkedHashMap<>(broken);
    }

    /** Number of ENABLED seals broken (the numerator of the progress line). */
    public synchronized int brokenCount() {
        int n = 0;
        for (String id : enabledSeals) {
            if (broken.containsKey(id)) {
                n++;
            }
        }
        return n;
    }

    /** Number of seals required for the unlock. */
    public int requiredCount() {
        return enabledSeals.size();
    }

    public synchronized boolean isUnlocked() {
        return broken.keySet().containsAll(enabledSeals);
    }

    public synchronized long unlockTimestampMillis() {
        return isUnlocked() ? unlockTimestampMillis : 0L;
    }

    /** Kill path. See class docs for the no-op cases. */
    public synchronized CreditResult credit(String sealId, String killer, long nowMillis) {
        if (!isKnown(sealId)) {
            return CreditResult.NOT_A_SEAL;
        }
        if (!isEnabled(sealId)) {
            return CreditResult.DISABLED;
        }
        if (broken.containsKey(sealId)) {
            return CreditResult.ALREADY_BROKEN;
        }
        broken.put(sealId, new SealRecord(nowMillis, killer));
        refreshUnlock(nowMillis);
        return CreditResult.CREDITED;
    }

    /** Admin path: mark a known seal broken. Returns true if state changed. */
    public synchronized boolean grant(String sealId, long nowMillis) {
        if (!isKnown(sealId) || broken.containsKey(sealId)) {
            return false;
        }
        broken.put(sealId, new SealRecord(nowMillis, null));
        refreshUnlock(nowMillis);
        return true;
    }

    /** Admin path: un-break a seal. Returns true if state changed. */
    public synchronized boolean revoke(String sealId) {
        if (!isKnown(sealId) || broken.remove(sealId) == null) {
            return false;
        }
        refreshUnlock(0L);
        return true;
    }

    /** Admin path: break every known seal at once. Returns true if state changed. */
    public synchronized boolean openAll(long nowMillis) {
        boolean changed = false;
        for (String id : allSeals) {
            if (!broken.containsKey(id)) {
                broken.put(id, new SealRecord(nowMillis, null));
                changed = true;
            }
        }
        refreshUnlock(nowMillis);
        return changed;
    }

    /** Admin path: clear everything and relock. */
    public synchronized void reset() {
        broken.clear();
        unlockTimestampMillis = 0L;
    }

    /**
     * Restore persisted state (used on load and by tests). Records for
     * unknown seal ids are ignored. A stored unlock timestamp is kept only
     * if the restored state is actually unlocked.
     */
    public synchronized void restore(Map<String, SealRecord> records, long storedUnlockTimestampMillis) {
        broken.clear();
        if (records != null) {
            for (Map.Entry<String, SealRecord> e : records.entrySet()) {
                if (isKnown(e.getKey()) && e.getValue() != null) {
                    broken.put(e.getKey(), e.getValue());
                }
            }
        }
        unlockTimestampMillis = isUnlocked() ? Math.max(0L, storedUnlockTimestampMillis) : 0L;
    }

    private void refreshUnlock(long nowMillis) {
        if (isUnlocked()) {
            if (unlockTimestampMillis == 0L) {
                unlockTimestampMillis = nowMillis;
            }
        } else {
            unlockTimestampMillis = 0L;
        }
    }
}

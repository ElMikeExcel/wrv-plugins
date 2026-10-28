package com.winkreyes.excelbosstrial.core;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The seal-attempt announcement window (0.2.3, owner, Oct 22,
 * 2026): for a 2-day window, attempts, fails, breaks and running
 * credit cooldowns are public — announced in chat as they happen,
 * repeated hourly, and rebroadcast to everyone whenever a player
 * joins. This class holds the window's pure decisions; the plugin
 * class owns the Bukkit glue (registries, broadcasts, the join
 * event). Everything here is gated on the same window check, so
 * the moment the window closes the whole feature goes quiet and
 * the verdict broadcasts revert to exactly their pre-0.2.3
 * behavior.
 *
 * Settled edges (build record, DESIGN-0.2.3):
 * - The window is active strictly while now < until; an unset
 *   until (0) or the master switch off means no window at all.
 * - Multiple seals on cooldown share ONE combined hourly line.
 * - A per-seal rally throttle keeps a re-opened seal from
 *   machine-gunning chat; it stamps only when it grants.
 */
public final class Announcements {

    private Announcements() {
    }

    /** True while the announcement window is live. */
    public static boolean windowActive(boolean enabled, long untilMs, long nowMs) {
        return enabled && untilMs > 0L && nowMs < untilMs;
    }

    /** True when the hourly cooldown announcement is due. */
    public static boolean hourlyDue(long lastBroadcastMs, long intervalMs, long nowMs) {
        return nowMs - lastBroadcastMs >= intervalMs;
    }

    /** One seal's slice of the combined cooldown line. */
    public record CooldownPart(String displayName, long remainingMs) {
    }

    /**
     * The combined cooldown list text: "The Warden seal (5h 42m
     * left), The Wither seal (12m left)". Empty string when no
     * seal is cooling down (callers stay silent on empty).
     * Parts render in the order given (the adapter passes
     * BossKind enum order).
     */
    public static String cooldownListText(List<CooldownPart> parts) {
        StringBuilder sb = new StringBuilder();
        for (CooldownPart part : parts) {
            if (part == null || part.remainingMs() <= 0L) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(part.displayName()).append(" seal (")
                    .append(CooldownBook.formatRemaining(part.remainingMs()))
                    .append(" left)");
        }
        return sb.toString();
    }

    /**
     * Per-seal rally-announce throttle: a seal's attempt line may
     * broadcast at most once per cooldown window of time, however
     * many encounters open inside it. Stamps only when it grants,
     * so a denied try never extends the wait.
     */
    public static final class RallyThrottle {

        private final Map<String, Long> lastGrantedBySeal = new LinkedHashMap<>();

        /** True (and stamped) when this seal may announce now. */
        public synchronized boolean tryAnnounce(String sealId, long nowMs, long cooldownMs) {
            Long last = lastGrantedBySeal.get(sealId);
            if (last != null && nowMs - last < cooldownMs) {
                return false;
            }
            lastGrantedBySeal.put(sealId, nowMs);
            return true;
        }

        /** The last granted instant for a seal (0 when never). */
        public synchronized long lastAnnouncedMs(String sealId) {
            return lastGrantedBySeal.getOrDefault(sealId, 0L);
        }
    }
}

// ExcelBossTrial - a Wink Reyes Valley Excel plugin.


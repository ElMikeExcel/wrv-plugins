package com.winkreyes.excelguide.core;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The Guide's manners, as clocks (SPEC guardrail 5): a per-player gap
 * between answers, a per-question gap (the same normalized question is
 * answered once, then the Guide stays quiet), and a global cap on
 * Guide messages per minute. Time comes from an injected millisecond
 * supplier so tests drive a fake clock.
 *
 * A "continuation" acquire is the second half of a clarify exchange:
 * it skips the per-player and per-question gaps (the player answered
 * within seconds, by design) but still counts toward the global cap.
 */
public final class RateLimiter {

    public enum Verdict { OK, PLAYER_COOLDOWN, QUESTION_COOLDOWN, GLOBAL_LIMIT }

    private final GuideConfig cfg;
    private final Map<UUID, Long> lastByPlayer = new HashMap<>();
    private final Map<String, Long> lastByQuestion = new HashMap<>();
    private final Deque<Long> globalWindow = new ArrayDeque<>();

    public RateLimiter(GuideConfig cfg) {
        this.cfg = cfg;
    }

    /**
     * Check and (on OK) record an answer of {@code lines} messages to
     * {@code player} for {@code questionKey} at {@code nowMillis}.
     */
    public synchronized Verdict tryAcquire(UUID player, String questionKey,
                                           int lines, boolean continuation, long nowMillis) {
        prune(nowMillis);
        if (globalWindow.size() + Math.max(1, lines) > cfg.globalPerMinute) {
            return Verdict.GLOBAL_LIMIT;
        }
        if (!continuation) {
            Long last = lastByPlayer.get(player);
            if (last != null && nowMillis - last < cfg.perPlayerSeconds * 1000L) {
                return Verdict.PLAYER_COOLDOWN;
            }
            if (questionKey != null) {
                Long lq = lastByQuestion.get(questionKey);
                if (lq != null && nowMillis - lq < cfg.perQuestionSeconds * 1000L) {
                    return Verdict.QUESTION_COOLDOWN;
                }
            }
        }
        for (int i = 0; i < Math.max(1, lines); i++) {
            globalWindow.addLast(nowMillis);
        }
        lastByPlayer.put(player, nowMillis);
        if (questionKey != null) {
            lastByQuestion.put(questionKey, nowMillis);
        }
        return Verdict.OK;
    }

    /** How many Guide messages went out in the trailing minute. */
    public synchronized int globalUsed(long nowMillis) {
        prune(nowMillis);
        return globalWindow.size();
    }

    /**
     * How long until an acquire with these parameters would succeed
     * (0.2.2) - the longest wait across every bucket that currently
     * blocks, 0 when nothing blocks. Mirrors {@link #tryAcquire}'s
     * checks exactly (continuations skip the per-player and
     * per-question gaps) but records nothing. The cooldown notice
     * quotes this number, so it is computed from the same state and
     * the same clock the block itself used.
     */
    public synchronized long remainingMillis(UUID player, String questionKey,
                                             int lines, boolean continuation, long nowMillis) {
        prune(nowMillis);
        long wait = 0;
        int need = Math.max(1, lines);
        if (globalWindow.size() + need > cfg.globalPerMinute) {
            // The (excess)-th oldest stamp must age out of the window.
            int excess = globalWindow.size() + need - cfg.globalPerMinute;
            int seen = 0;
            for (Long stamp : globalWindow) {
                if (++seen == excess) {
                    wait = Math.max(wait, stamp + 60_000L - nowMillis);
                    break;
                }
            }
        }
        if (!continuation) {
            Long last = lastByPlayer.get(player);
            if (last != null) {
                wait = Math.max(wait, last + cfg.perPlayerSeconds * 1000L - nowMillis);
            }
            if (questionKey != null) {
                Long lq = lastByQuestion.get(questionKey);
                if (lq != null) {
                    wait = Math.max(wait, lq + cfg.perQuestionSeconds * 1000L - nowMillis);
                }
            }
        }
        return Math.max(0, wait);
    }

    private void prune(long nowMillis) {
        long cutoff = nowMillis - 60_000L;
        while (!globalWindow.isEmpty() && globalWindow.peekFirst() <= cutoff) {
            globalWindow.removeFirst();
        }
    }
}

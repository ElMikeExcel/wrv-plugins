package com.winkreyes.excelguide.core;

import java.util.UUID;

/** Cooldowns and rate limits against raw timestamps (SPEC guardrail 5). */
public final class RateLimiterTest {

    private static final UUID A = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID B = UUID.fromString("22222222-2222-2222-2222-222222222222");

    public static void main(String[] args) {
        TestSupport.T t = new TestSupport.T();
        GuideConfig cfg = new GuideConfig(); // defaults: 20s player, 60s question, 6/min global
        long t0 = 1_000_000L;

        RateLimiter rl = new RateLimiter(cfg);
        t.eq("first answer ok", RateLimiter.Verdict.OK, rl.tryAcquire(A, "q1", 1, false, t0));
        t.eq("same player too soon", RateLimiter.Verdict.PLAYER_COOLDOWN,
                rl.tryAcquire(A, "q2", 1, false, t0 + 5_000));
        t.eq("other player fine", RateLimiter.Verdict.OK, rl.tryAcquire(B, "q2", 1, false, t0 + 5_000));
        t.eq("same question from someone else inside 60s", RateLimiter.Verdict.QUESTION_COOLDOWN,
                rl.tryAcquire(B, "q1", 1, false, t0 + 30_000));
        t.eq("player gap over at 20s", RateLimiter.Verdict.OK,
                rl.tryAcquire(A, "q3", 1, false, t0 + 21_000));
        t.eq("question gap over at 60s", RateLimiter.Verdict.OK,
                rl.tryAcquire(B, "q1", 1, false, t0 + 61_000));

        // Continuations skip the personal gaps (clarify replies).
        RateLimiter rl2 = new RateLimiter(cfg);
        t.eq("seed answer", RateLimiter.Verdict.OK, rl2.tryAcquire(A, "q", 1, false, t0));
        t.eq("continuation right after", RateLimiter.Verdict.OK,
                rl2.tryAcquire(A, "q", 1, true, t0 + 2_000));

        // Global cap: 6 messages a minute, lines count individually.
        RateLimiter rl3 = new RateLimiter(cfg);
        long now = t0;
        boolean allOk = true;
        for (int i = 0; i < 3; i++) {
            allOk &= rl3.tryAcquire(UUID.randomUUID(), "qq" + i, 2, false, now) == RateLimiter.Verdict.OK;
        }
        t.check("three 2-line answers fit the cap", allOk);
        t.eq("seventh message blocked", RateLimiter.Verdict.GLOBAL_LIMIT,
                rl3.tryAcquire(UUID.randomUUID(), "qx", 1, false, now));
        t.eq("cap is per trailing minute", RateLimiter.Verdict.OK,
                rl3.tryAcquire(UUID.randomUUID(), "qx", 1, false, now + 61_000));
        t.eq("global window prunes", 1, rl3.globalUsed(now + 61_000));

        t.finish("RateLimiterTest");
    }
}

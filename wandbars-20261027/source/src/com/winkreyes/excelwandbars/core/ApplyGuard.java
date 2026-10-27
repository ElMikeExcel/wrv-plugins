package com.winkreyes.excelwandbars.core;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Loop breaker for re-configuring a wand. Re-configuring re-activates the
 * wand, which fires Magic's activation event again; if Magic ever refused the
 * new values, the plugin would retry forever. This allows at most
 * maxPerWindow applies per player in windowMs, then refuses until the window
 * passes (each refusal is logged as THROTTLED).
 */
public final class ApplyGuard {
    private final int maxPerWindow;
    private final long windowMs;
    private final Map<UUID, Deque<Long>> hits = new HashMap<>();

    public ApplyGuard(int maxPerWindow, long windowMs) {
        this.maxPerWindow = Math.max(1, maxPerWindow);
        this.windowMs = Math.max(1, windowMs);
    }

    public boolean allow(UUID id, long now) {
        Deque<Long> d = hits.computeIfAbsent(id, k -> new ArrayDeque<>());
        while (!d.isEmpty() && (now - d.peekFirst() >= windowMs || now < d.peekFirst())) d.pollFirst();
        if (d.size() >= maxPerWindow) return false;
        d.addLast(now);
        return true;
    }

    public void forget(UUID id) { hits.remove(id); }
}


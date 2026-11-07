package com.winkreyes.excelautosort.core;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Recovery-buffer arithmetic (SPEC §6.2): the window, the cap, and
 * which entries a purge takes. Pure JDK; the Bukkit layer owns the
 * serialized payloads and just follows these plans.
 */
public final class BufferCore {

    private BufferCore() {}

    /** The timing facts of one buffer entry (payload lives in the store). */
    public record TimedEntry(String id, long removedAtMs, long expiresAtMs) {}

    public enum PurgeReason { EXPIRED, OVERFLOW }

    public record Purge(String id, PurgeReason reason) {}

    /**
     * What must go, in the order it must go: everything expired first
     * (earliest expiry first), then — if the remainder still exceeds
     * the cap — the oldest removals until the cap holds (§6.2: overflow
     * purges the oldest entries first). Expired entries do not count
     * toward the cap once purged.
     */
    public static List<Purge> purgePlan(List<TimedEntry> entries, long nowMs, int maxStacks) {
        List<Purge> out = new ArrayList<>();
        List<TimedEntry> alive = new ArrayList<>();
        List<TimedEntry> expired = new ArrayList<>();
        for (TimedEntry e : entries) {
            if (e.expiresAtMs() <= nowMs) expired.add(e);
            else alive.add(e);
        }
        expired.sort(Comparator.comparingLong(TimedEntry::expiresAtMs));
        for (TimedEntry e : expired) out.add(new Purge(e.id(), PurgeReason.EXPIRED));
        if (maxStacks >= 0 && alive.size() > maxStacks) {
            alive.sort(Comparator.comparingLong(TimedEntry::removedAtMs));
            int excess = alive.size() - maxStacks;
            for (int i = 0; i < excess; i++) out.add(new Purge(alive.get(i).id(), PurgeReason.OVERFLOW));
        }
        return out;
    }

    /** Display order for the recovery GUI/form: most recently removed first. */
    public static List<TimedEntry> displayOrder(List<TimedEntry> entries) {
        List<TimedEntry> out = new ArrayList<>(entries);
        out.sort(Comparator.comparingLong(TimedEntry::removedAtMs).reversed());
        return out;
    }

    /** Human "time left" for one entry, whole units, never negative. */
    public static String timeLeft(long expiresAtMs, long nowMs) {
        long ms = Math.max(0, expiresAtMs - nowMs);
        long days = ms / 86_400_000L;
        if (days >= 1) return days + (days == 1 ? " day" : " days");
        long hours = ms / 3_600_000L;
        if (hours >= 1) return hours + (hours == 1 ? " hour" : " hours");
        long mins = ms / 60_000L;
        if (mins >= 1) return mins + (mins == 1 ? " minute" : " minutes");
        return "under a minute";
    }
}

// ExcelAutoSort — Iris / Mike, for Wink Reyes Valley

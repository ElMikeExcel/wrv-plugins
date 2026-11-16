/* Excel WRV maintenance; existing authorship retained. */ package com.winkreyes.excelbosstrial.core;

import java.util.Map;
import java.util.UUID;

/**
 * Elder partial-progress expiry (0.5.6; seal-cycle law (a) +
 * design E-1..E-4/T-3): the window opens at the FIRST counted
 * elder, does not slide, expires a 1/3 or 2/3 at 24 h into a
 * respawn, and never touches a completed trio.
 */
public final class MonumentExpiryTest {

    private static int passed;
    private static int failed;

    private static void check(boolean cond, String label) {
        if (cond) {
            passed++;
        } else {
            failed++;
            System.out.println("FAIL: " + label);
        }
    }

    private static final long H = 3600000L;

    private static MonumentTracker trackerWithTrio() {
        MonumentTracker t = new MonumentTracker();
        t.register(UUID.randomUUID(), "world", 0, 64, 0);
        return t;
    }

    public static void main(String[] args) {
        // Build a monument with three elders.
        MonumentTracker t = new MonumentTracker();
        UUID e1 = UUID.randomUUID();
        UUID e2 = UUID.randomUUID();
        UUID e3 = UUID.randomUUID();
        t.register(e1, "world", 0, 64, 0);
        t.register(e2, "world", 10, 64, 0);
        t.register(e3, "world", 20, 64, 0);
        String mid = t.monumentOf(e1);
        check(mid != null && mid.equals(t.monumentOf(e2)) && mid.equals(t.monumentOf(e3)),
                "three elders cluster into one monument");

        long t0 = 1_000_000L;
        check(t.windowStartMs(mid) == 0L, "no window before any counted elder");
        check(!t.progressExpired(mid, t0 + 100 * H, 24 * H), "no progress cannot expire");

        // First counted elder opens the window at ITS timestamp.
        check(t.markCounted(e1, t0) == 1, "first elder counts: progress 1");
        check(t.windowStartMs(mid) == t0, "window opens at the first counted kill");
        check(!t.progressExpired(mid, t0 + 23 * H, 24 * H), "1/3 alive at 23h");
        check(t.progressExpired(mid, t0 + 24 * H, 24 * H), "1/3 expires at 24h");
        check(t.windowRemainingMs(mid, t0 + 20 * H, 24 * H) == 4 * H, "remaining reads 4h at 20h");

        // The window does NOT slide: a second elder at +20h
        // leaves the deadline at t0+24h.
        check(t.markCounted(e2, t0 + 20 * H) == 2, "second elder counts: progress 2");
        check(t.windowStartMs(mid) == t0, "window start stays at the FIRST kill");
        check(t.progressExpired(mid, t0 + 24 * H, 24 * H), "2/3 still expires at first-kill +24h");
        check(!t.progressExpired(mid, t0 + 24 * H, 0L), "expiry 0 disables expiry entirely");

        // Expiry flips the counted elders to a due respawn and
        // zeroes progress; the surviving elder stays TRACKED.
        check(t.expirePartialTrio(mid, t0 + 24 * H) == 2, "expiry flips both counted elders");
        check(t.progress(mid) == 0, "progress returns to 0 of 3");
        check(t.dueRespawns(t0 + 24 * H).size() == 2, "both flipped elders are due to respawn");
        check(t.record(e3).status == MonumentTracker.ElderStatus.TRACKED, "survivor stays tracked");
        check(t.expirePartialTrio(mid, t0 + 25 * H) == 0, "expiring an empty monument is a no-op");

        // A completed trio REFUSES expiry (generation turnover
        // owns it) and never reads as expired.
        MonumentTracker full = new MonumentTracker();
        UUID f1 = UUID.randomUUID();
        UUID f2 = UUID.randomUUID();
        UUID f3 = UUID.randomUUID();
        full.register(f1, "world", 0, 64, 0);
        full.register(f2, "world", 10, 64, 0);
        full.register(f3, "world", 20, 64, 0);
        String fmid = full.monumentOf(f1);
        full.markCounted(f1, t0);
        full.markCounted(f2, t0 + H);
        full.markCounted(f3, t0 + 2 * H);
        check(full.trioComplete(fmid), "trio complete");
        check(!full.progressExpired(fmid, t0 + 100 * H, 24 * H), "complete trio never expires");
        check(full.expirePartialTrio(fmid, t0 + 100 * H) == 0, "expirePartialTrio refuses a complete trio");
        check(full.progress(fmid) == 3, "complete trio progress untouched by expiry attempt");

        // Snapshot round-trip carries the timestamps.
        MonumentTracker src = new MonumentTracker();
        UUID s1 = UUID.randomUUID();
        src.register(s1, "world", 0, 64, 0);
        String smid = src.monumentOf(s1);
        src.markCounted(s1, t0 + 5 * H);
        MonumentTracker dst = new MonumentTracker();
        dst.restore(src.snapshot());
        check(dst.windowStartMs(smid) == t0 + 5 * H, "countedAtMs survives snapshot round-trip");
        check(dst.progressExpired(smid, t0 + 30 * H, 24 * H), "restored progress can expire");

        // Migration: a pre-timestamp counted record (countedAtMs
        // 0 in the snapshot) gets a fresh window at boot.
        Map<String, Object> snap = src.snapshot();
        @SuppressWarnings("unchecked")
        java.util.List<Object> elders = (java.util.List<Object>) snap.get("elders");
        @SuppressWarnings("unchecked")
        Map<String, Object> row = (Map<String, Object>) elders.get(0);
        row.put("countedAtMs", 0L);
        MonumentTracker mig = new MonumentTracker();
        mig.restore(snap);
        check(mig.windowStartMs(smid) == 0L, "pre-timestamp record has no window yet");
        check(!mig.progressExpired(smid, t0 + 1000 * H, 24 * H), "unmigrated record never reads expired");
        check(mig.migrateMissingCountedAt(t0 + 50 * H) == 1, "migration stamps one record");
        check(mig.windowStartMs(smid) == t0 + 50 * H, "migrated window starts at boot");
        check(!mig.progressExpired(smid, t0 + 73 * H, 24 * H), "migrated trio gets its full window");
        check(mig.migrateMissingCountedAt(t0 + 60 * H) == 0, "migration is idempotent");

        System.out.println("MonumentExpiryTest: " + passed + " passed, " + failed + " failed");
        if (failed > 0) {
            System.exit(1);
        }
    }
}

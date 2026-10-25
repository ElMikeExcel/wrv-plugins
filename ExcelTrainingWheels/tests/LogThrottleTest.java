import com.winkreyes.exceltrainingwheels.core.LogThrottle;

public class LogThrottleTest {
    public static void run() {
        System.out.println("LogThrottleTest");
        LogThrottle t = new LogThrottle(300_000L);
        Check.eq(0L, t.admit("SIEGE started|w;1;64;1", 1_000L), "first line for a door is logged");
        Check.eq(-1L, t.admit("SIEGE started|w;1;64;1", 2_000L), "repeat 1s later suppressed");
        Check.eq(-1L, t.admit("SIEGE started|w;1;64;1", 299_999L), "repeat just inside the window suppressed");
        Check.eq(0L, t.admit("SIEGE started|w;2;64;1", 3_000L), "a different door logs on its own");
        Check.eq(2L, t.admit("SIEGE started|w;1;64;1", 301_000L), "after the window: logged with the 2 suppressed");
        Check.eq(-1L, t.admit("SIEGE started|w;1;64;1", 301_500L), "window restarts from the logged line");
        Check.eq(0L, t.admit("SIEGE started|w;2;64;1", 400_000L), "nothing suppressed -> count 0");

        // Flood: 10,000 repeats in 5 min -> 1 line, then 1 line carrying 9,999.
        LogThrottle f = new LogThrottle(300_000L);
        int logged = 0;
        for (int i = 0; i < 10_000; i++) if (f.admit("grant", i * 10L) >= 0) logged++;
        Check.eq(1, logged, "10,000 grants inside the window -> 1 console line");
        Check.eq(9_999L, f.admit("grant", 400_000L), "next line reports the 9,999 suppressed");

        // Window 0 = off (0.1.4 behavior).
        LogThrottle off = new LogThrottle(0L);
        boolean all = true;
        for (int i = 0; i < 50; i++) all &= off.admit("x", 5L) == 0L;
        Check.ok(all, "window 0: every line passes");
        Check.eq(0, off.size(), "window 0 keeps no state");

        // Clock going backwards never wedges a key.
        LogThrottle back = new LogThrottle(300_000L);
        back.admit("k", 1_000_000L);
        Check.eq(0L, back.admit("k", 10L), "clock went backwards -> logged, not stuck");

        // Bounded memory.
        LogThrottle small = new LogThrottle(300_000L, 4);
        for (int i = 0; i < 100; i++) small.admit("door" + i, 1_000L + i);
        Check.ok(small.size() <= 4, "key map capped (" + small.size() + " <= 4)");
        Check.eq(0L, small.admit("door99", 2_000L) == -1L ? 0L : 1L, "most recent key still throttled after eviction");
        LogThrottle big = new LogThrottle(300_000L);
        for (int i = 0; i < 5_000; i++) big.admit("door" + i, i);
        Check.ok(big.size() <= LogThrottle.MAX_KEYS, "default cap holds under 5,000 doors");
        LogThrottle exp = new LogThrottle(1_000L, 3);
        exp.admit("a", 0L); exp.admit("b", 0L); exp.admit("c", 5_000L);
        exp.admit("d", 5_001L);
        Check.ok(exp.size() <= 3, "expired keys evicted first");
        Check.eq(-1L, exp.admit("c", 5_002L), "live key kept through eviction");

        // Null key is safe.
        LogThrottle n = new LogThrottle(1_000L);
        Check.eq(0L, n.admit(null, 0L), "null key logged once");
        Check.eq(-1L, n.admit(null, 1L), "null key throttled");

        // Config sanitizing.
        Check.eq(300L, LogThrottle.sanitizeSeconds(300L), "300s kept");
        Check.eq(0L, LogThrottle.sanitizeSeconds(-5L), "negative -> 0 (off)");
        Check.eq(0L, LogThrottle.sanitizeSeconds(0L), "0 -> off");
        Check.eq(86_400L, LogThrottle.sanitizeSeconds(10_000_000L), "huge -> capped at 1 day");

        // Suffix text.
        Check.eq("", LogThrottle.suffix(0L, 300L), "no suffix when nothing suppressed");
        Check.eq(" (+7 similar in the last 300s)", LogThrottle.suffix(7L, 300L), "suffix carries the count");
        Check.eq("", LogThrottle.suffix(-1L, 300L), "no suffix for a suppressed result");
    }
}

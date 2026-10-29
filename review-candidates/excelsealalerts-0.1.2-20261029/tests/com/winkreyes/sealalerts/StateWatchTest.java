/* Excel WRV maintained source; original attribution and license preserved. */ // Excel — WRV custom plugin source; original identity and credits preserved.
package com.winkreyes.sealalerts;

import org.junit.jupiter.api.Test;
import java.time.ZoneId;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class StateWatchTest {
    final Names n = new Names("gamertag", null); final ZoneId z = ZoneId.of("America/Chicago");
    @Test void baselineSilentThenDiffs() {
        StateWatch w = new StateWatch(n, z); List<Map<String, Object>> out = new ArrayList<>(); long now = 1_800_000_000_000L;
        w.observe(new StateWatch.Snapshot(Map.of("ender_dragon", "A"), Map.of(), 5), now, false, false, Map.of(), true, out::add);
        assertTrue(out.isEmpty());
        Map<String, String> b = new TreeMap<>(Map.of("ender_dragon", "A", "wither", "B"));
        w.observe(new StateWatch.Snapshot(b, Map.of("wither", now + 3_600_000L), 5), now, false, false, Map.of(), false, out::add);
        assertEquals(List.of("SEAL_BROKE", "COOLDOWN_STARTED"), out.stream().map(e -> e.get("type")).toList());
        assertEquals("2/5", out.get(0).get("count")); assertEquals("state.yml", out.get(0).get("source"));
        out.clear();
        w.observe(new StateWatch.Snapshot(b, Map.of("wither", now - 1), 5), now, false, false, Map.of(), false, out::add);
        assertEquals("COOLDOWN_ENDED", out.get(0).get("type"));
    }
    @Test void skipsWhatTheTapsAlreadySent() {
        StateWatch w = new StateWatch(n, z); List<Map<String, Object>> out = new ArrayList<>(); long now = 1_800_000_000_000L;
        w.observe(new StateWatch.Snapshot(Map.of(), Map.of(), 5), now, false, false, Map.of(), false, out::add);
        w.observe(new StateWatch.Snapshot(Map.of("warden", "A"), Map.of("warden", now + 1000_000L), 5), now, true, false, Map.of("warden", now), false, out::add);
        assertTrue(out.isEmpty(), out.toString());
    }
    @Test void persistedBaselineCatchesOfflineEdits() {
        StateWatch w = new StateWatch(n, z); List<Map<String, Object>> out = new ArrayList<>();
        w.baseline(List.of("ender_dragon", "wither"), List.of());
        w.observe(new StateWatch.Snapshot(Map.of("ender_dragon", "A"), Map.of(), 5), 1L, false, false, Map.of(), true, out::add);
        assertEquals("ADMIN_SEAL_CHANGE", out.get(0).get("type"));
        assertTrue(String.valueOf(out.get(0).get("detail")).contains("while the server was off"));
    }
}

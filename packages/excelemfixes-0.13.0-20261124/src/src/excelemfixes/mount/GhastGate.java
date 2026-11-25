package excelemfixes.mount;

import java.util.UUID;

/**
 * The Ghast Charm's purchase gate as the buy flow sees it:
 * "has this player finished at least the configured fraction
 * of any one quest package?" (D204 — halfway through Primis
 * OR any single expansion).
 *
 * <p>This interface is Bukkit-only on purpose: the buy flow
 * never touches EliteMobs types. The EliteMobs-backed
 * implementation lives in {@link EmQuestGate}, which the
 * module constructs only after confirming EliteMobs is
 * installed; when EliteMobs is absent the module hands the
 * buy flow a null gate, and a null gate while the gate is
 * enabled in config means purchases refuse (fail closed) —
 * an earned item is never sold on a gate nobody could read.
 */
public interface GhastGate {

    /** One gate reading. {@code available} is false when the
     *  underlying quest data could not be read at all — the
     *  caller must treat that as "cannot sell", never as a
     *  pass. When several packages exist, the progress fields
     *  describe the player's BEST package (the one closest to
     *  qualifying), which is what refusal messages quote. */
    record Progress(boolean available, String packageId, int completed,
                    int total, int required) {

        public boolean passes() {
            return available && total > 0 && completed >= required;
        }

        public static Progress unavailable() {
            return new Progress(false, null, 0, 0, 0);
        }
    }

    Progress check(UUID playerId);
}

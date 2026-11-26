package excelemfixes.partydrops;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;

/**
 * Who counts as a participant at an elite's death
 * (PARTY-DROPS-SPEC-2026-10-20 §2). A player counts when they are a
 * distinct real player (distinct UUID — Floodgate Bedrock accounts
 * are real players and count), online and ALIVE, in the same world
 * as the elite, within the party radius of the elite's death
 * location, and not a spectator. There is no damage requirement and
 * no formal party-membership requirement — presence at the kill is
 * what counts. The killer is counted like anyone else if in range.
 *
 * One deliberate divergence from ExcelTogether's near(): it never
 * checks isDead(); this rule follows Mike's wording — alive only.
 * The divergence can only make drop counting stricter.
 *
 * Pure data, no Bukkit types, so it is unit tested on the plain JVM.
 * The glue layer snapshots every online player into a Candidate
 * relative to the death location.
 */
public final class ParticipantCounter {

    /**
     * One online player's snapshot relative to the elite's death
     * location. dx/dy/dz are (player - death location).
     */
    public record Candidate(UUID uuid, boolean online, boolean alive, boolean spectator,
                            boolean sameWorld, double dx, double dy, double dz) {
    }

    private ParticipantCounter() {
    }

    /**
     * The distinct eligible participants, in snapshot order, capped
     * at participantCap. Duplicate UUIDs count once.
     */
    public static List<UUID> eligible(List<Candidate> candidates, double radius, int participantCap) {
        LinkedHashSet<UUID> found = new LinkedHashSet<>();
        if (candidates == null) return List.of();
        int cap = participantCap >= 1 ? participantCap : 5;
        for (Candidate c : candidates) {
            if (found.size() >= cap) break;
            if (c == null || c.uuid() == null) continue;
            if (!c.online() || !c.alive() || c.spectator()) continue;
            if (!PartyRange.within(c.sameWorld(), c.dx(), c.dy(), c.dz(), radius)) continue;
            found.add(c.uuid());
        }
        return new ArrayList<>(found);
    }

    /** How many participants count (the multiplier's input). */
    public static int count(List<Candidate> candidates, double radius, int participantCap) {
        return eligible(candidates, radius, participantCap).size();
    }
}

// ExcelEMFixes party-drops module — ported from ExcelPartyDrops 0.1.0 (Wave 3, D245).

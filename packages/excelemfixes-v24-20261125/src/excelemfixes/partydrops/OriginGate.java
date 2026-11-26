package excelemfixes.partydrops;

/**
 * The origin gate (PARTY-DROPS-SPEC-2026-10-20 §5). The multiplier
 * fires only for a genuinely natural elite that EliteMobs has not
 * flagged:
 *
 *   - the elite exists (EliteMobs present and reporting),
 *   - EliteEntity.isNaturalEntity() is true,
 *   - its spawn reason is exactly NATURAL,
 *   - EliteEntity.isTriggeredAntiExploit() is false.
 *
 * Spawner, spawn-egg, breeding, raid, command and custom-spawn
 * elites pay 1x — the farm rules' line, mirrored. EM custom bosses
 * and instanced/dungeon content are excluded by the same gate (they
 * are not natural spawns). An exploited kill is an unrewarded kill,
 * consistent across every reward stream.
 *
 * The spawn reason crosses this boundary as a plain String (the
 * Bukkit enum's name) so the gate stays pure and unit-testable.
 */
public final class OriginGate {

    private OriginGate() {
    }

    public static boolean eligible(boolean elitePresent, boolean isNaturalEntity,
                                   String spawnReasonName, boolean triggeredAntiExploit) {
        if (!elitePresent) return false;
        if (!isNaturalEntity) return false;
        if (!"NATURAL".equals(spawnReasonName)) return false;
        return !triggeredAntiExploit;
    }
}

// ExcelEMFixes party-drops module — ported from ExcelPartyDrops 0.1.0 (Wave 3, D245).

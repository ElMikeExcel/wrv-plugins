package com.winkreyes.excelsummons;

import java.util.ArrayList;
import java.util.List;

/**
 * Despawn legibility (0.1.11, P-SUMMONS-TUNING-1 — Mike's Archer
 * vanished at full health with no message, Nov 14, 2026). Every
 * wording a summon's owner hears about a summon fading or leaving
 * lives here, pure and unit-tested; the listener only gathers
 * facts (owner online? which reason? what applied hold?) and sends.
 *
 * The reason set is ONE enum, shared with the records: the same
 * names P-PROTECT-WATCH-1's per-player spawn/despawn log lines use,
 * so a "my summon vanished" report is provable from records in the
 * same words the player was told.
 *
 * Fade warnings (timed combat summons only; persistent families
 * never expire): T-30s fires only for durations of 150s or more —
 * a T-30 on the 90s tier-1 Archer would fire at two-thirds of its
 * life and train players to ignore warnings; T-15s fires for every
 * timed summon. Warnings are evaluated on the bridge's existing
 * ticks, so their precision is the tick period, and each threshold
 * fires exactly once per summon (the listener tracks that).
 *
 * No owner line exists for caster logout (no recipient) or server
 * shutdown (no recipient) — those paths pass a null reason to the
 * removal funnel and stay silent by design.
 *
 * Excel — Wink Reyes Valley (Iris / Mike).
 */
public final class DespawnNotice {

    private DespawnNotice() {}

    /** Why a summon left. EXPIRED = the lifetime sweep took it in a
     *  loaded chunk; UNLOADED = its time ran out while its chunk was
     *  unloaded (removed on chunk return, or its record retired past
     *  grace); DIED = EntityDeathEvent; DISMISSED = command, menu,
     *  admin, or the mount recast toggle. */
    public enum Reason { EXPIRED, DISMISSED, DIED, UNLOADED }

    /** Warning thresholds (seconds remaining) for a summon whose
     *  total duration is {@code durationMillis}: [30, 15] at
     *  150s+, [15] below that, [] for persistent (0) summons. */
    public static List<Integer> warningThresholds(long durationMillis) {
        if (durationMillis <= 0) return List.of();
        if (durationMillis >= 150_000L) return List.of(30, 15);
        return List.of(15);
    }

    /** The chat fade warning: "Your Guardian fades in 30 seconds." */
    public static String fadeWarning(String displayName, int seconds) {
        return "Your " + displayName + " fades in " + seconds + " seconds.";
    }

    /** The one-line despawn receipt. {@code appliedHoldPercent} is
     *  the hold the record ACTUALLY carried (ReservePolicy's applied
     *  walk) — 0 (adopted, or clamp-skipped) earns no mana clause. */
    public static String despawnLine(String displayName, Reason reason,
                                     int appliedHoldPercent) {
        String core;
        switch (reason) {
            case EXPIRED:
                core = "Your " + displayName + " has faded — its time ran out.";
                break;
            case DISMISSED:
                core = "Your " + displayName + " was dismissed.";
                break;
            case DIED:
                core = "Your " + displayName + " was slain.";
                break;
            case UNLOADED:
                core = "Your " + displayName
                        + " faded while away — its time ran out in an unloaded chunk.";
                break;
            default:
                core = "Your " + displayName + " is gone.";
                break;
        }
        return appliedHoldPercent > 0
                ? core + " (" + appliedHoldPercent + "% mana released.)"
                : core;
    }

    /** The anti-spam summary for ONE action that removed several of
     *  one owner's summons at once (a scope dismiss): names listed
     *  in removal order, the TOTAL applied hold released. Single
     *  removals never use this — they use despawnLine. */
    public static String summaryLine(List<String> displayNames, Reason reason,
                                     int totalAppliedHoldPercent) {
        String verb;
        switch (reason) {
            case DISMISSED: verb = "Dismissed"; break;
            case EXPIRED:   verb = "Faded"; break;
            case DIED:      verb = "Slain"; break;
            case UNLOADED:  verb = "Faded while away"; break;
            default:        verb = "Gone"; break;
        }
        String core = verb + ": " + String.join(", ", displayNames) + ".";
        return totalAppliedHoldPercent > 0
                ? core + " (" + totalAppliedHoldPercent + "% mana released.)"
                : core;
    }

    /** Defensive copy helper for callers building a name list. */
    public static List<String> namesOf(List<String> names) {
        return names == null ? List.of() : new ArrayList<>(names);
    }
}

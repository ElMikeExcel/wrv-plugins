package excelemfixes.questglow;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The pure decision core of the quest-giver-glow module: given
 * EliteMobs' own data (read by the module's EM bridge and handed
 * in as plain values), decide what each quest NPC should look
 * like to one player. No Bukkit, no EliteMobs types — the whole
 * availability definition lives here so the suites can pin it.
 *
 * The availability definition (all of it is EliteMobs' own
 * state, never a reimplementation):
 * - A quest is AVAILABLE to a player when EliteMobs' own accept
 *   check ({@code CustomQuest.hasPermissionForQuest}) says the
 *   player may take it: the quest is enabled, the player holds
 *   every accept permission EliteMobs requires (its chain gates
 *   ride on this), and the player is not locked out — a
 *   completed one-time quest stays locked out, a repeatable
 *   quest is locked out only inside its lockout window. The
 *   bridge computes that set; the core just consumes it.
 * - A quest the player has already accepted is never
 *   "available" — it is carried, not offered.
 * - A carried quest whose objectives are finished is a TURN-IN
 *   at the NPC its own file names as turnInNPC (which may be a
 *   different NPC than the one that gave it).
 *
 * Precedence: TURNIN beats ACCEPT at the same NPC — a player
 * standing at an NPC with a finished quest to hand in and a new
 * quest to pick up sees the turn-in color, because handing in
 * is the action that finishes something. (Decide-and-log D215.)
 *
 * OBJECTIVE (added in 0.17.0, the dialog-target fix): a carried
 * quest with an incomplete DIALOG objective lights that
 * objective's target NPC. It sits between the two older states:
 * TURNIN still wins (a finished quest is the most urgent
 * state), and OBJECTIVE beats ACCEPT (the carried quest is the
 * player's declared intent — they are already on their way to
 * this person — while an accept is only an opportunity).
 */
public final class QuestGlowCore {

    private QuestGlowCore() {}

    /** A world + point parsed from an EliteMobs spawn string. */
    public record Anchor(String world, double x, double y, double z) {}

    /**
     * One EliteMobs NPC that can glow: its config filename, its
     * cleaned display name (what the live entity's nameplate
     * reads after color/gradient markup is stripped), the quest
     * files it hands out, and the places it stands.
     */
    public record NpcInfo(String npcFile, String displayName,
                          List<String> questFiles,
                          List<Anchor> anchors) {}

    /**
     * Parse an EliteMobs spawn-location string
     * ("world,x,y,z[,yaw,pitch]") into an Anchor. Returns null
     * for the shapes EliteMobs uses for "no fixed home" (null,
     * blank, the literal "null", or anything malformed) — an NPC
     * without a parseable home simply never glows; it is never
     * guessed at.
     */
    public static Anchor parseAnchor(String raw) {
        if (raw == null) return null;
        String s = raw.trim();
        if (s.isEmpty() || s.equalsIgnoreCase("null")) return null;
        String[] parts = s.split(",");
        if (parts.length < 4) return null;
        String world = parts[0].trim();
        if (world.isEmpty()) return null;
        try {
            double x = Double.parseDouble(parts[1].trim());
            double y = Double.parseDouble(parts[2].trim());
            double z = Double.parseDouble(parts[3].trim());
            return new Anchor(world, x, y, z);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** Squared-distance radius check (no sqrt on the hot path). */
    public static boolean within(double dx, double dy, double dz,
                                 double radius) {
        return dx * dx + dy * dy + dz * dz <= radius * radius;
    }

    /**
     * The state of one NPC for one player.
     *
     * @param npc              the NPC being decided
     * @param availableQuests  quest files EliteMobs' own accept
     *                         check passes for this player,
     *                         minus the ones already accepted
     *                         (the bridge subtracts those)
     * @param turnInByQuest    the player's carried-and-finished
     *                         quests: quest file -> the NPC file
     *                         its turnInNPC names
     */
    public static GlowState stateFor(NpcInfo npc,
                                     Set<String> availableQuests,
                                     Map<String, String> turnInByQuest) {
        return stateFor(npc, availableQuests, turnInByQuest, null);
    }

    /**
     * The state of one NPC for one player, with the objective
     * state included.
     *
     * @param objectiveTargets NPC files named by an incomplete
     *                         DIALOG objective of a quest this
     *                         player carries (the bridge reads
     *                         them from the carried quests' own
     *                         objective objects)
     */
    public static GlowState stateFor(NpcInfo npc,
                                     Set<String> availableQuests,
                                     Map<String, String> turnInByQuest,
                                     Set<String> objectiveTargets) {
        if (npc == null) return GlowState.NONE;
        if (turnInByQuest != null) {
            for (String turnInNpc : turnInByQuest.values()) {
                if (npc.npcFile().equals(turnInNpc)) {
                    return GlowState.TURNIN;
                }
            }
        }
        if (objectiveTargets != null
                && objectiveTargets.contains(npc.npcFile())) {
            return GlowState.OBJECTIVE;
        }
        if (availableQuests != null && npc.questFiles() != null) {
            for (String quest : npc.questFiles()) {
                if (availableQuests.contains(quest)) {
                    return GlowState.ACCEPT;
                }
            }
        }
        return GlowState.NONE;
    }

    /**
     * Strength ranking for merging states when one live body is
     * claimed by more than one NPC entry: TURNIN strongest,
     * then OBJECTIVE, then ACCEPT, then NONE.
     */
    public static GlowState stronger(GlowState a, GlowState b) {
        return rank(b) > rank(a) ? b : a;
    }

    private static int rank(GlowState s) {
        if (s == null) return 0;
        return switch (s) {
            case NONE -> 0;
            case ACCEPT -> 1;
            case OBJECTIVE -> 2;
            case TURNIN -> 3;
        };
    }
}

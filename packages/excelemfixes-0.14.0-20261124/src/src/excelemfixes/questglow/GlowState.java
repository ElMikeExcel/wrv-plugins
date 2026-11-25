package excelemfixes.questglow;

/**
 * What one quest NPC should look like to one player right now.
 * The state is per (player, NPC) — the same NPC is ACCEPT for a
 * player who can pick up a quest there, TURNIN for a player
 * carrying a finished quest back to it, and NONE for everyone
 * else. NONE is the common case and must never produce a glow:
 * a glow shown to the wrong player is a worse defect than a
 * missing one.
 */
public enum GlowState {
    NONE,
    ACCEPT,
    TURNIN
}

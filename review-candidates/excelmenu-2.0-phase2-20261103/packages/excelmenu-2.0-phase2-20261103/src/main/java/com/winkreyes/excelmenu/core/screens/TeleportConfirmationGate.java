// ExcelMenu — an Excel plugin by WinkReyesValley. (Excel signature — MikeExcel)
// Excel signature - WRV maintained source; existing authorship notices preserved.
package com.winkreyes.excelmenu.core.screens;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * One-time state for teleport confirmation prompts (DESIGN-2.0 §2.4:
 * the gate ExcelBedrockMenu carried becomes a core service of the
 * unified plugin). Ported from ExcelBedrockMenu 1.5.28's
 * TeleportConfirmationGate with its semantics unchanged:
 *
 *   - a ticket is bound to the player (its id IS the player's UUID),
 *     the exact session object, and the world it was opened in;
 *   - begin() first purges expired tickets, then replaces any live
 *     ticket for the player;
 *   - answer() consumes the ticket either way (single-use even when
 *     a check fails) and teleports only when the choice was Yes, the
 *     player is online, the session and world still match, and the
 *     age is inside [0, TTL);
 *   - main-thread only, like the original (no synchronization).
 */
public final class TeleportConfirmationGate {

    public static final long TTL_NANOS = 60_000_000_000L;

    /** One issued prompt. The session is compared by identity. */
    public record Ticket(UUID id, Object session, UUID world, long created) {}

    private final Map<UUID, Ticket> pending = new HashMap<>();

    /** Issue a prompt for a player, purging expired tickets first.
     *  Any previous live prompt for the player is replaced. */
    public Ticket begin(UUID id, Object session, UUID world, long now) {
        pending.values().removeIf(t -> now - t.created() >= TTL_NANOS);
        Ticket ticket = new Ticket(id, session, world, now);
        pending.put(id, ticket);
        return ticket;
    }

    /** Consume the ticket and say whether the teleport may run. */
    public boolean answer(Ticket ticket, Object session, UUID world, long now,
                          boolean online, boolean yes) {
        if (ticket == null || pending.get(ticket.id()) != ticket) return false;
        pending.remove(ticket.id());
        return yes && online && ticket.session() == session
                && ticket.world().equals(world)
                && now - ticket.created() >= 0 && now - ticket.created() < TTL_NANOS;
    }

    /** Drop the ticket if it is still the live one (form closed,
     *  or the transport failed before the prompt could open). */
    public void cancel(Ticket ticket) {
        if (ticket != null && pending.get(ticket.id()) == ticket) {
            pending.remove(ticket.id());
        }
    }

    /** Drop a player's prompt because the menu session advanced. */
    public void invalidate(UUID id) {
        pending.remove(id);
    }

    public void clear() {
        pending.clear();
    }

    /**
     * Confirm outcome: only an explicit first-button choice
     * teleports. A null choice (the form was closed without
     * choosing) never does. (ExcelBedrockMenu's confirmChoiceTeleports,
     * kept beside the gate it feeds.)
     */
    public static boolean confirmChoiceTeleports(Boolean firstButtonClicked) {
        return Boolean.TRUE.equals(firstButtonClicked);
    }
}

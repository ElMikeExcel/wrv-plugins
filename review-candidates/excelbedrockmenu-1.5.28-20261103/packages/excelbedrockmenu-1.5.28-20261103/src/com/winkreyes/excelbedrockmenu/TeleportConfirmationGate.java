// Excel signature: WRV confirmation lifecycle; main-thread only.
package com.winkreyes.excelbedrockmenu;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
final class TeleportConfirmationGate {
    static final long TTL_NANOS = 60_000_000_000L;
    record Ticket(UUID id, Object session, UUID world, long created) {}
    private final Map<UUID, Ticket> pending = new HashMap<>();
    Ticket begin(UUID id, Object session, UUID world, long now) {
        pending.values().removeIf(t -> now-t.created() >= TTL_NANOS);
        Ticket ticket = new Ticket(id,session,world,now); pending.put(id,ticket); return ticket;
    }
    boolean answer(Ticket ticket, Object session, UUID world, long now, boolean online, boolean yes) {
        if (pending.get(ticket.id()) != ticket) return false;
        pending.remove(ticket.id());
        return yes && online && ticket.session() == session && ticket.world().equals(world)
                && now-ticket.created() >= 0 && now-ticket.created() < TTL_NANOS;
    }
    void cancel(Ticket ticket) { if(pending.get(ticket.id()) == ticket) pending.remove(ticket.id()); }
    void invalidate(UUID id) { pending.remove(id); }
    void clear() { pending.clear(); }
}

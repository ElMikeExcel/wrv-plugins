package com.winkreyes.exceltogether;

import java.util.*;

/**
 * Mirrors mcMMO party changes made BY HAND (/party accept, /party join, /party quit, kick, disband, leader
 * invite, new owner) into ExcelTogether parties. State-based: it compares what mcMMO says with what we hold and
 * fixes the difference, so running it twice or from our own mirroring is harmless.
 *
 * Loop guards: it never calls the link's invite/accept/leave (it only reads mcMMO), and anything we remove from
 * ours on purpose ({@link #suppress}) is not pulled back in until mcMMO shows them leaving or joining again.
 * Only players seen together in an mcMMO party and an ExcelTogether party ("mirrored") are ever removed from
 * ours because of an mcMMO leave, so players the mcMMO link never touched stay put.
 */
final class McmmoSync {
    static final int POLL_EVERY_TICKS = 2;      // Controller.tick runs about once a second
    private final Controller c;
    /** players known to be in both systems together: id -> mcMMO party name */
    private final Map<UUID, String> mirrored = new HashMap<>();
    /** removed from ours on purpose; do not re-adopt while mcMMO still lists them */
    private final Set<UUID> suppressed = new HashSet<>();
    /** told once that ours was full */
    private final Set<UUID> fullNoted = new HashSet<>();
    /** in an mcMMO party but did not fit in ours; retried on every poll until a seat frees (no new notes) */
    private final Set<UUID> waiting = new LinkedHashSet<>();
    /** just logged in (or plugin just enabled): check until mcMMO has their profile loaded, then adopt */
    private final Map<UUID, Integer> watching = new LinkedHashMap<>();
    static final int WATCH_MAX_POLLS = 30;     // about a minute
    /** told once that an mcMMO party joined by hand could not take them out of their ExcelTogether party */
    private final Set<UUID> keptNoted = new HashSet<>();

    McmmoSync(Controller c) { this.c = c; }

    /** Login or enable: adopt this player's mcMMO party (profile may load a moment later, so keep checking for a while). */
    void watch(UUID id) { if (id != null) watching.put(id, 0); }
    boolean isWaiting(UUID id) { return waiting.contains(id); }
    void suppress(UUID id) { suppressed.add(id); mirrored.remove(id); }
    void clear(UUID id) { suppressed.remove(id); }
    boolean isMirrored(UUID id) { return mirrored.containsKey(id); }
    boolean isSuppressed(UUID id) { return suppressed.contains(id); }

    /** Poll: everyone in an ExcelTogether party, plus players we mirrored. */
    void poll() {
        if (!c.link().available()) return;
        Set<UUID> ids = new LinkedHashSet<>(c.mgr().allMembers());
        ids.addAll(mirrored.keySet());
        ids.addAll(waiting);                  // full-party retry
        ids.addAll(watching.keySet());        // login adoption
        ids.addAll(suppressed);               // so a suppression ends as soon as mcMMO shows them gone
        if (!ids.isEmpty()) reconcile(ids, List.of());
    }

    /**
     * Called from inside an mcMMO party event. mcMMO fires these BEFORE it changes anything, so the old and new
     * parties still list their members here: collect who might be affected now, reconcile one tick later.
     */
    List<UUID> collect(UUID player, String oldParty, String newParty) {
        Set<UUID> ids = new LinkedHashSet<>();
        if (player != null) ids.add(player);
        McmmoLink link = c.link();
        for (String n : new String[]{oldParty, newParty}) {
            if (n == null) continue;
            McmmoLink.McParty p = link.partyByName(n);
            if (p.real()) ids.addAll(p.members.keySet());
        }
        // anyone we hold in a party with an affected player
        for (UUID u : new ArrayList<>(ids)) { PartyManager.Party pt = c.mgr().partyOf(u); if (pt != null) ids.addAll(pt.members); }
        return new ArrayList<>(ids);
    }

    void reconcile(Collection<UUID> ids, Collection<UUID> joined) {
        McmmoLink link = c.link();
        if (!link.available()) return;
        for (UUID id : new ArrayList<>(new LinkedHashSet<>(ids))) {
            if (id == null) continue;
            try { one(link, id, joined.contains(id)); } catch (RuntimeException ignored) { }
        }
    }

    private void one(McmmoLink link, UUID id, boolean explicitJoin) {
        McmmoLink.McParty mc = link.partyOf(id);
        if (mc == McmmoLink.McParty.UNKNOWN) {
            Integer n = watching.get(id);
            if (n != null) { if (n + 1 > WATCH_MAX_POLLS) watching.remove(id); else watching.put(id, n + 1); }
            return;
        }
        watching.remove(id);
        if (waiting.contains(id) && c.env().online(id) == null) waiting.remove(id);
        PartyManager mgr = c.mgr();
        PartyManager.Party ours = mgr.partyOf(id);

        if (!mc.real()) {                                   // not in any mcMMO party
            suppressed.remove(id); fullNoted.remove(id); waiting.remove(id); keptNoted.remove(id);
            if (mirrored.remove(id) != null && ours != null) quietLeave(id, "left the mcMMO party", " left the mcMMO party and so left the party.");
            return;
        }
        if (suppressed.contains(id)) {
            if (!explicitJoin) return;
            suppressed.remove(id);
        }
        Set<UUID> mates = new LinkedHashSet<>(mc.members.keySet()); mates.remove(id);

        if (ours != null) {
            boolean together = false;
            for (UUID m : mates) if (ours.members.contains(m)) { together = true; break; }
            if (together) { markMirrored(link, id, mc); syncLeader(mc, ours); return; }
            if (mirrored.containsKey(id)) {                 // they were together; now their mcMMO mates are different people (or nobody)
                quietLeave(id, mates.isEmpty() ? "left the mcMMO party" : "moved to another mcMMO party",
                        mates.isEmpty() ? " left the mcMMO party and so left the party." : " moved to another mcMMO party and so left the party.");
                ours = null;
                if (mates.isEmpty()) { mirrored.remove(id); return; }
            } else {
                // Never linked (ours was made without mcMMO, or the link could not follow). Decide only on a deliberate join by hand,
                // never on a poll: the player picked this mcMMO party, so mcMMO wins and they move, but only if there is somewhere to land.
                if (!explicitJoin || mates.isEmpty()) return;
                Landing l = landing(mc, mates, id);
                if (l == Landing.FULL) {
                    if (keptNoted.add(id)) c.tell(id, "Note: you joined an mcMMO party with different people, but their ExcelTogether party is full (" + mgr.maxSize() + "/" + mgr.maxSize() + "), so you stay in your current ExcelTogether party.");
                    return;
                }
                if (l == Landing.NONE) {
                    if (keptNoted.add(id)) c.tell(id, "Note: you joined an mcMMO party with different people, but none of them is online to form an ExcelTogether party yet, so you stay in your current one.");
                    return;
                }
                c.tell(id, "Note: you joined an mcMMO party with different people, so you left your old ExcelTogether party and joined theirs.");
                quietLeave(id, "joined a different mcMMO party", " joined a different mcMMO party and so left the party.");
                ours = null;
            }
        }
        if (mates.isEmpty()) return;                        // a one-person mcMMO party has nothing to mirror
        join(link, id, mc, mates);
    }

    /** id has no ExcelTogether party; give them the one their mcMMO mates are in, or make one. */
    private enum Landing { ANCHOR, FULL, BUILD, NONE }
    /** An ExcelTogether party that already holds some of the mates: the mcMMO leader's, else the biggest. */
    private PartyManager.Party anchorOf(McmmoLink.McParty mc, Set<UUID> mates) {
        PartyManager mgr = c.mgr();
        PartyManager.Party lp = mc.leader == null ? null : mgr.partyOf(mc.leader);
        if (lp != null && mates.contains(mc.leader)) return lp;
        PartyManager.Party anchor = null;
        for (UUID m : mates) { PartyManager.Party p = mgr.partyOf(m); if (p != null && (anchor == null || p.size() > anchor.size())) anchor = p; }
        return anchor;
    }
    /** Where would id land if adopted now (nothing is changed)? */
    private Landing landing(McmmoLink.McParty mc, Set<UUID> mates, UUID id) {
        PartyManager.Party a = anchorOf(mc, mates);
        if (a != null) return a.size() >= c.mgr().maxSize() ? Landing.FULL : Landing.ANCHOR;
        for (UUID m : mates) if (eligible(m)) return Landing.BUILD;
        return Landing.NONE;
    }

    private void join(McmmoLink link, UUID id, McmmoLink.McParty mc, Set<UUID> mates) {
        PartyManager mgr = c.mgr();
        if (c.env().online(id) == null) { waiting.remove(id); return; }      // only online players are adopted
        // 1) an ExcelTogether party that already holds some of the mates
        PartyManager.Party anchor = anchorOf(mc, mates);
        if (anchor != null) {
            PartyManager.AdoptOutcome o = mgr.adopt(id, anchor.leader);
            if (o.result == PartyManager.AdoptResult.FULL) {
                waiting.add(id);
                if (fullNoted.add(id)) c.tell(id, "Note: your mcMMO party joined, but the ExcelTogether party is full (" + mgr.maxSize() + "/" + mgr.maxSize() + "), so you stay in mcMMO only. Your Jobs bonus won't include them.");
                return;
            }
            if (o.result == PartyManager.AdoptResult.JOINED) joined(link, id, mc, o.party);
            return;
        }
        // 2) none of them has one: make one from everyone online who is free, mcMMO leader first, capped at the party size
        List<UUID> order = new ArrayList<>();
        Set<UUID> pool = new LinkedHashSet<>(); pool.add(id); pool.addAll(mates);
        if (mc.leader != null && pool.contains(mc.leader) && eligible(mc.leader)) order.add(mc.leader);
        for (UUID u : pool) if (!order.contains(u) && eligible(u)) order.add(u);
        if (order.size() < PartyManager.MIN_SIZE) return;     // not enough online, free players yet; a later pass or event retries
        UUID first = order.get(0); PartyManager.Party made = null;
        for (int k = 1; k < order.size(); k++) {
            UUID u = order.get(k);
            PartyManager.AdoptOutcome o = mgr.adopt(u, first);
            if (o.result == PartyManager.AdoptResult.FULL) {
                waiting.add(u);
                if (fullNoted.add(u)) c.tell(u, "Note: your mcMMO party joined, but the ExcelTogether party is full (" + mgr.maxSize() + "/" + mgr.maxSize() + "), so you stay in mcMMO only. Your Jobs bonus won't include them.");
                continue;
            }
            if (o.party != null) made = o.party;
            if (o.result == PartyManager.AdoptResult.CREATED) {
                markMirrored(link, first, mc);
                c.tell(first, "Your mcMMO party also made an ExcelTogether party (" + c.sizeText(o.party) + "). " + c.bonusNote());
            } else if (o.result == PartyManager.AdoptResult.JOINED) {
                c.tellParty(o.party, u, c.nameOf(u) + " joined the party (" + c.sizeText(o.party) + ").");
            }
            if (o.party != null) { waiting.remove(u); markMirrored(link, u, mc); c.tell(u, "You were added to the ExcelTogether party (" + c.sizeText(o.party) + ") because you are in the same mcMMO party. " + c.bonusNote()); }
        }
        // overflow: online mates that did not fit were noted above; others stay in mcMMO only
    }

    private boolean eligible(UUID u) { return c.env().online(u) != null && !c.mgr().inParty(u) && !suppressed.contains(u); }

    private void joined(McmmoLink link, UUID id, McmmoLink.McParty mc, PartyManager.Party p) {
        markMirrored(link, id, mc); waiting.remove(id);
        fullNoted.remove(id);
        c.tell(id, "You were added to the ExcelTogether party (" + c.sizeText(p) + ") because you joined their mcMMO party. " + c.bonusNote());
        c.tellParty(p, id, c.nameOf(id) + " joined the party (" + c.sizeText(p) + ").");
    }

    private void markMirrored(McmmoLink link, UUID id, McmmoLink.McParty mc) {
        mirrored.put(id, mc.name);
        link.adopted(id, mc.name);          // keeps the link's own leave/cleanup bookkeeping right (also after an mcMMO rename)
    }

    private void quietLeave(UUID id, String why, String others) {
        PartyManager.Change ch = new PartyManager.Change();
        PartyManager.LeaveResult r = c.mgr().leave(id, ch);
        mirrored.remove(id);
        if (ch.disbanded) for (UUID u : ch.remaining) mirrored.remove(u);
        if (r == PartyManager.LeaveResult.NOT_IN_PARTY) return;
        c.tell(id, "You left the ExcelTogether party because you " + why + ".");
        c.announce(ch, c.nameOf(id), others, false);
    }

    /** mcMMO is the source for the leader when it was changed by hand (/party owner). One way only: ours never pushes a leader back. */
    private void syncLeader(McmmoLink.McParty mc, PartyManager.Party ours) {
        if (mc.leader == null || mc.leader.equals(ours.leader) || !ours.members.contains(mc.leader)) return;
        if (!mirrored.containsKey(mc.leader)) return;
        if (c.mgr().setLeader(mc.leader)) {
            PartyManager.Party now = c.mgr().partyOf(mc.leader);
            for (UUID u : now.members) c.tell(u, u.equals(mc.leader) ? "You are now the party leader." : c.nameOf(mc.leader) + " is now the party leader.");
        }
    }
}

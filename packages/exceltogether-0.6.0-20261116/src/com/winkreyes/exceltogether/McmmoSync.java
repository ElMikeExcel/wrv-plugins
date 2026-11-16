package com.winkreyes.exceltogether;

import java.util.*;

/**
 * One-way mcMMO mirror (0.6.0, P-PARTY-REBUILD-1 unit 2 / U2-D1): the roster of
 * record (PartyManager) is the only source; this class writes it INTO mcMMO
 * and repairs drift. It never mutates the roster - the pre-0.6.0 bidirectional
 * adoption (the old adopt callers in this class, quietLeave's "mcMMO wins"
 * branch, and the guard set that held back the Nov 3, 2026 leave loop) is
 * deleted, so the loop's mechanism no longer exists rather than being guarded.
 *
 * Reconcile, per hub party: ensure the deterministic mcMMO mirror party
 * exists (name per D74/Q5: leader-name-based, hub-id fallback on collision),
 * add roster members who are not in it, remove anyone in it who is not on
 * the roster, and set its leader to the hub leader. A native mcMMO change
 * (a hand /party join/quit/owner that got past the door layer) is DRIFT:
 * the next reconcile restores the mirrored roster and writes a log line
 * naming the player. UNKNOWN reads (profile not loaded) never cause action.
 *
 * Login (watch) is mirror-repair, not adoption: a watched player who is on
 * a roster gets their mirror reconciled once mcMMO can read them; a watched
 * player on no roster who is still sitting in ANY mcMMO party (our mirror
 * or a native one) is cleared from that face, never adopted (0.6.0 U2-D4,
 * SCOPING.md §2.5 stale-face rule). The one exception is the first-boot
 * seed (LoginRebuild): while a player's seed attempt is still outstanding,
 * their mcMMO party may be its seed source, so the clear waits until the
 * seed settles - and if the seed claims them, they are reconciled instead.
 */
final class McmmoSync {
    static final int POLL_EVERY_TICKS = 2;      // Controller.tick runs about once a second
    static final int WATCH_MAX_POLLS = 30;      // about a minute of profile-load retries
    private final Controller c;
    /** hub party id -> mcMMO mirror party name (the deterministic name this sync created or discovered). */
    private final Map<Long, String> mirrorByHub = new LinkedHashMap<>();
    /** just logged in (or plugin just enabled): reconcile once mcMMO has their profile loaded. */
    private final Map<UUID, Integer> pendingRepair = new LinkedHashMap<>();

    McmmoSync(Controller c) { this.c = c; }

    /** Login or enable: mirror-repair for this player once their mcMMO profile can be read. */
    void watch(UUID id) { if (id != null) pendingRepair.put(id, 0); }

    /** Poll: reconcile every hub party's mirror, empty mirrors whose hub party is gone, finish login repairs. */
    void poll() {
        if (!c.link().available()) return;
        for (PartyManager.Party p : c.mgr().allParties()) reconcileParty(p);
        emptyDeadMirrors();
        for (UUID id : new ArrayList<>(pendingRepair.keySet())) repairLogin(id);
    }

    /**
     * Called from inside an mcMMO party event. mcMMO fires these BEFORE it changes anything, so the old and new
     * parties still list their members here: collect who might be affected now, reconcile one tick later.
     * The event is only a hint that drift may exist - reconciling never adopts from it.
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
        for (UUID u : new ArrayList<>(ids)) { PartyManager.Party pt = c.mgr().partyOf(u); if (pt != null) ids.addAll(pt.members); }
        return new ArrayList<>(ids);
    }

    /**
     * Reconcile the mirrors touched by these players (an mcMMO event hint, a hub
     * accept, an EM-side join). The {@code joined} hint from the old adoption
     * protocol is accepted for call-site compatibility and ignored: whether a
     * change arrived by hand or by poll changes nothing - mcMMO is never a source.
     */
    void reconcile(Collection<UUID> ids, Collection<UUID> joined) {
        McmmoLink link = c.link();
        if (!link.available()) return;
        Set<Long> done = new HashSet<>();
        for (UUID id : new LinkedHashSet<>(ids)) {
            if (id == null) continue;
            try {
                PartyManager.Party ours = c.mgr().partyOf(id);
                if (ours != null) { if (done.add(ours.id)) reconcileParty(ours); }
                else clearFromMirror(id);
            } catch (RuntimeException ignored) { }
        }
    }

    /**
     * A hub mutation just landed (leave / kick / disband / admin removal, driven
     * through Controller.announce): reconcile the survivors' mirrors at once and
     * take the removed players out of any mirror party they were in. The poll
     * would repair all of this anyway; this is the prompt path.
     */
    void hubChanged(Collection<UUID> affected) {
        if (!c.link().available()) return;
        Set<Long> done = new HashSet<>();
        for (UUID id : affected) {
            if (id == null) continue;
            try {
                PartyManager.Party ours = c.mgr().partyOf(id);
                if (ours != null) { if (done.add(ours.id)) reconcileParty(ours); }
                else clearFromMirror(id);
            } catch (RuntimeException ignored) { }
        }
        emptyDeadMirrors();
    }

    /** Make the mcMMO mirror of one hub party match the roster of record exactly. */
    private void reconcileParty(PartyManager.Party hub) {
        McmmoLink link = c.link();
        String name = mirrorByHub.get(hub.id);
        McmmoLink.McParty cur = null;
        if (name != null) {
            cur = link.partyByName(name);
            if (cur == McmmoLink.McParty.UNKNOWN) { for (UUID m : hub.members) pendingRepair.putIfAbsent(m, 0); return; }
            if (!cur.real()) cur = null;                    // the mirror party is gone (native disband = drift): recreated below
        }
        if (cur == null) {
            // Discovery (e.g. after a restart, when mcMMO kept the mirror but this
            // sync forgot its name): a party holding at least two roster members
            // including the leader IS the mirror; anything smaller is foreign.
            for (UUID m : hub.members) {
                McmmoLink.McParty cand = link.partyOf(m);
                if (!cand.real() || !cand.members.containsKey(hub.leader)) continue;
                long overlap = cand.members.keySet().stream().filter(hub.members::contains).count();
                if (overlap >= 2) { name = cand.name; cur = cand; mirrorByHub.put(hub.id, name); break; }
            }
        }
        boolean fresh = false;
        if (cur == null) {
            name = link.createMirrorParty(hub.leader, c.nameOf(hub.leader), "party" + hub.id);
            if (name == null) { pendingRepair.putIfAbsent(hub.leader, 0); return; }
            mirrorByHub.put(hub.id, name);
            cur = link.partyByName(name);
            if (!cur.real()) return;
            fresh = true;                                   // a just-created mirror is construction, not drift: no repair log lines
        }
        // Anyone in the mirror who is not on the roster is drift: out, with a log line.
        for (UUID extra : new ArrayList<>(cur.members.keySet())) {
            if (hub.members.contains(extra)) continue;
            link.removeFromParty(extra, name);
            if (!fresh) drift(extra, "removed from mcMMO party '" + name + "' - they are not on the party roster");
        }
        // Every roster member belongs in the mirror; a member found in a different
        // mcMMO party was moved there by hand - the roster wins, they are restored.
        cur = link.partyByName(name);
        if (!cur.real()) return;
        for (UUID m : hub.members) {
            if (cur.members.containsKey(m)) continue;
            McmmoLink.McParty before = link.partyOf(m);
            if (before == McmmoLink.McParty.UNKNOWN) { pendingRepair.putIfAbsent(m, 0); continue; }
            if (link.addToParty(m, name) && !fresh)
                drift(m, before.real()
                        ? "moved out of mcMMO party '" + before.name + "' back into the mirror party '" + name + "'"
                        : "restored to mcMMO mirror party '" + name + "'");
        }
        // The hub leader leads the mirror.
        cur = link.partyByName(name);
        if (cur.real() && !hub.leader.equals(cur.leader) && cur.members.containsKey(hub.leader)) {
            if (link.setPartyLeader(name, hub.leader, c.nameOf(hub.leader)) && !fresh)
                drift(hub.leader, "mcMMO mirror party '" + name + "' leadership restored to the roster leader");
        }
    }

    /** A player on no roster who is still in one of OUR mirror parties is cleared from that face (never adopted). */
    private void clearFromMirror(UUID id) {
        McmmoLink link = c.link();
        McmmoLink.McParty cur = link.partyOf(id);
        if (!cur.real()) return;
        if (!mirrorByHub.containsValue(cur.name)) return;   // a native party we never mirrored is not ours to clear
        link.removeFromParty(id, cur.name);
        drift(id, "cleared from mcMMO mirror party '" + cur.name + "' - they are not on the party roster");
    }

    /** Mirrors whose hub party no longer exists are emptied (their members are cleared by clearFromMirror on the way). */
    private void emptyDeadMirrors() {
        if (mirrorByHub.isEmpty()) return;
        Set<Long> live = new HashSet<>();
        for (PartyManager.Party p : c.mgr().allParties()) live.add(p.id);
        McmmoLink link = c.link();
        for (Map.Entry<Long, String> e : new ArrayList<>(mirrorByHub.entrySet())) {
            if (live.contains(e.getKey())) continue;
            McmmoLink.McParty cur = link.partyByName(e.getValue());
            if (cur.real()) for (UUID m : new ArrayList<>(cur.members.keySet())) link.removeFromParty(m, e.getValue());
            mirrorByHub.remove(e.getKey());
        }
    }

    /** Login repair for one watched player, retried while mcMMO cannot read them yet. */
    private void repairLogin(UUID id) {
        McmmoLink link = c.link();
        McmmoLink.McParty cur = link.partyOf(id);
        if (cur == McmmoLink.McParty.UNKNOWN) { retryWatch(id); return; }
        PartyManager.Party ours = c.mgr().partyOf(id);
        if (ours != null) { pendingRepair.remove(id); reconcileParty(ours); return; }
        if (!cur.real()) { pendingRepair.remove(id); return; }
        // On no roster but still listed in an mcMMO party at login: stale face
        // state (U2-D4). While this player's first-boot seed attempt is
        // outstanding the party may be its seed source - run the attempt and
        // wait for it to settle rather than tearing the source down.
        if (c.seedArmed() && !c.seedSettled(id)) {
            c.seedLogin(id);
            if (!c.seedSettled(id)) { retryWatch(id); return; }
            ours = c.mgr().partyOf(id);
            pendingRepair.remove(id);
            if (ours != null) { reconcileParty(ours); return; }
            cur = link.partyOf(id);
            if (!cur.real()) return;
        }
        pendingRepair.remove(id);
        link.removeFromParty(id, cur.name);
        drift(id, "cleared from mcMMO party '" + cur.name + "' - they are not on the party roster (stale face state, cleared at login, never adopted)");
    }

    /** One more watch retry for a login whose picture has not settled; gives up after WATCH_MAX_POLLS. */
    private void retryWatch(UUID id) {
        Integer n = pendingRepair.get(id);
        if (n != null) { if (n + 1 > WATCH_MAX_POLLS) pendingRepair.remove(id); else pendingRepair.put(id, n + 1); }
    }

    private void drift(UUID id, String what) {
        c.logInfo("ExcelTogether mcMMO mirror: drift repaired for " + c.nameOf(id) + " - " + what + " (the roster of record wins; mcMMO is a mirror, never a door).");
    }
}

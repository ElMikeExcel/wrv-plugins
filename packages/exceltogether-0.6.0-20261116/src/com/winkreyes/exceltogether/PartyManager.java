package com.winkreyes.exceltogether;

import java.util.*;
import java.util.function.Predicate;

/**
 * Party state, no Bukkit types, clock passed in so every rule can be unit tested.
 * A party is the group that gets the ExcelTogether bonus. Rules:
 *  - 2 to maxSize members. A party that drops to one member is disbanded.
 *  - Invites expire after inviteTtlMs. The invite is stored against the inviter; the party itself is created
 *    only when the invite is accepted (so an unanswered invite never leaves a one-person party behind).
 *  - A player is in at most one party.
 *  - A member who disconnects stays in the party for graceMs, then is removed. Rejoining in time cancels it.
 * 0.6.0 (P-PARTY-REBUILD-1): this class is the roster of record. Its state is
 * persisted by RosterStore (roster.yml) and rebuilt from that file at enable
 * via {@link #restore} - the pre-0.6.0 behaviour ("parties live in memory
 * only, a restart clears them", rebuilt by adopting whatever the EM/mcMMO
 * faces still held) is gone for the stored case. Every roster mutation
 * carries a {@link RosterOrigin} tag (the pre-0.6.0 signatures keep working
 * and tag with their historical caller class: door paths DOOR, adopt
 * LOGIN_REBUILD); the tag is persisted per party and exposed for the admin
 * status surface and the one-door regression tests. A change listener (set
 * by the plugin) is fired after every roster-changing mutation so the store
 * is written through on change; invites alone never fire it (an invite is
 * not roster state).
 */
public final class PartyManager {
    public static final int MIN_SIZE = 2, MAX_SIZE_LIMIT = 20, DEFAULT_SIZE = 5;
    public static final long DEFAULT_TTL_MS = 60_000L, DEFAULT_GRACE_MS = 300_000L;

    public enum InviteResult { OK, SELF, ALREADY_MEMBER, TARGET_IN_OTHER_PARTY, PARTY_FULL, DUPLICATE }
    public enum AcceptResult { OK, NO_INVITE, EXPIRED, PARTY_FULL, ALREADY_IN_PARTY, INVITER_OFFLINE }
    public enum LeaveResult { NOT_IN_PARTY, LEFT, DISBANDED_BY_LEAVE }
    public enum KickResult { NOT_IN_PARTY, NOT_LEADER, TARGET_NOT_MEMBER, SELF, OK }

    /** Immutable view of a party. */
    public static final class Party {
        public final long id; public final UUID leader; public final List<UUID> members;
        Party(long id, UUID leader, List<UUID> members) { this.id = id; this.leader = leader; this.members = Collections.unmodifiableList(new ArrayList<>(members)); }
        public int size() { return members.size(); }
    }
    public static final class Invite {
        public final UUID inviter, target; public final long expiresAt;
        Invite(UUID i, UUID t, long e) { inviter = i; target = t; expiresAt = e; }
    }
    /** What happened to a party because of leave/kick/sweep, so the plugin can tell the right people. */
    public static final class Change {
        public UUID removed; public UUID newLeader; public boolean disbanded; public List<UUID> remaining = List.of();
    }
    /** Result of a sweep: invites that timed out and members removed after a long disconnect. */
    public static final class SweepResult {
        public final List<Invite> expired = new ArrayList<>();
        public final List<Change> removed = new ArrayList<>();
    }
    public static final class AcceptOutcome {
        public final AcceptResult result; public final Party party; public final Invite invite; public final boolean created;
        AcceptOutcome(AcceptResult r, Party p, Invite i, boolean c) { result = r; party = p; invite = i; created = c; }
    }
    /**
     * 0.6.0: one persisted party, exactly as RosterStore reads/writes it - the
     * transport shape between PartyManager and the roster file. The origin is
     * the tag of the last mutation that created or changed this party.
     */
    public static final class RestoredParty {
        public final long id; public final UUID leader; public final List<UUID> members; public final RosterOrigin origin;
        public RestoredParty(long id, UUID leader, List<UUID> members, RosterOrigin origin) {
            this.id = id; this.leader = leader;
            this.members = Collections.unmodifiableList(new ArrayList<>(members));
            this.origin = origin == null ? RosterOrigin.LOGIN_REBUILD : origin;
        }
    }

    private final int maxSize; private final long ttlMs, graceMs;
    private final Predicate<UUID> online;
    private final Map<UUID, Long> partyOf = new HashMap<>();      // member -> party id
    private final Map<Long, List<UUID>> members = new LinkedHashMap<>();
    private final Map<Long, UUID> leaders = new HashMap<>();
    private final Map<Long, RosterOrigin> origins = new HashMap<>();   // 0.6.0: party id -> last mutation origin
    private final List<Invite> invites = new ArrayList<>();
    private final Map<UUID, Long> offlineSince = new HashMap<>();
    private final Map<UUID, String> names = new HashMap<>();
    private long nextId = 1;
    private RosterOrigin lastOrigin;                              // 0.6.0: origin of the most recent roster mutation (incl. disbands)
    private Runnable changeListener;                              // 0.6.0: fired after roster-changing mutations (the store write-through)

    public PartyManager(int maxSize, long ttlMs, long graceMs, Predicate<UUID> online) {
        this.maxSize = (maxSize >= MIN_SIZE && maxSize <= MAX_SIZE_LIMIT) ? maxSize : DEFAULT_SIZE;
        this.ttlMs = ttlMs > 0 ? ttlMs : DEFAULT_TTL_MS;
        this.graceMs = graceMs >= 0 ? graceMs : DEFAULT_GRACE_MS;
        this.online = online;
    }

    public int maxSize() { return maxSize; }
    public long ttlMs() { return ttlMs; }

    // ---------- 0.6.0: origin tags + persistence hooks ----------
    /** Wire the write-through hook (RosterStore save). Null clears it. A throwing listener never breaks party logic. */
    public synchronized void setChangeListener(Runnable r) { this.changeListener = r; }
    private void fireChanged() {
        Runnable r = changeListener;
        if (r == null) return;
        try { r.run(); } catch (Throwable ignored) { }
    }
    /** Origin tag of the party with this id, or null if there is no such party. */
    public synchronized RosterOrigin originOf(long partyId) { return origins.get(partyId); }
    /** Origin tag of the party this player belongs to, or null if they are in none. */
    public synchronized RosterOrigin originOfPlayer(UUID id) {
        Long pid = partyOf.get(id);
        return pid == null ? null : origins.get(pid);
    }
    /** Origin of the most recent roster mutation anywhere (survives disbands, which erase the party's own tag). */
    public synchronized RosterOrigin lastOrigin() { return lastOrigin; }
    private void tag(long pid, RosterOrigin origin) {
        RosterOrigin o = origin == null ? RosterOrigin.DOOR : origin;
        origins.put(pid, o);
        lastOrigin = o;
    }
    /** Every live party, oldest (lowest id) first - the snapshot RosterStore persists. */
    public synchronized List<Party> allParties() {
        List<Long> ids = new ArrayList<>(members.keySet());
        Collections.sort(ids);
        List<Party> out = new ArrayList<>();
        for (Long pid : ids) out.add(snapshot(pid));
        return out;
    }
    /** The snapshot in store shape (party + its origin tag), oldest first. */
    public synchronized List<RestoredParty> snapshotAll() {
        List<RestoredParty> out = new ArrayList<>();
        for (Party p : allParties()) out.add(new RestoredParty(p.id, p.leader, p.members, origins.get(p.id)));
        return out;
    }
    public synchronized int partyCount() { return members.size(); }
    public synchronized boolean isEmpty() { return members.isEmpty(); }
    /** The id the next created party will take - persisted so a restore never re-issues a live id. */
    public synchronized long nextId() { return nextId; }
    /** Copy of the last-known display names (persisted so offline members can still be listed after a restart). */
    public synchronized Map<UUID, String> namesSnapshot() { return new LinkedHashMap<>(names); }

    /**
     * 0.6.0: rebuild the roster of record from the persisted snapshot (called
     * once at enable, before any sync starts). Replaces all current roster
     * state; invites and offline-grace marks start empty (invites are
     * session state across a restart - they are not persisted; their RELOG
     * survival inside a session is Q4, landed in U2-D4 via onQuit no longer
     * purging them). Defensive by design, mirroring RosterStore's recovery rules so
     * a hand-edited snapshot cannot corrupt the manager: parties under
     * MIN_SIZE are dropped, a member listed in two parties stays in the
     * first, a leader missing from the member list is replaced by the
     * longest-standing member, and nextId is raised past every restored id.
     * Does NOT fire the change listener (restoring is not a mutation).
     * @return how many parties were restored
     */
    public synchronized int restore(List<RestoredParty> parties, Map<UUID, String> restoredNames, long nextIdHint) {
        partyOf.clear(); members.clear(); leaders.clear(); origins.clear();
        invites.clear(); offlineSince.clear();
        if (restoredNames != null) names.putAll(restoredNames);
        long maxId = 0;
        if (parties != null) {
            List<RestoredParty> sorted = new ArrayList<>(parties);
            sorted.sort(Comparator.comparingLong(p -> p.id));
            for (RestoredParty rp : sorted) {
                if (rp == null || rp.leader == null) continue;
                List<UUID> ms = new ArrayList<>();
                for (UUID u : rp.members) if (u != null && !partyOf.containsKey(u) && !ms.contains(u)) ms.add(u);
                if (ms.size() < MIN_SIZE) continue;   // too small to be a party: its members stay unclaimed
                UUID leader = ms.contains(rp.leader) ? rp.leader : ms.get(0);
                long pid = rp.id > 0 ? rp.id : ++maxId;
                members.put(pid, ms);
                leaders.put(pid, leader);
                origins.put(pid, rp.origin);
                for (UUID u : ms) partyOf.put(u, pid);
                if (pid > maxId) maxId = pid;
            }
        }
        nextId = Math.max(Math.max(nextIdHint, maxId + 1), 1L);
        if (!origins.isEmpty()) lastOrigin = RosterOrigin.LOGIN_REBUILD;
        return members.size();
    }

    // ---------- names (so offline members can still be listed) ----------
    public synchronized void rememberName(UUID id, String display) { if (id != null && display != null && !display.isEmpty()) names.put(id, display); }
    public synchronized String nameOf(UUID id) { return names.getOrDefault(id, "Someone"); }

    // ---------- queries ----------
    public synchronized Party partyOf(UUID id) {
        Long pid = partyOf.get(id);
        return pid == null ? null : snapshot(pid);
    }
    public synchronized boolean inParty(UUID id) { return partyOf.containsKey(id); }
    public synchronized boolean sameParty(UUID a, UUID b) {
        Long x = partyOf.get(a), y = partyOf.get(b);
        return x != null && x.equals(y);
    }
    public synchronized boolean isOfflineGrace(UUID id) { return offlineSince.containsKey(id); }
    private Party snapshot(long pid) { return new Party(pid, leaders.get(pid), members.get(pid)); }

    /** Valid (not expired) invites waiting for this player, oldest first. */
    public synchronized List<Invite> invitesFor(UUID target, long now) {
        List<Invite> out = new ArrayList<>();
        for (Invite i : invites) if (i.target.equals(target) && now <= i.expiresAt) out.add(i);
        return out;
    }
    /** Valid (not expired) invites sent by this player, oldest first. */
    public synchronized List<Invite> invitesFrom(UUID inviter, long now) {
        List<Invite> out = new ArrayList<>();
        for (Invite i : invites) if (i.inviter.equals(inviter) && now <= i.expiresAt) out.add(i);
        return out;
    }
    /** True if this player has any unexpired invite still waiting for an answer. */
    public synchronized boolean hasOutgoing(UUID inviter, long now) {
        for (Invite i : invites) if (i.inviter.equals(inviter) && now <= i.expiresAt) return true;
        return false;
    }
    public synchronized boolean hasInvite(UUID inviter, UUID target, long now) {
        for (Invite i : invites) if (i.inviter.equals(inviter) && i.target.equals(target) && now <= i.expiresAt) return true;
        return false;
    }

    // ---------- invite ----------
    public synchronized InviteResult invite(UUID inviter, UUID target, long now) {
        if (inviter.equals(target)) return InviteResult.SELF;
        Long mine = partyOf.get(inviter), theirs = partyOf.get(target);
        if (theirs != null) return theirs.equals(mine) ? InviteResult.ALREADY_MEMBER : InviteResult.TARGET_IN_OTHER_PARTY;
        if (mine != null && members.get(mine).size() >= maxSize) return InviteResult.PARTY_FULL;
        dropExpired(now);
        for (Invite i : invites) if (i.inviter.equals(inviter) && i.target.equals(target)) return InviteResult.DUPLICATE;
        invites.add(new Invite(inviter, target, now + ttlMs));
        return InviteResult.OK;
    }

    // ---------- accept / decline ----------
    /** inviter null = the newest valid invite. Door-origin accept (the pre-0.6.0 signature). */
    public synchronized AcceptOutcome accept(UUID target, UUID inviter, long now) {
        return accept(target, inviter, now, RosterOrigin.DOOR);
    }
    /** inviter null = the newest valid invite. */
    public synchronized AcceptOutcome accept(UUID target, UUID inviter, long now, RosterOrigin origin) {
        Invite found = null; boolean sawExpired = false;
        for (int k = invites.size() - 1; k >= 0; k--) {
            Invite i = invites.get(k);
            if (!i.target.equals(target) || (inviter != null && !i.inviter.equals(inviter))) continue;
            if (now > i.expiresAt) { sawExpired = true; continue; }
            found = i; break;
        }
        if (found == null) return new AcceptOutcome(sawExpired ? AcceptResult.EXPIRED : AcceptResult.NO_INVITE, null, null, false);
        if (partyOf.containsKey(target)) { invites.remove(found); return new AcceptOutcome(AcceptResult.ALREADY_IN_PARTY, null, found, false); }
        if (!online.test(found.inviter)) { invites.remove(found); return new AcceptOutcome(AcceptResult.INVITER_OFFLINE, null, found, false); }
        Long pid = partyOf.get(found.inviter);
        boolean created = false;
        if (pid == null) {
            pid = nextId++;
            members.put(pid, new ArrayList<>(List.of(found.inviter)));
            leaders.put(pid, found.inviter);
            partyOf.put(found.inviter, pid);
            created = true;
        } else if (members.get(pid).size() >= maxSize) {
            invites.remove(found);
            return new AcceptOutcome(AcceptResult.PARTY_FULL, null, found, false);
        }
        members.get(pid).add(target);
        partyOf.put(target, pid);
        offlineSince.remove(target);
        invites.removeIf(i -> i.target.equals(target));      // joined: every other invite to them is moot
        final long joined = pid;
        invites.removeIf(i -> Objects.equals(partyOf.get(i.inviter), joined) && Objects.equals(partyOf.get(i.target), joined));  // invites inside the party
        tag(pid, origin);
        fireChanged();
        return new AcceptOutcome(AcceptResult.OK, snapshot(pid), found, created);
    }

    /** Returns the declined invite, or null if there was none valid. inviter null = newest valid invite. */
    public synchronized Invite decline(UUID target, UUID inviter, long now) {
        for (int k = invites.size() - 1; k >= 0; k--) {
            Invite i = invites.get(k);
            if (!i.target.equals(target) || (inviter != null && !i.inviter.equals(inviter))) continue;
            invites.remove(k);
            return now > i.expiresAt ? null : i;
        }
        return null;
    }

    // ---------- adoption (a join that happened in mcMMO by hand) ----------
    public enum AdoptResult { JOINED, CREATED, FULL, ALREADY_IN_PARTY, SELF }
    public static final class AdoptOutcome {
        public final AdoptResult result; public final Party party;
        AdoptOutcome(AdoptResult r, Party p) { result = r; party = p; }
    }
    /**
     * Put joiner into anchor's party, making the party (anchor as leader) if anchor has none. No invite needed: mcMMO already agreed.
     * 0.6.0 note: this is a LOGIN_REBUILD-class entry by design brief - under the one-door design its only
     * surviving callers are the login rebuild / first-boot seed; the sync engines' adoption calls are deleted
     * with the unit-2 sync rewrite. Until then their adoptions are tagged truthfully as rebuilds, never DOOR.
     */
    public synchronized AdoptOutcome adopt(UUID joiner, UUID anchor) {
        return adopt(joiner, anchor, RosterOrigin.LOGIN_REBUILD);
    }
    public synchronized AdoptOutcome adopt(UUID joiner, UUID anchor, RosterOrigin origin) {
        if (joiner.equals(anchor)) return new AdoptOutcome(AdoptResult.SELF, null);
        if (partyOf.containsKey(joiner)) return new AdoptOutcome(AdoptResult.ALREADY_IN_PARTY, null);
        Long pid = partyOf.get(anchor);
        boolean created = false;
        if (pid == null) {
            pid = nextId++;
            members.put(pid, new ArrayList<>(List.of(anchor)));
            leaders.put(pid, anchor);
            partyOf.put(anchor, pid);
            offlineSince.remove(anchor);
            created = true;
        } else if (members.get(pid).size() >= maxSize) {
            return new AdoptOutcome(AdoptResult.FULL, null);
        }
        members.get(pid).add(joiner);
        partyOf.put(joiner, pid);
        offlineSince.remove(joiner);
        invites.removeIf(i -> i.target.equals(joiner));
        final long fp = pid;
        invites.removeIf(i -> Objects.equals(partyOf.get(i.inviter), fp) && Objects.equals(partyOf.get(i.target), fp));
        tag(pid, origin);
        fireChanged();
        return new AdoptOutcome(created ? AdoptResult.CREATED : AdoptResult.JOINED, snapshot(pid));
    }
    /** Make an existing member the leader. False if they are not in a party or already lead it. */
    public synchronized boolean setLeader(UUID member) { return setLeader(member, RosterOrigin.DOOR); }
    public synchronized boolean setLeader(UUID member, RosterOrigin origin) {
        Long pid = partyOf.get(member);
        if (pid == null || member.equals(leaders.get(pid))) return false;
        leaders.put(pid, member);
        tag(pid, origin);
        fireChanged();
        return true;
    }
    /** Every player currently in any party (online or in the offline grace window). */
    public synchronized List<UUID> allMembers() { return new ArrayList<>(partyOf.keySet()); }

    // ---------- leave / kick ----------
    public synchronized LeaveResult leave(UUID id, Change out) { return leave(id, out, RosterOrigin.DOOR); }
    public synchronized LeaveResult leave(UUID id, Change out, RosterOrigin origin) {
        Long pid = partyOf.get(id);
        if (pid == null) return LeaveResult.NOT_IN_PARTY;
        boolean disbanded = remove(pid, id, out, origin);
        fireChanged();
        return disbanded ? LeaveResult.DISBANDED_BY_LEAVE : LeaveResult.LEFT;
    }
    public synchronized KickResult kick(UUID leader, UUID target, Change out) { return kick(leader, target, out, RosterOrigin.DOOR); }
    public synchronized KickResult kick(UUID leader, UUID target, Change out, RosterOrigin origin) {
        Long pid = partyOf.get(leader);
        if (pid == null) return KickResult.NOT_IN_PARTY;
        if (!leaders.get(pid).equals(leader)) return KickResult.NOT_LEADER;
        if (leader.equals(target)) return KickResult.SELF;
        if (!pid.equals(partyOf.get(target))) return KickResult.TARGET_NOT_MEMBER;
        remove(pid, target, out, origin);
        fireChanged();
        return KickResult.OK;
    }

    /** Removes one member. Leader leaving passes leadership to the longest-standing member. Returns true if disbanded. */
    private boolean remove(long pid, UUID id, Change out, RosterOrigin origin) {
        List<UUID> m = members.get(pid);
        m.remove(id);
        partyOf.remove(id);
        offlineSince.remove(id);
        invites.removeIf(i -> i.inviter.equals(id));
        if (out != null) out.removed = id;
        lastOrigin = origin == null ? RosterOrigin.DOOR : origin;
        if (m.size() < MIN_SIZE) {
            if (out != null) { out.disbanded = true; out.remaining = new ArrayList<>(m); }
            for (UUID u : m) { partyOf.remove(u); offlineSince.remove(u); }
            members.remove(pid); leaders.remove(pid); origins.remove(pid);
            return true;
        }
        origins.put(pid, lastOrigin);
        if (leaders.get(pid).equals(id)) {
            UUID next = m.get(0);
            for (UUID u : m) if (!offlineSince.containsKey(u)) { next = u; break; }   // prefer someone who is online
            leaders.put(pid, next);
            if (out != null) out.newLeader = next;
        }
        if (out != null) out.remaining = new ArrayList<>(m);
        return false;
    }

    // ---------- connection events ----------
    /**
     * 0.6.0 (U2-D4, D74/Q4): a quit does NOT purge invites any more. A pending
     * hub invite is data, not session state: it survives either side's relog
     * for its remaining TTL and dies only by expiry (the sweep reports it and
     * Controller.tick messages both sides), by accept/decline, or by an accept
     * attempted while the inviter is offline (INVITER_OFFLINE, told to the
     * accepter). The pre-0.6.0 silent purge on quit is deleted. (A server
     * RESTART still starts invites empty - restore() below; invites are not
     * persisted in roster.yml.)
     */
    public synchronized void onQuit(UUID id, long now) {
        if (partyOf.containsKey(id)) offlineSince.put(id, now);
    }
    public synchronized void onJoin(UUID id) { offlineSince.remove(id); }

    private void dropExpired(long now) { invites.removeIf(i -> now > i.expiresAt); }

    /** Call about once a second. Expired invites are removed and returned; long-gone members are removed. */
    public synchronized SweepResult sweep(long now) {
        SweepResult r = new SweepResult();
        for (Iterator<Invite> it = invites.iterator(); it.hasNext(); ) {
            Invite i = it.next();
            if (now > i.expiresAt) { r.expired.add(i); it.remove(); }
        }
        for (UUID id : new ArrayList<>(offlineSince.keySet())) {
            Long since = offlineSince.get(id);
            if (since == null) continue;                 // already removed by an earlier disband in this sweep
            if (now - since < graceMs) continue;
            Long pid = partyOf.get(id);
            offlineSince.remove(id);
            if (pid == null) continue;
            Change c = new Change();
            // A grace expiry is not a door act: the party keeps its existing origin tag.
            remove(pid, id, c, origins.getOrDefault(pid, RosterOrigin.LOGIN_REBUILD));
            r.removed.add(c);
        }
        if (!r.removed.isEmpty()) fireChanged();
        return r;
    }
}

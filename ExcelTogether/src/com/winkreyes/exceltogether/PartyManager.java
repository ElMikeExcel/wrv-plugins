package com.winkreyes.exceltogether;

import java.util.*;
import java.util.function.Predicate;

/**
 * Party state, no Bukkit types, clock passed in so every rule can be unit tested.
 * A party is the group that gets the ExcelTogether bonus. Rules:
 *  - 2 to maxSize members. A party that drops to one member is disbanded.
 *  - Invites expire after inviteTtlMs. The invite is stored against the inviter; the party itself is created
 *    only when the invite is accepted (so an unanswered invite never leaves a one-person party behind).
 *  - A player is in at most one party. Parties live in memory only (a restart clears them).
 *  - A member who disconnects stays in the party for graceMs, then is removed. Rejoining in time cancels it.
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

    private final int maxSize; private final long ttlMs, graceMs;
    private final Predicate<UUID> online;
    private final Map<UUID, Long> partyOf = new HashMap<>();      // member -> party id
    private final Map<Long, List<UUID>> members = new LinkedHashMap<>();
    private final Map<Long, UUID> leaders = new HashMap<>();
    private final List<Invite> invites = new ArrayList<>();
    private final Map<UUID, Long> offlineSince = new HashMap<>();
    private final Map<UUID, String> names = new HashMap<>();
    private long nextId = 1;

    public PartyManager(int maxSize, long ttlMs, long graceMs, Predicate<UUID> online) {
        this.maxSize = (maxSize >= MIN_SIZE && maxSize <= MAX_SIZE_LIMIT) ? maxSize : DEFAULT_SIZE;
        this.ttlMs = ttlMs > 0 ? ttlMs : DEFAULT_TTL_MS;
        this.graceMs = graceMs >= 0 ? graceMs : DEFAULT_GRACE_MS;
        this.online = online;
    }

    public int maxSize() { return maxSize; }
    public long ttlMs() { return ttlMs; }

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
    /** inviter null = the newest valid invite. */
    public synchronized AcceptOutcome accept(UUID target, UUID inviter, long now) {
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

    // ---------- leave / kick ----------
    public synchronized LeaveResult leave(UUID id, Change out) {
        Long pid = partyOf.get(id);
        if (pid == null) return LeaveResult.NOT_IN_PARTY;
        boolean disbanded = remove(pid, id, out);
        return disbanded ? LeaveResult.DISBANDED_BY_LEAVE : LeaveResult.LEFT;
    }
    public synchronized KickResult kick(UUID leader, UUID target, Change out) {
        Long pid = partyOf.get(leader);
        if (pid == null) return KickResult.NOT_IN_PARTY;
        if (!leaders.get(pid).equals(leader)) return KickResult.NOT_LEADER;
        if (leader.equals(target)) return KickResult.SELF;
        if (!pid.equals(partyOf.get(target))) return KickResult.TARGET_NOT_MEMBER;
        remove(pid, target, out);
        return KickResult.OK;
    }

    /** Removes one member. Leader leaving passes leadership to the longest-standing member. Returns true if disbanded. */
    private boolean remove(long pid, UUID id, Change out) {
        List<UUID> m = members.get(pid);
        m.remove(id);
        partyOf.remove(id);
        offlineSince.remove(id);
        invites.removeIf(i -> i.inviter.equals(id));
        if (out != null) out.removed = id;
        if (m.size() < MIN_SIZE) {
            if (out != null) { out.disbanded = true; out.remaining = new ArrayList<>(m); }
            for (UUID u : m) { partyOf.remove(u); offlineSince.remove(u); }
            members.remove(pid); leaders.remove(pid);
            return true;
        }
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
    public synchronized void onQuit(UUID id, long now) {
        invites.removeIf(i -> i.target.equals(id) || i.inviter.equals(id));
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
            remove(pid, id, c);
            r.removed.add(c);
        }
        return r;
    }
}

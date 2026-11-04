package com.winkreyes.exceltogether;

import java.util.*;

/**
 * Reconciles the EliteMobs party (a session view) with the ExcelTogether hub, a sibling of
 * {@link McmmoSync} with the same guard-set vocabulary and the same state-based rule: compare what
 * EM says with what we hold and fix the difference, so running it twice is harmless (DESIGN §2).
 *
 * Loop guards (§2.4): a change read FROM EliteMobs is adopted into Together with zero EmLink drive
 * calls; adapter-driven EM changes are recorded in {@code emMirrored} before the next poll reads
 * them, so the poll sees convergence, not a new EM-door action; Together-originated removals
 * suppress in both syncs. At rest, a poll drives nothing and says nothing.
 *
 * EM extras are never yanked out of EM by a poll: an EM member who is not in the Together party is
 * adopted in (Case B pattern) or wait-listed when Together is full. The only EmSync-originated
 * leave drives are the leader-reform (§4) and enforcement for a player Together deliberately
 * removed (suppressed) who still sits in the EM twin - the §2.3 removal sentence read together
 * with test B8 (two different full parties sharing one member must not churn).
 *
 * 0.5.3 (BUG-VERDICT.md, Mike's ruling Nov 3, 2026 - a voluntary leave sticks): two changes.
 * (1) The EM door is a real door: a settled, mirrored, online member whose EM picture goes NONE
 * while their EM twin survives without them has LEFT the party - the hub leave runs with the
 * full mirror cascade, exactly like a Together-door leave. This replaces the §4 L4 rule for
 * deliberate leaves; quits and relogs are guarded out (offline players are never detected, and
 * the login watch must settle first), so materialise-on-login is unchanged. The config key
 * mesh.em.door-leave-ends-party (default true) restores the view-only semantics when off.
 * (2) Suppression is roster-keyed, not partyKey-keyed: a removed player is held out while EM
 * lists them in any party made only of the people they were removed from (a re-created twin
 * under a fresh Party UUID included), and the hold clears only when EM shows them partyless
 * or grouped with someone new.
 */
final class EmSync {
    static final int EM_WATCH_MAX_POLLS = 30;               // about a minute at the default cadence
    private static final int EM_CAP = 5;                    // Party.MAX_MEMBERS in EliteMobs (hard constant)

    private final Controller c;
    // knobs, set from config by the plugin (adjustability law; defaults are the settled values)
    private boolean loginMaterialize = true;
    private boolean leaderReform = false;
    private boolean doorLeaveEndsParty = true;
    private long quietMs = 45_000L;
    private long deferMaxMs = 300_000L;
    private boolean debug = false;

    /** players known to be in both systems together: id -> EM partyKey */
    private final Map<UUID, Object> emMirrored = new HashMap<>();
    /** removed from ours on purpose; do not re-adopt while EM still lists them with the same people */
    private final Set<UUID> emSuppressed = new HashSet<>();
    private final Map<UUID, Object> emSuppressedKey = new HashMap<>();
    /** who a suppressed player was removed from (0.5.3): the hold is roster-based, not key-based */
    private final Map<UUID, Set<UUID>> emSuppressedRoster = new HashMap<>();
    /** told once that a party was full */
    private final Set<UUID> emFullNoted = new HashSet<>();
    /** in one system but did not fit in the other; retried on every poll until a seat frees */
    private final Set<UUID> emWaiting = new LinkedHashSet<>();
    /** just logged in: check until both pictures settle */
    private final Map<UUID, Integer> emWatching = new LinkedHashMap<>();
    /** told once that a static EM/Together disagreement is being left alone */
    private final Set<UUID> emKeptNoted = new HashSet<>();
    /** EM partyKey seen at the previous poll, for deliberate-action detection (§2.3 Case E) */
    private final Map<UUID, Object> lastEmPartyKey = new HashMap<>();
    /** told once about an EliteMobs permission refusal */
    private final Set<UUID> emPermNoted = new HashSet<>();
    /** Together party ids the mesh treats as live (interactively formed, adopted, or accepted into) */
    private final Set<Long> emLiveParties = new HashSet<>();
    /** per-party ready-check quiet window (freeze F2): Together party id -> quiet until (ms) */
    private final Map<Long, Long> quietUntil = new HashMap<>();
    /** per-party deferred EM writes (freeze §7.1), keyed by leader-of-record */
    private final Map<UUID, Deque<Op>> deferred = new LinkedHashMap<>();
    /** Together parties already converged in this poll (party-level work runs once per party) */
    private final Set<Long> convergedThisPoll = new HashSet<>();

    private static final class Op {
        enum Kind { MATERIALIZE, ADD, REMOVE, REFORM }
        final Kind kind; final UUID target; final long at;
        Op(Kind kind, UUID target, long at) { this.kind = kind; this.target = target; this.at = at; }
    }

    EmSync(Controller c) { this.c = c; }

    void setLoginMaterialize(boolean v) { loginMaterialize = v; }
    void setLeaderReform(boolean v) { leaderReform = v; }
    void setDoorLeaveEndsParty(boolean v) { doorLeaveEndsParty = v; }
    boolean doorLeaveEndsParty() { return doorLeaveEndsParty; }
    void setQuietSeconds(long s) { quietMs = Math.max(0L, s) * 1000L; }
    void setDeferMaxSeconds(long s) { deferMaxMs = Math.max(0L, s) * 1000L; }
    void setDebug(boolean v) { debug = v; }
    boolean loginMaterialize() { return loginMaterialize; }
    boolean leaderReform() { return leaderReform; }

    void watch(UUID id) { if (id != null) emWatching.put(id, 0); }
    boolean isWaiting(UUID id) { return emWaiting.contains(id); }
    boolean isMirrored(UUID id) { return emMirrored.containsKey(id); }
    boolean isSuppressed(UUID id) { return emSuppressed.contains(id); }

    /** Together removed this player on purpose (leave/kick/disband): hold them out of re-adoption. */
    void suppress(UUID id) { suppress(id, null); }

    /**
     * As {@link #suppress(UUID)}, with the roster the player was removed from (the leaver plus
     * everyone who was in their party at the removal, from the PartyManager.Change). The roster
     * is what the hold compares against (0.5.3): an EM party re-created under a new partyKey
     * around the same people must not defeat the hold, while a group containing anyone new is a
     * deliberate fresh join and clears it. A null/empty hint falls back to the current EM picture.
     */
    void suppress(UUID id, Set<UUID> rosterHint) {
        if (id == null) return;
        Object key = emMirrored.get(id);
        if (key == null) {
            try { EmLink.EmParty cur = c.emLink().partyOf(id); if (cur.real()) key = cur.partyKey; } catch (Throwable ignored) { }
        }
        Set<UUID> roster = new HashSet<>();
        if (rosterHint != null) roster.addAll(rosterHint);
        if (roster.isEmpty()) {
            try { EmLink.EmParty cur = c.emLink().partyOf(id); if (cur.real()) roster.addAll(cur.members); } catch (Throwable ignored) { }
        }
        roster.add(id);
        emSuppressed.add(id);
        emSuppressedKey.put(id, key);
        emSuppressedRoster.put(id, roster);
        emMirrored.remove(id);
    }
    /** A Together accept undoes a suppression for the accepter (mirrors McmmoSync.clear). */
    void clear(UUID id) { emSuppressed.remove(id); emSuppressedKey.remove(id); emSuppressedRoster.remove(id); }

    /**
     * Does id's removal hold against this EM party? Roster-based (0.5.3): every member of the EM
     * party must be someone id was removed alongside. With no roster on record (a suppression
     * from before 0.5.3 semantics) the partyKey comparison is the fallback.
     */
    boolean suppressionApplies(UUID id, EmLink.EmParty em) {
        if (!emSuppressed.contains(id) || em == null || !em.real()) return false;
        Set<UUID> roster = emSuppressedRoster.get(id);
        if (roster == null || roster.isEmpty()) return Objects.equals(emSuppressedKey.get(id), em.partyKey);
        for (UUID m : em.members) if (!roster.contains(m)) return false;
        return true;
    }

    private void dbg(String line) { if (debug) c.logInfo("ExcelTogether mesh debug: " + line); }

    /** Party-level convergence runs once per party per cycle; a fresh cycle starts here. */
    void beginCycle() { convergedThisPoll.clear(); }

    // ---------- entry points from Controller ----------
    void poll() {
        EmLink link = c.emLink();
        if (!link.available()) return;
        beginCycle();
        runDeferred(link);
        Set<UUID> ids = new LinkedHashSet<>(c.mgr().allMembers());
        for (EmLink.EmParty p : link.allParties()) ids.addAll(p.members);
        ids.addAll(emMirrored.keySet());
        ids.addAll(emWaiting);
        ids.addAll(emWatching.keySet());
        ids.addAll(emSuppressed);
        ids.addAll(lastEmPartyKey.keySet());
        if (!ids.isEmpty()) reconcile(ids);
    }

    void reconcile(Collection<UUID> ids) {
        EmLink link = c.emLink();
        if (!link.available()) return;
        for (UUID id : new ArrayList<>(new LinkedHashSet<>(ids))) {
            if (id == null) continue;
            try { one(link, id); } catch (RuntimeException ignored) { }
        }
    }

    /** A Together-door accept just landed (DESIGN §5.2): drive the EM join now, don't wait for the poll. */
    void noteJoined(UUID target, UUID inviter) {
        EmLink link = c.emLink();
        if (!link.available()) return;
        PartyManager.Party ours = c.mgr().partyOf(target);
        if (ours != null) emLiveParties.add(ours.id);
        EmLink.EmParty em = link.partyOf(inviter);
        if (!em.real()) return;                       // no EM party yet: the poll materialises (party is live)
        if (em.members.contains(target)) { markEm(target, em.partyKey); markEm(inviter, em.partyKey); return; }
        if (frozenParty(link, ours, em.members)) { enqueue(ours.leader, Op.Kind.ADD, target); return; }
        if (!link.inviteDriven(inviter, target)) {
            EmLink.EmMirror r = link.invite(inviter, target);
            if (r != EmLink.EmMirror.OK) { noteDriveResult(ours, inviter, target, r, true); return; }
        }
        EmLink.EmMirror r = link.accept(target);
        EmLink.EmParty back = link.partyOf(inviter);
        if (back.real() && back.members.contains(target)) { markEm(target, back.partyKey); markEm(inviter, back.partyKey); }
        else if (r != EmLink.EmMirror.OK) noteDriveResult(ours, inviter, target, r, true);
    }

    /** Controller drove a Together-originated EM leave for this party; remember it as live-touched. */
    void noteLiveParty(PartyManager.Party pt) { if (pt != null) emLiveParties.add(pt.id); }

    // ---------- the reconcile cases (DESIGN §2.3) ----------
    private void one(EmLink link, UUID id) {
        EmLink.EmParty em = link.partyOf(id);
        if (em == EmLink.EmParty.UNKNOWN) {
            Integer n = emWatching.get(id);
            if (n != null) { if (n + 1 > EM_WATCH_MAX_POLLS) emWatching.remove(id); else emWatching.put(id, n + 1); }
            return;
        }
        Object key = em.real() ? em.partyKey : null;
        boolean hasPrev = lastEmPartyKey.containsKey(id);
        Object prevKey = lastEmPartyKey.get(id);
        boolean emChanged = hasPrev && prevKey != null && !prevKey.equals(key);

        if (emSuppressed.contains(id)) {
            if (!em.real()) {
                clear(id);                            // EM agrees they are out: the suppression is spent
            } else if (suppressionApplies(id, em)) {
                lastEmPartyKey.put(id, key);
                return;                               // still grouped only with the people they were removed from: hold
            } else {
                clear(id);                            // their EM party includes someone new: a deliberate fresh join, proceed
            }
        }
        PartyManager mgr = c.mgr();
        PartyManager.Party ours = mgr.partyOf(id);
        boolean wasWatching = emWatching.containsKey(id);
        if (em.real() || ours == null) emWatching.remove(id);   // a settled picture ends the watch; Case C keeps it until materialise marks them (markEm clears it), so a relog is never mistaken for an EM-door leave
        if (emWaiting.contains(id) && c.env().online(id) == null) emWaiting.remove(id);

        // 0.5.3: a settled member who stepped out at the EM door has left the party (see the
        // class header). Runs before the cases: Case C would otherwise materialise them back in.
        if (doorLeaveEndsParty && !em.real() && ours != null && !wasWatching && emDoorLeft(link, id, ours)) {
            leaveFromEmDoor(id);
            lastEmPartyKey.put(id, null);
            return;
        }

        if (!em.real() && ours == null) {             // Case A - both agree: partyless
            emMirrored.remove(id); emFullNoted.remove(id); emWaiting.remove(id); emKeptNoted.remove(id);
            lastEmPartyKey.put(id, null);
            return;
        }
        if (em.real() && ours == null) {              // Case B - EM only: adopt into Together
            caseB(link, id, em);
            lastEmPartyKey.put(id, key);
            return;
        }
        if (!em.real()) {                             // Case C - Together only: materialise
            // (0.5.3: a settled member's EM-door leave was already honoured above. Whoever
            // reaches here was never settled in EM - logging in, returning, or never faced -
            // so materialisation/convergence rebuilds their face, as before. With
            // doorLeaveEndsParty off, EM-door leavers land here too: the 0.5.x view semantics.)
            materialise(link, ours, false);
            lastEmPartyKey.put(id, null);
            return;
        }
        // both real
        int shared = 0;
        for (UUID m : em.members) if (ours.members.contains(m)) shared++;
        if (shared >= 2) {                            // Case D - the same party, modulo drift
            if (convergedThisPoll.add(ours.id)) converge(link, ours, em);
        } else {                                      // Case E - disjoint groups (only id in common)
            boolean moved = (emMirrored.containsKey(id) && !Objects.equals(emMirrored.get(id), key))
                    || emChanged || wasWatching;
            if (moved) {
                if (prevKey != null || emMirrored.containsKey(id)) {
                    quietLeaveEm(id, "You left the ExcelTogether party because you moved to another EliteMobs party.",
                            " moved to another EliteMobs party and so left the party.");
                } else {
                    c.tell(id, "Note: you joined an EliteMobs party with different people, so you left your old ExcelTogether party and joined theirs.");
                    quietLeaveEm(id, null, " joined a different EliteMobs party and so left the party.");
                }
                lastEmPartyKey.put(id, key);
                PartyManager.Party now = mgr.partyOf(id);
                if (now == null) caseB(link, id, em);
                return;
            }
            if (emKeptNoted.add(id)) c.tell(id, "Note: you are in a different EliteMobs party, but you stay in your ExcelTogether party. Leave one of them if you want them to line up.");
        }
        lastEmPartyKey.put(id, key);
    }

    /** Case B: an EM group with no Together party lands in Together (anchor or build), zero EmLink drives. */
    private void caseB(EmLink link, UUID id, EmLink.EmParty em) {
        PartyManager mgr = c.mgr();
        List<UUID> group = em.members;                // display order: EM leader first
        List<UUID> adopted = new ArrayList<>();
        PartyManager.Party anchor = null; int best = 0;
        if (em.leader != null) {
            PartyManager.Party lp = mgr.partyOf(em.leader);
            if (lp != null) { int n = countIn(group, lp); if (n > 0) { anchor = lp; best = n; } }
        }
        for (UUID m : group) {
            PartyManager.Party p = mgr.partyOf(m);
            if (p != null && (anchor == null || p.id != anchor.id)) {
                int n = countIn(group, p);
                if (n > best) { anchor = p; best = n; }
            }
        }
        if (anchor != null) {
            for (UUID m : group) {
                PartyManager.Party mp = mgr.partyOf(m);
                if (mp != null) { if (mp.id == anchor.id) markEm(m, em.partyKey); continue; }
                if (suppressionApplies(m, em) || c.env().online(m) == null) continue;
                PartyManager.AdoptOutcome o = mgr.adopt(m, anchor.leader);
                if (o.result == PartyManager.AdoptResult.FULL) { emWaiting.add(m); fullNoteTogether(m); continue; }
                if (o.result == PartyManager.AdoptResult.JOINED) {
                    adopted.add(m); markEm(m, em.partyKey);
                    c.tell(m, "You were added to the ExcelTogether party (" + c.sizeText(o.party) + ") because you are in the same EliteMobs party. " + c.bonusNote());
                    c.tellParty(o.party, m, c.nameOf(m) + " joined the party (" + c.sizeText(o.party) + ").");
                }
            }
            PartyManager.Party now = mgr.partyOf(anchor.leader);
            if (now != null) emLiveParties.add(now.id);
        } else {
            UUID first = null;
            for (UUID m : group) {
                if (c.env().online(m) != null && !mgr.inParty(m) && !suppressionApplies(m, em)) { first = m; break; }
            }
            if (first == null) return;
            for (UUID m : group) {
                if (m.equals(first) || mgr.inParty(m) || c.env().online(m) == null || suppressionApplies(m, em)) continue;
                PartyManager.AdoptOutcome o = mgr.adopt(m, first);
                if (o.result == PartyManager.AdoptResult.FULL) { emWaiting.add(m); fullNoteTogether(m); continue; }
                if (o.party != null) {
                    adopted.add(m); markEm(m, em.partyKey);
                    if (o.result == PartyManager.AdoptResult.CREATED) {
                        markEm(first, em.partyKey);
                        c.tell(first, "Your EliteMobs party also made an ExcelTogether party (" + c.sizeText(o.party) + "). " + c.bonusNote());
                    }
                    c.tell(m, "You were added to the ExcelTogether party (" + c.sizeText(o.party) + ") because you are in the same EliteMobs party. " + c.bonusNote());
                    c.tellParty(o.party, m, c.nameOf(m) + " joined the party (" + c.sizeText(o.party) + ").");
                }
            }
            PartyManager.Party made = mgr.partyOf(first);
            if (made != null) { emLiveParties.add(made.id); if (!adopted.contains(first)) adopted.add(first); }
        }
        if (!adopted.isEmpty()) c.mcmmoChanged(adopted, List.of());   // the mcMMO mirror follows, as after a Together accept
        dbg("caseB adopted " + adopted.size() + " from EM party " + em.partyKey);
    }

    private int countIn(List<UUID> group, PartyManager.Party p) {
        int n = 0; for (UUID m : group) if (p.members.contains(m)) n++; return n;
    }

    /** Case D: both hold the same party (a shared core of 2+). Converge at party level, once per poll. */
    private void converge(EmLink link, PartyManager.Party ours, EmLink.EmParty em) {
        PartyManager mgr = c.mgr();
        // 1) EM members missing from Together land in Together (outbound from EM: never frozen, §7 F3)
        List<UUID> adopted = new ArrayList<>();
        for (UUID m : em.members) {
            PartyManager.Party mp = mgr.partyOf(m);
            if (mp != null) { if (mp.id == ours.id) markEm(m, em.partyKey); continue; }
            if (suppressionApplies(m, em)) { driveRemove(link, ours, m); continue; }   // a deliberate removal the EM leg still shows: enforce it
            if (c.env().online(m) == null) continue;
            PartyManager.AdoptOutcome o = mgr.adopt(m, ours.leader);
            if (o.result == PartyManager.AdoptResult.FULL) { emWaiting.add(m); fullNoteTogether(m); continue; }
            if (o.result == PartyManager.AdoptResult.JOINED || o.result == PartyManager.AdoptResult.CREATED) {
                adopted.add(m); markEm(m, em.partyKey);
                c.tell(m, "You were added to the ExcelTogether party (" + c.sizeText(o.party) + ") because you are in the same EliteMobs party. " + c.bonusNote());
                c.tellParty(o.party, m, c.nameOf(m) + " joined the party (" + c.sizeText(o.party) + ").");
            }
        }
        if (!adopted.isEmpty()) c.mcmmoChanged(adopted, List.of());
        ours = mgr.partyOf(ours.leader);
        if (ours == null) return;
        emLiveParties.add(ours.id);
        // 2) leader parity (§4): best-effort; reform only when the config asks for it
        if (em.leader != null && !em.leader.equals(ours.leader) && leaderReform) { reform(link, ours); return; }
        // 3) Together members missing from EM are driven in (freeze-gated). The inviter is the
        //    EM party's own leader - the Together leader may be the one missing (§4 L4).
        EmLink.EmParty now = link.partyOf(em.leader);
        if (!now.real() || !Objects.equals(now.partyKey, em.partyKey)) return;   // EM moved under us: next poll sorts it
        List<UUID> missing = new ArrayList<>();
        for (UUID m : ours.members) if (!now.members.contains(m) && c.env().online(m) != null) missing.add(m);
        if (missing.isEmpty()) { refreshMirrored(link, ours, em); return; }
        if (frozenParty(link, ours, now.members)) {
            for (UUID m : missing) enqueue(ours.leader, Op.Kind.ADD, m);
            return;
        }
        for (UUID m : missing) if (!addOne(link, ours, now.leader, m)) break;      // a refusal quiets the party; the rest retries later
        refreshMirrored(link, ours, em);
    }

    private void refreshMirrored(EmLink link, PartyManager.Party ours, EmLink.EmParty em) {
        EmLink.EmParty back = link.partyOf(em.leader);
        if (!back.real() || !Objects.equals(back.partyKey, em.partyKey)) return;
        for (UUID m : back.members) {
            PartyManager.Party mp = c.mgr().partyOf(m);
            if (mp != null && mp.id == ours.id) { markEm(m, em.partyKey); emWaiting.remove(m); }
        }
    }

    /** §3.1 materialisation: a Together party appears in EM. Returns false when a refusal stopped it early. */
    private boolean materialise(EmLink link, PartyManager.Party ours, boolean fromQueue) {
        PartyManager mgr = c.mgr();
        if (ours == null) return true;
        if (!loginMaterialize && !emLiveParties.contains(ours.id)) return true;   // §3.2: login leg off for parties the mesh never touched
        List<UUID> online = new ArrayList<>();
        for (UUID m : ours.members) if (c.env().online(m) != null) online.add(m);
        if (online.size() < PartyManager.MIN_SIZE) return true;                   // a lone online member never forms an EM party
        if (c.env().online(ours.leader) == null) return true;                      // leader in offline grace: a stable picture comes first (§11D)
        EmLink.EmParty existing = link.partyOf(ours.leader);
        if (existing == EmLink.EmParty.UNKNOWN) return false;
        if (!existing.real()) {
            // the leader may be the one missing from EM (§4 L4): another member can hold the party
            for (UUID m : ours.members) {
                EmLink.EmParty cand = link.partyOf(m);
                if (cand == EmLink.EmParty.UNKNOWN) return false;
                if (cand.real()) { existing = cand; break; }
            }
        }
        if (existing.real()) {
            for (UUID m : existing.members) if (!ours.members.contains(m)) return true;  // strangers: Case D/E owns it, never merge blindly (§3.1 step 2)
            emLiveParties.add(ours.id);
            if (frozenParty(link, ours, existing.members)) {
                if (!fromQueue) enqueue(ours.leader, Op.Kind.MATERIALIZE, null);
                return false;
            }
            return addMissing(link, ours, existing, online, fromQueue);
        }
        if (frozenParty(link, ours, List.of())) {
            if (!fromQueue) enqueue(ours.leader, Op.Kind.MATERIALIZE, null);
            return false;
        }
        EmLink.EmMirror r = link.createParty(ours.leader);
        if (r != EmLink.EmMirror.OK) { noteDriveResult(ours, ours.leader, null, r, !fromQueue); return false; }
        emLiveParties.add(ours.id);
        EmLink.EmParty made = link.partyOf(ours.leader);
        if (!made.real()) return false;                                            // read-back failed: retry next cycle, never force (§3.1 step 4)
        for (UUID m : made.members) if (!ours.members.contains(m)) return true;    // formed around strangers: hand back
        return addMissing(link, ours, made, online, fromQueue);
    }

    private boolean addMissing(EmLink link, PartyManager.Party ours, EmLink.EmParty emNow, List<UUID> online, boolean fromQueue) {
        for (UUID m : online) {
            if (emNow.members.contains(m)) { markEm(m, emNow.partyKey); continue; }
            if (!addOne(link, ours, emNow.leader, m)) return false;
            EmLink.EmParty back = link.partyOf(emNow.leader);
            if (back.real()) emNow = back;
        }
        EmLink.EmParty back = link.partyOf(emNow.leader);
        if (back.real()) for (UUID m : back.members) if (ours.members.contains(m)) { markEm(m, back.partyKey); emWaiting.remove(m); }
        return true;
    }

    /** One invite+accept drive, invited by an EM insider. False = a refusal that quiets the party (caller stops this cycle). */
    private boolean addOne(EmLink link, PartyManager.Party ours, UUID inviter, UUID m) {
        // 0.5.3: every drive back into EM funnels through here (converge, materialise, the
        // deferred queue). A settled member who stepped out at the EM door is NEVER driven
        // back in, whichever party-mate's reconcile gets here first - their leave is honoured
        // instead. (one() checks the same condition; this guard closes the race where a mate
        // is reconciled before the leaver in the same poll.)
        if (doorLeaveEndsParty && emDoorLeft(link, m, ours)) { leaveFromEmDoor(m); return true; }
        EmLink.EmMirror r = link.invite(inviter, m);
        if (r == EmLink.EmMirror.FULL) { emWaiting.add(m); fullNoteEm(m); return true; }
        if (r == EmLink.EmMirror.TARGET_IN_OTHER_PARTY) return true;              // their own Case E handles them
        if (r == EmLink.EmMirror.NO_PERMISSION || r == EmLink.EmMirror.TARGET_NO_PERMISSION) { noteDriveResult(ours, inviter, m, r, false); return true; }
        if (r != EmLink.EmMirror.OK) { noteDriveResult(ours, inviter, m, r, true); return false; }
        r = link.accept(m);
        if (r == EmLink.EmMirror.FULL) { emWaiting.add(m); fullNoteEm(m); return true; }
        if (r == EmLink.EmMirror.TARGET_IN_OTHER_PARTY) return true;
        if (r == EmLink.EmMirror.NO_PERMISSION || r == EmLink.EmMirror.TARGET_NO_PERMISSION) { noteDriveResult(ours, inviter, m, r, false); return true; }
        if (r != EmLink.EmMirror.OK) { noteDriveResult(ours, inviter, m, r, true); return false; }
        return true;
    }

    /** The removal drive for a suppressed player still shown in the EM twin (freeze-gated like any EM write). */
    private void driveRemove(EmLink link, PartyManager.Party ours, UUID m) {
        if (frozenParty(link, ours, List.of(m))) { enqueue(ours.leader, Op.Kind.REMOVE, m); return; }
        EmLink.EmMirror r = link.leave(m);
        if (r != EmLink.EmMirror.OK) noteDriveResult(ours, m, null, r, true);
    }

    /** §4 leader-reform: re-form the EM party under the leader of record (leave all + materialise). */
    private void reform(EmLink link, PartyManager.Party ours) {
        EmLink.EmParty em = link.partyOf(ours.leader);
        if (em.real() && frozenParty(link, ours, em.members)) { enqueue(ours.leader, Op.Kind.REFORM, null); return; }
        if (em.real()) for (UUID m : em.members) link.leave(m);
        materialise(link, ours, false);
    }

    // ---------- freeze, quiet window, deferred ops (DESIGN §7) ----------
    private boolean frozenParty(EmLink link, PartyManager.Party ours, Collection<UUID> alsoCheck) {
        if (ours == null) return false;
        Long until = quietUntil.get(ours.id);
        if (until != null && c.env().now() < until) return true;
        Set<UUID> who = new HashSet<>(alsoCheck);
        for (UUID m : ours.members) if (c.env().online(m) != null) who.add(m);
        return link.dungeonBusy(who);
    }

    private void noteDriveResult(PartyManager.Party ours, UUID actor, UUID target, EmLink.EmMirror r, boolean quietParty) {
        if (r == EmLink.EmMirror.NO_PERMISSION) {
            if (emPermNoted.add(actor)) c.tell(actor, "Note: EliteMobs party permissions are off for you, so the party can't appear in EliteMobs. Your Jobs bonus still works.");
        } else if (r == EmLink.EmMirror.TARGET_NO_PERMISSION && target != null) {
            if (emPermNoted.add(target)) c.tell(actor, "Note: " + c.nameOf(target) + " can't use EliteMobs parties, so they are not in the EliteMobs party. Your Jobs bonus still works.");
        }
        if (quietParty && ours != null) quietUntil.put(ours.id, c.env().now() + quietMs);   // F2: any refusal defers the party
        dbg("drive refused (" + r + ") for party of " + (ours == null ? "?" : c.nameOf(ours.leader)));
    }

    private void enqueue(UUID leader, Op.Kind kind, UUID target) {
        Deque<Op> q = deferred.computeIfAbsent(leader, k -> new ArrayDeque<>());
        for (Op o : q) if (o.kind == kind && Objects.equals(o.target, target)) return;      // already queued: keep the first timestamp
        q.addLast(new Op(kind, target, c.env().now()));
    }

    private void runDeferred(EmLink link) {
        if (deferred.isEmpty()) return;
        long now = c.env().now();
        for (UUID leader : new ArrayList<>(deferred.keySet())) {
            Deque<Op> q = deferred.get(leader);
            if (q == null) continue;
            PartyManager.Party ours = c.mgr().partyOf(leader);
            if (ours == null) { deferred.remove(leader); continue; }                        // party gone: the ops are moot
            while (!q.isEmpty() && now - q.peekFirst().at > deferMaxMs) {                   // expiry binds even while frozen (§7.1)
                q.pollFirst();
                c.logInfo("ExcelTogether mesh: dropped a deferred EliteMobs party update for " + c.nameOf(leader) + " after " + (deferMaxMs / 1000) + "s. Reconciliation is state-based, so anything still needed is retried fresh.");
            }
            if (q.isEmpty()) { deferred.remove(leader); continue; }
            if (frozenParty(link, ours, List.of())) continue;
            while (!q.isEmpty()) {
                Op op = q.peekFirst();
                if (!applyOp(link, ours, op)) break;                                        // still blocked: stop this party's queue
                q.pollFirst();
            }
            if (q.isEmpty()) deferred.remove(leader);
        }
    }

    private boolean applyOp(EmLink link, PartyManager.Party ours, Op op) {
        switch (op.kind) {
            case MATERIALIZE: return materialise(link, ours, true);
            case ADD: {
                EmLink.EmParty em = link.partyOf(ours.leader);
                if (!em.real()) return materialise(link, ours, true);
                if (em.members.contains(op.target)) return true;
                return addOne(link, ours, em.leader, op.target);
            }
            case REMOVE: {
                EmLink.EmMirror r = link.leave(op.target);
                return r == EmLink.EmMirror.OK || r == EmLink.EmMirror.ABSENT;
            }
            case REFORM: reform(link, ours); return true;
            default: return true;
        }
    }

    // ---------- the EM-door leave (0.5.3) ----------
    /**
     * True when id stepped out of the EliteMobs party alone: at the previous settled poll they
     * were mirrored into EM party K, EM now shows them in no party, and K still exists - still
     * listing a Together party-mate, under the same key. When the whole twin is gone (the EM
     * party itself ended, or everyone quit) this is false and the ordinary cases decide. A
     * player who is offline is never detected: EM drops quitters itself, and a quit is not a
     * leave - on return, materialise rebuilds their face as before.
     */
    private boolean emDoorLeft(EmLink link, UUID id, PartyManager.Party ours) {
        Object key = emMirrored.get(id);
        if (key == null || !Objects.equals(key, lastEmPartyKey.get(id))) return false;
        if (c.env().online(id) == null) return false;
        for (UUID m : ours.members) {
            if (m.equals(id)) continue;
            EmLink.EmParty twin = link.partyOf(m);
            if (twin.real() && Objects.equals(twin.partyKey, key)) return true;
        }
        return false;
    }

    /** The EM-door leave lands in the hub with the full mirror cascade, like a Together-door leave. */
    private void leaveFromEmDoor(UUID id) {
        PartyManager.Change ch = new PartyManager.Change();
        PartyManager.LeaveResult r = c.mgr().leave(id, ch);
        emMirrored.remove(id);
        if (ch.disbanded) for (UUID u : ch.remaining) emMirrored.remove(u);
        if (r == PartyManager.LeaveResult.NOT_IN_PARTY) return;
        c.tell(id, "You left the party.");
        c.announce(ch, c.nameOf(id), " left the party.");
        dbg("EM-door leave honoured for " + c.nameOf(id));
    }

    // ---------- shared helpers ----------
    private void markEm(UUID id, Object partyKey) { emMirrored.put(id, partyKey); lastEmPartyKey.put(id, partyKey); emWaiting.remove(id); emWatching.remove(id); }
    private void fullNoteTogether(UUID id) {
        if (emFullNoted.add(id)) c.tell(id, "Note: your EliteMobs party joined, but the ExcelTogether party is full (" + c.mgr().maxSize() + "/" + c.mgr().maxSize() + "), so you stay in EliteMobs only. Your Jobs bonus won't include them.");
    }
    private void fullNoteEm(UUID id) {
        if (emFullNoted.add(id)) c.tell(id, "Note: the EliteMobs party is full (" + EM_CAP + "/" + EM_CAP + "), so you are not in the EliteMobs party yet. Your Jobs bonus still works, and you join it when a seat frees.");
    }
    private void quietLeaveEm(UUID id, String selfTell, String others) {
        PartyManager.Change ch = new PartyManager.Change();
        PartyManager.LeaveResult r = c.mgr().leave(id, ch);
        emMirrored.remove(id);
        if (ch.disbanded) for (UUID u : ch.remaining) emMirrored.remove(u);
        if (r == PartyManager.LeaveResult.NOT_IN_PARTY) return;
        if (selfTell != null) c.tell(id, selfTell);
        c.announce(ch, c.nameOf(id), others, false);
    }
}

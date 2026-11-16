package com.winkreyes.exceltogether;

import java.util.*;

/**
 * Reconciles the EliteMobs party (the visible FACE) with the ExcelTogether roster of record
 * (0.6.0, P-PARTY-REBUILD-1 one-door rebuild; SCOPING.md §2.2 Door B, §2.4 layer 4, §2.5).
 *
 * The face is materialise-only: the ONLY way an EM party comes to exist is {@code materialise}
 * (the old Case C) building it from the roster of record, plus the drives that converge an
 * existing face onto the roster. The 0.5.x bidirectional adoption is deleted:
 *  - Case B (EM only -&gt; adopt into Together) is gone. An EM membership with no roster
 *    behind it is stale face state: if it answers a live hub invite it completes that invite
 *    (popup-answer matching, below); otherwise it is REVERTED - the player is removed from
 *    the EM party, told once per session that parties are run with /party (Q3), and the
 *    revert is logged every time (Q1/Q3, decision D74).
 *  - Case D convergence no longer adopts EM members into the roster; EM members who are not
 *    on the roster are reverted out of the face, and roster members missing from the face
 *    are driven in.
 *  - Case E (disjoint groups) is no longer a kept divergence: the EM membership is stale
 *    face state, cleared (subject to the freeze deferral, Q8) and re-materialised from the
 *    roster on the next poll. No poll ever parks a split-brain.
 *
 * Popup-answer matching (Door B): Together drives an EM invite for every hub invite
 * (Controller.inviteTarget), and ReflectEmLink keeps the driven-invite ledger for
 * INVITE_LEDGER_MS. When the poll sees a player who is on no roster sitting in an EM party
 * with a player whose hub invite to them is still live AND whose driven invite is still in
 * the ledger, that EM membership change is the popup answer: the hub accept completes
 * through Controller.acceptFrom, exactly as if they had typed the accept. A ledger entry
 * alone never completes anything - after a door decline the hub invite is gone, so a late
 * native EM [ACCEPT] matches nothing and is reverted (RT-4). A foreign EM pending invite is
 * never consumed (the 0.5.6 ledger fix in noteJoined is untouched).
 *
 * Driven-invite materialisation (AT-1, rig run 416): the same drive has a second face the
 * revert rule must recognise. When a PARTY-LESS inviter's hub invite is driven into EM,
 * real EM materialises the inviter's own solo EM party to hold the pending invite - the
 * inviter's EM membership is Together's own drive, not a non-door change. The revert
 * therefore spares a roster-less player whose EM party stands behind one of their own
 * live hub invites with a live driven-invite ledger entry ({@link #drivenInviteMaterialisation}):
 * BOTH must be live, exactly as popup-answer matching requires, so the spare dies with
 * the invite (accept completes the party, decline/expiry re-arms the revert on the next
 * poll). The 0.5.x desk fakes missed this because their invite drive modelled only the
 * pending slot, never the inviter's membership change.
 *
 * Native-pending spare + native-answer completion (run 426, one-roster rebuild parts 2+3,
 * SCOPING.md SS5 parts 2-3; supervisor decisions D88-D93): a native /em party invite is the
 * EM door used as a formation door into the ONE roster (Mike, Nov 16, 2026). Part 1 gave
 * the mesh leg a pending read (EmLink.hasPendingInvite, target-keyed boolean only - the
 * BP-1 probe of the EM 10.9.14 jar of record found NO inviter-returning pending accessor
 * on PartyManager/Party, so pairing is by enumeration, D88): the revert spares a
 * roster-less inviter's solo materialised EM party while exactly one unambiguous native
 * pending pair (inviter, target) stands behind it, and the spare records the pair in the
 * native-pending observation map (D91). When the target then answers natively - the
 * pending slot is consumed, so only the observation survives - the roster join completes
 * through the door path itself: the hub invite is synthesised with PartyManager.invite
 * and the UNMODIFIED Controller.acceptFrom runs (D89), tagged DOOR (D90). The completion
 * check runs AFTER the suppression hold (D92(3)); a dungeon-busy completion defers
 * through the deferred queue (D92(4)); a hub-side refusal spends the observation and the
 * formed EM party falls back to the ordinary next-poll revert (D92(2)). Crossed or
 * foreign pendings pair NOTHING and complete NOTHING (D88/D92(1)). Formed native parties
 * with no pending provenance (B1 family) and post-decline native accepts (RT-4) keep
 * reverting exactly as before: neither ever produces an observation.
 *
 * What survives from the mesh by design:
 *  - The EM-door leave (0.5.3, Mike's ruling Nov 3, 2026): a settled, mirrored member whose
 *    EM picture goes NONE while their EM twin survives without them has LEFT - the hub leave
 *    runs with the full mirror cascade (RT-1). Quits/relogs are guarded out (offline players
 *    are never detected; the login watch must settle first). The config key
 *    mesh.em.door-leave-ends-party (default true) restores view-only semantics when off:
 *    with it off, an EM-door leaver is simply re-materialised from the roster.
 *  - Suppression (roster-keyed, 0.5.3): a player Together deliberately removed is enforced
 *    out of the EM face while EM still lists them with the people they were removed from;
 *    the hold spends itself when EM shows them partyless.
 *  - The freeze machinery (DESIGN §7): no EM write lands on a dungeon-frozen party; writes
 *    queue as deferred ops (drop after 300 s, log only) and apply on unfreeze (Q8) - the
 *    revert and stale-clear paths queue through the same machinery.
 *  - The 0.5.5 accept-time EM-conflict check survives as hub behaviour in Controller: an
 *    accept from a disjoint EM party now JOINS and the stale EM membership is cleared by
 *    this sync (noteJoined / the disjoint path below), never kept and never refused.
 *
 * At rest, a poll drives nothing and says nothing.
 *
 * Login rebuild (0.6.0 U2-D4): at login the watch settles into the cases above -
 * a roster member's face is materialised, a roster-less player's EM membership
 * is the stale-face revert. The single gate in front of the revert is the
 * first-boot seed (LoginRebuild, armed only when roster.yml was absent at
 * enable): an unsettled seed attempt runs first, EM-first (Q6), and an
 * UNKNOWN face read defers the revert rather than destroying a seed source.
 */
final class EmSync {
    static final int EM_WATCH_MAX_POLLS = 30;               // about a minute at the default cadence
    private static final int EM_CAP = 5;                    // Party.MAX_MEMBERS in EliteMobs (hard constant)
    /** Q3 notice (D74): told once per player per session when a non-door EM change is reverted. */
    static final String REVERT_NOTICE = "Note: parties here are run with /party, so that EliteMobs party change was undone - it had no /party invite behind it. Use /party invite to start or join a party.";

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
    /** removed from ours on purpose; their leftover EM listing is enforced out, never reverted-as-fresh or matched */
    private final Set<UUID> emSuppressed = new HashSet<>();
    private final Map<UUID, Object> emSuppressedKey = new HashMap<>();
    /** who a suppressed player was removed from (0.5.3): the hold is roster-based, not key-based */
    private final Map<UUID, Set<UUID>> emSuppressedRoster = new HashMap<>();
    /** told once that a party was full */
    private final Set<UUID> emFullNoted = new HashSet<>();
    /** in the roster but did not fit in the EM face; retried on every poll until a seat frees */
    private final Set<UUID> emWaiting = new LinkedHashSet<>();
    /** just logged in: check until both pictures settle */
    private final Map<UUID, Integer> emWatching = new LinkedHashMap<>();
    /** told the Q3 revert notice this session (the revert itself is logged every time) */
    private final Set<UUID> revertNoted = new HashSet<>();
    /** EM partyKey seen at the previous poll, for deliberate-action detection (the EM-door leave) */
    private final Map<UUID, Object> lastEmPartyKey = new HashMap<>();
    /** told once about an EliteMobs permission refusal */
    private final Set<UUID> emPermNoted = new HashSet<>();
    /** Together party ids the mesh treats as live (interactively formed or accepted into) */
    private final Set<Long> emLiveParties = new HashSet<>();
    /** per-party ready-check quiet window (freeze F2): Together party id -> quiet until (ms) */
    private final Map<Long, Long> quietUntil = new HashMap<>();
    /** per-party deferred EM writes (freeze §7.1), keyed by leader-of-record (or by the player for roster-less reverts) */
    private final Map<UUID, Deque<Op>> deferred = new LinkedHashMap<>();
    /** Together parties already converged in this poll (party-level work runs once per party) */
    private final Set<Long> convergedThisPoll = new HashSet<>();

    private static final class Op {
        enum Kind { MATERIALIZE, ADD, REMOVE, REFORM, REVERT, NATIVE_COMPLETE }
        final Kind kind; final UUID target; final long at;
        /** NATIVE_COMPLETE only: the observed native inviter whose pair completes. */
        final UUID inviter;
        Op(Kind kind, UUID target, long at) { this(kind, target, at, null); }
        Op(Kind kind, UUID target, long at, UUID inviter) { this.kind = kind; this.target = target; this.at = at; this.inviter = inviter; }
    }

    /** Run 426 parts 2+3 (D91): a native pending pair the spare observed live. TTL = EM's own invite timeout. */
    static final long NATIVE_OBS_MS = ReflectEmLink.INVITE_LEDGER_MS;
    private static final class NativeObs {
        final UUID inviter; final long at;
        NativeObs(UUID inviter, long at) { this.inviter = inviter; this.at = at; }
    }
    /** target -> the native pending pair observed by the spare. Written ONLY by nativePendingSpare; never from any drive Together makes. */
    private final Map<UUID, NativeObs> nativePendingObs = new HashMap<>();
    /**
     * The BP-1 pairing snapshot for the current poll cycle (inviter -> target), computed
     * once at beginCycle from the cycle-start picture (D88: ambiguity is judged per poll -
     * a crossed picture pairs NOTHING this poll, even after an earlier reconcile in the
     * same poll reverts one side and the remainder would look unambiguous on its own).
     */
    private final Map<UUID, UUID> cycleNativePairs = new HashMap<>();
    private boolean cyclePairingComputed;

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
    /** Test/observability hook: this player has been told the Q3 revert notice this session. */
    boolean wasRevertNoted(UUID id) { return revertNoted.contains(id); }
    /** Test/observability hook (run 426 parts 2+3): a live native-pending observation exists for this target. */
    boolean nativeObserved(UUID target) {
        NativeObs o = nativePendingObs.get(target);
        return o != null && c.env().now() - o.at >= 0 && c.env().now() - o.at < NATIVE_OBS_MS;
    }

    /** Together removed this player on purpose (leave/kick/disband): enforce them out of the EM face. */
    void suppress(UUID id) { suppress(id, null); }

    /**
     * As {@link #suppress(UUID)}, with the roster the player was removed from (the leaver plus
     * everyone who was in their party at the removal, from the PartyManager.Change). The roster
     * is what the hold compares against (0.5.3): an EM party re-created under a new partyKey
     * around the same people must not defeat the hold, while a group containing anyone new is
     * a fresh native group and falls through to the matching/revert rules. A null/empty hint
     * falls back to the current EM picture.
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
    void beginCycle() {
        convergedThisPoll.clear();
        cycleNativePairs.clear();
        cyclePairingComputed = false;
        computeNativePairingSnapshot();
    }

    /**
     * BP-1 pairing by enumeration, snapshot form (D88): from the cycle-start picture,
     * pair (inviter, target) only when exactly ONE roster-less player holds a solo
     * materialised EM party AND exactly one online roster-less target holds a native
     * pending invite - no driven-invite ledger entry from anyone (a driven pending is
     * never native), and no live hub invite (a hub invite beside a native pending is a
     * crossed invite, D92(1)). Any ambiguity pairs NOTHING for the whole cycle.
     */
    private void computeNativePairingSnapshot() {
        cyclePairingComputed = true;
        try {
            EmLink link = c.emLink();
            if (link == null || !link.available()) return;
            UUID soloInviter = null;
            int soloInviters = 0;
            for (EmLink.EmParty p : link.allParties()) {
                if (p == null || !p.real() || p.members.size() != 1) continue;
                UUID only = p.members.get(0);
                if (c.mgr().partyOf(only) != null) continue;
                soloInviters++;
                soloInviter = only;
            }
            if (soloInviters != 1 || soloInviter == null) return;
            List<UUID> universe = onlineUniverse(soloInviter);
            long now = c.env().now();
            UUID found = null;
            int targets = 0;
            for (UUID t : universe) {
                if (t.equals(soloInviter)) continue;
                if (c.mgr().partyOf(t) != null) continue;
                if (!link.hasPendingInvite(t)) continue;
                if (anyLedgerEntry(link, universe, t)) continue;                    // driven, not native
                if (!c.mgr().invitesFor(t, now).isEmpty()) continue;                // crossed with a hub invite
                targets++;
                found = t;
            }
            if (targets == 1) cycleNativePairs.put(soloInviter, found);
        } catch (RuntimeException ignored) { }
    }

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
        if (!em.real()) {
            // The inviter has no EM party yet: the poll materialises the face. But a TARGET
            // still sitting in a disjoint EM party is stale face state (0.6.0): the door accept
            // joined the roster of record, so the stale membership is cleared now (freeze-gated)
            // instead of being noted and kept as the 0.5.x Case E did. A target with no EM
            // party, a solo EM party, or one made only of Together party-mates is the benign
            // picture and still returns quietly: materialise converges it.
            EmLink.EmParty theirs = link.partyOf(target);
            if (theirs.real() && ours != null && !theirs.members.contains(inviter) && hasStranger(theirs, ours, target)) {
                clearStaleFace(link, ours, target, theirs);
            }
            return;
        }
        if (em.members.contains(target)) { markEm(target, em.partyKey); markEm(inviter, em.partyKey); return; }
        if (frozenParty(link, ours, em.members)) { enqueue(ours.leader, Op.Kind.ADD, target); return; }
        if (!link.inviteDriven(inviter, target)) {
            EmLink.EmMirror r = link.invite(inviter, target);
            if (r == EmLink.EmMirror.FOREIGN_PENDING) {
                // 0.5.6 (D4 ledger fix, kept verbatim): the target's pending EliteMobs invite
                // belongs to ANOTHER party and EM kept that slot untouched. Driving accept()
                // here is the mis-join itself - the blind accept consumes the foreign invite
                // and lands the target in the wrong EM party (R3e-prime, reproduced on the EM
                // rig). Refuse the EM drive instead: the Together join stands, both sides hear
                // why, and the party's quiet window arms like any other refusal. (When
                // inviteDriven() was true the pending slot is our own driven invite, so the
                // accept below is the genuine re-drive and stays untouched.)
                c.tell(target, "Note: you joined the ExcelTogether party, but the EliteMobs side was left untouched on purpose: an EliteMobs party invite from another party is still waiting for you. If you want that party instead, answer its EliteMobs invite (the invite buttons, or /em party).");
                c.tell(inviter, "Note: " + c.nameOf(target) + " joined your ExcelTogether party, but the EliteMobs side did not link: they still have another party's EliteMobs invite waiting, and it stays theirs until they answer it in EliteMobs.");
                dbg("foreign pending refused the blind accept for " + c.nameOf(target) + " into party of " + c.nameOf(inviter));
                noteDriveResult(ours, inviter, target, r, true); return;
            }
            if (r == EmLink.EmMirror.TARGET_IN_OTHER_PARTY) {
                // 0.6.0: the target sits in a disjoint EM party (the door accept already joined
                // the roster). Clear the stale membership (freeze-gated); the poll materialises
                // the roster face. Never a kept divergence, never a blind retry this cycle.
                EmLink.EmParty theirs = link.partyOf(target);
                if (theirs.real() && ours != null) clearStaleFace(link, ours, target, theirs);
                return;
            }
            if (r != EmLink.EmMirror.OK) {
                noteDriveResult(ours, inviter, target, r, true); return;
            }
        }
        EmLink.EmMirror r = link.accept(target);
        EmLink.EmParty back = link.partyOf(inviter);
        if (back.real() && back.members.contains(target)) { markEm(target, back.partyKey); markEm(inviter, back.partyKey); }
        else if (r != EmLink.EmMirror.OK) noteDriveResult(ours, inviter, target, r, true);
        else if (back != EmLink.EmParty.UNKNOWN) {
            // 0.5.5 (D4 read-back guard, kept): the accept claimed OK but the read-back does not
            // show the target in the inviter's EM party (a crossed pending invite is the known
            // way to land here: the blind accept joins the OTHER party). Say so with the reason,
            // and arm the party's quiet window like any other refusal. An UNKNOWN read-back is
            // not proof of divergence (the poll retries it), so it stays on the quiet path.
            c.tell(target, "Note: EliteMobs said the join worked, but you are not in " + c.nameOf(inviter) + "'s EliteMobs party, so the EliteMobs side did not line up. You are still in their ExcelTogether party and your Jobs bonus works.");
            if (ours != null) quietUntil.put(ours.id, c.env().now() + quietMs);
            dbg("accept read-back diverged for " + c.nameOf(target) + " into party of " + c.nameOf(inviter));
        }
    }

    /** Controller drove a Together-originated EM leave for this party; remember it as live-touched. */
    void noteLiveParty(PartyManager.Party pt) { if (pt != null) emLiveParties.add(pt.id); }

    /** True when em holds anyone besides self who is not on the roster party ours. */
    private boolean hasStranger(EmLink.EmParty em, PartyManager.Party ours, UUID self) {
        for (UUID m : em.members) if (!m.equals(self) && !ours.members.contains(m)) return true;
        return false;
    }

    // ---------- the reconcile cases (0.6.0: materialise, match, revert - never adopt) ----------
    private void one(EmLink link, UUID id) {
        EmLink.EmParty em = link.partyOf(id);
        if (em == EmLink.EmParty.UNKNOWN) {
            Integer n = emWatching.get(id);
            if (n != null) { if (n + 1 > EM_WATCH_MAX_POLLS) emWatching.remove(id); else emWatching.put(id, n + 1); }
            return;
        }
        Object key = em.real() ? em.partyKey : null;
        PartyManager mgr = c.mgr();
        PartyManager.Party ours = mgr.partyOf(id);
        boolean wasWatching = emWatching.containsKey(id);

        // Popup-answer matching runs before the suppression hold and the cases: a suppressed
        // player who was re-invited at the door and answered in EM must still complete (their
        // accept clears the suppression inside Controller.acceptFrom). A match needs ALL of:
        // no roster yet, the EM party contains the inviter, a LIVE hub invite, and a live
        // driven-invite ledger entry. Anything less falls through to the revert rules.
        if (ours == null && em.real() && popupAnswer(link, id, em)) {
            lastEmPartyKey.put(id, key);
            return;
        }

        if (emSuppressed.contains(id)) {
            if (!em.real()) {
                clear(id);                            // EM agrees they are out: the suppression is spent
            } else if (suppressionApplies(id, em)) {
                // Still grouped only with the people they were removed from: enforce the
                // removal on the face (the Controller leave drive may not have landed).
                if (link.dungeonBusy(em.members)) enqueue(id, Op.Kind.REVERT, id);
                else link.leave(id);
                lastEmPartyKey.put(id, key);
                return;
            } else {
                clear(id);                            // their EM party includes someone new: a fresh native group, proceed to the cases
            }
        }
        // 0.6.0 (U2-D4): the first-boot seed gate. While this player's one seed
        // attempt is outstanding, their EM party may be its EM-first source -
        // run the attempt now and, if a face read is still UNKNOWN, wait for a
        // later poll instead of reverting a possible seed source. Once settled,
        // the ordinary cases below decide (seeded -> converge/materialise,
        // unseeded -> the stale-face revert).
        if (ours == null && em.real() && c.seedArmed() && !c.seedSettled(id)) {
            c.seedLogin(id);
            if (!c.seedSettled(id)) { lastEmPartyKey.put(id, key); return; }
        }
        ours = mgr.partyOf(id);
        // Run 426 part 3 (D92(3)): the native-answer completion runs AFTER the suppression
        // hold above - a suppressed player must not re-enter via a native pending, and a
        // native pending carries no door re-invite that could clear a suppression the way
        // popupAnswer's door accept does, so there is no popupAnswer-style exception here.
        if (ours == null && em.real() && nativeAnswerCompletion(link, id, em)) {
            lastEmPartyKey.put(id, key);
            return;
        }
        ours = mgr.partyOf(id);
        if (em.real() || ours == null) emWatching.remove(id);   // a settled picture ends the watch; the materialise case keeps it until materialise marks them (markEm clears it), so a relog is never mistaken for an EM-door leave
        if (emWaiting.contains(id) && c.env().online(id) == null) emWaiting.remove(id);

        // 0.5.3 (kept): a settled member who stepped out at the EM door has left the party (see
        // the class header). Runs before the cases: materialise would otherwise rebuild them in.
        if (doorLeaveEndsParty && !em.real() && ours != null && !wasWatching && emDoorLeft(link, id, ours)) {
            leaveFromEmDoor(id);
            lastEmPartyKey.put(id, null);
            return;
        }

        if (!em.real() && ours == null) {             // both agree: partyless
            emMirrored.remove(id); emFullNoted.remove(id); emWaiting.remove(id);
            lastEmPartyKey.put(id, null);
            return;
        }
        if (em.real() && ours == null) {              // EM only, no popup match: stale face state - revert (was Case B adoption)
            // AT-1 (rig run 416): unless this EM party is the inviter's own driven-invite
            // materialisation - Together's drive created it to hold a live pending invite,
            // and reverting it would kill that invite (and the one-tap popup) with it.
            if (drivenInviteMaterialisation(link, id, em)) {
                dbg("driven-invite materialisation spared for " + c.nameOf(id) + " (a /party invite they sent is still waiting)");
                lastEmPartyKey.put(id, key);
                return;
            }
            // Run 426 part 2 (D88): a native /em party invite behind this solo
            // materialisation spares it while the pending invite lives - and records the
            // pair the part-3 completion will consume (D91).
            if (nativePendingSpare(link, id, em)) {
                dbg("native-pending materialisation spared for " + c.nameOf(id) + " (a native /em party invite they sent is still waiting)");
                lastEmPartyKey.put(id, key);
                return;
            }
            // Run 426 part 3, inviter side: the target already answered natively and sits
            // in this party; the completion runs on the TARGET's reconcile (this poll or
            // the next), so the inviter is spared until it lands or the observation dies.
            if (nativeCompletionPending(link, id, em)) {
                dbg("native-answer completion pending spared for " + c.nameOf(id) + " (a native answer is completing through the door)");
                lastEmPartyKey.put(id, key);
                return;
            }
            revertPlayer(link, id, em);
            lastEmPartyKey.put(id, key);
            return;
        }
        if (!em.real()) {                             // roster only: materialise the face (the only EM-party constructor)
            materialise(link, ours, false);
            lastEmPartyKey.put(id, null);
            return;
        }
        // both real
        int shared = 0;
        for (UUID m : em.members) if (ours.members.contains(m)) shared++;
        if (shared >= 2 || !hasStranger(em, ours, id)) {
            // The same party, modulo drift: converge at party level, once per poll. An EM
            // party holding only roster members (shared < 2 means id alone - the solo remnant
            // left when a mate stepped out at the EM door) also converges: driving the missing
            // mates in goes through addOne, whose door-leave guard honours a mate's EM-door
            // leave instead of resurrecting them.
            if (convergedThisPoll.add(ours.id)) converge(link, ours, em);
        } else {                                      // disjoint groups (id plus strangers): stale face - clear it, re-materialise next poll
            revertEmMember(link, ours, id, em.members);
        }
        lastEmPartyKey.put(id, key);
    }

    /**
     * Door B popup-answer matching: id sits in an EM party with a live hub inviter whose driven
     * EM invite is still in the ledger - the EM membership change IS their answer to the hub
     * invite, so the hub accept completes through the door path (Controller.acceptFrom, DOOR
     * origin, the accept-time checks included). Returns true when a match completed (or was
     * attempted through the door and spoken for); false when nothing matches.
     */
    private boolean popupAnswer(EmLink link, UUID id, EmLink.EmParty em) {
        PlayerView v = c.env().online(id);
        if (v == null) return false;
        for (PartyManager.Invite inv : c.mgr().invitesFor(id, c.env().now())) {
            if (!em.members.contains(inv.inviter)) continue;
            if (!link.inviteDriven(inv.inviter, id)) continue;
            dbg("popup answer matched for " + c.nameOf(id) + " to the hub invite from " + c.nameOf(inv.inviter));
            c.acceptFrom(v, inv.inviter);
            return true;
        }
        return false;
    }

    /**
     * AT-1 driven-invite materialisation: id is a party-less inviter whose driven EM invite
     * made EM materialise their (solo) party to hold the pending invite. True only while
     * BOTH halves of the drive are still live for the same (inviter, target) pair - a live
     * hub invite sent by id ({@code PartyManager.invitesFrom}) AND a live driven-invite
     * ledger entry ({@code EmLink.inviteDriven}) - mirroring popup-answer matching's
     * discipline: the ledger alone spares nothing (a native EM party formed while an old
     * ledger entry lingers, with no hub invite behind it, still reverts), and a hub invite
     * whose EM drive never landed (no ledger entry) spares nothing either. Any other member
     * of the materialised party is judged by their own reconcile, so a stranger who joins
     * it natively is still reverted out of it individually.
     */
    private boolean drivenInviteMaterialisation(EmLink link, UUID id, EmLink.EmParty em) {
        if (em == null || !em.real()) return false;
        long now = c.env().now();
        for (PartyManager.Invite inv : c.mgr().invitesFrom(id, now)) {
            if (link.inviteDriven(id, inv.target)) return true;
        }
        return false;
    }

    // ---------- run 426 parts 2+3: the native-pending spare + native-answer completion (D88-D93) ----------

    /** The online universe a pairing enumeration can see: id plus everyone visible to id. */
    private List<UUID> onlineUniverse(UUID id) {
        List<UUID> out = new ArrayList<>();
        out.add(id);
        PlayerView v = c.env().online(id);
        if (v != null) {
            try { for (PlayerView o : c.env().visibleOthers(v)) if (o != null && !out.contains(o.id())) out.add(o.id()); }
            catch (Throwable ignored) { }
        }
        return out;
    }

    /** True when target holds a driven-invite ledger entry from ANYONE in the universe (a driven pending is never a native one). */
    private boolean anyLedgerEntry(EmLink link, List<UUID> universe, UUID target) {
        for (UUID u : universe) if (!u.equals(target) && link.inviteDriven(u, target)) return true;
        return false;
    }

    /**
     * BP-1 pairing by enumeration (D88 step 2 - the BP-1 probe found no inviter-returning
     * pending accessor on the EM 10.9.14 jar of record, only the target-keyed boolean
     * Part 1 resolved). The pairing itself is the cycle snapshot (see
     * {@link #computeNativePairingSnapshot}); this evaluation-time half only re-checks
     * that inviter's own picture still matches it - solo materialisation, roster-less,
     * and the paired target's pending invite still live right now. Returns the paired
     * target, or null.
     */
    private UUID nativePendingTargetFor(EmLink link, UUID inviter, EmLink.EmParty em) {
        if (em == null || !em.real()) return null;
        if (em.members.size() != 1 || !em.members.contains(inviter)) return null;   // solo materialisation only
        if (c.mgr().partyOf(inviter) != null) return null;
        if (!cyclePairingComputed) computeNativePairingSnapshot();                  // a reconcile outside beginCycle: judge from now
        UUID target = cycleNativePairs.get(inviter);
        if (target == null) return null;
        return link.hasPendingInvite(target) ? target : null;
    }

    /**
     * Part 2, the native-pending spare (D88/D91): inviter's solo materialisation stands
     * behind one unambiguous live native pending pair. Firing the spare is the ONLY write
     * the observation map ever gets - it records the live pair (inviter, target, now) the
     * part-3 completion consumes after EM's pending slot itself is gone.
     */
    private boolean nativePendingSpare(EmLink link, UUID id, EmLink.EmParty em) {
        UUID target = nativePendingTargetFor(link, id, em);
        if (target == null) return false;
        nativePendingObs.put(target, new NativeObs(id, c.env().now()));
        return true;
    }

    /** A live observation naming exactly this (inviter, target) pair, or null (expired entries are pruned on read). */
    private NativeObs nativeObsFor(UUID target, UUID inviter, long now) {
        NativeObs o = nativePendingObs.get(target);
        if (o == null) return null;
        if (now - o.at < 0 || now - o.at >= NATIVE_OBS_MS) { nativePendingObs.remove(target); return null; }
        return o.inviter.equals(inviter) ? o : null;
    }

    /**
     * Part 3, inviter side: a live observation names id as the inviter of a target who is
     * already inside id's EM party - the native answer landed and the completion runs on
     * the target's reconcile. The inviter is spared until then; when the observation dies
     * unconsumed, the ordinary revert decides (D92(2) fallback shape).
     */
    private boolean nativeCompletionPending(EmLink link, UUID id, EmLink.EmParty em) {
        if (em == null || !em.real() || em.members.size() < 2) return false;
        long now = c.env().now();
        for (UUID m : em.members) {
            if (m.equals(id)) continue;
            if (c.mgr().partyOf(m) != null) continue;
            if (nativeObsFor(m, id, now) != null) return true;
        }
        return false;
    }

    /**
     * Part 3, the native-answer completion (D89/D90/D92): id is roster-less and sits in an
     * EM party with the inviter a live observation names for id - id answered a native
     * /em party invite, and EM has already consumed the pending slot, so the observation
     * is the only provenance left. Completes through the door path (see
     * {@link #driveNativeCompletion}). Returns true when the completion was attempted,
     * deferred, or spoken for; false when no live observation traces this membership -
     * formed native parties with no pending provenance (B1 family) and post-decline
     * native accepts (RT-4) return false here and keep reverting. A live hub invite to
     * id from anyone makes the picture crossed: nothing completes blindly (D92(1)).
     * A dungeon-busy party defers the completion through the deferred queue (D92(4)).
     */
    private boolean nativeAnswerCompletion(EmLink link, UUID id, EmLink.EmParty em) {
        NativeObs any = nativePendingObs.get(id);
        if (any == null) return false;
        long now = c.env().now();
        if (now - any.at < 0 || now - any.at >= NATIVE_OBS_MS) { nativePendingObs.remove(id); return false; }
        UUID inviter = any.inviter;
        if (inviter.equals(id) || !em.members.contains(inviter)) return false;
        if (!c.mgr().invitesFor(id, now).isEmpty()) return false;                   // crossed: a hub invite is also live
        if (link.dungeonBusy(em.members)) {
            enqueueNativeComplete(id, inviter);
            dbg("native-answer completion deferred (dungeon-busy) for " + c.nameOf(id) + " into party of " + c.nameOf(inviter));
            return true;
        }
        driveNativeCompletion(id, inviter);
        return true;
    }

    /**
     * The completion drive itself (D89): synthesise the hub invite through the roster's
     * own verb, then run the UNMODIFIED Controller.acceptFrom path - every accept-time
     * check and wording (PARTY_FULL, INVITER_OFFLINE, crossed-invite notices, suppression
     * clearing, the mcMMO one-way mirror write, EmSync.noteJoined) is the door's own, and
     * the roster origin lands as DOOR (D90). The observation is spent by the attempt
     * either way: if the hub side refuses, the formed EM party falls back to the ordinary
     * revert on the next poll (D92(2), the B1 shape) instead of retrying until TTL.
     * PARTY_FULL can already refuse at the synthesise step (the inviter's roster party
     * filled since the spare) - acceptFrom can never see that invite, so its exact
     * PARTY_FULL wordings are spoken here and the attempt ends the same way.
     */
    private void driveNativeCompletion(UUID target, UUID inviter) {
        PlayerView v = c.env().online(target);
        if (v == null) { nativePendingObs.remove(target); return; }
        PartyManager.InviteResult ir = c.mgr().invite(inviter, target, c.env().now());
        if (ir == PartyManager.InviteResult.PARTY_FULL) {
            c.tell(target, "That party filled up (" + c.mgr().maxSize() + "/" + c.mgr().maxSize() + "). Ask them to make room.");
            c.tell(inviter, c.nameOf(target) + " tried to join but your party is full.");
            nativePendingObs.remove(target);
            dbg("native-answer completion refused (party full) for " + c.nameOf(target) + " into party of " + c.nameOf(inviter));
            return;
        }
        if (ir != PartyManager.InviteResult.OK) { nativePendingObs.remove(target); return; }
        c.acceptFrom(v, inviter);
        nativePendingObs.remove(target);
        dbg("native-answer completion ran the door accept for " + c.nameOf(target) + " into party of " + c.nameOf(inviter));
    }

    /** Queue a dungeon-deferred native completion under the target's own roster-less queue (D92(4)); expiry rides the queue's deferMaxMs. */
    private void enqueueNativeComplete(UUID target, UUID inviter) {
        Deque<Op> q = deferred.computeIfAbsent(target, k -> new ArrayDeque<>());
        for (Op o : q) if (o.kind == Op.Kind.NATIVE_COMPLETE && target.equals(o.target)) return;
        q.addLast(new Op(Op.Kind.NATIVE_COMPLETE, target, c.env().now(), inviter));
    }

    /**
     * Q1/Q3 revert for a player on no roster whose EM membership matches no live hub invite:
     * tell them once per session, log every time, and remove them from the EM party. A frozen
     * (dungeon-busy) EM party defers the removal through the deferred queue (Q8). Each member
     * of a native EM party is reverted by their own reconcile, so the party dismantles within
     * one poll without any roster ever being created from it.
     */
    private void revertPlayer(EmLink link, UUID id, EmLink.EmParty em) {
        if (revertNoted.add(id)) c.tell(id, REVERT_NOTICE);
        c.logInfo("ExcelTogether mesh: reverted a non-door EliteMobs party change for " + c.nameOf(id) + " (no matching /party invite).");
        if (link.dungeonBusy(em.members)) { enqueue(id, Op.Kind.REVERT, id); return; }
        EmLink.EmMirror r = link.leave(id);
        if (r == EmLink.EmMirror.OK || r == EmLink.EmMirror.ABSENT) { emMirrored.remove(id); emWaiting.remove(id); }
        else dbg("revert leave refused (" + r + ") for " + c.nameOf(id) + "; state-based retry next poll");
    }

    /**
     * Revert one stale EM membership for a player who IS on the roster (a disjoint EM grouping,
     * or an extra member of a converged party's face): same Q3 notice/log discipline as
     * {@link #revertPlayer}, with the removal freeze-gated through the party's deferred queue.
     */
    private void revertEmMember(EmLink link, PartyManager.Party ours, UUID m, Collection<UUID> emMembers) {
        if (revertNoted.add(m)) c.tell(m, REVERT_NOTICE);
        c.logInfo("ExcelTogether mesh: reverted a non-door EliteMobs party change for " + c.nameOf(m) + " (no matching /party invite).");
        if (frozenParty(link, ours, emMembers)) { enqueue(ours.leader, Op.Kind.REMOVE, m); return; }
        EmLink.EmMirror r = link.leave(m);
        if (r != EmLink.EmMirror.OK && r != EmLink.EmMirror.ABSENT) noteDriveResult(ours, m, null, r, true);
    }

    /** Clear a door-accepter's stale disjoint EM membership (freeze-gated like any EM write). */
    private void clearStaleFace(EmLink link, PartyManager.Party ours, UUID target, EmLink.EmParty theirs) {
        if (frozenParty(link, ours, theirs.members)) { enqueue(ours.leader, Op.Kind.REMOVE, target); return; }
        EmLink.EmMirror r = link.leave(target);
        if (r != EmLink.EmMirror.OK && r != EmLink.EmMirror.ABSENT) noteDriveResult(ours, target, null, r, true);
        dbg("cleared stale EliteMobs membership for " + c.nameOf(target));
    }

    /** Both hold the same party (a shared core of 2+). Converge at party level, once per poll. Never adopts. */
    private void converge(EmLink link, PartyManager.Party ours, EmLink.EmParty em) {
        PartyManager mgr = c.mgr();
        // 1) EM members who are not on the roster are stale face state: revert them out of the
        //    face. (0.5.x adopted them into the roster here; that path is deleted.) A member of
        //    a DIFFERENT roster party is left for their own reconcile's disjoint path - tearing
        //    a roster party's face from here would fight it.
        for (UUID m : new ArrayList<>(em.members)) {
            PartyManager.Party mp = mgr.partyOf(m);
            if (mp != null) { if (mp.id == ours.id) markEm(m, em.partyKey); continue; }
            // An extra who is ANSWERING this party's live hub invite at the EM popup is not
            // stale: complete their accept through the door path instead of reverting them
            // (their own reconcile would match it too, but the poll reaches this party first).
            if (popupAnswer(link, m, em)) { ours = mgr.partyOf(ours.leader); if (ours == null) return; continue; }
            if (suppressionApplies(m, em)) { driveRemove(link, ours, m); continue; }   // a deliberate removal the EM leg still shows: enforce it
            revertEmMember(link, ours, m, em.members);
        }
        ours = mgr.partyOf(ours.leader);
        if (ours == null) return;
        emLiveParties.add(ours.id);
        // 2) leader parity (§4): best-effort; reform only when the config asks for it
        if (em.leader != null && !em.leader.equals(ours.leader) && leaderReform) { reform(link, ours); return; }
        // 3) Roster members missing from EM are driven in (freeze-gated). The inviter is the
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

    /** §3.1 materialisation: a roster party appears in EM. Returns false when a refusal stopped it early. */
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
            for (UUID m : existing.members) if (!ours.members.contains(m)) return true;  // strangers: the disjoint/converge paths own it, never merge blindly (§3.1 step 2)
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
        // 0.5.3 (kept): every drive back into EM funnels through here (converge, materialise,
        // the deferred queue). A settled member who stepped out at the EM door is NEVER driven
        // back in, whichever party-mate's reconcile gets here first - their leave is honoured
        // instead. (one() checks the same condition; this guard closes the race where a mate
        // is reconciled before the leaver in the same poll.)
        if (doorLeaveEndsParty && emDoorLeft(link, m, ours)) { leaveFromEmDoor(m); return true; }
        EmLink.EmMirror r = link.invite(inviter, m);
        // 0.5.6 (D4, kept): a FOREIGN_PENDING invite result lands in the generic refusal arm
        // below (noteDriveResult + stop this cycle) - accept() is never driven over it here.
        if (r == EmLink.EmMirror.FULL) { emWaiting.add(m); fullNoteEm(m); return true; }
        if (r == EmLink.EmMirror.TARGET_IN_OTHER_PARTY) return true;              // their own disjoint path clears the stale face
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
            while (!q.isEmpty() && now - q.peekFirst().at > deferMaxMs) {                   // expiry binds even while frozen (§7.1)
                q.pollFirst();
                c.logInfo("ExcelTogether mesh: dropped a deferred EliteMobs party update for " + c.nameOf(leader) + " after " + (deferMaxMs / 1000) + "s. Reconciliation is state-based, so anything still needed is retried fresh.");
            }
            if (q.isEmpty()) { deferred.remove(leader); continue; }
            PartyManager.Party ours = c.mgr().partyOf(leader);
            if (ours == null) {
                // Roster-less queue (a revert keyed by the player themself, Q8): only REVERT
                // ops live here - any other op is moot without a party. A revert applies as
                // soon as the player's current EM party is not dungeon-busy.
                while (!q.isEmpty()) {
                    Op op = q.peekFirst();
                    // Run 426 part 3 (D92(4)): a deferred native completion applies as soon
                    // as the party unfreezes, while its observation still lives and the
                    // pair still stands in EM together; a dead observation or a dissolved
                    // pair drops the op and the poll re-decides from state.
                    if (op.kind == Op.Kind.NATIVE_COMPLETE) {
                        EmLink.EmParty cur = link.partyOf(op.target);
                        if (!cur.real() || op.inviter == null || !cur.members.contains(op.inviter)
                                || nativeObsFor(op.target, op.inviter, now) == null
                                || c.mgr().partyOf(op.target) != null) { q.pollFirst(); continue; }
                        if (link.dungeonBusy(cur.members)) break;                          // still frozen: stop this queue
                        if (!c.mgr().invitesFor(op.target, now).isEmpty()) { q.pollFirst(); continue; }   // crossed meanwhile: never complete blindly
                        driveNativeCompletion(op.target, op.inviter);
                        q.pollFirst();
                        continue;
                    }
                    if (op.kind != Op.Kind.REVERT) { q.pollFirst(); continue; }
                    EmLink.EmParty cur = link.partyOf(op.target);
                    if (cur.real() && link.dungeonBusy(cur.members)) break;                 // still frozen: stop this queue
                    // AT-1: a revert queued while frozen must not land on a driven-invite
                    // materialisation whose invite is still live - drop the op; the poll
                    // re-decides from state if the invite dies first.
                    if (cur.real() && drivenInviteMaterialisation(link, op.target, cur)) { q.pollFirst(); continue; }
                    // Run 426 parts 2+3 twin guard: the same discipline for a native-pending
                    // materialisation (the spare re-decides) and for a pair whose native
                    // answer is completing through the door (the completion decides).
                    if (cur.real() && nativePendingSpare(link, op.target, cur)) { q.pollFirst(); continue; }
                    if (cur.real() && nativeCompletionPending(link, op.target, cur)) { q.pollFirst(); continue; }
                    if (cur.real()) {
                        EmLink.EmMirror r = link.leave(op.target);
                        if (r != EmLink.EmMirror.OK && r != EmLink.EmMirror.ABSENT) break;
                    }
                    q.pollFirst();
                }
                if (q.isEmpty()) deferred.remove(leader);
                continue;
            }
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
            case REVERT: {
                EmLink.EmMirror r = link.leave(op.target);
                return r == EmLink.EmMirror.OK || r == EmLink.EmMirror.ABSENT;
            }
            case REFORM: reform(link, ours); return true;
            default: return true;
        }
    }

    // ---------- the EM-door leave (0.5.3, kept) ----------
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
        // The definition requires EM to show them in NO party right now (0.6.0): a player who
        // left the twin AND joined another EM party in the same gap is a stale-face case for
        // their own reconcile, not a door leave - the mirrored key alone cannot tell the two
        // apart, and guessing "leave" here would hub-leave a player who never left.
        EmLink.EmParty cur = link.partyOf(id);
        if (cur == EmLink.EmParty.UNKNOWN || cur.real()) return false;
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
    private void fullNoteEm(UUID id) {
        if (emFullNoted.add(id)) c.tell(id, "Note: the EliteMobs party is full (" + EM_CAP + "/" + EM_CAP + "), so you are not in the EliteMobs party yet. Your Jobs bonus still works, and you join it when a seat frees.");
    }
}

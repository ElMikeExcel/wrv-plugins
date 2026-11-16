package com.winkreyes.exceltogether;

import java.util.*;

/**
 * Login rebuild formalisation (0.6.0, P-PARTY-REBUILD-1 unit 2 / U2-D4;
 * SCOPING.md §2.5, D74/Q6). Owns the two login-time rules that are neither
 * door acts nor sync drift repair:
 *
 *  1. THE FIRST-BOOT SEED. When the plugin enabled with NO roster.yml
 *     (RosterStore.Loaded.filePresent == false), the roster of record starts
 *     empty and the parties players are standing in at swap time exist only
 *     on the faces. The seed is a one-shot LOGIN_REBUILD-origin adoption pass
 *     over those faces, EM-FIRST (Q6): a player's EliteMobs party seeds
 *     before their mcMMO party is even considered, and when the two faces
 *     disagree about a player the EM grouping wins and the conflict is
 *     logged. It is armed per enable by the plugin (setArmed) and each
 *     player is seed-processed AT MOST ONCE per enable - a player whose
 *     first settled login found nothing (or whose group was seeded) is never
 *     seed-processed again this enable; later logins follow the ordinary
 *     rules. The moment a seed adopts, PartyManager's write-through listener
 *     creates roster.yml and the store governs from then on; on the next
 *     enable the file is present, the seed is not armed, and this pass never
 *     runs again. An existing-but-empty roster file is a stored state, NOT
 *     a seed trigger (Unit 1's distinction, RosterStoreTest-pinned).
 *
 *  2. (In the syncs, gated by this class's state.) STALE-FACE CLEARING: with
 *     the store governing (seed not armed, or this player already
 *     seed-processed), a player on no roster who is still listed in an EM or
 *     mcMMO party at login is CLEARED from the faces, never adopted - EmSync
 *     reverts the EM side on its poll, McmmoSync's login repair clears the
 *     mcMMO side; both consult {@link #settled} first so a face that might
 *     still be this player's seed source is never torn down before the seed
 *     has had its settled chance to read it.
 *
 * A face read of UNKNOWN (profile not loaded yet) never settles the player:
 * the attempt simply does not count, and the syncs' login watches retry it
 * on later polls. Seeding needs a group of at least PartyManager.MIN_SIZE -
 * a solo face listing is not a party and seeds nothing (it is stale face
 * state for the ordinary rules to clear). Offline members of a face party
 * seed with their group: PartyManager.adopt has no online gate, and the
 * faces report their full membership for offline players too.
 */
final class LoginRebuild {
    private final Controller c;
    /** Armed for this enable only when roster.yml was absent at enable (first boot). */
    private boolean armed;
    /** Players whose one seed attempt has settled this enable. */
    private final Set<UUID> processed = new HashSet<>();

    LoginRebuild(Controller c) { this.c = c; }

    void setArmed(boolean v) { armed = v; }
    boolean armed() { return armed; }

    /**
     * True when no seed decision is outstanding for this player: the seed is
     * not armed, or their one attempt already settled, or they are on the
     * roster of record (seeded, restored, or door-joined - nothing may be
     * adopted for them either way). The syncs' login paths must not clear a
     * face for a player who is not settled: it may be their seed source.
     */
    boolean settled(UUID id) {
        return !armed || processed.contains(id) || c.mgr().inParty(id);
    }

    /** Login (or a sync login path retrying): run this player's one seed attempt if it is due. */
    void onLogin(UUID id) {
        if (id == null || settled(id)) return;
        attempt(id);
    }

    /**
     * One seed attempt. EM first (Q6): the EM picture is read and, if it
     * holds a group, seeded before mcMMO is consulted at all. An UNKNOWN
     * read on the face that would decide leaves the player unsettled - the
     * attempt does not count and a later login-path retry decides.
     */
    private void attempt(UUID id) {
        PartyManager mgr = c.mgr();
        EmLink.EmParty em = EmLink.EmParty.NONE;
        if (c.emLink().available()) {
            try { em = c.emLink().partyOf(id); } catch (Throwable t) { em = EmLink.EmParty.UNKNOWN; }
            if (em == null || em == EmLink.EmParty.UNKNOWN) return;          // EM unreadable: retry later, never guess EM-first on a blind read
        }
        if (em.real() && em.members.size() >= PartyManager.MIN_SIZE) {
            logEmMcConflict(id, em);
            seedGroup(em.members, em.leader, "EliteMobs");
            processed.add(id);
            return;
        }
        McmmoLink.McParty mc = McmmoLink.McParty.NONE;
        if (c.link().available()) {
            try { mc = c.link().partyOf(id); } catch (Throwable t) { mc = McmmoLink.McParty.UNKNOWN; }
            if (mc == null || mc == McmmoLink.McParty.UNKNOWN) return;       // mcMMO unreadable: a group may still be standing there
        }
        if (mc.real() && mc.members.size() >= PartyManager.MIN_SIZE) {
            seedGroup(new ArrayList<>(mc.members.keySet()), mc.leader, "mcMMO");
        }
        processed.add(id);                                                   // settled: a group was seeded, or neither face holds one
    }

    /**
     * Q6 conflict evidence: when the EM group about to seed disagrees with
     * what mcMMO shows for any of its members, log it - EM wins by rule, the
     * mcMMO grouping is not merged, and its stale memberships are cleared by
     * the ordinary login rules once their owners are settled.
     */
    private void logEmMcConflict(UUID id, EmLink.EmParty em) {
        if (!c.link().available()) return;
        Set<UUID> emSet = new HashSet<>(em.members);
        for (UUID m : em.members) {
            McmmoLink.McParty mc;
            try { mc = c.link().partyOf(m); } catch (Throwable t) { continue; }
            if (mc == null || !mc.real()) continue;
            if (mc.members.keySet().equals(emSet)) continue;                 // the faces agree: no conflict
            c.logInfo("ExcelTogether first-boot seed: conflict for " + c.nameOf(m)
                    + " - EliteMobs party " + namesOf(emSet) + " and mcMMO party '" + mc.name + "' "
                    + namesOf(mc.members.keySet()) + " disagree; EM wins (Q6, EM-first) and the mcMMO grouping is not merged.");
            return;                                                          // one conflict line per seeded group is the evidence
        }
    }

    /**
     * Adopt one face party into the roster of record as LOGIN_REBUILD. The
     * face party's leader anchors (and leads) the seeded party. If the
     * anchor is already claimed by a roster party, the groups overlap: the
     * existing roster wins, NOTHING is merged in, and the overlap is logged
     * (this is the tail of an EM/mcMMO disagreement - the EM group seeded
     * first). A non-anchor member who is already claimed is skipped and
     * logged by name; the rest of the group still seeds.
     */
    private void seedGroup(List<UUID> members, UUID leader, String face) {
        PartyManager mgr = c.mgr();
        UUID anchor = (leader != null && members.contains(leader)) ? leader : members.get(0);
        for (UUID m : members) c.nameOf(m);                                  // populate last-known names for the persisted snapshot
        if (mgr.inParty(anchor)) {
            c.logInfo("ExcelTogether first-boot seed: a " + face + " party led by " + c.nameOf(anchor)
                    + " overlaps a roster party that already exists - not merged (the roster of record wins); "
                    + "its unclaimed members stay roster-less and their stale " + face + " membership is cleared.");
            return;
        }
        int adopted = 0;
        for (UUID m : members) {
            if (m.equals(anchor)) continue;
            if (mgr.inParty(m)) {
                c.logInfo("ExcelTogether first-boot seed: " + c.nameOf(m) + " is already on a roster party - skipped while seeding the "
                        + face + " party of " + c.nameOf(anchor) + " (conflict logged, Q6).");
                continue;
            }
            PartyManager.AdoptOutcome o = mgr.adopt(m, anchor, RosterOrigin.LOGIN_REBUILD);
            if (o.result == PartyManager.AdoptResult.CREATED || o.result == PartyManager.AdoptResult.JOINED) adopted++;
            else c.logInfo("ExcelTogether first-boot seed: could not seed " + c.nameOf(m) + " into the " + face
                    + " party of " + c.nameOf(anchor) + " (" + o.result + ") - they stay roster-less.");
        }
        if (adopted > 0) {
            PartyManager.Party p = mgr.partyOf(anchor);
            c.logInfo("ExcelTogether first-boot seed: seeded a party of " + (p == null ? adopted + 1 : p.size())
                    + " from " + face + " (leader " + c.nameOf(anchor) + ") with origin LOGIN_REBUILD - the roster of record governs from here.");
        }
    }

    private String namesOf(Collection<UUID> ids) {
        List<String> ns = new ArrayList<>();
        for (UUID u : ids) ns.add(c.nameOf(u));
        return ns.toString();
    }
}

package com.winkreyes.exceltogether;

import java.util.*;

/**
 * 0.6.0 (P-PARTY-REBUILD-1, U2-D3) - the door layer for /party.
 *
 * A pure decision table (no Bukkit types, so the headless suite can pin it):
 * given the raw text of a PlayerCommandPreprocessEvent, decide whether the
 * command is a /party roster command that ExcelTogether's one door must
 * answer (ROUTE - the caller cancels the event and dispatches the returned
 * args into Controller.command), or a command that must pass through to
 * mcMMO / Bukkit untouched (PASS - the caller does nothing).
 *
 * What is claimed, and what is NOT (SCOPING.md SS2.2/SS2.4, UNIT2-PLAN U2-D3):
 *  - This class ships the MECHANISM and a CANDIDATE verb table. The candidate
 *    roster/pass-through split below is read desk-side from the mcMMO fork
 *    source (~/workspace/mcmmo-edit/repo/src/main/resources/plugin.yml,
 *    mcmmo.commands.party.* children, :1146-1172 area): accept, create,
 *    disband, invite, join, kick, owner, password, quit, rename, lock/unlock
 *    on the mutation side; chat, help, info, teleport, xpshare/itemshare on
 *    the non-mutation side.
 *  - The AUTHORITATIVE split is rig enumeration U2-R2 (live jar + live
 *    LuckPerms state), and the live claim proof is U2-R3, which carries the
 *    D74 tripwire: if /party cannot be claimed without breaking mcMMO party
 *    chat, that escalates to Mike - it is not decided here. Accordingly the
 *    verb table is CONFIGURABLE (config.yml "door:" section, wired in
 *    ExcelTogether.onEnable): the rig result replaces the candidate sets by
 *    config, with no code change and no re-hard-coding.
 *  - "chat" is in the pass-through set by default and unknown subcommands
 *    pass through: this router must never intercept party chat or a verb it
 *    does not know about. A verb only routes when it is listed.
 *
 * Labels: plain "party" and the namespaced "mcmmo:party" are the door's
 * labels. Our own Bukkit-registered labels (excelparty / eparty / together,
 * and exceltogether:party / exceltogether:excelparty) PASS here - Bukkit
 * dispatches those to ExcelTogether.onCommand, the same Controller dispatch,
 * so routing them again would double-handle. "/ptp" and every other command
 * PASS: ptp is mcMMO's own command operating on the mirrored roster.
 */
public final class PartyCommandRouter {

    public enum Decision { ROUTE, PASS }

    /** The routing decision for one raw command line. args is the Controller dispatch array (empty for a bare /party). */
    public static final class Result {
        public final Decision decision;
        public final String label;        // normalised command label, no leading slash, lower-case ("" if unparseable)
        public final String sub;          // normalised first subcommand, lower-case ("" if none)
        public final String[] args;       // subcommand + rest, original case preserved for names
        Result(Decision d, String label, String sub, String[] args) { decision = d; this.label = label; this.sub = sub; this.args = args.clone(); }
        public boolean routed() { return decision == Decision.ROUTE; }
    }

    private static Result pass(String label, String sub, String[] args) { return new Result(Decision.PASS, label, sub, args); }
    private static Result route(String label, String sub, String[] args) { return new Result(Decision.ROUTE, label, sub, args); }

    /**
     * Candidate roster verbs (fork source, desk-side): the subcommands that
     * answer roster questions or mutate the roster of record and therefore
     * belong to the door. Controller.command already dispatches every one of
     * these (join -> accept alias, quit -> leave, info -> list).
     */
    public static final Set<String> DEFAULT_ROSTER_VERBS = Set.of(
            "invite", "accept", "decline", "leave", "kick", "list", "invites", "status", "help",
            "join", "quit", "info");

    /**
     * Candidate mcMMO party-mutation verbs with NO hub equivalent (fork
     * source, desk-side): create/disband/owner/password/rename/lock/unlock/
     * alliance. They must not reach mcMMO (that would mutate the mirror
     * behind the roster's back), so the door intercepts them and answers
     * with its own help/usage line instead (Controller's default branch).
     */
    public static final Set<String> DEFAULT_INTERCEPT_VERBS = Set.of(
            "create", "disband", "owner", "password", "rename", "lock", "unlock", "alliance");

    /**
     * Candidate non-roster verbs (fork source, desk-side): party chat and
     * the ptp/share toggles operate on mcMMO's mirrored roster without
     * mutating membership (SCOPING.md SS2.3) - they pass through untouched.
     * D74/Q7. This set is documentation-first: anything not in the roster or
     * intercept sets passes anyway; the set exists so the rig enumeration
     * (U2-R2) has an explicit place to record "verified pass-through".
     */
    public static final Set<String> DEFAULT_PASS_VERBS = Set.of(
            "chat", "teleport", "xpshare", "itemshare");

    private final Set<String> rosterVerbs;
    private final Set<String> interceptVerbs;
    private final Set<String> passVerbs;

    /** The candidate (fork-source) table. */
    public PartyCommandRouter() { this(DEFAULT_ROSTER_VERBS, DEFAULT_INTERCEPT_VERBS, DEFAULT_PASS_VERBS); }

    /**
     * A configured table (config.yml "door:" section). Any null/empty set
     * falls back to its candidate default, so a partial config can never
     * accidentally empty the door. Verbs are normalised to lower-case;
     * a verb listed as pass-through is REMOVED from the routing sets -
     * pass-through always wins, so a config (or the U2-R2 rig result) can
     * hand a verb back to mcMMO without a code change, and "chat" can be
     * forced safe even if it is also listed as a roster verb.
     */
    public PartyCommandRouter(Collection<String> roster, Collection<String> intercept, Collection<String> passThrough) {
        Set<String> p = norm(passThrough, DEFAULT_PASS_VERBS);
        Set<String> r = norm(roster, DEFAULT_ROSTER_VERBS);
        Set<String> i = norm(intercept, DEFAULT_INTERCEPT_VERBS);
        r.removeAll(p); i.removeAll(p);
        this.passVerbs = Collections.unmodifiableSet(p);
        this.rosterVerbs = Collections.unmodifiableSet(r);
        this.interceptVerbs = Collections.unmodifiableSet(i);
    }

    private static Set<String> norm(Collection<String> in, Set<String> fallback) {
        if (in == null || in.isEmpty()) return new LinkedHashSet<>(fallback);
        Set<String> out = new LinkedHashSet<>();
        for (String s : in) if (s != null && !s.trim().isEmpty()) out.add(s.trim().toLowerCase(Locale.ROOT));
        return out.isEmpty() ? new LinkedHashSet<>(fallback) : out;
    }

    /** Build from config-style comma/space separated strings ("" or null = candidate defaults). */
    public static PartyCommandRouter fromStrings(String roster, String intercept, String passThrough) {
        return new PartyCommandRouter(split(roster), split(intercept), split(passThrough));
    }

    private static List<String> split(String s) {
        if (s == null || s.trim().isEmpty()) return List.of();
        List<String> out = new ArrayList<>();
        for (String t : s.split("[,\\s]+")) if (!t.isEmpty()) out.add(t);
        return out;
    }

    public Set<String> rosterVerbs() { return rosterVerbs; }
    public Set<String> interceptVerbs() { return interceptVerbs; }
    public Set<String> passVerbs() { return passVerbs; }
    public boolean isRosterVerb(String sub) { return sub != null && rosterVerbs.contains(sub.toLowerCase(Locale.ROOT)); }
    public boolean isInterceptVerb(String sub) { return sub != null && interceptVerbs.contains(sub.toLowerCase(Locale.ROOT)); }
    public boolean isPassVerb(String sub) { return sub != null && passVerbs.contains(sub.toLowerCase(Locale.ROOT)); }

    /**
     * The decision table. Raw message as Bukkit delivers it (leading "/").
     * ROUTE only for the door labels with a bare command, a roster verb, or
     * an intercept verb; PASS for everything else, including unknown
     * subcommands and every non-party command.
     */
    public Result decide(String message) {
        if (message == null) return pass("", "", new String[0]);
        String m = message.trim();
        if (m.startsWith("/")) m = m.substring(1).trim();
        if (m.isEmpty()) return pass("", "", new String[0]);
        String[] tok = m.split("\\s+");
        String label = tok[0].toLowerCase(Locale.ROOT);
        String[] args = tok.length > 1 ? Arrays.copyOfRange(tok, 1, tok.length) : new String[0];
        String sub = args.length > 0 ? args[0].toLowerCase(Locale.ROOT) : "";
        if (!label.equals("party") && !label.equals("mcmmo:party")) return pass(label, sub, args);
        if (args.length == 0) return route(label, "", args);                    // bare /party opens the door's hub
        if (passVerbs.contains(sub)) return pass(label, sub, args);             // chat / ptp-adjacent: mcMMO keeps them (Q7)
        if (rosterVerbs.contains(sub) || interceptVerbs.contains(sub)) return route(label, sub, args);
        return pass(label, sub, args);                                          // unknown: never intercept what we do not know
    }
}

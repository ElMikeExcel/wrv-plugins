package com.winkreyes.excelsummons;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Console admin surface (ExcelSummons 0.1.3, Mike's Nov 3, 2026
 * console-admin rule; console-admin coverage audit Nov 9, 2026,
 * AUDIT.md §4 — the ExcelSummons packet).
 *
 * Through 0.1.2 an operator could read the GLOBAL summon count
 * from the console and reload settings, but had NO way to inspect
 * or fix ONE player's summons: no list-for-player, no
 * dismiss-for-player. A player whose summons were stuck could
 * only be fixed by that player, in game. The audit graded 0.1.2
 * FAIL on exactly that gap.
 *
 * 0.1.3 adds exactly the audit's packet, shaped on the AutoSort
 * 0.2.1 / Downed 0.1.2 precedent (one minor bump, admin
 * subcommands on the existing command, harness tests for the new
 * subcommands) — extending the EXISTING admin branch:
 *
 *   /summons admin list <player>          — print that player's
 *                              summons to the operator, in the
 *                              self-list line format, plus the
 *                              held-total line. Read-only.
 *   /summons admin dismiss <player>       — dismiss that player's
 *                              mount only (the existing dismiss
 *                              path with all=false).
 *   /summons admin dismiss <player> all   — dismiss the mount AND
 *                              all combat summons (all=true).
 *                              Companions and pack summons are
 *                              never touched by either form.
 *
 * 0.1.4 (FIX-PACKET-DESIGN §2.4) adds the PETS scope alongside
 * `all`, for BOTH the self dismiss and the admin dismiss:
 *
 *   /summons dismiss pets                 — dismiss the player's
 *                              own CAST-origin companion + pack
 *                              pets (the player-controlled release
 *                              for the new companion mana reserve).
 *   /summons admin dismiss <player> pets  — the same selection on
 *                              the target.
 *
 * Adopted (hand-tamed) pets are NEVER selected by any scope: a
 * dismiss removes the entity, and an adopted pet is the player's
 * own animal, not a conjured spell pet. Bare dismiss and
 * `dismiss all` keep their exact 0.1.3 meanings.
 *
 * Both are gated by the EXISTING node excelsummons.admin
 * (plugin.yml, default op — the audit's explicit condition).
 * The console is allowed explicitly for these admin forms (the
 * ExcelCosmetics / BrainGames / Downed shape:
 * console || hasPermission); the player-facing paths — self
 * list, self dismiss, the bare usage lines, admin reload, and
 * the console global count — are behavior-unchanged.
 *
 * Target resolution is an ONLINE player by name, exact and
 * case-insensitive. There is deliberately no offline form: the
 * registry's records belong to live entities, and a logged-off
 * caster's combat summons already leave with them.
 *
 * This class is the pure decision layer — routing, the gate,
 * target-name matching, the dismiss target selection, and the
 * audit-line format — with no Bukkit types, so the harness
 * (SummonsTest) exercises every branch. SummonsListener.dismiss
 * selects its targets through dismissTargets below, so the
 * selection the harness proves is the selection that runs live.
 * The Bukkit mechanics (online-player lookup, the dismiss call,
 * messages) live in ExcelSummons and stay thin.
 *
 * Excel — Wink Reyes Valley (Iris / Mike).
 */
public final class AdminPolicy {
    private AdminPolicy() { }

    /** The existing admin node. Registered in plugin.yml, default op. */
    public static final String PERMISSION = "excelsummons.admin";

    /** What a /summons invocation routes to. */
    public enum Route {
        /** Not an admin list/dismiss invocation: the pre-0.1.3
         *  surface (bare usage, self list, self dismiss, the
         *  console global count, admin reload, admin usage),
         *  unchanged. */
        LEGACY,
        /** admin list <player>, gate passed. */
        ADMIN_LIST,
        /** admin dismiss <player> [all], gate passed. */
        ADMIN_DISMISS,
        /** admin list/dismiss invocation with a missing/extra/
         *  blank target or a bad dismiss scope token. */
        ADMIN_USAGE,
        /** admin list/dismiss invocation from a sender without
         *  the gate. */
        ADMIN_DENIED
    }

    /** Which summons a dismiss takes (§2.4). MOUNT_ONLY is the
     *  bare dismiss (the mount, always); ALL adds every combat
     *  summon; PETS takes CAST-origin companion + pack pets only —
     *  never the mount, never combat, never an adopted pet. */
    public enum DismissScope { MOUNT_ONLY, ALL, PETS }

    /** A routed invocation; target is non-null only for
     *  ADMIN_LIST / ADMIN_DISMISS. all / scope are meaningful only
     *  for ADMIN_DISMISS (all == (scope == ALL), kept for the
     *  0.1.3 callers). */
    public record Parsed(Route route, String target, boolean all, DismissScope scope) {
        static Parsed of(Route route) {
            return new Parsed(route, null, false, DismissScope.MOUNT_ONLY);
        }
    }

    /**
     * The admin gate: the console always may (explicit, per the rule's
     * reference implementations); any other sender needs the node.
     * Bukkit's console satisfies hasPermission for an op-default node
     * anyway — the explicit console term keeps the intent true even if
     * a permission plugin ever re-resolves console grants.
     */
    public static boolean mayUseAdmin(boolean consoleSender, boolean holdsAdmin) {
        return consoleSender || holdsAdmin;
    }

    /** Is this second argument one of the 0.1.3 admin verbs?
     *  (The first argument is "admin" — these verbs extend the
     *  existing admin branch, they are not new top-level forms.) */
    public static boolean isAdminVerb(String arg) {
        return arg != null && ("list".equalsIgnoreCase(arg) || "dismiss".equalsIgnoreCase(arg));
    }

    /**
     * Route one /summons invocation. Anything that is not
     * "admin list …" / "admin dismiss …" is the legacy surface,
     * byte-for-byte the pre-0.1.3 behavior (including
     * "admin reload" and a bare "admin"). An admin list/dismiss
     * invocation is decided here completely: gate first (a sender
     * without the gate learns nothing about targets), then the
     * exact target form.
     */
    public static Parsed route(String[] args, boolean consoleSender, boolean holdsAdmin) {
        if (args == null || args.length < 2 || !"admin".equalsIgnoreCase(args[0])
                || !isAdminVerb(args[1]))
            return Parsed.of(Route.LEGACY);
        if (!mayUseAdmin(consoleSender, holdsAdmin)) return Parsed.of(Route.ADMIN_DENIED);
        if ("list".equalsIgnoreCase(args[1])) {
            if (args.length == 3 && !args[2].isBlank()) {
                return new Parsed(Route.ADMIN_LIST, args[2], false, DismissScope.MOUNT_ONLY);
            }
            return Parsed.of(Route.ADMIN_USAGE);
        }
        // dismiss <player> [all|pets]
        if (args.length == 3 && !args[2].isBlank()) {
            return new Parsed(Route.ADMIN_DISMISS, args[2], false, DismissScope.MOUNT_ONLY);
        }
        if (args.length == 4 && !args[2].isBlank() && "all".equalsIgnoreCase(args[3])) {
            return new Parsed(Route.ADMIN_DISMISS, args[2], true, DismissScope.ALL);
        }
        if (args.length == 4 && !args[2].isBlank() && "pets".equalsIgnoreCase(args[3])) {
            return new Parsed(Route.ADMIN_DISMISS, args[2], false, DismissScope.PETS);
        }
        return Parsed.of(Route.ADMIN_USAGE);
    }

    /**
     * Target-name matching: EXACT and case-insensitive — the
     * operator names the whole player, never a prefix. Null-safe.
     */
    public static boolean nameMatches(String playerName, String target) {
        return playerName != null && target != null && playerName.equalsIgnoreCase(target);
    }

    /**
     * Which branches a dismiss scope takes, BEFORE the origin
     * check (§2.4): MOUNT_ONLY and ALL take the mount (ALL adds
     * combat); PETS takes companion + pack branches. Origin is a
     * record-level fact checked in dismissTargets: the PETS scope
     * additionally requires origin == CAST, so an adopted pet is
     * never selected even though its branch matches.
     */
    public static boolean dismisses(SummonRecord.Branch branch, DismissScope scope) {
        if (scope == null) scope = DismissScope.MOUNT_ONLY;
        switch (scope) {
            case ALL:
                return branch == SummonRecord.Branch.MOUNT
                        || branch == SummonRecord.Branch.COMBAT;
            case PETS:
                return branch == SummonRecord.Branch.COMPANION
                        || branch == SummonRecord.Branch.PACK;
            case MOUNT_ONLY:
            default:
                return branch == SummonRecord.Branch.MOUNT;
        }
    }

    /**
     * Which of one owner's records a dismiss takes (the dismiss
     * path's own rule, unchanged from 0.1.0): the mount always;
     * with {@code all}, combat summons too. Companions and pack
     * summons are family — a dismiss never touches them. The
     * input is already scoped to one owner by
     * SummonRegistry.ofOwner; other owners are never in the list.
     */
    public static boolean dismisses(SummonRecord.Branch branch, boolean all) {
        return dismisses(branch, all ? DismissScope.ALL : DismissScope.MOUNT_ONLY);
    }

    /** The records a dismiss of {@code ownerRecords} would take,
     *  in registry order. SummonsListener.dismiss runs exactly
     *  this selection and despawns each record it returns. Under
     *  the PETS scope only CAST-origin records are selected —
     *  adopted pets survive every dismiss (§2.4 safety). */
    public static List<SummonRecord> dismissTargets(List<SummonRecord> ownerRecords,
                                                    DismissScope scope) {
        List<SummonRecord> out = new ArrayList<>();
        if (ownerRecords == null) return out;
        if (scope == null) scope = DismissScope.MOUNT_ONLY;
        for (SummonRecord rec : ownerRecords) {
            if (rec == null || !dismisses(rec.branch(), scope)) continue;
            if (scope == DismissScope.PETS
                    && rec.origin() != SummonRecord.Origin.CAST) continue;
            out.add(rec);
        }
        return out;
    }

    /** The 0.1.3 boolean form — delegates to the scope form so the
     *  two can never drift (all == ALL, bare == MOUNT_ONLY). */
    public static List<SummonRecord> dismissTargets(List<SummonRecord> ownerRecords,
                                                    boolean all) {
        return dismissTargets(ownerRecords,
                all ? DismissScope.ALL : DismissScope.MOUNT_ONLY);
    }

    /**
     * The INFO audit line for an admin dismiss. It names the
     * OPERATOR actor (CONSOLE or the operator's name) — an admin
     * dismiss is recorded as what it is, never as the target
     * player's own action.
     */
    public static String auditLine(String actor, String targetName, UUID targetId,
                                   int dismissed, boolean all) {
        return auditLine(actor, targetName, targetId, dismissed,
                all ? DismissScope.ALL : DismissScope.MOUNT_ONLY);
    }

    /** Scope form of the audit line (0.1.4): the scope phrase names
     *  exactly what the scope takes — "mount only", "mount + all
     *  combat", or "cast pets only". */
    public static String auditLine(String actor, String targetName, UUID targetId,
                                   int dismissed, DismissScope scope) {
        String who = actor == null || actor.isBlank() ? "CONSOLE" : actor;
        String phrase;
        if (scope == null) scope = DismissScope.MOUNT_ONLY;
        switch (scope) {
            case ALL:  phrase = "mount + all combat"; break;
            case PETS: phrase = "cast pets only"; break;
            case MOUNT_ONLY:
            default:   phrase = "mount only"; break;
        }
        return "Summons admin dismiss by " + who + " for " + targetName
                + " (" + targetId + "): dismissed " + dismissed + " summon(s) ("
                + phrase + ").";
    }
}

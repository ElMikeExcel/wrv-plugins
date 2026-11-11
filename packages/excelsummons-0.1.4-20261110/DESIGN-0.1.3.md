# ExcelSummons 0.1.3 — console admin (design + root cause)

An Excel plugin note for Wink Reyes Valley. November 10, 2026.
Commissioned from the console-admin coverage audit (Nov 9, 2026,
AUDIT.md §4), under Mike's Nov 3, 2026 console-admin rule.

## The problem (verified from the 0.1.2 source, Nov 9–10, 2026)

1. `/summons` offered a console operator exactly two acts: a global
   count (`list` from a non-player prints "The registry holds N
   summon(s).") and `admin reload` — both already worked from
   console under `excelsummons.admin`.
2. NO subcommand could inspect or fix ONE player's summons: no
   `list <player>`, no dismiss-for-player. A player whose summons
   were stuck (a mount that will not leave, combat summons holding
   mana reserve for a caster who cannot dismiss them) could only
   be fixed by that player, in game. The audit graded 0.1.2 FAIL
   on exactly that gap.

## The fix (this delta, and nothing else)

Shaped on the AutoSort 0.2.1 / Downed 0.1.2 precedent: one minor
bump, admin subcommands extending the EXISTING `admin` branch of
the existing command, console explicitly allowed for the admin
branch, player-facing paths unchanged, harness tests for the new
subcommands.

- Gate: the EXISTING node `excelsummons.admin` (plugin.yml,
  default **op** — the audit's explicit condition). No new
  permission node is created.
- New pure class `AdminPolicy`: routing, the gate, target-name
  matching, the dismiss target selection, and the audit-line
  format — no Bukkit types, so every branch is harness-testable.
  The gate is the ExcelCosmetics / BrainGames / Downed shape:
  `console || holdsAdmin`. Denial precedes all target handling.
- `ExcelSummons.onCommand` routes `admin list` / `admin dismiss`
  through AdminPolicy inside the existing `admin` branch, BEFORE
  the legacy admin body; every pre-0.1.3 branch (self `list`,
  self `dismiss [all]`, the bare usage lines, `admin reload`, and
  the console global count) is behavior-unchanged. The one shared
  refactor: the per-summon list line is rendered by one formatter
  (`Nameplate.listLine`) used by BOTH the self list and the admin
  list, and `SummonsListener.dismiss` selects its targets through
  `AdminPolicy.dismissTargets` — the exact selection the harness
  tests exercise. Rendered strings and dismiss semantics are
  byte-for-byte the 0.1.2 ones.
- Forms:
  - `/summons admin list <player>` — print that player's summons
    to the operator, in the SAME line format as the self list
    (spell, branch, time left, HP, mana hold), under a
    "Summons for <player>:" header, plus the held-total line
    ("Mana held by <player>'s summons: N% of their pool.") when
    combat summons hold reserve. Read-only: no state changes.
  - `/summons admin dismiss <player>` — dismiss that player's
    MOUNT only, through the EXISTING dismiss path
    (`SummonsListener.dismiss` with all=false) — exactly the
    self-dismiss without `all`.
  - `/summons admin dismiss <player> all` — dismiss the mount AND
    all combat summons (all=true), exactly the self
    `dismiss all`. Companions and pack summons are never touched
    by either form — the dismiss path's own rule, unchanged.
    The count is reported to the operator, and an INFO audit line
    names the operator actor (CONSOLE or operator name) — an
    admin dismiss never masquerades as the player's own action.
- Target resolution: an ONLINE player by name, exact and
  case-insensitive (`AdminPolicy.nameMatches` — no prefix or
  partial matching). An unknown or offline target is a clean
  operator message ("Summons admin: no player named '<target>'
  is online. Nothing was changed.") and no state change, for
  both verbs. There is no offline form: the registry's records
  belong to live entities, and a logged-off caster's combat
  summons already leave with them.
- Deliberate limits, stated plainly:
  - No tab-completion surface is added (0.1.2 registered none);
    the admin verbs are operator/console surface, typed in full.
  - The target player is not sent a message by the admin forms:
    the operator report + the INFO audit line are the record
    (a dismissed summon's own despawn is visible in game).
  - No gameplay value changes: caps 3/12, reserve percentages
    10/15/25/20/25, taunt radius 16, every config number — all
    as ruled. config.yml is byte-identical to 0.1.2.

## Proof shape

- SummonsTest: the 43 carried 0.1.2 tests unmodified at their
  totals, plus new AdminPolicy tests — gate matrix (console
  allowed; operator allowed; non-admin denied; the node is the
  existing `excelsummons.admin`), routing for both verbs
  (targets carried, case-insensitive, the `all` flag), denial
  preceding usage/targets, usage errors (missing/extra/blank
  target, bad dismiss scope token), target-name matching
  (exact, case-insensitive, no partial), dismiss all vs
  mount-only semantics exercised against a real SummonRegistry
  (companions/pack and other owners untouched), list-line
  format pins (the 0.1.2 self-list strings, byte-for-byte),
  legacy-surface regression pins for every sender kind, the
  audit-line pin, and plugin.yml/BuildInfo pins.
- Desk rig (Paper 26.3 + Magic 11.2.5, NMS fake players, real
  spawned summon entities, console dispatches through the real
  console sender): clean 0.1.3 enable, SEVERE 0, console admin
  list rendering a player's real summons, console admin dismiss
  mount-only vs `all` semantics on the live registry, unknown
  target no-ops, usage errors, a non-admin player's admin list
  AND dismiss both denied by state, and the legacy console
  global count + admin reload unchanged. Results:
  rig-0.1.3/RIG-RESULTS.md in the build folder.

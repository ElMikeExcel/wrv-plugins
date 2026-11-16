# ExcelDowned 0.1.2 — console admin (design + root cause)

An Excel plugin note for Wink Reyes Valley. November 10, 2026.
Commissioned from the console-admin coverage audit (Nov 9, 2026,
AUDIT.md §4), under Mike's Nov 3, 2026 console-admin rule.

## The problem (verified from the 0.1.1 source, Nov 9–10, 2026)

1. `/downed` offered a console operator exactly two acts: inspect a
   player (`/downed <player>`) and reload settings — both already
   worked from console under `exceldowned.admin`.
2. NO subcommand could mutate another player's session: no revive,
   no clear, no force-resolve. A player wedged in the downed state
   (a revive that never lands, a durable session restored from
   downed.yml for an unreachable player) could only bleed out on
   the 300 s clock or be waited out. The audit graded 0.1.1 FAIL
   on exactly that gap.

## The fix (this delta, and nothing else)

Shaped on the AutoSort 0.2.1 / BuildMode 1.3.3 precedent: one minor
bump, admin subcommands on the existing command, console explicitly
allowed for the admin branch only, player-facing paths unchanged,
harness tests for the new subcommands.

- Gate: the EXISTING node `exceldowned.admin` (plugin.yml, default
  **op** — the audit's explicit condition: "gated by the existing
  exceldowned.admin"). No new permission node is created.
- New pure class `AdminPolicy`: routing, the gate, target
  classification, and the revive/clear decisions — no Bukkit types,
  so every branch is harness-testable. The gate is the
  ExcelCosmetics / BrainGames shape: `console || holdsAdmin`.
  Denial precedes all target handling.
- `DownedCommands` routes `revive`/`clear` through AdminPolicy
  before the legacy surface; every pre-0.1.2 branch (self status,
  gated `<player>` inspection, gated `reload`, and `/giveup`) is
  behavior-unchanged.
- Forms:
  - `/downed revive <player>` — force-resolve an ONLINE downed
    session without death, through the EXISTING revive path (the
    same exactly-once session consumption the Resurrection spell
    uses; the target returns at the configured revive fraction —
    50% — with presentation cleared and the DownedReviveEvent
    fired). The record is `admin_revive` naming the operator actor
    (CONSOLE or operator name), never a fake spell cast; an INFO
    log line carries the same audit trail.
  - `/downed clear <player>` — remove a downed session outright:
    state surgery for a stuck session. Works online (presentation
    cleared) and OFFLINE (the durable downed.yml record is dropped,
    so the player returns standing at next login). Deliberately NOT
    a heal and NOT a death: no health is written, no death path
    runs, nothing drops. The record is `admin_clear` with the
    actor and the online flag.
- Deliberate limits, stated plainly:
  - Revive requires the target ONLINE (it restores live health
    through the spell's path). An offline-durable target is
    reported with its remaining bleed-out clock and left untouched;
    clear is the offline tool.
  - No tab-completion surface is added (0.1.1 registered none);
    the admin verbs are operator/console surface, typed in full.
  - No gameplay value changes: bleed-out 300 s, downed factor
    0.25, Resurrection mana 100 / stage-4 unlock (the spells
    packet, untouched), the /giveup button menu — all as ruled.
    config.yml is byte-identical to 0.1.1.

## Proof shape

- DownedTest 27/27: the 17 carried 0.1.0/0.1.1 state-machine tests
  unmodified, plus 10 new AdminPolicy tests — gate matrix, routing
  for both verbs (console + operator, case-insensitive), denial
  preceding usage/targets, usage errors, legacy-surface regression
  pins for every sender kind, target classification, the
  revive/clear decision matrices, admin acts exercised against a
  real session registry (revive consumes exactly once and never
  touches the death latch; clear removes a persistence-restored
  offline session), and plugin.yml/build pins.
- Desk rig (Paper 26.3 + Magic 11.2.5, NMS fake players, console
  dispatches through the real console sender): 17/17 checks —
  console revive at 50% with no death, console clear restoring
  nothing, redundant/unknown/usage no-ops, the offline pair
  (revive refuses, clear removes, player returns standing), a
  non-admin player's revive AND clear both denied by state, and
  the legacy console status + /giveup rejection unchanged.
  SEVERE 0. Results: rig-0.1.2/RIG-RESULTS.md.

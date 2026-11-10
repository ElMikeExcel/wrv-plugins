# ExcelDetails 0.2.1 — DESIGN (party-scoped display)

Commission: Mike's live report + ruling, Nov 9, 2026 (~5:11 PM CST):
the meter "seems to be working but it's showing me people's
information that are not in my party. It should only show people if
they are in my party not everyone on the server." Parent
source-verified the defect: 0.2.0 keeps ONE server-wide
SegmentTracker and SidebarRenderer rendered tracker.ranked() with
no party filter. (Marvin's and Jester's "working as intended"
verdicts are consistent with the defect, not against it: in a party
together, a party-scoped and a server-wide view look the same.)

## The fix — display scoping only

1. **The segment does NOT split.** Tracking, the segment clock,
   close/merge semantics, and overall totals stay exactly as 0.2.0:
   one shared server-wide encounter segment. Only the ROWS a viewer
   is shown change. (Splitting segments per party would fork the
   segment clock and the overall totals for zero player benefit.)
2. **Scope set per viewer** = {viewer} ∪ ExcelTogether party
   members ∪ EliteMobs party members. Mike's confirmed default:
   either party system counts. Solo → {viewer} → own rows only.
3. **Filter point:** SidebarRenderer.linesFor — ranked rows are
   reduced by the pure PartyScope.visibleRows before sections are
   composed; each section's total is recomputed over the FILTERED
   rows (PartyScope.totalOf), so the share % is within-group.
   Own-row pinning, budget shedding, and the no-data → no-box rule
   are 0.2.0's, now operating on the filtered set: a viewer with no
   in-scope rows sees no box.
4. **Seams (PartyLookup), both soft, reflection only** — no compile
   or load-order dependency added:
   - EliteMobs 10.9.12: PartyManager.getParty(UUID) →
     Party.getMembers() (verified by javap against the actual jar,
     ~/workspace/elitemobs-10.9.12/EliteMobs-10.9.12.jar).
   - ExcelTogether 0.5.3: plugin field `mgr` → partyOf(UUID) →
     Party.members (source: records package
     exceltogether-0.5.3-20261104).
   Each seam logs BOUND/UNBOUND once; any error fails soft to an
   empty member set for that system. The failure direction is
   safe by construction: a dead seam can only HIDE party rows,
   never show a stranger's.
5. **Everywhere else another player's rows could surface — audited:**
   - /details status: the sender's OWN current stats only (0.2.0,
     unchanged) — already private.
   - There is NO drill-down command in 0.2.0/0.2.1: the by-target /
     by-source breakdown maps are tracked but never rendered to any
     viewer. Nothing to scope.
   - %details_show_*% placeholders: the requesting player's own
     checkbox prefs — already private.
   - Per-player overall totals: keyed by player, only ever read
     back to that same player — already private (verified in
     DetailsCommands/DataFile paths).
   - The console segment-close announce carries no player rows.
   So the sidebar was the only cross-player surface, and it is the
   one this build scopes.

## Not changed

Checkbox mix, section composition + shedding, piggyback math,
persistence (data.yml format identical — a 0.2.1 ↔ 0.2.0 jar swap
needs no data migration either way), churn fix, placeholders,
commands, config keys/values (bundled config.yml byte-identical to
0.2.0's — the fix needs no config).
